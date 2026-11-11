<template>
  <Dialog :open="open" title="企业微信发布通知" :description="`${title} · 发送已发布的内容链接。`" size="lg" @update:open="emit('update:open', $event)">
    <div class="space-y-4"><div><label for="content-push-audience" class="field-label">发送范围</label><Select id="content-push-audience" v-model="audience" :disabled="busy" :options="audienceOptions" /></div><div v-if="audience === 'scope'"><label for="content-push-depts" class="field-label">部门名称或部门 ID（每行一个）</label><Textarea id="content-push-depts" v-model="departments" :disabled="busy" :rows="3" placeholder="行政人事部" /><p class="mt-1.5 text-xs text-muted-foreground">请至少填写一个部门；全员发送请在上方明确选择“全员”。</p></div><div v-if="audience === 'users'"><label for="content-push-users" class="field-label">员工账号（每行一个，最多 200 人）</label><Textarea id="content-push-users" v-model="usernames" :disabled="busy" :rows="3" /></div><div v-if="kind === 'hrdoc'"><label for="content-push-message" class="field-label">附加通知文案（可空）</label><Textarea id="content-push-message" v-model="customMessage" :disabled="busy" :rows="2" maxlength="300" /></div><div class="flex items-center justify-between gap-3 rounded-lg border p-3"><label for="content-push-force" class="text-xs leading-relaxed">24 小时内重复发送<br /><span class="text-muted-foreground">开启后还需再次确认，防止员工重复收到通知。</span></label><Switch id="content-push-force" v-model="force" :disabled="busy" /></div>
      <Callout v-if="error" tone="destructive" title="推送操作未完成">{{ error }}</Callout>
      <section class="rounded-lg border p-4"><div class="mb-3 flex flex-wrap items-center justify-between gap-2"><h3 class="text-sm font-semibold">{{ resultIsTest ? '本人测试结果' : '最近推送结果' }}</h3><Button size="sm" variant="ghost" :disabled="checking" @click="refresh()">{{ checking ? '查询中…' : '刷新结果' }}</Button></div><p v-if="!result" class="text-xs text-muted-foreground">{{ checking ? '正在查询发送记录…' : '暂无发送记录。' }}</p><template v-else><p class="text-sm font-medium" :class="['failed', 'partial'].includes(result.status) ? 'text-destructive' : ''">{{ statusLabel(result.status) }}</p><p v-if="result.status === 'queued'" class="mt-2 text-xs text-muted-foreground">任务已入队，正在等待真实发送结果；入队不代表发送成功。</p><p v-else class="mt-2 text-xs text-muted-foreground">目标 {{ result.targetCount ?? '—' }} 人 · 成功 {{ result.sent ?? '—' }} 人 · 失败 {{ result.failed ?? '—' }} 人</p><p v-if="result.errorMsg" class="mt-2 break-words text-xs text-destructive">{{ result.errorMsg }}</p><p v-if="result.finishedAt" class="mt-2 text-xs text-muted-foreground">{{ result.finishedAt.replace('T', ' ').slice(0, 19) }}</p></template></section>
    </div>
    <template #footer><Button variant="outline" :disabled="busy" @click="testSelf">只发给我测试</Button><Button :disabled="busy || result?.status === 'queued'" @click="prepareSend">发送发布通知</Button></template>
  </Dialog>
  <ConfirmDialog v-model:open="confirmOpen" title="确认发送企业微信通知？" :description="`《${title}》将发送给${audienceDescription}。通知中使用员工可见的已发布内容链接。`" confirm-text="确认发送" :busy="busy" @confirm="confirmSend" />
  <ConfirmDialog v-model:open="forceConfirmOpen" title="再次确认重复发送？" description="您已开启“24 小时内重复发送”。即使这条内容近期已推送，符合范围的员工仍会再次收到通知。" confirm-text="确认重复发送" tone="destructive" :busy="busy" @confirm="send" />
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import Dialog from '../../ui/Dialog.vue'
import ConfirmDialog from '../../ui/ConfirmDialog.vue'
import Button from '../../ui/Button.vue'
import Select from '../../ui/Select.vue'
import Switch from '../../ui/Switch.vue'
import Textarea from '../../ui/Textarea.vue'
import Callout from '../../ui/Callout.vue'
import { pushApi, type ContentPushResult } from '../../lib/portal-push'
import { errText } from '../../lib/toast'
const props = defineProps<{ open: boolean; kind: 'news' | 'hrdoc'; id: number; title: string; targetKey: string; initialDepartments?: string[] }>()
const emit = defineEmits<{ 'update:open': [value: boolean] }>()
const audience = ref('scope'), departments = ref(''), usernames = ref(''), customMessage = ref(''), force = ref(false), busy = ref(false), checking = ref(false), error = ref(''), confirmOpen = ref(false), forceConfirmOpen = ref(false), result = ref<ContentPushResult | null>(null), resultIsTest = ref(false)
const audienceOptions = computed(() => [{ value: 'scope', label: '指定部门' }, { value: 'all', label: '全员' }, ...(props.kind === 'hrdoc' ? [{ value: 'users', label: '指定员工' }] : [])])
const lines = (text: string) => [...new Set(text.split('\n').map(value => value.trim()).filter(Boolean))]
const audienceDescription = computed(() => audience.value === 'all' ? '全部符合条件的在职员工' : audience.value === 'users' ? `指定的 ${lines(usernames.value).length} 位员工` : `指定的 ${lines(departments.value).length} 个部门中的员工`)
let timer: ReturnType<typeof setTimeout> | undefined, epoch = 0, polls = 0, trigger = '', target = ''
function clearPoll() { if (timer) clearTimeout(timer); timer = undefined }
function statusLabel(status: string) { return ({ queued: '已入队，等待发送', sent: '发送成功', partial: '部分发送失败', failed: '发送失败', cooldown: '未发送：24 小时重复推送限制', no_recipients: '未发送：没有符合范围的员工' } as Record<string, string>)[status] ?? status }
async function refresh() {
  if (!props.open || !trigger || !target || checking.value) return
  const current = epoch; clearPoll(); checking.value = true; error.value = ''
  try { const latest = await pushApi.latest(trigger, target); if (current !== epoch) return; result.value = latest; if (latest?.status === 'queued' && polls++ < 20) { timer = setTimeout(() => { void refresh() }, 3000) } }
  catch (e) { if (current === epoch) error.value = errText(e) }
  finally { if (current === epoch) checking.value = false }
}
watch(() => props.open, open => {
  epoch++; clearPoll(); polls = 0; busy.value = false; checking.value = false; confirmOpen.value = false; forceConfirmOpen.value = false
  if (!open) return
  result.value = null; resultIsTest.value = false; error.value = ''; force.value = false; customMessage.value = ''; usernames.value = ''; departments.value = (props.initialDepartments ?? []).join('\n'); audience.value = departments.value ? 'scope' : 'all'
  trigger = props.kind === 'hrdoc' ? 'hr_doc' : 'news'; target = props.targetKey; void refresh()
})
function prepareSend() { error.value = ''; if (audience.value === 'scope' && !lines(departments.value).length) { error.value = '请至少填写一个部门，或明确选择全员'; return } if (audience.value === 'users' && (!lines(usernames.value).length || lines(usernames.value).length > 200)) { error.value = '请填写 1 至 200 个员工账号'; return } confirmOpen.value = true }
function confirmSend() { confirmOpen.value = false; if (force.value) forceConfirmOpen.value = true; else void send() }
async function submit(test: boolean) {
  if (busy.value) return; busy.value = true; error.value = ''; clearPoll(); const current = ++epoch; checking.value = false
  try {
    const body = props.kind === 'news' ? { notifyDeptScope: audience.value === 'all' ? [] : lines(departments.value), force: force.value } : { audience: audience.value, ...(audience.value === 'scope' ? { notifyDeptScope: lines(departments.value) } : {}), ...(audience.value === 'users' ? { userIds: lines(usernames.value) } : {}), customMessage: customMessage.value.trim() || undefined, force: force.value }
    const queued = test ? await pushApi.test(props.kind, props.id) : await pushApi.send(props.kind, props.id, body)
    if (current !== epoch) return
    result.value = queued; resultIsTest.value = test; trigger = queued.trigger || (props.kind === 'hrdoc' ? 'hr_doc' : 'news'); target = queued.targetKey || props.targetKey; polls = 0; confirmOpen.value = false; forceConfirmOpen.value = false
    timer = setTimeout(() => { void refresh() }, 1000)
  } catch (e) { if (current === epoch) { error.value = errText(e); confirmOpen.value = false; forceConfirmOpen.value = false } }
  finally { if (current === epoch) busy.value = false }
}
function testSelf() { void submit(true) }
function send() { void submit(false) }
onBeforeUnmount(() => { epoch++; clearPoll() })
</script>
