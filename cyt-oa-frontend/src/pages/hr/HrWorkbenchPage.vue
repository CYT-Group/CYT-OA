<template>
  <div class="min-w-0 space-y-5">
    <PageHeader
      title="人事工作台"
      description="招聘、花名册、入离职与员工历程，统一在个人中心办理。"
    >
      <template #actions>
        <Button variant="outline" :loading="refreshing" @click="refresh">
          <RefreshCw class="size-4" />
          刷新
        </Button>
      </template>
    </PageHeader>

    <section class="welcome-banner" aria-label="人事办理">
      <div class="min-w-0">
        <h2 class="welcome-title">你好，{{ session.name || '人事伙伴' }}</h2>
        <p class="text-muted-foreground">
          及时跟进招聘与人事待办，让每一次入职、成长与离职都有记录。
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button v-if="canEmployment" @click="router.push('/hr/employment?mode=onboard')">
          办理入职
        </Button>
        <Button
          v-if="canQueryEmployees"
          variant="outline"
          @click="router.push('/page/hrm-employee?roster=active')"
        >
          查看花名册
        </Button>
      </div>
    </section>

    <Callout v-if="error" title="人事概况未读取" tone="destructive">
      {{ error }}
      <template #action>
        <Button variant="outline" size="sm" :disabled="loading" @click="loadSummary">重试</Button>
      </template>
    </Callout>
    <Callout v-if="probationError" title="试用人数未读取" tone="destructive">
      {{ probationError }}
      <template #action>
        <Button variant="outline" size="sm" :disabled="probationLoading" @click="loadProbation">重试</Button>
      </template>
    </Callout>

    <section class="grid grid-cols-2 gap-3 xl:grid-cols-4" aria-label="人事概况">
      <component
        :is="card.to ? RouterLink : 'div'"
        v-for="card in cards"
        :key="card.label"
        v-bind="card.to ? { to: card.to } : {}"
        class="panel min-w-0 p-4 sm:p-5"
        :class="card.to ? 'group transition-colors hover:border-primary/40 focus-visible:outline-2 focus-visible:outline-primary' : ''"
      >
        <div class="flex items-center justify-between gap-2">
          <span class="text-xs text-muted-foreground">{{ card.label }}</span>
          <span class="flex size-8 shrink-0 items-center justify-center rounded-lg" :class="card.tone" aria-hidden="true">{{ card.icon }}</span>
        </div>
        <div class="mt-2 text-3xl font-semibold tabular-nums">{{ card.loading ? '…' : (card.value ?? '—') }}</div>
        <div class="mt-2 flex items-center justify-between gap-2 text-xs text-muted-foreground">
          <span>{{ card.note }}</span>
          <ArrowUpRight v-if="card.to" class="size-3.5 shrink-0 text-primary" aria-hidden="true" />
        </div>
      </component>
    </section>

    <section v-if="modules.length" class="space-y-3" aria-labelledby="hr-modules-title">
      <h2 id="hr-modules-title" class="text-sm font-semibold">人事业务</h2>
      <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <RouterLink
          v-for="item in modules"
          :key="item.title"
          :to="item.to"
          class="panel group flex min-w-0 flex-col p-5 transition-colors hover:border-primary/40 focus-visible:outline-2 focus-visible:outline-primary"
        >
          <div class="flex items-center justify-between gap-3">
            <span class="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-xl" aria-hidden="true">{{ item.icon }}</span>
            <ArrowUpRight class="size-4 text-muted-foreground group-hover:text-primary" aria-hidden="true" />
          </div>
          <h3 class="mt-4 text-sm font-semibold">{{ item.title }}</h3>
          <p class="mt-1 flex-1 text-xs leading-6 text-muted-foreground">{{ item.description }}</p>
          <span class="mt-3 text-xs text-primary">{{ item.action }}</span>
        </RouterLink>
      </div>
    </section>

    <div class="grid items-start gap-5 xl:grid-cols-[1fr_1.3fr]">
      <section v-if="canQueryEmployees" class="panel p-5" aria-labelledby="hr-todos-title">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <h2 id="hr-todos-title" class="panel-title">本月人事待办</h2>
          <span class="text-xs text-muted-foreground">{{ currentMonthLabel }}</span>
        </div>
        <p v-if="loading" class="py-8 text-center text-sm text-muted-foreground" role="status">正在读取待办…</p>
        <EmptyState
          v-else-if="summary && !visibleTodos.length"
          title="本月暂无人事待办"
          description="待入职、离职、合同到期与转正提醒会显示在这里。"
          compact
        />
        <div v-else class="mt-3 divide-y">
          <RouterLink
            v-for="item in visibleTodos"
            :key="item.label"
            :to="item.to"
            class="flex items-center gap-3 rounded-md py-4 text-sm hover:text-primary focus-visible:outline-2 focus-visible:outline-primary"
          >
            <span class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted text-lg" aria-hidden="true">{{ item.icon }}</span>
            <span class="min-w-0 flex-1">{{ item.label }}</span>
            <b class="tabular-nums">{{ item.value ?? '—' }}</b>
            <ChevronRight class="size-4 text-muted-foreground" aria-hidden="true" />
          </RouterLink>
        </div>
      </section>

      <section class="panel min-w-0 p-5" :class="{ 'xl:col-span-2': !canQueryEmployees }" aria-labelledby="hr-calendar-title">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <h2 id="hr-calendar-title" class="panel-title">人事日程</h2>
          <div class="flex items-center gap-2">
            <label for="hr-calendar-month" class="text-xs text-muted-foreground">月份</label>
            <Input id="hr-calendar-month" v-model="month" type="month" class="w-40" @change="loadCalendar" />
          </div>
        </div>
        <Callout v-if="calendarError" class="mt-4" title="日程未读取" tone="destructive">
          {{ calendarError }}
          <template #action>
            <Button variant="outline" size="sm" :disabled="calendarLoading" @click="loadCalendar">重试</Button>
          </template>
        </Callout>
        <p v-else-if="calendarLoading" class="py-8 text-center text-sm text-muted-foreground" role="status">正在读取日程…</p>
        <EmptyState v-else-if="!calendar.length" title="本月暂无人事日程" description="员工入职、转正及其他人事日程会显示在这里。" compact />
        <ul v-else class="mt-4 divide-y">
          <li v-for="(event, i) in calendar" :key="`${event.date}-${i}`" class="flex flex-wrap items-start gap-x-3 gap-y-2 py-4 text-sm">
            <span class="w-24 shrink-0 pt-0.5 text-xs tabular-nums text-muted-foreground">{{ event.date }}</span>
            <div class="min-w-0 flex-1 basis-36 space-y-1.5">
              <Badge>{{ event.typeName }}</Badge>
              <p class="break-words leading-6">{{ event.content }}</p>
            </div>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { ArrowUpRight, ChevronRight, RefreshCw } from '@lucide/vue';
import { api } from '../../lib/api';
import { session } from '../../lib/session';
import { todayShanghai } from '../../lib/hr-workspace';
import { errText } from '../../lib/toast';
import PageHeader from '../../ui/PageHeader.vue';
import Button from '../../ui/Button.vue';
import Badge from '../../ui/Badge.vue';
import Input from '../../ui/Input.vue';
import Callout from '../../ui/Callout.vue';
import EmptyState from '../../ui/EmptyState.vue';

interface Summary {
  employeeSurvey: Record<string, number>;
  recruitSurvey: Record<string, number>;
  todoSurvey: Record<string, number>;
}
const router = useRouter();
const summary = ref<Summary | null>(null), loading = ref(false), error = ref('');
const probationCount = ref<number | null>(null), probationLoading = ref(false), probationError = ref('');
const month = ref(todayShanghai().slice(0, 7));
const calendar = ref<{ date: string; typeName: string; content: string }[]>([]);
const calendarError = ref(''), calendarLoading = ref(false);
const refreshing = computed(() => loading.value || probationLoading.value || calendarLoading.value);
const canQueryEmployees = computed(() => session.has('hrm:employee:query'));
const canEmployment = computed(() => canQueryEmployees.value && session.has('hrm:employee:update') && session.has('hrm:contract-template:query'));
const currentMonthLabel = computed(() => {
  const [year, month] = todayShanghai().split('-');
  return `${year} 年 ${Number(month)} 月`;
});
const employeeTarget = (to: string) => canQueryEmployees.value ? to : undefined;
const cards = computed(() => [
  { label: '在职员工', value: summary.value?.employeeSurvey.activeCount, loading: loading.value, to: employeeTarget('/page/hrm-employee?roster=active'), icon: '👥', tone: 'bg-info-soft text-info', note: '当前在职人数' },
  { label: '试用期员工', value: probationCount.value, loading: probationLoading.value, to: employeeTarget('/page/hrm-employee?statusCategory=2'), icon: '⏳', tone: 'bg-warning-soft text-warning', note: '在职试用人数' },
  { label: '本月入职', value: summary.value?.employeeSurvey.entryThisMonthCount, loading: loading.value, to: employeeTarget('/page/hrm-employee?surveyType=1'), icon: '📥', tone: 'bg-success-soft text-success', note: '本月已入职' },
  { label: '本月离职', value: summary.value?.employeeSurvey.leaveThisMonthCount, loading: loading.value, to: employeeTarget('/page/hrm-employee?surveyType=2'), icon: '📤', tone: 'bg-muted text-muted-foreground', note: '本月已离职' }
]);
const recruitTarget = computed(() => [
  { permission: 'hrm:recruit:candidate:query', to: '/hr/recruit?tab=candidates' },
  { permission: 'hrm:recruit:interview:query', to: '/hr/recruit?tab=interviews' },
  { permission: 'hrm:recruit:post:query', to: '/hr/recruit?tab=posts' }
].find(item => session.has(item.permission))?.to);
const modules = computed(() => [
  { title: '招聘管理', icon: '🧑‍💼', description: '跟进候选人与简历，安排面试、记录反馈。', action: '查看招聘进度', to: recruitTarget.value },
  { title: '花名册', icon: '📒', description: '查询在职与离职员工，维护组织与任职资料。', action: '查看员工档案', to: employeeTarget('/page/hrm-employee?roster=active') },
  { title: '入离职与合同', icon: '📄', description: '办理入职与离职，生成文书并完成签订归档。', action: canEmployment.value ? '进入办理' : '查看合同模板', to: canEmployment.value ? '/hr/employment?mode=onboard' : session.has('hrm:contract-template:query') ? '/hr/templates' : undefined },
  { title: '员工生命周期', icon: '🔄', description: '串联入职、试用、转正、绩效与任职变更。', action: '查看员工历程', to: employeeTarget('/hr/lifecycle') }
].filter((item): item is typeof item & { to: string } => Boolean(item.to)));
const todos = computed(() => [
  { label: '待入职确认', value: summary.value?.todoSurvey.toEntryCount, to: '/page/hrm-employee?entryStatus=2&todoType=5', icon: '📝' },
  { label: '待离职办理', value: summary.value?.todoSurvey.toLeaveCount, to: '/page/hrm-employee?entryStatus=3&todoType=2', icon: '📤' },
  { label: '合同本月到期', value: summary.value?.todoSurvey.toExpireContractCount, to: '/page/hrm-employee?todoType=3', icon: '📑' },
  { label: '本月待转正', value: summary.value?.todoSurvey.toRegularCount, to: '/page/hrm-employee?roster=active&todoType=4', icon: '🌱' }
]);
const visibleTodos = computed(() => todos.value.filter(item => item.value !== 0));

let summaryRequest = 0;
async function loadSummary() {
  const request = ++summaryRequest;
  loading.value = true;
  error.value = '';
  try {
    const result = await api.get<Summary>('/admin-api/hrm/home/hr-statistics-summary');
    if (request === summaryRequest) summary.value = result;
  } catch (e) {
    if (request === summaryRequest) {
      summary.value = null;
      error.value = errText(e);
    }
  } finally {
    if (request === summaryRequest) loading.value = false;
  }
}
let probationRequest = 0;
async function loadProbation() {
  const request = ++probationRequest;
  probationCount.value = null;
  probationError.value = '';
  if (!canQueryEmployees.value) return;
  probationLoading.value = true;
  try {
    const result = await api.get<{ total: number }>('/admin-api/hrm/employee/page', { pageNo: 1, pageSize: 1, statusCategory: 2 });
    if (request === probationRequest) probationCount.value = result.total;
  } catch (e) {
    if (request === probationRequest) probationError.value = errText(e);
  } finally {
    if (request === probationRequest) probationLoading.value = false;
  }
}
let calendarRequest = 0;
async function loadCalendar() {
  const request = ++calendarRequest;
  calendarError.value = '';
  calendarLoading.value = true;
  try {
    if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(month.value)) throw new Error('请选择月份');
    const [y, m] = month.value.split('-').map(Number);
    const endDate = `${month.value}-${new Date(y!, m!, 0).getDate()}`;
    const result = await api.get<typeof calendar.value>('/admin-api/hrm/home/hr-calendar', { startDate: `${month.value}-01`, endDate });
    if (request === calendarRequest) calendar.value = result;
  } catch (e) {
    if (request === calendarRequest) {
      calendar.value = [];
      calendarError.value = errText(e);
    }
  } finally {
    if (request === calendarRequest) calendarLoading.value = false;
  }
}
async function refresh() {
  await Promise.allSettled([loadSummary(), loadProbation(), loadCalendar()]);
}
onMounted(() => void refresh());
</script>
