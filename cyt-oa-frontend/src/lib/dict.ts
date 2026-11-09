import { api } from './api'

export interface DictItem {
  id: number
  dictType: string
  label: string
  value: string
  sortOrder: number
  status: number
}

const cache = new Map<string, DictItem[]>()

/** 按字典类型取选项（缓存），value 转成字符串统一比较。 */
export async function dictOptions(type: string): Promise<DictItem[]> {
  if (cache.has(type)) return cache.get(type)!
  const data = await api.get<DictItem[]>(`/admin-api/system/dict/data`, { type })
  cache.set(type, data || [])
  return cache.get(type)!
}

export function dictLabel(type: string, value: unknown): string {
  const items = cache.get(type)
  if (!items) return value == null ? '' : String(value)
  return items.find((i) => i.value === String(value))?.label ?? String(value ?? '')
}
