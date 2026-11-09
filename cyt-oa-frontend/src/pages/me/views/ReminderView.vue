<template>
  <div class="space-y-5">
    <PageHeader title="我的提醒" description="待办、里程碑与公司动态，集中在一处处理">
      <template #actions>
        <Checkbox :model-value="unreadOnly" @update:model-value="onUnread">只看未读</Checkbox>
        <Button variant="outline" size="sm" @click="readAll">
          <CheckCheck class="size-4" />
          全部已读
        </Button>
        <Button variant="outline" size="sm" @click="openPref">
          <SlidersHorizontal class="size-4" />
          提醒偏好
        </Button>
      </template>
    </PageHeader>

    <SegmentedTabs
      :model-value="cat"
      :items="CATS.map((c) => ({ value: c.key, label: c.title }))"
      @update:model-value="pick"
    />

    <section v-if="myMilestones.length">
      <h2 class="section-title">关于我的里程碑</h2>
      <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        <div v-for="m in myMilestones" :key="m.id" class="panel hover-lift flex items-center gap-3 p-4">
          <span class="grid size-9 shrink-0 place-items-center rounded-xl bg-primary/8 text-primary">
            <UiIcon :name="m.type" :size="16" />
          </span>
          <div class="min-w-0">
            <b class="block truncate text-sm font-medium">{{ mileLabel(m) }}</b>
            <p class="mt-0.5 truncate text-xs text-muted-foreground">{{ mileSub(m) }}</p>
          </div>
        </div>
      </div>
    </section>

    <section v-if="colleagues.length">
      <h2 class="section-title">同事里程碑 · 送上祝福</h2>
      <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        <div v-for="c in colleagues" :key="c.userId + c.type + c.date" class="panel hover-lift flex items-center gap-3 p-4">
          <span class="grid size-9 shrink-0 place-items-center rounded-xl bg-warning-soft text-warning">
            <UiIcon :name="c.type" :size="16" />
          </span>
          <div class="min-w-0 flex-1">
            <b class="block truncate text-sm font-medium">
              {{ c.name }} {{ c.daysLeft === 0 ? '今天' : fmtMD(c.date) }}{{ c.typeText }}
            </b>
            <p class="mt-0.5 truncate text-xs text-muted-foreground">{{ c.deptName || '—' }} · {{ c.relation }}</p>
          </div>
          <Button
            size="sm"
            :variant="c.blessed ? 'outline' : 'default'"
            :disabled="c.blessed"
            @click="bless(c)"
          >
            {{ c.blessed ? '已送祝福' : '送祝福' }}
          </Button>
        </div>
      </div>
    </section>

    <section v-if="newsRows.length" class="panel">
      <div class="panel-header">
        <div class="panel-title"><PartyPopper class="size-4 text-muted-foreground" />公司喜报 · 可点赞祝福</div>
      </div>
      <div class="p-2 sm:p-3">
        <div v-for="row in newsRows" :key="row.id" class="rounded-xl px-3 py-3 transition-colors hover:bg-accent/50">
          <div class="flex items-start gap-3">
            <span class="grid size-8 shrink-0 place-items-center rounded-lg bg-info-soft text-info">
              <UiIcon name="megaphone" :size="15" />
            </span>
            <div class="min-w-0 flex-1">
              <b class="block text-sm font-medium">{{ row.title }}</b>
              <small class="mt-0.5 block text-xs text-muted-foreground">
                {{ row.category || '公司动态' }} · {{ relTime(row.publishedAt) }}
              </small>
            </div>
            <div class="flex shrink-0 items-center gap-1.5">
              <button
                class="inline-flex items-center gap-1 rounded-lg border px-2 py-1 text-xs transition-colors"
                :class="
                  eng[row.id]?.liked
                    ? 'border-destructive/20 bg-destructive/10 text-destructive'
                    : 'border-border text-muted-foreground hover:bg-accent'
                "
                @click="toggleLike(row)"
              >
                <Heart class="size-3.5" :fill="eng[row.id]?.liked ? 'currentColor' : 'none'" />
                {{ eng[row.id]?.likeCount ?? 0 }}
              </button>
              <Button variant="ghost" size="sm" @click="openComments(row)">
                <MessageCircle class="size-3.5" />
                评论 {{ eng[row.id]?.commentCount ?? 0 }}
              </Button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <div class="panel-title"><Bell class="size-4 text-muted-foreground" />提醒列表</div>
        <span class="text-xs text-muted-foreground">共 {{ total }} 条</span>
      </div>
      <div class="p-2 sm:p-3">
        <Callout v-if="loadError" tone="destructive" title="加载失败">{{ loadError }}</Callout>
        <template v-else>
          <EmptyState
            v-if="!rows.length"
            :title="unreadOnly ? '未读的已经清空了' : '这里很干净'"
            description="新的提醒会第一时间出现在这里"
            :icon="BellOff"
          />
          <div
            v-for="row in rows"
            :key="row.id"
            class="flex cursor-pointer items-center gap-3 rounded-xl px-3 py-3 transition-colors hover:bg-accent/60"
            @click="open(row)"
          >
            <span class="relative grid size-8 shrink-0 place-items-center rounded-lg bg-muted text-muted-foreground">
              <UiIcon :name="row.category" :size="15" />
              <span v-if="!row.read" class="absolute -right-0.5 -top-0.5 size-2 rounded-full bg-primary ring-2 ring-card" />
            </span>
            <div class="min-w-0 flex-1">
              <b class="block truncate text-sm" :class="row.read ? 'font-normal' : 'font-medium'">{{ row.title }}</b>
              <small class="mt-0.5 block truncate text-xs text-muted-foreground">
                {{ catText(row.category) }} · {{ relTime(row.createdAt) }}
              </small>
            </div>
            <Badge v-if="!row.read" variant="info">未读</Badge>
            <Button v-if="row.link" variant="outline" size="sm" @click.stop="open(row)">查看</Button>
          </div>
          <Pagination
            v-if="total > pageSize"
            :page="pageNo"
            :page-size="pageSize"
            :total="total"
            class="mt-3"
            @update:page="go"
          />
        </template>
      </div>
    </section>

    <Dialog
      :open="newsPick !== null"
      :title="newsPick?.title ?? '评论'"
      description="公司动态下的祝福与评论"
      @update:open="!$event && (newsPick = null)"
    >
      <div v-if="newsPick" class="space-y-4">
        <p class="rounded-xl bg-muted/60 p-3 text-sm leading-relaxed text-muted-foreground">
          {{ newsPick.summary || '暂无摘要' }}
        </p>

        <div>
          <p class="section-title">评论 · {{ eng[newsPick.id]?.commentCount ?? 0 }}</p>
          <EmptyState v-if="!eng[newsPick.id]?.comments.length" title="还没有人评论" description="来说第一句祝福吧" :icon="MessageCircle" />
          <div v-for="c in eng[newsPick.id]?.comments ?? []" :key="c.id" class="flex items-start gap-3 border-b border-border/60 py-2.5 last:border-0">
            <span class="grid size-8 shrink-0 place-items-center rounded-lg bg-muted text-muted-foreground">
              <UiIcon name="message" :size="15" />
            </span>
            <div class="min-w-0 flex-1">
              <p class="text-sm">{{ c.content }}</p>
              <p class="mt-0.5 text-[11px] text-muted-foreground">{{ c.displayName }} · {{ relTime(c.createdAt) }}</p>
            </div>
            <Button v-if="c.canDelete" variant="ghost" size="sm" class="text-destructive" @click="removeComment(newsPick.id, c.id)">
              删除
            </Button>
          </div>
        </div>

        <div>
          <Label>说两句</Label>
          <Textarea v-model="draftComment" :rows="2" placeholder="祝贺一句，或补一条信息" />
        </div>
      </div>
      <template #footer>
        <Button variant="outline" @click="newsPick = null">关闭</Button>
        <Button :loading="posting" @click="postComment">{{ posting ? '发送中…' : '发表评论' }}</Button>
      </template>
    </Dialog>

    <Dialog :open="prefOpen" title="提醒偏好" description="关掉某类后，同步时不再生成该类提醒，已有的记录保留" @update:open="prefOpen = $event">
      <div class="divide-y divide-border/60">
        <div v-for="p in PREF_FIELDS" :key="p.key" class="flex items-center gap-4 py-3">
          <div class="min-w-0 flex-1">
            <p class="text-sm font-medium">{{ p.title }}</p>
            <p class="mt-0.5 text-xs text-muted-foreground">{{ p.desc }}</p>
          </div>
          <Switch v-model="draft[p.key]" />
        </div>
      </div>
      <template #footer>
        <Button variant="outline" @click="prefOpen = false">取消</Button>
        <Button :loading="saving" @click="savePref">{{ saving ? '保存中…' : '保存' }}</Button>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  BellOff,
  CheckCheck,
  Heart,
  MessageCircle,
  PartyPopper,
  SlidersHorizontal
} from '@lucide/vue'
import UiIcon from '../../../components/UiIcon.vue'
import { meApi, type ColleagueMilestone, type MilestoneView, type NewsEngagement, type NewsRow, type Reminder, type ReminderPreference } from '../../../lib/me-api'
import { fmtMD, relTime } from '../../../lib/format'
import { toast, errText } from '../../../lib/toast'
import { loadMeSummary } from '../../../lib/me'
import PageHeader from '../../../ui/PageHeader.vue'
import SegmentedTabs from '../../../ui/SegmentedTabs.vue'
import Checkbox from '../../../ui/Checkbox.vue'
import Button from '../../../ui/Button.vue'
import Badge from '../../../ui/Badge.vue'
import Callout from '../../../ui/Callout.vue'
import EmptyState from '../../../ui/EmptyState.vue'
import Pagination from '../../../ui/Pagination.vue'
import Dialog from '../../../ui/Dialog.vue'
import Label from '../../../ui/Label.vue'
import Textarea from '../../../ui/Textarea.vue'
import Switch from '../../../ui/Switch.vue'

const route = useRoute()
const router = useRouter()

const CATS = [
  { key: '', title: '全部' },
  { key: 'todo', title: '待我办' },
  { key: 'milestone', title: '里程碑' },
  { key: 'compliance', title: '规则提醒' },
  { key: 'corp', title: '公司动态' }
] as const

const CAT_TEXT: Record<string, string> = { todo: '待我办', milestone: '里程碑', compliance: '规则提醒', corp: '公司动态' }

const PREF_FIELDS: { key: keyof ReminderPreference; title: string; desc: string }[] = [
  { key: 'reportDue', title: '汇报到期', desc: '本周汇报还没交时提醒' },
  { key: 'overdue', title: '汇报逾期', desc: '超过截止时间仍未提交' },
  { key: 'milestone', title: '里程碑', desc: '入职周年、生日、转正节点' },
  { key: 'corpNews', title: '公司动态', desc: '大促、新品与全员公告' }
]

const rows = ref<Reminder[]>([])
const miles = ref<MilestoneView[]>([])
const colleagues = ref<ColleagueMilestone[]>([])
const newsRows = ref<NewsRow[]>([])
const eng = reactive<Record<string, NewsEngagement>>({})
const newsPick = ref<NewsRow | null>(null)
const draftComment = ref('')
const posting = ref(false)
const total = ref(0)
const pageNo = ref(1)
const pageSize = 20
const cat = computed(() => String(route.query.cat ?? ''))
const unreadOnly = ref(false)
const loadError = ref('')
const prefOpen = ref(false)
const saving = ref(false)
const draft = reactive<ReminderPreference>({ reportDue: true, overdue: true, milestone: true, corpNews: true })

/** 快到期的排前面，凑满三张才补最近的往事，和 demo 的里程碑带一个口径。 */
const myMilestones = computed(() => {
  const upcoming = miles.value
    .filter((m) => (m.daysLeft ?? -1) >= 0)
    .sort((a, b) => (a.daysLeft ?? 0) - (b.daysLeft ?? 0))
  const past = miles.value
    .filter((m) => (m.daysLeft ?? 0) < 0)
    .sort((a, b) => b.date.localeCompare(a.date))
  return [...upcoming, ...past].slice(0, 3)
})

/** 标题尾巴上的日期交给副标题说，免得一张卡里写两遍。 */
function mileLabel(m: MilestoneView) {
  return m.title.endsWith(` · ${m.date}`) ? m.title.slice(0, -m.date.length - 3) : m.title
}

function mileSub(m: MilestoneView) {
  if (m.daysLeft === 0) return `${fmtMD(m.date)} 就是今天`
  if (m.daysLeft && m.daysLeft > 0) return `${fmtMD(m.date)} · 还有 ${m.daysLeft} 天`
  return `${fmtMD(m.date)} · 已过 ${Math.abs(m.daysLeft ?? 0)} 天`
}

function catText(key: string) {
  return CAT_TEXT[key] ?? key
}

/** 分类只认地址栏，左侧子菜单和外部链接改 URL 时同样会刷新列表 */
function pick(key: string) {
  if (cat.value === key) {
    void reload() // 点当前分类就当刷新
    return
  }
  void router.replace({ path: '/me/reminder', query: key ? { cat: key } : {} })
}

watch(cat, () => {
  pageNo.value = 1
  void reload()
})

async function reload() {
  loadError.value = ''
  try {
    const data = await meApi.reminders({
      cat: cat.value || undefined,
      unreadOnly: unreadOnly.value || undefined,
      pageNo: pageNo.value,
      pageSize
    })
    rows.value = data.list ?? []
    total.value = Number(data.total ?? 0)
  } catch (e) {
    loadError.value = errText(e)
  }
}

function go(next: number) {
  pageNo.value = next
  void reload()
}

function onUnread(value: boolean) {
  unreadOnly.value = value
  pageNo.value = 1
  void reload()
}

async function open(row: Reminder) {
  if (!row.read) {
    await meApi.markRead(row.id).catch(() => undefined)
    row.read = true
    void loadMeSummary(true)
  }
  if (row.link) router.push(row.link)
}

async function readAll() {
  try {
    const res = await meApi.markAllRead(cat.value || undefined)
    toast(`已标记 ${res.count} 条为已读`)
    void loadMeSummary(true)
    await reload()
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

async function openPref() {
  try {
    Object.assign(draft, await meApi.preferences())
    prefOpen.value = true
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

async function savePref() {
  saving.value = true
  try {
    await meApi.savePreferences({ ...draft })
    toast('偏好已保存')
    prefOpen.value = false
    await reload()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    saving.value = false
  }
}

async function loadMilestones() {
  try {
    const [mine, peers] = await Promise.all([meApi.milestones(), meApi.colleagueMilestones()])
    miles.value = mine
    colleagues.value = peers
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

async function bless(row: ColleagueMilestone) {
  try {
    await meApi.bless({ toUserId: row.userId, type: row.type, date: row.date })
    row.blessed = true
    toast(`已给 ${row.name} 送上${row.typeText}祝福，对方的「收到的认可」里能看到`)
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

async function loadNews() {
  try {
    const data = await meApi.newsPage({ pageNo: 1, pageSize: 3 })
    newsRows.value = data.list ?? []
    const states = await Promise.all(newsRows.value.map((row) => meApi.newsEngagement(row.id)))
    newsRows.value.forEach((row, i) => {
      eng[row.id] = states[i]
    })
  } catch (e) {
    newsRows.value = []
    toast(errText(e), 'bad')
  }
}

async function toggleLike(row: NewsRow) {
  const cur = eng[row.id]
  if (!cur) return
  try {
    const next = cur.liked ? await meApi.newsUnlike(row.id) : await meApi.newsLike(row.id)
    eng[row.id] = { ...cur, ...next }
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

function openComments(row: NewsRow) {
  newsPick.value = row
  draftComment.value = ''
}

async function postComment() {
  const row = newsPick.value
  const cur = row ? eng[row.id] : undefined
  if (!row || !cur) return
  if (!draftComment.value) {
    toast('评论先写点内容吧', 'bad')
    return
  }
  posting.value = true
  try {
    const added = await meApi.newsComment(row.id, draftComment.value)
    eng[row.id] = { ...cur, comments: [...cur.comments, added], commentCount: cur.commentCount + 1 }
    draftComment.value = ''
    toast('评论已发布')
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    posting.value = false
  }
}

async function removeComment(newsId: string, commentId: string) {
  const cur = eng[newsId]
  if (!cur) return
  try {
    await meApi.newsDeleteComment(newsId, commentId)
    eng[newsId] = {
      ...cur,
      comments: cur.comments.filter((c) => c.id !== commentId),
      commentCount: Math.max(cur.commentCount - 1, 0)
    }
    toast('评论已删除')
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

onMounted(() => {
  void reload()
  void loadMilestones()
  void loadNews()
  void loadMeSummary(true)
})
</script>
