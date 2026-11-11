<!--
ToastNotify — 全局轻提示（单例，挂在 App.vue 上）。

v1.3.1 起错误提示可读可复制：
- 错误文案常是后端 422 的字段路径（如 `blocks[1].type 未知块类型: 'compare'`），
  用户要贴给别人排查 —— 所以错误提示**文字可选中**、带「复制」按钮、
  停留 8 秒且鼠标移上去暂停倒计时（成功提示维持 3 秒一闪而过）
- 长文案自动换行，不再 `nowrap` 撑出屏幕
- 容器保持 pointer-events:none（不挡底下页面的点击），只让文字和按钮可交互
-->
<template>
  <Teleport to="body">
    <div
      class="portal-editor-floating toast"
      :class="{ on: visible, err: type === 'err' }"
      @mouseenter="pause()"
      @mouseleave="resume()"
    >
      <span class="toast-tx">{{ message }}</span>
      <button
        v-if="type === 'err'"
        type="button"
        class="toast-copy"
        :class="{ fail: copyState === 'fail' }"
        @click="copy"
      >
        {{ COPY_LABEL[copyState] }}
      </button>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useToast } from '../composables/useToast'

const { message, visible, type, pause, resume } = useToast()

// 复制反馈必须诚实：两条路都失败时明确说失败，别让用户以为复制好了
const COPY_LABEL = { idle: '复制', ok: '已复制', fail: '复制失败' }
const copyState = ref('idle')
watch(message, () => { copyState.value = 'idle' })

function legacyCopy(text) {
  const ta = document.createElement('textarea')
  ta.value = text
  ta.setAttribute('readonly', '')
  ta.style.cssText = 'position:fixed;top:0;left:0;opacity:0'
  document.body.appendChild(ta)
  ta.select()
  let ok = false
  try { ok = document.execCommand('copy') } catch { ok = false }
  document.body.removeChild(ta)
  return ok
}

async function copy() {
  const text = message.value
  let ok = false
  try {
    // 剪贴板 API 在非安全上下文 / 无权限时会抛，退回 execCommand
    await navigator.clipboard.writeText(text)
    ok = true
  } catch {
    ok = legacyCopy(text)
  }
  copyState.value = ok ? 'ok' : 'fail'
  setTimeout(() => { copyState.value = 'idle' }, 1800)
}
</script>

<style scoped>
.toast {
  position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%) translateY(20px);
  display: flex; align-items: center; gap: 12px;
  max-width: min(760px, calc(100vw - 48px));
  background: rgba(8,12,28,.95); color: #fff; padding: 10px 22px; border-radius: 12px;
  /* 弹窗内的操作也需要立即看到结果，提示必须显示在通用弹窗遮罩之上。 */
  font-size: 13px; line-height: 1.6; opacity: 0; transition: all .3s; z-index: 2000;
  pointer-events: none; white-space: normal;
}
.toast.on { opacity: 1; transform: translateX(-50%) translateY(0); }

/* 错误：换行显示 + 文字可选中 + 复制按钮 */
.toast.err { background: #7F1D1D; padding-right: 12px; white-space: normal; }
.toast-tx { min-width: 0; }
.toast.err .toast-tx {
  pointer-events: auto; cursor: text; user-select: text;
  overflow-wrap: anywhere; word-break: break-word;
}
.toast-copy {
  flex-shrink: 0; pointer-events: auto;
  padding: 3px 10px; border-radius: 7px; border: 1px solid rgba(255,255,255,.38);
  background: rgba(255,255,255,.12); color: #fff;
  font-family: inherit; font-size: 11.5px; font-weight: 600;
  cursor: pointer; white-space: nowrap;
}
.toast-copy:hover { background: rgba(255,255,255,.24); }
.toast-copy.fail { border-color: rgba(255,255,255,.3); background: transparent; opacity: .8; }
</style>
