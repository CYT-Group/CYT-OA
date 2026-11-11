<template>
  <div class="workspace" :class="{ 'workspace-collapsed': sidebarCollapsed }">
    <a href="#main-content" class="skip-link">跳转到内容</a>
    <aside class="workspace-sidebar">
      <router-link to="/" class="workspace-brand" aria-label="CTY OA 首页">
        <span class="brand-mark" aria-hidden="true">
          <PanelsTopLeft :size="19" :stroke-width="1.8" />
        </span>
        <span class="sidebar-label workspace-brand-copy">
          <span class="workspace-brand-name">
            CTY
            <span>OA</span>
          </span>
          <span class="workspace-brand-description">企业协同办公</span>
        </span>
      </router-link>

      <div class="workspace-navigation">
        <AppNav :zones="navigationZones" :collapsed="sidebarCollapsed" />
      </div>

      <div class="workspace-account-footer">
        <AccountMenu :user="accountUser" @logout="logout" />
      </div>
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
            >
              /
            </span>
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
        >
          <span class="mr-1" aria-hidden="true">📊</span>
          {{
            isHrPage
              ? "人事管理"
              : route.path.startsWith("/me")
                ? "个人办公"
                : "管理中心"
          }}
        </span>

        <div class="ml-auto flex shrink-0 items-center gap-1.5">
          <Button
            variant="ghost"
            size="icon-sm"
            aria-label="搜索页面或功能"
            :title="`搜索页面或功能（${shortcutLabel} K）`"
            @click="paletteOpen = true"
          >
            <span aria-hidden="true">🔍</span>
          </Button>
          <Button
            v-if="!session.isHrOnly"
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
          <div class="lg:hidden">
            <AccountMenu :user="accountUser" compact @logout="logout" />
          </div>
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
      title="CTY OA"
      description="导航与账号"
      side="left"
      tone="sidebar"
    >
      <div class="px-2.5 py-3">
        <AppNav
          :zones="navigationZones"
          expand-only
          @navigate="drawerOpen = false"
        />
      </div>
      <template #footer>
        <AccountMenu
          :user="accountUser"
          side="top"
          class="w-full"
          @logout="logout"
          @navigate="drawerOpen = false"
        />
      </template>
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
import { Menu, PanelLeft, PanelsTopLeft } from "@lucide/vue";
import AccountMenu from "../components/AccountMenu.vue";
import AppNav from "../components/AppNav.vue";
import CommandPalette from "../components/CommandPalette.vue";
import Button from "../ui/Button.vue";
import PortalToastNotify from "./portal/editor/legacy/components/ToastNotify.vue";
import PortalConfirmHost from "./portal/editor/legacy/components/ui/GlobalConfirmHost.vue";
import Sheet from "../ui/Sheet.vue";
import { navZones, type NavZone } from "../configs/nav";
import { filterPortalNavigation } from "../lib/portal-access";
import { findNavigationMatch, sidebarNavigation, personalizeNavigation } from "../lib/navigation";
import { loadPortalRollout } from "../lib/portal-rollout";
import { pageConfigs, tabbedPages } from "../configs/pages";
import { session } from "../lib/session";
import { meSummary, loadMeSummary } from "../lib/me";
import { workspaceContext, loadWorkspaceContext } from "../lib/workspace-context";

const route = useRoute();
const router = useRouter();
const drawerOpen = ref(false);
const paletteOpen = ref(false);
const sidebarCollapsed = ref(
  localStorage.getItem("cyt-oa-sidebar-collapsed") === "true",
);
const isHrPage = computed(
  () =>
    session.isHrOnly ||
    route.path.startsWith("/hr/") ||
    ["/page/hrm-employee", "/portal/contracts", "/portal/resumes"].includes(
      route.path,
    ),
);
// 人事页面与其他业务共用同一棵门户目录，只更新父子选中态，不替换侧栏。
const navigationZones = computed<NavZone[]>(() =>
  sidebarNavigation(visibleZones.value),
);
const accountUser = computed(() => ({
  name: session.name,
  department: meSummary.deptName,
  role: session.isAdmin
    ? "系统管理员"
    : session.isHrOnly
      ? "HR 管理员"
      : session.canAccessConsole
        ? "管理成员"
        : "员工",
}));
const shortcutLabel = /Mac|iPhone|iPad/.test(navigator.platform) ? "⌘" : "Ctrl";

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value;
  localStorage.setItem(
    "cyt-oa-sidebar-collapsed",
    String(sidebarCollapsed.value),
  );
}

const visibleZones = computed<NavZone[]>(() =>
  personalizeNavigation(filterPortalNavigation(navZones).filter(zone => !zone.adminOnly || session.canAccessConsole), {
    teamCount: Number(meSummary.teamCount),
    isAdmin: session.isAdmin,
    canAccessConsole: session.canAccessConsole,
    canApproveMaterials: session.isAdmin || workspaceContext.owner === session.token && workspaceContext.value?.canApproveMaterials === true,
    canApproveRequests: workspaceContext.owner === session.token && workspaceContext.value?.canApproveRequests === true,
  }),
);

const crumbs = computed<string[]>(() => {
  if (route.path === "/portal")
    return route.query.view === "tools"
      ? ["管理中心", "管理工具"]
      : ["管理中心"];
  const match = findNavigationMatch(visibleZones.value, route.fullPath);
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
  return ["CTY OA"];
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
  void loadWorkspaceContext();
  window.addEventListener("keydown", onKeydown);
  window.addEventListener("focus", refreshRollout);
});

function refreshRollout() {
  if (session.canAccessConsole) void loadPortalRollout(true).catch(() => {});
  void loadWorkspaceContext();
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
