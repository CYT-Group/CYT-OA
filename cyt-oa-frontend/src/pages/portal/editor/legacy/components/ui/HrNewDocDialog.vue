<!--
  HrNewDocDialog — 新建 HR 文档时的「选择开始方式」（v1.23）

  为什么要有这一步：用户上传原制度文档是最常见的路径，不该先让人面对一张空白编辑器
  再自己去找「智能排版」。所以新建先问一次：

    ① 上传制度文档 → 选完文件直接 AI 排版 → 进编辑器时已经是排好版的内容
    ② 手动创建     → 空白编辑器（原流程）

  选错了也能反悔：编辑器工具栏的「智能排版」随时可以上传，默认就是「上传文档」页。

  用法：
    <HrNewDocDialog v-model:open="newStartOpen"
                    @apply="blocks => { applyLayout(blocks); newStartOpen = false }"
                    @manual="newStartOpen = false"
                    @cancel="router.push('/portal/hr-documents')" />
-->
<template>
  <Teleport to="body">
    <Transition name="nd-fade">
      <div v-if="open" class="portal-editor-floating nd-mask" @click.self="cancel">
        <div class="nd-panel" role="dialog" aria-label="新建文档">
          <header class="nd-head">
            <div class="nd-head-text">
              <h3>新建文档</h3>
              <p>选一种方式开始 —— 之后随时可以切换</p>
            </div>
            <button class="nd-close" type="button" aria-label="关闭" title="取消新建" :disabled="busy" @click="cancel">✕</button>
          </header>

          <div class="nd-cards">
            <!-- ① 上传文档 → 自动 AI 排版 -->
            <button class="nd-card" type="button" :disabled="busy" @click="pickFile">
              <span class="nd-icon nd-icon-upload" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor"
                     stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 16V4" /><path d="m7 9 5-5 5 5" />
                  <path d="M4 16v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" />
                </svg>
              </span>
              <span class="nd-title">上传文档</span>
              <span class="nd-desc">AI 自动识别章节、匹配合适的版式，约 30 秒</span>
              <span class="nd-meta">支持 docx / txt / md，不超过 10MB</span>
              <input ref="fileInput" type="file" accept=".docx,.pdf,.txt,.md" hidden @change="onFile">
            </button>

            <!-- ② 手动创建 → 空白编辑器 -->
            <button class="nd-card" type="button" :disabled="busy" @click="manual">
              <span class="nd-icon nd-icon-manual" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor"
                     stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 20h9" /><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
                </svg>
              </span>
              <span class="nd-title">手动创建</span>
              <span class="nd-desc">从空白开始，自己写内容或用块编辑器排版</span>
              <span class="nd-meta">随时可点工具栏「✨ 智能排版」上传</span>
            </button>
          </div>

          <div v-if="busy || error" class="nd-status" :class="{ err: !!error }">
            <span v-if="busy" class="nd-spinner" aria-hidden="true"></span>
            <span>{{ busy ? '正在提交…' : error }}</span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import { hrDocApi } from '../../../bridge/api'

defineProps({ open: { type: Boolean, default: false } })
const emit = defineEmits(['update:open', 'submitted', 'manual', 'cancel'])

const fileInput = ref(null)
const busy = ref(false)
const error = ref('')



function pickFile() {
  if (busy.value) return
  error.value = ''
  fileInput.value?.click()
}

async function onFile(e) {
  const f = e.target.files && e.target.files[0]
  // 清空 value：否则再次选同一个文件不触发 change
  e.target.value = ''
  if (!f) return
  if (f.size > 10 * 1024 * 1024) {
    error.value = '文件超过 10MB，请拆分后再试'
    return
  }
  busy.value = true
  error.value = ''
  try {
    // v1.24 改成异步任务：提交后立刻关闭弹窗，进度和结果在任务中心看
    const r = await hrDocApi.createLayoutJobFile(f)
    emit('submitted', r.data)
  } catch (err) {
    error.value = err.response?.data?.detail || err.message || '提交失败，请重试'
  } finally {
    busy.value = false
  }
}

function manual() {
  if (!busy.value) emit('manual')
}

function cancel() {
  if (!busy.value) emit('cancel')
}
</script>

<style scoped>
.nd-mask {
  position: fixed;
  inset: 0;
  z-index: 1250;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(13, 16, 38, 0.42);
  backdrop-filter: blur(4px);
}
.nd-panel {
  width: min(680px, 100%);
  padding: 26px 26px 18px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 24px 64px rgba(13, 16, 38, 0.22);
  /* 裁掉任何可能溢出的子元素，避免底部出现多余色块 */
  overflow: hidden;
}

.nd-head {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}
.nd-head-text { flex: 1; min-width: 0; }
.nd-head h3 { margin: 0 0 5px; font-size: 17px; font-weight: 700; color: var(--t, #0d1026); }
.nd-head p { margin: 0; font-size: 12.5px; color: var(--t3, #5a5f7a); }
.nd-close {
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--t4, #8c92ab);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.nd-close:hover:not(:disabled) { background: rgba(99, 102, 241, 0.08); color: var(--t, #0d1026); }
.nd-close:disabled { opacity: 0.4; cursor: not-allowed; }

.nd-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.nd-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 20px 18px 16px;
  border: 1.5px solid rgba(99, 102, 241, 0.16);
  border-radius: 14px;
  background: #fff;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.16s, box-shadow 0.16s, transform 0.16s;
}
.nd-card:hover:not(:disabled) {
  border-color: rgba(99, 102, 241, 0.5);
  box-shadow: 0 8px 22px rgba(99, 102, 241, 0.12);
  transform: translateY(-2px);
}
.nd-card:disabled { opacity: 0.55; cursor: not-allowed; }

.nd-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  margin-bottom: 4px;
  border-radius: 12px;
}
.nd-icon-upload { background: rgba(99, 102, 241, 0.10); color: var(--v, #6366f1); }
.nd-icon-manual { background: rgba(140, 146, 171, 0.12); color: #5a5f7a; }

.nd-title { font-size: 14.5px; font-weight: 700; color: var(--t, #0d1026); }
.nd-desc { font-size: 12.5px; line-height: 1.6; color: var(--t3, #5a5f7a); }
.nd-meta { margin-top: 2px; font-size: 11.5px; color: var(--t4, #8c92ab); }

.nd-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 9px;
  background: rgba(99, 102, 241, 0.06);
  font-size: 12.5px;
  color: #4338ca;
}
.nd-status.err { background: rgba(220, 38, 38, 0.06); color: #b91c1c; }
.nd-spinner {
  width: 13px;
  height: 13px;
  border: 2px solid rgba(99, 102, 241, 0.25);
  border-top-color: var(--v, #6366f1);
  border-radius: 50%;
  animation: nd-spin 0.7s linear infinite;
}
@keyframes nd-spin { to { transform: rotate(360deg); } }

.nd-fade-enter-active, .nd-fade-leave-active { transition: opacity 0.18s; }
.nd-fade-enter-active .nd-panel, .nd-fade-leave-active .nd-panel { transition: transform 0.18s; }
.nd-fade-enter-from, .nd-fade-leave-to { opacity: 0; }
.nd-fade-enter-from .nd-panel, .nd-fade-leave-to .nd-panel { transform: translateY(8px) scale(0.985); }
</style>
