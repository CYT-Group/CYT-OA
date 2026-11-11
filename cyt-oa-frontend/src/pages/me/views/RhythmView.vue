<template>
  <div class="space-y-5">
    <PageHeader title="大促 · 新品" description="公司节奏、关键节点与倒计时，关注后节点开始前会提醒你">
      <template #actions>
        <SegmentedTabs :model-value="tab" :items="TABS.map((t) => ({ value: t.key, label: t.title }))" @update:model-value="select('tab', $event)" />
      </template>
    </PageHeader>

    <div class="flex flex-wrap items-center gap-2">
      <Select
        :model-value="kind"
        :options="KIND_OPTIONS"
        class="w-36"
        @update:model-value="onKind"
      />
      <Button variant="outline" size="sm" :disabled="loading" @click="load">
        <RefreshCw class="size-3.5" />
        刷新
      </Button>
      <span class="ml-auto text-xs text-muted-foreground">{{ shown.length }} 个节点</span>
    </div>

    <Callout v-if="loadError" tone="destructive" title="加载失败">{{ loadError }}</Callout>
    <p v-else-if="loading" class="py-10 text-center text-sm text-muted-foreground" role="status">正在读取活动节点…</p>

    <template v-else>
      <section v-if="tab === 'calendar'" class="panel">
        <div class="panel-header flex-wrap">
          <div class="flex items-center gap-1">
            <Button variant="outline" size="icon-sm" aria-label="上个月" @click="moveMonth(-1)">
              <ChevronLeft class="size-4" />
            </Button>
            <b class="min-w-24 text-center text-sm font-semibold tabular-nums">{{ cellYear }} 年 {{ cellMonth + 1 }} 月</b>
            <Button variant="outline" size="icon-sm" aria-label="下个月" @click="moveMonth(1)">
              <ChevronRight class="size-4" />
            </Button>
            <Button variant="ghost" size="sm" @click="backToday">回到本月</Button>
          </div>
          <div class="flex items-center gap-3 text-[11px] text-muted-foreground">
            <span class="inline-flex items-center gap-1.5"><i class="size-2 rounded-full bg-destructive" />大促</span>
            <span class="inline-flex items-center gap-1.5"><i class="size-2 rounded-full bg-primary" />新品</span>
            <span class="inline-flex items-center gap-1.5"><i class="size-2 rounded-full bg-info" />活动</span>
          </div>
        </div>

        <div class="overflow-x-auto p-3">
          <div class="min-w-[640px]">
            <div class="grid grid-cols-7 gap-1.5">
              <div v-for="d in DOW" :key="d" class="pb-1 text-center text-[11px] font-medium text-muted-foreground">{{ d }}</div>
              <div
                v-for="cell in cells"
                :key="cell.key"
                class="min-h-24 rounded-lg border p-1.5 transition-colors"
                :class="[
                  cell.date ? 'border-border/70 bg-card' : 'border-transparent bg-muted/30',
                  cell.date === today ? 'ring-2 ring-primary/40' : '',
                  cell.nodes.length ? 'bg-muted/30' : ''
                ]"
              >
                <div v-if="cell.date" class="mb-1 flex items-center justify-between">
                  <span class="text-[11px] tabular-nums" :class="cell.date === today ? 'font-semibold text-primary' : 'text-muted-foreground'">
                    {{ Number(cell.date.slice(8, 10)) }}
                  </span>
                  <span v-if="cell.date === today" class="text-[9px] font-medium text-primary">今天</span>
                </div>
                <button
                  v-for="node in cell.nodes"
                  :key="node.id"
                  :title="node.name"
                  class="mb-1 block w-full truncate rounded px-1.5 py-0.5 text-left text-[10px] font-medium transition-opacity hover:opacity-80"
                  :class="kindChip(node.kind)"
                  @click="openNode(node)"
                >
                  {{ node.name }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <template v-else>
        <section v-if="current" class="panel overflow-hidden">
          <div class="flex flex-wrap items-center gap-2.5 px-4 py-3">
            <span class="grid size-8 place-items-center rounded-lg border border-primary/20 bg-primary/8 text-primary">
              <Megaphone class="size-4" />
            </span>
            <b class="text-base font-semibold">{{ current.name }}</b>
            <Badge variant="secondary">{{ progressTag(current) }}</Badge>
            <Badge v-if="current.state !== 'past'" variant="outline">{{ countdown(current) }}</Badge>
            <Button variant="outline" size="sm" class="ml-auto" @click="toggleFollow(current)">
              {{ current.followed ? '已关注 · 取消' : '关注' }}
            </Button>
          </div>
          <div v-if="current.state === 'ongoing'" class="h-1 bg-muted">
            <div class="h-full bg-primary transition-[width]" :style="{ width: progressPct(current) + '%' }" />
          </div>
          <p class="border-t border-border px-4 py-3 text-sm leading-relaxed text-muted-foreground">
            {{ current.note || '暂无作战说明' }}
          </p>
        </section>

        <section class="panel">
          <div class="panel-header">
            <div class="panel-title"><CalendarRange class="size-4 text-muted-foreground" />{{ groupTitle }}</div>
            <span class="text-xs text-muted-foreground">{{ shown.length }} 个节点</span>
          </div>
          <div class="p-2 sm:p-3">
            <EmptyState v-if="!shown.length" title="这个区间里没有节点" description="换个筛选条件，或去日历视图看看其他日期" :icon="CalendarRange" />
            <div
              v-for="node in shown"
              :key="node.id"
              class="flex cursor-pointer items-center gap-3 rounded-xl px-3 py-3 transition-colors hover:bg-accent/60"
              @click="openNode(node)"
            >
              <div class="w-20 shrink-0 text-[11px] tabular-nums text-muted-foreground">{{ shortRange(node) }}</div>
              <span class="grid size-8 shrink-0 place-items-center rounded-lg bg-primary/8 text-primary">
                <UiIcon :name="node.kind" :size="15" />
              </span>
              <div class="min-w-0 flex-1">
                <b class="block truncate text-sm font-medium">{{ node.name }}</b>
                <small class="mt-0.5 block truncate text-xs text-muted-foreground">
                  {{ node.kindText }} · {{ deptBrief(node) }}
                </small>
              </div>
              <Badge v-if="node.followed" variant="outline" class="hidden sm:inline-flex">已关注</Badge>
              <Badge :variant="stateBadge(node.state)">{{ countdown(node) }}</Badge>
            </div>
          </div>
        </section>
      </template>

      <Dialog
        :open="picked !== null"
        :title="picked?.name ?? '节点详情'"
        :description="picked ? fullRange(picked) : ''"
        @update:open="!$event && (picked = null)"
      >
        <div v-if="picked" class="space-y-5">
          <dl class="kv-list">
            <div class="kv-item"><dt>类型</dt><dd>{{ picked.kindText }}</dd></div>
            <div class="kv-item"><dt>日期</dt><dd>{{ fullRange(picked) }}</dd></div>
            <div class="kv-item"><dt>进度</dt><dd>{{ countdown(picked) }}</dd></div>
            <div class="kv-item"><dt>关注</dt><dd>{{ picked.followed ? '已关注，开始前会提醒你' : '未关注' }}</dd></div>
          </dl>

          <div>
            <p class="section-title">作战说明</p>
            <p class="text-sm leading-relaxed text-muted-foreground">{{ picked.note || '暂无' }}</p>
          </div>

          <div v-if="picked.deptActions.length">
            <p class="section-title">各部门安排</p>
            <div class="space-y-1.5">
              <div v-for="act in picked.deptActions" :key="act.dept" class="flex gap-3 rounded-lg bg-muted/60 px-3 py-2 text-sm">
                <span class="shrink-0 font-medium">{{ act.dept }}</span>
                <span class="text-muted-foreground">{{ act.items.join('、') }}</span>
              </div>
            </div>
          </div>

          <div v-if="picked.linkedRefs.length">
            <p class="section-title">关联资料</p>
            <div class="flex flex-wrap gap-1.5">
              <Badge v-for="ref in picked.linkedRefs" :key="ref" variant="outline">{{ ref }}</Badge>
            </div>
          </div>
        </div>
        <template #footer>
          <div class="mr-auto flex items-center gap-2">
            <span class="text-xs text-muted-foreground">开始前</span>
            <Select v-model="remindBefore" :options="REMIND_OPTIONS" size="sm" class="w-28" />
          </div>
          <Button variant="outline" @click="picked = null">关闭</Button>
          <Button v-if="picked" :variant="picked.followed ? 'outline' : 'default'" @click="toggleFollow(picked)">
            {{ picked.followed ? '取消关注' : '关注' }}
          </Button>
        </template>
      </Dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CalendarRange, ChevronLeft, ChevronRight, Megaphone, RefreshCw } from '@lucide/vue'
import UiIcon from '../../../components/UiIcon.vue'
import { meApi, type RhythmNode } from '../../../lib/me-api'
import { daysBetween } from '../../../lib/format'
import { todayShanghai } from '../../../lib/hr-workspace'
import { toast, errText } from '../../../lib/toast'
import { loadMeSummary } from '../../../lib/me'
import PageHeader from '../../../ui/PageHeader.vue'
import SegmentedTabs from '../../../ui/SegmentedTabs.vue'
import Select from '../../../ui/Select.vue'
import Button from '../../../ui/Button.vue'
import Badge from '../../../ui/Badge.vue'
import Callout from '../../../ui/Callout.vue'
import Dialog from '../../../ui/Dialog.vue'
import EmptyState from '../../../ui/EmptyState.vue'
import type { BadgeTone } from '../../../lib/tone'

const route = useRoute()
const router = useRouter()

const TABS = [
  { key: 'now', title: '进行中' },
  { key: 'calendar', title: '日历视图' },
  { key: 'all', title: '全部节点' }
] as const

const KIND_OPTIONS = [
  { label: '全部类型', value: '' },
  { label: '大促', value: 'sale' },
  { label: '新品', value: 'new' },
  { label: '公司活动', value: 'event' }
]

const REMIND_OPTIONS = [
  { label: '当天', value: 0 },
  { label: '1 天', value: 1 },
  { label: '3 天', value: 3 },
  { label: '7 天', value: 7 }
]

const DOW = ['一', '二', '三', '四', '五', '六', '日']
const nodes = ref<RhythmNode[]>([])
const kind = computed(() => ['sale', 'new', 'event'].includes(String(route.query.kind)) ? String(route.query.kind) : '')
const picked = ref<RhythmNode | null>(null)
const remindBefore = ref(3)
const loadError = ref('')
const loading = ref(false)

const today = todayShanghai()
const cellYear = ref(Number(today.slice(0, 4)))
const cellMonth = ref(Number(today.slice(5, 7)) - 1)

const tab = computed(() => TABS.some(item => item.key === route.query.tab) ? String(route.query.tab) : 'now')

const shown = computed(() => {
  if (tab.value === 'all') return nodes.value
  return nodes.value.filter((n) => (tab.value === 'now' ? n.state === 'ongoing' : n.state !== 'past'))
})

const groupTitle = computed(() => (tab.value === 'now' ? '正在冲刺' : tab.value === 'all' ? '全部节点' : '未结束'))

const current = computed(() => {
  const list = nodes.value
  return list.find((n) => n.state === 'ongoing') ?? list.find((n) => n.state === 'upcoming') ?? null
})

function stateBadge(state: string): BadgeTone {
  return state === 'ongoing' ? 'warning' : state === 'upcoming' ? 'info' : 'success'
}

function kindChip(kindValue: string) {
  if (kindValue === 'sale') return 'bg-destructive/10 text-destructive'
  if (kindValue === 'new') return 'bg-primary/10 text-primary'
  return 'bg-info-soft text-info'
}

function countdown(node: RhythmNode) {
  if (node.state === 'ongoing') return `距结束 ${node.daysLeft} 天`
  if (node.state === 'upcoming') return `倒计时 ${node.daysLeft} 天`
  return `结束 ${Math.abs(node.daysLeft)} 天`
}

/** 进行中的节点要说清跑到第几天了，光看「距结束」推不出总长度。 */
function progressTag(node: RhythmNode) {
  if (node.state === 'ongoing') {
    const span = daysBetween(node.startDate, node.endDate) + 1
    return `第 ${dayIndex(node)} 天 / 共 ${span} 天`
  }
  return node.state === 'upcoming' ? '未开始' : '已结束'
}

function dayIndex(node: RhythmNode) {
  const span = daysBetween(node.startDate, node.endDate) + 1
  return Math.min(Math.max(daysBetween(node.startDate, today) + 1, 1), span)
}

function progressPct(node: RhythmNode) {
  const span = daysBetween(node.startDate, node.endDate) + 1
  return Math.round((dayIndex(node) / span) * 100)
}

function shortRange(node: RhythmNode) {
  return `${node.startDate.slice(5)} ~ ${node.endDate.slice(5)}`
}

function fullRange(node: RhythmNode) {
  return node.startDate === node.endDate ? node.startDate : `${node.startDate} ~ ${node.endDate}`
}

function deptBrief(node: RhythmNode) {
  if (!node.deptActions.length) return node.note || '暂无部门安排'
  return node.deptActions.map((a) => `${a.dept} ${a.items[0]}`).slice(0, 3).join(' · ')
}

function openNode(node: RhythmNode) {
  picked.value = node
}

async function toggleFollow(node: RhythmNode) {
  try {
    if (node.followed) {
      await meApi.unfollowNode(node.id)
      node.followed = false
      toast('已取消关注')
    } else {
      await meApi.followNode(node.id, remindBefore.value)
      node.followed = true
      toast(`已关注，开始前 ${remindBefore.value} 天提醒`)
    }
    void loadMeSummary(true)
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

interface CalendarCell {
  key: string
  date: string
  nodes: RhythmNode[]
}

const cells = computed<CalendarCell[]>(() => {
  const first = new Date(Date.UTC(cellYear.value, cellMonth.value, 1))
  const lead = (first.getUTCDay() + 6) % 7
  const days = new Date(Date.UTC(cellYear.value, cellMonth.value + 1, 0)).getUTCDate()
  const out: CalendarCell[] = []
  for (let i = 0; i < lead; i++) out.push({ key: `lead-${i}`, date: '', nodes: [] })
  for (let d = 1; d <= days; d++) {
    const iso = `${cellYear.value}-${String(cellMonth.value + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    out.push({ key: iso, date: iso, nodes: nodes.value.filter((n) => n.startDate <= iso && iso <= n.endDate) })
  }
  return out
})

function moveMonth(step: number) {
  const next = new Date(Date.UTC(cellYear.value, cellMonth.value + step, 1))
  cellYear.value = next.getUTCFullYear()
  cellMonth.value = next.getUTCMonth()
}

function backToday() {
  const now = todayShanghai()
  cellYear.value = Number(now.slice(0, 4))
  cellMonth.value = Number(now.slice(5, 7)) - 1
}

function select(key: string, value: string) {
  router.push({ path: '/me/rhythm', query: { ...route.query, [key]: value } })
}

function onKind(value: string | number | null | undefined) {
  select('kind', value == null ? '' : String(value))
}

let listRequest = 0
async function load() {
  const request = ++listRequest
  loading.value = true
  loadError.value = ''
  try {
    const params: { kind?: string; from?: string; to?: string } = kind.value ? { kind: kind.value } : {}
    if (tab.value === 'calendar') {
      const prefix = `${cellYear.value}-${String(cellMonth.value + 1).padStart(2, '0')}`
      params.from = `${prefix}-01`
      params.to = `${prefix}-${new Date(Date.UTC(cellYear.value, cellMonth.value + 1, 0)).getUTCDate()}`
    }
    const result = await meApi.rhythmNodes(params)
    if (request === listRequest) nodes.value = result
  } catch (e) {
    if (request === listRequest) { nodes.value = []; loadError.value = errText(e) }
  } finally {
    if (request === listRequest) loading.value = false
  }
}

watch([tab, kind], load)
watch([cellYear, cellMonth], () => { if (tab.value === 'calendar') void load() })

onMounted(load)
</script>
