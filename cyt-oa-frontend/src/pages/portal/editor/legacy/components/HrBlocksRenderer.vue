<!--
HrBlocksRenderer — HR 文档块格式渲染端唯一真相。

员工页 / 草稿预览 / 版本快照弹窗 / 编辑器内联预览四处共用；10 种块类型
（口径-HR文档块编辑-后端.md §2 冻结契约，v1.1 删 timeline）按 type 分发渲染。

安全边界：
- HTML 字段（text / imageText.text / callout.text / rawhtml.html）一律走
  HrDocRenderer（DOMPurify），本组件自身零 v-html
- 纯文本字段全部插值输出，无注入面
- 图片 src 只放行 https:// 或 / 开头（与后端 URL 校验同构），拦 javascript: 等伪协议

单块预览：编辑器给每个结构化块做「所见即所得」小预览时，传长度 1 的数组即可，
无需整篇上下文（代价是 section 块在单块预览里永远编号 1，可接受）。

样式 scoped 自持：观感对齐 portal-design.css 的 article-* 家族，但不依赖其全局类
（这些页面没引 portal-design 也要能正常渲染）。不用裸 <section> 元素——
portal-design.css 有 section{padding:120px} 全局规则，legacy 页引入时会误伤。
-->
<template>
  <div class="hr-blocks" :style="themeVars">
    <template v-for="e in entries" :key="e.idx">
      <!-- text：TipTap 富文本段 -->
      <HrDocRenderer v-if="e.block.type === 'text'" class="hb hb-text" :html="e.block.text" />

      <!-- section：小节标题；id 供大纲 scrollIntoView 定位。
           🔴 不再渲染自动编号（01/02）—— 用户 2026-10-20：「不要 01、02 这种编号，
           内容自定义就好」。标题里自己写的「1.1 / 1.2」就是编号，系统再补一个会打架。
           secNo 仍保留，只用于锚点 id。 -->
      <h2
        v-else-if="e.block.type === 'section'"
        :id="`sec-${e.secNo}`"
        class="hb hb-section"
      >
        <span class="hb-section-title">{{ e.block.title }}</span>
      </h2>

      <!-- image：图片 + 图说 -->
      <figure v-else-if="e.block.type === 'image'" class="hb hb-image">
        <img v-if="safeUrl(e.block.image)" :src="safeUrl(e.block.image)" :alt="e.block.caption || ''" loading="lazy">
        <figcaption v-if="e.block.caption">{{ e.block.caption }}</figcaption>
      </figure>

      <!-- imageText：图文段，桌面左图右文，无图时正文占满 -->
      <div
        v-else-if="e.block.type === 'imageText'"
        class="hb hb-imagetext"
        :class="{ 'no-media': !safeUrl(e.block.image) }"
      >
        <div v-if="safeUrl(e.block.image)" class="hb-it-media">
          <img :src="safeUrl(e.block.image)" :alt="e.block.title || ''" loading="lazy">
        </div>
        <div class="hb-it-body">
          <h3 v-if="e.block.title" class="hb-it-title">{{ e.block.title }}</h3>
          <HrDocRenderer :html="e.block.text" />
        </div>
      </div>

      <!-- callout：四色提示块 -->
      <div
        v-else-if="e.block.type === 'callout'"
        class="hb hb-callout"
        :class="`is-${calloutVariant(e.block.variant)}`"
      >
        <span class="hb-callout-icon" aria-hidden="true">{{ CALLOUT_ICON[calloutVariant(e.block.variant)] }}</span>
        <HrDocRenderer class="hb-callout-body" :html="e.block.text" />
      </div>

      <!-- keypoints：要点列表，桌面双列 -->
      <div v-else-if="e.block.type === 'keypoints'" class="hb hb-keypoints">
        <div v-for="(it, j) in e.block.items || []" :key="j" class="hb-kp">
          <span class="hb-kp-icon hb-icon" :class="iconShape(it.icon || '•')" aria-hidden="true">{{ it.icon || '•' }}</span>
          <div class="hb-kp-body">
            <div class="hb-kp-title">{{ it.title }}</div>
            <div v-if="it.desc" class="hb-kp-desc">{{ it.desc }}</div>
          </div>
        </div>
      </div>

      <!-- cards：卡片组，columns 决定 2/3 列 -->
      <div
        v-else-if="e.block.type === 'cards'"
        class="hb hb-cards"
        :class="Number(e.block.columns) === 3 ? 'cols-3' : 'cols-2'"
      >
        <div v-for="(it, j) in e.block.items || []" :key="j" class="hb-card">
          <span v-if="it.icon" class="hb-card-icon hb-icon" :class="iconShape(it.icon)" aria-hidden="true">{{ it.icon }}</span>
          <div class="hb-card-title">{{ it.title }}</div>
          <div v-if="it.text" class="hb-card-text">{{ it.text }}</div>
        </div>
      </div>

      <!-- steps：自动编号圆点步骤 -->
      <div v-else-if="e.block.type === 'steps'" class="hb hb-steps">
        <div v-for="(it, j) in e.block.items || []" :key="j" class="hb-step">
          <span class="hb-step-dot" aria-hidden="true">{{ j + 1 }}</span>
          <div class="hb-step-body">
            <div class="hb-step-title">{{ it.title }}</div>
            <div v-if="it.desc" class="hb-step-desc">{{ it.desc }}</div>
          </div>
        </div>
      </div>

      <!-- divider：分隔线，可带居中短语 -->
      <div
        v-else-if="e.block.type === 'divider'"
        class="hb hb-divider"
        :class="{ 'has-text': !!e.block.text }"
        role="separator"
      >
        <span v-if="e.block.text" class="hb-divider-text">{{ e.block.text }}</span>
      </div>

      <!-- rawhtml：原样 HTML（高级块），入库已过 nh3，这里再过 DOMPurify -->
      <HrDocRenderer v-else-if="e.block.type === 'rawhtml'" class="hb hb-rawhtml" :html="e.block.html" />

      <!-- ── v1.3 新增 9 种（对齐参考「制度编辑器」）── -->

      <!-- quote：引用（左侧竖线色 + 卡片底色都可自定义） -->
      <blockquote
        v-else-if="e.block.type === 'quote'"
        class="hb hb-quote"
        :style="quoteStyle(e.block.color, e.block.bg)"
      >
        <HrDocRenderer class="hb-rt" bare :html="e.block.text" />
      </blockquote>

      <!-- docref：文档引用块（v1.17 三层）。
           ① 默认文章标题 ② 可编辑的段落富文本（留空 → 目标文档封面说明 / 引言）
           ③ 触达按钮（文字与地址都可自定义，留空 → 前往《标题》→ + /hr-doc/<slug>）
           取值规则全在 utils/hr-docref.ts（纯函数，有单测）。
           🔴 **只有第三层按钮可跳转** —— 块本身不再是按钮（用户 2026-10-16：
           「只有点击第三层的触达按钮才可以跳转，现在是点击整个块都可以」）。
           两态只影响底色：拿得到目标文档封面数据 → is-cover（主题渐变/封面图底 + 白字）；
           拿不到（草稿/无权/已下架/列表未就绪）→ is-plain（浅色卡 + /hr-doc/slug 线索）。
           编辑器内联预览（disableNavigation）下按钮不跳转，但样式与阅读页逐值相同。 -->
      <div
        v-else-if="e.block.type === 'docref'"
        class="hb hb-docref"
        :class="e.docRef.hasCover ? 'is-cover' : 'is-plain'"
        :style="e.docRef.style"
      >
        <div class="hb-docref-head">
          <span v-if="e.docRef.icon" class="hb-docref-head-icon" aria-hidden="true">{{ e.docRef.icon }}</span>{{ e.docRef.title || '未选择文档' }}
        </div>
        <small v-if="!e.docRef.hasCover && (e.block.document_id || e.block.slug)" class="hb-docref-path">{{ e.block.document_id ? `/article/${e.block.document_id}` : `/hr-doc/${e.block.slug}` }}</small>
        <HrDocRenderer
          v-if="e.docRef.paragraph"
          class="hb-docref-text hb-rt"
          bare
          :html="e.docRef.paragraph"
        />
        <a
          v-if="e.docRef.button.url"
          class="hb-docref-btn"
          :href="e.docRef.button.url"
          :target="e.docRef.button.external ? '_blank' : undefined"
          :rel="e.docRef.button.external ? 'noopener noreferrer' : undefined"
          :title="disableNavigation ? '编辑时仅预览，阅读页可打开引用文档' : e.docRef.button.text"
          @click="onDocRefButtonClick($event, e.docRef.button)"
        >{{ e.docRef.button.text }}</a>
      </div>

      <!-- metrics：指标卡片（3 列，大数字 + 标签） -->
      <div v-else-if="e.block.type === 'metrics'" class="hb hb-metrics">
        <div v-for="(it, j) in e.block.items || []" :key="j" class="hb-metric">
          <div class="hb-metric-value">{{ it.value }}</div>
          <div class="hb-metric-label">{{ it.label }}</div>
        </div>
      </div>

      <!-- compare：对比卡（左右两栏，表头带 ✓ / ✗） -->
      <div v-else-if="e.block.type === 'compare'" class="hb hb-compare">
        <div
          v-for="(side, j) in [e.block.left, e.block.right]"
          :key="j"
          class="hb-compare-card"
          :style="{ borderLeftColor: safeColor(side && side.color) }"
        >
          <div
            class="hb-compare-head"
            :style="{ background: tint(side && side.color, 0.08), color: safeColor(side && side.color) }"
          >
            <span aria-hidden="true">{{ j === 0 ? '✓' : '✗' }}</span>{{ side && side.title }}
          </div>
          <ul class="hb-compare-list">
            <li v-for="(it, k) in nonEmpty(side && side.items)" :key="k">{{ it }}</li>
          </ul>
        </div>
      </div>

      <!-- status：状态列表（三档圆点 + 标签） -->
      <div v-else-if="e.block.type === 'status'" class="hb hb-status">
        <div v-for="(it, j) in e.block.items || []" :key="j" class="hb-status-row">
          <span
            class="hb-status-icon"
            :style="{ background: statusMeta(it.status).bg, color: statusMeta(it.status).color }"
            aria-hidden="true"
          >{{ statusMeta(it.status).icon }}</span>
          <span class="hb-status-text">{{ it.text }}</span>
          <span
            class="hb-status-tag"
            :style="{ background: statusMeta(it.status).bg, color: statusMeta(it.status).color }"
          >{{ it.tag }}</span>
        </div>
      </div>

      <!-- alert：警示横幅（四型，带标题的全宽横幅） -->
      <div
        v-else-if="e.block.type === 'alert'"
        class="hb hb-alert"
        :style="{ background: alertMeta(e.block.variant).bg, borderLeftColor: alertMeta(e.block.variant).color }"
      >
        <span class="hb-alert-icon" :style="{ color: alertMeta(e.block.variant).color }" aria-hidden="true">{{ alertMeta(e.block.variant).icon }}</span>
        <div class="hb-alert-body">
          <div class="hb-alert-title" :style="{ color: alertMeta(e.block.variant).color }">{{ e.block.title }}</div>
          <HrDocRenderer v-if="e.block.text" class="hb-alert-text hb-rt" bare :html="e.block.text" />
        </div>
      </div>

      <!-- bento：Bento 网格（2 列彩边卡片） -->
      <div v-else-if="e.block.type === 'bento'" class="hb hb-bento">
        <div
          v-for="(c, j) in e.block.cards || []"
          :key="j"
          class="hb-bento-card"
          :style="{ borderLeftColor: safeColor(c.color) }"
        >
          <div v-if="c.icon" class="hb-bento-icon" aria-hidden="true">{{ c.icon }}</div>
          <div class="hb-bento-title">{{ c.title }}</div>
          <HrDocRenderer v-if="c.desc" class="hb-bento-desc hb-rt" bare :html="c.desc" />
        </div>
      </div>

      <!-- numsec：编号章节（标题 + 正文）
           🔴 编号徽标已按用户 2026-10-20 要求下线（「不要 01、02 这种编号，内容自定义就好」）：
           章号写进标题文字即可，系统不再额外渲染徽标。wire 里的 num 字段保留（契约不变、
           老草稿不报错），只是不渲染。 -->
      <div
        v-else-if="e.block.type === 'numsec'" class="hb hb-numsec"
        :class="{ 'hb-numsec-colored': !!chapterStyle(e.block.color) }"
        :style="chapterStyle(e.block.color)"
      >
        <!-- 有标题：章头（配了章色就是彩色横幅），正文在下 -->
        <h2 v-if="e.block.title" class="hb-numsec-hd" :style="chapterStyle(e.block.color)">
          <span class="hb-numsec-title">{{ e.block.title }}</span>
        </h2>
        <!-- 标题为空：没有编号可做悬挂缩进标记，直接当正文块渲染。
             不渲染 h2，所以「没有名字的小节」自然也不会进目录。 -->
        <HrDocRenderer
          v-if="e.block.text"
          class="hb-numsec-text hb-rt" bare :html="e.block.text"
        />
      </div>

      <!-- tabcard：标签指标卡（页签切换 + 4 列指标） -->
      <div v-else-if="e.block.type === 'tabcard'" class="hb hb-tabcard">
        <div class="hb-tabcard-tabs" role="tablist">
          <button
            v-for="(t, j) in e.block.tabs || []"
            :key="j"
            type="button"
            role="tab"
            class="hb-tabcard-tab"
            :class="{ active: activeTabOf(e.idx, e.block) === j }"
            :aria-selected="activeTabOf(e.idx, e.block) === j"
            @click="setTab(e.idx, j)"
          >{{ t.name }}</button>
        </div>
        <div v-if="activeTabItem(e.idx, e.block)" class="hb-tabcard-card">
          <div class="hb-tabcard-head">{{ activeTabItem(e.idx, e.block).header }}</div>
          <div v-if="activeTabItem(e.idx, e.block).subHeader" class="hb-tabcard-sub">{{ activeTabItem(e.idx, e.block).subHeader }}</div>
          <div class="hb-tabcard-metrics">
            <div v-for="(m, k) in activeTabItem(e.idx, e.block).metrics || []" :key="k" class="hb-tabcard-metric">
              <div class="hb-tabcard-metric-value">{{ m.value }}</div>
              <div class="hb-tabcard-metric-label">{{ m.label }}</div>
            </div>
          </div>
          <HrDocRenderer
            v-if="activeTabItem(e.idx, e.block).note"
            class="hb-tabcard-note hb-rt"
            bare
            :html="activeTabItem(e.idx, e.block).note"
          />
        </div>
      </div>

      <!-- accordion：下拉表格（折叠列表） -->
      <div v-else-if="e.block.type === 'accordion'" class="hb hb-accordion">
        <div
          v-for="(it, j) in e.block.items || []"
          :key="j"
          class="hb-accordion-item"
          :class="{ open: accOpen(e.idx, j, it) }"
        >
          <button type="button" class="hb-accordion-head" :aria-expanded="accOpen(e.idx, j, it)" @click="toggleAcc(e.idx, j, it)">
            <span class="hb-accordion-icon" aria-hidden="true">{{ it.icon || '📄' }}</span>
            <span class="hb-accordion-title">{{ it.title }}</span>
            <span class="hb-accordion-arrow" aria-hidden="true">▼</span>
          </button>
          <div v-show="accOpen(e.idx, j, it)" class="hb-accordion-body">
            <ul>
              <li v-for="(line, k) in nonEmpty(it.lines)" :key="k">{{ line }}</li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 未知 type 静默跳过：后端 fail-loud 保证不会入库，这里只防御构造数据 -->
    </template>
  </div>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import HrDocRenderer from './HrDocRenderer.vue'
import { themeCssVars, resolveTheme } from '../data/hr-themes'
import { coverBackground, normalizeCover } from '../data/hr-cover'
import { usePublishedHrDocs } from '../composables/usePublishedHrDocs'
import { parseChapterColor } from '../utils/hr-chapter-color'
import { docRefButton, docRefParagraph, docRefTitle } from '../utils/hr-docref'

const router = useRouter()

/**
 * 引用块的第三层按钮 —— **整张卡唯一可跳转的东西**（v1.17）。
 * 站内地址走 router.push（不整页刷新）；外部 https 交给浏览器按 target=_blank 新窗口打开；
 * 编辑器内联预览（disableNavigation）一律拦住，点它只当预览。
 */
function onDocRefButtonClick(event, button) {
  if (props.disableNavigation) { event.preventDefault(); return }
  if (button.external || !button.url) return
  event.preventDefault()
  router.push(button.url)
}

const props = defineProps({
  // content_blocks / draft_blocks 原样传入；单块预览传长度 1 的数组
  blocks: { type: Array, default: () => [] },
  // 单块（编辑器内联预览走这个）：传对象引用而不是 `[blk]` 数组字面量。
  // 数组字面量每次父级渲染都是新引用 → 16 个内联预览全部跟着重渲染（打字/折叠时掉帧的元凶）。
  block: { type: Object, default: null },
  // 文档级主题配色（v1.5）：'' = 默认品牌紫 / 预设 key / #RRGGBB。
  // 挂在根节点上当 CSS 变量，块内 13 处品牌色自动跟随，块样式零改动。
  theme: { type: String, default: '' },
  // 块编辑器的单块内联预览不应把用户带离编辑页；阅读页与后台预览默认可跳。
  disableNavigation: { type: Boolean, default: false },
})

// 主题变量：--hr-primary / --hr-primary-rgb（后者供 rgba(...) 柔和底色用）
const themeVars = computed(() => themeCssVars(props.theme))

// 两种入口归一：单块优先
const source = computed(() => (props.block ? [props.block] : props.blocks))

// section 编号只数 section 块（穿插的其他块不占号），锚点 sec-{n} 与编号同源，
// 编辑器大纲按同一规则数号即可对上。idx 保留原始下标，供交互块（页签/折叠）的局部状态索引。
const entries = computed(() => {
  let sec = 0
  return (source.value || [])
    .map((b, idx) => ({ block: b, idx }))
    .filter(e => e.block && typeof e.block === 'object')
    .map(e => ({
      ...e,
      secNo: e.block.type === 'section' ? ++sec : 0,
      // docref 顺带把「封面视图」算出来（拿不到目标文档 → null，模板回落朴素卡）
      docRef: e.block.type === 'docref' ? docRefView(e.block) : null,
    }))
})

// ── v1.16/v1.17：文档引用块（三层 + 封面预览）──
// 封面（底色/图标/默认说明）是**目标文档的属性**、不属于块，所以渲染时按 slug 去
// 「已发布列表」里反查 theme / cover；块自己的 title / text / button_* 优先（见 utils/hr-docref.ts）。
// 该列表是会话级共享缓存（usePublishedHrDocs，与引用选择器同一份数据），一次会话只发一次请求。
const { docs: refDocs, load: loadRefDocs } = usePublishedHrDocs()
const hasDocRef = computed(() => (source.value || []).some(b => b && b.type === 'docref' && (b.document_id || b.slug)))
// 只有真存在 docref 块时才拉列表：普通文档零额外请求。
// load() 内部吞掉异常（失败静默降级成朴素卡），不会产生未处理的 rejection。
watch(hasDocRef, (yes) => { if (yes) loadRefDocs() }, { immediate: true })

// 列表 → Map：docref 每块要按 slug 查一次，线性 find 会在「几十块 × 几十篇」时白跑
const refById = computed(() => new Map(refDocs.value.map(d => [d.id, d])))
const refByLegacySlug = computed(() => new Map(refDocs.value.filter(d => d.legacy_slug).map(d => [d.legacy_slug, d])))

/**
 * 一次算全 docref 卡片三层要用的东西。**永远返回对象**（块自己就带内容，不会没东西可渲染）；
 * 只是拿不到目标文档时 `hasCover=false` → 卡片退回浅色底（is-plain）。
 *
 * 封面只取 **图标 / 说明 / 背景** 三样：中英标识、Meta 三栏是整屏封面专用的
 * 页级元素，塞进一张正文卡片里没有意义。
 * 三层文案的取值规则（块优先、留空回落）全在 utils/hr-docref.ts，这里只负责拼封面样式。
 *
 * 🔴 标题取**块里的快照**，不取目标文档当前标题：目标改名后卡片标题不该在列表加载完成时
 * 自己跳变一次，快照也让「保存时所见」保持可复现（与后端 title 快照口径一致）。
 */
function docRefView(block) {
  const meta = block.document_id
    ? refById.value.get(block.document_id)
    : refByLegacySlug.value.get(block.slug)
  const cover = normalizeCover(meta?.cover)
  const theme = resolveTheme(meta?.theme)
  return {
    hasCover: !!meta,
    // 拿不到封面数据时给个文件图标，别让第一层光秃秃
    icon: meta ? cover.icon : '📄',
    title: docRefTitle(block, meta),
    paragraph: docRefParagraph(block, meta),
    button: docRefButton(block, meta),
    // 背景与整屏封面同一套取值逻辑（主题渐变 / 封面图 / 深色），
    // 主题变量挂在卡片自己身上 → 卡内白底按钮的文字色跟着目标文档主题走
    style: meta
      ? { background: coverBackground(cover, theme.gradient), ...themeCssVars(meta.theme) }
      : undefined,
  }
}

// 与后端 image URL 校验同构：只认 https:// 或站内相对路径（开发环境落盘走 /）；
// 不合法直接不渲染 img，防 javascript: 伪协议
const safeUrl = (u) => (typeof u === 'string' && /^(https:\/\/|\/)/.test(u) ? u : '')

// 纯文本数组（compare.items / accordion.lines）渲染前剔除空白行——
// 编辑器用「一行一条」的 textarea 录入，尾随空行不该变成空条目（与参考同构）
const nonEmpty = (arr) => (Array.isArray(arr) ? arr.filter(s => String(s).trim()) : [])

const CALLOUT_VARIANTS = ['info', 'success', 'warning', 'danger']
const calloutVariant = (v) => (CALLOUT_VARIANTS.includes(v) ? v : 'info')
const CALLOUT_ICON = { info: '💡', success: '✅', warning: '⚠️', danger: '⛔' }

// ── v1.3：颜色与状态色板 ──
// 后端只放行 #RRGGBB（domain._check_value kind=color），这里同构兜底：
// 任何非法值退回中性灰，绝不把原始串拼进内联 style（掐死 CSS 注入面）
const safeColor = (c) => (typeof c === 'string' && /^#[0-9a-fA-F]{6}$/.test(c) ? c : '#64748b')
// quote.color = 左侧竖线色、quote.bg = 卡片底色（v1.18 起可各自自定义、相互独立）。
// 二者都走 themecolor 契约：空串/缺失/非法值 → 不写该项 → CSS 回落到文档主题默认；
// 只有严格 #RRGGBB 才写 inline style，绝不把原始串拼进 style（掐死 CSS 注入面，同 compare/bounded 的做法）。
const HEX6_RE = /^#[0-9a-fA-F]{6}$/
const quoteStyle = (color, bg) => {
  const style = {}
  if (typeof color === 'string' && HEX6_RE.test(color)) style.borderLeftColor = color
  if (typeof bg === 'string' && HEX6_RE.test(bg)) style.background = bg
  return Object.keys(style).length > 0 ? style : undefined
}
// 取同一色的浅底（#RRGGBB + alpha 后缀），与参考的 `color + '15'` 同构
const tint = (c, alpha) => {
  const hex = safeColor(c)
  const a = Math.round(Math.min(Math.max(alpha, 0), 1) * 255).toString(16).padStart(2, '0')
  return hex + a
}

// 状态列表三档（沿用参考色：达标=teal / 未达标=danger / 待改进=warn）
const STATUS_META = {
  pass: { icon: '✓', color: '#0d9488', bg: 'rgba(13,148,136,.12)' },
  fail: { icon: '✗', color: '#ef4444', bg: 'rgba(239,68,68,.12)' },
  warn: { icon: '!', color: '#f59e0b', bg: 'rgba(245,158,11,.14)' },
}
const statusMeta = (s) => STATUS_META[s] || STATUS_META.warn

// 警示横幅四型（info 用品牌色，其余沿用参考色）
const ALERT_META = {
  // info 型跟随文档主题色（参考同此：info 用 theme.primary，其余三型是语义色不跟主题）
  info: { icon: 'ℹ', color: 'var(--hr-primary, #6366f1)', bg: 'rgba(var(--hr-primary-rgb, 99, 102, 241), .08)' },
  warn: { icon: '⚠', color: '#f59e0b', bg: 'rgba(245,158,11,.10)' },
  danger: { icon: '✗', color: '#ef4444', bg: 'rgba(239,68,68,.08)' },
  success: { icon: '✓', color: '#22c55e', bg: 'rgba(34,197,94,.08)' },
}
const alertMeta = (v) => ALERT_META[v] || ALERT_META.warn

// ── v1.12：章节配色（numsec.color）──
// 用户 2026-10-14：「章节的颜色可以自定义，包括目录的也是」「没设置就根据文章内主题配色而定」。
// 语义：color 为空串 / 非法 → **不配色**（返回 undefined，整块不写任何变量），
//       CSS 自然回落到 --hr-primary（文档主题）；合法 #RRGGBB → 写局部别名 --hbc-toc。
// 解析逻辑抽到 utils/hr-chapter-color.ts（可单测，且目录导航共用同一份）。
function chapterStyle(color) {
  return parseChapterColor(color)?.style
}

// ── 交互块局部状态（tabcard 页签 / accordion 折叠）──
// 按块下标索引，只活在本次渲染里：不改数据、不落库。activeTab / item.open 只作初始默认值。
// ⚠️ 读取路径（activeTabOf / accOpen）必须**纯只读**：渲染期间往 reactive 上写会再触发一轮渲染，
// 十几个块各来一次就是肉眼可见的卡顿。写入只发生在事件处理里（setTab / toggleAcc）。
const ui = reactive({})
function bucketFor(i) {
  let b = ui[i]
  if (!b) { b = { tab: null, acc: {} }; ui[i] = b }
  return b
}
const tabCount = (blk) => (blk.tabs || []).length
function activeTabOf(i, blk) {
  const n = tabCount(blk)
  if (!n) return 0
  const st = ui[i]
  const t = st ? st.tab : null
  if (typeof t === 'number' && t >= 0 && t < n) return t
  return Math.min(Math.max(Number(blk.activeTab) || 0, 0), n - 1)
}
function setTab(i, n) { bucketFor(i).tab = n }
function activeTabItem(i, blk) { return (blk.tabs || [])[activeTabOf(i, blk)] }
function accOpen(i, j, item) {
  const st = ui[i]
  const acc = st && st.acc
  return acc && (j in acc) ? acc[j] : !!item.open
}
function toggleAcc(i, j, item) {
  const acc = bucketFor(i).acc
  acc[j] = !accOpen(i, j, item)
}

// cards/keypoints 的 icon 支持 emoji 或短文字：≤2 个码点走方形芯片，
// 更长的按胶囊标签排——芯片宽度全靠 padding 自适应，杜绝「字压出芯片」老 bug。
// 用 [...str] 按码点数而非 UTF-16 长度，单个 emoji（常占 2 个 code unit）才不会被误判成长文本。
const iconShape = (icon) => ([...String(icon).trim()].length <= 2 ? 'is-chip' : 'is-pill')
</script>

<style scoped>
.hr-blocks {
  color: var(--t, #0d1026);
  font-size: 15px;
  line-height: 1.75;
}

/* 块间距统一在这层控制，块内部样式不再各自留外边距余量 */
.hb { margin: 18px 0; }
.hr-blocks > .hb:first-child { margin-top: 0; }
.hr-blocks > .hb:last-child { margin-bottom: 0; }

/* ── 块内富文本正文（v1.4：quote / alert / bento / numsec / tabcard 的正文改富文本）──
   这 5 处的 HTML 走 HrDocRenderer 的 bare 模式（不带 .hr-doc-content 的文档级排版），
   字号行高由各自的 .hb-xxx-text 容器决定；这里统一把段落外边距收掉、多段之间只留一点，
   否则浏览器默认 p 的 1em 外边距会把紧凑卡片撑变形。

   🔴 v1.9.4：**表格样式必须在这里补一份**。HrDocRenderer 的表格排版（border-collapse /
   border / padding）只挂在 `.hr-doc-content` 下，而 bare 模式**不带这个 class**
   → 块内表格会退化成浏览器默认（无边框、无内边距、宽度收缩到内容宽），
   在 782px 的预览栏里只有 75×8px，看着就像「表格没生效」。
   同一段 HTML 在 text 块（非 bare）里是 687×140px 的正常表格，
   两处观感必须一致 —— 这就是当初「只在编辑框里体现」的真因。
   量测：`.hb-rt table` 687×140 / 边框 1px solid / padding 6px 10px（与编辑框同档）。 */
.hb-rt :deep(p) { margin: 0; }
.hb-rt :deep(p + p) { margin-top: 0.7em; }
.hb-rt :deep(ul),
.hb-rt :deep(ol) { margin: 0.4em 0; padding-left: 1.3em; }
.hb-rt :deep(li) { margin: 0.15em 0; }
.hb-rt :deep(a) { color: var(--hr-primary, var(--v, #6366f1)); text-decoration: none; border-bottom: 1px solid rgba(var(--hr-primary-rgb, 99, 102, 241), 0.25); }
/* ── v1.13：补 bare 模式缺失的块级排版（引用 / 标题 / 加重 / 代码块）──
   🔴 这是 v1.9.4 只补表格留下的漏项：`HrDocRenderer` 的段落/标题/引用排版**只写在
   `.hr-doc-content` 下**，而 bare 模式（quote/alert/bento/numsec/tabcard）刻意不带该 class
   → 在「编号章节」正文里用工具栏的「引用」/「标题」/「加粗」插入的这些标签**一直零样式**，
   预览与正式发布后跟普通文字没区别（用户 2026-10-15 实测到）。
   这里在 .hb-rt 补一份同档样式；色值统一走 --hr-primary，好跟随该文档自己的主题。
   ⚠️ 以后 .hr-doc-content 增删任何标签排版，都要回来问「.hb-rt 有对应规则吗」。 */
/* 块内引用（blockquote）：与 quote 块同一套观感（4px 线 / 更舒适内距）。
   竖线色与底色可被用户选的内联 style.border-left-color / background-color 覆盖
   （v1.18 BlockquoteWithColor 的 blockquoteColor / blockquoteBg，见 RichTextEditor）。 */
.hb-rt :deep(blockquote) {
  margin: 0.7em 0;
  padding: 10px 16px;
  border-left: 4px solid var(--hr-primary, var(--v, #6366f1));
  border-radius: 0 10px 10px 0;
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.045);
  color: var(--t2, #2D3258);
}
.hb-rt :deep(blockquote p) { margin: 0; }
.hb-rt :deep(h1),
.hb-rt :deep(h2),
.hb-rt :deep(h3),
.hb-rt :deep(h4) {
  color: var(--t, #0d1026);
  font-weight: 600;
  line-height: 1.3;
  margin: 0.9em 0 0.4em;
}
.hb-rt :deep(h1) { font-size: 1.5em; font-weight: 400; }
.hb-rt :deep(h2) { font-size: 1.22em; }
.hb-rt :deep(h3) { font-size: 1.05em; }
.hb-rt :deep(h4) { font-size: 0.96em; }
.hb-rt :deep(strong),
.hb-rt :deep(b) { color: var(--t, #0d1026); font-weight: 600; }
/* 🔴 作者用工具栏给加粗文字选过颜色时必须让位：Color 标记渲染成外层 <span style="color:…">，
   而上面写在 strong 上的显式 color **胜过从父级继承来的内联色**（CSS 里显式值 > 继承），
   于是编辑器里红色的加粗，一进「预览」小面板和正式发布页就变黑（用户 2026-11-03 实测）。
   只在这类带内联 color 的 span 里把颜色交还给继承，其余加粗仍是 --t 墨色，排版不变。 */
.hb-rt :deep(span[style*="color:"] :is(strong, b)) { color: inherit; }
.hb-rt :deep(pre) {
  font-family: var(--fm, ui-monospace, monospace);
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.06);
  border: 1px solid rgba(var(--hr-primary-rgb, 99, 102, 241), 0.12);
  border-radius: 8px;
  padding: 8px 10px;
  margin: 0.6em 0;
  overflow-x: auto;
  font-size: 0.9em;
}
.hb-rt :deep(pre code) { background: none; padding: 0; }
.hb-rt :deep(code) {
  font-family: var(--fm, ui-monospace, monospace);
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.08);
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 0.94em;
}
/* 表格（与 .hr-doc-content 的表格排版同档，略收字号以适配紧凑卡片）
   ⚠️ 不要给 <table> 设 display:block —— 那会让各列宽度各算各的、表格变参差。
   宽度自适应交给 width:100%（列宽仍由浏览器按内容分配，与编辑框一致）；
   TipTap 落库残留的 .tableWrapper 承担横向滚动。 */
.hb-rt :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0.7em 0;
  font-size: 0.94em;
}
/* 自适应宽度（表格菜单「自适应宽度」开关）：强制铺满块内正文，忽略内联宽度与固定列宽 */
.hb-rt :deep(table.hr-doc--auto-width) { width: 100% !important; table-layout: auto !important; }
.hb-rt :deep(table.hr-doc--auto-width col) { width: auto !important; }
.hb-rt :deep(table.hr-doc--auto-width th),
.hb-rt :deep(table.hr-doc--auto-width td) { width: auto !important; }
/* TipTap 保存下来的表格外层包裹（.tableWrapper 是编辑态产物，落库 HTML 里可能有）：
   让它承担横向滚动，表格本身保持正常布局 */
.hb-rt :deep(.tableWrapper) {
  overflow-x: auto;
  margin: 0.7em 0;
}
.hb-rt :deep(.tableWrapper table) { margin: 0; }
.hb-rt :deep(th),
.hb-rt :deep(td) {
  border: 1px solid var(--bd, rgba(var(--hr-primary-rgb, 99, 102, 241), 0.22));
  padding: 6px 10px;
  text-align: left;
  vertical-align: top;
  line-height: 1.7;
  /* 空单元格也要占位（否则空表格塌成一条线） */
  min-width: 48px;
}
.hb-rt :deep(th) {
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.06);
  font-weight: 600;
  color: var(--t2, #2d3258);
}
/* 斑马纹 + 悬停提亮（与编辑器/阅读页一致；只作用于 tbody，表头不参与）
   .hr-doc--no-zebra = 表格菜单里「斑马纹」被显式关闭 */
.hb-rt :deep(table:not(.hr-doc--no-zebra) tbody tr:nth-child(even)) { background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.03); }
.hb-rt :deep(table tbody tr:hover) { background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.05); }
/* 首列做表头（左侧加粗）：由编辑器表格菜单写入 hr-doc--first-col 时生效
   🔴 用 class 而非 data-* —— 后端 nh3 白名单会剥掉所有 data-*（见 RteTableMenu 注释）。 */
.hb-rt :deep(table.hr-doc--first-col tbody td:first-child) {
  font-weight: 600;
  color: var(--t2, #2d3258);
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.04);
}

/* ── section 小节标题 ──
   🔴 层级：章节标题（.hb-numsec-title 24px）必须**明显大于**小节标题（17px）。
   演进过程：最初 22 / 18 是倒挂的 → 改成 22 / 20 仍只差 2px，而小节是纯黑字、
   章是横幅上的白字，黑色视觉重量压过白字，看起来小节比章还重（用户 2026-10-20 第二次反馈
   「章节标题之间层级不对」）→ 现在 24 / 17，差 7px，且 17px 与图文段标题 / 富文本 h3 同一档。
   段前间距 24px（用户：「段前分隔太大」，原 44px）。 */
.hb-section {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin: 24px 0 12px;
  /* 🔴 缩进阶梯：一级（章）顶格 → 二级（小节）缩进约 1 个字符 → 正文顶格。
     章头横幅靠 margin 左右出血把内 padding 抵消掉，文字落在正文左边界（见 .hb-numsec-colored）；
     这里给小节补 18px（17px 字号约一个字），从属关系一眼看得出来。
     早先版本把小节也对齐到横幅文字（16px），结果「一级也缩进、二级也缩进」，等于没有阶梯
     （用户 2026-10-20：「一级标题顶格，二级标题应该开始有缩进才对」）。 */
  padding-left: 18px;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--t, #0d1026);
  /* 大纲锚点定位时让标题露在固定顶栏下方 */
  scroll-margin-top: calc(var(--nav-h, 64px) + 16px);
}

/* ── image 图片块 ── */
.hb-image {
  margin: 24px 0;
  text-align: center;
}
.hb-image img {
  max-width: 100%;
  border-radius: 12px;
  border: 1px solid var(--bd, rgba(var(--hr-primary-rgb, 99, 102, 241), 0.13));
}
.hb-image figcaption {
  margin-top: 10px;
  font-size: 12.5px;
  color: var(--t4, #8c92ab);
}

/* ── imageText 图文段 ── */
.hb-imagetext {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 3fr);
  gap: 24px;
  align-items: start;
  margin: 24px 0;
}
.hb-imagetext.no-media { grid-template-columns: minmax(0, 1fr); }
/* 文字列必须能收缩：grid 子项默认 min-width:auto，内容（长串/长链接）会把列顶宽，
   配合 HrDocRenderer 的 overflow-wrap:anywhere 才能真正折行，而不是撑出横向滚动条。 */
.hb-it-body { min-width: 0; }
.hb-it-media img {
  display: block;
  width: 100%;
  border-radius: 12px;
  border: 1px solid var(--bd, rgba(var(--hr-primary-rgb, 99, 102, 241), 0.13));
}
.hb-it-title {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 600;
  color: var(--t, #0d1026);
}

/* ── callout 提示块（四色变体） ── */
.hb-callout {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 14px 18px;
  border-radius: 10px;
  border-left: 3px solid;
  font-size: 14px;
}
.hb-callout-icon { flex-shrink: 0; font-size: 16px; line-height: 1.6; }
.hb-callout-body { min-width: 0; font-size: 14px; }
.hb-callout-body :deep(p:first-child) { margin-top: 0; }
.hb-callout-body :deep(p:last-child) { margin-bottom: 0; }
.hb-callout.is-info    { background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.06);  border-color: #6366f1; }
.hb-callout.is-success { background: rgba(16, 185, 129, 0.07);  border-color: #059669; }
.hb-callout.is-warning { background: rgba(245, 158, 11, 0.08);  border-color: #d97706; }
.hb-callout.is-danger  { background: rgba(244, 63, 94, 0.06);   border-color: #e11d48; }

/* ── keypoints 要点列表 ── */
.hb-keypoints {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 20px;
}
.hb-kp {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid var(--bd, rgba(var(--hr-primary-rgb, 99, 102, 241), 0.13));
  border-radius: 10px;
}
/* 要点 icon 芯片：flex 行内不许被压缩，但也不许挤走正文——上限 40% 行宽，超长换行 */
.hb-kp-icon { flex-shrink: 0; max-width: 40%; }
.hb-kp-body { min-width: 0; }
.hb-kp-title { font-weight: 600; font-size: 14px; color: var(--t, #0d1026); }
.hb-kp-desc {
  margin-top: 3px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--t3, #5a5f7a);
  white-space: pre-line;
}

/* ── cards 卡片组 ── */
.hb-cards { display: grid; gap: 16px; }
.hb-cards.cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.hb-cards.cols-3 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.hb-card {
  padding: 20px 22px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid var(--bd, rgba(var(--hr-primary-rgb, 99, 102, 241), 0.13));
  border-radius: 12px;
}
.hb-card-icon { margin-bottom: 12px; max-width: 100%; }
.hb-card-title { font-weight: 600; font-size: 15px; color: var(--t, #0d1026); }
.hb-card-text {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--t3, #5a5f7a);
  white-space: pre-line;
}

/* ── icon 芯片（keypoints / cards 共用）──
   宽度全靠 padding 自适应（禁固定宽/绝对定位）：短 icon（≤2 码点，emoji/单双字）
   走近方形芯片，长文字走胶囊标签；超长时允许折行，永不压到旁边/下方的标题上 */
.hb-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 8px;
  border-radius: 10px;
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.08);
  font-size: 17px;
  line-height: 1.4;
  overflow-wrap: anywhere;
  word-break: break-word;
}
/* 方形芯片：min-width=min-height 保证 1~2 字符时视觉近正方，内容多一点也只会横向撑开 */
.hb-icon.is-chip {
  min-width: 34px;
  min-height: 34px;
}
/* 胶囊标签：长文字降字号提字重，观感是「标签」而不是被撑爆的图标框 */
.hb-icon.is-pill {
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--hr-primary, var(--v, #6366f1));
}

/* ── steps 步骤（编号圆点） ── */
.hb-steps { display: flex; flex-direction: column; gap: 16px; }
.hb-step { display: flex; gap: 16px; }
.hb-step-dot {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--hr-primary, var(--v, #6366f1));
  color: #fff;
  font-family: var(--fm, ui-monospace, monospace);
  font-size: 12px;
  font-weight: 700;
}
.hb-step-title { font-weight: 600; font-size: 15px; padding-top: 3px; color: var(--t, #0d1026); }
.hb-step-desc {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--t3, #5a5f7a);
  white-space: pre-line;
}

/* ── divider 分隔线 ──
   🔴 上下留白别太大：34px 时一条线像被孤立成一段（用户 2026-10-20 截图：
   「分割线前后行距大概我框起来这么多就好」，约 16px 上下）。 */
.hb-divider {
  display: flex;
  align-items: center;
  margin: 16px 0;
}
.hb-divider.has-text { gap: 14px; }
.hb-divider::before,
.hb-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--bd, rgba(var(--hr-primary-rgb, 99, 102, 241), 0.16));
}
.hb-divider-text {
  font-size: 12px;
  color: var(--t4, #8c92ab);
  letter-spacing: 0.12em;
}

/* ══════════ v1.3 新增 9 种块（对齐参考「制度编辑器」的渲染） ══════════ */

/* ── quote 引用（v1.18：竖线 + 底色都可自定义）──
   默认（未选色）：主题色竖线 + 同色浅底；用户在编辑器里选了竖线色 / 底色后，
   inline style 覆盖这两项。观感对齐内联 blockquote（见 .hb-rt blockquote）。 */
.hb-quote {
  margin: 24px 0;
  padding: 16px 22px;
  border-left: 4px solid var(--hr-primary, var(--v, #6366f1));
  border-radius: 0 12px 12px 0;
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.05);
  box-shadow: 0 2px 12px rgba(13, 16, 38, 0.04);
  /* 排版落在容器上（内容由 bare 模式的 HrDocRenderer 渲染，不再固定是单个 <p>） */
  font-size: 15px;
  line-height: 1.85;
  color: var(--t2, #2d3258);
}

/* ── docref 文档引用卡片（v1.17 三层：标题 / 段落富文本 / 触达按钮）──
   🔴 **整张卡不再可点**，只有第三层按钮是链接（用户 2026-10-16 明确要求）。
   两态只影响底色：is-cover 用目标文档的封面（渐变/图/深色，内联 style 给）+ 白字；
   is-plain 拿不到封面数据 → 浅色卡 + 深字。 */
.hb-docref { display: block; width: 100%; border-radius: 12px; }
.hb-docref.is-plain {
  padding: 16px 18px; border: 1px solid rgba(var(--hr-primary-rgb, 99,102,241), .18);
  background: rgba(255,255,255,.85); color: var(--t2, #2d3258);
  box-shadow: 0 5px 18px rgba(13,16,38,.045);
}
.hb-docref.is-cover {
  padding: 28px 24px; text-align: center; color: #fff;
  box-shadow: 0 8px 26px rgba(13,16,38,.14);
}

/* ① 第一层：默认文章标题 */
.hb-docref-head { font-size: 16px; font-weight: 700; line-height: 1.5; }
.hb-docref.is-cover .hb-docref-head { color: #fff; }
.hb-docref-head-icon { margin-right: 6px; }
/* is-plain 才给的目标路径线索（v1.15 的观感，标出这张卡指的是哪篇） */
.hb-docref-path { display: block; margin-top: 2px; color: var(--t4, #8c92ab); font-family: var(--fm, monospace); font-size: 11.5px; }

/* ② 第二层：段落富文本（走 HrDocRenderer 的 bare 模式，排版在 .hb-rt 里补过一份） */
.hb-docref-text { margin-top: 6px; font-size: 13.5px; line-height: 1.7; }
.hb-docref.is-cover .hb-docref-text { color: rgba(255,255,255,.88); }
/* 🔴 封面态必须把文字色阶整体翻白：`.hb-rt` 的 标题/加粗/引用 都写死了 var(--t)/var(--t2)，
   不覆盖就会出现「白卡上的深色字印在彩色底上」——正是「块内富文本样式挂错作用域」那类坑。
   这里只改这三个变量的局部值，块内 20 多处排版规则自动跟着变白，不逐条写 :deep 覆盖。
   用户自己在工具栏选的字体颜色是内联 style，优先级更高，照样生效。 */
.hb-docref.is-cover .hb-docref-text {
  --t: #fff;
  --t2: rgba(255,255,255,.94);
  --t3: rgba(255,255,255,.88);
  --bd: rgba(255,255,255,.3);
}
.hb-docref.is-cover .hb-docref-text :deep(a) { color: #fff; border-bottom-color: rgba(255,255,255,.45); }

/* ③ 第三层：触达按钮 —— 整张卡唯一可点的东西。文字色取 var(--hr-primary)：
   is-cover 时内联 style 已把**目标文档**的主题变量挂在这张卡上，所以按钮文字跟着目标主题走。 */
.hb-docref-btn {
  display: inline-block; margin-top: 18px; padding: 11px 22px; border-radius: 10px;
  font-size: 13.5px; font-weight: 700; text-decoration: none;
  transition: transform .16s, box-shadow .16s, background .16s;
}
.hb-docref.is-cover .hb-docref-btn { background: #fff; color: var(--hr-primary, #6366f1); }
.hb-docref.is-cover .hb-docref-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(0,0,0,.18); }
.hb-docref.is-plain .hb-docref-btn {
  background: rgba(var(--hr-primary-rgb, 99,102,241), .1); color: var(--hr-primary, #6366f1);
}
.hb-docref.is-plain .hb-docref-btn:hover { background: rgba(var(--hr-primary-rgb, 99,102,241), .18); }

/* ── metrics 指标卡片（3 列） ── */
.hb-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
.hb-metric {
  padding: 18px 12px;
  border-radius: 10px;
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.045);
  text-align: center;
}
.hb-metric-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.25;
  color: var(--hr-primary, var(--v, #6366f1));
  letter-spacing: -0.01em;
  overflow-wrap: anywhere;
}
.hb-metric-label {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--t4, #8c92ab);
}

/* ── compare 对比卡（左右两栏） ── */
.hb-compare {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.hb-compare-card {
  border: 1px solid var(--bd, rgba(var(--hr-primary-rgb, 99, 102, 241), 0.13));
  border-left: 4px solid;
  border-radius: 12px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.85);
}
.hb-compare-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  font-size: 13.5px;
  font-weight: 600;
}
.hb-compare-list {
  list-style: none;
  margin: 0;
  padding: 12px 14px;
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--t3, #5a5f7a);
}
.hb-compare-list li { position: relative; padding-left: 14px; }
.hb-compare-list li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: inherit;
}

/* ── status 状态列表 ── */
.hb-status { display: flex; flex-direction: column; }
.hb-status-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--bd, rgba(var(--hr-primary-rgb, 99, 102, 241), 0.13));
  font-size: 14px;
}
.hb-status-row:last-child { border-bottom: none; }
.hb-status-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
}
.hb-status-text { flex: 1; min-width: 0; color: var(--t, #0d1026); }
.hb-status-tag {
  flex-shrink: 0;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
}

/* ── alert 警示横幅（四型） ── */
.hb-alert {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 18px;
  border-radius: 10px;
  border-left: 4px solid;
}
.hb-alert-icon { flex-shrink: 0; font-size: 17px; line-height: 1.5; }
.hb-alert-body { min-width: 0; }
.hb-alert-title { font-size: 14.5px; font-weight: 600; }
.hb-alert-text {
  margin-top: 3px;
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--t3, #5a5f7a);
}

/* ── bento Bento 网格（2 列彩边卡） ── */
.hb-bento {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.hb-bento-card {
  padding: 14px 18px;
  border-left: 3px solid;
  border-radius: 0 10px 10px 0;
  background: rgba(255, 255, 255, 0.6);
}
.hb-bento-icon { font-size: 19px; line-height: 1.3; }
.hb-bento-title {
  margin-top: 4px;
  font-size: 14.5px;
  font-weight: 600;
  color: var(--t, #0d1026);
}
.hb-bento-desc {
  margin-top: 3px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--t4, #8c92ab);
}

/* ── numsec 章节标题 ──
   🔴 编号徽标已下线（用户 2026-10-20：不要 01/02 自动编号）。
   🔴 字号层级：章节 24px > 小节 17px，必须**一眼能分出主次**——
   22 / 20 那版只差 2px，加上小节是黑字、章是白字，视觉上反而小节更重。
   章色（v1.12）：配了 color 就给整块挂 --hbc-toc 局部别名 → 章头横幅走渐变；
   同时 h2 自身带变量，左侧目录扫到就把该条也染成同色（「章头 + 目录联动」）。 */
.hb-numsec { padding: 6px 0; }
.hb-numsec-hd {
  display: flex;
  align-items: center;
  gap: 10px;
  /* 是 h2，显式清掉浏览器默认的 margin / 字号（标题字号由 .hb-numsec-title 决定） */
  margin: 0;
  font-size: inherit;
  line-height: inherit;
}
.hb-numsec-title {
  min-width: 0;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.35;
  color: var(--t, #0d1026);
}
.hb-numsec-text {
  margin-top: 6px;
  font-size: 14.5px;
  line-height: 1.8;
  color: var(--t3, #5a5f7a);
}

/* ── v1.12：配了章色的编号章节 = 章头横幅 ──
   渐变暗端用 color-mix 从同一个 --hbc-toc 算出来，所以**只存一个颜色**。
   --hbc-toc-rgb 供 rgba() 用（正文强调等）。 */
.hb-numsec-colored > .hb-numsec-hd {
  /* 左右各出血 16px，把横幅内 padding 的 16px 抵消掉 —— 章标题文字回到正文左边界（顶格）。
     不出血的话章文字比正文右移 16px，就变成「一级也缩进」，层级方向反了
     （用户 2026-10-20：一级标题顶格，二级标题才开始缩进）。 */
  margin: 0 -16px;
  padding: 12px 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--hbc-toc), color-mix(in srgb, var(--hbc-toc) 55%, #000));
}
.hb-numsec-colored > .hb-numsec-hd .hb-numsec-title { color: #fff; }
/* 正文首行加一条同色强调（只染首段，不整段变色，保持可读性） */
.hb-numsec-colored > .hb-numsec-text :deep(> p:first-child) {
  border-left: 3px solid var(--hbc-toc);
  padding-left: 10px;
}
/* 打印：横幅改纯黑省墨、白字，与第 7 章既有打印规则一致 */
@media print {
  .hb-numsec-colored > .hb-numsec-hd {
    background: #000 !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
  .hb-numsec-colored > .hb-numsec-text :deep(> p:first-child) { border-left-color: #000; }
}

/* ── tabcard 标签指标卡 ── */
.hb-tabcard-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.hb-tabcard-tab {
  padding: 5px 14px;
  border: 1px solid var(--bd, rgba(var(--hr-primary-rgb, 99, 102, 241), 0.22));
  border-radius: 999px;
  background: #fff;
  color: var(--t3, #5a5f7a);
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.hb-tabcard-tab:hover { border-color: var(--hr-primary, var(--v, #6366f1)); color: var(--hr-primary, var(--v, #6366f1)); }
.hb-tabcard-tab.active {
  background: var(--hr-primary, var(--v, #6366f1));
  border-color: var(--hr-primary, var(--v, #6366f1));
  color: #fff;
}
.hb-tabcard-card {
  padding: 16px 18px;
  border: 1px solid var(--bd, rgba(var(--hr-primary-rgb, 99, 102, 241), 0.13));
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.85);
}
.hb-tabcard-head {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--hr-primary, var(--v, #6366f1));
}
.hb-tabcard-sub {
  margin-top: 2px;
  font-size: 12.5px;
  color: var(--t4, #8c92ab);
}
.hb-tabcard-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 12px 0;
}
.hb-tabcard-metric {
  padding: 10px 6px;
  border-radius: 8px;
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.045);
  text-align: center;
}
.hb-tabcard-metric-value {
  font-size: 17px;
  font-weight: 700;
  color: var(--hr-primary, var(--v, #6366f1));
  overflow-wrap: anywhere;
}
.hb-tabcard-metric-label {
  margin-top: 2px;
  font-size: 11.5px;
  color: var(--t4, #8c92ab);
}
.hb-tabcard-note {
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--t3, #5a5f7a);
}

/* ── accordion 下拉表格 ── */
.hb-accordion { display: flex; flex-direction: column; gap: 10px; }
.hb-accordion-item {
  border: 1px solid var(--bd, rgba(var(--hr-primary-rgb, 99, 102, 241), 0.13));
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}
.hb-accordion-head {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: transparent;
  color: inherit;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}
.hb-accordion-head:hover { background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.04); }
.hb-accordion-icon { flex-shrink: 0; font-size: 16px; }
.hb-accordion-title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--t, #0d1026);
}
.hb-accordion-arrow {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--t4, #8c92ab);
  transition: transform 0.2s;
}
.hb-accordion-item.open .hb-accordion-arrow { transform: rotate(180deg); }
.hb-accordion-body {
  padding: 12px 16px 14px 42px;
  border-top: 1px solid var(--bd, rgba(var(--hr-primary-rgb, 99, 102, 241), 0.13));
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--t3, #5a5f7a);
}
.hb-accordion-body ul { list-style: none; margin: 0; padding: 0; }
.hb-accordion-body li { position: relative; padding-left: 14px; }
.hb-accordion-body li::before {
  content: '●';
  position: absolute;
  left: 0;
  top: 1px;
  color: var(--hr-primary, var(--v, #6366f1));
  font-size: 8px;
}

/* 排版任务的阅读纸张有独立宽度，按实际正文宽度响应，而非外层浏览器宽度。 */
@container hr-layout-preview (max-width: 768px) {
  .hb-keypoints, .hb-cards.cols-2, .hb-cards.cols-3, .hb-metrics, .hb-compare, .hb-bento, .hb-imagetext { grid-template-columns: minmax(0, 1fr); }
  .hb-tabcard-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .hb-section { font-size: 16px; margin-top: 20px; }
  .hb-numsec-title { font-size: 21px; }
}

/* ── 移动端：多列结构一律降单列 ── */
@media (max-width: 768px) {
  .hb-keypoints,
  .hb-cards.cols-2,
  .hb-cards.cols-3,
  .hb-metrics,
  .hb-compare,
  .hb-bento { grid-template-columns: minmax(0, 1fr); }
  .hb-tabcard-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .hb-imagetext { grid-template-columns: minmax(0, 1fr); }
  /* 移动端同样保持「章明显大于节」：章节 21px / 小节 16px */
  .hb-section { font-size: 16px; margin-top: 20px; }
  .hb-numsec-title { font-size: 21px; }
}
</style>
