<template>
  <div ref="root" :class="cn('grid grid-cols-1 gap-x-4 gap-y-4 sm:grid-cols-2', compact ? 'gap-y-3' : '')">
    <div
      v-for="f in fields"
      :key="f.key"
      :data-field="f.key"
      :class="cn('flex min-w-0 flex-col', isWide(f) ? 'sm:col-span-2' : '')"
    >
      <template v-if="f.type === 'switch'">
        <div class="flex items-center justify-between gap-4 rounded-lg border border-input bg-card px-3 py-2">
          <div class="min-w-0">
            <Label :for="fieldId(f.key)" class="mb-0" :required="f.required">{{ f.label }}</Label>
            <p v-if="f.hint" class="mt-0.5 text-[11px] leading-tight text-muted-foreground">{{ f.hint }}</p>
          </div>
          <Switch
            :id="fieldId(f.key)"
            :aria-label="f.label"
            :aria-invalid="!!errors[f.key]"
            :aria-describedby="errors[f.key] ? `${fieldId(f.key)}-error` : undefined"
            :model-value="Boolean(model[f.key])"
            :disabled="readonly"
            @update:model-value="model[f.key] = $event"
          />
        </div>
      </template>

      <template v-else>
        <Label :for="fieldId(f.key)" :required="f.required" :class="f.hint ? 'mb-0.5' : ''">{{
          f.label
        }}</Label>
        <p v-if="f.hint" class="mb-1.5 text-[11px] leading-tight text-muted-foreground">{{ f.hint }}</p>

        <Select
          :id="fieldId(f.key)"
          :aria-label="f.label"
          :aria-invalid="!!errors[f.key]"
          :aria-describedby="errors[f.key] ? `${fieldId(f.key)}-error` : undefined"
          v-if="f.type === 'select'"
          :model-value="model[f.key]"
          :options="resolvedOptions[f.key] ?? []"
          :disabled="readonly"
          placeholder="请选择"
          @update:model-value="model[f.key] = $event ?? ''"
        />

        <MultiSelect
          :id="fieldId(f.key)"
          :aria-label="f.label"
          :aria-invalid="!!errors[f.key]"
          :aria-describedby="errors[f.key] ? `${fieldId(f.key)}-error` : undefined"
          v-else-if="f.type === 'members'"
          :model-value="model[f.key] ?? []"
          :options="multipleOptions(f)"
          :disabled="readonly"
          placeholder="请选择"
          @update:model-value="model[f.key] = $event"
        />

        <Textarea
          :id="fieldId(f.key)"
          :aria-label="f.label"
          :aria-invalid="!!errors[f.key]"
          :aria-describedby="errors[f.key] ? `${fieldId(f.key)}-error` : undefined"
          v-else-if="f.type === 'textarea'"
          v-model="model[f.key]"
          :rows="compact ? 2 : 3"
          :disabled="readonly"
          :placeholder="f.placeholder"
        />

        <Input
          :id="fieldId(f.key)"
          :aria-label="f.label"
          :aria-invalid="!!errors[f.key]"
          :aria-describedby="errors[f.key] ? `${fieldId(f.key)}-error` : undefined"
          v-else-if="f.type === 'datetime'"
          v-model="model[f.key]"
          type="datetime-local"
          :disabled="readonly"
        />

        <Input
          :id="fieldId(f.key)"
          :aria-label="f.label"
          :aria-invalid="!!errors[f.key]"
          :aria-describedby="errors[f.key] ? `${fieldId(f.key)}-error` : undefined"
          v-else-if="f.type === 'date'"
          v-model="model[f.key]"
          type="date"
          :disabled="readonly"
        />

        <Input
          :id="fieldId(f.key)"
          :aria-label="f.label"
          :aria-invalid="!!errors[f.key]"
          :aria-describedby="errors[f.key] ? `${fieldId(f.key)}-error` : undefined"
          v-else-if="f.type === 'number'"
          :model-value="model[f.key] ?? ''"
          type="number"
          :disabled="readonly"
          @update:model-value="model[f.key] = $event"
        />

        <Input
          :id="fieldId(f.key)"
          :aria-label="f.label"
          :aria-invalid="!!errors[f.key]"
          :aria-describedby="errors[f.key] ? `${fieldId(f.key)}-error` : undefined"
          v-else
          v-model="model[f.key]"
          :placeholder="f.placeholder"
          :disabled="readonly"
        />
      </template>

      <p
        v-if="errors[f.key]"
        :id="`${fieldId(f.key)}-error`"
        role="alert"
        class="mt-1.5 text-xs text-destructive"
      >
        {{ errors[f.key] }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, useId, watch } from 'vue'
import type { FieldConfig } from '../configs/types'
import { dictOptions } from '../lib/dict'
import { cn } from '../lib/utils'
import Input from '../ui/Input.vue'
import Label from '../ui/Label.vue'
import MultiSelect from '../ui/MultiSelect.vue'
import Select from '../ui/Select.vue'
import Switch from '../ui/Switch.vue'
import Textarea from '../ui/Textarea.vue'

const props = defineProps<{
  fields: FieldConfig[]
  /** 初始值：日期类给 ISO 串，其余原样 */
  initial?: Record<string, unknown>
  /** 明细表单字段少，行距收紧一点，但同样走两列网格，不再单列混排 */
  compact?: boolean
  readonly?: boolean
}>()

const fieldPrefix = useId()
const fieldId = (key: string) => `${fieldPrefix}-${key}`
const root = ref<HTMLElement | null>(null)
const model = reactive<Record<string, any>>({})
const errors = reactive<Record<string, string>>({})

/** 字典按类型缓存，label 给人看、value 给后端。 */
const dictCache = reactive<Record<string, { label: string; value: string }[]>>({})

async function ensureDict(type: string) {
  if (dictCache[type]) return
  const items = await dictOptions(type)
  dictCache[type] = items.map((i) => ({ label: i.label, value: i.value }))
}

/** 选项统一走 computed：字典异步回来、或父级事后补上 options（用品台账等），
 *  下拉都会自己更新，不用重建整个表单，也不会把用户已经填的值冲掉。 */
const resolvedOptions = computed<Record<string, { label: string; value: string }[]>>(() => {
  const out: Record<string, { label: string; value: string }[]> = {}
  for (const f of props.fields) {
    out[f.key] = f.dictType
      ? (dictCache[f.dictType] ?? [])
      : (f.options ?? []).map((o) => ({ label: o.label, value: String(o.value) }))
  }
  return out
})

function multipleOptions(f: FieldConfig) {
  return resolvedOptions.value[f.key] ?? []
}

function isWide(f: FieldConfig) {
  return f.type === 'textarea' || f.type === 'members'
}

watch(
  () => props.fields,
  async (fields) => {
    const keys = new Set(fields.map((f) => f.key))
    for (const k of Object.keys(model)) if (!keys.has(k)) delete model[k]
    for (const f of fields) {
      if (f.dictType) await ensureDict(f.dictType)
      // 只补没填过的字段：父级刷新 options 时不能把已输入的内容清空
      if (f.key in model) continue
      const seeded = props.initial?.[f.key]
      // 多选框的 option 值是字符串，回填时必须统一成字符串，否则编辑时选不中已有的人
      if (f.type === 'members') model[f.key] = Array.isArray(seeded) ? seeded.map(String) : []
      else model[f.key] = seeded ?? (f.type === 'switch' ? false : '')
    }
  },
  { immediate: true }
)

function validate() {
  for (const k of Object.keys(errors)) delete errors[k]
  let ok = true
  let first = ''
  for (const f of props.fields) {
    const v = model[f.key]
    if (f.required && (v === '' || v === null || v === undefined || (Array.isArray(v) && !v.length))) {
      errors[f.key] = '不能为空'
      if (!first) first = f.key
      ok = false
    }
  }
  if (first) {
    void nextTick(() => {
      const holder = root.value?.querySelector<HTMLElement>(`[data-field="${first}"]`)
      const el = holder?.matches('input,textarea,select,button')
        ? holder
        : holder?.querySelector<HTMLElement>('input,textarea,select,button')
      el?.focus()
      el?.scrollIntoView?.({ block: 'nearest' })
    })
  }
  return ok
}

/** 把表单值转成后端要的载荷：空串剔除、日期补时分、数字与字典值转数值。 */
function payload(): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  for (const f of props.fields) {
    const v = model[f.key]
    if (v === '' || v === null || v === undefined) continue
    if (f.type === 'datetime') out[f.key] = String(v).length === 16 ? `${v}:00` : v
    else if (f.type === 'date') out[f.key] = `${v}T00:00:00`
    else if (f.type === 'members') out[f.key] = (v as string[]).map(Number)
    else if (f.type === 'number' || f.type === 'select') out[f.key] = Number.isNaN(Number(v)) ? v : Number(v)
    else if (f.type === 'switch') out[f.key] = Boolean(v)
    else out[f.key] = v
  }
  return out
}

defineExpose({ validate, payload, model, errors })
</script>
