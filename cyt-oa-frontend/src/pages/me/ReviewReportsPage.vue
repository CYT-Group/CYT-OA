<template>
  <div class="min-w-0 space-y-5">
    <PageHeader title="述职报告" description="整理阶段成果、下一步计划与需要的支持，提交后由直属上级点评。">
      <template #actions><Button variant="outline" :loading="loading" @click="load"><RefreshCw class="size-4" />刷新</Button><Button @click="compose()"><PenLine class="size-4" />写述职</Button></template>
    </PageHeader>
    <div class="flex flex-wrap items-center justify-between gap-3">
      <SegmentedTabs :model-value="view" :items="viewTabs" @update:model-value="changeView" />
      <div class="flex flex-wrap gap-2"><Input v-model="query" aria-label="搜索述职报告" placeholder="搜索标题、周期或作者" class="sm:w-64" /><Select v-if="view === 'mine'" v-model="status" :options="[{ value: '', label: '全部状态' }, { value: 'draft', label: '草稿' }, { value: 'submitted', label: '已提交' }]" aria-label="述职状态" class="sm:w-36" /></div>
    </div>
    <p v-if="loading" class="py-12 text-center text-sm text-muted-foreground" role="status">正在读取述职报告…</p>
    <Callout v-else-if="error" title="述职报告未读取" tone="destructive">{{ error }}</Callout>
    <EmptyState v-else-if="!filteredRows.length" :title="query || status ? '没有匹配的述职报告' : view === 'team' ? '暂无已提交的团队述职' : '还没有述职报告'" :description="view === 'team' ? '直属下级提交的述职报告会显示在这里' : '记录阶段成果，保存草稿后可继续修改或提交'">
      <Button v-if="view === 'mine' && !query && !status" @click="compose()">写第一份述职</Button>
    </EmptyState>
    <template v-else>
      <p class="text-xs text-muted-foreground">共 {{ filteredRows.length }} 份{{ view === 'team' && session.isAdmin ? '全公司已提交述职' : view === 'team' ? '直属下级已提交述职' : '述职报告' }}</p>
      <div class="grid gap-4 lg:grid-cols-2">
        <article v-for="row in filteredRows" :key="row.id" class="panel flex min-w-0 flex-col p-5">
          <div class="flex items-start justify-between gap-3"><div class="min-w-0"><button type="button" class="break-words text-left text-sm font-semibold hover:text-primary focus-visible:outline-2 focus-visible:outline-primary" @click="openDetail(row.id)">{{ row.title }}</button><p class="mt-2 text-xs text-muted-foreground">{{ row.ownerName }}<span v-if="row.data.period"> · {{ row.data.period }}</span></p></div><Badge :variant="row.status === 'submitted' ? 'success' : 'outline'">{{ row.status === 'submitted' ? '已提交' : '草稿' }}</Badge></div>
          <p class="mt-4 line-clamp-3 flex-1 whitespace-pre-wrap break-words text-sm leading-7 text-muted-foreground">{{ row.data.summary || '草稿尚未填写阶段成果' }}</p>
          <div class="mt-5 flex flex-wrap items-center justify-between gap-2 border-t pt-4"><span class="text-xs text-muted-foreground">{{ row.data.comments?.length || 0 }} 条点评</span><div class="flex gap-2"><Button v-if="row.editable && row.status === 'draft'" variant="outline" size="sm" @click="compose(row)">继续填写</Button><Button variant="soft" size="sm" @click="openDetail(row.id)">{{ row.canComment ? '查看与点评' : '查看全文' }}</Button></div></div>
        </article>
      </div>
    </template>

    <Dialog :open="form !== null" :title="form?.id ? '继续填写述职' : '新建述职报告'" size="lg" @update:open="closeForm">
      <form v-if="form" id="review-report-form" class="space-y-4" @submit.prevent="save(false)">
        <Callout v-if="formError" title="保存或提交未完成" tone="destructive">{{ formError }}</Callout>
        <div><label for="review-title" class="field-label">述职标题 *</label><Input id="review-title" v-model.trim="form.title" required maxlength="255" placeholder="如：试用期转正述职" :disabled="saving" /></div>
        <div><label for="review-period" class="field-label">述职周期</label><Input id="review-period" v-model.trim="form.period" maxlength="100" placeholder="如：2026 年第三季度" :disabled="saving" /></div>
        <div><label for="review-summary" class="field-label">阶段成果 *</label><Textarea id="review-summary" v-model.trim="form.summary" :rows="6" maxlength="4000" placeholder="说明完成的工作、成果与关键数据" :disabled="saving" /><p class="mt-1 text-xs text-muted-foreground">草稿可先保存，提交前需补充阶段成果。</p></div>
        <div><label for="review-plan" class="field-label">下一阶段计划</label><Textarea id="review-plan" v-model.trim="form.plan" :rows="4" maxlength="4000" :disabled="saving" /></div>
        <div><label for="review-problem" class="field-label">风险与需要的支持</label><Textarea id="review-problem" v-model.trim="form.problem" :rows="3" maxlength="2000" :disabled="saving" /></div>
      </form>
      <template #footer><Button variant="outline" :disabled="saving" @click="closeForm(false)">取消</Button><Button variant="outline" :loading="saving" @click="save(false)">保存草稿</Button><Button :disabled="saving || !form?.title.trim() || !form?.summary.trim()" @click="save(true)">保存并提交</Button></template>
    </Dialog>

    <Dialog :open="detailOpen" :title="detail?.title || '述职报告'" size="xl" @update:open="closeDetail">
      <p v-if="detailLoading" class="py-10 text-center text-sm text-muted-foreground" role="status">正在读取述职全文…</p>
      <Callout v-else-if="detailError" title="述职全文未读取" tone="destructive">{{ detailError }}</Callout>
      <template v-else-if="detail">
        <div class="flex flex-wrap items-center gap-3 text-xs text-muted-foreground"><span>{{ detail.ownerName }}</span><span v-if="detail.data.period">{{ detail.data.period }}</span><Badge :variant="detail.status === 'submitted' ? 'success' : 'outline'">{{ detail.status === 'submitted' ? '已提交' : '草稿' }}</Badge></div>
        <div class="mt-6 space-y-6"><section v-for="section in sections" :key="section.key"><h3 class="text-sm font-semibold">{{ section.title }}</h3><p class="mt-2 whitespace-pre-wrap break-words text-sm leading-7 text-muted-foreground">{{ detail.data[section.key] || '未填写' }}</p></section></div>
        <section class="mt-6 border-t pt-5"><h3 class="text-sm font-semibold">点评记录</h3><EmptyState v-if="!detail.data.comments?.length" title="暂无点评" compact /><ul v-else class="mt-3 space-y-3"><li v-for="comment in detail.data.comments" :key="comment.id" class="rounded-lg bg-muted/60 p-4"><div class="flex flex-wrap items-center gap-2 text-xs text-muted-foreground"><span>{{ comment.reviewerName }}</span><Badge v-if="comment.grade" variant="outline">{{ comment.grade }}</Badge><span>{{ fmtTime(comment.createdAt) }}</span></div><p class="mt-2 whitespace-pre-wrap break-words text-sm leading-7">{{ comment.reviewText }}</p></li></ul></section>
        <form v-if="detail.canComment && detail.status === 'submitted'" class="mt-5 space-y-3 border-t pt-5" @submit.prevent="comment">
          <h3 class="text-sm font-semibold">写点评</h3><Callout v-if="commentError" tone="destructive">{{ commentError }}</Callout><div><label for="review-grade" class="field-label">评级（选填）</label><Input id="review-grade" v-model.trim="grade" maxlength="30" class="sm:max-w-xs" placeholder="如：通过 / 待改进" :disabled="commenting" /></div><div><label for="review-comment" class="field-label">点评内容 *</label><Textarea id="review-comment" v-model.trim="reviewText" :rows="4" maxlength="2000" required :disabled="commenting" /></div><Button type="submit" :disabled="!reviewText.trim()" :loading="commenting">发送点评</Button>
        </form>
      </template>
      <template #footer><Button variant="outline" @click="closeDetail(false)">关闭</Button><Button v-if="detail?.editable && detail.status === 'draft'" @click="compose(detail)">继续填写</Button></template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { PenLine, RefreshCw } from '@lucide/vue';
import { api } from '../../lib/api';
import { session } from '../../lib/session';
import { meSummary } from '../../lib/me';
import { fmtTime } from '../../lib/format';
import { errText, toast } from '../../lib/toast';
import PageHeader from '../../ui/PageHeader.vue';
import Button from '../../ui/Button.vue';
import Badge from '../../ui/Badge.vue';
import Input from '../../ui/Input.vue';
import Textarea from '../../ui/Textarea.vue';
import Select from '../../ui/Select.vue';
import SegmentedTabs from '../../ui/SegmentedTabs.vue';
import Callout from '../../ui/Callout.vue';
import EmptyState from '../../ui/EmptyState.vue';
import Dialog from '../../ui/Dialog.vue';

interface ReviewComment { id: string; reviewerId: string; reviewerName: string; reviewText: string; grade?: string; createdAt: string }
interface ReviewRecord { id: string; title: string; status: 'draft' | 'submitted'; ownerId: string; ownerName: string; data: { period: string; summary: string; plan: string; problem: string; comments: ReviewComment[] }; version: number; editable: boolean; canComment: boolean }
interface ReviewForm { id?: string; version?: number; title: string; period: string; summary: string; plan: string; problem: string }
const base = '/api/workspace/review-reports';
const route = useRoute(), router = useRouter();
const view = computed(() => route.query.view === 'team' ? 'team' : 'mine');
const viewTabs = computed(() => [{ value: 'mine', label: '我的述职' }, ...(session.isAdmin || meSummary.teamCount > 0 ? [{ value: 'team', label: session.isAdmin ? '全公司述职' : '团队述职' }] : [])]);
const rows = ref<ReviewRecord[]>([]), loading = ref(false), error = ref('');
const query = ref(''), status = ref('');
const filteredRows = computed(() => rows.value.filter(row => {
  const text = query.value.trim().toLocaleLowerCase();
  return (view.value === 'team' || !status.value || row.status === status.value) && (!text || `${row.title} ${row.ownerName} ${row.data.period || ''}`.toLocaleLowerCase().includes(text));
}));
const form = ref<ReviewForm | null>(null), formError = ref(''), saving = ref(false);
const detail = ref<ReviewRecord | null>(null), detailOpen = ref(false), detailLoading = ref(false), detailError = ref('');
const grade = ref(''), reviewText = ref(''), commenting = ref(false), commentError = ref('');
const sections: { key: 'summary' | 'plan' | 'problem'; title: string }[] = [{ key: 'summary', title: '阶段成果' }, { key: 'plan', title: '下一阶段计划' }, { key: 'problem', title: '风险与需要的支持' }];
let listRequest = 0, detailRequest = 0;
async function load() {
  const request = ++listRequest; loading.value = true; error.value = '';
  try { const result = await api.get<ReviewRecord[]>(base, { view: view.value }); if (request === listRequest) rows.value = result; }
  catch (e) { if (request === listRequest) { rows.value = []; error.value = errText(e); } }
  finally { if (request === listRequest) loading.value = false; }
}
function changeView(next: string) { status.value = ''; void router.replace({ path: route.path, query: next === 'team' ? { view: 'team' } : {} }); }
function compose(row?: ReviewRecord) {
  if (row && (!row.editable || row.status !== 'draft')) return;
  formError.value = '';
  form.value = row ? { id: row.id, version: row.version, title: row.title, period: row.data.period || '', summary: row.data.summary || '', plan: row.data.plan || '', problem: row.data.problem || '' } : { title: '', period: '', summary: '', plan: '', problem: '' };
  if (detailOpen.value) closeDetail(false);
}
function closeForm(open: boolean) { if (!open && !saving.value) { form.value = null; formError.value = ''; } }
async function save(submitAfter: boolean) {
  if (!form.value || saving.value) return;
  if (!form.value.title.trim()) { formError.value = '请填写述职标题'; return; }
  if (submitAfter && !form.value.summary.trim()) { formError.value = '请先填写阶段成果，再提交述职'; return; }
  saving.value = true; formError.value = '';
  let saved: ReviewRecord | null = null;
  try {
    const body = { title: form.value.title.trim(), period: form.value.period.trim(), summary: form.value.summary.trim(), plan: form.value.plan.trim(), problem: form.value.problem.trim(), version: form.value.version };
    saved = form.value.id ? await api.put<ReviewRecord>(`${base}/${encodeURIComponent(form.value.id)}`, body) : await api.post<ReviewRecord>(base, body);
    form.value.id = saved.id; form.value.version = saved.version;
    if (submitAfter) await api.post<ReviewRecord>(`${base}/${encodeURIComponent(saved.id)}/submit`, {});
    toast(submitAfter ? '述职报告已提交' : '述职草稿已保存'); form.value = null;
    await load();
  } catch (e) { formError.value = `${saved ? '草稿已保存，提交未完成：' : ''}${errText(e)}`; if (saved) void load(); }
  finally { saving.value = false; }
}
function openDetail(id: string) { void router.replace({ path: route.path, query: { ...route.query, id } }); }
async function readDetail(id: string) {
  const request = ++detailRequest; detailOpen.value = true; detailLoading.value = true; detailError.value = ''; detail.value = null; commentError.value = ''; grade.value = ''; reviewText.value = '';
  try { const result = await api.get<ReviewRecord>(`${base}/${encodeURIComponent(id)}`); if (request === detailRequest) detail.value = result; }
  catch (e) { if (request === detailRequest) detailError.value = errText(e); }
  finally { if (request === detailRequest) detailLoading.value = false; }
}
function closeDetail(open: boolean) {
  if (open || commenting.value) return;
  ++detailRequest; detailOpen.value = false; detail.value = null;
  const query = { ...route.query }; delete query.id;
  void router.replace({ path: route.path, query });
}
async function comment() {
  if (!detail.value?.canComment || commenting.value || !reviewText.value.trim()) return;
  const id = detail.value.id; commenting.value = true; commentError.value = '';
  try { const result = await api.post<ReviewRecord>(`${base}/${encodeURIComponent(id)}/comment`, { reviewText: reviewText.value.trim(), grade: grade.value.trim() || undefined }); if (detail.value?.id === id) { detail.value = result; reviewText.value = ''; grade.value = ''; } toast('点评已发送'); await load(); }
  catch (e) { commentError.value = errText(e); }
  finally { commenting.value = false; }
}
watch(view, load, { immediate: true });
watch(() => route.query.id, value => { if (typeof value === 'string' && value) void readDetail(value); else { ++detailRequest; detailOpen.value = false; detail.value = null; } }, { immediate: true });
</script>
