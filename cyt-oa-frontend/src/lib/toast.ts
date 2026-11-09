import { toast as sonner } from 'vue-sonner'
import { ApiError } from './api'

/** 全站轻提示统一走 sonner，各页不再自建 toast DOM。 */
export function toast(msg: string, tone: 'ok' | 'bad' = 'ok') {
  if (tone === 'bad') sonner.error(msg)
  else sonner.success(msg)
}

export function errText(e: unknown, fallback = '操作失败，请稍后再试'): string {
  if (e instanceof ApiError) return e.message
  // dev 下 HMR 可能让 api.ts 出现多份实例，instanceof 失效，按结构识别 ApiError
  if (e instanceof Error && typeof (e as { code?: unknown }).code === 'number' && e.message) return e.message
  return fallback
}
