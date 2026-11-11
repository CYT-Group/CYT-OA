<!--
HrDocCover — 整屏封面（对齐参考「制度编辑器」2026-10-12 更新后的 pc-fullcover）。

结构（自上而下）：背景三态 → 主题色光晕 → 中英标识 → 大标题 →
图标 + 封面说明 → Meta 三栏（执行日期/适用范围/发布部门）→ 下滑提示。

三处共用：员工阅读页 / 管理员预览页 / 编辑器分栏预览。
`compact` 给编辑器窄栏用（降低 min-height，字号收一档）。图片按原始比例完整显示，
文字与图片共用网格单元；窄栏文字较多时由内容撑高，图片仍保持完整。

⚠️ 标题**整串纯白**，不做尾词高亮（2026-10-13 用户拍板）。
参考稿原本会把结尾关键词（中文末 2 字 / 英文末词）用主题色标出来，但主题色与
`gradient` 档的背景**同源**（背景就是主题色渐变）→ 标了要么隐形、要么像「印花了」，
在默认档上得不偿失。切分函数 `splitTitle` 已一并移除，别再加回来。

🔴 模板一律读 `c`（normalizeCover 后的完整对象），**不要直读 `cover` prop**（v1.9.4）。
prop 是调用方传什么就是什么：`form.cover` 可能来自后端旧数据 `{}`，也可能整块缺失。
归一后所有字段同源，缺键自动补默认值。
-->
<template>
  <div class="hr-cover" :class="{ compact, 'has-image': hasImage }" :style="bgStyle">
    <img v-if="hasImage" ref="imageEl" :key="imageSource" class="fc-image" :src="imageSource" alt="" @error="onImageError" />
    <div v-else class="fc-glow" :style="{ background: primary }"></div>
    <div class="hr-cover-inner">
      <div v-if="c.label_en || c.label_cn" class="fc-labels">
        <span v-if="c.label_en" class="fc-label-en">{{ c.label_en }}</span>
        <span v-if="c.label_cn" class="fc-label-cn">{{ c.label_cn }}</span>
      </div>
      <h1 class="fc-title">{{ titleText }}</h1>
      <div v-if="c.icon || c.desc" class="fc-icon-row">
        <div v-if="c.icon" class="fc-iconbox">{{ c.icon }}</div>
        <p v-if="c.desc" class="fc-desc">{{ c.desc }}</p>
      </div>
      <div v-if="metas.length" class="fc-meta">
        <div v-for="m in metas" :key="m.label" class="fm-item">
          <span class="fm-label">{{ m.label }}</span>
          <span class="fm-value">{{ m.value }}</span>
        </div>
      </div>
    </div>
    <div class="fc-scroll-hint">下滑查看正文<span class="fc-arrow">↓</span></div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { resolveTheme, themeCssVars } from '../data/hr-themes'
import { COVER_DARK_BG, coverBackground, normalizeCover } from '../data/hr-cover'

const props = defineProps({
  title: { type: String, default: '' },
  // 后端返回的封面配置对象；缺键自动补默认（后端已 normalize，这里再兜一层）
  cover: { type: Object, default: null },
  // 文档主题：gradient 档的背景与光晕都取它
  theme: { type: String, default: '' },
  // 编辑器分栏预览用：降 min-height、收字号
  compact: { type: Boolean, default: false },
})

const c = computed(() => normalizeCover(props.cover))
const themeResolved = computed(() => resolveTheme(props.theme))
const primary = computed(() => themeResolved.value.primary)
const imageSource = computed(() => c.value.bg_type === 'image' ? c.value.image : '')
const imageEl = ref(null)
const imageFailed = ref(false)
const hasImage = computed(() => !!imageSource.value && !imageFailed.value)
watch(imageSource, () => { imageFailed.value = false })
function onImageError(event) {
  // 换图后旧请求仍可能回错，只允许当前图片触发回落。
  if (event.currentTarget === imageEl.value) imageFailed.value = true
}

// 主题变量直接挂封面根节点：光晕（以及块内品牌色）要跟着主题走。
// 挂在组件内部而不是靠父级传 style —— 封面在阅读页里位于 .hr-doc-main 之外，
// 拿不到那边的变量，自己算一份最稳。
const bgStyle = computed(() => ({
  // 图片由真实 img 决定比例；背景只负责加载失败或文字撑高后的留白。
  background: c.value.bg_type === 'image' ? COVER_DARK_BG : coverBackground(c.value, themeResolved.value.gradient),
  ...themeCssVars(props.theme),
}))

const titleText = computed(() => props.title || '（未填标题）')

// Meta 三栏：只显示填了的项（参考同此）
const metas = computed(() => [
  { label: '执行日期', value: c.value.date },
  { label: '适用范围', value: c.value.scope },
  { label: '发布部门', value: c.value.dept },
].filter(m => m.value))
</script>

<style scoped>
.hr-cover {
  /* 页级定位交给调用方（阅读页 .hr-doc-cover 才需要让开固定导航；
     预览页顶栏已经让过了，这里再来一次会多出一段空隙） */
  width: 100%;
  box-sizing: border-box;
  min-height: 420px;
  padding: 56px 32px 64px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  z-index: 1;
}
.hr-cover.compact { min-height: 300px; padding: 40px 24px 52px; }

/* 图片和文字同格叠放，行高取二者所需高度的较大者，不能用高度上限换取裁切。 */
.hr-cover.has-image {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  min-height: 0;
  padding: 0;
  overflow: visible;
  isolation: isolate;
}
.fc-image {
  grid-area: 1 / 1;
  align-self: center;
  display: block;
  width: 100%;
  height: auto;
  min-width: 0;
}
.hr-cover.has-image .hr-cover-inner {
  grid-area: 1 / 1;
  place-self: center;
  width: min(100%, 624px);
  max-width: none;
  min-width: 0;
  box-sizing: border-box;
  padding: 56px 32px 64px;
  overflow-wrap: anywhere;
  /* 浅色照片上也要读得清白字，不靠遮暗或裁切原图。 */
  text-shadow: 0 1px 3px rgba(15, 23, 42, 0.72), 0 0 10px rgba(15, 23, 42, 0.28);
}
.hr-cover.has-image.compact .hr-cover-inner { padding: 40px 24px 52px; }
.hr-cover.has-image .fc-icon-row { min-width: 0; max-width: 100%; }
.hr-cover.has-image .fc-desc { min-width: 0; }
.hr-cover.has-image .fm-item { min-width: 0; max-width: 100%; }
.hr-cover.has-image .fc-meta { max-width: 100%; }
.hr-cover.has-image .fc-scroll-hint { text-shadow: 0 1px 3px rgba(15, 23, 42, 0.72); }

/* 主题色光晕（参考的 fc-glow）：圆形模糊，居中偏上 */
.fc-glow {
  position: absolute;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.25;
  top: 30%;
  left: 50%;
  transform: translateX(-50%);
  pointer-events: none;
}

.hr-cover-inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 10px;
  max-width: 560px;
}
.hr-cover.compact .hr-cover-inner { max-width: 100%; }

.fc-labels { display: flex; flex-direction: column; align-items: center; gap: 2px; margin-bottom: 2px; }
.fc-label-en {
  font-family: var(--fm);
  font-size: 11px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.45);
}
.fc-label-cn { font-size: 12.5px; font-weight: 500; color: rgba(255, 255, 255, 0.6); }

.fc-title {
  font-size: 30px;
  font-weight: 800;
  line-height: 1.3;
  letter-spacing: 0.02em;
  color: #fff;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.22);
}
.hr-cover.compact .fc-title { font-size: 22px; }

.fc-icon-row { display: flex; align-items: center; gap: 12px; margin-top: 4px; text-align: left; }
.fc-iconbox {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.14);
}
.fc-desc {
  font-size: 13.5px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.72);
  max-width: 380px;
}
.hr-cover.compact .fc-desc { max-width: 100%; font-size: 12.5px; }

.fc-meta { display: flex; gap: 26px; flex-wrap: wrap; justify-content: center; margin-top: 8px; }
.fm-item { display: flex; flex-direction: column; align-items: center; gap: 3px; }
.fm-label {
  font-family: var(--fm);
  font-size: 10.5px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.42);
}
.fm-value { font-size: 12.5px; color: rgba(255, 255, 255, 0.85); }

.fc-scroll-hint {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 18px;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.5);
}
.fc-arrow { font-size: 14px; line-height: 1; }
@media (prefers-reduced-motion: no-preference) {
  .fc-arrow { animation: fc-bounce 1.8s ease-in-out infinite; }
  @keyframes fc-bounce {
    0%, 100% { transform: translateY(0); opacity: 0.55; }
    50% { transform: translateY(4px); opacity: 1; }
  }
}

@media (max-width: 768px) {
  .hr-cover { min-height: 340px; padding: 40px 20px 52px; }
  .hr-cover.has-image .hr-cover-inner { padding: 40px 20px 52px; }
  .fc-title { font-size: 23px; }
  .fc-desc { font-size: 12.5px; }
  .fc-meta { gap: 18px; }
}

/* 打印 / 导出 PDF：封面是给屏幕的（「下滑查看正文」+ 弹跳箭头都是屏幕语义），打印时去掉 */
@media print {
  .fc-scroll-hint { display: none !important; }
}
</style>
