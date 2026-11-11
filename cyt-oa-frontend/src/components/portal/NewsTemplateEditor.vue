<template>
  <div class="space-y-5">
    <div><label for="news-template-cover" class="field-label">模板封面</label><PortalImageField id="news-template-cover" v-model="blocks.cover_image" label="动态封面" folder="news/cover" /></div>
    <HrBlockFields :value="blocks" :fields="newsTemplateFields[type] ?? []" id-prefix="news-template" />
    <div v-if="type === 'article'"><h3 class="mb-3 text-sm font-semibold">图文内容</h3><div v-for="(block, i) in blocks.content_blocks ?? []" :key="i" class="mb-3 rounded-lg border p-3"><div class="mb-3 flex items-center justify-between"><span class="text-xs text-muted-foreground">{{ i + 1 }} · {{ block.type === 'text' ? '文字段' : block.type === 'image' ? '图片' : '图文段' }}</span><div class="flex gap-1"><Button size="sm" variant="ghost" :disabled="i === 0" @click="move(blocks.content_blocks, i, -1)">上移</Button><Button size="sm" variant="ghost" :disabled="i === blocks.content_blocks.length - 1" @click="move(blocks.content_blocks, i, 1)">下移</Button><Button size="sm" variant="ghost" @click="blocks.content_blocks.splice(i, 1)">删除</Button></div></div><HrBlockFields :value="block" :fields="blockFields[block.type] ?? []" :id-prefix="`news-article-${i}`" /></div><div class="flex flex-wrap gap-2"><Button v-for="kind in articleTypes" :key="kind.value" variant="outline" size="sm" :disabled="(blocks.content_blocks?.length ?? 0) >= 100" @click="addArticle(kind.value)">添加{{ kind.label }}</Button></div></div>
    <div v-if="type === 'simple' || type === 'cross_dept'"><h3 class="mb-3 text-sm font-semibold">正文段落</h3><div v-for="(_, i) in blocks.paragraphs ?? []" :key="i" class="mb-3"><div class="mb-2 flex items-center justify-between"><span class="text-xs">段落 {{ i + 1 }}</span><Button variant="ghost" size="sm" @click="blocks.paragraphs.splice(i, 1)">删除</Button></div><HrRichText v-model="blocks.paragraphs[i]" :label="`段落 ${i + 1}`" /></div><Button variant="outline" size="sm" @click="(blocks.paragraphs ??= []).push('')">添加段落</Button></div>
    <div><h3 class="mb-3 text-sm font-semibold">图片集</h3><div v-for="(_, i) in blocks.gallery ?? []" :key="i" class="mb-3 rounded-lg border p-3"><div class="mb-2 flex items-center justify-between"><span class="text-xs">图片 {{ i + 1 }}</span><Button size="sm" variant="ghost" @click="blocks.gallery.splice(i, 1)">删除</Button></div><PortalImageField v-model="blocks.gallery[i]" :label="`图集图片 ${i + 1}`" folder="news/gallery" /></div><Button variant="outline" size="sm" :disabled="(blocks.gallery?.length ?? 0) >= 30" @click="(blocks.gallery ??= []).push('')">添加图片</Button></div>
  </div>
</template>
<script setup lang="ts">
import Button from '../../ui/Button.vue'
import HrBlockFields from './HrBlockFields.vue'
import HrRichText from './HrRichText.vue'
import PortalImageField from './PortalImageField.vue'
import { blockFields, newBlock } from '../../lib/portal-hr'
import { newsTemplateFields } from '../../lib/portal-content'
const props = defineProps<{ type: string; blocks: Record<string, any> }>()
const articleTypes = [{ value: 'text', label: '文字' }, { value: 'image', label: '图片' }, { value: 'imageText', label: '图文' }]
function addArticle(type: string) { (props.blocks.content_blocks ??= []).push(newBlock(type)) }
function move(items: unknown[], i: number, delta: number) { const item = items.splice(i, 1)[0]; items.splice(i + delta, 0, item) }
</script>
