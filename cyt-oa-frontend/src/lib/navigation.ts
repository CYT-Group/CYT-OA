/** 菜单声明的查询参数决定具体入口，其余页面筛选参数不影响导航选中态。 */
export function matchesNavigation(target: string, current: string): boolean {
  const expected = new URL(target, 'http://oa.local')
  const actual = new URL(current, 'http://oa.local')
  const descendant = expected.pathname === '/portal/hr-documents'
    && actual.pathname.startsWith(expected.pathname + '/')
  if (expected.pathname !== actual.pathname && !descendant) return false
  let matches = true
  expected.searchParams.forEach((value, key) => {
    if (actual.searchParams.get(key) !== value) matches = false
  })
  return matches
}
