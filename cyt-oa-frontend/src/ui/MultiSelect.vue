<template>
  <PopoverRoot v-model:open="open">
    <PopoverTrigger as-child>
      <button
        v-bind="$attrs"
        type="button"
        :class="
          cn(
            'flex h-9 w-full items-center justify-between gap-2 rounded-lg border border-input bg-card px-3 text-left text-sm shadow-xs transition-colors',
            'focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40',
            'disabled:cursor-not-allowed disabled:opacity-50',
            props.class
          )
        "
        :disabled="disabled"
      >
        <span :class="cn('truncate', !summary && 'text-muted-foreground/70')">{{
          summary || placeholder
        }}</span>
        <ChevronDown class="size-4 shrink-0 text-muted-foreground" />
      </button>
    </PopoverTrigger>
    <PopoverPortal>
      <PopoverContent
        align="start"
        :side-offset="4"
        class="z-[70] w-[var(--reka-popover-trigger-width)] min-w-56 rounded-xl border bg-popover p-2 text-popover-foreground shadow-lg data-[state=open]:animate-fade-in"
      >
        <div v-if="options.length > 8" class="relative mb-2">
          <Search
            class="pointer-events-none absolute left-2 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground"
          />
          <input
            v-model="keyword"
            class="h-8 w-full rounded-md border border-input bg-background pl-7 pr-2 text-xs outline-none focus:border-ring"
            placeholder="搜索选项"
          />
        </div>
        <div class="max-h-60 space-y-0.5 overflow-y-auto">
          <div
            v-for="opt in filtered"
            :key="String(opt.value)"
            role="button"
            tabindex="0"
            class="flex w-full cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm hover:bg-accent"
            @click="toggle(opt.value)"
            @keydown.enter.prevent="toggle(opt.value)"
            @keydown.space.prevent="toggle(opt.value)"
          >
            <Checkbox :model-value="checked(opt.value)" @click.stop @update:model-value="toggle(opt.value)" />
            <span class="truncate">{{ opt.label }}</span>
          </div>
          <p v-if="!filtered.length" class="px-2 py-6 text-center text-xs text-muted-foreground">
            没有匹配项
          </p>
        </div>
        <div class="mt-2 flex items-center justify-between border-t pt-2">
          <span class="text-xs text-muted-foreground">已选 {{ modelValue.length }} 项</span>
          <Button size="sm" variant="ghost" type="button" @click="emit('update:modelValue', [])">清空</Button>
        </div>
      </PopoverContent>
    </PopoverPortal>
  </PopoverRoot>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { PopoverContent, PopoverPortal, PopoverRoot, PopoverTrigger } from 'reka-ui'
import { ChevronDown, Search } from '@lucide/vue'
import { cn } from '../lib/utils'
import Checkbox from './Checkbox.vue'
import Button from './Button.vue'
import type { SelectOption, SelectValueType } from './types'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    modelValue: SelectValueType[]
    options: SelectOption[]
    placeholder?: string
    disabled?: boolean
    class?: string
  }>(),
  { placeholder: '请选择' }
)

const emit = defineEmits<{ 'update:modelValue': [value: SelectValueType[]] }>()
const open = ref(false)
const keyword = ref('')

const filtered = computed(() => {
  const kw = keyword.value.trim()
  return kw ? props.options.filter((o) => o.label.includes(kw)) : props.options
})

const summary = computed(() => {
  const labels = props.options
    .filter((o) => props.modelValue.some((v) => String(v) === String(o.value)))
    .map((o) => o.label)
  return labels.length > 2 ? `${labels.slice(0, 2).join('、')} 等 ${labels.length} 人` : labels.join('、')
})

function checked(value: SelectValueType) {
  return props.modelValue.some((v) => String(v) === String(value))
}

function toggle(value: SelectValueType) {
  if (props.disabled) return
  emit(
    'update:modelValue',
    checked(value)
      ? props.modelValue.filter((v) => String(v) !== String(value))
      : [...props.modelValue, value]
  )
}
</script>
