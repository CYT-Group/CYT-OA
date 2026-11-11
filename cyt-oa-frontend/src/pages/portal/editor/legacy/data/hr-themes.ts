/**
 * HR 文档主题配色 —— 对齐参考「制度编辑器」的 6 套预设 + 自由取色。
 *
 * 值域（后端 domain.require_valid_theme 同源，fail-loud）：
 *   ''        默认，用项目品牌紫
 *   key       6 套预设之一（violet / blue / orange / green / slate / rose）
 *   #RRGGBB   自由取色
 *
 * 主题色怎么落地：渲染端把 `--hr-primary` / `--hr-primary-rgb` 两个 CSS 变量挂在
 * `.hr-blocks` 上，块内所有品牌色都写成 `var(--hr-primary, var(--v, #6366f1))`，
 * 于是**一处赋值、14 处品牌色自动跟随**，不用改任何块样式。
 * `--hr-primary-rgb` 供 `rgba(var(--hr-primary-rgb), .08)` 这类柔和底色用。
 *
 * ⚠️ primary 必须始终是 6 位十六进制：渲染端会把它直接拼进内联 style（封面渐变）。
 * 后端已把值域收紧到「预设 key 或严格 #RRGGBB」，所以这里可以放心拼接。
 */

export interface HrThemePreset {
  key: string
  name: string
  primary: string
  secondary: string
}

export interface ResolvedTheme extends HrThemePreset {
  gradient: string
}

export const HR_THEMES: HrThemePreset[] = [
  { key: 'violet', name: '优雅紫', primary: '#7c3aed', secondary: '#0d9488' },
  { key: 'blue', name: '商务蓝', primary: '#2563eb', secondary: '#0d9488' },
  { key: 'orange', name: '暖橙', primary: '#ea580c', secondary: '#f59e0b' },
  { key: 'green', name: '森林绿', primary: '#16a34a', secondary: '#65a30d' },
  { key: 'slate', name: '极简灰', primary: '#334155', secondary: '#64748b' },
  { key: 'rose', name: '玫红', primary: '#e11d48', secondary: '#f59e0b' },
]

/** 未设主题时的默认主色 = 项目品牌紫（--v），保证与门户其它页面一致 */
export const DEFAULT_THEME_PRIMARY = '#6366f1'
/** 默认主题的渐变收尾色（同色系提亮，不引入第二种色相） */
const DEFAULT_THEME_SECONDARY = '#a78bfa'

const HEX6 = /^#[0-9a-fA-F]{6}$/

/**
 * 把主题值解析成 { key, name, primary, secondary, gradient }。
 * 非法值（理论上进不来）一律回落到默认主题，绝不把未校验的字符串拼进 CSS。
 */
export function resolveTheme(value?: string | null): ResolvedTheme {
  if (value && HEX6.test(value)) {
    const secondary = lighten(value, 0.3)
    return {
      key: value,
      name: '自选色',
      primary: value,
      secondary,
      gradient: `linear-gradient(135deg, ${value} 0%, ${secondary} 100%)`,
    }
  }
  const preset: HrThemePreset = HR_THEMES.find(t => t.key === value) || {
    key: '', name: '品牌紫', primary: DEFAULT_THEME_PRIMARY, secondary: DEFAULT_THEME_SECONDARY,
  }
  return {
    ...preset,
    gradient: `linear-gradient(135deg, ${preset.primary} 0%, ${preset.secondary} 100%)`,
  }
}

/** #RRGGBB → [r, g, b]；非法输入回落品牌紫的通道值 */
export function hexToRgb(hex?: string | null): [number, number, number] {
  const safe = HEX6.test(hex || '') ? (hex as string) : DEFAULT_THEME_PRIMARY
  const n = parseInt(safe.slice(1), 16)
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255]
}

/** 朝白色混色提亮（0~1）；用于自选色的渐变收尾 */
export function lighten(hex: string, pct = 0.3): string {
  const [r, g, b] = hexToRgb(hex)
  const mix = (c: number): number => Math.round(c + (255 - c) * pct)
  return '#' + [mix(r), mix(g), mix(b)].map((v: number) => v.toString(16).padStart(2, '0')).join('')
}

/** 渲染端挂在根节点上的 CSS 变量；块内所有品牌色跟着它走 */
export function themeCssVars(value?: string | null): Record<string, string> {
  const { primary } = resolveTheme(value)
  const [r, g, b] = hexToRgb(primary)
  return { '--hr-primary': primary, '--hr-primary-rgb': `${r}, ${g}, ${b}` }
}
