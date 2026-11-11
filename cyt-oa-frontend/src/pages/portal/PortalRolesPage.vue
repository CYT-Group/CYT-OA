<template>
  <div class="space-y-4">
    <PageHeader
      title="角色权限"
      description="按角色维护门户能力；成员角色在账号管理中分配"
    >
      <template #actions
        ><Button variant="outline" @click="router.push('/page/sys-users')"
          >账号管理</Button
        ><Button @click="edit()"
          ><Plus class="size-4" />新增角色</Button
        ></template
      >
    </PageHeader>
    <Callout v-if="error" tone="destructive" title="加载失败"
      >{{ error
      }}<template #action
        ><Button @click="load">重试</Button></template
      ></Callout
    >
    <section class="panel overflow-hidden">
      <div class="table-toolbar justify-between border-b">
        <Input
          v-model="roleSearch"
          placeholder="搜索角色名称或标识"
          aria-label="搜索角色"
          class="sm:w-64"
        />
        <span class="text-xs text-muted-foreground"
          >{{ filteredRoles.length }} 个角色</span
        >
      </div>
      <div v-if="loading" class="p-8 text-sm text-muted-foreground">
        正在加载角色…
      </div>
      <div v-else class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>角色名称</th>
              <th>标识</th>
              <th>能力</th>
              <th>备注</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="role in filteredRoles" :key="role.id">
              <td class="font-medium">
                {{ role.name }}
                <Badge v-if="role.builtIn" variant="secondary">内置</Badge>
              </td>
              <td>{{ role.code }}</td>
              <td>
                {{
                  role.code === "admin"
                    ? "全部能力"
                    : `${role.permissions.length} 项`
                }}
              </td>
              <td>{{ role.remark || "—" }}</td>
              <td class="sticky-col text-right">
                <div class="flex items-center justify-end gap-1">
                  <Button
                    v-if="role.code !== 'admin'"
                    size="sm"
                    variant="ghost"
                    @click="edit(role)"
                    >编辑授权</Button
                  ><PortalRowActions
                    :actions="[
                      !role.builtIn && {
                        label: '删除角色',
                        destructive: true,
                        onSelect: () => (deleting = role),
                      },
                    ]"
                    :label="`${role.name}的更多操作`"
                  />
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
    <Dialog
      :open="open"
      :title="editing ? `编辑角色 · ${editing.name}` : '新增角色'"
      size="xl"
      @update:open="open = $event"
    >
      <form id="portal-role-form" class="space-y-5" @submit.prevent="save">
        <div class="grid gap-4 sm:grid-cols-2">
          <div>
            <Label for="role-name" required>角色名称</Label
            ><Input
              id="role-name"
              v-model="form.name"
              required
              maxlength="64"
            />
          </div>
          <div>
            <Label for="role-code" required>角色标识</Label
            ><Input
              id="role-code"
              v-model="form.code"
              :disabled="!!editing"
              required
              pattern="[a-z][a-z0-9_-]{1,63}"
              placeholder="如 hr_editor"
            />
          </div>
        </div>
        <div>
          <Label for="role-remark">备注</Label
          ><Input id="role-remark" v-model="form.remark" maxlength="255" />
        </div>
        <div class="rounded-lg border">
          <div class="border-b p-4">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <h3 class="text-sm font-medium">门户能力授权</h3>
              <Badge variant="secondary">已选 {{ selected.length }} 项</Badge>
            </div>
            <p class="mt-1 text-xs text-muted-foreground">
              角色管理、API 密钥管理固定归系统管理员。
            </p>
            <div class="mt-3 flex flex-wrap items-center gap-3">
              <Input
                v-model="permissionSearch"
                placeholder="搜索功能或能力标识"
                aria-label="搜索能力"
                class="min-w-40 flex-1"
              /><Checkbox v-model="selectedOnly">仅看已选</Checkbox>
            </div>
          </div>
          <div class="max-h-[45vh] overflow-y-auto p-4">
            <details
              v-for="group in permissionGroups"
              :key="group.name"
              open
              class="mb-4 last:mb-0"
            >
              <summary class="cursor-pointer text-xs font-semibold">
                {{ group.name }}
                <span class="ml-2 font-normal text-muted-foreground"
                  >{{
                    group.items.filter((cap) =>
                      selected.includes(cap.permission),
                    ).length
                  }}
                  / {{ group.items.length }}</span
                >
              </summary>
              <div class="mt-3 grid gap-3 sm:grid-cols-2">
                <Checkbox
                  v-for="cap in group.items"
                  :key="cap.permission"
                  :model-value="selected.includes(cap.permission)"
                  :disabled="fixed.includes(cap.permission)"
                  @update:model-value="toggle(cap.permission, $event)"
                  ><span
                    >{{ cap.name
                    }}<span
                      v-if="fixed.includes(cap.permission)"
                      class="ml-1 text-xs text-muted-foreground"
                      >仅管理员</span
                    ></span
                  ></Checkbox
                >
              </div>
            </details>
            <p
              v-if="!permissionGroups.length"
              class="py-6 text-center text-sm text-muted-foreground"
            >
              没有匹配的能力，请调整搜索条件。
            </p>
          </div>
        </div>
      </form>
      <template #footer
        ><Button variant="outline" @click="open = false">取消</Button
        ><Button form="portal-role-form" type="submit" :loading="busy"
          >保存授权</Button
        ></template
      >
    </Dialog>
    <ConfirmDialog
      :open="!!deleting"
      title="删除角色"
      :description="`确认删除「${deleting?.name}」？有成员的角色不能删除。`"
      tone="destructive"
      :busy="busy"
      @update:open="!$event && (deleting = null)"
      @confirm="remove"
    />
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { Plus } from "@lucide/vue";
import { api } from "../../lib/api";
import { toast, errText } from "../../lib/toast";
import Button from "../../ui/Button.vue";
import PageHeader from "../../ui/PageHeader.vue";
import Callout from "../../ui/Callout.vue";
import Badge from "../../ui/Badge.vue";
import Dialog from "../../ui/Dialog.vue";
import ConfirmDialog from "../../ui/ConfirmDialog.vue";
import Input from "../../ui/Input.vue";
import Label from "../../ui/Label.vue";
import Checkbox from "../../ui/Checkbox.vue";
import PortalRowActions from "../../components/portal/PortalRowActions.vue";
interface Role {
  id: number;
  code: string;
  name: string;
  remark: string;
  builtIn: boolean;
  permissions: string[];
}
const router = useRouter(),
  roles = ref<Role[]>([]),
  capabilities = ref<{ permission: string; name: string }[]>([]);
const loading = ref(false),
  busy = ref(false),
  error = ref(""),
  open = ref(false),
  editing = ref<Role | null>(null),
  deleting = ref<Role | null>(null),
  selected = ref<string[]>([]);
const form = reactive({ code: "", name: "", remark: "" }),
  fixed = ["can_manage_roles", "can_manage_ai_keys"];
const roleSearch = ref(""),
  permissionSearch = ref(""),
  selectedOnly = ref(false);
const filteredRoles = computed(() =>
  roles.value.filter((role) =>
    `${role.name} ${role.code}`
      .toLowerCase()
      .includes(roleSearch.value.trim().toLowerCase()),
  ),
);
const permissionGroups = computed(() => {
  const groups = [
    {
      name: "入口与内容",
      keys: [
        "can_enter_admin",
        "can_create_content",
        "can_edit_any",
        "can_review",
        "can_delete",
        "can_view_any_status",
        "can_view_own_draft",
        "can_moderate_comments",
      ],
    },
    {
      name: "人事与制度",
      keys: [
        "can_edit_hr_documents",
        "can_view_hr_sop",
        "can_view_signoff_stats",
        "can_manage_resumes",
        "can_manage_celebrations",
        "can_generate_contracts",
      ],
    },
    {
      name: "资源与培训",
      keys: [
        "can_manage_brand_docs",
        "can_manage_materials",
        "can_approve_any_material_apply",
        "can_configure_site_owners",
        "can_manage_training",
        "can_manage_events",
        "can_manage_products",
        "can_import",
      ],
    },
    { name: "系统与分析", keys: [] as string[] },
  ];
  const assigned = new Set(groups.flatMap((group) => group.keys));
  const query = permissionSearch.value.trim().toLowerCase();
  return groups
    .map((group, index) => ({
      name: group.name,
      items: capabilities.value.filter(
        (cap) =>
          (index === groups.length - 1
            ? !assigned.has(cap.permission)
            : group.keys.includes(cap.permission)) &&
          (!selectedOnly.value || selected.value.includes(cap.permission)) &&
          `${cap.name} ${cap.permission}`.toLowerCase().includes(query),
      ),
    }))
    .filter((group) => group.items.length);
});
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const r = await api.get<{
      roles: Role[];
      capabilities: typeof capabilities.value;
    }>("/admin-api/portal/roles/list");
    roles.value = r.roles;
    capabilities.value = r.capabilities;
  } catch (e) {
    error.value = errText(e);
  } finally {
    loading.value = false;
  }
}
function edit(role?: Role) {
  permissionSearch.value = "";
  selectedOnly.value = false;
  editing.value = role ?? null;
  Object.assign(form, {
    code: role?.code ?? "",
    name: role?.name ?? "",
    remark: role?.remark ?? "",
  });
  selected.value = (role?.permissions ?? []).filter((p) => !fixed.includes(p));
  open.value = true;
}
function toggle(key: string, value: boolean) {
  selected.value = value
    ? [...new Set([...selected.value, key])]
    : selected.value.filter((x) => x !== key);
}
async function save() {
  busy.value = true;
  try {
    await api.post("/admin-api/portal/roles/save", {
      ...form,
      id: editing.value?.id,
      permissions: selected.value,
    });
    open.value = false;
    toast("角色授权已保存");
    await load();
  } catch (e) {
    toast(errText(e), "bad");
  } finally {
    busy.value = false;
  }
}
async function remove() {
  if (!deleting.value) return;
  busy.value = true;
  try {
    await api.del("/admin-api/portal/roles/delete", { id: deleting.value.id });
    deleting.value = null;
    toast("角色已删除");
    await load();
  } catch (e) {
    toast(errText(e), "bad");
  } finally {
    busy.value = false;
  }
}
onMounted(load);
</script>
