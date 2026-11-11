<!--
  HrLayoutDialog — 提交「智能排版」任务（v1.23 改为异步）

  三种来源，按场景自动选默认 tab：
    1. 当前文档 —— 编辑器里已有内容时默认选中，直接排版，不用重新粘贴
    2. 上传文档 —— 新建空文档时默认选中，支持 docx / txt / md
    3. 粘贴文字 —— 兜底

  🔴 提交后**立刻关闭**，不等 AI：长文档要跑 2 分钟，前台转圈体验很差。
  进度和结果在 HrLayoutJobsDrawer（任务中心）里看，点「应用」才写进编辑器。

  用法：
    <HrLayoutDialog v-model:open="layoutOpen" :seed-text="layoutSeedText" @submitted="onJobSubmitted" />
-->
<template>
  <Teleport to="body">
    <Transition name="ld-fade">
      <div v-if="open" class="portal-editor-floating ld-mask" @click.self="close">
        <div class="ld-panel" role="dialog" aria-label="智能排版">
          <header class="ld-head">
            <div class="ld-head-text">
              <h3>智能排版</h3>
              <p>AI 按章节拆块并匹配版式。提交后在后台运行，完成时通知你。</p>
            </div>
            <button class="ld-close" type="button" aria-label="关闭" @click="close">✕</button>
          </header>

          <div class="ld-input">
            <div class="ld-tabs" role="tablist">
              <button
                v-if="seedText"
                type="button" role="tab" :aria-selected="source === 'current'"
                :class="{ on: source === 'current' }" @click="source = 'current'"
              >当前文档</button>
              <button
                type="button" role="tab" :aria-selected="source === 'upload'"
                :class="{ on: source === 'upload' }" @click="source = 'upload'"
              >上传文档</button>
              <button
                type="button" role="tab" :aria-selected="source === 'paste'"
                :class="{ on: source === 'paste' }" @click="source = 'paste'"
              >粘贴文字</button>
            </div>

            <!-- 当前文档：直接排版编辑器里的内容 -->
            <div v-if="source === 'current'" class="ld-current">
              <div class="ld-current-hint">
                <span class="ld-current-icon" aria-hidden="true">✓</span>
                <span>将直接对编辑器里的当前内容排版，不需要重新粘贴。</span>
              </div>
              <div class="ld-current-meta">
                <span>{{ seedText.length }} 字 · {{ seedLines }} 段</span>
                <button v-if="seedCanExpand" type="button" class="ld-expand" @click="expanded = !expanded">
                  {{ expanded ? '收起' : '查看全文' }}
                </button>
              </div>
              <pre v-if="!seedBlockCount" class="ld-current-preview" :class="{ expanded }">{{ seedPreview }}</pre>
              <div class="ld-current-warn">
                <span aria-hidden="true">⚠</span>
                <span v-if="seedBlockCount">
                  将重新组织这 {{ seedBlockCount }} 个块的层级与版式；<strong>应用后可在工具栏点「撤销排版」还原</strong>。
                </span>
                <span v-else>应用后会替换当前正文；保存前关掉页面即可放弃。</span>
              </div>
            </div>

            <!-- 上传文档 -->
            <label v-else-if="source === 'upload'" class="ld-drop" :class="{ filled: !!fileName, busy: loading }">
              <input type="file" accept=".docx,.txt,.md" hidden @change="onFile">
              <span class="ld-drop-icon" aria-hidden="true">⬆</span>
              <span class="ld-drop-main">{{ fileName || '点击选择文档' }}</span>
              <span class="ld-drop-sub">{{ fileName ? '点击可重新选择' : '支持 docx / txt / md，不超过 10MB' }}</span>
            </label>

            <!-- 粘贴文字 -->
            <textarea
              v-else
              v-model="text"
              class="ld-textarea"
              :disabled="loading"
              placeholder="把制度全文粘贴到这里…&#10;&#10;例如：&#10;第一章 总则&#10;1.1 适用范围&#10;本制度适用于公司全体正式员工…"
            ></textarea>

            <div class="ld-meta">
              <span v-if="source === 'paste'">{{ text.length }} 字</span>
              <span v-else-if="source === 'current'">直接使用编辑器内容</span>
              <span v-else>{{ fileName ? '文件已选择' : '请选择文件' }}</span>
              <span v-if="error" class="ld-error">{{ error }}</span>
            </div>
          </div>

          <footer class="ld-foot">
            <button class="ld-btn" type="button" :disabled="loading" @click="close">取消</button>
            <button
              class="ld-btn primary" type="button"
              :disabled="loading || !canSubmit"
              @click="submit"
            >{{ loading ? '提交中…' : '提交排版' }}</button>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { hrDocApi } from '../../../bridge/api'
import { useToast } from '../../composables/useToast'

const props = defineProps({
  open: { type: Boolean, default: false },
  // 编辑器里的当前内容（纯文本）。非空时默认走「当前文档」，不用重新粘贴。
  seedText: { type: String, default: '' },
  // 当前内容已有的块数（>0 说明是老文档重排，要明确告知「会替换」并提示可撤销）
  seedBlockCount: { type: Number, default: 0 },
})
const emit = defineEmits(['update:open', 'submitted'])

const toast = useToast()

const source = ref('paste')   // current | upload | paste
const text = ref('')
const file = ref(null)
const loading = ref(false)
const error = ref('')
const expanded = ref(false)

const fileName = computed(() => (file.value ? file.value.name : ''))
const seedLines = computed(() =>
  (props.seedText || '').split('\n').filter(l => l.trim()).length
)
const seedCanExpand = computed(() => {
  const lines = (props.seedText || '').split('\n').filter(l => l.trim())
  return lines.length > 4 || (props.seedText || '').length > 160
})
const seedPreview = computed(() => {
  const raw = props.seedText || ''
  if (expanded.value) return raw
  const lines = raw.split('\n').filter(l => l.trim())
  let head = lines.slice(0, 4).join('\n')
  if (head.length > 160) head = head.slice(0, 160) + '…'
  else if (lines.length > 4) head += '\n…'
  return head
})
const canSubmit = computed(() => {
  if (source.value === 'current') return !!props.seedText
  if (source.value === 'upload') return !!file.value
  return !!text.value.trim()
})

// 打开时按场景选默认来源：有当前内容就用它，否则让用户上传原文档
watch(() => props.open, (v) => {
  if (!v) { error.value = ''; return }
  source.value = props.seedText ? 'current' : 'upload'
})

function close() {
  if (loading.value) return
  emit('update:open', false)
}

function onFile(e) {
  const f = e.target.files && e.target.files[0]
  if (!f) return
  if (f.size > 10 * 1024 * 1024) { error.value = '文件超过 10MB'; return }
  file.value = f
  error.value = ''
}

async function submit() {
  loading.value = true
  error.value = ''
  try {
    let r
    if (source.value === 'current') {
      r = await hrDocApi.createLayoutJob({
        text: props.seedText, source_type: 'current', source_name: '当前文档',
      })
    } else if (source.value === 'upload') {
      r = await hrDocApi.createLayoutJobFile(file.value)
    } else {
      r = await hrDocApi.createLayoutJob({
        text: text.value.trim(), source_type: 'paste', source_name: '粘贴文本',
      })
    }
    emit('submitted', r.data)
    emit('update:open', false)
    toast.show('已提交，后台排版中 —— 完成后在「任务」里应用', 'ok')
  } catch (e) {
    error.value = e.response?.data?.detail || e.message || '提交失败'
    toast.show('提交失败：' + error.value, 'err')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ld-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(13, 16, 38, 0.42);
  backdrop-filter: blur(4px);
}
.ld-panel {
  display: flex;
  flex-direction: column;
  width: min(720px, 100%);
  max-height: calc(100vh - 48px);
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 64px rgba(13, 16, 38, 0.22);
  overflow: hidden;
}

.ld-head {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px 22px 16px;
  border-bottom: 1px solid rgba(99, 102, 241, 0.10);
}
.ld-head-text { flex: 1; min-width: 0; }
.ld-head h3 { margin: 0 0 4px; font-size: 16px; font-weight: 700; color: var(--t, #0d1026); }
.ld-head p { margin: 0; font-size: 12.5px; line-height: 1.6; color: var(--t3, #5a5f7a); }
.ld-close {
  flex: 0 0 auto;
  width: 30px; height: 30px;
  border: none; border-radius: 8px;
  background: transparent; color: var(--t4, #8c92ab);
  font-size: 14px; cursor: pointer;
}
.ld-close:hover { background: rgba(99, 102, 241, 0.08); color: var(--t, #0d1026); }

.ld-input { padding: 16px 22px 18px; }

.ld-tabs {
  display: inline-flex;
  gap: 2px;
  margin-bottom: 14px;
  padding: 3px;
  border-radius: 10px;
  background: rgba(99, 102, 241, 0.06);
}
.ld-tabs button {
  height: 30px;
  padding: 0 14px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--t3, #5a5f7a);
  font-family: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.ld-tabs button:hover { color: var(--t, #0d1026); }
.ld-tabs button.on {
  background: #fff;
  color: var(--v, #6366f1);
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(13, 16, 38, 0.08);
}

.ld-current { border: 1px solid rgba(99, 102, 241, 0.16); border-radius: 10px; overflow: hidden; }
.ld-current-hint {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 10px 14px;
  background: rgba(34, 197, 94, 0.06);
  border-bottom: 1px solid rgba(34, 197, 94, 0.16);
  font-size: 12.5px;
  color: #166534;
}
.ld-current-icon { font-weight: 700; }
.ld-current-preview {
  margin: 0;
  max-height: 168px;
  overflow-y: auto;
  padding: 12px 16px;
  font-family: inherit;
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--t3, #5a5f7a);
  white-space: pre-wrap;
  word-break: break-word;
}
.ld-current-preview.expanded { max-height: min(40vh, 320px); }
.ld-current-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 9px 16px 0;
  font-size: 11.5px;
  color: var(--t4, #8c92ab);
}
.ld-expand {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--v, #6366f1);
  font-family: inherit;
  font-size: 11.5px;
  cursor: pointer;
}
.ld-expand:hover { text-decoration: underline; }
.ld-current-warn {
  display: flex;
  gap: 6px;
  padding: 8px 14px;
  border-top: 1px solid rgba(217, 119, 6, 0.16);
  background: rgba(217, 119, 6, 0.06);
  color: #92400e;
  font-size: 11.5px;
  line-height: 1.55;
}

.ld-drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 38px 20px;
  border: 1.5px dashed rgba(99, 102, 241, 0.28);
  border-radius: 12px;
  background: rgba(99, 102, 241, 0.025);
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.ld-drop:hover { border-color: rgba(99, 102, 241, 0.5); background: rgba(99, 102, 241, 0.05); }
.ld-drop.filled { border-style: solid; border-color: rgba(34, 197, 94, 0.4); background: rgba(34, 197, 94, 0.04); }
.ld-drop.busy { opacity: 0.6; pointer-events: none; }
.ld-drop-icon { font-size: 20px; color: var(--v, #6366f1); }
.ld-drop-main { font-size: 13.5px; font-weight: 600; color: var(--t, #0d1026); }
.ld-drop-sub { font-size: 12px; color: var(--t4, #8c92ab); }

.ld-textarea {
  width: 100%;
  height: min(42vh, 340px);
  padding: 14px 16px;
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 10px;
  font-family: inherit;
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--t, #0d1026);
  resize: vertical;
  box-sizing: border-box;
}
.ld-textarea:focus { outline: none; border-color: rgba(99, 102, 241, 0.5); box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08); }

.ld-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--t4, #8c92ab);
}
.ld-error { color: #dc2626; }

.ld-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 22px;
  border-top: 1px solid rgba(99, 102, 241, 0.10);
  background: #fcfcfe;
}
.ld-btn {
  height: 36px;
  padding: 0 16px;
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 9px;
  background: #fff;
  color: var(--t2, #2d3258);
  font-family: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.ld-btn:hover:not(:disabled) { border-color: rgba(99, 102, 241, 0.4); }
.ld-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ld-btn.primary {
  border-color: transparent;
  background: var(--v, #6366f1);
  color: #fff;
  font-weight: 600;
}
.ld-btn.primary:hover:not(:disabled) { background: #4f46e5; }

.ld-fade-enter-active, .ld-fade-leave-active { transition: opacity 0.18s; }
.ld-fade-enter-active .ld-panel, .ld-fade-leave-active .ld-panel { transition: transform 0.18s; }
.ld-fade-enter-from, .ld-fade-leave-to { opacity: 0; }
.ld-fade-enter-from .ld-panel, .ld-fade-leave-to .ld-panel { transform: translateY(8px) scale(0.985); }
</style>
