<template>
  <DropdownMenuItem
    :disabled="disabled"
    data-slot="dropdown-item"
    :data-destructive="destructive || undefined"
    :class="
      cn(
        'ui-menu-item relative flex min-h-9 cursor-pointer select-none items-center gap-2.5 rounded-lg px-2.5 py-2 text-[13px] leading-5 outline-none transition-colors [&_svg]:size-4 [&_svg]:shrink-0 [&_svg]:stroke-[1.7]',
        'data-[highlighted]:bg-accent data-[highlighted]:text-accent-foreground',
        'data-[disabled]:pointer-events-none data-[disabled]:opacity-50',
        destructive &&
          'text-destructive data-[highlighted]:bg-destructive/10 data-[highlighted]:text-destructive',
        props.class,
      )
    "
    @select="emit('select', $event)"
  >
    <slot />
  </DropdownMenuItem>
</template>

<script setup lang="ts">
import { DropdownMenuItem } from "reka-ui";
import { cn } from "../lib/utils";

const props = defineProps<{
  disabled?: boolean;
  destructive?: boolean;
  class?: string;
}>();
const emit = defineEmits<{ select: [event: Event] }>();
</script>
