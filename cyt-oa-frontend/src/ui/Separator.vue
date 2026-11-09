<template>
  <div :class="cn('shrink-0 bg-border', vertical ? 'h-full w-px' : 'h-px w-full', props.class)" role="separator" />
</template>

<script setup lang="ts">
import { cn } from '../lib/utils'

const props = defineProps<{ vertical?: boolean; class?: string }>()
</script>
