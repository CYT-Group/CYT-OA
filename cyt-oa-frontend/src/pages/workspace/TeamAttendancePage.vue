<template>
  <div class="min-w-0 space-y-5">
    <PageHeader title="团队考勤" description="按月查看直属团队的考勤记录，系统管理员可切换全公司范围。" />
    <form class="panel flex flex-wrap items-end gap-3 p-4" @submit.prevent="applyFilters"><div><label for="team-attendance-month" class="field-label">月份</label><Input id="team-attendance-month" v-model="monthInput" type="month" required /></div><div v-if="session.isAdmin"><label for="team-attendance-scope" class="field-label">数据范围</label><Select id="team-attendance-scope" v-model="scopeInput" class="w-40" :options="[{ value: 'team', label: '直属团队' }, { value: 'company', label: '全公司' }]" /></div><Button type="submit" variant="outline">查询</Button></form>
    <Callout v-if="error" tone="destructive">{{ error }}<template #action><Button variant="outline" size="sm" @click="load">重试</Button></template></Callout>
    <section class="grid grid-cols-2 gap-3 lg:grid-cols-4" aria-label="团队考勤统计"><div v-for="card in cards" :key="card.key" class="panel p-5"><p class="text-xs text-muted-foreground">{{ card.label }}</p><p class="mt-3 text-2xl font-semibold tabular-nums">{{ loading ? '…' : (data?.summary[card.key] ?? '—') }}</p></div></section>
    <section class="panel overflow-hidden"><p v-if="loading" role="status" class="p-8 text-sm text-muted-foreground">正在读取考勤记录…</p><EmptyState v-else-if="!data?.list.length && !error" title="本月暂无考勤记录" description="这里只显示已有记录，不根据未打卡推断缺勤。" compact /><div v-else-if="data?.list.length" class="overflow-x-auto"><table class="data-table min-w-[640px]"><thead><tr><th>员工</th><th>部门</th><th>考勤时间</th><th>类型</th><th>状态</th><th>备注</th></tr></thead><tbody><tr v-for="item in data.list" :key="item.id"><td class="font-medium">{{ item.name }}</td><td>{{ item.deptName || '—' }}</td><td>{{ dateText(item.attendanceTime) }}</td><td>{{ typeLabels[item.type] || '—' }}</td><td><Badge>{{ statusLabels[item.status] || '—' }}</Badge></td><td>{{ item.remark || '—' }}</td></tr></tbody></table></div><Pagination v-if="data" class="border-t px-4 py-3" :page="page" :page-size="20" :total="Number(data.total)" :disabled="loading" @update:page="value => router.push({ query: { ...route.query, page: value > 1 ? String(value) : undefined } })" /></section>
  </div>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../../lib/api';
import { session } from '../../lib/session';
import { workspaceBase } from '../../lib/workspace-api';
import { dateText, todayShanghai } from '../../lib/hr-workspace';
import { errText } from '../../lib/toast';
import PageHeader from '../../ui/PageHeader.vue';
import Button from '../../ui/Button.vue';
import Input from '../../ui/Input.vue';
import Select from '../../ui/Select.vue';
import Badge from '../../ui/Badge.vue';
import Callout from '../../ui/Callout.vue';
import EmptyState from '../../ui/EmptyState.vue';
import Pagination from '../../ui/Pagination.vue';
interface AttendancePage { list: { id: string; userId: string; name: string; deptName: string; attendanceTime: string; type: number; status: number; remark?: string }[]; total: number; summary: { records: number; normal: number; late: number; early: number; leave: number; travel: number } }
const route = useRoute(), router = useRouter();
const month = computed(() => /^\d{4}-(0[1-9]|1[0-2])$/.test(String(route.query.month)) ? String(route.query.month) : todayShanghai().slice(0, 7));
const scope = computed(() => session.isAdmin && route.query.scope === 'company' ? 'company' : 'team');
const page = computed(() => { const value = Number(route.query.page || 1); return Number.isSafeInteger(value) && value > 0 ? value : 1; });
const monthInput = ref(month.value), scopeInput = ref(scope.value), data = ref<AttendancePage | null>(null), loading = ref(false), error = ref('');
const cards = [{ key: 'records', label: '考勤记录' }, { key: 'normal', label: '正常记录' }, { key: 'late', label: '迟到记录' }, { key: 'early', label: '早退记录' }] as const;
const typeLabels: Record<number, string> = { 1: '上班', 2: '下班', 3: '请假', 4: '出差' }, statusLabels: Record<number, string> = { 1: '正常', 2: '迟到', 3: '早退', 4: '请假', 5: '出差' };
let request = 0;
async function load() { const id = ++request; loading.value = true; error.value = ''; monthInput.value = month.value; scopeInput.value = scope.value; try { const result = await api.get<AttendancePage>(`${workspaceBase}/team-attendance`, { month: month.value, scope: scope.value, pageNo: page.value, pageSize: 20 }); if (id === request) data.value = result; } catch (e) { if (id === request) { data.value = null; error.value = errText(e); } } finally { if (id === request) loading.value = false; } }
function applyFilters() { if (monthInput.value === month.value && scopeInput.value === scope.value && page.value === 1) void load(); else void router.push({ query: { month: monthInput.value, scope: scopeInput.value } }); }
watch(() => [month.value, scope.value, page.value], load, { immediate: true });
</script>
