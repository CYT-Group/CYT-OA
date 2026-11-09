<template>
  <div :class="cn('flex flex-col items-center justify-center px-6 text-center', compact ? 'py-10' : 'py-16')">
    <div class="mb-4 flex size-11 items-center justify-center rounded-xl border border-border bg-muted/60 text-muted-foreground">
      <component :is="icon" v-if="icon" class="size-5" :stroke-width="1.7" />
      <Inbox v-else class="size-5" :stroke-width="1.7" />
    </div>
    <p class="text-sm font-medium text-foreground">{{ title }}</p>
    <p v-if="description" class="mt-1 max-w-sm text-xs leading-relaxed text-muted-foreground">{{ description }}</p>
    <div v-if="$slots.default" class="mt-4">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { Inbox } from '@lucide/vue'
import { cn } from '../lib/utils'

withDefaults(defineProps<{ title: string; description?: string; icon?: Component; compact?: boolean }>(), {
  compact: false
})
</script>
