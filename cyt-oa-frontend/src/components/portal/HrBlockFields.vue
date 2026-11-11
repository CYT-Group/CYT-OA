<template>
  <div class="space-y-3">
    <div v-for="field in fields" :key="field.key">
      <template v-if="field.kind === 'array'">
        <p class="mb-2 text-xs font-medium">{{ field.label }} <span class="text-muted-foreground">{{ value[field.key]?.length ?? 0 }} / {{ field.max ?? 30 }}</span></p>
        <div v-for="(item, i) in value[field.key] ?? []" :key="i" class="mb-2 rounded-lg border bg-muted/20 p-3">
          <div class="mb-2 flex items-center justify-between gap-2"><span class="text-xs text-muted-foreground">{{ field.label }} {{ i + 1 }}</span><div class="flex gap-1">
            <Button type="button" size="icon-sm" variant="ghost" :disabled="i === 0" :aria-label="`上移${field.label}`" @click="move(value[field.key], i, -1)"><ArrowUp class="size-3" /></Button>
            <Button type="button" size="icon-sm" variant="ghost" :disabled="i === value[field.key].length - 1" :aria-label="`下移${field.label}`" @click="move(value[field.key], i, 1)"><ArrowDown class="size-3" /></Button>
            <Button type="button" size="icon-sm" variant="ghost" :aria-label="`删除${field.label}`" @click="value[field.key].splice(i, 1)"><Trash2 class="size-3" /></Button>
          </div></div>
          <HrBlockFields :value="item" :fields="field.fields ?? []" :id-prefix="`${idPrefix}-${field.key}-${i}`" />
        </div>
        <Button type="button" variant="outline" size="sm" :disabled="(value[field.key]?.length ?? 0) >= (field.max ?? 30)" @click="add(field)"><Plus class="size-3" />添加{{ field.label }}</Button>
      </template>
      <fieldset v-else-if="field.kind === 'object'" class="rounded-lg border p-3">
        <legend class="px-1 text-xs font-medium">{{ field.label }}</legend><HrBlockFields :value="value[field.key]" :fields="field.fields ?? []" :id-prefix="`${idPrefix}-${field.key}`" />
      </fieldset>
      <template v-else>
        <label :for="`${idPrefix}-${field.key}`" class="mb-1.5 block text-xs font-medium">{{ field.label }}</label>
        <PortalImageField v-if="field.kind === 'image'" :id="`${idPrefix}-${field.key}`" v-model="value[field.key]" :label="field.label" folder="hr/doc-img" />
        <HrRichText v-else-if="field.kind === 'rich'" :id="`${idPrefix}-${field.key}`" v-model="value[field.key]" :label="field.label" />
        <Textarea v-else-if="field.kind === 'long'" :id="`${idPrefix}-${field.key}`" v-model="value[field.key]" :rows="field.key === 'html' ? 10 : 3" />
        <Textarea v-else-if="field.kind === 'lines'" :id="`${idPrefix}-${field.key}`" :model-value="(value[field.key] ?? []).join('\n')" :rows="3" @update:model-value="value[field.key] = String($event).split('\n').filter(v => v.trim())" />
        <Switch v-else-if="field.kind === 'bool'" :id="`${idPrefix}-${field.key}`" v-model="value[field.key]" />
        <Select v-else-if="field.kind === 'select'" :id="`${idPrefix}-${field.key}`" v-model="value[field.key]" :options="field.options ?? []" />
        <Input v-else :id="`${idPrefix}-${field.key}`" :model-value="value[field.key]" :type="field.kind === 'number' ? 'number' : 'text'" @update:model-value="value[field.key] = field.kind === 'number' ? ($event === '' ? undefined : Number($event)) : $event" />
      </template>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ArrowUp, ArrowDown, Plus, Trash2 } from '@lucide/vue'
import Input from '../../ui/Input.vue'
import Textarea from '../../ui/Textarea.vue'
import Select from '../../ui/Select.vue'
import Switch from '../../ui/Switch.vue'
import Button from '../../ui/Button.vue'
import HrRichText from './HrRichText.vue'
import PortalImageField from './PortalImageField.vue'
import { newFieldObject, type BlockField } from '../../lib/portal-hr'
const props = defineProps<{ value: Record<string, any>; fields: BlockField[]; idPrefix: string }>()
function move(items: unknown[], i: number, delta: number) { const item = items.splice(i, 1)[0]; items.splice(i + delta, 0, item) }
function add(field: BlockField) { if (!Array.isArray(props.value[field.key])) props.value[field.key] = []; props.value[field.key].push(newFieldObject(field.fields ?? [])) }
</script>
