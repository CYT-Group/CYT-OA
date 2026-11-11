/** 管理页面的时间展示统一使用北京时间，保留原始值供无法解析的历史数据核对。 */
export function formatPortalDateTime(value?: string | null): string {
  if (!value) return '—'
  const normalized = value.trim().replace(' ', 'T')
  const localDateTime = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?$/.test(normalized)
  const date = new Date(localDateTime ? `${normalized}+08:00` : normalized)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hourCycle: 'h23'
  }).format(date)
}
