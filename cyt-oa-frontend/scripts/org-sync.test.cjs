const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const { parse, compileScript } = require('@vue/compiler-sfc');
const vue = require('vue');

// Exercise the page's setup and async transitions without starting a real sync.
function page(initialStatus) {
  const mounted = [];
  const unmounted = [];
  const timers = new Map();
  const notifications = [];
  let currentStatus = initialStatus;
  let rejectStatus = false;
  let nextTimer = 0;
  let starts = 0;
  let finishStart;
  let getResponse = () => [];
  const source = fs.readFileSync(path.join(__dirname, '../src/pages/OrgPage.vue'), 'utf8');
  const { descriptor } = parse(source);
  const script = compileScript(descriptor, { id: 'org-sync-test' }).content;
  const output = ts.transpileModule(script, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText;
  const schedule = (fn, delay) => { const id = ++nextTimer; timers.set(id, { fn, delay }); return id; };
  const context = {
    exports: {},
    setTimeout: schedule,
    clearTimeout: id => timers.delete(id),
    setInterval: schedule,
    clearInterval: id => timers.delete(id),
    require(name) {
      if (name === 'vue') return { ...vue, onMounted: fn => mounted.push(fn), onBeforeUnmount: fn => unmounted.push(fn) };
      if (name === 'vue-router') return { useRouter: () => ({ push() {} }) };
      if (name === '../lib/session') return { session: { has: () => true } };
      if (name === '../lib/toast') return { toast: (...args) => notifications.push(args) };
      if (name === '../lib/tone') return { toneBadge: value => value };
      if (name === '../lib/api') return { api: {
        async get(url, params) {
          if (url.endsWith('/sync-status')) {
            if (rejectStatus) throw new Error('temporarily offline');
            return currentStatus;
          }
          return getResponse(url, params);
        },
        async post() {
          starts++;
          if (finishStart) await finishStart;
          currentStatus = { id: 'new', status: 'running' };
          return currentStatus;
        }
      } };
      if (name.endsWith('.vue') || name === '@lucide/vue') return {};
      throw new Error('Unexpected dependency: ' + name);
    }
  };
  vm.runInNewContext(output, context);
  const state = context.exports.default.setup({}, { expose() {} });
  return {
    state, timers, notifications,
    respond: fn => { getResponse = fn; },
    mount: async () => { for (const fn of mounted) await fn(); },
    unmount: () => unmounted.forEach(fn => fn()),
    status: value => { currentStatus = value; },
    offline: value => { rejectStatus = value; },
    starts: () => starts,
    holdStart: promise => { finishStart = promise; }
  };
}

test('重新进入页面不把历史成功记录当作新通知，仍保留更新时间', async () => {
  const p = page({ id: 'old', status: 'success', finishedAt: '2026-11-11T06:30:00' });
  await p.mount();
  assert.equal(p.state.showSyncNotice.value, false);
  assert.equal(p.state.latest.value.finishedAt, '2026-11-11T06:30:00');
  assert.equal(p.notifications.length, 0);
  assert.equal(p.timers.size, 0);
});

test('同步运行时可见进度，成功后移除横幅并发送一次完成通知', async () => {
  const p = page(null);
  await p.mount();
  await p.state.runSync();
  assert.equal(p.state.showSyncNotice.value, true);
  p.status({ id: 'new', status: 'success', created: 2, updated: 176, departments: 65 });
  await p.state.pollSync();
  assert.equal(p.state.showSyncNotice.value, false);
  assert.equal(p.notifications.length, 1);
  assert.match(p.notifications[0][0], /同步完成.*新增 2 人.*更新 176 人/);
  p.unmount();
  assert.equal(p.timers.size, 0);
});

test('失败提示持续到关闭，新失败记录不会被旧的关闭操作隐藏', async () => {
  const p = page({ id: 'failed-1', status: 'failed', message: 'connection failed' });
  await p.mount();
  assert.equal(p.state.showSyncNotice.value, true);
  p.state.dismissedRunId.value = 'failed-1';
  assert.equal(p.state.showSyncNotice.value, false);
  p.status({ id: 'failed-2', status: 'failed', message: 'another failure' });
  await p.state.loadStatus();
  assert.equal(p.state.showSyncNotice.value, true);
});

test('状态读取临时失败不会伪报完成，离开页面后停止轮询', async () => {
  const p = page({ id: 'running', status: 'running' });
  await p.mount();
  p.offline(true);
  await p.state.pollSync();
  assert.equal(p.state.syncing.value, true);
  assert.equal(p.notifications.length, 0);
  assert.ok([...p.timers.values()].some(t => t.delay === 5000));
  p.unmount();
  assert.equal(p.timers.size, 0);
  await p.state.pollSync();
  assert.equal(p.timers.size, 0);
});

test('启动请求期间阻止重复同步，离开页面后的晚响应不会启动计时器', async () => {
  const p = page(null);
  let release;
  p.holdStart(new Promise(resolve => { release = resolve; }));
  const pending = p.state.runSync();
  await p.state.runSync();
  assert.equal(p.starts(), 1);
  assert.equal(p.state.syncing.value, true);
  p.unmount();
  release();
  await pending;
  assert.equal(p.timers.size, 0);
});

test('离开页面后到达的启动失败不再弹出过期通知', async () => {
  const p = page(null);
  let reject;
  p.holdStart(new Promise((_, fail) => { reject = fail; }));
  const pending = p.state.runSync();
  p.unmount();
  reject(new Error('connection failed'));
  await pending;
  assert.equal(p.notifications.length, 0);
  assert.equal(p.timers.size, 0);
});


test('刷新组织树会重新加载当前部门成员，避免同步后仍显示旧成员', async () => {
  const p = page(null);
  let reads = 0;
  p.respond(url => {
    if (url.endsWith('/org-tree')) return [{ id: '1', parentId: null, name: '部门' }];
    if (url.endsWith('/members')) return { total: 1, list: [{ userId: String(++reads), name: '新资料' }] };
    return [];
  });
  await p.mount();
  assert.equal(p.state.members.value[0].userId, '1');
  await p.state.loadTree();
  assert.equal(p.state.members.value[0].userId, '2');
});

test('快速切换部门时较晚到达的旧请求不会覆盖当前部门成员', async () => {
  const p = page(null);
  let releaseOld;
  p.respond((url, params) => params.deptId === 'old'
    ? new Promise(resolve => { releaseOld = resolve; })
    : { total: 1, list: [{ userId: 'new' }] });
  p.state.selectedId.value = 'old';
  const old = p.state.loadMembers();
  p.state.selectedId.value = 'new';
  await p.state.loadMembers();
  releaseOld({ total: 1, list: [{ userId: 'old' }] });
  await old;
  assert.equal(p.state.members.value[0].userId, 'new');
  assert.equal(p.state.membersLoading.value, false);
});
