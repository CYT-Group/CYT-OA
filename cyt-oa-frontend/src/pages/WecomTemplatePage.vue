<template>
  <div>
  <PageHeader
    eyebrow="企业微信"
    title="审批模板"
    description="企微审批模板与 OA 业务别名的绑定关系；新建与更新会真实写入企业微信。"
  >
    <template #actions>
      <template v-if="canManage">
        <Button variant="outline" size="sm" @click="bindOpen = true">
          <Link2 class="size-3.5" />
          绑定已有模板
        </Button>
        <Button size="sm" @click="openCreate">
          <Plus class="size-3.5" />
          新建模板
        </Button>
      </template>
    </template>
  </PageHeader>

  <Callout v-if="err" tone="destructive" title="模板列表加载失败" class="mt-4">
    {{ err }}
    <template #action>
      <Button variant="outline" size="sm" @click="load">重试</Button>
    </template>
  </Callout>

  <section class="panel mt-4 overflow-hidden">
    <div class="panel-header">
      <div class="panel-title">
        已绑定模板
        <Badge variant="secondary" class="tabular-nums">{{ items.length }}</Badge>
      </div>
      <Button variant="ghost" size="icon-sm" aria-label="刷新" :disabled="loading" @click="load">
        <RefreshCw :class="['size-3.5', loading && 'animate-spin']" />
      </Button>
    </div>

    <div v-if="items.length" class="overflow-x-auto" :class="{ 'pointer-events-none opacity-60': loading }">
      <table class="data-table">
        <thead>
          <tr>
            <th>业务别名</th>
            <th>企微模板 ID</th>
            <th class="w-28">字段别名</th>
            <th>描述</th>
            <th class="w-40">更新时间</th>
            <th class="w-28 text-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in items" :key="t.alias">
            <td class="whitespace-nowrap font-medium">{{ t.alias }}</td>
            <td class="whitespace-nowrap">
              <div class="flex items-center gap-1">
                <code
                  class="rounded-md bg-muted px-1.5 py-0.5 font-mono text-[11px] text-muted-foreground"
                  :title="t.template_id"
                >{{ shortId(t.template_id) }}</code>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  :title="`复制完整 ID：${t.template_id}`"
                  :aria-label="`复制 ${t.alias} 的模板 ID`"
                  @click="copyId(t.template_id)"
                >
                  <Copy class="size-3.5" />
                </Button>
              </div>
            </td>
            <td>
              <Button
                variant="link"
                size="sm"
                :title="aliasNames(t) || '暂无字段别名'"
                :disabled="detailLoading"
                @click="openDetail(t)"
              >{{ aliasCount(t) }} 个</Button>
            </td>
            <td class="max-w-72">
              <span class="block truncate text-muted-foreground" :title="t.description || ''">{{ t.description || '—' }}</span>
            </td>
            <td class="whitespace-nowrap text-xs tabular-nums text-muted-foreground">{{ fmtTime(t.updated_at) }}</td>
            <td>
              <div class="flex items-center justify-end gap-1">
                <Button variant="ghost" size="sm" :disabled="detailLoading" @click="openDetail(t)">详情</Button>
                <DropdownMenu v-if="canManage">
                  <template #trigger>
                    <Button variant="ghost" size="icon-sm" :aria-label="`${t.alias} 的更多操作`">
                      <MoreHorizontal class="size-4" />
                    </Button>
                  </template>
                  <DropdownMenuItem @select="openUpdate(t)">
                    <Pencil class="size-3.5 text-muted-foreground" />
                    <span class="flex flex-col">
                      <span>更新模板</span>
                      <small class="text-xs text-muted-foreground">在企微上修改字段</small>
                    </span>
                  </DropdownMenuItem>
                  <DropdownMenuItem @select="openClone(t)">
                    <CopyPlus class="size-3.5 text-muted-foreground" />
                    <span class="flex flex-col">
                      <span>克隆模板</span>
                      <small class="text-xs text-muted-foreground">复制一份新的企微模板</small>
                    </span>
                  </DropdownMenuItem>
                  <DropdownMenuSeparator class="my-1 h-px bg-border" />
                  <DropdownMenuItem destructive @select="openUnbindConfirm(t)">
                    <Unlink class="size-3.5" />
                    <span class="flex flex-col">
                      <span>解绑</span>
                      <small class="text-xs opacity-70">只删本地别名，企微模板不动</small>
                    </span>
                  </DropdownMenuItem>
                </DropdownMenu>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <EmptyState
      v-if="!items.length"
      :icon="FileText"
      :title="loading ? '加载中…' : '还没有绑定模板'"
      :description="loading ? undefined : '企微不提供「列出全部模板」的接口，这里只展示已绑定的模板。可以粘贴一张真实审批单号反查模板，或在企微上新建并自动绑定。'"
    >
      <div v-if="canManage" class="flex items-center gap-2">
        <Button variant="outline" size="sm" @click="bindOpen = true">绑定已有模板</Button>
        <Button size="sm" @click="openCreate">新建模板</Button>
      </div>
    </EmptyState>
  </section>

  <Dialog
    :open="bindOpen"
    title="绑定企微模板"
    description="用历史审批单反查模板 ID，确认控件后再保存业务别名。"
    size="lg"
    @update:open="(v) => { if (!v) closeBind() }"
  >
    <div class="flex flex-col gap-5">
      <div>
        <Label>从历史审批单回填</Label>
        <div class="flex gap-2">
          <Input
            v-model="pullNo"
            class="flex-1"
            placeholder="企微审批单号"
            @keyup.enter="pullFromApproval"
          />
          <Button
            variant="outline"
            :disabled="pullLoading || !pullNo.trim()"
            :loading="pullLoading"
            @click="pullFromApproval"
          >取模板 ID</Button>
        </div>
        <p class="mt-1.5 text-xs text-muted-foreground">
          企微不给模板列表接口：粘贴一张真实审批单的单号即可反查 template_id，单号在企微审批单详情页。
        </p>
        <Callout v-if="pullErr" tone="destructive" class="mt-2">{{ pullErr }}</Callout>
        <Callout v-else-if="pullHit" tone="success" class="mt-2">
          「{{ pullHit.name }}」· 申请人 {{ pullHit.applicant }} · 提交于 {{ fmtSubmit(pullHit.submitted_at) }}
        </Callout>
      </div>

      <div class="grid gap-4 sm:grid-cols-2">
        <div class="flex flex-col">
          <Label required>企微模板 ID</Label>
          <Input v-model="bindForm.templateId" placeholder="如 3WKiKYkcsBp9YS7EFM9PKmh6byp8vyLEmgqC7BLU" />
          <p class="mt-1 text-xs text-muted-foreground">企微管理后台审批模板的 template_id</p>
        </div>
        <div class="flex flex-col">
          <Label required>业务别名</Label>
          <Input v-model="bindForm.alias" placeholder="leave" />
          <p class="mt-1 text-xs text-muted-foreground">OA 提交时用的名字，如 leave</p>
        </div>
        <div class="flex flex-col sm:col-span-2">
          <Label>描述</Label>
          <Input v-model="bindForm.description" placeholder="选填" />
        </div>
      </div>

      <div v-if="previewLoading" class="text-sm text-muted-foreground">拉取模板中…</div>
      <Callout v-else-if="previewErr" tone="destructive" title="模板预览拉取失败">{{ previewErr }}</Callout>
      <div v-else-if="preview" class="overflow-hidden rounded-xl border">
        <div class="flex items-center justify-between border-b bg-muted/40 px-3 py-2 text-xs">
          <span class="font-medium">{{ preview.name }}</span>
          <span class="text-muted-foreground">{{ preview.fields.length }} 个控件</span>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>控件</th>
              <th class="w-32">类型</th>
              <th class="w-20">必填</th>
              <th class="w-56">业务别名（选填）</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="f in preview.fields" :key="f.id">
              <td>
                <div class="font-medium">{{ f.label }}</div>
                <div class="font-mono text-[11px] text-muted-foreground">{{ f.id }}</div>
              </td>
              <td class="text-muted-foreground">{{ controlLabel(f.control) }}</td>
              <td>
                <Badge :variant="f.required ? 'warning' : 'info'">{{ f.required ? '必填' : '选填' }}</Badge>
              </td>
              <td>
                <Input v-model="bindAliases[f.id]" :placeholder="suggestAlias(f)" class="h-8" />
              </td>
            </tr>
          </tbody>
        </table>
        <p class="border-t bg-muted/20 px-3 py-2 text-xs text-muted-foreground">
          业务别名是 OA 提单时字段的键：请假流程用 vacation（请假时长控件）和 reason（事由控件）。别名留空则该控件不参与别名映射。
        </p>
      </div>
    </div>
    <template #footer>
      <Button variant="outline" @click="closeBind">取消</Button>
      <Button
        variant="outline"
        :disabled="previewLoading || !bindForm.templateId"
        :loading="previewLoading"
        @click="fetchPreview"
      >拉取模板预览</Button>
      <Button
        :disabled="!preview || !bindForm.alias"
        :loading="binding"
        @click="doBind"
      >绑定</Button>
    </template>
  </Dialog>

  <Dialog
    :open="designer.open"
    :title="designer.mode === 'create' ? '新建企微审批模板' : `更新模板 · ${designer.template}`"
    description="字段会按顺序写入企微模板；更新只改字段定义，不删除已有审批单。"
    size="xl"
    @update:open="designer.open = $event"
  >
    <div class="grid gap-4 sm:grid-cols-2">
      <div class="flex flex-col">
        <Label required>模板名称</Label>
        <Input v-model="designer.name" placeholder="如 请假申请" />
      </div>
      <div v-if="designer.mode === 'create'" class="flex flex-col">
        <Label required>业务别名</Label>
        <Input v-model="designer.alias" placeholder="leave" />
      </div>
      <div class="flex flex-col sm:col-span-2">
        <Label>描述</Label>
        <Input v-model="designer.description" placeholder="选填" />
      </div>
    </div>

    <div class="mt-5 flex items-center justify-between">
      <h3 class="text-sm font-semibold">字段</h3>
      <span class="text-xs text-muted-foreground">共 {{ designer.rows.length }} 个</span>
    </div>
    <div class="mt-2 flex flex-col gap-2.5">
      <div v-for="(row, i) in designer.rows" :key="i" class="rounded-xl border p-3">
        <div class="grid gap-2 sm:grid-cols-[minmax(0,1.3fr)_minmax(0,1fr)_10rem_auto]">
          <Input v-model="row.label" placeholder="字段名，如 请假事由" />
          <Input v-model="row.key" placeholder="键，如 reason" />
          <Select v-model="row.control" :options="CONTROL_TYPES" />
          <div class="flex items-center justify-end gap-3">
            <Checkbox v-model="row.required">必填</Checkbox>
            <Button
              variant="link"
              size="sm"
              class="text-destructive hover:text-destructive/80"
              @click="designer.rows.splice(i, 1)"
            >移除</Button>
          </div>
        </div>
        <div v-if="row.control === 'Selector'" class="mt-2 grid gap-2 sm:grid-cols-[1fr_auto]">
          <Textarea v-model="row.optionsText" :rows="3" placeholder="候选项，一行一个" />
          <Checkbox v-model="row.multiple" class="self-start pt-1">多选</Checkbox>
        </div>
      </div>
      <Button variant="outline" size="sm" class="self-start" @click="addRow">
        <Plus class="size-3.5" />
        加字段
      </Button>
    </div>

    <template #footer>
      <Button variant="outline" @click="designer.open = false">取消</Button>
      <Button :loading="saving" @click="saveDesigner">
        {{ designer.mode === 'create' ? '创建（企微上真实建模板）' : '更新（企微上真实改模板）' }}
      </Button>
    </template>
  </Dialog>

  <Dialog
    :open="cloneOpen"
    title="克隆模板"
    description="复制源模板的字段定义，在企微上生成一份新模板。"
    size="sm"
    @update:open="cloneOpen = $event"
  >
    <div class="flex flex-col gap-4">
      <div class="flex flex-col">
        <Label required>源模板</Label>
        <Select
          v-model="cloneForm.template"
          :options="items.map((t) => ({ label: t.alias, value: t.alias }))"
          placeholder="选择已绑定的模板"
        />
      </div>
      <div class="flex flex-col">
        <Label required>新模板名称</Label>
        <Input v-model="cloneForm.name" placeholder="如 请假申请（副本）" />
      </div>
      <div class="flex flex-col">
        <Label>新别名（选填）</Label>
        <Input v-model="cloneForm.alias" placeholder="leaves" />
      </div>
      <div class="flex flex-col">
        <Label>描述（选填）</Label>
        <Input v-model="cloneForm.description" />
      </div>
    </div>
    <template #footer>
      <Button variant="outline" @click="cloneOpen = false">取消</Button>
      <Button :loading="cloning" @click="doClone">克隆</Button>
    </template>
  </Dialog>

  <Dialog
    :open="!!detail"
    :title="`模板详情 · ${detail?.template ?? ''}`"
    size="lg"
    @update:open="(v) => { if (!v) detail = null }"
  >
    <template v-if="detail">
      <div class="grid gap-x-6 gap-y-3 sm:grid-cols-2">
        <div>
          <p class="text-xs text-muted-foreground">模板名</p>
          <p class="mt-0.5 text-sm font-medium">{{ detail.name }}</p>
        </div>
        <div>
          <p class="text-xs text-muted-foreground">版本指纹</p>
          <p class="mt-0.5 break-all font-mono text-xs">{{ detail.version }}</p>
        </div>
        <div class="sm:col-span-2">
          <p class="text-xs text-muted-foreground">企微模板 ID</p>
          <div class="mt-0.5 flex items-center gap-1">
            <p class="break-all font-mono text-xs">{{ detail.template_id }}</p>
            <Button variant="ghost" size="icon-sm" aria-label="复制模板 ID" @click="copyId(detail.template_id)">
              <Copy class="size-3.5" />
            </Button>
          </div>
        </div>
      </div>

      <div class="mt-4 overflow-hidden rounded-xl border">
        <table class="data-table">
          <thead>
            <tr>
              <th>控件</th>
              <th class="w-28">类型</th>
              <th class="w-20">必填</th>
              <th class="w-28">业务别名</th>
              <th>候选项</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="f in detail.fields" :key="f.id">
              <td>
                <div class="font-medium">{{ f.label }}</div>
                <div class="font-mono text-[11px] text-muted-foreground">{{ f.id }}</div>
              </td>
              <td>
                <div class="flex items-center gap-1">
                  {{ controlLabel(f.control) }}
                  <Badge v-if="f.read_only" variant="info">只读</Badge>
                </div>
              </td>
              <td>
                <Badge :variant="f.required ? 'warning' : 'info'">{{ f.required ? '必填' : '选填' }}</Badge>
              </td>
              <td>{{ f.key && f.key !== f.id && f.key !== f.label ? f.key : '—' }}</td>
              <td class="max-w-56">
                <span class="block truncate text-muted-foreground" :title="optionText(f)">{{ optionText(f) || '—' }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
    <template #footer>
      <Button variant="outline" @click="detail = null">关闭</Button>
    </template>
  </Dialog>

  <ConfirmDialog
    :open="confirmOpen"
    :title="`解绑「${unbindTarget?.alias ?? ''}」？`"
    description="只删除本地的业务别名与字段映射，企微上的审批模板不会被改动。之后仍可重新绑定。"
    confirm-text="解绑"
    tone="destructive"
    :busy="unbinding"
    @update:open="confirmOpen = $event"
    @confirm="doUnbind"
  />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Copy, CopyPlus, FileText, Link2, MoreHorizontal, Pencil, Plus, RefreshCw, Unlink } from '@lucide/vue'
import { DropdownMenuSeparator } from 'reka-ui'
import { api } from '../lib/api'
import { errText, toast } from '../lib/toast'
import { session } from '../lib/session'
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
import Select from '../ui/Select.vue'
import Textarea from '../ui/Textarea.vue'

const CONTROL_TYPES = [
  { value: 'Text', label: '单行文本' },
  { value: 'Textarea', label: '多行文本' },
  { value: 'Number', label: '数字' },
  { value: 'Money', label: '金额' },
  { value: 'Date', label: '日期' },
  { value: 'DateRange', label: '日期区间' },
  { value: 'Selector', label: '单选/多选' },
  { value: 'Contact', label: '成员' },
  { value: 'Vacation', label: '请假时长' },
  { value: 'Location', label: '位置' },
  { value: 'PhoneNumber', label: '手机号' },
  { value: 'Tips', label: '说明文字' },
  { value: 'File', label: '附件' }
]

interface TplItem {
  alias: string
  template_id: string
  field_aliases?: string | Record<string, string>
  description?: string
  updated_at?: string
}

interface TplField {
  key: string
  id: string
  label: string
  control: string
  required: boolean
  read_only?: boolean
  options?: { key: string; label: string }[]
}

interface TplDetail {
  template: string
  template_id: string
  name: string
  version: string
  fields: TplField[]
}

interface DesignerRow {
  key: string
  label: string
  control: string
  required: boolean
  multiple: boolean
  optionsText: string
  id: string
}

/** 按审批单反查到的模板归属，网关的 snake_case 口径。 */
interface ApprovalHit {
  approval_no: string
  template_id: string
  name: string
  applicant: string
  submitted_at: string
}

const canManage = computed(() => session.has('system:wecom-template:manage'))

const items = ref<TplItem[]>([])
const loading = ref(false)
const err = ref('')

async function load() {
  loading.value = true
  err.value = ''
  try {
    const data = await api.get<{ items: TplItem[] }>('/admin-api/system/wecom-templates')
    items.value = data.items ?? []
  } catch (e) {
    err.value = errText(e)
  } finally {
    loading.value = false
  }
}

function aliasCount(t: TplItem): number {
  if (!t.field_aliases) return 0
  if (typeof t.field_aliases === 'string') {
    try {
      return Object.keys(JSON.parse(t.field_aliases) || {}).length
    } catch {
      return 0
    }
  }
  return Object.keys(t.field_aliases).length
}

function controlLabel(v: string): string {
  return CONTROL_TYPES.find((c) => c.value === v)?.label ?? v
}

function fmtTime(v?: string): string {
  if (!v) return '—'
  return v.replace('T', ' ').slice(0, 16)
}

/** 长 ID 只露首尾，完整值放到 title 和复制按钮里（GitHub 展示 SHA 的做法）。 */
function shortId(v: string): string {
  return v.length > 20 ? `${v.slice(0, 10)}…${v.slice(-4)}` : v
}

async function copyId(v: string) {
  try {
    await navigator.clipboard.writeText(v)
    toast('已复制模板 ID')
  } catch {
    toast('复制失败，请从详情里手动选择', 'bad')
  }
}

function aliasNames(t: TplItem): string {
  let value = t.field_aliases
  if (typeof value === 'string') {
    try { value = JSON.parse(value) } catch { return '' }
  }
  return value && typeof value === 'object' ? Object.keys(value).join('、') : ''
}

// ===== 绑定 =====
const bindOpen = ref(false)
const bindForm = reactive({ templateId: '', alias: '', description: '' })
const bindAliases = reactive<Record<string, string>>({})
const preview = ref<TplDetail | null>(null)
const previewLoading = ref(false)
const previewErr = ref('')
const binding = ref(false)

function suggestAlias(f: TplField): string {
  if (f.control === 'Vacation') return 'vacation'
  if ((f.control === 'Text' || f.control === 'Textarea') && /事由|原因|说明|备注/.test(f.label)) return 'reason'
  return ''
}

function closeBind() {
  bindOpen.value = false
  preview.value = null
  previewErr.value = ''
  pullNo.value = ''
  pullHit.value = null
  pullErr.value = ''
  for (const k of Object.keys(bindAliases)) delete bindAliases[k]
  bindForm.templateId = ''
  bindForm.alias = ''
  bindForm.description = ''
}

async function fetchPreview() {
  previewLoading.value = true
  previewErr.value = ''
  preview.value = null
  for (const k of Object.keys(bindAliases)) delete bindAliases[k]
  try {
    const data = await api.get<TplDetail>('/admin-api/system/wecom-templates/get', {
      template: bindForm.templateId.trim()
    })
    preview.value = data
    for (const f of data.fields ?? []) {
      const s = suggestAlias(f)
      if (s) bindAliases[f.id] = s
    }
  } catch (e) {
    previewErr.value = errText(e)
  } finally {
    previewLoading.value = false
  }
}

// ===== 从历史审批单反查模板 ID =====
const pullNo = ref('')
const pullHit = ref<ApprovalHit | null>(null)
const pullLoading = ref(false)
const pullErr = ref('')

/** 单号拿到 template_id 后顺手拉预览，绑定要用的控件信息一步到位。 */
async function pullFromApproval() {
  const no = pullNo.value.trim()
  if (!no) return
  pullLoading.value = true
  pullErr.value = ''
  pullHit.value = null
  try {
    const data = await api.get<ApprovalHit>('/admin-api/system/wecom-templates/from-approval', {
      approvalNo: no
    })
    pullHit.value = data
    bindForm.templateId = data.template_id
    await fetchPreview()
  } catch (e) {
    pullErr.value = errText(e)
  } finally {
    pullLoading.value = false
  }
}

/** 企微的 apply_time 是秒级时间戳，网关原样传上来。 */
function fmtSubmit(v?: string): string {
  const ms = Number(v) * 1000
  return ms > 0 ? new Date(ms).toLocaleString('zh-CN', { hour12: false }) : '—'
}

async function doBind() {
  binding.value = true
  try {
    const aliases: Record<string, string> = {}
    for (const f of preview.value?.fields ?? []) {
      const v = bindAliases[f.id]?.trim()
      if (v) aliases[v] = f.id
    }
    await api.post('/admin-api/system/wecom-templates/bind', {
      templateId: bindForm.templateId.trim(),
      alias: bindForm.alias.trim(),
      fieldAliases: aliases,
      description: bindForm.description.trim()
    })
    toast('绑定成功')
    bindOpen.value = false
    await load()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    binding.value = false
  }
}

// ===== 设计器（新建/更新） =====
const designer = reactive<{
  open: boolean
  mode: 'create' | 'update'
  template: string
  name: string
  alias: string
  description: string
  rows: DesignerRow[]
}>({
  open: false,
  mode: 'create',
  template: '',
  name: '',
  alias: '',
  description: '',
  rows: []
})
const saving = ref(false)

function blankRow(): DesignerRow {
  return { key: '', label: '', control: 'Text', required: false, multiple: false, optionsText: '', id: '' }
}

function addRow() {
  designer.rows.push(blankRow())
}

function openCreate() {
  designer.mode = 'create'
  designer.template = ''
  designer.name = ''
  designer.alias = ''
  designer.description = ''
  designer.rows = [blankRow()]
  designer.open = true
}

async function openUpdate(t: TplItem) {
  try {
    const data = await api.get<TplDetail>('/admin-api/system/wecom-templates/get', { template: t.alias })
    designer.mode = 'update'
    designer.template = t.alias
    designer.name = data.name
    designer.alias = ''
    designer.description = ''
    designer.rows = (data.fields ?? []).map((f) => ({
      key: f.key && f.key !== f.id ? f.key : '',
      label: f.label,
      control: f.control,
      required: f.required,
      multiple: false,
      optionsText: (f.options ?? []).map((o) => o.label).join('\n'),
      id: f.id
    }))
    designer.open = true
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

function validateDesigner(): string {
  if (!designer.name.trim()) return '模板名称不能为空'
  if (designer.mode === 'create' && !designer.alias.trim()) return '业务别名不能为空'
  if (!designer.rows.length) return '至少一个字段'
  for (const [i, row] of designer.rows.entries()) {
    if (!/^[a-zA-Z][a-zA-Z0-9_-]{0,63}$/.test(row.key)) return `第 ${i + 1} 个字段的键不合法（字母开头，只含字母数字下划线连字符）`
    if (!row.label.trim()) return `第 ${i + 1} 个字段名不能为空`
    if (row.control === 'Selector') {
      const options = row.optionsText.split('\n').map((s) => s.trim()).filter(Boolean)
      if (!options.length) return `第 ${i + 1} 个字段的候选项不能为空`
    }
  }
  return ''
}

async function saveDesigner() {
  const problem = validateDesigner()
  if (problem) {
    toast(problem, 'bad')
    return
  }
  saving.value = true
  try {
    const fields = designer.rows.map((row) => ({
      key: row.key.trim(),
      label: row.label.trim(),
      control: row.control,
      required: row.required,
      multiple: row.control === 'Selector' ? row.multiple : undefined,
      id: row.id || undefined,
      options:
        row.control === 'Selector'
          ? row.optionsText.split('\n').map((s) => s.trim()).filter(Boolean)
          : undefined
    }))
    if (designer.mode === 'create') {
      await api.post('/admin-api/system/wecom-templates/create', {
        name: designer.name.trim(),
        alias: designer.alias.trim(),
        description: designer.description.trim(),
        fields
      })
      toast('模板已创建并绑定')
    } else {
      await api.post('/admin-api/system/wecom-templates/update', {
        template: designer.template,
        name: designer.name.trim(),
        description: designer.description.trim(),
        fields
      })
      toast('模板已更新')
    }
    designer.open = false
    await load()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    saving.value = false
  }
}

// ===== 克隆 =====
const cloneOpen = ref(false)
const cloneForm = reactive({ template: '', name: '', alias: '', description: '' })
const cloning = ref(false)

function openClone(t: TplItem) {
  cloneForm.template = t.alias
  cloneForm.name = ''
  cloneForm.alias = ''
  cloneForm.description = ''
  cloneOpen.value = true
}

async function doClone() {
  if (!cloneForm.template || !cloneForm.name.trim()) {
    toast('源模板和新模板名称不能为空', 'bad')
    return
  }
  cloning.value = true
  try {
    await api.post('/admin-api/system/wecom-templates/clone', {
      template: cloneForm.template,
      name: cloneForm.name.trim(),
      alias: cloneForm.alias.trim(),
      description: cloneForm.description.trim()
    })
    toast('克隆成功')
    cloneOpen.value = false
    await load()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    cloning.value = false
  }
}

// ===== 解绑：菜单里点一下只是打开确认框，不会直接删 =====
const confirmOpen = ref(false)
const unbindTarget = ref<TplItem | null>(null)
const unbinding = ref(false)

function openUnbindConfirm(t: TplItem) {
  unbindTarget.value = t
  confirmOpen.value = true
}

async function doUnbind() {
  const target = unbindTarget.value
  if (!target || unbinding.value) return
  unbinding.value = true
  try {
    await api.post('/admin-api/system/wecom-templates/unbind', { alias: target.alias })
    toast('已解绑')
    confirmOpen.value = false
    unbindTarget.value = null
    await load()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    unbinding.value = false
  }
}

// ===== 详情 =====
const detail = ref<TplDetail | null>(null)
const detailLoading = ref(false)

async function openDetail(t: TplItem) {
  detailLoading.value = true
  try {
    detail.value = await api.get<TplDetail>('/admin-api/system/wecom-templates/get', {
      template: t.alias
    })
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    detailLoading.value = false
  }
}

function optionText(f: TplField): string {
  return (f.options ?? []).map((o) => o.label).join(' / ')
}

onMounted(() => {
  void load()
})
</script>
