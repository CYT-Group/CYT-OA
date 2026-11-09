<template>
  <div>
    <PageHeader v-if="config" :title="group?.title ?? config.title" :description="group?.desc ?? config.desc">
      <template #actions>
        <Button v-if="canCreate" size="sm" @click="openCreate">
          <Plus class="size-3.5" />
          新增
        </Button>
        <Button
          v-for="a in config.pageActions || []"
          :key="a.label"
          size="sm"
          :variant="actionVariant(a.tone)"
          @click="runPageAction(a)"
        >
          {{ a.label }}
        </Button>
      </template>
    </PageHeader>

    <section v-if="!config" class="panel mt-5">
      <EmptyState title="未找到页面配置" :description="`页面标识：${key}`" :icon="FileQuestion" />
    </section>

    <template v-else>
      <SegmentedTabs
        v-if="tabs.length > 1"
        class="mt-4"
        :model-value="activeTab?.key ?? ''"
        :items="tabItems"
        @update:model-value="onTab"
      />

      <section class="panel mt-4 overflow-hidden">
        <div class="table-caption">
          <h2 class="panel-title">{{ config.title }}<Badge variant="secondary">数据列表</Badge></h2>
          <span v-if="!loadError" class="text-xs tabular-nums text-muted-foreground">{{
            loading ? '正在加载…' : `共 ${total} 条记录`
          }}</span>
        </div>
        <div class="table-toolbar">
          <form
            v-if="searchFields.length"
            ref="searchForm"
            class="flex flex-wrap items-center gap-2"
            @submit.prevent="doSearch"
          >
            <template v-for="(f, fi) in searchFields" :key="f.key">
              <Select
                v-if="isSelect(f)"
                v-model="search[f.key]"
                size="sm"
                class="w-40"
                :aria-label="f.label"
                :options="[{ label: f.blankLabel || `全部${f.label}`, value: '' }, ...optionsOf(f)]"
              />
              <Input
                v-else
                v-model="search[f.key]"
                class="h-9 w-40 text-[13px] sm:w-56"
                :placeholder="f.placeholder || `搜索${f.label}`"
                :aria-label="f.label"
                :data-page-search="fi === 0 ? '' : undefined"
              />
            </template>
            <Button type="submit" size="sm" :disabled="loading">
              <Search class="size-3.5" />
              查询
            </Button>
            <Button type="button" variant="ghost" size="sm" @click="resetSearch">重置</Button>
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              aria-label="刷新"
              :disabled="loading"
              class="sm:hidden"
              @click="load"
            >
              <RefreshCw :class="['size-3.5', loading && 'animate-spin']" />
            </Button>
          </form>

          <div
            class="ml-auto flex shrink-0 items-center gap-1.5"
            :class="searchFields.length ? 'hidden sm:flex' : ''"
          >
            <span v-if="searchFields.length" class="mr-1 hidden text-[11px] text-muted-foreground md:inline">
              按 <span class="kbd">/</span> 搜索
            </span>
            <Button variant="ghost" size="icon-sm" aria-label="刷新" :disabled="loading" @click="load">
              <RefreshCw :class="['size-3.5', loading && 'animate-spin']" />
            </Button>
          </div>
        </div>

        <div
          v-if="selected.size"
          class="flex flex-wrap items-center gap-2 border-t border-border/70 bg-primary/[0.04] px-3 py-2"
        >
          <span class="text-xs text-muted-foreground">
            已选 <b class="font-medium tabular-nums text-foreground">{{ selected.size }}</b> 项
          </span>
          <Button variant="ghost" size="sm" @click="clearSelection">取消选择</Button>
          <Button v-if="canDelete" variant="destructive" size="sm" :disabled="busy" @click="askBulkDelete">
            <Trash2 class="size-3.5" />
            批量删除
          </Button>
        </div>

        <Callout v-if="loadError" tone="destructive" title="数据加载失败" class="mx-5 mb-5">
          {{ loadError }}
          <template #action><Button variant="outline" size="sm" @click="load">重试</Button></template>
        </Callout>

        <div v-if="rows.length || loading" class="border-t border-border/70">
          <div class="max-h-[calc(100dvh-15rem)] overflow-auto">
            <table class="data-table" :class="{ 'opacity-60': loading && rows.length }">
              <thead>
                <tr>
                  <th class="w-10 pr-0">
                    <Checkbox
                      :model-value="allSelected ? true : selected.size ? 'indeterminate' : false"
                      :disabled="!rows.length || loading"
                      aria-label="全选当前页"
                      @update:model-value="toggleAll"
                    />
                  </th>
                  <th v-for="c in config.columns" :key="c.key" :style="c.width ? { width: c.width } : {}">
                    <button
                      type="button"
                      class="group/th -mx-1 inline-flex items-center gap-1 rounded px-1 py-0.5 transition-colors hover:text-foreground"
                      @click="toggleSort(c.key)"
                    >
                      {{ c.label }}
                      <component
                        :is="sortIcon(c.key)"
                        class="size-3 shrink-0"
                        :class="
                          sortKey === c.key
                            ? 'text-foreground'
                            : 'text-muted-foreground/40 group-hover/th:text-muted-foreground'
                        "
                      />
                    </button>
                  </th>
                  <th v-if="hasRowActions" class="sticky-col whitespace-nowrap text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in sortedRows" :key="String(row.id)" :data-selected="isSelected(row)">
                  <td class="pr-0">
                    <Checkbox
                      :model-value="isSelected(row)"
                      :aria-label="`选择 ${String(row.id)}`"
                      @update:model-value="toggleRow(row)"
                    />
                  </td>
                  <td
                    v-for="(c, ci) in config.columns"
                    :key="c.key"
                    :class="ci === 0 ? 'font-medium text-foreground' : ''"
                  >
                    <Badge v-if="pillOf(row, c)" :variant="toneBadge(pillOf(row, c)!.tone)">
                      {{ pillOf(row, c)!.text }}
                    </Badge>
                    <span v-else-if="c.ellipsis" class="block max-w-72 truncate" :title="cellText(row, c)">{{
                      cellText(row, c)
                    }}</span>
                    <template v-else>{{ cellText(row, c) }}</template>
                  </td>
                  <td v-if="hasRowActions" class="sticky-col">
                    <div class="flex items-center justify-end gap-0.5 whitespace-nowrap">
                      <Button v-if="canEdit" variant="ghost" size="sm" @click="openEdit(row)">编辑</Button>
                      <DropdownMenu v-if="visibleRowActions(row).length">
                        <template #trigger>
                          <Button variant="ghost" size="icon-sm" aria-label="更多操作">
                            <MoreHorizontal class="size-4" />
                          </Button>
                        </template>
                        <DropdownMenuItem v-if="canDelete" destructive @select="askDelete(row)">
                          <Trash2 class="size-3.5" />
                          删除
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          v-for="a in visibleRowActions(row)"
                          :key="a.label"
                          :destructive="a.tone === 'dan'"
                          @select="runRowAction(a, row)"
                          >{{ a.label }}</DropdownMenuItem
                        >
                      </DropdownMenu>
                      <Button
                        v-else-if="canDelete"
                        variant="ghost"
                        size="sm"
                        class="text-destructive hover:bg-destructive/10 hover:text-destructive"
                        @click="askDelete(row)"
                        >删除</Button
                      >
                    </div>
                  </td>
                </tr>
                <template v-if="loading && !rows.length">
                  <tr v-for="i in 8" :key="`sk-${i}`">
                    <td class="pr-0"><Skeleton class="size-4" /></td>
                    <td v-for="(c, ci) in config.columns" :key="c.key">
                      <Skeleton :class="ci === 0 ? 'h-3.5 w-32' : 'h-3.5 w-20'" />
                    </td>
                    <td v-if="hasRowActions" class="sticky-col"><Skeleton class="ml-auto h-3.5 w-24" /></td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>

        <EmptyState
          v-if="!loading && !rows.length && !loadError"
          :icon="Inbox"
          title="暂无数据"
          description="调整搜索条件，或新增一条记录"
          compact
          class="border-t border-border/70"
        >
          <Button v-if="canCreate" variant="outline" size="sm" @click="openCreate"
            ><Plus class="size-3.5" />新增记录</Button
          >
        </EmptyState>

        <div v-if="total > 0" class="border-t border-border/70 px-5 py-4">
          <Pagination :page="pageNo" :page-size="pageSize" :total="total" @update:page="goto" />
        </div>
      </section>
    </template>

    <Dialog
      :open="editOpen"
      :title="editing ? '编辑' : '新增'"
      :description="editing ? `修改「${config?.title ?? ''}」记录` : `填写后保存到「${config?.title ?? ''}」`"
      size="lg"
      @update:open="editOpen = $event"
    >
      <form class="grid grid-cols-1 gap-x-4 gap-y-4 sm:grid-cols-2" @submit.prevent="save">
        <div
          v-for="f in formFields"
          :key="f.key"
          class="flex flex-col"
          :class="{ 'sm:col-span-2': f.type === 'textarea' || f.json }"
        >
          <template v-if="f.type === 'switch'">
            <div class="flex items-center justify-between gap-4 rounded-lg border px-3 py-2.5">
              <div class="min-w-0">
                <Label :for="`record-${f.key}`" class="mb-0">{{ f.label }}</Label>
                <p v-if="f.hint" class="mt-0.5 text-xs text-muted-foreground">{{ f.hint }}</p>
              </div>
              <Switch :id="`record-${f.key}`" v-model="form[f.key]" :aria-label="f.label" />
            </div>
          </template>
          <template v-else>
            <Label :for="`record-${f.key}`" :required="f.required">
              {{ f.label }}
              <span v-if="f.hint" class="font-normal text-muted-foreground">{{ f.hint }}</span>
            </Label>
            <Select
              v-if="isSelect(f)"
              :id="`record-${f.key}`"
              :aria-label="f.label"
              :aria-invalid="!!formErr[f.key]"
              :aria-describedby="formErr[f.key] ? `record-error-${f.key}` : undefined"
              v-model="form[f.key]"
              :options="[{ label: f.blankLabel || '请选择', value: '' }, ...optionsOf(f)]"
            />
            <Textarea
              v-else-if="f.type === 'textarea' || f.json"
              :id="`record-${f.key}`"
              :aria-invalid="!!formErr[f.key]"
              :aria-describedby="formErr[f.key] ? `record-error-${f.key}` : undefined"
              v-model="form[f.key]"
              :rows="f.json ? 6 : 4"
              :placeholder="f.placeholder || `请输入${f.label}`"
            />
            <Input
              v-else
              :id="`record-${f.key}`"
              :aria-invalid="!!formErr[f.key]"
              :aria-describedby="formErr[f.key] ? `record-error-${f.key}` : undefined"
              v-model="form[f.key]"
              :type="
                f.type === 'number'
                  ? 'number'
                  : f.type === 'datetime'
                    ? 'datetime-local'
                    : f.type === 'date'
                      ? 'date'
                      : 'text'
              "
              :readonly="f.readonly"
              :placeholder="f.placeholder || `请输入${f.label}`"
            />
            <p
              v-if="formErr[f.key]"
              :id="`record-error-${f.key}`"
              class="mt-1 text-xs text-destructive"
              role="alert"
            >
              {{ formErr[f.key] }}
            </p>
          </template>
        </div>
      </form>
      <template #footer>
        <Button variant="outline" @click="editOpen = false">取消</Button>
        <Button :loading="saving" @click="save">保存</Button>
      </template>
    </Dialog>

    <Dialog
      :open="!!promptState"
      :title="promptState?.action.label ?? ''"
      @update:open="
        (v) => {
          if (!v) promptState = null
        }
      "
    >
      <div v-if="promptState?.action.prompt" class="flex flex-col">
        <Label :required="promptState.action.prompt.required">{{ promptState.action.prompt.label }}</Label>
        <Textarea
          v-if="promptState.action.prompt.type !== 'number'"
          v-model="promptValue"
          :rows="3"
          :placeholder="`请输入${promptState.action.prompt.label}`"
        />
        <Input v-else v-model="promptValue" type="number" />
      </div>
      <template #footer>
        <Button variant="outline" @click="promptState = null">取消</Button>
        <Button :loading="busy" @click="confirmPrompt">确定</Button>
      </template>
    </Dialog>

    <ConfirmDialog
      :open="!!confirmState"
      title="请确认"
      :description="confirmState?.text ?? ''"
      :tone="confirmState?.danger ? 'destructive' : 'default'"
      :busy="busy"
      @update:open="
        (v) => {
          if (!v) confirmState = null
        }
      "
      @confirm="confirmYes"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  ArrowUp,
  ChevronsUpDown,
  FileQuestion,
  Inbox,
  MoreHorizontal,
  Plus,
  RefreshCw,
  Search,
  Trash2
} from '@lucide/vue'
import { pageConfigs, tabbedPages } from '../configs/pages'
import type { ColumnConfig, FieldConfig, PageAction, RowAction } from '../configs/types'
import { api } from '../lib/api'
import { dictOptions, type DictItem } from '../lib/dict'
import { remoteOptions, type SelectOption } from '../lib/options'
import { errText, toast } from '../lib/toast'
import { toneBadge } from '../lib/tone'
import Badge from '../ui/Badge.vue'
import Button from '../ui/Button.vue'
import Callout from '../ui/Callout.vue'
import Checkbox from '../ui/Checkbox.vue'
import ConfirmDialog from '../ui/ConfirmDialog.vue'
import Dialog from '../ui/Dialog.vue'
import DropdownMenu from '../ui/DropdownMenu.vue'
import DropdownMenuItem from '../ui/DropdownMenuItem.vue'
import EmptyState from '../ui/EmptyState.vue'
import Input from '../ui/Input.vue'
import Label from '../ui/Label.vue'
import PageHeader from '../ui/PageHeader.vue'
import Pagination from '../ui/Pagination.vue'
import SegmentedTabs from '../ui/SegmentedTabs.vue'
import Select from '../ui/Select.vue'
import Skeleton from '../ui/Skeleton.vue'
import Switch from '../ui/Switch.vue'
import Textarea from '../ui/Textarea.vue'

const route = useRoute()
const router = useRouter()
const key = computed(() => String(route.params.key ?? ''))
const group = computed(() => tabbedPages[key.value])
const tabs = computed(() => group.value?.tabs ?? [])
/** 第一个页签是默认态，不占查询参数，URL 保持干净的 /page/{key} */
const activeTab = computed(
  () => tabs.value.find((t) => t.key === String(route.query.tab ?? '')) ?? tabs.value[0]
)
const configKey = computed(() => (group.value ? activeTab.value?.config : key.value) ?? '')
const config = computed(() => pageConfigs[configKey.value])

const tabItems = computed(() => tabs.value.map((t) => ({ value: t.key, label: t.title })))

function onTab(tabKey: string) {
  const tab = tabs.value.find((t) => t.key === tabKey)
  if (tab) pickTab(tab)
}

function pickTab(tab: { key: string }) {
  const query = { ...route.query } as Record<string, string>
  if (tab.key === tabs.value[0]?.key) delete query.tab
  else query.tab = tab.key
  router.push({ path: `/page/${key.value}`, query })
}

const rows = ref<Record<string, unknown>[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 10
const loading = ref(false)
const loadError = ref('')
const search = reactive<Record<string, string>>({})
const dictCache = new Map<string, DictItem[]>()
/** 来自接口的下拉选项（负责人要选在职员工这种），按 url + 取值口径缓存 */
const optionCache = new Map<string, SelectOption[]>()
const optionKey = (f: FieldConfig) =>
  `${f.optionsApi}#${f.optionLabel ?? 'label'}#${f.optionValue ?? 'value'}`

const editOpen = ref(false)
const editing = ref<Record<string, unknown> | null>(null)
const saving = ref(false)
const form = reactive<Record<string, any>>({})
const formErr = reactive<Record<string, string>>({})

const busy = ref(false)
const promptState = ref<{ action: RowAction | PageAction; row?: Record<string, unknown> } | null>(null)
const promptValue = ref('')
const confirmState = ref<{ text: string; fn: () => void; danger?: boolean } | null>(null)

// ===== 派生 =====
const searchFields = computed(() => config.value?.search ?? [])
const formFields = computed(() => config.value?.fields ?? [])
const canCreate = computed(
  () =>
    config.value !== undefined &&
    config.value.canCreate !== false &&
    !!config.value.fields?.length &&
    !!config.value.createApi
)
const canEdit = computed(
  () =>
    config.value !== undefined &&
    config.value.canEdit !== false &&
    !!config.value.fields?.length &&
    !!config.value.updateApi
)
const canDelete = computed(
  () => config.value !== undefined && config.value.canDelete !== false && !!config.value.deleteApi
)
const hasRowActions = computed(() => canEdit.value || canDelete.value || !!config.value?.rowActions?.length)

const searchForm = ref<HTMLFormElement | null>(null)
const selected = ref(new Set<string>())
const sortKey = ref('')
const sortDir = ref<'asc' | 'desc'>('asc')

const allSelected = computed(
  () => rows.value.length > 0 && rows.value.every((row) => selected.value.has(String(row.id)))
)
const selectedRows = computed(() => rows.value.filter((row) => selected.value.has(String(row.id))))

const sortedRows = computed(() => {
  if (!sortKey.value) return rows.value
  const col = config.value?.columns.find((c) => c.key === sortKey.value)
  const valueOf = (row: Record<string, unknown>) => (col ? cellText(row, col) : row[sortKey.value])
  const list = [...rows.value]
  list.sort((a, b) => {
    const av = valueOf(a)
    const bv = valueOf(b)
    const an = numericOf(av)
    const bn = numericOf(bv)
    const base =
      an !== null && bn !== null
        ? an - bn
        : String(av ?? '').localeCompare(String(bv ?? ''), 'zh-CN', { numeric: true })
    return sortDir.value === 'asc' ? base : -base
  })
  return list
})

/** 纯数字或日期串按数值比，其余按中文排序 */
function numericOf(v: unknown): number | null {
  if (typeof v === 'number') return Number.isFinite(v) ? v : null
  const s = String(v ?? '').trim()
  if (!s) return null
  if (/^-?\d+(\.\d+)?$/.test(s)) return Number(s)
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) {
    const t = Date.parse(s.replace(' ', 'T'))
    return Number.isNaN(t) ? null : t
  }
  return null
}

function sortIcon(key: string) {
  if (sortKey.value !== key) return ChevronsUpDown
  return sortDir.value === 'asc' ? ArrowUp : ArrowDown
}

function toggleSort(key: string) {
  if (sortKey.value !== key) {
    sortKey.value = key
    sortDir.value = 'asc'
    return
  }
  sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
}

function isSelected(row: Record<string, unknown>) {
  return selected.value.has(String(row.id))
}

function toggleRow(row: Record<string, unknown>) {
  const next = new Set(selected.value)
  const id = String(row.id)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selected.value = next
}

function toggleAll(value: boolean) {
  selected.value = value ? new Set(rows.value.map((row) => String(row.id))) : new Set()
}

function clearSelection() {
  selected.value = new Set()
}

/** 搜索框在表格上方，列表页按 / 直接跳过去，省一次鼠标 */
function onShortcut(event: KeyboardEvent) {
  if (event.key !== '/' || event.metaKey || event.ctrlKey || event.altKey) return
  const target = event.target as HTMLElement | null
  if (target && (['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName) || target.isContentEditable)) return
  const el = searchForm.value?.querySelector<HTMLElement>('input, button')
  if (!el) return
  event.preventDefault()
  el.focus()
}

onMounted(() => window.addEventListener('keydown', onShortcut))
onBeforeUnmount(() => window.removeEventListener('keydown', onShortcut))

// ===== 数据加载 =====
// 换页面和换页签都要重头来：分页、搜索条件是按上一份表格配置建的
watch(configKey, () => init(), { immediate: true })

async function init() {
  if (!config.value) return
  pageNo.value = 1
  resetSearchModel()
  await preloadDicts()
  await load()
}

/** 键全部预置成空串：select 的 v-model 落在 undefined 上会显示成空白框，看不出「已发布（默认）」这档 */
function resetSearchModel() {
  for (const k of Object.keys(search)) delete search[k]
  for (const f of searchFields.value) search[f.key] = ''
}

async function preloadDicts() {
  const types = new Set<string>()
  for (const f of config.value?.search ?? []) if (f.dictType) types.add(f.dictType)
  for (const c of config.value?.columns ?? []) if (c.dictType) types.add(c.dictType)
  for (const f of config.value?.fields ?? []) if (f.dictType) types.add(f.dictType)
  await Promise.all(
    [...types].map(async (t) => {
      try {
        dictCache.set(t, await dictOptions(t))
      } catch {
        // 字典拉取失败不阻塞页面
      }
    })
  )
  const remote = new Set<FieldConfig>()
  for (const list of [config.value?.search ?? [], config.value?.fields ?? []]) {
    for (const f of list) if (f.optionsApi) remote.add(f)
  }
  await Promise.all(
    [...remote].map(async (f) => {
      optionCache.set(optionKey(f), await remoteOptions(f.optionsApi!, f.optionLabel, f.optionValue))
    })
  )
}

async function load() {
  const params: Record<string, unknown> = { pageNo: pageNo.value, pageSize }
  for (const [k, v] of Object.entries(search)) {
    if (v !== '' && v != null) params[k] = v
  }
  loading.value = true
  loadError.value = ''
  try {
    const data = await api.get<{ total: number; list: Record<string, unknown>[] }>(config.value!.api, params)
    rows.value = data?.list ?? []
    total.value = Number(data?.total ?? 0)
    clearSelection()
  } catch (e) {
    loadError.value = errText(e)
  } finally {
    loading.value = false
  }
}

function doSearch() {
  pageNo.value = 1
  load()
}

function resetSearch() {
  resetSearchModel()
  pageNo.value = 1
  load()
}

function goto(p: number) {
  if (p < 1 || p > Math.max(1, Math.ceil(total.value / pageSize)) || p === pageNo.value) return
  pageNo.value = p
  load()
}

// ===== 单元格渲染 =====
function fmtVal(v: unknown): string {
  if (v == null || v === '') return '-'
  const s = String(v)
  if (/^\d{4}-\d{2}-\d{2}T\d{2}/.test(s)) {
    const t = s.slice(0, 16).replace('T', ' ')
    return / 00:00$/.test(t) ? t.slice(0, 10) : t
  }
  return s
}

function pillOf(
  row: Record<string, unknown>,
  c: ColumnConfig
): { text: string; tone: 'ok' | 'warn' | 'bad' | 'info' } | undefined {
  if (!c.statusMap) return undefined
  return c.statusMap[String(row[c.key])]
}

function cellText(row: Record<string, unknown>, c: ColumnConfig): string {
  const v = row[c.key]
  if (c.statusMap) {
    const m = c.statusMap[String(v)]
    if (m) return m.text
  }
  if (c.map) {
    const m = c.map[String(v)]
    if (m !== undefined) return m
  }
  if (c.dictType && v != null && v !== '') {
    const item = dictCache.get(c.dictType)?.find((i) => i.value === String(v))
    if (item) return item.label
  }
  return fmtVal(v)
}

// ===== 选项 =====
function isSelect(f: FieldConfig): boolean {
  return f.searchType === 'select' || (f.type === 'select' && !!(f.options || f.dictType || f.optionsApi))
}

function optionsOf(f: FieldConfig): { label: string; value: string }[] {
  if (f.options) return f.options
  if (f.optionsApi) return optionCache.get(optionKey(f)) ?? []
  if (f.dictType) {
    return (dictCache.get(f.dictType) ?? []).map((i) => ({ label: i.label, value: i.value }))
  }
  return []
}

function actionVariant(tone?: 'pri' | 'dan' | 'gho'): 'default' | 'destructive' | 'outline' {
  if (tone === 'pri') return 'default'
  if (tone === 'dan') return 'destructive'
  return 'outline'
}

// ===== 新增 / 编辑 =====
function openCreate() {
  editing.value = null
  for (const k of Object.keys(form)) delete form[k]
  for (const k of Object.keys(formErr)) delete formErr[k]
  editOpen.value = true
}

async function openEdit(row: Record<string, unknown>) {
  let source = row
  const getApi = config.value?.getApi
  if (getApi) {
    // 列表回包是省字段的（动态/文档的正文只在详情里），照列表回填就等于把正文清空
    try {
      source = { ...row, ...(await api.get<Record<string, unknown>>(getApi, { id: row.id })) }
    } catch (e) {
      toast(errText(e), 'bad')
      return
    }
  }
  editing.value = source
  for (const k of Object.keys(form)) delete form[k]
  for (const k of Object.keys(formErr)) delete formErr[k]
  for (const f of formFields.value) {
    const v = source[f.key]
    if (f.json) form[f.key] = jsonInput(v)
    else if (f.type === 'datetime') form[f.key] = dtInput(v)
    else if (f.type === 'date') form[f.key] = v == null ? '' : String(v).slice(0, 10)
    else if (f.type === 'switch') form[f.key] = Boolean(v)
    else if (isSelect(f) && !optionsOf(f).some((o) => o.value === String(v))) form[f.key] = ''
    else form[f.key] = v ?? ''
  }
  editOpen.value = true
}

/** null / 空值留白，别把 'null' 摊给用户改 */
function jsonInput(v: unknown): string {
  if (v == null) return ''
  if (typeof v === 'object' && (Array.isArray(v) ? v.length === 0 : Object.keys(v).length === 0)) return ''
  return JSON.stringify(v, null, 2)
}

function dtInput(v: unknown): string {
  if (v == null || v === '') return ''
  const s = String(v).replace(' ', 'T')
  return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(s) ? s.slice(0, 16) : ''
}

function toWire(v: unknown, f: FieldConfig): unknown {
  if (f.json) {
    // 留白 = 不传这个键；后端是全量覆盖语义，宁可不传也别传错形状
    if (v === '' || v == null) return undefined
    return JSON.parse(String(v))
  }
  if (f.type === 'number') {
    if (v === '' || v == null) return undefined
    return Number(v)
  }
  if (f.type === 'datetime') {
    if (v === '' || v == null) return undefined
    // 后端 LocalDateTime 走 ISO（yyyy-MM-ddTHH:mm:ss）
    let s = String(v).replace(' ', 'T')
    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(s)) s += ':00'
    return s
  }
  if (f.type === 'date') {
    // 后端日期字段均为 LocalDateTime，补零时提交
    return v === '' || v == null ? undefined : String(v) + 'T00:00:00'
  }
  if (f.type === 'switch') return Boolean(v)
  return v ?? ''
}

async function save() {
  const cfg = config.value!
  for (const k of Object.keys(formErr)) delete formErr[k]
  let ok = true
  for (const f of formFields.value) {
    if (f.required && (form[f.key] === undefined || form[f.key] === null || form[f.key] === '')) {
      formErr[f.key] = '不能为空'
      ok = false
      continue
    }
    if (f.json && form[f.key] !== '' && form[f.key] != null) {
      try {
        JSON.parse(String(form[f.key]))
      } catch {
        formErr[f.key] = '不是合法的 JSON，检查一下括号和逗号'
        ok = false
      }
    }
  }
  if (!ok) return
  const payload: Record<string, unknown> = {}
  for (const f of formFields.value) {
    const w = toWire(form[f.key], f)
    if (w !== undefined) payload[f.key] = w
  }
  if (editing.value) payload.id = editing.value.id
  saving.value = true
  try {
    if (editing.value) await api.put(cfg.updateApi!, payload)
    else await api.post(cfg.createApi!, payload)
    editOpen.value = false
    toast('保存成功', 'ok')
    await load()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    saving.value = false
  }
}

// ===== 行操作 / 页面操作 =====
function visibleRowActions(row: Record<string, unknown>) {
  return (config.value?.rowActions ?? []).filter((a) => !a.when || a.when(row))
}

function runRowAction(a: RowAction, row: Record<string, unknown>) {
  if (a.prompt) {
    promptState.value = { action: a, row }
    promptValue.value = ''
    return
  }
  if (a.confirm) {
    confirmState.value = { text: a.confirm, fn: () => execAction(a, row), danger: a.tone === 'dan' }
    return
  }
  execAction(a, row)
}

function runPageAction(a: PageAction) {
  if (a.prompt) {
    promptState.value = { action: a }
    promptValue.value = ''
    return
  }
  if (a.confirm) {
    confirmState.value = { text: a.confirm, fn: () => execAction(a), danger: a.tone === 'dan' }
    return
  }
  execAction(a)
}

async function confirmPrompt() {
  const st = promptState.value
  if (!st?.action.prompt) return
  const pr = st.action.prompt
  const v = promptValue.value.trim()
  if (pr.required && !v) {
    toast(`请填写${pr.label}`, 'bad')
    return
  }
  await execAction(st.action, st.row, v)
}

async function execAction(a: RowAction | PageAction, row?: Record<string, unknown>, promptVal?: string) {
  busy.value = true
  try {
    if (a.method === 'download') {
      await api.download(a.api, { id: row?.id }, 'export.pdf')
      toast(a.successMsg || '已开始下载', 'ok')
      return
    }
    const body: Record<string, unknown> = row
      ? { ...(a.body ? (a as RowAction).body!(row) : { id: row.id }) }
      : { ...(a.body ? (a as PageAction).body!() : {}) }
    if (a.prompt && promptVal !== undefined && promptVal !== '') {
      body[a.prompt.key] = a.prompt.type === 'number' ? Number(promptVal) : promptVal
    }
    const method = a.method === 'delete' ? 'del' : a.method || 'post'
    await api[method](a.api, body)
    toast(a.successMsg || '操作成功', 'ok')
    promptState.value = null
    await load()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    busy.value = false
  }
}

function askDelete(row: Record<string, unknown>) {
  confirmState.value = { text: '确认删除该记录？删除后不可恢复', fn: () => execDelete(row), danger: true }
}

async function execDelete(row: Record<string, unknown>) {
  busy.value = true
  try {
    await api.del(config.value!.deleteApi!, { id: row.id })
    toast('已删除', 'ok')
    confirmState.value = null
    await load()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    busy.value = false
  }
}

function askBulkDelete() {
  const count = selected.value.size
  if (!count) return
  confirmState.value = {
    text: `确认删除选中的 ${count} 条记录？删除后不可恢复`,
    danger: true,
    fn: execBulkDelete
  }
}

async function execBulkDelete() {
  const apiUrl = config.value?.deleteApi
  if (!apiUrl) return
  const ids = selectedRows.value.map((row) => row.id)
  busy.value = true
  let ok = 0
  let fail = 0
  try {
    for (const id of ids) {
      try {
        await api.del(apiUrl, { id })
        ok++
      } catch {
        fail++
      }
    }
    confirmState.value = null
    clearSelection()
    await load()
    toast(fail ? `已删除 ${ok} 条，${fail} 条失败` : `已删除 ${ok} 条`, fail ? 'bad' : 'ok')
  } finally {
    busy.value = false
  }
}

function confirmYes() {
  const st = confirmState.value
  confirmState.value = null
  st?.fn()
}
</script>
