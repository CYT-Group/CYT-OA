<template>
  <div>
  <PageHeader
    title="访问分析"
    description="展示页每次进页面打的浏览埋点：浏览量、访客与热门内容。今日按服务器时区零点起算。"
  >
    <template #actions>
      <Button variant="outline" size="sm" :disabled="loading" @click="loadAll">
        <RefreshCw :class="['size-3.5', loading && 'animate-spin']" />
        刷新
      </Button>
    </template>
  </PageHeader>

  <div class="mt-5 flex flex-wrap items-center gap-3">
    <SegmentedTabs
      :model-value="String(days)"
      :items="rangeTabs"
      @update:model-value="(v) => setDays(Number(v))"
    />
    <span v-if="summary" class="text-xs text-muted-foreground">
      窗口内被访问过的页面 {{ n(summary.activePages) }} 个
    </span>
  </div>

  <Callout v-if="err" tone="destructive" title="看板数据加载失败" class="mt-4">
    {{ err }}
    <template #action>
      <Button variant="outline" size="sm" @click="loadAll">重试</Button>
    </template>
  </Callout>

  <div class="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
    <StatCard
      v-for="s in statCards"
      :key="s.label"
      :title="s.label"
      :value="s.value"
      :hint="s.desc"
      :icon="s.icon"
      :tone="s.tone"
    />
  </div>

  <section class="panel mt-4">
    <div class="panel-header">
      <div class="panel-title">
        每日趋势
        <span v-if="chart" class="text-xs font-normal text-muted-foreground">
          峰值 {{ n(chart.peak.pv) }} PV · {{ chart.peak.date.slice(5) }}
        </span>
      </div>
      <div class="flex items-center gap-4 text-xs text-muted-foreground">
        <span class="flex items-center gap-1.5"><i class="size-1.5 rounded-full bg-primary"></i>PV 浏览量</span>
        <span class="flex items-center gap-1.5"><i class="size-1.5 rounded-full bg-muted-foreground/45"></i>UV 访客数</span>
      </div>
    </div>
    <div ref="chartHost" class="p-4">
      <div v-if="loading && !trend.length" class="space-y-3">
        <Skeleton class="h-[200px] w-full" />
      </div>
      <div v-else-if="chart" class="relative w-full" @mouseleave="hover = null">
        <svg
          :key="days"
          :viewBox="`0 0 ${chart.W} ${chart.H}`"
          :width="chart.W"
          height="240"
          class="block w-full select-none"
          role="img"
          aria-label="每日 PV 与 UV 趋势"
        >
          <defs>
            <linearGradient id="analytics-area" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" style="stop-color: var(--primary); stop-opacity: 0.14" />
              <stop offset="100%" style="stop-color: var(--primary); stop-opacity: 0" />
            </linearGradient>
          </defs>

          <g class="text-muted-foreground">
            <line
              v-for="g in chart.grids"
              :key="g.y"
              :x1="chart.padL"
              :x2="chart.W - chart.padR"
              :y1="g.y"
              :y2="g.y"
              stroke="var(--border)"
              stroke-dasharray="3 4"
            />
            <text
              v-for="g in chart.grids.filter((g) => g.label !== '')"
              :key="`l-${g.y}`"
              :x="chart.padL - 8"
              :y="g.y + 3"
              text-anchor="end"
              font-size="10"
              fill="currentColor"
              class="tabular-nums"
            >
              {{ g.label }}
            </text>
            <text
              v-for="t in chart.xTicks"
              :key="`x-${t.i}`"
              :x="t.x"
              :y="chart.H - 8"
              :text-anchor="t.i === 0 ? 'start' : t.i === trend.length - 1 ? 'end' : 'middle'"
              font-size="10"
              fill="currentColor"
              class="tabular-nums"
            >
              {{ t.label }}
            </text>
          </g>

          <path v-if="chart.pvArea" :d="chart.pvArea" fill="url(#analytics-area)" />
          <path
            v-if="chart.pvLine"
            :d="chart.pvLine"
            fill="none"
            stroke="var(--primary)"
            stroke-width="2"
            stroke-linecap="round"
            pathLength="1"
            class="chart-line"
          />
          <path
            v-if="chart.uvLine"
            :d="chart.uvLine"
            fill="none"
            stroke="var(--muted-foreground)"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-opacity="0.45"
            pathLength="1"
            class="chart-line chart-line-late"
          />
          <circle
            v-if="chart.points.length === 1"
            :cx="chart.points[0].x"
            :cy="chart.points[0].y"
            r="3.5"
            fill="var(--primary)"
          />

          <g v-if="hoverPoint">
            <line
              :x1="hoverPoint.x"
              :x2="hoverPoint.x"
              :y1="chart.padT"
              :y2="chart.baseline"
              stroke="var(--border-strong)"
              stroke-dasharray="3 3"
            />
            <circle :cx="hoverPoint.x" :cy="hoverPoint.yUv" r="3" fill="var(--muted-foreground)" stroke="var(--card)" stroke-width="2" />
            <circle :cx="hoverPoint.x" :cy="hoverPoint.y" r="3.5" fill="var(--primary)" stroke="var(--card)" stroke-width="2" />
          </g>

          <rect
            v-for="z in chart.zones"
            :key="z.index"
            :x="z.x"
            :y="chart.padT"
            :width="z.width"
            :height="chart.baseline - chart.padT"
            fill="transparent"
            @mouseenter="hover = z.index"
          />
        </svg>

        <div
          v-if="hoverPoint"
          class="pointer-events-none absolute z-10 w-36 rounded-lg border border-border bg-card px-2.5 py-2 shadow-xl"
          :style="{ left: tipLeft, top: tipTop, transform: 'translate(-50%, calc(-100% - 12px))' }"
        >
          <p class="text-[11px] tabular-nums text-muted-foreground">{{ hoverPoint.date }}</p>
          <div class="mt-1.5 space-y-1 text-xs">
            <p class="flex items-center justify-between gap-4">
              <span class="flex items-center gap-1.5 text-muted-foreground">
                <i class="size-1.5 rounded-full bg-primary"></i>PV
              </span>
              <b class="tabular-nums">{{ n(hoverPoint.pv) }}</b>
            </p>
            <p class="flex items-center justify-between gap-4">
              <span class="flex items-center gap-1.5 text-muted-foreground">
                <i class="size-1.5 rounded-full bg-muted-foreground/45"></i>UV
              </span>
              <b class="tabular-nums">{{ n(hoverPoint.uv) }}</b>
            </p>
          </div>
        </div>
      </div>
      <EmptyState
        v-else
        :icon="ChartNoAxesColumn"
        title="暂无趋势数据"
        description="换一个时间窗口，或确认展示页是否已接入浏览埋点"
      />
    </div>
  </section>

  <div class="mt-4 grid items-start gap-4 lg:grid-cols-2">
    <section class="panel">
      <div class="panel-header">
        <div class="panel-title">热门页面</div>
        <span class="text-xs text-muted-foreground">按 PV 排序 · Top 10</span>
      </div>
      <div v-if="pages.length" class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th class="w-9"></th>
              <th>页面</th>
              <th class="w-24 text-right">PV</th>
              <th class="w-16 text-right">UV</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(p, i) in pages" :key="p.pageUrl">
              <td class="text-xs tabular-nums text-muted-foreground/70">{{ String(i + 1).padStart(2, '0') }}</td>
              <td class="max-w-0">
                <div class="truncate font-medium">{{ p.pageTitle || p.pageUrl }}</div>
                <div class="truncate text-xs text-muted-foreground">{{ p.pageUrl }}</div>
              </td>
              <td>
                <div class="flex items-center justify-end gap-2">
                  <span class="text-xs tabular-nums text-muted-foreground">{{ n(p.pv) }}</span>
                  <span class="h-1.5 w-14 overflow-hidden rounded-full bg-muted">
                    <span class="block h-full rounded-full bg-primary/70" :style="{ width: pvWidth(p.pv) }"></span>
                  </span>
                </div>
              </td>
              <td class="text-right tabular-nums">{{ n(p.uv) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState v-else title="暂无数据" description="该窗口内还没有页面访问记录" />
    </section>

    <section class="panel">
      <div class="panel-header">
        <div class="panel-title">活跃用户</div>
        <span class="text-xs text-muted-foreground">Top 10</span>
      </div>
      <div v-if="users.length" class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>访问人</th>
              <th class="w-16 text-right">PV</th>
              <th class="w-20 text-right">页面数</th>
              <th class="w-20 text-right">轨迹</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.username">
              <td class="max-w-0">
                <div class="flex items-center gap-2.5">
                  <Avatar :name="u.displayName || u.username" size="sm" />
                  <div class="min-w-0">
                    <div class="truncate font-medium">{{ u.displayName || u.username }}</div>
                    <div class="truncate text-xs text-muted-foreground">
                      {{ [u.username, u.department].filter(Boolean).join(' · ') }}
                    </div>
                  </div>
                </div>
              </td>
              <td class="text-right tabular-nums">{{ n(u.pageViews) }}</td>
              <td class="text-right tabular-nums">{{ n(u.uniquePages) }}</td>
              <td class="text-right">
                <Button
                  variant="ghost"
                  size="sm"
                  :disabled="historyFor === u.username && loadingHistory"
                  @click="openHistory(u.username)"
                >
                  {{ historyFor === u.username ? '已展开' : '查看' }}
                </Button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState v-else title="暂无数据" description="匿名访问不计入活跃用户统计" />
    </section>
  </div>

  <section v-if="historyFor" class="panel mt-4">
    <div class="panel-header">
      <div class="panel-title">{{ historyName }} 的浏览轨迹</div>
      <span class="text-xs text-muted-foreground">近 {{ days }} 天 · 最多 50 条</span>
    </div>
    <div class="panel-body">
      <p v-if="loadingHistory" class="text-sm text-muted-foreground">加载中…</p>
      <div v-else-if="history.length" class="space-y-0">
        <div
          v-for="(h, i) in history"
          :key="`${h.pageUrl}-${h.viewedAt}-${i}`"
          class="relative flex gap-3 pb-4 last:pb-0"
        >
          <div class="flex flex-col items-center">
            <span
              class="flex size-7 shrink-0 items-center justify-center rounded-full border border-border bg-muted/60 text-muted-foreground"
            >
              <Eye class="size-3.5" />
            </span>
            <span v-if="i < history.length - 1" class="mt-1 w-px flex-1 bg-border"></span>
          </div>
          <div class="min-w-0 flex-1 pb-0.5">
            <div class="flex flex-wrap items-baseline gap-x-2">
              <span class="truncate text-sm font-medium">{{ h.pageTitle || h.pageUrl }}</span>
              <span class="text-xs tabular-nums text-muted-foreground">{{ fmt(h.viewedAt) }}</span>
            </div>
            <div class="truncate text-xs text-muted-foreground">{{ h.pageUrl }}</div>
          </div>
        </div>
      </div>
      <EmptyState v-else title="该窗口内没有记录" />
    </div>
  </section>

  <p class="mt-5 text-xs text-muted-foreground">数据来自展示页浏览埋点；长窗口的查询会慢一些。</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { CalendarDays, ChartNoAxesColumn, Eye, RefreshCw, UserCheck, Users } from '@lucide/vue'
import Avatar from '../../ui/Avatar.vue'
import Button from '../../ui/Button.vue'
import Callout from '../../ui/Callout.vue'
import EmptyState from '../../ui/EmptyState.vue'
import PageHeader from '../../ui/PageHeader.vue'
import SegmentedTabs from '../../ui/SegmentedTabs.vue'
import Skeleton from '../../ui/Skeleton.vue'
import StatCard from '../../ui/StatCard.vue'
import { api } from '../../lib/api'
import { errText } from '../../lib/toast'

interface Summary {
  days: number
  totalPv: number | string
  totalUv: number | string
  todayPv: number | string
  todayUv: number | string
  activePages: number | string
}
interface Trend { date: string; pv: number | string; uv: number | string }
interface TopPage { pageUrl: string; pageTitle?: string; pv: number | string; uv: number | string }
interface ActiveUser { username: string; displayName?: string; department?: string; pageViews: number | string; uniquePages: number | string }
interface HistoryRow { pageUrl: string; pageTitle?: string; viewedAt: string }

const RANGES = [7, 30, 90, 365]
const rangeTabs = RANGES.map((d) => ({ value: String(d), label: `近 ${d} 天` }))

const days = ref(30)
const summary = ref<Summary | null>(null)
const trend = ref<Trend[]>([])
const pages = ref<TopPage[]>([])
const users = ref<ActiveUser[]>([])
const history = ref<HistoryRow[]>([])
const historyFor = ref('')
const historyName = ref('')
const loading = ref(false)
const loadingHistory = ref(false)
const err = ref('')
const hover = ref<number | null>(null)

/** Long 走字符串序列化，拿到手是 "123"，不转数值就没法比大小 */
const n = (v: unknown) => Number(v ?? 0)

const statCards = computed(() => {
  const s = summary.value
  return [
    { label: '窗口浏览量', value: s ? String(n(s.totalPv)) : '…', desc: `近 ${days.value} 天 PV`, icon: Eye, tone: 'primary' as const },
    { label: '窗口访问人数', value: s ? String(n(s.totalUv)) : '…', desc: `近 ${days.value} 天去重`, icon: Users, tone: 'info' as const },
    { label: '今日浏览量', value: s ? String(n(s.todayPv)) : '…', desc: '服务器时区零点起算', icon: CalendarDays, tone: 'success' as const },
    { label: '今日访问人数', value: s ? String(n(s.todayUv)) : '…', desc: '今日去重', icon: UserCheck, tone: 'warning' as const }
  ]
})

const chartHost = ref<HTMLElement | null>(null)
const chartWidth = ref(860)
let observer: ResizeObserver | null = null

/** 有量级差的计数轴：1/1.5/2/3/5/7.5 × 10^n，避免 120 顶到 200 的空白 */
function niceMax(v: number) {
  const target = Math.max(1, v)
  const mag = Math.pow(10, Math.floor(Math.log10(target)))
  const norm = target / mag
  const step = norm <= 1 ? 1 : norm <= 1.5 ? 1.5 : norm <= 2 ? 2 : norm <= 3 ? 3 : norm <= 5 ? 5 : norm <= 7.5 ? 7.5 : 10
  return Math.max(4, step * mag)
}

/** Catmull-Rom 转三次贝塞尔，控制点夹在绘图区内，避免曲线冲出网格 */
function smoothPath(pts: [number, number][]) {
  if (pts.length < 2) return ''
  const top = Math.min(...pts.map((p) => p[1]))
  const bottom = Math.max(...pts.map((p) => p[1]))
  const clamp = (y: number) => Math.min(bottom, Math.max(top, y))
  let d = `M ${pts[0][0]} ${pts[0][1]}`
  for (let i = 0; i < pts.length - 1; i++) {
    const p0 = pts[i - 1] ?? pts[i]
    const p1 = pts[i]
    const p2 = pts[i + 1]
    const p3 = pts[i + 2] ?? p2
    const t = 0.16
    d += ` C ${p1[0] + (p2[0] - p0[0]) * t} ${clamp(p1[1] + (p2[1] - p0[1]) * t)}, ${p2[0] - (p3[0] - p1[0]) * t} ${clamp(p2[1] - (p3[1] - p1[1]) * t)}, ${p2[0]} ${p2[1]}`
  }
  return d
}

const chart = computed(() => {
  const list = trend.value
  if (!list.length) return null
  const W = chartWidth.value
  const H = 240
  const padL = 34
  const padR = 10
  const padT = 12
  const padB = 26
  const innerW = W - padL - padR
  const innerH = H - padT - padB
  const baseline = padT + innerH
  const yMax = niceMax(Math.max(...list.map((t) => Math.max(n(t.pv), n(t.uv)))))
  const xAt = (i: number) => padL + (list.length === 1 ? innerW / 2 : (i / (list.length - 1)) * innerW)
  const yAt = (v: number | string) => padT + innerH - (n(v) / yMax) * innerH

  const pvPts: [number, number][] = list.map((t, i) => [xAt(i), yAt(t.pv)])
  const uvPts: [number, number][] = list.map((t, i) => [xAt(i), yAt(t.uv)])
  const pvLine = smoothPath(pvPts)
  const pvArea = pvLine ? `${pvLine} L ${pvPts[pvPts.length - 1][0]} ${baseline} L ${pvPts[0][0]} ${baseline} Z` : ''

  const grids = [0, 0.25, 0.5, 0.75, 1].map((r) => ({
    y: padT + innerH - r * innerH,
    label: r === 0 || r === 0.5 || r === 1 ? String(Math.round(yMax * r)) : ''
  }))

  const step = Math.max(1, Math.ceil(list.length / 6))
  const ticks: { i: number; x: number; label: string }[] = []
  for (let i = 0; i < list.length; i += step) ticks.push({ i, x: xAt(i), label: list[i].date.slice(5) })
  if (ticks[ticks.length - 1]?.i !== list.length - 1) {
    ticks.push({ i: list.length - 1, x: xAt(list.length - 1), label: list[list.length - 1].date.slice(5) })
  }

  const bandW = innerW / Math.max(1, list.length - 1)
  const zones = list.map((_, i) => ({
    index: i,
    x: Math.max(padL, Math.min(padL + innerW - bandW, xAt(i) - bandW / 2)),
    width: list.length === 1 ? innerW : bandW
  }))

  const points = list.map((t, i) => ({ x: xAt(i), y: yAt(t.pv), yUv: yAt(t.uv), date: t.date, pv: t.pv, uv: t.uv }))
  const peak = list.reduce((best, t) => (n(t.pv) > n(best.pv) ? t : best), list[0])

  return { W, H, padL, padR, padT, padB, baseline, pvLine, uvLine: smoothPath(uvPts), pvArea, grids, xTicks: ticks, zones, points, peak }
})

const hoverPoint = computed(() => {
  const c = chart.value
  if (!c || hover.value == null) return null
  return c.points[hover.value] ?? null
})

const tipLeft = computed(() => {
  const c = chart.value
  if (!c || !hoverPoint.value) return '0%'
  const pct = (hoverPoint.value.x / c.W) * 100
  return `${Math.min(92, Math.max(8, pct))}%`
})

const tipTop = computed(() => {
  if (!hoverPoint.value) return '0%'
  return `${(hoverPoint.value.y / 240) * 100}%`
})

function pvWidth(v: number | string) {
  const top = Math.max(1, ...pages.value.map((p) => n(p.pv)))
  return `${Math.max(2, Math.round((n(v) / top) * 100))}%`
}

function fmt(v: unknown) {
  const s = String(v ?? '')
  return /^\d{4}-\d{2}-\d{2}T\d{2}/.test(s) ? s.slice(5, 16).replace('T', ' ') : s
}

async function loadAll() {
  loading.value = true
  err.value = ''
  hover.value = null
  history.value = []
  historyFor.value = ''
  historyName.value = ''
  const d = { days: days.value }
  try {
    const [s, t, p, u] = await Promise.all([
      api.get<Summary>('/admin-api/portal/analytics/summary', d),
      api.get<Trend[]>('/admin-api/portal/analytics/daily-trend', d),
      api.get<TopPage[]>('/admin-api/portal/analytics/popular-pages', { ...d, limit: 10 }),
      api.get<ActiveUser[]>('/admin-api/portal/analytics/active-users', { ...d, limit: 10 })
    ])
    summary.value = s
    trend.value = t ?? []
    pages.value = p ?? []
    users.value = u ?? []
  } catch (e) {
    err.value = errText(e, '看板数据加载失败')
    summary.value = null
    trend.value = []
    pages.value = []
    users.value = []
  } finally {
    loading.value = false
  }
}

function setDays(v: number) {
  if (days.value === v) return
  days.value = v
  loadAll()
}

async function openHistory(username: string) {
  if (historyFor.value === username) return
  loadingHistory.value = true
  try {
    history.value = await api.get<HistoryRow[]>('/admin-api/portal/analytics/user-history', {
      username,
      days: days.value,
      limit: 50
    })
    historyFor.value = username
    historyName.value = users.value.find((u) => u.username === username)?.displayName || username
  } catch (e) {
    err.value = errText(e, '轨迹加载失败')
  } finally {
    loadingHistory.value = false
  }
}

onMounted(() => {
  if (chartHost.value) {
    observer = new ResizeObserver((entries) => {
      chartWidth.value = Math.max(320, Math.round(entries[0].contentRect.width))
    })
    observer.observe(chartHost.value)
  }
  loadAll()
})

onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped>
.chart-line {
  stroke-dasharray: 1;
  stroke-dashoffset: 1;
  animation: chart-draw 0.9s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}

.chart-line-late {
  animation-delay: 0.12s;
}

@keyframes chart-draw {
  to {
    stroke-dashoffset: 0;
  }
}
</style>
