<!--
  HrLayoutJobsLauncher — 后台全局的「排版任务」入口（v1.24）

  为什么放这里：任务入口原本只在编辑器工具栏里，用户退出编辑器回列表就看不到进度了。
  挂到 AdminLayout（所有后台页的容器）后，任何后台页面都能看到任务角标、打开任务中心。

  「应用到编辑器」靠 inject 拿编辑器的 apply 函数：
  - 在编辑器页 → 直接灌进当前正文
  - 不在编辑器页 → 按钮提示「请先打开要应用的文档」（不误导用户）
-->
<template>
  <div class="ljl">
    <button
      v-if="!hideWhenEmpty || jobs.length"
      class="ljl-btn"
      :class="{ busy: activeCount > 0 }"
      type="button"
      :title="activeCount ? `${activeCount} 个排版任务进行中` : '排版任务中心'"
      @click="open = true"
    >
      <span class="ljl-icon" :class="{ spin: activeCount > 0 }" aria-hidden="true">🗂</span>
      <span class="ljl-text">排版任务</span>
      <span v-if="activeCount" class="ljl-badge" :title="`${activeCount} 个任务进行中`">{{ activeCount }}</span>
      <span
        v-else-if="unseenCount"
        class="ljl-dot"
        :title="`${unseenCount} 个任务已完成，点开查看`"
      ></span>
    </button>

    <HrLayoutJobsDrawer
      v-model:open="open"
      :jobs="jobs"
      @apply="onApply"
      @retry="onRetry"
      @remove="onRemove"
    />
  </div>
</template>

<script setup>
import { computed, inject, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import HrLayoutJobsDrawer from './HrLayoutJobsDrawer.vue'
import { hrDocApi } from '../../../bridge/api'
import { useToast } from '../../composables/useToast'

const toast = useToast()
const router = useRouter()
const route = useRoute()

// 编辑器页会 provide 这个函数；其它页面 inject 到 null
const applyToEditor = inject('hrLayoutApply', null)

defineProps({ hideWhenEmpty: { type: Boolean, default: false } })

const jobs = ref([])
const open = ref(false)
let timer = null
let loading = false

const activeCount = computed(() =>
  jobs.value.filter(j => j.status === 'pending' || j.status === 'running').length
)
// 绿点语义 =「有完成但还没看过的任务」。早先直接数 succeeded 总数，
// 结果只要历史上成功过一次就永久亮着、点开也不灭，等于没信息量。
const SEEN_KEY = 'cyt_layout_jobs_seen'
function loadSeen() {
  try {
    const raw = JSON.parse(localStorage.getItem(SEEN_KEY) || '[]')
    return Array.isArray(raw) ? raw : []
  } catch {
    return []
  }
}
const seenIds = ref(loadSeen())
const unseenCount = computed(() => {
  const seen = new Set(seenIds.value)
  return jobs.value.filter(j => j.status === 'succeeded' && !seen.has(j.id)).length
})

// 打开抽屉 = 看过了：把当前已完成的 id 全部记为已读，绿点随之熄灭
function markSeen() {
  const ids = jobs.value.filter(j => j.status === 'succeeded').map(j => j.id)
  if (!ids.length) return
  const merged = [...new Set([...seenIds.value, ...ids])].slice(-200)
  seenIds.value = merged
  try {
    localStorage.setItem(SEEN_KEY, JSON.stringify(merged))
  } catch {
    // 隐私模式下写不了 localStorage：绿点这次不灭，不影响功能
  }
}
watch(open, (v) => { if (v) markSeen() })

async function refresh() {
  if (loading) return
  loading = true
  try {
    const r = await hrDocApi.listLayoutJobs()
    jobs.value = r.data || []
  } catch {
    // 任务中心是辅助能力，拉不到列表不影响页面本身
  } finally {
    loading = false
    schedule()
  }
}

function schedule() {
  clearTimeout(timer)
  // 有任务在跑 4 秒一次；全完成 30 秒一次（只为了刷新角标）
  timer = setTimeout(refresh, activeCount.value > 0 ? 4000 : 30000)
}

/**
 * 应用排版结果。
 *
 * 之前用 inject 判断「在不在编辑器」，跨页/重定向场景拿不到上下文就退化成
 * 「请先打开文档」——用户点了没反应，体验很差。改成：
 *   ① 在编辑器页 → 派发全局事件，编辑器自己接收（不依赖组件树）
 *   ② 不在编辑器页 → 直接跳到新建页并带上 jobId，编辑器加载后自动应用
 * 这样「应用到编辑器」在任何页面都是有效动作。
 */
function onApply({ job, blocks }) {
  if (!Array.isArray(blocks) || !blocks.length) {
    toast.show('排版结果为空，无法应用', 'err')
    return
  }
  open.value = false
  if (applyToEditor) {
    applyToEditor(blocks)
    toast.show(`已应用 ${blocks.length} 个块；检查后再保存`, 'ok')
    return
  }
  if (isInEditor()) {
    window.dispatchEvent(new CustomEvent('hr-layout-apply', { detail: blocks }))
    toast.show(`已应用 ${blocks.length} 个块；检查后再保存`, 'ok')
    return
  }
  // 不在编辑器：跳过去，由编辑器读取 ?job= 自己应用
  router.push({ path: '/portal/hr-documents/new', query: { job: String(job.id) } })
}

function isInEditor() {
  return /^\/portal\/hr-documents\/(new|id\/\d+\/edit)$/.test(route.path)
}

async function onRetry(id) {
  try {
    await hrDocApi.retryLayoutJob(id)
    toast.show('已重新提交', 'ok')
    refresh()
  } catch (e) {
    toast.show('重试失败：' + (e.response?.data?.detail || e.message), 'err')
  }
}

async function onRemove(id) {
  try {
    await hrDocApi.deleteLayoutJob(id)
    jobs.value = jobs.value.filter(j => j.id !== id)
  } catch (e) {
    toast.show('删除失败：' + (e.response?.data?.detail || e.message), 'err')
  }
}

onMounted(refresh)
onUnmounted(() => clearTimeout(timer))

// 编辑器提交任务后会 router.push 之外没别的手段通知这里，靠轮询兜住
defineExpose({ refresh })
</script>

<style scoped>
.ljl { display: inline-flex; }

.ljl-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 9px;
  background: #fff;
  color: var(--t2, #2d3258);
  font-family: inherit;
  font-size: 12.5px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, box-shadow 0.15s;
}
.ljl-btn:hover { border-color: rgba(99, 102, 241, 0.42); box-shadow: 0 2px 8px rgba(99, 102, 241, 0.08); }
.ljl-btn.busy {
  border-color: rgba(99, 102, 241, 0.35);
  background: rgba(99, 102, 241, 0.05);
  color: var(--v, #6366f1);
  font-weight: 600;
}
.ljl-icon { font-size: 13px; }
.ljl-icon.spin { display: inline-block; animation: ljl-spin 1.8s linear infinite; }
@keyframes ljl-spin { to { transform: rotate(360deg); } }
.ljl-text { white-space: nowrap; }
.ljl-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 17px;
  height: 17px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--v, #6366f1);
  color: #fff;
  font-size: 10.5px;
  font-weight: 700;
}
/* 有已完成任务时用一个小绿点提示，不打扰但有迹可循 */
.ljl-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.16);
}
</style>
