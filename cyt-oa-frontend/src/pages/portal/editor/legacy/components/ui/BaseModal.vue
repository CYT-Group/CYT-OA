<!--
  BaseModal — 后台通用弹框底座（v1.8）

  为什么要它：之前同一个后台里躺着两套弹框 —— `ui/ConfirmDialog.vue`（居中卡片 +
  36px 大 emoji 图标 + 双按钮）和列表页自己写的 `.modal-mask`（版本历史弹窗），
  遮罩浓度、圆角、内边距、按钮各不一样，观感散。这里定一套底座，新的弹框都基于它。

  约定（与 ConfirmDialog 保持同一套视觉 token，两个弹框并排不会打架）：
    - 遮罩 rgba(13,16,38,.45) + 背景模糊
    - 面板圆角 16、内边距 24、最大宽按 size 走
    - 标题栏右上角统一「✕」关闭；底部按钮区用 global.css 的 .btn 家族

  行为：
    - Esc 关闭（除非 persistent）
    - 点遮罩关闭（除非 persistent）—— persistent 用于「表单填到一半别误关」的场景
    - 打开时锁 body 滚动（避免背景跟着滚）

  用法：
    <BaseModal v-model:open="show" title="推送企微" size="md">
      <p>正文…</p>
      <template #foot>
        <button class="btn" @click="show = false">取消</button>
        <button class="btn btn-primary" @click="ok">确定</button>
      </template>
    </BaseModal>
-->
<template>
  <Teleport to="body">
    <Transition name="bm-fade">
      <div v-if="open" class="portal-editor-floating bm-mask" @click.self="onMaskClick">
        <div class="bm-panel" :class="`bm-${size}`" role="dialog" :aria-label="title">
          <header class="bm-head">
            <h3 class="bm-title">{{ title }}</h3>
            <button class="bm-x" type="button" aria-label="关闭" @click="close">✕</button>
          </header>
          <div class="bm-body"><slot /></div>
          <footer v-if="$slots.foot" class="bm-foot"><slot name="foot" /></footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  /** sm 380 / md 520 / lg 720 —— 只影响最大宽 */
  size: { type: String, default: 'md' },
  /** 点遮罩 / 按 Esc 都不关（表单类弹框防误关）；右上角 ✕ 仍然可关 */
  persistent: { type: Boolean, default: false },
})
const emit = defineEmits(['update:open', 'close'])

function close() {
  emit('update:open', false)
  emit('close')
}

function onMaskClick() {
  if (!props.persistent) close()
}

function onKey(e) {
  if (e.key === 'Escape' && !props.persistent) close()
}

// 打开时锁 body 滚动 + 挂 Esc；关闭时全部还原（切页由 onBeforeUnmount 兜底）
watch(() => props.open, (v) => {
  if (v) {
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', onKey)
  } else {
    document.body.style.overflow = ''
    window.removeEventListener('keydown', onKey)
  }
}, { immediate: true })

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  window.removeEventListener('keydown', onKey)
})
</script>

<style scoped>
.bm-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(13, 16, 38, 0.45);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.bm-panel {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(99, 102, 241, 0.1);
  box-shadow: 0 24px 60px rgba(13, 16, 38, 0.18);
  width: 100%;
  max-height: calc(100vh - 40px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.bm-sm { max-width: 380px; }
.bm-md { max-width: 520px; }
.bm-lg { max-width: 720px; }

.bm-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 24px 0;
}
.bm-title { margin: 0; font-size: 16px; font-weight: 700; color: var(--t, #0d1026); }
.bm-x {
  flex: none;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--t4, #8c92ab);
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.15s;
}
.bm-x:hover { background: rgba(99, 102, 241, 0.08); color: var(--v, #6366f1); }

.bm-body { padding: 14px 24px 4px; overflow-y: auto; }

.bm-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 24px 20px;
  border-top: 1px solid rgba(99, 102, 241, 0.1);
  margin-top: 14px;
}

.bm-fade-enter-active,
.bm-fade-leave-active { transition: opacity 0.18s ease; }
.bm-fade-enter-from,
.bm-fade-leave-to { opacity: 0; }
.bm-fade-enter-active .bm-panel { transition: transform 0.18s ease; }
.bm-fade-enter-from .bm-panel { transform: translateY(8px) scale(0.98); }
</style>
