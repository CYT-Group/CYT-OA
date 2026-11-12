import { computed, ref } from 'vue'

export type HrSyncId = string | number
export interface HrSyncIssue { employeeId: HrSyncId; name: string; reason: string }
export interface HrSyncConflict {
  employeeId: HrSyncId
  name: string
  reason?: string
  field?: string
  currentValue?: string | number | null
  upstreamValue?: string | number | null
}
export interface HrSyncResult {
  scanned: number
  requested: number
  succeeded: number
  failed: number
  skipped: number
  unchanged: number
  updatedEmployees: number
  updatedFields: number
  failures: HrSyncIssue[]
  skippedEmployees: HrSyncIssue[]
  conflicts: HrSyncConflict[]
  checkedAt: string
}
export interface HrSyncRun {
  id: HrSyncId
  status: 'running' | 'success' | 'partial' | 'failed'
  stage: 'directory' | 'hr' | 'complete'
  actor: string | null
  message: string | null
  createdAt: string
  finishedAt: string | null
  directory: { created: number; updated: number; departments: number; conflicts: number; missing: number } | null
  result: HrSyncResult | null
}

/** 只在已知任务运行时轮询；瞬时读取失败不等于任务失败。 */
export function createHrSyncTracker(options: {
  canStart: () => boolean
  start: () => Promise<HrSyncRun>
  status: (id?: HrSyncId) => Promise<HrSyncRun | null>
  onFinished?: (run: HrSyncRun) => void | Promise<void>
}) {
  const run = ref<HrSyncRun | null>(null)
  const starting = ref(false), reading = ref(false)
  const startError = ref(''), statusError = ref('')
  const syncing = computed(() => starting.value || run.value?.status === 'running')
  let disposed = false, generation = 0
  let timer: ReturnType<typeof setTimeout> | undefined
  let notifiedId: string | undefined
  const errorText = (error: unknown, fallback: string) => error instanceof Error ? error.message : fallback

  function stopTimer() {
    if (timer !== undefined) clearTimeout(timer)
    timer = undefined
  }
  function schedule(delay: number) {
    stopTimer()
    if (!disposed && run.value?.status === 'running') timer = setTimeout(refresh, delay)
  }
  function accept(next: HrSyncRun, live: boolean) {
    if (!next || next.id == null || !['running', 'success', 'partial', 'failed'].includes(next.status) || !['directory', 'hr', 'complete'].includes(next.stage))
      throw new Error('同步记录格式不完整，请刷新页面后重新读取。')
    run.value = next
    if (next.status === 'running') schedule(3000)
    else {
      stopTimer()
      if (live && notifiedId !== String(next.id)) {
        notifiedId = String(next.id)
        void Promise.resolve(options.onFinished?.(next)).catch(() => {})
      }
    }
  }
  async function refresh() {
    if (disposed) return
    stopTimer()
    const request = ++generation
    const previous = run.value
    reading.value = true
    statusError.value = ''
    try {
      const next = await options.status(previous?.status === 'running' ? previous.id : undefined)
      if (disposed || request !== generation) return
      if (!next && previous?.status === 'running') {
        statusError.value = '暂未读到当前任务，后台是否完成尚未确认。请重新读取同步记录。'
        schedule(5000)
      } else if (next) accept(next, previous?.status === 'running' && String(previous.id) === String(next.id))
      else run.value = null
    } catch (error) {
      if (disposed || request !== generation) return
      statusError.value = errorText(error, '同步记录读取失败，请重新读取。')
      schedule(5000)
    } finally {
      if (!disposed && request === generation) reading.value = false
    }
  }
  async function start() {
    if (disposed || syncing.value || reading.value || !options.canStart()) return
    stopTimer()
    ++generation
    starting.value = true
    startError.value = statusError.value = ''
    try {
      const next = await options.start()
      if (!disposed) accept(next, true)
    } catch (error) {
      if (disposed) return
      startError.value = errorText(error, '同步启动未确认，请先重新读取同步记录。')
      await refresh()
      if (run.value?.status === 'running') startError.value = ''
    } finally {
      if (!disposed) starting.value = false
    }
  }
  function dispose() {
    disposed = true
    ++generation
    stopTimer()
  }
  return { run, starting, reading, syncing, startError, statusError, start, refresh, dispose }
}
