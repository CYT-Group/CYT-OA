<template>
  <div class="space-y-5">
    <PageHeader
      title="入口上线管理"
      description="按批次开放后台与 HR 功能，保存后统一更新管理入口。"
    >
      <template #actions
        ><Button
          variant="outline"
          :disabled="loading || saving || dirty"
          @click="load"
          ><RefreshCw class="size-4" />刷新</Button
        ><Button :disabled="!dirty || loading" :loading="saving" @click="save"
          ><Save class="size-4" />保存上线设置</Button
        ></template
      >
    </PageHeader>
    <Callout v-if="error" title="上线设置未保存" tone="destructive"
      >{{ error
      }}<template #action
        ><Button variant="outline" size="sm" :disabled="saving" @click="load"
          >重新加载并放弃本地修改</Button
        ></template
      ></Callout
    >
    <section class="panel space-y-2 p-4 text-sm">
      <p>
        开启后，具有对应角色权限的人员可看到并进入该功能。关闭后，导航与管理中心隐藏入口，直接访问页面也会提示尚未开放。
      </p>
      <p class="text-xs leading-relaxed text-muted-foreground">
        开关不会修改角色权限、业务数据、员工展示内容或自动任务。正在编辑的页面保留到下次进入时检查。管理中心与本设置入口固定保留，只有系统管理员可修改上线设置。
      </p>
      <p v-if="snapshot?.updatedAt" class="pt-1 text-xs text-muted-foreground">
        最近保存：{{ snapshot.updatedBy || "管理员" }} ·
        {{ formatPortalDateTime(snapshot.updatedAt) }}
      </p>
      <p v-else-if="!loading" class="pt-1 text-xs text-muted-foreground">
        当前沿用全部开启状态，可按上线计划调整。
      </p>
    </section>
    <div
      v-if="loading"
      class="panel p-10 text-center text-sm text-muted-foreground"
    >
      正在读取入口设置…
    </div>
    <template v-else-if="snapshot">
      <section
        v-for="group in groups"
        :key="group.group"
        class="panel overflow-hidden"
      >
        <div class="panel-header flex-wrap">
          <div class="flex items-center gap-3">
            <h2 class="panel-title">{{ group.group }}</h2>
            <Badge variant="secondary"
              >计划开启
              {{ group.items.filter((item) => draft[item.key]).length }} /
              {{ group.items.length }}</Badge
            >
          </div>
          <div class="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              :disabled="saving"
              @click="setGroup(group.items, true)"
              >整组开启</Button
            ><Button
              variant="ghost"
              size="sm"
              :disabled="saving"
              @click="setGroup(group.items, false)"
              >整组关闭</Button
            >
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>功能入口</th>
                <th class="w-28">当前状态</th>
                <th class="sticky-col text-right">计划状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in group.items" :key="item.key">
                <td>
                  <div class="flex items-center gap-3">
                    <UiIcon
                      :name="item.icon"
                      :size="17"
                      class="shrink-0 text-muted-foreground"
                    /><span class="font-medium">{{ item.title }}</span
                    ><span
                      v-if="draft[item.key] !== snapshot.entries[item.key]"
                      class="text-xs text-warning"
                      >{{ draft[item.key] ? "待开启" : "待关闭" }}</span
                    >
                  </div>
                </td>
                <td>
                  <Badge
                    :variant="
                      snapshot.entries[item.key] ? 'success' : 'secondary'
                    "
                    >{{
                      snapshot.entries[item.key] ? "已开放" : "未开放"
                    }}</Badge
                  >
                </td>
                <td class="sticky-col">
                  <div class="flex items-center justify-end gap-3">
                    <span class="text-xs text-muted-foreground">{{
                      draft[item.key] ? "开启" : "关闭"
                    }}</span
                    ><Switch
                      :model-value="draft[item.key]"
                      :disabled="saving"
                      :aria-label="`${item.title}入口开关`"
                      @update:model-value="draft[item.key] = $event"
                    />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
      <div
        class="panel sticky bottom-4 z-10 flex flex-wrap items-center justify-between gap-3 p-4 shadow-md"
      >
        <p class="text-sm">
          <template v-if="dirty"
            ><span class="font-medium text-primary"
              >{{ changes.length }} 项修改待保存</span
            ><span class="ml-2 text-xs text-muted-foreground"
              >尚未影响其他用户</span
            ></template
          ><span v-else class="text-muted-foreground">所有入口状态已同步</span>
        </p>
        <div class="flex gap-2">
          <Button variant="outline" :disabled="!dirty || saving" @click="reset"
            >撤销修改</Button
          ><Button :disabled="!dirty" :loading="saving" @click="save"
            >保存上线设置</Button
          >
        </div>
      </div>
    </template>
    <ConfirmDialog
      :open="discardOpen"
      title="放弃未保存的上线设置？"
      description="入口开关尚未保存，离开后会丢失本次调整。"
      confirm-text="放弃修改并离开"
      @update:open="!$event && settleLeave(false)"
      @confirm="settleLeave(true)"
    />
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from "vue";
import { onBeforeRouteLeave } from "vue-router";
import { RefreshCw, Save } from "@lucide/vue";
import { portalMenus } from "../../configs/portal";
import type { MenuLeaf } from "../../configs/types";
import { fixedPortalEntries } from "../../lib/portal-entry-policy";
import {
  applyPortalRollout,
  portalRolloutApi,
  type PortalRolloutSettings,
} from "../../lib/portal-rollout";
import { formatPortalDateTime } from "../../lib/portal-management-format";
import { errText, toast } from "../../lib/toast";
import UiIcon from "../../components/UiIcon.vue";
import PageHeader from "../../ui/PageHeader.vue";
import Button from "../../ui/Button.vue";
import Badge from "../../ui/Badge.vue";
import Callout from "../../ui/Callout.vue";
import Switch from "../../ui/Switch.vue";
import ConfirmDialog from "../../ui/ConfirmDialog.vue";
const groups = portalMenus
  .map((group) => ({
    ...group,
    items: group.items.filter((item) => !fixedPortalEntries.has(item.key)),
  }))
  .filter((group) => group.items.length);
const snapshot = ref<PortalRolloutSettings>(),
  draft = ref<Record<string, boolean>>({}),
  loading = ref(true),
  saving = ref(false),
  error = ref("");
const changes = computed(() =>
  snapshot.value
    ? Object.keys(draft.value).filter(
        (key) => draft.value[key] !== snapshot.value!.entries[key],
      )
    : [],
);
const dirty = computed(() => changes.value.length > 0);
const discardOpen = ref(false);
let leaveDecision: ((value: boolean) => void) | undefined;
function settleLeave(value: boolean) {
  discardOpen.value = false;
  leaveDecision?.(value);
  leaveDecision = undefined;
}
onBeforeRouteLeave(() =>
  dirty.value
    ? new Promise<boolean>((resolve) => {
        settleLeave(false);
        leaveDecision = resolve;
        discardOpen.value = true;
      })
    : true,
);
function beforeUnload(event: BeforeUnloadEvent) {
  if (dirty.value) {
    event.preventDefault();
    event.returnValue = "";
  }
}
function accept(value: PortalRolloutSettings) {
  applyPortalRollout(value);
  snapshot.value = value;
  draft.value = { ...value.entries };
}
async function load() {
  loading.value = true;
  error.value = "";
  try {
    accept(await portalRolloutApi.settings());
  } catch (failure) {
    error.value = errText(failure);
  } finally {
    loading.value = false;
  }
}
function reset() {
  if (snapshot.value) draft.value = { ...snapshot.value.entries };
  error.value = "";
}
function setGroup(items: MenuLeaf[], enabled: boolean) {
  for (const item of items) draft.value[item.key] = enabled;
}
async function save() {
  if (!snapshot.value || !dirty.value || saving.value) return;
  saving.value = true;
  error.value = "";
  const count = changes.value.length;
  try {
    accept(
      await portalRolloutApi.save(
        snapshot.value.revision,
        Object.fromEntries(
          changes.value.map((key) => [key, draft.value[key]!]),
        ),
      ),
    );
    toast(`已更新 ${count} 个入口`);
  } catch (failure) {
    error.value = errText(failure);
  } finally {
    saving.value = false;
  }
}
onMounted(() => {
  load();
  window.addEventListener("beforeunload", beforeUnload);
});
onBeforeUnmount(() => {
  settleLeave(false);
  window.removeEventListener("beforeunload", beforeUnload);
});
</script>
