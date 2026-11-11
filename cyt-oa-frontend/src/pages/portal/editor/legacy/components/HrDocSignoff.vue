<!--
HrDocSignoff — 员工签收卡（阅读页 + 编辑器实时预览两处共用）。

v1.19：签收卡是**页面级**元素（由「需要签收」开关控制、贴在正文末尾），编辑者在正文里看不到它，
于是在「设置 → 签收卡」分区也渲染同一张卡的**实时预览**（preview 态：交互惰性、只给观感），
保证「编辑所见 = 发布所见」——同 HrBlocksRenderer 四处共用的思路。

presentational：所有文案从 props 来（signoff 已是 mergeSignoff 补全后的对象），
签收动作由 @sign / @toggle-ack 冒给父级；阅读页（HrDocPage）传真实状态，编辑器传 preview。
-->
<template>
  <section :id="preview ? undefined : 'hr-doc-signoff'" class="hr-doc-signoff">
    <p v-if="preview" class="sg-preview-hint">签收卡预览（编辑时所见即发布所见）</p>
    <h3 class="sg-title">{{ signoff.icon }} {{ signoff.title }}</h3>

    <div v-if="loading" class="sg-loading">加载中…</div>

    <div v-else-if="signed" class="sg-signed">
      <div class="sg-signed-icon" aria-hidden="true">✅</div>
      <div class="sg-signed-title">您已签收《{{ title }}》{{ version }}</div>
      <div class="sg-signed-time">签收时间：{{ formatTime(signedAt) }}</div>
      <div class="sg-signed-note">{{ signoff.signed_note }}</div>
    </div>

    <div v-else class="sg-unsigned">
      <HrDocRenderer bare class="sg-lead" :html="leadHtml" />
      <label class="sg-ack">
        <input :checked="currentAcked" type="checkbox" @change="onAck">
        <span>{{ signoff.ack_label }}</span>
      </label>
      <button
        type="button"
        class="sg-btn"
        :disabled="!currentAcked || signing"
        @click="onSign"
      >{{ signing ? '提交中…' : signoff.button_text }}</button>
      <div v-if="error" class="sg-err">{{ error }}</div>
      <HrDocRenderer v-if="tipHtml" bare class="sg-tip" :html="tipHtml" />
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import HrDocRenderer from './HrDocRenderer.vue'
import { signoffLeadHtml, signoffTipHtml } from '../utils/hr-signoff'

const props = defineProps({
  signoff: { type: Object, required: true },   // mergeSignoff 补全后的完整配置
  title: { type: String, default: '' },
  version: { type: String, default: '' },
  chaptersText: { type: String, default: '全部内容' }, // 默认引导语里「全部 N 章」用
  signed: { type: Boolean, default: false },
  signedAt: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  signing: { type: Boolean, default: false },
  acked: { type: Boolean, default: false },
  error: { type: String, default: '' },
  preview: { type: Boolean, default: false },  // 编辑器内预览：交互惰性
})
const emit = defineEmits(['toggle-ack', 'sign'])

const leadHtml = computed(() => signoffLeadHtml(props.signoff, {
  title: props.title, version: props.version, chaptersText: props.chaptersText,
}))
const tipHtml = computed(() => signoffTipHtml(props.signoff))

// 预览态：勾选框用一个本地 ref，让用户能看「不勾禁用 → 勾上可点」的切换；阅读态完全受父级控制
const pvAcked = ref(false)
const currentAcked = computed(() => (props.preview ? pvAcked.value : props.acked))
function onAck(e) {
  if (props.preview) pvAcked.value = e.target.checked
  else emit('toggle-ack', e.target.checked)
}
function onSign() {
  if (props.preview) return   // 预览不真签
  emit('sign')
}

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return String(iso)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
</script>

<style scoped>
/* 观感对齐设计稿（企微截图）：**统一深靛蓝卡** + 白字 + **深墨绿按钮**。
   设计稿实测：卡背景 ≈ #0E1428→#1A2040 渐变、按钮纯色 ≈ #1F5F4C、卡内**无**浅黄子块。
   （旧版卡里套了一层 rgba(245,158,11,.1) 浅黄块 + 亮绿按钮 → 割裂显丑，已去掉。） */
.hr-doc-signoff {
  margin-top: 48px;
  padding: 30px 34px;
  border-radius: 16px;
  background: linear-gradient(150deg, #0e1428 0%, #1a2040 100%);
  color: #fff;
  box-shadow: 0 12px 36px rgba(14, 20, 40, 0.26);
}
.sg-title { margin: 0 0 16px; font-size: 16.5px; font-weight: 700; color: #fff; letter-spacing: .01em; }
.sg-preview-hint { margin: -6px 0 14px; font-size: 11.5px; letter-spacing: .04em; opacity: .55; }
.sg-loading { padding: 24px; text-align: center; font-size: 13px; opacity: .6; }

.sg-signed {
  padding: 22px 24px;
  border-radius: 12px;
  background: rgba(16, 185, 129, 0.14);
  text-align: center;
}
.sg-signed-icon { font-size: 34px; }
.sg-signed-title { margin-top: 8px; font-size: 17px; font-weight: 700; }
.sg-signed-time { margin-top: 4px; font-size: 13px; opacity: .85; }
.sg-signed-note { margin-top: 10px; font-size: 12px; line-height: 1.6; opacity: .7; }

/* 未签收：内容直接铺在深色卡上（对齐设计稿，不再套浅黄子块） */
.sg-unsigned { padding: 0; }
.sg-lead { font-size: 14px; line-height: 1.85; color: rgba(255, 255, 255, 0.92); margin-bottom: 16px; }
.sg-lead :deep(p) { margin: 0; }
.sg-lead :deep(p + p) { margin-top: 0.5em; }
.sg-ack {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 16px; font-size: 13.5px; color: rgba(255, 255, 255, 0.92);
  cursor: pointer; user-select: none;
}
.sg-ack input { width: 16px; height: 16px; cursor: pointer; accent-color: #10b981; }
.sg-btn {
  width: 100%;
  padding: 15px;
  border: 0;
  border-radius: 12px;
  background: linear-gradient(180deg, #24684f, #1b5142);  /* 深墨绿，对齐设计稿 #1F5F4C */
  color: #fff;
  font: inherit;
  font-size: 14.5px;
  font-weight: 700;
  letter-spacing: .02em;
  cursor: pointer;
  transition: filter .2s, opacity .2s;
}
.sg-btn:hover:not(:disabled) { filter: brightness(1.08); }
.sg-btn:disabled { opacity: .4; cursor: not-allowed; }
.sg-err { margin-top: 10px; font-size: 12px; color: #fca5a5; text-align: center; }
.sg-tip {
  margin: 16px 0 0;
  padding: 11px 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.07);
  font-size: 12px;
  line-height: 1.65;
  color: rgba(255, 255, 255, 0.8);
}
.sg-tip :deep(p) { margin: 0; }

@media (max-width: 768px) {
  .hr-doc-signoff { padding: 22px 20px; }
}

/* 打印 / 导出 PDF：签收卡转成「纸质签字栏」—— 去深底/按钮，留虚线框说明签收状态 */
@media print {
  .hr-doc-signoff {
    margin-top: 24px;
    padding: 16px;
    border: 1.5pt dashed #666;
    border-radius: 6px;
    background: none !important;
    color: #000 !important;
  }
  .hr-doc-signoff :deep(*) { color: #000 !important; }
  .sg-title { margin-bottom: 8px; }
  .sg-btn,
  .sg-ack,
  .sg-err { display: none !important; }
  .sg-unsigned,
  .sg-signed { border: 0 !important; background: none !important; padding: 0 !important; text-align: left; }
  .sg-signed-icon { display: none; }
}
</style>
