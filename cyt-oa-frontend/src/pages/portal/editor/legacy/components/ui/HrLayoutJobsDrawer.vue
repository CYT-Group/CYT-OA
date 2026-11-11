<template>
  <PortalLayoutJobsDialog
    :open="open"
    :jobs="jobs"
    :theme="theme"
    @update:open="$emit('update:open', $event)"
    @apply="$emit('apply', $event)"
    @retry="$emit('retry', $event)"
    @remove="$emit('remove', $event)"
  />
</template>
<script setup>
import PortalLayoutJobsDialog from "../../../components/PortalLayoutJobsDialog.vue";
defineProps({
  open: { type: Boolean, default: false },
  jobs: { type: Array, default: () => [] },
  theme: { type: String, default: "" },
});
defineEmits(["update:open", "apply", "retry", "remove"]);
</script>
