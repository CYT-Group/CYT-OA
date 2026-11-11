<!--
RteTableMenu — 表格操作菜单（面板内容，无定位）

两个宿主共用：
- RteToolbar 的「表格操作 ▾」下拉（宿主用 :deep 给 .rte-tblpanel 加 absolute 定位）
- RichTextEditor 的右键菜单（宿主用 fixed 容器包裹，定位到鼠标处）

选中整行/整列/全表用 ProseMirror CellSelection（原生只支持矩形选区，
不支持 Ctrl+点选不相邻单元格）；列宽写到单元格 colwidth 属性，
序列化成 style="width:npx"（后端 nh3 白名单允许 style+width，可持久化）。
-->
<template>
  <div class="rte-tblpanel">
    <!-- Row operations -->
    <div class="tp-section">
      <div class="tp-section-hd">行操作</div>
      <div class="tp-actions">
        <button type="button" class="tp-btn" @click="run(c => c.addRowBefore())" title="上方插入行">
          <span class="tp-icon">⬆</span>上方插入
        </button>
        <button type="button" class="tp-btn" @click="run(c => c.addRowAfter())" title="下方插入行">
          <span class="tp-icon">⬇</span>下方插入
        </button>
        <button type="button" class="tp-btn danger" @click="run(c => c.deleteRow())" title="删除当前行">
          <span class="tp-icon">✕</span>删除行
        </button>
      </div>
    </div>

    <!-- Column operations -->
    <div class="tp-section">
      <div class="tp-section-hd">列操作</div>
      <div class="tp-actions">
        <button type="button" class="tp-btn" @click="run(c => c.addColumnBefore())" title="左侧插入列">
          <span class="tp-icon">⬅</span>左侧插入
        </button>
        <button type="button" class="tp-btn" @click="run(c => c.addColumnAfter())" title="右侧插入列">
          <span class="tp-icon">➡</span>右侧插入
        </button>
        <button type="button" class="tp-btn danger" @click="run(c => c.deleteColumn())" title="删除当前列">
          <span class="tp-icon">✕</span>删除列
        </button>
      </div>
    </div>

    <div class="tp-sep" />

    <!-- Selection -->
    <div class="tp-section">
      <div class="tp-section-hd">选中</div>
      <div class="tp-actions tp-actions--sm">
        <button type="button" class="tp-btn tp-btn--sm" @click="selectTableRow">整行</button>
        <button type="button" class="tp-btn tp-btn--sm" @click="selectTableCol">整列</button>
        <button type="button" class="tp-btn tp-btn--sm" @click="selectWholeTable">全表</button>
      </div>
    </div>

    <!-- Cell operations -->
    <div class="tp-section">
      <div class="tp-section-hd">单元格</div>
      <div class="tp-actions">
        <button type="button" class="tp-btn" :disabled="!editor.can().mergeCells()" @click="run(c => c.mergeCells())">
          <span class="tp-icon">⊞</span>合并
        </button>
        <button type="button" class="tp-btn" :disabled="!editor.can().splitCell()" @click="run(c => c.splitCell())">
          <span class="tp-icon">⊟</span>拆分
        </button>
        <button type="button" class="tp-btn" @click="setColWidth" title="精确设置列宽">
          <span class="tp-icon">↔</span>列宽…
        </button>
        <button type="button" class="tp-btn" @click="run(c => c.setCellAttribute('colwidth', null))" title="恢复自动列宽">
          自动
        </button>
      </div>
    </div>

    <div class="tp-sep" />

    <!-- Table style -->
    <div class="tp-section">
      <div class="tp-section-hd">表格样式</div>
      <div class="tp-actions tp-actions--sm">
        <button type="button" class="tp-btn tp-btn--sm" @click="run(c => c.toggleHeaderRow())" title="表头行开/关">
          表头行
        </button>
        <button type="button" class="tp-btn tp-btn--sm" :class="{ on: firstColHeader }" @click="toggleFirstColHeader">
          首列加粗
        </button>
        <button type="button" class="tp-btn tp-btn--sm" :class="{ on: zebraOn }" @click="toggleZebra">
          斑马纹
        </button>
        <button type="button" class="tp-btn tp-btn--sm" :class="{ on: autoWidth }" @click="toggleAutoWidth" title="铺满正文宽度，忽略固定列宽">
          自适应宽度
        </button>
        <button type="button" ref="thBgBtn" class="tp-btn tp-btn--sm" :class="{ on: !!thBgCurrent }" @click="pickThBg" title="设置表头底色">
          <span class="tp-color-dot" :style="thBgCurrent ? { background: thBgCurrent } : {}" />表头色
        </button>
        <button v-if="thBgCurrent" type="button" class="tp-btn tp-btn--sm danger" @click="clearThBg">清除底色</button>
      </div>
      <input ref="thBgInput" type="color" class="th-bg-picker" @change="onThBgChange" />
    </div>

    <div class="tp-sep" />

    <!-- Delete -->
    <button type="button" class="tp-btn tp-btn--full danger" @click="run(c => c.deleteTable())">
      <span class="tp-icon">🗑</span>删除整个表格
    </button>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { CellSelection, TableMap, selectionCell } from '@tiptap/pm/tables'
import { CLS_AUTO_WIDTH, CLS_FIRST_COL, CLS_NO_ZEBRA, toggleClassTokens } from './rte-table-extensions.js'

const props = defineProps({
  editor: { type: Object, required: true },
})
const emit = defineEmits(['close'])

/** 菜单动作统一入口：跑命令并通知宿主收起菜单 */
function run(fn) {
  fn(props.editor.chain().focus()).run()
  emit('close')
}

function withSelectionCell(make) {
  const { state, view } = props.editor
  let $cell = null
  try { $cell = selectionCell(state) } catch { /* 不在表格里 */ }
  if (!$cell) return
  view.dispatch(state.tr.setSelection(make(state, $cell)))
  view.focus()
  emit('close')
}
const selectTableRow = () => withSelectionCell((s, $c) => CellSelection.rowSelection($c))
const selectTableCol = () => withSelectionCell((s, $c) => CellSelection.colSelection($c))
const selectWholeTable = () => withSelectionCell((s, $c) => {
  const table = $c.node(-1)
  const start = $c.start(-1)
  const map = TableMap.get(table)
  return CellSelection.create(s.doc, start + map.map[0], start + map.map[map.map.length - 1])
})

function setColWidth() {
  const v = window.prompt('列宽（像素，30–800；留空取消）', '160')
  if (v == null || v.trim() === '') return
  const n = Math.round(parseFloat(v))
  if (!n || Number.isNaN(n) || n < 30 || n > 800) return
  props.editor.chain().focus().setCellAttribute('colwidth', [n]).run()
  emit('close')
}

// ── 表格级视觉开关（v1.9.5）──
// 🔴 用 class 落库，**不要用 data-* 属性**：后端 nh3 白名单（services/html_sanitizer.py）
//    只放行 class / id / style，所有 data-* 会被静默剥掉（实测：全库 54 篇文档里
//    data-colwidth 出现 0 次 —— 「拖动调列宽」其实一直是失效的）。class 能原样落库、
//    原样回读，所以视觉开关一律走 class 修饰符，后端零改动。
// 渲染端（HrDocRenderer / HrBlocksRenderer / RichTextEditor）按同名 class 上样式，
// 做到「编辑器所见 = 阅读页所见」。
// 常量与 toggleClassToken 与编辑器共用一份（rte-table-extensions.js），避免两边写漂。

/** 光标所在表格节点（不在表格里返回 null） */
function currentTable() {
  const { state } = props.editor
  const { $from } = state.selection
  for (let d = $from.depth; d > 0; d--) {
    const n = $from.node(d)
    if (n.type.name === 'table') return { node: n, pos: $from.before(d) }
  }
  return null
}

function currentClasses() {
  const t = currentTable()
  return String((t && t.node.attrs && t.node.attrs.class) || '').split(/\s+/).filter(Boolean)
}

/** 整串重写 class（去重 + 保序） */
function writeClasses(list) {
  const t = currentTable()
  if (!t) return
  const uniq = [...new Set(list.filter(Boolean))]
  const { tr } = props.editor.state
  tr.setNodeMarkup(t.pos, undefined, { ...t.node.attrs, class: uniq.join(' ') })
  props.editor.view.dispatch(tr)
  props.editor.view.focus()
  emit('close')
}

/** 首列加粗（左侧做成表头观感）：给 <table> 加/去 hr-doc--first-col */
const firstColHeader = computed(() => currentClasses().includes(CLS_FIRST_COL))

function toggleFirstColHeader() {
  if (!currentTable()) return
  writeClasses(toggleClassTokens(currentClasses(), CLS_FIRST_COL, !firstColHeader.value))
}

// 斑马纹：默认由 CSS 恒开（tbody 偶数行浅底）；这里给它一个显式开关，
// 关掉时加 hr-doc--no-zebra，渲染端据此跳过斑马纹规则。
// 🔴 注意极性：token 是「关闭」标记 → 当前开着（zebraOn=true）时这次点击要**加上** token，
//    所以第三个参数传 zebraOn（不是 !zebraOn）。传反了会「点了没反应」。
const zebraOn = computed(() => !currentClasses().includes(CLS_NO_ZEBRA))

function toggleZebra() {
  if (!currentTable()) return
  writeClasses(toggleClassTokens(currentClasses(), CLS_NO_ZEBRA, zebraOn.value))
}

// ── 自适应宽度（v1.12.2，用户「表格加一个自适应宽度」）──
// 打开时给 <table> 加 hr-doc--auto-width：渲染端用 !important 强制 width:100% 并清掉
// 单元格/col 的固定宽度 → 不管文档里残留了什么内联宽度或固定列宽，表格都铺满正文，
// 解决「阅读页表格比正文窄、右侧留白」的问题。关掉则恢复按内容/固定列宽显示。
const autoWidth = computed(() => currentClasses().includes(CLS_AUTO_WIDTH))

function toggleAutoWidth() {
  if (!currentTable()) return
  writeClasses(toggleClassTokens(currentClasses(), CLS_AUTO_WIDTH, !autoWidth.value))
}

// ── 表头底色 ──
const thBgBtn = ref(null)
const thBgInput = ref(null)
const thBgCurrent = computed(() => {
  const t = currentTable()
  if (!t) return null
  let color = null
  t.node.descendants((node) => {
    if (node.type.name === 'tableHeader' && node.attrs.thbg) {
      color = node.attrs.thbg
    }
  })
  return color || null
})
function pickThBg() {
  const input = thBgInput.value
  if (!input) return
  // 🔴 原生 color input 按按钮位置 fixed 定位到视口，避免弹到左上角
  const btn = thBgBtn.value
  if (btn) {
    const r = btn.getBoundingClientRect()
    input.style.position = 'fixed'
    input.style.top = (r.bottom + 4) + 'px'
    input.style.left = r.left + 'px'
    input.style.zIndex = '9999'
  }
  input.click()
}
function onThBgChange(e) {
  const color = e.target.value
  if (!color) return
  setThBg(color)
}
function clearThBg() { setThBg(null) }
function setThBg(color) {
  const t = currentTable()
  if (!t) return
  const { state, view } = props.editor
  const { tr } = state
  t.node.descendants((node, pos) => {
    if (node.type.name === 'tableHeader') {
      const absPos = t.pos + 1 + pos
      tr.setNodeMarkup(absPos, undefined, { ...node.attrs, thbg: color || null })
    }
  })
  view.dispatch(tr)
  view.focus()
  emit('close')
}
</script>

<style scoped>
/* ── Color picker (hidden, positioned by JS) ── */
.th-bg-picker {
  /* hidden by default; positioned via JS (pickThBg) when the user clicks "表头色" */
  position: fixed;
  visibility: hidden;
}

/* ── Panel ── */
.rte-tblpanel {
  background: #fff;
  border: 1px solid rgba(99,102,241,0.12);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(13,16,38,0.12), 0 2px 8px rgba(13,16,38,0.06);
  padding: 6px 8px;
  min-width: 230px;
  max-width: 290px;
  max-height: min(64vh, 560px);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 13px;
  user-select: none;
}

/* ── Section ── */
.tp-section { display: flex; flex-direction: column; gap: 3px; }
.tp-section-hd {
  font-size: 10.5px;
  font-weight: 600;
  color: rgba(13,16,38,0.35);
  text-transform: uppercase;
  letter-spacing: 0.6px;
  padding: 2px 6px 0;
}

/* ── Action row ── */
.tp-actions { display: flex; gap: 3px; flex-wrap: wrap; }
.tp-actions--sm { gap: 2px; }

/* ── Separator ── */
.tp-sep {
  height: 1px;
  background: rgba(99,102,241,0.08);
  margin: 2px 4px;
}

/* ── Buttons ── */
.tp-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 9px;
  font-size: 12px;
  line-height: 1.3;
  background: #fff;
  border: 1px solid rgba(99,102,241,0.1);
  border-radius: 7px;
  color: rgba(13,16,38,0.75);
  cursor: pointer;
  transition: all 0.12s;
  white-space: nowrap;
  font-family: inherit;
}
.tp-btn:hover {
  background: rgba(99,102,241,0.06);
  border-color: rgba(99,102,241,0.22);
  color: rgba(13,16,38,0.9);
}
.tp-btn:active { transform: scale(0.97); }
.tp-btn:disabled { opacity: 0.35; cursor: not-allowed; transform: none; }

/* small variant */
.tp-btn--sm { padding: 3px 7px; font-size: 11.5px; border-radius: 6px; }

/* full width */
.tp-btn--full { width: 100%; justify-content: center; }

/* active/on state */
.tp-btn.on {
  background: rgba(99,102,241,0.08);
  border-color: rgba(99,102,241,0.3);
  color: #6366F1;
  font-weight: 500;
}

/* danger variant */
.tp-btn.danger { color: #b91c1c; border-color: rgba(244,63,94,0.12); }
.tp-btn.danger:hover { background: rgba(244,63,94,0.06); border-color: rgba(244,63,94,0.28); }

/* ── Icons ── */
.tp-icon { font-size: 11px; opacity: 0.7; flex-shrink: 0; }

/* ── Color dot ── */
.tp-color-dot {
  display: inline-block;
  width: 10px; height: 10px;
  border-radius: 3px;
  border: 1px solid rgba(99,102,241,0.18);
  background: rgba(99,102,241,0.12);
  flex-shrink: 0;
}

/* ── Scrollbar ── */
.rte-tblpanel::-webkit-scrollbar { width: 5px; }
.rte-tblpanel::-webkit-scrollbar-track { background: transparent; }
.rte-tblpanel::-webkit-scrollbar-thumb {
  background: rgba(99,102,241,0.15);
  border-radius: 8px;
}
.rte-tblpanel::-webkit-scrollbar-thumb:hover { background: rgba(99,102,241,0.3); }
</style>
