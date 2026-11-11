<template>
  <section class="panel overflow-hidden">
    <div class="table-caption flex-wrap gap-3">
      <div>
        <h2 class="panel-title">{{ title }}</h2>
        <p class="mt-1 text-xs text-muted-foreground">归档前需移走该分类的业务引用；归档后可恢复，业务记录不会删除。</p>
      </div>
      <div class="flex shrink-0 gap-2">
        <Button variant="outline" size="sm" :disabled="loading || busy" @click="load"><RefreshCw class="size-3.5" />刷新</Button>
        <Button v-if="canManage" size="sm" :disabled="busy || loading" @click="openCreate"><Plus class="size-3.5" />新建分类</Button>
      </div>
    </div>
    <form class="table-toolbar" @submit.prevent>
      <Input v-model="query" class="h-9 w-full text-[13px] sm:w-60" placeholder="搜索分类名称或标识" aria-label="搜索分类" />
      <Select v-model="stateFilter" size="sm" class="w-36" aria-label="分类状态" :options="[{ value: 'active', label: '在用分类' }, { value: 'archived', label: '已归档' }, { value: 'all', label: '全部状态' }]" />
      <span class="ml-auto text-xs text-muted-foreground">{{ displayedRows.length }} 个分类</span>
    </form>
    <Callout v-if="error" title="分类加载失败" tone="destructive" class="m-5">{{ error }}<template #action><Button variant="ghost" size="sm" @click="load">重试</Button></template></Callout>
    <div v-else-if="loading" class="space-y-2 border-t p-4"><Skeleton v-for="i in 4" :key="i" class="h-12 w-full" /></div>
    <EmptyState v-else-if="!displayedRows.length" :title="rows.length ? '没有匹配的分类' : '还没有分类'" :description="rows.length ? '调整关键词或切换分类状态。' : '建立分类后，可在对应业务中选择使用。'"><Button v-if="rows.length" variant="outline" size="sm" @click="query = ''; stateFilter = 'all'">显示全部分类</Button><Button v-else-if="canManage" size="sm" @click="openCreate"><Plus />新建分类</Button></EmptyState>
    <template v-else>
      <div class="max-h-[calc(100dvh-22rem)] overflow-auto border-t border-border/70">
        <table class="data-table min-w-[680px]">
          <thead><tr><th>名称</th><th v-if="twoLevel">层级 / 所属大类</th><th class="num">引用数</th><th>状态</th><th class="sticky-col text-right">操作</th></tr></thead>
          <tbody>
            <tr v-for="row in displayedRows" :key="row.id" :class="row.archivedAt ? 'opacity-60' : ''">
              <td class="max-w-80">
                <span class="inline-block max-w-64 truncate align-middle" :title="row.name" :class="row.depth === 2 ? 'pl-4' : 'font-medium'">{{ row.attrs.icon || '' }} {{ row.name }}</span>
                <span v-if="row.code" class="ml-2 text-xs text-muted-foreground">{{ row.code }}</span>
                <p v-if="row.attrs.description" class="mt-1 max-w-80 truncate text-xs text-muted-foreground">{{ row.attrs.description }}</p>
                <Badge v-if="row.attrs.employee_hidden" variant="outline">员工端隐藏</Badge>
              </td>
              <td v-if="twoLevel" class="text-xs text-muted-foreground">{{ row.depth === 1 ? '大类' : (parentName(row) || '未归类小类') }}</td>
              <td class="num">{{ row.depth === 1 && twoLevel ? `${row.childrenCount ?? 0} 个小类` : `${row.refCount ?? 0} 条` }}</td>
              <td><Badge :variant="row.archivedAt ? 'secondary' : 'success'">{{ row.archivedAt ? '已归档' : '在用' }}</Badge></td>
              <td class="sticky-col">
                <div v-if="canManage" class="flex justify-end gap-1 whitespace-nowrap">
                  <Button v-if="!row.archivedAt" variant="ghost" size="sm" :disabled="busy" @click="openEdit(row)">编辑</Button>
                  <Button v-else variant="ghost" size="sm" :disabled="busy" @click="restore(row)">恢复</Button>
                  <DropdownMenu><template #trigger><Button variant="ghost" size="icon-sm" :disabled="busy" :aria-label="`${row.name}的更多操作`"><MoreHorizontal /></Button></template>
                    <template v-if="!row.archivedAt"><DropdownMenuItem :disabled="busy || !canMove(row, -1)" @select="move(row, -1)"><ArrowUp class="size-3.5" />上移</DropdownMenuItem><DropdownMenuItem :disabled="busy || !canMove(row, 1)" @select="move(row, 1)"><ArrowDown class="size-3.5" />下移</DropdownMenuItem><DropdownMenuItem :disabled="busy || !!row.refCount" @select="ask(row, 'archive')">{{ row.refCount ? '归档（先移走业务引用）' : '归档分类' }}</DropdownMenuItem></template>
                    <DropdownMenuItem destructive :disabled="busy || !!row.refCount || !!row.childrenCount" @select="ask(row, 'delete')">{{ row.refCount || row.childrenCount ? '删除（仍有引用或子类）' : '永久删除分类' }}</DropdownMenuItem>
                  </DropdownMenu>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
    <Dialog :open="editorOpen" :title="form.id ? '编辑分类' : '新建分类'" @update:open="editorOpen = $event">
      <form id="portal-category-form" class="space-y-4" @submit.prevent="save">
        <Callout v-if="saveError" tone="destructive">{{ saveError }}</Callout>
        <div><label :for="`${prefix}-name`" class="field-label">分类名称 *</label><Input :id="`${prefix}-name`" v-model="form.name" required :maxlength="twoLevel ? 100 : 32" /></div>
        <div v-if="twoLevel && !form.id"><label :for="`${prefix}-parent-new`" class="field-label">分类层级</label><Select :id="`${prefix}-parent-new`" v-model="form.parentId" :options="parentOptions" /></div>
        <div v-else-if="twoLevel && form.depth === 2"><label :for="`${prefix}-parent-edit`" class="field-label">所属大类</label><Select :id="`${prefix}-parent-edit`" v-model="form.parentId" :options="[{ value: null, label: '未归类' }, ...groups.map(g => ({ value: g.id, label: g.name }))]" /></div>
        <div v-if="!twoLevel || isGroupForm"><label :for="`${prefix}-code`" class="field-label">{{ twoLevel ? '大类标识 *' : '分类标识' }}</label><Input :id="`${prefix}-code`" v-model="form.code" :required="twoLevel" :readonly="!!form.id && !!form.originalCode" :pattern="twoLevel ? '[a-z][a-z0-9_\-]{1,31}' : undefined" maxlength="32" :placeholder="twoLevel ? '例如 amazon，建立后不能修改' : '可留空'" /></div>
        <template v-if="isGroupForm">
          <div><label :for="`${prefix}-icon`" class="field-label">图标</label><Input :id="`${prefix}-icon`" v-model="form.icon" maxlength="16" placeholder="例如 📚" /></div>
          <div><label :for="`${prefix}-description`" class="field-label">简介</label><Textarea :id="`${prefix}-description`" v-model="form.description" maxlength="200" :rows="3" /></div>
        </template>
        <Checkbox v-if="scope === 'material.office'" v-model="form.employeeHidden">在员工领用目录中隐藏</Checkbox>
        <div><label :for="`${prefix}-sort`" class="field-label">排序</label><Input :id="`${prefix}-sort`" v-model="form.sortOrder" type="number" min="0" max="1000000000" /></div>
      </form>
      <template #footer><Button variant="outline" :disabled="busy" @click="editorOpen = false">取消</Button><Button type="submit" form="portal-category-form" :loading="busy">保存</Button></template>
    </Dialog>
    <ConfirmDialog :open="!!pending" :title="pending?.action === 'delete' ? '删除分类' : '归档分类'" :description="confirmText" :tone="pending?.action === 'delete' ? 'destructive' : 'default'" :busy="busy" @update:open="!$event && (pending = null)" @confirm="confirm" />
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ArrowDown, ArrowUp, MoreHorizontal, Plus, RefreshCw } from '@lucide/vue'
import { categoryApi, type PortalCategory } from '../../lib/portal-training'
import { session } from '../../lib/session'
import { errText, toast } from '../../lib/toast'
import Badge from '../../ui/Badge.vue'
import Button from '../../ui/Button.vue'
import Callout from '../../ui/Callout.vue'
import Checkbox from '../../ui/Checkbox.vue'
import ConfirmDialog from '../../ui/ConfirmDialog.vue'
import Dialog from '../../ui/Dialog.vue'
import EmptyState from '../../ui/EmptyState.vue'
import DropdownMenu from '../../ui/DropdownMenu.vue'
import DropdownMenuItem from '../../ui/DropdownMenuItem.vue'
import Skeleton from '../../ui/Skeleton.vue'
import Input from '../../ui/Input.vue'
import Select from '../../ui/Select.vue'
import Textarea from '../../ui/Textarea.vue'

const props = withDefaults(defineProps<{ scope: string; title?: string }>(), { title: '分类管理' })
const emit = defineEmits<{ changed: [] }>()
const rows = ref<PortalCategory[]>([])
const query = ref(''), stateFilter = ref('active')
const loading = ref(false), busy = ref(false), error = ref(''), saveError = ref(''), editorOpen = ref(false)
const pending = ref<{ row: PortalCategory; action: 'archive' | 'delete' } | null>(null)
let request = 0
const prefix = computed(() => `category-${props.scope.replace('.', '-')}`)
const twoLevel = computed(() => props.scope === 'training.course')
const canManage = computed(() => session.has(twoLevel.value ? 'can_manage_training' : 'can_manage_materials'))
const groups = computed(() => rows.value.filter(r => r.depth === 1 && !r.archivedAt))
const form = reactive({ id: null as number | null, name: '', code: '', originalCode: '', parentId: null as number | null, depth: 1, icon: '', description: '', employeeHidden: false, sortOrder: '0', attrs: {} as Record<string, unknown> })
const parentOptions = computed(() => [{ value: null, label: '大类' }, ...groups.value.map(g => ({ value: g.id, label: `小类，归入「${g.name}」` }))])
const isGroupForm = computed(() => twoLevel.value && (form.id ? form.depth === 1 : form.parentId === null))
const compare = (a: PortalCategory, b: PortalCategory) => a.sortOrder - b.sortOrder || a.id - b.id
const sortedRows = computed(() => {
  const active = rows.value.filter(r => !r.archivedAt)
  if (!twoLevel.value) return [...active.sort(compare), ...rows.value.filter(r => r.archivedAt).sort(compare)]
  const out: PortalCategory[] = []
  for (const parent of active.filter(r => r.depth === 1).sort(compare)) {
    out.push(parent, ...active.filter(r => r.parentId === parent.id).sort(compare))
  }
  const seen = new Set(out.map(r => r.id))
  return [...out, ...active.filter(r => !seen.has(r.id)).sort(compare), ...rows.value.filter(r => r.archivedAt).sort(compare)]
})
const displayedRows = computed(() => sortedRows.value.filter(row => {
  if (stateFilter.value === 'active' && row.archivedAt) return false
  if (stateFilter.value === 'archived' && !row.archivedAt) return false
  const keyword = query.value.trim().toLowerCase()
  return !keyword || `${row.name} ${row.code ?? ''} ${parentName(row) ?? ''}`.toLowerCase().includes(keyword)
}))
const confirmText = computed(() => {
  if (!pending.value) return ''
  const { row, action } = pending.value
  if (action === 'delete') return `永久删除「${row.name}」？删除后无法恢复。`
  return `归档「${row.name}」？已有记录会保留。${row.depth === 1 && twoLevel.value ? '其下的小类将移入未归类。' : ''}`
})
function parentName(row: PortalCategory) { return rows.value.find(r => r.id === row.parentId)?.name }
function siblings(row: PortalCategory) { return rows.value.filter(r => !r.archivedAt && r.depth === row.depth && r.parentId === row.parentId).sort(compare) }
function canMove(row: PortalCategory, delta: number) { const all = siblings(row); const i = all.findIndex(r => r.id === row.id) + delta; return i >= 0 && i < all.length }
async function load() {
  const seq = ++request
  loading.value = true; error.value = ''
  try { const data = await categoryApi.adminList(props.scope); if (seq === request) rows.value = data }
  catch (e) { if (seq === request) { error.value = errText(e); rows.value = [] } }
  finally { if (seq === request) loading.value = false }
}
function openCreate() {
  Object.assign(form, { id: null, name: '', code: '', originalCode: '', parentId: null, depth: 1, icon: '', description: '', employeeHidden: false, sortOrder: String(Math.max(0, ...rows.value.map(r => r.sortOrder)) + 1), attrs: {} })
  saveError.value = ''; editorOpen.value = true
}
function openEdit(row: PortalCategory) {
  Object.assign(form, { id: row.id, name: row.name, code: row.code ?? '', originalCode: row.code ?? '', parentId: row.parentId, depth: row.depth, icon: String(row.attrs.icon ?? ''), description: String(row.attrs.description ?? ''), employeeHidden: !!row.attrs.employee_hidden, sortOrder: String(row.sortOrder), attrs: { ...row.attrs } })
  saveError.value = ''; editorOpen.value = true
}
async function save() {
  if (busy.value) return
  busy.value = true; saveError.value = ''
  try {
    const body: Record<string, unknown> = { name: form.name.trim(), sortOrder: Number(form.sortOrder), attrs: { ...form.attrs } }
    if (!body.name) throw new Error('请填写分类名称')
    if (isGroupForm.value) body.attrs = { icon: form.icon.trim(), description: form.description.trim() }
    if (props.scope === 'material.office') body.attrs = { ...form.attrs, employee_hidden: form.employeeHidden }
    if (!form.id) { body.scope = props.scope; if (twoLevel.value) body.parentId = form.parentId; if (form.code.trim()) body.code = form.code.trim() }
    else if (twoLevel.value && form.depth === 2) body.parentId = form.parentId
    else if (!form.originalCode && form.code.trim()) body.code = form.code.trim()
    await categoryApi.save(form.id, body)
    editorOpen.value = false; toast('分类已保存'); await load(); emit('changed')
  } catch (e) { saveError.value = e instanceof Error ? e.message : errText(e) }
  finally { busy.value = false }
}
function ask(row: PortalCategory, action: 'archive' | 'delete') { pending.value = { row, action } }
async function confirm() {
  if (!pending.value || busy.value) return
  busy.value = true
  try {
    const { row, action } = pending.value
    if (action === 'delete') await categoryApi.delete(row.id)
    else await categoryApi.state(row.id, 'archive')
    pending.value = null; toast(action === 'delete' ? '分类已删除' : '分类已归档'); await load(); emit('changed')
  } catch (e) { toast(errText(e), 'bad') }
  finally { busy.value = false }
}
async function restore(row: PortalCategory) {
  busy.value = true
  try { await categoryApi.state(row.id, 'restore'); toast('分类已恢复'); await load(); emit('changed') }
  catch (e) { toast(errText(e), 'bad') }
  finally { busy.value = false }
}
async function move(row: PortalCategory, delta: number) {
  if (busy.value || !canMove(row, delta)) return
  const all = siblings(row); const i = all.findIndex(r => r.id === row.id); [all[i], all[i + delta]] = [all[i + delta], all[i]]
  busy.value = true
  try { await categoryApi.reorder(props.scope, row.parentId, all.map(r => r.id)); await load(); emit('changed') }
  catch (e) { toast(errText(e), 'bad') }
  finally { busy.value = false }
}
watch(() => props.scope, () => { editorOpen.value = false; pending.value = null; rows.value = []; void load() }, { immediate: true })
</script>
