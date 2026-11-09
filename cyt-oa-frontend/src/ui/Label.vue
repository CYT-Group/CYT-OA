<template>
  <label :class="cn('mb-1.5 flex items-center gap-1 text-xs font-medium text-foreground', props.class)">
    <slot />
    <span v-if="required" class="text-destructive" aria-hidden="true">*</span>
  </label>
</template>

<script setup lang="ts">
import { cn } from '../lib/utils'

const props = defineProps<{ required?: boolean; class?: string }>()
</script>
