<template>
  <section class="panel overflow-hidden" aria-label="招聘概况" :aria-busy="loading">
    <header class="flex flex-wrap items-center justify-between gap-3 border-b px-5 py-3">
      <div>
        <h2 class="text-sm font-semibold">招聘概况</h2>
        <p class="mt-1 text-xs text-muted-foreground">全部招聘数据，点击阶段查看对应候选人。</p>
      </div>
      <Button variant="ghost" size="sm" :disabled="loading" @click="refresh">刷新概况</Button>
    </header>
    <div class="grid grid-cols-2 divide-x divide-y sm:grid-cols-4 sm:divide-y-0">
      <RouterLink
        v-for="card in cards"
        :key="card.label"
        :to="card.to"
        class="min-w-0 px-5 py-4 hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring"
      >
        <span class="text-xs text-muted-foreground">{{ card.label }}</span>
        <strong class="mt-2 block text-2xl font-semibold tabular-nums">{{ loading ? '…' : (card.value ?? '—') }}</strong>
      </RouterLink>
    </div>
    <Callout v-if="error" tone="destructive" class="mx-5 my-3">{{ error }}</Callout>
    <div v-if="session.has('hrm:recruit:candidate:query')" class="border-t px-5 py-4">
      <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
        <h3 class="text-xs font-medium">招聘阶段分布</h3>
        <button type="button" class="rounded text-xs text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring" @click="emit('select', '')">全部候选人</button>
      </div>
      <div class="grid grid-cols-2 gap-x-5 gap-y-2 xl:grid-cols-4">
        <button
          v-for="stage in stages"
          :key="stage.value"
          type="button"
          class="min-w-0 rounded-lg border px-3 py-2.5 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          :class="selectedStatus === stage.value ? 'border-primary bg-primary/8' : 'border-transparent hover:bg-accent'"
          :aria-pressed="selectedStatus === stage.value"
          :aria-label="`筛选${stage.label}候选人`"
          @click="emit('select', stage.value)"
        >
          <span class="flex items-center justify-between gap-2 text-xs">
            <span>{{ stage.label }}</span>
            <b class="tabular-nums">{{ loading ? '…' : (stage.count ?? '—') }}</b>
          </span>
          <span class="mt-2 block h-1.5 overflow-hidden rounded-full bg-muted" aria-hidden="true">
            <span class="block h-full rounded-full bg-primary/65" :style="{ width: `${stage.width}%` }" />
          </span>
        </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { api } from '../lib/api';
import { session } from '../lib/session';
import { candidateStages, hrBase, type HrPage } from '../lib/hr-workspace';
import { errText } from '../lib/toast';
import Button from '../ui/Button.vue';
import Callout from '../ui/Callout.vue';

defineProps<{ selectedStatus: string }>();
const emit = defineEmits<{ select: [status: string] }>();
interface StatusCount { status: number; count: number }
const candidates = ref<StatusCount[] | null>(null);
const posts = ref<StatusCount[] | null>(null);
const interviews = ref<number | null>(null);
const loading = ref(false), error = ref('');
const count = (items: StatusCount[] | null, status: number) =>
  items === null ? undefined : items.find(item => item.status === status)?.count ?? 0;
const cards = computed(() => [
  ...(session.has('hrm:recruit:post:query') ? [{ label: '在招岗位', value: count(posts.value, 1), to: '/hr/recruit?tab=posts&status=1' }] : []),
  ...(session.has('hrm:recruit:candidate:query') ? [{ label: '候选人数', value: candidates.value?.reduce((sum, item) => sum + Number(item.count), 0), to: '/hr/recruit?tab=candidates' }] : []),
  ...(session.has('hrm:recruit:interview:query') ? [{ label: '待面试', value: interviews.value, to: '/hr/recruit?tab=interviews&result=1' }] : []),
  ...(session.has('hrm:recruit:candidate:query') ? [{ label: '已发 offer', value: count(candidates.value, 5), to: '/hr/recruit?tab=candidates&status=5' }] : [])
]);
const stages = computed(() => {
  const max = Math.max(1, ...(candidates.value?.map(item => item.count) ?? []));
  return Object.entries(candidateStages).map(([value, label]) => {
    const total = count(candidates.value, Number(value));
    return { value, label, count: total, width: total === undefined ? 0 : total / max * 100 };
  });
});
let requestId = 0;
async function refresh() {
  const request = ++requestId;
  loading.value = true;
  error.value = '';
  const params = { pageNo: 1, pageSize: 1 };
  const results = await Promise.allSettled([
    session.has('hrm:recruit:candidate:query') ? api.get<StatusCount[]>('/admin-api/hrm/recruit/candidate/status-count', params) : Promise.resolve(null),
    session.has('hrm:recruit:post:query') ? api.get<StatusCount[]>('/admin-api/hrm/recruit/post/status-count', params) : Promise.resolve(null),
    session.has('hrm:recruit:interview:query') ? api.get<HrPage<unknown>>(`${hrBase}/interviews`, { ...params, result: 1 }).then(result => Number(result.total)) : Promise.resolve(null)
  ]);
  if (request !== requestId) return;
  const [candidateResult, postResult, interviewResult] = results;
  candidates.value = candidateResult!.status === 'fulfilled' ? candidateResult!.value : null;
  posts.value = postResult!.status === 'fulfilled' ? postResult!.value : null;
  interviews.value = interviewResult!.status === 'fulfilled' ? interviewResult!.value : null;
  const failures = results.flatMap((result, index) => result.status === 'rejected' ? [`${['候选人', '职位', '面试'][index]}：${errText(result.reason)}`] : []);
  error.value = failures.join('；');
  loading.value = false;
}
defineExpose({ refresh });
onMounted(() => { void refresh(); });
</script>
