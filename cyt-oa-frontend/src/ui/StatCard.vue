<template>
  <div
    :class="
      cn(
        'panel group relative overflow-hidden p-[18px] transition-colors',
        props.class,
      )
    "
    :title="hint"
  >
    <span
      v-if="icon || glyph"
      :class="
        cn('mb-2.5 grid size-[34px] place-items-center rounded-lg', toneChip)
      "
      aria-hidden="true"
    >
      <span v-if="glyph" class="text-lg">{{ glyph }}</span>
      <component
        v-else-if="icon"
        :is="icon"
        class="size-[17px]"
        :stroke-width="1.7"
      />
    </span>
    <p class="text-2xl font-bold leading-[1.6] tabular-nums text-foreground">
      {{ value }}
    </p>
    <p class="truncate text-xs leading-[1.6] text-muted-foreground">
      {{ title }}
    </p>
    <p v-if="hint" class="sr-only">
      {{ hint }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, type Component } from "vue";
import { cn } from "../lib/utils";

const props = withDefaults(
  defineProps<{
    title: string;
    value: string | number;
    hint?: string;
    icon?: Component;
    glyph?: string;
    tone?: "primary" | "info" | "success" | "warning" | "destructive";
    class?: string;
  }>(),
  { tone: "primary" },
);

const toneChip = computed(
  () =>
    ({
      primary: "bg-primary/10 text-primary ring-primary/15 dark:bg-primary/12",
      info: "bg-info-soft text-info ring-info/15 dark:bg-info-soft",
      success:
        "bg-success-soft text-success ring-success/15 dark:bg-success-soft",
      warning:
        "bg-warning-soft text-warning ring-warning/20 dark:bg-warning-soft",
      destructive: "bg-destructive/10 text-destructive ring-destructive/15",
    })[props.tone],
);
</script>
