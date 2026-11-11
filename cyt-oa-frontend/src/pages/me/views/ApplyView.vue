<template>
  <div>
  <PageHeader
    :title="view === 'start' ? '我要办事' : view === 'mine' ? '我的申请' : view === 'history' ? '历史记录' : '发起申请'"
    :description="view === 'history' ? '查看近期已经结束的申请记录' : '选择单据类型填写后提交，审批在企业微信进行，结论可随时回读'"
  >
    <template #actions>
      <Button v-if="view === 'mine' || view === 'history'" size="sm" @click="router.push('/me/apply?view=start')">发起申请</Button>
      <Button variant="outline" size="sm" :disabled="loading" @click="reload">
        <RefreshCw class="size-3.5" />
        刷新
      </Button>
    </template>
  </PageHeader>

  <section v-if="view === 'all' || view === 'start'" class="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-4">
    <button
      v-for="t in TILES"
      :key="t.type"
      type="button"
      class="group flex flex-col gap-3 rounded-xl border bg-card p-4 text-left shadow-xs transition-[border-color,background-color,box-shadow] hover:border-primary/40 hover:bg-accent/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
      @click="open(t.type)"
    >
      <span class="flex w-full items-center justify-between">
        <span class="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
          <UiIcon :name="t.icon" :size="18" />
        </span>
        <ArrowUpRight class="size-4 text-muted-foreground/0 transition-colors group-hover:text-muted-foreground" />
      </span>
      <span>
        <span class="block text-sm font-semibold">{{ t.title }}</span>
        <span class="mt-0.5 block text-xs text-muted-foreground">{{ t.desc }}</span>
      </span>
    </button>
    <RouterLink
      v-for="item in portalTiles"
      :key="item.type"
      :to="`/me/requests?view=start&type=${item.type}`"
      class="group flex flex-col gap-3 rounded-xl border bg-card p-4 text-left shadow-xs transition-colors hover:border-primary/40 hover:bg-accent/30 focus-visible:outline-2 focus-visible:outline-primary"
    >
      <span class="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-xl" aria-hidden="true">{{ item.icon }}</span>
      <span><span class="block text-sm font-semibold">{{ item.title }}</span><span class="mt-0.5 block text-xs text-muted-foreground">{{ item.desc }}</span></span>
    </RouterLink>
  </section>

  <template v-if="view !== 'start'">
  <div class="mt-6 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
    <SegmentedTabs :model-value="type" :items="typeTabs" @update:model-value="pick" />
    <Select
      class="w-40 shrink-0"
      :model-value="status"
      :options="statusOptions"
      aria-label="申请状态"
      @update:model-value="onStatus"
    />
  </div>
  <p v-if="!type" class="mt-2 text-xs text-muted-foreground">汇总最近 50 条记录；选择单据类型后可分页查看完整记录。</p>

  <section class="mt-4 overflow-hidden rounded-xl border bg-card shadow-xs">
    <header class="flex items-center justify-between gap-3 border-b px-4 py-3">
      <h2 class="text-sm font-semibold">{{ typeTitle || '全部申请' }}</h2>
      <span class="text-xs text-muted-foreground">{{ !type ? '近期匹配' : '共' }} {{ total }} 条</span>
    </header>

    <p v-if="loading" class="py-10 text-center text-sm text-muted-foreground" role="status">正在读取申请…</p>
    <div v-else-if="loadError" class="p-4">
      <Callout tone="destructive" title="加载失败">{{ loadError }}</Callout>
    </div>

    <EmptyState
      v-else-if="!visibleRows.length"
      :icon="FileText"
      :title="type ? '这类单据还没有记录' : '还没有发起过申请'"
      :description="view === 'history' ? '近期已结束的申请会显示在这里' : '通过「我要办事」发起申请，提交后会出现在这里'"
    />

    <ul v-else class="divide-y">
      <li
        v-for="row in visibleRows"
        :key="row.type + row.id"
        class="flex flex-col gap-2.5 px-4 py-3 transition-colors hover:bg-muted/40 sm:flex-row sm:items-center"
      >
        <div class="flex min-w-0 flex-1 items-center gap-3">
          <span class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
            <UiIcon :name="row.type" :size="16" />
          </span>
          <div class="min-w-0">
            <p class="truncate text-sm font-medium">{{ row.title || row.typeText }}</p>
            <p class="mt-0.5 flex flex-wrap items-center gap-x-1.5 text-xs text-muted-foreground">
              <span>{{ row.typeText }}</span>
              <span v-if="row.no">· {{ row.no }}</span>
              <span>{{ fmtTime(row.createTime).slice(5, 16) }}</span>
              <span v-if="row.startTime">· {{ fmtTime(row.startTime).slice(5, 16) }} 起</span>
            </p>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2 pl-12 sm:justify-end sm:pl-0">
          <span
            v-if="row.amount"
            class="inline-flex items-center rounded-md bg-muted px-1.5 py-0.5 text-[11px] font-medium tabular-nums text-muted-foreground"
          >¥{{ row.amount }}</span>
          <Badge :variant="row.type === 'schedule' ? 'outline' : toneBadge(flowTone(row.status))">{{ row.type === 'schedule' ? '已创建' : row.statusText || '未提交' }}</Badge>
          <Button
            v-if="row.status !== null && [-1, 0].includes(Number(row.status)) && row.type !== 'schedule'"
            size="sm"
            :disabled="actionBusy === `${row.type}:${row.id}`"
            @click="submitExisting(row)"
          >提交</Button>
          <Button
            v-if="Number(row.status) === 1 && row.type !== 'schedule'"
            variant="outline"
            size="sm"
            class="text-destructive hover:bg-destructive/10 hover:text-destructive"
            @click="cancel(row)"
          >撤回方式</Button>
          <Button
            v-if="Number(row.status) === 1 && row.type !== 'schedule'"
            variant="outline"
            size="sm"
            :disabled="syncing === `${row.type}:${row.id}`"
            @click="syncApproval(row)"
          >
            <LoaderCircle v-if="syncing === `${row.type}:${row.id}`" class="size-3.5 animate-spin" />
            {{ syncing === `${row.type}:${row.id}` ? '同步中…' : '同步状态' }}
          </Button>
        </div>
      </li>
    </ul>

    <div v-if="!loading && !loadError && type && total > pageSize" class="border-t px-4 py-3">
      <Pagination :page="pageNo" :page-size="pageSize" :total="total" @update:page="go" />
    </div>
  </section>
  </template>

  <Dialog
    :open="!!current"
    :title="current?.title ?? ''"
    :description="current ? `${current.desc} · 提交后进入企业微信审批` : undefined"
    size="lg"
    @update:open="(value) => { if (!value) current = null }"
  >
    <div v-if="current" class="space-y-5">
      <FieldForm ref="mainRef" :fields="current.fields" />
      <template v-if="current.line">
        <div class="flex items-center gap-2 border-t pt-4">
          <span class="text-sm font-semibold">{{ current.line.title }}</span>
        </div>
        <FieldForm ref="lineRef" :fields="current.line.fields" compact />
      </template>
      <Callout v-if="current.note" tone="info" title="提交须知">{{ current.note }}</Callout>
    </div>
    <template #footer>
      <Button variant="outline" @click="current = null">取消</Button>
      <Button :disabled="busy" @click="create">
        <LoaderCircle v-if="busy" class="size-4 animate-spin" />
        {{ busy ? '提交中…' : '提交申请' }}
      </Button>
    </template>
  </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ArrowUpRight, FileText, LoaderCircle, RefreshCw } from '@lucide/vue'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import FieldForm from '../../../components/FieldForm.vue'
import UiIcon from '../../../components/UiIcon.vue'
import { pageConfigs } from '../../../configs/pages'
import type { FieldConfig } from '../../../configs/types'
import { fmtTime, flowTone } from '../../../lib/format'
import { loadMeSummary } from '../../../lib/me'
import { meApi, type ApplyItem, type Option } from '../../../lib/me-api'
import { remoteOptions } from '../../../lib/options'
import { toast, errText } from '../../../lib/toast'
import { toneBadge } from '../../../lib/tone'
import Badge from '../../../ui/Badge.vue'
import Button from '../../../ui/Button.vue'
import Callout from '../../../ui/Callout.vue'
import Dialog from '../../../ui/Dialog.vue'
import EmptyState from '../../../ui/EmptyState.vue'
import PageHeader from '../../../ui/PageHeader.vue'
import Pagination from '../../../ui/Pagination.vue'
import SegmentedTabs from '../../../ui/SegmentedTabs.vue'
import Select from '../../../ui/Select.vue'

interface ApplyForm {
  type: string
  title: string
  icon: string
  desc: string
  fields: FieldConfig[]
  line?: { title: string; fields: FieldConfig[] }
  note?: string
}

const route = useRoute()
const router = useRouter()

const ledger = reactive<{ items: Option[] }>({ items: [] })

const timeField = (key: string, label: string): FieldConfig => ({ key, label, type: 'datetime', required: true })
const reasonField: FieldConfig = { key: 'reason', label: '申请事由', type: 'textarea', required: true }
const urgencyField: FieldConfig = { key: 'urgency', label: '紧急程度', type: 'select', dictType: 'oa_apply_urgency', required: true }
const titleField: FieldConfig = { key: 'title', label: '标题', required: true }

/** 用品下拉在打开时才知道，所以表单按台账选项生成。 */
function forms(): Record<string, ApplyForm> {
  return {
    leave: {
      type: 'leave', title: '请假', icon: 'leave', desc: '年假 / 事假等',
      fields: fieldsOf('oa-leave'),
      note: '审批在企业微信进行，审批结论点列表里的「同步状态」回读。'
    },
    overtime: {
      type: 'overtime', title: '加班', icon: 'overtime', desc: '工作日与周末加班',
      fields: fieldsOf('oa-overtime'),
      note: '审批在企业微信进行，审批结论点列表里的「同步状态」回读。'
    },
    reimbursement: {
      type: 'reimbursement', title: '费用报销', icon: 'reimbursement', desc: '一笔一明细',
      fields: [
        titleField, urgencyField,
        { key: 'paymentMethod', label: '支付方式', type: 'select', dictType: 'oa_reimbursement_payment_method', required: true },
        { key: 'customerName', label: '相关客户', required: true },
        { key: 'witnessUserId', label: '证明人编号', type: 'number', hint: '审批时需要第三方证明' },
        reasonField
      ],
      line: {
        title: '费用明细',
        fields: [
          { key: 'expenseType', label: '费用类型', type: 'select', dictType: 'oa_expense_type', required: true },
          { key: 'expenseTime', label: '费用发生时间', type: 'datetime', required: true },
          { key: 'description', label: '费用说明', required: true },
          { key: 'price', label: '金额', type: 'number', required: true },
          { key: 'invoiceCount', label: '票据张数', type: 'number', required: true }
        ]
      },
      note: '审批在企业微信进行，审批结论点列表里的「同步状态」回读。'
    },
    travel: {
      type: 'travel', title: '出差申请', icon: 'travel', desc: '一段行程一明细',
      fields: [reasonField, timeField('startTime', '开始日期'), timeField('endTime', '结束日期'),
        { key: 'companion', label: '同行人' },
        { key: 'estimatedPrice', label: '预计费用', type: 'number' }],
      line: {
        title: '行程明细',
        fields: [
          timeField('startTime', '出发时间'), timeField('endTime', '到达时间'),
          { key: 'transportType', label: '交通方式', type: 'select', dictType: 'oa_transport_type', required: true },
          { key: 'remark', label: '备注' }
        ]
      },
      note: '审批在企业微信进行，审批结论点列表里的「同步状态」回读。'
    },
    'travel-reimbursement': {
      type: 'travel-reimbursement', title: '出差报销', icon: 'travel-reimbursement', desc: '关联出差单',
      fields: [
        reasonField, timeField('startTime', '开始日期'), timeField('endTime', '结束日期'),
        { key: 'travelApplyId', label: '关联出差申请编号', type: 'number', hint: '留空则独立报销' },
        { key: 'remark', label: '备注', type: 'textarea' }
      ],
      line: {
        title: '费用明细',
        fields: [
          { key: 'expenseType', label: '费用类型', type: 'select', dictType: 'oa_expense_type', required: true },
          timeField('expenseTime', '发生时间'),
          { key: 'departureCity', label: '出发城市' },
          { key: 'arrivalCity', label: '到达城市' },
          { key: 'price', label: '金额', type: 'number', required: true },
          { key: 'description', label: '费用说明' }
        ]
      },
      note: '审批在企业微信进行，审批结论点列表里的「同步状态」回读。'
    },
    supply: {
      type: 'supply', title: '用品领用', icon: 'supply', desc: '办公用品领用 / 借用',
      fields: [
        timeField('applyTime', '领用日期'),
        { key: 'useType', label: '使用类型', type: 'select', required: true, options: [
          { label: '领用', value: '1' }, { label: '借用', value: '2' }
        ] },
        { key: 'pickupMethod', label: '领取方式', type: 'select', dictType: 'oa_supply_pickup_method', required: true },
        reasonField
      ],
      line: {
        title: '领用明细',
        fields: [
          {
            key: 'itemId', label: '办公用品', type: 'select', required: true,
            hint: ledger.items.length ? undefined : '暂无可选用品，请先到管理端「用品台账」新增',
            options: ledger.items.map((i) => ({ label: `${i.label}（${i.hint ?? '—'}）`, value: String(i.value) }))
          },
          { key: 'applyQuantity', label: '数量', type: 'number', required: true }
        ]
      },
      note: '审批在企业微信进行，审批结论点列表里的「同步状态」回读。'
    },
    schedule: {
      type: 'schedule', title: '新建日程', icon: 'schedule', desc: '提醒与派任务',
      fields: fieldsOf('oa-schedule')
    }
  }
}

function fieldsOf(key: string): FieldConfig[] {
  return pageConfigs[key]?.fields ?? []
}

const TILES = computed(() => {
  const all = forms()
  return Object.values(all).map((f) => ({ type: f.type, title: f.title, icon: f.icon, desc: f.desc }))
})
const portalTiles = [
  { type: 'purchase', title: '采购申请', icon: '🛒', desc: '采购内容与费用审批' },
  { type: 'seal', title: '用章申请', icon: '📑', desc: '文件用章与用途审批' }
]

const typeTabs = computed(() => [
  { value: '', label: '全部' },
  ...TILES.value.map((t) => ({ value: t.type, label: t.title }))
])

const typeTitle = computed(() => TILES.value.find((t) => t.type === type.value)?.title ?? '')

const STATUS_OPTIONS = [
  { label: '全部状态', value: 0 },
  { label: '审批中', value: 1 },
  { label: '已通过', value: 2 },
  { label: '已驳回', value: 3 },
  { label: '已撤销', value: 4 },
  { label: '未提交', value: -1 }
]

const type = computed(() => {
  const value = String(route.query.type ?? '')
  return forms()[value] ? value : ''
})
const view = computed(() => ['start', 'mine', 'history'].includes(String(route.query.view)) ? String(route.query.view) : 'all')
const statusFromQuery = () => {
  const value = Number(route.query.status ?? (view.value === 'history' ? 2 : 0))
  const choices = view.value === 'history' ? [2, 3, 4] : [0, 1, 2, 3, 4, -1]
  return choices.includes(value) ? value : view.value === 'history' ? 2 : 0
}
const status = ref(statusFromQuery())
const statusOptions = computed(() => view.value === 'history' ? STATUS_OPTIONS.filter(option => [2, 3, 4].includes(option.value)) : STATUS_OPTIONS)
const rows = ref<ApplyItem[]>([])
const visibleRows = computed(() => rows.value)
const total = ref(0)
const pageNo = ref(1)
const pageSize = 10
const loadError = ref('')
const loading = ref(false)
const busy = ref(false)
const actionBusy = ref('')
const syncing = ref('')
const current = ref<ApplyForm | null>(null)
const mainRef = ref<InstanceType<typeof FieldForm> | null>(null)
const lineRef = ref<InstanceType<typeof FieldForm> | null>(null)

/** 类型只认地址栏，左侧「请假 / 加班…」子菜单改 URL 时列表会跟着换 */
function pick(next: string) {
  if (type.value === next) {
    void reload() // 点当前类型就当刷新
    return
  }
  const query = { ...route.query } as Record<string, string>
  if (next) query.type = next
  else delete query.type
  void router.replace({ path: '/me/apply', query })
}

function onStatus(value: unknown) {
  const next = Number(value ?? 0)
  void router.replace({ path: '/me/apply', query: { ...route.query, status: String(next) } })
}

watch([type, view, () => route.query.status], () => {
  status.value = statusFromQuery()
  pageNo.value = 1
  void reload()
})

let listRequest = 0
async function reload() {
  const request = ++listRequest
  if (view.value === 'start') { loading.value = false; return }
  loading.value = true
  loadError.value = ''
  try {
    if (!type.value) {
      const data = await meApi.recentApplies(50)
      if (request !== listRequest) return
      rows.value = status.value ? data.filter(row => Number(row.status) === status.value) : data
      total.value = rows.value.length
    } else {
      const data = await meApi.applies({ type: type.value, status: status.value || undefined, pageNo: pageNo.value, pageSize })
      if (request !== listRequest) return
      rows.value = data.list ?? []
      total.value = Number(data.total ?? 0)
    }
  } catch (e) {
    if (request === listRequest) { rows.value = []; total.value = 0; loadError.value = errText(e) }
  } finally {
    if (request === listRequest) loading.value = false
  }
}

function go(next: number) {
  pageNo.value = next
  void reload()
}

/** 用品台账异步返回后只补充选项，保留用户已填写的内容。 */
function syncLedgerOptions() {
  const form = current.value
  if (form?.type !== 'supply') return
  const target = form.line?.fields.find((field) => field.key === 'itemId')
  if (!target) return
  target.options = ledger.items.map((item) => ({
    label: `${item.label}（${item.hint ?? '—'}）`,
    value: String(item.value)
  }))
  target.hint = ledger.items.length ? undefined : '暂无可选用品，请先到管理端「用品台账」新增'
}

watch(() => ledger.items, syncLedgerOptions)

/** 请假表单复用管理端页面配置，其中「审批人候选」这类下拉的选项要按 optionsApi 自己拉一次。
 *  remoteOptions 按 url 缓存，重复打开表单不会再发请求。 */
async function fillRemoteOptions(form: ApplyForm) {
  for (const f of [...form.fields, ...(form.line?.fields ?? [])]) {
    if (!f.optionsApi || f.options?.length) continue
    f.options = await remoteOptions(f.optionsApi)
  }
}

async function open(next: string) {
  current.value = forms()[next] ?? null
  if (!current.value) return
  if (type.value !== next || route.query.new) {
    const query = { ...route.query }
    query.type = next
    delete query.new
    void router.replace({ path: '/me/apply', query })
  }
  if (next === 'supply' && !ledger.items.length) {
    ledger.items = await meApi.supplyItems().catch((e) => {
      toast(errText(e), 'bad')
      return ledger.items
    })
  }
  if (next === 'leave') await fillRemoteOptions(current.value)
}

watch(() => route.query.new, (value) => {
  if (typeof value === 'string' && forms()[value]) void open(value)
}, { immediate: true })

async function create() {
  if (busy.value) return
  const form = current.value
  if (!form || !mainRef.value) return
  const main = mainRef.value
  if (!main.validate()) return
  let line = null as null | Record<string, unknown>
  if (form.line && lineRef.value) {
    if (!lineRef.value.validate()) return
    line = lineRef.value.payload()
  }
  const body: Record<string, unknown> = { ...main.payload() }
  if (line) body.items = [line]
  busy.value = true
  let createdId = ''
  try {
    const id = await meApi.createApply(form.type, body)
    createdId = id ? String(id) : ''
    // 按钮写的是「提交申请」，就得真的进审批流；日程没有流程，落库即完成
    if (id && form.type !== 'schedule') await startFlow(form.type, String(id))
    toast(form.type === 'schedule' ? '日程已创建' : `${form.title}已提交，等待审批`)
    current.value = null
    void loadMeSummary(true)
    await router.replace({ path: '/me/apply', query: { view: 'mine', type: form.type } })
    await reload()
  } catch (e) {
    if (createdId) {
      current.value = null
      toast(`申请已保存为草稿，审批提交失败：${errText(e)}。可在「未提交」列表继续提交。`, 'bad')
      await router.replace({ path: '/me/apply', query: { view: 'mine', type: form.type, status: '-1' } })
      await reload()
    } else toast(errText(e), 'bad')
  } finally {
    busy.value = false
  }
}

/** 用品的提交只认编号，其余类型的提交接口认 {id} */
async function startFlow(type: string, id: string) {
  if (type === 'supply') await meApi.submitApplyById(type, id)
  else await meApi.submitApply(type, { id })
}

/** 未提交的草稿（travel/schedule 之类直接落库的）补一次 submit 起流程 */
async function submitExisting(row: ApplyItem) {
  const key = `${row.type}:${row.id}`
  if (actionBusy.value) return
  actionBusy.value = key
  try {
    await startFlow(row.type, row.id)
    toast('已提交审批')
    void loadMeSummary(true)
    await reload()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    actionBusy.value = ''
  }
}

function cancel(_row: ApplyItem) {
  toast('请在企业微信撤回原审批单，再回到本页点击「同步状态」。')
}

/** 审批结论只在公司企业微信里，OA 不接回调，点了才回读一次。 */
async function syncApproval(row: ApplyItem) {
  if (syncing.value) return
  syncing.value = `${row.type}:${row.id}`
  try {
    await meApi.syncApply(row.type, row.id)
    toast('已从企业微信刷新审批状态')
    void loadMeSummary(true)
    await reload()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    syncing.value = ''
  }
}

onMounted(() => {
  void reload()
  // 预拉用品台账，打开表单即可选择实际库存条目
  void meApi.supplyItems().then((r) => { ledger.items = r }).catch(() => {})
})
</script>
