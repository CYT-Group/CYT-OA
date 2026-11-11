<template>
  <div>
  <PageHeader
    :title="tab === 'voice' ? '我要发声' : '我的资料'"
    description="人事档案、贡献足迹、认可与提醒偏好都汇总在这里"
  />

  <SegmentedTabs class="mt-5" :model-value="tab" :items="tabItems" @update:model-value="pick" />

  <div v-if="loadError" class="mt-4">
    <Callout tone="destructive" title="加载失败">{{ loadError }}</Callout>
  </div>

  <!-- 基本资料 -->
  <div v-else-if="tab === 'basic'" class="mt-5 grid gap-4 lg:grid-cols-5">
    <section class="overflow-hidden rounded-xl border bg-card shadow-xs lg:col-span-3">
      <div class="border-b px-5 py-4">
        <div v-if="profile" class="flex flex-wrap items-center gap-4">
          <Avatar :name="profile.name" size="lg" />
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="text-lg font-semibold tracking-tight">{{ profile.name || '未命名' }}</h2>
              <Badge v-if="hasValue(profile.status)" variant="success">{{ EMPLOY_STATUS[profile.status] }}</Badge>
              <Badge v-if="hasValue(profile.entryStatus)" variant="outline">{{ ENTRY_STATUS[profile.entryStatus] }}</Badge>
            </div>
            <p class="mt-1 text-sm text-muted-foreground">
              {{ [profile.deptName, profile.postName, profile.postLevel].filter(hasValue).join(' · ') || '部门与岗位待同步' }}
            </p>
            <p v-if="hasValue(profile.jobNumber)" class="mt-0.5 text-xs text-muted-foreground">工号 {{ profile.jobNumber }}</p>
          </div>
        </div>
        <div v-else class="text-sm text-muted-foreground">还没有人事档案</div>
      </div>

      <dl v-if="infoItems.length" class="grid gap-x-6 gap-y-4 px-5 py-5 sm:grid-cols-2">
        <div v-for="item in infoItems" :key="item.label" class="min-w-0">
          <dt class="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">{{ item.label }}</dt>
          <dd class="mt-0.5 truncate text-sm font-medium">{{ item.value }}</dd>
        </div>
      </dl>
      <EmptyState
        v-else
        :icon="Contact"
        title="还没有人事档案"
        description="姓名、部门和岗位由企业微信通讯录同步，其余资料请联系 HR 补全"
      />
    </section>

    <section class="rounded-xl border bg-card shadow-xs lg:col-span-2">
      <div class="border-b px-5 py-4">
        <h2 class="text-sm font-semibold">在职状态</h2>
      </div>
      <div class="px-5 py-4">
        <p class="text-xs leading-relaxed text-muted-foreground">
          已获取的资料优先展示，空字段暂不展示。姓名、部门和岗位由企微通讯录同步，人事补充资料请联系 HR。
        </p>
        <div class="mt-5 flex items-center justify-between">
          <h3 class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">近期里程碑</h3>
          <span class="text-[11px] text-muted-foreground">{{ milestones.length }} 条</span>
        </div>
        <div v-if="!milestones.length" class="py-8 text-center text-xs text-muted-foreground">暂无里程碑记录</div>
        <ul v-else class="mt-2 divide-y">
          <li
            v-for="m in milestones.slice(0, 5)"
            :key="m.id"
            class="flex items-center gap-3 py-2.5"
          >
            <span class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
              <UiIcon :name="m.type" :size="14" />
            </span>
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm">{{ m.title || m.typeText }}</p>
              <p class="text-[11px] text-muted-foreground">{{ fmtDay(m.date) }}</p>
            </div>
            <Badge v-if="m.daysLeft !== null && m.daysLeft > 0" variant="info">还有 {{ m.daysLeft }} 天</Badge>
            <Badge v-else-if="m.daysLeft === 0" variant="warning">就是今天</Badge>
            <Badge v-else variant="outline">已过</Badge>
          </li>
        </ul>
      </div>
    </section>
  </div>

  <!-- 我的足迹 -->
  <div v-else-if="tab === 'footprint'" class="mt-5 space-y-4">
    <div class="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-4">
      <StatCard
        v-for="f in FOOTPRINT_CARDS"
        :key="f.key"
        :title="f.label"
        :value="footprint?.[f.key] ?? 0"
        :icon="f.icon"
        :tone="f.tone"
      />
    </div>

    <section class="overflow-hidden rounded-xl border bg-card shadow-xs">
      <header class="flex items-center justify-between border-b px-5 py-3.5">
        <h2 class="text-sm font-semibold">里程碑全记录</h2>
        <span class="text-xs text-muted-foreground">数字均由业务表反查，不是人工登记的荣誉墙</span>
      </header>
      <EmptyState v-if="!milestones.length" :icon="Award" title="还没有里程碑记录" />
      <ul v-else class="divide-y">
        <li v-for="m in milestones" :key="m.id" class="flex items-center gap-3 px-5 py-3">
          <span class="w-20 shrink-0 text-xs tabular-nums text-muted-foreground">{{ fmtDay(m.date) }}</span>
          <span class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
            <UiIcon :name="m.type" :size="14" />
          </span>
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-medium">{{ m.title || m.typeText }}</p>
            <p class="text-[11px] text-muted-foreground">{{ m.typeText }}</p>
          </div>
          <Badge v-if="m.daysLeft !== null && m.daysLeft > 0" variant="info">倒计时 {{ m.daysLeft }} 天</Badge>
          <Badge v-else-if="m.daysLeft === 0" variant="warning">今天</Badge>
        </li>
      </ul>
    </section>
  </div>

  <!-- 提醒偏好 -->
  <div v-else-if="tab === 'preference'" class="mt-5 max-w-2xl">
    <section class="overflow-hidden rounded-xl border bg-card shadow-xs">
      <header class="border-b px-5 py-4">
        <h2 class="text-sm font-semibold">我的提醒偏好</h2>
        <p class="mt-1 text-xs text-muted-foreground">关掉某类后，同步时不再生成该类提醒；已经收到的记录保留。</p>
      </header>
      <ul class="divide-y">
        <li v-for="p in PREF_FIELDS" :key="p.key" class="flex items-center gap-3 px-5 py-3.5">
          <span class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
            <UiIcon :name="p.icon" :size="16" />
          </span>
          <div class="min-w-0 flex-1">
            <p class="text-sm font-medium">{{ p.title }}</p>
            <p class="text-xs text-muted-foreground">{{ p.desc }}</p>
          </div>
          <Switch v-model="pref[p.key]" />
        </li>
      </ul>
      <footer class="flex items-center justify-end gap-2 border-t px-5 py-3">
        <Button variant="outline" @click="resetPref">还原</Button>
        <Button :disabled="savingPref" @click="savePref">
          <LoaderCircle v-if="savingPref" class="size-4 animate-spin" />
          {{ savingPref ? '保存中…' : '保存偏好' }}
        </Button>
      </footer>
    </section>
  </div>

  <!-- 收到的认可 -->
  <div v-else-if="tab === 'recognition'" class="mt-5">
    <section class="overflow-hidden rounded-xl border bg-card shadow-xs">
      <header class="flex flex-wrap items-center justify-between gap-3 border-b px-5 py-3.5">
        <div>
          <h2 class="text-sm font-semibold">我收到的认可</h2>
          <p class="mt-0.5 text-xs text-muted-foreground">来自同事的点赞与表扬，可在人事档案的绩效记录里追溯</p>
        </div>
        <Select
          class="w-36"
          :model-value="period"
          :options="periodOptions"
          placeholder="全部时间"
          @update:model-value="onPeriod"
        />
      </header>

      <EmptyState
        v-if="!recognitions.length"
        :icon="Heart"
        title="还没有收到认可"
        description="先去交一份漂亮的周报，或帮同事推进一件难事"
      />
      <ul v-else class="divide-y">
        <li v-for="r in recognitions" :key="r.id" class="flex gap-3 px-5 py-4">
          <span class="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full bg-destructive/10 text-destructive">
            <Heart class="size-4" />
          </span>
          <div class="min-w-0 flex-1">
            <p class="text-sm leading-relaxed">{{ r.note || r.scope || '点赞' }}</p>
            <p class="mt-1 text-xs text-muted-foreground">
              {{ r.fromName || '同事' }} · {{ r.typeText }}<span v-if="r.period"> · {{ r.period }}</span>
            </p>
          </div>
        </li>
      </ul>
      <footer class="flex items-center gap-1.5 border-t px-5 py-3 text-xs text-muted-foreground">
        <Heart class="size-3.5" />
        累计收到认可 {{ footprint?.recognitions ?? 0 }} 次
      </footer>
    </section>
  </div>

  <!-- 我要发声 -->
  <div v-else class="mt-5 space-y-4">
    <div class="grid gap-3 sm:grid-cols-3">
      <button
        v-for="c in CHANNELS"
        :key="c.key"
        type="button"
        class="flex items-start gap-3 rounded-xl border bg-card p-4 text-left shadow-xs transition-[border-color,background-color,box-shadow] hover:border-primary/40 hover:bg-accent/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
        :class="channel === c.key ? 'border-primary/60 ring-2 ring-primary/15' : ''"
        @click="channel = c.key"
      >
        <span
          class="flex size-9 shrink-0 items-center justify-center rounded-lg"
          :class="channel === c.key ? 'bg-primary text-primary-foreground' : 'bg-primary/10 text-primary'"
        >
          <UiIcon :name="c.icon" :size="18" />
        </span>
        <span class="min-w-0">
          <span class="block text-sm font-semibold">{{ c.title }}</span>
          <span class="mt-0.5 block text-xs text-muted-foreground">{{ c.desc }}</span>
        </span>
      </button>
    </div>

    <section class="rounded-xl border bg-card p-4 shadow-xs">
      <Textarea
        v-model.trim="content"
        :rows="3"
        maxlength="2000"
        :placeholder="`写给${channelTitle}的内容…`"
      />
      <div class="mt-3 flex flex-wrap items-center gap-3">
        <Checkbox v-model="anonymous">匿名提交</Checkbox>
        <span class="text-xs text-muted-foreground">最多 2000 字 · {{ content.length }}/2000</span>
        <Button class="ml-auto" size="sm" :disabled="sending" @click="send">
          <LoaderCircle v-if="sending" class="size-3.5 animate-spin" />
          {{ sending ? '提交中…' : '发送' }}
        </Button>
      </div>
    </section>

    <section class="overflow-hidden rounded-xl border bg-card shadow-xs">
      <header class="flex items-center justify-between border-b px-5 py-3.5">
        <h2 class="text-sm font-semibold">我发过的声</h2>
        <span class="text-xs text-muted-foreground">{{ voices.length }} 条</span>
      </header>
      <EmptyState v-if="!voices.length" :icon="MessageSquare" title="还没有提交过内容" />
      <ul v-else class="divide-y">
        <li v-for="v in voices" :key="v.id" class="flex gap-3 px-5 py-3.5">
          <span class="w-20 shrink-0 pt-0.5 text-xs tabular-nums text-muted-foreground">
            {{ fmtTime(v.createdAt).slice(5, 16) }}
          </span>
          <span class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
            <UiIcon :name="v.channel" :size="14" />
          </span>
          <div class="min-w-0 flex-1">
            <p class="text-sm leading-relaxed">{{ v.content }}</p>
            <p class="mt-1 text-[11px] text-muted-foreground">{{ v.channelText }} · {{ v.authorName }}</p>
          </div>
          <Badge :variant="voiceTone(v.status)">{{ STATUS_TEXT[v.status] ?? v.status }}</Badge>
        </li>
      </ul>
    </section>
  </div>
  </div>
</template>

<script setup lang="ts">
import {
  Award,
  CalendarCheck,
  Contact,
  FileText,
  Heart,
  LoaderCircle,
  MessageSquare,
  Receipt,
  Send,
  Sparkles
} from '@lucide/vue'
import { computed, onMounted, reactive, ref, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UiIcon from '../../../components/UiIcon.vue'
import { fmtDay, fmtTime } from '../../../lib/format'
import {
  meApi,
  type Footprint,
  type MilestoneView,
  type Profile,
  type RecognitionView,
  type ReminderPreference,
  type VoiceView
} from '../../../lib/me-api'
import { errText, toast } from '../../../lib/toast'
import Avatar from '../../../ui/Avatar.vue'
import Badge from '../../../ui/Badge.vue'
import type { BadgeTone } from '../../../lib/tone'
import Button from '../../../ui/Button.vue'
import Callout from '../../../ui/Callout.vue'
import Checkbox from '../../../ui/Checkbox.vue'
import EmptyState from '../../../ui/EmptyState.vue'
import PageHeader from '../../../ui/PageHeader.vue'
import SegmentedTabs from '../../../ui/SegmentedTabs.vue'
import Select from '../../../ui/Select.vue'
import StatCard from '../../../ui/StatCard.vue'
import Switch from '../../../ui/Switch.vue'
import Textarea from '../../../ui/Textarea.vue'

const TABS = [
  { key: 'basic', title: '基本资料' },
  { key: 'footprint', title: '我的足迹' },
  { key: 'preference', title: '提醒偏好' },
  { key: 'recognition', title: '收到的认可' },
  { key: 'voice', title: '我要发声' }
] as const

const tabItems = TABS.map((t) => ({ value: t.key, label: t.title }))

const FOOTPRINT_CARDS: {
  key: keyof Footprint
  label: string
  icon: Component
  tone: 'primary' | 'info' | 'success' | 'warning' | 'destructive'
}[] = [
  { key: 'reports', label: '提交报告（含草稿）', icon: FileText, tone: 'info' },
  { key: 'submittedReports', label: '已提交报告', icon: Send, tone: 'primary' },
  { key: 'applies', label: '发起的单据', icon: Receipt, tone: 'warning' },
  { key: 'attendanceRecords', label: '考勤记录', icon: CalendarCheck, tone: 'success' },
  { key: 'recognitions', label: '收到的认可', icon: Heart, tone: 'destructive' },
  { key: 'milestones', label: '里程碑', icon: Award, tone: 'info' },
  { key: 'voices', label: '发声条数', icon: Sparkles, tone: 'primary' }
]

const EMPLOY_STATUS: Record<number, string> = { 1: '在职', 2: '实习', 3: '离职', 4: '外包' }
const ENTRY_STATUS: Record<number, string> = { 1: '正式', 2: '试用', 3: '实习' }

const PREF_FIELDS: { key: keyof ReminderPreference; title: string; desc: string; icon: string }[] = [
  { key: 'reportDue', title: '汇报到期', desc: '本周汇报还没交时提醒', icon: 'file' },
  { key: 'overdue', title: '汇报逾期', desc: '超过截止时间仍未提交', icon: 'alert-circle' },
  { key: 'milestone', title: '里程碑', desc: '入职周年、生日、转正节点', icon: 'award' },
  { key: 'corpNews', title: '公司动态', desc: '大促、新品与全员公告', icon: 'megaphone' }
]

const CHANNELS = [
  { key: 'suggestion', title: '建议箱', icon: 'suggestion', desc: '给公司提建议' },
  { key: 'ceo', title: 'CEO 信箱', icon: 'ceo', desc: '可实名 / 匿名' },
  { key: 'survey', title: '满意度调研', icon: 'survey', desc: '季度一次' }
] as const

const STATUS_TEXT: Record<string, string> = { open: '待受理', handling: '处理中', done: '已回复', closed: '已归档' }

const route = useRoute()
const router = useRouter()

const tab = computed(() => normalizeTab(String(route.query.tab ?? 'basic')))
const loadError = ref('')
const hasValue = (value: unknown) => value !== null && value !== undefined && (typeof value !== 'string' || value.trim() !== '')
const profile = ref<Profile | null>(null)
const footprint = ref<Footprint | null>(null)
const milestones = ref<MilestoneView[]>([])
const recognitions = ref<RecognitionView[]>([])
const voices = ref<VoiceView[]>([])
const period = ref('')
const savingPref = ref(false)
const pref = reactive<ReminderPreference>({ reportDue: true, overdue: true, milestone: true, corpNews: true })
const channel = computed<string>({
  get: () => CHANNELS.find(item => item.key === route.query.channel)?.key ?? 'suggestion',
  set: value => { void router.replace({ path: '/me/profile', query: { ...route.query, tab: 'voice', channel: value } }) }
})
const content = ref('')
const anonymous = ref(false)
const sending = ref(false)

const periods = computed(() => {
  const set = new Set(recognitions.value.map((r) => r.period).filter(Boolean))
  return [...set].sort().reverse()
})

const periodOptions = computed(() => [
  { label: '全部时间', value: '' },
  ...periods.value.map((p) => ({ label: p, value: p }))
])

const channelTitle = computed(() => CHANNELS.find((c) => c.key === channel.value)?.title ?? '心声')

const probationText = computed(() => {
  if (!profile.value) return '—'
  if (profile.value.probation === 1) return '已转正'
  if (profile.value.probation === 0) return profile.value.regularTime ? `试用期，${fmtDay(profile.value.regularTime)} 转正` : '试用期'
  return '—'
})

const infoItems = computed(() => {
  const p = profile.value
  if (!p) return []
  const items: { label: string; value: string }[] = []
  const push = (label: string, value: unknown) => {
    if (hasValue(value)) items.push({ label, value: String(value) })
  }
  push('姓名', p.name)
  push('工号', p.jobNumber)
  push('部门', p.deptName)
  push('岗位', [p.postName, p.postLevel].filter(hasValue).join(' · '))
  push('工作城市', p.workCity)
  push('直属上级', p.managerName)
  push('我带', p.subCount ? `${p.subCount} 人` : '')
  push('生日', p.birthday ? fmtDay(p.birthday) : '')
  push('入职', p.entryTime ? `${fmtDay(p.entryTime)}${hasValue(p.workAge) ? `（司龄 ${p.workAge} 年）` : ''}` : '')
  push('转正', probationText.value === '—' ? '' : probationText.value)
  return items
})

function voiceTone(status: string): BadgeTone {
  if (status === 'open') return 'warning'
  if (status === 'handling') return 'info'
  if (status === 'done') return 'success'
  return 'outline'
}

function normalizeTab(value: string) {
  return TABS.some((t) => t.key === value) ? value : 'basic'
}

/** 只改地址栏，显示分支由上面的 computed 跟着变 */
function pick(next: string) {
  void router.replace({ path: '/me/profile', query: next === 'basic' ? {} : { tab: next } })
}

function onPeriod(value: unknown) {
  period.value = String(value ?? '')
  void loadRecognitions()
}

function resetPref() {
  if (profile.value?.preference) Object.assign(pref, profile.value.preference)
}

async function loadRecognitions() {
  try {
    recognitions.value = await meApi.recognitions(period.value || undefined)
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

async function savePref() {
  savingPref.value = true
  try {
    await meApi.savePreferences({ ...pref })
    toast('偏好已保存')
    if (profile.value) profile.value.preference = { ...pref }
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    savingPref.value = false
  }
}

async function send() {
  if (!content.value) {
    toast('先写点内容', 'bad')
    return
  }
  sending.value = true
  try {
    await meApi.submitVoice({ channel: channel.value, content: content.value, anonymous: anonymous.value })
    toast('已收到，受理结果会回到列表')
    content.value = ''
    anonymous.value = false
    voices.value = await meApi.voices()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  try {
    const [data, ms, rs, vs] = await Promise.all([
      meApi.profile(),
      meApi.milestones(),
      meApi.recognitions(),
      meApi.voices()
    ])
    profile.value = data
    footprint.value = data.footprint
    if (data.preference) Object.assign(pref, data.preference)
    milestones.value = ms
    recognitions.value = rs
    voices.value = vs
  } catch (e) {
    loadError.value = errText(e)
  }
})
</script>
