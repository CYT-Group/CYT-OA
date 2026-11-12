const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const vue = require('vue');
const { parse, compileScript } = require('@vue/compiler-sfc');

function trackerFixture({ latest = null, allowed = true, started } = {}) {
  const timers = new Map(), reads = [], finished = [];
  let counter = 0, starts = 0, nextStatus = latest, pendingStart;
  const source = fs.readFileSync(path.join(__dirname, '../src/lib/hr-sync.ts'), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText;
  const context = { exports: {}, Error, Promise, setTimeout: (fn, delay) => { const id = ++counter; timers.set(id, { fn, delay }); return id; }, clearTimeout: id => timers.delete(id), require: name => { if (name === 'vue') return vue; throw new Error(name); } };
  vm.runInNewContext(code, context);
  const tracker = context.exports.createHrSyncTracker({
    canStart: () => allowed,
    start: async () => { starts++; if (pendingStart) return pendingStart; return started || running(); },
    status: async id => { reads.push(id); if (nextStatus instanceof Error) throw nextStatus; return typeof nextStatus === 'function' ? nextStatus(id) : nextStatus; },
    onFinished: run => finished.push(run),
  });
  return { tracker, timers, reads, finished, status: value => { nextStatus = value; }, holdStart: value => { pendingStart = value; }, starts: () => starts };
}
function running(id = 'run-1', stage = 'directory') {
  return { id, status: 'running', stage, actor: '测试人事', message: '', createdAt: '2026-11-12T09:00:00', finishedAt: null, directory: null, result: null };
}
function completed(status = 'partial', id = 'run-1') {
  return { ...running(id), status, stage: 'complete', finishedAt: '2026-11-12T09:01:00', directory: { created: 2, updated: 5, departments: 3, conflicts: 1, missing: 1 }, result: { scanned: 10, requested: 9, succeeded: 7, failed: 1, skipped: 2, unchanged: 4, updatedEmployees: 3, updatedFields: 6, failures: [{ employeeId: '9', name: '失败员工', reason: '企微暂不可用' }], skippedEmployees: [{ employeeId: 10, name: '保留员工', reason: '未返回人事信息，保留本地状态' }], conflicts: [], checkedAt: '2026-11-12T09:01:00' } };
}

test('刷新恢复历史部分成功及全部统计，不重新轮询或伪报本次完成', async () => {
  const p = trackerFixture({ latest: completed() });
  await p.tracker.refresh();
  assert.equal(p.tracker.run.value.status, 'partial');
  assert.equal(p.tracker.run.value.result.scanned, 10);
  assert.equal(p.tracker.run.value.result.requested, 9);
  assert.equal(p.tracker.run.value.result.skipped, 2);
  assert.equal(p.timers.size, 0);
  assert.equal(p.finished.length, 0);
});

test('恢复运行任务，按当前runId读取目录和人事阶段，终态只刷新一次', async () => {
  const p = trackerFixture({ latest: running(123) });
  await p.tracker.refresh();
  assert.deepEqual(p.reads, [undefined]);
  assert.equal(p.tracker.syncing.value, true);
  assert.equal([...p.timers.values()][0].delay, 3000);
  p.status({ ...running(123, 'hr'), directory: { created: 1, updated: 2, departments: 3, conflicts: 0, missing: 0 } });
  await p.tracker.refresh();
  assert.equal(p.reads.at(-1), 123);
  assert.equal(p.tracker.run.value.stage, 'hr');
  p.status(completed('partial', 123));
  await p.tracker.refresh();
  assert.equal(p.tracker.syncing.value, false);
  assert.equal(p.timers.size, 0);
  assert.equal(p.finished.length, 1);
  await p.tracker.refresh();
  assert.equal(p.finished.length, 1);
  assert.equal(p.reads.at(-1), undefined);
});

test('状态暂时失败或任务未读到时保留运行态，不将其当作同步完成', async () => {
  const p = trackerFixture({ latest: running() });
  await p.tracker.refresh();
  p.status(new Error('暂时无法读取状态'));
  await p.tracker.refresh();
  assert.equal(p.tracker.syncing.value, true);
  assert.match(p.tracker.statusError.value, /暂时无法读取/);
  assert.equal([...p.timers.values()][0].delay, 5000);
  p.status(null);
  await p.tracker.refresh();
  assert.equal(p.tracker.syncing.value, true);
  assert.match(p.tracker.statusError.value, /尚未确认/);
  assert.equal(p.finished.length, 0);
});

test('目录失败保留失败阶段和原因，没有HR结果时不编造零成功统计', async () => {
  const p = trackerFixture({ latest: running() });
  await p.tracker.refresh();
  p.status({ ...running(), status: 'failed', message: '通讯录读取失败', finishedAt: '2026-11-12T09:01:00' });
  await p.tracker.refresh();
  assert.equal(p.tracker.run.value.stage, 'directory');
  assert.equal(p.tracker.run.value.result, null);
  assert.equal(p.tracker.run.value.message, '通讯录读取失败');
  assert.equal(p.timers.size, 0);
});

test('不完整的旧响应不能被当作同步成功，初始读取失败也不凭空持续轮询', async () => {
  const p = trackerFixture({ latest: { scanned: 10, updatedFields: 0, failures: [] } });
  await p.tracker.refresh();
  assert.equal(p.tracker.run.value, null);
  assert.match(p.tracker.statusError.value, /格式不完整/);
  assert.equal(p.timers.size, 0);
  assert.equal(p.finished.length, 0);
});

test('启动立即跟踪异步Run，启动请求未返回前阻止重复同步', async () => {
  const p = trackerFixture();
  let release;
  p.holdStart(new Promise(resolve => { release = resolve; }));
  const first = p.tracker.start();
  await p.tracker.start();
  assert.equal(p.starts(), 1);
  assert.equal(p.tracker.syncing.value, true);
  release(running());
  await first;
  assert.equal(p.tracker.run.value.stage, 'directory');
  assert.equal(p.timers.size, 1);
});

test('只有查询权限时仍可恢复记录，缺少目录同步权限不能启动', async () => {
  const p = trackerFixture({ latest: completed('success'), allowed: false });
  await p.tracker.refresh();
  await p.tracker.start();
  assert.equal(p.tracker.run.value.status, 'success');
  assert.equal(p.starts(), 0);
  assert.equal(p.timers.size, 0);
});

test('启动响应丢失后查询最新任务，发现后台已开始就继续跟踪', async () => {
  const p = trackerFixture({ latest: running('recovered') });
  p.holdStart(Promise.reject(new Error('请求超时')));
  await p.tracker.start();
  assert.equal(p.tracker.run.value.id, 'recovered');
  assert.equal(p.tracker.syncing.value, true);
  assert.equal(p.tracker.startError.value, '');
  assert.equal(p.timers.size, 1);
});

test('离开页面清除计时器，晚到的启动和状态响应不再触发轮询或刷新', async () => {
  const p = trackerFixture();
  let release;
  p.holdStart(new Promise(resolve => { release = resolve; }));
  const started = p.tracker.start();
  p.tracker.dispose();
  release(running());
  await started;
  assert.equal(p.timers.size, 0);
  assert.equal(p.tracker.run.value, null);
  const q = trackerFixture({ latest: running() });
  await q.tracker.refresh();
  let finish;
  q.status(() => new Promise(resolve => { finish = resolve; }));
  const status = q.tracker.refresh();
  q.tracker.dispose();
  finish(completed('success'));
  await status;
  await q.tracker.refresh();
  assert.equal(q.timers.size, 0);
  assert.equal(q.finished.length, 0);
  assert.equal(q.reads.length, 2);
});

function employeePage(permissions, query = {}) {
  const source = fs.readFileSync(path.join(__dirname, '../src/pages/EmployeePage.vue'), 'utf8');
  const script = compileScript(parse(source).descriptor, { id: 'employee-sync-test' }).content;
  const code = ts.transpileModule(script, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText;
  const replacements = [];
  const context = { exports: {}, require(name) {
    if (name === 'vue') return { ...vue, onMounted() {}, onBeforeUnmount() {} };
    if (name === 'vue-router') return { useRoute: () => ({ query }), useRouter: () => ({ replace: value => replacements.push(value), push() {} }) };
    if (name === '../lib/session') return { session: { has: permission => permissions.includes(permission), isAdmin: false } };
    if (name === '../lib/api') return { api: { get: async () => [], post: async () => running() } };
    if (name === '../lib/hr-workspace') return { hrBase: '/admin-api/hrm/workspace' };
    if (name === '../lib/useTableSort') return { useTableSort: rows => ({ sortKey: vue.ref(''), sortDir: vue.ref('asc'), sorted: rows, toggleSort() {} }) };
    if (name === '../lib/hr-sync') return { createHrSyncTracker: () => ({ run: vue.ref(null), syncing: vue.ref(false), reading: vue.ref(false), startError: vue.ref(''), statusError: vue.ref(''), refresh() {}, start() {}, dispose() {} }) };
    if (name.endsWith('.vue') || name === '@lucide/vue') return {};
    throw new Error(name);
  } };
  vm.runInNewContext(code, context);
  return { state: context.exports.default.setup({}, { expose() {} }), replacements };
}

test('花名册同步按钮需要查询、维护及目录同步三项权限，任职选项含放弃入职', () => {
  const all = ['hrm:employee:query', 'hrm:employee:update', 'system:directory:sync'];
  assert.equal(employeePage(all).state.canSyncHr.value, true);
  for (const missing of all) assert.equal(employeePage(all.filter(p => p !== missing)).state.canSyncHr.value, false, missing);
  assert.ok(employeePage(all).state.entryFilterOptions.some(option => option.value === '5' && option.label === '放弃入职'));
});

test('在职、试用、离职分类沿用服务端状态分类，切换时移除过期筛选', () => {
  for (const [query, expected] of [[{ roster: 'active' }, 11], [{ statusCategory: '2' }, 2], [{ roster: 'left' }, 15], [{ roster: 'all' }, undefined]])
    assert.equal(employeePage([], query).state.statusCategory.value, expected);
  const p = employeePage([], { roster: 'left', entryStatus: '4', employeeId: '22', surveyType: '2' });
  p.state.setRoster('probation');
  const next = p.replacements[0].query;
  assert.equal(next.statusCategory, '2');
  for (const key of ['roster', 'entryStatus', 'employeeId', 'surveyType']) assert.equal(key in next, false, key);
});
