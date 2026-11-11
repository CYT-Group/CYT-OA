<template>
  <DialogRoot :open="open" @update:open="emit('update:open', $event)">
    <DialogPortal>
      <DialogOverlay class="versions-overlay" />
      <DialogContent class="versions-window">
        <header class="versions-heading">
          <div class="min-w-0">
            <DialogTitle class="text-base font-semibold">历史版本</DialogTitle
            ><DialogDescription
              class="mt-1 truncate text-xs text-muted-foreground"
              >{{ title }} · 查看发布时的内容快照</DialogDescription
            >
          </div>
          <DialogClose as-child
            ><Button variant="ghost" size="icon-sm" aria-label="关闭历史版本"
              ><X /></Button
          ></DialogClose>
        </header>
        <div v-if="listLoading" class="versions-state" role="status">
          <LoaderCircle class="size-6 animate-spin text-primary" />
          <p>正在读取版本记录…</p>
        </div>
        <div v-else-if="listError" class="versions-state" role="alert">
          <p>{{ listError }}</p>
          <Button variant="outline" size="sm" @click="loadVersions"
            >重新加载</Button
          >
        </div>
        <EmptyState
          v-else-if="!versions.length"
          class="m-auto"
          title="暂无已发布版本"
          description="首次发布后会保留版本快照，可在这里回看。"
        />
        <div v-else class="versions-layout" :data-view="mobileView">
          <aside class="versions-list" aria-label="历史版本列表">
            <h2>
              发布记录 <span>{{ versions.length }} 个版本</span>
            </h2>
            <ol class="hr-version-table">
              <li v-for="(item, index) in versions" :key="item.version">
                <button
                  type="button"
                  :aria-pressed="selectedVersion === item.version"
                  :class="{ selected: selectedVersion === item.version }"
                  @click="selectVersion(item.version)"
                >
                  <span class="version-label"
                    >{{ item.version
                    }}<Badge v-if="!index" variant="secondary"
                      >最新版本</Badge
                    ></span
                  ><span class="version-title">{{ item.title || title }}</span
                  ><span class="version-meta">{{
                    item.published_by || "—"
                  }}</span
                  ><time>{{ formatPortalDateTime(item.published_at) }}</time
                  ><span class="version-view"
                    >查看快照<ChevronRight class="size-3"
                  /></span>
                </button>
              </li>
            </ol>
          </aside>
          <section class="versions-preview" aria-label="历史版本快照">
            <div class="versions-toolbar">
              <Button
                variant="ghost"
                size="sm"
                class="versions-mobile-back"
                @click="mobileView = 'list'"
                ><ArrowLeft />版本列表</Button
              ><span class="text-sm font-medium">{{ selectedVersion }}</span
              ><span class="ml-auto text-xs text-muted-foreground"
                >历史快照 · 只读</span
              >
            </div>
            <div v-if="detailLoading" class="versions-state" role="status">
              <LoaderCircle class="size-6 animate-spin text-primary" />
              <p>正在加载版本正文…</p>
            </div>
            <div v-else-if="detailError" class="versions-state" role="alert">
              <p>{{ detailError }}</p>
              <Button
                variant="outline"
                size="sm"
                @click="loadDetail(selectedVersion)"
                >重新加载</Button
              >
            </div>
            <PortalDocumentReader
              v-else-if="snapshot"
              :blocks="snapshot.content_blocks"
              :html="snapshot.content_html || ''"
              :theme="snapshot.theme || ''"
              :content-key="selectedVersion"
            />
          </section>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
<script setup lang="ts">
import { ref, watch } from "vue";
import {
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
} from "reka-ui";
import { ArrowLeft, ChevronRight, LoaderCircle, X } from "@lucide/vue";
import { articleApi } from "../bridge/api";
import { formatPortalDateTime } from "../../../../lib/portal-management-format";
import { errText } from "../../../../lib/toast";
import Button from "../../../../ui/Button.vue";
import Badge from "../../../../ui/Badge.vue";
import EmptyState from "../../../../ui/EmptyState.vue";
import PortalDocumentReader from "./PortalDocumentReader.vue";
const props = defineProps<{
  open: boolean;
  documentId: number | string;
  title: string;
}>();
const emit = defineEmits<{ "update:open": [value: boolean] }>();
const versions = ref<Record<string, any>[]>([]),
  snapshot = ref<Record<string, any> | null>(null),
  selectedVersion = ref(""),
  mobileView = ref("list");
const listLoading = ref(false),
  detailLoading = ref(false),
  listError = ref(""),
  detailError = ref("");
let listRequest = 0,
  detailRequest = 0;
async function loadVersions() {
  const request = ++listRequest;
  ++detailRequest;
  listLoading.value = true;
  listError.value = "";
  versions.value = [];
  snapshot.value = null;
  selectedVersion.value = "";
  try {
    const response = await articleApi.adminVersions(Number(props.documentId));
    if (request !== listRequest || !props.open) return;
    versions.value = response.data || [];
    if (versions.value[0]) {
      selectedVersion.value = versions.value[0].version;
      loadDetail(selectedVersion.value);
    }
  } catch (error) {
    if (request === listRequest && props.open) listError.value = errText(error);
  } finally {
    if (request === listRequest) listLoading.value = false;
  }
}
async function loadDetail(version: string) {
  const request = ++detailRequest;
  detailLoading.value = true;
  detailError.value = "";
  snapshot.value = null;
  try {
    const response = await articleApi.adminVersion(
      Number(props.documentId),
      version,
    );
    if (request === detailRequest && props.open) snapshot.value = response.data;
  } catch (error) {
    if (request === detailRequest && props.open)
      detailError.value = errText(error);
  } finally {
    if (request === detailRequest) detailLoading.value = false;
  }
}
function selectVersion(version: string) {
  selectedVersion.value = version;
  mobileView.value = "preview";
  loadDetail(version);
}
watch(
  () => [props.open, props.documentId],
  ([open]) => {
    if (open) {
      mobileView.value = "list";
      loadVersions();
    } else {
      ++listRequest;
      ++detailRequest;
    }
  },
  { immediate: true },
);
</script>
<style scoped>
.versions-overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: var(--overlay);
}
.versions-window {
  position: fixed;
  inset: 24px;
  z-index: 60;
  display: flex;
  max-width: 1380px;
  margin: 0 auto;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--card);
  color: var(--foreground);
  font-family: var(--font-sans);
  box-shadow: var(--shadow-xl-val);
  outline: none;
}
.versions-heading {
  display: flex;
  flex: 0 0 auto;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px 24px;
  border-bottom: 1px solid var(--border);
}
.versions-layout {
  display: grid;
  flex: 1;
  min-height: 0;
  grid-template-columns: 224px minmax(0, 1fr);
}
.versions-list {
  display: flex;
  min-height: 0;
  flex-direction: column;
  border-right: 1px solid var(--border);
}
.versions-list h2 {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 16px;
  color: var(--foreground);
  font-size: 12px;
  font-weight: 550;
}
.versions-list h2 span {
  color: var(--muted-foreground);
  font-size: 11px;
  font-weight: 400;
}
.hr-version-table {
  overflow-y: auto;
  margin: 0;
  padding: 0 8px 12px;
  list-style: none;
}
.hr-version-table li {
  margin-bottom: 6px;
}
.hr-version-table button {
  display: flex;
  width: 100%;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 14px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  text-align: left;
  cursor: pointer;
}
.hr-version-table button.selected {
  background: var(--info-soft);
  border-color: color-mix(in srgb, var(--primary) 25%, var(--border));
}
.hr-version-table button:hover {
  background: var(--muted);
}
.hr-version-table button:focus-visible {
  outline: 2px solid var(--ring);
  outline-offset: -2px;
}
.version-label {
  display: flex;
  width: 100%;
  justify-content: space-between;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
}
.version-title {
  font-size: 12px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}
.version-meta,
.hr-version-table time {
  color: var(--muted-foreground);
  font-size: 11px;
}
.version-view {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  color: var(--primary);
  font-size: 11px;
}
.versions-preview {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
}
.versions-toolbar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--border);
}
.versions-state {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 24px;
  color: var(--muted-foreground);
  font-size: 13px;
  text-align: center;
}
.versions-preview :deep(.document-reader) {
  flex: 1;
  height: auto;
}
.versions-mobile-back {
  display: none;
}
@media (max-width: 767px) {
  .versions-window {
    inset: 0;
    width: 100%;
    height: 100dvh;
    border: 0;
    border-radius: 0;
  }
  .versions-heading {
    padding: 14px 16px;
  }
  .versions-layout {
    display: flex;
  }
  .versions-list,
  .versions-preview {
    width: 100%;
  }
  .versions-layout[data-view="list"] .versions-preview,
  .versions-layout[data-view="preview"] .versions-list {
    display: none;
  }
  .versions-mobile-back {
    display: inline-flex;
  }
}
</style>
