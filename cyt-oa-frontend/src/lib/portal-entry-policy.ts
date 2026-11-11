import { portalMenus } from "../configs/portal";
import type { MenuLeaf } from "../configs/types";

export const fixedPortalEntries = new Set(["portal-hub", "portal-rollout"]);
export const configurablePortalEntries = portalMenus
  .flatMap((group) => group.items)
  .filter((item) => !fixedPortalEntries.has(item.key));
export const configurablePortalKeys = new Set(
  configurablePortalEntries.map((item) => item.key),
);

/** 共享一个页面的样品/办公用品按 query 精确区分；文档的编辑和预览归属同一入口。 */
export function portalEntryForRoute(
  path: string,
  query: Record<string, unknown> = {},
): MenuLeaf | undefined {
  // Vue Router 默认忽略路径大小写和末尾斜线，开关必须采用相同匹配语义。
  const routePath = path.toLowerCase().replace(/\/+$/, "") || "/";
  if (routePath === "/portal/materials") {
    return configurablePortalEntries.find(
      (item) =>
        item.key ===
        (query.kind === "office" ? "portal-supplies" : "portal-samples"),
    );
  }
  return configurablePortalEntries.find((item) => {
    const entryPath = (item.to ?? `/page/${item.key}`).split("?")[0]!;
    return routePath === entryPath || routePath.startsWith(entryPath + "/");
  });
}

export function portalEntryKey(item: {
  key?: string;
  to?: string;
}): string | undefined {
  if (
    item.key &&
    (configurablePortalKeys.has(item.key) || fixedPortalEntries.has(item.key))
  )
    return item.key;
  if (!item.to) return undefined;
  const url = new URL(item.to, "http://oa.local");
  return portalEntryForRoute(url.pathname, Object.fromEntries(url.searchParams))
    ?.key;
}

export function safePortalEntryTarget(value: unknown): string {
  if (typeof value !== "string") return "/portal";
  try {
    const url = new URL(value, "http://oa.local");
    if (
      url.origin === "http://oa.local" &&
      portalEntryForRoute(url.pathname, Object.fromEntries(url.searchParams))
    )
      return url.pathname + url.search;
  } catch {
    /* 无效回跳回到管理中心。 */
  }
  return "/portal";
}
