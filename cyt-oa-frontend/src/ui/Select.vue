<template>
  <SelectRoot :model-value="inner" :disabled="disabled" @update:model-value="onSelect">
    <SelectTrigger
      v-bind="$attrs"
      :aria-label="ariaLabel"
      :class="
        cn(
          'flex w-full items-center justify-between gap-2 rounded-lg border border-input bg-card px-3 text-left text-sm transition-colors',
          'hover:border-border-strong',
          'focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30',
          '[&_[data-placeholder]]:text-muted-foreground/65 disabled:cursor-not-allowed disabled:opacity-50',
          size === 'sm' ? 'h-8 text-xs' : 'h-9',
          props.class
        )
      "
    >
      <SelectValue v-if="selectedLabel" :placeholder="placeholder">
        <span class="truncate">{{ selectedLabel }}</span>
      </SelectValue>
      <SelectValue v-else :placeholder="placeholder" />
      <SelectIcon class="text-muted-foreground">
        <ChevronDown class="size-4" />
      </SelectIcon>
    </SelectTrigger>
    <SelectPortal>
      <SelectContent
        position="popper"
        :side-offset="4"
        class="z-[70] max-h-72 min-w-[var(--reka-select-trigger-width)] overflow-hidden rounded-lg border border-border bg-popover text-popover-foreground shadow-xl data-[state=open]:animate-fade-in"
      >
        <SelectViewport class="p-1">
          <SelectItem
            v-for="opt in normalized"
            :key="opt.key"
            :value="opt.key"
            :disabled="opt.disabled"
            class="relative flex cursor-pointer select-none items-center rounded-md py-1.5 pl-8 pr-2 text-sm outline-none data-[disabled]:pointer-events-none data-[highlighted]:bg-accent data-[highlighted]:text-accent-foreground data-[disabled]:opacity-50"
          >
            <SelectItemIndicator class="absolute left-2 inline-flex items-center">
              <Check class="size-4" />
            </SelectItemIndicator>
            <SelectItemText>{{ opt.label }}</SelectItemText>
          </SelectItem>
          <p v-if="!normalized.length" class="px-3 py-6 text-center text-xs text-muted-foreground">
            暂无选项
          </p>
        </SelectViewport>
      </SelectContent>
    </SelectPortal>
  </SelectRoot>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  SelectContent,
  SelectIcon,
  SelectItem,
  SelectItemIndicator,
  SelectItemText,
  SelectPortal,
  SelectRoot,
  SelectTrigger,
  SelectValue,
  SelectViewport
} from 'reka-ui'
import { Check, ChevronDown } from '@lucide/vue'
import { cn } from '../lib/utils'
import type { RawSelectOption, SelectOption, SelectValueType } from './types'

defineOptions({ inheritAttrs: false })

const EMPTY = '__cyt_empty__'

const props = withDefaults(
  defineProps<{
    modelValue?: SelectValueType
    options: RawSelectOption[]
    placeholder?: string
    ariaLabel?: string
    disabled?: boolean
    size?: 'sm' | 'md'
    class?: string
  }>(),
  { placeholder: '请选择', size: 'md' }
)

const emit = defineEmits<{ 'update:modelValue': [value: SelectValueType] }>()

const normalized = computed(() =>
  props.options.map((raw, index) => {
    const opt = Array.isArray(raw)
      ? { label: raw[1], value: raw[0] as SelectValueType, disabled: false }
      : (raw as SelectOption)
    const value = opt.value
    return {
      key: value === null || value === undefined || value === '' ? `${EMPTY}${index}` : String(value),
      value,
      label: opt.label ?? '',
      disabled: opt.disabled
    }
  })
)

const inner = computed(() => {
  const hit = normalized.value.find((o) => o.value === props.modelValue)
  return hit ? hit.key : undefined
})

const selectedLabel = computed(() => normalized.value.find((o) => o.key === inner.value)?.label ?? '')

function onSelect(key: unknown) {
  const hit = normalized.value.find((o) => o.key === String(key))
  emit('update:modelValue', hit ? hit.value : null)
}
</script>
