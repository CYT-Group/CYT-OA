<template>
  <PortalEditorFrame :mode="view">
    <component :is="page" :key="pageKey" />
  </PortalEditorFrame>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import PortalEditorFrame from './PortalEditorFrame.vue'
import HrDocEditorView from './legacy/views/admin/HrDocEditorView.vue'
import HrDocPreviewView from './legacy/views/admin/HrDocPreviewView.vue'
import HrDocAdminView from './legacy/views/admin/HrDocAdminView.vue'
const props = withDefaults(defineProps<{ view?: 'list' | 'editor' | 'preview' }>(), { view: 'editor' })
const route = useRoute()
const pageKey = ref(route.path)
watch(() => route.path, (path, previous) => {
  // 新建保存后由编辑器自己加载 ID 文档；保留实例才能在块保存失败时保住本地正文。
  if (props.view === 'editor' && previous === '/portal/hr-documents/new'
    && /^\/portal\/hr-documents\/id\/\d+\/edit$/.test(path)) return
  pageKey.value = path
}, { flush: 'sync' })
const page = computed(() => props.view === 'list' ? HrDocAdminView : props.view === 'preview' ? HrDocPreviewView : HrDocEditorView)
</script>
