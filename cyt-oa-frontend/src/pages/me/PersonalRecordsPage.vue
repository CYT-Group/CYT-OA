<template>
  <div class="min-w-0 space-y-5">
    <PageHeader :title="currentTab.label" :description="currentTab.description">
      <template #actions>
        <Button variant="outline" :loading="loading" @click="refresh(true)"><RefreshCw class="size-4" />刷新</Button>
      </template>
    </PageHeader>
    <SegmentedTabs :model-value="tab" :items="tabs" @update:model-value="changeTab" />

    <template v-if="tab === 'attendance'">
      <div class="flex flex-wrap items-end gap-3">
        <div class="space-y-1.5">
          <label for="records-month" class="field-label">考勤月份</label>
          <Input id="records-month" :model-value="month" type="month" class="w-44" @update:model-value="changeFilter('month', String($event))" />
        </div>
        <div class="w-40 space-y-1.5">
          <label for="records-status" class="field-label">考勤状态</label>
          <Select id="records-status" :model-value="attendanceStatus" :options="attendanceStatuses" @update:model-value="changeFilter('status', String($event ?? ''))" />
        </div>
        <Button variant="soft" @click="router.push('/me/apply?new=leave')">申请请假</Button>
      </div>
      <Callout v-if="summaryError" title="考勤概况未读取" tone="destructive">{{ summaryError }}</Callout>
      <section class="grid grid-cols-2 gap-3 xl:grid-cols-4" aria-label="本月考勤概况">
        <div v-for="stat in attendanceStats" :key="stat.label" class="panel p-4 sm:p-5">
          <p class="text-xs text-muted-foreground">{{ stat.label }}</p>
          <p class="mt-3 text-2xl font-semibold tabular-nums">{{ summaryLoading ? '…' : (stat.value ?? '—') }}</p>
          <p class="mt-2 text-xs text-muted-foreground">所选月份的记录数</p>
        </div>
      </section>
      <section class="panel overflow-hidden">
        <div class="panel-header"><h2 class="panel-title">考勤与请假记录</h2><span class="text-xs text-muted-foreground">共 {{ attendanceTotal }} 条</span></div>
        <p v-if="loading" class="py-10 text-center text-sm text-muted-foreground" role="status">正在读取考勤记录…</p>
        <Callout v-else-if="error" class="m-4" title="记录未读取" tone="destructive">{{ error }}</Callout>
        <EmptyState v-else-if="!attendance.length" title="该月份暂无匹配记录" description="打卡、请假与出差记录会按实际状态显示在这里" compact />
        <div v-else class="overflow-x-auto">
          <table class="data-table min-w-[560px]">
            <thead><tr><th scope="col">考勤时间</th><th scope="col">类型</th><th scope="col">状态</th><th scope="col">备注</th></tr></thead>
            <tbody><tr v-for="row in attendance" :key="row.id">
              <td class="whitespace-nowrap tabular-nums">{{ fmtTime(row.attendanceTime) }}</td>
              <td>{{ attendanceTypes[row.type] ?? '其他' }}</td>
              <td><Badge :variant="attendanceTone(row.status)">{{ attendanceStatusLabels[row.status] ?? '未分类' }}</Badge></td>
              <td class="max-w-sm whitespace-normal break-words">{{ row.remark || '—' }}</td>
            </tr></tbody>
          </table>
        </div>
        <div v-if="attendanceTotal > pageSize" class="border-t p-4"><Pagination :page="page" :page-size="pageSize" :total="attendanceTotal" @update:page="changePage" /></div>
      </section>
    </template>

    <template v-else-if="tab === 'training'">
      <div class="flex flex-wrap gap-3">
        <Input v-model="trainingQuery" aria-label="搜索培训课程" placeholder="搜索课程、分类或讲师" class="sm:max-w-sm" />
        <Select v-model="trainingState" :options="trainingStates" aria-label="培训状态" class="sm:w-40" />
      </div>
      <p v-if="loading" class="py-10 text-center text-sm text-muted-foreground" role="status">正在读取培训记录…</p>
      <Callout v-else-if="error" title="培训课程未读取" tone="destructive">{{ error }}</Callout>
      <template v-else>
        <Callout v-if="trainingProgressError" title="学习时间未读取" tone="destructive">{{ trainingProgressError }}；课程与课时进度仍可查看。</Callout>
        <div class="flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-muted-foreground">
          <span>可见课程 {{ courses.length }} 门</span><span>已完成 {{ courses.filter(course => course.completed).length }} 门</span><span>当前匹配 {{ filteredCourses.length }} 门</span>
        </div>
        <EmptyState v-if="!filteredCourses.length" :title="trainingQuery || trainingState ? '没有匹配的培训课程' : '暂无可查看的培训课程'" description="已发布并对你开放的课程及本人学习记录会显示在这里" />
        <div v-else class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <section v-for="course in filteredCourses" :key="course.id" class="panel flex min-w-0 flex-col p-5">
            <div class="flex items-start justify-between gap-3"><Badge variant="outline">{{ course.category || '未分类' }}</Badge><Badge :variant="course.completed ? 'success' : course.progressPercent > 0 || course.startedAt ? 'info' : 'outline'">{{ course.completed ? '已完成' : course.progressPercent > 0 || course.startedAt ? '学习中' : '未开始' }}</Badge></div>
            <h2 class="mt-4 break-words text-sm font-semibold">{{ course.title }}</h2>
            <p v-if="course.description" class="mt-2 line-clamp-3 text-xs leading-6 text-muted-foreground">{{ course.description }}</p>
            <p class="mt-3 text-xs text-muted-foreground">{{ course.instructor ? `讲师 ${course.instructor} · ` : '' }}{{ course.lessonCount }} 课时</p>
            <div class="mt-4 flex-1">
              <div class="flex justify-between text-xs text-muted-foreground"><span>学习进度</span><span class="tabular-nums">{{ course.progressPercent }}%</span></div>
              <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-muted" role="progressbar" :aria-label="`${course.title}学习进度`" :aria-valuenow="course.progressPercent" aria-valuemin="0" aria-valuemax="100"><div class="h-full rounded-full bg-primary" :style="{ width: `${course.progressPercent}%` }" /></div>
              <p v-if="course.completedAt" class="mt-3 text-xs text-muted-foreground">完成于 {{ fmtDay(course.completedAt) }}</p>
              <p v-else-if="course.startedAt" class="mt-3 text-xs text-muted-foreground">开始于 {{ fmtDay(course.startedAt) }}</p>
            </div>
            <Button class="mt-5 w-full" variant="outline" @click="openCourse(course.id)">查看课时与记录</Button>
          </section>
        </div>
      </template>
    </template>

    <template v-else-if="isMaterialTab">
      <div class="flex flex-wrap gap-3">
        <Input v-model="materialQuery" aria-label="搜索申领记录" placeholder="搜索单号、物资名称或申领人" class="sm:max-w-sm" />
        <Select v-model="materialStatus" :options="materialStatuses" aria-label="申领状态" class="sm:w-40" />
        <Select v-if="tab === 'approvals'" v-model="materialKind" :options="[{ value: 'all', label: '全部物资' }, { value: 'office', label: '办公用品' }, { value: 'sample', label: '样品' }]" aria-label="物资类型" class="sm:w-40" />
      </div>
      <p v-if="loading" class="py-10 text-center text-sm text-muted-foreground" role="status">正在读取申领记录…</p>
      <Callout v-else-if="error" title="申领记录未读取" tone="destructive">{{ error }}</Callout>
      <EmptyState v-else-if="!filteredMaterials.length" :title="materialQuery || materialStatus ? '没有匹配的申领记录' : '暂无申领记录'" :description="tab === 'approvals' ? '当前管理站点内的物资审批记录会显示在这里' : '本人办公用品和样品的申领状态、明细与归还记录会显示在这里'" />
      <template v-else>
        <p class="text-xs text-muted-foreground">共 {{ filteredMaterials.length }} 条{{ tab === 'approvals' ? '；仅展示当前账号获授权的站点明细。' : '；物资浏览与申领请从业务入口操作。' }}</p>
        <article v-for="record in filteredMaterials" :key="record.id" class="panel overflow-hidden">
          <div class="flex flex-wrap items-start justify-between gap-3 p-5">
            <div class="min-w-0"><h2 class="break-words text-sm font-semibold">{{ record.applyNo || `申领记录 ${record.id}` }}</h2><p class="mt-1 text-xs text-muted-foreground">{{ fmtTime(record.createdAt) }}<span v-if="tab === 'approvals'"> · {{ record.applicantName }}<span v-if="record.department"> · {{ record.department }}</span></span></p><p v-if="record.purpose" class="mt-2 break-words text-sm">{{ record.purpose }}</p></div>
            <Badge :variant="materialTone(record.status)">{{ materialStatusLabels[record.status] || record.status }}</Badge>
          </div>
          <div class="overflow-x-auto border-t"><table class="data-table min-w-[600px]"><thead><tr><th scope="col">物资</th><th scope="col">规格</th><th scope="col">申请数量</th><th scope="col">批准数量</th><th scope="col">已归还</th><th scope="col">站点</th></tr></thead><tbody><tr v-for="item in record.items" :key="item.id"><td>{{ item.materialName }}<p v-if="item.materialSku" class="mt-1 text-xs text-muted-foreground">{{ item.materialSku }}</p></td><td>{{ item.spec || '—' }}</td><td>{{ item.quantity }}</td><td>{{ item.approvedQty ?? '—' }}</td><td>{{ item.returnedQuantity ?? 0 }}</td><td>{{ item.site || '—' }}</td></tr></tbody></table></div>
          <p v-if="record.expectReturnDate || record.actualReturnDate || record.remark" class="flex flex-wrap gap-x-5 gap-y-2 border-t p-4 text-xs text-muted-foreground"><span v-if="record.expectReturnDate">应还 {{ fmtDay(record.expectReturnDate) }}</span><span v-if="record.actualReturnDate">已还 {{ fmtDay(record.actualReturnDate) }}</span><span v-if="record.remark" class="break-words">{{ record.remark }}</span></p>
        </article>
      </template>
    </template>

    <template v-else>
      <p v-if="loading" class="py-10 text-center text-sm text-muted-foreground" role="status">正在读取已送达祝贺…</p>
      <template v-else>
        <Callout v-if="error" title="贺卡未读取" tone="destructive">{{ error }}</Callout>
        <section class="space-y-3" aria-labelledby="received-cards-title">
          <h2 id="received-cards-title" class="text-sm font-semibold">已送达贺卡</h2>
          <EmptyState v-if="!error && !cards.length" title="还没有收到贺卡" description="已送达的生日、入职周年与其他贺卡会保留在这里" compact />
          <div v-if="cards.length" class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            <article v-for="card in cards" :key="card.id" class="panel flex min-w-0 flex-col overflow-hidden">
              <div class="border-b bg-primary/6 p-5"><Badge variant="outline">{{ celebrationKinds[card.kind] || '祝贺' }}</Badge><h3 class="mt-3 break-words text-base font-semibold">{{ card.title }}</h3><p class="mt-1 text-xs text-muted-foreground">{{ fmtTime(card.sentAt) }} 已送达</p></div>
              <div class="flex flex-1 flex-col p-5"><p class="flex-1 whitespace-pre-wrap break-words text-sm leading-7">{{ card.message }}</p><a v-if="safeHref(card.url)" :href="safeHref(card.url)" target="_blank" rel="noopener noreferrer" class="mt-5 inline-flex w-fit items-center gap-1 text-xs font-medium text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-primary">查看贺卡<ExternalLink class="size-3.5" aria-hidden="true" /></a></div>
            </article>
          </div>
        </section>
        <section class="panel overflow-hidden">
          <div class="panel-header"><h2 class="panel-title">同事送来的祝福</h2></div>
          <Callout v-if="blessingsError" class="m-4" title="同事祝福未读取" tone="destructive">{{ blessingsError }}</Callout>
          <EmptyState v-else-if="!blessings.length" title="还没有收到同事祝福" description="同事送来的生日与周年祝福会显示在这里" compact />
          <ul v-else class="divide-y"><li v-for="blessing in blessings" :key="blessing.id" class="flex gap-3 p-5"><span aria-hidden="true" class="text-xl">💌</span><div class="min-w-0"><p class="whitespace-pre-wrap break-words text-sm leading-6">{{ blessing.note || '祝福已送达' }}</p><p class="mt-1 text-xs text-muted-foreground">{{ blessing.fromName || '同事' }} · {{ fmtTime(blessing.createdAt) }}</p></div></li></ul>
        </section>
      </template>
    </template>

    <Dialog :open="courseOpen" :title="courseDetail?.title || '课时与学习记录'" size="lg" @update:open="closeCourse">
      <p v-if="courseLoading" class="py-8 text-center text-sm text-muted-foreground" role="status">正在读取课时…</p>
      <Callout v-else-if="courseError" title="课时未读取" tone="destructive">{{ courseError }}</Callout>
      <template v-else-if="courseDetail">
        <p v-if="courseDetail.description" class="mb-4 whitespace-pre-wrap text-sm leading-7 text-muted-foreground">{{ courseDetail.description }}</p>
        <EmptyState v-if="!courseDetail.lessons.length" title="课程暂无课时" compact />
        <ul v-else class="divide-y"><li v-for="lesson in courseDetail.lessons" :key="lesson.id" class="flex items-start justify-between gap-4 py-4"><div class="min-w-0"><p class="break-words text-sm font-medium">{{ lesson.title }}</p><p class="mt-1 text-xs text-muted-foreground">{{ lessonKinds[lesson.kind] || '课件' }}<span v-if="lesson.my?.completedAt"> · 完成于 {{ fmtDay(lesson.my.completedAt) }}</span></p></div><Badge :variant="lesson.my?.status === 'completed' ? 'success' : 'outline'">{{ lesson.my?.status === 'completed' ? '已完成' : `${lesson.my?.percent ?? 0}%` }}</Badge></li></ul>
      </template>
      <template #footer><Button variant="outline" @click="closeCourse(false)">关闭</Button></template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ExternalLink, RefreshCw } from '@lucide/vue';
import { api } from '../../lib/api';
import { session } from '../../lib/session';
import { meApi, type RecognitionView } from '../../lib/me-api';
import { todayShanghai } from '../../lib/hr-workspace';
import { fmtDay, fmtTime } from '../../lib/format';
import { errText } from '../../lib/toast';
import type { BadgeTone } from '../../lib/tone';
import PageHeader from '../../ui/PageHeader.vue';
import Button from '../../ui/Button.vue';
import Badge from '../../ui/Badge.vue';
import Input from '../../ui/Input.vue';
import Select from '../../ui/Select.vue';
import SegmentedTabs from '../../ui/SegmentedTabs.vue';
import Callout from '../../ui/Callout.vue';
import EmptyState from '../../ui/EmptyState.vue';
import Pagination from '../../ui/Pagination.vue';
import Dialog from '../../ui/Dialog.vue';

interface Attendance { id: string; type: number; status: number; attendanceTime: string; remark?: string }
interface Course { id: string; title: string; description?: string; category: string; instructor?: string; lessonCount: number; progressPercent: number; completed: boolean; startedAt?: string; completedAt?: string }
interface TrainingProgress { trainingId: string; status: string; startedAt?: string; completedAt?: string }
interface Lesson { id: string; title: string; kind: string; my?: { status: string; percent: number; completedAt?: string } }
interface Card { id: string; kind: string; title: string; message: string; sentAt: string; url: string }
interface MaterialRecord { id: string; applyNo: string; applicantName: string; department?: string; purpose?: string; expectReturnDate?: string; actualReturnDate?: string; status: string; remark?: string; createdAt: string; items: { id: string; materialName: string; materialSku?: string; spec?: string; quantity: number; approvedQty?: number; returnedQuantity?: number; site?: string }[] }
const route = useRoute(), router = useRouter();
const allTabs = [
  { value: 'attendance', label: '考勤与请假', description: '按月份查看本人考勤与请假记录。' },
  { value: 'training', label: '培训记录', description: '查看对你开放的培训课程、课时与本人学习进度。' },
  { value: 'celebrations', label: '我的祝贺', description: '查看已送达贺卡与同事送来的祝福。' },
  { value: 'office', label: '办公用品申领记录', description: '查询本人办公用品的申请状态与发放明细。' },
  { value: 'sample', label: '样品申领记录', description: '查询本人样品的借用、归还与审批记录。' },
  { value: 'approvals', label: '物资审批记录', description: '查询获授权站点内的办公用品与样品审批记录。' }
];
const tab = computed(() => allTabs.some(item => item.value === route.query.tab) ? String(route.query.tab) : 'attendance');
const isMaterialTab = computed(() => ['office', 'sample', 'approvals'].includes(tab.value));
const tabs = computed(() => allTabs.filter(item => isMaterialTab.value ? ['office', 'sample'].includes(item.value) || item.value === 'approvals' && (tab.value === 'approvals' || session.isAdmin || session.has('can_approve_any_material_apply')) : ['attendance', 'training', 'celebrations'].includes(item.value)));
const currentTab = computed(() => allTabs.find(item => item.value === tab.value)!);
const month = computed(() => /^\d{4}-(0[1-9]|1[0-2])$/.test(String(route.query.month || '')) ? String(route.query.month) : todayShanghai().slice(0, 7));
const attendanceStatus = computed(() => /^[1-5]$/.test(String(route.query.status || '')) ? String(route.query.status) : '');
const page = computed(() => /^\d+$/.test(String(route.query.page || '')) ? Math.max(1, Number(route.query.page)) : 1);
const pageSize = 20;
const attendance = ref<Attendance[]>([]), attendanceTotal = ref(0);
const totals = ref<(number | null)[]>([null, null, null, null]);
const loading = ref(false), error = ref(''), summaryLoading = ref(false), summaryError = ref('');
const attendanceStatusLabels: Record<number, string> = { 1: '正常', 2: '迟到', 3: '早退', 4: '请假', 5: '出差' };
const attendanceTypes: Record<number, string> = { 1: '上班打卡', 2: '下班打卡', 3: '请假', 4: '出差' };
const attendanceStatuses = [{ value: '', label: '全部状态' }, ...Object.entries(attendanceStatusLabels).map(([value, label]) => ({ value, label }))];
const attendanceStats = computed(() => ['考勤记录', '正常记录', '迟到记录', '早退记录'].map((label, index) => ({ label, value: totals.value[index] })));
const trainingQuery = ref(''), trainingState = ref('');
const courses = ref<Course[]>([]), trainingProgressError = ref('');
const trainingStates = [{ value: '', label: '全部状态' }, { value: 'not-started', label: '未开始' }, { value: 'started', label: '学习中' }, { value: 'completed', label: '已完成' }];
const filteredCourses = computed(() => courses.value.filter(course => {
  const query = trainingQuery.value.trim().toLocaleLowerCase();
  const state = course.completed ? 'completed' : course.progressPercent > 0 || course.startedAt ? 'started' : 'not-started';
  return (!trainingState.value || state === trainingState.value) && (!query || `${course.title} ${course.category} ${course.instructor || ''}`.toLocaleLowerCase().includes(query));
}));
const cards = ref<Card[]>([]), blessings = ref<RecognitionView[]>([]), blessingsError = ref('');
const materialQuery = ref(''), materialStatus = ref(''), materialKind = ref('all'), materialRecords = ref<MaterialRecord[]>([]);
const materialStatusLabels: Record<string, string> = { pending: '待审批', borrowed: '借用中', issued: '已发放', returned: '已归还', rejected: '已驳回', closed: '已关闭' };
const materialStatuses = [{ value: '', label: '全部状态' }, ...Object.entries(materialStatusLabels).map(([value, label]) => ({ value, label }))];
const filteredMaterials = computed(() => materialRecords.value.filter(record => {
  const query = materialQuery.value.trim().toLocaleLowerCase();
  return (!materialStatus.value || record.status === materialStatus.value) && (!query || `${record.applyNo} ${record.applicantName} ${record.items.map(item => `${item.materialName} ${item.materialSku || ''}`).join(' ')}`.toLocaleLowerCase().includes(query));
}));
const celebrationKinds: Record<string, string> = { birthday: '生日祝贺', anniversary: '入职周年', promotion: '晋升祝贺', custom: '特别祝贺' };
const lessonKinds: Record<string, string> = { video: '视频', pdf: 'PDF 文档', article: '图文', cards: '图文卡片', link: '外部课件' };
const courseOpen = ref(false), courseLoading = ref(false), courseError = ref('');
const courseDetail = ref<{ title: string; description?: string; lessons: Lesson[] } | null>(null);

function attendanceTone(status: number): BadgeTone { return status === 1 ? 'success' : status === 2 || status === 3 ? 'warning' : 'info'; }
function materialTone(status: string): BadgeTone { return status === 'pending' ? 'warning' : status === 'rejected' ? 'destructive' : status === 'issued' || status === 'returned' ? 'success' : 'outline'; }
function changeTab(next: string) { void router.replace({ path: route.path, query: { tab: next } }); }
function changeFilter(key: string, value: string) {
  const query = { ...route.query };
  query.tab = 'attendance';
  delete query.page;
  if (value) query[key] = value; else delete query[key];
  void router.replace({ path: route.path, query });
}
function changePage(next: number) { void router.replace({ path: route.path, query: { ...route.query, page: String(next) } }); }
function monthParams() {
  const [year, numericMonth] = month.value.split('-').map(Number);
  const end = new Date(Date.UTC(year!, numericMonth!, 0)).getUTCDate();
  return { 'attendanceTime[0]': `${month.value}-01 00:00:00`, 'attendanceTime[1]': `${month.value}-${end} 23:59:59` };
}
function safeHref(value: string) {
  try { const url = new URL(value, window.location.origin); return /^https?:$/.test(url.protocol) && !url.username && !url.password ? url.href : undefined; } catch { return undefined; }
}

let requestId = 0, summaryMonth = '';
async function refresh(force = false) {
  const request = ++requestId;
  loading.value = true;
  error.value = '';
  try {
    if (tab.value === 'attendance') {
      const params = monthParams();
      const selectedMonth = month.value;
      const needsSummary = force || summaryMonth !== selectedMonth || Boolean(summaryError.value);
      if (needsSummary) { summaryLoading.value = true; summaryError.value = ''; }
      const [records, summary] = await Promise.allSettled([
        api.get<{ list: Attendance[]; total: number }>('/admin-api/oa/attendance/my-page', { ...params, status: attendanceStatus.value || undefined, pageNo: page.value, pageSize }),
        needsSummary ? Promise.all([undefined, 1, 2, 3].map(status => api.get<{ total: number }>('/admin-api/oa/attendance/my-page', { ...params, status, pageNo: 1, pageSize: 1 }))) : Promise.resolve(null)
      ]);
      if (request !== requestId) return;
      if (records.status === 'fulfilled') { attendance.value = records.value.list; attendanceTotal.value = Number(records.value.total); }
      else { attendance.value = []; attendanceTotal.value = 0; error.value = errText(records.reason); }
      if (summary.status === 'fulfilled' && summary.value) { totals.value = summary.value.map(item => Number(item.total)); summaryMonth = selectedMonth; }
      else if (summary.status === 'rejected') { totals.value = [null, null, null, null]; summaryError.value = errText(summary.reason); }
      summaryLoading.value = false;
    } else if (tab.value === 'training') {
      const [catalog, progress] = await Promise.allSettled([
        api.get<{ category: string; courses: Course[] }[]>('/api/portal/training/catalog'),
        api.get<TrainingProgress[]>('/api/portal/training/my-progress')
      ]);
      if (request !== requestId) return;
      trainingProgressError.value = progress.status === 'rejected' ? errText(progress.reason) : '';
      if (catalog.status === 'rejected') { courses.value = []; error.value = errText(catalog.reason); }
      else {
        const mine = progress.status === 'fulfilled' ? progress.value : [];
        courses.value = catalog.value.flatMap(group => group.courses.map(course => {
          const own = mine.filter(item => String(item.trainingId) === String(course.id));
          const completedAt = own.map(item => item.completedAt).filter(Boolean).sort().at(-1);
          const startedAt = own.map(item => item.startedAt).filter(Boolean).sort().at(0);
          const completed = Boolean(course.completed || own.some(item => item.status === 'completed'));
          return { ...course, category: group.category, completed, completedAt, startedAt, progressPercent: completed ? 100 : Math.max(0, Math.min(100, Number(course.progressPercent) || 0)) };
        }));
      }
    } else if (isMaterialTab.value) {
      const approvals = tab.value === 'approvals';
      const result = await api.get<MaterialRecord[]>(approvals ? '/api/workspace/materials-approval-records' : '/api/workspace/material-records', { kind: approvals ? materialKind.value : tab.value });
      if (request === requestId) materialRecords.value = result;
    } else {
      const [received, recognitions] = await Promise.allSettled([
        api.get<Card[]>('/api/workspace/celebrations'), meApi.recognitions()
      ]);
      if (request !== requestId) return;
      if (received.status === 'fulfilled') cards.value = received.value;
      else { cards.value = []; error.value = errText(received.reason); }
      if (recognitions.status === 'fulfilled') { blessings.value = recognitions.value.filter(item => item.type === 'blessing'); blessingsError.value = ''; }
      else { blessings.value = []; blessingsError.value = errText(recognitions.reason); }
    }
  } catch (e) { if (request === requestId) error.value = errText(e); }
  finally { if (request === requestId) { loading.value = false; summaryLoading.value = false; } }
}
let courseRequest = 0;
async function openCourse(id: string) {
  const request = ++courseRequest;
  courseOpen.value = true; courseLoading.value = true; courseError.value = ''; courseDetail.value = null;
  try {
    const result = await api.get<typeof courseDetail.value>('/api/portal/training/get', { id });
    if (request === courseRequest) courseDetail.value = result;
  } catch (e) { if (request === courseRequest) courseError.value = errText(e); }
  finally { if (request === courseRequest) courseLoading.value = false; }
}
function closeCourse(open: boolean) { if (!open) { ++courseRequest; courseOpen.value = false; courseDetail.value = null; courseLoading.value = false; } }
watch(() => route.fullPath, () => { closeCourse(false); void refresh(); }, { immediate: true });
watch(materialKind, () => { if (tab.value === 'approvals') void refresh(); });
</script>
