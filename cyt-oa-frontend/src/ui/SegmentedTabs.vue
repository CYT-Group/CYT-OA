<template>
  <div
    :class="
      cn(
        'inline-flex max-w-full items-center gap-0.5 overflow-x-auto rounded-lg bg-muted p-0.5',
        props.class,
      )
    "
  >
    <button
      v-for="item in items"
      :key="item.value"
      type="button"
      :aria-pressed="modelValue === item.value"
      :class="
        cn(
          'inline-flex shrink-0 items-center gap-1.5 rounded-md px-2.5 py-1.5 text-xs font-medium transition-[color,background-color,box-shadow] duration-150',
          modelValue === item.value
            ? 'bg-card text-foreground shadow-sm ring-1 ring-border/70'
            : 'text-muted-foreground hover:text-foreground',
        )
      "
      @click="emit('update:modelValue', item.value)"
    >
      {{ item.label }}
      <span
        v-if="item.count !== undefined"
        :class="
          cn(
            'rounded px-1 text-[10px] tabular-nums',
            modelValue === item.value
              ? 'bg-primary/10 text-primary'
              : 'bg-background/60 text-muted-foreground',
          )
        "
      >
        {{ item.count > 99 ? "99+" : item.count }}
      </span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { cn } from "../lib/utils";

export interface TabItem {
  value: string;
  label: string;
  count?: number;
}

const props = defineProps<{
  modelValue: string;
  items: TabItem[];
  class?: string;
}>();
const emit = defineEmits<{ "update:modelValue": [value: string] }>();
</script>
