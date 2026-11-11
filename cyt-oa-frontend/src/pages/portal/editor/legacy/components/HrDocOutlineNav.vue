<!--
HrDocOutlineNav — HR 文档阅读页左侧文章导航（竖线+圆点时间线观感，对齐 About 页）。

条目来源：渲染完成后扫描父页传入的正文容器 DOM，统一提取
  - 块格式的小节锚点（section 块渲染成 h2#sec-{n}）
  - 双格式正文里的 h2-h4 标题（老 HTML 文档 / text 块内标题）
无 id 的标题就地补 `h-{i}` 锚点，因此块格式与老 HTML 文档走同一条提取路径。

行为：scroll-spy 高亮当前节（IntersectionObserver 叫醒 + 几何位置判定）、
点击平滑滚动（提取时给标题内联 scroll-margin-top，防 sticky 顶栏遮挡）。
宽屏（≥1100px）左侧吸顶；窄屏（<1100px）侧栏隐藏，改由右下角悬浮按钮
唤出左侧抽屉（v1.21，用户 2026-10-20：「往下滑目录就看不到了」）。
容器里扫不出任何标题时整个导航不渲染（FAB 与抽屉也不出现），
并通过 count 事件告诉父页（父页据此决定要不要切两栏网格）。

样式 scoped 自持，不用裸 <section>（portal-design 全局 padding 地雷）。
-->
<template>
  <nav v-if="items.length" ref="outlineEl" class="hr-outline" aria-label="文档大纲">
    <ul class="ol-list">
      <li
        v-for="it in items"
        :key="it.id"
        class="ol-item"
        :class="[`lv-${it.level}`, { active: it.id === activeId, 'has-color': !!it.chapColor }]"
        :style="it.chapColor ? { '--ol-color': it.chapColor } : undefined"
      >
        <button type="button" class="ol-link" @click="jumpTo(it.id)">{{ it.text }}</button>
      </li>
    </ul>
  </nav>

  <!-- 窄屏（<1100px）：左侧目录被隐藏，改由右下角悬浮按钮唤出抽屉（v1.21）。
       🔴 必须 Teleport 到 body —— 祖先若有 backdrop-filter / transform，
       position:fixed 会相对该祖先定位而不是视口（门户的 .glass-panel 就带 backdrop-filter）。 -->
  <Teleport to="body">
    <button
      v-if="fab && items.length && !drawerOpen"
      type="button"
      class="portal-editor-floating ol-fab"
      :aria-label="`打开文档目录，共 ${items.length} 条`"
      @click="openDrawer"
    >
      <svg viewBox="0 0 16 16" width="18" height="18" aria-hidden="true">
        <path d="M6 3.5h8M6 8h8M6 12.5h8" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
        <circle cx="2.6" cy="3.5" r="1.1" fill="currentColor" />
        <circle cx="2.6" cy="8" r="1.1" fill="currentColor" />
        <circle cx="2.6" cy="12.5" r="1.1" fill="currentColor" />
      </svg>
    </button>

    <Transition name="ol-fade">
      <div v-if="drawerOpen" class="portal-editor-floating ol-mask" @click="closeDrawer"></div>
    </Transition>

    <Transition name="ol-slide">
      <aside
        v-if="drawerOpen"
        class="portal-editor-floating ol-drawer"
        role="dialog"
        aria-modal="true"
        aria-label="文档目录"
      >
        <div class="ol-drawer-hd">
          <span class="ol-drawer-title">目录</span>
          <span class="ol-drawer-count">{{ items.length }}</span>
          <button type="button" class="ol-drawer-close" aria-label="关闭目录" @click="closeDrawer">✕</button>
        </div>
        <div class="ol-drawer-body">
          <ul class="ol-list">
            <li
              v-for="it in items"
              :key="`d-${it.id}`"
              class="ol-item"
              :class="[`lv-${it.level}`, { active: it.id === activeId, 'has-color': !!it.chapColor }]"
              :style="it.chapColor ? { '--ol-color': it.chapColor } : undefined"
            >
              <button type="button" class="ol-link" @click="jumpFromDrawer(it.id)">{{ it.text }}</button>
            </li>
          </ul>
        </div>
      </aside>
    </Transition>
  </Teleport>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { parseChapterColor } from '../utils/hr-chapter-color'

const props = defineProps({
  // 正文渲染容器的 DOM 元素（父页模板 ref 直接传入）；null = 还没挂载，不渲染
  container: { type: Object, default: null },
  // 内容标识：换文档 / 换草稿时父页传新值触发重扫（直接传 doc 对象即可）
  contentKey: { type: [String, Number, Object, Array], default: 0 },
  // 窄屏悬浮按钮（v1.21）：页面级目录（阅读页 / 独立预览页）用默认 true；
  // 嵌在**编辑器预览面板**里必须传 false —— 面板是内部滚动容器，
  // 全局 fixed 按钮会浮到整个编辑器的右下角，跟预览区完全错位。
  fab: { type: Boolean, default: true },
})

const emit = defineEmits(['count'])

const items = ref([])
const activeId = ref('')
const outlineEl = ref(null)
let io = null
let headingEls = []

// sticky 顶栏高度 + 余量。scroll-spy 判定线与点击落点（scroll-margin-top）用同一偏移，
// 这样「点谁滚到顶」和「谁被高亮」永远一致
const TOP_OFFSET = 96

// 扫容器收集标题。sec-{n} 锚点本身就是 h2，只选标题标签即可全覆盖——
// 不额外按 id 前缀选，防止老 HTML 文档里恰好带 sec- 开头 id 的非标题容器混进大纲
// 读标题上的章节配色变量（numsec 配了 color 时渲染端会挂 --hbc-toc）。
// 只在**计算后**取：变量若只写在祖先块上也会被继承解析出来。
function readChapterColor(el) {
  try {
    return parseChapterColor(getComputedStyle(el).getPropertyValue('--hbc-toc'))?.color ?? ''
  } catch {
    return ''
  }
}
async function scan() {
  await nextTick()
  if (io) {
    io.disconnect()
    io = null
  }
  headingEls = []
  const root = props.container
  if (!root || typeof root.querySelectorAll !== 'function') {
    items.value = []
    emit('count', 0)
    return
  }
  const found = []
  let idx = 0
  root.querySelectorAll('h2, h3, h4').forEach((el) => {
    // 🔴 图文段（imageText）的标题是**块内说明性小标题**，不是文档结构 —— 不进目录。
    // 它是 h3，若照标签名收录就会以「二级」身份插在小节中间（用户 2026-10-20：
    // 「为什么这个打卡凭证要求是这个层级」）。真正的 h3 只该来自 text 块里用户手写的标题。
    if (el.classList.contains('hb-it-title')) return
    // 小节块标题里带编号前缀 span（section 的 01 / numsec 的 01），
    // 只取标题文本部分当条目文案 —— 编号是视觉装饰，目录里不要
    const text = (
      el.querySelector('.hb-section-title')?.textContent
      ?? el.querySelector('.hb-numsec-title')?.textContent
      ?? el.textContent
      ?? ''
    ).trim()
    if (!text) return
    if (!el.id) el.id = `h-${idx}`
    idx += 1
    // v-html 内容 scoped CSS 够不着，scroll-margin-top 只能在提取时内联补上
    el.style.scrollMarginTop = `${TOP_OFFSET}px`
    const tag = el.tagName.toLowerCase()
    // 🔴 层级按**块的语义**判，不能只看标签名：section 块（小节）为了进目录 + 做锚点，
    // 模板里渲染的就是 <h2>，光看标签名会和小节标题（numsec，也是 h2）一样算成 level 1
    // → 目录里章和节缩进完全相同，「看着没有缩进」（用户 2026-10-20）。
    // 所以 section 显式压成 level 2；其余仍按 h2/h3/h4 推。
    const level = el.classList.contains('hb-section')
      ? 2
      : tag === 'h4' ? 3 : tag === 'h3' ? 2 : 1
    // 章节配色（v1.12）：标题上若挂了 --hbc-toc（numsec 配了 color），
    // 把该条染成同色 → 实现「章头 + 目录联动」。取不到就是未配色，走默认样式。
    const chapColor = readChapterColor(el)
    found.push({ id: el.id, text, level, el, chapColor })
  })
  headingEls = found.map(f => f.el)
  items.value = found.map(({ id, text, level, chapColor }) => ({ id, text, level, chapColor }))
  emit('count', found.length)
  if (!found.length) return
  // scroll-spy：IO 只负责在「有标题穿过判定线」时叫醒 recompute，
  // 谁高亮由 recompute 按几何位置算——比只看 isIntersecting 可靠（快滚/回滚方向都对）
  io = new IntersectionObserver(recompute, {
    rootMargin: `-${TOP_OFFSET}px 0px 0px 0px`,
    threshold: 0,
  })
  headingEls.forEach(el => io.observe(el))
  recompute()
}

// 当前节 = 最后一个顶边已越过判定线的标题；全都没越线就高亮第一个
function recompute() {
  if (!headingEls.length) return
  let current = headingEls[0].id
  for (const el of headingEls) {
    if (el.getBoundingClientRect().top <= TOP_OFFSET + 4) current = el.id
    else break
  }
  if (current === activeId.value) return
  activeId.value = current
  nextTick(ensureActiveVisible)
}

// 目录比可视区高时（矮窗口 / 长文档 / 编辑器预览面板），把当前章节滚进目录可视区。
// 否则往下读时「我在哪」那一行可能落在目录外，看起来像目录少了几条。
// 🔴 只改目录自己的 scrollTop，不调 scrollIntoView —— 后者会连带把外层面板也滚走。
function ensureActiveVisible() {
  const ol = outlineEl.value
  if (!ol || ol.scrollHeight <= ol.clientHeight + 2) return
  const act = ol.querySelector('.ol-item.active')
  if (!act) return
  const box = ol.getBoundingClientRect()
  const r = act.getBoundingClientRect()
  const pad = 6
  if (r.top < box.top + pad) ol.scrollTop -= (box.top + pad - r.top)
  else if (r.bottom > box.bottom - pad) ol.scrollTop += (r.bottom - (box.bottom - pad))
}

function jumpTo(id) {
  const el = headingEls.find(e => e.id === id)
  if (!el) return
  // 立即高亮目标项，不等平滑滚动结束后 IO 回调再切
  activeId.value = id
  el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

watch(() => [props.container, props.contentKey], scan, { immediate: true })

// ── 窄屏抽屉（v1.21）──
// 只在 <1100px 出现（FAB 由 CSS 控制显隐），宽屏用户完全看不到这一套。
const drawerOpen = ref(false)
// 打开时锁背景滚动；记住原值再恢复，避免把全局 `overflow-x:hidden` 冲掉
let prevBodyOverflow = ''

function openDrawer() {
  if (!items.value.length || drawerOpen.value) return
  prevBodyOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  drawerOpen.value = true
}

function closeDrawer() {
  if (!drawerOpen.value) return
  drawerOpen.value = false
  document.body.style.overflow = prevBodyOverflow
}

// 点条目：先关抽屉（恢复滚动）再滚，否则 body 还被锁着、滚动不生效
function jumpFromDrawer(id) {
  closeDrawer()
  jumpTo(id)
}

function onKeydown(e) {
  if (e.key === 'Escape') closeDrawer()
}

// 抽屉开着时把窗口拉宽到两栏断点以上 → 自动收起，别留一个只剩遮罩的状态
function onResize() {
  if (window.innerWidth >= 1100) closeDrawer()
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  if (io) io.disconnect()
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('resize', onResize)
  // 卸载时抽屉还开着（例如路由跳走）→ 必须把 body 滚动还回去
  if (drawerOpen.value) document.body.style.overflow = prevBodyOverflow
})
</script>

<style scoped>
/* sticky 跟随滚动；超长大纲自己滚，不撑破视口 */
.hr-outline {
  position: sticky;
  top: calc(var(--nav-h, 64px) + 24px);
  align-self: start;
  max-height: calc(100vh - var(--nav-h, 64px) - 48px);
  overflow-y: auto;
  padding: 4px 0;
}

.ol-list {
  position: relative;
  list-style: none;
  margin: 0;
  padding: 0;
}
/* 贯穿竖线：上下淡出渐变，观感对齐 About 页时间线 */
.ol-list::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 12px;
  bottom: 12px;
  width: 1px;
  background: linear-gradient(
    to bottom,
    transparent,
    var(--bd, rgba(99, 102, 241, 0.18)) 10%,
    var(--bd, rgba(99, 102, 241, 0.18)) 90%,
    transparent
  );
}

.ol-item { position: relative; padding: 2px 0 2px 26px; }
/* 圆点：默认空心灰边，当前节实心主色 + 光晕（同 About 时间线 active 态） */
.ol-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--bd, rgba(99, 102, 241, 0.3));
}
.ol-item.active::before {
  border-color: var(--hr-primary, var(--v, #6366f1));
  background: var(--hr-primary, var(--v, #6366f1));
  box-shadow: 0 0 0 4px rgba(var(--hr-primary-rgb, 99, 102, 241), 0.15);
}

/* 层级缩进：每深一级 +16px（约一个字），圆点缩小但仍钉在竖线上。
   早先 lv-2 只比 lv-1 多 12px（26 → 38），文字只挪了一点，看着像「没有缩进」
   （用户 2026-10-20）。现在 26 / 42 / 58，从属关系一眼能看出来。 */
.ol-item.lv-2 { padding-left: 42px; }
.ol-item.lv-3 { padding-left: 58px; }
.ol-item.lv-2::before,
.ol-item.lv-3::before {
  width: 7px;
  height: 7px;
  left: 2px;
  top: 11px;
}

.ol-link {
  display: block;
  width: 100%;
  padding: 3px 0;
  background: none;
  border: 0;
  cursor: pointer;
  text-align: left;
  font-size: 13px;
  line-height: 1.5;
  color: var(--t3, #5a5f7a);
  overflow-wrap: anywhere;
}
.ol-item.lv-2 .ol-link,
.ol-item.lv-3 .ol-link { font-size: 12.5px; }
.ol-link:hover { color: var(--hr-primary, var(--v, #6366f1)); }
.ol-item.active > .ol-link { color: var(--t, #0d1026); font-weight: 600; }

/* ── v1.12：本章配了章色（numsec.color）→ 目录条目同步染色（「章头 + 目录联动」）──
   --ol-color 是 scan() 从标题的 --hbc-toc 读出来的；取不到就不加 has-color，走默认。
   圆点、文字、hover 一起换成章色；active 仍保留加粗（高亮语义不丢）。 */
.ol-item.has-color::before { border-color: var(--ol-color); }
.ol-item.has-color.active::before {
  border-color: var(--ol-color);
  background: var(--ol-color);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--ol-color) 18%, transparent);
}
.ol-item.has-color > .ol-link { color: var(--ol-color); }
.ol-item.has-color > .ol-link:hover { color: color-mix(in srgb, var(--ol-color) 72%, #000); }
.ol-item.has-color.active > .ol-link {
  color: var(--ol-color);
  font-weight: 600;
}

/* 窄屏隐藏：父页网格也只在 ≥1100px 生效，两边同一断点 */
@media (max-width: 1099.98px) {
  .hr-outline { display: none; }
}

/* ══════════ v1.21：窄屏悬浮目录（FAB + 抽屉）══════════
   断点与上面「窄屏隐藏 .hr-outline」严格一致（1099.98），两套 UI 不重叠：
   宽屏只有左侧吸顶，窄屏只有 FAB + 抽屉。 */

.ol-fab {
  display: none;               /* 宽屏不显示，仅窄屏由下面的媒体查询打开 */
  position: fixed;
  right: 20px;
  bottom: calc(24px + env(safe-area-inset-bottom, 0px));
  z-index: 1200;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: var(--hr-primary, var(--v, #6366f1));
  color: #fff;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(13, 16, 38, 0.22);
  transition: transform 0.15s, box-shadow 0.15s;
}
.ol-fab:hover { transform: translateY(-1px); box-shadow: 0 10px 26px rgba(13, 16, 38, 0.26); }
.ol-fab:active { transform: scale(0.96); }
.ol-fab:focus-visible { outline: 3px solid rgba(99, 102, 241, 0.35); outline-offset: 2px; }

.ol-mask {
  position: fixed;
  inset: 0;
  z-index: 1300;
  background: rgba(13, 16, 38, 0.34);
}

.ol-drawer {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 1301;
  display: flex;
  flex-direction: column;
  width: min(82vw, 320px);
  padding: 16px 16px 10px;
  background: #fff;
  border-radius: 0 16px 16px 0;
  box-shadow: 10px 0 34px rgba(13, 16, 38, 0.18);
}
.ol-drawer-hd {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 4px 12px;
  border-bottom: 1px solid var(--bd, rgba(99, 102, 241, 0.14));
}
.ol-drawer-title { font-size: 14px; font-weight: 600; color: var(--t, #0d1026); }
.ol-drawer-count {
  font-family: var(--fm, ui-monospace, monospace);
  font-size: 11.5px;
  padding: 1px 7px;
  border-radius: 999px;
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.1);
  color: var(--hr-primary, var(--v, #6366f1));
}
.ol-drawer-close {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--t3, #5a5f7a);
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
}
.ol-drawer-close:hover { background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.08); color: var(--t, #0d1026); }

.ol-drawer-body { flex: 1; min-height: 0; overflow-y: auto; padding: 10px 4px 16px; }
/* 抽屉里条目放大到可点按尺寸（侧栏那套 13px / 3px padding 在手指下太窄） */
.ol-drawer .ol-link { font-size: 14px; padding: 8px 0; }
.ol-drawer .ol-item.lv-2 .ol-link,
.ol-drawer .ol-item.lv-3 .ol-link { font-size: 13.5px; }
/* 条目内距变大 → 圆点跟着下移，仍钉在竖线上 */
.ol-drawer .ol-item::before { top: 15px; }
.ol-drawer .ol-item.lv-2::before,
.ol-drawer .ol-item.lv-3::before { top: 17px; }

/* 进出场：遮罩淡入淡出、抽屉左侧滑入 */
.ol-fade-enter-active, .ol-fade-leave-active { transition: opacity 0.2s ease; }
.ol-fade-enter-from, .ol-fade-leave-to { opacity: 0; }
.ol-slide-enter-active, .ol-slide-leave-active { transition: transform 0.24s cubic-bezier(0.22, 0.61, 0.36, 1); }
.ol-slide-enter-from, .ol-slide-leave-to { transform: translateX(-100%); }

@media (max-width: 1099.98px) {
  .ol-fab { display: inline-flex; }
}

/* 打印：FAB / 遮罩 / 抽屉都不该出现在纸上 */
@media print {
  .ol-fab, .ol-mask, .ol-drawer { display: none !important; }
}

/* 尊重系统「减少动态效果」 */
@media (prefers-reduced-motion: reduce) {
  .ol-fab,
  .ol-fade-enter-active, .ol-fade-leave-active,
  .ol-slide-enter-active, .ol-slide-leave-active { transition: none; }
}
</style>
