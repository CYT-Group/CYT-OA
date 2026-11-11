<template>
  <div class="min-w-0 space-y-5">
    <PageHeader title="门户申请与审批" description="在门户内提交申请、跟进审批节点。企业微信单据可从原申请记录查看。"><template #actions><Button variant="outline" @click="router.push('/me/apply?view=mine')">企微申请记录</Button><Button @click="edit()">新建门户申请</Button></template></PageHeader>
    <div class="flex flex-wrap gap-2"><SegmentedTabs :model-value="view" :items="tabs" @update:model-value="value => router.push({ query: { view: value } })" /></div>
    <section class="panel overflow-hidden"><form class="table-toolbar flex-wrap gap-2" @submit.prevent="applyFilters"><Select v-model="typeFilter" class="w-40" aria-label="申请类型" :options="[{ value: '', label: '全部类型' }, ...typeOptions]" /><Select v-model="statusFilter" class="w-40" aria-label="审批状态" :options="[{ value: '', label: '全部状态' }, ...Object.entries(statusLabels).map(([value, label]) => ({ value, label }))]" /><Input v-model="monthFilter" type="month" aria-label="申请创建月份" class="w-40" /><Button type="submit" variant="outline">查询</Button><Button variant="ghost" :disabled="loading" @click="load">刷新</Button></form><Callout v-if="error" tone="destructive" class="m-5">{{ error }}</Callout><p v-if="loading" role="status" class="p-8 text-sm text-muted-foreground">正在读取门户申请…</p><EmptyState v-else-if="!rows.length && !error" title="暂无匹配的门户申请" description="新建申请，或切换查看范围与筛选条件。" compact /><ul v-else class="divide-y"><li v-for="item in rows" :key="item.id"><button type="button" class="flex w-full flex-wrap items-center justify-between gap-3 px-5 py-4 text-left hover:bg-accent focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring" @click="open(item.id)"><span class="min-w-0 flex-1"><b class="block break-words text-sm font-medium">{{ item.title }}</b><small class="mt-2 block text-xs text-muted-foreground">{{ item.typeText }} · {{ item.ownerName }} · {{ dateText(item.createdAt) }}</small></span><Badge>{{ statusLabels[item.status] || item.status }}</Badge><span v-if="item.canApprove" class="text-xs text-primary">待你处理</span></button></li></ul></section>
    <Dialog :open="opened" :title="editing ? '编辑申请草稿' : '新建门户申请'" size="lg" @update:open="value => { if (!busy) opened = value }"><form id="request-form" class="space-y-4" @submit.prevent="save(true)"><Callout v-if="formError" tone="destructive">{{ formError }}</Callout><div><label for="request-title" class="field-label">申请标题 *</label><Input id="request-title" v-model.trim="form.title" required maxlength="255" /></div><div><label for="request-type" class="field-label">申请类型</label><Select id="request-type" v-model="form.type" :options="typeOptions" :disabled="!!editing" /></div><div v-if="['leave', 'travel', 'overtime'].includes(form.type)" class="grid gap-4 sm:grid-cols-2"><div><label for="request-start" class="field-label">开始时间 *</label><Input id="request-start" v-model="form.startAt" type="datetime-local" required /></div><div><label for="request-end" class="field-label">结束时间 *</label><Input id="request-end" v-model="form.endAt" type="datetime-local" required /></div></div><div v-if="form.type === 'leave'"><label for="request-leave" class="field-label">假期类型</label><Select id="request-leave" v-model="form.leaveKind" :options="[{ value: 'annual', label: '年假' }, { value: 'sick', label: '病假' }, { value: 'personal', label: '事假' }, { value: 'compensatory', label: '调休' }]" /></div><div v-if="['expense', 'travel', 'purchase'].includes(form.type)"><label for="request-amount" class="field-label">申请金额（元）*</label><Input id="request-amount" v-model="form.amount" type="number" min="0.01" step="0.01" required /></div><div v-if="form.type === 'expense'"><label for="request-expense" class="field-label">费用类别</label><Select id="request-expense" v-model="form.expenseKind" :options="[{ value: 'hotel', label: '差旅住宿' }, { value: 'transport', label: '差旅交通' }, { value: 'cityTransport', label: '市内交通' }, { value: 'hospitality', label: '业务招待' }]" /></div><div v-if="form.type === 'travel'"><label for="request-destination" class="field-label">出差目的地 *</label><Input id="request-destination" v-model.trim="form.destination" required maxlength="255" /></div><div v-if="form.type === 'purchase'"><label for="request-items" class="field-label">采购明细 *</label><Textarea id="request-items" v-model="form.items" required maxlength="4000" :rows="4" /></div><div v-if="form.type === 'seal'" class="grid gap-4 sm:grid-cols-2"><div><label for="request-seal" class="field-label">印章类型 *</label><Input id="request-seal" v-model.trim="form.sealType" required maxlength="100" /></div><div><label for="request-document" class="field-label">用章文件 *</label><Input id="request-document" v-model.trim="form.documentName" required maxlength="255" /></div><div><label for="request-copies" class="field-label">用章份数 *</label><Input id="request-copies" v-model="form.copies" type="number" min="1" max="10000" step="1" required /></div></div><div><label for="request-reason" class="field-label">申请事由 *</label><Textarea id="request-reason" v-model.trim="form.reason" required maxlength="4000" :rows="4" /></div></form><template #footer><Button variant="outline" :disabled="busy" @click="opened = false">取消</Button><Button variant="outline" :loading="busy" @click="save(false)">保存草稿</Button><Button type="submit" form="request-form" :loading="busy">提交审批</Button></template></Dialog>
    <Dialog :open="detailOpen" :title="detail?.title || '申请详情'" size="lg" @update:open="value => { if (!busy) detailOpen = value }"><p v-if="detailLoading" role="status" class="text-sm text-muted-foreground">正在读取申请详情…</p><Callout v-if="detailError" tone="destructive">{{ detailError }}</Callout><template v-if="detail"><div class="flex flex-wrap items-center gap-3"><Badge>{{ statusLabels[detail.status] }}</Badge><span class="text-xs text-muted-foreground">{{ detail.typeText }} · {{ detail.ownerName }}</span></div><dl class="mt-5 grid gap-4 sm:grid-cols-2"><div v-for="field in detailFields" :key="field.key" class="min-w-0"><dt class="text-xs text-muted-foreground">{{ field.label }}</dt><dd class="mt-1 whitespace-pre-wrap break-words text-sm leading-6">{{ displayField(field.key, detail.data[field.key]) }}</dd></div></dl><section class="mt-5 border-t pt-4"><h3 class="text-sm font-semibold">审批进度</h3><p v-if="!detail.steps.length" class="mt-3 text-xs text-muted-foreground">草稿尚未提交审批。</p><ol class="mt-3 space-y-3"><li v-for="step in detail.steps" :key="step.stepNo" class="rounded-lg border p-3"><div class="flex flex-wrap items-center justify-between gap-2 text-sm"><span>第 {{ step.stepNo }} 级 · {{ step.approverName }}</span><Badge>{{ step.status === 'waiting' ? '等待前级审批' : step.status === 'pending' ? '待审批' : statusLabels[step.status] }}</Badge></div><p v-if="step.comment" class="mt-2 whitespace-pre-wrap text-sm text-muted-foreground">{{ step.comment }}</p></li></ol></section><div v-if="detail.canApprove" class="mt-4"><label for="approval-comment" class="field-label">审批意见（驳回时必填）</label><Textarea id="approval-comment" v-model.trim="comment" maxlength="1000" :rows="3" /></div></template><template #footer><template v-if="detail && !detailLoading"><template v-if="detail.canApprove"><Button variant="destructive" :disabled="busy" @click="act('reject')">驳回申请</Button><Button :loading="busy" @click="act('approve')">通过申请</Button></template><template v-else-if="detail.ownerId === session.user?.userId.toString() && ['draft', 'pending'].includes(detail.status)"><Button variant="outline" :disabled="busy" @click="cancelTarget = detail">撤销申请</Button><Button v-if="detail.status === 'draft'" @click="edit(detail)">编辑草稿</Button></template></template></template></Dialog>
    <Dialog :open="!!cancelTarget" title="撤销申请" @update:open="value => { if (!value && !busy) cancelTarget = null }"><p class="text-sm">确定撤销「{{ cancelTarget?.title }}」？</p><Callout v-if="cancelError" class="mt-3" tone="destructive">{{ cancelError }}</Callout><template #footer><Button variant="outline" :disabled="busy" @click="cancelTarget = null">返回</Button><Button variant="destructive" :loading="busy" @click="cancel">撤销申请</Button></template></Dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { workspaceApi, type WorkspaceRequest } from '../../lib/workspace-api';
import { session } from '../../lib/session';
import { meSummary } from '../../lib/me';
import { dateText } from '../../lib/hr-workspace';
import { errText, toast } from '../../lib/toast';
import PageHeader from '../../ui/PageHeader.vue';
import Button from '../../ui/Button.vue';
import Input from '../../ui/Input.vue';
import Select from '../../ui/Select.vue';
import Textarea from '../../ui/Textarea.vue';
import Badge from '../../ui/Badge.vue';
import Callout from '../../ui/Callout.vue';
import EmptyState from '../../ui/EmptyState.vue';
import Dialog from '../../ui/Dialog.vue';
import SegmentedTabs from '../../ui/SegmentedTabs.vue';
const route = useRoute(), router = useRouter();
const types = { leave: '请假', expense: '报销', travel: '出差', purchase: '采购', seal: '用章', overtime: '加班' };
const typeOptions = Object.entries(types).map(([value, label]) => ({ value, label }));
const statusLabels: Record<string, string> = { draft: '草稿', pending: '审批中', approved: '已通过', rejected: '已驳回', cancelled: '已撤销' };
const tabs = computed(() => [{ value: 'mine', label: '我的门户申请' }, { value: 'history', label: '历史记录' }, { value: 'pending', label: '待我审批' }, { value: 'processed', label: '我已处理' }, ...(meSummary.teamCount > 0 || session.isAdmin ? [{ value: 'team', label: '团队申请' }] : []), ...(session.isAdmin ? [{ value: 'company', label: '全公司申请' }] : [])]);
const view = computed(() => ['history', 'pending', 'processed', 'team', 'company'].includes(String(route.query.view)) ? String(route.query.view) : 'mine');
const rows = ref<WorkspaceRequest[]>([]), typeFilter = ref(''), statusFilter = ref(''), monthFilter = ref(''), loading = ref(false), error = ref(''), busy = ref(false);
const opened = ref(false), editing = ref<WorkspaceRequest | null>(null), formError = ref(''), detail = ref<WorkspaceRequest | null>(null), detailOpen = ref(false), detailError = ref(''), detailLoading = ref(false), comment = ref(''), cancelTarget = ref<WorkspaceRequest | null>(null), cancelError = ref('');
const defaults = () => ({ title: '', type: String(route.query.type || 'purchase'), reason: '', startAt: '', endAt: '', leaveKind: 'annual', amount: '', expenseKind: 'hotel', destination: '', items: '', sealType: '', documentName: '', copies: '1' });
const form = reactive<Record<string, any>>(defaults());
const fieldLabels: Record<string, string> = { reason: '申请事由', startAt: '开始时间', endAt: '结束时间', days: '请假天数', leaveKind: '假期类型', amount: '申请金额（元）', expenseKind: '费用类别', destination: '目的地', items: '采购明细', sealType: '印章类型', documentName: '用章文件', copies: '份数' };
const displayField = (key: string, value: unknown) => key === 'leaveKind' ? ({ annual: '年假', sick: '病假', personal: '事假', compensatory: '调休' } as Record<string, string>)[String(value)] || value : key === 'expenseKind' ? ({ hotel: '差旅住宿', transport: '差旅交通', cityTransport: '市内交通', hospitality: '业务招待' } as Record<string, string>)[String(value)] || value : value;
const detailFields = computed(() => Object.entries(fieldLabels).filter(([key]) => detail.value?.data[key] != null).map(([key, label]) => ({ key, label })));
const requestMonth = (value: string) => { if (/Z$|[+-]\d{2}:?\d{2}$/.test(value)) { const date = new Date(value); return Number.isNaN(date.getTime()) ? '' : new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit' }).format(date); } return value.slice(0, 7); };
let request = 0;
async function load() { const id = ++request; loading.value = true; error.value = ''; typeFilter.value = String(route.query.type || ''); statusFilter.value = String(route.query.status || ''); monthFilter.value = /^\d{4}-(0[1-9]|1[0-2])$/.test(String(route.query.month)) ? String(route.query.month) : ''; try { const data = await workspaceApi.requests(view.value, typeFilter.value, statusFilter.value); if (id === request) rows.value = monthFilter.value ? data.filter(item => requestMonth(item.createdAt) === monthFilter.value) : data; } catch (e) { if (id === request) { rows.value = []; error.value = errText(e); } } finally { if (id === request) loading.value = false; } }
function applyFilters() { if (String(route.query.type || '') === typeFilter.value && String(route.query.status || '') === statusFilter.value && String(route.query.month || '') === monthFilter.value) void load(); else void router.push({ query: { view: view.value, type: typeFilter.value || undefined, status: statusFilter.value || undefined, month: monthFilter.value || undefined } }); }
function edit(item?: WorkspaceRequest) { editing.value = item || null; Object.assign(form, defaults(), item ? { ...item.data, title: item.title } : {}); formError.value = ''; detailOpen.value = false; opened.value = true; }
async function save(submit: boolean) {
  if (busy.value) return;
  const nativeForm = document.getElementById('request-form') as HTMLFormElement | null;
  if (!nativeForm?.reportValidity()) return;
  busy.value = true;
  formError.value = '';
  try {
    const saved = await workspaceApi.saveRequest(editing.value?.id, { ...form, version: editing.value?.version }, submit);
    opened.value = false;
    toast(submit ? '申请已提交审批' : '申请草稿已保存');
    const alreadySelected = route.query.view === 'mine' && route.query.requestId === saved.id;
    await router.replace({ query: { view: 'mine', requestId: saved.id } });
    if (alreadySelected) await Promise.all([load(), open(saved.id)]);
  } catch (e) { formError.value = errText(e); }
  finally { busy.value = false; }
}
let detailRequest = 0;
async function open(id: string) { const req = ++detailRequest; detailOpen.value = true; detail.value = null; detailError.value = ''; detailLoading.value = true; comment.value = ''; try { const data = await workspaceApi.request(id); if (req === detailRequest) detail.value = data; } catch (e) { if (req === detailRequest) detailError.value = errText(e); } finally { if (req === detailRequest) detailLoading.value = false; } }
async function act(action: 'approve' | 'reject') {
  if (!detail.value || busy.value) return;
  detailError.value = '';
  if (action === 'reject' && !comment.value) { detailError.value = '请填写驳回原因'; return; }
  busy.value = true;
  try {
    const id = detail.value.id;
    await workspaceApi.requestAction(id, action, comment.value);
    toast(action === 'approve' ? '申请已通过当前节点' : '申请已驳回');
    await Promise.all([load(), open(id)]);
  } catch (e) { detailError.value = errText(e); }
  finally { busy.value = false; }
}
async function cancel() { if (!cancelTarget.value || busy.value) return; busy.value = true; cancelError.value = ''; try { const id = cancelTarget.value.id; await workspaceApi.requestAction(id, 'cancel'); cancelTarget.value = null; toast('申请已撤销'); await Promise.all([load(), open(id)]); } catch (e) { cancelError.value = errText(e); } finally { busy.value = false; } }
watch(() => route.fullPath, () => { void load(); if (route.query.requestId) void open(String(route.query.requestId)); if (route.query.view === 'start') edit(); }, { immediate: true });
</script>
