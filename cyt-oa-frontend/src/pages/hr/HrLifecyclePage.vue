<template>
  <div class="min-w-0 space-y-4">
    <PageHeader
      title="员工生命周期"
      description="按员工查看入职、任职变更、合同与归档记录。"
    />
    <div class="lifecycle-layout">
      <aside
        class="lifecycle-people panel"
        aria-label="生命周期员工列表"
        :aria-busy="peopleLoading"
      >
        <form
          class="shrink-0 space-y-3 border-b p-4"
          @submit.prevent="applyPeopleFilters"
        >
          <h2 class="text-sm font-semibold">
            员工列表
            <span
              class="ml-1 text-xs font-normal tabular-nums text-muted-foreground"
              >{{ total }} 人</span
            >
          </h2>
          <Input
            v-model="search"
            name="employee-search"
            aria-label="搜索生命周期员工"
            placeholder="搜索姓名、工号…"
            autocomplete="off"
          />
          <div class="flex min-w-0 gap-2">
            <Select
              v-model="entryStatus"
              aria-label="任职状态"
              :options="[
                { value: '', label: '全部员工' },
                { value: '0', label: '待完善' },
                { value: '1', label: '在职' },
                { value: '2', label: '待入职' },
                { value: '3', label: '待离职' },
                { value: '4', label: '已离职' }
              ]"
              class="min-w-0 flex-1"
            />
            <Button variant="outline" type="submit" :loading="peopleLoading"
              >查询</Button
            >
          </div>
        </form>
        <div class="lifecycle-people-scroll" aria-live="polite">
          <Callout v-if="peopleError" tone="destructive" class="m-3">
            {{ peopleError }}
            <template #action
              ><Button variant="outline" size="sm" @click="loadPeople"
                >重试</Button
              ></template
            >
          </Callout>
          <p
            v-else-if="peopleLoading"
            class="p-5 text-xs text-muted-foreground"
            role="status"
          >
            正在读取员工…
          </p>
          <EmptyState
            v-else-if="!people.length"
            title="暂无匹配员工"
            description="试试其他姓名、工号或任职状态。"
            compact
          />
          <ul v-else class="divide-y">
            <li v-for="person in people" :key="person.id">
              <button
                type="button"
                class="lifecycle-person"
                :class="
                  String(person.id) === selectedId
                    ? 'lifecycle-person-active'
                    : ''
                "
                :aria-pressed="String(person.id) === selectedId"
                @click="chooseEmployee(String(person.id))"
              >
                <Avatar :name="person.name" />
                <span class="min-w-0 flex-1">
                  <span class="flex min-w-0 items-center justify-between gap-2">
                    <span
                      class="truncate text-sm font-medium"
                      :title="person.name"
                      >{{ person.name }}</span
                    >
                    <span class="shrink-0 text-[11px] text-muted-foreground">{{
                      entryLabels[person.entryStatus] || '待完善'
                    }}</span>
                  </span>
                  <span
                    class="mt-1 block truncate text-xs text-muted-foreground"
                    :title="`${person.deptName || '未分配部门'} · ${person.postName || '未维护岗位'}`"
                    >{{ person.deptName || '未分配部门' }} ·
                    {{ person.postName || '未维护岗位' }}</span
                  >
                </span>
              </button>
            </li>
          </ul>
        </div>
        <Pagination
          class="shrink-0 border-t px-3 py-2"
          :page="page"
          :page-size="20"
          :total="total"
          :disabled="peopleLoading"
          compact
          @update:page="changePage"
        />
      </aside>
      <section
        class="lifecycle-detail panel"
        aria-label="员工历程详情"
        :aria-busy="loading"
      >
        <Callout v-if="error" tone="destructive" class="m-4">
          {{ error }}
          <template #action
            ><Button variant="outline" size="sm" @click="select(selectedId)"
              >重试</Button
            ></template
          >
        </Callout>
        <p
          v-if="loading || (peopleLoading && !employee && !error)"
          class="p-8 text-sm text-muted-foreground"
          role="status"
        >
          {{ loading ? '正在汇总员工历程…' : '正在读取员工…' }}
        </p>
        <EmptyState
          v-else-if="!employee && !error"
          :title="people.length ? '选择一名员工查看历程' : '暂无可查看的员工'"
          description="在员工列表中选择员工，查看对应档案和历程记录。"
        />
        <template v-if="employee && !loading">
          <header class="shrink-0 border-b p-5">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div class="flex min-w-0 flex-1 items-center gap-3">
                <Avatar :name="employee.name" />
                <div class="min-w-0">
                  <div class="flex flex-wrap items-center gap-2">
                    <h2 class="break-words text-lg font-semibold">
                      {{ employee.name }}
                    </h2>
                    <Badge>{{
                      entryLabels[employee.entryStatus] || '待完善'
                    }}</Badge>
                  </div>
                  <p
                    class="mt-1 break-words text-xs leading-relaxed text-muted-foreground"
                  >
                    {{ employee.deptName || '未分配部门' }} ·
                    {{ employee.postName || '未维护岗位' }}
                  </p>
                </div>
              </div>
              <RouterLink
                :to="{
                  path: '/page/hrm-employee',
                  query: { employeeId: employee.id }
                }"
                class="inline-flex h-8 shrink-0 items-center rounded-lg border bg-card px-3 text-xs font-medium shadow-xs hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >查看完整档案</RouterLink
              >
            </div>
            <dl
              class="mt-4 grid grid-cols-2 gap-x-5 gap-y-3 text-xs sm:grid-cols-3"
            >
              <div class="min-w-0">
                <dt class="text-muted-foreground">工号</dt>
                <dd class="mt-1 break-words font-medium">
                  {{ employee.jobNumber || '未登记' }}
                </dd>
              </div>
              <div class="min-w-0">
                <dt class="text-muted-foreground">入职日期</dt>
                <dd class="mt-1 font-medium tabular-nums">
                  {{ employee.entryTime?.slice(0, 10) || '待完善' }}
                </dd>
              </div>
              <div class="min-w-0">
                <dt class="text-muted-foreground">直属上级</dt>
                <dd class="mt-1 break-words font-medium">
                  {{ employee.leaderEmployeeName || '未维护' }}
                </dd>
              </div>
            </dl>
            <Button
              v-if="employee.entryStatus === 4"
              variant="outline"
              size="sm"
              class="mt-3"
              @click="certificate"
              >下载离职证明</Button
            >
          </header>
          <div
            class="flex shrink-0 flex-wrap items-center justify-between gap-3 border-b px-5 py-3"
          >
            <SegmentedTabs
              v-model="detailTab"
              aria-label="员工详情视图"
              :items="[
                { value: 'timeline', label: '历程记录', count: events.length },
                {
                  value: 'contracts',
                  label: '合同登记',
                  count: contracts.length
                }
              ]"
            />
            <Select
              v-if="detailTab === 'timeline'"
              v-model="filter"
              aria-label="历程类型"
              class="w-36"
              :options="[
                { value: '', label: '全部记录' },
                { value: 'entry', label: '入职' },
                { value: 'change', label: '任职变更' },
                { value: 'performance', label: '绩效归档' },
                { value: 'document', label: '文书归档' },
                { value: 'leave', label: '离职' }
              ]"
            />
          </div>
          <div class="lifecycle-detail-scroll p-5">
            <section v-if="detailTab === 'timeline'" aria-label="历程记录">
              <h3 class="sr-only">历程记录</h3>
              <EmptyState
                v-if="!filteredEvents.length"
                :title="filter ? '暂无此类历程记录' : '暂无已登记历程'"
                :description="
                  filter
                    ? '可切换到全部记录查看其他类型的历程。'
                    : '登记入职、任职变更或完成文书归档后，记录会显示在这里。'
                "
                compact
              >
                <Button
                  v-if="filter"
                  variant="outline"
                  size="sm"
                  @click="filter = ''"
                  >查看全部记录</Button
                >
              </EmptyState>
              <ol v-else class="ml-2 space-y-6 border-l pl-5">
                <li
                  v-for="(event, i) in filteredEvents"
                  :key="`${event.type}-${i}`"
                  class="relative"
                >
                  <span
                    aria-hidden="true"
                    class="absolute -left-[25px] top-1.5 size-2 rounded-full bg-primary ring-4 ring-card"
                  />
                  <p class="text-xs tabular-nums text-muted-foreground">
                    {{ dateText(event.eventTime) }}
                  </p>
                  <h4 class="mt-1 text-sm font-semibold">
                    {{ event.title
                    }}<Badge
                      v-if="event.type === 'change' && !event.appliedTime"
                      class="ml-2"
                      >计划生效</Badge
                    >
                  </h4>
                  <p
                    class="mt-2 whitespace-pre-wrap break-words text-sm leading-relaxed text-muted-foreground"
                  >
                    {{ event.description || '—' }}
                  </p>
                  <p
                    v-if="event.oldPostName || event.newPostName"
                    class="mt-1 text-xs"
                  >
                    岗位：{{ event.oldPostName || '—' }} →
                    {{ event.newPostName || '—' }}
                  </p>
                  <Button
                    v-if="event.documentId"
                    variant="link"
                    size="sm"
                    @click="openDocument(event.documentId)"
                    >查看归档文书</Button
                  >
                </li>
              </ol>
            </section>
            <section v-else aria-label="合同登记">
              <h3 class="sr-only">合同登记</h3>
              <EmptyState
                v-if="!contracts.length"
                title="暂无合同登记"
                description="该员工已登记的合同及期限将显示在这里。"
                compact
              />
              <div v-else class="overflow-x-auto">
                <table class="data-table min-w-[560px]">
                  <thead>
                    <tr>
                      <th scope="col">编号</th>
                      <th scope="col">签约公司</th>
                      <th scope="col">合同期限</th>
                      <th scope="col">签订日</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="contract in contracts" :key="contract.id">
                      <td>{{ contract.no || contract.id }}</td>
                      <td>{{ contract.signCompany || '—' }}</td>
                      <td>
                        {{ contract.startTime?.slice(0, 10) || '—' }} 至
                        {{ contract.endTime?.slice(0, 10) || '—' }}
                      </td>
                      <td>{{ contract.signTime?.slice(0, 10) || '—' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
          </div>
        </template>
      </section>
    </div>
    <Dialog
      :open="!!document"
      title="归档文书"
      size="lg"
      @update:open="!$event && (document = null)"
    >
      <pre
        class="whitespace-pre-wrap break-words font-sans text-sm leading-7"
        >{{ document?.content }}</pre
      >
      <template #footer>
        <Button
          variant="outline"
          @click="
            document &&
            textDownload(
              `${employee?.name || '员工'}-文书.txt`,
              document.content
            )
          "
          >下载</Button
        >
        <Button @click="document = null">关闭</Button>
      </template>
    </Dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../../lib/api';
import {
  hrBase,
  dateText,
  textDownload,
  type HrEmployee,
  type HrPage
} from '../../lib/hr-workspace';
import { errText } from '../../lib/toast';
import Avatar from '../../ui/Avatar.vue';
import Badge from '../../ui/Badge.vue';
import Button from '../../ui/Button.vue';
import Callout from '../../ui/Callout.vue';
import Dialog from '../../ui/Dialog.vue';
import EmptyState from '../../ui/EmptyState.vue';
import Input from '../../ui/Input.vue';
import PageHeader from '../../ui/PageHeader.vue';
import Pagination from '../../ui/Pagination.vue';
import Select from '../../ui/Select.vue';
import SegmentedTabs from '../../ui/SegmentedTabs.vue';
interface Event {
  type: string;
  title: string;
  eventTime: string;
  description: string;
  documentId?: string;
  oldPostName?: string;
  newPostName?: string;
  appliedTime?: string;
}
interface Contract {
  id: string;
  no: string;
  signCompany: string;
  startTime: string;
  endTime: string;
  signTime: string;
}
const route = useRoute(),
  router = useRouter(),
  search = ref(String(route.query.search || '')),
  entryStatus = ref(String(route.query.entryStatus || '')),
  page = computed(() => {
    const value = Number(route.query.peoplePage);
    return Number.isSafeInteger(value) && value > 0 ? value : 1;
  }),
  total = ref(0),
  people = ref<HrEmployee[]>([]),
  selectedId = ref(''),
  employee = ref<(HrEmployee & { leaderEmployeeName?: string }) | null>(null),
  events = ref<Event[]>([]),
  contracts = ref<Contract[]>([]),
  filter = computed({
    get: () => String(route.query.eventType || ''),
    set: (value: string) => {
      void router.replace({
        query: { ...route.query, eventType: value || undefined }
      });
    }
  }),
  detailTab = computed({
    get: () => (route.query.view === 'contracts' ? 'contracts' : 'timeline'),
    set: (value: string) => {
      void router.replace({
        query: {
          ...route.query,
          view: value === 'contracts' ? value : undefined
        }
      });
    }
  }),
  error = ref(''),
  peopleError = ref(''),
  loading = ref(false),
  peopleLoading = ref(false),
  document = ref<{ content: string } | null>(null);
const entryLabels: Record<number, string> = {
    0: '待完善',
    1: '在职',
    2: '待入职',
    3: '待离职',
    4: '已离职'
  },
  filteredEvents = computed(() =>
    events.value.filter((e) => !filter.value || e.type === filter.value)
  );
let peopleRequest = 0,
  detailRequest = 0;
function applyPeopleFilters() {
  const query = {
    ...route.query,
    search: search.value.trim() || undefined,
    entryStatus: entryStatus.value || undefined,
    peoplePage: undefined,
    employeeId: undefined
  };
  if (
    String(route.query.search || '') === (query.search || '') &&
    String(route.query.entryStatus || '') === (query.entryStatus || '') &&
    page.value === 1
  ) {
    void loadPeople();
  } else {
    void router.replace({ query });
  }
}
function changePage(value: number) {
  void router.replace({
    query: {
      ...route.query,
      peoplePage: value > 1 ? value : undefined,
      employeeId: undefined
    }
  });
}
function chooseEmployee(id: string) {
  if (String(route.query.employeeId || '') === id) return;
  void router.replace({ query: { ...route.query, employeeId: id } });
}
async function loadPeople() {
  const request = ++peopleRequest;
  peopleLoading.value = true;
  peopleError.value = '';
  try {
    const r = await api.get<HrPage<HrEmployee>>(
      '/admin-api/hrm/employee/page',
      {
        search: String(route.query.search || '') || undefined,
        entryStatus: String(route.query.entryStatus || '') || undefined,
        pageNo: page.value,
        pageSize: 20
      }
    );
    if (request === peopleRequest) {
      const lastPage = Math.max(1, Math.ceil(Number(r.total) / 20));
      if (page.value > lastPage) {
        changePage(lastPage);
        return;
      }
      people.value = r.list;
      total.value = Number(r.total);
      if (!route.query.employeeId && r.list.length)
        chooseEmployee(String(r.list[0].id));
    }
  } catch (e) {
    if (request === peopleRequest) {
      people.value = [];
      total.value = 0;
      peopleError.value = errText(e);
    }
  } finally {
    if (request === peopleRequest) peopleLoading.value = false;
  }
}
async function select(id: string) {
  if (!id) return;
  const request = ++detailRequest;
  selectedId.value = id;
  loading.value = true;
  error.value = '';
  employee.value = null;
  events.value = [];
  contracts.value = [];
  try {
    const results = await Promise.allSettled([
      api.get<HrEmployee>('/admin-api/hrm/employee/get', { id }),
      api.get<{ events: Event[]; contracts: Contract[] }>(
        `${hrBase}/lifecycle`,
        { employeeId: id }
      )
    ]);
    if (request !== detailRequest) return;
    for (const [i, r] of results.entries()) {
      if (r.status === 'rejected') throw r.reason;
      if (i === 0) employee.value = r.value as HrEmployee;
      else {
        const v = r.value as { events: Event[]; contracts: Contract[] };
        events.value = v.events;
        contracts.value = v.contracts;
      }
    }
  } catch (e) {
    if (request === detailRequest) {
      error.value = errText(e);
      employee.value = null;
    }
  } finally {
    if (request === detailRequest) loading.value = false;
  }
}
async function openDocument(id: string) {
  try {
    document.value = await api.get(`${hrBase}/documents/${id}`);
  } catch (e) {
    error.value = errText(e);
  }
}
async function certificate() {
  if (!employee.value) return;
  try {
    const r = await api.get<{ content: string }>(`${hrBase}/certificate`, {
      employeeId: employee.value.id
    });
    textDownload(`${employee.value.name}-离职证明.txt`, r.content);
  } catch (e) {
    error.value = errText(e);
  }
}
watch(
  () => route.query.employeeId,
  (id) => {
    if (id && String(id) !== selectedId.value) void select(String(id));
    else if (!id) {
      detailRequest++;
      selectedId.value = '';
      employee.value = null;
      events.value = [];
      contracts.value = [];
      loading.value = false;
      error.value = '';
    }
  },
  { immediate: true }
);
watch(
  () =>
    JSON.stringify([route.query.search, route.query.entryStatus, page.value]),
  () => {
    search.value = String(route.query.search || '');
    entryStatus.value = String(route.query.entryStatus || '');
    void loadPeople();
  },
  { immediate: true }
);
</script>

<style scoped>
.lifecycle-layout {
  display: grid;
  align-items: start;
  gap: 1rem;
  min-width: 0;
}
.lifecycle-people {
  display: flex;
  height: 21rem;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
}
.lifecycle-people-scroll,
.lifecycle-detail-scroll {
  min-height: 0;
  min-width: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}
.lifecycle-people-scroll {
  flex: 1;
}
.lifecycle-person {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  text-align: left;
  border-left: 3px solid transparent;
  transition:
    background-color 150ms ease,
    color 150ms ease;
}
.lifecycle-person:hover {
  background: var(--accent);
}
.lifecycle-person:focus-visible {
  outline: 2px solid var(--ring);
  outline-offset: -3px;
}
.lifecycle-person-active {
  border-left-color: var(--primary);
  background: color-mix(in srgb, var(--primary) 6%, var(--card));
  color: var(--primary);
}
.lifecycle-detail {
  display: flex;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
}
@media (min-width: 1024px) {
  .lifecycle-layout {
    --lifecycle-height: clamp(
      24rem,
      calc(100dvh - var(--workspace-topbar-height) - 9rem),
      52rem
    );
    grid-template-columns: 296px minmax(0, 1fr);
  }
  .lifecycle-people {
    height: var(--lifecycle-height);
  }
  .lifecycle-detail {
    max-height: var(--lifecycle-height);
  }
}
@media (prefers-reduced-motion: reduce) {
  .lifecycle-person {
    transition: none;
  }
}
</style>
