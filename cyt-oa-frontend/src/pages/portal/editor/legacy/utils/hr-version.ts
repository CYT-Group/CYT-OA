/**
 * HR 文档版本号的纯函数：解析、比较、建议及最终发布校验。
 * 草稿保存不受版本校验阻拦；建议值只供发布确认使用，不自动改写表单。
 * 无法比较的自定义名称允许作为新号，仍须检查是否已使用。
 */

/** 拆解后的版本号：前缀 + 数字段 + 后缀 */
export interface ParsedVersion {
  /** 数字前的前缀，如 "v"（原样保留，不擅自改写大小写） */
  prefix: string
  /** 数字段，"v1.0" → [1, 0]；"v2" → [2]；"v1.0.3" → [1, 0, 3] */
  segments: number[]
  /** 数字后的后缀，如 "-beta"（原样保留） */
  suffix: string
}

// 前缀任意非数字 / 数字段（点分）/ 后缀任意。中间的 `\d+(?:\.\d+)*` 是贪心的，
// 所以 "v1.0-beta" 的 "-beta" 会正确落到后缀里，而 "1.2.3" 整体进数字段。
const VERSION_RE = /^([^\d]*)(\d+(?:\.\d+)*)(.*)$/

/** 解析版本号；认不出数字（或空串）时返回 null，调用方必须处理 null（= 放弃判断） */
export function parseVersion(v: unknown): ParsedVersion | null {
  const s = String(v ?? '').trim()
  if (!s) return null
  const m = s.match(VERSION_RE)
  if (!m) return null
  const segments = m[2].split('.').map(Number)
  if (segments.some((n) => !Number.isFinite(n))) return null
  return { prefix: m[1], segments, suffix: m[3] }
}

/**
 * 递增一档：末段 +1，个位段满 9 进位。
 *
 *   v1.0 → v1.1      v0.9 → v1.0      v1.9 → v2.0
 *   v1.9.9 → v2.0.0  v2   → v3        v1.0-beta → v1.1-beta
 *
 * 「0.9 → 1.0」是用户拍板的口径（次号按 0-9 单个数位滚动）。
 * 但多位数的末段（如 "1.10"）只加一、不进位 —— 那种写法显然是指「次号 10」，
 * 若按十位进位会变成 2.0，等于把用户的号跳掉一整段。
 *
 * 解析不出来时返回空串（调用方据此不展示建议、也不改用户的输入）。
 */
export function bumpVersion(v: unknown): string {
  const p = parseVersion(v)
  if (!p) return ''
  const segs = [...p.segments]
  let i = segs.length - 1
  segs[i] += 1
  // 满 10 才进位（= 原本就是个位的 9），且不进到「凭空多一位」—— 最左段允许变成 10
  while (i > 0 && segs[i] === 10) {
    segs[i] = 0
    segs[i - 1] += 1
    i -= 1
  }
  return `${p.prefix}${segs.join('.')}${p.suffix}`
}

/**
 * 比较两个版本号：a > b 返回 1，a < b 返回 -1，相等返回 0。
 * 段数不等时短的按 0 补齐（"v1" == "v1.0"）。
 * **任一侧解析不出来就返回 null**（= 无法比较），调用方不得把它当成 0 用。
 */
export function compareVersion(a: unknown, b: unknown): number | null {
  const pa = parseVersion(a)
  const pb = parseVersion(b)
  if (!pa || !pb) return null
  const n = Math.max(pa.segments.length, pb.segments.length)
  for (let i = 0; i < n; i += 1) {
    const x = pa.segments[i] ?? 0
    const y = pb.segments[i] ?? 0
    if (x !== y) return x < y ? -1 : 1
  }
  return 0
}

/**
 * 目标版本号是否「没有递增」（≤ 已发布版本）。
 * 任一侧解析不出来 → false（放弃判断，不弹框也不提示）。
 */
export function isVersionStale(target: unknown, published: unknown): boolean {
  const c = compareVersion(target, published)
  return c !== null && c <= 0
}

/**
 * 建议的下一个版本号：以「已发布版本」为基准递增。
 *
 * 之所以以已发布版本而不是当前目标值为基准：这个建议只在「目标 ≤ 已发布」时才会被用到，
 * 此时两者中较大的一定是已发布版本。仍取 max 是为了函数本身在别的调用场景下也说得通。
 */
export function suggestNextVersion(published: unknown, target?: unknown): string {
  const c = compareVersion(target, published)
  const base = c !== null && c > 0 ? target : published
  return bumpVersion(base)
}

/** 仅发布新版本时校验；草稿可以保留空值或尚未调整的版本号。 */
export function publishVersionError(
  target: string,
  { publishedBefore = false, currentVersion = '', usedVersions = [] as string[] } = {},
): string {
  const value = target.trim()
  if (!value) return '请填写本次发布的版本号'
  if (value.length > 16) return '版本号最多 16 个字符'
  if (!publishedBefore) return ''
  if (value === currentVersion || usedVersions.includes(value)) return '此版本号已发布过，请使用新的版本号'
  const order = compareVersion(value, currentVersion)
  if (order !== null && order <= 0) return `目标版本号需高于 ${currentVersion}`
  return ''
}
