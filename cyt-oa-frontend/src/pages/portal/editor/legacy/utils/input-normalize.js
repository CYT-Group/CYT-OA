/**
 * 输入规范化 — 中文输入法下的全角字符处理
 *
 * 为什么要它（2026-10-14 踩）：
 * 「设置表格行列」用了 `window.prompt` + 正则校验，用户报「**会被吞 2**」。
 * 实测根因**不是丢字符**，而是：中文输入法默认输出**全角**数字与符号，
 * `２`(U+FF12) `＊`(U+FF0A) `，`(U+FF0C) 在屏幕上与半角 `2` `*` `,` **几乎看不出区别**，
 * 于是正则 `/^(\d{1,3})\s*[x×*,\s]\s*(\d{1,3})$/` 把它们全判为「格式不对」
 * → 用户以为「我明明打了 2，它却把 2 吞了」。
 *
 * 结论：**任何接受键盘输入并做数字/符号解析的入口，都必须先过一遍这里**。
 * 只做「全角 → 半角」单向转换，不改变用户能表达的语义（不猜、不纠正错别字）。
 */

/** ASCII 可见区：全角 ！(U+FF01) ~ ～(U+FF5E) 与半角差 0xFEE0 */
const FULLWIDTH_OFFSET = 0xfee0
const FULLWIDTH_START = 0xff01
const FULLWIDTH_END = 0xff5e

/** 表意空格 U+3000 → 普通空格 */
const IDEOGRAPHIC_SPACE = 0x3000

/**
 * 把全角 ASCII 字符转成半角（0xFF01–0xFF5E → 0x21–0x7E），
 * 全角空格（U+3000）转普通空格。其余字符原样保留。
 *
 * @param {string} s
 * @returns {string}
 */
export function toHalfWidth(s) {
  if (s == null) return ''
  let out = ''
  for (const ch of String(s)) {
    const code = ch.codePointAt(0)
    if (code === IDEOGRAPHIC_SPACE) {
      out += ' '
    } else if (code >= FULLWIDTH_START && code <= FULLWIDTH_END) {
      out += String.fromCharCode(code - FULLWIDTH_OFFSET)
    } else {
      out += ch
    }
  }
  return out
}

/** 把全角字符转半角后去首尾空白 */
export function normalizeInput(s) {
  return toHalfWidth(s).trim()
}

/** 该字符串里是否含全角 ASCII 字符（用于给用户提示「已自动转换」） */
export function hasFullWidth(s) {
  if (s == null) return false
  for (const ch of String(s)) {
    const code = ch.codePointAt(0)
    if (code === IDEOGRAPHIC_SPACE) return true
    if (code >= FULLWIDTH_START && code <= FULLWIDTH_END) return true
  }
  return false
}

/**
 * 解析「行 × 列」尺寸表达式。
 *
 * 支持的写法（在 `normalizeInput` 之后判定）：
 *   5x4 / 5×4 / 5*4 / 5,4 / 5 4 / 5x 4 / 5 x 4 ...
 *
 * @param {string} raw 用户原始输入
 * @param {{rowMax?:number, colMax?:number}} [limits]
 * @returns {{ok:true, rows:number, cols:number, converted:boolean}
 *          | {ok:false, reason:'empty'|'format'|'range', rows?:number, cols?:number}}
 */
export function parseSize(raw, limits = {}) {
  const rowMax = limits.rowMax ?? 100
  const colMax = limits.colMax ?? 20
  if (raw == null || String(raw).trim() === '') return { ok: false, reason: 'empty' }

  const converted = hasFullWidth(raw)
  const s = normalizeInput(raw)

  // 分隔符：x / X / ×(U+00D7) / *(半角，全角已转) / , / 空白
  const m = s.match(/^(\d{1,3})\s*[xX×*,\s]\s*(\d{1,3})$/)
  if (!m) return { ok: false, reason: 'format' }

  const rows = Number(m[1])
  const cols = Number(m[2])
  if (rows < 1 || cols < 1 || rows > rowMax || cols > colMax) {
    return { ok: false, reason: 'range', rows, cols }
  }
  return { ok: true, rows, cols, converted }
}
