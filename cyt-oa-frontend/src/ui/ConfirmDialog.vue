<template>
  <Dialog :open="open" :title="title" size="sm" @update:open="emit('update:open', $event)">
    <p class="text-sm leading-relaxed text-muted-foreground">{{ description }}</p>
    <template #footer>
      <Button variant="outline" :disabled="busy" @click="emit('update:open', false)">{{ cancelText }}</Button>
      <Button :variant="tone === 'destructive' ? 'destructive' : 'default'" :loading="busy" @click="emit('confirm')">
        {{ confirmText }}
      </Button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import Dialog from './Dialog.vue'
import Button from './Button.vue'

withDefaults(
  defineProps<{
    open: boolean
    title: string
    description: string
    confirmText?: string
    cancelText?: string
    tone?: 'default' | 'destructive'
    busy?: boolean
  }>(),
  { confirmText: '确认', cancelText: '取消', tone: 'default' }
)

const emit = defineEmits<{ 'update:open': [value: boolean]; confirm: [] }>()
</script>
