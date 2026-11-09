<template>
  <button :type="type" :class="cn(buttonVariants({ variant, size }), props.class)" :disabled="disabled || loading">
    <LoaderCircle v-if="loading" class="size-4 animate-spin" />
    <slot />
  </button>
</template>

<script setup lang="ts">
import { cva, type VariantProps } from 'class-variance-authority'
import { LoaderCircle } from '@lucide/vue'
import { cn } from '../lib/utils'

const buttonVariants = cva(
  'inline-flex shrink-0 select-none items-center justify-center gap-1.5 whitespace-nowrap rounded-lg font-medium transition-[filter,color,background-color,border-color,box-shadow,transform] duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/45 focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:pointer-events-none disabled:opacity-45 [&_svg]:size-4 [&_svg]:shrink-0',
  {
    variants: {
      variant: {
        default: 'btn-solid-primary text-primary-foreground disabled:bg-primary disabled:bg-none',
        secondary:
          'bg-secondary text-secondary-foreground shadow-xs hover:bg-secondary/70 active:scale-[0.985] disabled:shadow-none',
        outline:
          'border border-border bg-card text-foreground shadow-xs hover:border-border-strong hover:bg-accent hover:text-accent-foreground active:scale-[0.985]',
        ghost: 'text-muted-foreground hover:bg-accent hover:text-foreground',
        destructive: 'btn-solid-danger text-destructive-foreground',
        soft: 'bg-primary/10 text-primary hover:bg-primary/15',
        link: 'text-primary underline-offset-4 hover:underline'
      },
      size: {
        sm: 'h-8 px-3 text-xs',
        md: 'h-9 px-3.5 text-sm',
        lg: 'h-10 px-5 text-sm',
        icon: 'size-9 p-0',
        'icon-sm': 'size-8 p-0'
      }
    },
    defaultVariants: { variant: 'default', size: 'md' }
  }
)

type Variants = VariantProps<typeof buttonVariants>

const props = withDefaults(
  defineProps<{
    variant?: Variants['variant']
    size?: Variants['size']
    type?: 'button' | 'submit' | 'reset'
    disabled?: boolean
    loading?: boolean
    class?: string
  }>(),
  { variant: 'default', size: 'md', type: 'button' }
)
</script>
