<template>
  <div class="min-w-0 space-y-4">
    <PageHeader title="培训管理" description="维护培训课程、课时和学习范围，发布后员工可在培训中心学习。">
      <template #actions>
        <Button v-if="canManage" variant="outline" size="sm" :disabled="busy" @click="pending = { type: 'scan' }"><FolderSync />从 NAS 扫描导入</Button>
        <Button v-if="canManage" size="sm" @click="openCourse()"><Plus />新建课程</Button>
      </template>
    </PageHeader>

    <div v-if="canViewStats" class="grid gap-3 sm:grid-cols-3">
      <StatCard title="已发布课程" :value="stats ? stats.totalCourses : '–'" :icon="BookOpen" />
      <StatCard title="开始学习人数" :value="stats ? stats.usersStarted : '–'" :icon="Users" tone="info" />
      <StatCard title="累计完成次数" :value="stats ? stats.completions : '–'" :icon="GraduationCap" tone="success" />
    </div>
    <Callout v-if="statsError" tone="warning" title="培训统计加载失败">{{ statsError }}<template #action><Button variant="ghost" size="sm" @click="loadStats">重试</Button></template></Callout>

    <SegmentedTabs v-model="tab" :items="[{ value: 'courses', label: '课程与课时' }, { value: 'categories', label: '培训分类' }]" />
    <PortalCategoryPanel v-if="tab === 'categories'" scope="training.course" @changed="refresh" />
    <section v-else class="panel overflow-hidden">
      <div class="table-caption"><h2 class="panel-title">课程列表</h2><span v-if="!loadError" class="text-xs text-muted-foreground">{{ loading ? '正在加载…' : `共 ${total} 门课程` }}</span></div>
      <form class="table-toolbar flex-wrap gap-2" @submit.prevent="search">
        <Input v-model="filter.q" class="h-9 w-full text-[13px] sm:w-60" aria-label="课程关键词" placeholder="搜索课程标题或简介" />
        <Select v-model="filter.status" size="sm" class="w-36" aria-label="课程状态" :options="[{ value: '', label: '全部状态' }, { value: 'draft', label: '草稿' }, { value: 'published', label: '已发布' }]" />
        <Select v-model="filter.category" size="sm" class="w-44" aria-label="课程分类" :options="[{ value: '', label: '全部分类' }, ...leafCategories.map(c => ({ value: c.name, label: c.name }))]" />
        <Button size="sm" type="submit" :disabled="loading"><Search />查询</Button>
        <Button variant="ghost" size="sm" :disabled="loading" @click="reset">重置</Button>
        <Button variant="ghost" class="ml-auto" size="icon-sm" aria-label="刷新课程" :disabled="loading" @click="refresh"><RefreshCw :class="loading ? 'animate-spin' : ''" /></Button>
      </form>
      <div v-if="canManage && selected.length" class="flex flex-wrap items-center gap-2 border-t bg-muted/40 px-5 py-3">
        <span class="text-xs text-muted-foreground">已选 {{ selected.length }} 门</span>
        <Button size="sm" variant="outline" :disabled="busy" @click="askBatch('publish')">批量发布</Button>
        <Button size="sm" variant="outline" :disabled="busy" @click="askBatch('unpublish')">批量下架</Button>
        <Button size="sm" variant="ghost" @click="selected = []">取消选择</Button>
      </div>
      <Callout v-if="loadError" tone="destructive" title="课程加载失败" class="m-5">{{ loadError }}<template #action><Button variant="outline" size="sm" @click="loadCourses">重试</Button></template></Callout>
      <div v-else-if="loading && !courses.length" class="space-y-2 border-t p-4"><Skeleton v-for="i in 5" :key="i" class="h-14 w-full" /></div>
      <EmptyState v-else-if="!courses.length" :title="applied.q || applied.status || applied.category ? '没有匹配的课程' : '还没有培训课程'" description="从一门课程开始，添加课时并设置学习范围。" :icon="BookOpen"><Button v-if="applied.q || applied.status || applied.category" size="sm" variant="outline" @click="reset">清除筛选</Button><Button v-else-if="canManage" size="sm" @click="openCourse()"><Plus />新建课程</Button></EmptyState>
      <div v-else class="max-h-[calc(100dvh-22rem)] overflow-auto border-t border-border/70">
        <table class="data-table min-w-[940px]">
          <thead><tr><th v-if="canManage" class="w-10"><Checkbox :model-value="allSelected" aria-label="选择本页课程" @update:model-value="toggleAll" /></th><th>课程</th><th>分类 / 讲师</th><th class="num">课时</th><th>学习范围</th><th>状态</th><th class="sticky-col text-right">操作</th></tr></thead>
          <tbody>
            <tr v-for="course in courses" :key="course.id" :data-selected="selected.includes(course.id)">
              <td v-if="canManage"><Checkbox :model-value="selected.includes(course.id)" :aria-label="`选择${course.title}`" @update:model-value="toggleSelected(course.id, $event)" /></td>
              <td class="max-w-72"><p class="truncate font-medium" :title="course.title">{{ course.title }}</p><p v-if="course.description" class="mt-1 truncate text-xs text-muted-foreground" :title="course.description">{{ course.description }}</p><Badge v-if="course.level === 'required'" class="mt-1" variant="warning">必修</Badge></td>
              <td><p>{{ course.category }}</p><p class="mt-1 text-xs text-muted-foreground">{{ course.instructor || '未设置讲师' }}</p></td>
              <td class="num"><p>{{ course.lessonCount }} 节</p><p class="mt-1 max-w-48 text-xs text-muted-foreground">{{ formatKinds(course) }}</p></td>
              <td class="max-w-48 text-xs text-muted-foreground"><p class="truncate" :title="course.deptScope?.join('、')">{{ course.deptScope?.length ? course.deptScope.join('、') : '全员' }}</p></td>
              <td><Badge :variant="course.status === 'published' ? 'success' : 'secondary'">{{ course.status === 'published' ? '已发布' : '草稿' }}</Badge></td>
              <td class="sticky-col"><div class="flex justify-end gap-1 whitespace-nowrap">
                <Button size="sm" variant="ghost" :disabled="busy" @click="openLessons(course)">课时</Button>
                <template v-if="canManage">
                  <Button size="sm" variant="ghost" :disabled="busy" @click="openCourse(course)">编辑</Button>
                  <DropdownMenu>
                    <template #trigger><Button variant="ghost" size="icon-sm" :disabled="busy" :aria-label="`${course.title}的更多操作`"><MoreHorizontal /></Button></template>
                    <DropdownMenuItem :disabled="busy || (course.status !== 'published' && !course.lessonCount)" @select="pending = { type: 'state', course, action: course.status === 'published' ? 'unpublish' : 'publish' }">{{ course.status === 'published' ? '下架课程' : '发布课程' }}</DropdownMenuItem>
                    <DropdownMenuItem destructive :disabled="busy" @select="pending = { type: 'deleteCourse', course }">删除课程</DropdownMenuItem>
                  </DropdownMenu>
                </template>
              </div></td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination class="border-t px-5 py-3" :page="page" :page-size="pageSize" :total="total" @update:page="changePage" />
    </section>

    <Dialog :open="courseOpen" :title="courseForm.id ? '编辑课程' : '新建课程'" size="lg" @update:open="!busy && (courseOpen = $event)">
      <form id="portal-training-course-form" class="grid gap-4 sm:grid-cols-2" @submit.prevent="saveCourse">
        <Callout v-if="formError" class="sm:col-span-2" tone="destructive">{{ formError }}</Callout>
        <div class="sm:col-span-2"><label for="training-course-title" class="field-label">课程名称 *</label><Input id="training-course-title" v-model="courseForm.title" required maxlength="200" /></div>
        <div><label for="training-course-category" class="field-label">课程小类 *</label><Input id="training-course-category" v-model="courseForm.category" list="training-leaf-categories" required maxlength="100" placeholder="选择或输入小类名称" /><datalist id="training-leaf-categories"><option v-for="category in leafCategories" :key="category.id" :value="category.name" /></datalist></div>
        <div><label for="training-course-level" class="field-label">学习要求</label><Select id="training-course-level" v-model="courseForm.level" :options="[{ value: 'elective', label: '选修' }, { value: 'required', label: '必修' }]" /></div>
        <div><label for="training-course-instructor" class="field-label">讲师</label><Input id="training-course-instructor" v-model="courseForm.instructor" maxlength="100" /></div>
        <div><label for="training-course-order" class="field-label">排序</label><Input id="training-course-order" v-model="courseForm.sortOrder" type="number" min="0" max="1000000000" /></div>
        <div class="sm:col-span-2"><label for="training-course-description" class="field-label">课程简介</label><Textarea id="training-course-description" v-model="courseForm.description" :rows="4" /></div>
        <div class="sm:col-span-2"><label for="training-course-cover" class="field-label">封面图片地址</label><Input id="training-course-cover" v-model="courseForm.coverImage" type="url" maxlength="255" placeholder="https://…" /></div>
        <div class="sm:col-span-2"><label for="training-course-scope" class="field-label">可见部门</label><Textarea id="training-course-scope" v-model="courseForm.deptText" :rows="3" placeholder="每行一个部门名称；留空表示全员可见" /><p class="mt-1 text-xs text-muted-foreground">使用组织中的完整部门名称，每行一个；留空为全员可见。</p></div>
        <Callout v-if="!courseForm.id" class="sm:col-span-2">课程将保存为草稿，添加至少一节课时后可发布。</Callout>
      </form>
      <template #footer><Button variant="outline" :disabled="busy" @click="courseOpen = false">取消</Button><Button type="submit" form="portal-training-course-form" :loading="busy">保存课程</Button></template>
    </Dialog>

    <Dialog :open="!!lessonCourse" :title="`课时管理 · ${lessonCourse?.title ?? ''}`" size="xl" @update:open="!busy && closeLessons($event)">
      <div class="min-w-0 space-y-4">
        <div class="flex flex-wrap items-center justify-between gap-2"><p class="text-xs text-muted-foreground">调整标题与排序；更换课件时可删除课时后重新上传。</p><Button v-if="canManage" size="sm" :disabled="busy || lessonsLoading" @click="openLesson()"><Plus />添加课时</Button></div>
        <Callout v-if="lessonError" tone="destructive" title="课时加载失败">{{ lessonError }}<template #action><Button size="sm" variant="outline" @click="loadLessons">重试</Button></template></Callout>
        <p v-else-if="lessonsLoading" class="py-10 text-center text-sm text-muted-foreground">正在加载课时…</p>
        <EmptyState v-else-if="!lessons.length" title="这门课程还没有课时" description="上传视频、PDF，或添加图文、卡片与外链。"><Button v-if="canManage" size="sm" @click="openLesson()"><Plus />添加课时</Button></EmptyState>
        <div v-else class="overflow-x-auto rounded-lg border">
          <table class="data-table min-w-[700px]"><thead><tr><th class="num">顺序</th><th>课时</th><th>形式</th><th>附加信息</th><th class="text-right">操作</th></tr></thead>
            <tbody><tr v-for="lesson in lessons" :key="lesson.id"><td class="num">{{ lesson.sortOrder }}</td><td class="font-medium">{{ lesson.title }}</td><td><Badge variant="secondary">{{ kindLabel(lesson.kind) }}</Badge></td><td class="text-xs text-muted-foreground">{{ lessonInfo(lesson) }}</td><td class="sticky-col"><div class="flex justify-end gap-1 whitespace-nowrap"><Button variant="ghost" size="sm" :disabled="busy" @click="preview(lesson)">查看课件</Button><Button v-if="canManage" variant="ghost" size="sm" :disabled="busy" @click="openLesson(lesson)">编辑</Button><DropdownMenu v-if="canManage"><template #trigger><Button variant="ghost" size="icon-sm" :disabled="busy" :aria-label="`${lesson.title}的更多操作`"><MoreHorizontal /></Button></template><DropdownMenuItem destructive :disabled="busy" @select="pending = { type: 'deleteLesson', lesson }">删除课时</DropdownMenuItem></DropdownMenu></div></td></tr></tbody>
          </table>
        </div>
      </div>
      <template #footer><Button variant="outline" :disabled="busy" @click="closeLessons(false)">关闭</Button></template>
    </Dialog>

    <Dialog :open="lessonOpen" :title="lessonForm.id ? '编辑课时' : '添加课时'" size="lg" @update:open="!busy && !uploading && (lessonOpen = $event)">
      <form id="portal-training-lesson-form" class="space-y-4" @submit.prevent="saveLesson">
        <Callout v-if="lessonSaveError" tone="destructive">{{ lessonSaveError }}</Callout>
        <div><label for="training-lesson-title" class="field-label">课时标题 *</label><Input id="training-lesson-title" v-model="lessonForm.title" required maxlength="200" /></div>
        <div><label for="training-lesson-order" class="field-label">排序</label><Input id="training-lesson-order" v-model="lessonForm.sortOrder" type="number" min="0" max="1000000000" /></div>
        <template v-if="!lessonForm.id">
          <div><label for="training-lesson-kind" class="field-label">课件形式</label><Select id="training-lesson-kind" v-model="lessonForm.kind" :options="trainingKinds" :disabled="uploading" @update:model-value="clearUpload" /></div>
          <template v-if="lessonForm.kind === 'video' || lessonForm.kind === 'pdf'">
            <div><label for="training-lesson-file" class="field-label">上传{{ lessonForm.kind === 'video' ? 'MP4 视频' : 'PDF' }} *</label><Input id="training-lesson-file" type="file" :accept="lessonForm.kind === 'video' ? '.mp4' : '.pdf'" :disabled="uploading" @change="uploadFile" /><p class="mt-1 text-xs text-muted-foreground">上传完成后点击「保存课时」。文件直接上传到课件存储。</p></div>
            <div v-if="uploading || uploadedName" class="rounded-lg border bg-muted/30 p-3 text-xs"><p>{{ uploading ? `正在上传 ${uploadProgress}%` : `已上传：${uploadedName}` }}</p><div v-if="uploading" class="mt-2 h-1.5 overflow-hidden rounded-full bg-muted"><div class="h-full bg-primary transition-all" :style="{ width: `${uploadProgress}%` }" /></div><Button v-if="uploading" variant="ghost" size="sm" class="mt-2" @click="cancelUpload">取消上传</Button></div>
          </template>
          <div v-else-if="lessonForm.kind === 'link'"><label for="training-lesson-url" class="field-label">课件链接 *</label><Input id="training-lesson-url" v-model="lessonForm.url" type="url" required maxlength="500" placeholder="https://…" /></div>
          <template v-else>
            <div v-if="lessonForm.kind === 'article'"><label for="training-article-key" class="field-label">NAS 中的 HTML 课件路径</label><Input id="training-article-key" v-model="lessonForm.objectKey" maxlength="500" placeholder="例如 培训/课程/article.html" /><p class="mt-1 text-xs text-muted-foreground">可填写 HTML 路径，或在下方填写图片路径，两者选一。</p></div>
            <div><label for="training-pages" class="field-label">NAS 中的图片路径{{ lessonForm.kind === 'cards' ? ' *' : '' }}</label><Textarea id="training-pages" v-model="lessonForm.pagesText" :required="lessonForm.kind === 'cards'" :rows="5" placeholder="每行一个图片文件路径，按阅读顺序填写" /></div>
            <div v-if="lessonForm.kind === 'article' && lessonForm.objectKey.trim()"><label for="training-article-pdf" class="field-label">配套 PDF 路径</label><Input id="training-article-pdf" v-model="lessonForm.pdfKey" maxlength="500" placeholder="可留空" /></div>
          </template>
        </template>
      </form>
      <template #footer><Button variant="outline" :disabled="busy || uploading" @click="lessonOpen = false">取消</Button><Button type="submit" form="portal-training-lesson-form" :loading="busy" :disabled="uploading">保存课时</Button></template>
    </Dialog>

    <Dialog :open="!!media" title="查看课件" size="lg" @update:open="!$event && (media = null)">
      <div v-if="media" class="space-y-3">
        <p class="font-medium">{{ media.title }}</p>
        <p class="text-xs text-muted-foreground">课件链接有有效期；过期后请重新打开。</p>
        <a v-if="safeMediaUrl(media.url)" :href="safeMediaUrl(media.url)" target="_blank" rel="noopener noreferrer" class="inline-flex items-center gap-2 text-sm text-primary underline underline-offset-4"><ExternalLink class="size-4" />打开课件</a>
        <a v-if="safeMediaUrl(media.pdfUrl)" :href="safeMediaUrl(media.pdfUrl)" target="_blank" rel="noopener noreferrer" class="ml-4 text-sm text-primary underline underline-offset-4">下载配套 PDF</a>
        <div v-if="media.pages?.length" class="space-y-3"><img v-for="(src, i) in media.pages" :key="src" :src="safeMediaUrl(src)" :alt="`课件第 ${i + 1} 页`" loading="lazy" class="w-full rounded border" /></div>
        <Callout v-if="!safeMediaUrl(media.url) && !media.pages?.length" tone="warning">课件未返回可用地址。</Callout>
      </div>
    </Dialog>

    <Dialog :open="!!batchResult || !!importResult" :title="batchResult ? '批量操作结果' : 'NAS 导入结果'" size="lg" @update:open="closeResult">
      <div v-if="batchResult" class="space-y-3"><p class="text-sm">成功处理 {{ batchResult.published ?? batchResult.unpublished ?? 0 }} 门课程；跳过 {{ batchResult.skipped.length }} 门。</p><ul v-if="batchResult.skipped.length" class="space-y-2 text-sm"><li v-for="row in batchResult.skipped" :key="row.id" class="rounded border p-3">{{ row.title || `课程 #${row.id}` }}<span class="ml-2 text-muted-foreground">{{ skipReason(row.reason) }}</span></li></ul></div>
      <div v-if="importResult" class="space-y-3"><p class="text-sm leading-7">扫描到 {{ importResult.coursesSeen }} 门课程，新建 {{ importResult.coursesCreated }} 门课程、{{ importResult.categoriesCreated }} 个分类；新建 {{ importResult.lessonsCreated }} 节课时，更新 {{ importResult.lessonsUpdated }} 节。</p><Callout>导入课程保存为草稿，请核对课程与课时后发布。</Callout><details v-if="importResult.skipped.length" class="rounded border p-3 text-xs"><summary class="cursor-pointer">查看 {{ importResult.skipped.length }} 个跳过的文件</summary><ul class="mt-3 space-y-1 break-all"><li v-for="key in importResult.skipped" :key="key">{{ key }}</li></ul></details></div>
    </Dialog>
    <ConfirmDialog :open="!!pending" :title="confirmation.title" :description="confirmation.description" :tone="confirmation.destructive ? 'destructive' : 'default'" :busy="busy" @update:open="!busy && !$event && (pending = null)" @confirm="confirmAction" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { BookOpen, ExternalLink, FolderSync, GraduationCap, MoreHorizontal, Plus, RefreshCw, Search, Users } from '@lucide/vue'
import { categoryApi, kindLabel, safeMediaUrl, trainingApi, trainingKinds, type PortalCategory, type TrainingBatchResult, type TrainingCourse, type TrainingImportResult, type TrainingLesson, type TrainingStats } from '../../lib/portal-training'
import { session } from '../../lib/session'
import { errText, toast } from '../../lib/toast'
import PortalCategoryPanel from '../../components/portal/PortalCategoryPanel.vue'
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
import PageHeader from '../../ui/PageHeader.vue'
import Pagination from '../../ui/Pagination.vue'
import SegmentedTabs from '../../ui/SegmentedTabs.vue'
import Select from '../../ui/Select.vue'
import StatCard from '../../ui/StatCard.vue'
import Textarea from '../../ui/Textarea.vue'

const canManage = computed(() => session.has('can_manage_training'))
const canViewStats = computed(() => session.has('can_view_dashboards'))
const tab = ref('courses'), courses = ref<TrainingCourse[]>([]), categories = ref<PortalCategory[]>([])
const stats = ref<TrainingStats | null>(null), statsError = ref('')
const loading = ref(false), loadError = ref(''), busy = ref(false), page = ref(1), total = ref(0), pageSize = 20
const filter = reactive({ q: '', status: '', category: '' }), applied = ref({ ...filter }), selected = ref<number[]>([])
let courseRequest = 0, lessonRequest = 0
const leafCategories = computed(() => categories.value.filter(c => c.depth === 2))
const allSelected = computed(() => courses.value.length > 0 && courses.value.every(c => selected.value.includes(c.id)))
const courseOpen = ref(false), formError = ref('')
const courseForm = reactive({ id: null as number | null, title: '', category: '', level: 'elective', instructor: '', sortOrder: '0', description: '', coverImage: '', deptText: '' })
const lessonCourse = ref<TrainingCourse | null>(null), lessons = ref<TrainingLesson[]>([]), lessonsLoading = ref(false), lessonError = ref('')
const lessonOpen = ref(false), lessonSaveError = ref('')
const lessonForm = reactive({ id: null as number | null, title: '', sortOrder: '0', kind: 'video', objectKey: '', pdfKey: '', pagesText: '', url: '' })
const uploading = ref(false), uploadProgress = ref(0), uploadedName = ref('')
let uploadRequest: XMLHttpRequest | null = null
const media = ref<{ title: string; url?: string; pdfUrl?: string; pages?: string[] } | null>(null)
const batchResult = ref<TrainingBatchResult | null>(null), importResult = ref<TrainingImportResult | null>(null)
type Pending = { type: 'scan' } | { type: 'deleteCourse'; course: TrainingCourse } | { type: 'deleteLesson'; lesson: TrainingLesson } | { type: 'state'; course: TrainingCourse; action: 'publish' | 'unpublish' } | { type: 'batch'; ids: number[]; action: 'publish' | 'unpublish' }
const pending = ref<Pending | null>(null)
const confirmation = computed(() => {
  const action = pending.value
  if (!action) return { title: '', description: '', destructive: false }
  switch (action.type) {
    case 'scan': return { title: '从 NAS 扫描导入', description: '扫描培训存储目录并导入课程、课时。已有课程不会重复创建，新课程保存为草稿。确认开始？', destructive: false }
    case 'deleteCourse': return { title: '删除课程', description: `删除「${action.course.title}」及其所有课时和课时学习进度？删除后无法恢复。`, destructive: true }
    case 'deleteLesson': return { title: '删除课时', description: `删除「${action.lesson.title}」及其学习进度？删除后无法恢复。`, destructive: true }
    case 'state': return { title: action.action === 'publish' ? '发布课程' : '下架课程', description: action.action === 'publish' ? `发布「${action.course.title}」后，学习范围内的员工可浏览学习。` : `下架「${action.course.title}」后，课程将回到草稿状态。`, destructive: false }
    case 'batch': return { title: action.action === 'publish' ? '批量发布' : '批量下架', description: `确认${action.action === 'publish' ? '发布' : '下架'}所选 ${action.ids.length} 门课程？无法处理的课程会列在结果中。`, destructive: false }
  }
})
function errorMessage(e: unknown) { return e instanceof Error ? e.message : errText(e) }
async function loadCourses() {
  const seq = ++courseRequest; loading.value = true; loadError.value = ''
  try { const data = await trainingApi.page({ pageNo: page.value, pageSize, ...applied.value }); if (seq === courseRequest) { courses.value = data.list; total.value = data.total; selected.value = [] } }
  catch (e) { if (seq === courseRequest) { courses.value = []; total.value = 0; loadError.value = errText(e) } }
  finally { if (seq === courseRequest) loading.value = false }
}
async function loadStats() {
  if (!canViewStats.value) return
  statsError.value = ''
  try { stats.value = await trainingApi.stats() } catch (e) { stats.value = null; statsError.value = errText(e) }
}
async function loadCategories() {
  try { categories.value = await categoryApi.list('training.course') }
  catch (e) { toast(`分类加载失败：${errText(e)}`, 'bad') }
}
function search() { applied.value = { ...filter }; page.value = 1; void loadCourses() }
function reset() { Object.assign(filter, { q: '', status: '', category: '' }); search() }
function changePage(value: number) { page.value = value; void loadCourses() }
async function refresh() { await Promise.all([loadCourses(), loadStats(), loadCategories()]) }
function toggleSelected(id: number, checked: boolean) { selected.value = checked ? [...new Set([...selected.value, id])] : selected.value.filter(x => x !== id) }
function toggleAll(checked: boolean) { selected.value = checked ? courses.value.map(c => c.id) : [] }
function formatKinds(course: TrainingCourse) { return Object.entries(course.kindCounts ?? {}).filter(([, n]) => n > 0).map(([k, n]) => `${kindLabel(k)} ${n}`).join(' · ') }
function lessonInfo(lesson: TrainingLesson) { return [lesson.durationSec ? `${Math.floor(lesson.durationSec / 60)} 分 ${lesson.durationSec % 60} 秒` : '', lesson.pageCount ? `${lesson.pageCount} 页` : '', lesson.hasPdf ? '含配套 PDF' : '', lesson.sizeBytes ? `${(lesson.sizeBytes / 1024 / 1024).toFixed(1)} MB` : ''].filter(Boolean).join(' · ') || '–' }
function openCourse(course?: TrainingCourse) {
  Object.assign(courseForm, course ? { ...course, id: course.id, instructor: course.instructor ?? '', description: course.description ?? '', coverImage: course.coverImage ?? '', sortOrder: String(course.sortOrder), deptText: course.deptScope?.join('\n') ?? '' } : { id: null, title: '', category: '', level: 'elective', instructor: '', sortOrder: '0', description: '', coverImage: '', deptText: '' })
  formError.value = ''; courseOpen.value = true
}
async function saveCourse() {
  if (busy.value) return
  busy.value = true; formError.value = ''
  try {
    const body = { id: courseForm.id ?? undefined, title: courseForm.title.trim(), category: courseForm.category.trim(), level: courseForm.level, instructor: courseForm.instructor.trim(), sortOrder: Number(courseForm.sortOrder), description: courseForm.description, coverImage: courseForm.coverImage.trim(), deptScope: [...new Set(courseForm.deptText.split(/\r?\n/).map(x => x.trim()).filter(Boolean))] }
    if (!body.title || !body.category) throw new Error('请填写课程名称和小类')
    const result = await trainingApi.save(body)
    const isNew = !courseForm.id
    courseOpen.value = false; toast('课程已保存'); await refresh()
    if (isNew) await openLessons(result)
    else if (lessonCourse.value?.id === result.id) lessonCourse.value = result
  } catch (e) { formError.value = errorMessage(e) }
  finally { busy.value = false }
}
async function openLessons(course: TrainingCourse) { lessonCourse.value = course; lessons.value = []; lessonError.value = ''; await loadLessons() }
function closeLessons(open: boolean) { if (!open) { lessonCourse.value = null; lessonRequest++; lessons.value = [] } }
async function loadLessons() {
  if (!lessonCourse.value) return
  const seq = ++lessonRequest, id = lessonCourse.value.id; lessonsLoading.value = true; lessonError.value = ''
  try { const data = await trainingApi.detail(id); if (seq === lessonRequest) lessons.value = [...data.lessons].sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id) }
  catch (e) { if (seq === lessonRequest) lessonError.value = errText(e) }
  finally { if (seq === lessonRequest) lessonsLoading.value = false }
}
function openLesson(lesson?: TrainingLesson) {
  Object.assign(lessonForm, { id: lesson?.id ?? null, title: lesson?.title ?? '', sortOrder: String(lesson?.sortOrder ?? (Math.max(0, ...lessons.value.map(l => l.sortOrder)) + 1)), kind: lesson?.kind ?? 'video', objectKey: '', pdfKey: '', pagesText: '', url: lesson?.url ?? '' })
  lessonSaveError.value = ''; clearUpload(); lessonOpen.value = true
}
function clearUpload() { uploadedName.value = ''; uploadProgress.value = 0; lessonForm.objectKey = ''; lessonForm.pdfKey = ''; lessonForm.pagesText = ''; lessonForm.url = '' }
async function saveLesson() {
  if (!lessonCourse.value || busy.value || uploading.value) return
  busy.value = true; lessonSaveError.value = ''
  try {
    const body: Record<string, unknown> = { id: lessonForm.id ?? undefined, title: lessonForm.title.trim(), sortOrder: Number(lessonForm.sortOrder) }
    if (!body.title) throw new Error('请填写课时标题')
    if (!lessonForm.id) {
      body.courseId = lessonCourse.value.id; body.kind = lessonForm.kind
      const pages = lessonForm.pagesText.split(/\r?\n/).map(x => x.trim()).filter(Boolean)
      if (['video', 'pdf'].includes(lessonForm.kind)) { if (!lessonForm.objectKey) throw new Error('请先上传课件'); body.objectKey = lessonForm.objectKey }
      if (lessonForm.kind === 'link') { if (!/^https?:\/\//i.test(lessonForm.url)) throw new Error('请填写 http 或 https 链接'); body.url = lessonForm.url.trim() }
      if (lessonForm.kind === 'cards') { if (!pages.length) throw new Error('请填写图片路径'); body.pages = pages }
      if (lessonForm.kind === 'article') {
        if (!!lessonForm.objectKey.trim() === !!pages.length) throw new Error('HTML 路径与图片路径须选择其中一种')
        if (pages.length) body.pages = pages
        else { body.objectKey = lessonForm.objectKey.trim(); if (lessonForm.pdfKey.trim()) body.pdfKey = lessonForm.pdfKey.trim() }
      }
    }
    await trainingApi.saveLesson(body); lessonOpen.value = false; toast('课时已保存'); await Promise.all([loadLessons(), loadCourses(), loadStats()])
  } catch (e) { lessonSaveError.value = errorMessage(e) }
  finally { busy.value = false }
}
async function uploadFile(event: Event) {
  const input = event.target as HTMLInputElement, file = input.files?.[0]
  if (!file || !lessonCourse.value || uploading.value) return
  clearUpload(); uploading.value = true; lessonSaveError.value = ''
  try {
    const sign = await trainingApi.uploadUrl(lessonCourse.value.id, file.name, lessonForm.kind)
    if (!safeMediaUrl(sign.uploadUrl)) throw new Error('上传地址无效')
    await new Promise<void>((resolve, reject) => {
      const xhr = new XMLHttpRequest(); uploadRequest = xhr
      xhr.open('PUT', sign.uploadUrl); xhr.timeout = 600000
      xhr.upload.onprogress = e => { if (e.lengthComputable) uploadProgress.value = Math.round(e.loaded / e.total * 100) }
      xhr.onload = () => xhr.status >= 200 && xhr.status < 300 ? resolve() : reject(new Error(`课件上传失败（HTTP ${xhr.status}）`))
      xhr.onerror = () => reject(new Error('课件上传失败，请检查存储连接后重试'))
      xhr.ontimeout = () => reject(new Error('课件上传超时，请重新上传'))
      xhr.onabort = () => reject(new Error('上传已取消'))
      xhr.send(file)
    })
    lessonForm.objectKey = sign.objectKey; uploadedName.value = file.name
    if (!lessonForm.title.trim()) lessonForm.title = file.name.replace(/\.[^.]+$/, '')
  } catch (e) { lessonSaveError.value = errorMessage(e) }
  finally { uploadRequest = null; uploading.value = false; input.value = '' }
}
function cancelUpload() { uploadRequest?.abort() }
async function preview(lesson: TrainingLesson) {
  busy.value = true
  try { media.value = { title: lesson.title, ...await trainingApi.media(lesson.id) } }
  catch (e) { toast(errText(e), 'bad') }
  finally { busy.value = false }
}
function askBatch(action: 'publish' | 'unpublish') { if (selected.value.length) pending.value = { type: 'batch', action, ids: [...selected.value] } }
function skipReason(reason: string) { return ({ no_lessons: '还没有课时', already_published: '已经发布', already_draft: '已经是草稿', not_found: '课程不存在' } as Record<string, string>)[reason] ?? reason }
function closeResult(open: boolean) { if (!open) { batchResult.value = null; importResult.value = null } }
async function confirmAction() {
  if (!pending.value || busy.value) return
  const action = pending.value; busy.value = true
  try {
    switch (action.type) {
      case 'scan': importResult.value = await trainingApi.scan(); break
      case 'deleteCourse': await trainingApi.delete(action.course.id); toast('课程已删除'); if (lessonCourse.value?.id === action.course.id) closeLessons(false); break
      case 'deleteLesson': await trainingApi.deleteLesson(action.lesson.id); toast('课时已删除'); await loadLessons(); break
      case 'state': await trainingApi.state(action.course.id, action.action); toast(action.action === 'publish' ? '课程已发布' : '课程已下架'); break
      case 'batch': batchResult.value = await trainingApi.batch(action.ids, action.action); break
    }
    pending.value = null; await refresh()
  } catch (e) { toast(errText(e), 'bad') }
  finally { busy.value = false }
}
onMounted(() => { void refresh() })
onUnmounted(() => { courseRequest++; lessonRequest++; uploadRequest?.abort() })
</script>
