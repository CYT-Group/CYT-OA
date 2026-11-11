<template>
  <div class="min-w-0 space-y-4">
    <PageHeader title="品牌手册库" description="上传和维护品牌 HTML 手册；替换会保留历史版本，可随时回滚。"><template #actions><Button size="sm" @click="openUpload()"><Upload />上传手册</Button></template></PageHeader>
    <section class="panel overflow-hidden">
      <form class="table-toolbar flex-wrap gap-2" @submit.prevent="search">
        <Input v-model="filter.q" class="h-9 w-full text-[13px] sm:w-56" aria-label="手册关键词" placeholder="搜索手册标题" />
        <Select v-model="filter.brandCode" size="sm" class="w-40" aria-label="品牌" :options="[{ value: '', label: '全部品牌' }, ...brandOptions]" />
        <Select v-model="filter.status" size="sm" class="w-36" aria-label="手册状态" :options="[{ value: '', label: '全部状态' }, { value: 'published', label: '已发布' }, { value: 'offline', label: '已下架' }]" />
        <Button type="submit" size="sm" :disabled="loading"><Search />查询</Button><Button variant="ghost" size="sm" :disabled="loading" @click="reset">重置</Button>
      </form>
      <Callout v-if="error" tone="destructive" title="手册加载失败" class="m-5">{{ error }}<template #action><Button size="sm" variant="outline" @click="load">重试</Button></template></Callout>
      <div v-else-if="loading" class="space-y-2 border-t p-4"><Skeleton v-for="i in 4" :key="i" class="h-14 w-full" /></div>
      <EmptyState v-else-if="!rows.length" :title="applied.q || applied.brandCode || applied.status ? '没有匹配的品牌手册' : '还没有品牌手册'" description="上传 HTML 手册后，员工可查看已发布版本。" :icon="BookOpen"><Button v-if="applied.q || applied.brandCode || applied.status" size="sm" variant="outline" @click="reset">清除筛选</Button><Button v-else size="sm" @click="openUpload()"><Upload />上传手册</Button></EmptyState>
      <div v-else class="max-h-[calc(100dvh-17rem)] overflow-auto border-t border-border/70"><table class="data-table min-w-[840px]"><thead><tr><th>手册</th><th>品牌 / 类型</th><th class="num">文件大小</th><th>状态</th><th>更新人 / 时间</th><th class="sticky-col text-right">操作</th></tr></thead><tbody>
        <tr v-for="row in rows" :key="row.id"><td class="max-w-72 font-medium"><p class="truncate" :title="row.title">{{ row.title }}</p></td><td><p>{{ brandLabel(row.brandCode) }}</p><p class="mt-1 text-xs text-muted-foreground">{{ typeLabel(row.docType) }}</p></td><td class="num text-xs">{{ sizeLabel(row.fileSize) }}</td><td><Badge :variant="row.status === 'published' ? 'success' : 'secondary'">{{ row.status === 'published' ? '已发布' : '已下架' }}</Badge></td><td class="text-xs text-muted-foreground"><p>{{ row.updatedBy || '–' }}</p><p class="mt-1">{{ formatPortalDateTime(row.updatedAt) }}</p></td><td class="sticky-col"><div class="flex justify-end gap-1 whitespace-nowrap"><Button variant="ghost" size="sm" :disabled="busy" @click="preview(row)">预览</Button><Button variant="ghost" size="sm" :disabled="busy" @click="openEdit(row)">编辑</Button><DropdownMenu><template #trigger><Button variant="ghost" size="icon-sm" :disabled="busy" :aria-label="`${row.title}的更多操作`"><MoreHorizontal /></Button></template><DropdownMenuItem :disabled="busy" @select="openUpload(row)">替换文件</DropdownMenuItem><DropdownMenuItem :disabled="busy" @select="openHistory(row)">版本历史</DropdownMenuItem><DropdownMenuItem destructive :disabled="busy" @select="pending = { type: 'delete', row }">删除手册</DropdownMenuItem></DropdownMenu></div></td></tr>
      </tbody></table></div>
      <Pagination class="border-t px-5 py-3" :page="page" :page-size="20" :total="total" @update:page="changePage" />
    </section>
    <Dialog :open="uploadOpen" :title="replacing ? '替换手册文件' : '上传品牌手册'" @update:open="!busy && (uploadOpen = $event)">
      <form id="brand-upload-form" class="space-y-4" @submit.prevent="upload">
        <Callout v-if="formError" tone="destructive">{{ formError }}</Callout>
        <Callout>同一品牌和文档类型只保留一份当前手册。新手册默认发布；替换会保留版本历史和当前发布状态。</Callout>
        <div><label for="brand-doc-brand" class="field-label">品牌 *</label><Select id="brand-doc-brand" v-model="uploadForm.brandCode" :options="uploadBrands" :disabled="replacing" /></div>
        <div><label for="brand-doc-type" class="field-label">文档类型 *</label><Select id="brand-doc-type" v-model="uploadForm.docType" :options="uploadTypes" :disabled="replacing" /></div>
        <div><label for="brand-doc-title" class="field-label">标题 *</label><Input id="brand-doc-title" v-model="uploadForm.title" required maxlength="120" /></div>
        <div><label for="brand-doc-file" class="field-label">HTML 文件 *</label><Input id="brand-doc-file" :key="fileKey" type="file" accept=".html" required @change="file = ($event.target as HTMLInputElement).files?.[0] ?? null" /><p class="mt-1 text-xs text-muted-foreground">UTF-8 编码，最大 20MB。</p></div>
      </form>
      <template #footer><Button variant="outline" :disabled="busy" @click="uploadOpen = false">取消</Button><Button type="submit" form="brand-upload-form" :loading="busy">上传手册</Button></template>
    </Dialog>
    <Dialog :open="!!editing" title="编辑手册" @update:open="!busy && !$event && (editing = null)">
      <form id="brand-edit-form" class="space-y-4" @submit.prevent="saveEdit"><Callout v-if="formError" tone="destructive">{{ formError }}</Callout><div><label for="brand-edit-title" class="field-label">标题 *</label><Input id="brand-edit-title" v-model="editForm.title" required maxlength="120" /></div><div><label for="brand-edit-status" class="field-label">状态</label><Select id="brand-edit-status" v-model="editForm.status" :options="[{ value: 'published', label: '发布，员工可查看' }, { value: 'offline', label: '下架' }]" /></div></form>
      <template #footer><Button variant="outline" :disabled="busy" @click="editing = null">取消</Button><Button type="submit" form="brand-edit-form" :loading="busy">保存</Button></template>
    </Dialog>
    <Dialog :open="!!historyDoc" :title="`版本历史 · ${historyDoc?.title ?? ''}`" size="lg" @update:open="!busy && !$event && (historyDoc = null)">
      <Callout v-if="historyError" tone="destructive">{{ historyError }}</Callout><p v-else-if="historyLoading" class="text-sm text-muted-foreground">正在加载版本…</p><EmptyState v-else-if="!versions.length" title="还没有历史版本" /><div v-else class="overflow-auto"><table class="data-table"><thead><tr><th>版本</th><th>上传时间</th><th>上传人</th><th>大小</th><th>操作</th></tr></thead><tbody><tr v-for="version in versions" :key="version.id"><td>#{{ version.id }}</td><td>{{ formatPortalDateTime(version.createdAt) }}</td><td>{{ version.uploadedBy }}</td><td>{{ sizeLabel(version.fileSize) }}</td><td><Button variant="outline" size="sm" :disabled="busy" @click="historyDoc && (pending = { type: 'rollback', row: historyDoc, versionId: version.id })">回滚到此版本</Button></td></tr></tbody></table></div>
    </Dialog>
    <Dialog :open="!!previewDoc" :title="previewDoc?.title ?? '手册预览'" size="xl" @update:open="!$event && closePreview()">
      <p v-if="previewLoading" class="text-sm text-muted-foreground">正在读取手册…</p><Callout v-if="previewError" tone="destructive">{{ previewError }}</Callout><iframe v-if="previewUrl" :src="previewUrl" :title="previewDoc?.title" sandbox="allow-scripts" referrerpolicy="no-referrer" class="h-[65dvh] w-full rounded border bg-white" />
      <template #footer><Button variant="outline" :disabled="!previewBlob" @click="downloadPreview"><Download />下载 HTML</Button><Button variant="outline" @click="closePreview">关闭</Button></template>
    </Dialog>
    <ConfirmDialog :open="!!pending" :title="pending?.type === 'delete' ? '删除品牌手册' : '回滚品牌手册'" :description="pending?.type === 'delete' ? `永久删除「${pending.row.title}」及其版本记录？` : `把「${pending?.row.title ?? ''}」恢复到版本 #${pending?.type === 'rollback' ? pending.versionId : ''}？`" :tone="pending?.type === 'delete' ? 'destructive' : 'default'" :busy="busy" @update:open="!busy && !$event && (pending = null)" @confirm="confirm" />
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { BookOpen, Download, MoreHorizontal, Search, Upload } from '@lucide/vue'
import { api } from '../../lib/api'
import { portalFile, savePortalFile } from '../../lib/portal-files'
import { errText, toast } from '../../lib/toast'
import Badge from '../../ui/Badge.vue'
import Button from '../../ui/Button.vue'
import Callout from '../../ui/Callout.vue'
import ConfirmDialog from '../../ui/ConfirmDialog.vue'
import Dialog from '../../ui/Dialog.vue'
import EmptyState from '../../ui/EmptyState.vue'
import DropdownMenu from '../../ui/DropdownMenu.vue'
import DropdownMenuItem from '../../ui/DropdownMenuItem.vue'
import Skeleton from '../../ui/Skeleton.vue'
import { formatPortalDateTime } from '../../lib/portal-management-format'
import Input from '../../ui/Input.vue'
import PageHeader from '../../ui/PageHeader.vue'
import Pagination from '../../ui/Pagination.vue'
import Select from '../../ui/Select.vue'
interface BrandDoc { id: number; brandCode: string; docType: string; title: string; fileSize: number; status: string; updatedBy?: string; updatedAt?: string }
interface Version { id: number; fileSize: number; uploadedBy: string; createdAt: string }
const base = '/admin-api/portal/brand-docs'
const brandOptions = [{ value: 'linsoul', label: 'Linsoul' }, { value: 'thieaudio', label: 'ThieAudio' }, { value: 'kiwiears', label: 'KiwiEars' }, { value: 'ziigaat', label: 'ZiiGaat' }, { value: 'tripowin', label: 'Tripowin' }, { value: 'epomaker', label: 'Epomaker' }, { value: 'kiiboom', label: 'KiiBOOM' }, { value: 'mechlands', label: 'Mechlands' }]
const typeOptions = [{ value: 'manual', label: '品牌手册' }, { value: 'playbook', label: '品牌执行手册' }, { value: 'manual-en', label: 'Brand Manual (EN)' }]
const brandLabel = (code: string) => brandOptions.find(x => x.value === code)?.label ?? code
const typeLabel = (code: string) => typeOptions.find(x => x.value === code)?.label ?? code
const sizeLabel = (bytes: number) => bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.ceil(bytes / 1024)} KB`
const rows = ref<BrandDoc[]>([]), page = ref(1), total = ref(0), loading = ref(false), busy = ref(false), error = ref('')
const filter = reactive({ q: '', brandCode: '', status: '' }), applied = ref({ ...filter })
let loadSeq = 0, historySeq = 0, previewSeq = 0
const uploadOpen = ref(false), replacing = ref(false), file = ref<File | null>(null), fileKey = ref(0), formError = ref('')
const uploadForm = reactive({ brandCode: 'kiwiears', docType: 'manual', title: '' })
const uploadBrands = computed(() => brandOptions.some(x => x.value === uploadForm.brandCode) ? brandOptions : [...brandOptions, { value: uploadForm.brandCode, label: uploadForm.brandCode }])
const uploadTypes = computed(() => typeOptions.some(x => x.value === uploadForm.docType) ? typeOptions : [...typeOptions, { value: uploadForm.docType, label: uploadForm.docType }])
const editing = ref<BrandDoc | null>(null), editForm = reactive({ title: '', status: 'published' })
const historyDoc = ref<BrandDoc | null>(null), versions = ref<Version[]>([]), historyLoading = ref(false), historyError = ref('')
const previewDoc = ref<BrandDoc | null>(null), previewUrl = ref(''), previewBlob = ref<Blob | null>(null), previewLoading = ref(false), previewError = ref('')
const pending = ref<{ type: 'delete'; row: BrandDoc } | { type: 'rollback'; row: BrandDoc; versionId: number } | null>(null)
async function load() {
  const seq = ++loadSeq; loading.value = true; error.value = ''
  try { const result = await api.get<{ list: BrandDoc[]; total: number }>(`${base}/page`, { ...applied.value, pageNo: page.value, pageSize: 20 }); if (seq === loadSeq) { rows.value = result.list; total.value = result.total } }
  catch (e) { if (seq === loadSeq) { rows.value = []; total.value = 0; error.value = errText(e) } }
  finally { if (seq === loadSeq) loading.value = false }
}
function search() { applied.value = { ...filter }; page.value = 1; void load() }
function reset() { Object.assign(filter, { q: '', brandCode: '', status: '' }); search() }
function changePage(next: number) { page.value = next; void load() }
function openUpload(row?: BrandDoc) { Object.assign(uploadForm, row ? { brandCode: row.brandCode, docType: row.docType, title: row.title } : { brandCode: 'kiwiears', docType: 'manual', title: '' }); replacing.value = !!row; file.value = null; fileKey.value++; formError.value = ''; uploadOpen.value = true }
async function upload() {
  if (busy.value) return
  if (!file.value || !/\.html$/i.test(file.value.name) || file.value.size > 20 * 1024 * 1024 || !file.value.size) { formError.value = '请选择不超过 20MB 的非空 HTML 文件'; return }
  busy.value = true; formError.value = ''
  try { const body = new FormData(); body.append('file', file.value); body.append('brandCode', uploadForm.brandCode); body.append('docType', uploadForm.docType); body.append('title', uploadForm.title.trim()); await api.post(`${base}/upload`, body, 90000); uploadOpen.value = false; toast('品牌手册已上传'); await load() }
  catch (e) { formError.value = errText(e) }
  finally { busy.value = false }
}
function openEdit(row: BrandDoc) { editing.value = row; Object.assign(editForm, { title: row.title, status: row.status }); formError.value = '' }
async function saveEdit() { if (!editing.value || busy.value) return; busy.value = true; formError.value = ''; try { await api.put(`${base}/update`, { id: editing.value.id, ...editForm }); editing.value = null; toast('手册已保存'); await load() } catch (e) { formError.value = errText(e) } finally { busy.value = false } }
async function openHistory(row: BrandDoc) { const seq = ++historySeq; historyDoc.value = row; versions.value = []; historyError.value = ''; historyLoading.value = true; try { const data = await api.get<Version[]>(`${base}/versions`, { id: row.id }); if (seq === historySeq) versions.value = data } catch (e) { if (seq === historySeq) historyError.value = errText(e) } finally { if (seq === historySeq) historyLoading.value = false } }
async function preview(row: BrandDoc) { closePreview(); const seq = ++previewSeq; previewDoc.value = row; previewLoading.value = true; previewError.value = ''; try { const { blob } = await portalFile(`${base}/file?id=${row.id}`); if (seq === previewSeq) { previewBlob.value = blob; previewUrl.value = URL.createObjectURL(blob) } } catch (e) { if (seq === previewSeq) previewError.value = errText(e) } finally { if (seq === previewSeq) previewLoading.value = false } }
function closePreview() { previewSeq++; if (previewUrl.value) URL.revokeObjectURL(previewUrl.value); previewUrl.value = ''; previewBlob.value = null; previewDoc.value = null }
function downloadPreview() { if (previewBlob.value) savePortalFile(previewBlob.value, `${previewDoc.value?.title ?? '品牌手册'}.html`) }
async function confirm() { if (!pending.value || busy.value) return; busy.value = true; try { const action = pending.value; if (action.type === 'delete') { await api.del(`${base}/delete`, { id: action.row.id }); historyDoc.value = null; toast('手册已删除') } else { await api.post(`${base}/rollback`, { id: action.row.id, versionId: action.versionId }); toast('手册已回滚'); await openHistory(action.row) } pending.value = null; await load() } catch (e) { toast(errText(e), 'bad') } finally { busy.value = false } }
onMounted(() => { void load() })
onUnmounted(() => { loadSeq++; historySeq++; closePreview() })
</script>
