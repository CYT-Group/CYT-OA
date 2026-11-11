/**
 * 消息盒子的时间显示（铃铛下拉 + 收件箱页共用）。
 *
 * 后端时间戳是 utcnow 的裸串（无 tz，见 CLAUDE.md 约束），解析时补 Z 按 UTC 读，
 * 再换算到本机时区——直接 new Date('2026-08-27T03:00:00') 会被当本地时间读错 8 小时。
 */

function parseUtc(ts: string | null | undefined): Date | null {
  if (!ts) return null
  const s = String(ts)
  const d = new Date(/[Z+]/.test(s.slice(10)) ? s : s + 'Z')
  return isNaN(d.getTime()) ? null : d
}

/** 相对时间：刚刚 / N 分钟前 / N 小时前 / 昨天 / MM-DD / YYYY-MM-DD（跨年） */
export function timeAgo(ts: string | null | undefined): string {
  const d = parseUtc(ts)
  if (!d) return ''
  const diff = Date.now() - d.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86400_000) return `${Math.floor(diff / 3600_000)} 小时前`
  if (diff < 2 * 86400_000) return '昨天'
  const now = new Date()
  const mmdd = `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  return d.getFullYear() === now.getFullYear() ? mmdd : `${d.getFullYear()}-${mmdd}`
}

/** 完整本地时间 YYYY-MM-DD HH:mm（收件箱行悬浮/详情用） */
export function fullTime(ts: string | null | undefined): string {
  const d = parseUtc(ts)
  if (!d) return ''
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
