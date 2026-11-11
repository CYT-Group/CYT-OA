import type { NavItem, NavSub, NavZone } from "../configs/nav";

/** 条件模块与页签共用此处裁剪，侧栏、搜索和面包屑使用同一结果。 */
export function personalizeNavigation(zones: NavZone[], context: { teamCount: number; isAdmin: boolean; canAccessConsole: boolean; canApproveMaterials: boolean; canApproveRequests?: boolean }): NavZone[] {
  const visible = (item: NavSub) =>
    (!item.requiresTeam || context.teamCount > 0) &&
    (!item.requiresApprover || context.teamCount > 0 || context.canAccessConsole || context.canApproveRequests) &&
    (!item.requiresMaterialsApprover || context.canApproveMaterials);
  return zones.map(zone => ({
    ...zone,
    items: zone.items.filter(visible).map(item => {
      const children = item.children?.filter(visible);
      return {
        ...item,
        ...(item.key === 'report' ? { title: context.teamCount > 0 || context.canAccessConsole ? '报告中心' : '我的报告' } : {}),
        ...(item.key === 'overview' && context.isAdmin ? { to: '/admin' } : {}),
        ...(children ? { children, to: children[0]?.to ?? item.to } : {}),
      };
    }).filter(item => !item.children || item.children.length > 0),
  })).filter(zone => zone.items.length > 0);
}

/** 先按权限裁剪目录，再把页签入口收拢；没有主入口权限时保留可访问的独立入口。 */
export function sidebarNavigation(zones: NavZone[]): NavZone[] {
  return zones.map((zone) => ({
    ...zone,
    items: zone.items.map((item) => {
      if (!item.children?.length) return item;
      if (item.sidebarLink)
        return {
          ...item,
          children: undefined,
          aliases: [
            ...(item.aliases ?? []),
            ...item.children.flatMap((child) => [
              child.to,
              ...(child.aliases ?? []),
            ]),
          ],
        };
      const keys = new Set(
        item.children.map((child) => child.key).filter(Boolean),
      );
      keys.add(item.key);
      const children = item.children
        .filter(
          (child) => !child.sidebarParent || !keys.has(child.sidebarParent),
        )
        .map((child) => ({
          ...child,
          aliases: [
            ...(child.aliases ?? []),
            ...item
              .children!.filter((detail) => !!child.key && detail.sidebarParent === child.key)
              .flatMap((detail) => [detail.to, ...(detail.aliases ?? [])]),
          ],
        }));
      return {
        ...item,
        children,
        to: children[0]?.to ?? item.to,
        aliases: [
          ...(item.aliases ?? []),
          ...item.children
            .filter((child) => child.sidebarParent === item.key)
            .flatMap((child) => [child.to, ...(child.aliases ?? [])]),
        ],
      };
    }),
  }));
}

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
