<!--
GlobalConfirmHost — 全局确认弹框宿主（在 App.vue 挂一次）

配合 composables/useConfirm.ts 的 confirmDialog() / promptDialog() 使用，
把原来散落各处的原生 confirm()/alert() 统一成站内样式。
-->
<template>
  <ConfirmDialog
    v-model:open="confirmState.open"
    :title="confirmState.title"
    :message="confirmState.message"
    :confirm-text="confirmState.confirmText"
    :cancel-text="confirmState.cancelText"
    :tone="confirmState.tone"
    :comment-label="confirmState.commentLabel"
    @confirm="onConfirm"
    @cancel="onCancel"
  />
</template>

<script setup>
import ConfirmDialog from './ConfirmDialog.vue'
import { confirmState, settleConfirm } from '../../composables/useConfirm'

function onConfirm({ comment } = {}) { settleConfirm(true, comment || '') }
function onCancel() { settleConfirm(false) }
</script>
