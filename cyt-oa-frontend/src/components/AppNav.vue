<template>
  <nav
    class="app-navigation"
    :class="{ 'nav-collapsed': collapsed }"
    aria-label="系统导航"
  >
    <section v-for="zone in zones" :key="zone.key">
      <h2
        class="nav-zone-title"
        :class="{ 'nav-zone-management': zone.adminOnly }"
      >
        <span v-if="zone.glyph" aria-hidden="true">{{ zone.glyph }} </span
        >{{ zone.title }}
      </h2>
      <div v-for="item in zone.items" :key="item.key" class="nav-item-row">
        <DropdownMenu
          v-if="collapsed && item.children?.length"
          side="right"
          align="start"
        >
          <template #trigger
            ><button
              type="button"
              class="nav-item nav-item-header"
              :class="{ 'nav-item-active': active?.item.key === item.key }"
              :aria-label="item.title"
              :title="item.title"
            >
              <NavIcon :name="item.icon" :glyph="item.glyph ?? ''" /></button
          ></template>
          <p class="px-2 py-2 text-xs font-semibold text-muted-foreground">
            {{ item.title }}
          </p>
          <DropdownMenuItem
            v-for="child in item.children"
            :key="child.to"
            @select="navigate(child.to)"
            ><NavIcon
              v-if="child.glyph || child.icon"
              :name="child.icon"
              :glyph="child.glyph ?? ''"
            />{{ child.title }}</DropdownMenuItem
          >
        </DropdownMenu>
        <template v-else>
          <button
            v-if="item.children?.length"
            type="button"
            class="nav-item nav-item-header"
            :class="{ 'nav-item-active': active?.item.key === item.key }"
            :aria-expanded="isOpen(item.key)"
            :aria-controls="`nav-submenu-${item.key}`"
            @click="activate(item)"
          >
            <NavIcon :name="item.icon" :glyph="item.glyph ?? ''" />
            <span class="nav-item-label">{{ item.title }}</span>
            <span v-if="item.automatic" class="nav-automatic">自动</span>
            <span v-if="badgeOf(item)" class="nav-count">{{
              badgeOf(item)
            }}</span>
            <span class="nav-arrow" aria-hidden="true">{{
              isOpen(item.key) ? "▼" : "▶"
            }}</span>
          </button>
          <router-link
            v-else
            :to="item.to"
            class="nav-item nav-item-header"
            :class="{ 'nav-item-active': active?.item.key === item.key }"
            :aria-current="active?.item.key === item.key ? 'page' : undefined"
            :aria-label="collapsed ? item.title : undefined"
            :title="collapsed ? item.title : undefined"
            @click="emit('navigate')"
          >
            <NavIcon :name="item.icon" :glyph="item.glyph ?? ''" /><span
              class="nav-item-label"
              >{{ item.title }}</span
            ><span v-if="badgeOf(item) && !collapsed" class="nav-count">{{
              badgeOf(item)
            }}</span>
          </router-link>
          <div
            v-if="item.children?.length && isOpen(item.key) && !collapsed"
            :id="`nav-submenu-${item.key}`"
            class="nav-submenu"
          >
            <router-link
              v-for="child in item.children"
              :key="child.to"
              :to="child.to"
              class="nav-child"
              :class="{
                'nav-child-active':
                  active?.item.key === item.key &&
                  active?.child?.to === child.to,
              }"
              :aria-current="
                active?.item.key === item.key && active?.child?.to === child.to
                  ? 'page'
                  : undefined
              "
              @click="emit('navigate')"
            >
              <NavIcon
                v-if="child.glyph || child.icon"
                :name="child.icon"
                :glyph="child.glyph ?? ''"
              /><span>{{ child.title }}</span>
            </router-link>
          </div>
        </template>
      </div>
    </section>
  </nav>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { NavItem, NavZone } from "../configs/nav";
import { findNavigationMatch } from "../lib/navigation";
import { meSummary } from "../lib/me";
import NavIcon from "./NavIcon.vue";
import DropdownMenu from "../ui/DropdownMenu.vue";
import DropdownMenuItem from "../ui/DropdownMenuItem.vue";
const props = defineProps<{
  zones: NavZone[];
  collapsed?: boolean;
  expandOnly?: boolean;
}>();
const emit = defineEmits<{ navigate: [] }>();
const route = useRoute(),
  router = useRouter();
const active = computed(() => findNavigationMatch(props.zones, route.fullPath));
const open = ref(new Set<string>());
const isOpen = (key: string) => open.value.has(key);
function expand(key: string, on: boolean) {
  open.value = new Set<string>(on ? [key] : []);
}
function badgeOf(item: NavItem): number | "" {
  return item.badge
    ? meSummary.counts[item.badge as keyof typeof meSummary.counts] || ""
    : "";
}
function navigate(path: string) {
  void router.push(path);
  emit("navigate");
}
function activate(item: NavItem) {
  if (isOpen(item.key)) {
    expand(item.key, false);
    return;
  }
  expand(item.key, true);
  // 展开当前模块时保留正在阅读的二级页，其他模块进入第一个可访问入口。
  if (!props.expandOnly && active.value?.item.key !== item.key)
    navigate(item.to);
}
watch(
  () => active.value?.item.key,
  (key) => {
    if (key && active.value?.item.children?.length) expand(key, true);
    else open.value = new Set();
  },
  { immediate: true },
);
</script>
