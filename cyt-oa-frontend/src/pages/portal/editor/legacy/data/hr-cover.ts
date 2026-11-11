/**
 * 整屏封面配置 —— 对齐参考「制度编辑器」2026-10-12 更新后的 `pc-fullcover`。
 *
 * 后端 domain.DEFAULT_COVER 是值域与默认值的唯一真相（未知键 422、缺键用默认）。
 * 这里只做两件事：给前端一份同源默认值 + 把背景三态算成 CSS 背景。
 *
 * 键（10 个）：
 *   bg_type  'dark' | 'gradient' | 'image'  背景风格
 *   image    封面图 URL（https:// 或站内 /）；仅 bg_type='image' 时用
 *   label_en 英文标识（大写小字）  label_cn 中文标识
 *   icon     封面图标（emoji）
 *   desc     封面说明
 *   date     执行日期   scope 适用范围   dept 发布部门
 *   nav_tabs 导航标签数组
 */

export type CoverBgType = 'dark' | 'gradient' | 'image'

export interface HrCover {
  bg_type: CoverBgType
  image: string
  label_en: string
  label_cn: string
  icon: string
  desc: string
  date: string
  scope: string
  dept: string
  nav_tabs: string[]
}

/** 与后端 domain.DEFAULT_COVER 逐键一致；默认走主题渐变（存量文档外观不变） */
export const DEFAULT_COVER: HrCover = {
  bg_type: 'gradient',
  image: '',
  label_en: '',
  label_cn: '',
  icon: '',
  desc: '',
  date: '',
  scope: '',
  dept: '',
  nav_tabs: [],
}

/** 封面图标候选（参考的 ce-icon-pick 是点选式，这里给一排常用的） */
export const COVER_ICONS = [
  '📄', '📋', '📌', '📊', '📈', '🏢', '👥', '💰', '🎁', '✈️',
  '🏖️', '⏰', '🛡️', '🔒', '⚖️', '🚗', '💡', '🎯', '📝', '🌟',
]

/** 深色封面背景（参考的 dark 档：深蓝黑渐变） */
export const COVER_DARK_BG = 'linear-gradient(160deg, #0f172a 0%, #1e293b 50%, #0f172a 100%)'

/** 补全缺键；nav_tabs 一定是新数组（别共享 DEFAULT_COVER 里那个） */
export function normalizeCover(value?: Partial<HrCover> | null): HrCover {
  const out: HrCover = { ...DEFAULT_COVER, nav_tabs: [] }
  if (value) Object.assign(out, value)
  out.nav_tabs = Array.isArray(out.nav_tabs) ? [...out.nav_tabs] : []
  return out
}

/** 背景样式：image 档用图（无图回落深色），gradient 档用主题渐变，dark 档用深色 */
export function coverBackground(cover: HrCover, themeGradient: string): string {
  if (cover.bg_type === 'image' && cover.image) {
    return `url(${JSON.stringify(cover.image)}) center / cover no-repeat`
  }
  if (cover.bg_type === 'gradient') return themeGradient
  return COVER_DARK_BG
}

/**
 * 兼容 v1.5 的 `cover_image`（单张封面图，已被 v1.6 的 cover.image 取代）。
 * **只在封面配置整个为空时**才把老字段当图片背景用 —— 否则会覆盖用户已选的渐变/深色。
 * 用户下次在编辑器里保存，值就落到 cover.image，老字段自然退役。
 */
export function withLegacyCoverImage(
  cover: unknown,
  legacyImage?: string | null,
): HrCover {
  const raw = (cover || {}) as Partial<HrCover>
  const out = normalizeCover(raw)
  if (legacyImage && Object.keys(raw).length === 0) {
    out.image = legacyImage
    out.bg_type = 'image'
  }
  return out
}
