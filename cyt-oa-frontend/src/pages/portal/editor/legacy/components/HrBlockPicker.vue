<!--
HrBlockPicker — 「添加块」类型选择面板（19 种块，可继续扩）。

为什么单独抽出来：块类型会一直加，早先是「一张卡片一个说明」的平铺网格，
19 种就已经 7 行、把面板撑得比视口还高，再加只会继续往下堆。改成：
- **分组**（文字与图文 / 提示与强调 / 列表与卡片 / 数据展示 / 折叠与高级）——
  以后新增块是往已有分组里塞，不是往列表尾巴上接
- **紧凑芯片**（图标 + 名称，5 列）—— 描述不再常驻，改由底部一行随 hover / 聚焦实时显示
- **搜索框**（打开即聚焦，回车选中第一个匹配）—— 块再多也能两下点到
- 面板限高 + 内部滚动，永不超过视口

性能约定（v1.3.2）：
- 面板**自管理开合状态**（`open()/close()` + `defineExpose`），父级不再持有 picker 响应式对象。
  否则父级模板里读一次 `picker.open`，开关面板就会把整个编辑器（20 个块卡片 + 16 个内联预览）
  重新 diff 一遍 —— 实测 28ms，直接掉帧。
- 位置**按点击点算**（`event.clientX/clientY`），不做 `getBoundingClientRect()`：
  挂载后立刻量尺寸会强制整页同步重排（同样是几十毫秒的代价）。
- Teleport 到 body + position:fixed：脱离编辑器文档流，不参与其布局。

「不抖」约定（v1.3.3）—— 面板打开期间**几何必须恒定**，任何一处随内容变化都会看成抖动：
- 面板用 `v-show` 常驻 DOM（不再 v-if 反复挂载/卸载），尺寸由 `height` 固定，**不随内容长短伸缩**。
  早先按内容撑高：搜索过滤时面板从 422px 塌到 158px，整个面板一跳。
- `.hbp-body` 上 `scrollbar-gutter: stable`：19 个芯片时 body 内部有滚动条，过滤后没结果就没滚动条，
  没有这条时滚动条一进一出会让 5 列网格横向重排（实测芯片宽 111.8 ↔ 114.8px）。
- 底栏用**固定高度**（不是 min-height）：悬停时底栏从 36 长到 38px，会把 body 挤矮 2px。
- `focus({ preventScroll: true })`：本页 html 带 `scroll-behavior: smooth`，
  万一聚焦触发滚动会「平滑地飘一下」，非常像抖动。
- 进出场只做**透明度**过渡（0.12s，与锚点插入条淡入同拍），不做位移/缩放——有位移才叫抖。
-->
<template>
  <Teleport to="body">
    <Transition name="hbp-fade">
      <div v-show="st.open" class="portal-editor-floating hbp-layer" :style="layerStyle" @click.stop>
        <div class="hbp" :style="{ height: st.panelH + 'px' }">
          <div class="hbp-hd">
            <span class="hbp-search-ico" aria-hidden="true">🔍</span>
            <input
              ref="searchEl"
              v-model="q"
              class="hbp-search"
              type="text"
              placeholder="搜索块类型…"
              @keydown.enter.prevent="pickFirst"
              @keydown.esc.prevent="close"
            >
            <span class="hbp-count">{{ flat.length }}</span>
          </div>

          <div class="hbp-body">
            <template v-for="g in visibleGroups" :key="g.name">
              <div class="hbp-group-name">{{ g.name }}</div>
              <div class="hbp-grid">
                <button
                  v-for="t in g.items"
                  :key="t.type"
                  type="button"
                  class="hbp-chip"
                  :title="`${t.label} — ${t.desc}`"
                  @click="pick(t.type)"
                  @mouseenter="hovered = t"
                  @mouseleave="hovered = null"
                  @focus="hovered = t"
                  @blur="hovered = null"
                >
                  <span class="hbp-icon" aria-hidden="true">{{ t.icon }}</span>
                  <span class="hbp-name">{{ t.label }}</span>
                  <span v-if="t.advanced" class="hbp-dot" title="高级块" aria-hidden="true"></span>
                </button>
              </div>
            </template>
            <div v-if="!flat.length" class="hbp-empty">没有匹配「{{ q }}」的块</div>
          </div>

          <div class="hbp-ft" :class="{ hint: !hovered }">
            <template v-if="hovered">
              <span class="hbp-ft-icon" aria-hidden="true">{{ hovered.icon }}</span>
              <b>{{ hovered.label }}</b>
              <span class="hbp-ft-desc">{{ hovered.desc }}</span>
            </template>
            <template v-else>点一个块插入到当前位置 · 可直接输入搜索</template>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, reactive, ref } from 'vue'

const props = defineProps({
  // [{ name, items: [{ type, icon, label, desc, advanced? }] }]
  groups: { type: Array, default: () => [] },
})
const emit = defineEmits(['pick', 'close'])

const GAP = 6          // 面板与点击点的间距
const EDGE = 12        // 与视口边缘的安全距离
const MAX_H = 520      // 面板最高（19 个块铺满约 434px，520 是留给后续加块的上限）
const MIN_H = 240      // 再挤也要留出可用的高度

const st = reactive({ open: false, index: 0, up: false, panelH: MAX_H, x: 0, y: 0 })
const q = ref('')
const hovered = ref(null)
const searchEl = ref(null)

// 扁平化一次，搜索与计数共用
const flat = computed(() => props.groups.flatMap(g => g.items))

const visibleGroups = computed(() => {
  const kw = q.value.trim().toLowerCase()
  if (!kw) return props.groups
  const hit = flat.value.filter(t =>
    t.label.toLowerCase().includes(kw)
    || t.desc.toLowerCase().includes(kw)
    || t.type.toLowerCase().includes(kw))
  return hit.length ? [{ name: '搜索结果', items: hit }] : []
})

// 面板以点击点为锚：向下弹 → 顶边贴点击点；向上弹 → 底边贴点击点。
// 横向以点击点居中并夹在视口内（面板宽度 min(620px, 100vw-32px)）。
const layerStyle = computed(() => {
  const vw = window.innerWidth
  const vh = window.innerHeight
  const half = Math.min(310, (vw - 32) / 2)
  const x = Math.min(Math.max(st.x, half + EDGE), vw - half - EDGE)
  const s = { left: `${x}px`, transform: 'translateX(-50%)' }
  if (st.up) s.bottom = `${vh - st.y + GAP}px`
  else s.top = `${st.y + GAP}px`
  return s
})

/**
 * 由父级调用：在点击点处打开面板（或把已打开的面板挪到新插入点）。
 * index = 要插入到第几块之前。面板尺寸在打开那一刻算定，之后**不再变**。
 */
function open(index, evt) {
  const vh = window.innerHeight
  const y = evt && evt.clientY ? evt.clientY : vh / 2
  const x = evt && evt.clientX ? evt.clientX : window.innerWidth / 2
  const below = vh - y - GAP - EDGE
  const above = y - GAP - EDGE
  const room = Math.max(above, below)          // 上下两侧里较宽裕的那一侧
  st.up = below < MIN_H + 80 && above > below
  // 固定高度：可用空间内取满，但夹在 [min(MIN_H, room), MAX_H] —— 既不会顶出视口，
  // 也不会因为内容多少而伸缩（伸缩=整个面板在跳）
  st.panelH = Math.min(Math.max(Math.round(st.up ? above : below), Math.min(MIN_H, room)), MAX_H)
  st.x = x
  st.y = y
  st.index = index
  const wasOpen = st.open
  st.open = true
  // 换插入点（面板已开着）时保留已输入的搜索词；只有全新打开才清空
  if (!wasOpen) {
    q.value = ''
    hovered.value = null
  }
  // 打开即聚焦搜索框：想找块就直接打字，不想找就直接点芯片。
  // preventScroll 必须加：本页 html 带 scroll-behavior:smooth，聚焦万一触发滚动会「平滑地飘一下」。
  nextTick(() => searchEl.value && searchEl.value.focus({ preventScroll: true }))
}

function close() {
  if (!st.open) return
  st.open = false
  emit('close')
}

function pick(type) {
  const index = st.index
  st.open = false
  emit('pick', type, index)
}

function pickFirst() {
  const first = visibleGroups.value[0] && visibleGroups.value[0].items[0]
  if (first) pick(first.type)
}

defineExpose({ open, close, isOpen: () => st.open, currentIndex: () => st.index })
</script>

<style scoped>
/* 定位层：脱离文档流，挂在 body 上，不参与编辑器的布局 */
.hbp-layer {
  position: fixed;
  z-index: 60;
  width: min(620px, calc(100vw - 32px));
  /* 面板自己不参与滚动锚定，免得页面滚动时被浏览器「顺手」挪位置 */
  overflow-anchor: none;
}

/* 进出场只做透明度（0.12s，与 .hbe-gap.active 插入条的淡入同拍）。
   刻意不做位移 / 缩放 —— 有位移才叫抖。 */
.hbp-fade-enter-active,
.hbp-fade-leave-active { transition: opacity 0.12s ease; }
.hbp-fade-enter-from,
.hbp-fade-leave-to { opacity: 0; }

.hbp {
  display: flex;
  flex-direction: column;
  /* height 由行内 style 给定（打开时算定，不再变），border-box 才能让高度是准的 */
  box-sizing: border-box;
  background: #fff;
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 14px;
  box-shadow: 0 14px 44px rgba(13, 16, 38, 0.16);
  overflow: hidden;
}

/* ── 头部：搜索 ── */
.hbp-hd {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(99, 102, 241, 0.12);
  background: rgba(99, 102, 241, 0.03);
}
.hbp-search-ico { font-size: 12px; opacity: 0.55; }
.hbp-search {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  color: var(--t);
}
.hbp-search::placeholder { color: var(--t5, #b3b7c9); }
.hbp-count {
  flex-shrink: 0;
  font-family: var(--fm, ui-monospace, monospace);
  font-size: 10.5px;
  color: var(--t5, #b3b7c9);
}

/* ── 主体：分组 + 芯片网格（超高时内部滚动，不撑破视口）──
   flex: 1 1 auto —— 面板高度已由外层固定，body 吃掉「面板高 - 页头 - 页脚」的余量。
   scrollbar-gutter: stable —— 19 个芯片时 body 里有滚动条、搜索过滤到没结果时没有，
   没有这条时滚动条一进一出会让 5 列网格横向重排（实测芯片宽 111.8 ↔ 114.8px，看着就是抖）。 */
.hbp-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  scrollbar-gutter: stable;
  overflow-anchor: none;
  padding: 8px 12px 4px;
}
.hbp-group-name {
  margin: 6px 0 4px;
  font-family: var(--fm, ui-monospace, monospace);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--t5, #b3b7c9);
}
.hbp-group-name:first-child { margin-top: 0; }
.hbp-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 5px;
}
.hbp-chip {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 7px 4px 6px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.12s, border-color 0.12s;
}
.hbp-chip:hover,
.hbp-chip:focus-visible {
  background: rgba(99, 102, 241, 0.07);
  border-color: rgba(99, 102, 241, 0.22);
  outline: none;
}
.hbp-icon { font-size: 16px; line-height: 1.15; }
.hbp-name {
  font-size: 11px;
  font-weight: 600;
  color: var(--t);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}
/* 高级块（原样 HTML）只点一个小橙点，不占版面 */
.hbp-dot {
  position: absolute;
  top: 6px;
  right: 7px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #f59e0b;
}
.hbp-empty {
  padding: 26px 8px;
  text-align: center;
  font-size: 12.5px;
  color: var(--t4, #8c92ab);
}

/* ── 底部：随 hover 实时显示说明（描述不再常驻每个卡片）──
   固定高度（不是 min-height）：悬停时底栏内容从「一行提示」换成「图标+名称+说明」，
   实测底栏会长 2px（36 → 38），把 body 挤矮 2px —— 每次进出芯片都让面板内部动一下。
   高度 + line-height 都钉死，内容再怎么换都不改布局。 */
.hbp-ft {
  flex-shrink: 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 14px;
  border-top: 1px solid rgba(99, 102, 241, 0.12);
  background: rgba(99, 102, 241, 0.03);
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
  color: var(--t3, #5a5f7a);
}
.hbp-ft.hint { color: var(--t5, #b3b7c9); }
.hbp-ft-icon { font-size: 13px; line-height: 18px; }
.hbp-ft b { color: var(--v, #6366f1); font-weight: 700; }
.hbp-ft-desc { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

@media (max-width: 768px) {
  .hbp-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
</style>
