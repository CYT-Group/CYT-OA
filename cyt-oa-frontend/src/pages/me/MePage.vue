<template>
  <component :is="current" v-if="current" :key="view" />
  <div v-else class="panel flex flex-col items-center gap-2 p-12 text-center">
    <p class="text-sm font-medium">没有这个页面</p>
    <p class="text-xs text-muted-foreground">页面标识：{{ view }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import OverviewView from './views/OverviewView.vue'
import RhythmView from './views/RhythmView.vue'
import ReminderView from './views/ReminderView.vue'
import ReportView from './views/ReportView.vue'
import ApplyView from './views/ApplyView.vue'
import ProfileView from './views/ProfileView.vue'
import TeamView from './views/TeamView.vue'

const route = useRoute()

const views: Record<string, typeof OverviewView> = {
  overview: OverviewView,
  rhythm: RhythmView,
  reminder: ReminderView,
  report: ReportView,
  apply: ApplyView,
  profile: ProfileView,
  team: TeamView
}

const view = computed(() => String(route.params.view ?? 'overview'))
const current = computed(() => views[view.value] ?? null)
</script>
