import { api } from './api'

export interface SelectOption {
  label: string
  value: string
}

const cache = new Map<string, SelectOption[]>()

/**
 * 选项来自接口时按 url + 取值口径缓存。接口既可能是分页包（{list:[...]}），也可能是裸数组。
 * 拉取失败留空列表：下拉框空着比整页报错好，用户能刷新重试。
 */
export async function remoteOptions(url: string, labelKey = 'label', valueKey = 'value'): Promise<SelectOption[]> {
  const id = `${url}#${labelKey}#${valueKey}`
  if (cache.has(id)) return cache.get(id)!
  let rows: Record<string, unknown>[] = []
  try {
    const data = await api.get<unknown>(url)
    rows = (Array.isArray(data) ? data : ((data as { list?: Record<string, unknown>[] })?.list ?? [])) as Record<string, unknown>[]
  } catch {
    return []
  }
  const items = rows.map((row) => ({ label: String(row[labelKey] ?? ''), value: String(row[valueKey] ?? '') }))
  cache.set(id, items)
  return items
}
