<template>
  <div v-if="!editing">
    <PageHeader
      :title="title"
      :description="
        isNews
          ? '发布公司动态，选择图文模板，审批待发布内容和管理评论。'
          : '维护知识文档、外链和标签，管理草稿与内容审批。'
      "
      ><template #actions
        ><Button v-if="canCreate" @click="startCreate"
          ><Plus class="size-4" />{{ isNews ? "发布动态" : "新建文档" }}</Button
        ></template
      ></PageHeader
    >
    <section class="panel mt-4 overflow-hidden">
      <form
        class="table-toolbar flex flex-wrap gap-2"
        @submit.prevent="applyFilters"
      >
        <Input
          v-model="query.q"
          placeholder="搜索标题或正文"
          aria-label="搜索内容"
          class="sm:w-56"
        /><Select
          v-model="query.status"
          :options="statuses"
          aria-label="内容状态"
          class="sm:w-40"
        /><Input
          v-model="query.category"
          placeholder="分类"
          aria-label="分类"
          class="sm:w-32"
        /><Input
          v-if="!isNews"
          v-model="query.department"
          placeholder="归属部门"
          aria-label="归属部门"
          class="sm:w-36"
        /><Button type="submit" variant="outline" :disabled="loading"
          ><Search class="size-4" />查询</Button
        ><Button
          v-if="
            query.q ||
            query.category ||
            query.department ||
            query.status !== 'all'
          "
          variant="ghost"
          :disabled="loading"
          @click="resetFilters"
          >重置</Button
        ><span class="ml-auto text-xs text-muted-foreground"
          >共 {{ total }} 条</span
        >
      </form>
      <Callout v-if="error" title="内容加载失败" tone="destructive" class="m-4"
        >{{ error
        }}<template #action
          ><Button variant="outline" size="sm" @click="load"
            >重试</Button
          ></template
        ></Callout
      >
      <div
        v-if="loading"
        class="p-10 text-center text-sm text-muted-foreground"
      >
        正在加载内容…
      </div>
      <div v-else-if="rows.length" class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>标题</th>
              <th>分类</th>
              <th>{{ isNews ? "稿型" : "部门" }}</th>
              <th>作者</th>
              <th>状态</th>
              <th>发布时间</th>
              <th class="sticky-col text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>
                <button
                  type="button"
                  class="portal-table-title font-medium text-left hover:text-primary focus-visible:underline" :title="row.title"
                  @click="openContent(row.id)"
                >
                  {{ row.title }}</button
                ><span v-if="row.isFeatured" class="ml-2 text-primary">★</span>
                <p
                  v-if="row.summary"
                  class="mt-1 max-w-72 truncate text-xs text-muted-foreground"
                >
                  {{ row.summary }}
                </p>
              </td>
              <td>{{ row.category || "—" }}</td>
              <td>
                {{
                  isNews
                    ? templateLabel(row.templateType ?? "")
                    : row.department || "全员"
                }}
              </td>
              <td>{{ row.author || row.authorUsername || "—" }}</td>
              <td>
                <Badge
                  :variant="
                    row.status === 'published'
                      ? 'default'
                      : row.status === 'rejected'
                        ? 'destructive'
                        : 'outline'
                  "
                  >{{ stateLabel(row.status) }}</Badge
                >
              </td>
              <td class="whitespace-nowrap text-xs text-muted-foreground">
                {{ time(row.publishedAt || row.createdAt) }}
              </td>
              <td class="sticky-col">
                <div class="flex items-center justify-end gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    :disabled="busy"
                    @click="openContent(row.id)"
                    >{{ canEditRecord(row) ? "编辑" : "查看" }}</Button
                  ><PortalRowActions
                    :disabled="busy"
                    :label="`${row.title}的更多操作`"
                    :actions="[
                      canReview &&
                        row.status === 'pending' && {
                          label: '审批通过',
                          onSelect: () => openReview(row, 'approve'),
                        },
                      canReview &&
                        row.status === 'pending' && {
                          label: '驳回内容',
                          destructive: true,
                          onSelect: () => openReview(row, 'reject'),
                        },
                      isNews &&
                        canReview &&
                        row.status === 'published' && {
                          label: '推送通知 / 测试',
                          onSelect: () => openPush(row),
                        },
                      isNews && {
                        label: '管理评论',
                        onSelect: () => openComments(row),
                      },
                      canDelete && {
                        label: '删除内容',
                        destructive: true,
                        onSelect: () => askDelete(row),
                      },
                    ]"
                  />
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState
        v-else-if="!error"
        title="暂无匹配内容"
        description="调整筛选条件或创建新内容。"
      /><Pagination
        :page="page"
        :page-size="20"
        :total="total"
        class="border-t px-5 py-3"
        :disabled="loading"
        @update:page="changePage"
      />
    </section>
  </div>
  <div v-else>
    <PageHeader
      :title="
        recordId
          ? `${canEdit ? '编辑' : '查看'}${isNews ? '动态' : '知识文档'}`
          : isNews
            ? '发布公司动态'
            : '新建知识文档'
      "
      :description="
        dirty ? '有未保存的修改' : '内容会按当前账号权限发布或进入审批。'
      "
      ><template #actions
        ><Button variant="ghost" :disabled="busy" @click="closeEditor"
          ><ArrowLeft class="size-4" />返回</Button
        ><Button variant="outline" @click="previewOpen = true"
          ><Eye class="size-4" />预览</Button
        ><Button
          v-if="isNews && canReview && currentRecord?.status === 'published'"
          variant="outline"
          @click="openPush(currentRecord)"
          >推送通知 / 测试</Button
        ><Button
          v-if="!isNews && canEdit"
          variant="outline"
          :loading="busy"
          @click="submit('draft')"
          >保存草稿</Button
        ><Button v-if="canEdit" :disabled="busy" @click="submitOpen = true">{{
          canEditAny ? "发布" : "提交审批"
        }}</Button></template
      ></PageHeader
    >
    <Callout
      v-if="editorError"
      title="内容保存失败"
      tone="destructive"
      class="mt-4"
      >{{ editorError }}</Callout
    >
    <section
      v-if="canCreate && canEdit"
      class="panel mt-4 flex flex-wrap items-end gap-3 p-4"
    >
      <div class="min-w-40 flex-1">
        <label for="content-polish-field" class="field-label">AI 润色字段</label
        ><Select
          id="content-polish-field"
          :model-value="activePolishField"
          :options="polishFields"
          @update:model-value="polishField = String($event)"
        />
      </div>
      <Button variant="outline" @click="openPolish">AI 润色</Button>
    </section>
    <fieldset
      :disabled="!canEdit"
      class="mt-4 grid min-w-0 items-start gap-4 xl:grid-cols-[minmax(0,1fr)_300px]"
    >
      <section class="panel min-w-0 p-4 sm:p-5">
        <label for="content-title" class="field-label">标题</label
        ><Input
          id="content-title"
          v-model="form.title"
          maxlength="200"
          :disabled="!canEdit"
          placeholder="请输入标题"
        />
        <template v-if="isNews"
          ><div class="my-5 flex items-center justify-between border-y py-3">
            <label for="content-template-enabled" class="text-sm font-medium"
              >使用图文模板</label
            ><Switch
              id="content-template-enabled"
              v-model="form.useTemplate"
              :disabled="!canEdit"
            />
          </div>
          <template v-if="form.useTemplate"
            ><label for="content-template" class="field-label">稿型模板</label
            ><Select
              id="content-template"
              v-model="form.templateType"
              :options="newsTemplates"
              :disabled="!canEdit"
              @update:model-value="changeTemplate" /><NewsTemplateEditor
              v-if="canEdit"
              class="mt-5"
              :class="!canEdit ? 'pointer-events-none' : ''"
              :type="form.templateType"
              :blocks="form.blocks" /><HrBlockPreview
              v-else
              class="mt-5"
              :blocks="previewBlocks" /></template
          ><template v-else
            ><label class="field-label mt-5">正文</label
            ><HrRichText
              v-model="form.content"
              :readonly="!canEdit"
              label="动态正文"
              :class="!canEdit ? 'pointer-events-none' : ''" /></template
        ></template>
        <template v-else
          ><label class="field-label mt-5">正文</label
          ><HrRichText
            v-model="form.content"
            :readonly="!canEdit"
            label="知识文档正文"
            :class="!canEdit ? 'pointer-events-none' : ''"
        /></template>
      </section>
      <aside class="space-y-4">
        <section class="panel space-y-4 p-4">
          <h2 class="text-sm font-semibold">
            {{ isNews ? "发布设置" : "文档信息" }}
          </h2>
          <div>
            <label for="content-category" class="field-label">分类</label
            ><Input
              id="content-category"
              v-model="form.category"
              maxlength="50"
              :disabled="!canEdit"
            />
          </div>
          <template v-if="isNews"
            ><div>
              <label for="content-summary" class="field-label">摘要</label
              ><Textarea
                id="content-summary"
                v-model="form.summary"
                :rows="4"
                :disabled="!canEdit"
              />
            </div>
            <div v-if="!form.useTemplate">
              <label for="content-cover" class="field-label">封面图</label
              ><PortalImageField
                id="content-cover"
                v-model="form.coverImage"
                folder="news/cover"
                label="动态封面"
              />
            </div>
            <div class="flex items-center justify-between">
              <label for="content-featured" class="text-xs">精选置顶</label
              ><Switch
                id="content-featured"
                v-model="form.isFeatured"
                :disabled="!canEdit"
              />
            </div>
            <template v-if="!recordId"
              ><div class="flex items-center justify-between">
                <label for="content-notify" class="text-xs"
                  >发布后推送企业微信</label
                ><Switch
                  id="content-notify"
                  v-model="form.notifyWecom"
                  :disabled="!canEdit"
                /></div></template
            ><template v-else
              ><div>
                <label for="content-notify-mode" class="field-label"
                  >本次发布推送设置</label
                ><Select
                  id="content-notify-mode"
                  v-model="form.notifyMode"
                  :disabled="!canEdit"
                  :options="[
                    { value: 'keep', label: '保留原意图，本次不额外推送' },
                    { value: 'yes', label: '本次发布后推送' },
                    { value: 'no', label: '取消推送意图，不推送' },
                  ]"
                />
              </div>
              <p
                v-if="currentRecord?.pendingNotifyWecom"
                class="text-xs text-muted-foreground"
              >
                作者已申请审批通过后推送。范围：{{
                  currentRecord.pendingNotifyDeptScope?.length
                    ? currentRecord.pendingNotifyDeptScope.join("、")
                    : "全员"
                }}。
              </p></template
            >
            <div v-if="willNotify">
              <label for="content-notify-depts" class="field-label"
                >推送部门（每行一个，留空全员）</label
              ><Textarea
                id="content-notify-depts"
                v-model="form.notifyDepts"
                :rows="3"
              />
              <p class="mt-2 text-xs text-muted-foreground">
                进入审批的动态会保留推送意图，审批通过后生效。发送结果请查看推送记录。
              </p>
            </div></template
          ><template v-else
            ><div>
              <label for="content-department" class="field-label"
                >归属部门</label
              ><Input
                id="content-department"
                v-model="form.department"
                maxlength="50"
                :disabled="!canEdit"
              />
            </div>
            <div>
              <label for="content-doc-url" class="field-label"
                >外链文档地址</label
              ><Input
                id="content-doc-url"
                v-model="form.docUrl"
                maxlength="500"
                :disabled="!canEdit"
                placeholder="https://…"
              />
            </div>
            <div>
              <label for="content-tags" class="field-label"
                >标签（逗号分隔）</label
              ><Input
                id="content-tags"
                v-model="form.tagsText"
                :disabled="!canEdit"
              /></div
          ></template>
        </section>
        <Callout
          v-if="recordId && initialStatus"
          :title="`当前状态：${stateLabel(initialStatus)}`"
          >{{
            canEditAny
              ? "发布后更新员工可见内容。"
              : "修改已发布内容将重新进入审批。"
          }}</Callout
        >
      </aside>
    </fieldset>
  </div>
  <Dialog
    v-model:open="previewOpen"
    :title="isNews ? '动态预览' : '知识文档预览'"
    size="xl"
    ><article class="mx-auto max-w-3xl">
      <img
        v-if="isNews && hrSafeUrl(previewCover)"
        :src="hrSafeUrl(previewCover)"
        alt="动态封面"
        class="mb-5 max-w-full rounded-xl"
      />
      <p class="mb-2 text-xs text-primary">
        {{ form.category
        }}<span v-if="form.department"> · {{ form.department }}</span>
      </p>
      <h1 class="mb-3 text-3xl font-semibold">
        {{ form.title || "未填写标题" }}
      </h1>
      <p v-if="form.summary" class="mb-5 text-sm text-muted-foreground">
        {{ form.summary }}
      </p>
      <div
        v-if="isNews && form.useTemplate"
        class="mb-5 flex flex-wrap gap-4 text-xs text-muted-foreground"
      >
        <span v-if="form.blocks.event_date">{{ form.blocks.event_date }}</span
        ><span v-if="form.blocks.event_location">{{
          form.blocks.event_location
        }}</span
        ><span v-if="form.blocks.event_booth"
          >展位：{{ form.blocks.event_booth }}</span
        ><span v-if="form.blocks.brand_name || form.blocks.brand">{{
          form.blocks.brand_name || form.blocks.brand
        }}</span>
      </div>
      <HrBlockPreview
        v-if="isNews && form.useTemplate"
        :blocks="previewBlocks"
      /><HrHtmlContent v-else :html="form.content" /><a
        v-if="!isNews && hrSafeUrl(form.docUrl)"
        :href="hrSafeUrl(form.docUrl)"
        target="_blank"
        rel="noopener noreferrer"
        class="mt-5 inline-block text-primary underline"
        >打开外链文档</a
      >
    </article></Dialog
  >
  <ConfirmDialog
    v-model:open="submitOpen"
    :title="canEditAny ? '确认发布内容？' : '确认提交审批？'"
    :description="
      willNotify
        ? '内容发布后将向配置范围内的员工推送企业微信通知。需要审批时，审批通过后发送。'
        : '将保存当前标题、正文和发布设置。'
    "
    :confirm-text="canEditAny ? '确认发布' : '提交审批'"
    :busy="busy"
    @confirm="submit()"
  />
  <Dialog
    v-model:open="reviewOpen"
    :title="reviewDecision === 'approve' ? '审批通过' : '驳回内容'"
    size="sm"
    ><p class="mb-4 text-sm font-medium">{{ reviewRow?.title }}</p>
    <p v-if="reviewLoading" class="mb-3 text-xs text-muted-foreground">
      正在核对当前内容与作者推送设置…
    </p>
    <Callout
      v-else-if="isNews && reviewDecision === 'approve'"
      class="mb-4"
      :title="
        reviewRow?.pendingNotifyWecom
          ? '作者已申请发布后推送'
          : '作者未要求推送'
      "
      >{{
        reviewRow?.pendingNotifyWecom
          ? `推送范围：${reviewRow.pendingNotifyDeptScope?.length ? reviewRow.pendingNotifyDeptScope.join("、") : "全员"}。`
          : "选择跟随作者设置时，本次审批不会推送通知。"
      }}</Callout
    ><label for="content-review-comment" class="field-label">{{
      reviewDecision === "reject" ? "驳回原因（必填）" : "审批意见"
    }}</label
    ><Textarea
      id="content-review-comment"
      v-model="reviewComment"
      maxlength="500"
      :rows="3"
    /><template v-if="isNews && reviewDecision === 'approve'"
      ><label for="content-review-notify" class="field-label mt-4"
        >企业微信推送</label
      ><Select
        id="content-review-notify"
        v-model="reviewNotify"
        :options="[
          { value: '', label: '跟随作者推送设置' },
          { value: 'yes', label: '发布后推送' },
          { value: 'no', label: '本次不推送' },
        ]" /><template v-if="reviewNotify === 'yes'"
        ><label for="content-review-depts" class="field-label mt-4"
          >推送部门（每行一个，留空全员）</label
        ><Textarea
          id="content-review-depts"
          v-model="reviewDepts"
          :rows="2" /></template></template
    ><Callout v-if="reviewError" tone="destructive" class="mt-3">{{
      reviewError
    }}</Callout
    ><template #footer
      ><Button variant="outline" :disabled="busy" @click="reviewOpen = false"
        >取消</Button
      ><Button
        :variant="reviewDecision === 'reject' ? 'destructive' : 'default'"
        :loading="busy"
        :disabled="reviewLoading || !reviewReady"
        @click="review"
        >确认{{ reviewDecision === "approve" ? "通过" : "驳回" }}</Button
      ></template
    ></Dialog
  >
  <ConfirmDialog
    v-model:open="deleteOpen"
    title="删除内容？"
    :description="`《${deleteRow?.title ?? ''}》删除后无法恢复。`"
    confirm-text="删除"
    tone="destructive"
    :busy="busy"
    @confirm="remove"
  />
  <Dialog v-model:open="commentsOpen" title="动态评论管理" size="lg"
    ><p class="mb-4 font-medium">{{ commentRow?.title }}</p>
    <Callout v-if="commentsError" tone="destructive">{{
      commentsError
    }}</Callout>
    <p
      v-if="commentsLoading"
      class="py-8 text-center text-sm text-muted-foreground"
    >
      正在加载评论…
    </p>
    <template v-else-if="engagement"
      ><p class="mb-4 text-xs text-muted-foreground">
        {{ engagement.likeCount }} 次点赞 · {{ engagement.commentCount }} 条评论
      </p>
      <div
        v-for="comment in engagement.comments"
        :key="comment.id"
        class="border-t py-3"
      >
        <div class="mb-2 flex items-center justify-between gap-2">
          <span class="text-xs text-muted-foreground"
            >{{
              comment.displayName ||
              comment.userName ||
              comment.username ||
              "员工"
            }}
            · {{ time(comment.createdAt)
            }}<span v-if="comment.parentId">
              · 回复 #{{ comment.parentId }}</span
            ></span
          ><Button
            v-if="comment.canDelete"
            variant="ghost"
            size="sm"
            :disabled="busy"
            @click="
              deleteCommentId = comment.id;
              deleteCommentOpen = true;
            "
            >删除</Button
          >
        </div>
        <p class="whitespace-pre-wrap break-words text-sm">
          {{ comment.content }}
        </p>
      </div>
      <EmptyState
        v-if="!engagement.comments.length"
        title="暂无评论" /></template
  ></Dialog>
  <ConfirmDialog
    v-model:open="deleteCommentOpen"
    title="删除这条评论？"
    description="删除后评论将从动态详情中移除。"
    confirm-text="删除评论"
    tone="destructive"
    :busy="busy"
    @confirm="deleteComment"
  />
  <ConfirmDialog
    v-model:open="discardOpen"
    title="放弃未保存的修改？"
    description="返回列表将丢失当前编辑内容。"
    confirm-text="放弃修改"
    tone="destructive"
    @confirm="
      discardOpen = false;
      editing = false;
      load();
    "
  />
  <Callout
    v-if="identityError"
    class="mt-4"
    tone="destructive"
    title="编辑权限信息加载失败"
    >{{ identityError
    }}<template #action
      ><Button variant="outline" size="sm" @click="loadIdentity"
        >重试</Button
      ></template
    ></Callout
  >
  <PortalContentPushDialog
    v-model:open="pushOpen"
    kind="news"
    :id="pushRow?.id ?? 0"
    :title="pushRow?.title ?? ''"
    :target-key="pushRow ? `news:${pushRow.id}` : ''"
  />
  <PortalAiTextDialog
    v-model:open="polishOpen"
    mode="polish"
    :kind="kind"
    :text="polishSource"
    :description="`采用后替换「${polishFields.find((field) => field.value === activePolishField)?.label ?? '正文'}」的编辑内容；发布由您另外操作。`"
    @apply="applyPolish"
  />
</template>
<script setup lang="ts">
import {
  computed,
  onMounted,
  onBeforeUnmount,
  reactive,
  ref,
  watch,
} from "vue";
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from "vue-router";
import { Plus, Search, ArrowLeft, Eye } from "@lucide/vue";
import PortalRowActions from "../../components/portal/PortalRowActions.vue";
import PageHeader from "../../ui/PageHeader.vue";
import Button from "../../ui/Button.vue";
import Input from "../../ui/Input.vue";
import Textarea from "../../ui/Textarea.vue";
import Select from "../../ui/Select.vue";
import Switch from "../../ui/Switch.vue";
import Badge from "../../ui/Badge.vue";
import Callout from "../../ui/Callout.vue";
import Dialog from "../../ui/Dialog.vue";
import ConfirmDialog from "../../ui/ConfirmDialog.vue";
import Pagination from "../../ui/Pagination.vue";
import EmptyState from "../../ui/EmptyState.vue";
import HrRichText from "../../components/portal/HrRichText.vue";
import HrHtmlContent from "../../components/portal/HrHtmlContent.vue";
import HrBlockPreview from "../../components/portal/HrBlockPreview.vue";
import PortalImageField from "../../components/portal/PortalImageField.vue";
import NewsTemplateEditor from "../../components/portal/NewsTemplateEditor.vue";
import PortalAiTextDialog from "../../components/portal/PortalAiTextDialog.vue";
import PortalContentPushDialog from "../../components/portal/PortalContentPushDialog.vue";
import {
  contentApi,
  newsTemplates,
  newsTemplateDefaults,
  newsPreviewBlocks,
  type ContentKind,
  type ContentRecord,
  type NewsEngagement,
} from "../../lib/portal-content";
import { hrSafeUrl } from "../../lib/portal-hr";
import { flushPortalRichTextImages } from "../../lib/portal-richtext";
import { api } from "../../lib/api";
import { session } from "../../lib/session";
import { errText, toast } from "../../lib/toast";
const props = withDefaults(defineProps<{ kind?: ContentKind }>(), {
  kind: "news",
});
const route = useRoute();
const router = useRouter();
const isNews = computed(() => props.kind === "news"),
  title = computed(() => (isNews.value ? "公司动态管理" : "知识库管理"));
const canCreate = computed(() => session.has("can_create_content")),
  canEditAny = computed(() => session.has("can_edit_any")),
  canReview = computed(() => session.has("can_review")),
  canDelete = computed(() => session.has("can_delete"));
const statuses = [
  { value: "all", label: "全部可见内容" },
  { value: "published", label: "已发布" },
  { value: "pending", label: "待审批" },
  { value: "draft", label: "草稿" },
  { value: "rejected", label: "已驳回" },
];
const query = reactive({
    q: typeof route.query.q === 'string' ? route.query.q : '',
    status: statuses.some(status => status.value === route.query.status) ? String(route.query.status) : 'all',
    category: typeof route.query.category === 'string' ? route.query.category : '',
    department: typeof route.query.department === 'string' ? route.query.department : '',
  }),
  rows = ref<ContentRecord[]>([]),
  total = ref(0),
  page = ref(/^[1-9]\d{0,6}$/.test(String(route.query.page ?? '')) ? Number(route.query.page) : 1),
  loading = ref(false),
  error = ref(""),
  busy = ref(false);
function resetFilters() {
  Object.assign(query, { q: "", status: "all", category: "", department: "" });
  applyFilters();
}
function applyFilters() {
  const next = { ...route.query };
  for (const key of ['q', 'status', 'category', 'department', 'page']) delete next[key];
  if (query.q.trim()) next.q = query.q.trim();
  if (query.status !== 'all') next.status = query.status;
  if (query.category.trim()) next.category = query.category.trim();
  if (!isNews.value && query.department.trim()) next.department = query.department.trim();
  if (router.resolve({ query: next }).fullPath === route.fullPath) void load();
  else void router.push({ query: next });
}
function changePage(value: number) {
  void router.push({ query: { ...route.query, page: value > 1 ? String(value) : undefined } });
}
const editing = ref(false),
  recordId = ref<number>(),
  baseline = ref(""),
  editorError = ref(""),
  initialStatus = ref(""),
  previewOpen = ref(false),
  submitOpen = ref(false),
  discardOpen = ref(false);
const currentRecord = ref<ContentRecord>(),
  pushRow = ref<ContentRecord>(),
  pushOpen = ref(false);
const currentUsername = ref(""),
  identityError = ref("");
const canEditRecord = (row: ContentRecord) =>
  canEditAny.value ||
  Boolean(
    currentUsername.value && row.authorUsername === currentUsername.value,
  );
const canEdit = computed(() =>
  recordId.value
    ? Boolean(currentRecord.value && canEditRecord(currentRecord.value))
    : canCreate.value,
);
let identityPromise: Promise<void> | undefined;
function loadIdentity(): Promise<void> {
  if (currentUsername.value) return Promise.resolve();
  if (identityPromise) return identityPromise;
  identityError.value = "";
  identityPromise = api
    .get<{ username: string }>("/api/auth/me")
    .then((me) => {
      if (typeof me?.username !== "string" || !me.username)
        throw new Error("身份响应缺少员工账号");
      currentUsername.value = me.username;
    })
    .catch((e) => {
      if (!canEditAny.value)
        identityError.value = errText(
          e,
          "当前员工账号加载失败，请重试；内容暂按只读展示。",
        );
    })
    .finally(() => {
      identityPromise = undefined;
    });
  return identityPromise;
}
function openPush(row: ContentRecord) {
  pushRow.value = row;
  pushOpen.value = true;
}
function emptyForm() {
  return {
    title: "",
    summary: "",
    category: "",
    content: "",
    department: "",
    docUrl: "",
    tagsText: "",
    coverImage: "",
    templateType: "article",
    useTemplate: true,
    blocks: newsTemplateDefaults("article"),
    isFeatured: false,
    notifyWecom: false,
    notifyMode: "keep",
    notifyDepts: "",
  };
}
const form = reactive(emptyForm()),
  dirty = computed(
    () => editing.value && JSON.stringify(form) !== baseline.value,
  );
const willNotify = computed(
  () =>
    isNews.value &&
    (recordId.value ? form.notifyMode === "yes" : form.notifyWecom),
);
const polishOpen = ref(false),
  polishField = ref("body"),
  polishApplyTarget = ref("body");
const polishFields = computed(() => {
  if (!isNews.value) return [{ value: "body", label: "正文" }];
  const fields = [{ value: "summary", label: "摘要" }];
  if (!form.useTemplate) fields.unshift({ value: "body", label: "正文" });
  else {
    for (const [key, label] of [
      ["intro", "引言"],
      ["story", "品牌故事"],
      ["message", "感言"],
      ["extra_text", "补充说明"],
    ])
      if (typeof form.blocks[key!] === "string")
        fields.push({ value: `field:${key}`, label: label! });
    for (const [key, label] of [
      ["paragraphs", "段落"],
      ["content_blocks", "图文段"],
      ["sections", "展会段落"],
      ["recap_sections", "活动回顾"],
    ])
      (form.blocks[key!] ?? []).forEach((item: any, i: number) => {
        if (typeof item === "string" || typeof item?.text === "string")
          fields.push({
            value: `item:${key}:${i}`,
            label: `${label} ${i + 1}`,
          });
      });
    if (
      form.templateType === "article" &&
      !(form.blocks.content_blocks ?? []).some(
        (item: any) => typeof item.text === "string",
      )
    )
      fields.push({ value: "article:new", label: "新增文字段" });
  }
  return fields;
});
const activePolishField = computed(() =>
  polishFields.value.some((field) => field.value === polishField.value)
    ? polishField.value
    : (polishFields.value[0]?.value ?? "body"),
);
function plainText(value: unknown) {
  return (
    new DOMParser().parseFromString(String(value ?? ""), "text/html").body
      .textContent ?? ""
  );
}
function polishText(target: string) {
  if (target === "body") return form.content;
  if (target === "summary") return form.summary;
  const [kind, key, index] = target.split(":");
  if (kind === "field") return form.blocks[key!];
  if (kind === "item") {
    const item = form.blocks[key!]?.[Number(index)];
    return typeof item === "string" ? item : (item?.text ?? "");
  }
  return "";
}
const polishSource = computed(() =>
  plainText(polishText(activePolishField.value)),
);
function openPolish() {
  polishApplyTarget.value = activePolishField.value;
  polishOpen.value = true;
}
function applyPolish(result: string | unknown[]) {
  if (typeof result !== "string") return;
  const target = polishApplyTarget.value,
    plain = plainText(result);
  const rich = /<\/?[a-z][\s\S]*>/i.test(result)
    ? result
    : result
        .split(/\n\s*\n/)
        .map(
          (text) =>
            `<p>${text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/\n/g, "<br>")}</p>`,
        )
        .join("");
  if (target === "body") form.content = rich;
  else if (target === "summary") form.summary = plain;
  else if (target === "article:new")
    (form.blocks.content_blocks ??= []).push({ type: "text", text: rich });
  else {
    const [kind, key, index] = target.split(":");
    if (kind === "field") form.blocks[key!] = key === "intro" ? plain : rich;
    else if (kind === "item") {
      const item = form.blocks[key!]?.[Number(index)];
      if (typeof item === "string") form.blocks[key!][Number(index)] = rich;
      else if (item) item.text = rich;
    }
  }
  toast("润色结果已载入编辑器，请检查后发布");
}
const previewCover = computed(() =>
    form.useTemplate ? form.blocks.cover_image : form.coverImage,
  ),
  previewBlocks = computed(() =>
    newsPreviewBlocks(form.templateType, form.blocks),
  );
function time(value?: string) {
  return value ? value.replace("T", " ").slice(0, 16) : "—";
}
function stateLabel(value: string) {
  return (
    (
      {
        published: "已发布",
        pending: "待审批",
        draft: "草稿",
        rejected: "已驳回",
      } as Record<string, string>
    )[value] ?? value
  );
}
function templateLabel(value: string) {
  return (
    newsTemplates.find((template) => template.value === value)?.label ?? value
  );
}
let request = 0;
async function load() {
  const id = ++request;
  loading.value = true;
  error.value = "";
  try {
    const result = await contentApi.page(props.kind, {
      pageNo: page.value,
      pageSize: 20,
      q: query.q || undefined,
      status: query.status,
      category: query.category || undefined,
      department: !isNews.value ? query.department || undefined : undefined,
    });
    if (id === request) {
      rows.value = result.list;
      total.value = result.total;
    }
  } catch (e) {
    if (id === request) {
      error.value = errText(e);
      rows.value = [];
      total.value = 0;
    }
  } finally {
    if (id === request) loading.value = false;
  }
}
let templateDrafts: Record<string, Record<string, any>> = {},
  lastTemplate = "article";
function startCreate() {
  recordId.value = undefined;
  currentRecord.value = undefined;
  initialStatus.value = "";
  Object.assign(form, emptyForm());
  templateDrafts = {};
  lastTemplate = "article";
  if (isNews.value) form.category = "工作动态";
  baseline.value = JSON.stringify(form);
  editorError.value = "";
  editing.value = true;
}
async function openContent(id: number) {
  busy.value = true;
  try {
    const row = await contentApi.get(props.kind, id);
    if (!canEditAny.value) await loadIdentity();
    currentRecord.value = row;
    recordId.value = id;
    initialStatus.value = row.status;
    Object.assign(form, emptyForm(), {
      title: row.title,
      category: row.category ?? "",
      summary: row.summary ?? "",
      content: row.content ?? "",
      department: row.department ?? "",
      docUrl: row.docUrl ?? "",
      tagsText: (row.tags ?? []).join(", "),
      coverImage: row.coverImage ?? "",
      templateType: row.templateType || "simple",
      useTemplate: Boolean(row.blocks && Object.keys(row.blocks).length),
      blocks: {
        ...newsTemplateDefaults(row.templateType || "simple"),
        ...JSON.parse(JSON.stringify(row.blocks ?? {})),
      },
      isFeatured: row.isFeatured ?? false,
      notifyDepts: (row.pendingNotifyDeptScope ?? []).join("\n"),
    });
    templateDrafts = {};
    lastTemplate = form.templateType;
    baseline.value = JSON.stringify(form);
    editorError.value = "";
    editing.value = true;
  } catch (e) {
    toast(errText(e), "bad");
  } finally {
    busy.value = false;
  }
}
function changeTemplate() {
  templateDrafts[lastTemplate] = JSON.parse(JSON.stringify(form.blocks));
  const common = {
    cover_image: form.blocks.cover_image || "",
    gallery: form.blocks.gallery ?? [],
    ...(typeof form.blocks.intro === "string"
      ? { intro: form.blocks.intro }
      : {}),
  };
  form.blocks = {
    ...newsTemplateDefaults(form.templateType),
    ...common,
    ...templateDrafts[form.templateType],
  };
  lastTemplate = form.templateType;
  if (!recordId.value)
    form.category =
      newsTemplates.find((template) => template.value === form.templateType)
        ?.category ?? form.category;
}
function closeEditor() {
  if (dirty.value) discardOpen.value = true;
  else {
    editing.value = false;
    load();
  }
}
async function submit(status?: string) {
  if (busy.value) return;
  busy.value = true;
  editorError.value = "";
  try {
    await flushPortalRichTextImages();
    if (!form.title.trim()) throw new Error("请填写标题");
    const notify = willNotify.value;
    const notifyFields =
      !recordId.value || form.notifyMode !== "keep"
        ? {
            notifyWecom: notify,
            notifyDeptScope: form.notifyDepts
              .split("\n")
              .map((v) => v.trim())
              .filter(Boolean),
          }
        : {};
    const body = isNews.value
      ? {
          title: form.title.trim(),
          summary: form.summary,
          content: form.content,
          category: form.category,
          coverImage: form.useTemplate
            ? form.blocks.cover_image || ""
            : form.coverImage,
          templateType: form.useTemplate ? form.templateType : "simple",
          blocks: form.useTemplate ? form.blocks : null,
          isFeatured: form.isFeatured,
          ...notifyFields,
        }
      : {
          title: form.title.trim(),
          category: form.category,
          content: form.content,
          department: form.department,
          docUrl: form.docUrl,
          tags: form.tagsText
            .split(/[,，]/)
            .map((v) => v.trim())
            .filter(Boolean),
          status: status === "draft" ? "draft" : undefined,
        };
    const result = await contentApi.save(props.kind, body, recordId.value);
    baseline.value = JSON.stringify(form);
    editing.value = false;
    submitOpen.value = false;
    toast(
      result.status === "pending"
        ? notify
          ? "内容已提交审批，推送意图已记录"
          : "内容已提交审批"
        : result.status === "draft"
          ? "草稿已保存"
          : "内容已发布",
    );
    if (
      isNews.value &&
      result.status === "published" &&
      notify &&
      canReview.value
    )
      openPush(result);
    await load();
  } catch (e) {
    editorError.value = e instanceof Error ? e.message : errText(e);
    submitOpen.value = false;
  } finally {
    busy.value = false;
  }
}
const reviewOpen = ref(false),
  reviewDecision = ref("approve"),
  reviewRow = ref<ContentRecord>(),
  reviewComment = ref(""),
  reviewNotify = ref(""),
  reviewDepts = ref(""),
  reviewError = ref(""),
  reviewLoading = ref(false),
  reviewReady = ref(false);
async function openReview(row: ContentRecord, decision: string) {
  reviewRow.value = row;
  reviewDecision.value = decision;
  reviewComment.value = "";
  reviewNotify.value = "";
  reviewDepts.value = "";
  reviewError.value = "";
  reviewOpen.value = true;
  reviewLoading.value = true;
  reviewReady.value = false;
  try {
    const detail = await contentApi.get(props.kind, row.id);
    if (reviewRow.value?.id === row.id && reviewOpen.value) {
      reviewRow.value = detail;
      reviewReady.value = detail.status === "pending";
      if (detail.status !== "pending")
        reviewError.value = "内容状态已变化，请关闭弹窗并刷新列表。";
    }
  } catch (e) {
    reviewError.value = errText(e);
  } finally {
    reviewLoading.value = false;
  }
}
async function review() {
  if (
    !reviewRow.value ||
    busy.value ||
    reviewLoading.value ||
    !reviewReady.value ||
    reviewRow.value.status !== "pending"
  )
    return;
  busy.value = true;
  reviewError.value = "";
  try {
    if (reviewDecision.value === "reject" && !reviewComment.value.trim())
      throw new Error("请填写驳回原因");
    const notify =
      isNews.value &&
      reviewDecision.value === "approve" &&
      (reviewNotify.value === "yes" ||
        (reviewNotify.value === "" && reviewRow.value.pendingNotifyWecom));
    await contentApi.review(props.kind, reviewDecision.value, {
      id: reviewRow.value.id,
      comment: reviewComment.value.trim(),
      ...(isNews.value &&
      reviewDecision.value === "approve" &&
      reviewNotify.value
        ? {
            notifyWecom: reviewNotify.value === "yes",
            ...(reviewNotify.value === "yes"
              ? {
                  notifyDeptScope: reviewDepts.value
                    .split("\n")
                    .map((v) => v.trim())
                    .filter(Boolean),
                }
              : {}),
          }
        : {}),
    });
    reviewOpen.value = false;
    toast("审批已处理");
    if (notify) openPush({ ...reviewRow.value, status: "published" });
    await load();
  } catch (e) {
    reviewError.value = e instanceof Error ? e.message : errText(e);
  } finally {
    busy.value = false;
  }
}
const deleteOpen = ref(false),
  deleteRow = ref<ContentRecord>();
function askDelete(row: ContentRecord) {
  deleteRow.value = row;
  deleteOpen.value = true;
}
async function remove() {
  if (!deleteRow.value || busy.value) return;
  busy.value = true;
  try {
    await contentApi.remove(props.kind, deleteRow.value.id);
    deleteOpen.value = false;
    toast("内容已删除");
    await load();
  } catch (e) {
    toast(errText(e), "bad");
  } finally {
    busy.value = false;
  }
}
const commentsOpen = ref(false),
  commentRow = ref<ContentRecord>(),
  engagement = ref<NewsEngagement>(),
  commentsLoading = ref(false),
  commentsError = ref(""),
  deleteCommentOpen = ref(false),
  deleteCommentId = ref(0);
async function openComments(row: ContentRecord) {
  commentRow.value = row;
  commentsOpen.value = true;
  engagement.value = undefined;
  await loadComments();
}
async function loadComments() {
  if (!commentRow.value) return;
  const target = commentRow.value.id;
  commentsLoading.value = true;
  commentsError.value = "";
  try {
    const result = await contentApi.engagement(target);
    if (commentRow.value.id === target) engagement.value = result;
  } catch (e) {
    commentsError.value = errText(e);
  } finally {
    commentsLoading.value = false;
  }
}
async function deleteComment() {
  if (!commentRow.value || busy.value) return;
  busy.value = true;
  try {
    await contentApi.deleteComment(commentRow.value.id, deleteCommentId.value);
    deleteCommentOpen.value = false;
    await loadComments();
    toast("评论已删除");
  } catch (e) {
    toast(errText(e), "bad");
  } finally {
    busy.value = false;
  }
}
function beforeUnload(event: BeforeUnloadEvent) {
  if (dirty.value) {
    event.preventDefault();
    event.returnValue = "";
  }
}
const confirmLeave = () =>
  !dirty.value || window.confirm("内容有未保存的修改，确认离开并放弃修改？");
onBeforeRouteLeave(confirmLeave);
onBeforeRouteUpdate(confirmLeave);
watch(() => route.query.action, action => {
  if (action === 'create' && canCreate.value && !editing.value) startCreate();
  else if (action !== 'create' && !dirty.value) editing.value = false;
}, { immediate: true });
watch(editing, value => {
  if (!value && route.query.action === 'create') {
    const next = { ...route.query }; delete next.action;
    void router.replace({ query: next });
  }
});
watch(() => [route.query.q, route.query.status, route.query.category, route.query.department, route.query.page], () => {
  query.q = typeof route.query.q === 'string' ? route.query.q : '';
  query.status = statuses.some(status => status.value === route.query.status) ? String(route.query.status) : 'all';
  query.category = typeof route.query.category === 'string' ? route.query.category : '';
  query.department = typeof route.query.department === 'string' ? route.query.department : '';
  page.value = /^[1-9]\d{0,6}$/.test(String(route.query.page ?? '')) ? Number(route.query.page) : 1;
  void load();
});
watch(
  () => props.kind,
  () => {
    editing.value = false;
    recordId.value = undefined;
    page.value = 1;
    Object.assign(query, {
      q: "",
      category: "",
      department: "",
      status: route.query.status === "pending" ? "pending" : "all",
    });
    load();
  },
);
onMounted(() => {
  void loadIdentity();
  load();
  if (/^[1-9]\d{0,14}$/.test(String(route.query.id ?? "")))
    openContent(Number(route.query.id));
  window.addEventListener("beforeunload", beforeUnload);
});
onBeforeUnmount(() => window.removeEventListener("beforeunload", beforeUnload));
</script>
