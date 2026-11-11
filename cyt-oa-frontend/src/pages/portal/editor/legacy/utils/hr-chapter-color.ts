/**
 * 章节配色（numsec.color）的纯函数工具 —— 渲染端与目录导航共用一份。
 *
 * 语义（用户 2026-10-14）：「章节的颜色可以自定义，包括目录的也是」
 *                       + 「没设置就根据文章内主题配色而定」
 *  - color 为空串 / 非 6 位 hex → **不配色**（返回 null），CSS 自然回落到 --hr-primary
 *  - 合法 #RRGGBB → 返回 { color, rgb, style }
 *
 * 与后端 `domain/hr_documents.py` 的 `_check_value(kind='themecolor')` 同构：
 * 那里放行空串、非空值必须是 6 位十六进制。前端不负责报错（后端 fail-loud），
 * 这里只做「拼进内联 style 前的最后一道形状闸」——非法值一律当未配色，绝不拼进 style。
 *
 * 单独成文件的意义：`.vue` 里的纯函数测不了（happy-dom 里挂组件成本高），
 * 抽出来后 `hr-chapter-color.test.ts` 直接覆盖边界（空串 / 短 hex / 脏值 / 大小写）。
 */

const HEX6 = /^#[0-9a-fA-F]{6}$/

export interface ChapterColor {
  /** 归一后的 #RRGGBB（原样大小写） */
  color: string
  /** "r, g, b" —— 供 rgba(var(--hbc-toc-rgb), .08) 这类用法 */
  rgb: string
  /** 直接可绑到 :style 上的局部变量别名（章头与目录共用 --hbc-toc） */
  style: Record<string, string>
}

/** 解析章色；未配色 / 非法 → null（调用方据此不写任何变量） */
export function parseChapterColor(raw?: string | null): ChapterColor | null {
  const c = (raw ?? '').trim()
  if (!HEX6.test(c)) return null
  const n = parseInt(c.slice(1), 16)
  const rgb = `${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}`
  return {
    color: c,
    rgb,
    style: { '--hbc-toc': c, '--hbc-toc-rgb': rgb },
  }
}

/** 只要能被 CSS 用（rgb 通道）时用这个；未配色返回 null */
export function chapterRgb(raw?: string | null): string | null {
  return parseChapterColor(raw)?.rgb ?? null
}
