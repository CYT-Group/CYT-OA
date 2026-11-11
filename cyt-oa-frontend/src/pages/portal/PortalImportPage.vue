<template>
  <div class="space-y-5">
    <PageHeader
      title="批量导入"
      description="上传文件、核对数据，再确认导入。"
    />
    <ol class="grid max-w-4xl grid-cols-3 gap-2" aria-label="导入进度">
      <li
        v-for="(step, index) in ['选择文件', '核对数据', '完成导入']"
        :key="step"
        class="flex items-center gap-2 border-b-2 pb-3 text-xs sm:text-sm"
        :class="
          currentStep >= index + 1
            ? 'border-primary text-foreground'
            : 'border-border text-muted-foreground'
        "
        :aria-current="currentStep === index + 1 ? 'step' : undefined"
      >
        <span
          class="grid size-6 shrink-0 place-items-center rounded-full text-xs"
          :class="
            currentStep >= index + 1
              ? 'bg-primary text-primary-foreground'
              : 'bg-muted text-muted-foreground'
          "
          >{{ index + 1 }}</span
        >{{ step }}
      </li>
    </ol>
    <section class="panel max-w-4xl space-y-5 p-4 sm:p-5">
      <div class="flex flex-wrap items-end justify-between gap-3">
        <div class="min-w-48 flex-1 sm:max-w-xs">
          <Label for="import-type">导入内容</Label
          ><Select
            id="import-type"
            v-model="type"
            :options="types"
            :disabled="busy"
          />
        </div>
        <Button
          variant="outline"
          size="sm"
          :disabled="busy"
          @click="downloadTemplate"
          ><Download class="size-4" />下载导入模板</Button
        >
      </div>
      <div
        class="rounded-xl border border-dashed p-6 text-center transition-colors sm:p-8"
        :class="
          dragging ? 'border-primary bg-primary/5' : 'border-border bg-muted/20'
        "
        @dragover.prevent="dragging = !busy"
        @dragleave.prevent="dragging = false"
        @drop.prevent="dropFile"
      >
        <input
          id="import-file"
          ref="fileInput"
          type="file"
          accept=".csv,.xlsx,.xls"
          class="sr-only"
          :disabled="busy"
          aria-label="选择导入文件"
          @change="pickFile"
        />
        <component
          :is="file ? FileSpreadsheet : Upload"
          class="mx-auto mb-3 size-8 text-muted-foreground"
          :stroke-width="1.5"
        />
        <p class="break-all text-sm font-medium">
          {{ file?.name || "将 CSV 或 Excel 文件拖到这里" }}
        </p>
        <p class="mt-1.5 text-xs text-muted-foreground">
          {{
            file ? formatSize(file.size) : "支持 .csv、.xlsx、.xls，最大 10MB"
          }}
        </p>
        <div class="mt-4 flex justify-center gap-2">
          <Button
            variant="outline"
            size="sm"
            :disabled="busy"
            @click="fileInput?.click()"
            >{{ file ? "更换文件" : "选择文件" }}</Button
          ><Button
            v-if="file"
            variant="ghost"
            size="sm"
            :disabled="busy"
            @click="acceptFile(null)"
            >移除</Button
          >
        </div>
      </div>
      <p class="text-xs leading-relaxed text-muted-foreground">
        最多 2000 行。CSV 使用 UTF-8 编码；Excel
        公式请先粘贴为值。下载模板可查看必填列和示例。
      </p>
      <Callout v-if="error" tone="destructive" title="文件处理失败">{{
        error
      }}</Callout>
      <div
        class="flex flex-wrap items-center justify-between gap-3 border-t pt-4"
      >
        <span class="text-xs text-muted-foreground">{{
          result?.preview
            ? "核对下方结果后，可导入通过校验的记录。"
            : "检查文件只做校验，不写入数据。"
        }}</span>
        <div class="flex gap-2">
          <Button
            :variant="result?.preview ? 'outline' : 'default'"
            :loading="busy"
            :disabled="!file"
            @click="run(true)"
            >{{ result?.preview ? "重新检查" : "检查文件" }}</Button
          ><Button
            v-if="result?.preview && result.valid > 0"
            :loading="busy"
            @click="confirmOpen = true"
            >导入 {{ result.valid }} 条记录</Button
          >
        </div>
      </div>
    </section>
    <section v-if="result" class="panel max-w-4xl overflow-hidden">
      <div class="panel-header">
        <h2 class="panel-title">
          {{ result.preview ? "文件检查结果" : "导入结果" }}
        </h2>
        <Badge :variant="result.failed ? 'outline' : 'secondary'">{{
          result.preview ? "待确认" : "已完成"
        }}</Badge>
      </div>
      <div class="grid grid-cols-3 divide-x border-b py-4">
        <div
          v-for="metric in [
            { label: '文件记录', value: result.total },
            {
              label: result.preview ? '通过校验' : '成功导入',
              value: result.preview ? result.valid : result.imported,
            },
            { label: '需修正', value: result.failed },
          ]"
          :key="metric.label"
          class="px-4 sm:px-5"
        >
          <p class="text-xs text-muted-foreground">{{ metric.label }}</p>
          <p class="mt-1 text-xl font-semibold tabular-nums">
            {{ metric.value }}
          </p>
        </div>
      </div>
      <div v-if="result.errors.length" class="max-h-80 overflow-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th class="w-28">文件行号</th>
              <th>需要修正的内容</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in result.errors" :key="item.row">
              <td class="tabular-nums">{{ item.row }}</td>
              <td class="whitespace-normal!">{{ item.message }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="flex items-center gap-2 p-5 text-sm">
        <CheckCircle2 class="size-4 text-success" />{{
          result.preview
            ? "全部记录校验通过，可以确认导入。"
            : "记录已导入，可前往对应管理页面查看。"
        }}
      </div>
      <Callout
        v-if="!result.preview && result.imported > 0"
        class="m-4"
        title="导入已完成"
        >再次导入同一文件会创建新记录，请先核对列表。</Callout
      >
    </section>
    <section v-if="session.isAdmin" class="max-w-4xl border-t pt-4">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <p class="text-xs text-muted-foreground">
          员工身份与部门以企业微信为准，请在组织架构中同步。
        </p>
        <Button size="sm" variant="ghost" @click="router.push('/org')"
          >管理组织架构<ArrowRight class="size-3.5"
        /></Button>
      </div>
    </section>
    <ConfirmDialog
      :open="confirmOpen"
      title="确认批量导入"
      :description="`将创建 ${result?.valid ?? 0} 条内容。无效行会单独报告，培训课程保存为草稿。`"
      :busy="busy"
      confirm-text="确认导入"
      @update:open="confirmOpen = $event"
      @confirm="run(false)"
    />
  </div>
</template>
<script setup lang="ts">
import { computed, ref, watch } from "vue";
import {
  Upload,
  FileSpreadsheet,
  Download,
  CheckCircle2,
  ArrowRight,
} from "@lucide/vue";
import Badge from "../../ui/Badge.vue";
import { useRouter } from "vue-router";
import { api } from "../../lib/api";
import { session } from "../../lib/session";
import { errText, toast } from "../../lib/toast";
import PageHeader from "../../ui/PageHeader.vue";
import Label from "../../ui/Label.vue";
import Select from "../../ui/Select.vue";
import Button from "../../ui/Button.vue";
import Callout from "../../ui/Callout.vue";
import ConfirmDialog from "../../ui/ConfirmDialog.vue";
interface ImportResult {
  preview: boolean;
  total: number;
  valid: number;
  imported: number;
  failed: number;
  errors: { row: number; message: string }[];
}
const router = useRouter(),
  type = ref("wiki"),
  file = ref<File | null>(null),
  fileInput = ref<HTMLInputElement | null>(null),
  busy = ref(false),
  error = ref(""),
  result = ref<ImportResult | null>(null),
  confirmOpen = ref(false);
const types = [
  { label: "知识库", value: "wiki" },
  { label: "培训课程", value: "training" },
  { label: "展会活动", value: "events" },
  { label: "新品动态", value: "products" },
];
const templates: Record<string, string> = {
  wiki: "标题,分类,部门,内容,链接,标签\n知识文档示例,通用,,,,",
  training: "标题,分类,级别,描述,讲师\n课程示例,入职培训,选修,,",
  events:
    "标题,描述,地点,开始日期,结束日期,类型\n展会示例,,,2026-11-10,2026-11-11,exhibition",
  products: "名称,品牌,分类,描述,链接,上市日期\n产品示例,,,,,2026-11-10",
};
const dragging = ref(false);
const currentStep = computed(() =>
  result.value ? (result.value.preview ? 2 : 3) : 1,
);
function formatSize(bytes: number) {
  return bytes < 1024 * 1024
    ? `${(bytes / 1024).toFixed(1)} KB`
    : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}
function acceptFile(next: File | null) {
  if (busy.value) return;
  result.value = null;
  error.value = "";
  file.value = null;
  if (fileInput.value) fileInput.value.value = "";
  if (next && !/\.(csv|xlsx|xls)$/i.test(next.name)) {
    error.value = "请选择 CSV 或 Excel 文件。";
    return;
  }
  if (next && next.size > 10 * 1024 * 1024) {
    error.value = "文件超过 10MB，请拆分后再导入。";
    return;
  }
  file.value = next;
}
function pickFile(event: Event) {
  acceptFile((event.target as HTMLInputElement).files?.[0] ?? null);
}
function dropFile(event: DragEvent) {
  dragging.value = false;
  if (!busy.value) acceptFile(event.dataTransfer?.files[0] ?? null);
}
function downloadTemplate() {
  const url = URL.createObjectURL(
    new Blob(["\uFEFF" + templates[type.value] + "\n"], {
      type: "text/csv;charset=utf-8",
    }),
  );
  const a = document.createElement("a");
  a.href = url;
  a.download = `${type.value}_template.csv`;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
async function run(preview: boolean) {
  if (!file.value || busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    const form = new FormData();
    form.append("file", file.value);
    form.append("type", type.value);
    form.append("preview", String(preview));
    result.value = await api.post<ImportResult>(
      "/admin-api/portal/import",
      form,
      120000,
    );
    confirmOpen.value = false;
    if (!preview) {
      toast(`导入完成：${result.value.imported} 条`);
      file.value = null;
      if (fileInput.value) fileInput.value.value = "";
    }
  } catch (e) {
    error.value = errText(e);
  } finally {
    busy.value = false;
  }
}
watch(type, () => {
  result.value = null;
  error.value = "";
});
</script>
