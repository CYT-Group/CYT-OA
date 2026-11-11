<template>
  <div class="space-y-2"><div class="flex items-center gap-2"><Input :id="id" :model-value="modelValue" :aria-label="label" placeholder="https://… 或站内图片地址" @update:model-value="emit('update:modelValue', String($event))" /><label class="shrink-0"><span class="inline-flex h-9 cursor-pointer items-center rounded-lg border px-3 text-xs hover:bg-muted" :class="busy ? 'pointer-events-none opacity-50' : ''">{{ busy ? '上传中…' : '上传图片' }}</span><input ref="picker" type="file" accept="image/jpeg,image/png,image/webp,image/gif" class="sr-only" :aria-label="`上传${label}`" :disabled="busy" @change="upload" /></label></div><p v-if="error" class="text-xs text-destructive">{{ error }}</p><img v-if="hrSafeUrl(modelValue)" :src="hrSafeUrl(modelValue)" :alt="`${label}预览`" class="max-h-40 max-w-full rounded-md border object-contain" /></div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import Input from '../../ui/Input.vue'
import { api } from '../../lib/api'
import { hrSafeUrl } from '../../lib/portal-hr'
import { errText } from '../../lib/toast'
const props = withDefaults(defineProps<{ modelValue?: string; label?: string; id?: string; folder?: string }>(), { modelValue: '', label: '图片', folder: 'news' })
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const picker = ref<HTMLInputElement>(), busy = ref(false), error = ref('')
async function upload() { const file = picker.value?.files?.[0]; if (!file) return; error.value = ''; if (!['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type) || file.size > 10 * 1024 * 1024) { error.value = '请选择 10MB 以内的 JPG、PNG、WebP 或 GIF 图片'; if (picker.value) picker.value.value = ''; return } busy.value = true; try { const data = new FormData(); data.append('file', file); data.append('folder', props.folder); const result = await api.post<{ url: string }>('/admin-api/portal/media/upload', data, 120000); if (!hrSafeUrl(result.url)) throw new Error('上传服务未返回有效图片地址'); emit('update:modelValue', result.url) } catch (e) { error.value = e instanceof Error ? e.message : errText(e) } finally { busy.value = false; if (picker.value) picker.value.value = '' } }
</script>
