const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

function utility(name) {
  const filename = path.join(__dirname, '../src/pages/portal/editor/legacy/utils', name + '.ts')
  const compiled = ts.transpileModule(fs.readFileSync(filename, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  }).outputText
  const context = { exports: {}, URL }
  vm.runInNewContext(compiled, context)
  return context.exports
}

const { docRefButton } = utility('hr-docref')
const { isHrDocumentListHistory } = utility('hr-admin-navigation')

test('文档引用优先采用员工端规范地址，同时保留自定义地址', () => {
  const target = { canonical_url: 'https://portal.example.test/hr-doc/handbook' }
  assert.equal(docRefButton({ document_id: 7 }, target).url, target.canonical_url)
  assert.equal(docRefButton({ button_url: '/custom?ref=7', document_id: 7 }, target).url, '/custom?ref=7')
  assert.equal(docRefButton({ document_id: 7 }, { canonical_url: 'http://localhost:3000/article/7' }).external, true)
})

test('缺少目标摘要的旧文档引用回落 OA 预览路由并编码业务标识', () => {
  assert.equal(docRefButton({ document_id: 7 }).url, '/portal/hr-documents/id/7/preview')
  assert.equal(docRefButton({ slug: '制度/2026?x=1&next=#章' }).url,
    '/portal/hr-documents?legacySlug=%E5%88%B6%E5%BA%A6%2F2026%3Fx%3D1%26next%3D%23%E7%AB%A0&mode=preview')
  assert.equal(docRefButton({}).url, '')
})

test('只有精确列表历史可返回，编辑页、相似前缀与外部站点均回落列表', () => {
  const origin = 'http://127.0.0.1:4810'
  assert.equal(isHrDocumentListHistory('/portal/hr-documents?search=手册&page=3', origin), true)
  assert.equal(isHrDocumentListHistory('/portal/hr-documents/id/77/edit', origin), false)
  assert.equal(isHrDocumentListHistory('/portal/hr-documents/id/77/preview', origin), false)
  assert.equal(isHrDocumentListHistory('/portal/hr-documents/new', origin), false)
  assert.equal(isHrDocumentListHistory('/portal/hr-documents-other', origin), false)
  assert.equal(isHrDocumentListHistory('https://other.example/portal/hr-documents', origin), false)
  assert.equal(isHrDocumentListHistory(null, origin), false)
})
