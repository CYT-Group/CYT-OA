<template>
  <div class="portal-editor-frame portal-editor-scope" :class="`portal-editor-frame--${mode}`"><slot /></div>
</template>
<script setup lang="ts">
import './styles/portal-editor.css'
import './styles/oa-workspace.css'
import '@fontsource/outfit/300.css'
import '@fontsource/outfit/400.css'
import '@fontsource/outfit/500.css'
import '@fontsource/outfit/600.css'
import '@fontsource/outfit/700.css'
import '@fontsource/noto-sans-sc/300.css'
import '@fontsource/noto-sans-sc/400.css'
import '@fontsource/noto-sans-sc/500.css'
import '@fontsource/noto-sans-sc/700.css'
import '@fontsource/jetbrains-mono/400.css'
import '@fontsource/jetbrains-mono/500.css'
import '@fontsource/syne/600.css'
import '@fontsource/syne/700.css'
import '@fontsource/syne/800.css'
import '@fontsource/dm-mono/300.css'
import '@fontsource/dm-mono/400.css'
withDefaults(defineProps<{ mode?: 'list' | 'editor' | 'preview' }>(), { mode: 'editor' })
</script>
