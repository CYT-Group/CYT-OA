<template>
  <div>
    <PageHeader
      title="制度签收看板"
      description="查看在职员工的制度签收覆盖率，按部门筛选并导出未签名单。"
      ><template #actions
        ><Button variant="outline" :disabled="loading" @click="load"
          ><RefreshCw class="size-4" />刷新</Button
        ></template
      ></PageHeader
    >
    <section class="panel mt-4 p-4">
      <form
        class="grid grid-cols-1 items-end gap-3 sm:grid-cols-[minmax(0,1fr)_11rem_auto]"
        @submit.prevent="load"
      >
        <div class="min-w-0">
          <label for="signoff-document" class="field-label">制度文档</label
          ><Select
            id="signoff-document"
            v-model="selected"
            :options="documentOptions"
            @update:model-value="
              version = '';
              load();
            "
          />
        </div>
        <div class="min-w-0">
          <label for="signoff-version" class="field-label"
            >历史版本（留空看当前）</label
          ><Input
            id="signoff-version"
            v-model="version"
            placeholder="例如 v1.0"
            maxlength="16"
          />
        </div>
        <Button type="submit" :disabled="loading">查询</Button>
      </form>
      <p v-if="documentsError" class="mt-3 text-xs text-destructive">
        {{ documentsError }}
        <button type="button" class="underline" @click="loadDocuments">
          重新加载文档选项
        </button>
      </p>
    </section>
    <Callout
      v-if="error"
      title="签收统计加载失败"
      tone="destructive"
      class="mt-4"
      >{{ error
      }}<template #action
        ><Button size="sm" variant="outline" @click="load"
          >重试</Button
        ></template
      ></Callout
    >
    <div
      v-if="loading"
      class="panel mt-4 p-10 text-center text-sm text-muted-foreground"
    >
      正在加载签收统计…
    </div>
    <template v-else-if="stats">
      <div class="mt-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <section
          v-for="metric in metrics"
          :key="metric.label"
          class="panel p-4"
        >
          <p class="text-xs text-muted-foreground">{{ metric.label }}</p>
          <p class="mt-2 text-2xl font-semibold tabular-nums">
            {{ metric.value }}
          </p>
        </section>
      </div>
      <section class="panel mt-4 p-4">
        <div
          class="mb-2 flex items-center justify-between gap-3 text-xs text-muted-foreground"
        >
          <span>当前统计版本：{{ stats.version }}</span
          ><span
            >{{ stats.signed_count }} / {{ stats.total_active }} 已签收</span
          >
        </div>
        <div
          class="h-2 overflow-hidden rounded-full bg-muted"
          role="progressbar"
          :aria-valuenow="stats.coverage_pct"
          :aria-valuemin="0"
          :aria-valuemax="100"
          aria-label="制度签收覆盖率"
        >
          <div
            class="h-full bg-primary transition-all"
            :style="{
              width: `${Math.max(0, Math.min(100, stats.coverage_pct))}%`,
            }"
          />
        </div>
      </section>
      <section class="panel mt-4 overflow-hidden">
        <div class="table-caption">
          <h2 class="panel-title">
            未签收员工
            <Badge variant="secondary">{{ stats.not_signed_count }} 人</Badge>
          </h2>
        </div>
        <div class="table-toolbar flex flex-wrap gap-2">
          <Input
            v-model="search"
            aria-label="搜索未签员工"
            placeholder="搜索姓名或账号"
            class="sm:w-64"
          /><Select
            v-model="department"
            :options="departmentOptions"
            aria-label="按部门筛选"
            class="sm:w-48"
          /><Button
            variant="outline"
            :disabled="!filtered.length"
            @click="exportCsv"
            ><Download class="size-4" />导出 CSV（{{
              filtered.length
            }}）</Button
          ><Button
            :disabled="
              !stats.not_signed_count || notifying || !!notifyUnavailable
            "
            @click="notifyOpen = true"
            >发送企微催签（{{ stats.not_signed_count }}）</Button
          >
        </div>
        <p
          v-if="notifyUnavailable"
          class="mx-5 mb-3 text-xs text-muted-foreground"
        >
          {{ notifyUnavailable }}
        </p>
        <Callout
          v-if="notifyResult"
          class="m-4"
          :tone="notifyFailed ? 'destructive' : undefined"
          title="催签结果"
          >{{ notifyResult }}</Callout
        >
        <div v-if="filtered.length" class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>#</th>
                <th>姓名</th>
                <th>账号</th>
                <th>部门</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(person, i) in paged" :key="person.username">
                <td class="text-muted-foreground">
                  {{ (listPage - 1) * 20 + i + 1 }}
                </td>
                <td class="font-medium">
                  {{ person.display_name || person.username }}
                </td>
                <td>{{ person.username }}</td>
                <td>{{ person.department || "—" }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <EmptyState
          v-else
          :title="
            stats.not_signed_count === 0 ? '全员已签收' : '没有匹配的未签员工'
          "
          :description="
            stats.not_signed_count === 0
              ? '当前版本已完成全部在职员工签收。'
              : '调整姓名或部门筛选条件。'
          "
        /><Pagination
          class="border-t px-5 py-3"
          :page="listPage"
          :page-size="20"
          :total="filtered.length"
          @update:page="listPage = $event"
        />
      </section>
    </template>
    <ConfirmDialog
      v-model:open="notifyOpen"
      title="确认向未签员工发送企微提醒？"
      :description="`将向当前制度 ${stats?.version ?? ''} 的全部 ${stats?.not_signed_count ?? 0} 位未签员工发送阅读与签收提醒。上方搜索和部门筛选仅用于查看及导出，不缩小催签范围。`"
      confirm-text="发送提醒"
      :busy="notifying"
      @confirm="notify"
    />
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from 'vue-router';
import { RefreshCw, Download } from "@lucide/vue";
import PageHeader from "../../ui/PageHeader.vue";
import Button from "../../ui/Button.vue";
import Input from "../../ui/Input.vue";
import Select from "../../ui/Select.vue";
import Badge from "../../ui/Badge.vue";
import Callout from "../../ui/Callout.vue";
import EmptyState from "../../ui/EmptyState.vue";
import Pagination from "../../ui/Pagination.vue";
import ConfirmDialog from "../../ui/ConfirmDialog.vue";
import {
  hrApi,
  csvDownload,
  type HrDocument,
  type SignoffStats,
} from "../../lib/portal-hr";
import { errText } from "../../lib/toast";
const route = useRoute(), router = useRouter();
function routeDocument() {
  if (/^[1-9]\d{0,18}$/.test(String(route.query.documentId ?? ''))) return `id:${route.query.documentId}`;
  if (['incentive', 'handbook'].includes(String(route.query.document))) return `legacy:${route.query.document}`;
  return '';
}
const selected = ref(routeDocument()),
  version = ref(typeof route.query.version === 'string' ? route.query.version : ''),
  loading = ref(true),
  error = ref(""),
  stats = ref<SignoffStats | null>(null);
const documents = ref<HrDocument[]>([]),
  documentsError = ref(""),
  search = ref(""),
  department = ref(""),
  listPage = ref(1);
const notifyOpen = ref(false),
  notifying = ref(false),
  notifyResult = ref(""),
  notifyFailed = ref(false);
const documentOptions = computed(() => [
  { value: "legacy:incentive", label: "奖惩与纪律责任制度" },
  { value: "legacy:handbook", label: "员工手册" },
  ...documents.value.map((doc) => ({
    value: `id:${doc.id}`,
    label: `${doc.title} · ${doc.version}`,
  })),
]);
const selectedDocument = computed(() =>
  documents.value.find((doc) => `id:${doc.id}` === selected.value),
);
const notifyUnavailable = computed(() => {
  const doc = selectedDocument.value;
  if (doc?.offlineAt)
    return "制度已下架，可继续核对统计和导出名单，重新上架后再催签。";
  if (doc && stats.value && stats.value.version !== doc.version)
    return "当前为历史版本，请切回已发布版本后催签。";
  return "";
});
const departmentOptions = computed(() => [
  { value: "", label: "所有部门" },
  ...Array.from(
    new Set(
      (stats.value?.not_signed ?? [])
        .map((person) => person.department)
        .filter((v): v is string => Boolean(v)),
    ),
  )
    .sort()
    .map((value) => ({ value, label: value })),
]);
const filtered = computed(() => {
  const q = search.value.trim().toLowerCase();
  return (stats.value?.not_signed ?? []).filter(
    (person) =>
      (!department.value || person.department === department.value) &&
      (!q ||
        `${person.display_name ?? ""} ${person.username}`
          .toLowerCase()
          .includes(q)),
  );
});
const paged = computed(() =>
  filtered.value.slice((listPage.value - 1) * 20, listPage.value * 20),
);
const metrics = computed(() =>
  stats.value
    ? [
        { label: "在职员工", value: stats.value.total_active },
        { label: "已签收", value: stats.value.signed_count },
        { label: "未签收", value: stats.value.not_signed_count },
        { label: "签收覆盖率", value: `${stats.value.coverage_pct}%` },
      ]
    : [],
);
watch([search, department], () => {
  listPage.value = 1;
});
let request = 0;
async function load() {
  if (!selected.value) return;
  const current = ++request;
  const next = { ...route.query };
  delete next.documentId; delete next.document; delete next.version;
  if (selected.value.startsWith('id:')) next.documentId = selected.value.slice(3);
  else next.document = selected.value.slice(7);
  if (version.value.trim()) next.version = version.value.trim();
  if (router.resolve({ query: next }).fullPath !== route.fullPath) void router.replace({ query: next });
  loading.value = true;
  error.value = "";
  stats.value = null;
  listPage.value = 1;
  notifyResult.value = "";
  try {
    const ref = selected.value.startsWith("id:")
      ? { documentId: Number(selected.value.slice(3)) }
      : { document: selected.value.slice(7) };
    const result = await hrApi.stats({
      ...ref,
      version: version.value.trim() || undefined,
    });
    if (current === request) {
      stats.value = result;
      department.value = "";
    }
  } catch (e) {
    if (current === request) error.value = errText(e);
  } finally {
    if (current === request) loading.value = false;
  }
}
async function notify() {
  if (!stats.value || notifying.value || notifyUnavailable.value || !stats.value.not_signed_count) return;
  const targetDocument = selected.value, targetVersion = stats.value.version;
  notifying.value = true;
  notifyResult.value = "";
  try {
    const docRef = selected.value.startsWith("id:")
      ? { documentId: Number(selected.value.slice(3)) }
      : { document: selected.value.slice(7) };
    const result = await hrApi.notify({
      ...docRef,
      version: stats.value.version,
    });
    if (selected.value !== targetDocument || stats.value?.version !== targetVersion) return;
    notifyFailed.value = result.failed > 0;
    notifyResult.value = `目标 ${result.targetCount} 人，成功发送 ${result.sent} 人，失败 ${result.failed} 人。${result.errors?.length ? result.errors.join("；") : ""}`;
    notifyOpen.value = false;
  } catch (e) {
    if (selected.value !== targetDocument || stats.value?.version !== targetVersion) return;
    notifyFailed.value = true;
    notifyResult.value = errText(e);
    notifyOpen.value = false;
  } finally {
    notifying.value = false;
  }
}
watch(() => [route.query.documentId, route.query.document, route.query.version], () => {
  const fromRoute = routeDocument();
  const nextVersion = typeof route.query.version === 'string' ? route.query.version : '';
  if ((!fromRoute || fromRoute === selected.value) && nextVersion === version.value) return;
  if (fromRoute) selected.value = fromRoute;
  version.value = nextVersion;
  notifyOpen.value = false;
  void load();
});
async function loadDocuments() {
  documentsError.value = "";
  try {
    documents.value = await hrApi.signoffDocuments();
    if (!selected.value) {
      const initial =
        documents.value.find((doc) => !doc.offlineAt) ?? documents.value[0];
      selected.value = initial ? `id:${initial.id}` : "legacy:incentive";
    }
  } catch (e) {
    documentsError.value = `文档选项加载失败：${errText(e)}`;
    if (!selected.value) selected.value = "legacy:incentive";
  }
}
function exportCsv() {
  if (!stats.value) return;
  const title =
    documentOptions.value.find((doc) => doc.value === selected.value)?.label ??
    "制度";
  csvDownload(`${title}_${stats.value.version}_未签收.csv`, [
    ["姓名", "账号", "部门", "制度版本"],
    ...filtered.value.map((person) => [
      person.display_name || "",
      person.username,
      person.department || "",
      stats.value!.version,
    ]),
  ]);
}
onMounted(async () => {
  await loadDocuments();
  await load();
});
</script>
