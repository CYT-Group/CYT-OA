<template>
  <div>
    <PageHeader title="花名册" description="在职、离职员工与完整档案集中查询，企微资料随同步更新。">
      <template #actions>
        <Button v-if="session.has('hrm:employee:export')" variant="outline" size="sm" @click="exportEmployees">导出花名册</Button>
        <Button v-if="session.has('hrm:employee:update') && session.has('hrm:contract-template:query')" size="sm" @click="router.push('/hr/employment?mode=onboard')">办理入职</Button>
        <Button
          v-if="canSyncHr"
          variant="outline"
          size="sm"
          :disabled="hrSyncing || hrStatusLoading"
          @click="syncHr"
        >
          <LoaderCircle v-if="hrSyncing" class="size-3.5 animate-spin" />
          {{ hrSyncing ? '企微花名册同步中…' : '同步企微花名册' }}
        </Button>
        <Button v-if="session.isAdmin" variant="outline" size="sm" @click="router.push('/org')">
          <Network class="size-3.5" />
          组织架构
        </Button>
      </template>
    </PageHeader>
    <SegmentedTabs class="mt-4" :model-value="roster || (statusCategory === 2 ? 'probation' : 'all')" :items="[{ value: 'active', label: '在职员工' }, { value: 'probation', label: '试用员工' }, { value: 'left', label: '离职员工' }, { value: 'all', label: '全部档案' }]" @update:model-value="setRoster" />

    <Callout v-if="hrSyncError" class="mt-4" tone="destructive" title="同步启动未确认">
      {{ hrSyncError }}
      <template #action><Button variant="ghost" size="sm" :disabled="hrStatusLoading" @click="refreshHrSync">读取最新状态</Button></template>
    </Callout>
    <Callout v-if="hrStatusError" class="mt-4" tone="warning" title="同步记录未读取">
      {{ hrStatusError }}
      <template #action><Button variant="ghost" size="sm" :disabled="hrStatusLoading" @click="refreshHrSync">重新读取</Button></template>
    </Callout>
    <Callout v-if="departmentError" class="mt-4" tone="warning" title="部门选项未加载">
      {{ departmentError }}
      <template #action><Button variant="ghost" size="sm" @click="loadDepartments">重试部门选项</Button></template>
    </Callout>

    <section
      v-if="hrRun"
      class="mt-4 overflow-hidden rounded-xl border bg-card shadow-xs"
      aria-label="企微花名册同步记录"
    >
      <header class="flex flex-wrap items-start justify-between gap-3 px-4 py-3">
        <div class="min-w-0 w-full sm:w-auto sm:flex-1">
          <div class="flex flex-wrap items-center gap-2">
            <LoaderCircle v-if="hrRun.status === 'running'" class="size-4 animate-spin text-primary" aria-hidden="true" />
            <h2 class="text-sm font-semibold">{{ hrSyncTitle }}</h2>
            <Badge :variant="hrSyncTone">{{ hrSyncStatusLabel }}</Badge>
          </div>
          <p class="mt-1 text-xs leading-6 text-muted-foreground">
            {{ hrSyncStageLabel }}<template v-if="hrRun.actor"> · {{ hrRun.actor }}</template>
            · {{ syncTime(hrRun.finishedAt || hrRun.createdAt) }}
          </p>
        </div>
        <div class="flex w-full shrink-0 items-center justify-end gap-1 sm:w-auto">
          <Button variant="ghost" size="sm" :disabled="hrStatusLoading" @click="refreshHrSync">刷新记录</Button>
          <Button variant="ghost" size="sm" :aria-expanded="hrSyncExpanded" @click="hrSyncExpanded = !hrSyncExpanded">{{ hrSyncExpanded ? '收起详情' : '展开详情' }}</Button>
        </div>
      </header>
      <div v-if="hrSyncExpanded" class="space-y-4 border-t px-4 py-4">
        <Callout v-if="hrRun.message" :tone="hrSyncCalloutTone">{{ hrRun.message }}</Callout>
        <p v-if="hrRun.status === 'running'" class="text-xs leading-6 text-muted-foreground" role="status">
          {{ hrRun.stage === 'directory' ? '正在读取真实企微成员并建立或更新关联档案，随后读取人事信息。' : '通讯录阶段已完成，正在读取生日、入职、员工状态、试用期与转正信息。' }}
          可以离开页面，重新进入后会恢复同步记录。
        </p>
        <section v-if="hrRun.directory" aria-label="通讯录同步统计">
          <h3 class="text-xs font-semibold">通讯录阶段</h3>
          <dl class="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-5">
            <div v-for="metric in directoryMetrics" :key="metric.label" class="rounded-lg bg-muted/45 p-3">
              <dt class="text-xs text-muted-foreground">{{ metric.label }}</dt><dd class="mt-1 text-lg font-semibold tabular-nums">{{ metric.value ?? '—' }}</dd>
            </div>
          </dl>
        </section>
        <section v-if="hrResult" aria-label="人事同步统计">
          <h3 class="text-xs font-semibold">人事阶段</h3>
          <dl class="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div v-for="metric in hrMetrics" :key="metric.label" class="rounded-lg bg-muted/45 p-3">
              <dt class="text-xs text-muted-foreground">{{ metric.label }}</dt><dd class="mt-1 text-lg font-semibold tabular-nums">{{ metric.value ?? '—' }}</dd>
            </div>
          </dl>
          <p class="mt-3 text-xs leading-6 text-muted-foreground">检查档案数为成功、失败与跳过之和；实际读取为发往企微人事接口的请求数。无法读取或未关联的本地档案会保留，不会因此自动归为离职。</p>
        </section>
        <details v-if="hrResult?.failures?.length" open class="rounded-lg border border-destructive/20 p-3">
          <summary class="cursor-pointer text-xs font-semibold text-destructive">失败原因（{{ hrResult.failures.length }}）</summary>
          <ul class="mt-3 max-h-64 space-y-2 overflow-auto text-xs leading-6"><li v-for="(item, index) in hrResult.failures" :key="`${item.employeeId}-${index}`">{{ syncEmployee(item) }}：{{ item.reason }}</li></ul>
        </details>
        <details v-if="hrResult?.skippedEmployees?.length" open class="rounded-lg border border-warning/25 p-3">
          <summary class="cursor-pointer text-xs font-semibold text-warning">跳过原因（{{ hrResult.skippedEmployees.length }}）</summary>
          <ul class="mt-3 max-h-64 space-y-2 overflow-auto text-xs leading-6"><li v-for="(item, index) in hrResult.skippedEmployees" :key="`${item.employeeId}-${index}`">{{ syncEmployee(item) }}：{{ item.reason }}</li></ul>
        </details>
        <details v-if="hrResult?.conflicts?.length" open class="rounded-lg border border-warning/25 p-3">
          <summary class="cursor-pointer text-xs font-semibold text-warning">冲突明细（{{ hrResult.conflicts.length }}）</summary>
          <ul class="mt-3 max-h-64 space-y-2 overflow-auto text-xs leading-6"><li v-for="(item, index) in hrResult.conflicts" :key="`${item.employeeId}-${item.field}-${index}`">{{ syncEmployee(item) }}：{{ syncConflict(item) }}</li></ul>
        </details>
        <p v-if="hrRun.status === 'success'" class="text-xs leading-6 text-muted-foreground">本次同步已完成，尝试读取的员工均处理成功。</p>
      </div>
    </section>

    <Callout v-if="error" class="mt-4" tone="destructive" title="加载失败">
      {{ error }}
      <template #action>
        <Button variant="ghost" size="sm" class="text-destructive" @click="load">重试</Button>
      </template>
    </Callout>

    <section class="panel mt-4 overflow-hidden">
      <div class="table-toolbar">
        <form class="flex flex-wrap items-center gap-2" @submit.prevent="searchList">
          <Input
            v-model="search"
            class="h-9 w-40 text-[13px] sm:w-56"
            aria-label="搜索员工"
            placeholder="姓名、工号或手机号"
            data-page-search
          />
          <Select
            v-if="!roster && !statusCategory"
            size="sm"
            class="w-40"
            aria-label="任职状态"
            :model-value="entryStatus"
            :options="entryFilterOptions"
            @update:model-value="onEntryStatus"
          />
          <Select v-model="department" :options="departmentOptions" aria-label="员工部门" size="sm" class="w-40" />
          <Button type="submit" size="sm">
            <Search class="size-3.5" />
            查询
          </Button>
        </form>
        <div class="ml-auto hidden shrink-0 items-center gap-3 sm:flex">
          <span class="hidden text-[11px] text-muted-foreground lg:inline">按当前页隐藏多数为空的可选列</span>
          <span class="text-xs tabular-nums text-muted-foreground">共 {{ total }} 人</span>
        </div>
      </div>

      <div v-if="loading" class="border-t border-border/70 p-4 space-y-2">
        <Skeleton v-for="i in 6" :key="i" class="h-10 w-full" />
      </div>

      <EmptyState
        v-else-if="!rows.length && !error"
        :icon="Users"
        title="暂无员工档案"
        description="同步企微通讯录后自动建档"
        compact
        class="border-t border-border/70"
      />

      <div v-else class="max-h-[calc(100dvh-15rem)] overflow-auto border-t border-border/70">
        <table class="data-table min-w-[760px]">
          <thead>
            <tr>
              <th>
                <SortButton
                  label="员工 / 工号"
                  :active="sortKey === 'name'"
                  :direction="sortDir"
                  @sort="toggleSort('name')"
                />
              </th>
              <th v-if="optionalColumns.deptName">
                <SortButton
                  label="主部门"
                  :active="sortKey === 'deptName'"
                  :direction="sortDir"
                  @sort="toggleSort('deptName')"
                />
              </th>
              <th v-if="optionalColumns.postName">
                <SortButton
                  label="岗位"
                  :active="sortKey === 'postName'"
                  :direction="sortDir"
                  @sort="toggleSort('postName')"
                />
              </th>
              <th v-if="optionalColumns.leaderEmployeeName">
                <SortButton
                  label="直属上级"
                  :active="sortKey === 'leaderEmployeeName'"
                  :direction="sortDir"
                  @sort="toggleSort('leaderEmployeeName')"
                />
              </th>
              <th>
                <SortButton
                  label="任职状态"
                  :active="sortKey === 'entryStatus'"
                  :direction="sortDir"
                  @sort="toggleSort('entryStatus')"
                />
              </th>
              <th v-if="optionalColumns.entryTime">
                <SortButton
                  label="入职日期"
                  :active="sortKey === 'entryTime'"
                  :direction="sortDir"
                  @sort="toggleSort('entryTime')"
                />
              </th>
              <th class="sticky-col text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in sortedRows" :key="row.id">
              <td>
                <div class="flex items-center gap-2.5">
                  <Avatar :name="row.name" size="sm" />
                  <div class="min-w-0">
                    <p class="truncate text-sm font-medium">{{ row.name }}</p>
                    <p v-if="hasValue(row.jobNumber)" class="truncate text-[11px] text-muted-foreground">
                      {{ row.jobNumber }}
                    </p>
                  </div>
                </div>
              </td>
              <td v-if="optionalColumns.deptName" class="text-muted-foreground">
                {{ hasValue(row.deptName) ? row.deptName : '—' }}
              </td>
              <td v-if="optionalColumns.postName" class="text-muted-foreground">
                {{ hasValue(row.postName) ? row.postName : '—' }}
              </td>
              <td v-if="optionalColumns.leaderEmployeeName" class="text-muted-foreground">
                {{ hasValue(row.leaderEmployeeName) ? row.leaderEmployeeName : '—' }}
              </td>
              <td>
                <Badge :variant="row.entryStatus === 1 ? 'success' : 'info'">
                  {{ entryLabels[row.entryStatus ?? 0] || '待完善' }}
                </Badge>
                <p v-if="hasValue(row.status)" class="mt-1 text-xs text-muted-foreground">{{ statusLabels[row.status ?? 0] || '用工状态待完善' }}</p>
              </td>
              <td v-if="optionalColumns.entryTime" class="num text-muted-foreground">
                {{ hasValue(row.entryTime) ? date(row.entryTime) : '—' }}
              </td>
              <td class="sticky-col text-right">
                <Button variant="ghost" size="sm" @click="open(row.id)">查看档案</Button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="!loading && total > 20" class="border-t border-border/70 px-3 py-2">
        <Pagination :page="page" :page-size="20" :total="total" @update:page="go" />
      </div>
    </section>

    <Sheet
      :open="!!detail"
      side="right"
      class="max-w-2xl"
      :title="detail ? `${detail.name} · 人事档案` : ''"
      :description="
        detail ? `档案编号 ${detail.id} · ${detail.userId ? '已关联企微账号' : '未关联登录账号'}` : undefined
      "
      @update:open="
        (value) => {
          if (!value) close()
        }
      "
    >
      <div v-if="detail" class="px-5 py-4">
        <SegmentedTabs
          :model-value="tab"
          :items="[
            { value: '资料', label: '资料' },
            { value: '变更记录', label: '变更记录' }
          ]"
          @update:model-value="tab = $event"
        />

        <template v-if="tab === '资料'">
          <EmployeeLifecycleActions
            :key="detail.id + ':' + detail.entryStatus + ':' + detail.status"
            :employee="detail"
            @updated="refreshDetail"
          />
          <div class="mt-3 flex flex-wrap gap-2"><Button variant="outline" size="sm" @click="router.push(`/hr/lifecycle?employeeId=${detail.id}`)">员工生命周期与合同</Button><Button v-if="detail.entryStatus === 4" variant="outline" size="sm" @click="downloadCertificate">下载离职证明</Button></div>

          <h3 class="mt-6 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            企微资料
          </h3>
          <p v-if="detail.userId" class="mt-1 text-xs text-muted-foreground">生日、入职、任职与用工状态、试用期及转正信息按企微成功返回的值同步。</p>
          <dl class="mt-2 grid gap-x-6 gap-y-3 rounded-xl border bg-muted/20 p-4 sm:grid-cols-2">
            <div v-for="item in workFacts" :key="item.label">
              <dt class="text-[11px] text-muted-foreground">{{ item.label }}</dt>
              <dd class="mt-0.5 text-sm font-medium">{{ item.value }}</dd>
            </div>
          </dl>

          <div class="mt-6 flex flex-wrap items-center justify-between gap-2">
            <div>
              <h3 class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                人事补充信息
              </h3>
              <p class="mt-0.5 text-xs text-muted-foreground">
                首次登记后，任职状态通过入转调离流程维护。账号启停不会改变此状态。
              </p>
            </div>
            <Checkbox v-if="canEdit" v-model="showEmptyFields">补充空白字段</Checkbox>
          </div>

          <div class="mt-3 grid gap-4 sm:grid-cols-2">
            <div v-if="showEmptyFields || hasValue(detail.jobNumber)" class="flex flex-col">
              <Label>工号</Label>
              <Input v-model="form.jobNumber" :disabled="!canEdit" maxlength="64" />
            </div>

            <div v-if="showEmptyFields || hasValue(detail.leaderEmployeeId)" class="flex flex-col">
              <Label>直属上级</Label>
              <Input
                v-model="managerSearch"
                :disabled="!canEdit"
                placeholder="搜索姓名或工号"
                @input="findManagers"
              />
              <Select
                v-model="form.leaderEmployeeId"
                class="mt-2"
                :options="managerOptions"
                :disabled="!canEdit"
              />
            </div>

            <div v-if="showEmptyFields || hasValue(detail.entryStatus)" class="flex flex-col">
              <Label>任职状态</Label>
              <Select
                v-model="form.entryStatus"
                :options="entryFormOptions"
                :disabled="!canEdit || !unconfirmed"
              />
            </div>

            <div v-if="!detail.userId && (showEmptyFields || hasValue(detail.entryTime))" class="flex flex-col">
              <Label>入职日期</Label>
              <Input v-model="form.entryTime" type="date" :disabled="!canEdit || !unconfirmed" />
            </div>

            <div v-if="showEmptyFields || hasValue(detail.type)" class="flex flex-col">
              <Label>聘用形式</Label>
              <Select v-model="form.type" :options="typeFormOptions" :disabled="!canEdit || !unconfirmed" />
            </div>

            <div v-if="showEmptyFields || hasValue(detail.status)" class="flex flex-col">
              <Label>用工状态</Label>
              <Select
                v-model="form.status"
                :options="statusFormOptions"
                :disabled="!canEdit || !unconfirmed"
              />
            </div>

            <div v-if="!detail.userId && (showEmptyFields || hasValue(detail.birthday))" class="flex flex-col">
              <Label>生日（仅人事可见）</Label>
              <Input v-model="form.birthday" type="date" :disabled="!canEdit" />
            </div>

            <div v-if="showEmptyFields || hasValue(detail.postLevel)" class="flex flex-col">
              <Label>职级</Label>
              <Input v-model="form.postLevel" :disabled="!canEdit" maxlength="255" />
            </div>

            <div v-if="showEmptyFields || hasValue(detail.workCity)" class="flex flex-col">
              <Label>工作城市</Label>
              <Input v-model="form.workCity" :disabled="!canEdit" maxlength="64" />
            </div>

            <div v-if="showEmptyFields || hasValue(detail.workAddress)" class="flex flex-col">
              <Label>工作地址</Label>
              <Input v-model="form.workAddress" :disabled="!canEdit" maxlength="255" />
            </div>

            <div v-if="showEmptyFields || hasValue(detail.remark)" class="flex flex-col sm:col-span-2">
              <Label>备注</Label>
              <Textarea v-model="form.remark" :rows="2" :disabled="!canEdit" maxlength="500" />
            </div>
          </div>

          <details
            v-if="hasValue(detail.idNumber) || hasValue(detail.address)"
            class="mt-5 rounded-xl border px-4 py-3"
          >
            <summary class="cursor-pointer text-xs font-medium text-muted-foreground">受限个人资料</summary>
            <div class="mt-2 space-y-1 text-sm">
              <p v-if="hasValue(detail.idNumber)">证件号码：{{ detail.idNumber }}</p>
              <p v-if="hasValue(detail.address)">户籍地址：{{ detail.address }}</p>
            </div>
          </details>

          <Callout v-if="detailError" class="mt-4" tone="destructive">{{ detailError }}</Callout>
          <Callout v-if="saved" class="mt-4" tone="success">人事资料已保存</Callout>
        </template>

        <template v-else>
          <Callout v-if="historyError" class="mt-4" tone="destructive">{{ historyError }}</Callout>
          <EmptyState v-else-if="!history.length" :icon="History" title="暂无变更记录" />
          <ol v-else class="mt-4 space-y-4 border-l pl-4">
            <li v-for="h in history" :key="h.id" class="relative">
              <span
                class="absolute -left-[21px] top-1.5 size-2.5 rounded-full bg-primary ring-4 ring-background"
              />
              <div class="flex flex-wrap items-baseline gap-2">
                <b class="text-sm">{{ changeLabels[h.type] || '人事变更' }}</b>
                <small class="text-xs text-muted-foreground">{{ h.effectTime?.replace('T', ' ') }}</small>
              </div>
              <p v-if="h.oldDeptName || h.newDeptName" class="mt-1 text-xs text-muted-foreground">
                部门：{{ h.oldDeptName || '未分配' }} → {{ h.newDeptName || '未分配' }}
              </p>
              <p v-if="h.oldPostName || h.newPostName" class="mt-1 text-xs text-muted-foreground">
                岗位：{{ h.oldPostName || '—' }} → {{ h.newPostName || '—' }}
              </p>
              <p
                v-if="h.oldLeaderEmployeeName || h.newLeaderEmployeeName"
                class="mt-1 text-xs text-muted-foreground"
              >
                直属上级：{{ h.oldLeaderEmployeeName || '未维护' }} →
                {{ h.newLeaderEmployeeName || '未维护' }}
              </p>
              <p v-if="h.remark" class="mt-1 text-sm">{{ h.remark }}</p>
            </li>
          </ol>
        </template>
      </div>

      <template #footer>
        <Button variant="outline" @click="close">关闭</Button>
        <Button v-if="canEdit && tab === '资料'" :disabled="saving" @click="save">
          <LoaderCircle v-if="saving" class="size-4 animate-spin" />
          {{ saving ? '保存中…' : '保存人事资料' }}
        </Button>
      </template>
    </Sheet>
  </div>
</template>

<script setup lang="ts">
import { History, LoaderCircle, Network, Search, Users } from '@lucide/vue'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmployeeLifecycleActions from '../components/EmployeeLifecycleActions.vue'
import { api } from '../lib/api'
import { session } from '../lib/session'
import { hrBase, textDownload, downloadHrFile } from '../lib/hr-workspace'
import { useTableSort } from '../lib/useTableSort'
import { createHrSyncTracker, type HrSyncConflict, type HrSyncIssue } from '../lib/hr-sync'
import Avatar from '../ui/Avatar.vue'
import Badge from '../ui/Badge.vue'
import Button from '../ui/Button.vue'
import Callout from '../ui/Callout.vue'
import Checkbox from '../ui/Checkbox.vue'
import EmptyState from '../ui/EmptyState.vue'
import Input from '../ui/Input.vue'
import Label from '../ui/Label.vue'
import PageHeader from '../ui/PageHeader.vue'
import Pagination from '../ui/Pagination.vue'
import SegmentedTabs from '../ui/SegmentedTabs.vue'
import Select from '../ui/Select.vue'
import Sheet from '../ui/Sheet.vue'
import Skeleton from '../ui/Skeleton.vue'
import SortButton from '../ui/SortButton.vue'
import Textarea from '../ui/Textarea.vue'

interface Employee {
  id: string
  name: string
  userId: string | null
  jobNumber: string | null
  deptName: string | null
  postName: string | null
  mobile: string | null
  email: string | null
  leaderEmployeeId: string | null
  leaderEmployeeName: string | null
  entryStatus: number | null
  type: number | null
  status: number | null
  entryTime: string | null
  birthday: string | null
  probation?: number | null
  regularTime?: string | null
  leaveTime?: string | null
  postLevel: string | null
  workCity: string | null
  workAddress: string | null
  remark: string | null
  idNumber: string | null
  address: string | null
}
interface Change {
  id: string
  type: number
  effectTime: string
  oldDeptName: string
  newDeptName: string
  oldPostName: string
  newPostName: string
  oldLeaderEmployeeName: string
  newLeaderEmployeeName: string
  remark: string
}

const entryLabels: Record<number, string> = { 0: '待完善', 1: '在职', 2: '待入职', 3: '待离职', 4: '离职', 5: '放弃入职' }
const statusLabels: Record<number, string> = {
  1: '正式',
  2: '试用',
  3: '实习',
  4: '兼职',
  5: '劳务',
  6: '顾问',
  7: '返聘',
  8: '外包'
}
const changeLabels: Record<number, string> = {
  4: '转正',
  5: '调岗',
  6: '晋升',
  7: '降级',
  8: '转全职',
  9: '再入职',
  10: '企微组织同步',
  11: '档案维护'
}

const route = useRoute()
const router = useRouter()
const roster = computed(() => ['active', 'left'].includes(String(route.query.roster)) ? String(route.query.roster) : '')
const statusCategory = computed(() => roster.value === 'active' ? 11 : roster.value === 'left' ? 15 : route.query.statusCategory === '2' ? 2 : undefined)
function setRoster(value: string) {
  const query = { ...route.query }
  for (const key of ['roster', 'statusCategory', 'entryStatus', 'surveyType', 'todoType', 'employeeId']) delete query[key]
  if (value === 'probation') query.statusCategory = '2'
  else if (value !== 'all') query.roster = value
  void router.replace({ query })
}
const department = ref('')
const departmentError = ref('')
const departmentOptions = ref([{ value: '', label: '全部部门' }])
async function loadDepartments() {
  departmentError.value = ''
  try {
    const items = await api.get<{ value: string; label: string }[]>(`${hrBase}/departments`)
    departmentOptions.value = [{ value: '', label: '全部部门' }, ...items]
  } catch (e) { departmentError.value = e instanceof Error ? e.message : '部门选项加载失败' }
}
const search = ref(''),
  entryStatus = ref(String(route.query.entryStatus || '')),
  page = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref('')
const rows = ref<Employee[]>([]),
  detail = ref<Employee | null>(null),
  history = ref<Change[]>([]),
  historyError = ref(''),
  detailError = ref(''),
  saved = ref(false),
  saving = ref(false),
  tab = ref('资料')

const {
  sortKey,
  sortDir,
  sorted: sortedRows,
  toggleSort
} = useTableSort<Employee>(rows, (row, key) => {
  if (key === 'entryStatus') return entryLabels[row.entryStatus ?? 0] ?? ''
  return (row as unknown as Record<string, unknown>)[key]
})
const hasValue = (value: unknown) =>
  value !== null && value !== undefined && (typeof value !== 'string' || value.trim() !== '')

const optionalColumns = computed(() => {
  const visible = (key: 'deptName' | 'postName' | 'leaderEmployeeName' | 'entryTime') => {
    const count = rows.value.filter((row) => hasValue(row[key])).length
    return count > 0 && count / rows.value.length >= 0.5
  }
  return {
    deptName: visible('deptName'),
    postName: visible('postName'),
    leaderEmployeeName: visible('leaderEmployeeName'),
    entryTime: visible('entryTime')
  }
})

const managers = ref<Employee[]>([]),
  managerSearch = ref('')
const showEmptyFields = ref(false)
const form = reactive<Record<string, string>>({})
const canEdit = computed(() => session.has('hrm:employee:update'))
const canSyncHr = computed(() => session.has('hrm:employee:query') && canEdit.value && session.has('system:directory:sync'))

const entryFilterOptions = [
  { label: '全部任职状态', value: '' },
  ...Object.entries(entryLabels).map(([value, label]) => ({ label, value }))
]
const entryFormOptions = computed(() =>
  Object.entries(entryLabels).map(([value, label]) => ({
    label,
    value,
    disabled: unconfirmed.value && Number(value) > 2
  }))
)
const typeFormOptions = [
  { label: '待完善', value: '' },
  { label: '正式', value: '1' },
  { label: '非正式', value: '2' }
]
const statusFormOptions = [
  { label: '待完善', value: '' },
  ...Object.entries(statusLabels).map(([value, label]) => ({ label, value }))
]
const managerOptions = computed(() => [
  { label: '未维护', value: '' },
  ...managers.value.map((m) => ({ label: `${m.name} · ${m.jobNumber || m.id}`, value: m.id }))
])
const workFacts = computed(() => {
  const d = detail.value
  if (!d) return []
  return [
    { label: '姓名', value: d.name },
    { label: '手机号', value: d.mobile },
    { label: '邮箱', value: d.email },
    { label: '主部门', value: d.deptName },
    { label: '岗位', value: d.postName },
    { label: '任职状态', value: entryLabels[d.entryStatus ?? 0] || '待完善' },
    ...(hasValue(d.status) ? [{ label: '用工状态', value: statusLabels[d.status ?? 0] || '待完善' }] : []),
    ...(d.probation != null ? [{ label: '试用期', value: `${d.probation} 个月` }] : []),
    ...(d.regularTime ? [{ label: '转正日期', value: d.regularTime.slice(0, 10) }] : []),
    ...(d.leaveTime ? [{ label: '离职日期', value: d.leaveTime.slice(0, 10) }] : []),
    ...(d.userId ? [
      { label: '生日', value: d.birthday?.slice(0, 10) || '企微未提供' },
      { label: '入职日期', value: d.entryTime?.slice(0, 10) || '企微未提供' }
    ] : [])
  ].filter((item) => hasValue(item.value)) as { label: string; value: string }[]
})

let employeePageDisposed = false
const syncBase = '/admin-api/hrm/employee/wecom'
const hrSyncTracker = createHrSyncTracker({
  canStart: () => canSyncHr.value,
  start: () => api.post(`${syncBase}/sync`),
  status: runId => api.get(`${syncBase}/sync-status`, runId == null ? undefined : { runId }),
  onFinished: async () => {
    if (employeePageDisposed) return
    await Promise.allSettled([load(), loadDepartments()])
    if (!employeePageDisposed && detail.value) await open(detail.value.id)
  },
})
const { run: hrRun, syncing: hrSyncing, reading: hrStatusLoading, startError: hrSyncError, statusError: hrStatusError, refresh: refreshHrSync } = hrSyncTracker
const hrResult = computed(() => hrRun.value?.result ?? null)
const hrSyncExpanded = ref(true)
const hrSyncTitle = computed(() => hrRun.value?.status === 'running' ? '企微花名册同步进行中' : hrRun.value?.status === 'partial' ? '企微花名册部分完成' : hrRun.value?.status === 'failed' ? '企微花名册同步未完成' : '企微花名册同步完成')
const hrSyncStatusLabel = computed(() => hrRun.value ? ({ running: '同步中', success: '已完成', partial: '部分完成', failed: '失败' })[hrRun.value.status] : '')
const hrSyncStageLabel = computed(() => hrRun.value ? ({ directory: '通讯录阶段', hr: '人事信息阶段', complete: '通讯录与人事阶段已结束' })[hrRun.value.stage] : '')
const hrSyncTone = computed(() => hrRun.value?.status === 'partial' ? 'warning' : hrRun.value?.status === 'failed' ? 'destructive' : hrRun.value?.status === 'success' ? 'success' : 'info')
const hrSyncCalloutTone = computed(() => hrRun.value?.status === 'partial' ? 'warning' : hrRun.value?.status === 'failed' ? 'destructive' : hrRun.value?.status === 'success' ? 'success' : 'info')
const directoryMetrics = computed(() => {
  const d = hrRun.value?.directory
  return d ? [{ label: '新增人员', value: d.created }, { label: '更新人员', value: d.updated }, { label: '部门', value: d.departments }, { label: '关联冲突', value: d.conflicts }, { label: '目录未返回', value: d.missing }] : []
})
const hrMetrics = computed(() => {
  const r = hrResult.value
  return r ? [{ label: '检查档案', value: r.scanned }, { label: '实际读取', value: r.requested }, { label: '成功', value: r.succeeded }, { label: '失败', value: r.failed }, { label: '跳过', value: r.skipped }, { label: '未变化', value: r.unchanged }, { label: '更新员工', value: r.updatedEmployees }, { label: '更新字段', value: r.updatedFields }] : []
})
const syncTime = (value: string | null) => value ? value.replace('T', ' ').slice(0, 19) : '时间未提供'
const syncEmployee = (item: HrSyncIssue | HrSyncConflict) => item.name || `档案 #${item.employeeId}`
function syncConflict(item: HrSyncConflict) {
  const labels: Record<string, string> = { birthday: '生日', entryTime: '入职日期', entryStatus: '任职状态', status: '用工状态', probation: '试用期', regularTime: '转正日期', leaveTime: '离职日期' }
  const value = (raw: string | number | null | undefined) => raw == null || raw === '' ? '未提供' : item.field === 'entryStatus' ? entryLabels[Number(raw)] || String(raw) : item.field === 'status' ? statusLabels[Number(raw)] || String(raw) : String(raw)
  return `${labels[item.field || ''] || item.field || '档案信息'}：${item.reason ? `${item.reason}；` : ''}本地 ${value(item.currentValue)}，企微 ${value(item.upstreamValue)}`
}
async function syncHr() {
  if (!canSyncHr.value) return
  hrSyncExpanded.value = true
  await hrSyncTracker.start()
}

const unconfirmed = computed(() => !detail.value?.entryStatus)
const date = (v: string | null) => v?.slice(0, 10) || '待完善'
const fields = [
  'jobNumber',
  'leaderEmployeeId',
  'entryTime',
  'birthday',
  'entryStatus',
  'type',
  'status',
  'postLevel',
  'workCity',
  'workAddress',
  'remark'
] as const

function searchList() {
  page.value = 1
  void load()
}

function onEntryStatus(value: unknown) {
  entryStatus.value = String(value ?? '')
  page.value = 1
  void load()
}

function go(next: number) {
  page.value = next
  void load()
}

let employeeListRequest = 0
async function load() {
  const request = ++employeeListRequest
  loading.value = true
  error.value = ''
  try {
    const r = await api.get<{ list: Employee[]; total: number }>('/admin-api/hrm/employee/page', {
      search: search.value || undefined,
      deptId: department.value || undefined,
      entryStatus: statusCategory.value ? undefined : entryStatus.value || undefined,
      statusCategory: statusCategory.value,
      surveyType: route.query.surveyType || undefined,
      todoType: route.query.todoType || undefined,
      pageNo: page.value,
      pageSize: 20
    })
    if (request !== employeeListRequest) return
    rows.value = r.list
    total.value = Number(r.total)
  } catch (e) {
    if (request !== employeeListRequest) return
    rows.value = []
    error.value = e instanceof Error ? e.message : '员工加载失败'
  } finally {
    if (request === employeeListRequest) loading.value = false
  }
}

async function exportEmployees() {
  try { await downloadHrFile('/admin-api/hrm/employee/export-excel', { search: search.value || undefined, deptId: department.value || undefined, statusCategory: statusCategory.value, entryStatus: statusCategory.value ? undefined : entryStatus.value || undefined, surveyType: route.query.surveyType || undefined, todoType: route.query.todoType || undefined }, '员工花名册.xlsx') }
  catch (e) { error.value = e instanceof Error ? e.message : '导出失败' }
}
async function downloadCertificate() {
  if (!detail.value) return
  try { const r = await api.get<{ content: string }>(`${hrBase}/certificate`, { employeeId: detail.value.id }); textDownload(`${detail.value.name}-离职证明.txt`, r.content) }
  catch (e) { detailError.value = e instanceof Error ? e.message : '离职证明生成失败' }
}

async function open(id: string) {
  detailError.value = ''
  saved.value = false
  tab.value = '资料'
  history.value = []
  historyError.value = ''
  showEmptyFields.value = false
  try {
    const d = await api.get<Employee>('/admin-api/hrm/employee/get', { id })
    detail.value = d
    for (const key of fields) form[key] = String(d[key] ?? '')
    form.entryStatus = String(d.entryStatus ?? 0)
    form.entryTime = d.entryTime?.slice(0, 10) || ''
    form.birthday = d.birthday?.slice(0, 10) || ''
    managerSearch.value = ''
    managers.value = []
    if (d.leaderEmployeeId)
      managers.value = [{ id: d.leaderEmployeeId, name: d.leaderEmployeeName || '当前上级' } as Employee]
    await router.replace({ query: { ...route.query, employeeId: id } })
    try {
      history.value = await api.get<Change[]>('/admin-api/hrm/employee/change-record/list', {
        employeeId: id
      })
    } catch (e) {
      historyError.value = e instanceof Error ? e.message : '变更记录加载失败'
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '档案加载失败'
  }
}

let managerRequest = 0
async function findManagers() {
  const request = ++managerRequest
  try {
    const r = await api.get<{ list: Employee[] }>('/admin-api/hrm/employee/simple-page', {
      search: managerSearch.value,
      pageNo: 1,
      pageSize: 30
    })
    if (request !== managerRequest) return
    const selected = managers.value.find((m) => m.id === form.leaderEmployeeId)
    managers.value = r.list.filter((m) => m.id !== detail.value?.id)
    if (selected && !managers.value.some((m) => m.id === selected.id)) managers.value.unshift(selected)
  } catch (e) {
    detailError.value = e instanceof Error ? e.message : '上级列表加载失败'
  }
}

async function save() {
  if (!detail.value) return
  saving.value = true
  detailError.value = ''
  saved.value = false
  try {
    const body: Record<string, unknown> = { id: detail.value.id }
    for (const key of fields) body[key] = form[key] || null
    for (const key of ['entryStatus', 'type', 'status'])
      body[key] = form[key] === '' ? null : Number(form[key])
    // 已登记的时间原样回传，避免日期控件抹去旧数据中的时分秒。
    body.entryTime = unconfirmed.value
      ? form.entryTime
        ? form.entryTime + 'T00:00:00'
        : null
      : detail.value.entryTime
    body.birthday = form.birthday ? form.birthday + 'T00:00:00' : null
    await api.put('/admin-api/hrm/employee/supplement', body)
    await open(detail.value.id)
    saved.value = true
    await load()
  } catch (e) {
    detailError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

function close() {
  detail.value = null
  const query = { ...route.query }
  delete query.employeeId
  void router.replace({ query })
}

async function refreshDetail() {
  if (detail.value) await open(detail.value.id)
  await load()
}

watch(
  () => route.query.employeeId,
  (id) => {
    if (id && id !== detail.value?.id) void open(String(id))
  }
)
watch(() => [route.query.roster, route.query.statusCategory, route.query.entryStatus, route.query.surveyType, route.query.todoType], () => { page.value = 1; entryStatus.value = String(route.query.entryStatus || ''); void load() })
onMounted(() => {
  void load()
  void loadDepartments()
  if (session.has('hrm:employee:query')) void refreshHrSync()
  if (route.query.employeeId) void open(String(route.query.employeeId))
})
onBeforeUnmount(() => {
  employeePageDisposed = true
  hrSyncTracker.dispose()
  employeeListRequest++
  managerRequest++
})
</script>
