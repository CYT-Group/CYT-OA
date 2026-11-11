<template>
  <div
    class="portal-editor-frame portal-editor-scope portal-rte-embedded"
    :aria-label="label"
  >
    <HrHtmlContent
      v-if="readonly"
      :html="modelValue"
      class="rounded-lg border bg-card p-4 text-sm"
    />
    <RichTextEditor
      v-else
      ref="richEditor"
      :model-value="modelValue"
      :placeholder="label"
      modern-toolbar-selects
      @update:model-value="emit('update:modelValue', $event)"
    />
  </div>
</template>
<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from "vue";
import RichTextEditor from "../../pages/portal/editor/legacy/components/ui/RichTextEditor.vue";
import HrHtmlContent from "./HrHtmlContent.vue";
import { registerPortalRichText } from "../../lib/portal-richtext";
import "../../pages/portal/editor/styles/portal-editor.css";
import "../../pages/portal/editor/styles/oa-workspace.css";
withDefaults(
  defineProps<{ modelValue?: string; label?: string; readonly?: boolean }>(),
  {
    modelValue: "",
    label: "正文",
    readonly: false,
  },
);
const emit = defineEmits<{ "update:modelValue": [value: string] }>();
const richEditor = ref<{ flushPendingImages: () => Promise<void> } | null>(
  null,
);
async function flushPendingImages() {
  await richEditor.value?.flushPendingImages();
}
let unregister: (() => void) | undefined;
onMounted(() => {
  unregister = registerPortalRichText(flushPendingImages);
});
onBeforeUnmount(() => unregister?.());
defineExpose({ flushPendingImages });
</script>
<style scoped>
.portal-rte-embedded {
  min-width: 0;
  background: transparent;
}
.portal-rte-embedded :deep(.rte) {
  border-color: var(--border);
  border-radius: 8px;
  background: var(--card);
}
.portal-rte-embedded :deep(.rte-content) {
  min-height: 180px;
  max-height: 480px;
  padding: 16px;
  font-family: var(--font-sans);
  color: var(--foreground);
  background: var(--card);
}
.portal-rte-embedded :deep(.rte-toolbar) {
  border-color: var(--border);
  background: var(--muted);
}
.portal-rte-embedded :deep(.rte-toolbar button) {
  color: var(--foreground);
}
.portal-rte-embedded :deep(.rte-toolbar button.active) {
  background: var(--primary);
  color: var(--primary-foreground);
}
.portal-rte-embedded :deep(.rte-urlbar) {
  background: var(--muted);
}
</style>
