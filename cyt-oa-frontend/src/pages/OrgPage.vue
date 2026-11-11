<template>
  <div>
    <PageHeader
      title="组织架构"
      description="查看部门层级、负责人和成员的工作联系方式。"
    >
      <template #actions>
        <Button
          v-if="canQueryEmployee"
          variant="ghost"
          size="sm"
          @click="router.push('/page/hrm-employee')"
          >员工档案 <ArrowUpRight class="size-3.5"
        /></Button>
        <Button
          variant="outline"
          size="sm"
          :disabled="treeLoading"
          @click="loadTree"
        >
          <RefreshCw :class="['size-3.5', treeLoading && 'animate-spin']" />
          刷新
        </Button>
        <Button v-if="canSync" variant="outline" size="sm" @click="openRuns"
          >同步记录</Button
        >
        <Button v-if="canSync" size="sm" :loading="syncing" @click="runSync">
          <RefreshCw v-if="!syncing" class="size-3.5" />
          {{ syncing ? `同步中 ${elapsed}s` : '同步企微通讯录' }}
        </Button>
      </template>
    </PageHeader>

    <Callout
      v-if="showSyncNotice"
      :tone="syncTone"
      :title="headline"
      class="mt-4"
      :role="syncing ? 'status' : 'alert'"
      aria-live="polite"
    >
      {{ subline }}
      <template #action>
        <button
          v-if="!syncing"
          type="button"
          class="grid size-7 shrink-0 place-items-center rounded-md hover:bg-destructive/10 focus-visible:outline-2 focus-visible:outline-ring"
          aria-label="关闭同步错误提示"
          @click="dismissedRunId = latest?.id ?? ''"
        >
          <X class="size-4" aria-hidden="true" />
        </button>
      </template>
    </Callout>

    <div class="mt-4 grid items-start gap-4 lg:grid-cols-[300px_minmax(0,1fr)]">
      <section class="panel overflow-hidden lg:sticky lg:top-20">
        <div class="border-b p-3">
          <div class="relative">
            <Search
              class="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground"
            />
            <Input
              v-model="filter"
              class="pl-8"
              placeholder="搜索部门"
              aria-label="搜索部门"
            />
          </div>
          <div
            class="mt-3 flex flex-wrap items-center justify-between gap-2 text-[11px] text-muted-foreground"
          >
            <span>{{ nodes.length }} 个部门 · 数字为直属人数</span>
            <span
              v-if="latest?.status === 'success'"
              :title="`最近同步：${fmtTime(latest.finishedAt || latest.createdAt)}`"
              >已同步 {{ fmtTime(latest.finishedAt || latest.createdAt) }}</span
            >
          </div>
        </div>

        <div
          v-if="flatTree.length"
          ref="treeEl"
          class="max-h-56 overflow-y-auto p-2 transition-opacity lg:max-h-[calc(100dvh-22rem)]"
          :class="{ 'opacity-55': treeLoading }"
        >
          <div
            v-for="row in flatTree"
            :key="row.node.id"
            :data-dept="row.node.id"
            role="button"
            tabindex="0"
            :aria-pressed="row.node.id === selectedId"
            class="flex cursor-pointer items-center gap-1 rounded-lg py-1.5 pr-2 text-sm transition-colors focus-visible:outline-2 focus-visible:outline-ring"
            :class="
              row.node.id === selectedId
                ? 'bg-primary/10 font-medium text-primary'
                : 'hover:bg-accent/60'
            "
            :style="{ paddingLeft: `${row.depth * 16 + 8}px` }"
            @click="selectId(row.node.id)"
            @keydown.enter.self.prevent="selectId(row.node.id)"
            @keydown.space.self.prevent="selectId(row.node.id)"
          >
            <button
              class="grid size-5 shrink-0 place-items-center rounded text-muted-foreground transition-transform hover:bg-accent"
              :class="{
                'rotate-90': isOpen(row.node.id),
                invisible: !row.hasChildren
              }"
              :aria-expanded="isOpen(row.node.id)"
              :aria-label="expanded.has(row.node.id) ? '收起' : '展开'"
              @click.stop="toggle(row.node.id)"
            >
              <ChevronRight class="size-3.5" />
            </button>
            <span class="min-w-0 flex-1 truncate" :title="row.node.namePath">{{
              row.node.name
            }}</span>
            <Badge
              :variant="countOf(row.node.memberCount) ? 'secondary' : 'outline'"
              class="tabular-nums"
            >
              {{ countOf(row.node.memberCount) }}
            </Badge>
          </div>
        </div>

        <EmptyState
          v-else
          :icon="Building2"
          :title="
            filter ? '没有匹配的部门' : treeLoading ? '加载中…' : '暂无部门数据'
          "
          :description="
            filter
              ? '换个关键词试试'
              : treeLoading
                ? undefined
                : '点上方「同步企微通讯录」从企业微信拉取'
          "
        />
      </section>

      <div class="min-w-0">
        <section v-if="!selected" class="panel">
          <EmptyState
            :icon="Building2"
            title="选择一个部门"
            description="左侧点击部门即可查看成员、负责人与子部门人数"
          />
        </section>

        <template v-else>
          <section class="panel">
            <div class="panel-header flex-wrap">
              <nav
                class="flex min-w-0 flex-1 flex-wrap items-center gap-0.5 text-xs"
                aria-label="部门位置"
              >
                <template v-for="(c, i) in crumbs" :key="c.id">
                  <ChevronRight
                    v-if="i"
                    class="size-3 shrink-0 text-muted-foreground/50"
                  />
                  <button
                    class="max-w-40 truncate rounded px-1 py-0.5 transition-colors"
                    :class="
                      i === crumbs.length - 1
                        ? 'font-medium text-foreground'
                        : 'text-muted-foreground hover:bg-accent hover:text-foreground'
                    "
                    :disabled="i === crumbs.length - 1"
                    @click="selectId(c.id)"
                  >
                    {{ c.name }}
                  </button>
                </template>
              </nav>
            </div>
            <div
              class="flex flex-wrap items-start justify-between gap-4 border-t px-5 py-4"
            >
              <div>
                <h2 class="text-lg font-semibold tracking-tight">
                  {{ selected.name }}
                </h2>
                <p class="mt-1 text-xs text-muted-foreground">
                  直属成员 {{ countOf(selected.memberCount) }} 人
                </p>
              </div>
              <div class="text-xs text-muted-foreground">
                <span>部门负责人</span>
                <div class="mt-1 flex items-center gap-1.5">
                  <span class="text-sm font-medium text-foreground">{{
                    selected.leaderName || '未指定'
                  }}</span>
                  <Button
                    v-if="canUpdateLeader"
                    variant="ghost"
                    size="sm"
                    class="h-6 px-1.5"
                    @click="openLeader"
                    >调整</Button
                  >
                </div>
              </div>
            </div>
            <div
              v-if="directChildren.length"
              class="flex flex-wrap gap-2 border-t px-5 py-3"
              aria-label="下级部门"
            >
              <button
                v-for="child in directChildren"
                :key="child.id"
                type="button"
                class="inline-flex items-center gap-2 rounded-md border px-2.5 py-1.5 text-xs hover:border-primary/40 hover:bg-primary/5 focus-visible:outline-2 focus-visible:outline-ring"
                @click="selectId(child.id)"
              >
                <Building2 class="size-3.5 text-muted-foreground" />{{
                  child.name
                }}<ChevronRight class="size-3 text-muted-foreground" />
              </button>
            </div>
          </section>

          <section class="panel mt-4 overflow-hidden">
            <div class="panel-header">
              <div class="panel-title">
                成员通讯录
                <span class="text-xs font-normal text-muted-foreground"
                  >共 {{ total }} 人</span
                >
              </div>
              <Checkbox
                :model-value="includeChildren"
                @update:model-value="onIncludeChildren"
                >含子部门成员</Checkbox
              >
              <Button
                variant="ghost"
                size="icon-sm"
                aria-label="刷新成员"
                :disabled="membersLoading"
                @click="loadMembers"
              >
                <RefreshCw
                  :class="['size-3.5', membersLoading && 'animate-spin']"
                />
              </Button>
            </div>

            <div
              v-if="members.length"
              class="overflow-x-auto"
              :class="{ 'pointer-events-none opacity-60': membersLoading }"
            >
              <table class="data-table">
                <thead>
                  <tr>
                    <th>姓名</th>
                    <th>岗位</th>
                    <th>主部门</th>
                    <th>工作联系方式</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="m in members"
                    :key="m.userId"
                    class="transition-colors hover:bg-muted/40"
                  >
                    <td>
                      <div class="flex items-center gap-2.5">
                        <Avatar :name="m.name" size="sm" />
                        <div class="min-w-0">
                          <div class="truncate font-medium">{{ m.name }}</div>
                          <div class="truncate text-xs text-muted-foreground">
                            {{ m.wecomUserid }}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td>{{ m.position || '—' }}</td>
                    <td class="max-w-48">
                      <span
                        class="block truncate"
                        :title="m.mainDeptName || ''"
                        >{{ shortDept(m.mainDeptName) }}</span
                      >
                    </td>
                    <td>
                      <div class="flex flex-col items-start gap-1 text-xs">
                        <a
                          v-if="m.mobile"
                          :href="`tel:${m.mobile}`"
                          class="inline-flex items-center gap-1.5 hover:text-primary"
                          ><Phone class="size-3" />{{ m.mobile }}</a
                        >
                        <a
                          v-if="m.email"
                          :href="`mailto:${m.email}`"
                          class="inline-flex items-center gap-1.5 break-all hover:text-primary"
                          ><Mail class="size-3" />{{ m.email }}</a
                        >
                        <span
                          v-if="!m.mobile && !m.email"
                          class="text-muted-foreground"
                          >—</span
                        >
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <EmptyState
              v-if="!members.length"
              :icon="Users"
              :title="membersLoading ? '加载中…' : '该部门暂无成员'"
              :description="
                membersLoading
                  ? undefined
                  : '勾选「含子部门成员」可查看下级部门的人'
              "
            />
          </section>

          <Pagination
            v-if="total > 0"
            class="mt-4"
            :page="pageNo"
            :page-size="pageSize"
            :total="total"
            @update:page="goto"
          />
        </template>
      </div>
    </div>

    <Dialog
      :open="leaderOpen"
      :title="`部门负责人 · ${selected?.name ?? ''}`"
      description="本地维护部门负责人；留空即取消指定。"
      size="sm"
      @update:open="leaderOpen = $event"
    >
      <Label>负责人</Label>
      <Select
        v-model="leaderValue"
        :options="[{ label: '不指定', value: '' }, ...candidates]"
      />
      <template #footer>
        <Button variant="outline" @click="leaderOpen = false">取消</Button>
        <Button :loading="savingLeader" @click="saveLeader">保存</Button>
      </template>
    </Dialog>

    <Dialog
      :open="runsOpen"
      title="同步记录"
      description="每次同步的触发方式、结果与耗时；运行中的记录会实时刷新状态"
      size="xl"
      @update:open="runsOpen = $event"
    >
      <div v-if="runs.length" class="overflow-x-auto rounded-lg border">
        <table class="data-table text-xs">
          <thead>
            <tr>
              <th>时间</th>
              <th>触发</th>
              <th>操作人</th>
              <th>状态</th>
              <th class="text-right">新增</th>
              <th class="text-right">更新</th>
              <th class="text-right">部门</th>
              <th class="text-right">冲突</th>
              <th class="text-right">未返回</th>
              <th class="text-right">耗时</th>
              <th>说明</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in runs" :key="r.id">
              <td class="whitespace-nowrap">{{ fmtTime(r.createdAt) }}</td>
              <td class="whitespace-nowrap">
                {{ r.triggerType === 'schedule' ? '定时' : '手动' }}
              </td>
              <td class="whitespace-nowrap">{{ r.actor || '—' }}</td>
              <td>
                <Badge :variant="toneBadge(toneOf(r.status))">{{
                  labelOf(r.status)
                }}</Badge>
              </td>
              <td class="text-right tabular-nums">
                {{ r.status === 'running' ? '—' : (r.created ?? 0) }}
              </td>
              <td class="text-right tabular-nums">
                {{ r.status === 'running' ? '—' : (r.updated ?? 0) }}
              </td>
              <td class="text-right tabular-nums">
                {{ r.status === 'running' ? '—' : (r.departments ?? 0) }}
              </td>
              <td
                class="text-right tabular-nums"
                :class="{
                  'font-medium text-destructive': (r.conflicts ?? 0) > 0
                }"
              >
                {{ r.status === 'running' ? '—' : (r.conflicts ?? 0) }}
              </td>
              <td
                class="text-right tabular-nums"
                :class="{
                  'font-medium text-destructive': (r.missing ?? 0) > 0
                }"
              >
                {{ r.status === 'running' ? '—' : (r.missing ?? 0) }}
              </td>
              <td class="whitespace-nowrap text-right tabular-nums">
                {{
                  r.durationMs == null
                    ? '—'
                    : (r.durationMs / 1000).toFixed(1) + 's'
                }}
              </td>
              <td class="max-w-56">
                <span
                  class="block truncate text-muted-foreground"
                  :title="r.message || ''"
                  >{{ r.message || '—' }}</span
                >
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState v-else :icon="History" :title="runsErr || '还没有同步记录'" />
      <template #footer>
        <Button variant="outline" @click="runsOpen = false">关闭</Button>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowUpRight,
  Building2,
  ChevronRight,
  History,
  Mail,
  Phone,
  RefreshCw,
  Search,
  Users,
  X
} from '@lucide/vue'
import { api } from '../lib/api'
import { session } from '../lib/session'
import { toast } from '../lib/toast'
import { toneBadge } from '../lib/tone'
import Avatar from '../ui/Avatar.vue'
import Badge from '../ui/Badge.vue'
import Button from '../ui/Button.vue'
import Callout from '../ui/Callout.vue'
import Checkbox from '../ui/Checkbox.vue'
import Dialog from '../ui/Dialog.vue'
import EmptyState from '../ui/EmptyState.vue'
import Input from '../ui/Input.vue'
import Label from '../ui/Label.vue'
import PageHeader from '../ui/PageHeader.vue'
import Pagination from '../ui/Pagination.vue'
import Select from '../ui/Select.vue'

interface OrgNode {
  id: string
  parentId: string | null
  name: string
  namePath: string
  leaderUserid: string | null
  leaderName: string | null
  memberCount: number | string
}

interface OrgMember {
  userId: string
  wecomUserid: string
  name: string
  position: string | null
  mobile: string | null
  email: string | null
  mainDeptName: string | null
  deptNames: string[]
  enabled: boolean
  jobNumber: string | null
}

type RunStatus = 'running' | 'success' | 'failed'

interface SyncRun {
  id: string
  triggerType: string
  actor: string | null
  status: RunStatus
  created: number | null
  updated: number | null
  departments: number | null
  conflicts: number | null
  missing: number | null
  message: string | null
  durationMs: number | null
  createdAt: string | null
  finishedAt: string | null
}

const router = useRouter()
const canSync = computed(() => session.has('system:directory:sync'))
const canUpdateLeader = computed(() => session.has('system:dept:update-leader'))
const canQueryEmployee = computed(() => session.has('hrm:employee:query'))

const nodes = ref<OrgNode[]>([])
const treeLoading = ref(true)
const treeEl = ref<HTMLElement | null>(null)
const filter = ref('')
const selectedId = ref<string | null>(null)
const expanded = ref(new Set<string>())
const includeChildren = ref(false)

const members = ref<OrgMember[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 10
const membersLoading = ref(false)

const candidates = ref<{ value: string; label: string }[]>([])
const leaderOpen = ref(false)
const leaderValue = ref('')
const savingLeader = ref(false)

const latest = ref<SyncRun | null>(null)
const dismissedRunId = ref('')
const syncStarting = ref(false)
const elapsed = ref(0)
const runsOpen = ref(false)
const runs = ref<SyncRun[]>([])
const runsErr = ref('')

let pollTimer: ReturnType<typeof setTimeout> | undefined
let tickTimer: ReturnType<typeof setInterval> | undefined
let disposed = false
let memberRequest = 0

const syncing = computed(
  () => syncStarting.value || latest.value?.status === 'running'
)
// 历史成功结果只用于显示更新时间，不当作本次操作的新通知。
const showSyncNotice = computed(
  () =>
    syncing.value ||
    (latest.value?.status === 'failed' &&
      latest.value.id !== dismissedRunId.value)
)
const byId = computed(() => new Map(nodes.value.map((n) => [n.id, n])))
const selected = computed(() =>
  selectedId.value ? (byId.value.get(selectedId.value) ?? null) : null
)

const childrenOf = computed(() => {
  const map = new Map<string | null, OrgNode[]>()
  for (const n of nodes.value) {
    // parent 不存在或指向自己都当顶层，防环
    const pid =
      n.parentId && byId.value.has(n.parentId) && n.parentId !== n.id
        ? n.parentId
        : null
    if (!map.has(pid)) map.set(pid, [])
    map.get(pid)!.push(n)
  }
  return map
})

const countOf = (v: number | string | null | undefined) => Number(v ?? 0) || 0

const directChildren = computed(
  () => childrenOf.value.get(selectedId.value) ?? []
)

/** 搜索时保留命中部门及其全部祖先，否则命中项会脱离父级变成孤儿。 */
const keepIds = computed(() => {
  const q = filter.value.trim().toLowerCase()
  if (!q) return null
  const keep = new Set<string>()
  for (const n of nodes.value) {
    if (!n.name.toLowerCase().includes(q)) continue
    const trail = new Set<string>()
    let cur: OrgNode | undefined = n
    while (cur && !trail.has(cur.id)) {
      keep.add(cur.id)
      trail.add(cur.id)
      cur = cur.parentId ? byId.value.get(cur.parentId) : undefined
    }
  }
  return keep
})

interface TreeRow {
  node: OrgNode
  depth: number
  hasChildren: boolean
}
const flatTree = computed<TreeRow[]>(() => {
  const keep = keepIds.value
  const rows: TreeRow[] = []
  const walk = (id: string | null, depth: number) => {
    for (const node of childrenOf.value.get(id) ?? []) {
      if (keep && !keep.has(node.id)) continue
      const kids = (childrenOf.value.get(node.id) ?? []).filter(
        (k) => !keep || keep.has(k.id)
      )
      rows.push({ node, depth, hasChildren: kids.length > 0 })
      if (kids.length && isOpen(node.id)) walk(node.id, depth + 1)
    }
  }
  walk(null, 0)
  return rows
})

// 搜索命中的一律展开，清空搜索后回到用户自己的折叠状态
function isOpen(id: string) {
  return keepIds.value ? true : expanded.value.has(id)
}

const crumbs = computed(() => {
  const out: OrgNode[] = []
  const trail = new Set<string>()
  let cur: OrgNode | null = selected.value
  while (cur && out.length < 32 && !trail.has(cur.id)) {
    out.unshift(cur)
    trail.add(cur.id)
    cur = cur.parentId ? (byId.value.get(cur.parentId) ?? null) : null
  }
  return out
})

const syncTone = computed<'info' | 'success' | 'destructive'>(() => {
  if (syncing.value) return 'info'
  if (latest.value?.status === 'success') return 'success'
  if (latest.value?.status === 'failed') return 'destructive'
  return 'info'
})

const headline = computed(() => {
  if (syncing.value) return '正在同步企微通讯录'
  if (!latest.value) return '还没有同步过通讯录'
  return latest.value.status === 'success' ? '通讯录同步成功' : '通讯录同步失败'
})

const subline = computed(() => {
  const r = latest.value
  if (!r) return '正在启动同步，请稍候'
  if (r.status === 'running')
    return `已用 ${elapsed.value}s，页面显示的仍是上一次同步的数据`
  const at = fmtTime(r.createdAt)
  if (r.status === 'success') {
    return `${at} · 新增 ${r.created ?? 0} 人、更新 ${r.updated ?? 0} 人、部门 ${r.departments ?? 0} 个`
  }
  return `${at} · ${r.message || '失败原因见同步记录'}`
})

/** 主部门列只显示末级名，完整层级挂在 title 里，一行塞不下整条路径。 */
function shortDept(namePath: string | null) {
  if (!namePath) return '—'
  const parts = namePath.split('/')
  return parts[parts.length - 1] || namePath
}

function toneOf(status: RunStatus) {
  return status === 'success' ? 'ok' : status === 'failed' ? 'bad' : 'info'
}

function labelOf(status: RunStatus) {
  return status === 'success' ? '成功' : status === 'failed' ? '失败' : '运行中'
}

function showToast(msg: string, tone: 'ok' | 'bad' = 'ok') {
  toast(msg, tone)
}

function fmtDate(v: string | null | undefined) {
  return v ? v.slice(0, 10) : '—'
}

function fmtTime(v: string | null | undefined) {
  if (!v) return '—'
  const t = v.replace('T', ' ').slice(0, 16)
  const d = fmtDate(v)
  return t === d + ' 00:00' ? d : t
}

/** 刷新树时不重挂 DOM，只把选中的部门滚回视野，避免看着看着跳回顶部。 */
async function revealSelected() {
  await nextTick()
  if (!selectedId.value) return
  treeEl.value
    ?.querySelector(`[data-dept="${selectedId.value}"]`)
    ?.scrollIntoView({ block: 'nearest' })
}

async function loadTree() {
  treeLoading.value = true
  try {
    const list = await api.get<OrgNode[]>(
      '/admin-api/system/directory/org-tree'
    )
    nodes.value = list ?? []
    // 65 个部门一屏能扫完，默认全展开
    expanded.value = new Set(nodes.value.map((n) => n.id))
    if (!selectedId.value || !byId.value.has(selectedId.value)) {
      selectedId.value = nodes.value[0]?.id ?? null
      pageNo.value = 1
    }
    await loadMembers()
    await revealSelected()
  } catch (e) {
    showToast(e instanceof Error ? e.message : '部门树加载失败', 'bad')
  } finally {
    treeLoading.value = false
  }
}

async function loadMembers() {
  const request = ++memberRequest
  if (!selectedId.value) {
    members.value = []
    total.value = 0
    membersLoading.value = false
    return
  }
  membersLoading.value = true
  try {
    const data = await api.get<{
      total: number
      page: number
      size: number
      list: OrgMember[]
    }>('/admin-api/system/directory/org-tree/members', {
      deptId: selectedId.value,
      includeChildren: includeChildren.value,
      pageNo: pageNo.value,
      pageSize
    })
    if (disposed || request !== memberRequest) return
    members.value = data?.list ?? []
    total.value = Number(data?.total ?? 0)
  } catch (e) {
    if (!disposed && request === memberRequest)
      showToast(e instanceof Error ? e.message : '成员加载失败', 'bad')
  } finally {
    if (request === memberRequest) membersLoading.value = false
  }
}

async function loadStatus() {
  try {
    const status =
      (await api.get<SyncRun>('/admin-api/system/directory/sync-status')) ??
      null
    if (!disposed) latest.value = status
    return true
  } catch {
    // 暂时读不到状态不能当作同步已结束，保留运行态并继续轮询。
    return false
  }
}

function selectId(id: string) {
  if (id === selectedId.value) return
  selectedId.value = id
  members.value = []
  total.value = 0
  pageNo.value = 1
  void loadMembers()
  void revealSelected()
}

function toggle(id: string) {
  const next = new Set(expanded.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expanded.value = next
}

function onIncludeChildren(v: boolean) {
  includeChildren.value = v
  reloadMembers()
}

function reloadMembers() {
  pageNo.value = 1
  void loadMembers()
}

function goto(p: number) {
  pageNo.value = p
  void loadMembers()
}

async function openLeader() {
  leaderOpen.value = true
  leaderValue.value = selected.value?.leaderUserid ?? ''
  if (candidates.value.length) return
  try {
    candidates.value =
      (await api.get<{ value: string; label: string }[]>(
        '/admin-api/system/users/leader-candidates'
      )) ?? []
  } catch {
    candidates.value = []
  }
}

async function saveLeader() {
  if (!selected.value) return
  savingLeader.value = true
  try {
    await api.put('/admin-api/system/directory/departments/leader', {
      id: selected.value.id,
      leaderUserid: leaderValue.value || null
    })
    showToast('负责人已更新')
    leaderOpen.value = false
    await loadTree()
  } catch (e) {
    showToast(e instanceof Error ? e.message : '保存失败', 'bad')
  } finally {
    savingLeader.value = false
  }
}

function startTicking() {
  if (disposed) return
  elapsed.value = 0
  if (tickTimer) clearInterval(tickTimer)
  tickTimer = setInterval(() => (elapsed.value += 1), 1000)
}

function stopTicking() {
  if (tickTimer) clearInterval(tickTimer)
  tickTimer = undefined
}

function schedulePoll(delay: number) {
  if (disposed) return
  if (pollTimer) clearTimeout(pollTimer)
  pollTimer = setTimeout(pollSync, delay)
}

/** 同步跑在后台线程，页面每 3 秒读一次运行记录；关掉页面那趟照样会跑完并留痕。 */
async function pollSync() {
  const loaded = await loadStatus()
  if (disposed) return
  if (!loaded) {
    schedulePoll(5000)
    return
  }
  if (!syncing.value) {
    stopTicking()
    reportRun(latest.value)
    void loadTree()
    return
  }
  schedulePoll(3000)
}

function reportRun(r: SyncRun | null) {
  if (!r || r.status === 'running') return
  if (r.status === 'failed') {
    showToast(
      r.message ? `同步失败：${r.message}` : '同步失败，原因见同步记录',
      'bad'
    )
    return
  }
  const parts = [
    `新增 ${r.created ?? 0} 人`,
    `更新 ${r.updated ?? 0} 人`,
    `部门 ${r.departments ?? 0} 个`
  ]
  if (r.conflicts) parts.push(`冲突 ${r.conflicts} 项`)
  if (r.missing) parts.push(`未返回 ${r.missing} 人`)
  showToast('同步完成：' + parts.join('，'))
}

async function runSync() {
  if (syncing.value) return
  syncStarting.value = true
  dismissedRunId.value = ''
  try {
    const started = await api.post<SyncRun>('/admin-api/system/directory/sync')
    if (disposed) return
    if (started) latest.value = started
    startTicking()
    schedulePoll(2000)
  } catch (e) {
    if (disposed) return
    showToast(e instanceof Error ? e.message : '同步启动失败', 'bad')
    void loadStatus()
  } finally {
    syncStarting.value = false
  }
}

async function openRuns() {
  runsOpen.value = true
  runsErr.value = ''
  try {
    runs.value =
      (await api.get<SyncRun[]>('/admin-api/system/directory/runs')) ?? []
  } catch (e) {
    runsErr.value = e instanceof Error ? e.message : '记录加载失败'
  }
}

onMounted(async () => {
  await Promise.all([loadTree(), loadStatus()])
  // 页面外（定时任务或上一趟）还在跑的同步，进来接着盯
  if (syncing.value) {
    startTicking()
    schedulePoll(3000)
  }
})

onBeforeUnmount(() => {
  disposed = true
  if (pollTimer) clearTimeout(pollTimer)
  stopTicking()
})
</script>
