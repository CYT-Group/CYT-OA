<template>
  <DialogRoot :open="open" @update:open="emit('update:open', $event)">
    <DialogPortal>
      <DialogOverlay
        class="ui-dialog-overlay fixed inset-0 z-[60] data-[state=open]:animate-fade-in"
      />
      <DialogContent
        :class="
          cn(
            'fixed left-1/2 top-1/2 z-[60] flex max-h-[calc(100dvh-2rem)] w-[calc(100vw-2rem)] -translate-x-1/2 -translate-y-1/2 flex-col rounded-xl border bg-card text-card-foreground shadow-2xl',
            'data-[state=open]:animate-scale-in focus:outline-none',
            size === 'sm'
              ? 'max-w-md'
              : size === 'lg'
                ? 'max-w-3xl'
                : size === 'xl'
                  ? 'max-w-5xl'
                  : 'max-w-xl',
            props.class
          )
        "
      >
        <header class="flex items-start justify-between gap-4 border-b px-5 py-3.5">
          <div class="min-w-0">
            <DialogTitle class="text-base font-semibold tracking-tight">
              {{ title }}
            </DialogTitle>
            <DialogDescription v-if="description" class="mt-1 text-xs text-muted-foreground">
              {{ description }}
            </DialogDescription>
            <VisuallyHidden v-else
              ><DialogDescription>{{ title }}</DialogDescription></VisuallyHidden
            >
          </div>
          <DialogClose
            class="-mr-1 -mt-0.5 inline-flex size-8 shrink-0 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-accent hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
            aria-label="关闭"
          >
            <X class="size-4" />
          </DialogClose>
        </header>
        <div class="min-h-0 flex-1 overflow-y-auto px-5 py-4">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="flex items-center justify-end gap-2 border-t px-5 py-3">
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
  DialogTitle,
  VisuallyHidden
} from 'reka-ui'
import { X } from '@lucide/vue'
import { cn } from '../lib/utils'

const props = withDefaults(
  defineProps<{
    open: boolean
    title: string
    description?: string
    size?: 'sm' | 'md' | 'lg' | 'xl'
    class?: string
  }>(),
  { size: 'md' }
)

const emit = defineEmits<{ 'update:open': [value: boolean] }>()
</script>
