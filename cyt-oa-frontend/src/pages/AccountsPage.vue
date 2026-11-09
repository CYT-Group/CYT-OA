<template>
  <div>
    <PageHeader
      title="账号与权限"
      description="这里只管理登录与权限。停用不会删除员工档案；通讯录同步不会自动恢复已停用账号。"
    />

    <Callout v-if="error" class="mt-4" tone="destructive" title="账号加载失败">
      {{ error }}
      <template #action>
        <Button variant="ghost" size="sm" class="text-destructive" @click="load">重试</Button>
      </template>
    </Callout>

    <section class="panel mt-4 overflow-hidden">
      <div class="table-toolbar">
        <form class="flex flex-wrap items-center gap-2" @submit.prevent="search">
          <Input
            v-model="keyword"
            class="h-9 w-40 text-[13px] sm:w-56"
            aria-label="账号搜索"
            placeholder="姓名、账号或企微 userid"
            data-page-search
          />
          <Select
            size="sm"
            class="w-40"
            aria-label="登录状态"
            :model-value="enabled"
            :options="ENABLED_OPTIONS"
            @update:model-value="onEnabled"
          />
          <Button type="submit" size="sm">
            <Search class="size-3.5" />
            查询
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            aria-label="刷新"
            :disabled="loading"
            class="sm:hidden"
            @click="load"
          >
            <RefreshCw :class="['size-3.5', loading && 'animate-spin']" />
          </Button>
        </form>
        <div class="ml-auto hidden shrink-0 items-center gap-3 sm:flex">
          <span class="text-xs tabular-nums text-muted-foreground">共 {{ total }} 个账号</span>
          <Button variant="ghost" size="icon-sm" aria-label="刷新" :disabled="loading" @click="load">
            <RefreshCw :class="['size-3.5', loading && 'animate-spin']" />
          </Button>
        </div>
      </div>

      <div v-if="loading" class="border-t border-border/70 p-4 space-y-2">
        <Skeleton v-for="i in 6" :key="i" class="h-10 w-full" />
      </div>

      <EmptyState
        v-else-if="!users.length"
        :icon="Users"
        title="暂无匹配账号"
        compact
        class="border-t border-border/70"
      />

      <div v-else class="max-h-[calc(100dvh-15rem)] overflow-auto border-t border-border/70">
        <table class="data-table min-w-[860px]">
          <thead>
            <tr>
              <th>
                <SortButton
                  label="姓名 / 账号"
                  :active="sortKey === 'name'"
                  :direction="sortDir"
                  @sort="toggleSort('name')"
                />
              </th>
              <th>
                <SortButton
                  label="身份来源"
                  :active="sortKey === 'source'"
                  :direction="sortDir"
                  @sort="toggleSort('source')"
                />
              </th>
              <th>
                <SortButton
                  label="部门"
                  :active="sortKey === 'mainDeptName'"
                  :direction="sortDir"
                  @sort="toggleSort('mainDeptName')"
                />
              </th>
              <th>角色</th>
              <th>
                <SortButton
                  label="企微状态"
                  :active="sortKey === 'wecom'"
                  :direction="sortDir"
                  @sort="toggleSort('wecom')"
                />
              </th>
              <th>
                <SortButton
                  label="登录状态"
                  :active="sortKey === 'enabled'"
                  :direction="sortDir"
                  @sort="toggleSort('enabled')"
                />
              </th>
              <th v-if="canManage" class="sticky-col text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in sortedUsers" :key="u.id">
              <td>
                <div class="flex items-center gap-2.5">
                  <Avatar :name="u.name" size="sm" />
                  <div class="min-w-0">
                    <p class="truncate text-sm font-medium">{{ u.name }}</p>
                    <p class="truncate text-[11px] text-muted-foreground">{{ u.username }}</p>
                  </div>
                </div>
              </td>
              <td>
                <Badge :variant="u.localAccount ? 'default' : 'outline'">
                  {{ u.localAccount ? '本地账号' : '企业微信' }}
                </Badge>
              </td>
              <td class="text-muted-foreground">{{ u.mainDeptName || '—' }}</td>
              <td>
                <div class="flex flex-wrap gap-1">
                  <Badge v-for="role in u.roles" :key="role" variant="secondary">{{ role }}</Badge>
                  <span v-if="!u.roles.length" class="text-xs text-muted-foreground">未分配</span>
                </div>
              </td>
              <td class="text-muted-foreground">{{ wecomLabels[u.wecomStatus ?? 0] || '—' }}</td>
              <td>
                <Badge :variant="u.enabled ? 'success' : 'destructive'">{{
                  u.enabled ? '启用' : '停用'
                }}</Badge>
              </td>
              <td v-if="canManage" class="sticky-col">
                <div class="flex items-center justify-end gap-1">
                  <Button
                    v-if="session.has('system:role:query')"
                    variant="ghost"
                    size="sm"
                    @click="editRoles(u)"
                    >分配角色</Button
                  >
                  <Button
                    variant="ghost"
                    size="sm"
                    :class="
                      u.enabled ? 'text-destructive hover:bg-destructive/10 hover:text-destructive' : ''
                    "
                    :disabled="busy || (u.enabled && protectedUser(u))"
                    @click="askStatus(u)"
                    >{{ u.enabled ? '停用' : '启用' }}</Button
                  >
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="!loading && total > 20" class="border-t border-border/70 px-3 py-2">
        <Pagination :page="page" :page-size="20" :total="total" @update:page="go" />
      </div>
    </section>

    <Dialog
      :open="!!editing"
      :title="editing ? `${editing.name} · 角色权限` : ''"
      description="保存后该账号的现有会话将失效，重新登录后生效"
      size="sm"
      @update:open="
        (value) => {
          if (!value) editing = null
        }
      "
    >
      <div v-if="editing" class="space-y-1.5">
        <div
          v-for="r in roles"
          :key="r.code"
          role="button"
          tabindex="0"
          class="flex cursor-pointer items-center gap-3 rounded-lg border px-3 py-2.5 transition-colors hover:bg-muted/40"
          :class="selectedRoles.includes(r.code) ? 'border-primary/40 bg-primary/5' : ''"
          @click="toggleRole(r.code)"
          @keydown.enter.prevent="toggleRole(r.code)"
          @keydown.space.prevent="toggleRole(r.code)"
        >
          <Checkbox
            :model-value="selectedRoles.includes(r.code)"
            @click.stop
            @update:model-value="toggleRole(r.code)"
          />
          <div class="min-w-0 flex-1">
            <p class="text-sm font-medium">{{ r.name }}</p>
            <p class="text-[11px] text-muted-foreground">{{ r.code }}</p>
          </div>
        </div>
        <p v-if="!roles.length" class="py-6 text-center text-xs text-muted-foreground">暂无可分配角色</p>
      </div>
      <template #footer>
        <Button variant="outline" @click="editing = null">取消</Button>
        <Button :disabled="busy" @click="saveRoles">
          <LoaderCircle v-if="busy" class="size-4 animate-spin" />
          {{ busy ? '保存中…' : '保存角色' }}
        </Button>
      </template>
    </Dialog>

    <ConfirmDialog
      :open="!!pending"
      :title="pending ? `${pending.enabled ? '停用' : '启用'}登录账号` : ''"
      :description="
        pending
          ? `确认${pending.enabled ? '停用' : '启用'} ${pending.name} 的登录权限？员工档案和历史业务记录将保留。`
          : ''
      "
      :tone="pending?.enabled ? 'destructive' : 'default'"
      :busy="busy"
      :confirm-text="pending ? (pending.enabled ? '停用' : '启用') : '确认'"
      @update:open="
        (value) => {
          if (!value) pending = null
        }
      "
      @confirm="changeStatus"
    />
  </div>
</template>

<script setup lang="ts">
import { LoaderCircle, RefreshCw, Search, Users } from '@lucide/vue'
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import { session } from '../lib/session'
import { errText, toast } from '../lib/toast'
import { useTableSort } from '../lib/useTableSort'
import Avatar from '../ui/Avatar.vue'
import Badge from '../ui/Badge.vue'
import Button from '../ui/Button.vue'
import Callout from '../ui/Callout.vue'
import Checkbox from '../ui/Checkbox.vue'
import ConfirmDialog from '../ui/ConfirmDialog.vue'
import Dialog from '../ui/Dialog.vue'
import EmptyState from '../ui/EmptyState.vue'
import Input from '../ui/Input.vue'
import PageHeader from '../ui/PageHeader.vue'
import Pagination from '../ui/Pagination.vue'
import Select from '../ui/Select.vue'
import Skeleton from '../ui/Skeleton.vue'
import SortButton from '../ui/SortButton.vue'

interface Account {
  id: string
  name: string
  username: string
  localAccount: boolean
  mainDeptName: string | null
  roles: string[]
  wecomStatus: number | null
  enabled: boolean
}
interface Role {
  code: string
  name: string
}

const ENABLED_OPTIONS = [
  { label: '全部账号', value: '' },
  { label: '启用', value: 'true' },
  { label: '停用', value: 'false' }
]

const users = ref<Account[]>([])

const {
  sortKey,
  sortDir,
  sorted: sortedUsers,
  toggleSort
} = useTableSort<Account>(users, (u, key) => {
  switch (key) {
    case 'source':
      return u.localAccount ? '本地账号' : '企业微信'
    case 'wecom':
      return wecomLabels[u.wecomStatus ?? 0] ?? ''
    case 'enabled':
      return u.enabled ? '启用' : '停用'
    default:
      return (u as unknown as Record<string, unknown>)[key]
  }
})
const roles = ref<Role[]>([])
const keyword = ref('')
const enabled = ref('')
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const editing = ref<Account | null>(null)
const pending = ref<Account | null>(null)
const selectedRoles = ref<string[]>([])
const canManage = computed(() => session.has('system:user:assign-role'))
const wecomLabels: Record<number, string> = { 1: '已激活', 2: '已禁用', 4: '未激活', 5: '已退出' }
const protectedUser = (u: Account) =>
  String(u.id) === String(session.user?.userId) || u.roles.includes('admin')

function search() {
  page.value = 1
  void load()
}

function onEnabled(value: unknown) {
  enabled.value = String(value ?? '')
  page.value = 1
  void load()
}

function go(next: number) {
  page.value = next
  void load()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const r = await api.get<{ list: Account[]; total: number }>('/admin-api/system/users', {
      keyword: keyword.value || undefined,
      enabled: enabled.value || undefined,
      page: page.value,
      size: 20
    })
    users.value = r.list
    total.value = Number(r.total)
  } catch (e) {
    users.value = []
    error.value = errText(e)
  } finally {
    loading.value = false
  }
}

async function editRoles(u: Account) {
  try {
    roles.value = await api.get<Role[]>('/admin-api/system/roles')
    selectedRoles.value = [...u.roles]
    editing.value = u
  } catch (e) {
    toast(errText(e), 'bad')
  }
}

function toggleRole(code: string) {
  selectedRoles.value = selectedRoles.value.includes(code)
    ? selectedRoles.value.filter((c) => c !== code)
    : [...selectedRoles.value, code]
}

async function saveRoles() {
  if (!editing.value) return
  busy.value = true
  try {
    await api.put(`/admin-api/system/users/${editing.value.id}/roles`, { roles: selectedRoles.value })
    editing.value = null
    toast('角色已更新')
    await load()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    busy.value = false
  }
}

function askStatus(u: Account) {
  pending.value = u
}

async function changeStatus() {
  if (!pending.value) return
  busy.value = true
  try {
    await api.put(`/admin-api/system/users/${pending.value.id}/status`, { enabled: !pending.value.enabled })
    pending.value = null
    toast('账号登录状态已更新')
    await load()
  } catch (e) {
    toast(errText(e), 'bad')
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>
