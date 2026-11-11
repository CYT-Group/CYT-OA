<!--
RteTablePicker — 插入表格的行列网格选择器（Word / 飞书同款交互）

点工具栏「▦ 表格」弹出，鼠标在网格上划过即高亮；顶部实时显示「3 × 4」以及
一个「首行表头」的小复选框；点一下即插入对应尺寸的表格，Esc 或点外部关闭。

为什么单独一个组件：
- 网格 hover 状态 + 尺寸标签是纯局部 UI 状态，塞进 RteToolbar 会让那个组件
  （本来就 300+ 行）继续膨胀，而且它已经有「表格操作 ▾」下拉的定位逻辑了。
- 与 RteTableMenu（菜单内容无定位）同套路：这里也**只管面板本体，不负责定位**，
  由宿主 RteToolbar 用 :deep 的 absolute 容器挂到按钮下方。

行列上限与 TipTap / 后端 nh3 都无冲突；10×10 纯粹是「网格画得下、鼠标划得动」的
体验上限，超过用「设置行列…」精确输入（同样支持，见下）。

🔴 2026-10-14 两个修复：
1. **「设置行列」吞 2**：原用 `window.prompt` + 正则，中文输入法默认输出**全角**数字/符号
   （`２` U+FF12 / `＊` U+FF0A / `，` U+FF0C），屏幕上与半角几乎无差 → 全被判「格式不对」，
   用户以为「2 被吞了」。现在统一走 `utils/input-normalize.js` 先转半角，并把原生
   prompt/alert 换成**页内样式化弹框**（原生弹窗无法做样式，且会阻塞）。
2. **面板被裁**：宿主 `.rte` 有 `overflow:hidden`（收圆角），面板 absolute 超出即被剪
   （实测底边超出 94px，最后 5 行网格 + 「设置行列…」按钮整块消失）。
   → 弹框走 `<Teleport to="body">` 脱离该容器；网格面板本体不受影响，但它也不再靠
   「往下长」来放东西了。
-->
<template>
  <div ref="rootEl" class="rte-tp">
    <div class="rte-tp-hd">
      <span class="rte-tp-size">{{ label }}</span>
      <label class="rte-tp-opt" title="第一行作为表头（浅底加粗）">
        <input v-model="headerRow" type="checkbox"> 首行表头
      </label>
    </div>

    <!-- 网格：n×m 小方块，hover 到 (r,c) 时第 r 行前 c 列全部高亮 -->
    <div class="rte-tp-grid" @mouseleave="resetHover">
      <div v-for="r in MAX" :key="r" class="rte-tp-row">
        <span
          v-for="c in MAX" :key="c"
          class="rte-tp-cell"
          :class="{ on: r <= hover.r && c <= hover.c }"
          @mouseover="setHover(r, c)"
          @click="pick(r, c)"
        ></span>
      </div>
    </div>

    <div class="rte-tp-ft">
      <button type="button" class="rte-tp-custom" @click="openCustom">设置行列…</button>
    </div>

    <!-- 页内「设置行列」弹框：替代原生 prompt/alert（无法样式化、且阻塞） -->
    <Teleport to="body">
      <Transition name="rte-tp-fade">
        <div v-if="dlgOpen" class="portal-editor-floating rte-tpd-mask" @click.self="closeDialog">
          <div class="rte-tpd" role="dialog" aria-label="设置表格行列">
            <header class="rte-tpd-h">
              <h3>设置表格行列</h3>
              <button type="button" class="rte-tpd-x" aria-label="关闭" @click="closeDialog">✕</button>
            </header>

            <div class="rte-tpd-b">
              <div class="rte-tpd-row">
                <label class="rte-tpd-f">
                  <span>行数</span>
                  <input
                    ref="rowEl" v-model="dRows" type="text" inputmode="numeric"
                    :class="{ bad: err.rows }" placeholder="1–100"
                    @keydown.enter.prevent="confirmCustom"
                  >
                </label>
                <span class="rte-tpd-x2">×</span>
                <label class="rte-tpd-f">
                  <span>列数</span>
                  <input
                    v-model="dCols" type="text" inputmode="numeric"
                    :class="{ bad: err.cols }" placeholder="1–20"
                    @keydown.enter.prevent="confirmCustom"
                  >
                </label>
              </div>

              <p class="rte-tpd-hint" :class="{ warn: dlgHintWarn }">
                {{ dlgHint }}
              </p>

              <div class="rte-tpd-quick">
                <span class="rte-tpd-quick-lbl">常用：</span>
                <button
                  v-for="q in QUICK" :key="q"
                  type="button" class="rte-tpd-q" @click="applyQuick(q)"
                >{{ q }}</button>
              </div>
            </div>

            <footer class="rte-tpd-foot">
              <button type="button" class="rte-tpd-btn" @click="closeDialog">取消</button>
              <button type="button" class="rte-tpd-btn primary" @click="confirmCustom">插入表格</button>
            </footer>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { parseSize } from '../../utils/input-normalize.js'

const props = defineProps({
  // 当前是否开启「首行表头」的初值由宿主决定（跟随上次选择）
  defaultHeaderRow: { type: Boolean, default: true },
})
const emit = defineEmits(['pick', 'close'])

const MAX = 10
const ROW_MAX = 100
const COL_MAX = 20
const QUICK = ['3x3', '2x2', '4x3', '5x4']

// 🔴 用 reactive 对象 + **逐字段赋值**：不要整体重赋值（`hover = {…}`）。
//    实测在 <script setup> 里对 reactive 常量整体重赋值时，v-for 子元素上的
//    @mouseenter 处理函数不会生效（事件能到 DOM，handler 不跑，且**不报错**）。
//    统一走 setHover() 改属性，行为确定。
const hover = reactive({ r: 1, c: 1 })

function setHover(r, c) {
  hover.r = r
  hover.c = c
}
function resetHover() {
  hover.r = 0
  hover.c = 0
}

// 首行表头：默认开（与旧版 insertTable 的 withHeaderRow: true 行为一致）
const headerRow = ref(props.defaultHeaderRow)

const label = computed(() => {
  const { r, c } = hover
  return r > 0 && c > 0 ? `${c} × ${r} 表格` : '插入表格'
})

function pick(rows, cols) {
  if (rows < 1 || cols < 1) return
  emit('pick', { rows, cols, withHeaderRow: headerRow.value })
  emit('close')
}

// ── 「设置行列」弹框状态 ──────────────────────────────
const dlgOpen = ref(false)
const rowEl = ref(null)
const dRows = ref('5')
const dCols = ref('4')
const err = reactive({ rows: '', cols: '' })
// 打开弹框时把网格当前尺寸带进来（用户往往「就想要再大一点」）
let opener = { rows: 5, cols: 4 }

/** 提示语与即时校验：写清楚「全角已自动转换」这种反馈，避免用户再怀疑被吞 */
const dlgHint = computed(() => {
  if (err.rows || err.cols) return '行数填 1–100、列数填 1–20 的整数'
  return `行 1–${ROW_MAX} · 列 1–${COL_MAX}；支持 3x3、3×3、3*3、3,3 等写法`
})
const dlgHintWarn = computed(() => !!(err.rows || err.cols))

function openCustom() {
  opener = hover.r > 0 && hover.c > 0
    ? { rows: hover.r, cols: hover.c }
    : { rows: 5, cols: 4 }
  dRows.value = String(opener.rows)
  dCols.value = String(opener.cols)
  err.rows = ''
  err.cols = ''
  dlgOpen.value = true
  nextTick(() => focusRows())
}

/**
 * 聚焦第一个输入框。弹框用 Transition 出现，**过渡首帧仍是隐藏态**，
 * 此时 focus() 会被静默忽略（不报错、activeElement 不动）——与项目既有结论一致。
 * 所以「没落上就 100ms 后重试」，比写死定时器稳。
 */
function focusRows(attempt = 0) {
  const el = rowEl.value
  if (!el) return
  el.focus({ preventScroll: true })
  el.select?.()
  if (document.activeElement !== el && attempt < 4) {
    setTimeout(() => focusRows(attempt + 1), 100)
  }
}

function closeDialog() {
  dlgOpen.value = false
}

function applyQuick(q) {
  const p = parseSize(q, { rowMax: ROW_MAX, colMax: COL_MAX })
  if (!p.ok) return
  dRows.value = String(p.rows)
  dCols.value = String(p.cols)
  err.rows = ''
  err.cols = ''
}

/**
 * 提交。两个输入框各自解析，**分别给出错误**（旧版只有一个笼统的 alert）。
 * 输入先过 `parseSize` → 内部会做「全角转半角」，所以 `２` 与 `2` 等价。
 */
function confirmCustom() {
  err.rows = ''
  err.cols = ''

  const onlyRows = String(dRows.value).trim()
  const onlyCols = String(dCols.value).trim()

  // 正常路径：两框各是一个整数 → 拼成 "RxC" 交给同一个解析器
  const single = parseSize(`${onlyRows}x${onlyCols}`, { rowMax: ROW_MAX, colMax: COL_MAX })
  if (single.ok) {
    emit('pick', { rows: single.rows, cols: single.cols, withHeaderRow: headerRow.value })
    emit('close')
    dlgOpen.value = false
    return
  }

  // 失败路径：定位到具体哪个框，逐框标红 + 具体文案
  const pRows = parseSize(`${onlyRows}x1`, { rowMax: ROW_MAX, colMax: 1 })
  const pCols = parseSize(`1x${onlyCols}`, { rowMax: 1, colMax: COL_MAX })
  if (!pRows.ok) {
    err.rows = pRows.reason === 'range' ? `行数需在 1–${ROW_MAX}` : `请输入 1–${ROW_MAX} 的整数`
  }
  if (!pCols.ok) {
    err.cols = pCols.reason === 'range' ? `列数需在 1–${COL_MAX}` : `请输入 1–${COL_MAX} 的整数`
  }
  if (single.reason === 'range' && err.rows === err.cols) {
    // 行、列都合法但整体超限 —— 极少数（如 100x20 没问题，200x2 已在 pRows 拦下）
    err.rows = `行数需在 1–${ROW_MAX}`
  }
}

function onKey(e) {
  if (e.key === 'Escape' && dlgOpen.value) {
    e.stopPropagation()
    closeDialog()
  }
}

function onPickerKey(e) {
  if (e.key === 'Escape') emit('close')
}
onMounted(() => document.addEventListener('keydown', onPickerKey))
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onPickerKey)
  window.removeEventListener('keydown', onKey, true)
})

// 打开时挂 Esc（捕获阶段，先于编辑器/宿主的按键处理）；关闭时清理并对齐状态
watch(dlgOpen, (v) => {
  if (v) {
    window.addEventListener('keydown', onKey, true)
  } else {
    window.removeEventListener('keydown', onKey, true)
    err.rows = ''
    err.cols = ''
  }
})

const rootEl = ref(null)
defineExpose({ rootEl })
</script>

<style scoped>
.rte-tp {
  background: #fff;
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 10px;
  box-shadow: 0 12px 36px rgba(13, 16, 38, 0.14);
  padding: 10px 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 7px;
  user-select: none;
}
.rte-tp-hd { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.rte-tp-size { font-size: 12px; font-weight: 600; color: var(--t2, #2d3258); }
.rte-tp-opt { display: inline-flex; align-items: center; gap: 4px; font-size: 11.5px; color: var(--t4, #8c92ab); cursor: pointer; }
.rte-tp-opt input { margin: 0; cursor: pointer; }

.rte-tp-grid { display: flex; flex-direction: column; gap: 2px; }
.rte-tp-row { display: flex; gap: 2px; }
.rte-tp-cell {
  width: 15px;
  height: 15px;
  border: 1px solid rgba(99, 102, 241, 0.28);
  border-radius: 3px;
  background: #fff;
  cursor: pointer;
  transition: background .1s, border-color .1s;
}
.rte-tp-cell.on {
  background: rgba(99, 102, 241, 0.22);
  border-color: rgba(99, 102, 241, 0.6);
}

.rte-tp-ft { display: flex; justify-content: flex-end; }
.rte-tp-custom {
  padding: 2px 8px;
  font-size: 11.5px;
  background: #fff;
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 6px;
  color: var(--t3, #5a5f7a);
  cursor: pointer;
}
.rte-tp-custom:hover { background: rgba(99, 102, 241, 0.08); }
</style>

<style>
/* 「设置行列」弹框：Teleport 到 body，所以不能用 scoped */
.rte-tpd-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(13, 16, 38, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.rte-tpd {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid rgba(99, 102, 241, 0.12);
  box-shadow: 0 24px 60px rgba(13, 16, 38, 0.2);
  overflow: hidden;
}
.rte-tpd-h {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px 0;
}
.rte-tpd-h h3 { margin: 0; font-size: 15px; font-weight: 700; color: var(--t, #0d1026); }
.rte-tpd-x {
  flex: none;
  width: 26px; height: 26px;
  border: none; border-radius: 7px; background: transparent;
  color: var(--t4, #8c92ab); font-size: 13px; line-height: 1; cursor: pointer;
  transition: all .15s;
}
.rte-tpd-x:hover { background: rgba(99, 102, 241, 0.08); color: var(--v, #6366f1); }

.rte-tpd-b { padding: 16px 20px 6px; }
.rte-tpd-row { display: flex; align-items: flex-end; gap: 10px; }
.rte-tpd-f { flex: 1; display: flex; flex-direction: column; gap: 5px; }
.rte-tpd-f > span { font-size: 12px; font-weight: 600; color: var(--t2, #2d3258); }
.rte-tpd-f input {
  width: 100%;
  box-sizing: border-box;
  padding: 9px 11px;
  font-size: 14px;
  font-family: inherit;
  border-radius: 8px;
  border: 1px solid rgba(99, 102, 241, 0.2);
  background: #fff;
  color: var(--t, #0d1026);
  outline: none;
  transition: border-color .15s, box-shadow .15s;
}
.rte-tpd-f input:focus { border-color: var(--v, #6366f1); box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12); }
.rte-tpd-f input.bad { border-color: #dc2626; box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.1); }
.rte-tpd-x2 { padding-bottom: 10px; color: var(--t4, #8c92ab); font-size: 14px; }

.rte-tpd-hint { margin: 10px 0 0; font-size: 11.5px; color: var(--t4, #8c92ab); line-height: 1.6; }
.rte-tpd-hint.warn { color: #dc2626; }

.rte-tpd-quick { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-top: 14px; }
.rte-tpd-quick-lbl { font-size: 11.5px; color: var(--t4, #8c92ab); }
.rte-tpd-q {
  padding: 3px 9px;
  font-size: 11.5px;
  font-family: inherit;
  border-radius: 6px;
  border: 1px solid rgba(99, 102, 241, 0.18);
  background: #fff;
  color: var(--t3, #5a5f7a);
  cursor: pointer;
}
.rte-tpd-q:hover { background: rgba(99, 102, 241, 0.08); border-color: rgba(99, 102, 241, 0.4); }

.rte-tpd-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 20px 18px;
  border-top: 1px solid rgba(99, 102, 241, 0.1);
  margin-top: 14px;
}
.rte-tpd-btn {
  padding: 8px 18px;
  font-size: 13px;
  font-family: inherit;
  border-radius: 8px;
  border: 1px solid rgba(99, 102, 241, 0.2);
  background: #fff;
  color: var(--t2, #2d3258);
  cursor: pointer;
  transition: all .15s;
}
.rte-tpd-btn:hover { background: rgba(99, 102, 241, 0.06); }
.rte-tpd-btn.primary {
  background: var(--v, #6366f1);
  border-color: var(--v, #6366f1);
  color: #fff;
  font-weight: 600;
}
.rte-tpd-btn.primary:hover { background: #5558e8; }

.rte-tp-fade-enter-active, .rte-tp-fade-leave-active { transition: opacity .16s ease; }
.rte-tp-fade-enter-from, .rte-tp-fade-leave-to { opacity: 0; }
.rte-tp-fade-enter-active .rte-tpd { transition: transform .16s ease; }
.rte-tp-fade-enter-from .rte-tpd { transform: translateY(8px) scale(.98); }
</style>
