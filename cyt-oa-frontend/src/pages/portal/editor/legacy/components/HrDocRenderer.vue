<!--
HrDocRenderer — HR 文档 CMS 安全渲染器。

后端已通过 nh3 sanitize；前端再用 DOMPurify 二次过滤防御深度。
唯一允许 v-html 的位置（已在 eslint.config.js 白名单声明）。

用法：
  <HrDocRenderer :html="doc.content_html" />
  <HrDocRenderer bare class="hb-alert-text" :html="block.text" />   <!-- 块内紧凑正文 -->
-->
<template>
  <!-- eslint-disable-next-line vue/no-v-html -->
  <article :class="bare ? null : 'hr-doc-content'" v-html="safeHtml"></article>
</template>

<script setup>
import { computed } from 'vue'
import DOMPurify from 'dompurify'

const props = defineProps({
  html: { type: String, default: '' },
  // bare：给「块内紧凑正文」用（警示横幅正文、Bento 卡片说明、页签备注等）。
  // 不加 .hr-doc-content —— 那套是文档级排版（固定 15px 字号、段落外边距、h1/h2 字号），
  // 套进 12.5~13.5px 的紧凑卡片里会把卡片撑变形。bare 下字号行高由调用方容器 CSS 决定。
  bare: { type: Boolean, default: false },
})

// 与后端 services/html_sanitizer.py 的标签白名单保持一致；
// 后端是 source of truth，前端做防御性二次清洗。
const PURIFY_CONFIG = {
  ALLOWED_TAGS: [
    'a', 'abbr', 'article', 'aside', 'b', 'blockquote', 'br', 'code', 'div',
    'em', 'figcaption', 'figure', 'footer', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'header', 'hr', 'i', 'img', 'li', 'main', 'mark', 'nav', 'ol', 'p', 'pre',
    'section', 'small', 'span', 'strong', 'sub', 'summary', 'details', 'sup',
    'table', 'tbody', 'td', 'tfoot', 'th', 'thead', 'tr', 'u', 'ul',
  ],
  ALLOWED_ATTR: [
    'class', 'id', 'style', 'href', 'target', 'rel', 'title', 'src', 'alt', 'width',
    'height', 'loading', 'colspan', 'rowspan', 'align', 'valign', 'scope',
    'border', 'cellspacing', 'cellpadding', 'start', 'type', 'data-tab', 'data-key',
  ],
  ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto|tel):|[^a-z]|[a-z+.\-]+(?:[^a-z+.\-:]|$))/i,
}

const safeHtml = computed(() => {
  if (!props.html) return ''
  const clean = DOMPurify.sanitize(props.html, PURIFY_CONFIG)
  return clean
})
</script>

<style scoped>
/* 🔴 长串必须能断行（用户 2026-10-20 截图：图文段右侧一长串「1111…」不换行，
   把整页撑出水平滚动条、内容跑到屏幕外）。
   `overflow-wrap` 是**继承属性**，只写在根 <article> 上就能覆盖全文（含 bare 模式，
   bare 不带 .hr-doc-content）。用 `anywhere` 而不是 `break-word`：前者会一并
   参与 min-content 尺寸计算，grid/flex 子项（如图文段的文字列）才真的能收窄。 */
article {
  overflow-wrap: anywhere;
}
.hr-doc-content {
  color: var(--t);
  line-height: 1.75;
  font-size: 15px;
}
/* 让原有页面 CSS class（sop-* / hi-* / hr-* / .glass-panel 等）继续生效；
   这里只补默认排版，不覆盖 class 样式。 */
.hr-doc-content :deep(h1),
.hr-doc-content :deep(h2),
.hr-doc-content :deep(h3) {
  color: var(--t);
  font-weight: 600;
  margin: 1.6em 0 0.6em;
  line-height: 1.3;
}
.hr-doc-content :deep(h1) { font-size: 28px; font-weight: 300; }
.hr-doc-content :deep(h2) { font-size: 22px; }
.hr-doc-content :deep(h3) { font-size: 17px; }
.hr-doc-content :deep(p)   { margin: 0.6em 0; color: var(--t2); }
.hr-doc-content :deep(ul),
.hr-doc-content :deep(ol)  { margin: 0.6em 0; padding-left: 1.4em; color: var(--t2); }
.hr-doc-content :deep(li)  { margin: 0.3em 0; }
.hr-doc-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
  font-size: 14px;
  font-variant-numeric: tabular-nums;
}
/* 自适应宽度（表格菜单「自适应宽度」开关）：强制铺满正文，忽略内联宽度与固定列宽。
   用 !important 才能压过表格自身残留的 style="width:…" 与单元格 width。 */
.hr-doc-content :deep(table.hr-doc--auto-width) { width: 100% !important; table-layout: auto !important; }
.hr-doc-content :deep(table.hr-doc--auto-width col) { width: auto !important; }
.hr-doc-content :deep(table.hr-doc--auto-width th),
.hr-doc-content :deep(table.hr-doc--auto-width td) { width: auto !important; }
.hr-doc-content :deep(th),
.hr-doc-content :deep(td) {
  border: 1px solid var(--bd);
  padding: 8px 12px;
  text-align: left;
  vertical-align: top;
  line-height: 1.7;
}
.hr-doc-content :deep(th) { background: rgba(99,102,241,0.05); font-weight: 600; color: var(--t2); }
/* 斑马纹 + 悬停提亮（与编辑器一致；只作用于 tbody，表头不参与）
   .hr-doc--no-zebra = 表格菜单里「斑马纹」被显式关闭 */
.hr-doc-content :deep(table:not(.hr-doc--no-zebra) tbody tr:nth-child(even)) { background: rgba(99,102,241,0.03); }
.hr-doc-content :deep(table tbody tr:hover) { background: rgba(99,102,241,0.05); }
/* 首列做表头（左侧加粗）：由编辑器表格菜单写入 hr-doc--first-col 时生效。
   🔴 用 class 而非 data-* —— 后端 nh3 白名单会剥掉所有 data-*（见 RteTableMenu 注释）。 */
.hr-doc-content :deep(table.hr-doc--first-col tbody td:first-child) {
  font-weight: 600;
  color: var(--t2);
  background: rgba(99,102,241,0.04);
}
.hr-doc-content :deep(a) {
  color: var(--v);
  text-decoration: none;
  border-bottom: 1px solid rgba(99,102,241,0.25);
}
.hr-doc-content :deep(a:hover) { border-bottom-color: var(--v); }
.hr-doc-content :deep(strong) { color: var(--t); font-weight: 600; }
/* 🔴 同 HrBlocksRenderer：显式写在 strong 上的 color 会盖掉外层 Color 标记 span 的内联色，
   作者选的红色加粗在发布页会变黑。带内联 color 的 span 内让颜色回归继承。 */
.hr-doc-content :deep(span[style*="color:"] :is(strong, b)) { color: inherit; }
.hr-doc-content :deep(code) {
  font-family: var(--fm);
  background: rgba(99,102,241,0.08);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}
/* 块内引用（blockquote）：v1.18 加粗竖线、圆角与内距，和 quote 块对齐。
   竖线色/底色可被 BlockquoteWithColor 选的内联 style 覆盖。 */
.hr-doc-content :deep(blockquote) {
  border-left: 4px solid var(--hr-primary, var(--v, #6366f1));
  padding: 10px 16px;
  margin: 1em 0;
  border-radius: 0 10px 10px 0;
  background: rgba(var(--hr-primary-rgb, 99,102,241), 0.045);
  color: var(--t2);
}
</style>
