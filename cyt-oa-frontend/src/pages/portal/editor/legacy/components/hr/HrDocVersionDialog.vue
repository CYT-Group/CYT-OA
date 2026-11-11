<!-- 发布新版本的最终确认。输入先留在弹窗内，取消不改侧栏目标号。 -->
<template>
  <BaseModal v-model:open="visible" :title="publishedBefore ? '确认发布新版本' : '确认首次发布'" size="md" persistent>
    <p class="vd-lead">草稿已保存，<b>确认后才会发布</b>。取消会保留草稿。</p>
    <p v-if="publishedBefore" class="vd-hint">
      {{ bodyChanged ? '正文有变化，将发布一个新版本。' : '你选择了新的目标版本号，将发布一个新版本。' }}
      需要签收的文档发布新版本后，员工需重新签收。
    </p>
    <div class="vd-flow">
      <div class="vd-current"><span>已发布版本</span><strong>{{ publishedBefore ? currentVersion : '尚未发布' }}</strong></div>
      <span class="vd-arrow" aria-hidden="true">→</span>
      <label class="vd-target" for="hr-publish-version"><span>本次发布版本</span>
        <input id="hr-publish-version" ref="versionEl" v-model="target" type="text" :disabled="busy" :aria-invalid="!!validationError" aria-describedby="hr-version-feedback" placeholder="如 v1.1" @keydown.enter.prevent="apply">
      </label>
    </div>
    <p id="hr-version-feedback" class="vd-feedback" :class="{ error: !!validationError }" aria-live="polite">{{ validationError || '可修改为自定义版本号，最多 16 个字符。' }}</p>
    <p v-if="error" class="vd-server-error" role="alert">{{ error }}</p>
    <template #foot>
      <button type="button" class="btn" :disabled="busy" @click="visible = false">取消发布</button>
      <button type="button" class="btn btn-primary" :disabled="busy || !!validationError" @click="apply">{{ busy ? '发布中…' : '确认发布' }}</button>
    </template>
  </BaseModal>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import BaseModal from '../ui/BaseModal.vue'
import { publishVersionError } from '../../utils/hr-version'

const props = defineProps({
  open: { type: Boolean, default: false },
  currentVersion: { type: String, default: '' },
  initialVersion: { type: String, default: '' },
  publishedBefore: { type: Boolean, default: false },
  bodyChanged: { type: Boolean, default: false },
  usedVersions: { type: Array, default: () => [] },
  busy: { type: Boolean, default: false },
  error: { type: String, default: '' },
})
const emit = defineEmits(['update:open', 'confirm'])
const target = ref('')
const versionEl = ref(null)
const visible = computed({ get: () => props.open, set: value => { if (!props.busy) emit('update:open', value) } })
const validationError = computed(() => publishVersionError(target.value, {
  publishedBefore: props.publishedBefore,
  currentVersion: props.currentVersion,
  usedVersions: props.usedVersions,
}))
watch(() => props.open, async open => {
  if (!open) return
  target.value = props.initialVersion
  await nextTick()
  versionEl.value?.focus({ preventScroll: true })
  versionEl.value?.select()
}, { immediate: true })
function apply() {
  if (props.busy || validationError.value) return
  emit('confirm', target.value.trim())
}
</script>

<style scoped>
.vd-lead { margin: 0 0 8px; color: var(--t2); font-size: 14px; line-height: 1.7; }
.vd-hint { margin: 0 0 18px; color: var(--t3); font-size: 12px; line-height: 1.7; }
.vd-flow { display: grid; grid-template-columns: minmax(0,1fr) auto minmax(0,1fr); align-items: center; gap: 12px; margin-top: 18px; }
.vd-current, .vd-target { min-width: 0; min-height: 82px; box-sizing: border-box; display: flex; flex-direction: column; justify-content: center; gap: 8px; padding: 12px; border: 1px solid var(--bd); border-radius: 10px; background: #F5F6FB; }
.vd-current > span, .vd-target > span { font-size: 11px; color: var(--t3); }
.vd-current strong { font-family: var(--fm); font-size: 16px; overflow-wrap: anywhere; }
.vd-target { background: #F5F3FF; border-color: rgba(99,102,241,.3); }
.vd-target input { min-width: 0; width: 100%; box-sizing: border-box; padding: 7px 9px; border: 1px solid var(--bd); border-radius: 6px; background: white; color: var(--t); font-size: 14px; font-family: var(--fm); }
.vd-target input:focus { outline: 2px solid var(--v); outline-offset: 1px; }
.vd-arrow { color: var(--t4); }
.vd-feedback { margin: 10px 0 0; font-size: 12px; line-height: 1.6; color: var(--t3); }
.vd-feedback.error, .vd-server-error { color: #B91C1C; }
.vd-server-error { font-size: 13px; line-height: 1.7; margin: 12px 0 0; }
@media (max-width: 420px) { .vd-flow { gap: 7px; } .vd-current, .vd-target { padding: 9px; } }
</style>
