<!--
HrDocDraftHistory — HR 文档编辑器「草稿历史」右侧抽屉（口径-HR文档块编辑-前端.md §6）。

列表 = 每次保存草稿后端自动留的快照（相同内容去重、每文档最多 30 条），
与「已发布版本」是两张表：发布快照是签收对账凭据不可删，这里的草稿历史可随手清。

三个操作：
- 预览：抽屉内嵌只读渲染，块快照走 HrBlocksRenderer、HTML 快照走 HrDocRenderer
  （详情按需拉取并缓存，列表端点不带正文）
- 恢复：只把快照内容发给父组件填回编辑器（父组件因此自然变 dirty），
  用户点「保存草稿」才落库——有未保存修改时多拦一道覆盖确认
- 删除：确认后调删除端点并刷新列表

「当前」标记：最新一条快照与编辑器正在编辑的内容（标题 + 格式 + 正文三元组）
一致时打标。只比对最新一条（打开抽屉时静默预拉它的详情）——保存时后端也只和
最新一条去重，逐条拉 30 份全文做比对不值得。
-->
<template>
  <Teleport to="body">
    <Transition name="dh-fade">
      <div v-if="open" class="portal-editor-floating dh-mask" @click.self="close"></div>
    </Transition>
    <Transition name="dh-slide">
      <aside v-if="open" class="portal-editor-floating dh-drawer" role="dialog" aria-label="草稿历史">
        <div class="dh-head">
          <span class="dh-head-title">🕘 草稿历史</span>
          <span v-if="items.length" class="dh-count">{{ items.length }} 条</span>
          <button class="dh-close" title="关闭" @click="close">✕</button>
        </div>
        <div class="dh-body">
          <div v-if="listLoading" class="dh-tip">加载中…</div>
          <div v-else-if="listError" class="dh-tip err">
            {{ listError }}
            <button class="dh-retry" @click="loadList">重试</button>
          </div>
          <div v-else-if="!items.length" class="dh-tip">
            还没有历史版本。<br>
            每次点「保存草稿」会自动留一版（内容没变不重复留），最多保留最近 30 条。
          </div>
          <template v-else>
            <div v-for="it in items" :key="it.id" class="dh-item">
              <div class="dh-row1">
                <span class="dh-time" :title="fullTime(it.created_at)">{{ timeAgo(it.created_at) }}</span>
                <span class="dh-abs">{{ fullTime(it.created_at) }}</span>
                <span v-if="it.id === currentId" class="dh-cur" title="编辑器当前内容与这条快照一致">当前</span>
              </div>
              <div class="dh-title">{{ it.title || '（无标题）' }}</div>
              <div class="dh-meta">
                <span class="dh-fmt" :class="it.format === 'blocks' ? 'is-blocks' : 'is-html'">
                  {{ it.format === 'blocks' ? '⧉ 块' : '¶ 整篇' }}
                </span>
                <span v-if="it.summary" class="dh-sum">{{ it.summary }}</span>
                <span v-if="it.saved_by" class="dh-by">{{ it.saved_by }} 保存</span>
              </div>
              <div class="dh-acts">
                <button class="dh-btn" @click="togglePreview(it)">{{ previewId === it.id ? '收起预览' : '预览' }}</button>
                <button class="dh-btn" @click="askRestore(it)">恢复</button>
                <button class="dh-btn danger" @click="askDelete(it)">删除</button>
              </div>
              <div v-if="previewId === it.id" class="dh-preview">
                <div v-if="detailLoading === it.id" class="dh-tip">载入内容…</div>
                <template v-else-if="details[it.id]">
                  <HrBlocksRenderer v-if="isBlocksSnapshot(details[it.id])" :blocks="details[it.id].content_blocks || []" />
                  <HrDocRenderer v-else :html="details[it.id].content_html || ''" />
                </template>
                <div v-else class="dh-tip err">内容载入失败，请重试</div>
              </div>
            </div>
          </template>
        </div>
      </aside>
    </Transition>
  </Teleport>

  <!-- 恢复第一道确认：说明恢复只是载入编辑器，可反悔 -->
  <ConfirmDialog
    v-model:open="restoreOpen"
    title="恢复这个版本？"
    message="快照内容会载入编辑器（格式随快照自动切换），点「保存草稿」后才真正生效；不保存离开即放弃。"
    confirm-text="恢复"
    tone="warn"
    @confirm="onRestoreConfirm"
  />
  <!-- 有未保存修改时的第二道确认（口径 §6：恢复会覆盖当前编辑内容） -->
  <ConfirmDialog
    v-model:open="overwriteOpen"
    title="覆盖未保存的修改？"
    message="编辑器里有尚未保存的修改，恢复会用快照内容覆盖它们，且无法找回。确定继续？"
    confirm-text="仍然恢复"
    tone="danger"
    @confirm="doRestore"
  />
  <!-- 删除确认 -->
  <ConfirmDialog
    v-model:open="deleteOpen"
    title="删除这条草稿历史？"
    :message="deleteTarget ? `${fullTime(deleteTarget.created_at)} 的快照删除后无法恢复（不影响已发布版本和当前草稿）。` : ''"
    confirm-text="删除"
    tone="danger"
    :busy="deleting"
    @confirm="doDelete"
  />
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import ConfirmDialog from './ui/ConfirmDialog.vue'
import HrBlocksRenderer from './HrBlocksRenderer.vue'
import HrDocRenderer from './HrDocRenderer.vue'
import { articleApi } from '../../bridge/api'
import { timeAgo, fullTime } from '../utils/messageTime'
import { useToast } from '../composables/useToast'

const props = defineProps({
  open: { type: Boolean, default: false },
  documentId: { type: Number, required: true },
  // 父组件的未保存标记：恢复前决定要不要多拦一道覆盖确认
  dirty: { type: Boolean, default: false },
  // 编辑器当前内容 { title, format: 'blocks'|'html', html, blocks }，「当前」标记比对用
  current: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['update:open', 'restore'])
const toast = useToast()

const items = ref([])
const listLoading = ref(false)
const listError = ref('')
// 详情缓存：id → 完整快照（预览 /「当前」比对 / 恢复三处共用，抽屉开着期间不重复拉）
const details = reactive({})
const detailLoading = ref(null)
const previewId = ref(null)

const restoreOpen = ref(false)
const overwriteOpen = ref(false)
const restoreTarget = ref(null)
const deleteOpen = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)

function close() {
  emit('update:open', false)
}

// 每次打开都重拉：别的 admin 可能刚保存/删除过
watch(() => props.open, (v) => { if (v) loadList() })

async function loadList() {
  listLoading.value = true
  listError.value = ''
  previewId.value = null
  try {
    const r = await articleApi.adminDraftVersions(props.documentId)
    items.value = r.data || []
    // 静默预拉最新一条的详情，供「当前」标记比对；失败不打扰（标记缺失可接受）
    if (items.value.length) fetchDetail(items.value[0].id, true)
  } catch (e) {
    listError.value = '加载失败：' + (e.response?.data?.detail || e.message)
  } finally {
    listLoading.value = false
  }
}

async function fetchDetail(id, silent = false) {
  if (details[id]) return details[id]
  detailLoading.value = id
  try {
    const r = await articleApi.adminDraftVersion(props.documentId, id)
    details[id] = r.data
    return r.data
  } catch (e) {
    if (!silent) toast.show('载入快照失败：' + (e.response?.data?.detail || e.message), 'err')
    return null
  } finally {
    if (detailLoading.value === id) detailLoading.value = null
  }
}

// 快照格式判别：详情里 content_blocks 非 null = 块格式；列表条目退回 format 字段
function isBlocksSnapshot(d) {
  return d.content_blocks != null || d.format === 'blocks'
}

// 递归按键名排序再序列化：块数组比对不受 JSON 键序影响
// （编辑器里的块经过 reactive 代理和深拷贝，键序未必和后端 JSONB 回读一致）
function stableStr(v) {
  if (Array.isArray(v)) return '[' + v.map(stableStr).join(',') + ']'
  if (v && typeof v === 'object') {
    return '{' + Object.keys(v).sort().map(k => JSON.stringify(k) + ':' + stableStr(v[k])).join(',') + '}'
  }
  return JSON.stringify(v)
}

// 「当前」标记：最新一条快照的三元组（标题 + 格式 + 正文）与编辑器一致才亮
const currentId = computed(() => {
  const first = items.value[0]
  if (!first) return null
  const d = details[first.id]
  if (!d) return null
  const cur = props.current || {}
  if ((d.title || '') !== (cur.title || '')) return null
  const blocks = isBlocksSnapshot(d)
  if (blocks !== (cur.format === 'blocks')) return null
  if (blocks) return stableStr(d.content_blocks || []) === stableStr(cur.blocks || []) ? first.id : null
  return (d.content_html || '') === (cur.html || '') ? first.id : null
})

function togglePreview(it) {
  if (previewId.value === it.id) { previewId.value = null; return }
  previewId.value = it.id
  fetchDetail(it.id)
}

function askRestore(it) {
  restoreTarget.value = it
  restoreOpen.value = true
}
function onRestoreConfirm() {
  restoreOpen.value = false // ConfirmDialog 不自关，父级负责收
  if (props.dirty) { overwriteOpen.value = true; return }
  doRestore()
}
async function doRestore() {
  overwriteOpen.value = false
  const it = restoreTarget.value
  if (!it) return
  const d = await fetchDetail(it.id)
  if (!d) return // 拉详情失败已 toast，别拿空内容去覆盖编辑器
  emit('restore', d)
}

function askDelete(it) {
  deleteTarget.value = it
  deleteOpen.value = true
}
async function doDelete() {
  const it = deleteTarget.value
  if (!it) return
  deleting.value = true
  try {
    await articleApi.adminDraftVersionDelete(props.documentId, it.id)
    deleteOpen.value = false
    if (previewId.value === it.id) previewId.value = null
    delete details[it.id]
    toast.show('已删除该草稿版本', 'ok')
    await loadList()
  } catch (e) {
    toast.show('删除失败：' + (e.response?.data?.detail || e.message), 'err')
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
/* 层级：盖过顶栏(30)/导航(200)，让 ConfirmDialog(1000) 压在抽屉上 */
.dh-mask { position: fixed; inset: 0; z-index: 900; background: rgba(15, 23, 42, 0.32); }
.dh-drawer {
  position: fixed; top: 0; right: 0; bottom: 0; z-index: 901;
  width: min(460px, 94vw);
  display: flex; flex-direction: column;
  background: #fff;
  border-left: 1px solid rgba(99, 102, 241, 0.16);
  box-shadow: -12px 0 40px rgba(15, 23, 42, 0.14);
}
.dh-fade-enter-active, .dh-fade-leave-active { transition: opacity 0.2s; }
.dh-fade-enter-from, .dh-fade-leave-to { opacity: 0; }
.dh-slide-enter-active, .dh-slide-leave-active { transition: transform 0.22s ease; }
.dh-slide-enter-from, .dh-slide-leave-to { transform: translateX(100%); }

.dh-head {
  display: flex; align-items: center; gap: 8px;
  padding: 14px 18px; border-bottom: 1px solid rgba(99, 102, 241, 0.12);
  font-size: 14px; font-weight: 700; color: var(--t);
}
.dh-head-title { flex: 1; }
.dh-count { font-size: 12px; font-weight: 500; color: var(--t4); }
.dh-close {
  width: 28px; height: 28px; border-radius: 7px; border: 1px solid rgba(99, 102, 241, 0.18);
  background: #fff; color: var(--t4); cursor: pointer; font-size: 12px; flex-shrink: 0;
}
.dh-close:hover { color: #dc2626; border-color: rgba(220, 38, 38, 0.3); }

.dh-body { flex: 1; overflow-y: auto; padding: 14px 18px 28px; }
.dh-tip { font-size: 13px; color: var(--t4); line-height: 1.7; padding: 8px 2px; }
.dh-tip.err { color: #b91c1c; }
.dh-retry {
  margin-left: 8px; padding: 3px 10px; border-radius: 7px; border: 1px solid rgba(99, 102, 241, 0.25);
  background: #fff; color: var(--v); font-size: 12px; cursor: pointer; font-family: inherit;
}

.dh-item {
  padding: 12px 14px; margin-bottom: 10px;
  border: 1px solid rgba(99, 102, 241, 0.14); border-radius: 12px;
  background: rgba(99, 102, 241, 0.02);
}
.dh-row1 { display: flex; align-items: baseline; gap: 8px; }
.dh-time { font-size: 13px; font-weight: 700; color: var(--t2); }
.dh-abs { font-size: 11px; color: var(--t5); flex: 1; }
.dh-cur {
  font-size: 11px; font-weight: 700; color: #059669;
  background: rgba(16, 185, 129, 0.12); border-radius: 6px; padding: 1px 8px;
  flex-shrink: 0;
}
.dh-title {
  margin-top: 4px; font-size: 13px; color: var(--t);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.dh-meta { display: flex; align-items: center; gap: 10px; margin-top: 5px; font-size: 12px; color: var(--t4); }
.dh-fmt { font-size: 11px; font-weight: 700; border-radius: 6px; padding: 1px 8px; }
.dh-fmt.is-blocks { color: var(--v); background: rgba(99, 102, 241, 0.1); }
.dh-fmt.is-html { color: #0369a1; background: rgba(14, 165, 233, 0.1); }
.dh-acts { display: flex; gap: 8px; margin-top: 9px; }
.dh-btn {
  padding: 4px 12px; border-radius: 8px; border: 1px solid rgba(99, 102, 241, 0.2);
  background: #fff; color: var(--t2); font-size: 12px; font-weight: 600; font-family: inherit;
  cursor: pointer; transition: all 0.15s;
}
.dh-btn:hover { border-color: var(--v); color: var(--v); }
.dh-btn.danger:hover { border-color: rgba(220, 38, 38, 0.45); color: #dc2626; }

/* 内嵌预览：只读缩小版，限高滚动，别把抽屉撑爆 */
.dh-preview {
  margin-top: 10px; padding: 12px 14px;
  border: 1px solid rgba(99, 102, 241, 0.12); border-radius: 10px;
  background: #fff; max-height: 46vh; overflow-y: auto;
  font-size: 13px;
}
.dh-preview :deep(img) { max-width: 100%; }
</style>
