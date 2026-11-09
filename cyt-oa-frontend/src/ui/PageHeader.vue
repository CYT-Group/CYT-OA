<template>
  <div class="page-header flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
    <div class="min-w-0">
      <p v-if="eyebrow" class="eyebrow">
        {{ eyebrow }}
      </p>
      <div class="flex flex-wrap items-center gap-2.5">
        <h1 class="text-[22px] font-semibold tracking-tight sm:text-2xl">{{ title }}</h1>
        <slot name="badge" />
      </div>
      <p
        v-if="description || $slots.description"
        class="mt-1.5 max-w-2xl text-sm leading-relaxed text-muted-foreground"
      >
        <slot name="description">{{ description }}</slot>
      </p>
    </div>
    <div v-if="$slots.actions" class="flex flex-wrap items-center gap-2">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; description?: string; eyebrow?: string }>()
</script>
