<template>
  <input
    :value="modelValue ?? ''"
    :type="type"
    :class="
      cn(
        'flex h-9 w-full rounded-lg border border-input bg-card px-3 py-1 text-sm text-foreground transition-colors',
        'hover:border-border-strong',
        'placeholder:text-muted-foreground/65',
        'focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30',
        'disabled:cursor-not-allowed disabled:opacity-50',
        'file:mr-2 file:rounded-md file:border-0 file:bg-muted file:px-2 file:py-1 file:text-xs file:font-medium',
        props.class
      )
    "
    @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
  />
</template>

<script setup lang="ts">
import { cn } from '../lib/utils'

const props = withDefaults(
  defineProps<{
    modelValue?: string | number | null
    type?: string
    class?: string
  }>(),
  { type: 'text' }
)

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
</script>
