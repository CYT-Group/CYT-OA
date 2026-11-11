<template>
  <div class="min-w-0 space-y-5">
    <PageHeader title="目标·OKR" description="明确季度目标，跟进关键结果，查看公司、部门与个人目标的对齐关系。">
      <template #actions><Button @click="edit()">新建目标</Button></template>
    </PageHeader>
    <div class="flex flex-wrap items-center justify-between gap-3">
      <SegmentedTabs :model-value="alignment ? 'align' : 'my'" :items="[{ value: 'my', label: '我的目标' }, { value: 'align', label: '对齐视图' }]" @update:model-value="setTab" />
      <form class="flex items-center gap-2" @submit.prevent="setPeriod"><label for="okr-period" class="text-xs text-muted-foreground">季度</label><Input id="okr-period" v-model="periodInput" required pattern="20[0-9]{2}-Q[1-4]" class="w-32" aria-label="目标季度" /><Button variant="outline" type="submit">查询</Button></form>
    </div>
    <Callout v-if="error" tone="destructive">{{ error }}<template #action><Button size="sm" variant="outline" @click="load">重试</Button></template></Callout>
    <p v-if="loading" class="panel p-8 text-sm text-muted-foreground" role="status">正在读取目标…</p>
    <EmptyState v-else-if="!rows.length && !error" title="本季度暂无目标" description="新建一个目标，再添加可以衡量的关键结果。" />
    <div v-else class="space-y-4">
      <article v-for="item in ordered" :key="item.id" class="panel min-w-0 p-5" :class="alignment && item.parentId ? 'border-l-4 border-l-primary/35 sm:ml-6' : ''">
        <div class="flex flex-wrap items-start justify-between gap-3"><div class="min-w-0 flex-1"><div class="flex flex-wrap items-center gap-2"><Badge>{{ scopes[item.data.scope] }}</Badge><Badge variant="secondary">{{ workspaceStatus[item.status] }}</Badge><span class="text-xs text-muted-foreground">{{ item.ownerName }} · {{ item.data.period }}</span></div><h2 class="mt-3 break-words text-base font-semibold">{{ item.title }}</h2><p v-if="item.parentId" class="mt-2 text-xs text-primary">对齐：{{ parentTitle(item.parentId) }}</p><p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-muted-foreground">{{ item.data.description }}</p></div><Button v-if="item.editable" variant="outline" size="sm" @click="edit(item)">更新进度</Button></div>
        <div class="mt-4 flex items-center gap-3"><div class="h-2 flex-1 overflow-hidden rounded-full bg-muted"><div class="h-full rounded-full bg-primary" :style="{ width: `${item.progress}%` }" /></div><b class="text-sm tabular-nums">{{ item.progress }}%</b></div>
        <ul class="mt-4 divide-y"><li v-for="(kr, index) in item.data.keyResults" :key="index" class="flex min-w-0 items-center justify-between gap-4 py-3 text-sm"><span class="min-w-0 break-words">{{ kr.title }}</span><span class="shrink-0 text-xs tabular-nums text-muted-foreground">{{ kr.current }} / {{ kr.target }} {{ kr.unit }}</span></li></ul>
      </article>
    </div>
    <Dialog :open="opened" :title="editing ? '更新目标' : '新建目标'" size="lg" @update:open="value => { if (!saving) opened = value }">
      <form id="objective-form" class="space-y-4" @submit.prevent="save">
        <Callout v-if="formError" tone="destructive">{{ formError }}</Callout>
        <div><label for="objective-title" class="field-label">目标名称 *</label><Input id="objective-title" v-model="form.title" required maxlength="255" /></div>
        <div class="grid gap-4 sm:grid-cols-3"><div><label for="objective-period" class="field-label">季度 *</label><Input id="objective-period" v-model="form.period" required pattern="20[0-9]{2}-Q[1-4]" /></div><div><label for="objective-scope" class="field-label">目标层级</label><Select id="objective-scope" v-model="form.scope" :options="scopeOptions" /></div><div><label for="objective-status" class="field-label">状态</label><Select id="objective-status" v-model="form.status" :options="[{ value: 'active', label: '进行中' }, { value: 'completed', label: '已完成' }, { value: 'archived', label: '已归档' }]" /></div></div>
        <div><label for="objective-parent" class="field-label">对齐上级目标</label><Select id="objective-parent" v-model="form.parentId" :options="[{ value: '', label: '不关联上级目标' }, ...parents.filter(item => item.id !== editing?.id && item.data.period === form.period).map(item => ({ value: item.id, label: `${item.title}（${item.ownerName}）` }))]" /></div>
        <div><label for="objective-description" class="field-label">说明</label><Textarea id="objective-description" v-model="form.description" :rows="3" maxlength="4000" /></div>
        <fieldset class="space-y-3"><legend class="mb-2 text-sm font-semibold">关键结果</legend><div v-for="(kr, index) in form.keyResults" :key="index" class="space-y-3 rounded-lg border p-3"><div class="flex items-center gap-2"><Input v-model="kr.title" :aria-label="`关键结果 ${index + 1} 名称`" required maxlength="255" placeholder="要达成的结果" /><Button v-if="form.keyResults.length > 1" variant="ghost" size="sm" :aria-label="`移除关键结果 ${index + 1}`" @click="form.keyResults.splice(index, 1)">移除</Button></div><div class="grid grid-cols-2 gap-3 sm:grid-cols-4"><div v-for="field in krFields" :key="field.key"><label :for="`kr-${index}-${field.key}`" class="field-label">{{ field.label }}</label><Input :id="`kr-${index}-${field.key}`" v-model="kr[field.key]" :type="field.key === 'unit' ? 'text' : 'number'" :min="field.key === 'current' ? 0 : field.key === 'unit' ? undefined : 0.01" step="any" required /></div></div></div><Button v-if="form.keyResults.length < 20" variant="outline" size="sm" @click="form.keyResults.push(newKr())">添加关键结果</Button></fieldset>
      </form>
      <template #footer><Button v-if="editing" variant="destructive" :disabled="saving" @click="deleteTarget = editing">删除目标</Button><Button variant="outline" :disabled="saving" @click="opened = false">取消</Button><Button form="objective-form" type="submit" :loading="saving">保存目标</Button></template>
    </Dialog>
    <Dialog :open="!!deleteTarget" title="删除目标" description="删除后无法恢复；有关联的下级目标时需先调整对齐关系。" @update:open="value => { if (!value && !saving) deleteTarget = null }"><p class="text-sm">确定删除「{{ deleteTarget?.title }}」？</p><Callout v-if="formError" class="mt-3" tone="destructive">{{ formError }}</Callout><template #footer><Button variant="outline" :disabled="saving" @click="deleteTarget = null">取消</Button><Button variant="destructive" :loading="saving" @click="remove">删除目标</Button></template></Dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { workspaceApi, workspaceStatus, type Objective, type WorkspaceContext, type KeyResult } from '../../lib/workspace-api';
import { todayShanghai } from '../../lib/hr-workspace';
import { errText, toast } from '../../lib/toast';
import PageHeader from '../../ui/PageHeader.vue';
import Button from '../../ui/Button.vue';
import Badge from '../../ui/Badge.vue';
import Input from '../../ui/Input.vue';
import Select from '../../ui/Select.vue';
import Textarea from '../../ui/Textarea.vue';
import Dialog from '../../ui/Dialog.vue';
import Callout from '../../ui/Callout.vue';
import EmptyState from '../../ui/EmptyState.vue';
import SegmentedTabs from '../../ui/SegmentedTabs.vue';
const route = useRoute(), router = useRouter();
const day = todayShanghai(), currentPeriod = `${day.slice(0, 4)}-Q${Math.floor((Number(day.slice(5, 7)) - 1) / 3) + 1}`;
const period = computed(() => /^20\d{2}-Q[1-4]$/.test(String(route.query.period)) ? String(route.query.period) : currentPeriod);
const periodInput = ref(period.value), alignment = computed(() => route.query.tab === 'align');
const rows = ref<Objective[]>([]), parents = ref<Objective[]>([]), context = ref<WorkspaceContext | null>(null);
const loading = ref(false), error = ref(''), formError = ref(''), opened = ref(false), saving = ref(false), editing = ref<Objective | null>(null), deleteTarget = ref<Objective | null>(null);
const scopes: Record<string, string> = { personal: '个人目标', department: '部门目标', company: '公司目标' };
const scopeOptions = computed(() => Object.entries(scopes).filter(([key]) => key === 'personal' || context.value?.admin || key === 'department' && Number(context.value?.teamCount) > 0).map(([value, label]) => ({ value, label })));
const krFields = [{ key: 'current', label: '当前值' }, { key: 'target', label: '目标值' }, { key: 'unit', label: '单位' }, { key: 'weight', label: '权重' }] as const;
const newKr = (): KeyResult => ({ title: '', target: 100, current: 0, weight: 1, unit: '%' });
const form = reactive({ title: '', period: period.value, scope: 'personal', status: 'active', parentId: '', description: '', keyResults: [newKr()] });
const parentTitle = (id: string) => parents.value.find(item => item.id === id)?.title || '上级目标';
const ordered = computed(() => {
  if (!alignment.value) return rows.value;
  const result: Objective[] = [], seen = new Set<string>();
  const append = (item: Objective) => { if (seen.has(item.id)) return; seen.add(item.id); result.push(item); rows.value.filter(child => child.parentId === item.id).forEach(append); };
  rows.value.filter(item => !item.parentId || !rows.value.some(parent => parent.id === item.parentId)).forEach(append);
  rows.value.forEach(append); return result;
});
let request = 0;
async function load() {
  const id = ++request; loading.value = true; error.value = ''; periodInput.value = period.value;
  try { const result = await Promise.all([workspaceApi.objectives(period.value, alignment.value), workspaceApi.objectives(period.value, true), workspaceApi.context()]); if (id !== request) return; [rows.value, parents.value, context.value] = result; }
  catch (e) { if (id === request) { rows.value = []; error.value = errText(e); } } finally { if (id === request) loading.value = false; }
}
function setTab(tab: string) { void router.push({ query: { ...route.query, tab } }); }
function setPeriod() { if (periodInput.value === period.value) void load(); else void router.push({ query: { ...route.query, period: periodInput.value } }); }
function edit(item?: Objective) { editing.value = item || null; formError.value = ''; Object.assign(form, item ? { title: item.title, period: item.data.period, scope: item.data.scope, status: item.status, parentId: item.parentId || '', description: item.data.description, keyResults: item.data.keyResults.map(kr => ({ ...kr })) } : { title: '', period: period.value, scope: 'personal', status: 'active', parentId: '', description: '', keyResults: [newKr()] }); opened.value = true; }
async function save() { if (saving.value) return; saving.value = true; formError.value = ''; try { const saved = await workspaceApi.saveObjective(editing.value?.id, { ...form, version: editing.value?.version, keyResults: form.keyResults.map(kr => ({ ...kr, current: Number(kr.current), target: Number(kr.target), weight: Number(kr.weight) })) }); opened.value = false; toast('目标已保存'); if (saved.data.period !== period.value) await router.push({ query: { ...route.query, period: saved.data.period } }); else await load(); } catch (e) { formError.value = errText(e); } finally { saving.value = false; } }
async function remove() { if (!deleteTarget.value || saving.value) return; saving.value = true; formError.value = ''; try { await workspaceApi.deleteObjective(deleteTarget.value.id); deleteTarget.value = null; opened.value = false; toast('目标已删除'); await load(); } catch (e) { formError.value = errText(e); } finally { saving.value = false; } }
watch(() => [period.value, alignment.value], load, { immediate: true });
</script>
