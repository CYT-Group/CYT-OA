import { session } from "./session";
import type { NavZone } from "../configs/nav";
import { portalEntryEnabled } from "./portal-rollout";
import { portalEntryKey } from "./portal-entry-policy";

export function hasPortalPermission(item: {
  permission?: string;
  anyPermissions?: string[];
  allPermissions?: string[];
}) {
  if (item.permission && !session.has(item.permission)) return false;
  if (item.allPermissions?.some((permission) => !session.has(permission)))
    return false;
  return (
    !item.anyPermissions?.length ||
    item.anyPermissions.some((p) => session.has(p))
  );
}

export function allowed(item: {
  key?: string;
  to?: string;
  permission?: string;
  anyPermissions?: string[];
  allPermissions?: string[];
  systemOnly?: boolean;
}) {
  return (
    (!item.systemOnly || session.isAdmin) &&
    hasPortalPermission(item) &&
    portalEntryEnabled(portalEntryKey(item))
  );
}

export function filterPortalNavigation(zones: NavZone[]): NavZone[] {
  return zones
    .map((zone) => ({
      ...zone,
      items: zone.items.flatMap((item) => {
        if (zone.adminOnly && item.key === "console" && !session.isAdmin)
          return [];
        if (item.systemOnly && !session.isAdmin) return [];
        if (!hasPortalPermission(item)) return [];
        if (!item.children) return allowed(item) ? [item] : [];
        const children = item.children.filter(allowed);
        return children.length
          ? [{ ...item, to: children[0]!.to, children }]
          : [];
      }),
    }))
    .filter((zone) => zone.items.length > 0);
}
