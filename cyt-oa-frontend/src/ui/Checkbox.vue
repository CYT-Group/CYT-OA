<template>
  <label class="inline-flex cursor-pointer select-none items-center gap-2" :class="{ 'cursor-not-allowed opacity-50': disabled }">
    <CheckboxRoot
      :model-value="modelValue"
      :disabled="disabled"
      :class="
        cn(
          'peer flex size-4 shrink-0 items-center justify-center rounded-[4px] border border-border-strong bg-card text-primary-foreground transition-colors',
          'hover:border-muted-foreground/45',
          'data-[state=checked]:border-primary data-[state=checked]:bg-primary',
          'data-[state=indeterminate]:border-primary data-[state=indeterminate]:bg-primary',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/35 focus-visible:ring-offset-1 focus-visible:ring-offset-background'
        )
      "
      @update:model-value="emit('update:modelValue', $event === true)"
    >
      <CheckboxIndicator class="flex items-center justify-center">
        <Minus v-if="modelValue === 'indeterminate'" class="size-3" :stroke-width="3" />
        <Check v-else class="size-3" :stroke-width="3" />
      </CheckboxIndicator>
    </CheckboxRoot>
    <span v-if="$slots.default" class="text-sm"><slot /></span>
  </label>
</template>

<script setup lang="ts">
import { CheckboxIndicator, CheckboxRoot } from 'reka-ui'
import { Check, Minus } from '@lucide/vue'
import { cn } from '../lib/utils'

defineProps<{ modelValue?: boolean | 'indeterminate'; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
</script>
