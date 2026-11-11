/**
 * rte-table-extensions.js — 富文本编辑器的表格扩展（列宽持久化 + 表级外观 class）
 *
 * 从 RichTextEditor.vue 抽出来，**只为可测试**：单测 / 回归测试要 import 同一份代码，
 * 复制一份到测试里等于没守住（改了源文件测试还绿）。
 * 见 rte-table-extensions.test.js。
 *
 * 两条关键结论（都是实测踩出来的，别改回去）：
 *
 * 1. **表级外观用 class，不用 data-\***
 *    后端 nh3 白名单（backend/services/html_sanitizer.py 的 ALLOWED_ATTRIBUTES）
 *    只放行 class / id / style，所有 data-* 会被**静默剥掉**。
 *    实测：全库 54 篇 HR 文档里 `data-colwidth` 出现 0 次 ——
 *    也就是说「拖动调列宽」这个既有功能其实一直是失效的（落库时被剥了）。
 *    class 能原样落库 + 原样回读，所以表级开关一律走 class 修饰符，后端零改动。
 *
 * 2. **光 addAttributes 不够，还要子类化 NodeView**
 *    Table 原生没有 addAttributes（dist 里只有 parseHTML/renderHTML）→ setNodeMarkup
 *    写的 attrs 在 getHTML() 时被丢掉；加 addAttributes 后序列化正常了，
 *    但 resizable:true 会启用 TipTap 的 TableView（NodeView），它自建 DOM 且
 *    **只抄 node.attrs.style 到 <table>，完全不抄 class** → 编辑态里 <table> 拿不到 class，
 *    按 class 命中的样式（首列加粗 / 斑马纹）全部失效。
 *    症状很隐蔽：序列化/落库正常，只有界面不生效。
 */
import { Table, TableCell, TableHeader } from '@tiptap/extension-table'

/** 表格基础 class（也由 Table.configure 的 HTMLAttributes 统一补） */
export const BASE_TABLE_CLASS = 'hr-doc-table'
/** 首列做表头（左侧加粗 + 浅底） */
export const CLS_FIRST_COL = 'hr-doc--first-col'
/** 显式关闭斑马纹（默认是开的） */
export const CLS_NO_ZEBRA = 'hr-doc--no-zebra'
/** 自适应宽度：强制铺满正文宽度，忽略表格内联宽度 / 固定列宽（用户 2026-10-27 要求） */
export const CLS_AUTO_WIDTH = 'hr-doc--auto-width'

// ── 列宽持久化 ──
// 序列化时同时写 data-colwidth（编辑器内复制粘贴用）与 style="width:npx"（数据库回读用）；
// 解析时优先读 data-colwidth，退回读 style width。
// ⚠️ data-colwidth 会被后端 nh3 剥掉，但同一单元格的 style:width 会保留下来
//    → 跨保存周期的列宽实际由 style 承载。
export const colwidthAttribute = {
  default: null,
  parseHTML: (el) => {
    const dc = el.getAttribute('data-colwidth')
    if (dc) {
      const arr = dc.split(',').map(n => parseInt(n, 10)).filter(n => !Number.isNaN(n))
      if (arr.length) return arr
    }
    const w = el.style?.width
    if (w && /px$/.test(w)) {
      const total = Math.round(parseFloat(w))
      if (total > 0) {
        const span = parseInt(el.getAttribute('colspan') || '1', 10) || 1
        return Array.from({ length: span }, () => Math.round(total / span))
      }
    }
    return null
  },
  renderHTML: (attrs) => {
    if (!attrs.colwidth) return {}
    return {
      'data-colwidth': attrs.colwidth.join(','),
      style: `width: ${attrs.colwidth.reduce((a, b) => a + b, 0)}px`,
    }
  },
}

// ── 表头底色（v1.14 新增，用户要求「表头底色可编辑」）──
// 落在 <th> 的 style="background-color: ..." 上；后端 nh3 白名单（ALLOWED_CSS_PROPERTIES）
// 已放行 background-color，可原样落库 + 回读。渲染端 .hb-rt / .hr-doc-content 的 th
// 默认背景用 rgba(var(--hr-primary-rgb),0.06)，内联 background-color 优先级更高会盖掉它，
// 清掉 thbg（置空）即回到主题浅底。
// ⚠️ parseHTML 读的是 DOM 归一化后的 backgroundColor（rgb(...)），统一转回 #rrggbb 存，
//    否则 <input type=color> 拿 rgb(...) 会显示异常；renderHTML 原样输出。
function normalizeCssColor(c) {
  c = String(c || '').trim()
  if (!c) return null
  const m = c.match(/^rgb\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)$/i)
  if (m) {
    const h = (n) => Number(n).toString(16).padStart(2, '0')
    return `#${h(m[1])}${h(m[2])}${h(m[3])}`
  }
  if (/^#[0-9a-fA-F]{3}$/.test(c) || /^#[0-9a-fA-F]{6}$/.test(c)) return c
  return c // color name 等其它合法形式原样保留
}
export const thbgAttribute = {
  default: null,
  parseHTML: (el) => normalizeCssColor(el.style?.backgroundColor),
  renderHTML: (attrs) => {
    if (!attrs.thbg) return {}
    return { style: `background-color: ${attrs.thbg}` }
  },
}

export const TableCellWithWidth = TableCell.extend({
  addAttributes() { return { ...this.parent?.(), colwidth: colwidthAttribute } },
})
export const TableHeaderWithWidth = TableHeader.extend({
  addAttributes() { return { ...this.parent?.(), colwidth: colwidthAttribute, thbg: thbgAttribute } },
})

// ── 表级 class 属性 ──
// parseHTML：把 <table class="hr-doc-table hr-doc--first-col …"> 的整串 class 读回来。
// renderHTML：要**去掉 base class** 再输出（base 由 options.HTMLAttributes 统一补），
// 否则 mergeAttributes 会把两串拼起来 → class 里出现两个 hr-doc-table。
export const tableClassAttribute = {
  default: null,
  parseHTML: (el) => el.getAttribute('class'),
  renderHTML: (attrs) => {
    const rest = String(attrs.class || '')
      .split(/\s+/)
      .filter(c => c && c !== BASE_TABLE_CLASS)
    return rest.length ? { class: rest.join(' ') } : {}
  },
}

/** 把 class 同步进 NodeView 真实 DOM 的 View 子类工厂 */
export function makeTableClassView(BaseView) {
  return class extends BaseView {
    constructor(node, cellMinWidth) {
      super(node, cellMinWidth)
      this.__syncClass()
    }
    update(node) {
      const ok = super.update(node)
      if (ok !== false) this.__syncClass()
      return ok
    }
    __syncClass() {
      const raw = String((this.node && this.node.attrs && this.node.attrs.class) || '')
      const extra = raw.split(/\s+/).filter(c => c && c !== BASE_TABLE_CLASS)
      this.table.className = [BASE_TABLE_CLASS, ...extra].filter(Boolean).join(' ')
    }
  }
}

/** 最终给 RichTextEditor 用的表格节点扩展：class 属性 + class 同步进 NodeView */
export const TableNode = Table.extend({
  addAttributes() {
    return { ...this.parent?.(), class: tableClassAttribute }
  },
  addOptions() {
    const parent = this.parent?.() || {}
    return { ...parent, View: makeTableClassView(parent.View) }
  },
})

/** 表级 class 辅助：在 class 数组里增删一个修饰符 token（给 RteTableMenu 用） */
export function toggleClassTokens(list, token, on) {
  const arr = [...list]
  const has = arr.includes(token)
  if (on == null ? has : !on) return arr.filter(c => c !== token)
  return has ? arr : [...arr, token]
}
