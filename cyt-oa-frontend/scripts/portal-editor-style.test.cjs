const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const editor = path.join(__dirname, '../src/pages/portal/editor')

test('HR 全局兼容动画不占用 OA 动画名称', () => {
  for (const filename of ['portal-editor.css', 'oa-workspace.css']) {
    const css = fs.readFileSync(path.join(editor, 'styles', filename), 'utf8')
    for (const [, name] of css.matchAll(/@(?:-webkit-)?keyframes\s+([\w-]+)/g)) {
      assert.ok(name.startsWith('portal-legacy-'), `${filename}: unscoped animation ${name}`)
    }
  }
})

test('HR 工作区不向 body 注入旧字体、变量或通配间距 reset', () => {
  const css = fs.readFileSync(path.join(editor, 'styles/portal-editor.css'), 'utf8')
  const frame = fs.readFileSync(path.join(editor, 'PortalEditorFrame.vue'), 'utf8')
  assert.doesNotMatch(css, /body\.portal-editor-(?:active|fullscreen)/)
  assert.doesNotMatch(css, /\)\s+\*[^{}]*\{[^{}]*(?:margin|padding)\s*:/)
  assert.doesNotMatch(frame, /document\.body|ToastNotify|GlobalConfirmHost/)
})
