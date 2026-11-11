import { reactive } from "vue";
import { session } from "./session";
import { configurablePortalKeys } from "./portal-entry-policy";

export interface PortalRolloutEntries {
  entries: Record<string, boolean>;
  revision: number | string;
}
export interface PortalRolloutSettings extends PortalRolloutEntries {
  updatedBy?: string | null;
  updatedAt?: string | null;
}
export const portalRollout = reactive({
  owner: "",
  loaded: false,
  entries: {} as Record<string, boolean>,
  error: "",
  revision: "0",
});
let inflight: { token: string; promise: Promise<void> } | undefined;

function validate(snapshot: PortalRolloutEntries) {
  if (
    !snapshot ||
    !snapshot.entries ||
    Array.isArray(snapshot.entries) ||
    snapshot.revision == null ||
    !/^(0|[1-9]\d{0,18})$/.test(String(snapshot.revision)) ||
    [...configurablePortalKeys].some(
      (key) => typeof snapshot.entries[key] !== "boolean",
    )
  )
    throw new Error("入口状态数据不完整，请重新加载。");
}
export function applyPortalRollout(snapshot: PortalRolloutEntries) {
  validate(snapshot);
  if (
    portalRollout.owner === session.token &&
    portalRollout.loaded &&
    BigInt(snapshot.revision) < BigInt(portalRollout.revision)
  )
    return;
  portalRollout.owner = session.token;
  portalRollout.entries = { ...snapshot.entries };
  portalRollout.loaded = true;
  portalRollout.error = "";
  portalRollout.revision = String(snapshot.revision);
}
export function portalEntryEnabled(key?: string): boolean {
  if (!key || !configurablePortalKeys.has(key)) return true;
  return (
    portalRollout.owner === session.token &&
    portalRollout.loaded &&
    portalRollout.entries[key] === true
  );
}
export async function loadPortalRollout(force = false): Promise<void> {
  const token = session.token;
  if (!token || !session.canAccessConsole) return;
  if (portalRollout.owner !== token) {
    portalRollout.owner = token;
    portalRollout.loaded = false;
    portalRollout.entries = {};
    portalRollout.error = "";
    portalRollout.revision = "0";
  }
  if (!force && portalRollout.loaded) return;
  if (inflight?.token === token) return inflight.promise;
  // 路由守卫使用本模块，延迟加载网络层以避免 api → router → store 的循环依赖。
  const pending: Promise<void> = import("./api")
    .then(({ api }) =>
      api.get<PortalRolloutEntries>("/admin-api/portal/rollout/entries"),
    )
    .then((snapshot) => {
      if (session.token === token && portalRollout.owner === token)
        applyPortalRollout(snapshot);
    })
    .catch((error) => {
      if (session.token === token && portalRollout.owner === token) {
        portalRollout.loaded = false;
        portalRollout.entries = {};
        portalRollout.error =
          error instanceof Error ? error.message : "入口状态加载失败";
      }
      throw error;
    })
    .finally(() => {
      if (inflight?.promise === pending) inflight = undefined;
    });
  inflight = { token, promise: pending };
  return pending;
}

export const portalRolloutApi = {
  settings: async () =>
    (await import("./api")).api.get<PortalRolloutSettings>(
      "/admin-api/portal/rollout/settings",
    ),
  save: async (revision: number | string, entries: Record<string, boolean>) =>
    (await import("./api")).api.put<PortalRolloutSettings>(
      "/admin-api/portal/rollout/settings",
      {
        revision,
        entries,
      },
    ),
};
