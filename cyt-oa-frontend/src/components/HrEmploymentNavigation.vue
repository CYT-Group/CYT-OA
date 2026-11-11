<template>
  <nav
    aria-label="入离职与合同"
    class="flex flex-wrap gap-1 rounded-xl border bg-card p-1"
  >
    <RouterLink
      v-for="item in items"
      :key="item.to"
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
import { allowed } from '../lib/portal-access';
import { session } from '../lib/session';
import { matchesNavigation } from '../lib/navigation';
const route = useRoute();
const current = computed(() =>
  route.path === '/hr/employment' && !route.query.mode
    ? '/hr/employment?mode=onboard'
    : route.fullPath
);
const items = computed(() => [
  ...(session.has('hrm:employee:update') &&
  session.has('hrm:employee:query') &&
  session.has('hrm:contract-template:query')
    ? [
        { title: '入职办理', to: '/hr/employment?mode=onboard' },
        { title: '离职办理', to: '/hr/employment?mode=offboard' }
      ]
    : []),
  ...(session.has('hrm:contract-template:query')
    ? [{ title: '合同模板', to: '/hr/templates' }]
    : []),
  ...(allowed({ key: 'portal-contracts', permission: 'can_generate_contracts' })
    ? [{ title: 'Word 合同生成', to: '/portal/contracts' }]
    : [])
]);
</script>
