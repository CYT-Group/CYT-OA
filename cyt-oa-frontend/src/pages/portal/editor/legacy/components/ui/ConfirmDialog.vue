<!--
  ConfirmDialog — 模态确认对话框，替代浏览器原生 confirm()
  用法（在父组件用 v-model:open）：

    <button @click="confirmOpen = true">删除</button>
    <ConfirmDialog
      v-model:open="confirmOpen"
      title="确认删除？"
      message="删除后无法恢复。"
      confirm-text="删除"
      tone="danger"
      @confirm="doDelete"
    />
-->
<template>
  <Teleport to="body">
    <Transition name="cd-fade">
      <div v-if="open" class="portal-editor-floating cd-mask" @click.self="onMaskClick">
        <div class="cd-panel" role="dialog" :aria-label="title">
          <!-- v1.8：图标从 36px 大 emoji 换成内联 SVG —— 正式后台里 emoji 太随意，
               而且不同系统渲染差异大（Windows 的 ⚠️ 和 macOS 完全两个样） -->
          <div class="cd-icon" :class="`cd-icon-${tone}`">
            <svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="currentColor"
                 stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <template v-if="tone === 'danger'">
                <path d="M12 9v4" /><path d="M12 17h.01" />
                <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
              </template>
              <template v-else-if="tone === 'warn'">
                <circle cx="12" cy="12" r="9" /><path d="M12 8v4" /><path d="M12 16h.01" />
              </template>
              <template v-else>
                <circle cx="12" cy="12" r="9" /><path d="m8.5 12.5 2.5 2.5 4.5-5" />
              </template>
            </svg>
          </div>
          <h3 class="cd-title">{{ title }}</h3>
          <!-- v1.22：支持结构化正文（#body slot）。长文案拆成多段左对齐，比一整坨居中文字好读；
               短文案继续走 message 居中。 -->
          <div v-if="$slots.body" class="cd-body"><slot name="body" /></div>
          <p v-else-if="message" class="cd-msg">{{ message }}</p>
          <textarea
            v-if="commentLabel"
            v-model="comment"
            class="cd-comment"
            :placeholder="commentLabel"
          ></textarea>
          <div class="cd-actions">
            <button v-if="!hideCancel" class="cd-btn cancel" @click="cancel">{{ cancelText }}</button>
            <button
              class="cd-btn confirm"
              :class="`tone-${tone}`"
              :disabled="busy"
              @click="confirm"
            >
              {{ busy ? '处理中...' : confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '确认操作？' },
  message: { type: String, default: '' },
  confirmText: { type: String, default: '确认' },
  cancelText: { type: String, default: '取消' },
  tone: { type: String, default: 'primary' }, // primary | danger | warn
  commentLabel: { type: String, default: '' }, // 给值则显示 textarea
  busy: { type: Boolean, default: false },
  // 纯提示（只有「知道了」一个按钮）：给上传失败这类「只能确认、没得选」的场景用
  hideCancel: { type: Boolean, default: false },
})

const emit = defineEmits(['update:open', 'confirm', 'cancel'])

const comment = ref('')

watch(() => props.open, (v) => { if (!v) comment.value = '' })

function cancel() {
  emit('update:open', false)
  emit('cancel')
}
// 带输入框（commentLabel）时是表单弹窗：误点遮罩不关闭，只认按钮；纯确认框保留点外取消
function onMaskClick() {
  if (props.commentLabel) return
  cancel()
}
function confirm() {
  emit('confirm', { comment: comment.value })
  // 🔴 纯提示框（hideCancel，如「上传失败」「推送成功」）**自己关掉**。
  // 它没有任何后续异步动作，父级没什么可做的 —— 但父级一旦忘了写 @confirm，
  // 就会出现「点『知道了』没反应」这种最难查的 bug（已踩：推送结果框 + 上传失败框）。
  // 确认框（有取消按钮）**不**自动关：父级要用它显示 busy 状态，关的时机由父级决定。
  // 直接 emit 而不是复用 cancel() —— cancel() 会顺带发一个 cancel 事件，
  // 对「点了知道了」这种语义是错的（而且组件里根本没有 close() 这个函数，
  // 我第一版就是写成了 close()，被单测当场抓出来）。
  if (props.hideCancel) emit('update:open', false)
}
</script>

<style scoped>
.cd-mask {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(13,16,38,0.45); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center; padding: 16px;
}
.cd-panel {
  background: rgba(255,255,255,0.98); backdrop-filter: blur(20px);
  border-radius: 16px; max-width: 420px; width: 100%;
  padding: 28px 24px; text-align: center;
  box-shadow: 0 24px 60px rgba(13,16,38,0.18);
  border: 1px solid rgba(99,102,241,0.10);
}
.cd-icon { font-size: 36px; margin-bottom: 8px; }
.cd-title { font-size: 18px; font-weight: 700; color: var(--t, #0D1026); margin: 0 0 8px; }
.cd-msg { font-size: 13px; color: var(--t3, #5A5F7A); line-height: 1.65; margin: 0 0 16px; }
/* v1.22 结构化正文：容器左对齐 + 段间距，具体段落/提示块的样式由调用方给
   （slot 内容带父组件 scope id，本组件的 scoped 样式够不着） */
.cd-body {
  text-align: left;
  font-size: 13px;
  line-height: 1.7;
  color: var(--t3, #5A5F7A);
  margin: 0 0 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.cd-body :slotted(p) { margin: 0; }
.cd-comment {
  width: 100%; min-height: 80px; padding: 10px 12px; margin-bottom: 12px;
  border: 1px solid var(--bd, rgba(99,102,241,0.12)); border-radius: 9px;
  font-family: inherit; font-size: 13px; resize: vertical; box-sizing: border-box;
  background: rgba(255,255,255,0.5); color: var(--t);
}
.cd-comment:focus { outline: none; border-color: var(--v, #6366F1); background: #fff; }

.cd-actions { display: flex; gap: 10px; justify-content: center; }
.cd-btn {
  padding: 9px 22px; border-radius: 8px; border: none; cursor: pointer;
  font-size: 13px; font-weight: 600; font-family: inherit; transition: .15s;
  min-width: 88px;
}
.cd-btn.cancel { background: rgba(140,146,171,0.10); color: var(--t2); }
.cd-btn.cancel:hover { background: rgba(140,146,171,0.18); }
.cd-btn.confirm.tone-primary { background: var(--grad, linear-gradient(135deg,#6366F1,#A855F7,#EC4899)); color: #fff; }
.cd-btn.confirm.tone-danger { background: linear-gradient(135deg,#EF4444,#DC2626); color: #fff; }
.cd-btn.confirm.tone-warn   { background: linear-gradient(135deg,#F59E0B,#D97706); color: #fff; }
.cd-btn.confirm:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 18px rgba(0,0,0,0.10); }
.cd-btn.confirm:disabled { opacity: 0.6; cursor: wait; }

.cd-fade-enter-active, .cd-fade-leave-active { transition: opacity 0.18s; }
.cd-fade-enter-from, .cd-fade-leave-to { opacity: 0; }
.cd-fade-enter-active .cd-panel, .cd-fade-leave-active .cd-panel { transition: transform 0.2s; }
.cd-fade-enter-from .cd-panel { transform: translateY(8px) scale(0.97); }
.cd-fade-leave-to .cd-panel { transform: translateY(8px) scale(0.97); }

.cd-icon-danger { color: #DC2626; }
.cd-icon-warn { color: #D97706; }
.cd-icon-primary { color: var(--v, #6366F1); }
</style>
