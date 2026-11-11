<!--
HrDocAdminView — HR 文档 CMS 管理后台（列表 + 状态筛选 + 直接发布）

按 category 分组列出所有 HR 文档：
- 草稿 / 待审 / 已发布 / 驳回 状态筛选
- 直接跳转到编辑器
- v1.9：发布不再需要审批 —— 草稿/驳回行点「发布」即对员工可见
  （原「提交审批 → 后台审批中心通过」两段式已随审批中心一起下线）
- 已发布行给「推送企微」（发布与推送解耦，推送是手动动作）
- 历史版本查看
-->
<template>
  <div class="oa-hr-admin">
    <PageHeader title="HR 文档" description="维护员工手册、制度与流程，管理版本、发布和签收。">
      <template #actions>
        <HrLayoutJobsLauncher class="hr-list-jobs" />
        <Button variant="outline" :disabled="loading" @click="loadDocs"><RefreshCw />刷新</Button>
        <Button @click="goNew"><Plus />新建文档</Button>
      </template>
    </PageHeader>

    <section class="panel hr-list-panel">
      <div class="hr-list-tabs">
        <div class="hr-view-switch" aria-label="文档视图">
          <Button :variant="filter.deleted ? 'ghost' : 'secondary'" size="sm" @click="filter.deleted = false"><Files />文档列表</Button>
          <Button :variant="filter.deleted ? 'secondary' : 'ghost'" size="sm" @click="filter.deleted = true"><Trash2 />回收站</Button>
        </div>
        <span class="hr-result-count">共 {{ filteredDocs.length }} 篇</span>
      </div>
      <form class="table-toolbar hr-filter-toolbar" @submit.prevent>
        <div class="hr-search"><Search /><Input v-model="filter.q" aria-label="搜索标题" placeholder="搜索文档标题" /></div>
        <div class="hr-category-filter"><Select v-model="filter.category" :options="categoryOptions" aria-label="按分类筛选" /></div>
        <div class="hr-status-filter"><Select v-model="filter.status" :options="statusOptions" aria-label="按状态筛选" /></div>
        <div class="hr-range-filter"><Select v-model="filter.timeRange" :options="timeRangeOptions" aria-label="按更新时间筛选" /></div>
        <Button v-if="hasFilters" variant="ghost" size="sm" @click="resetFilters">重置筛选</Button>
      </form>
      <div v-if="filter.timeRange === 'custom'" class="hr-date-range">
        <span>更新时间</span><Input v-model="filter.dateFrom" type="date" aria-label="起始日期" /><span>至</span><Input v-model="filter.dateTo" type="date" aria-label="结束日期" />
      </div>
      <div v-if="filter.deleted" class="hr-bin-notice"><Trash2 /><p>已删除文档不对员工展示。恢复后保留原有正文、版本和签收记录。</p></div>
      <div v-if="loading" class="hr-list-loading" role="status"><LoaderCircle class="animate-spin" />正在加载文档…</div>
      <EmptyState v-else-if="!filteredDocs.length" :icon="Files" :title="filter.deleted ? '回收站为空' : '没有符合条件的文档'" :description="hasFilters ? '调整筛选条件或清空搜索后重试。' : '创建文档后，在这里管理草稿与发布版本。'" compact>
        <Button v-if="hasFilters" variant="outline" size="sm" @click="resetFilters">清空筛选</Button>
        <Button v-else-if="!filter.deleted" size="sm" @click="goNew"><Plus />新建文档</Button>
      </EmptyState>
      <div v-else class="hr-table-scroll">
        <table class="data-table hr-doc-table">
          <colgroup><col /><col style="width:130px" /><col style="width:112px" /><col style="width:112px" /><col :style="{width: filter.deleted ? '130px' : '84px'}" /><col style="width:148px" /><col :style="{width: filter.deleted ? '120px' : '194px'}" /></colgroup>
          <thead><tr>
            <th>文档标题</th><th>分类</th><th>版本</th><th>发布状态</th><th>{{ filter.deleted ? '删除时间' : '签收' }}</th>
            <th :aria-sort="sortDir === 'desc' ? 'descending' : 'ascending'"><button class="hr-sort-button" @click="toggleSort('updated_at')">更新时间 <ArrowDown v-if="sortDir === 'desc'" /><ArrowUp v-else /></button></th>
            <th>操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="d in pagedDocs" :key="d.id" :class="{ 'hr-deleted-row': d.deleted_at }" :tabindex="d.deleted_at ? undefined : 0" @click="!d.deleted_at && goEdit(d.id)" @keydown.enter.self="!d.deleted_at && goEdit(d.id)">
              <td><div class="hr-title-cell"><FileText /><div><span class="hr-document-title" :title="effectiveTitle(d)">{{ effectiveTitle(d) || '无标题文档' }}</span><span v-if="hasPendingDraft(d)" class="hr-draft-hint">有未发布修改</span></div></div></td>
              <td>{{ catLabel(d.category) }}</td>
              <td><span class="hr-version">{{ d.version || '—' }}</span><small v-if="d.draft_version && d.draft_version !== d.version">草稿 {{ d.draft_version }}</small></td>
              <td><Badge :variant="statusVariant(displayStatus(d))">{{ statusLabel(displayStatus(d)) }}</Badge></td>
              <td><template v-if="filter.deleted"><span>{{ fmtDate(d.deleted_at) }}</span><small>{{ fmtTime(d.deleted_at) }}</small></template><Badge v-else-if="effectiveRequiresSignoff(d)" variant="outline">需签收</Badge><span v-else class="text-muted-foreground">—</span></td>
              <td><span class="hr-date">{{ fmtDate(d.updated_at) }}</span><small>{{ fmtTime(d.updated_at) }}</small></td>
              <td @click.stop @keydown.stop><div class="hr-row-actions">
                <Button v-if="d.deleted_at" variant="outline" size="sm" :disabled="busy[d.id]" @click="openUndelete(d)"><RotateCcw />恢复</Button>
                <template v-else>
                  <Button v-if="displayStatus(d) === 'offline' && !hasPendingDraft(d)" variant="ghost" size="sm" class="text-primary" :disabled="busy[d.id]" @click="openRestore(d)">重新上架</Button>
                  <Button v-else-if="displayStatus(d) === 'published' && !hasPendingDraft(d)" variant="ghost" size="sm" class="text-primary" @click="openPush(d)">推送企微</Button>
                  <Button v-else variant="ghost" size="sm" class="text-primary" :disabled="busy[d.id]" @click="openPublish(d)">发布</Button>
                  <Button variant="ghost" size="sm" @click="goEdit(d.id)">编辑</Button>
                  <DropdownMenu><template #trigger><Button variant="ghost" size="icon-sm" aria-label="更多文档操作"><Ellipsis /></Button></template>
                    <DropdownMenuItem @select="goPreview(d)"><Eye />预览文档</DropdownMenuItem>
                    <DropdownMenuItem @select="openVersions(d)"><History />历史版本</DropdownMenuItem>
                    <DropdownMenuItem v-if="canOffline(d)" destructive :disabled="busy[d.id]" @select="openOffline(d)"><Archive />下架</DropdownMenuItem>
                    <DropdownMenuItem v-if="canDelete(d)" destructive :disabled="busy[d.id]" @select="openDelete(d)"><Trash2 />删除</DropdownMenuItem>
                  </DropdownMenu>
                </template>
              </div></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="filteredDocs.length" class="hr-pagination-row">
        <div class="hr-page-size"><span>每页</span><Select v-model="pageSize" :options="PAGE_SIZE_OPTIONS.map(n => ({value:n,label:n+' 条'}))" size="sm" aria-label="每页条数" /></div>
        <Pagination v-model:page="page" :page-size="pageSize" :total="filteredDocs.length" />
      </div>
    </section>

    <Dialog v-model:open="versionsModal.open" :title="versionsModal.title + ' · 历史版本'" size="lg" class="portal-editor-floating">
      <div v-if="versionsModal.loading" class="hr-list-loading">正在加载历史版本…</div>
      <div v-else-if="versionsModal.versions.length" class="overflow-x-auto"><table class="data-table hr-version-table"><thead><tr><th>版本</th><th>标题</th><th>发布人</th><th>发布时间</th><th>操作</th></tr></thead><tbody>
        <tr v-for="v in versionsModal.versions" :key="v.version"><td>{{ v.version }}</td><td>{{ v.title }}</td><td>{{ v.published_by || '—' }}</td><td>{{ v.published_at ? v.published_at.slice(0,16).replace('T',' ') : '—' }}</td><td><Button variant="ghost" size="sm" @click="viewVersion(versionsModal.id,v.version)">查看快照</Button></td></tr>
      </tbody></table></div>
      <EmptyState v-else title="暂无已发布版本" description="首次发布后自动保留版本快照。" compact />
    </Dialog>
    <Dialog v-model:open="snapshotModal.open" :title="snapshotModal.title + ' · ' + snapshotModal.version" size="xl" class="portal-editor-floating">
      <HrBlocksRenderer v-if="snapshotModal.blocks" :blocks="snapshotModal.blocks" />
      <HrDocRenderer v-else :html="snapshotModal.html" />
    </Dialog>

    <!-- 发布确认（v1.9：审批已下线，发布是唯一推进动作，确认后立即对员工可见） -->
    <ConfirmDialog
      v-model:open="publishDialog.open"
      :title="`发布《${publishDialog.title}》？`"
      :message="publishDialog.message"
      confirm-text="发布"
      :busy="publishDialog.busy"
      @confirm="doPublish"
    />

    <!-- v1.20 下架确认（带原因输入）：下架只关员工端可见性，内容/版本/签收全留 -->
    <ConfirmDialog
      v-model:open="offlineDialog.open"
      :title="`下架《${offlineDialog.title}》？`"
      :message="offlineDialog.message"
      comment-label="下架原因（选填，最多 200 字）"
      confirm-text="下架"
      tone="danger"
      :busy="offlineDialog.busy"
      @confirm="doOffline"
    />

    <!-- v1.20 重新上架：只清下架标记，不升版本、不写新快照 -->
    <ConfirmDialog
      v-model:open="restoreDialog.open"
      :title="`重新上架《${restoreDialog.title}》？`"
      :message="restoreDialog.message"
      confirm-text="重新上架"
      :busy="restoreDialog.busy"
      @confirm="doRestore"
    />

    <!-- v1.22 逻辑删除：进回收站（列表默认不显示），内容/版本/签收记录全留，可恢复 -->
    <ConfirmDialog
      v-model:open="deleteDialog.open"
      :title="`删除《${deleteDialog.title}》？`"
      confirm-text="删除"
      tone="danger"
      :busy="deleteDialog.busy"
      @confirm="doDelete"
    >
      <template #body>
        <p>删除后管理列表不再显示，文档进入回收站。</p>
        <p>正文、历史版本、已签收记录都会保留，随时可以恢复。</p>
      </template>
    </ConfirmDialog>

    <HrDocPushDialog
      v-if="pushDialog.doc"
      v-model:open="pushDialog.open"
      :document-id="pushDialog.doc.id"
      :doc-title="effectiveTitle(pushDialog.doc)"
      :version="pushDialog.doc.version || ''"
      :dept-scope="effectiveDeptScope(pushDialog.doc)"
      :last-push="pushDialog.doc.last_push || null"
      @pushed="onPushed"
    />

    <ConfirmDialog
      v-model:open="pushResult.open"
      :title="pushResult.title"
      :message="pushResult.message"
      :tone="pushResult.tone"
      confirm-text="知道了"
      hide-cancel
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ConfirmDialog from '../../components/ui/ConfirmDialog.vue'
import HrDocPushDialog from '../../components/hr/HrDocPushDialog.vue'
import HrLayoutJobsLauncher from '../../components/ui/HrLayoutJobsLauncher.vue'
import HrDocRenderer from '../../components/HrDocRenderer.vue'
import HrBlocksRenderer from '../../components/HrBlocksRenderer.vue'
import { articleApi } from '../../../bridge/api'
import { PageHeader, Button, Input, Select, Badge, Pagination, EmptyState, DropdownMenu, DropdownMenuItem, Dialog } from '../../../bridge/oa-ui'
import { Plus, RefreshCw, Search, Files, FileText, Trash2, LoaderCircle, ArrowDown, ArrowUp, RotateCcw, Ellipsis, Eye, History, Archive } from '@lucide/vue'
import { useToast } from '../../composables/useToast'

const toast = useToast()
const router = useRouter()
const route = useRoute()

const docs = ref([])
const loading = ref(false)
const busy = reactive({})

// 筛选状态同步进 URL query：去编辑器再返回（或刷新）时搜索/筛选不丢
const filter = reactive({
  category: String(route.query.category || ''),
  status: String(route.query.status || ''),
  q: String(route.query.q || ''),
  // v1.22：回收站视图（只看已逻辑删除的）。与正常列表互斥 —— 后端按 deleted 参数分流。
  deleted: route.query.deleted === '1',
  // 更新时间范围：all / today / 7d / 30d / 90d / custom（前端过滤，列表本来就全量拉下来）
  timeRange: String(route.query.timeRange || 'all'),
  dateFrom: String(route.query.dateFrom || ''),
  dateTo: String(route.query.dateTo || ''),
})

// 回收站开关：切换要重新拉列表（deleted 是后端过滤，不是前端筛）
watch(() => filter.deleted, () => { page.value = 1; loadDocs() })

const CAT_LABEL = {
  handbook: '员工手册', incentive: '奖惩制度', hr: 'HR 制度',
  'hr-sop': '行政人事 SOP', policy: '其他政策',
}
const catLabel = (k) => CAT_LABEL[k] || k

// 时间显示到秒，但**分两行**：一行 19 字符在 13% 列宽里必然被 ellipsis 截成「19:15:…」。
// 日期在上、时间在下（浅色小字），既完整又不占横向空间。
function splitDateTime(iso) {
  if (!iso) return ['—', '']
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ['—', '']
  const p = (n) => String(n).padStart(2, '0')
  return [
    `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`,
    `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`,
  ]
}
const fmtDate = (iso) => splitDateTime(iso)[0]
const fmtTime = (iso) => splitDateTime(iso)[1]

const RANGE_DAYS = { '7d': 7, '30d': 30, '90d': 90 }

/** 按「更新时间」筛选。自定义区间用本地零点/末秒，避免把当天漏掉。 */
function inTimeRange(iso) {
  const r = filter.timeRange
  if (r === 'all') return true
  if (!iso) return false
  const t = new Date(iso).getTime()
  if (Number.isNaN(t)) return false
  if (r === 'custom') {
    if (filter.dateFrom && t < new Date(`${filter.dateFrom}T00:00:00`).getTime()) return false
    if (filter.dateTo && t > new Date(`${filter.dateTo}T23:59:59`).getTime()) return false
    return true
  }
  if (r === 'today') {
    const start = new Date()
    start.setHours(0, 0, 0, 0)
    return t >= start.getTime()
  }
  const days = RANGE_DAYS[r]
  return days ? t >= Date.now() - days * 86400000 : true
}

// 筛选下拉的选项：分类直接由 CAT_LABEL 派生（避免两处维护），空串统一表示「全部」
const categoryOptions = [
  { value: '', label: '全部分类' },
  ...Object.entries(CAT_LABEL).map(([value, label]) => ({ value, label })),
]
const timeRangeOptions = [
  { value: 'all', label: '全部时间' },
  { value: 'today', label: '今天' },
  { value: '7d', label: '近 7 天' },
  { value: '30d', label: '近 30 天' },
  { value: '90d', label: '近 90 天' },
  { value: 'custom', label: '自定义区间' },
]
const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'draft', label: '草稿' },
  { value: 'pending', label: '待审' },
  { value: 'published', label: '已发布' },
  { value: 'offline', label: '已下架' },
  { value: 'rejected', label: '驳回' },
]

// draft 元信息覆盖：admin 改稿后未审批通过前，列表显示草稿值（更准确反映"通过后将发布什么"）
const effectiveTitle = (d) => (d.draft_title != null ? d.draft_title : d.title)
const effectiveRequiresSignoff = (d) =>
  d.draft_requires_signoff != null ? d.draft_requires_signoff : d.requires_signoff
const effectiveDeptScope = (d) =>
  d.draft_dept_scope != null ? d.draft_dept_scope : d.dept_scope

// v1.20 下架：**显示态** = 下架优先于 status。下架是可见性维度（offline_at），
// 不是 status 列取值；但列表上要能当一种状态筛/显示，所以统一走这个派生函数。
// 🔴 2026-10-19：**有线上快照（published_at）也优先于 status** ——
// 已发布的文档一旦保存草稿，status 会变回 'draft'，但员工端仍在看上次发布的快照。
// 之前按 status 显示成「草稿」，会让人误以为「没发布怎么员工能看到」。
const displayStatus = (d) => {
  // 🔴 不显示「已删除」（用户 2026-10-21）：回收站里每一行都是已删除，再标一次是零信息量。
  // 这里始终给**删除前的状态** —— 它才是有用的判断依据：
  // 已发布 = 恢复后员工立刻能看；草稿 = 恢复后还是草稿。
  // 「已删除」这件事由行灰化 + 删除时间列 + 卡片标题「回收站」表达，不重复。
  if (d.offline_at) return 'offline'
  if (d.published_at) return 'published'
  return d.status
}
// 「已发布但改了草稿」：员工看的是上一版，列表要提示还有未发布的修改
const hasPendingDraft = (d) => !!d.published_at && d.status !== 'published'
// 只有「有线上快照且未下架」的文档可以下架（从未发布的没有可见性可关）。
// 🔴 判据用 published_at 而非 status：改过草稿的已发布文档 status='draft'，但员工仍能看到，
// 必须允许下架（后端同样按 published_at 校验）。
const canOffline = (d) => !!d.published_at && !d.offline_at

// ── 排序（v1.23）──
// 默认按更新时间降序：打开列表最想知道「最近改了什么」。
// 列表接口一次给全量、分页在前端做，所以排序也放前端（不用动后端 order_by）。
const sortKey = ref('updated_at')
const sortDir = ref(route.query.sortDir === 'asc' ? 'asc' : 'desc')   // 'desc' | 'asc'
function toggleSort(key) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'desc' ? 'asc' : 'desc'
  } else {
    sortKey.value = key
    sortDir.value = 'desc'
  }
  // 换排序回第一页，否则可能停在已经不存在的页码上
  page.value = 1
}
// ↕ 未排序 / ↓ 降序 / ↑ 升序
const sortMark = (key) => (sortKey.value !== key ? '↕' : (sortDir.value === 'desc' ? '↓' : '↑'))

const filteredDocs = computed(() => {
  const q = filter.q.trim().toLowerCase()
  const list = docs.value.filter((d) => {
    if (filter.category && d.category !== filter.category) return false
    if (filter.status && displayStatus(d) !== filter.status) return false
    if (!inTimeRange(d.updated_at)) return false
    if (q) {
      // 正式标题与未发布的草稿标题都可搜索。
      const title = (d.title || '').toLowerCase()
      const draftTitle = (d.draft_title || '').toLowerCase()
      if (!(title.includes(q) || draftTitle.includes(q))) return false
    }
    return true
  })
  // v1.23：按更新时间排序，默认降序（最近改过的排最前 —— 用户要的默认视图）
  const dir = sortDir.value === 'desc' ? -1 : 1
  return list.sort((a, b) => {
    const va = a[sortKey.value] ? new Date(a[sortKey.value]).getTime() : 0
    const vb = b[sortKey.value] ? new Date(b[sortKey.value]).getTime() : 0
    return (va - vb) * dir
  })
})

// ── 分页（照 BrandDocsAdminView 的约定）──
// 列表接口一次给全量，分页在前端做：筛完再切页。
// 每页条数可调，选项用 Ant Design 的 pageSizeOptions 默认值 [10, 20, 50, 100]，
// 默认 10（AntD 的 defaultPageSize 也是 10）。
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100]
const pageSize = ref(PAGE_SIZE_OPTIONS.includes(Number(route.query.pageSize)) ? Number(route.query.pageSize) : 10)
const page = ref(Math.max(1, Number(route.query.page) || 1))
const pageCount = computed(() =>
  Math.max(1, Math.ceil(filteredDocs.value.length / pageSize.value)))
const pagedDocs = computed(() =>
  filteredDocs.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
// 筛选条件一变就回第一页 —— 否则可能停在一个已经不存在的页码上（比如从 54 篇筛到 3 篇）
watch(() => [filter.q, filter.category, filter.status, filter.timeRange, filter.dateFrom, filter.dateTo], () => { page.value = 1 })
// 改每页条数也回第一页：第 6 页在每页 50 条时根本不存在（AntD 同样会重置页码）
watch(pageSize, () => { page.value = 1 })
// 数据变少时把页码夹回合法范围
watch(pageCount, (n) => { if (page.value > n) page.value = n })

async function loadDocs() {
  loading.value = true
  try {
    // v1.22：deleted=true 切到回收站（后端过滤），默认只拉正常文档
    const r = await articleApi.adminList(filter.deleted ? { deleted: 1 } : {})
    // api interceptor 已 unwrap envelope → r === {success, data, error}；docs 数组在 r.data
    docs.value = r.data || []
  } catch (e) {
    toast.show('加载失败：' + (e.response?.data?.detail || e.message), 'err')
  } finally {
    loading.value = false
  }

}

function goNew() {
  router.push('/portal/hr-documents/new')
}
function goEdit(id) {
  router.push(`/portal/hr-documents/id/${id}/edit`)
}

function goPreview(d) { router.push(`/portal/hr-documents/id/${d.id}/preview`) }
const hasFilters = computed(() => !!(filter.q || filter.category || filter.status || filter.timeRange !== 'all'))
function resetFilters() { Object.assign(filter, {q:'',category:'',status:'',timeRange:'all',dateFrom:'',dateTo:''}); page.value=1 }
const statusLabel = (status) => ({draft:'草稿',pending:'待审',published:'已发布',offline:'已下架',rejected:'已驳回'}[status] || status)
const statusVariant = (status) => ({published:'success',pending:'warning',offline:'secondary',rejected:'destructive'}[status] || 'secondary')
watch(() => [filter.category, filter.status, filter.q, filter.deleted, filter.timeRange, filter.dateFrom, filter.dateTo, page.value, pageSize.value, sortDir.value], () => {
  const query = {}
  for (const key of ['category','status','q','dateFrom','dateTo']) if (filter[key]) query[key] = filter[key]
  if (filter.deleted) query.deleted='1'
  if (filter.timeRange !== 'all') query.timeRange=filter.timeRange
  if (page.value > 1) query.page=String(page.value)
  if (pageSize.value !== 10) query.pageSize=String(pageSize.value)
  if (sortDir.value !== 'desc') query.sortDir=sortDir.value
  router.replace({ query })
})

// ── 直接发布（v1.9：审批环节已下线，草稿/驳回直接发布）──
const publishDialog = reactive({ open: false, busy: false, doc: null, title: '', message: '' })

function openPublish(d) {
  publishDialog.doc = d
  publishDialog.title = effectiveTitle(d)
  publishDialog.message = `发布后立即对员工可见（版本 ${d.draft_version || d.version}）。`
  publishDialog.open = true
}

async function doPublish() {
  const d = publishDialog.doc
  if (!d) return
  publishDialog.busy = true
  busy[d.id] = true
  try {
    await articleApi.adminPublish(d.id)
    publishDialog.open = false
    toast.show(`《${effectiveTitle(d)}》已发布，员工端立即可见`, 'ok')
    await loadDocs()
  } catch (e) {
    publishDialog.open = false
    // 发布有前置校验（必须有草稿内容、版本号不能重复）—— 把后端原因原样透出，别吞掉
    toast.show('发布失败：' + (e.response?.data?.detail || e.message), 'err')
  } finally {
    publishDialog.busy = false
    busy[d.id] = false
  }
}

// ── 下架 / 重新上架（v1.20）──
// 下架只关员工端可见性：正文 / 封面 / 历史版本 / 已签收记录全部保留。
// 重新上架**不升版本、不写新快照** —— 内容没变就没有新版本（否则会被「版本已发布过」拦住）；
// 下架后改了稿再发布，走的是普通「发布」，后端发布成功会顺带清下架标记。
const offlineDialog = reactive({ open: false, busy: false, doc: null, title: '', message: '' })
const restoreDialog = reactive({ open: false, busy: false, doc: null, title: '', message: '' })
// v1.22 逻辑删除（进回收站）
const deleteDialog = reactive({ open: false, busy: false, doc: null, title: '', message: '' })

function openOffline(d) {
  offlineDialog.doc = d
  offlineDialog.title = effectiveTitle(d)
  offlineDialog.message =
    `下架后员工端列表立即隐藏，已发出的链接会提示「文档已下架」。` +
    `正文、历史版本、已签收记录全部保留，可随时重新上架（版本 ${d.version}）。` +
    `若本文档绑定旧页面入口，下架后会恢复显示原来的页面。`
  offlineDialog.open = true
}

async function doOffline({ comment } = {}) {
  const d = offlineDialog.doc
  if (!d) return
  offlineDialog.busy = true
  busy[d.id] = true
  try {
    await articleApi.adminOffline(d.id, (comment || '').trim() || undefined)
    offlineDialog.open = false
    toast.show(`《${effectiveTitle(d)}》已下架，员工端不再可见`, 'ok')
    await loadDocs()
  } catch (e) {
    offlineDialog.open = false
    toast.show('下架失败：' + (e.response?.data?.detail || e.message), 'err')
  } finally {
    offlineDialog.busy = false
    busy[d.id] = false
  }
}

// 列表卡片标题：回收站视图换个名字，别让用户以为进错了页面
// 🔴 script 里访问 computed 必须 .value（模板才自动解包）—— 写 filteredDocs.length 会得到 undefined
const listCardTitle = computed(() =>
  filter.deleted
    ? `回收站（${filteredDocs.value.length} 篇）`
    : `文档列表（${filteredDocs.value.length} 篇）`
)

// ── v1.22 逻辑删除（进回收站，可恢复）──
// 🔴 已发布且未下架的文档**不许删**（用户 2026-10-21）：员工端可能正在看、企微里还有链接，
// 必须先「下架」让员工端不可见，再删。判据用 published_at + offline_at，不用 status。
const canDelete = (d) => !d.deleted_at && !(d.published_at && !d.offline_at)

function openDelete(d) {
  if (!canDelete(d)) {
    toast.show('这篇文档已发布且未下架 —— 请先「下架」，再删除', 'err')
    return
  }
  deleteDialog.doc = d
  deleteDialog.title = effectiveTitle(d)
  // 正文走 #body slot（分段 + 警示块），这里不再拼一段长文案
  deleteDialog.open = true
}

async function doDelete() {
  const d = deleteDialog.doc
  if (!d) return
  deleteDialog.busy = true
  busy[d.id] = true
  try {
    await articleApi.adminDelete(d.id)
    deleteDialog.open = false
    toast.show(`《${effectiveTitle(d)}》已删除，可在回收站恢复`, 'ok')
    await loadDocs()
  } catch (e) {
    deleteDialog.open = false
    toast.show('删除失败：' + (e.response?.data?.detail || e.message), 'err')
  } finally {
    deleteDialog.busy = false
    busy[d.id] = false
  }
}

// 恢复不弹确认框：低风险、可反复操作
async function openUndelete(d) {
  busy[d.id] = true
  try {
    await articleApi.adminUndelete(d.id)
    toast.show(`《${effectiveTitle(d)}》已恢复`, 'ok')
    await loadDocs()
  } catch (e) {
    toast.show('恢复失败：' + (e.response?.data?.detail || e.message), 'err')
  } finally {
    busy[d.id] = false
  }
}

function openRestore(d) {
  restoreDialog.doc = d
  restoreDialog.title = effectiveTitle(d)
  restoreDialog.message =
    `上架后员工端立即可见，版本仍为 ${d.version}（不产生新版本）。` +
    `如果下架期间改过内容，请到编辑器里点「发布」以发布新版本。`
  restoreDialog.open = true
}

async function doRestore() {
  const d = restoreDialog.doc
  if (!d) return
  restoreDialog.busy = true
  busy[d.id] = true
  try {
    await articleApi.adminRestore(d.id)
    restoreDialog.open = false
    toast.show(`《${effectiveTitle(d)}》已重新上架，员工端立即可见`, 'ok')
    await loadDocs()
  } catch (e) {
    restoreDialog.open = false
    toast.show('上架失败：' + (e.response?.data?.detail || e.message), 'err')
  } finally {
    restoreDialog.busy = false
    busy[d.id] = false
  }
}

// ── 推送企微（v1.8：与审核拆开后新增）──
const pushDialog = reactive({ open: false, doc: null })
const pushResult = reactive({ open: false, title: '', message: '', tone: 'primary' })

async function openPush(d) {
  // 🔴 last_push 只有详情端点（adminGet）返回，列表端点没有 ——
  // 直接用列表项的话「上次推送时间」永远是「尚未推送过」，24h 频控提示也就永远不出现。
  pushDialog.doc = d
  pushDialog.open = true
  try {
    const res = await articleApi.adminGet(d.id)
    if (res?.data) pushDialog.doc = { ...d, ...res.data }
  } catch {
    // 拉不到详情不影响推送本身，弹框按「尚未推送过」显示即可
  }
}

async function onPushed(result) {
  await loadDocs()   // 刷新列表：详情里的 last_push 变了，下次打开弹框能看到
  const bits = []
  if (result.skipped) bits.push('被 24 小时频控拦下，一条都没发')
  else if (result.no_recipients) bits.push('按这个范围没圈到任何收件人')
  else bits.push(`已发送 ${result.sent} 人`)
  if (result.failed) bits.push(`失败 ${result.failed} 人`)
  const bad = result.skipped || result.failed || result.no_recipients
  pushResult.title = bad ? '推送未全部成功' : '推送成功'
  pushResult.message = bits.join('；') + '。'
  pushResult.tone = bad ? 'warn' : 'primary'
  pushResult.open = true
}

// 操作列的「⋯」菜单


const versionsModal = reactive({ open: false, id: 0, title: '', loading: false, versions: [] })
async function openVersions(d) {
  versionsModal.id = d.id
  versionsModal.title = d.title
  versionsModal.open = true
  versionsModal.loading = true
  versionsModal.versions = []
  try {
    const r = await articleApi.adminVersions(d.id)
    versionsModal.versions = r.data || []
  } catch (e) {
    toast.show('加载历史失败', 'err')
  } finally {
    versionsModal.loading = false
  }
}
function closeVersions() { versionsModal.open = false }

const snapshotModal = reactive({ open: false, title: '', version: '', html: '', blocks: null })
async function viewVersion(id, version) {
  try {
    const r = await articleApi.adminVersion(id, version)
    snapshotModal.title = r.data.title
    snapshotModal.version = version
    snapshotModal.html = r.data.content_html
    // 块格式版本的快照存 content_blocks（HTML 列留 NULL），非空才走块渲染
    snapshotModal.blocks = Array.isArray(r.data.content_blocks) && r.data.content_blocks.length > 0
      ? r.data.content_blocks
      : null
    snapshotModal.open = true
  } catch (e) {
    toast.show('加载快照失败', 'err')
  }
}
function closeSnapshot() { snapshotModal.open = false }

onMounted(loadDocs)
</script>

<style scoped>
.oa-hr-admin { min-width: 0; font-family: var(--font-sans); color: var(--foreground); }
.hr-list-panel { margin-top: 20px; padding: 0; overflow: hidden; }
.hr-list-tabs { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:14px 18px; border-bottom:1px solid var(--border); }
.hr-view-switch { display:flex; align-items:center; gap:4px; }
.hr-result-count { color:var(--muted-foreground); font-size:12px; white-space:nowrap; }
.hr-filter-toolbar { padding:16px 18px; gap:10px; }
.hr-search { position:relative; width:240px; }
.hr-search > :deep(svg) { position:absolute; width:15px; height:15px; left:11px; top:11px; color:var(--muted-foreground); pointer-events:none; }
.hr-search :deep(input) { padding-left:33px; }
.hr-category-filter { width:154px; } .hr-status-filter { width:138px; } .hr-range-filter { width:150px; }
.hr-date-range { display:flex; align-items:center; flex-wrap:wrap; gap:9px; padding:0 18px 16px; font-size:12px; color:var(--muted-foreground); }
.hr-date-range :deep(input) { width:170px; }
.hr-bin-notice { display:flex; align-items:center; gap:9px; margin:0 18px 16px; padding:10px 12px; border-radius:8px; background:var(--muted); font-size:12px; color:var(--muted-foreground); }
.hr-bin-notice > svg { width:16px; height:16px; flex-shrink:0; }
.hr-list-loading { display:flex; justify-content:center; align-items:center; gap:9px; min-height:200px; font-size:13px; color:var(--muted-foreground); }
.hr-list-loading > svg { width:18px; height:18px; }
.hr-table-scroll { overflow:auto; border-top:1px solid var(--border); scrollbar-width:thin; }
.hr-doc-table { min-width:1080px; width:100%; table-layout:fixed; }
.hr-doc-table :is(thead,tbody) { display:table-header-group; } .hr-doc-table tbody { display:table-row-group; }
.hr-doc-table tr { display:table-row; } .hr-doc-table :is(th,td) { display:table-cell; vertical-align:middle; text-align:left; padding:13px 16px; }
.hr-doc-table tbody tr { cursor:pointer; } .hr-doc-table tbody tr:focus-visible { outline:2px solid var(--ring); outline-offset:-2px; }
.hr-doc-table td { white-space:normal; font-size:13px; line-height:1.5; }
.hr-doc-table td small { display:block; margin-top:3px; font-size:11px; color:var(--muted-foreground); font-weight:400; }
.hr-title-cell { display:flex; align-items:center; gap:10px; min-width:0; }
.hr-title-cell > svg { width:17px; height:17px; flex-shrink:0; color:var(--muted-foreground); }
.hr-title-cell > div { min-width:0; }
.hr-document-title { display:block; color:var(--foreground); font-weight:500; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.hr-draft-hint { display:block; margin-top:3px; font-size:11px; font-weight:400; color:var(--warning); }
.hr-date,.hr-version { font-variant-numeric:tabular-nums; }
.hr-sort-button { display:inline-flex; align-items:center; gap:5px; cursor:pointer; background:transparent; border:0; font:inherit; color:inherit; }
.hr-sort-button svg { width:13px; height:13px; }
.hr-row-actions { display:flex; align-items:center; gap:0; white-space:nowrap; }
.hr-row-actions :deep(button) { padding-left:8px; padding-right:8px; }
.hr-deleted-row { cursor:default !important; }
.hr-pagination-row { display:flex; align-items:center; gap:16px; padding:14px 18px; border-top:1px solid var(--border); }
.hr-pagination-row > :last-child { flex:1; }
.hr-page-size { display:flex; align-items:center; gap:8px; color:var(--muted-foreground); font-size:12px; white-space:nowrap; }
.hr-page-size > :deep(button) { width:88px; }
@media(max-width:700px) { .hr-list-panel { margin-top:16px; } .hr-filter-toolbar { display:grid; grid-template-columns:1fr 1fr; padding:12px; } .hr-search { width:100%; grid-column:1/-1; } .hr-category-filter,.hr-status-filter,.hr-range-filter { width:100%; } .hr-list-tabs,.hr-pagination-row { padding:12px; } .hr-pagination-row { align-items:stretch; flex-direction:column; } }
</style>
