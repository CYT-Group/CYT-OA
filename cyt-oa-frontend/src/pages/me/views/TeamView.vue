<template>
  <div>
  <PageHeader
    title="我的团队"
    description="周报看板、待点评与团队关怀集中在一个视图里"
  >
    <template #actions>
      <Button variant="outline" size="sm" :loading="loading || radarLoading" @click="refresh">
        <RefreshCw class="size-3.5" />
        刷新
      </Button>
    </template>
  </PageHeader>

  <p v-if="loading && !board" class="py-10 text-center text-sm text-muted-foreground" role="status">正在读取团队…</p>
  <div v-else-if="loadError" class="mt-4">
    <Callout tone="destructive" title="加载失败">{{ loadError }}</Callout>
  </div>

  <template v-else-if="board">
    <div class="mt-5 grid grid-cols-2 gap-3 xl:grid-cols-4">
      <StatCard
        title="本周周报提交"
        :value="`${board.submittedThisWeek}/${board.members.length}`"
        :hint="`${pendingMembers} 人本周还没交`"
        :icon="Send"
        :tone="rateTone"
      />
      <StatCard title="待我点评" :value="board.pendingReviews.length" hint="已提交、还没写过点评" :icon="MessageSquare" tone="warning" />
      <StatCard title="团队人数" :value="board.members.length" hint="来自人事档案的直属下级" :icon="Users" tone="info" />
      <StatCard
        title="看板窗口"
        :value="`${board.weeks.length} 周`"
        :hint="`${board.weeks[0]?.label ?? '—'} 起`"
        :icon="CalendarRange"
        tone="success"
      />
    </div>

    <div class="mt-4 rounded-xl border border-warning/25 bg-warning-soft p-4 text-warning">
      <div class="flex flex-wrap items-center gap-3">
        <span class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-warning/15">
          <Award class="size-4" />
        </span>
        <div class="min-w-0 flex-1">
          <p class="text-sm font-semibold">
            {{ board.star ? `本月之星：${board.star.name}` : '本月还没提名之星' }}
          </p>
          <p class="mt-0.5 text-xs text-warning/90">
            {{ board.star ? '你提名的，在对方的「收到的认可」里能看到' : '在下方看板的「认可」列点 ★ 提名' }}
          </p>
        </div>
        <Button v-if="board.star" variant="outline" size="sm" class="border-warning/30 bg-card text-warning hover:bg-warning/10" @click="praise(board.star.userId)">
          公开表扬
        </Button>
      </div>
    </div>

    <SegmentedTabs class="mt-6" :model-value="tab" :items="tabItems" @update:model-value="pick" />

    <template v-if="tab === 'milestone'">
      <h2 class="mt-5 text-sm font-semibold">团队祝贺与关怀</h2>
      <Callout v-if="radarError" class="mt-3" title="团队里程碑未读取" tone="destructive">{{ radarError }}</Callout>
      <p v-else-if="radarLoading" class="py-8 text-center text-sm text-muted-foreground" role="status">正在读取团队里程碑…</p>
      <EmptyState v-else-if="!radar.length" title="近期暂无团队祝贺与关怀事项" description="团队成员的生日、入职周年和转正评估会显示在这里" compact />
      <div v-else class="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <div
          v-for="r in radar"
          :key="r.userId + r.type + r.date"
          class="flex items-center gap-3 rounded-xl border bg-card p-4 shadow-xs"
        >
          <span class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <UiIcon :name="r.type" :size="16" />
          </span>
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-medium">{{ r.name }} · {{ r.daysLeft === 0 ? '今天' : fmtMD(r.date) }}{{ r.typeText }}</p>
            <p class="mt-0.5 truncate text-xs text-muted-foreground">{{ r.deptName || '—' }} · {{ mileSub(r) }}</p>
          </div>
          <Button
            v-if="r.action === 'bless'"
            size="sm"
            :variant="r.blessed ? 'outline' : 'default'"
            :disabled="r.blessed || blessingBusy === `${r.userId}:${r.type}:${r.date}`"
            @click="bless(r)"
          >{{ r.blessed ? '已送祝福' : '送祝福' }}</Button>
          <Button v-else variant="outline" size="sm" @click="arrange(r)">去安排</Button>
        </div>
      </div>
    </template>

    <!-- 周报看板 -->
    <section v-if="tab === 'board'" class="mt-4 overflow-hidden rounded-xl border bg-card shadow-xs">
      <header class="flex flex-wrap items-center justify-between gap-2 border-b px-4 py-3.5">
        <div>
          <h2 class="text-sm font-semibold">周报提交看板 · 我的下属</h2>
          <p class="mt-0.5 text-xs text-muted-foreground">点格子看正文，认可列给下属点赞 / 提名之星</p>
        </div>
        <div class="flex items-center gap-3 text-[11px] text-muted-foreground">
          <span class="inline-flex items-center gap-1"><i class="inline-block size-2.5 rounded-sm bg-success" />已交</span>
          <span class="inline-flex items-center gap-1"><i class="inline-block size-2.5 rounded-sm bg-warning" />有草稿</span>
          <span class="inline-flex items-center gap-1"><i class="inline-block size-2.5 rounded-sm bg-destructive" />未交</span>
        </div>
      </header>
      <div class="overflow-x-auto">
        <table class="w-full min-w-[760px] border-collapse text-sm">
          <thead>
            <tr class="border-b bg-muted/50 text-left text-xs text-muted-foreground">
              <th class="px-4 py-2.5 font-medium">成员</th>
              <th v-for="w in board.weeks" :key="w.periodKey" class="px-1 py-2.5 text-center font-medium">{{ w.label }}</th>
              <th class="px-3 py-2.5 font-medium">认可</th>
              <th class="px-4 py-2.5 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in board.members" :key="m.userId" class="border-b last:border-b-0 hover:bg-muted/30">
              <td class="px-4 py-2.5">
                <div class="flex items-center gap-2.5">
                  <Avatar :name="m.name" size="sm" />
                  <div class="min-w-0">
                    <p class="truncate text-sm font-medium">{{ m.name }}</p>
                    <p class="truncate text-[11px] text-muted-foreground">{{ m.postName || m.deptName || '—' }}</p>
                  </div>
                </div>
              </td>
              <td v-for="cell in m.cells" :key="cell.periodKey" class="px-1 py-2.5 text-center">
                <button
                  type="button"
                  class="inline-flex size-7 items-center justify-center rounded-md border text-xs font-semibold transition-transform hover:scale-110 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
                  :class="cellClass(cell.state)"
                  :title="`${cell.periodKey} ${cellText(cell)}`"
                  @click="openReport(cell)"
                >{{ SQ_CHAR[cell.state] ?? '·' }}</button>
              </td>
              <td class="px-3 py-2.5">
                <div class="flex items-center gap-1.5">
                  <Button
                    size="sm"
                    :variant="m.liked ? 'soft' : 'outline'"
                    :title="m.liked ? '本周已经点过赞了' : '本周点赞'"
                    @click="like(m)"
                  >
                    <Heart class="size-3.5" :class="m.liked ? 'fill-current' : ''" />
                    赞
                  </Button>
                  <Button
                    size="sm"
                    :variant="m.starred ? 'soft' : 'outline'"
                    :title="m.starred ? '本月之星就是你提的' : '提名本月之星'"
                    @click="nominate(m)"
                  >
                    <Star class="size-3.5" :class="m.starred ? 'fill-current' : ''" />
                    之星
                  </Button>
                </div>
              </td>
              <td class="px-4 py-2.5">
                <Button variant="ghost" size="sm" @click="assign(m)">
                  <Send class="size-3.5" />
                  派任务
                </Button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="!board.members.length" class="px-4 py-10 text-center text-sm text-muted-foreground">还没有直属下级</div>
    </section>

    <!-- 待我点评 -->
    <section v-else-if="tab === 'review'" class="mt-4 overflow-hidden rounded-xl border bg-card shadow-xs">
      <header class="border-b px-4 py-3.5">
        <h2 class="text-sm font-semibold">团队待我处理</h2>
      </header>
      <EmptyState v-if="!board.pendingReviews.length" :icon="MessageSquare" title="下属的周报都点评过了" />
      <ul v-else class="divide-y">
        <li v-for="r in board.pendingReviews" :key="r.id" class="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center">
          <span class="w-20 shrink-0 text-xs tabular-nums text-muted-foreground">{{ r.periodKey }}</span>
          <span class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
            <FileText class="size-4" />
          </span>
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-medium">{{ r.title || `${r.authorName ?? '成员'}的周报` }}</p>
            <p class="text-xs text-muted-foreground">{{ r.authorName }} · {{ relTime(r.submittedTime) }}</p>
          </div>
          <div class="flex items-center gap-2 pl-10 sm:pl-0">
            <Badge variant="info">待点评</Badge>
            <Button size="sm" @click="openReview(r.id)">阅 / 点评</Button>
          </div>
        </li>
      </ul>
    </section>

    <!-- 成员与动态 -->
    <section v-else-if="tab === 'members'" class="mt-4 space-y-3">
      <Input v-model="memberSearch" aria-label="搜索团队成员" placeholder="搜索姓名、部门或岗位" class="sm:max-w-sm" />
      <EmptyState v-if="!filteredMembers.length" :title="memberSearch ? '没有匹配的团队成员' : '还没有直属下级'" compact />
      <div v-else class="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
      <div v-for="m in filteredMembers" :key="m.userId" class="min-w-0 rounded-xl border bg-card p-4 shadow-xs">
        <div class="flex items-center gap-3">
          <Avatar :name="m.name" />
          <div class="min-w-0">
            <p class="text-sm font-semibold">{{ m.name }}</p>
            <p class="truncate text-xs text-muted-foreground">{{ m.postName || '—' }} · {{ m.deptName || '—' }}</p>
          </div>
        </div>
        <dl class="mt-4 space-y-2 text-xs">
          <div class="flex items-center justify-between gap-3">
            <dt class="text-muted-foreground">入职</dt>
            <dd class="font-medium">{{ fmtDay(m.entryTime) }}</dd>
          </div>
          <div class="flex items-center justify-between gap-3">
            <dt class="text-muted-foreground">本周周报</dt>
            <dd class="font-medium">{{ weekState(m) }}</dd>
          </div>
          <div class="flex items-center justify-between gap-3">
            <dt class="text-muted-foreground">近 {{ board.weeks.length }} 周</dt>
            <dd class="font-medium">{{ submittedCount(m) }} 份已交</dd>
          </div>
        </dl>
        <div class="mt-4 flex items-center gap-2">
          <Button variant="outline" size="sm" @click="assign(m)">派任务</Button>
          <Button variant="ghost" size="sm" @click="nudge(m)">催一催</Button>
        </div>
      </div>
      </div>
    </section>

    <!-- 给团队发话 -->
    <section class="mt-6 rounded-xl border bg-card p-4 shadow-xs">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 class="text-sm font-semibold">向团队发通知 / 派任务</h2>
          <p class="mt-0.5 text-xs text-muted-foreground">发送后出现在成员的提醒与日程里</p>
        </div>
        <span class="text-xs text-muted-foreground">已选 {{ pickedUsers.length }} / {{ board.members.length }} 人</span>
      </div>

      <div class="mt-3 space-y-2">
        <Input v-model.trim="note.title" placeholder="标题：一句话说清要什么" />
        <Textarea v-model.trim="note.description" :rows="2" placeholder="补充说明（可留空）" />
      </div>

      <div class="mt-3 flex flex-wrap items-center gap-1.5">
        <span class="mr-1 text-xs text-muted-foreground">收件人</span>
        <button
          v-for="m in board.members"
          :key="m.userId"
          type="button"
          class="inline-flex items-center gap-1 rounded-md border px-2 py-1 text-xs font-medium transition-colors"
          :class="pickedUsers.includes(m.userId)
            ? 'border-primary/30 bg-primary/10 text-primary'
            : 'border-input bg-card text-muted-foreground hover:bg-accent'"
          @click="toggleUser(m.userId)"
        >
          <Check v-if="pickedUsers.includes(m.userId)" class="size-3" />
          {{ m.name }}
        </button>
        <Button variant="ghost" size="sm" @click="pickAll">
          {{ pickedUsers.length === board.members.length ? '清空' : '全选' }}
        </Button>
      </div>

      <div class="mt-3 flex flex-wrap items-center gap-3 border-t pt-3">
        <label class="flex items-center gap-2 text-xs text-muted-foreground">
          截止
          <Input v-model="note.endTime" type="datetime-local" class="h-8 w-48 text-xs" />
        </label>
        <Button class="ml-auto" :disabled="sending" @click="sendNote">
          <LoaderCircle v-if="sending" class="size-4 animate-spin" />
          {{ sending ? '发送中…' : `发给 ${pickedUsers.length} 人` }}
        </Button>
      </div>
    </section>
  </template>

  <Dialog
    :open="!!detail"
    :title="detail ? (detail.title || `${detail.authorName ?? ''}的周报`) : ''"
    :description="detail ? `${detail.periodKey} · ${detail.authorName} · ${fmtTime(detail.createTime)}` : undefined"
    size="lg"
    @update:open="(value) => { if (!value) closeDetail() }"
  >
    <div v-if="detail" class="space-y-5">
      <section v-if="detail.workItems.length">
        <h3 class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">本周完成</h3>
        <ul class="mt-2 space-y-1.5">
          <li v-for="(item, i) in detail.workItems" :key="'w' + i" class="flex items-center justify-between gap-3 rounded-lg border px-3 py-2">
            <span class="min-w-0 text-sm">{{ item.content }}</span>
            <Badge variant="secondary">进度 {{ item.progress ?? 0 }}%</Badge>
          </li>
        </ul>
      </section>

      <section v-if="detail.summary">
        <h3 class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">总结</h3>
        <p class="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-muted-foreground">{{ detail.summary }}</p>
      </section>

      <section>
        <h3 class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">下周计划</h3>
        <ul v-if="detail.planItems.length" class="mt-2 space-y-1.5">
          <li v-for="(p, i) in detail.planItems" :key="'p' + i" class="rounded-lg border px-3 py-2 text-sm">{{ p }}</li>
        </ul>
        <p v-else class="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-muted-foreground">{{ detail.plan || '未填写' }}</p>
      </section>

      <section v-if="detail.problem">
        <h3 class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">风险 / 需要支持</h3>
        <p class="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-muted-foreground">{{ detail.problem }}</p>
      </section>

      <section v-if="!reviewed" class="border-t pt-4">
        <h3 class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">写点评</h3>
        <div class="mt-3 grid gap-3 sm:grid-cols-[140px_1fr]">
          <div>
            <Label>评级</Label>
            <Select v-model="grade" :options="gradeOptions" />
          </div>
          <div class="flex flex-col">
            <Label required>点评内容</Label>
            <Textarea v-model.trim="reviewText" :rows="3" placeholder="先说做得好的，再给一条具体的下一步" />
            <p v-if="commentError" class="mt-1 text-xs text-destructive">{{ commentError }}</p>
          </div>
        </div>
      </section>
      <section v-else class="border-t pt-4">
        <h3 class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">我的点评</h3>
        <div class="mt-2 rounded-lg border-l-2 border-primary/40 bg-muted/50 px-3 py-2 text-sm leading-relaxed">
          <span class="mr-2 text-xs font-medium text-primary">已点评</span>{{ reviewed }}
        </div>
      </section>
    </div>
    <template #footer>
      <Button variant="outline" @click="closeDetail">关闭</Button>
      <Button v-if="!reviewed" :disabled="busy" @click="submitComment">
        <LoaderCircle v-if="busy" class="size-4 animate-spin" />
        {{ busy ? '提交中…' : '提交点评' }}
      </Button>
    </template>
  </Dialog>
  </div>
</template>

<script setup lang="ts">
import {
  Award,
  CalendarRange,
  Check,
  FileText,
  Heart,
  LoaderCircle,
  MessageSquare,
  RefreshCw,
  Send,
  Star,
  Users
} from '@lucide/vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UiIcon from '../../../components/UiIcon.vue'
import { fmtDay, fmtMD, fmtTime, relTime } from '../../../lib/format'
import { loadMeSummary } from '../../../lib/me'
import {
  meApi,
  type ColleagueMilestone,
  type Report,
  type TeamBoard,
  type TeamMember,
  type TrackCell
} from '../../../lib/me-api'
import { errText, toast } from '../../../lib/toast'
import Avatar from '../../../ui/Avatar.vue'
import Badge from '../../../ui/Badge.vue'
import Button from '../../../ui/Button.vue'
import Callout from '../../../ui/Callout.vue'
import Dialog from '../../../ui/Dialog.vue'
import EmptyState from '../../../ui/EmptyState.vue'
import Input from '../../../ui/Input.vue'
import Label from '../../../ui/Label.vue'
import PageHeader from '../../../ui/PageHeader.vue'
import SegmentedTabs from '../../../ui/SegmentedTabs.vue'
import Select from '../../../ui/Select.vue'
import StatCard from '../../../ui/StatCard.vue'
import Textarea from '../../../ui/Textarea.vue'

const TABS = [
  { key: 'board', title: '团队周报看板' },
  { key: 'review', title: '待我点评' },
  { key: 'members', title: '团队成员' },
  { key: 'milestone', title: '团队祝贺' }
] as const

const SQ_CHAR: Record<string, string> = { submitted: '✓', draft: '…', missing: '!' }
const GRADES = ['A', 'B', 'C', 'D']
const gradeOptions = [{ label: '不评级', value: '' }, ...GRADES.map((g) => ({ label: `${g} 档`, value: g }))]

const route = useRoute()
const router = useRouter()

const tab = computed(() => normalizeTab(String(route.query.tab ?? 'board')))
const board = ref<TeamBoard | null>(null)
const radar = ref<ColleagueMilestone[]>([])
const loadError = ref('')
const loading = ref(false)
const radarLoading = ref(false)
const radarError = ref('')
const blessingBusy = ref('')
const memberSearch = ref('')
const filteredMembers = computed(() => {
  const query = memberSearch.value.trim().toLocaleLowerCase()
  return (board.value?.members ?? []).filter(member => !query || `${member.name} ${member.deptName} ${member.postName}`.toLocaleLowerCase().includes(query))
})
const detail = ref<Report | null>(null)
const grade = ref('')
const reviewText = ref('')
const commentError = ref('')
const busy = ref(false)
const sending = ref(false)
const pickedUsers = ref<string[]>([])

/** 默认把整个团队都带上，去掉一两个比全选省事 */
const note = reactive({
  title: '',
  description: '',
  endTime: defaultDeadline()
})

const tabItems = computed(() =>
  TABS.map((t) => ({
    value: t.key,
    label: t.title,
    count: t.key === 'review' ? board.value?.pendingReviews.length : undefined
  }))
)

const reviewed = computed(() => {
  const me = board.value?.me.userId
  const hit = detail.value?.comments.find((c) => String(c.reviewerId) === String(me))
  return hit?.reviewText ?? ''
})

const rateTone = computed<'success' | 'warning' | 'destructive' | 'primary'>(() => {
  if (!board.value || !board.value.members.length) return 'primary'
  const ratio = board.value.submittedThisWeek / board.value.members.length
  return ratio >= 1 ? 'success' : ratio > 0 ? 'warning' : 'destructive'
})

const pendingMembers = computed(() => {
  if (!board.value) return 0
  const last = board.value.weeks.length - 1
  return board.value.members.filter((m) => m.cells[last]?.state !== 'submitted').length
})

function normalizeTab(value: string) {
  return TABS.some((t) => t.key === value) ? value : 'board'
}

function defaultDeadline() {
  const d = new Date()
  d.setDate(d.getDate() + ((5 - d.getDay() + 7) % 7 || 7))
  d.setHours(18, 0, 0, 0)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}

function cellText(cell: TrackCell) {
  return cell.state === 'submitted' ? '已交' : cell.state === 'draft' ? '有草稿' : '未交'
}

function cellClass(state: string) {
  if (state === 'submitted') return 'border-success/20 bg-success-soft text-success'
  if (state === 'draft') return 'border-warning/20 bg-warning-soft text-warning'
  if (state === 'missing') return 'border-destructive/20 bg-destructive/10 text-destructive'
  return 'border-transparent bg-muted text-muted-foreground'
}

function weekState(member: TeamMember) {
  const last = (board.value?.weeks.length ?? 1) - 1
  const cell = member.cells[last]
  return cell ? cellText(cell) : '—'
}

function submittedCount(member: TeamMember) {
  return member.cells.filter((c) => c.state === 'submitted').length
}

/** 只改地址栏，显示分支由上面的 computed 跟着变 */
function pick(next: string) {
  void router.replace({ path: '/me/team', query: next === 'board' ? {} : { tab: next } })
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await meApi.teamBoard()
    board.value = data
    pickedUsers.value = data.members.map((m) => m.userId)
  } catch (e) {
    loadError.value = errText(e)
  } finally {
    loading.value = false
  }
}

async function loadRadar() {
  radarLoading.value = true
  radarError.value = ''
  try {
    radar.value = await meApi.teamMilestones()
  } catch (e) {
    radar.value = []
    radarError.value = errText(e)
  } finally {
    radarLoading.value = false
  }
}

function mileSub(row: ColleagueMilestone) {
  if (row.action !== 'bless') return `${fmtMD(row.date)} 到期 · 准备述职`
  if (row.daysLeft === 0) return '就是今天'
  return `${fmtMD(row.date)} · 还有 ${row.daysLeft} 天`
}

async function bless(row: ColleagueMilestone) {
  if (blessingBusy.value || row.blessed) return
  blessingBusy.value = `${row.userId}:${row.type}:${row.date}`
  try {
    await meApi.bless({ toUserId: row.userId, type: row.type, date: row.date })
    row.blessed = true
    toast(`已给 ${row.name} 送上${row.typeText}祝福，对方的「收到的认可」里能看到`)
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    blessingBusy.value = ''
  }
}

/** 转正评估不是祝福能解决的事，把收件人和话术填好，主管确认后发出去。 */
function arrange(row: ColleagueMilestone) {
  pickedUsers.value = [row.userId]
  note.title = `${row.name} · ${fmtMD(row.date)} 转正评估，准备述职`
  note.description = '把这段时间的结果、下一步计划整理成述职材料，我们先对一轮。'
  toast(`已选中 ${row.name}，在下方确认后发给 TA`)
}

async function like(member: TeamMember) {
  if (member.liked) {
    toast(`${member.name} 本周已经点过赞了`)
    return
  }
  try {
    await meApi.recognize({ toUserId: member.userId, type: 'like' })
    member.liked = true
    toast(`已给 ${member.name} 本周的赞，对方的「收到的认可」里能看到`)
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

async function nominate(member: TeamMember) {
  try {
    await meApi.recognize({ toUserId: member.userId, type: 'star_nomination' })
    board.value?.members.forEach((m) => {
      m.starred = m.userId === member.userId
    })
    if (board.value) board.value.star = { userId: member.userId, name: member.name }
    toast(`${member.name} 成为你提名的本月之星，提了别人会自动替换`)
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

async function praise(userId: string) {
  const star = board.value?.star
  if (!star) return
  try {
    await meApi.recognize({ toUserId: userId, type: 'praise' })
    toast(`已公开表扬 ${star.name}，对方的「收到的认可」里能看到`)
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

async function openReport(cell: TrackCell) {
  if (!cell.reportId) {
    toast(`${cell.periodKey} 还没有提交记录`, 'bad')
    return
  }
  await openDetail(cell.reportId)
}

async function openReview(id: string) {
  await openDetail(id)
}

async function openDetail(id: string) {
  try {
    detail.value = await meApi.reportDetail(id)
    grade.value = ''
    reviewText.value = ''
    commentError.value = ''
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

function closeDetail() {
  detail.value = null
}

async function submitComment() {
  if (!detail.value) return
  if (!reviewText.value) {
    commentError.value = '点评内容不能为空'
    return
  }
  busy.value = true
  try {
    await meApi.comment(detail.value.id, { grade: grade.value || undefined, reviewText: reviewText.value })
    toast('点评已送达作者')
    detail.value = null
    void loadMeSummary(true)
    await load()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    busy.value = false
  }
}

function toggleUser(userId: string) {
  pickedUsers.value = pickedUsers.value.includes(userId)
    ? pickedUsers.value.filter((u) => u !== userId)
    : [...pickedUsers.value, userId]
}

function pickAll() {
  const members = board.value?.members ?? []
  pickedUsers.value = pickedUsers.value.length === members.length ? [] : members.map((m) => m.userId)
}

function assign(member: TeamMember) {
  pickedUsers.value = [member.userId]
  void router.replace({ path: '/me/team', query: { tab: 'members' } })
  toast(`已选中 ${member.name}，在下方写清任务后发送`)
}

function nudge(member: TeamMember) {
  const last = (board.value?.weeks.length ?? 1) - 1
  const cell = member.cells[last]
  pickedUsers.value = [member.userId]
  note.title = `${member.name} · ${cell?.periodKey ?? '本周'}周报还没交`
  note.description = '交完在群里知会一声，我这边直接看。'
  toast('已填好催办内容，确认后发送')
}

async function sendNote() {
  if (!note.title) {
    toast('先写标题', 'bad')
    return
  }
  if (!pickedUsers.value.length) {
    toast('至少选一个收件人', 'bad')
    return
  }
  sending.value = true
  try {
    const start = new Date()
    const p = (n: number) => String(n).padStart(2, '0')
    const iso = (d: Date) =>
      `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:00`
    await meApi.createApply('schedule', {
      type: 1,
      priority: 2,
      title: note.title,
      description: note.description || undefined,
      startTime: iso(start),
      endTime: `${note.endTime}:00`,
      remind: true,
      participantUserIds: pickedUsers.value.map(String)
    })
    toast(`已发给 ${pickedUsers.value.length} 人，对方的提醒里会出现这条`)
    note.title = ''
    note.description = ''
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    sending.value = false
  }
}

async function refresh() {
  await Promise.allSettled([load(), loadRadar()])
}

onMounted(() => {
  void refresh()
  void loadMeSummary(true)
})
</script>
