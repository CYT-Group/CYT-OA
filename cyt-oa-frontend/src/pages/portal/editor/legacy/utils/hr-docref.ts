/**
 * docref（文档引用块）三层取值 —— 纯函数，渲染端与单测共用（v1.17）。
 *
 * 块 wire（契约见 `doc_ww/口径-HR文档块编辑-后端.md` §20）：
 * ```ts
 * { type: 'docref', document_id, title, text, button_text, button_url }
 * 历史快照中的 slug 仍可读，不再由新编辑器写入。
 * ```
 * 三层语义（用户 2026-10-16：「那个块需要分三层」）：
 *   ① `title`       默认文章标题 —— 选择引用时的快照（后端保证非空）
 *   ② `text`        可自定义编辑的段落富文本；**空 = 未自定义** → 回落目标文档的封面说明 / 引言
 *   ③ `button_*`    触达按钮的文字与地址；**空 = 未自定义** → 回落「前往《标题》→」+ 目标文档的规范地址
 *
 * 🔴 三层都是「块自己的内容优先，留空才回落」：所以**老块（只有 slug/title）行为不变**，
 * 新块想覆盖哪一层就填哪一层。空串是「未自定义」而不是「清空」—— 与 `numsec.color` 同一口径。
 */
import type { HrCover } from '../data/hr-cover'

export interface DocRefBlock {
  document_id?: number
  slug?: string
  title?: string
  text?: string
  button_text?: string
  button_url?: string
}

/** 目标文档的展示字段（来自 `GET /articles/published` 的摘要） */
export interface DocRefTarget {
  title?: string
  canonical_url?: string
  intro?: string | null
  theme?: string
  cover?: Partial<HrCover> | null
}

/**
 * 富文本「有没有内容」：TipTap 的空文档会序列化成 `<p></p>`（甚至 `<p><br></p>`），
 * 所以不能只看字符串是否为空 —— 那样用户明明没填，卡片也会认为「已自定义」而丢掉封面说明。
 */
export function isEmptyHtml(html?: string | null): boolean {
  if (!html) return true
  return !String(html)
    .replace(/<[^>]*>/g, '')        // 去掉全部标签
    .replace(/&nbsp;/gi, ' ')       // 编辑器常见的空白实体
    .trim()
}

/** ① 第一层标题：块里的快照优先，兜底取目标文档当前标题 */
export function docRefTitle(block?: DocRefBlock | null, target?: DocRefTarget | null): string {
  return (block?.title || '').trim() || (target?.title || '').trim()
}

/** ② 第二层段落：块里自定义优先；空 → 目标封面说明；再空 → 目标文档引言（都是「这篇在讲什么」） */
export function docRefParagraph(block?: DocRefBlock | null, target?: DocRefTarget | null): string {
  if (!isEmptyHtml(block?.text)) return String(block?.text)
  const desc = typeof target?.cover?.desc === 'string' ? target.cover.desc.trim() : ''
  if (desc) return desc
  return (target?.intro || '').trim()
}

export interface DocRefButton {
  text: string
  url: string
  /** 外部 HTTP(S) 链接：交给浏览器新窗口打开；站内路径走 router.push */
  external: boolean
}

/** ③ 第三层按钮：文字与地址都可自定义，留空各自回落默认（站内指向被引用文档） */
export function docRefButton(block?: DocRefBlock | null, target?: DocRefTarget | null): DocRefButton {
  const text = (block?.button_text || '').trim() || `前往《${docRefTitle(block, target)}》→`
  const customUrl = block?.button_url || ''
  const fallbackUrl = block?.document_id
    ? `/portal/hr-documents/id/${encodeURIComponent(String(block.document_id))}/preview`
    : block?.slug ? `/portal/hr-documents?legacySlug=${encodeURIComponent(block.slug)}&mode=preview` : ''
  const url = customUrl.trim() ? customUrl : (target?.canonical_url || '').trim() || fallbackUrl
  return { text, url, external: /^https?:\/\//i.test(url) }
}
