<template>
  <DropdownMenu v-if="visible.length">
    <template #trigger>
      <Button
        variant="ghost"
        size="icon-sm"
        :disabled="disabled"
        :aria-label="label"
        :title="label"
      >
        <Ellipsis class="size-4" />
      </Button>
    </template>
    <DropdownMenuItem
      v-for="action in visible"
      :key="action.label"
      :destructive="action.destructive"
      :disabled="disabled || action.disabled"
      @select="action.onSelect()"
    >
      {{ action.label }}
    </DropdownMenuItem>
  </DropdownMenu>
</template>
<script setup lang="ts">
import { computed } from "vue";
import { Ellipsis } from "@lucide/vue";
import Button from "../../ui/Button.vue";
import DropdownMenu from "../../ui/DropdownMenu.vue";
import DropdownMenuItem from "../../ui/DropdownMenuItem.vue";
interface PortalRowAction {
  label: string;
  destructive?: boolean;
  disabled?: boolean;
  onSelect: () => unknown;
}
const props = withDefaults(
  defineProps<{
    actions: (PortalRowAction | false | null | undefined)[];
    disabled?: boolean;
    label?: string;
  }>(),
  { label: "更多操作" },
);
const visible = computed(() =>
  props.actions.filter((action): action is PortalRowAction => Boolean(action)),
);
</script>
