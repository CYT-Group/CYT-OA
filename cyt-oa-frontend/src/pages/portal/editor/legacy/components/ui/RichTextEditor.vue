<!--
RichTextEditor — 全站公用 TipTap 富文本编辑器（Word 式工具栏）

目前使用方：HR 文档 CMS（HrDocEditorView）、公司动态发布（NewsAdminView）。
新页面需要富文本时直接复用本组件，不要再引第二套编辑器。

Props：
- v-model     ：HTML 字符串双向绑定
- placeholder ：占位文案
- compact     ：精简模式（矮编辑框；隐藏 标题/代码/分隔线 等长文档专用按钮，
                字体/字号/对齐/表格/链接 等常用功能全保留）
- headings    ：compact 下也显示 h2-h4 标题下拉（HR 块编辑用；默认关闭，
                news/reports 等既有页面不传即行为不变）

能力：字体、字号、加粗/斜体/下划线/删除线、对齐、列表、引用、链接、图片、表格、撤销重做。

安全约定：
- 工具按钮只暴露渲染端 DOMPurify 白名单允许的标签/属性（HrDocRenderer.vue）
- HR 文档流会再经后端 nh3 sanitize；后端白名单是 source of truth

用法：
  <RichTextEditor v-model="contentHtml" />
  <RichTextEditor v-model="para" compact placeholder="写一段…" />
-->
<template>
  <div class="rte" :class="{ compact }" :style="themeVars">
    <!-- hideToolbar：父页面自己渲染 RteToolbar（如放进页面顶栏）时隐藏内置工具栏 -->
    <RteToolbar v-if="editor && !hideToolbar" :editor="editor" :compact="compact" :headings="headings" :modern-toolbar-selects="modernToolbarSelects" :img-busy="imgUploading" @link="openLink" @image="pickImage" />

    <!-- 链接/图片 URL 输入条（替代 window.prompt，交互友好） -->
    <div v-if="urlMode" class="rte-urlbar">
      <span class="rte-url-lbl">🔗 链接 URL</span>
      <input
        ref="urlInputEl" v-model="urlValue" type="text"
        placeholder="https:// 或站内 /xxx，回车确定"
        @keydown.enter.prevent="confirmUrl" @keydown.esc.prevent="urlMode = null"
      >
      <button type="button" class="rte-url-ok" @click="confirmUrl">确定</button>
      <button v-if="editor.isActive('link')" type="button" @click="removeLink">移除链接</button>
      <button type="button" @click="urlMode = null">取消</button>
    </div>

    <!-- 图片本地上传（🖼 按钮触发；压缩逻辑与 ImageUpload 共用） -->
    <input ref="imgInputEl" type="file" accept="image/png,image/jpeg,image/webp,image/gif" hidden @change="onImageFile">

    <editor-content :editor="editor" class="rte-content" @contextmenu="onCtxMenu" />

    <!-- 表格右键菜单：在表格内右键弹出（表格外仍是浏览器原生菜单） -->
    <div
      v-if="tableCtx.open" class="rte-ctxmenu"
      :style="{ left: tableCtx.x + 'px', top: tableCtx.y + 'px' }"
    >
      <RteTableMenu :editor="editor" @close="tableCtx.open = false" />
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, watch, reactive, ref, nextTick, computed } from 'vue'
import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { Blockquote } from '@tiptap/extension-blockquote'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import Placeholder from '@tiptap/extension-placeholder'
// TipTap v3 把 Table / Row / Cell / Header 全合在一个包里（不再分多包）
// Table / TableCell / TableHeader 在 rte-table-extensions.js 里扩展，这里只用到 TableRow
import { TableRow } from '@tiptap/extension-table'
import { CellSelection } from '@tiptap/pm/tables'
// 字体/字号（TextStyle mark 上的内联样式，输出 <span style="font-family/font-size: ...">）
import { TextStyle, Color, FontSize, FontFamily } from '@tiptap/extension-text-style'
// 对齐（段落/标题上的 text-align 内联样式）
import TextAlign from '@tiptap/extension-text-align'
import { uploadApi } from '../../../bridge/api'
import { compressImage } from '../../utils/imageCompress'
import { useToast } from '../../composables/useToast'
import { themeCssVars } from '../../data/hr-themes'
import RteToolbar from './RteToolbar.vue'
import RteTableMenu from './RteTableMenu.vue'
// 表格扩展（列宽持久化 + 表级外观 class + NodeView class 同步）抽到单独模块，
// 单测才能 import 到**同一份**代码；细则与「为什么用 class 不用 data-*」都写在那边。
import {
  BASE_TABLE_CLASS, TableNode, TableCellWithWidth,
    TableHeaderWithWidth,
} from './rte-table-extensions.js'

// 引用色 / 底色：扩展 Blockquote 支持 blockquoteColor（竖线）+ blockquoteBg（底色）两个属性
// （v1.18：用户要「块内引用也能改竖线颜色和背景颜色」）。
// 二者都落在 <blockquote> 的 inline style 上；后端 nh3 白名单（ALLOWED_CSS_PROPERTIES）
// 已放行 border-left-color / background-color，前端 DOMPurify 只卡 style 属性本身，不逐属性筛。
// 🔴 两个属性各自 renderHTML 都返回 `style` 键 —— 直接各写一半会互相覆盖（后者胜）。
// 所以用一个 bqStyleAttr(attrs) 把「当前两个色」一次算全，两个属性返回**同一条完整 style**，
// 无论框架是覆盖还是合并，结果都一致。
function bqStyleAttr(attrs) {
  const parts = []
  if (attrs.blockquoteColor) parts.push(`border-left-color: ${attrs.blockquoteColor}`)
  if (attrs.blockquoteBg) parts.push(`background-color: ${attrs.blockquoteBg}`)
  return parts.length > 0 ? { style: parts.join('; ') } : {}
}
const BlockquoteWithColor = Blockquote.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      blockquoteColor: {
        default: null,
        parseHTML: (el) => el.style.borderLeftColor || null,
        renderHTML: (attrs) => bqStyleAttr(attrs),
      },
      blockquoteBg: {
        default: null,
        parseHTML: (el) => el.style.backgroundColor || null,
        renderHTML: (attrs) => bqStyleAttr(attrs),
      },
    }
  },
})

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '在这里写文档内容…' },
  // 精简模式（新闻正文段落等短内容用）：矮编辑框，隐藏标题/代码/分隔线等长文档专用按钮
  // （加粗/斜体/列表/引用/链接/表格 全保留）
  compact: { type: Boolean, default: false },
  // 设置面板按需使用站内下拉，默认保留其他编辑器的工具栏行为。
  modernToolbarSelects: { type: Boolean, default: false },
  // compact 下也要标题下拉时打开（透传给 RteToolbar；默认关闭保证既有消费方零变化）
  headings: { type: Boolean, default: false },
  // true = 不渲染内置工具栏，由父页面自己放置 <RteToolbar :editor="...">（如文档编辑器顶栏）
  hideToolbar: { type: Boolean, default: false },
  // 图片上传目录（后端 ALLOWED_FOLDERS 白名单内）
  uploadFolder: { type: String, default: 'images' },
  // HR 文档编辑态传入文档主题；其他消费方不传时 themeCssVars('') 回落品牌紫。
  theme: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])
const toast = useToast()
const themeVars = computed(() => themeCssVars(props.theme))

const editor = new Editor({
  extensions: [
    StarterKit.configure({ heading: { levels: [2, 3, 4] }, blockquote: false }),
    BlockquoteWithColor,
    Placeholder.configure({ placeholder: props.placeholder }),
    Link.configure({ openOnClick: false, autolink: true, HTMLAttributes: { rel: 'noopener noreferrer' } }),
    Image,
    // resizable：拖动列右边框调宽（colwidth 经 style width 序列化持久化，见上）
    // TableNode：表级外观 class（首列加粗 / 斑马纹）+ 把 class 同步进 NodeView 的真实 DOM，见上
    TableNode.configure({ resizable: true, HTMLAttributes: { class: 'hr-doc-table' } }),
    TableRow,
    TableHeaderWithWidth,
    TableCellWithWidth,
    TextStyle,
    Color,
    FontSize,
    FontFamily,
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
  ],
  content: props.modelValue,
  // 保留已有 HTML 里的连续空格（默认按 HTML 规则折叠，重新编辑时会丢空格）
  parseOptions: { preserveWhitespace: true },
  onUpdate: ({ editor }) => emit('update:modelValue', editor.getHTML()),
})

watch(() => props.modelValue, (v) => {
  if (editor && v !== editor.getHTML()) {
    editor.commands.setContent(v || '', { emitUpdate: false, parseOptions: { preserveWhitespace: true } })
  }
})

// ── 表格右键菜单 ──
const tableCtx = reactive({ open: false, x: 0, y: 0 })

function onCtxMenu(e) {
  const info = editor.view.posAtCoords({ left: e.clientX, top: e.clientY })
  if (!info) return
  const $pos = editor.state.doc.resolve(info.pos)
  let inTable = false
  for (let d = $pos.depth; d > 0; d--) {
    if ($pos.node(d).type.name === 'table') { inTable = true; break }
  }
  if (!inTable) return // 表格外不拦截，保留浏览器原生右键
  e.preventDefault()
  // 右键点在已有单元格选区内 → 保留选区（对选区做合并/改宽）；否则光标移到点击处
  const sel = editor.state.selection
  const keepSel = sel instanceof CellSelection && info.pos >= sel.from && info.pos <= sel.to
  if (!keepSel) editor.chain().setTextSelection(info.pos).run()
  // menu max height (64vh, cap 600px) — matches .rte-tblpanel max-height
  const menuMaxH = Math.min(window.innerHeight * 0.64, 600)
  tableCtx.x = Math.min(e.clientX, window.innerWidth - 260)
  // prefer cursor Y; flip above if it would overflow bottom
  let y = e.clientY
  if (y + menuMaxH > window.innerHeight - 8) {
    y = Math.max(8, window.innerHeight - menuMaxH - 8)
  }
  tableCtx.y = y
  tableCtx.open = true
}

function onDocMousedownCtx(e) {
  if (tableCtx.open && !e.target.closest('.rte-ctxmenu')) tableCtx.open = false
}
function onDocKeydownCtx(e) {
  if (e.key === 'Escape') tableCtx.open = false
}
function onDocScrollCtx(e) {
  // Close on any scroll outside the menu.  Keep open only when scrolling
  // inside .rte-ctxmenu itself (the menu's own overflow:auto scrollbar).
  // 🔴 e.target may be `document` on page-level scrolls — document has no
  //    .closest() method, so guard with typeof check to avoid silent TypeError.
  if (!tableCtx.open) return
  const t = e.target
  if (t && typeof t.closest === 'function' && t.closest('.rte-ctxmenu')) return
  tableCtx.open = false
}

onMounted(() => {
  document.addEventListener('mousedown', onDocMousedownCtx)
  document.addEventListener('keydown', onDocKeydownCtx)
  document.addEventListener('scroll', onDocScrollCtx, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onDocMousedownCtx)
  document.removeEventListener('keydown', onDocKeydownCtx)
  document.removeEventListener('scroll', onDocScrollCtx, true)
  for (const url of pendingImages.keys()) URL.revokeObjectURL(url)
  pendingImages.clear()
  editor?.destroy()
})

// 链接/图片用工具栏下方的 URL 输入条（不用 window.prompt，避免打断编辑流）
const urlMode = ref(null) // null | 'link'
const urlValue = ref('')
const urlInputEl = ref(null)

// ── 图片：延迟上传 ──
// 选图时只压缩 + 以 blob: 地址插入本地预览，不发请求；
// 父组件保存前调用 flushPendingImages()：把正文里仍存在的本地图逐张上传，
// blob: 替换为正式 URL。中途被删掉的图不会上传（无孤儿文件）。
const imgInputEl = ref(null)
const imgUploading = ref(false) // 选图时的压缩阶段 busy 态
const pendingImages = new Map() // blobUrl -> File（待上传）

function pickImage() {
  imgInputEl.value?.click()
}

async function onImageFile(e) {
  let file = e.target.files?.[0]
  if (!file) return
  if (file.size > 25 * 1024 * 1024) {
    toast.show('图片超过 25MB，请先手动压缩', 'err')
    e.target.value = ''
    return
  }
  imgUploading.value = true
  try {
    file = await compressImage(file, 1600)
    if (file.size > 10 * 1024 * 1024) {
      toast.show('压缩后仍超过 10MB，请换一张或手动压缩', 'err')
      return
    }
    const blobUrl = URL.createObjectURL(file)
    pendingImages.set(blobUrl, file)
    editor.chain().focus().setImage({ src: blobUrl }).run()
  } catch (err) {
    toast.show('图片处理失败：' + (err.message || '未知错误'), 'err')
  } finally {
    imgUploading.value = false
    e.target.value = ''
  }
}

// 保存前调用：上传正文里仍存在的本地图并替换地址。任一张失败即抛错（调用方应中止保存）。
async function flushPendingImages() {
  if (!pendingImages.size) return
  // 收集文档中仍在使用的 blob 地址；已被删除的直接释放
  const used = new Set()
  editor.state.doc.descendants(node => {
    if (node.type.name === 'image' && pendingImages.has(node.attrs.src)) used.add(node.attrs.src)
  })
  for (const url of [...pendingImages.keys()]) {
    if (!used.has(url)) { URL.revokeObjectURL(url); pendingImages.delete(url) }
  }
  for (const url of used) {
    const file = pendingImages.get(url)
    let real
    try {
      const res = await uploadApi.uploadFile(file, props.uploadFolder)
      real = res?.data?.url
    } catch (err) {
      const msg = err.message || ''
      throw new Error(/timeout|ECONNABORTED/i.test(msg) ? '图片上传超时，请检查网络后重试' : msg || '图片上传失败')
    }
    if (!real) throw new Error('图片上传未返回地址')
    // 替换所有引用该 blob 的图片节点（onUpdate 会随之同步 v-model）
    editor.commands.command(({ tr, state }) => {
      state.doc.descendants((node, pos) => {
        if (node.type.name === 'image' && node.attrs.src === url) {
          tr.setNodeMarkup(pos, undefined, { ...node.attrs, src: real })
        }
      })
      return true
    })
    URL.revokeObjectURL(url)
    pendingImages.delete(url)
  }
}

// editor / openLink / pickImage / imgUploading 供父页面外置 RteToolbar 使用
defineExpose({ flushPendingImages, hasPendingImages: () => pendingImages.size > 0, editor, openLink, pickImage, imgUploading })

function openLink() {
  urlValue.value = editor.getAttributes('link').href || ''
  urlMode.value = 'link'
  nextTick(() => urlInputEl.value?.focus())
}
function confirmUrl() {
  const url = urlValue.value.trim()
  if (url) editor.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
  else editor.chain().focus().extendMarkRange('link').unsetLink().run()
  urlMode.value = null
  urlValue.value = ''
}
function removeLink() {
  editor.chain().focus().extendMarkRange('link').unsetLink().run()
  urlMode.value = null
}
</script>

<style scoped>
.rte {
  border: 1px solid var(--bd, rgba(99,102,241,0.18));
  border-radius: 12px;
  background: rgba(255,255,255,0.95);
  overflow: hidden;
}
/* 工具栏样式在 RteToolbar.vue（独立组件） */
.rte-urlbar {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-bottom: 1px solid var(--bd, rgba(99,102,241,0.18));
  background: rgba(99,102,241,0.05);
}
.rte-url-lbl { font-size: 12px; color: var(--t2, #2D3258); white-space: nowrap; font-weight: 600; }
.rte-urlbar input {
  flex: 1; min-width: 0; padding: 6px 10px; border-radius: 8px;
  border: 1px solid var(--bd, rgba(99,102,241,0.18)); font-size: 12.5px; outline: none; background: #fff;
}
.rte-urlbar input:focus { border-color: var(--v, #6366F1); }
.rte-urlbar button {
  padding: 5px 12px; font-size: 12px; border-radius: 7px; cursor: pointer;
  background: #fff; border: 1px solid var(--bd, rgba(99,102,241,0.18)); color: var(--t2, #2D3258);
}
.rte-urlbar button.rte-url-ok { background: var(--v, #6366F1); color: #fff; border-color: var(--v, #6366F1); }
.rte-content {
  padding: 16px 20px;
  min-height: 360px;
  max-height: 70vh;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.7;
  color: var(--t, #0D1026);
}
.rte-content :deep(h2) { font-size: 1.4em; margin: 1em 0 .5em; font-weight: 700; }
.rte-content :deep(h3) { font-size: 1.2em; margin: .9em 0 .4em; font-weight: 700; }
.rte-content :deep(h4) { font-size: 1.05em; margin: .8em 0 .4em; font-weight: 600; }
.rte-content :deep(p) { margin: .5em 0; }
.rte-content :deep(ul), .rte-content :deep(ol) { padding-left: 1.5em; margin: .5em 0; }
.rte-content :deep(blockquote) {
  border-left: 4px solid var(--hr-primary, var(--v, #6366F1));
  /* border-left-color 由 blockquoteColor、background-color 由 blockquoteBg 的 inline style 覆盖
     （见 BlockquoteWithColor 扩展；此处默认值与预览/阅读页保持一致） */
  padding: .5em 1em;
  margin: .8em 0;
  border-radius: 0 8px 8px 0;
  background: rgba(var(--hr-primary-rgb, 99,102,241), 0.05);
  color: var(--t2, #2D3258);
}
.rte-content :deep(code) {
  background: rgba(99,102,241,0.1);
  padding: .15em .4em;
  border-radius: 4px;
  font-family: var(--fm, monospace);
  font-size: .92em;
}
.rte-content :deep(pre) {
  background: #1a1d2e;
  color: #e8eaf0;
  padding: .8em 1em;
  border-radius: 8px;
  overflow-x: auto;
}
.rte-content :deep(pre code) { background: transparent; color: inherit; padding: 0; }
.rte-content :deep(a) { color: var(--v, #6366F1); text-decoration: underline; }
.rte-content :deep(img) { max-width: 100%; height: auto; border-radius: 6px; }
.rte-content :deep(table) {
  border-collapse: collapse;
  margin: 1em 0;
  width: 100%;
  /* 表格整体更精致：外框 + 圆角靠 .tableWrapper 承载，单元格只画内分隔线 */
  font-variant-numeric: tabular-nums;
}
/* 自适应宽度（表格菜单开关）：编辑态也强制铺满，所见即所得 */
.rte-content :deep(table.hr-doc--auto-width) { width: 100% !important; table-layout: auto !important; }
.rte-content :deep(table.hr-doc--auto-width col) { width: auto !important; }
.rte-content :deep(table.hr-doc--auto-width th),
.rte-content :deep(table.hr-doc--auto-width td) { width: auto !important; }
.rte-content :deep(table th),
.rte-content :deep(table td) {
  border: 1px solid rgba(99, 102, 241, 0.18);
  padding: 8px 12px;
  text-align: left;
  font-size: .95em;
  line-height: 1.7;
  vertical-align: top;
  position: relative; /* 列宽拖拽手柄的定位基准 */
}
/* 表头：浅底 + 加粗 + 主题色字，与正文拉开层次 */
.rte-content :deep(table th) {
  background: rgba(99, 102, 241, 0.08);
  font-weight: 600;
  color: var(--t2, #2d3258);
}
/* 斑马纹：从第 2 行起隔行浅底（只看 tbody，表头不受影响）。
   tbody 里第 1 行是整表第 2 行 → 给偶数行上底色即为「隔行」。
   .hr-doc--no-zebra 是「斑马纹」按钮的显式关闭态（菜单里可再打开）。
   不用 :has()（兼容性还没普及到可以依赖）。 */
.rte-content :deep(table:not(.hr-doc--no-zebra) tbody tr:nth-child(even)) {
  background: rgba(99, 102, 241, 0.03);
}
/* 首列做表头（左侧加粗）：表格菜单「首列加粗」开关写入 hr-doc--first-col，不强加给所有表格 */
.rte-content :deep(table.hr-doc--first-col tbody td:first-child) {
  font-weight: 600;
  color: var(--t2, #2d3258);
  background: rgba(99, 102, 241, 0.04);
}
/* 悬停整行轻微提亮，方便长表格横向读数 */
.rte-content :deep(table tbody tr:hover) { background: rgba(99, 102, 241, 0.05); }
/* 列宽拖拽（Table resizable）：手柄 + 拖动光标 + 横向滚动容器 */
.rte-content :deep(.tableWrapper) { overflow-x: auto; }
.rte-content :deep(table .column-resize-handle) {
  position: absolute;
  right: -2px;
  top: 0;
  bottom: -2px;
  width: 4px;
  background: rgba(99, 102, 241, 0.55);
  pointer-events: none;
}
.rte-content :deep(.resize-cursor) { cursor: col-resize; }
.rte-content :deep(table th) { background: rgba(99,102,241,0.08); font-weight: 600; }
/* 单元格选中态（拖选 / 整行 / 整列 / 全表）——ProseMirror 只挂 .selectedCell 类，
   样式必须自己给，否则"选中了但看不见"，用户会以为不能选 */
.rte-content :deep(table .selectedCell) { position: relative; }
.rte-content :deep(table .selectedCell)::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 2;
  background: rgba(99, 102, 241, 0.14);
  pointer-events: none;
}

.rte-content :deep(.ProseMirror) { outline: none; min-height: 320px; }
/* 表格右键菜单容器（面板本体样式在 RteTableMenu） */
.rte-ctxmenu { position: fixed; z-index: 400; }
/* compact 模式：矮编辑框（新闻段落等短内容） */
.rte.compact .rte-content { min-height: 140px; max-height: 48vh; padding: 12px 14px; font-size: 13px; }
.rte.compact .rte-content :deep(.ProseMirror) { min-height: 116px; }
.rte-content :deep(.ProseMirror:focus) { outline: none; }
.rte-content :deep(.ProseMirror.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  color: var(--t4, #8C92AB);
  pointer-events: none;
  float: left;
  height: 0;
}
</style>
