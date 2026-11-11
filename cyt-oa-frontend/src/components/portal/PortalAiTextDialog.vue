<template>
  <Dialog :open="open" :title="mode === 'layout' ? 'AI 智能排版' : 'AI 文字润色'" :description="description" size="xl" @update:open="setOpen">
    <div class="grid items-start gap-5 lg:grid-cols-2"><div><label for="portal-ai-input" class="field-label">待处理文字</label><Textarea id="portal-ai-input" v-model="input" :rows="14" maxlength="100000" :disabled="busy" /><div class="mt-3 flex flex-wrap gap-2"><Button :loading="busy" :disabled="!input.trim()" @click="generate">{{ result ? '重新生成' : '生成预览' }}</Button><Button v-if="busy" variant="outline" @click="cancel">取消生成</Button></div><Callout v-if="error" tone="destructive" class="mt-3">{{ error }}</Callout></div><section class="min-w-0 rounded-lg border p-4"><h3 class="mb-4 text-sm font-semibold">生成结果预览</h3><p v-if="busy" class="py-6 text-sm text-muted-foreground">正在生成，可以取消并继续编辑。</p><HrBlockPreview v-else-if="Array.isArray(result)" :blocks="result" /><HrHtmlContent v-else-if="typeof result === 'string'" :html="asHtml(result)" /><p v-else class="py-6 text-sm text-muted-foreground">生成后在这里检查结果；采用后仅更新编辑器内容。</p></section></div>
    <template #footer><Button variant="outline" @click="setOpen(false)">取消</Button><Button :disabled="!result || busy" @click="apply">{{ mode === 'layout' ? '采用排版' : '采用润色' }}</Button></template>
  </Dialog>
</template>
<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import Dialog from '../../ui/Dialog.vue'
import Button from '../../ui/Button.vue'
import Textarea from '../../ui/Textarea.vue'
import Callout from '../../ui/Callout.vue'
import HrHtmlContent from './HrHtmlContent.vue'
import HrBlockPreview from './HrBlockPreview.vue'
import { http } from '../../lib/api'
import { errText } from '../../lib/toast'
import type { HrBlock } from '../../lib/portal-hr'
const props = defineProps<{ open: boolean; mode: 'polish' | 'layout'; text: string; kind?: string; description?: string }>()
const emit = defineEmits<{ 'update:open': [value: boolean]; apply: [result: string | HrBlock[]] }>()
const input = ref(''), busy = ref(false), error = ref(''), result = ref<string | HrBlock[] | null>(null)
let controller: AbortController | undefined, ticket = 0
function cancel() { ticket++; controller?.abort(); controller = undefined; busy.value = false }
function setOpen(value: boolean) { if (!value) cancel(); emit('update:open', value) }
watch(() => props.open, open => { if (open) { input.value = props.text; error.value = ''; result.value = null } else cancel() })
function asHtml(value: string) { if (/<\/?[a-z][\s\S]*>/i.test(value)) return value; return value.split(/\n\s*\n/).map(text => `<p>${text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br>')}</p>`).join('') }
async function generate() { if (busy.value || !input.value.trim()) return; const current = ++ticket; controller = new AbortController(); busy.value = true; error.value = ''; result.value = null; try { const response = await http.post(`/admin-api/portal/ai/${props.mode}`, { text: input.value, ...(props.mode === 'polish' ? { kind: props.kind || 'news' } : {}) }, { timeout: 150000, signal: controller.signal }) as unknown; if (current !== ticket) return; if (props.mode === 'layout') { const blocks = (response as { blocks?: unknown })?.blocks; if (!Array.isArray(blocks) || !blocks.every(block => block && typeof block.type === 'string')) throw new Error('AI 未返回有效内容块，请重新生成'); result.value = blocks } else { if (typeof response !== 'string' || !response.trim()) throw new Error('AI 未返回有效润色结果，请重新生成'); result.value = response } } catch (e) { if (current === ticket) error.value = e instanceof Error ? e.message : errText(e) } finally { if (current === ticket) { busy.value = false; controller = undefined } } }
function apply() { if (result.value && !busy.value) { emit('apply', result.value); setOpen(false) } }
onBeforeUnmount(cancel)
</script>
