<template>
  <DropdownMenu
    :side="side ?? (compact ? 'bottom' : 'right')"
    align="end"
    class="workspace-account-menu"
  >
    <template #trigger>
      <button
        v-bind="$attrs"
        type="button"
        class="workspace-account-trigger"
        :class="{ 'account-trigger-compact': compact }"
        :aria-label="`${user.name || '未登录'}的账号菜单`"
        :title="compact ? '账号菜单' : undefined"
      >
        <Avatar :name="user.name" class="workspace-account-avatar ring-0" />
        <span v-if="!compact" class="sidebar-label account-trigger-copy">
          <span class="account-trigger-name">{{ user.name || "未登录" }}</span>
          <span class="account-trigger-department">{{
            user.department || "个人工作空间"
          }}</span>
        </span>
        <ChevronsUpDown
          v-if="!compact"
          class="sidebar-label size-3.5 shrink-0 text-sidebar-muted"
          aria-hidden="true"
        />
      </button>
    </template>

    <div class="workspace-account-summary">
      <Avatar :name="user.name" class="workspace-account-avatar ring-0" />
      <div class="min-w-0 flex-1">
        <strong class="block truncate text-sm font-semibold">{{
          user.name || "未登录"
        }}</strong>
        <span class="mt-1 block text-xs text-muted-foreground">{{
          user.role
        }}</span>
      </div>
    </div>
    <DropdownMenuSeparator class="ui-menu-separator" />
    <DropdownMenuItem v-if="!session.isHrOnly" @select="openProfile"
      ><User aria-hidden="true" />个人资料</DropdownMenuItem
    >
    <DropdownMenuItem @select="toggleTheme">
      <Sun v-if="isDark" aria-hidden="true" /><Moon v-else aria-hidden="true" />
      {{ isDark ? "切换到浅色模式" : "切换到深色模式" }}
    </DropdownMenuItem>
    <DropdownMenuSeparator class="ui-menu-separator" />
    <DropdownMenuItem destructive @select="emit('logout')"
      ><LogOut aria-hidden="true" />退出登录</DropdownMenuItem
    >
  </DropdownMenu>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import { ChevronsUpDown, LogOut, Moon, Sun, User } from "@lucide/vue";
import { DropdownMenuSeparator } from "reka-ui";
import Avatar from "../ui/Avatar.vue";
import DropdownMenu from "../ui/DropdownMenu.vue";
import DropdownMenuItem from "../ui/DropdownMenuItem.vue";
import { isDark, toggleTheme } from "../lib/theme";
import { session } from "../lib/session";

defineOptions({ inheritAttrs: false });
defineProps<{
  user: { name: string; department?: string; role: string };
  compact?: boolean;
  side?: "top" | "right" | "bottom";
}>();
const emit = defineEmits<{ logout: []; navigate: [] }>();
const router = useRouter();
function openProfile() {
  emit("navigate");
  void router.push("/me/profile");
}
</script>
