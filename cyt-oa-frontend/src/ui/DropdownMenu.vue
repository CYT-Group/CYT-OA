<template>
  <DropdownMenuRoot>
    <DropdownMenuTrigger as-child>
      <slot name="trigger" />
    </DropdownMenuTrigger>
    <DropdownMenuPortal>
      <DropdownMenuContent
        :align="align"
        :side="side"
        :side-offset="6"
        data-slot="dropdown-content"
        :class="
          cn(
            'ui-menu z-[70] min-w-48 overflow-hidden rounded-xl border border-border bg-popover p-1.5 text-popover-foreground data-[state=open]:animate-fade-in',
            props.class,
          )
        "
      >
        <slot />
      </DropdownMenuContent>
    </DropdownMenuPortal>
  </DropdownMenuRoot>
</template>

<script setup lang="ts">
import {
  DropdownMenuContent,
  DropdownMenuPortal,
  DropdownMenuRoot,
  DropdownMenuTrigger,
} from "reka-ui";
import { cn } from "../lib/utils";

const props = withDefaults(
  defineProps<{
    align?: "start" | "center" | "end";
    side?: "top" | "right" | "bottom" | "left";
    class?: string;
  }>(),
  {
    align: "end",
    side: "bottom",
  },
);
</script>
