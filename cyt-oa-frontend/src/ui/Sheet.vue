<template>
  <DialogRoot :open="open" @update:open="emit('update:open', $event)">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-[60] bg-black/45 backdrop-blur-[2px] data-[state=open]:animate-fade-in dark:bg-black/70" />
      <DialogContent
        :class="
          cn(
            'fixed inset-y-0 z-[60] flex w-[90vw] max-w-md flex-col shadow-2xl focus:outline-none',
            tone === 'sidebar'
              ? 'border-sidebar-border bg-sidebar text-sidebar-foreground'
              : 'border bg-card text-card-foreground',
            side === 'left'
              ? 'left-0 border-r data-[state=open]:animate-slide-in-left'
              : 'right-0 border-l data-[state=open]:animate-slide-in-right',
            props.class
          )
        "
      >
        <header
          :class="
            cn(
              'flex items-center justify-between gap-3 border-b px-4 py-3',
              tone === 'sidebar' ? 'border-sidebar-border' : 'border-border'
            )
          "
        >
          <div class="min-w-0">
            <DialogTitle class="text-sm font-semibold">{{ title }}</DialogTitle>
            <DialogDescription
              v-if="description"
              :class="cn('mt-0.5 text-xs', tone === 'sidebar' ? 'text-sidebar-muted' : 'text-muted-foreground')"
            >
              {{ description }}
            </DialogDescription>
          </div>
          <DialogClose
            :class="
              cn(
                'inline-flex size-8 items-center justify-center rounded-lg transition-colors',
                tone === 'sidebar'
                  ? 'text-sidebar-muted hover:bg-sidebar-accent hover:text-sidebar-foreground'
                  : 'text-muted-foreground hover:bg-accent hover:text-foreground'
              )
            "
            aria-label="关闭"
          >
            <X class="size-4" />
          </DialogClose>
        </header>
        <div class="min-h-0 flex-1 overflow-y-auto">
          <slot />
        </div>
        <footer
          v-if="$slots.footer"
          :class="
            cn(
              'flex shrink-0 items-center justify-end gap-2 border-t px-4 py-3',
              tone === 'sidebar' ? 'border-sidebar-border' : 'border-border'
            )
          "
        >
          <slot name="footer" />
        </footer>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>

<script setup lang="ts">
import {
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle
} from 'reka-ui'
import { X } from '@lucide/vue'
import { cn } from '../lib/utils'

const props = withDefaults(
  defineProps<{
    open: boolean
    title: string
    description?: string
    side?: 'left' | 'right'
    tone?: 'default' | 'sidebar'
    class?: string
  }>(),
  { side: 'left', tone: 'default' }
)

const emit = defineEmits<{ 'update:open': [value: boolean] }>()
</script>
