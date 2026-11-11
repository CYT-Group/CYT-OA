/** 登录回填只回 OA 站内页面，拒绝编码后的外跳与认证端点循环。 */
export function parseLoginTarget(raw: unknown): string | null {
  if (typeof raw !== "string" || raw.length > 2048 || raw.trim() !== raw)
    return null;
  let decoded = raw;
  try {
    for (let pass = 0; pass < 4; pass++) {
      if (
        !decoded.startsWith("/") ||
        decoded.startsWith("//") ||
        /[\\\u0000-\u001f\u007f]/.test(decoded)
      )
        return null;
      const next = decodeURIComponent(decoded);
      if (next === decoded) break;
      decoded = next;
      if (pass === 3 && /%[0-9a-f]{2}/i.test(decoded)) return null;
    }
    if (
      !decoded.startsWith("/") ||
      decoded.startsWith("//") ||
      /[\\\u0000-\u001f\u007f]/.test(decoded)
    )
      return null;
    const origin = "https://oa.invalid";
    const target = new URL(decoded, origin);
    if (
      target.origin !== origin ||
      /^\/(?:login|api|admin-api)(?:\/|$)/i.test(target.pathname)
    )
      return null;
    const original = new URL(raw, origin);
    return original.pathname + original.search + original.hash;
  } catch {
    return null;
  }
}

/** 失败回填优先保留地址栏明确指定的合法目标；无目标时才恢复上次事务。 */
export function selectLoginTarget(
  queryTarget: unknown,
  pendingTarget: unknown,
): string {
  return (
    parseLoginTarget(queryTarget) ?? parseLoginTarget(pendingTarget) ?? "/"
  );
}

export function safeLoginTarget(raw: unknown): string {
  return selectLoginTarget(raw, undefined);
}

/** 有交接参数时必须先完成换人登录，不能沿用浏览器里另一位员工的 OA 会话。 */
export function hasLoginHandoff(query: Record<string, unknown>): boolean {
  return ["wecom_code", "wecom_error", "sso_error"].some((key) =>
    Object.prototype.hasOwnProperty.call(query, key),
  );
}

export interface WecomConfiguration {
  available: boolean;
  publicBaseUrl: string;
  portalBaseUrl: string;
  oauthCallbackUri: string;
  statePrefix: string;
  message?: string;
}

export function validWecomPanel(
  config: WecomConfiguration,
  params: Record<string, string>,
  origin: string,
): boolean {
  try {
    const portal = new URL(config.portalBaseUrl);
    const local = ["localhost", "127.0.0.1", "[::1]"].includes(portal.hostname);
    return (
      config.available === true &&
      config.publicBaseUrl === origin &&
      portal.origin === config.portalBaseUrl &&
      (portal.protocol === "https:" ||
        (local && portal.protocol === "http:")) &&
      config.oauthCallbackUri === portal.origin + "/api/wework/callback" &&
      params.redirect_uri === config.oauthCallbackUri &&
      params.login_type === "CorpApp" &&
      params.redirect_type === "top" &&
      params.panel_size === "small" &&
      params.lang === "zh" &&
      Boolean(params.appid) &&
      Boolean(params.agentid) &&
      Object.values(params).every((value) => typeof value === "string") &&
      Boolean(config.statePrefix) &&
      Boolean(params.state?.startsWith(config.statePrefix)) &&
      /^[A-Za-z0-9_-]{43}$/.test(params.state.slice(config.statePrefix.length))
    );
  } catch {
    return false;
  }
}
