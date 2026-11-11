<template>
  <DialogRoot :open="open" @update:open="emit('update:open', $event)">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-[80] bg-black/40 data-[state=open]:animate-fade-in" />
      <DialogContent
        class="fixed left-1/2 top-[18vh] z-[80] w-[calc(100vw-2rem)] max-w-lg -translate-x-1/2 overflow-hidden rounded-2xl border bg-popover text-popover-foreground shadow-2xl focus:outline-none data-[state=open]:animate-fade-in"
        @open-auto-focus.prevent
      >
        <VisuallyHidden
          ><DialogTitle>快速跳转</DialogTitle
          ><DialogDescription>搜索页面名称，使用方向键选择并按回车跳转。</DialogDescription></VisuallyHidden
        >
        <div class="flex items-center gap-2 border-b px-4">
          <Search class="size-4 shrink-0 text-muted-foreground" />
          <input
            ref="inputEl"
            v-model="keyword"
            class="h-12 w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground/70"
            placeholder="搜索页面，回车跳转"
            aria-label="搜索页面"
            @keydown.down.prevent="move(1)"
            @keydown.up.prevent="move(-1)"
            @keydown.enter.prevent="choose(filtered[active])"
          />
          <kbd class="rounded border bg-muted px-1.5 py-0.5 text-[10px] text-muted-foreground">esc</kbd>
        </div>
        <div class="max-h-[min(60vh,380px)] overflow-y-auto p-2">
          <button
            v-for="(entry, index) in filtered"
            :key="entry.to + entry.title"
            type="button"
            :class="
              cn(
                'flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm transition-colors',
                index === active ? 'bg-accent text-accent-foreground' : 'text-foreground'
              )
            "
            @mouseenter="active = index"
            @click="choose(entry)"
          >
            <NavIcon :name="entry.icon" :glyph="entry.glyph ?? ''" />
            <span class="flex-1 truncate">{{ entry.title }}</span>
            <span class="shrink-0 text-[11px] text-muted-foreground">{{ entry.group }}</span>
          </button>
          <p v-if="!filtered.length" class="px-3 py-8 text-center text-xs text-muted-foreground">
            没有匹配的页面
          </p>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import {
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
  VisuallyHidden
} from 'reka-ui'
import { Search } from '@lucide/vue'
import type { NavZone } from '../configs/nav'
import NavIcon from './NavIcon.vue'
import { cn } from '../lib/utils'

interface Entry {
  title: string
  to: string
  group: string
  icon: string
  glyph?: string
}

const props = defineProps<{ open: boolean; zones: NavZone[] }>()
const emit = defineEmits<{ 'update:open': [value: boolean]; navigate: [path: string] }>()

const keyword = ref('')
const active = ref(0)
const inputEl = ref<HTMLInputElement | null>(null)

const entries = computed<Entry[]>(() =>
  props.zones.flatMap((zone) => {
    const list: Entry[] = []
    for (const item of zone.items) {
      if (!item.children?.length) {
        list.push({ title: item.title, to: item.to, group: zone.title, icon: item.icon, glyph: item.glyph })
      } else {
        for (const child of item.children) {
          list.push({
            title: child.title,
            to: child.to,
            group: `${zone.title} · ${item.title}`,
            icon: child.icon || item.icon,
            glyph: child.glyph
          })
        }
      }
    }
    return list
  })
)

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const list = kw
    ? entries.value.filter((e) => `${e.title}${e.group}`.toLowerCase().includes(kw))
    : entries.value
  return list.slice(0, 40)
})

watch(
  () => props.open,
  (open) => {
    if (!open) return
    keyword.value = ''
    active.value = 0
    void nextTick(() => inputEl.value?.focus())
  }
)

watch(filtered, () => (active.value = 0))

function move(step: number) {
  if (!filtered.value.length) return
  active.value = (active.value + step + filtered.value.length) % filtered.value.length
}

function choose(entry?: Entry) {
  if (!entry) return
  emit('update:open', false)
  emit('navigate', entry.to)
}
</script>
