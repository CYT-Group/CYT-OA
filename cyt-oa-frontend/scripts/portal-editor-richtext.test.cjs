const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
function registry() {
  const source = fs.readFileSync(path.join(__dirname, '../src/lib/portal-richtext.ts'), 'utf8')
  const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const context = { exports: {} }
  vm.runInNewContext(compiled, context)
  return context.exports
}
test('正文与多个模板段落的图片上传完成后才能构造保存内容', async () => {
  const { registerPortalRichText, flushPortalRichTextImages } = registry()
  const fields = ['blob:first', 'blob:second']
  fields.forEach((_, index) => registerPortalRichText(async () => {
    await Promise.resolve()
    fields[index] = `/api/portal/media/images/${index}.png`
  }))
  await flushPortalRichTextImages()
  const saved = JSON.stringify(fields)
  assert.equal(saved.includes('blob:'), false)
  assert.equal(fields.length, 2)
})
test('图片上传失败会中止保存并保留可重试的编辑内容', async () => {
  const { registerPortalRichText, flushPortalRichTextImages } = registry()
  let persisted = false
  registerPortalRichText(async () => { throw new Error('图片上传失败') })
  await assert.rejects(async () => { await flushPortalRichTextImages(); persisted = true }, /图片上传失败/)
  assert.equal(persisted, false)
})
test('移除的模板块和已卸载编辑器不再参与其他页面保存', async () => {
  const { registerPortalRichText, flushPortalRichTextImages } = registry()
  let removedCalls = 0, activeCalls = 0
  const remove = registerPortalRichText(async () => { removedCalls++ })
  registerPortalRichText(async () => { activeCalls++ })
  remove()
  await flushPortalRichTextImages()
  assert.equal(removedCalls, 0)
  assert.equal(activeCalls, 1)
})
