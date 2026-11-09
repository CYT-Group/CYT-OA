<template>
  <span
    :class="
      cn(
        'inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full font-semibold ring-1',
        src ? 'bg-muted ring-border' : TONES[tone],
        size === 'sm' ? 'size-7 text-[11px]' : size === 'lg' ? 'size-12 text-base' : 'size-9 text-xs',
        props.class
      )
    "
    :title="name ?? undefined"
  >
    <img v-if="src" :src="src" :alt="name ?? ''" class="size-full object-cover" />
    <span v-else>{{ initials }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '../lib/utils'

const props = withDefaults(
  defineProps<{ name?: string | null; src?: string | null; size?: 'sm' | 'md' | 'lg'; class?: string }>(),
  { size: 'md' }
)

/** 姓名散列到固定色板，同一个人在任何列表里颜色一致 */
const TONES = [
  'bg-sky-50 text-sky-700 ring-sky-600/15 dark:bg-sky-400/10 dark:text-sky-300 dark:ring-sky-300/15',
  'bg-violet-50 text-violet-700 ring-violet-600/15 dark:bg-violet-400/10 dark:text-violet-300 dark:ring-violet-300/15',
  'bg-emerald-50 text-emerald-700 ring-emerald-600/15 dark:bg-emerald-400/10 dark:text-emerald-300 dark:ring-emerald-300/15',
  'bg-amber-50 text-amber-700 ring-amber-600/15 dark:bg-amber-400/10 dark:text-amber-300 dark:ring-amber-300/15',
  'bg-rose-50 text-rose-700 ring-rose-600/15 dark:bg-rose-400/10 dark:text-rose-300 dark:ring-rose-300/15',
  'bg-cyan-50 text-cyan-700 ring-cyan-600/15 dark:bg-cyan-400/10 dark:text-cyan-300 dark:ring-cyan-300/15',
  'bg-fuchsia-50 text-fuchsia-700 ring-fuchsia-600/15 dark:bg-fuchsia-400/10 dark:text-fuchsia-300 dark:ring-fuchsia-300/15',
  'bg-indigo-50 text-indigo-700 ring-indigo-600/15 dark:bg-indigo-400/10 dark:text-indigo-300 dark:ring-indigo-300/15'
] as const

const initials = computed(() => {
  const text = (props.name ?? '').trim()
  if (!text) return '?'
  return text.slice(0, 1).toUpperCase()
})

const tone = computed(() => {
  const text = props.name ?? ''
  let hash = 0
  for (const ch of text) hash = (hash * 31 + ch.codePointAt(0)!) >>> 0
  return hash % TONES.length
})
</script>
