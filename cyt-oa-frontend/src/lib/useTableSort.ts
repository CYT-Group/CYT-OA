import { computed, ref, type Ref } from 'vue'

/** 当前页内的客户端排序；翻页后按新一页重排，不改变服务端查询条件 */
export function useTableSort<T>(rows: Ref<T[]>, valueOf: (row: T, key: string) => unknown) {
  const sortKey = ref('')
  const sortDir = ref<'asc' | 'desc'>('asc')

  const sorted = computed<T[]>(() => {
    if (!sortKey.value) return rows.value
    const key = sortKey.value
    const list = [...rows.value]
    list.sort((a, b) => {
      const av = valueOf(a, key)
      const bv = valueOf(b, key)
      const an = numericOf(av)
      const bn = numericOf(bv)
      const base =
        an !== null && bn !== null
          ? an - bn
          : String(av ?? '').localeCompare(String(bv ?? ''), 'zh-CN', { numeric: true })
      return sortDir.value === 'asc' ? base : -base
    })
    return list
  })

  function toggleSort(key: string) {
    if (sortKey.value !== key) {
      sortKey.value = key
      sortDir.value = 'asc'
      return
    }
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  }

  return { sortKey, sortDir, sorted, toggleSort }
}

/** 纯数字或日期串按数值比，其余按中文排序 */
function numericOf(v: unknown): number | null {
  if (typeof v === 'number') return Number.isFinite(v) ? v : null
  const s = String(v ?? '').trim()
  if (!s) return null
  if (/^-?\d+(\.\d+)?$/.test(s)) return Number(s)
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) {
    const t = Date.parse(s.replace(' ', 'T'))
    return Number.isNaN(t) ? null : t
  }
  return null
}
