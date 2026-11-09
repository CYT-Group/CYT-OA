<template>
  <button
    type="button"
    class="group/sort -mx-1 inline-flex items-center gap-1 rounded px-1 py-0.5 transition-colors hover:text-foreground"
    @click="emit('sort')"
  >
    {{ label }}
    <component
      :is="icon"
      class="size-3 shrink-0"
      :class="active ? 'text-foreground' : 'text-muted-foreground/40 group-hover/sort:text-muted-foreground'"
    />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown, ArrowUp, ChevronsUpDown } from '@lucide/vue'

const props = withDefaults(
  defineProps<{ label: string; active?: boolean; direction?: 'asc' | 'desc' }>(),
  { direction: 'asc' }
)
const emit = defineEmits<{ sort: [] }>()

const icon = computed(() => (props.active ? (props.direction === 'asc' ? ArrowUp : ArrowDown) : ChevronsUpDown))
</script>
