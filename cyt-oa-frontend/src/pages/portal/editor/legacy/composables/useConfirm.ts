/**
 * 全局确认弹框 —— 替代原生 `confirm()` / `alert()`。
 *
 * 为什么不用原生：
 *   原生弹框由浏览器渲染（系统字体、无圆角、按钮文案是「确定/取消」英文环境还会变），
 *   和站内设计完全脱节，而且会阻塞主线程。
 *
 * 用法（需配合 App.vue 里挂载的 <GlobalConfirmHost />）：
 *   if (!(await confirmDialog({ title: '删除这一段？', tone: 'danger' }))) return
 *   const reason = await promptDialog({ title: '下架原因', commentLabel: '选填' })  // 取消返回 null
 *
 * 约定：同一时刻只允许一个弹框；新的请求会把上一个按「取消」结算，避免 Promise 永远挂着。
 */
import { reactive } from 'vue'

export type ConfirmTone = 'primary' | 'danger' | 'warn'

export interface ConfirmOptions {
  title?: string
  message?: string
  confirmText?: string
  cancelText?: string
  tone?: ConfirmTone
}

export interface PromptOptions extends ConfirmOptions {
  /** 给值则显示输入框（textarea 的 placeholder） */
  commentLabel: string
}

interface HostState {
  open: boolean
  mode: 'confirm' | 'prompt'
  title: string
  message: string
  confirmText: string
  cancelText: string
  tone: ConfirmTone
  commentLabel: string
}

export const confirmState = reactive<HostState>({
  open: false,
  mode: 'confirm',
  title: '',
  message: '',
  confirmText: '确认',
  cancelText: '取消',
  tone: 'primary',
  commentLabel: '',
})

let resolver: ((value: unknown) => void) | null = null

function openDialog(opts: ConfirmOptions & { commentLabel?: string }, mode: 'confirm' | 'prompt') {
  // 上一个还没结算就再开一个 → 先把上一个当「取消」，避免调用方 await 永远不返回
  if (resolver) { const prev = resolver; resolver = null; prev(mode === 'prompt' ? null : false) }
  Object.assign(confirmState, {
    mode,
    title: opts.title ?? '确认操作？',
    message: opts.message ?? '',
    confirmText: opts.confirmText ?? '确认',
    cancelText: opts.cancelText ?? '取消',
    tone: opts.tone ?? 'primary',
    commentLabel: opts.commentLabel ?? '',
    open: true,
  })
  return new Promise<any>((resolve) => { resolver = resolve })
}

/** 确认框：确定返回 true，取消返回 false */
export function confirmDialog(opts: ConfirmOptions = {}): Promise<boolean> {
  return openDialog(opts, 'confirm')
}

/** 带输入框的确认框：确定返回输入的文本（可为空串），取消返回 null */
export function promptDialog(opts: PromptOptions): Promise<string | null> {
  return openDialog(opts, 'prompt')
}

/** 由 GlobalConfirmHost 调用：结算当前弹框 */
export function settleConfirm(confirmed: boolean, comment = '') {
  const r = resolver
  resolver = null
  confirmState.open = false
  if (!r) return
  if (confirmState.mode === 'prompt') r(confirmed ? comment : null)
  else r(confirmed)
}
