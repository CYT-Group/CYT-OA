import type { NavItem, NavSub, NavZone } from "../configs/nav";

/** 菜单声明的查询参数决定具体入口，其余页面筛选参数不影响导航选中态。 */
export function matchesNavigation(target: string, current: string): boolean {
  const expected = new URL(target, "http://oa.local");
  const actual = new URL(current, "http://oa.local");
  const descendant =
    expected.pathname === "/portal/hr-documents" &&
    actual.pathname.startsWith(expected.pathname + "/");
  if (expected.pathname !== actual.pathname && !descendant) return false;
  let matches = true;
  expected.searchParams.forEach((value, key) => {
    if (actual.searchParams.get(key) !== value) matches = false;
  });
  return matches;
}

/** 同一个业务页的二级入口以查询参数区分，选择最具体的一项，避免多行同时选中。 */
export function findNavigationMatch(
  zones: NavZone[],
  current: string,
): { zone: NavZone; item: NavItem; child?: NavSub } | undefined {
  let best: { zone: NavZone; item: NavItem; child?: NavSub } | undefined;
  let bestScore = -1;
  for (const zone of zones)
    for (const item of zone.items) {
      const candidates = [
        ...[item.to, ...(item.aliases ?? [])].map((to) => ({
          to,
          child: undefined as NavSub | undefined,
        })),
        ...(item.children ?? []).flatMap((child) =>
          [child.to, ...(child.aliases ?? [])].map((to) => ({ to, child })),
        ),
      ];
      for (const candidate of candidates) {
        if (!matchesNavigation(candidate.to, current)) continue;
        const target = new URL(candidate.to, "http://oa.local");
        const score =
          target.pathname.length * 100 +
          [...target.searchParams].length * 10 +
          Number(!!candidate.child);
        if (score > bestScore) {
          bestScore = score;
          best = { zone, item, child: candidate.child };
        }
      }
    }
  return best;
}
