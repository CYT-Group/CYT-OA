<template>
  <DialogRoot :open="open" @update:open="emit('update:open', $event)">
    <DialogPortal>
      <DialogOverlay class="jd-overlay" />
      <DialogContent class="jd-window">
        <header class="jd-head">
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <DialogTitle class="text-base font-semibold">排版任务</DialogTitle
              ><Badge v-if="activeCount" variant="info"
                >{{ activeCount }} 项进行中</Badge
              >
            </div>
            <DialogDescription class="mt-1 text-xs text-muted-foreground"
              >先检查排版结果，再应用到文档；应用后仍需保存草稿。</DialogDescription
            >
          </div>
          <DialogClose as-child
            ><Button variant="ghost" size="icon-sm" aria-label="关闭排版任务"
              ><X /></Button
          ></DialogClose>
        </header>
        <div
          class="jd-workspace"
          :class="{ 'jd-focus': focusMode }"
          :data-mobile-view="mobileView"
        >
          <aside class="jd-jobs" aria-label="排版任务列表">
            <div class="jd-list-heading">
              <h2>最近任务</h2>
              <span>{{ jobs.length }} 项</span>
            </div>
            <EmptyState
              v-if="!jobs.length"
              title="还没有排版任务"
              description="在文档编辑器中点击「智能排版」提交任务。"
            />
            <ul v-else ref="list" class="jd-list">
              <li
                v-for="job in jobs"
                :key="job.id"
                :data-job-id="keyOf(job)"
                class="jd-item"
                :class="[
                  `is-${job.status}`,
                  { 'is-selected': keyOf(job) === selectedId },
                ]"
              >
                <button
                  class="jd-select"
                  type="button"
                  :aria-pressed="keyOf(job) === selectedId"
                  @click="selectJob(job)"
                >
                  <span class="jd-job-status"
                    ><component
                      :is="statusIcon(job.status)"
                      :class="{ 'animate-spin': isActive(job) }"
                    /><span>{{ statusLabel(job.status) }}</span
                    ><span class="jd-source">{{
                      sourceLabel(job.source_type)
                    }}</span></span
                  >
                  <strong :title="job.source_name || '未命名文本'">{{
                    job.source_name || "未命名文本"
                  }}</strong>
                  <span class="jd-job-meta"
                    >{{ job.input_chars || 0 }} 字<template
                      v-if="job.status === 'succeeded'"
                    >
                      · {{ job.block_count || 0 }} 个块</template
                    ><template v-if="elapsed(job)">
                      · {{ elapsed(job) }}</template
                    ></span
                  >
                  <span v-if="job.created_at" class="jd-job-date">{{
                    formatPortalDateTime(job.created_at)
                  }}</span>
                </button>
                <button
                  v-if="job.status === 'succeeded'"
                  type="button"
                  class="jd-preview-link"
                  @click="selectJob(job)"
                >
                  预览<ChevronRight class="size-3" />
                </button>
              </li>
            </ul>
          </aside>
          <section class="jd-detail" aria-label="排版结果预览">
            <div class="jd-detail-nav">
              <Button
                variant="ghost"
                size="sm"
                class="jd-mobile-back"
                @click="mobileView = 'list'"
                ><ArrowLeft />任务列表</Button
              >
              <Button
                variant="ghost"
                size="sm"
                class="jd-focus-toggle"
                :aria-pressed="focusMode"
                @click="focusMode = !focusMode"
                ><PanelLeft />{{
                  focusMode ? "显示任务列表" : "专注预览"
                }}</Button
              >
              <span class="ml-auto text-xs text-muted-foreground">{{
                selectedJob
                  ? statusLabel(selectedJob.status)
                  : "选择任务查看详情"
              }}</span>
            </div>
            <template v-if="selectedJob">
              <div class="jd-result-heading">
                <h2>{{ selectedJob.source_name || "未命名文本" }}</h2>
                <details
                  v-if="selectedJob.summary"
                  :key="selectedJob.id"
                  class="jd-summary"
                >
                  <summary>查看排版说明</summary>
                  <p>{{ selectedJob.summary }}</p>
                </details>
              </div>
              <div
                v-if="selectedJob.status === 'succeeded'"
                class="jd-reader-tools"
              >
                <SegmentedTabs
                  v-model="previewWidth"
                  :items="[
                    { value: 'desktop', label: '桌面阅读' },
                    { value: 'mobile', label: '手机阅读' },
                  ]"
                  aria-label="预览阅读宽度"
                />
                <Button
                  variant="ghost"
                  size="sm"
                  :disabled="!blocks.length"
                  @click="scrollToTop"
                  ><ArrowUp />回到开头</Button
                >
              </div>
              <div
                class="jd-preview"
                :class="`jd-width-${previewWidth}`"
                :data-reader-ready="!loading && !error && blocks.length > 0"
                :aria-busy="loading"
                tabindex="0"
                aria-label="排版正文滚动区域"
              >
                <div
                  v-if="isActive(selectedJob)"
                  class="jd-state"
                  role="status"
                >
                  <LoaderCircle class="size-8 animate-spin text-primary" />
                  <h3>{{ selectedJob.stage || "正在排版" }}</h3>
                  <p>任务会在后台继续运行，完成后可在这里查看排版结果。</p>
                  <div
                    class="jd-progress"
                    role="progressbar"
                    aria-label="排版进度"
                    :aria-valuenow="progress"
                    aria-valuemin="0"
                    aria-valuemax="100"
                  >
                    <span :style="{ width: `${progress}%` }" />
                  </div>
                  <span class="text-xs text-muted-foreground"
                    >{{ progress }}%</span
                  >
                </div>
                <div
                  v-else-if="selectedJob.status === 'failed'"
                  class="jd-state"
                  role="status"
                >
                  <CircleAlert class="size-8 text-destructive" />
                  <h3>排版未完成</h3>
                  <p>{{ selectedJob.error || "任务处理失败，可重新提交。" }}</p>
                </div>
                <div v-else-if="loading" class="jd-state" role="status">
                  <LoaderCircle class="size-8 animate-spin text-primary" />
                  <h3>正在加载排版结果</h3>
                  <p>加载完成后可查看全文和交互内容。</p>
                </div>
                <div v-else-if="error" class="jd-state" role="alert">
                  <CircleAlert class="size-8 text-destructive" />
                  <h3>排版结果加载失败</h3>
                  <p>{{ error }}</p>
                  <Button
                    variant="outline"
                    size="sm"
                    @click="loadSelected(true)"
                    ><RefreshCw />重新加载</Button
                  >
                </div>
                <PortalDocumentReader
                  v-else-if="blocks.length"
                  :key="selectedId"
                  ref="reader"
                  :blocks="blocks"
                  :theme="theme"
                  :width="previewWidth"
                  :content-key="selectedId"
                  :disable-navigation="true"
                />
                <EmptyState
                  v-else
                  title="这个任务没有排版内容"
                  description="可以重新加载结果，或返回编辑器重新提交排版。"
                  ><Button
                    variant="outline"
                    size="sm"
                    @click="loadSelected(true)"
                    >重新加载</Button
                  ></EmptyState
                >
              </div>
              <footer class="jd-footer">
                <p>
                  {{
                    selectedJob.status === "succeeded"
                      ? "预览不会修改文档，应用后可在编辑器中撤销排版。"
                      : "可关闭窗口，稍后返回查看任务结果。"
                  }}
                </p>
                <div class="flex items-center gap-2">
                  <Button
                    v-if="!isActive(selectedJob)"
                    variant="ghost"
                    size="sm"
                    :disabled="applying"
                    @click="emit('remove', selectedJob.id)"
                    ><Trash2 />删除任务</Button
                  >
                  <Button
                    v-if="selectedJob.status === 'failed'"
                    size="sm"
                    @click="emit('retry', selectedJob.id)"
                    ><RefreshCw />重新提交</Button
                  >
                  <Button
                    v-if="selectedJob.status === 'succeeded'"
                    size="sm"
                    :disabled="loading || !!error || !blocks.length"
                    :loading="applying"
                    @click="apply"
                    ><Check />应用到编辑器</Button
                  >
                </div>
              </footer>
            </template>
            <EmptyState
              v-else
              class="m-auto"
              title="选择一个任务查看结果"
              description="排版完成后，可在这里检查全文、章节和不同阅读宽度。"
            />
          </section>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue";
import {
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
} from "reka-ui";
import {
  ArrowLeft,
  ArrowUp,
  Check,
  CheckCircle2,
  ChevronRight,
  CircleAlert,
  Clock3,
  LoaderCircle,
  PanelLeft,
  RefreshCw,
  Trash2,
  X,
} from "@lucide/vue";
import { hrDocApi } from "../bridge/api";
import { formatPortalDateTime } from "../../../../lib/portal-management-format";
import { errText } from "../../../../lib/toast";
import Badge from "../../../../ui/Badge.vue";
import Button from "../../../../ui/Button.vue";
import EmptyState from "../../../../ui/EmptyState.vue";
import SegmentedTabs from "../../../../ui/SegmentedTabs.vue";
import PortalDocumentReader from "./PortalDocumentReader.vue";
interface LayoutJob {
  id: number | string;
  status: string;
  source_name?: string;
  source_type?: string;
  input_chars?: number;
  block_count?: number;
  created_at?: string;
  finished_at?: string;
  progress?: number;
  stage?: string;
  summary?: string;
  error?: string;
}
type Block = Record<string, any>;
const props = withDefaults(
  defineProps<{ open: boolean; jobs: LayoutJob[]; theme?: string }>(),
  { theme: "" },
);
const emit = defineEmits<{
  "update:open": [value: boolean];
  apply: [value: { job: LayoutJob; blocks: Block[] }];
  retry: [id: number | string];
  remove: [id: number | string];
}>();
const selectedId = ref(""),
  mobileView = ref("list"),
  focusMode = ref(false),
  previewWidth = ref("desktop");
const list = ref<HTMLElement>(),
  reader = ref<InstanceType<typeof PortalDocumentReader>>(),
  applying = ref(false);
const cache = ref<Record<string, { fingerprint: string; blocks: Block[] }>>({}),
  errors = ref<Record<string, string>>({}),
  pending = ref<Record<string, boolean>>({});
const requests = new Map<string, Promise<Block[]>>();
const keyOf = (job: LayoutJob) => String(job.id);
const fingerprint = (job: LayoutJob) =>
  `${job.status}:${job.created_at || ""}:${job.finished_at || ""}:${job.block_count || 0}`;
const selectedJob = computed(() =>
  props.jobs.find((job) => keyOf(job) === selectedId.value),
);
const isActive = (job: LayoutJob) =>
  job.status === "running" || job.status === "pending";
const activeCount = computed(() => props.jobs.filter(isActive).length);
const loading = computed(() => !!pending.value[selectedId.value]);
const error = computed(() => errors.value[selectedId.value] || "");
const blocks = computed(() => {
  const job = selectedJob.value,
    value = cache.value[selectedId.value];
  return job && value?.fingerprint === fingerprint(job) ? value.blocks : [];
});
const progress = computed(() =>
  Math.max(0, Math.min(100, Number(selectedJob.value?.progress) || 0)),
);
function statusLabel(status: string) {
  return (
    (
      {
        pending: "等待处理",
        running: "正在排版",
        succeeded: "排版完成",
        failed: "排版失败",
      } as Record<string, string>
    )[status] || "等待结果"
  );
}
function statusIcon(status: string) {
  return status === "succeeded"
    ? CheckCircle2
    : status === "failed"
      ? CircleAlert
      : status === "running"
        ? LoaderCircle
        : Clock3;
}
function sourceLabel(source?: string) {
  return (
    (
      { upload: "上传文档", paste: "粘贴文字", current: "当前文档" } as Record<
        string,
        string
      >
    )[source || ""] || "文本"
  );
}
function elapsed(job: LayoutJob) {
  if (!job.created_at) return "";
  const seconds = Math.max(
    0,
    Math.round(
      ((job.finished_at ? new Date(job.finished_at).getTime() : Date.now()) -
        new Date(job.created_at).getTime()) /
        1000,
    ),
  );
  if (!Number.isFinite(seconds)) return "";
  return seconds < 60
    ? `${seconds} 秒`
    : `${Math.floor(seconds / 60)} 分 ${seconds % 60} 秒`;
}
async function ensureDetail(job: LayoutJob, force = false): Promise<Block[]> {
  const id = keyOf(job),
    version = fingerprint(job),
    requestKey = `${id}:${version}`;
  if (!force && cache.value[id]?.fingerprint === version)
    return cache.value[id]!.blocks;
  const existing = requests.get(requestKey);
  if (existing) return existing;
  pending.value = { ...pending.value, [id]: true };
  errors.value = { ...errors.value, [id]: "" };
  const request = hrDocApi
    .getLayoutJob(Number(job.id))
    .then((response) => {
      const result = response.data?.blocks;
      if (
        !Array.isArray(result) ||
        result.some(
          (block) =>
            !block ||
            typeof block !== "object" ||
            typeof block.type !== "string",
        )
      )
        throw new Error("排版结果格式不完整，请重新加载。");
      const current = props.jobs.find((item) => keyOf(item) === id);
      if (current && fingerprint(current) === version)
        cache.value = {
          ...cache.value,
          [id]: { fingerprint: version, blocks: result },
        };
      return result;
    })
    .catch((failure) => {
      const current = props.jobs.find((item) => keyOf(item) === id);
      if (current && fingerprint(current) === version)
        errors.value = { ...errors.value, [id]: errText(failure) };
      throw failure;
    })
    .finally(() => {
      requests.delete(requestKey);
      pending.value = {
        ...pending.value,
        [id]: [...requests.keys()].some((key) => key.startsWith(`${id}:`)),
      };
    });
  requests.set(requestKey, request);
  return request;
}
async function loadSelected(force = false) {
  const job = selectedJob.value;
  if (!job || job.status !== "succeeded") return;
  try {
    await ensureDetail(job, force);
  } catch {
    /* 在结果区域保留错误与重试入口。 */
  }
}
function selectJob(job: LayoutJob, reveal = true) {
  selectedId.value = keyOf(job);
  if (reveal) mobileView.value = "preview";
  loadSelected();
  nextTick(() => {
    const container = list.value;
    const item = container?.querySelector<HTMLElement>(
      `[data-job-id="${CSS.escape(keyOf(job))}"]`,
    );
    if (!container?.clientHeight || !item) return;
    const bounds = container.getBoundingClientRect(),
      row = item.getBoundingClientRect();
    if (row.top < bounds.top || row.bottom > bounds.bottom)
      container.scrollTop += row.top - bounds.top;
  });
}
function scrollToTop() {
  reader.value?.scrollToTop();
}
async function apply() {
  const job = selectedJob.value;
  if (!job || applying.value || job.status !== "succeeded") return;
  const version = fingerprint(job);
  applying.value = true;
  try {
    const result = await ensureDetail(job);
    if (
      props.open &&
      selectedJob.value &&
      selectedId.value === keyOf(job) &&
      fingerprint(selectedJob.value) === version &&
      result.length
    )
      emit("apply", { job, blocks: JSON.parse(JSON.stringify(result)) });
  } catch {
    /* 与预览共享错误提示，不应用不完整内容。 */
  } finally {
    applying.value = false;
  }
}
watch(
  () =>
    [
      props.open,
      props.jobs.map((job) => `${keyOf(job)}:${fingerprint(job)}`).join("|"),
    ] as const,
  ([open]) => {
    if (!open) {
      focusMode.value = false;
      return;
    }
    if (!selectedJob.value && props.jobs[0]) selectJob(props.jobs[0], false);
    else loadSelected();
  },
  { immediate: true },
);
watch(selectedId, async () => {
  await nextTick();
  scrollToTop();
});
</script>
<style scoped>
.jd-overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: var(--overlay);
}
.jd-window {
  position: fixed;
  inset: 24px;
  z-index: 60;
  display: flex;
  flex-direction: column;
  max-width: 1380px;
  margin: 0 auto;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--card);
  color: var(--foreground);
  box-shadow: var(--shadow-xl-val);
  font-family: var(--font-sans);
  outline: none;
}
.jd-head {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 24px;
  border-bottom: 1px solid var(--border);
}
.jd-workspace {
  display: grid;
  flex: 1;
  min-height: 0;
  grid-template-columns: 248px minmax(0, 1fr);
}
.jd-jobs {
  display: flex;
  min-height: 0;
  flex-direction: column;
  border-right: 1px solid var(--border);
  background: var(--card);
}
.jd-list-heading {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px 12px;
  color: var(--muted-foreground);
  font-size: 12px;
}
.jd-list-heading h2 {
  font-weight: 500;
}
.jd-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 0 8px 12px;
  margin: 0;
  list-style: none;
}
.jd-item {
  position: relative;
  margin-bottom: 4px;
  border: 1px solid transparent;
  border-radius: 8px;
}
.jd-item.is-selected {
  border-color: color-mix(in srgb, var(--primary) 25%, var(--border));
  background: var(--info-soft);
}
.jd-select {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 5px;
  padding: 12px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.jd-select:hover {
  background: color-mix(in srgb, var(--primary) 4%, transparent);
}
.jd-select:focus-visible,
.jd-preview-link:focus-visible {
  outline: 2px solid var(--ring);
  outline-offset: -2px;
}
.jd-select strong {
  order: -1;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  font-size: 13px;
  font-weight: 550;
  line-height: 1.6;
  overflow-wrap: anywhere;
}
.jd-job-status {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 5px;
  color: var(--muted-foreground);
  font-size: 11px;
}
.jd-job-status svg {
  width: 13px;
  height: 13px;
}
.is-succeeded .jd-job-status > svg {
  color: var(--success);
}
.is-failed .jd-job-status > svg {
  color: var(--destructive);
}
.is-running .jd-job-status > svg {
  color: var(--primary);
}
.jd-source {
  margin-left: auto;
  color: var(--muted-foreground);
}
.jd-job-meta,
.jd-job-date {
  color: var(--muted-foreground);
  font-size: 11px;
  line-height: 1.5;
}
.jd-job-date {
  font-size: 10px;
}
.jd-preview-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  margin: -2px 0 12px 12px;
  padding: 3px 0;
  color: var(--primary);
  font-size: 11px;
  cursor: pointer;
}
.jd-detail {
  display: flex;
  min-height: 0;
  min-width: 0;
  flex-direction: column;
}
.jd-detail-nav {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-bottom: 1px solid var(--border);
}
.jd-result-heading {
  flex: 0 0 auto;
  padding: 18px 24px 16px;
}
.jd-result-heading h2 {
  font-size: 17px;
  font-weight: 600;
  line-height: 1.5;
  overflow-wrap: anywhere;
}
.jd-result-heading p {
  margin-top: 6px;
  max-height: 64px;
  overflow-y: auto;
  color: var(--muted-foreground);
  font-size: 12px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}
.jd-summary {
  margin-top: 5px;
}
.jd-summary summary {
  width: fit-content;
  padding: 3px 0;
  color: var(--muted-foreground);
  font-size: 12px;
  cursor: pointer;
}
.jd-summary summary:hover {
  color: var(--primary);
}
.jd-summary summary:focus-visible {
  outline: 2px solid var(--ring);
  outline-offset: 2px;
  border-radius: 4px;
}
.jd-reader-tools {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 0 24px 12px;
}
.jd-reader-tools > :last-child {
  margin-left: auto;
}
.jd-preview {
  flex: 1;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  background: var(--background);
  padding: 24px;
  outline-offset: -3px;
}
.jd-preview[data-reader-ready="true"] {
  padding: 0;
  overflow: hidden;
}
.jd-state {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 32px 16px;
  text-align: center;
}
.jd-state h3 {
  font-size: 15px;
  font-weight: 550;
}
.jd-state p {
  max-width: 460px;
  color: var(--muted-foreground);
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.jd-progress {
  width: min(320px, 100%);
  height: 6px;
  overflow: hidden;
  border-radius: 4px;
  background: var(--muted);
}
.jd-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--primary);
  transition: width 0.2s;
}
.jd-footer {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--border);
}
.jd-footer p {
  max-width: 440px;
  color: var(--muted-foreground);
  font-size: 11px;
  line-height: 1.7;
}
.jd-focus {
  grid-template-columns: minmax(0, 1fr);
}
.jd-focus .jd-jobs {
  display: none;
}
.jd-mobile-back {
  display: none;
}
@media (max-width: 1100px) {
  .jd-workspace {
    grid-template-columns: 250px minmax(0, 1fr);
  }
  .jd-focus {
    grid-template-columns: minmax(0, 1fr);
  }
  .jd-preview {
    padding: 18px;
  }
}
@media (max-width: 767px) {
  .jd-window {
    inset: 0;
    width: 100%;
    height: 100dvh;
    max-width: none;
    border: 0;
    border-radius: 0;
  }
  .jd-head {
    padding: 14px 16px;
  }
  .jd-workspace {
    display: flex;
  }
  .jd-jobs {
    width: 100%;
    border-right: 0;
  }
  .jd-detail {
    width: 100%;
  }
  .jd-workspace[data-mobile-view="list"] .jd-detail,
  .jd-workspace[data-mobile-view="preview"] .jd-jobs {
    display: none;
  }
  .jd-mobile-back {
    display: inline-flex;
  }
  .jd-focus-toggle {
    display: none;
  }
  .jd-list {
    padding: 0 10px 16px;
  }
  .jd-result-heading {
    padding: 14px 16px 12px;
  }
  .jd-result-heading h2 {
    font-size: 15px;
  }
  .jd-result-heading p {
    max-height: 42px;
    font-size: 11px;
  }
  .jd-reader-tools {
    padding: 0 12px 10px;
    gap: 6px;
  }
  .jd-reader-tools > :last-child {
    display: none;
  }
  .jd-preview {
    padding: 12px;
  }
  .jd-footer {
    padding: 12px 16px max(12px, env(safe-area-inset-bottom));
  }
  .jd-footer > div {
    width: 100%;
    justify-content: flex-end;
  }
}
@media (prefers-reduced-motion: reduce) {
  .jd-progress span {
    transition: none;
  }
}
</style>
