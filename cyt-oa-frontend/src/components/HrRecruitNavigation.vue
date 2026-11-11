<template>
  <nav
    aria-label="招聘管理"
    class="flex flex-wrap gap-1 rounded-xl border bg-card p-1"
  >
    <RouterLink
      v-for="item in items"
      :key="item.key"
      :to="item.to"
      class="rounded-lg px-3 py-2 text-xs font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      :class="
        matchesNavigation(item.to, current)
          ? 'bg-primary/10 text-primary'
          : 'text-muted-foreground hover:bg-accent hover:text-foreground'
      "
      :aria-current="matchesNavigation(item.to, current) ? 'page' : undefined"
    >
      {{ item.title }}
    </RouterLink>
  </nav>
</template>
<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { navZones } from '../configs/nav';
import { allowed } from '../lib/portal-access';
import { matchesNavigation } from '../lib/navigation';
const route = useRoute();
const keys = new Set([
  'hr-candidates',
  'hr-interviews',
  'hr-posts',
  'portal-resumes'
]);
const pages =
  navZones
    .flatMap((zone) => zone.items)
    .find((item) => item.key === 'people-management')?.children ?? [];
const items = computed(() =>
  pages
    .filter((item) => item.key && keys.has(item.key) && allowed(item))
    .map((item) => ({
      ...item,
      title: item.key === 'hr-candidates' ? '候选人与简历' : item.title
    }))
);
const current = computed(() =>
  route.path === '/hr/recruit' && !route.query.tab
    ? (items.value.find((item) => item.to.startsWith('/hr/recruit'))?.to ??
      route.fullPath)
    : route.fullPath
);
</script>
