<!--
RteToolbar — 富文本编辑器工具栏（独立组件）

接收 TipTap editor 实例作为 prop（TipTap 官方 MenuBar 模式），
所以可以渲染在任何位置（编辑框顶部 / 页面顶栏），不依赖 Teleport——
Teleport 目标与编辑器同子树时整页卸载会触发 Vue 的 parentNode null 崩溃，弃用。

链接 / 图片两个动作依赖 RichTextEditor 内部状态（URL 输入条 / 文件选择器），
通过 emit('link') / emit('image') 回抛给编辑器组件处理。
-->
<template>
  <div ref="rootEl" class="rte-toolbar" :class="{ compact }">
    <!-- 样式组：标题（长文档；compact 下由 headings 开关决定是否显示） + 字体 + 字号 -->
    <SelectField v-if="modernToolbarSelects && (!compact || headings)" class="rte-modern-select rte-modern-heading" :model-value="headingValue" :options="[{ value: 'p', label: '正文' }, { value: '2', label: '标题 2' }, { value: '3', label: '标题 3' }, { value: '4', label: '标题 4' }]" aria-label="段落样式" @update:model-value="setHeading" />
    <select v-else-if="!compact || headings" class="rte-select" :value="headingValue" @change="setHeading($event.target.value)" title="段落样式">
      <option value="p">正文</option>
      <option value="2">标题 2</option>
      <option value="3">标题 3</option>
      <option value="4">标题 4</option>
    </select>
    <SelectField v-if="modernToolbarSelects" class="rte-modern-select rte-modern-font" :model-value="fontFamilyValue" :options="[{ value: '', label: '字体' }, ...FONT_FAMILIES]" aria-label="字体" @update:model-value="setFontFamily" />
    <select v-else class="rte-select rte-font" :value="fontFamilyValue" @change="setFontFamily($event.target.value)" title="字体">
      <option value="">字体</option>
      <option v-for="f in FONT_FAMILIES" :key="f.value" :value="f.value" :style="{ fontFamily: f.value }">{{ f.label }}</option>
    </select>
    <SelectField v-if="modernToolbarSelects" class="rte-modern-select rte-modern-size" :model-value="fontSizeValue" :options="[{ value: '', label: '字号' }, ...FONT_SIZES.map(value => ({ value, label: value.replace('px', '') }))]" aria-label="字号" @update:model-value="setFontSize" />
    <select v-else class="rte-select rte-size" :value="fontSizeValue" @change="setFontSize($event.target.value)" title="字号">
      <option value="">字号</option>
      <option v-for="s in FONT_SIZES" :key="s" :value="s">{{ s.replace('px', '') }}</option>
    </select>
    <!-- 文字颜色（2026-10-15 用户：「能编辑字体的地方都加上编辑颜色这样」）：
         与字体/字号同一组，选中文字 → 选色即上色；选「不加颜色」清除。
         弹层复用 ChapterColorPopover（章色那个选色器），文案走 props。
         🔴 面板 Teleport 到 body：宿主 .rte 有 overflow:hidden（为圆角收边），
            absolute 面板往下长会被整块剪掉 —— 同下方表格选择器，定位靠按钮 rect 现算。 -->
    <span ref="colorAnchorEl" class="rte-color">
      <button
        ref="colorPopBtn"
        type="button" :class="{ active: colorPopOpen || !!textColor }"
        title="文字颜色：选中文字后选色；选「不加颜色」清除"
        @click.stop="toggleColorPop"
      ><span class="rte-color-bar" :style="textColor ? { background: textColor } : undefined" />颜色</button>
      <Teleport to="body">
        <div v-if="colorPopOpen" ref="colorPopEl" class="portal-editor-floating rte-cp-pop-fixed" :style="colorPopStyle">
          <ChapterColorPopover
            :model-value="textColor"
            clear-label="不加颜色"
            clear-hint="当前：未设置文字颜色"
            set-hint="当前：选中文字用此色；未选中则整段生效"
            @select="applyTextColor"
            @close="colorPopOpen = false"
          />
        </div>
      </Teleport>
    </span>
    <span class="rte-sep" />
    <!-- 格式刷：捕获光标处格式 → 选中目标文字松开鼠标即应用；双击连续刷，Esc/再点取消 -->
    <button
      type="button" :class="{ active: !!painter }"
      @click="togglePainter" @dblclick.prevent="lockPainter"
      title="格式刷：光标放在源格式上点我，然后点一下目标词（或拖选一段）即应用；双击可连续刷多处，Esc 取消"
    >🖌 格式刷{{ painter && painter.sticky ? ' ·连续' : '' }}</button>
    <span class="rte-sep" />
    <!-- 字符格式组 -->
    <button type="button" :class="{ active: editor.isActive('bold') }" @click="editor.chain().focus().toggleBold().run()" title="加粗（Ctrl+B）"><b>B</b></button>
    <button type="button" :class="{ active: editor.isActive('italic') }" @click="editor.chain().focus().toggleItalic().run()" title="斜体（Ctrl+I）"><i>I</i></button>
    <button type="button" :class="{ active: editor.isActive('underline') }" @click="editor.chain().focus().toggleUnderline().run()" title="下划线（Ctrl+U）"><u>U</u></button>
    <button type="button" :class="{ active: editor.isActive('strike') }" @click="editor.chain().focus().toggleStrike().run()" title="删除线"><s>S</s></button>
    <button v-if="!compact" type="button" :class="{ active: editor.isActive('code') }" @click="editor.chain().focus().toggleCode().run()" title="行内代码">{ }</button>
    <span class="rte-sep" />
    <!-- 对齐组 -->
    <button type="button" :class="{ active: editor.isActive({ textAlign: 'left' }) }" @click="editor.chain().focus().setTextAlign('left').run()" title="左对齐">⯇≡</button>
    <button type="button" :class="{ active: editor.isActive({ textAlign: 'center' }) }" @click="editor.chain().focus().setTextAlign('center').run()" title="居中">≡</button>
    <button type="button" :class="{ active: editor.isActive({ textAlign: 'right' }) }" @click="editor.chain().focus().setTextAlign('right').run()" title="右对齐">≡⯈</button>
    <button type="button" :class="{ active: editor.isActive({ textAlign: 'justify' }) }" @click="editor.chain().focus().setTextAlign('justify').run()" title="两端对齐">☰</button>
    <span class="rte-sep" />
    <!-- 段落组 -->
    <button type="button" :class="{ active: editor.isActive('bulletList') }" @click="editor.chain().focus().toggleBulletList().run()" title="无序列表">• 列表</button>
    <button type="button" :class="{ active: editor.isActive('orderedList') }" @click="editor.chain().focus().toggleOrderedList().run()" title="有序列表">1. 列表</button>
    <button type="button" :class="{ active: editor.isActive('blockquote') }" @click="toggleBlockquote" title="引用">" 引用</button>
    <!-- 引用配色（v1.19）：竖线 + 背景二合一面板（实时预览 + 快捷色），替掉原来两个散按钮 -->
    <span v-if="editor.isActive('blockquote')" ref="bqAnchorEl" class="rte-color" title="引用配色">
      <button type="button" :class="{ active: bqPopOpen || !!(bq.line || bq.bg) }" @click.stop="toggleBqPop">
        <span class="rte-color-bar" :style="bq.line ? { background: bq.line } : (bq.bg ? { background: bq.bg } : undefined)" />
        引用配色
      </button>
      <Teleport to="body">
        <div v-if="bqPopOpen" class="portal-editor-floating rte-bq-pop-fixed" :style="bqPopStyle">
          <QuoteColorPopover
            :line="bq.line"
            :bg="bq.bg"
            @update="applyBq"
            @close="bqPopOpen = false"
          />
        </div>
      </Teleport>
    </span>
    <button v-if="!compact" type="button" :class="{ active: editor.isActive('codeBlock') }" @click="editor.chain().focus().toggleCodeBlock().run()" title="代码块">code块</button>
    <span class="rte-sep" />
    <!-- 插入组 -->
    <button type="button" :class="{ active: editor.isActive('link') }" @click="emit('link')" title="插入链接">🔗 链接</button>
    <button v-if="!compact" type="button" :disabled="imgBusy" @click="emit('image')" title="插入本地图片（保存时才上传）">{{ imgBusy ? '⏳ 处理中' : '🖼 图片' }}</button>
    <!-- 插入表格：网格自选行列（Word / 飞书同款），也可「设置行列…」精确输入 -->
    <span ref="tablePickEl" class="rte-tblpick">
      <button
        ref="tablePickBtn"
        type="button" :class="{ active: tablePickOpen }"
        title="插入表格（自选行列）" @click="toggleTablePick"
      >▦ 表格</button>
      <!-- 🔴 面板 Teleport 到 body：宿主 `.rte` 有 overflow:hidden（为圆角收边），
           absolute 面板往下长会被整块剪掉（实测底边超出 94px，「设置行列…」按钮消失）。
           定位靠 openTablePick() 里按按钮 rect 现算。 -->
      <Teleport to="body">
        <div
          v-if="tablePickOpen" ref="pickPopEl" class="portal-editor-floating rte-tp-pop-fixed"
          :style="pickPopStyle"
        >
          <RteTablePicker
            :default-header-row="lastHeaderRow"
            @pick="insertTable" @close="tablePickOpen = false"
          />
        </div>
      </Teleport>
    </span>
    <template v-if="!compact">
      <span class="rte-sep" />
      <button type="button" @click="editor.chain().focus().setHorizontalRule().run()" title="水平线">― 分隔</button>
      <button type="button" @click="editor.chain().focus().setParagraph().run()" title="普通段落">¶</button>
    </template>
    <span class="rte-sep" />
    <button type="button" @click="editor.chain().focus().undo().run()" :disabled="!editor.can().undo()" title="撤销（Ctrl+Z）">↶</button>
    <button type="button" @click="editor.chain().focus().redo().run()" :disabled="!editor.can().redo()" title="重做">↷</button>
  </div>
</template>

<script setup>
import { computed, ref, reactive, nextTick, onMounted, onBeforeUnmount } from 'vue'
import RteTablePicker from './RteTablePicker.vue'
import SelectField from './SelectField.vue'
// 文字颜色弹层：与 HR 块编辑「编号章节」的章色选择器**同一个组件**（UI 一致，文案走 props）
import ChapterColorPopover from './ChapterColorPopover.vue'
import QuoteColorPopover from './QuoteColorPopover.vue'

const props = defineProps({
  editor: { type: Object, required: true },
  modernToolbarSelects: { type: Boolean, default: false },
  compact: { type: Boolean, default: false },
  // compact 默认不放标题下拉（新闻段落等短内容用不上）；HR 块编辑的文字段
  // 需要 h2-h4 进大纲，显式传 true 打开。不传 = news/reports 等既有页面零变化。
  headings: { type: Boolean, default: false },
  imgBusy: { type: Boolean, default: false }, // 图片压缩阶段的 busy 态（由编辑器组件传入）
})
const emit = defineEmits(['link', 'image'])

// ── 插入表格：行列网格选择（面板体在 RteTablePicker）──
// 记住上次「首行表头」的选择，连续插入多张表不用每次重勾（旧版恒 true）
const tablePickOpen = ref(false)
const tablePickEl = ref(null)
const tablePickBtn = ref(null)
const pickPopEl = ref(null)
const lastHeaderRow = ref(true)
// 面板 fixed 坐标（相对按钮，按视口翻转兜底）
const pickPos = reactive({ left: 0, top: 0 })

const pickPopStyle = computed(() => ({
  left: pickPos.left + 'px',
  top: pickPos.top + 'px',
}))

function onDocClickTablePick(e) {
  if (!tablePickOpen.value) return
  const t = e.target
  const inAnchor = tablePickEl.value && tablePickEl.value.contains(t)
  const inPop = pickPopEl.value && pickPopEl.value.contains(t)
  // 🔴 「设置行列…」弹框是 Teleport 到 body 的另一棵树（不在 pickPopEl 里），
  //    若只判 inAnchor/inPop，点弹框任意按钮都会被当成「点外部」→ 整个选择器被卸载
  //    → 表现为「弹框一闪就没了 / 校验根本没机会显示」。用标记类把它也算进来。
  const inDialog = !!(t && t.closest && t.closest('.rte-tpd-mask'))
  if (!inAnchor && !inPop && !inDialog) tablePickOpen.value = false
}

/** 按按钮位置现算面板坐标；底部空间不足时向上翻转（面板高约 241px） */
function placePickPop() {
  const btn = tablePickBtn.value
  if (!btn) return
  const r = btn.getBoundingClientRect()
  const POP_H = 250
  const POP_W = 200
  const gap = 6
  let top = r.bottom + gap
  if (top + POP_H > window.innerHeight - 8) top = Math.max(8, r.top - gap - POP_H)
  let left = r.left
  if (left + POP_W > window.innerWidth - 8) left = Math.max(8, window.innerWidth - 8 - POP_W)
  pickPos.left = left
  pickPos.top = top
}

function toggleTablePick() {
  tablePickOpen.value = !tablePickOpen.value
  if (tablePickOpen.value) nextTick(placePickPop)
}

// 面板 fixed 定位：滚动/缩放时跟随（否则一滚就跑位）
function onWinScrollResize() {
  if (tablePickOpen.value) placePickPop()
  if (colorPopOpen.value) placeColorPop()
  if (bqPopOpen.value) placeBqPop()
}
window.addEventListener('scroll', onWinScrollResize, true)
window.addEventListener('resize', onWinScrollResize)

/**
 * 按网格/自定义尺寸插入表格。
 * 行数上限由 TipTap 与后端校验共同兜底（这里已在面板侧限制），插入后光标落在
 * 第一个单元格，方便直接开打。
 */
function insertTable({ rows, cols, withHeaderRow }) {
  lastHeaderRow.value = withHeaderRow !== false
  props.editor.chain().focus()
    .insertTable({ rows, cols, withHeaderRow: lastHeaderRow.value })
    .run()
}

// ── 格式刷 ──
// 捕获范围：字符级 marks（粗/斜/下划线/删除线/行内代码）+ 字号/字体/**文字颜色** + 对齐。
// 不搬块类型（标题↔正文、列表），避免刷坏文档结构；不动链接。
// 应用时机挂在编辑区 mouseup（PM 拖选过程中 selection 会连续变化，不能用 selectionUpdate）。
const painter = ref(null) // null | { ...捕获的格式, sticky: 是否连续刷 }
const rootEl = ref(null)  // 工具栏根元素（区分工具栏上的 mouseup 与刷格式的 mouseup）

function captureFormat() {
  const e = props.editor
  const ts = e.getAttributes('textStyle')
  return {
    bold: e.isActive('bold'),
    italic: e.isActive('italic'),
    underline: e.isActive('underline'),
    strike: e.isActive('strike'),
    code: e.isActive('code'),
    fontSize: ts.fontSize || '',
    fontFamily: ts.fontFamily || '',
    color: ts.color || '',
    align: ['left', 'center', 'right', 'justify'].find(a => e.isActive({ textAlign: a })) || '',
  }
}

function applyFormat(f) {
  const e = props.editor
  let c = e.chain().focus()
  c = f.bold ? c.setBold() : c.unsetBold()
  c = f.italic ? c.setItalic() : c.unsetItalic()
  c = f.underline ? c.setUnderline() : c.unsetUnderline()
  c = f.strike ? c.setStrike() : c.unsetStrike()
  c = f.code ? c.setCode() : c.unsetCode()
  c = f.fontSize ? c.setFontSize(f.fontSize) : c.unsetFontSize()
  c = f.fontFamily ? c.setFontFamily(f.fontFamily) : c.unsetFontFamily()
  c = f.color ? c.setColor(f.color) : c.unsetColor()
  if (f.align) c = c.setTextAlign(f.align)
  c.run()
}

// 光标处向两侧扩展出「词」的选区（Word 手感：拿着刷子点一下单词即刷整词）。
// 边界 = 空白 + 常见中英文标点；点在空白/标点上则返回 false（不动作）。
function selectWordAt(pos) {
  const e = props.editor
  const $pos = e.state.doc.resolve(pos)
  if (!$pos.parent.isTextblock) return false
  const text = $pos.parent.textContent
  let off = $pos.parentOffset
  if (!text) return false
  const isBoundary = ch => /[\s，。、！？；：·…—""''（）【】《》,.!?;:'"()[\]{}<>]/.test(ch)
  if (off >= text.length || isBoundary(text[off])) {
    if (off > 0 && !isBoundary(text[off - 1])) off--   // 光标贴在词尾的情况
    else return false
  }
  let s = off
  let t = off
  while (s > 0 && !isBoundary(text[s - 1])) s--
  while (t < text.length && !isBoundary(text[t])) t++
  const base = $pos.start()
  e.commands.setTextSelection({ from: base + s, to: base + t })
  return true
}

// mouseup 挂在 document（捕获）而不是编辑区：拖选时鼠标常松在编辑区边界外，
// 只监听编辑区会收不到事件，造成「选完还要再点一下才刷」。
let dragFromEditor = false
function onPaintMouseDown(ev) {
  dragFromEditor = props.editor.view.dom.contains(ev.target)
}

function onPaintMouseUp(ev) {
  // 工具栏自身的点击（切换按钮/下拉）不算刷，也不取消
  if (rootEl.value && rootEl.value.contains(ev.target)) return
  const started = dragFromEditor
  // 等 ProseMirror 把本次点击/拖选的 selection 定稿后再读
  setTimeout(() => {
    if (!painter.value) return
    const e = props.editor
    const { from, to } = e.state.selection
    const upInEditor = e.view.dom.contains(ev.target)
    // 完全在编辑区外的点击（起点和松开都在外面）→ 视为取消格式刷
    if (!started && !upInEditor) { stopPainter(); return }
    // 单击（空选区）→ 自动扩展成整词再刷；点在空白/标点上则忽略
    if (from === to && (!upInEditor || !selectWordAt(from))) return
    applyFormat(painter.value)
    if (!painter.value.sticky) stopPainter()
  }, 0)
}

function onPaintKeydown(ev) {
  if (ev.key === 'Escape') stopPainter()
}

// 刷子形状的自定义光标（Word 手感），热点在左下角笔尖处；不支持时退回文本光标
const BRUSH_SVG = `<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 22 22"><path d="M20 2c-3.2.2-8 4-11 8l3 3c4-3 7.8-7.8 8-11z" fill="#4338CA" stroke="#fff" stroke-width="1.2"/><path d="M8.5 11.5 4.5 13c-1.5.6-1.8 2.8-3 4 2 .9 4.8.6 6.3-.9l3.2-3.1z" fill="#D97706" stroke="#fff" stroke-width="1.2"/></svg>`
const BRUSH_CURSOR = `url("data:image/svg+xml;charset=utf-8,${encodeURIComponent(BRUSH_SVG)}") 2 19, text`

function startPainter(sticky) {
  painter.value = { ...captureFormat(), sticky }
  dragFromEditor = false
  document.addEventListener('mousedown', onPaintMouseDown, true)
  document.addEventListener('mouseup', onPaintMouseUp, true)
  window.addEventListener('keydown', onPaintKeydown)
  props.editor.view.dom.style.cursor = BRUSH_CURSOR // 视觉提示：拿着刷子
}

function stopPainter() {
  if (!painter.value) return
  painter.value = null
  document.removeEventListener('mousedown', onPaintMouseDown, true)
  document.removeEventListener('mouseup', onPaintMouseUp, true)
  window.removeEventListener('keydown', onPaintKeydown)
  const dom = props.editor.view?.dom
  if (dom) dom.style.cursor = ''
}

function togglePainter() {
  if (painter.value) stopPainter()
  else startPainter(false)
}
// 双击 = 连续刷（dblclick 前会先触发两次 click：start→stop，这里重新以 sticky 启动）
function lockPainter() {
  if (painter.value) painter.value.sticky = true
  else startPainter(true)
}

onMounted(() => {
  document.addEventListener('click', onDocClickTablePick)
})
onBeforeUnmount(() => {
  stopPainter()
  document.removeEventListener('click', onDocClickTablePick)
  window.removeEventListener('scroll', onWinScrollResize, true)
  window.removeEventListener('resize', onWinScrollResize)
})

// ── 文字颜色（弹层复用 ChapterColorPopover；定位走 Teleport+fixed，同表格选择器）──
// 为什么不用组件自带的 absolute：宿主 .rte 有 overflow:hidden，absolute 面板会被剪掉
const HEX6 = /^#[0-9a-fA-F]{6}$/
const colorPopOpen = ref(false)
const colorAnchorEl = ref(null)
const colorPopEl = ref(null)
const colorPos = reactive({ left: 0, top: 0 })
const colorPopStyle = computed(() => ({ left: colorPos.left + 'px', top: colorPos.top + 'px' }))

// 光标处文字颜色（Color mark 存在 textStyle 上，与 fontSize/fontFamily 同一个 mark）
const textColor = computed(() => props.editor.getAttributes('textStyle').color || '')

// ── 引用配色（v1.19）：竖线色 + 背景色二合一面板（替掉原来的「引用色 / 引用底色」两个散按钮）──
const bq = computed(() => {
  const a = props.editor.getAttributes('blockquote')
  return { line: a.blockquoteColor || '', bg: a.blockquoteBg || '' }
})
const bqPopOpen = ref(false)
const bqAnchorEl = ref(null)
const bqPos = reactive({ left: 0, top: 0 })
const bqPopStyle = computed(() => ({ left: bqPos.left + 'px', top: bqPos.top + 'px' }))

function applyBq({ line, bg }) {
  // 统一写两个属性：line / bg 各走 updateAttributes；空串 = 清掉（回落主题）。
  const attrs = { blockquoteColor: null, blockquoteBg: null }
  if (HEX6.test(line || '')) attrs.blockquoteColor = line
  if (HEX6.test(bg || '')) attrs.blockquoteBg = bg
  // 配色实时预览时保留色板 / 色值输入框的焦点，避免原生色板刚拖动就关闭。
  props.editor.chain().updateAttributes('blockquote', attrs).run()
}

function placeBqPop() {
  const anchor = bqAnchorEl.value
  if (!anchor) return
  const r = anchor.getBoundingClientRect()
  const POP_H = 360
  const POP_W = 292
  const gap = 6
  let top = r.bottom + gap
  if (top + POP_H > window.innerHeight - 8) top = Math.max(8, r.top - gap - POP_H)
  let left = r.left
  if (left + POP_W > window.innerWidth - 8) left = Math.max(8, window.innerWidth - 8 - POP_W)
  bqPos.left = left
  bqPos.top = top
}

function toggleBqPop() {
  bqPopOpen.value = !bqPopOpen.value
  if (bqPopOpen.value) {
    colorPopOpen.value = false
    nextTick(placeBqPop)
  }
}

function toggleBlockquote() {
  bqPopOpen.value = false
  props.editor.chain().focus().toggleBlockquote().run()
}

function applyTextColor(v) {
  const c = (v || '').trim()
  // 走 applyTextStyle：没选中文字时作用于**光标所在段落**（见该函数上的注释）。
  // 🔴 focus:false —— 取色器拖动时若 focus 编辑器，会抢走 <input type=color> 的焦点，
  //    原生取色面板随即关闭，用户就没法在色条上连续滑动选色（2026-10-27 反馈）。
  applyTextStyle(chain => (c ? chain.setColor(c) : chain.unsetColor()), { focus: false })
  // 不再自动收起弹层：拖动色条会连续触发 input，收起一次就再也滑不动了。
  // 关闭交给「点外部 / Esc / 再点按钮」。
}

/** 按按钮位置现算面板坐标；底部空间不足时向上翻转（面板高约 250px，与 ccp 实际高度同量级） */
function placeColorPop() {
  const btn = colorAnchorEl.value
  if (!btn) return
  const r = btn.getBoundingClientRect()
  const POP_H = 250
  const POP_W = 224
  const gap = 6
  let top = r.bottom + gap
  if (top + POP_H > window.innerHeight - 8) top = Math.max(8, r.top - gap - POP_H)
  let left = r.left
  if (left + POP_W > window.innerWidth - 8) left = Math.max(8, window.innerWidth - 8 - POP_W)
  colorPos.left = left
  colorPos.top = top
}

function toggleColorPop() {
  colorPopOpen.value = !colorPopOpen.value
  if (colorPopOpen.value) {
    bqPopOpen.value = false
    nextTick(placeColorPop)
  }
}

// ── 字体 / 字号 / 段落样式下拉 ──
const FONT_SIZES = [
  '10px', '11px', '12px', '13px', '14px', '15px', '16px', '17px', '18px',
  '20px', '22px', '24px', '26px', '28px', '32px', '36px', '40px', '48px', '56px', '64px', '72px',
]
// 全部用系统常见字体（无需引入 webfont）；value 写全 fallback 链保证跨端显示
const FONT_FAMILIES = [
  { label: '微软雅黑', value: '"Microsoft YaHei", sans-serif' },
  { label: '宋体', value: 'SimSun, serif' },
  { label: '黑体', value: 'SimHei, sans-serif' },
  { label: '楷体', value: 'KaiTi, serif' },
  { label: '仿宋', value: 'FangSong, serif' },
  { label: 'Arial', value: 'Arial, sans-serif' },
  { label: 'Times New Roman', value: '"Times New Roman", serif' },
  { label: 'Georgia', value: 'Georgia, serif' },
  { label: 'Courier New', value: '"Courier New", monospace' },
]

const fontFamilyValue = computed(() => props.editor.getAttributes('textStyle').fontFamily || '')
const fontSizeValue = computed(() => props.editor.getAttributes('textStyle').fontSize || '')
const headingValue = computed(() => {
  for (const level of [2, 3, 4]) {
    if (props.editor.isActive('heading', { level })) return String(level)
  }
  return 'p'
})

// ── 文字样式（颜色 / 字号 / 字体）在「没有选中文字」时的作用范围 ──
// 🔴 2026-10-16 用户实测反馈：「我这个字体颜色改为绿并未渲染」。
// 真因不是渲染端：TipTap 的 setColor / setFontSize 在**选区为空**时只写 stored marks
// （= 只影响**之后输入**的文字），已有文字一个字都不变；可工具栏的色块 / 下拉会立刻显示新值
// → 用户看到「我改了颜色，界面上却什么都没变」，而且从界面上完全看不出为什么。
// 取舍：**光标落在哪一段，就认为用户要改这一段**（色板提示也写着「选中文字用此色」，
// 而「整段变色」正是最常见的诉求）。真想只让之后输入的字变色，把光标放进空段落即可
// —— 空段落没有文字可改，这里自然回落到 stored marks。
function applyTextStyle(pick, { focus = true } = {}) {
  const e = props.editor
  const sel = e.state.selection
  const $from = sel.$from
  const expandable = sel.empty && $from.parent.isTextblock && $from.start() !== $from.end()
  const base = focus ? e.chain().focus() : e.chain()
  if (!expandable) {
    pick(base).run()
    return
  }
  // 扩到整段 → 应用 → **把光标收回原处**：不然用户点一下颜色，整段就停在选中态，
  // 接着输入会把整段替换掉（比「没生效」更糟）。
  const caret = sel.from
  pick(base.setTextSelection({ from: $from.start(), to: $from.end() }))
    .setTextSelection(caret)
    .run()
}

function setFontSize(v) {
  applyTextStyle(chain => (v ? chain.setFontSize(v) : chain.unsetFontSize()))
}
function setFontFamily(v) {
  applyTextStyle(chain => (v ? chain.setFontFamily(v) : chain.unsetFontFamily()))
}
function setHeading(v) {
  if (v === 'p') props.editor.chain().focus().setParagraph().run()
  else props.editor.chain().focus().toggleHeading({ level: Number(v) }).run()
}
</script>

<style scoped>
.rte-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 8px 10px;
  background: rgba(245,245,250,0.85);
  border-bottom: 1px solid var(--bd, rgba(99,102,241,0.18));
}
.rte-toolbar button {
  padding: 4px 10px;
  font-size: 13px;
  background: white;
  border: 1px solid rgba(99,102,241,0.18);
  border-radius: 6px;
  color: var(--t2, #2D3258);
  cursor: pointer;
  transition: all .15s;
}
.rte-toolbar button:hover { background: rgba(99,102,241,0.08); }
.rte-toolbar button.active {
  background: var(--v, #6366F1);
  color: white;
  border-color: var(--v, #6366F1);
}
.rte-toolbar button:disabled { opacity: 0.4; cursor: not-allowed; }
.rte-select {
  padding: 4px 6px; font-size: 12.5px; border-radius: 6px; cursor: pointer;
  border: 1px solid rgba(99,102,241,0.18); background: #fff; color: var(--t2, #2D3258); outline: none;
}
.rte-select:hover, .rte-select:focus { border-color: var(--v, #6366F1); }
.rte-size { width: 62px; }
.rte-font { max-width: 118px; }
.rte-modern-font, .rte-modern-heading { width: 118px; }
.rte-modern-size { width: 70px; }
.rte-modern-select :deep(.sf-trigger) { height: 28px; padding: 0 7px; font-size: 12px; }
.rte-modern-select :deep(.sf-panel) { max-height: 220px; }
.rte-sep {
  width: 1px;
  background: rgba(99,102,241,0.18);
  margin: 2px 4px;
}
.rte-sub { display: inline-flex; gap: 4px; padding: 0 6px; }
/* 文字颜色：按钮内嵌一个小色块（选了什么色就显示什么色，没选是空心框） */
.rte-color { position: relative; display: inline-flex; }
.rte-color button { display: inline-flex; align-items: center; gap: 5px; }
.rte-color-bar {
  width: 12px; height: 12px; border-radius: 3px; flex: none;
  background: transparent;
  box-shadow: inset 0 0 0 1px rgba(13,16,38,.22);
}
/* 插入表格网格选择器：面板本体样式在 RteTablePicker，这里只负责挂到按钮下方。
   🔴 面板 Teleport 到 body（宿主 `.rte` 有 overflow:hidden 会剪掉溢出的面板），
   所以这里用 fixed 定位 + 由 placePickPop() 现算坐标；z-index 与其它浮层同级。 */
.rte-tblpick { position: relative; display: inline-flex; }
/* compact 模式：更紧凑的按钮 */
.rte-toolbar.compact { padding: 5px 8px; }
.rte-toolbar.compact button { padding: 2px 8px; font-size: 12px; }
</style>

<style>
/* 插入表格面板 / 文字颜色弹层：Teleport 到 body，scoped 管不到，所以单独开一个全局块。
   坐标由 RteToolbar.placePickPop() / placeColorPop() 写进 inline style（left/top），
   这里只管观感（ccp 自身样式仍在 ChapterColorPopover.vue 的 scoped 里生效）。
   🔴 ccp 自带 `position:absolute; top:calc(100%+6px); right:0`（为「挂在触发器下方」设计）。
   Teleport 到 body 后，参照物变成我们这个 fixed 容器 → 那三条会把它推到容器下方 230px、
   并贴到容器右边。必须在这里归位（body > 选择器把特异性提到 3 级，压过 scoped 的 2 级）。 */
body > .rte-cp-pop-fixed > .ccp,
body > .rte-bq-pop-fixed > .ccp {
  top: 0;
  right: auto;
  left: 0;
}
.rte-tp-pop-fixed,
.rte-cp-pop-fixed,
.rte-bq-pop-fixed {
  position: fixed;
  z-index: 1200;
}
</style>
