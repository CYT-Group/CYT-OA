<template>
  <div class="workspace" :class="{ 'workspace-collapsed': sidebarCollapsed }">
    <a href="#main-content" class="skip-link">跳转到内容</a>
    <aside class="workspace-sidebar">
      <router-link to="/" class="workspace-brand">
        <span class="brand-mark">
          <LayoutGrid class="size-[15px]" :stroke-width="2.2" />
        </span>
        <span class="sidebar-label min-w-0">
          <b class="block truncate text-[17px] font-semibold tracking-wide text-sidebar-foreground"
            >畅盈通 <span class="font-normal text-sidebar-muted">OA</span></b
          >
          <small class="block text-[10px] tracking-[0.18em] text-sidebar-muted">WORK TOGETHER, BETTER</small>
        </span>
      </router-link>

      <div v-if="session.isAdmin" class="workspace-switch sidebar-label" aria-label="切换工作空间">
        <router-link to="/me/overview" :class="{ active: !adminSpace }"
          ><House class="size-3.5" />我的工作台</router-link
        >
        <router-link to="/admin" :class="{ active: adminSpace }"
          ><BriefcaseBusiness class="size-3.5" />管理控制台</router-link
        >
      </div>
      <div v-else class="sidebar-label px-6 pb-3 text-xs text-sidebar-muted">我的工作空间</div>
      <router-link
        v-if="session.isAdmin && sidebarCollapsed"
        :to="adminSpace ? '/me/overview' : '/admin'"
        class="mx-auto mb-4 grid size-10 place-items-center rounded-lg bg-sidebar-accent text-sidebar-foreground"
        :title="adminSpace ? '切换到我的工作台' : '切换到管理控制台'"
        :aria-label="adminSpace ? '切换到我的工作台' : '切换到管理控制台'"
        ><House v-if="adminSpace" class="size-4" /><BriefcaseBusiness v-else class="size-4"
      /></router-link>

      <div class="workspace-navigation">
        <AppNav :zones="navigationZones" :collapsed="sidebarCollapsed" />
      </div>

      <div class="sidebar-label mx-4 mb-4 rounded-lg border border-sidebar-border px-3 py-3">
        <p class="flex items-center gap-2 text-xs font-medium text-sidebar-foreground">
          <span class="size-1.5 rounded-full bg-emerald-400" />让协作更简单
        </p>
        <p class="mt-1.5 text-[11px] leading-relaxed text-sidebar-muted">日常事务有序，重要工作专注。</p>
      </div>

      <div class="shrink-0 border-t border-sidebar-border p-2">
        <DropdownMenu>
          <template #trigger>
            <button
              type="button"
              class="flex w-full items-center gap-2.5 rounded-lg p-2 text-left transition-colors hover:bg-sidebar-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60"
            >
              <Avatar :name="session.name" size="sm" />
              <span class="sidebar-label min-w-0 flex-1">
                <b class="block truncate text-[13px] font-medium leading-tight text-sidebar-foreground">
                  {{ session.name || '未登录' }}
                </b>
                <small class="block truncate text-[11px] text-sidebar-muted">
                  {{ session.isAdmin ? '管理员' : '员工' }} · {{ deptName }}
                </small>
              </span>
              <ChevronsUpDown class="sidebar-label size-3.5 shrink-0 text-sidebar-muted" />
            </button>
          </template>
          <DropdownMenuItem @select="router.push('/me/profile')">
            <User class="text-muted-foreground" />
            个人资料
          </DropdownMenuItem>
          <DropdownMenuItem @select="toggleTheme">
            <Moon v-if="!isDark" class="text-muted-foreground" />
            <Sun v-else class="text-muted-foreground" />
            {{ isDark ? '切换到浅色' : '切换到深色' }}
          </DropdownMenuItem>
          <DropdownMenuSeparator class="my-1 h-px bg-border" />
          <DropdownMenuItem destructive @select="logout">
            <LogOut />
            退出登录
          </DropdownMenuItem>
        </DropdownMenu>
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

        <nav class="flex min-w-0 items-center gap-2 text-xs" aria-label="面包屑">
          <template v-for="(crumb, index) in crumbs" :key="crumb">
            <span v-if="index > 0" class="hidden select-none text-border-strong sm:inline">/</span>
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

        <div class="ml-auto flex shrink-0 items-center gap-1.5">
          <span class="mr-4 hidden text-xs text-muted-foreground xl:inline">{{ headerDate }}</span>
          <button type="button" class="topbar-search" @click="paletteOpen = true">
            <Search class="size-3.5" />
            <span>搜索页面或功能</span>
            <span class="kbd ml-5">{{ shortcutLabel }} K</span>
          </button>
          <Button
            variant="ghost"
            size="icon-sm"
            class="sm:hidden"
            aria-label="搜索页面"
            @click="paletteOpen = true"
          >
            <Search />
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            class="relative"
            aria-label="查看我的提醒"
            @click="router.push('/me/reminder')"
          >
            <Bell />
            <span
              v-if="meSummary.counts.reminder"
              class="absolute right-1.5 top-1 size-1.5 rounded-full bg-destructive ring-2 ring-card"
            />
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            :aria-label="isDark ? '切换到浅色' : '切换到深色'"
            @click="toggleTheme"
          >
            <Sun v-if="isDark" />
            <Moon v-else />
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

      <main id="main-content" class="workspace-main" tabindex="-1">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
      <footer class="workspace-footer">
        <span>畅盈通 OA · 协同办公平台</span><span>专注工作，高效协作</span>
      </footer>
    </div>

    <Sheet v-model:open="drawerOpen" title="畅盈通 OA" description="导航与账号" side="left" tone="sidebar">
      <div class="border-b border-sidebar-border px-4 py-3">
        <div class="flex items-center gap-2.5">
          <Avatar :name="session.name" size="sm" />
          <div class="min-w-0">
            <b class="block truncate text-sm font-medium text-sidebar-foreground">{{
              session.name || '未登录'
            }}</b>
            <small class="block truncate text-xs text-sidebar-muted">
              {{ session.isAdmin ? '管理员' : '员工' }} · {{ deptName }}
            </small>
          </div>
        </div>
      </div>
      <div class="px-2.5 py-3">
        <AppNav :zones="visibleZones" @navigate="drawerOpen = false" />
      </div>
      <div class="space-y-0.5 border-t border-sidebar-border p-2">
        <button
          type="button"
          class="flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-sm text-sidebar-muted hover:bg-sidebar-accent hover:text-sidebar-foreground"
          @click="toggleTheme"
        >
          <Sun v-if="isDark" class="size-4" />
          <Moon v-else class="size-4" />
          {{ isDark ? '切换到浅色模式' : '切换到深色模式' }}
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

    <CommandPalette v-model:open="paletteOpen" :zones="visibleZones" @navigate="navigateTo" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  BriefcaseBusiness,
  ChevronsUpDown,
  House,
  LayoutGrid,
  LogOut,
  Menu,
  Moon,
  PanelLeft,
  Search,
  Sun,
  User
} from '@lucide/vue'
import { DropdownMenuSeparator } from 'reka-ui'
import AppNav from '../components/AppNav.vue'
import CommandPalette from '../components/CommandPalette.vue'
import Avatar from '../ui/Avatar.vue'
import Button from '../ui/Button.vue'
import DropdownMenu from '../ui/DropdownMenu.vue'
import DropdownMenuItem from '../ui/DropdownMenuItem.vue'
import Sheet from '../ui/Sheet.vue'
import { navZones, type NavZone } from '../configs/nav'
import { pageConfigs, tabbedPages } from '../configs/pages'
import { session } from '../lib/session'
import { meSummary, loadMeSummary } from '../lib/me'
import { isDark, toggleTheme } from '../lib/theme'

const route = useRoute()
const router = useRouter()
const drawerOpen = ref(false)
const paletteOpen = ref(false)
const sidebarCollapsed = ref(localStorage.getItem('cyt-oa-sidebar-collapsed') === 'true')
const adminSpace = computed(() => session.isAdmin && !route.path.startsWith('/me'))
const navigationZones = computed(() =>
  visibleZones.value.filter((zone) => (adminSpace.value ? zone.adminOnly : !zone.adminOnly))
)
const headerDate = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())
const shortcutLabel = /Mac|iPhone|iPad/.test(navigator.platform) ? '⌘' : 'Ctrl'

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('cyt-oa-sidebar-collapsed', String(sidebarCollapsed.value))
}

const visibleZones = computed<NavZone[]>(() =>
  navZones.filter((zone) => {
    if (zone.adminOnly) return session.isAdmin
    if (zone.key === 'team') return meSummary.teamCount > 0
    return true
  })
)

const deptName = computed(() => meSummary.deptName || '未分配部门')

function norm(path: string) {
  return path.split('?')[0]
}

const crumbs = computed<string[]>(() => {
  if (route.name === 'page') {
    const key = String(route.params.key ?? '')
    return ['管理', (tabbedPages[key] ?? pageConfigs[key])?.title ?? '未找到页面']
  }
  if (route.name === 'console') return ['管理', '控制台概览']
  for (const zone of visibleZones.value) {
    for (const item of zone.items) {
      if (norm(item.to) === norm(route.fullPath)) return [zone.title, item.title]
      const child = item.children?.find((c) => norm(c.to) === norm(route.fullPath))
      if (child) return [zone.title, item.title, child.title]
    }
  }
  return ['畅盈通 OA']
})

function navigateTo(path: string) {
  paletteOpen.value = false
  router.push(path)
}

watch(
  () => route.fullPath,
  () => {
    drawerOpen.value = false
    window.scrollTo({ top: 0 })
  }
)

function onKeydown(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    paletteOpen.value = !paletteOpen.value
    return
  }
  if (event.key !== '/' || event.metaKey || event.ctrlKey || event.altKey) return
  const target = event.target as HTMLElement | null
  if (target && (/^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName) || target.isContentEditable)) return
  const search = document.querySelector<HTMLElement>('[data-page-search]')
  if (!search) return
  event.preventDefault()
  const field = /^(INPUT|TEXTAREA|SELECT)$/.test(search.tagName)
    ? search
    : search.querySelector<HTMLElement>('input:not([type="hidden"]), button')
  field?.focus()
}

onMounted(() => {
  void loadMeSummary()
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))

function logout() {
  drawerOpen.value = false
  session.clear()
  router.push('/login')
}
</script>
