<template>
  <div class="hr-block-preview space-y-5 text-sm">
    <template v-for="(b, i) in blocks" :key="i">
      <h2 v-if="b.type === 'section'" class="border-b pb-2 text-xl font-semibold">{{ b.title }}</h2>
      <figure v-else-if="b.type === 'image'"><img v-if="hrSafeUrl(b.image)" :src="hrSafeUrl(b.image)" :alt="b.caption ?? ''" class="mx-auto max-w-full rounded-lg" /><figcaption class="mt-2 text-center text-xs text-muted-foreground">{{ b.caption }}</figcaption></figure>
      <div v-else-if="b.type === 'imageText'" class="grid gap-4 sm:grid-cols-2"><img v-if="hrSafeUrl(b.image)" :src="hrSafeUrl(b.image)" :alt="b.title ?? ''" class="rounded-lg" /><div><h3 class="mb-2 font-semibold">{{ b.title }}</h3><HrHtmlContent :html="b.text" /></div></div>
      <div v-else-if="['text', 'rawhtml'].includes(b.type)"><HrHtmlContent :html="b.type === 'rawhtml' ? b.html : b.text" /></div>
      <div v-else-if="b.type === 'divider'" class="flex items-center gap-3 text-xs text-muted-foreground"><hr class="flex-1" />{{ b.text }}<hr class="flex-1" /></div>
      <blockquote v-else-if="b.type === 'quote'" class="rounded-r-lg border-l-4 border-primary bg-muted/40 p-4" :style="{ borderColor: safeColor(b.color), backgroundColor: safeColor(b.bg) }"><HrHtmlContent :html="b.text" /></blockquote>
      <div v-else-if="b.type === 'numsec'"><h2 class="mb-3 rounded-md bg-primary/10 px-4 py-3 text-xl font-semibold" :style="{ color: safeColor(b.color) }">{{ b.title }}</h2><HrHtmlContent :html="b.text" /></div>
      <div v-else-if="['callout', 'alert'].includes(b.type)" class="rounded-lg border-l-4 bg-muted/40 p-4" :class="b.variant === 'danger' ? 'border-destructive' : b.variant === 'warning' || b.variant === 'warn' ? 'border-warning' : 'border-primary'"><h3 v-if="b.title" class="mb-1 font-semibold">{{ b.title }}</h3><HrHtmlContent :html="b.text" /></div>
      <div v-else-if="b.type === 'docref'" class="rounded-lg border bg-muted/30 p-4"><h3 class="mb-2 font-semibold">{{ b.title }}</h3><HrHtmlContent :html="b.text" /><p class="mt-3 text-primary">{{ b.button_text || `前往《${b.title || '文档'}》→` }}</p></div>
      <div v-else-if="b.type === 'compare'" class="grid gap-3 sm:grid-cols-2"><div v-for="(side, j) in [b.left, b.right]" :key="j" class="rounded-lg border border-l-4 p-4" :style="{ borderLeftColor: safeColor(side?.color) }"><h3 class="mb-2 font-semibold">{{ side?.title }}</h3><ul class="list-inside list-disc space-y-2"><li v-for="(line, k) in side?.items ?? []" :key="k">{{ line }}</li></ul></div></div>
      <div v-else-if="b.type === 'tabcard'" class="rounded-lg border p-4"><div class="mb-4 flex flex-wrap gap-1"><Button v-for="(tab, j) in b.tabs ?? []" :key="j" size="sm" :variant="(tabSelection[i] ?? b.activeTab) === j ? 'secondary' : 'ghost'" @click="tabSelection[i] = j">{{ tab.name || `页签 ${j + 1}` }}</Button></div><template v-for="(tab, j) in b.tabs ?? []" :key="j"><div v-if="(tabSelection[i] ?? b.activeTab) === j"><h3 class="font-semibold">{{ tab.header }}</h3><p class="text-xs text-muted-foreground">{{ tab.subHeader }}</p><div class="my-4 grid grid-cols-3 gap-2"><div v-for="(m, k) in tab.metrics ?? []" :key="k"><strong class="block text-xl">{{ m.value }}</strong>{{ m.label }}</div></div><HrHtmlContent :html="tab.note" /></div></template></div>
      <div v-else-if="b.type === 'accordion'" class="divide-y rounded-lg border"><details v-for="(item, j) in b.items ?? []" :key="j" :open="item.open" class="p-4"><summary class="cursor-pointer font-medium">{{ item.icon }} {{ item.title }}</summary><ul class="mt-3 list-inside list-disc space-y-1"><li v-for="(line, k) in item.lines ?? []" :key="k">{{ line }}</li></ul></details></div>
      <div v-else-if="b.type === 'status'" class="space-y-2"><div v-for="(item, j) in b.items ?? []" :key="j" class="flex gap-3 rounded-md bg-muted/30 p-3"><span :class="item.status === 'fail' ? 'text-destructive' : 'text-primary'">{{ item.status === 'pass' ? '✓' : item.status === 'fail' ? '✕' : '!' }}</span><span class="flex-1">{{ item.text }}</span><span class="text-xs text-muted-foreground">{{ item.tag }}</span></div></div>
      <div v-else :class="['grid gap-3', b.type === 'steps' ? 'grid-cols-1' : b.type === 'metrics' || b.columns === 3 ? 'sm:grid-cols-3' : 'sm:grid-cols-2']"><div v-for="(item, j) in b.type === 'bento' ? b.cards ?? [] : b.items ?? []" :key="j" class="rounded-lg border p-4" :style="b.type === 'bento' ? { borderLeft: `3px solid ${safeColor(item.color) || 'var(--primary)'}` } : undefined"><span class="mr-2">{{ b.type === 'steps' ? `${j + 1}.` : item.icon }}</span><strong :class="b.type === 'metrics' ? 'block text-2xl' : ''">{{ b.type === 'metrics' ? item.value : item.title }}</strong><p v-if="b.type === 'metrics'" class="mt-1 text-xs text-muted-foreground">{{ item.label }}</p><HrHtmlContent v-else-if="b.type === 'bento'" :html="item.desc" /><p v-else class="mt-2 whitespace-pre-wrap leading-relaxed text-muted-foreground">{{ item.desc || item.text }}</p></div></div>
    </template>
  </div>
</template>
<script setup lang="ts">
import { reactive } from 'vue'
import Button from '../../ui/Button.vue'
import HrHtmlContent from './HrHtmlContent.vue'
import { hrSafeUrl, type HrBlock } from '../../lib/portal-hr'
defineProps<{ blocks: HrBlock[] }>()
const tabSelection = reactive<Record<number, number>>({})
function safeColor(value: unknown) { return typeof value === 'string' && /^#[0-9a-f]{6}$/i.test(value) ? value : undefined }
</script>
