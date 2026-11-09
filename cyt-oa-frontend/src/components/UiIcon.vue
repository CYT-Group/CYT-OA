<template>
  <component :is="icon" :size="size" :stroke-width="1.8" aria-hidden="true" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { iconOf } from '../lib/icons'

const props = withDefaults(defineProps<{ name: string; size?: number }>(), { size: 16 })
const icon = computed(() => iconOf(props.name))
</script>
