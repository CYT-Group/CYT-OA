<!--
  HrLayoutJobsDrawer — AI 排版任务中心（v1.23）

  为什么要有它：长文档排版要跑 2 分钟，前台不能一直转圈。
  提交后任务在后台跑，这里看进度、看结果、点「应用」写进编辑器。

  设计取舍：
  - 右侧抽屉而不是独立页面 —— 任务是为「当前这篇文档」服务的，不该跳走
  - 列表只拉状态（不带 blocks），点「预览/应用」才取一次详情，省流量
  - 完成后不自动应用 —— 覆盖正文是破坏性操作，必须用户点一下
-->
<template>
  <Teleport to="body">
    <Transition name="jd-fade">
      <div v-if="open" class="portal-editor-floating jd-mask" @click.self="close">
        <aside class="jd-panel" role="dialog" aria-label="排版任务">
          <header class="jd-head">
            <div class="jd-head-text">
              <h3>排版任务<span v-if="activeCount" class="jd-badge">{{ activeCount }}</span></h3>
              <p>后台运行，完成后点「应用」写入编辑器</p>
            </div>
            <button class="jd-close" type="button" aria-label="关闭" @click="close">✕</button>
          </header>

          <div v-if="!jobs.length" class="jd-empty">
            <span class="jd-empty-icon" aria-hidden="true">🗂</span>
            <p>还没有排版任务</p>
            <span>在编辑器点「智能排版」提交一个</span>
          </div>

          <ul v-else class="jd-list">
            <li v-for="job in jobs" :key="job.id" class="jd-item" :class="'is-' + job.status">
              <!-- 状态 + 标题 -->
              <div class="jd-item-top">
                <span class="jd-status" :class="'st-' + job.status" aria-hidden="true">
                  <span v-if="job.status === 'running' || job.status === 'pending'" class="jd-spin"></span>
                  <template v-else>{{ job.status === 'succeeded' ? '✓' : '✕' }}</template>
                </span>
                <div class="jd-item-title">
                  <strong>{{ job.source_name || '未命名文本' }}</strong>
                  <span class="jd-item-meta">
                    {{ job.input_chars }} 字
                    <template v-if="job.status === 'succeeded'"> · {{ job.block_count }} 个块</template>
                    <template v-if="elapsed(job)"> · {{ elapsed(job) }}</template>
                  </span>
                </div>
                <span class="jd-tag">{{ sourceLabel(job.source_type) }}</span>
              </div>

              <!-- 进行中：进度条 -->
              <div v-if="job.status === 'pending' || job.status === 'running'" class="jd-progress">
                <div class="jd-bar"><i :style="{ width: Math.max(4, job.progress) + '%' }"></i></div>
                <span class="jd-progress-text">{{ job.stage || '排队中' }}</span>
              </div>

              <!-- 失败：错误 -->
              <p v-else-if="job.status === 'failed'" class="jd-error">{{ job.error || '排版失败' }}</p>

              <!-- 完成：说明 -->
              <p v-else-if="job.summary" class="jd-summary">{{ job.summary }}</p>

              <!-- 预览区 -->
              <div v-if="previewId === job.id && detail[job.id]" class="jd-preview">
                <HrBlocksRenderer :blocks="detail[job.id]" :disable-navigation="true" />
              </div>

              <!-- 操作 -->
              <div class="jd-actions">
                <template v-if="job.status === 'succeeded'">
                  <button class="jd-btn" type="button" @click="togglePreview(job)">
                    {{ previewId === job.id ? '收起' : '预览' }}
                  </button>
                  <button class="jd-btn primary" type="button" :disabled="loadingId === job.id" @click="apply(job)">
                    {{ loadingId === job.id ? '应用中…' : '应用到编辑器' }}
                  </button>
                </template>
                <template v-else-if="job.status === 'failed'">
                  <button class="jd-btn" type="button" :disabled="loadingId === job.id" @click="$emit('retry', job.id)">
                    重新提交
                  </button>
                </template>
                <span class="jd-spacer"></span>
                <button
                  v-if="job.status === 'succeeded' || job.status === 'failed'"
                  class="jd-btn ghost" type="button" @click="$emit('remove', job.id)"
                >删除</button>
              </div>
            </li>
          </ul>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import HrBlocksRenderer from '../HrBlocksRenderer.vue'
import { hrDocApi } from '../../../bridge/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  jobs: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:open', 'apply', 'retry', 'remove'])

const previewId = ref(null)
const detail = ref({})       // job.id → blocks（点预览/应用时才拉）
const loadingId = ref(null)

const activeCount = computed(() =>
  props.jobs.filter(j => j.status === 'pending' || j.status === 'running').length
)

function close() {
  emit('update:open', false)
}

function sourceLabel(t) {
  return { upload: '上传', paste: '粘贴', current: '当前文档' }[t] || '文本'
}

function elapsed(job) {
  if (!job.created_at) return ''
  const end = job.finished_at ? new Date(job.finished_at) : new Date()
  const sec = Math.max(0, Math.round((end - new Date(job.created_at)) / 1000))
  if (sec < 60) return `${sec} 秒`
  return `${Math.floor(sec / 60)} 分 ${sec % 60} 秒`
}

async function ensureDetail(job) {
  if (detail.value[job.id]) return detail.value[job.id]
  const r = await hrDocApi.getLayoutJob(job.id)
  detail.value = { ...detail.value, [job.id]: r.data?.blocks || [] }
  return detail.value[job.id]
}

async function togglePreview(job) {
  if (previewId.value === job.id) {
    previewId.value = null
    return
  }
  try {
    await ensureDetail(job)
    previewId.value = job.id
  } catch (e) {
    // 预览失败不阻塞主流程，用户还能点「应用」再试
    console.warn('取任务详情失败', e)
  }
}

async function apply(job) {
  loadingId.value = job.id
  try {
    const blocks = await ensureDetail(job)
    emit('apply', { job, blocks })
  } finally {
    loadingId.value = null
  }
}

// 关闭时收起预览，避免下次打开还挂着一个大块
watch(() => props.open, (v) => { if (!v) previewId.value = null })
</script>

<style scoped>
.jd-mask {
  position: fixed;
  inset: 0;
  z-index: 1240;
  display: flex;
  justify-content: flex-end;
  background: rgba(13, 16, 38, 0.36);
  backdrop-filter: blur(2px);
}
.jd-panel {
  display: flex;
  flex-direction: column;
  width: min(480px, 100%);
  height: 100%;
  background: #fff;
  box-shadow: -12px 0 40px rgba(13, 16, 38, 0.16);
  overflow: hidden;
}

.jd-head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid rgba(99, 102, 241, 0.1);
}
.jd-head-text { flex: 1; min-width: 0; }
.jd-head h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 4px;
  font-size: 15.5px;
  font-weight: 700;
  color: var(--t, #0d1026);
}
.jd-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--v, #6366f1);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}
.jd-head p { margin: 0; font-size: 12px; color: var(--t3, #5a5f7a); }
.jd-close {
  flex: 0 0 auto;
  width: 28px; height: 28px;
  border: none; border-radius: 8px;
  background: transparent; color: var(--t4, #8c92ab);
  font-size: 13px; cursor: pointer;
}
.jd-close:hover { background: rgba(99, 102, 241, 0.08); color: var(--t, #0d1026); }

.jd-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 40px 20px;
  color: var(--t4, #8c92ab);
  font-size: 12.5px;
}
.jd-empty-icon { font-size: 30px; opacity: 0.6; margin-bottom: 4px; }
.jd-empty p { margin: 0; font-size: 13.5px; font-weight: 600; color: var(--t2, #2d3258); }

.jd-list {
  flex: 1;
  overflow-y: auto;
  margin: 0;
  padding: 12px 14px 20px;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.jd-item {
  padding: 13px 14px;
  border: 1px solid rgba(99, 102, 241, 0.12);
  border-radius: 12px;
  background: #fff;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.jd-item:hover { border-color: rgba(99, 102, 241, 0.26); box-shadow: 0 3px 12px rgba(99, 102, 241, 0.07); }
.jd-item.is-failed { border-color: rgba(220, 38, 38, 0.2); }
.jd-item.is-succeeded { border-color: rgba(34, 197, 94, 0.22); }

.jd-item-top { display: flex; align-items: flex-start; gap: 9px; }
.jd-status {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px; height: 20px;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 700;
}
.st-pending, .st-running { background: rgba(99, 102, 241, 0.12); color: var(--v, #6366f1); }
.st-succeeded { background: rgba(34, 197, 94, 0.14); color: #16a34a; }
.st-failed { background: rgba(220, 38, 38, 0.12); color: #dc2626; }
.jd-spin {
  width: 10px; height: 10px;
  border: 1.6px solid rgba(99, 102, 241, 0.3);
  border-top-color: var(--v, #6366f1);
  border-radius: 50%;
  animation: jd-spin 0.75s linear infinite;
}
@keyframes jd-spin { to { transform: rotate(360deg); } }

.jd-item-title { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.jd-item-title strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--t, #0d1026);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.jd-item-meta { font-size: 11.5px; color: var(--t4, #8c92ab); }
.jd-tag {
  flex: 0 0 auto;
  padding: 1px 7px;
  border-radius: 6px;
  background: rgba(99, 102, 241, 0.07);
  color: var(--v, #6366f1);
  font-size: 11px;
}

.jd-progress { margin: 10px 0 2px; }
.jd-bar {
  height: 5px;
  border-radius: 3px;
  background: rgba(99, 102, 241, 0.1);
  overflow: hidden;
}
.jd-bar i {
  display: block;
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
  transition: width 0.4s ease;
}
.jd-progress-text { display: block; margin-top: 5px; font-size: 11.5px; color: var(--t3, #5a5f7a); }

.jd-error {
  margin: 9px 0 0;
  padding: 7px 9px;
  border-radius: 7px;
  background: rgba(220, 38, 38, 0.05);
  color: #b91c1c;
  font-size: 11.5px;
  line-height: 1.55;
}
.jd-summary {
  margin: 9px 0 0;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--t3, #5a5f7a);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.jd-preview {
  margin-top: 10px;
  padding: 14px 16px;
  max-height: 320px;
  overflow-y: auto;
  border-radius: 9px;
  background: #f8f9fd;
  border: 1px solid rgba(99, 102, 241, 0.1);
}

.jd-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 11px;
}
.jd-spacer { flex: 1; }
.jd-btn {
  height: 29px;
  padding: 0 12px;
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 8px;
  background: #fff;
  color: var(--t2, #2d3258);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.jd-btn:hover:not(:disabled) { border-color: rgba(99, 102, 241, 0.42); }
.jd-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.jd-btn.primary {
  border-color: transparent;
  background: var(--v, #6366f1);
  color: #fff;
  font-weight: 600;
}
.jd-btn.primary:hover:not(:disabled) { background: #4f46e5; }
.jd-btn.ghost {
  border-color: transparent;
  background: transparent;
  color: var(--t4, #8c92ab);
}
.jd-btn.ghost:hover:not(:disabled) { color: #dc2626; background: rgba(220, 38, 38, 0.05); }

.jd-fade-enter-active, .jd-fade-leave-active { transition: opacity 0.18s; }
.jd-fade-enter-active .jd-panel, .jd-fade-leave-active .jd-panel { transition: transform 0.22s ease; }
.jd-fade-enter-from, .jd-fade-leave-to { opacity: 0; }
.jd-fade-enter-from .jd-panel, .jd-fade-leave-to .jd-panel { transform: translateX(24px); }
</style>
