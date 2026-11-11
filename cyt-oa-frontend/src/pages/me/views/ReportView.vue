<template>
  <div class="space-y-5">
    <PageHeader :title="REPORT_TYPE_OPTIONS.find(option => option.value === reportType)?.label || '我的报告'" description="周报、月报与日报的提交、点评和轨迹">
      <template #actions>
        <Button @click="compose()">
          <PenLine class="size-4" />
          写{{ REPORT_TYPE_OPTIONS.find(option => option.value === trackType)?.label || '汇报' }}
        </Button>
      </template>
    </PageHeader>

    <div class="flex flex-wrap items-center gap-2">
      <SegmentedTabs
        :model-value="tab"
        :items="TABS.map((t) => ({ value: t.key, label: t.title }))"
        @update:model-value="pick('tab', $event)"
      />
    </div>

    <section class="panel">
      <div class="panel-header">
        <div class="panel-title"><CalendarRange class="size-4 text-muted-foreground" />近 12 周提交轨迹</div>
        <Select :model-value="trackType" :options="TRACK_TYPE_OPTIONS" size="sm" class="w-28" @update:model-value="onTrackType" />
      </div>
      <div class="p-4">
        <EmptyState v-if="!track.length" title="还没有可统计的周期" description="提交或保存草稿后，这里会生成轨迹" :icon="CalendarRange" compact />
        <div v-else>
          <div class="grid grid-cols-6 gap-2 sm:grid-cols-12">
            <button
              v-for="(cell, idx) in track"
              :key="cell.periodKey"
              :title="`${cell.label} ${cellText(cell)}`"
              class="group flex flex-col items-center gap-1.5"
              @click="openCell(cell)"
            >
              <span
                class="grid aspect-square w-full max-w-10 place-items-center rounded-lg border text-xs transition-transform group-hover:-translate-y-0.5"
                :class="[trackClass(cell.state), idx === track.length - 1 ? 'ring-2 ring-primary/30' : '']"
              >
                <Check v-if="cell.state === 'submitted'" class="size-4" />
                <Ellipsis v-else-if="cell.state === 'draft'" class="size-4" />
                <X v-else class="size-4" />
              </span>
              <span class="w-full truncate text-center text-[10px] text-muted-foreground">{{ cell.label }}</span>
            </button>
          </div>
          <div class="mt-4 flex flex-wrap items-center gap-3 text-[11px] text-muted-foreground">
            <span class="inline-flex items-center gap-1.5"><i class="size-2.5 rounded bg-success/60" />已交</span>
            <span class="inline-flex items-center gap-1.5"><i class="size-2.5 rounded bg-warning/60" />草稿</span>
            <span class="inline-flex items-center gap-1.5"><i class="size-2.5 rounded bg-destructive/50" />未交</span>
          </div>
        </div>
      </div>
    </section>

    <Callout v-if="loadError" tone="destructive" title="加载失败">{{ loadError }}</Callout>

    <template v-else-if="tab === 'draft'">
      <section class="panel">
        <div class="panel-header">
          <div class="panel-title"><FileClock class="size-4 text-muted-foreground" />待提交</div>
          <span class="text-xs text-muted-foreground">{{ drafts.length }} 份</span>
        </div>
        <div class="p-2 sm:p-3">
          <EmptyState v-if="!drafts.length" title="没有压在手里的汇报" description="新建汇报或保存草稿后会出现在这里" :icon="FileCheck2" compact />
          <div v-for="row in drafts" :key="row.id" class="flex flex-wrap items-center gap-3 rounded-xl px-3 py-3 hover:bg-accent/50">
            <span class="w-20 shrink-0 text-xs tabular-nums text-muted-foreground">{{ row.periodKey }}</span>
            <span class="grid size-8 shrink-0 place-items-center rounded-lg bg-warning-soft text-warning">
              <UiIcon name="file" :size="15" />
            </span>
            <div class="min-w-0 flex-1">
              <b class="block truncate text-sm font-medium">{{ row.title || row.typeText }}</b>
              <small class="mt-0.5 block truncate text-xs text-muted-foreground">
                {{ row.typeText }} · 草稿保存于 {{ fmtTime(row.createTime) }}
              </small>
            </div>
            <Badge variant="warning">待提交</Badge>
            <Button size="sm" @click="compose(row)">继续填写</Button>
            <Button variant="outline" size="sm" @click="submit(row.id)">提交</Button>
          </div>
        </div>
      </section>
    </template>

    <template v-else-if="tab === 'timeline'">
      <section class="panel">
        <div class="panel-header">
          <div class="panel-title"><History class="size-4 text-muted-foreground" />报告时间线</div>
          <span class="text-xs text-muted-foreground">共 {{ total }} 份</span>
        </div>
        <div class="p-2 sm:p-3">
          <EmptyState v-if="!rows.length" title="还没有汇报记录" description="提交第一份汇报后会显示在这里" :icon="FileText" compact />
          <div
            v-for="row in rows"
            :key="row.id"
            class="flex cursor-pointer flex-wrap items-center gap-3 rounded-xl px-3 py-3 transition-colors hover:bg-accent/60"
            @click="openDetail(row.id)"
          >
            <span class="w-20 shrink-0 text-xs tabular-nums text-muted-foreground">{{ row.periodKey }}</span>
            <span class="grid size-8 shrink-0 place-items-center rounded-lg bg-muted text-muted-foreground">
              <UiIcon name="file" :size="15" />
            </span>
            <div class="min-w-0 flex-1">
              <b class="block truncate text-sm font-medium">{{ row.title || row.typeText }}</b>
              <small class="mt-0.5 block truncate text-xs text-muted-foreground">
                {{ row.typeText }} · {{ fmtTime(row.createTime) }}
              </small>
            </div>
            <Badge v-if="row.comments.length" variant="outline">{{ row.comments.length }} 条点评</Badge>
            <Badge :variant="toneBadge(reportTone(row.status))">{{ row.statusText }}</Badge>
            <Button variant="outline" size="sm" @click.stop="exportPdf(row)">导出</Button>
          </div>
          <Pagination
            v-if="total > pageSize"
            :page="pageNo"
            :page-size="pageSize"
            :total="total"
            class="mt-3"
            @update:page="go"
          />
        </div>
      </section>
    </template>

    <template v-else-if="tab === 'review'">
      <section class="panel">
        <div class="panel-header">
          <div class="panel-title"><MessageSquareQuote class="size-4 text-muted-foreground" />收到的点评</div>
        </div>
        <div class="space-y-4 p-3 sm:p-4">
          <EmptyState v-if="!reviewed.length" title="暂时没收到点评" description="上级点评你的报告后会显示在这里" :icon="MessageSquareQuote" compact />
          <div v-for="row in reviewed" :key="row.id" class="space-y-2.5">
            <div
              class="flex cursor-pointer flex-wrap items-center gap-3 rounded-xl border border-border/70 bg-muted/30 px-3 py-2.5"
              @click="openDetail(row.id)"
            >
              <span class="w-16 shrink-0 text-xs tabular-nums text-muted-foreground">{{ row.periodKey }}</span>
              <span class="grid size-8 shrink-0 place-items-center rounded-lg bg-card text-muted-foreground">
                <UiIcon name="message" :size="15" />
              </span>
              <div class="min-w-0 flex-1">
                <b class="block truncate text-sm font-medium">{{ row.title || row.typeText }}</b>
                <small class="mt-0.5 block truncate text-xs text-muted-foreground">{{ row.typeText }} · {{ row.statusText }}</small>
              </div>
              <Button variant="outline" size="sm" @click.stop="openDetail(row.id)">查看全文</Button>
            </div>
            <div v-for="cm in row.comments" :key="cm.id" class="rounded-xl border border-border/70 bg-card p-3.5">
              <p class="text-xs font-medium text-muted-foreground">
                {{ cm.reviewerName }} · {{ cm.grade ? cm.grade + ' 档 · ' : '' }}{{ relTime(cm.createTime) }}
              </p>
              <p class="mt-1.5 text-sm leading-relaxed">{{ cm.reviewText }}</p>
              <div v-if="cm.reply" class="mt-2 rounded-lg bg-success-soft px-3 py-2 text-sm">
                <span class="mr-1.5 text-xs font-medium text-success">我的回复</span>{{ cm.reply }}
              </div>
              <div v-else class="mt-2.5 flex gap-2">
                <Input v-model="replyDraft[cm.id]" placeholder="回复上级点评" class="h-8 text-xs" />
                <Button size="sm" :disabled="!replyDraft[cm.id]" @click="sendReply(row.id, cm.id)">回复</Button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </template>

    <template v-else>
      <section class="panel">
        <div class="panel-header">
          <div class="panel-title"><Layers class="size-4 text-muted-foreground" />逐期明细</div>
        </div>
        <div class="p-2 sm:p-3">
          <EmptyState v-if="!rows.length" title="这个类型下还没有汇报" description="换个类型或先写一份" :icon="Layers" compact />
          <div
            v-for="row in rows"
            :key="row.id"
            class="flex cursor-pointer flex-wrap items-center gap-3 rounded-xl px-3 py-3 transition-colors hover:bg-accent/60"
            @click="openDetail(row.id)"
          >
            <span class="w-20 shrink-0 text-xs tabular-nums text-muted-foreground">{{ row.periodKey }}</span>
            <span class="grid size-8 shrink-0 place-items-center rounded-lg bg-muted text-muted-foreground">
              <UiIcon :name="row.status === 2 ? 'submitted' : 'draft'" :size="15" />
            </span>
            <div class="min-w-0 flex-1">
              <b class="block truncate text-sm font-medium">{{ row.title || row.typeText }}</b>
              <small class="mt-0.5 block truncate text-xs text-muted-foreground">{{ row.typeText }}</small>
            </div>
            <Badge :variant="toneBadge(reportTone(row.status))">{{ row.statusText }}</Badge>
            <Badge v-if="row.comments.length" variant="outline">{{ row.comments.length }} 条点评</Badge>
          </div>
        </div>
      </section>
    </template>

    <Dialog
      :open="form !== null"
      :title="form?.id ? '继续填写' : '新建汇报'"
      description="周期自动按汇报类型计算"
      size="lg"
      @update:open="!$event && (form = null)"
    >
      <div v-if="form" class="space-y-4">
        <div class="form-grid">
          <div>
            <Label required>汇报类型</Label>
            <Select
              :model-value="form.type"
              :options="REPORT_TYPE_OPTIONS"
              :disabled="!!form.id"
              @update:model-value="(v) => form && (form.type = Number(v))"
            />
          </div>
          <div>
            <Label>标题<span class="field-hint ml-1">留空自动生成</span></Label>
            <Input v-model="form.title" placeholder="如：新旗舰首发周复盘" />
          </div>
        </div>

        <div>
          <Label required>① 本周完成</Label>
          <Textarea v-model="form.summary" :rows="4" placeholder="一行一条，写清结果而不只是动作" />
        </div>
        <div>
          <Label>② 下周计划</Label>
          <Textarea v-model="form.plan" :rows="3" placeholder="按优先级排，能落到人和时间最好" />
        </div>
        <div>
          <Label>③ 风险与需要的支持</Label>
          <Textarea v-model="form.problem" :rows="3" placeholder="卡在哪、要谁配合" />
        </div>
        <div>
          <Label>④ 关键数据</Label>
          <Textarea v-model="form.remark" :rows="2" placeholder="GMV / 转化率 / 客单等，可留空" />
        </div>
      </div>
      <template #footer>
        <Button variant="outline" @click="form = null">取消</Button>
        <Button variant="outline" :disabled="busy" @click="save(false)">存草稿</Button>
        <Button :disabled="!form?.summary" :loading="busy" @click="save(true)">{{ busy ? '提交中…' : '提交' }}</Button>
      </template>
    </Dialog>

    <Dialog
      :open="detail !== null"
      :title="detail ? `${detail.title || detail.typeText} · ${detail.periodKey}` : '报告详情'"
      description="报告全文、点评与回复"
      size="lg"
      @update:open="!$event && (detail = null)"
    >
      <div v-if="detail" class="space-y-5">
        <dl class="kv-list">
          <div class="kv-item"><dt>编号</dt><dd>{{ detail.no || '—' }}</dd></div>
          <div class="kv-item"><dt>类型</dt><dd>{{ detail.typeText }}</dd></div>
          <div class="kv-item"><dt>状态</dt><dd>{{ detail.statusText }}</dd></div>
          <div class="kv-item"><dt>提交时间</dt><dd>{{ fmtTime(detail.createTime) }}</dd></div>
          <div class="kv-item sm:col-span-2"><dt>周期</dt><dd>{{ fmtTime(detail.startTime) }} ~ {{ fmtTime(detail.endTime) }}</dd></div>
        </dl>

        <div>
          <p class="section-title">① 本周完成</p>
          <p class="whitespace-pre-wrap text-sm leading-relaxed text-muted-foreground">{{ detail.summary || '未填写' }}</p>
          <div v-if="detail.workItems.length" class="mt-2 flex flex-wrap gap-1.5">
            <Badge v-for="(w, i) in detail.workItems" :key="i" variant="secondary">{{ w.content }} {{ w.progress }}%</Badge>
          </div>
        </div>

        <div>
          <p class="section-title">② 下周计划</p>
          <div v-if="detail.planItems.length" class="flex flex-wrap gap-1.5">
            <Badge v-for="(p, i) in detail.planItems" :key="'p' + i" variant="outline">{{ p }}</Badge>
          </div>
          <p v-else class="whitespace-pre-wrap text-sm leading-relaxed text-muted-foreground">{{ detail.plan || '未填写' }}</p>
        </div>

        <div>
          <p class="section-title">③ 风险与需要的支持</p>
          <p class="whitespace-pre-wrap text-sm leading-relaxed text-muted-foreground">{{ detail.problem || '无' }}</p>
        </div>

        <div>
          <p class="section-title">④ 关键数据</p>
          <p class="whitespace-pre-wrap text-sm leading-relaxed text-muted-foreground">{{ detail.remark || '未填写' }}</p>
        </div>

        <div>
          <p class="section-title">点评与回复</p>
          <p v-if="!detail.comments.length" class="text-sm text-muted-foreground">还没有点评</p>
          <div v-for="cm in detail.comments" :key="cm.id" class="mb-2.5 rounded-xl border border-border/70 bg-muted/30 p-3.5 last:mb-0">
            <p class="text-xs font-medium text-muted-foreground">
              {{ cm.reviewerName }} · {{ cm.grade ? cm.grade + ' 档 · ' : '' }}{{ relTime(cm.createTime) }}
            </p>
            <p class="mt-1.5 text-sm leading-relaxed">{{ cm.reviewText }}</p>
            <div v-if="cm.reply" class="mt-2 rounded-lg bg-success-soft px-3 py-2 text-sm">
              <span class="mr-1.5 text-xs font-medium text-success">我的回复</span>{{ cm.reply }}
            </div>
            <div v-else class="mt-2.5 flex gap-2">
              <Input v-model="replyDraft[cm.id]" placeholder="回复上级点评" class="h-8 text-xs" />
              <Button size="sm" :disabled="!replyDraft[cm.id]" @click="sendReply(detail.id, cm.id)">回复</Button>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <Button v-if="detail?.status === 2" variant="outline" @click="detail && cancelReport(detail.id)">撤销提交</Button>
        <Button v-if="detail" variant="outline" @click="exportPdf(detail)">导出 PDF</Button>
        <Button v-if="detail && detail.status !== 2" @click="submit(detail.id)">提交</Button>
        <Button variant="ghost" @click="detail = null">关闭</Button>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CalendarRange,
  Check,
  Ellipsis,
  FileCheck2,
  FileClock,
  FileText,
  History,
  Layers,
  MessageSquareQuote,
  PenLine,
  X
} from '@lucide/vue'
import UiIcon from '../../../components/UiIcon.vue'
import { meApi, REPORT_SUBMITTED, type Report, type TrackCell } from '../../../lib/me-api'
import { fmtTime, relTime, reportTone } from '../../../lib/format'
import { toneBadge } from '../../../lib/tone'
import { toast, errText } from '../../../lib/toast'
import { loadMeSummary } from '../../../lib/me'
import PageHeader from '../../../ui/PageHeader.vue'
import SegmentedTabs from '../../../ui/SegmentedTabs.vue'
import Select from '../../../ui/Select.vue'
import Button from '../../../ui/Button.vue'
import Badge from '../../../ui/Badge.vue'
import Callout from '../../../ui/Callout.vue'
import EmptyState from '../../../ui/EmptyState.vue'
import Pagination from '../../../ui/Pagination.vue'
import Dialog from '../../../ui/Dialog.vue'
import Label from '../../../ui/Label.vue'
import Input from '../../../ui/Input.vue'
import Textarea from '../../../ui/Textarea.vue'

const route = useRoute()
const router = useRouter()

const TABS = [
  { key: 'draft', title: '待提交' },
  { key: 'timeline', title: '报告时间线' },
  { key: 'track', title: '提交轨迹' },
  { key: 'review', title: '收到的点评' }
] as const

const REPORT_TYPE_OPTIONS = [
  { label: '周报', value: 2 },
  { label: '月报', value: 3 },
  { label: '日报', value: 1 }
]

const TRACK_TYPE_OPTIONS = [
  { label: '周报', value: 2 },
  { label: '月报', value: 3 },
  { label: '日报', value: 1 }
]

const tab = computed(() => String(route.query.tab ?? 'draft'))
const reportType = computed(() => [1, 2, 3].includes(Number(route.query.type)) ? Number(route.query.type) : undefined)
const trackType = ref(reportType.value ?? 2)
const track = ref<TrackCell[]>([])
const rows = ref<Report[]>([])
const drafts = ref<Report[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 10
const loadError = ref('')
const busy = ref(false)
const form = ref<null | { id?: string; type: number; title: string; summary: string; plan: string; problem: string; remark: string }>(null)
const detail = ref<Report | null>(null)
const replyDraft = reactive<Record<string, string>>({})

const reviewed = computed(() => rows.value.filter((r) => r.comments.length > 0))

function cellText(cell: TrackCell) {
  return cell.state === 'submitted' ? '已交' : cell.state === 'draft' ? '有草稿' : '未交'
}

function trackClass(state: string) {
  if (state === 'submitted') return 'border-success/20 bg-success-soft text-success'
  if (state === 'draft') return 'border-warning/25 bg-warning-soft text-warning'
  return 'border-destructive/20 bg-destructive/5 text-destructive'
}

function onTrackType(value: string | number | null | undefined) {
  pick('type', String(value == null ? 2 : Number(value)))
}

async function loadTrack() {
  try {
    track.value = await meApi.reportTrack({ type: trackType.value, weeks: 12 })
  } catch (e) {
    loadError.value = errText(e)
  }
}

async function load() {
  loadError.value = ''
  try {
    if (tab.value === 'draft') {
      const all = await meApi.reportDrafts()
      drafts.value = reportType.value ? all.filter(row => row.type === reportType.value) : all
    } else if (tab.value === 'review') {
      const data = await meApi.reports({ type: reportType.value, status: REPORT_SUBMITTED, pageNo: 1, pageSize: 50 })
      rows.value = data.list ?? []
      total.value = Number(data.total ?? 0)
    } else {
      const data = await meApi.reports({ type: reportType.value, pageNo: pageNo.value, pageSize })
      rows.value = data.list ?? []
      total.value = Number(data.total ?? 0)
    }
  } catch (e) {
    loadError.value = errText(e)
  }
}

function pick(key: string, value: string) {
  const query = { ...route.query } as Record<string, string>
  if (value) query[key] = value
  else delete query[key]
  router.push({ path: '/me/report', query })
}

function go(next: number) {
  pageNo.value = next
  void load()
}

async function openDetail(id: string) {
  try {
    detail.value = await meApi.reportDetail(id)
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

async function openCell(cell: TrackCell) {
  if (cell.reportId) await openDetail(cell.reportId)
  else if (cell.state === 'missing') compose()
}

function compose(row?: Report) {
  form.value = row
    ? {
        id: row.id,
        type: row.type,
        title: row.title ?? '',
        summary: row.summary ?? '',
        plan: row.plan ?? '',
        problem: row.problem ?? '',
        remark: row.remark ?? ''
      }
    : { type: trackType.value, title: '', summary: '', plan: '', problem: '', remark: '' }
}

async function save(submitAfter: boolean) {
  if (!form.value) return
  busy.value = true
  try {
    const id = await meApi.saveDraft({ ...form.value })
    if (submitAfter) await meApi.submitReport(String(id))
    toast(submitAfter ? '已提交' : '草稿已保存')
    form.value = null
    detail.value = null
    void loadMeSummary(true)
    await Promise.all([load(), loadTrack()])
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    busy.value = false
  }
}

async function submit(id: string) {
  try {
    await meApi.submitReport(id)
    toast('已提交')
    detail.value = null
    void loadMeSummary(true)
    await Promise.all([load(), loadTrack()])
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

async function cancelReport(id: string) {
  try {
    await meApi.cancelReport(id)
    toast('已撤回为草稿')
    detail.value = null
    void loadMeSummary(true)
    await Promise.all([load(), loadTrack()])
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

async function sendReply(reportId: string, commentId: string) {
  const text = replyDraft[commentId]
  if (!text) return
  try {
    await meApi.reply(commentId, text)
    toast('回复已发出')
    delete replyDraft[commentId]
    detail.value = await meApi.reportDetail(reportId)
    await load()
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

async function exportPdf(row: Report) {
  try {
    await meApi.exportReport(row.id, row.no)
    toast('已开始下载')
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

watch(tab, () => {
  pageNo.value = 1
  void load()
})
watch(trackType, loadTrack)
watch(reportType, value => {
  trackType.value = value ?? 2
  pageNo.value = 1
  void load()
})

onMounted(() => {
  void load()
  void loadTrack()
})
</script>
