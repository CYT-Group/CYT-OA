<template>
  <div class="space-y-4">
    <PageHeader
      :title="costsOnly ? 'API 成本' : 'AI 管理'"
      :description="
        costsOnly
          ? '按月份查看调用量、token 用量和估算费用'
          : 'Gemini 模型、档位、密钥与功能提示词'
      "
    />
    <SegmentedTabs v-if="!costsOnly" v-model="tab" :items="tabs" />
    <Callout v-if="error" tone="destructive" title="加载失败"
      >{{ error
      }}<template #action
        ><Button @click="load">重试</Button></template
      ></Callout
    >
    <div v-if="loading" class="panel p-8 text-sm text-muted-foreground">
      正在加载…
    </div>
    <template v-else-if="tab === 'models' && !costsOnly">
      <section class="panel space-y-4 p-5">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <h2 class="panel-title">模型目录</h2>
          <div class="flex gap-2">
            <Button
              size="sm"
              variant="outline"
              :loading="busy"
              @click="syncModels"
              >从 Gemini 同步</Button
            ><Button size="sm" @click="editModel()"
              ><Plus class="size-4" />添加模型</Button
            >
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>模型</th>
                <th>标识</th>
                <th>状态</th>
                <th>输入 / 输出价</th>
                <th class="sticky-col text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in currentModels" :key="m.id">
                <td>
                  <span class="portal-table-title" :title="m.displayName">{{
                    m.displayName
                  }}</span>
                </td>
                <td>
                  <span
                    class="block max-w-64 truncate text-xs text-muted-foreground"
                    :title="m.modelId"
                    >{{ m.modelId }}</span
                  >
                </td>
                <td>
                  <Badge :variant="m.enabled ? 'default' : 'secondary'">{{
                    m.enabled ? "启用" : "停用"
                  }}</Badge>
                </td>
                <td>{{ m.inputPrice ?? "—" }} / {{ m.outputPrice ?? "—" }}</td>
                <td class="sticky-col text-right">
                  <div class="flex items-center justify-end gap-1">
                    <Button size="sm" variant="ghost" @click="editModel(m)"
                      >编辑</Button
                    ><PortalRowActions
                      :actions="[
                        {
                          label: '删除模型',
                          destructive: true,
                          onSelect: () => askDelete(m),
                        },
                      ]"
                      :label="`${m.displayName}的更多操作`"
                    />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <p
            v-if="!currentModels.length"
            class="p-5 text-sm text-muted-foreground"
          >
            暂无模型，请添加模型或迁入旧模型目录。
          </p>
        </div>
        <p class="text-xs text-muted-foreground">
          价格单位：美元 / 百万 token；未维护价格的调用仅统计用量。
        </p>
      </section>
      <section class="panel space-y-4 p-5">
        <h2 class="panel-title">默认档位</h2>
        <div class="grid gap-4 sm:grid-cols-3">
          <div v-for="tier in tiers" :key="tier.key">
            <Label :for="tier.key">{{ tier.label }}</Label
            ><Select
              :id="tier.key"
              v-model="provider[tier.key]"
              :options="modelOptions"
              placeholder="选择模型"
            />
          </div>
        </div>
        <Button size="sm" :loading="busy" @click="saveProvider"
          >保存档位</Button
        >
      </section>
      <section v-if="session.isAdmin" class="panel space-y-4 p-5">
        <h2 class="panel-title">API 密钥</h2>
        <p class="text-sm text-muted-foreground">
          页面密钥：{{
            data.providers?.[0]?.apiKeyHint || "未设置"
          }}。密钥保存后只显示末四位。
        </p>
        <div class="flex flex-wrap items-end gap-2">
          <div class="min-w-48 flex-1">
            <Label for="ai-key">新密钥</Label
            ><Input
              id="ai-key"
              v-model="key"
              type="password"
              autocomplete="new-password"
              placeholder="输入 Gemini API key"
            />
          </div>
          <Button :loading="busy" :disabled="!key" @click="saveKey"
            >保存密钥</Button
          ><Button
            variant="outline"
            :loading="busy"
            @click="
              confirm = {
                text: '清除页面密钥？之后使用服务器环境配置。',
                run: clearKey,
              }
            "
            >清除</Button
          >
        </div>
      </section>
    </template>
    <section
      v-else-if="tab === 'tasks' && !costsOnly"
      class="grid min-w-0 items-start gap-4 lg:grid-cols-[220px_minmax(0,1fr)]"
    >
      <div class="lg:hidden">
        <Label for="ai-task-picker">选择功能</Label
        ><Select
          id="ai-task-picker"
          :model-value="selectedTask?.name || ''"
          :options="
            data.tasks.map((task) => ({ label: task.label, value: task.name }))
          "
          @update:model-value="requestTask(String($event))"
        />
      </div>
      <div class="panel hidden max-h-[650px] overflow-auto p-2 lg:block">
        <Input
          v-model="taskSearch"
          placeholder="搜索功能"
          aria-label="搜索 AI 功能"
          class="mb-2"
        />
        <button
          v-for="t in filteredTasks"
          :key="t.name"
          class="mb-1 w-full rounded-lg p-3 text-left text-sm hover:bg-accent"
          :class="
            selectedTask?.name === t.name ? 'bg-primary/10 text-primary' : ''
          "
          @click="requestTask(t.name)"
        >
          <b class="block font-medium">{{ t.label }}</b
          ><small class="mt-1 block text-xs text-muted-foreground">{{
            t.name
          }}</small>
        </button>
      </div>
      <div v-if="selectedTask" class="panel min-w-0 space-y-4 p-4 sm:p-5">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <h2 class="panel-title">{{ selectedTask.label }}</h2>
          <Badge v-if="taskDirty" variant="outline">未保存</Badge>
        </div>
        <p class="text-xs text-muted-foreground">
          {{ selectedTask.description }}
        </p>
        <details class="rounded-lg border bg-muted/20 p-3 text-xs">
          <summary class="cursor-pointer font-medium">
            查看当前生效提示词
          </summary>
          <div class="mt-3 max-h-64 space-y-3 overflow-auto">
            <div>
              <p class="mb-1 text-muted-foreground">系统提示词</p>
              <pre
                class="whitespace-pre-wrap break-words font-sans leading-relaxed"
                >{{
                  selectedTask.override?.systemPrompt ||
                  selectedTask.systemPrompt ||
                  "未设置"
                }}</pre
              >
            </div>
            <div>
              <p class="mb-1 text-muted-foreground">用户模板</p>
              <pre
                class="whitespace-pre-wrap break-words font-sans leading-relaxed"
                >{{
                  selectedTask.override?.userTemplate ||
                  selectedTask.userTemplate ||
                  "未设置"
                }}</pre
              >
            </div>
          </div>
          <Button
            class="mt-3"
            size="sm"
            variant="outline"
            @click="loadEffectivePrompts"
            >载入编辑区</Button
          >
        </details>
        <div class="grid gap-4 sm:grid-cols-2">
          <div>
            <Label for="task-model">模型覆盖</Label
            ><Select
              id="task-model"
              v-model="taskForm.model"
              :options="[{ label: '使用默认档位', value: '' }, ...modelOptions]"
            />
          </div>
          <div>
            <Label for="task-effort">思考档位</Label
            ><Select
              id="task-effort"
              v-model="taskForm.effort"
              :options="[
                { label: '使用默认档位', value: '' },
                { label: '低', value: 'low' },
                { label: '中', value: 'medium' },
                { label: '高', value: 'high' },
              ]"
            />
          </div>
        </div>
        <div>
          <Label for="task-system">系统提示词覆盖</Label
          ><Textarea
            id="task-system"
            v-model="taskForm.systemPrompt"
            :rows="8"
            :placeholder="selectedTask.systemPrompt"
          />
        </div>
        <div>
          <Label for="task-template">用户模板覆盖</Label
          ><Textarea
            id="task-template"
            v-model="taskForm.userTemplate"
            :rows="7"
            :placeholder="selectedTask.userTemplate"
          />
          <p class="mt-1 text-xs text-muted-foreground">
            占位符：{{ placeholderList || "无" }}。留空使用代码默认。
          </p>
        </div>
        <div>
          <Label for="task-note">变更说明</Label
          ><Input id="task-note" v-model="taskForm.note" maxlength="200" />
        </div>
        <div class="flex flex-wrap gap-2">
          <Button :loading="busy" @click="saveTask">保存版本</Button
          ><Button variant="outline" @click="showVersions">版本历史</Button
          ><Button
            variant="outline"
            @click="
              confirm = {
                text: '将该功能恢复为代码默认配置？',
                run: () => restoreVersion(0),
              }
            "
            >恢复默认</Button
          >
        </div>
        <div class="border-t pt-4">
          <Label for="ai-trial">试运行内容</Label
          ><Textarea id="ai-trial" v-model="trial" :rows="4" /><Button
            class="mt-2"
            size="sm"
            variant="outline"
            :loading="testing"
            :disabled="!trial.trim()"
            @click="testRun"
            >试运行已保存配置</Button
          >
          <pre
            v-if="trialResult"
            class="mt-3 max-h-80 overflow-auto whitespace-pre-wrap rounded-lg bg-muted p-4 text-xs"
            >{{ trialResult }}</pre
          >
        </div>
      </div>
    </section>
    <template v-else-if="tab === 'costs' || costsOnly">
      <div class="flex items-center gap-2">
        <Label for="cost-month" class="mb-0">统计月份</Label
        ><Input
          id="cost-month"
          v-model="month"
          type="month"
          class="w-44"
        /><Button variant="outline" @click="loadCosts">查询</Button>
      </div>
      <div v-if="costData" class="space-y-4">
        <div class="grid gap-3 sm:grid-cols-4">
          <StatCard
            title="调用次数"
            :value="costData.summary.calls ?? '—'"
          /><StatCard
            title="估算费用（USD）"
            :value="costData.summary.costUsd ?? '—'"
          /><StatCard
            title="输入 token"
            :value="costData.summary.tokensIn ?? '—'"
          /><StatCard
            title="输出 token"
            :value="costData.summary.tokensOut ?? '—'"
          />
        </div>
        <section class="panel overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>功能</th>
                <th>渠道 / 模型</th>
                <th>调用量</th>
                <th>输入 token</th>
                <th>输出 token</th>
                <th>费用（USD）</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="r in costData.endpoints"
                :key="`${r.endpoint}-${r.model}`"
              >
                <td>{{ r.endpoint }}</td>
                <td>{{ r.provider }} / {{ r.model || "—" }}</td>
                <td>{{ r.calls }}</td>
                <td>{{ r.tokensIn ?? "—" }}</td>
                <td>{{ r.tokensOut ?? "—" }}</td>
                <td>{{ r.costUsd ?? "—" }}</td>
              </tr>
            </tbody>
          </table>
          <p
            v-if="!costData.endpoints.length"
            class="p-6 text-sm text-muted-foreground"
          >
            本月暂无调用记录
          </p>
        </section>
      </div>
    </template>
    <Dialog
      :open="modelOpen"
      :title="modelForm.id ? '编辑模型' : '添加模型'"
      @update:open="modelOpen = $event"
      ><form id="ai-model-form" class="space-y-4" @submit.prevent="saveModel">
        <div v-for="field in modelFields" :key="field.key">
          <Label :for="`model-${field.key}`" :required="field.required">{{
            field.label
          }}</Label
          ><Input
            :id="`model-${field.key}`"
            v-model="modelForm[field.key]"
            :type="field.type ?? 'text'"
            :required="field.required"
            :min="field.type === 'number' ? 0 : undefined"
            :step="field.type === 'number' ? '0.0001' : undefined"
          />
        </div>
        <Checkbox v-model="modelForm.enabled">启用模型</Checkbox>
      </form>
      <template #footer
        ><Button variant="outline" @click="modelOpen = false">取消</Button
        ><Button form="ai-model-form" type="submit" :loading="busy"
          >保存</Button
        ></template
      ></Dialog
    >
    <Dialog
      :open="versionsOpen"
      title="提示词版本历史"
      size="lg"
      @update:open="versionsOpen = $event"
      ><div
        v-for="v in versions"
        :key="v.versionNo"
        class="mb-3 rounded-lg border p-4"
      >
        <div class="flex items-center justify-between gap-3">
          <p class="text-sm font-medium">
            v{{ v.versionNo }} · {{ v.createdBy }} · {{ v.createdAt }}
          </p>
          <Button
            size="sm"
            variant="outline"
            @click="
              confirm = {
                text: `恢复到 v${v.versionNo}？将新增一个版本。`,
                run: () => restoreVersion(v.versionNo),
              }
            "
            >恢复</Button
          >
        </div>
        <p class="mt-2 text-xs text-muted-foreground">
          {{ v.note || "无变更说明" }}
        </p>
        <details class="mt-3 text-xs">
          <summary>查看提示词</summary>
          <pre class="mt-2 whitespace-pre-wrap"
            >{{ v.systemPrompt || "代码默认" }}
{{ v.userTemplate || "代码默认" }}</pre
          >
        </details>
      </div>
      <p v-if="!versions.length" class="text-sm text-muted-foreground">
        尚无配置版本
      </p></Dialog
    >
    <ConfirmDialog
      :open="!!pendingTask"
      title="切换功能？"
      description="当前提示词有未保存的修改，切换后会放弃这些修改。"
      confirm-text="放弃修改并切换"
      @update:open="!$event && (pendingTask = null)"
      @confirm="confirmTaskSwitch"
    />
    <ConfirmDialog
      :open="!!confirm"
      title="确认操作"
      :description="confirm?.text ?? ''"
      :busy="busy"
      @update:open="!$event && (confirm = null)"
      @confirm="runConfirm"
    />
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { Plus } from "@lucide/vue";
import { api } from "../../lib/api";
import { session } from "../../lib/session";
import { toast, errText } from "../../lib/toast";
import PageHeader from "../../ui/PageHeader.vue";
import Button from "../../ui/Button.vue";
import Callout from "../../ui/Callout.vue";
import SegmentedTabs from "../../ui/SegmentedTabs.vue";
import Input from "../../ui/Input.vue";
import Textarea from "../../ui/Textarea.vue";
import Select from "../../ui/Select.vue";
import Label from "../../ui/Label.vue";
import Dialog from "../../ui/Dialog.vue";
import ConfirmDialog from "../../ui/ConfirmDialog.vue";
import Badge from "../../ui/Badge.vue";
import Checkbox from "../../ui/Checkbox.vue";
import PortalRowActions from "../../components/portal/PortalRowActions.vue";
import StatCard from "../../ui/StatCard.vue";
type Row = Record<string, any>;
const props = defineProps<{ costsOnly?: boolean }>();
const tab = ref(props.costsOnly ? "costs" : "models"),
  loading = ref(false),
  error = ref(""),
  busy = ref(false),
  testing = ref(false),
  data = ref<{ models: Row[]; tasks: Row[]; providers: Row[] }>({
    models: [],
    tasks: [],
    providers: [],
  });
const tabs = computed(() => [
  { label: "模型与密钥", value: "models" },
  { label: "功能与提示词", value: "tasks" },
  ...(session.has("can_view_api_costs")
    ? [{ label: "调用成本", value: "costs" }]
    : []),
]);
const provider = reactive<Record<string, string>>({
    tierFast: "",
    tierStandard: "",
    tierQuality: "",
  }),
  tiers = [
    { key: "tierFast", label: "快速" },
    { key: "tierStandard", label: "标准" },
    { key: "tierQuality", label: "高质量" },
  ];
const placeholderList = computed(() =>
  (selectedTask.value?.placeholders ?? [])
    .map((value: string) => "{" + value + "}")
    .join("、"),
);
const currentModels = computed(() =>
  data.value.models.filter((model) => model.provider === "gemini"),
);
const taskSearch = ref(""),
  taskBaseline = ref(""),
  pendingTask = ref<Row | null>(null);
const filteredTasks = computed(() =>
  data.value.tasks.filter((task) =>
    `${task.label} ${task.name}`
      .toLowerCase()
      .includes(taskSearch.value.trim().toLowerCase()),
  ),
);
const taskDirty = computed(
  () => !!selectedTask.value && JSON.stringify(taskForm) !== taskBaseline.value,
);
const modelOptions = computed(() =>
  data.value.models
    .filter((x) => x.provider === "gemini" && x.enabled)
    .map((x) => ({ label: x.displayName, value: x.modelId })),
);
const modelOpen = ref(false),
  modelForm = reactive<Row>({}),
  selectedTask = ref<Row | null>(null),
  taskForm = reactive({
    model: "",
    effort: "",
    systemPrompt: "",
    userTemplate: "",
    note: "",
  }),
  key = ref(""),
  trial = ref(""),
  trialResult = ref(""),
  versions = ref<Row[]>([]),
  versionsOpen = ref(false),
  confirm = ref<{ text: string; run: () => Promise<void> } | null>(null);
const modelFields = [
  { key: "modelId", label: "Gemini 模型标识", required: true },
  { key: "displayName", label: "显示名称", required: true },
  { key: "inputPrice", label: "输入价（USD / 百万 token）", type: "number" },
  { key: "outputPrice", label: "输出价（USD / 百万 token）", type: "number" },
  { key: "note", label: "备注" },
];
const month = ref(
    new Intl.DateTimeFormat("sv-SE", {
      year: "numeric",
      month: "2-digit",
      timeZone: "Asia/Shanghai",
    }).format(new Date()),
  ),
  costData = ref<Row | null>(null);
async function load() {
  loading.value = true;
  error.value = "";
  try {
    if (props.costsOnly) {
      await loadCosts();
      return;
    }
    data.value = await api.get("/admin-api/portal/ai/config");
    const p = data.value.providers[0] ?? {};
    for (const t of tiers) provider[t.key] = p[t.key] ?? "";
    if (!selectedTask.value && data.value.tasks.length)
      chooseTask(data.value.tasks[0]!);
  } catch (e) {
    error.value = errText(e);
  } finally {
    loading.value = false;
  }
}
function editModel(m?: Row) {
  for (const k of Object.keys(modelForm)) delete modelForm[k];
  Object.assign(
    modelForm,
    m ?? {
      modelId: "",
      displayName: "",
      enabled: true,
      inputPrice: "",
      outputPrice: "",
      note: "",
    },
  );
  modelOpen.value = true;
}
async function syncModels() {
  await mutate(async () => {
    const result = await api.post<{ available: number; added: number }>(
      "/admin-api/portal/ai/models/sync",
      {},
      120000,
    );
    toast(`已发现 ${result.available} 个模型，新增 ${result.added} 个`);
  });
}
async function saveModel() {
  await mutate(async () => {
    await api.post("/admin-api/portal/ai/models/save", { ...modelForm });
    modelOpen.value = false;
  });
}
function askDelete(m: Row) {
  confirm.value = {
    text: `删除模型「${m.displayName}」？引用中的模型不能删除。`,
    run: async () => {
      await api.del("/admin-api/portal/ai/models/delete", { id: m.id });
      await load();
    },
  };
}
function chooseTask(t: Row) {
  selectedTask.value = t;
  Object.assign(taskForm, {
    model: t.override?.model ?? "",
    effort: t.override?.effort ?? "",
    systemPrompt: t.override?.systemPrompt ?? "",
    userTemplate: t.override?.userTemplate ?? "",
    note: "",
  });
  trialResult.value = "";
  taskBaseline.value = JSON.stringify(taskForm);
}
function requestTask(name: string) {
  const target = data.value.tasks.find((task) => task.name === name);
  if (!target || target.name === selectedTask.value?.name) return;
  if (taskDirty.value) pendingTask.value = target;
  else chooseTask(target);
}
function confirmTaskSwitch() {
  if (pendingTask.value) chooseTask(pendingTask.value);
  pendingTask.value = null;
}
function loadEffectivePrompts() {
  const task = selectedTask.value;
  if (!task) return;
  taskForm.systemPrompt =
    task.override?.systemPrompt || task.systemPrompt || "";
  taskForm.userTemplate =
    task.override?.userTemplate || task.userTemplate || "";
}
async function mutate(fn: () => Promise<void>) {
  busy.value = true;
  try {
    await fn();
    toast("已保存");
    await load();
  } catch (e) {
    toast(errText(e), "bad");
  } finally {
    busy.value = false;
  }
}
async function saveProvider() {
  await mutate(async () => {
    await api.put("/admin-api/portal/ai/provider", provider);
  });
}
async function saveKey() {
  await mutate(async () => {
    await api.put("/admin-api/portal/ai/key", { key: key.value });
    key.value = "";
  });
}
async function clearKey() {
  await api.del("/admin-api/portal/ai/key");
  await load();
}
async function saveTask() {
  if (!selectedTask.value) return;
  const name = selectedTask.value.name;
  await mutate(async () => {
    await api.post("/admin-api/portal/ai/tasks/save", {
      taskName: name,
      ...taskForm,
    });
    await load();
    const saved = data.value.tasks.find((x) => x.name === name);
    if (saved) chooseTask(saved);
  });
}
async function showVersions() {
  if (!selectedTask.value) return;
  try {
    versions.value = await api.get("/admin-api/portal/ai/tasks/versions", {
      taskName: selectedTask.value.name,
    });
    versionsOpen.value = true;
  } catch (e) {
    toast(errText(e), "bad");
  }
}
async function restoreVersion(version: number) {
  if (!selectedTask.value) return;
  const name = selectedTask.value.name;
  await api.post("/admin-api/portal/ai/tasks/restore", {
    taskName: name,
    version,
  });
  await load();
  const saved = data.value.tasks.find((x) => x.name === name);
  if (saved) chooseTask(saved);
  await showVersions();
}
async function testRun() {
  testing.value = true;
  trialResult.value = "";
  try {
    trialResult.value = await api.post<string>(
      "/admin-api/portal/ai/test",
      { taskName: selectedTask.value?.name, input: trial.value },
      150000,
    );
  } catch (e) {
    toast(errText(e), "bad");
  } finally {
    testing.value = false;
  }
}
async function loadCosts() {
  error.value = "";
  try {
    costData.value = await api.get("/admin-api/portal/ai/costs", {
      month: month.value,
    });
  } catch (e) {
    costData.value = null;
    error.value = errText(e);
  }
}
async function runConfirm() {
  const c = confirm.value;
  if (!c) return;
  busy.value = true;
  try {
    await c.run();
    confirm.value = null;
    toast("操作完成");
  } catch (e) {
    toast(errText(e), "bad");
  } finally {
    busy.value = false;
  }
}
watch(tab, (v) => {
  if (v === "costs") void loadCosts();
});
onMounted(load);
</script>
