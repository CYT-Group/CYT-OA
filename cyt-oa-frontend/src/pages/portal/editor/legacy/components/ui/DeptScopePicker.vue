<!--
DeptScopePicker — 部门多选器（chips 风格）

数据源：/api/portal/directory/departments（distinct portal_users.department）
用法：
  <DeptScopePicker v-model="form.dept_scope" :empty-label="'全员可见'" />

v-model 约定：
  - null / [] → 表示「全员可见」（不限定部门）
  - ['市场部', '销售部'] → 限定这些部门

设计：
  - 折叠状态：显示当前选中的部门 chips + 「+ 添加」按钮
  - 展开状态：下拉面板列出所有部门 + 搜索框 + 复选框 + 员工数提示
  - 「清空选择」按钮 → v-model 设为 [] （= 全员可见）
-->
<template>
  <div ref="rootEl" class="dsp" :class="{ open: expanded }" @keydown.esc.prevent.stop="close" @focusout="onFocusOut">
    <!-- 折叠态：当前选中的 chips -->
    <div class="dsp-display" @click.self="toggle">
      <button
        v-if="!modelValue || modelValue.length === 0" ref="triggerEl" type="button" class="dsp-empty"
        :aria-expanded="expanded" :aria-controls="panelId" @click="toggle"
      >🌐 {{ emptyLabel }}（点击限定部门）</button>
      <template v-else>
        <span v-for="dept in modelValue" :key="dept" class="dsp-chip">
          {{ dept }}
          <button type="button" class="dsp-chip-x" @click.stop="remove(dept)" :title="`移除 ${dept}`">×</button>
        </span>
        <button ref="triggerEl" type="button" class="dsp-add" :aria-expanded="expanded" :aria-controls="panelId" @click="toggle">+ 添加</button>
        <button type="button" class="dsp-clear" @click.stop="clear" title="清空 = 改为全员可见">清空</button>
      </template>
    </div>

    <!-- 展开态：下拉面板 -->
    <div v-if="expanded" :id="panelId" class="dsp-panel" @click.stop>
      <div class="dsp-panel-hd">
        <input
          ref="searchEl" v-model="search"
          aria-label="搜索部门"
          type="text"
          class="dsp-search"
          placeholder="搜索部门名…"
          @click.stop
        >
        <button type="button" class="dsp-close" @click="close">收起</button>
      </div>
      <div v-if="loading" class="dsp-loading">加载部门列表…</div>
      <div v-else-if="filtered.length === 0" class="dsp-empty-state">
        {{ search ? '没有匹配的部门' : '通讯录里暂无部门数据' }}
      </div>
      <ul v-else class="dsp-list">
        <li v-for="dept in filtered" :key="dept.name" class="dsp-item">
          <label class="dsp-check">
            <input
              type="checkbox"
              :checked="isSelected(dept.name)"
              @change="toggleDept(dept.name)"
            >
            <span class="dsp-name">{{ dept.name }}</span>
            <span class="dsp-count">{{ dept.user_count }} 人</span>
          </label>
        </li>
      </ul>
      <div class="dsp-panel-ft">
        <span class="dsp-hint">
          已选 <strong>{{ modelValue?.length || 0 }}</strong> 个部门
          ({{ totalSelectedUsers }} 人)
          · 不选 = 全员可见
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, useId } from 'vue'
import { directoryApi } from '../../../bridge/api'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  emptyLabel: { type: String, default: '全员可见' },
})
const emit = defineEmits(['update:modelValue'])

const expanded = ref(false)
const rootEl = ref(null)
const triggerEl = ref(null)
const searchEl = ref(null)
const panelId = `dept-scope-${useId()}`
const loading = ref(false)
const departments = ref([])  // [{ name, user_count }, ...]
const search = ref('')

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return departments.value
  return departments.value.filter((d) => d.name.toLowerCase().includes(q))
})

const totalSelectedUsers = computed(() => {
  if (!props.modelValue?.length) return 0
  const map = new Map(departments.value.map((d) => [d.name, d.user_count]))
  return props.modelValue.reduce((sum, n) => sum + (map.get(n) || 0), 0)
})

function isSelected(name) {
  return (props.modelValue || []).includes(name)
}

function toggleDept(name) {
  const cur = props.modelValue || []
  if (cur.includes(name)) {
    emit('update:modelValue', cur.filter((n) => n !== name))
  } else {
    emit('update:modelValue', [...cur, name])
  }
}

function remove(name) {
  emit('update:modelValue', (props.modelValue || []).filter((n) => n !== name))
  nextTick(() => triggerEl.value?.focus({ preventScroll: true }))
}

function clear() {
  emit('update:modelValue', [])
  nextTick(() => triggerEl.value?.focus({ preventScroll: true }))
}

async function toggle() {
  expanded.value = !expanded.value
  if (expanded.value) nextTick(() => searchEl.value?.focus({ preventScroll: true }))
  if (expanded.value && departments.value.length === 0 && !loading.value) {
    loading.value = true
    try {
      const r = await directoryApi.departments()
      departments.value = r.data || []
    } catch (e) {
      console.error('加载部门列表失败', e)
    } finally {
      loading.value = false
    }
  }
}

function close() {
  if (!expanded.value) return
  expanded.value = false
  nextTick(() => triggerEl.value?.focus({ preventScroll: true }))
}
function onDocPointerDown(e) {
  if (expanded.value && !rootEl.value?.contains(e.target)) expanded.value = false
}
function onFocusOut(e) {
  // Tab 离开选择器只收起，不能把焦点拉回去阻塞键盘继续填写。
  if (!rootEl.value?.contains(e.relatedTarget)) expanded.value = false
}
onMounted(() => document.addEventListener('pointerdown', onDocPointerDown, true))
onBeforeUnmount(() => document.removeEventListener('pointerdown', onDocPointerDown, true))

onMounted(() => {
  // 预加载（让用户第一次点开瞬间显示）
  if (departments.value.length === 0) {
    directoryApi.departments().then(
      (r) => { departments.value = r.data || [] },
      () => {},
    )
  }
})
</script>

<style scoped>
.dsp {
  position: relative;
  border: 1px solid rgba(99,102,241,0.22);
  border-radius: 10px;
  background: white;
}
.dsp.open { border-color: var(--v, #6366F1); }

.dsp-display {
  min-height: 38px;
  padding: 6px 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  cursor: pointer;
}
.dsp-empty {
  border: none;
  background: transparent;
  text-align: left;
  padding: 2px 0;
  font-family: inherit;
  cursor: pointer;
  color: var(--t4, #8C92AB);
  font-size: 13px;
  flex: 1;
}
.dsp-empty:focus-visible, .dsp-add:focus-visible { outline: 2px solid var(--v, #6366F1); outline-offset: 3px; }
.dsp-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 4px 3px 10px;
  background: rgba(99,102,241,0.10);
  color: var(--v, #6366F1);
  border-radius: 14px;
  font-size: 12px;
  font-weight: 500;
}
.dsp-chip-x {
  background: rgba(99,102,241,0.18);
  border: none;
  color: var(--v, #6366F1);
  width: 16px;
  height: 16px;
  border-radius: 50%;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  padding: 0;
}
.dsp-chip-x:hover { background: rgba(99,102,241,0.35); }
.dsp-add, .dsp-clear {
  padding: 3px 10px;
  border: 1px dashed rgba(99,102,241,0.3);
  background: transparent;
  color: var(--v, #6366F1);
  border-radius: 14px;
  font-size: 12px;
  cursor: pointer;
}
.dsp-add:hover { background: rgba(99,102,241,0.06); }
.dsp-clear {
  border-style: solid;
  border-color: rgba(244,63,94,0.25);
  color: #b91c1c;
}
.dsp-clear:hover { background: rgba(244,63,94,0.08); }

.dsp-panel {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  max-height: 360px;
  background: white;
  border: 1px solid var(--v, #6366F1);
  border-radius: 10px;
  box-shadow: 0 12px 32px rgba(13,16,38,0.18);
  z-index: 50;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.dsp-panel-hd {
  padding: 10px 12px;
  display: flex;
  gap: 8px;
  border-bottom: 1px solid rgba(99,102,241,0.12);
}
.dsp-search {
  flex: 1;
  padding: 6px 10px;
  border: 1px solid rgba(99,102,241,0.18);
  border-radius: 6px;
  font-size: 13px;
  background: rgba(245,245,250,0.5);
}
.dsp-search:focus { outline: 2px solid var(--v); outline-offset: -1px; background: white; }
.dsp-close {
  padding: 4px 12px;
  background: rgba(99,102,241,0.08);
  color: var(--v, #6366F1);
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}
.dsp-loading, .dsp-empty-state {
  text-align: center;
  padding: 30px 20px;
  color: var(--t4, #8C92AB);
  font-size: 13px;
}
.dsp-list {
  list-style: none;
  margin: 0;
  padding: 4px;
  overflow-y: auto;
  flex: 1;
}
.dsp-item:hover { background: rgba(99,102,241,0.05); }
.dsp-check {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  cursor: pointer;
  border-radius: 6px;
  font-size: 13px;
}
.dsp-check input { width: 16px; height: 16px; cursor: pointer; }
.dsp-name { flex: 1; color: var(--t, #0D1026); }
.dsp-count {
  color: var(--t4, #8C92AB);
  font-size: 11px;
  font-family: var(--fm, monospace);
}
.dsp-panel-ft {
  padding: 8px 12px;
  border-top: 1px solid rgba(99,102,241,0.12);
  background: rgba(245,245,250,0.5);
  font-size: 12px;
  color: var(--t3, #5A5F7A);
}
.dsp-hint strong { color: var(--v, #6366F1); }
</style>
