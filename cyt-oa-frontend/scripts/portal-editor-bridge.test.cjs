const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

// 运行实际适配器，只替换网络边界；不依赖登录态、数据库或付费服务。
function bridge(api) {
  const source = fs.readFileSync(path.join(__dirname, '../src/pages/portal/editor/bridge/api.ts'), 'utf8')
    .replace("import { api } from '../../../../lib/api'", 'const api = globalThis.__api')
    .replaceAll('import.meta.env.VITE_PORTAL_DISPLAY_BASE_URL', '""')
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  }).outputText
  const context = { exports: {}, __api: api, FormData, Error }
  vm.runInNewContext(compiled, context)
  return context.exports
}
const plain = value => JSON.parse(JSON.stringify(value))

test('读取后保存保留混合命名的块、封面和签收字段', async () => {
  const opaque = {
    contentBlocks: [{ type: 'tabcard', activeTab: 1, tabs: [{ subHeader: '副标题', metrics: [] }] }],
    draftCover: { nav_tabs: [{ button_text: '立即查看', bgColor: '#3366ff' }] },
    draftSignoff: { button_text: '我已阅读', customMessage: '签收说明' },
  }
  let saved
  const { articleApi } = bridge({
    get: async () => ({ id: '9', draftVersion: 'v2.0', ...opaque }),
    put: async (_, payload) => { saved = plain(payload); return { id: 9, ...payload } },
  })
  const doc = (await articleApi.adminGet(9)).data
  assert.equal(doc.id, 9)
  await articleApi.adminUpdate(9, {
    blocks: doc.content_blocks, cover: doc.draft_cover, signoff: doc.draft_signoff,
    draft_version: doc.draft_version,
  })
  assert.deepEqual(saved.blocks, opaque.contentBlocks)
  assert.deepEqual(saved.cover, opaque.draftCover)
  assert.deepEqual(saved.signoff, opaque.draftSignoff)
  assert.equal(saved.draftVersion, 'v2.0')
})

test('列表聚合所有服务端分页，回收站和已发布引用分别使用正确数据源', async () => {
  const calls = []
  const { articleApi } = bridge({ get: async (url, query) => {
    calls.push({ url, query: plain(query ?? {}) })
    if (url.endsWith('/published')) return [{ id: '8', intro: '默认说明', theme: '#3366ff', cover: { label_cn: '制度' } }]
    return query.pageNo === 1 ? { list: [{ id: '1' }], total: '2' } : { list: [{ id: '2' }], total: '2' }
  } })
  const list = await articleApi.adminList({ deleted: 1 })
  assert.deepEqual(plain(list.data.map(row => row.id)), [1, 2])
  assert.equal(calls[0].query.deleted, true)
  assert.equal(calls[1].query.pageNo, 2)
  const published = (await articleApi.listPublished()).data[0]
  assert.equal(Number.isInteger(published.id), true)
  assert.equal(published.intro, '默认说明')
  assert.equal(published.theme, '#3366ff')
  assert.deepEqual(plain(published.cover), { label_cn: '制度' })
  assert.equal(calls[2].url, '/api/portal/hrdoc/published')
})

test('列表直接发布先获取最新草稿版本和并发凭据', async () => {
  let payload
  const { articleApi } = bridge({
    get: async () => ({ id: 42, revision: 'latest-revision', draftVersion: 'v3.0', version: 'v2.0' }),
    post: async (url, body) => { payload = { url, ...plain(body) }; return { id: 42 } },
  })
  await articleApi.adminPublish(42)
  assert.deepEqual(payload, {
    url: '/admin-api/portal/hrdoc/publish?id=42', expectedRevision: 'latest-revision', version: 'v3.0',
  })
})

test('旧编辑深链通过别名解析稳定 ID，草稿不依赖员工可见列表', async () => {
  const calls = []
  const { articleApi } = bridge({ get: async (url, query) => {
    calls.push({ url, query: plain(query) })
    return url.endsWith('/resolve') ? { id: '35' } : { id: '35', status: 'draft', title: '未发布制度' }
  } })
  const doc = (await articleApi.adminGet('旧制度别名')).data
  assert.equal(doc.id, 35)
  assert.deepEqual(calls, [
    { url: '/admin-api/portal/hrdoc/resolve', query: { slug: '旧制度别名' } },
    { url: '/admin-api/portal/hrdoc/get', query: { id: 35 } },
  ])
})

test('排版任务和测试推送保留预览参数，返回原编辑器可读的状态', async () => {
  const calls = []
  const { hrDocApi } = bridge({ post: async (url, body) => {
    calls.push({ url, body: plain(body) })
    return url.includes('/layout/') ? { id: '3', sourceType: 'paste', blockCount: '2', status: 'pending' }
      : { sent: 1, failed: 0, noRecipients: false }
  } })
  const job = (await hrDocApi.createLayoutJob({ text: '原文', source_type: 'paste', source_name: '手工输入' })).data
  assert.equal(calls[0].body.sourceType, 'paste')
  assert.equal(job.block_count, 2)
  const result = (await hrDocApi.adminTestPushToMe(7, { title: '未保存标题', version: 'v3.0', requires_signoff: true })).data
  assert.equal(calls[1].body.requiresSignoff, true)
  assert.equal(calls[1].body.title, '未保存标题')
  assert.equal(calls[1].url, '/admin-api/portal/hrdoc/compat/test-push-to-me?id=7')
  assert.equal(result.sent, 1)
  assert.equal(result.no_recipients, false)
})

test('OA错误转换后仍能在原编辑器就地显示详细原因', async () => {
  const { articleApi } = bridge({ put: async () => { throw Object.assign(new Error('版本已被他人更新'), { code: 409 }) } })
  await assert.rejects(articleApi.adminUpdate(3, {}), error =>
    error.response.status === 409 && error.response.data.detail === '版本已被他人更新')
})
