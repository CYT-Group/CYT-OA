<!-- HR 文档引用块选择器：每个块实例独立维护搜索词、结果和请求状态。 -->
<template>
  <div class="hdrp">
    <div v-if="selected && !editing" class="hdrp-selected">
      <span class="hdrp-icon" aria-hidden="true">📄</span>
      <span class="hdrp-selected-text">
        <strong>{{ title }}</strong>
        <small>{{ documentId ? `/article/${documentId}` : `/hr-doc/${slug}` }}</small>
      </span>
      <button type="button" class="hdrp-change" @click="startEditing">更换</button>
    </div>

    <div v-else class="hdrp-search">
      <input
        ref="searchEl"
        v-model="query"
        type="search"
        class="hdrp-input"
        placeholder="搜索已发布文档标题…"
        @keydown.enter.prevent="pickFirst"
      >
      <div class="hdrp-results">
        <div v-if="loading" class="hdrp-state">加载中…</div>
        <button v-else-if="error" type="button" class="hdrp-state hdrp-error" @click="loadDocs">{{ error }}</button>
        <div v-else-if="searched && !results.length" class="hdrp-state">没有匹配的已发布文档</div>
        <button
          v-for="doc in results"
          :key="doc.id"
          type="button"
          class="hdrp-result"
          @click="pick(doc)"
        >
          <span class="hdrp-icon" aria-hidden="true">📄</span>
          <span class="hdrp-result-text">
            <strong>{{ doc.title }}</strong>
            <small>{{ doc.canonical_url }}</small>
          </span>
        </button>
      </div>
      <button v-if="selected" type="button" class="hdrp-cancel" @click="editing = false">取消更换</button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { usePublishedHrDocs } from '../../composables/usePublishedHrDocs'

// published 端点只接收 category，不支持服务端搜索/分页参数 → 列表整份取回、前端筛选。
// 缓存收敛到 usePublishedHrDocs（渲染端也要按 slug 反查同一份数据的 theme/cover），
// 各组件实例仍各自持有 query 与过滤结果。
const { docs, failed: docsFailed, load: loadPublishedDocs } = usePublishedHrDocs()

const props = defineProps({
  documentId: { type: Number, default: 0 },
  slug: { type: String, default: '' },
  title: { type: String, default: '' },
})
const emit = defineEmits(['select'])

const selected = computed(() => !!(props.documentId || props.slug) && !!props.title)
const editing = ref(!selected.value)
const query = ref('')
const results = computed(() => {
  const keyword = query.value.trim().toLocaleLowerCase()
  const rows = keyword
    ? docs.value.filter(doc => doc.title.toLocaleLowerCase().includes(keyword))
    : docs.value
  return rows.slice(0, 20)
})
const loading = ref(false)
const error = ref('')
const searched = ref(false)
const searchEl = ref(null)
let alive = true

async function loadDocs() {
  loading.value = true
  error.value = ''
  // load() 自己吞掉异常（只置 failed），所以成败看 docsFailed，别用 try/catch 猜
  await loadPublishedDocs()
  if (!alive) return
  loading.value = false
  searched.value = true
  if (docsFailed.value) error.value = '加载文档列表失败，点击重试'
}

function startEditing() {
  editing.value = true
  if (!searched.value || error.value) loadDocs()
  nextTick(() => searchEl.value?.focus())
}

function pick(doc) {
  emit('select', { document_id: doc.id, title: doc.title })
  editing.value = false
  query.value = ''
}

function pickFirst() {
  if (results.value.length) pick(results.value[0])
}

watch(selected, (value) => {
  if (!value) startEditing()
})

if (!selected.value) loadDocs()
onBeforeUnmount(() => {
  alive = false
})
</script>

<style scoped>
.hdrp { position: relative; }
.hdrp-selected {
  display: flex; align-items: center; gap: 10px; padding: 11px 14px;
  background: linear-gradient(135deg, rgba(99,102,241,.055), rgba(99,102,241,.015));
  border: 1px solid rgba(99,102,241,.16); border-radius: 10px;
}
.hdrp-icon { flex: none; font-size: 19px; }
.hdrp-selected-text,
.hdrp-result-text { min-width: 0; display: flex; flex: 1; flex-direction: column; gap: 2px; }
.hdrp-selected-text strong,
.hdrp-result-text strong { overflow: hidden; color: var(--t, #0d1026); font-size: 13.5px; text-overflow: ellipsis; white-space: nowrap; }
.hdrp-selected-text small,
.hdrp-result-text small { color: var(--t4, #8c92ab); font-family: var(--fm, monospace); font-size: 11px; }
.hdrp-change,
.hdrp-cancel {
  flex: none; padding: 5px 10px; border: 1px solid rgba(99,102,241,.2); border-radius: 7px;
  background: #fff; color: var(--v, #6366f1); cursor: pointer; font: inherit; font-size: 12px;
}
.hdrp-search { position: relative; }
.hdrp-input {
  width: 100%; padding: 9px 11px; border: 1px solid rgba(99,102,241,.18); border-radius: 8px;
  outline: none; background: #fff; color: var(--t, #0d1026); font: inherit; font-size: 13px;
}
.hdrp-input:focus { border-color: var(--v, #6366f1); box-shadow: 0 0 0 3px rgba(99,102,241,.08); }
.hdrp-results {
  max-height: 220px; margin-top: 5px; overflow-y: auto; border: 1px solid rgba(99,102,241,.14);
  border-radius: 9px; background: #fff; box-shadow: 0 8px 24px rgba(13,16,38,.08);
}
.hdrp-result {
  display: flex; width: 100%; align-items: center; gap: 8px; padding: 9px 11px;
  border: 0; border-bottom: 1px solid rgba(99,102,241,.07); background: transparent;
  cursor: pointer; text-align: left; font: inherit;
}
.hdrp-result:last-child { border-bottom: 0; }
.hdrp-result:hover { background: rgba(99,102,241,.06); }
.hdrp-state { display: block; width: 100%; padding: 14px 10px; border: 0; background: transparent; color: var(--t4, #8c92ab); font: inherit; font-size: 12px; text-align: center; }
.hdrp-error { color: #b91c1c; cursor: pointer; }
.hdrp-cancel { margin-top: 7px; color: var(--t3, #4a5072); }
</style>
