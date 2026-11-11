<template>
  <div class="workspace" :class="{ 'workspace-collapsed': sidebarCollapsed }">
    <a href="#main-content" class="skip-link">跳转到内容</a>
    <aside class="workspace-sidebar">
      <router-link to="/" class="workspace-brand">
        <span class="brand-mark"> OA </span>
        <span class="sidebar-label min-w-0">
          <b
            class="block truncate text-sm font-semibold text-sidebar-foreground"
            >畅韵OA</b
          >
          <small class="block text-[10px] text-sidebar-muted"
            >个人办公与企业协作</small
          >
        </span>
      </router-link>

      <div class="workspace-identity">
        <DropdownMenu side="right" align="start" class="workspace-account-menu">
          <template #trigger>
            <button
              type="button"
              aria-label="账号菜单"
              class="workspace-account-trigger focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60"
            >
              <Avatar
                :name="session.name"
                class="workspace-identity-avatar ring-0"
              />
              <span class="sidebar-label min-w-0 flex-1">
                <b
                  class="block truncate text-[13px] font-medium leading-tight text-sidebar-foreground"
                >
                  {{ session.name || "未登录" }}
                </b>
                <small class="block truncate text-[11px] text-sidebar-muted">
                  {{ deptName }}
                </small>
                <span class="workspace-identity-role">{{
                  session.isAdmin ? "管理员" : "员工"
                }}</span>
              </span>
              <ChevronsUpDown
                class="sidebar-label size-3.5 shrink-0 text-sidebar-muted"
              />
              <span class="workspace-identity-tier sidebar-label"
                ><span aria-hidden="true">{{
                  session.canAccessConsole ? "🛡️" : "👤"
                }}</span
                >{{
                  session.isAdmin
                    ? "系统管理员"
                    : session.canAccessConsole
                      ? "管理员"
                      : "普通员工"
                }}</span
              >
            </button>
          </template>
          <div class="workspace-account-summary">
            <Avatar :name="session.name" />
            <div class="min-w-0">
              <strong>{{ session.name || "未登录" }}</strong
              ><span
                >{{ session.isAdmin ? "管理员" : "员工" }} ·
                {{ deptName }}</span
              >
            </div>
          </div>
          <DropdownMenuSeparator class="ui-menu-separator" />
          <DropdownMenuItem @select="router.push('/me/profile')">
            <User class="text-muted-foreground" />
            个人资料
          </DropdownMenuItem>
          <DropdownMenuItem @select="toggleTheme">
            <Moon v-if="!isDark" class="text-muted-foreground" />
            <Sun v-else class="text-muted-foreground" />
            {{ isDark ? "切换到浅色" : "切换到深色" }}
          </DropdownMenuItem>
          <DropdownMenuSeparator class="ui-menu-separator" />
          <DropdownMenuItem destructive @select="logout">
            <LogOut />
            退出登录
          </DropdownMenuItem>
        </DropdownMenu>
      </div>

      <div class="workspace-navigation">
        <AppNav :zones="navigationZones" :collapsed="sidebarCollapsed" />
      </div>

      <button
        type="button"
        class="workspace-sidebar-footer text-left transition-colors hover:bg-sidebar-accent hover:text-destructive"
        aria-label="退出登录"
        @click="logout"
      >
        <LogOut class="size-3.5 shrink-0" /><span class="sidebar-label"
          >退出登录</span
        >
      </button>
    </aside>

    <div class="workspace-content">
      <header class="workspace-topbar">
        <Button
          variant="ghost"
          size="icon-sm"
          class="lg:hidden"
          aria-label="打开导航"
          @click="drawerOpen = true"
        >
          <Menu />
        </Button>
        <Button
          variant="ghost"
          size="icon-sm"
          class="hidden lg:inline-flex"
          :aria-label="sidebarCollapsed ? '展开侧栏' : '收起侧栏'"
          :aria-expanded="!sidebarCollapsed"
          @click="toggleSidebar"
        >
          <PanelLeft />
        </Button>
        <span class="mx-2 hidden h-4 w-px bg-border sm:block" />

        <nav
          class="flex min-w-0 items-center gap-2 text-xs"
          aria-label="面包屑"
        >
          <template v-for="(crumb, index) in crumbs" :key="crumb">
            <span
              v-if="index > 0"
              class="hidden select-none text-border-strong sm:inline"
              >/</span
            >
            <span
              :class="
                index === crumbs.length - 1
                  ? 'truncate font-medium text-foreground'
                  : 'hidden truncate text-muted-foreground sm:inline'
              "
            >
              {{ crumb }}
            </span>
          </template>
        </nav>
        <span
          class="ml-3 hidden shrink-0 rounded-md bg-primary/8 px-2 py-1 text-[10px] font-medium text-primary sm:inline-flex"
          ><span class="mr-1" aria-hidden="true">📊</span
          >{{ route.path.startsWith("/me") ? "个人办公" : "管理中心" }}</span
        >

        <div class="ml-auto flex shrink-0 items-center gap-1.5">
          <Button
            variant="ghost"
            size="icon-sm"
            aria-label="搜索页面或功能"
            :title="`搜索页面或功能（${shortcutLabel} K）`"
            @click="paletteOpen = true"
            ><span aria-hidden="true">🔍</span></Button
          >
          <Button
            variant="ghost"
            size="icon-sm"
            class="relative"
            aria-label="查看我的提醒"
            @click="router.push('/me/reminder')"
          >
            <span aria-hidden="true">🔔</span>
            <span
              v-if="meSummary.counts.reminder"
              class="absolute right-1.5 top-1 size-1.5 rounded-full bg-destructive ring-2 ring-card"
            />
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            class="lg:hidden"
            aria-label="账号菜单"
            @click="drawerOpen = true"
          >
            <Avatar :name="session.name" size="sm" />
          </Button>
        </div>
      </header>

      <main
        id="main-content"
        class="workspace-main"
        :class="{
          'portal-management': route.path.startsWith('/portal'),
          'portal-document-workspace': route.path.startsWith(
            '/portal/hr-documents/',
          ),
        }"
        tabindex="-1"
      >
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component
              :is="Component"
              :key="
                route.path.startsWith('/portal/hr-documents')
                  ? 'portal-hr-workspace'
                  : route.path
              "
            />
          </transition>
        </router-view>
      </main>
    </div>
    <template v-if="route.path.startsWith('/portal')">
      <PortalToastNotify />
      <PortalConfirmHost />
    </template>

    <Sheet
      v-model:open="drawerOpen"
      title="畅韵OA"
      description="导航与账号"
      side="left"
      tone="sidebar"
    >
      <div class="border-b border-sidebar-border px-4 py-3">
        <div class="flex items-center gap-2.5">
          <Avatar :name="session.name" size="sm" />
          <div class="min-w-0">
            <b
              class="block truncate text-sm font-medium text-sidebar-foreground"
              >{{ session.name || "未登录" }}</b
            >
            <small class="block truncate text-xs text-sidebar-muted">
              {{ session.isAdmin ? "管理员" : "员工" }} · {{ deptName }}
            </small>
          </div>
        </div>
      </div>
      <div class="px-2.5 py-3">
        <AppNav
          :zones="navigationZones"
          expand-only
          @navigate="drawerOpen = false"
        />
      </div>
      <div class="space-y-0.5 border-t border-sidebar-border p-2">
        <button
          type="button"
          class="flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-sm text-sidebar-muted hover:bg-sidebar-accent hover:text-sidebar-foreground"
          @click="toggleTheme"
        >
          <Sun v-if="isDark" class="size-4" />
          <Moon v-else class="size-4" />
          {{ isDark ? "切换到浅色模式" : "切换到深色模式" }}
        </button>
        <button
          type="button"
          class="flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-sm text-destructive hover:bg-destructive/10"
          @click="logout"
        >
          <LogOut class="size-4" />
          退出登录
        </button>
      </div>
    </Sheet>

    <CommandPalette
      v-model:open="paletteOpen"
      :zones="visibleZones"
      @navigate="navigateTo"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  ChevronsUpDown,
  LogOut,
  Menu,
  Moon,
  PanelLeft,
  Sun,
  User,
} from "@lucide/vue";
import { DropdownMenuSeparator } from "reka-ui";
import AppNav from "../components/AppNav.vue";
import CommandPalette from "../components/CommandPalette.vue";
import Avatar from "../ui/Avatar.vue";
import Button from "../ui/Button.vue";
import PortalToastNotify from "./portal/editor/legacy/components/ToastNotify.vue";
import PortalConfirmHost from "./portal/editor/legacy/components/ui/GlobalConfirmHost.vue";
import DropdownMenu from "../ui/DropdownMenu.vue";
import DropdownMenuItem from "../ui/DropdownMenuItem.vue";
import Sheet from "../ui/Sheet.vue";
import { navZones, type NavZone } from "../configs/nav";
import { filterPortalNavigation } from "../lib/portal-access";
import { findNavigationMatch } from "../lib/navigation";
import { loadPortalRollout } from "../lib/portal-rollout";
import { pageConfigs, tabbedPages } from "../configs/pages";
import { session } from "../lib/session";
import { meSummary, loadMeSummary } from "../lib/me";
import { isDark, toggleTheme } from "../lib/theme";

const route = useRoute();
const router = useRouter();
const drawerOpen = ref(false);
const paletteOpen = ref(false);
const sidebarCollapsed = ref(
  localStorage.getItem("cyt-oa-sidebar-collapsed") === "true",
);
const navigationZones = computed<NavZone[]>(() => {
  const personal = visibleZones.value.filter((zone) => !zone.adminOnly);
  return [
    {
      key: "me",
      title: "个人办公",
      items: personal.flatMap((zone) => zone.items),
    },
    ...visibleZones.value.filter((zone) => zone.adminOnly),
  ];
});
const shortcutLabel = /Mac|iPhone|iPad/.test(navigator.platform) ? "⌘" : "Ctrl";

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value;
  localStorage.setItem(
    "cyt-oa-sidebar-collapsed",
    String(sidebarCollapsed.value),
  );
}

const visibleZones = computed<NavZone[]>(() =>
  filterPortalNavigation(navZones)
    .filter((zone) => !zone.adminOnly || session.canAccessConsole)
    .map((zone) => ({
      ...zone,
      items: zone.items.filter(
        (item) => !item.requiresTeam || meSummary.teamCount > 0,
      ),
    }))
    .filter((zone) => zone.items.length > 0),
);

const deptName = computed(() => meSummary.deptName || "未分配部门");

const crumbs = computed<string[]>(() => {
  const match = findNavigationMatch(navigationZones.value, route.fullPath);
  if (match)
    return [
      match.zone.title,
      match.item.title,
      ...(match.child ? [match.child.title] : []),
    ];
  if (route.name === "page") {
    const key = String(route.params.key ?? "");
    return [
      "管理",
      (tabbedPages[key] ?? pageConfigs[key])?.title ?? "未找到页面",
    ];
  }
  return ["畅韵OA"];
});

function navigateTo(path: string) {
  paletteOpen.value = false;
  router.push(path);
}

watch(
  () => route.fullPath,
  () => {
    drawerOpen.value = false;
    window.scrollTo({ top: 0, behavior: "instant" });
  },
);

function onKeydown(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
    event.preventDefault();
    paletteOpen.value = !paletteOpen.value;
    return;
  }
  if (event.key !== "/" || event.metaKey || event.ctrlKey || event.altKey)
    return;
  const target = event.target as HTMLElement | null;
  if (
    target &&
    (/^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName) ||
      target.isContentEditable)
  )
    return;
  const search = document.querySelector<HTMLElement>("[data-page-search]");
  if (!search) return;
  event.preventDefault();
  const field = /^(INPUT|TEXTAREA|SELECT)$/.test(search.tagName)
    ? search
    : search.querySelector<HTMLElement>('input:not([type="hidden"]), button');
  field?.focus();
}

onMounted(() => {
  void loadMeSummary();
  window.addEventListener("keydown", onKeydown);
  window.addEventListener("focus", refreshRollout);
});

function refreshRollout() {
  if (session.canAccessConsole) void loadPortalRollout(true).catch(() => {});
}
onBeforeUnmount(() => {
  window.removeEventListener("keydown", onKeydown);
  window.removeEventListener("focus", refreshRollout);
});

function logout() {
  drawerOpen.value = false;
  session.clear();
  router.push("/login");
}
</script>
