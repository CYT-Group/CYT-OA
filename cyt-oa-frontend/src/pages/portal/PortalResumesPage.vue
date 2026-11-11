<template>
  <div class="min-w-0 space-y-4">
    <PageHeader title="简历识别" description="上传候选人简历，AI 提取后由 HR 校对确认。"><template #actions><Button size="sm" @click="uploadOpen = true"><Upload />上传简历</Button></template></PageHeader>
    <HrRecruitNavigation />
    <section class="panel overflow-hidden">
      <form class="table-toolbar flex-wrap gap-2" @submit.prevent="search"><Input v-model="filter.q" class="h-9 w-full text-[13px] sm:w-64" aria-label="候选人关键词" placeholder="搜索姓名、电话或邮箱" /><Select v-model="filter.status" size="sm" class="w-40" aria-label="简历状态" :options="[{ value: '', label: '全部状态' }, { value: 'parsed', label: '待校对' }, { value: 'confirmed', label: '已确认' }, { value: 'failed', label: '识别失败' }]" /><Button type="submit" size="sm" :disabled="loading"><Search />查询</Button><Button variant="ghost" size="sm" :disabled="loading" @click="reset">重置</Button><Button variant="ghost" size="icon-sm" class="ml-auto" aria-label="刷新简历" :disabled="loading" @click="load"><RefreshCw /></Button></form>
      <Callout v-if="loadError" tone="destructive" title="简历列表加载失败" class="m-5">{{ loadError }}<template #action><Button size="sm" variant="outline" @click="load">重试</Button></template></Callout><div v-else-if="loading" class="space-y-2 border-t p-4"><Skeleton v-for="i in 4" :key="i" class="h-14 w-full" /></div><EmptyState v-else-if="!rows.length" :title="applied.q || applied.status ? '没有匹配的简历' : '还没有候选人简历'" description="上传简历后，校对识别结果并安排面试。" :icon="FileText"><Button v-if="applied.q || applied.status" size="sm" variant="outline" @click="reset">清除筛选</Button><Button v-else size="sm" @click="uploadOpen = true"><Upload />上传简历</Button></EmptyState>
      <div v-else class="max-h-[calc(100dvh-17rem)] overflow-auto border-t border-border/70"><table class="data-table min-w-[1000px]"><thead><tr><th>候选人</th><th>联系方式</th><th>学历 / 工作年限</th><th>当前 / 期望岗位</th><th>状态</th><th>上传时间</th><th class="sticky-col text-right">操作</th></tr></thead><tbody><tr v-for="row in rows" :key="row.id"><td class="max-w-52"><p class="truncate font-medium" :title="row.name || row.sourceFilename">{{ row.name || row.sourceFilename }}</p><p class="mt-1 text-xs text-muted-foreground">{{ row.currentCity || '–' }}</p><Badge v-if="row.duplicateOfId" variant="warning" class="mt-1">疑似重复</Badge></td><td class="text-xs"><p>{{ row.phoneMasked || '–' }}</p><p class="mt-1 text-muted-foreground">{{ row.email || '–' }}</p></td><td><p>{{ row.highestDegree || '–' }}</p><p class="mt-1 text-xs text-muted-foreground">{{ row.yearsOfExperience == null ? '–' : `${row.yearsOfExperience} 年` }}</p></td><td class="max-w-60 text-xs"><p class="truncate" :title="row.currentTitle">{{ row.currentTitle || '–' }}</p><p class="mt-1 truncate text-muted-foreground" :title="row.expectedPosition">期望：{{ row.expectedPosition || '–' }}</p></td><td><Badge :variant="statusVariant(row.status)">{{ statusLabel(row.status) }}</Badge></td><td class="text-xs text-muted-foreground">{{ formatPortalDateTime(row.createdAt) }}</td><td class="sticky-col"><div class="flex justify-end gap-1"><Button size="sm" variant="ghost" :disabled="busy" @click="openRecord(row.id)">查看 / 校对</Button><DropdownMenu><template #trigger><Button size="icon-sm" variant="ghost" :disabled="busy" :aria-label="`${row.name || row.sourceFilename}的更多操作`"><MoreHorizontal /></Button></template><DropdownMenuItem destructive :disabled="busy" @select="pending = { kind: 'delete', row }">删除简历</DropdownMenuItem></DropdownMenu></div></td></tr></tbody></table></div>
      <Pagination class="border-t px-5 py-3" :page="page" :page-size="20" :total="total" @update:page="page = $event; load()" />
    </section>
    <Dialog :open="uploadOpen" title="上传并识别简历" size="lg" @update:open="!parsing && (uploadOpen = $event)">
      <div class="min-w-0 space-y-4"><p class="text-xs leading-6 text-muted-foreground">支持 PDF、docx、PNG、JPG、WebP，每份不超过 10MB。可选择多份文件依次识别，识别结果须由 HR 核对。</p><Input type="file" accept=".pdf,.docx,.png,.jpg,.jpeg,.webp" multiple :disabled="parsing" aria-label="选择简历文件" @change="selectFiles" /><Callout v-if="uploadError" tone="destructive">{{ uploadError }}</Callout><div v-for="(item, i) in queue" :key="i" class="flex items-start justify-between gap-3 rounded-lg border p-3"><div class="min-w-0"><p class="break-all text-sm font-medium">{{ item.file.name }}</p><p class="mt-1 text-xs" :class="item.error ? 'text-destructive' : 'text-muted-foreground'">{{ item.error || ({ pending: '等待识别', running: 'AI 正在识别…', done: '识别完成，等待 HR 校对', failed: '识别失败' }[item.status]) }}</p></div><Button v-if="item.id && !parsing" variant="outline" size="sm" @click="uploadOpen = false; openRecord(item.id)">查看</Button></div></div>
      <template #footer><Button variant="outline" :disabled="parsing" @click="uploadOpen = false">关闭</Button><Button :loading="parsing" :disabled="!queue.some(item => item.status === 'pending')" @click="parseFiles">开始识别</Button></template>
    </Dialog>
    <Dialog :open="detailId !== null" :title="detail?.name ? `简历校对 · ${detail.name}` : '简历详情与校对'" size="xl" @update:open="!$event && requestClose()">
      <p v-if="detailLoading" class="py-8 text-sm text-muted-foreground">正在读取简历…</p><Callout v-else-if="detailError" tone="destructive" title="详情加载失败">{{ detailError }}<template #action><Button size="sm" variant="outline" @click="detailId && openRecord(detailId)">重试</Button></template></Callout>
      <template v-else-if="detail">
        <div class="mb-4 flex flex-wrap items-center gap-2"><Badge :variant="statusVariant(detail.status)">{{ statusLabel(detail.status) }}</Badge><span class="break-all text-xs text-muted-foreground">{{ detail.sourceFilename }}</span><Button variant="outline" size="sm" class="ml-auto" :disabled="busy" @click="loadFile"><FileText />查看原件</Button><Button variant="outline" size="sm" :disabled="busy" @click="downloadFile"><Download />下载原件</Button><DropdownMenu><template #trigger><Button variant="ghost" size="icon-sm" :disabled="busy" aria-label="简历更多操作"><MoreHorizontal /></Button></template><DropdownMenuItem :disabled="busy || dirty || detail.status === 'confirmed'" @select="pending = { kind: 'reparse', row: detail }">{{ detail.status === 'confirmed' ? '已确认，修改校对后可重新识别' : dirty ? '保存校对后可重新识别' : '重新识别原件' }}</DropdownMenuItem></DropdownMenu></div>
        <Callout v-if="detail.errorMsg" tone="destructive" class="mb-4" title="AI 识别未完成">{{ detail.errorMsg }}</Callout>
        <Callout v-if="detail.duplicateOfId" tone="warning" class="mb-4">疑似与简历 #{{ detail.duplicateOfId }} 重复，请核对后再确认。<template #action><Button variant="outline" size="sm" :disabled="busy || dirty" @click="openRecord(detail.duplicateOfId)">查看该简历</Button></template></Callout>
        <Callout v-if="saveError" tone="destructive" class="mb-4">{{ saveError }}</Callout>
        <SegmentedTabs v-model="detailTab" :items="[{ value: 'basic', label: '基本资料' }, { value: 'history', label: '教育与工作经历' }, { value: 'skills', label: '技能与摘要' }, { value: 'interviews', label: '面试流程' }]" />
        <PortalResumeInterviews v-if="detailTab === 'interviews'" :key="detail.id" class="mt-4" :resume-id="detail.id" :can-create="['parsed', 'confirmed'].includes(detail.status) && !!detail.name" :default-position="detail.expectedPosition" />
        <form v-show="detailTab !== 'interviews'" id="resume-edit-form" class="mt-4 space-y-4" @submit.prevent="save">
          <Checkbox v-if="!detail.extracted?.is_resume || detail.status === 'failed'" v-model="isResume">经核对，附件是一份有效的个人简历</Checkbox>
          <div v-if="detailTab === 'basic'" class="grid gap-4 sm:grid-cols-2">
            <div v-for="field in commonFields" :key="field.key"><label :for="`resume-${field.key}`" class="field-label">{{ field.label }}</label><Select v-if="field.options" :id="`resume-${field.key}`" v-model="form[field.key]" :options="[{ value: '', label: '未填写' }, ...field.options.map(value => ({ value, label: value }))]" /><Input v-else :id="`resume-${field.key}`" v-model="form[field.key]" :type="field.type || 'text'" :maxlength="field.max" :min="field.key === 'years_of_experience' ? 0 : undefined" :max="field.type === 'date' ? today : field.key === 'years_of_experience' ? 99.9 : undefined" :step="field.key === 'years_of_experience' ? 0.1 : undefined" /></div>
          </div>
          <div v-else-if="detailTab === 'history'" class="space-y-6">
            <section v-for="section in historySections" :key="section.key" class="space-y-3"><div class="flex items-center justify-between"><h3 class="text-sm font-semibold">{{ section.label }}</h3><Button variant="outline" size="sm" :disabled="busy || histories[section.key].length >= 100" @click="addHistory(section.key)"><Plus />添加</Button></div><p v-if="!histories[section.key].length" class="text-xs text-muted-foreground">未提取到{{ section.label }}，可对照原件补充。</p><div v-for="(item, i) in histories[section.key]" :key="i" class="rounded-lg border p-4"><div class="mb-3 flex items-center justify-between"><span class="text-xs text-muted-foreground">第 {{ i + 1 }} 条</span><Button variant="ghost" size="sm" :disabled="busy" @click="histories[section.key].splice(i, 1)"><Trash2 />移除</Button></div><div class="grid gap-3 sm:grid-cols-2"><div v-for="field in section.fields" :key="field.key" :class="field.key === 'description' ? 'sm:col-span-2' : ''"><label :for="`resume-${section.key}-${i}-${field.key}`" class="field-label">{{ field.label }}</label><Textarea v-if="field.key === 'description'" :id="`resume-${section.key}-${i}-${field.key}`" v-model="item[field.key]" :rows="3" /><Input v-else :id="`resume-${section.key}-${i}-${field.key}`" v-model="item[field.key]" /></div></div></div></section>
          </div>
          <div v-else class="space-y-4"><div><label for="resume-summary" class="field-label">候选人摘要</label><Textarea id="resume-summary" v-model="summary" :rows="5" maxlength="10000" /></div><div v-for="field in listFields" :key="field.key"><label :for="`resume-${field.key}`" class="field-label">{{ field.label }}</label><Textarea :id="`resume-${field.key}`" v-model="listTexts[field.key]" :rows="3" placeholder="每行一项" /></div></div>
          <Callout v-if="warnings.length" tone="warning" title="需要 HR 复核"><ul class="list-inside list-disc"><li v-for="(warning, i) in warnings" :key="i">{{ warning }}</li></ul></Callout>
        </form>
      </template>
      <template #footer><div class="flex w-full flex-wrap items-center justify-between gap-2"><span class="text-xs" :class="dirty ? 'text-amber-600 dark:text-amber-400' : 'text-muted-foreground'">{{ dirty ? '有未保存的校对内容' : '校对后可确认并安排面试' }}</span><div class="flex flex-wrap gap-2"><Button variant="outline" :disabled="busy" @click="requestClose">关闭</Button><Button type="submit" form="resume-edit-form" :variant="dirty ? 'default' : 'outline'" :loading="busy" :disabled="!detail || !dirty">保存校对</Button><Button v-if="detail?.status !== 'confirmed'" :variant="!dirty && detail?.status === 'parsed' ? 'default' : 'outline'" :disabled="busy || !detail || dirty || detail.status !== 'parsed'" @click="detail && (pending = { kind: 'confirm', row: detail })">确认简历</Button></div></div></template>
    </Dialog>
    <Dialog :open="fileOpen" title="简历原件" size="xl" @update:open="!$event && closeFile()"><p v-if="fileLoading" class="text-sm text-muted-foreground">正在读取原件…</p><Callout v-else-if="fileError" tone="destructive">{{ fileError }}</Callout><iframe v-else-if="fileUrl && detail?.fileExt === 'pdf'" :src="fileUrl" title="简历 PDF 原件" class="h-[65dvh] w-full rounded border" /><img v-else-if="fileUrl && detail && ['png', 'jpg', 'jpeg', 'webp'].includes(detail.fileExt)" :src="fileUrl" alt="简历原件" class="mx-auto max-w-full rounded border" /><p v-else-if="fileBlob" class="text-sm text-muted-foreground">Word 原件可下载后查看。</p><template #footer><Button variant="outline" :disabled="!fileBlob" @click="fileBlob && savePortalFile(fileBlob, detail?.sourceFilename || '简历')"><Download />下载原件</Button><Button variant="outline" @click="closeFile">关闭</Button></template></Dialog>
    <ConfirmDialog :open="!!pending" :title="confirmTitle" :description="confirmDescription" :tone="pending?.kind === 'delete' ? 'destructive' : 'default'" :busy="busy" @update:open="!busy && !$event && (pending = null)" @confirm="act" />
    <ConfirmDialog :open="discardOpen" title="放弃未保存的校对" description="关闭后本次未保存的校对内容会丢失。确认关闭？" tone="destructive" @update:open="discardOpen = $event" @confirm="discardOpen = false; closeDetail()" />
  </div>
</template>
<script setup lang="ts">
import HrRecruitNavigation from '../../components/HrRecruitNavigation.vue'
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Download, FileText, MoreHorizontal, Plus, RefreshCw, Search, Trash2, Upload } from '@lucide/vue'
import { api } from '../../lib/api'
import { portalFile, savePortalFile } from '../../lib/portal-files'
import { errText, toast } from '../../lib/toast'
import PortalResumeInterviews from '../../components/portal/PortalResumeInterviews.vue'
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
import { formatPortalDateTime } from '../../lib/portal-management-format'
import Input from '../../ui/Input.vue'
import PageHeader from '../../ui/PageHeader.vue'
import Pagination from '../../ui/Pagination.vue'
import SegmentedTabs from '../../ui/SegmentedTabs.vue'
import Select from '../../ui/Select.vue'
import Textarea from '../../ui/Textarea.vue'
interface Resume { id: number; status: string; sourceFilename: string; fileExt: string; name?: string; phoneMasked?: string; email?: string; currentCity?: string; highestDegree?: string; yearsOfExperience?: number; currentTitle?: string; expectedPosition?: string; duplicateOfId?: number; createdAt?: string; extracted?: Record<string, unknown>; errorMsg?: string }
type HistoryKey = 'education' | 'work_history'
type QueueItem = { file: File; status: 'pending' | 'running' | 'done' | 'failed'; error: string; id?: number }
const base = '/admin-api/portal/resumes', today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
const rows = ref<Resume[]>([]), page = ref(1), total = ref(0), loading = ref(false), loadError = ref(''), busy = ref(false)
const filter = reactive({ q: '', status: '' }), applied = ref({ ...filter })
const uploadOpen = ref(false), uploadError = ref(''), parsing = ref(false), queue = ref<QueueItem[]>([])
const detailId = ref<number | null>(null), detail = ref<Resume | null>(null), detailLoading = ref(false), detailError = ref(''), saveError = ref(''), detailTab = ref('basic'), isResume = ref(true), discardOpen = ref(false)
let listSeq = 0, detailSeq = 0, fileSeq = 0, alive = true
const commonFields: { key: string; label: string; type?: string; max?: number; options?: string[] }[] = [{ key: 'name', label: '候选人姓名', max: 100 }, { key: 'gender', label: '性别', options: ['男', '女'] }, { key: 'birth_date', label: '出生日期', type: 'date' }, { key: 'phone', label: '联系电话', type: 'tel', max: 32 }, { key: 'email', label: '电子邮箱', type: 'email', max: 120 }, { key: 'current_city', label: '当前城市', max: 50 }, { key: 'highest_degree', label: '最高学历', options: ['博士', '硕士', '本科', '大专', '高中及以下', '其他'] }, { key: 'years_of_experience', label: '工作年限', type: 'number' }, { key: 'current_company', label: '当前公司', max: 120 }, { key: 'current_title', label: '当前职位', max: 120 }, { key: 'expected_position', label: '期望职位', max: 120 }, { key: 'expected_salary', label: '期望薪资', max: 60 }]
const historySections: { key: HistoryKey; label: string; fields: { key: string; label: string }[] }[] = [{ key: 'education', label: '教育经历', fields: [{ key: 'school', label: '院校' }, { key: 'degree', label: '学历' }, { key: 'major', label: '专业' }, { key: 'start', label: '开始时间' }, { key: 'end', label: '结束时间' }] }, { key: 'work_history', label: '工作经历', fields: [{ key: 'company', label: '公司' }, { key: 'title', label: '职位' }, { key: 'start', label: '开始时间' }, { key: 'end', label: '结束时间' }, { key: 'description', label: '工作内容' }] }]
const listFields = [{ key: 'skills', label: '技能' }, { key: 'certificates', label: '证书' }, { key: 'languages', label: '语言' }]
const form = reactive<Record<string, string>>({}), histories = reactive<Record<HistoryKey, Record<string, string>[]>>({ education: [], work_history: [] }), listTexts = reactive<Record<string, string>>({}), summary = ref(''), warnings = ref<string[]>([]), origin = ref('')
const dirty = computed(() => !!detail.value && JSON.stringify(extraction()) !== origin.value)
const pending = ref<{ kind: 'delete' | 'confirm' | 'reparse'; row: Resume } | null>(null)
const confirmTitle = computed(() => ({ delete: '删除简历', confirm: '确认简历', reparse: '重新识别简历' }[pending.value?.kind ?? 'confirm']))
const confirmDescription = computed(() => !pending.value ? '' : pending.value.kind === 'delete' ? `永久删除「${pending.value.row.name || pending.value.row.sourceFilename}」及其原件？` : pending.value.kind === 'reparse' ? '重新调用 AI 识别原件会覆盖当前校对内容。确认继续？' : '确认已核对候选人资料与原件一致，并将简历标记为已确认？')
const fileOpen = ref(false), fileLoading = ref(false), fileError = ref(''), fileBlob = ref<Blob | null>(null), fileUrl = ref('')
const statusLabel = (status: string) => ({ parsed: '待校对', confirmed: '已确认', failed: '识别失败' } as Record<string, string>)[status] ?? status
const statusVariant = (status: string) => status === 'confirmed' ? 'success' as const : status === 'failed' ? 'destructive' as const : 'warning' as const
async function load() { const seq = ++listSeq; loading.value = true; loadError.value = ''; try { const data = await api.get<{ list: Resume[]; total: number }>(`${base}/page`, { ...applied.value, pageNo: page.value, pageSize: 20 }); if (seq === listSeq) { rows.value = data.list; total.value = data.total } } catch (e) { if (seq === listSeq) { rows.value = []; total.value = 0; loadError.value = errText(e) } } finally { if (seq === listSeq) loading.value = false } }
function search() { applied.value = { ...filter }; page.value = 1; void load() }
function reset() { Object.assign(filter, { q: '', status: '' }); search() }
function selectFiles(event: Event) { const input = event.target as HTMLInputElement; uploadError.value = ''; const files = Array.from(input.files ?? []); if (files.length > 20) { uploadError.value = '每次最多选择 20 份简历'; return } queue.value = files.map(file => ({ file, status: 'pending', error: '' })); input.value = '' }
async function parseFiles() {
  if (parsing.value) return
  parsing.value = true
  try { for (const item of queue.value) {
    if (!alive) break
    if (item.status !== 'pending') continue
    if (!/\.(pdf|docx|png|jpe?g|webp)$/i.test(item.file.name) || !item.file.size || item.file.size > 10 * 1024 * 1024) { item.status = 'failed'; item.error = '文件须为 10MB 以内的非空 PDF、docx 或图片'; continue }
    item.status = 'running'
    try { const body = new FormData(); body.append('file', item.file); const result = await api.post<Resume>(`${base}/parse`, body, 180000); item.id = result.id; item.status = result.status === 'failed' ? 'failed' : 'done'; item.error = result.errorMsg ?? ''; if (alive) await load() }
    catch (e) { item.status = 'failed'; item.error = errText(e) }
  } } finally { parsing.value = false }
}
function fill(record: Resume) {
  detail.value = record; const data = record.extracted ?? {}
  for (const field of commonFields) form[field.key] = data[field.key] == null ? '' : String(data[field.key])
  isResume.value = data.is_resume === true; summary.value = String(data.summary ?? '')
  for (const section of historySections) histories[section.key] = Array.isArray(data[section.key]) ? (data[section.key] as Record<string, unknown>[]).map(item => ({ ...item } as Record<string, string>)) : []
  for (const field of listFields) listTexts[field.key] = Array.isArray(data[field.key]) ? (data[field.key] as unknown[]).map(String).join('\n') : ''
  warnings.value = Array.isArray(data.warnings) ? data.warnings.map(String) : []
  origin.value = JSON.stringify(extraction())
}
function extraction() {
  const data: Record<string, unknown> = { ...detail.value?.extracted, is_resume: isResume.value, summary: summary.value.trim() || null, warnings: [...warnings.value] }
  for (const field of commonFields) { const value = String(form[field.key] ?? '').trim(); data[field.key] = field.key === 'years_of_experience' ? value === '' ? null : Number(value) : value || null }
  for (const section of historySections) data[section.key] = histories[section.key].map(item => ({ ...item }))
  for (const field of listFields) data[field.key] = String(listTexts[field.key] ?? '').split(/\r?\n/).map(x => x.trim()).filter(Boolean)
  return data
}
function addHistory(key: HistoryKey) { histories[key].push(Object.fromEntries(historySections.find(s => s.key === key)!.fields.map(f => [f.key, '']))) }
async function openRecord(id: number) { if (busy.value) return; const seq = ++detailSeq; closeFile(); detailId.value = id; detail.value = null; detailError.value = ''; saveError.value = ''; detailLoading.value = true; detailTab.value = 'basic'; try { const data = await api.get<Resume>(`${base}/get`, { id }); if (seq === detailSeq) fill(data) } catch (e) { if (seq === detailSeq) detailError.value = errText(e) } finally { if (seq === detailSeq) detailLoading.value = false } }
function requestClose() { if (busy.value) return; if (dirty.value) discardOpen.value = true; else closeDetail() }
function closeDetail() { detailSeq++; closeFile(); detailId.value = null; detail.value = null }
async function save() { if (!detail.value || busy.value || !dirty.value) return; busy.value = true; saveError.value = ''; try { const data = await api.put<Resume>(`${base}/update`, { id: detail.value.id, extracted: extraction() }); fill(data); await load(); toast('校对结果已保存，请确认简历') } catch (e) { saveError.value = errText(e) } finally { busy.value = false } }
async function act() { if (!pending.value || busy.value) return; busy.value = true; try { const action = pending.value; if (action.kind === 'delete') { await api.del(`${base}/delete`, { id: action.row.id }); if (detailId.value === action.row.id) closeDetail(); toast('简历已删除') } else { const data = await api.post<Resume>(`${base}/${action.kind}`, { id: action.row.id }, action.kind === 'reparse' ? 180000 : undefined); if (detailId.value === data.id) fill(data); toast(data.status === 'failed' ? (data.errorMsg || '重新识别失败，请查看详情') : action.kind === 'confirm' ? '简历已确认' : '重新识别完成', data.status === 'failed' ? 'bad' : 'ok') } pending.value = null; await load() } catch (e) { toast(errText(e), 'bad') } finally { busy.value = false } }
async function loadFile() { if (!detail.value) return; closeFile(); const seq = ++fileSeq, id = detail.value.id; fileOpen.value = true; fileLoading.value = true; fileError.value = ''; try { const data = await portalFile(`${base}/file?id=${id}`); if (seq === fileSeq) { fileBlob.value = data.blob; fileUrl.value = URL.createObjectURL(data.blob) } } catch (e) { if (seq === fileSeq) fileError.value = errText(e) } finally { if (seq === fileSeq) fileLoading.value = false } }
function closeFile() { fileSeq++; fileOpen.value = false; fileBlob.value = null; if (fileUrl.value) URL.revokeObjectURL(fileUrl.value); fileUrl.value = '' }
async function downloadFile() { if (!detail.value || busy.value) return; const record = detail.value; busy.value = true; try { const data = await portalFile(`${base}/file?id=${record.id}`); savePortalFile(data.blob, data.filename || record.sourceFilename) } catch (e) { toast(errText(e), 'bad') } finally { busy.value = false } }
onMounted(() => { void load() })
onUnmounted(() => { alive = false; listSeq++; detailSeq++; closeFile() })
</script>
