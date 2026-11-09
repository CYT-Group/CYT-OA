<template>
  <textarea
    :value="modelValue ?? ''"
    :rows="rows"
    :class="
      cn(
        'flex w-full resize-y rounded-lg border border-input bg-card px-3 py-2 text-sm text-foreground transition-colors',
        'hover:border-border-strong',
        'placeholder:text-muted-foreground/65',
        'focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30',
        'disabled:cursor-not-allowed disabled:opacity-50',
        props.class
      )
    "
    @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
  />
</template>

<script setup lang="ts">
import { cn } from '../lib/utils'

const props = withDefaults(
  defineProps<{
    modelValue?: string | null
    rows?: number
    class?: string
  }>(),
  { rows: 4 }
)

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
</script>
