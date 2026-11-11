<!--
HrDocSideRail — 侧栏工具列：「下载 PDF」按钮 + 签收状态卡（v1.19d 抽成共用）。

用户拍板（2026-10-16）：下载 PDF 放**目录下面**——阅读页侧栏如此，两处预览也同样
（v1.19f 起预览不再用「正文下方横向小条」的简化摆法，而是和阅读页一样的侧栏列竖排）。

- 阅读页（HrDocPage，preview=false）：真实——PDF 走打印、签收卡「前往签收」滚动锚点。
- 预览页（编辑器内联预览 / 独立预览页，preview=true）：**PDF 同样真实可用**（走打印；用户拍板
  「从列表点预览也要能下载 PDF」）；只有**签收状态卡**保持惰性（点击不跳转；签收逻辑暂不做）。
- 目录 HrDocOutlineNav **不进本组件**：目录的摆位/两栏判定各处不同，由父级把本组件排在目录后面。
- 布局：竖向列（跟父级侧栏列 flex-direction:column 排）；PDF 按钮窄列下自动换行。
-->
<template>
  <div class="hr-side-rail">
    <button
      v-if="pdf" type="button"
      class="hr-doc-pdf"
      title="下载 PDF（浏览器打印 / 另存为 PDF）"
      @click="$emit('download')"
    >
      <span class="pdf-icon" aria-hidden="true">📥</span>
      <span class="pdf-label">下载 PDF</span>
      <span class="pdf-hint">浏览器打印</span>
    </button>

    <div v-if="signoff && !loading" class="hr-doc-signstate" :class="{ signed }">
      <template v-if="signed">
        <div class="ss-icon">✅</div>
        <div class="ss-title">已签收 {{ version }}</div>
        <div class="ss-time">{{ formatTime(signedAt) }}</div>
      </template>
      <template v-else>
        <div class="ss-icon">⚠️</div>
        <div class="ss-title">尚未签收</div>
        <a v-if="!preview" class="ss-link" href="#hr-doc-signoff" @click.prevent="$emit('goto-signoff')">前往签收 →</a>
        <span v-else class="ss-link ss-preview">预览态 · 不跳转</span>
      </template>
    </div>
  </div>
</template>

<script setup>
defineProps({
  pdf: { type: Boolean, default: false },        // 阅读页是否显示「下载 PDF」
  signoff: { type: Boolean, default: false },     // 是否需要签收（显示状态卡）
  signed: { type: Boolean, default: false },
  signedAt: { type: String, default: '' },
  version: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  preview: { type: Boolean, default: false },     // 预览态：签收状态卡惰性（PDF 仍真实可用）
})
defineEmits(['download', 'goto-signoff'])

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return String(iso)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
</script>

<style scoped>
.hr-side-rail { display: flex; flex-direction: column; gap: 16px; }

/* 下载 PDF：走浏览器打印（对话框里可另存为 PDF）；窄侧栏列（编辑器预览 150px）下自动换行 */
.hr-doc-pdf {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 8px;
  padding: 12px 14px;
  border: 1px solid rgba(var(--hr-primary-rgb, 99, 102, 241), 0.24);
  border-radius: 10px;
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.08);
  color: var(--hr-primary, var(--v));
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s, transform .15s;
}
.hr-doc-pdf:hover { background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.16); transform: translateY(-1px); }
.pdf-icon { font-size: 14px; }
.pdf-hint { margin-left: auto; font-size: 10px; opacity: .6; }

/* 侧栏签收状态卡：未签收给一条「前往签收」的捷径，已签收显示留痕时间 */
.hr-doc-signstate {
  padding: 18px 16px;
  border: 1px solid rgba(245, 158, 11, 0.35);
  border-radius: 12px;
  background: rgba(245, 158, 11, 0.08);
  text-align: center;
}
.hr-doc-signstate.signed {
  border-color: rgba(16, 185, 129, 0.35);
  background: rgba(16, 185, 129, 0.08);
}
.ss-icon { font-size: 22px; }
.ss-title { margin-top: 6px; font-size: 13.5px; font-weight: 700; color: var(--t); }
.ss-time { margin-top: 2px; font-size: 11.5px; color: var(--t4); font-family: var(--fm); }
.ss-link {
  display: inline-block;
  margin-top: 8px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--hr-primary, var(--v));
  text-decoration: none;
}
.ss-link:hover { text-decoration: underline; }
.ss-preview { cursor: default; opacity: .6; }
.ss-preview:hover { text-decoration: none; }

/* 打印：工具列不进 PDF（阅读页还会把整条侧栏 display:none，这里再兜一层） */
@media print {
  .hr-side-rail { display: none !important; }
}
</style>
