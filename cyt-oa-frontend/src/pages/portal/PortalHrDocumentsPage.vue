<template>
  <div v-if="!editing">
    <PageHeader title="HR 文档管理" description="维护制度与 SOP，保存草稿、发布版本和管理员工阅读范围。">
      <template #actions><Button v-if="canEdit" @click="startCreate"><Plus class="size-4" />新建文档</Button></template>
    </PageHeader>
    <section class="panel mt-4 overflow-hidden">
      <form class="table-toolbar flex flex-wrap gap-2" @submit.prevent="page = 1; load()">
        <Input v-model="query.q" aria-label="搜索文档标题" placeholder="搜索标题" class="sm:w-56" />
        <Select v-model="query.category" aria-label="文档分类" class="sm:w-36" :options="[{ value: '', label: '所有分类' }, ...hrCategories]" />
        <Select v-model="query.status" aria-label="发布状态" class="sm:w-36" :options="statuses" />
        <Select v-model="query.deleted" aria-label="文档区域" class="sm:w-32" :options="[{ value: 'false', label: '文档库' }, { value: 'true', label: '回收站' }]" />
        <Button type="submit" variant="outline" :disabled="loading"><Search class="size-4" />查询</Button>
      </form>
      <Callout v-if="loadError" title="文档加载失败" tone="destructive" class="m-4">{{ loadError }}<template #action><Button size="sm" variant="outline" @click="load">重试</Button></template></Callout>
      <div v-if="loading" class="p-10 text-center text-sm text-muted-foreground">正在加载文档…</div>
      <div v-else-if="rows.length" class="overflow-x-auto"><table class="data-table"><thead><tr><th>标题</th><th>分类</th><th>状态</th><th>版本</th><th>更新时间</th><th class="text-right">操作</th></tr></thead><tbody>
        <tr v-for="row in rows" :key="row.id"><td><b class="font-medium">{{ row.draftTitle ?? row.title }}</b><p class="mt-1 text-xs text-muted-foreground">#{{ row.id }} <span v-if="row.requiresSignoff">· 需要签收</span></p></td><td>{{ categoryLabel(row.category) }}</td><td><Badge :variant="row.deletedAt ? 'destructive' : row.offlineAt ? 'secondary' : row.status === 'published' ? 'default' : 'outline'">{{ stateLabel(row) }}</Badge></td><td>{{ row.draftVersion ?? row.version }}</td><td class="whitespace-nowrap text-muted-foreground">{{ time(row.updatedAt) }}</td><td><div class="flex justify-end gap-1">
          <Button size="sm" variant="ghost" :disabled="busy" @click="openDocument(row.id)">{{ canEdit ? '编辑' : '查看' }}</Button>
          <Button v-if="canEdit && row.deletedAt" size="sm" variant="ghost" :disabled="busy" @click="requestAction(row, 'undelete', '恢复文档')">恢复</Button>
          <template v-else-if="canEdit"><Button v-if="row.offlineAt" size="sm" variant="ghost" :disabled="busy" @click="requestAction(row, 'restore', '重新上架')">上架</Button><Button v-else-if="row.publishedAt" size="sm" variant="ghost" :disabled="busy" @click="requestAction(row, 'offline', '下架文档')">下架</Button><Button v-if="!row.publishedAt || row.offlineAt" size="sm" variant="ghost" :disabled="busy" @click="requestAction(row, 'delete', '移入回收站')">删除</Button></template>
        </div></td></tr>
      </tbody></table></div>
      <EmptyState v-else-if="!loadError" title="没有匹配的文档" description="调整筛选条件，或新建一篇文档。" />
      <Pagination class="border-t px-5 py-3" :page="page" :page-size="20" :total="total" @update:page="page = $event; load()" />
    </section>
  </div>

  <div v-else>
    <PageHeader :title="form.title || '新建 HR 文档'" :description="document?.id ? `文档 #${document.id} · ${stateLabel(document)}${dirty ? ' · 有未保存修改' : ''}` : '新文档将先保存为草稿。'">
      <template #actions>
        <Button variant="ghost" :disabled="busy" @click="closeEditor"><ArrowLeft class="size-4" />返回</Button>
        <Button variant="outline" @click="previewOpen = true"><Eye class="size-4" />预览</Button>
        <Button v-if="canEdit && !document?.deletedAt" variant="outline" :disabled="busy" @click="layoutOpen = true">AI 智能排版</Button>
        <Button v-if="canEdit && document?.publishedAt && !document.offlineAt && !document.deletedAt" variant="outline" @click="pushDocument = document; pushOpen = true">推送通知 / 测试</Button>
        <Button v-if="document?.id" variant="outline" :disabled="busy" @click="openHistory"><History class="size-4" />版本</Button>
        <Button v-if="canEdit && !document?.deletedAt" variant="outline" :loading="busy" @click="save"><Save class="size-4" />保存草稿</Button>
        <Button v-if="canEdit && !document?.deletedAt" :disabled="busy" @click="preparePublish">发布</Button>
      </template>
    </PageHeader>
    <Callout v-if="editorError" class="mt-4" title="操作未完成" tone="destructive">{{ editorError }}</Callout>
    <div class="mt-4 grid items-start gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
      <section class="panel min-w-0 p-4 sm:p-5">
        <label for="hr-title" class="field-label">文档标题</label><Input id="hr-title" v-model="form.title" :disabled="!canEdit" maxlength="200" placeholder="请输入文档标题" />
        <div class="my-5 flex flex-wrap items-center justify-between gap-2 border-y py-3"><h2 class="text-sm font-semibold">正文 <span class="text-xs font-normal text-muted-foreground">{{ form.format === 'blocks' ? `${form.blocks.length} / 100 块` : 'HTML 文档' }}</span></h2><Button v-if="form.format === 'html' && canEdit" variant="outline" size="sm" @click="convertBlocks">转换为内容块</Button></div>
        <template v-if="form.format === 'blocks'">
          <details v-for="(block, i) in form.blocks" :key="blockId(block)" open class="mb-3 rounded-lg border">
            <summary class="flex cursor-pointer flex-wrap items-center justify-between gap-2 bg-muted/30 px-3 py-2"><span class="text-sm font-medium">{{ i + 1 }} · {{ blockLabel(block.type) }}</span><span v-if="canEdit" class="flex gap-0.5" @click.stop>
              <Button size="icon-sm" variant="ghost" :disabled="i === 0" aria-label="上移内容块" @click.prevent="moveBlock(i, -1)"><ArrowUp class="size-3.5" /></Button><Button size="icon-sm" variant="ghost" :disabled="i === form.blocks.length - 1" aria-label="下移内容块" @click.prevent="moveBlock(i, 1)"><ArrowDown class="size-3.5" /></Button><Button size="icon-sm" variant="ghost" :disabled="form.blocks.length >= 100" aria-label="复制内容块" @click.prevent="duplicateBlock(i)"><Copy class="size-3.5" /></Button><Button size="icon-sm" variant="ghost" aria-label="删除内容块" @click.prevent="form.blocks.splice(i, 1)"><Trash2 class="size-3.5" /></Button>
            </span></summary><div class="p-3 sm:p-4" :class="!canEdit ? 'pointer-events-none opacity-70' : ''"><HrBlockFields :value="block" :fields="blockFields[block.type] ?? []" :id-prefix="`hr-block-${blockId(block)}`" /></div>
          </details>
          <EmptyState v-if="!form.blocks.length" title="添加第一个内容块" description="用文字、图片和结构化卡片组织文档。" />
          <div v-if="canEdit" class="mt-4 flex flex-wrap gap-2"><Select v-model="addType" :options="blockTypes" aria-label="新增内容块类型" class="w-44" /><Button variant="outline" :disabled="form.blocks.length >= 100" @click="addBlock"><Plus class="size-4" />添加块</Button></div>
        </template>
        <HrRichText v-else v-model="form.contentHtml" label="文档正文" :class="!canEdit ? 'pointer-events-none' : ''" />
      </section>
      <aside class="space-y-4" :class="!canEdit ? 'pointer-events-none opacity-70' : ''">
        <section class="panel space-y-4 p-4"><h2 class="text-sm font-semibold">文档设置</h2>
          <div><label for="hr-category" class="field-label">一级分类</label><Select id="hr-category" v-model="form.category" :options="hrCategories" /></div>
          <div><label for="hr-topic" class="field-label">制度主题</label><Select id="hr-topic" v-model="form.topic" :options="hrTopics" /></div>
          <div><label for="hr-version" class="field-label">草稿目标版本号</label><Input id="hr-version" v-model="form.draftVersion" maxlength="16" placeholder="v1.0" /></div>
          <div><label for="hr-intro" class="field-label">引言</label><Textarea id="hr-intro" v-model="form.intro" :rows="3" maxlength="500" /></div>
          <div><label for="hr-depts" class="field-label">可见部门（每行一个，留空为全员）</label><Textarea id="hr-depts" v-model="form.depts" :rows="2" /></div>
          <div><label for="hr-tags" class="field-label">标签（用逗号分隔，最多 10 个）</label><Input id="hr-tags" v-model="form.tagsText" /></div>
          <div><label for="hr-theme" class="field-label">文档主题</label><Select id="hr-theme" v-model="form.theme" :options="themes" /></div>
          <div class="flex items-center justify-between"><label for="hr-toc" class="text-xs">启用目录</label><Switch id="hr-toc" v-model="form.tocEnabled" /></div>
          <div v-if="form.tocEnabled"><label for="hr-toc-pos" class="field-label">目录位置</label><Select id="hr-toc-pos" v-model="form.tocPosition" :options="[{ value: 'left', label: '左侧' }, { value: 'right', label: '右侧' }]" /></div>
          <div class="flex items-center justify-between"><label for="hr-pdf" class="text-xs">允许下载 PDF</label><Switch id="hr-pdf" v-model="form.downloadPdfEnabled" /></div>
          <p v-if="document?.id" class="break-all text-xs text-muted-foreground">稳定地址：/article/{{ document.id }}<br v-if="document.slug" /><span v-if="document.slug">旧地址标识：{{ document.slug }}</span></p>
        </section>
        <details class="panel p-4"><summary class="cursor-pointer text-sm font-semibold">封面设置</summary><HrBlockFields class="mt-4" :value="form.cover" :fields="coverFields" id-prefix="hr-cover" /></details>
        <section class="panel p-4"><div class="flex items-center justify-between"><label for="hr-signoff" class="text-sm font-semibold">需要员工签收</label><Switch id="hr-signoff" v-model="form.requiresSignoff" /></div><details v-if="form.requiresSignoff" class="mt-4"><summary class="cursor-pointer text-xs text-muted-foreground">签收卡片文案</summary><HrBlockFields class="mt-3" :value="form.signoff" :fields="signoffFields" id-prefix="hr-signoff-card" /></details></section>
      </aside>
    </div>
  </div>

  <Dialog v-model:open="previewOpen" title="文档预览" size="xl"><article class="mx-auto max-w-3xl">
    <div class="mb-6 rounded-xl bg-primary/10 px-6 py-8" :class="form.cover.bg_type === 'dark' ? 'bg-foreground text-background' : ''"><img v-if="form.cover.bg_type === 'image' && hrSafeUrl(form.cover.image)" :src="hrSafeUrl(form.cover.image)" alt="文档封面" class="mb-5 max-w-full rounded-lg" /><p class="text-xs opacity-70">{{ form.cover.label_en }} {{ form.cover.label_cn }}</p><h1 class="my-3 text-3xl font-semibold">{{ form.cover.icon }} {{ form.title || '未填写标题' }}</h1><p class="leading-relaxed">{{ form.cover.desc || form.intro }}</p><div class="mt-5 flex flex-wrap gap-5 text-xs opacity-70"><span v-if="form.cover.date">执行日期：{{ form.cover.date }}</span><span v-if="form.cover.scope">适用范围：{{ form.cover.scope }}</span><span v-if="form.cover.dept">发布部门：{{ form.cover.dept }}</span></div></div>
    <HrBlockPreview v-if="form.format === 'blocks'" :blocks="form.blocks" /><HrHtmlContent v-else :html="form.contentHtml" />
    <div v-if="form.requiresSignoff" class="mt-8 rounded-lg border p-5"><h3 class="mb-3 font-semibold">{{ form.signoff.icon }} {{ form.signoff.title }}</h3><HrHtmlContent :html="form.signoff.lead" /><p class="my-3 text-sm text-muted-foreground">☐ {{ form.signoff.ack_label }}</p><Button disabled>{{ form.signoff.button_text }}</Button><HrHtmlContent class="mt-3 text-xs" :html="form.signoff.tip" /></div>
  </article></Dialog>

  <Dialog v-model:open="publishOpen" title="确认发布文档" description="发布后员工将看到当前草稿；发布版本会保留快照。" size="sm"><label for="hr-publish-version" class="field-label">发布版本号</label><Input id="hr-publish-version" v-model="publishVersion" maxlength="16" /><p v-if="document?.publishedAt" class="mt-3 text-xs text-muted-foreground">当前已发布版本：{{ document.version }}。正文有改动时，请使用新的版本号。</p><Callout v-if="publishError" class="mt-3" tone="destructive">{{ publishError }}</Callout><template #footer><Button variant="outline" :disabled="busy" @click="publishOpen = false">取消</Button><Button :loading="busy" @click="publish">确认发布</Button></template></Dialog>

  <Dialog v-model:open="historyOpen" title="版本历史" size="xl"><div class="flex flex-wrap gap-2"><Button :variant="!historyDraft ? 'secondary' : 'ghost'" size="sm" @click="historyDraft = false; loadHistory()">发布版本</Button><Button :variant="historyDraft ? 'secondary' : 'ghost'" size="sm" @click="historyDraft = true; loadHistory()">草稿快照</Button></div><Callout v-if="historyError" tone="destructive" class="mt-3">{{ historyError }}</Callout><div v-if="historyLoading" class="py-8 text-center text-sm text-muted-foreground">正在加载版本…</div><div v-else class="mt-4 grid gap-5 md:grid-cols-[220px_minmax(0,1fr)]"><div class="space-y-2"><div v-for="version in history" :key="version.id" class="rounded-lg border p-3"><Button variant="ghost" size="sm" class="h-auto w-full justify-start whitespace-normal text-left" @click="viewVersion(version)">{{ historyDraft ? version.title || '草稿快照' : version.version }}<br />{{ time(version.publishedAt || version.createdAt) }}</Button><p class="mt-1 text-xs text-muted-foreground">{{ version.publishedBy || version.savedBy }} {{ version.summary }}</p><Button v-if="historyDraft && canEdit" size="sm" variant="ghost" @click="requestDeleteDraft(version.id)">删除快照</Button></div><p v-if="!history.length" class="py-5 text-xs text-muted-foreground">暂无版本记录。</p></div><div class="min-w-0"><template v-if="snapshot"><div class="mb-4 flex items-center justify-between gap-3"><h3 class="font-medium">{{ snapshot.title }}</h3><Button v-if="canEdit && !document?.deletedAt" variant="outline" size="sm" @click="applySnapshot">载入编辑器</Button></div><p class="mb-4 text-xs text-muted-foreground">载入后需保存草稿，再次发布才会更新员工看到的内容。</p><HrBlockPreview v-if="snapshot.contentBlocks != null" :blocks="snapshot.contentBlocks" /><HrHtmlContent v-else :html="snapshot.contentHtml" /></template><EmptyState v-else title="选择一个版本预览" /></div></div></Dialog>

  <Dialog v-model:open="actionOpen" :title="actionTitle" size="sm"><p class="text-sm text-muted-foreground">{{ actionDescription }}</p><template v-if="actionKey === 'offline'"><label for="hr-offline-reason" class="field-label mt-4">下架原因</label><Textarea id="hr-offline-reason" v-model="offlineReason" maxlength="200" :rows="3" /></template><Callout v-if="actionError" tone="destructive" class="mt-3">{{ actionError }}</Callout><template #footer><Button variant="outline" :disabled="busy" @click="actionOpen = false">取消</Button><Button :variant="actionKey === 'delete' || actionKey === 'deleteDraft' ? 'destructive' : 'default'" :loading="busy" @click="performAction">确认</Button></template></Dialog>
  <ConfirmDialog v-model:open="discardOpen" title="放弃未保存的修改？" description="返回文档列表会丢失当前编辑内容。您也可以取消并保存草稿。" confirm-text="放弃修改" tone="destructive" @confirm="discardOpen = false; editing = false; load()" />
  <PortalAiTextDialog v-model:open="layoutOpen" mode="layout" :text="layoutText" description="检查生成的内容块；采用后替换编辑器正文，保存草稿和发布由您另外操作。" @apply="applyLayout" />
  <PortalContentPushDialog v-model:open="pushOpen" kind="hrdoc" :id="pushDocument?.id ?? 0" :title="pushDocument?.title ?? ''" :target-key="pushTargetKey" :initial-departments="pushDocument?.deptScope ?? []" />
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute } from 'vue-router'
import { Plus, Search, ArrowLeft, Eye, History, Save, ArrowUp, ArrowDown, Copy, Trash2 } from '@lucide/vue'
import PageHeader from '../../ui/PageHeader.vue'
import Button from '../../ui/Button.vue'
import Input from '../../ui/Input.vue'
import Textarea from '../../ui/Textarea.vue'
import Select from '../../ui/Select.vue'
import Switch from '../../ui/Switch.vue'
import Badge from '../../ui/Badge.vue'
import Callout from '../../ui/Callout.vue'
import EmptyState from '../../ui/EmptyState.vue'
import Pagination from '../../ui/Pagination.vue'
import Dialog from '../../ui/Dialog.vue'
import ConfirmDialog from '../../ui/ConfirmDialog.vue'
import HrBlockFields from '../../components/portal/HrBlockFields.vue'
import HrBlockPreview from '../../components/portal/HrBlockPreview.vue'
import HrHtmlContent from '../../components/portal/HrHtmlContent.vue'
import HrRichText from '../../components/portal/HrRichText.vue'
import { flushPortalRichTextImages } from '../../lib/portal-richtext'
import PortalAiTextDialog from '../../components/portal/PortalAiTextDialog.vue'
import PortalContentPushDialog from '../../components/portal/PortalContentPushDialog.vue'
import { hrApi, hrCategories, hrTopics, blockTypes, blockFields, newBlock, hrSafeUrl, type HrDocument, type HrBlock, type HrVersion, type BlockField } from '../../lib/portal-hr'
import { session } from '../../lib/session'
import { errText, toast } from '../../lib/toast'

const canEdit = computed(() => session.has('can_edit_hr_documents'))
const route = useRoute()
const rows = ref<HrDocument[]>([]), page = ref(1), total = ref(0), loading = ref(false), loadError = ref(''), busy = ref(false)
const query = reactive({ q: '', category: '', status: '', deleted: 'false' })
const statuses = [{ value: '', label: '所有状态' }, { value: 'draft', label: '草稿' }, { value: 'published', label: '已发布' }, { value: 'offline', label: '已下架' }, { value: 'pending', label: '待审批' }, { value: 'rejected', label: '已驳回' }]
const editing = ref(false), document = ref<HrDocument | null>(null), baseline = ref(''), editorError = ref(''), discardOpen = ref(false)
function emptyForm() { return { title: '', category: 'policy', topic: 'admin', draftVersion: 'v1.0', intro: '', depts: '', tagsText: '', theme: '', tocEnabled: true, tocPosition: 'left', downloadPdfEnabled: false, requiresSignoff: false, coverImage: '', cover: { bg_type: 'gradient', image: '', label_en: '', label_cn: '', icon: '', desc: '', date: '', scope: '', dept: '', nav_tabs: [] } as Record<string, any>, signoff: { icon: '📝', title: '员工签收', lead: '', ack_label: '我确认已完整阅读并理解本文档全部内容', button_text: '我已阅读并同意', tip: '', signed_note: '签收记录已存档。' } as Record<string, any>, format: 'blocks', blocks: [] as HrBlock[], contentHtml: '' } }
const form = reactive(emptyForm())
const dirty = computed(() => editing.value && JSON.stringify(form) !== baseline.value)
const addType = ref('text'), previewOpen = ref(false), publishOpen = ref(false), publishVersion = ref(''), publishRevision = ref(''), publishError = ref('')
const layoutOpen = ref(false)
const pushOpen = ref(false), pushDocument = ref<HrDocument | null>(null)
const pushTargetKey = computed(() => pushDocument.value ? `${pushDocument.value.slug || pushDocument.value.legacySlug || `article:${pushDocument.value.id}`}:${pushDocument.value.version || 'draft'}` : '')
const layoutText = computed(() => { if (form.format !== 'blocks') return new DOMParser().parseFromString(form.contentHtml, 'text/html').body.textContent ?? ''; const lines: string[] = []; function text(value: unknown) { if (typeof value === 'string') lines.push(new DOMParser().parseFromString(value, 'text/html').body.textContent ?? ''); else if (Array.isArray(value)) value.forEach(text); else if (value && typeof value === 'object') Object.entries(value).forEach(([key, v]) => { if (!['type', 'image', 'color', 'bg', 'icon', 'button_url', 'slug'].includes(key)) text(v) }) }; form.blocks.forEach(text); return lines.join('\n\n') })
function applyLayout(result: string | HrBlock[]) { if (Array.isArray(result)) { form.blocks = JSON.parse(JSON.stringify(result)); form.format = 'blocks'; toast('排版已载入编辑器，请检查并保存草稿') } }
const themes = [{ value: '', label: '默认主题' }, ...['violet', 'blue', 'orange', 'green', 'slate', 'rose'].map((value, i) => ({ value, label: ['紫色', '蓝色', '橙色', '绿色', '石板', '玫红'][i]! }))]
const coverFields: BlockField[] = [{ key: 'bg_type', label: '背景风格', kind: 'select', options: [{ value: 'gradient', label: '主题色' }, { value: 'dark', label: '深色' }, { value: 'image', label: '图片' }] }, ...[['image', '封面图片地址'], ['label_en', '英文标识'], ['label_cn', '中文标识'], ['icon', '图标'], ['desc', '封面说明'], ['date', '执行日期'], ['scope', '适用范围'], ['dept', '发布部门']].map(([key, label]) => ({ key: key!, label: label!, ...(key === 'image' ? { kind: 'image' as const } : {}) })), { key: 'nav_tabs', label: '导航标签（每行一个，最多 8 个）', kind: 'lines' }]
const signoffFields: BlockField[] = [['icon', '图标'], ['title', '签收标题'], ['ack_label', '确认文字'], ['button_text', '按钮文字'], ['signed_note', '已签说明']].map(([key, label]) => ({ key: key!, label: label! }))
signoffFields.push({ key: 'lead', label: '签收引言', kind: 'rich' }, { key: 'tip', label: '签收提示', kind: 'rich' })
let request = 0
async function load() { const current = ++request; loading.value = true; loadError.value = ''; try { const res = await hrApi.page({ pageNo: page.value, pageSize: 20, ...query, category: query.category || undefined, status: query.status || undefined, q: query.q || undefined }); if (current === request) { rows.value = res.list; total.value = res.total } } catch (e) { if (current === request) { loadError.value = errText(e); rows.value = []; total.value = 0 } } finally { if (current === request) loading.value = false } }
function time(value?: string) { return value ? value.replace('T', ' ').slice(0, 16) : '—' }
function categoryLabel(value: string) { return hrCategories.find(v => v.value === value)?.label ?? value }
function stateLabel(doc: HrDocument) { return doc.deletedAt ? '回收站' : doc.offlineAt ? '已下架' : ({ draft: '草稿', pending: '待审批', published: '已发布', rejected: '已驳回' } as Record<string, string>)[doc.status] ?? doc.status }
function blockLabel(value: string) { return blockTypes.find(v => v.value === value)?.label ?? value }
const blockIds = new WeakMap<object, number>(); let nextBlockId = 0
function blockId(block: object) { if (!blockIds.has(block)) blockIds.set(block, ++nextBlockId); return blockIds.get(block)! }
function startCreate() { document.value = null; Object.assign(form, emptyForm()); baseline.value = JSON.stringify(form); editorError.value = ''; editing.value = true }
function populate(doc: HrDocument) {
  document.value = doc
  Object.assign(form, emptyForm())
  for (const key of ['title', 'requiresSignoff', 'intro', 'coverImage', 'theme', 'tocEnabled', 'tocPosition', 'downloadPdfEnabled']) { const draft = 'draft' + key[0]!.toUpperCase() + key.slice(1); (form as Record<string, any>)[key] = doc[draft] ?? doc[key] ?? (form as Record<string, any>)[key] }
  form.category = doc.category; form.topic = doc.topic || 'admin'; form.draftVersion = doc.draftVersion ?? doc.version ?? 'v1.0'
  form.depts = (doc.draftDeptScope ?? doc.deptScope ?? []).join('\n'); form.tagsText = (doc.draftTags ?? doc.tags ?? []).join(', ')
  const cover = doc.draftCover ?? doc.cover ?? {}
  form.cover = { ...form.cover, ...cover }; if (!Object.keys(cover).length && form.coverImage) { form.cover.image = form.coverImage; form.cover.bg_type = 'image' }
  form.signoff = { ...form.signoff, ...(doc.draftSignoff ?? doc.signoff ?? {}) }
  const blocks = doc.draftBlocks ?? (doc.draftContent != null ? null : doc.contentBlocks)
  form.format = blocks != null ? 'blocks' : 'html'; form.blocks = JSON.parse(JSON.stringify(blocks ?? [])); form.contentHtml = doc.draftContent ?? doc.contentHtml ?? ''
  baseline.value = JSON.stringify(form)
}
async function openDocument(id: number) { busy.value = true; try { populate(await hrApi.get(id)); editorError.value = ''; editing.value = true } catch (e) { toast(errText(e), 'bad') } finally { busy.value = false } }
function closeEditor() { if (dirty.value) discardOpen.value = true; else { editing.value = false; load() } }
function addBlock() { if (form.blocks.length < 100) form.blocks.push(newBlock(addType.value)) }
function moveBlock(i: number, delta: number) { const b = form.blocks.splice(i, 1)[0]!; form.blocks.splice(i + delta, 0, b) }
function duplicateBlock(i: number) { if (form.blocks.length < 100) form.blocks.splice(i + 1, 0, JSON.parse(JSON.stringify(form.blocks[i]))) }
function convertBlocks() { form.blocks = [{ type: 'text', text: form.contentHtml }]; form.format = 'blocks' }
async function saveDraft(): Promise<HrDocument> {
  await flushPortalRichTextImages()
  if (!form.title.trim()) throw new Error('请填写文档标题')
  const common = { title: form.title.trim(), category: form.category, topic: form.topic, intro: form.intro, coverImage: form.coverImage, theme: form.theme, tags: form.tagsText.split(/[,，]/).map(v => v.trim()).filter(Boolean), deptScope: form.depts.split('\n').map(v => v.trim()).filter(Boolean), requiresSignoff: form.requiresSignoff, tocEnabled: form.tocEnabled, tocPosition: form.tocPosition, downloadPdfEnabled: form.downloadPdfEnabled, cover: form.cover, signoff: form.signoff }
  if (!document.value?.id) { document.value = await hrApi.create({ ...common, version: form.draftVersion, contentHtml: '' }) }
  const blocks = JSON.parse(JSON.stringify(form.blocks)) as HrBlock[]
  for (const b of blocks) if (b.type === 'docref') { if (!b.document_id) delete b.document_id; if (!b.slug) delete b.slug }
  await hrApi.update({ ...common, id: document.value.id, draftVersion: form.draftVersion, ...(form.format === 'blocks' ? { blocks } : { contentHtml: form.contentHtml }) })
  const saved = await hrApi.get(document.value.id); populate(saved); return saved
}
function message(e: unknown) { return e instanceof Error ? e.message : errText(e) }
async function save() { if (busy.value) return; busy.value = true; editorError.value = ''; try { await saveDraft(); toast('草稿已保存') } catch (e) { editorError.value = message(e) } finally { busy.value = false } }
async function preparePublish() { if (busy.value) return; busy.value = true; editorError.value = ''; try { const saved = await saveDraft(); publishVersion.value = form.draftVersion; publishRevision.value = saved.revision ?? ''; publishError.value = ''; publishOpen.value = true } catch (e) { editorError.value = message(e) } finally { busy.value = false } }
async function publish() { if (!document.value?.id || busy.value) return; busy.value = true; publishError.value = ''; try { if (!publishRevision.value) throw new Error('缺少发布确认凭据，请关闭弹窗重新发起发布'); await hrApi.publish(document.value.id, publishRevision.value, publishVersion.value); populate(await hrApi.get(document.value.id)); publishOpen.value = false; toast('文档已发布') } catch (e) { publishError.value = message(e) } finally { busy.value = false } }
const historyOpen = ref(false), historyDraft = ref(false), historyLoading = ref(false), historyError = ref(''), history = ref<HrVersion[]>([]), snapshot = ref<HrVersion | null>(null)
let historyRequest = 0
async function openHistory() { historyOpen.value = true; historyDraft.value = false; await loadHistory() }
async function loadHistory() { if (!document.value) return; const req = ++historyRequest; snapshot.value = null; historyLoading.value = true; historyError.value = ''; try { const result = await hrApi.versions(document.value.id, historyDraft.value); if (req === historyRequest) history.value = result } catch (e) { if (req === historyRequest) { history.value = []; historyError.value = message(e) } } finally { if (req === historyRequest) historyLoading.value = false } }
async function viewVersion(version: HrVersion) { if (!document.value) return; try { snapshot.value = await hrApi.version(document.value.id, historyDraft.value ? version.id : version.version!, historyDraft.value) } catch (e) { historyError.value = message(e) } }
function applySnapshot() { if (!snapshot.value) return; form.format = snapshot.value.contentBlocks != null ? 'blocks' : 'html'; form.blocks = JSON.parse(JSON.stringify(snapshot.value.contentBlocks ?? [])); form.contentHtml = snapshot.value.contentHtml ?? ''; if (snapshot.value.title) form.title = snapshot.value.title; historyOpen.value = false; toast('版本已载入编辑器，请保存草稿') }
const actionOpen = ref(false), actionTitle = ref(''), actionKey = ref(''), actionRow = ref<HrDocument | null>(null), offlineReason = ref(''), actionError = ref(''), draftVersionId = ref(0)
const actionDescription = computed(() => actionKey.value === 'deleteDraft' ? '此草稿快照删除后无法恢复，当前编辑内容不受影响。' : actionKey.value === 'offline' ? `《${actionRow.value?.title ?? ''}》下架后员工将无法访问，内容和版本记录仍保留。` : `确认${actionTitle.value}《${actionRow.value?.draftTitle ?? actionRow.value?.title ?? ''}》？`)
function requestAction(row: HrDocument, key: string, title: string) { actionRow.value = row; actionKey.value = key; actionTitle.value = title; actionError.value = ''; offlineReason.value = ''; actionOpen.value = true }
function requestDeleteDraft(id: number) { draftVersionId.value = id; actionKey.value = 'deleteDraft'; actionTitle.value = '删除草稿快照'; actionError.value = ''; actionOpen.value = true }
async function performAction() { if (busy.value) return; busy.value = true; actionError.value = ''; try { if (actionKey.value === 'deleteDraft' && document.value) { await hrApi.deleteDraft(document.value.id, draftVersionId.value); await loadHistory() } else if (actionRow.value) { if (actionKey.value === 'delete') await hrApi.remove(actionRow.value.id); else await hrApi.action(actionRow.value.id, actionKey.value, actionKey.value === 'offline' ? { reason: offlineReason.value } : undefined); await load() } actionOpen.value = false; toast('操作已完成') } catch (e) { actionError.value = message(e) } finally { busy.value = false } }
function beforeUnload(event: BeforeUnloadEvent) { if (dirty.value) { event.preventDefault(); event.returnValue = '' } }
const confirmLeave = () => !dirty.value || window.confirm('文档有未保存的修改，确认离开并放弃修改？')
onBeforeRouteLeave(confirmLeave); onBeforeRouteUpdate(confirmLeave)
async function openLinkedDocument() {
  if (route.query.mode === 'new' && canEdit.value) { startCreate(); return }
  const rawId = String(route.query.id ?? ''), legacy = String(route.query.legacySlug ?? '')
  let id = /^[1-9]\d{0,14}$/.test(rawId) ? Number(rawId) : 0
  if (!id && /^[a-z0-9][a-z0-9-]{1,63}$/.test(legacy)) { try { let no = 1; let count = 0; do { const res = await hrApi.page({ pageNo: no++, pageSize: 100 }); const found = res.list.find(doc => doc.legacySlug === legacy); if (found) { id = found.id; break } count += res.list.length; if (!res.list.length || count >= res.total) break } while (!id); if (!id) loadError.value = '未找到旧链接对应的文档，请在文档库中搜索标题。' } catch (e) { loadError.value = errText(e) } }
  if (id) { await openDocument(id); if (editing.value && route.query.mode === 'preview') previewOpen.value = true }
}
onMounted(() => { load(); openLinkedDocument(); window.addEventListener('beforeunload', beforeUnload) })
onBeforeUnmount(() => window.removeEventListener('beforeunload', beforeUnload))
</script>
