<template>
  <div :class="cn('panel group relative overflow-hidden p-5 transition-colors', props.class)">
    <div class="relative flex items-start justify-between gap-3">
      <div class="min-w-0">
        <p class="truncate text-[13px] font-medium text-muted-foreground">{{ title }}</p>
        <p class="mt-4 text-[30px] font-semibold leading-none tracking-tight tabular-nums text-foreground">
          {{ value }}
        </p>
      </div>
      <span v-if="icon" :class="cn('grid size-11 shrink-0 place-items-center rounded-xl', toneChip)">
        <component :is="icon" class="size-[18px]" :stroke-width="1.9" />
      </span>
    </div>
    <p
      v-if="hint"
      class="relative mt-4 truncate border-t border-border/70 pt-3 text-[11px] text-muted-foreground"
    >
      {{ hint }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import { cn } from '../lib/utils'

const props = withDefaults(
  defineProps<{
    title: string
    value: string | number
    hint?: string
    icon?: Component
    tone?: 'primary' | 'info' | 'success' | 'warning' | 'destructive'
    class?: string
  }>(),
  { tone: 'primary' }
)

const toneChip = computed(
  () =>
    ({
      primary: 'bg-primary/10 text-primary ring-primary/15 dark:bg-primary/12',
      info: 'bg-info-soft text-info ring-info/15 dark:bg-info-soft',
      success: 'bg-success-soft text-success ring-success/15 dark:bg-success-soft',
      warning: 'bg-warning-soft text-warning ring-warning/20 dark:bg-warning-soft',
      destructive: 'bg-destructive/10 text-destructive ring-destructive/15'
    })[props.tone]
)
</script>
