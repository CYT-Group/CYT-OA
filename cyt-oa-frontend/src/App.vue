<template>
  <router-view />
  <Toaster
    position="top-center"
    :theme="colorMode"
    rich-colors
    close-button
    :duration="2800"
    :toast-options="{ class: 'rounded-xl border-border shadow-lg' }"
  />
</template>

<script setup lang="ts">
import { Toaster } from 'vue-sonner'
import 'vue-sonner/style.css'
import { colorMode } from './lib/theme'
</script>
