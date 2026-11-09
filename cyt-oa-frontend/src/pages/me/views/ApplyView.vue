<template>
  <div>
  <PageHeader
    title="发起申请"
    description="选择单据类型填写后提交，审批在企业微信进行，结论可随时回读"
  >
    <template #actions>
      <Button variant="outline" size="sm" @click="reload">
        <RefreshCw class="size-3.5" />
        刷新
      </Button>
    </template>
  </PageHeader>

  <section class="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-4">
    <button
      v-for="t in TILES"
      :key="t.type"
      type="button"
      class="group flex flex-col gap-3 rounded-xl border bg-card p-4 text-left shadow-xs transition-all hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
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
  </section>

  <div class="mt-6 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
    <SegmentedTabs :model-value="type" :items="typeTabs" @update:model-value="pick" />
    <Select
      class="w-40 shrink-0"
      :model-value="status"
      :options="STATUS_OPTIONS"
      @update:model-value="onStatus"
    />
  </div>

  <section class="mt-4 overflow-hidden rounded-xl border bg-card shadow-xs">
    <header class="flex items-center justify-between gap-3 border-b px-4 py-3">
      <h2 class="text-sm font-semibold">{{ typeTitle || '全部申请' }}</h2>
      <span class="text-xs text-muted-foreground">共 {{ total }} 条</span>
    </header>

    <div v-if="loadError" class="p-4">
      <Callout tone="destructive" title="加载失败">{{ loadError }}</Callout>
    </div>

    <EmptyState
      v-else-if="!rows.length"
      :icon="FileText"
      :title="type ? '这类单据还没有记录' : '还没有发起过申请'"
      :description="type ? '点上方卡片新建一张，提交后会出现在这里' : '从上方选择一种单据类型开始'"
    />

    <ul v-else class="divide-y">
      <li
        v-for="row in rows"
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
          <Badge :variant="toneBadge(flowTone(row.status))">{{ row.statusText || '未提交' }}</Badge>
          <Button
            v-if="row.status === -1 || row.status === 0"
            size="sm"
            @click="submitExisting(row)"
          >提交</Button>
          <Button
            v-else-if="row.processInstanceId"
            variant="outline"
            size="sm"
            class="text-destructive hover:bg-destructive/10 hover:text-destructive"
            @click="cancel(row)"
          >撤销</Button>
          <Button
            v-else-if="row.status === 1 && row.type !== 'schedule'"
            variant="outline"
            size="sm"
            :disabled="syncing === row.id"
            @click="syncApproval(row)"
          >
            <LoaderCircle v-if="syncing === row.id" class="size-3.5 animate-spin" />
            {{ syncing === row.id ? '同步中…' : '同步状态' }}
          </Button>
        </div>
      </li>
    </ul>

    <div v-if="!loadError && total > rows.length" class="border-t px-4 py-3">
      <Pagination :page="pageNo" :page-size="pageSize" :total="total" @update:page="go" />
    </div>
  </section>

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
import { useRoute, useRouter } from 'vue-router'
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

const ledger = reactive<{ seals: Option[]; items: Option[] }>({ seals: [], items: [] })

const timeField = (key: string, label: string): FieldConfig => ({ key, label, type: 'datetime', required: true })
const reasonField: FieldConfig = { key: 'reason', label: '申请事由', type: 'textarea', required: true }
const urgencyField: FieldConfig = { key: 'urgency', label: '紧急程度', type: 'select', dictType: 'oa_apply_urgency', required: true }
const titleField: FieldConfig = { key: 'title', label: '标题', required: true }

/** 印章与用品的下拉在打开时才知道，所以表单是算出来的而不是常量。 */
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
    seal: {
      type: 'seal', title: '用印申请', icon: 'seal', desc: '合同 / 证明用章',
      fields: [
        {
          key: 'sealId', label: '印章', type: 'select', required: true,
          hint: ledger.seals.length ? undefined : '暂无可选印章，请先到管理端「印章台账」新增',
          options: ledger.seals.map((s) => ({ label: s.label, value: String(s.value) }))
        },
        { key: 'type', label: '用印类型', type: 'select', required: true, options: [
          { label: '合同用印', value: '1' }, { label: '协议用印', value: '2' }, { label: '证明用印', value: '3' },
          { label: '授权用印', value: '4' }, { label: '其他用印', value: '5' }
        ] },
        { key: 'mode', label: '用印方式', type: 'select', required: true, options: [
          { label: '现场用印', value: '1' }, { label: '外借', value: '2' }
        ] },
        { key: 'documentTitle', label: '文件标题' },
        { key: 'documentCount', label: '文件份数', type: 'number', required: true },
        { key: 'contractPrice', label: '合同金额', type: 'number' },
        { key: 'contractParty', label: '合同对方' },
        timeField('expectedUseTime', '预计用印时间'),
        { key: 'expectedReturnTime', label: '预计归还时间', type: 'datetime' },
        { key: 'urgent', label: '是否紧急', type: 'switch', required: true },
        reasonField
      ],
      note: '审批在企业微信进行，审批结论点列表里的「同步状态」回读。外借需要按时归还，实际归还时间由印章保管人登记。'
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

const type = computed(() => String(route.query.type ?? ''))
const status = ref(0)
const rows = ref<ApplyItem[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 10
const loadError = ref('')
const busy = ref(false)
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
  status.value = Number(value ?? 0)
  pageNo.value = 1
  void reload()
}

watch(type, () => {
  pageNo.value = 1
  void reload()
})

async function reload() {
  if (!type.value) {
    // 跨类型列表用聚合接口，避免一次发 8 个分页请求
    loadError.value = ''
    try {
      rows.value = await meApi.recentApplies(30)
      if (status.value) rows.value = rows.value.filter((r) => r.status === status.value)
      total.value = rows.value.length
    } catch (e) {
      loadError.value = errText(e)
    }
    return
  }
  loadError.value = ''
  try {
    const data = await meApi.applies({
      type: type.value,
      status: status.value || undefined,
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

/** 印章 / 用品台账是异步的：弹窗先开，数据回来后把对应字段的 options 原地补上。
 *  FieldForm 的选项走 computed，会自动刷新下拉；不重建表单，也就不会冲掉已填内容。 */
function syncLedgerOptions(kind: string) {
  const form = current.value
  if (!form || form.type !== kind) return
  const fieldKey = kind === 'seal' ? 'sealId' : 'itemId'
  const list = kind === 'seal' ? ledger.seals : ledger.items
  const target = [...form.fields, ...(form.line?.fields ?? [])].find((f) => f.key === fieldKey)
  if (!target) return
  target.options = list.map((o) => ({
    label: kind === 'seal' ? o.label : `${o.label}（${o.hint ?? '—'}）`,
    value: String(o.value)
  }))
  target.hint = list.length
    ? undefined
    : (kind === 'seal' ? '暂无可选印章，请先到管理端「印章台账」新增' : '暂无可选用品，请先到管理端「用品台账」新增')
}

watch(() => [ledger.seals, ledger.items], () => {
  if (current.value?.type === 'seal') syncLedgerOptions('seal')
  if (current.value?.type === 'supply') syncLedgerOptions('supply')
})

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
  if (type.value !== next) void router.replace({ path: '/me/apply', query: { type: next } })
  if (next === 'seal' && !ledger.seals.length) {
    ledger.seals = await meApi.seals().catch((e) => {
      toast(errText(e), 'bad')
      return ledger.seals
    })
  }
  if (next === 'supply' && !ledger.items.length) {
    ledger.items = await meApi.supplyItems().catch((e) => {
      toast(errText(e), 'bad')
      return ledger.items
    })
  }
  if (next === 'leave') await fillRemoteOptions(current.value)
}

async function create() {
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
  try {
    const id = await meApi.createApply(form.type, body)
    // 按钮写的是「提交申请」，就得真的进审批流；日程没有流程，落库即完成
    if (id && form.type !== 'schedule') await startFlow(form.type, String(id))
    toast(`${form.title}已提交，等待审批`)
    current.value = null
    void loadMeSummary(true)
    await reload()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    busy.value = false
  }
}

/** 用印 / 用品的提交只认编号，其余类型的提交接口认 {id} */
async function startFlow(type: string, id: string) {
  if (type === 'seal' || type === 'supply') await meApi.submitApplyById(type, id)
  else await meApi.submitApply(type, { id })
}

/** 未提交的草稿（travel/schedule 之类直接落库的）补一次 submit 起流程 */
async function submitExisting(row: ApplyItem) {
  try {
    await startFlow(row.type, row.id)
    toast('已提交审批')
    void loadMeSummary(true)
    await reload()
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

async function cancel(row: ApplyItem) {
  try {
    await meApi.cancelApply(row.type, row.id, '本人撤销')
    toast('已撤销')
    void loadMeSummary(true)
    await reload()
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

/** 审批结论只在公司企业微信里，OA 不接回调，点了才回读一次。 */
async function syncApproval(row: ApplyItem) {
  syncing.value = row.id
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
  // 预拉印章 / 用品台账：点开表单时下拉里直接有真实选项，不用先等一次请求
  void meApi.seals().then((r) => { ledger.seals = r }).catch(() => {})
  void meApi.supplyItems().then((r) => { ledger.items = r }).catch(() => {})
})
</script>
