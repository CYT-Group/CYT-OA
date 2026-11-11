<script lang="ts">
import { defineComponent, h, type VNodeChild } from 'vue'
import { hrSafeUrl } from '../../lib/portal-hr'

const allowed = new Set('p div span strong b em i u s strike h1 h2 h3 h4 h5 h6 ul ol li blockquote pre code br hr a img table thead tbody tr th td figure figcaption sub sup'.split(' '))
const dropped = new Set(['script', 'style', 'iframe', 'object', 'embed', 'svg', 'math', 'form', 'input', 'button', 'link', 'meta'])
/** 解析白名单节点为 Vue VNode，不向 DOM 注入 HTML；旧 HTML 与块内容共用此渲染边界。 */
export default defineComponent({
  props: { html: { type: String, default: '' } },
  setup(props) {
    function node(value: Node): VNodeChild {
      if (value.nodeType === Node.TEXT_NODE) return value.textContent ?? ''
      if (!(value instanceof Element)) return null
      const tag = value.tagName.toLowerCase()
      if (dropped.has(tag)) return null
      const children = Array.from(value.childNodes).map(node)
      if (!allowed.has(tag)) return children
      const attrs: Record<string, unknown> = {}
      if (tag === 'a') { const url = hrSafeUrl(value.getAttribute('href')); if (url) { attrs.href = url; attrs.target = '_blank'; attrs.rel = 'noopener noreferrer' } }
      if (tag === 'img') { const url = hrSafeUrl(value.getAttribute('src')); if (!url) return null; attrs.src = url; attrs.alt = value.getAttribute('alt') ?? ''; attrs.loading = 'lazy' }
      for (const key of ['colspan', 'rowspan']) {
        const v = value.getAttribute(key); if (v && /^\d{1,2}$/.test(v)) attrs[key] = v
      }
      const style: Record<string, string> = {}
      const element = value as HTMLElement
      for (const key of ['color', 'backgroundColor']) {
        const v = element.style?.[key as 'color']; if (v && /^(#[0-9a-f]{3,8}|rgba?\([\d.,%\s]+\)|[a-z]+)$/i.test(v)) style[key] = v
      }
      if (['left', 'center', 'right', 'justify'].includes(element.style?.textAlign)) style.textAlign = element.style.textAlign
      attrs.style = style
      return h(tag, attrs, children)
    }
    return () => h('div', { class: 'hr-html-content' }, Array.from(new DOMParser().parseFromString(props.html, 'text/html').body.childNodes).map(node))
  }
})
</script>
<style scoped>
.hr-html-content { overflow-wrap:anywhere; line-height:1.8; }
.hr-html-content :deep(p) { margin:.5em 0; }
.hr-html-content :deep(h1),.hr-html-content :deep(h2),.hr-html-content :deep(h3),.hr-html-content :deep(h4) { font-weight:600;margin:1em 0 .4em;line-height:1.4; }
.hr-html-content :deep(h2) { font-size:1.5em; }.hr-html-content :deep(h3) { font-size:1.2em; }
.hr-html-content :deep(ul) { list-style:disc;padding-left:1.5em; }.hr-html-content :deep(ol) { list-style:decimal;padding-left:1.5em; }
.hr-html-content :deep(img) { max-width:100%;height:auto; }.hr-html-content :deep(table) { width:100%;border-collapse:collapse; }
.hr-html-content :deep(td),.hr-html-content :deep(th) { border:1px solid var(--border);padding:.4em .6em; }
.hr-html-content :deep(a) { color:var(--primary);text-decoration:underline; }.hr-html-content :deep(blockquote) { border-left:3px solid var(--primary);padding-left:1em; }
</style>
