<!--
SelectField — 轻量下拉选择（替代原生 <select>，用于后台筛选/表单）

为什么不用原生 select：
  原生下拉的箭头和展开面板由操作系统渲染，样式无法统一（Windows 上是一块灰箭头 + 白列表），
  和站内的圆角/配色对不上。这里用 button + listbox 自绘，视觉可控，同时保留键盘操作。

交互
- 点击 / Enter / Space / ↓ 打开；Esc 关闭；点击外部或失焦关闭
- ↑↓ 移动高亮，Home/End 跳首尾，Enter 选中
- 选中项带勾；面板打开时箭头翻转
- 动效遵循 prefers-reduced-motion

用法
  <SelectField v-model="filter.status" :options="[{ value: '', label: '全部' }, ...]" />
-->
<template>
  <div ref="root" class="sf" :class="{ 'is-open': open, 'is-disabled': disabled }" @focusout="onFocusOut">
    <button
      ref="trigger"
      type="button"
      class="sf-trigger"
      role="combobox"
      :aria-label="ariaLabel || undefined"
      :aria-expanded="open"
      :aria-controls="listId"
      :aria-activedescendant="open && options[activeIndex] ? `${listId}-${activeIndex}` : undefined"
      :disabled="disabled"
      @click="toggle"
      @keydown="onKeydown"
    >
      <span class="sf-label" :class="{ 'is-placeholder': currentLabel === '' }">{{ currentLabel || placeholder }}</span>
      <svg class="sf-chevron" viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
        <path d="M4 6.5 8 10.5 12 6.5" fill="none" stroke="currentColor" stroke-width="1.6"
              stroke-linecap="round" stroke-linejoin="round" />
      </svg>
    </button>

    <Transition name="sf-pop">
      <div v-if="open" class="sf-panel" :id="listId" role="listbox" :aria-label="ariaLabel || placeholder">
        <button
          v-for="(opt, i) in options"
          :id="`${listId}-${i}`"
          :key="String(opt.value)"
          type="button"
          class="sf-option"
          :class="{ 'is-active': i === activeIndex, 'is-selected': opt.value === modelValue }"
          role="option"
          tabindex="-1"
          :aria-selected="opt.value === modelValue"
          @mousedown.prevent
          @mouseenter="activeIndex = i"
          @click="select(opt)"
        >
          <span class="sf-option-label">{{ opt.label }}</span>
          <svg v-if="opt.value === modelValue" class="sf-check" viewBox="0 0 16 16" width="13" height="13" aria-hidden="true">
            <path d="M3 8.5 6.2 11.5 13 4.5" fill="none" stroke="currentColor" stroke-width="1.8"
                  stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number, Boolean, null], default: '' },
  /** [{ value, label }]，value 允许空串（表示「全部」这类选项） */
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: '请选择' },
  disabled: { type: Boolean, default: false },
  ariaLabel: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'change'])

const root = ref(null)
const trigger = ref(null)
const open = ref(false)
const activeIndex = ref(0)
const listId = `sf-${useId()}`

const currentLabel = computed(() => {
  const hit = props.options.find(o => o.value === props.modelValue)
  return hit ? hit.label : ''
})

function syncActive() {
  const i = props.options.findIndex(o => o.value === props.modelValue)
  activeIndex.value = i >= 0 ? i : 0
}

function openMenu() {
  if (props.disabled) return
  syncActive()
  open.value = true
  nextTick(() => scrollActiveIntoView())
}

function closeMenu() {
  open.value = false
}

function toggle() {
  open.value ? closeMenu() : openMenu()
}

function select(opt) {
  emit('update:modelValue', opt.value)
  emit('change', opt.value)
  closeMenu()
  nextTick(() => trigger.value?.focus())
}

function onKeydown(e) {
  if (props.disabled) return
  const n = props.options.length
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    e.preventDefault()
    if (!open.value) return openMenu()
    if (!n) return
    activeIndex.value = (activeIndex.value + (e.key === 'ArrowDown' ? 1 : -1) + n) % n
    scrollActiveIntoView()
  } else if (e.key === 'Home' || e.key === 'End') {
    if (!open.value) return
    e.preventDefault()
    activeIndex.value = e.key === 'Home' ? 0 : n - 1
    scrollActiveIntoView()
  } else if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    if (!open.value) openMenu()
    else if (props.options[activeIndex.value]) select(props.options[activeIndex.value])
  } else if (e.key === 'Escape') {
    if (!open.value) return
    e.preventDefault()
    closeMenu()
  } else if (e.key === 'Tab') {
    closeMenu()
  }
}

function scrollActiveIntoView() {
  const panel = root.value?.querySelector('.sf-panel')
  const el = panel?.querySelector('.sf-option.is-active')
  if (!panel || !el) return
  // 只滚面板，别把整页带着滚
  const p = panel.getBoundingClientRect()
  const r = el.getBoundingClientRect()
  if (r.top < p.top) panel.scrollTop -= (p.top - r.top)
  else if (r.bottom > p.bottom) panel.scrollTop += (r.bottom - p.bottom)
}

function onPointerDown(e) {
  if (!root.value?.contains(e.target)) closeMenu()
}
function onFocusOut(e) {
  if (!root.value?.contains(e.relatedTarget)) closeMenu()
}
// 面板打开期间在 document 上兜底监听 Esc：焦点可能已经不在触发器上（例如用鼠标点开后又点了别处再按 Esc），
// 只挂在触发器上会漏。捕获阶段监听，避免被内层组件的 keydown 拦截。
function onDocKeydown(e) {
  if (e.key === 'Escape') closeMenu()
}

watch(open, (v) => {
  if (v) {
    document.addEventListener('pointerdown', onPointerDown, true)
    document.addEventListener('keydown', onDocKeydown, true)
  } else {
    document.removeEventListener('pointerdown', onPointerDown, true)
    document.removeEventListener('keydown', onDocKeydown, true)
  }
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onPointerDown, true)
  document.removeEventListener('keydown', onDocKeydown, true)
})
</script>

<style scoped>
.sf { position: relative; width: 100%; }

/* 触发器：与 .field input/select 完全同款（36px 高、同描边、同 focus 环） */
.sf-trigger {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  width: 100%; height: 36px; padding: 0 10px;
  border: 1px solid rgba(99, 102, 241, 0.2); border-radius: 8px;
  background: #fff; color: var(--t, #0D1026);
  font-family: inherit; font-size: 14px; text-align: left; cursor: pointer;
  transition: border-color .16s ease, box-shadow .18s ease;
}
.sf-trigger:hover:not(:disabled) { border-color: rgba(99, 102, 241, 0.35); }
.sf-trigger:focus-visible {
  outline: none; border-color: var(--v, #6366F1);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.14);
}
.sf.is-open .sf-trigger {
  border-color: var(--v, #6366F1);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.14);
}
.sf.is-disabled .sf-trigger { opacity: .55; cursor: not-allowed; background: rgba(140, 146, 171, 0.06); }

.sf-label { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sf-label.is-placeholder { color: var(--t4, #8C92AB); }
.sf-chevron {
  flex: 0 0 auto; color: var(--t4, #8C92AB);
  transition: transform .18s ease, color .16s ease;
}
.sf.is-open .sf-chevron { transform: rotate(180deg); color: var(--v, #6366F1); }

/* 面板 */
.sf-panel {
  position: absolute; z-index: 60; left: 0; right: 0; top: calc(100% + 6px);
  max-height: 280px; overflow-y: auto; padding: 4px;
  border: 1px solid rgba(99, 102, 241, 0.16); border-radius: 10px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(13, 16, 38, 0.12), 0 2px 6px rgba(13, 16, 38, 0.06);
  scrollbar-width: thin;
}
.sf-panel::-webkit-scrollbar { width: 8px; }
.sf-panel::-webkit-scrollbar-thumb { background: rgba(99, 102, 241, 0.22); border-radius: 999px; }

.sf-option {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  width: 100%; padding: 7px 9px; border: 0; border-radius: 7px;
  background: transparent; color: var(--t2, #2D3258);
  font-family: inherit; font-size: 13px; text-align: left; cursor: pointer;
  transition: background-color .12s ease, color .12s ease;
}
.sf-option.is-active { background: rgba(99, 102, 241, 0.08); color: var(--t, #0D1026); }
.sf-option.is-selected { color: var(--v, #6366F1); font-weight: 600; }
.sf-option-label { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sf-check { flex: 0 0 auto; }

/* 动效：轻微下移淡入（尊重「减少动态效果」） */
.sf-pop-enter-active, .sf-pop-leave-active { transition: opacity .14s ease, transform .14s ease; }
.sf-pop-enter-from, .sf-pop-leave-to { opacity: 0; transform: translateY(-4px); }
@media (prefers-reduced-motion: reduce) {
  .sf-pop-enter-active, .sf-pop-leave-active { transition: none; }
  .sf-chevron { transition: none; }
}
</style>
