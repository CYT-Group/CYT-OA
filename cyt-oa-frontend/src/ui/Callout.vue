<template>
  <div :class="cn('flex gap-3 rounded-lg border p-3 text-sm', toneClass, props.class)">
    <component :is="iconComponent" class="mt-0.5 size-4 shrink-0" :stroke-width="1.9" />
    <div class="min-w-0 flex-1">
      <p v-if="title" class="font-medium">{{ title }}</p>
      <div v-if="$slots.default" :class="cn('text-xs leading-relaxed', title ? 'mt-0.5' : '', toneText)">
        <slot />
      </div>
    </div>
    <slot name="action" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Info, TriangleAlert, CircleCheck, CircleAlert } from '@lucide/vue'
import { cn } from '../lib/utils'

const props = withDefaults(
  defineProps<{
    tone?: 'info' | 'success' | 'warning' | 'destructive'
    title?: string
    class?: string
  }>(),
  { tone: 'info' }
)

const iconComponent = computed(
  () => ({ info: Info, success: CircleCheck, warning: TriangleAlert, destructive: CircleAlert })[props.tone]
)

const toneClass = computed(
  () =>
    ({
      info: 'border-info/20 bg-info-soft text-info',
      success: 'border-success/20 bg-success-soft text-success',
      warning: 'border-warning/25 bg-warning-soft text-warning',
      destructive: 'border-destructive/20 bg-destructive/5 text-destructive'
    })[props.tone]
)

const toneText = computed(
  () =>
    ({
      info: 'text-info/90',
      success: 'text-success/90',
      warning: 'text-warning/90',
      destructive: 'text-destructive/90'
    })[props.tone]
)
</script>
