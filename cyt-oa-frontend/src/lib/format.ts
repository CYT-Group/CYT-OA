import type { Tone } from '../configs/types'

/** 后端时间给的是 ISO 串（LocalDateTime），统一按本地时区裁剪显示。 */
export function fmtDay(value?: string | null): string {
  if (!value) return '—'
  return value.slice(0, 10)
}

export function fmtMD(value?: string | null): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return `${d.getMonth() + 1}-${String(d.getDate()).padStart(2, '0')}`
}

export function fmtTime(value?: string | null): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value.replace('T', ' ').slice(0, 16)
  return `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())} ${p2(d.getHours())}:${p2(d.getMinutes())}`
}

function p2(n: number) {
  return String(n).padStart(2, '0')
}

/** 提醒列表的时间列：今天内的用相对说法，超过一周退化成日期。 */
export function relTime(value?: string | null): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  const diff = Date.now() - d.getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins} 分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days <= 7) return `${days} 天前`
  return fmtTime(value).slice(0, 10)
}

/** warm-flow 流程状态 → 徽章色调（申请单与审批列表共用） */
export function flowTone(status?: number | null): Tone {
  switch (status) {
    case 1:
      return 'warn'
    case 2:
      return 'ok'
    case 3:
      return 'bad'
    default:
      return 'info'
  }
}

export function reportTone(status?: number | null): Tone {
  return status === 2 ? 'ok' : 'info'
}

/** 两个 YYYY-MM-DD 相差几天（to - from），只用于日历口径，不跨时区。 */
export function daysBetween(from: string, to: string): number {
  return Math.round((new Date(to).getTime() - new Date(from).getTime()) / 86400000)
}

/** 本地今天的 YYYY-MM-DD。toISOString 走 UTC，东八区早上会把日期算前一天。 */
export function todayISO(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}
