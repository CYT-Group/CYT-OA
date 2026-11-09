<template>
  <nav class="app-navigation space-y-5" :class="{ 'nav-collapsed': collapsed }" aria-label="系统导航">
    <section v-for="zone in zones" :key="zone.key">
      <button
        type="button"
        class="nav-zone-title"
        :aria-expanded="!zoneClosed(zone.key)"
        @click="toggleZone(zone.key)"
      >
        <span class="flex-1 truncate text-left">{{ zone.title }}</span>
        <ChevronDown
          class="size-3 shrink-0 transition-transform duration-200"
          :class="zoneClosed(zone.key) ? '-rotate-90' : ''"
        />
      </button>

      <div v-show="collapsed || !zoneClosed(zone.key)" class="mt-1 space-y-px">
        <div v-for="item in zone.items" :key="item.key">
          <DropdownMenu v-if="collapsed && item.children?.length" side="right" align="start">
            <template #trigger>
              <button
                type="button"
                class="nav-item flex min-h-10 w-full items-center rounded-lg text-sidebar-muted hover:bg-sidebar-accent hover:text-sidebar-foreground"
                :class="hasActiveChild(item) ? 'bg-sidebar-active text-white' : ''"
                :aria-label="item.title"
                :title="item.title"
              >
                <component :is="iconOf(item.icon)" class="nav-item-icon" :stroke-width="1.8" />
              </button>
            </template>
            <p class="px-2 py-2 text-xs font-semibold text-muted-foreground">{{ item.title }}</p>
            <DropdownMenuItem
              v-for="child in item.children"
              :key="child.to"
              @select="navigate(child.to)"
              >{{ child.title }}</DropdownMenuItem
            >
          </DropdownMenu>
          <div v-else class="nav-item-row group/item flex items-center gap-0.5" :class="{ 'nav-parent-active': hasActiveChild(item) }">
            <router-link
              :to="item.to"
              :title="collapsed ? item.title : undefined"
              :aria-current="exactActive(item) ? 'page' : undefined"
              :class="
                cn(
                  'nav-item flex min-h-10 min-w-0 flex-1 items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-colors',
                  exactActive(item)
                    ? 'nav-item-active bg-sidebar-active font-medium text-white'
                    : hasActiveChild(item)
                      ? 'font-medium text-sidebar-foreground'
                      : 'text-sidebar-muted hover:bg-sidebar-accent hover:text-sidebar-foreground'
                )
              "
              @click="emit('navigate')"
            >
              <component :is="iconOf(item.icon)" class="nav-item-icon" :stroke-width="1.8" />
              <span class="nav-item-label flex-1 truncate">{{ item.title }}</span>
              <span
                v-if="badgeOf(item) && !collapsed"
                :class="
                  cn(
                    'min-w-[18px] rounded px-1 text-center text-[10px] font-semibold leading-[16px] tabular-nums',
                    item.badge === 'reminder'
                      ? 'bg-destructive text-white'
                      : 'bg-sidebar-accent text-sidebar-foreground'
                  )
                "
              >
                {{ badgeOf(item) }}
              </span>
            </router-link>
            <button
              v-if="item.children?.length && !collapsed"
              type="button"
              class="nav-expand flex size-8 shrink-0 items-center justify-center rounded-md text-sidebar-muted transition-colors hover:bg-sidebar-accent hover:text-sidebar-foreground"
              :aria-expanded="isOpen(item.key)"
              :aria-label="`${isOpen(item.key) ? '收起' : '展开'}${item.title}子菜单`"
              @click="expand(item.key, !isOpen(item.key))"
            >
              <ChevronRight
                class="size-3.5 transition-transform"
                :class="isOpen(item.key) ? 'rotate-90' : ''"
              />
            </button>
          </div>

          <div
            v-if="item.children?.length && isOpen(item.key) && !collapsed"
            class="nav-submenu"
          >
            <router-link
              v-for="child in item.children"
              :key="child.to"
              :to="child.to"
              :aria-current="samePath(child.to) ? 'page' : undefined"
              :class="
                cn(
                  'nav-child',
                  samePath(child.to)
                    ? 'nav-child-active'
                    : 'text-sidebar-muted hover:bg-sidebar-accent hover:text-sidebar-foreground'
                )
              "
              @click="emit('navigate')"
            >
              {{ child.title }}
            </router-link>
          </div>
        </div>
      </div>
    </section>
  </nav>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronDown, ChevronRight } from '@lucide/vue'
import type { NavItem, NavZone } from '../configs/nav'
import { meSummary } from '../lib/me'
import { iconOf } from '../lib/icons'
import { cn } from '../lib/utils'
import DropdownMenu from '../ui/DropdownMenu.vue'
import DropdownMenuItem from '../ui/DropdownMenuItem.vue'

const props = defineProps<{ zones: NavZone[]; collapsed?: boolean }>()
const emit = defineEmits<{ navigate: [] }>()

const route = useRoute()
const router = useRouter()

function navigate(path: string) {
  void router.push(path)
  emit('navigate')
}

const OPEN_KEY = 'cyt-oa-nav-open'
const ZONE_KEY = 'cyt-oa-nav-zone-closed'

/** 本地存储的展开状态可能被手工改坏，读失败时按默认值走 */
function readSet(key: string): Set<string> {
  try {
    const saved: unknown = JSON.parse(localStorage.getItem(key) || '[]')
    return new Set(Array.isArray(saved) ? saved.filter((x): x is string => typeof x === 'string') : [])
  } catch {
    return new Set()
  }
}

function saveSet(key: string, set: Set<string>) {
  try {
    localStorage.setItem(key, JSON.stringify([...set]))
  } catch {
    /* 隐私模式下写不了，忽略即可 */
  }
}

// 桌面侧栏与移动抽屉共用同一份展开状态
const open = ref<Set<string>>(readSet(OPEN_KEY))
const closedZones = ref<Set<string>>(readSet(ZONE_KEY))

function badgeOf(item: NavItem): number | '' {
  if (!item.badge) return ''
  return meSummary.counts[item.badge as keyof typeof meSummary.counts] ?? ''
}

function isOpen(key: string) {
  return open.value.has(key)
}

function expand(key: string, on: boolean) {
  const next = new Set(open.value)
  if (on) next.add(key)
  else next.delete(key)
  open.value = next
  saveSet(OPEN_KEY, next)
}

function zoneClosed(key: string) {
  return closedZones.value.has(key)
}

function toggleZone(key: string) {
  const next = new Set(closedZones.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  closedZones.value = next
  saveSet(ZONE_KEY, next)
}

function norm(path: string) {
  return path.split('?')[0]
}

function samePath(path: string) {
  return norm(path) === norm(route.fullPath)
}

function hasActiveChild(item: NavItem) {
  return item.children?.some((c) => samePath(c.to)) ?? false
}

function exactActive(item: NavItem) {
  return samePath(item.to) && !hasActiveChild(item)
}

/** 进到某个页面时自动展开它的子菜单；只在「一级项换人了」时展开 */
let autoOpened = ''
watch(
  () => route.fullPath,
  (full) => {
    const parent = findParentKey(full)
    if (!parent || parent === autoOpened) return
    autoOpened = parent
    if (!isOpen(parent)) expand(parent, true)
  },
  { immediate: true }
)

function findParentKey(full: string): string {
  for (const zone of props.zones) {
    for (const item of zone.items) {
      if (!item.children?.length) continue
      if (item.children.some((c) => norm(c.to) === norm(full))) return item.key
    }
  }
  return ''
}
</script>
