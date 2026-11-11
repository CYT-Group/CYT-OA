/**
 * 员工签收卡配置的取值规则（v1.19，纯函数 + 单测）。
 *
 * 用户 2026-10-16：「签收卡是可配置的」→ 签收卡整张的文案都存文档级 `signoff`（JSONB，
 * 后端 `domain.DEFAULT_SIGNOFF` 同款键集），阅读页（HrDocPage）与编辑表单共用这一份取值。
 *
 * 口径：
 * - 每个键都可编辑；**空串 / 空 HTML = 未自定义** → 回落「线上默认模板」，
 *   （与 quote.color 的 themecolor、docref 的三层回落同一理念：空串是「没配」不是「配成空」）。
 * - `lead` / `tip` 是富文本（HTML），判空走 hr-docref 的 isEmptyHtml（认 TipTap 空文档）。
 * - 默认 lead 里会把《标题》版本 + 全部 N 章插值进去——这些是纯文本，拼 HTML 前先转义。
 */
import { isEmptyHtml } from './hr-docref'

/** 旧文档沿用字符串签收键，避免现有企微催签遗漏已签员工；新文档才按 ID 写证据。 */
export function signoffReference(doc: { id: number; legacy_slug?: string | null }): { document: string } | { document_id: number } {
  return doc.legacy_slug ? { document: doc.legacy_slug } : { document_id: doc.id }
}

export interface SignoffConfig {
  icon: string
  title: string
  lead: string
  ack_label: string
  button_text: string
  tip: string
  signed_note: string
}

/** 与后端 domain.DEFAULT_SIGNOFF 逐值一致（线上写死模板的等价物）。 */
export const DEFAULT_SIGNOFF: SignoffConfig = {
  icon: '📝',
  title: '员工签收',
  lead: '',
  ack_label: '我确认已完整阅读并理解本文档全部内容',
  button_text: '✓ 我已阅读并同意（线上签收）',
  tip: '',
  signed_note: '签收记录已存档。本文档发布新版本时，您需要重新签收。',
}

const esc = (s: unknown): string =>
  String(s ?? '').replace(/[&<>"']/g, (c) => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))

/** 把后端/草稿的 `signoff` 合并回完整对象（老库是 `{}`，缺键用默认）。 */
export function mergeSignoff(raw: Partial<SignoffConfig> | null | undefined): SignoffConfig {
  return { ...DEFAULT_SIGNOFF, ...(raw || {}) }
}

/**
 * 签收卡引导区（承诺几条）的 HTML：
 * - 用户自定义了 `lead`（非空、非 TipTap 空文档）→ 原样用；
 * - 否则回落默认模板，把《标题》+ 版本 + 「全部 N 章内容」插进去。
 */
export function signoffLeadHtml(
  cfg: Partial<SignoffConfig>,
  meta: { title?: string; version?: string; chaptersText?: string },
): string {
  const lead = (cfg.lead || '').trim()
  if (lead && !isEmptyHtml(lead)) return lead
  const chapters = esc(meta.chaptersText || '全部内容')
  return (
    '<p>点击下方按钮即代表您：<br>' +
    `① 已完整阅读《${esc(meta.title)}》${esc(meta.version)} ${chapters}<br>` +
    '② 理解本文档对您的约束力<br>' +
    '③ 签收记录将存入您的人事档案</p>'
  )
}

/** 签收卡底部留痕小字：自定义优先，否则回落默认文案。 */
export function signoffTipHtml(cfg: Partial<SignoffConfig>): string {
  const tip = (cfg.tip || '').trim()
  if (tip && !isEmptyHtml(tip)) return tip
  return '<p>⚖️ 线上签收用于人事档案存档；签收时间、版本号、IP 均会留痕，等同纸质签字。</p>'
}
