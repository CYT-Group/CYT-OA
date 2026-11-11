import { api } from './api'
import type { BlockField, HrBlock } from './portal-hr'
export type ContentKind = 'news' | 'wiki'
export interface ContentRecord { id: number; title: string; summary?: string; content?: string; category?: string; department?: string; docUrl?: string; tags?: string[]; templateType?: string; blocks?: Record<string, any> | null; coverImage?: string; isFeatured?: boolean; author?: string; authorUsername?: string; status: string; createdAt?: string; publishedAt?: string; viewCount?: number; pendingNotifyWecom?: boolean; pendingNotifyDeptScope?: string[] | null }
export interface NewsComment { id: number; userName?: string; username?: string; displayName?: string; content: string; parentId?: number; createdAt?: string; canDelete: boolean }
export interface NewsEngagement { likeCount: number; liked: boolean; commentCount: number; comments: NewsComment[] }
export const contentApi = {
  page: (kind: ContentKind, query: Record<string, unknown>) => api.get<{ list: ContentRecord[]; total: number }>(`/admin-api/portal/${kind}/page`, query),
  get: (kind: ContentKind, id: number) => api.get<ContentRecord>(`/admin-api/portal/${kind}/get`, { id }),
  save: (kind: ContentKind, body: Record<string, unknown>, id?: number) => id ? api.put<ContentRecord>(`/admin-api/portal/${kind}/update`, { ...body, id }) : api.post<ContentRecord>(`/admin-api/portal/${kind}/create`, body),
  remove: (kind: ContentKind, id: number) => api.del(`/admin-api/portal/${kind}/delete`, { id }),
  review: (kind: ContentKind, decision: string, body: unknown) => api.post(`/admin-api/portal/${kind}/${decision}`, body),
  engagement: (id: number) => api.get<NewsEngagement>('/api/portal/news/engagement', { id }),
  deleteComment: (id: number, commentId: number) => api.del('/api/portal/news/comment', { id, commentId })
}
export const newsTemplates = [
  { value: 'article', label: '图文动态', category: '工作动态' }, { value: 'simple', label: '公司公告', category: '公司公告' },
  { value: 'cross_dept', label: '跨部门通知', category: '跨部门通知' }, { value: 'exhibition', label: '展会动态', category: '展会' },
  { value: 'event', label: '线下活动', category: '线下活动' }, { value: 'brand', label: '品牌风采', category: '品牌风采' },
  { value: 'product', label: '产品动态', category: '产品动态' }, { value: 'team', label: '团队动态', category: '团队聚餐' }
]
export function newsTemplateDefaults(type: string): Record<string, any> {
  const common = { cover_image: '', gallery: [] }
  switch (type) {
    case 'article': return { ...common, intro: '', content_blocks: [{ type: 'text', text: '' }] }
    case 'simple': return { ...common, intro: '', paragraphs: [''] }
    case 'cross_dept': return { ...common, dept: '', intro: '', paragraphs: [''] }
    case 'exhibition': return { ...common, event_name: '', event_date: '', event_location: '', event_booth: '', intro: '', sections: [] }
    case 'event': return { ...common, event_name: '', event_date: '', event_location: '', intro: '', recap_sections: [] }
    case 'brand': return { ...common, brand_name: '', brand_tagline: '', story: '', highlights: [{ value: '', label: '' }] }
    case 'team': return { ...common, occasion: '', message: '' }
    case 'product': return { ...common, brand: '', product_name: '', tagline: '', highlights: [{ icon: '', title: '', text: '' }], specs: [{ label: '', value: '' }], extra_text: '' }
    default: return { ...common }
  }
}
const field = (key: string, label: string, kind?: BlockField['kind']): BlockField => ({ key, label, kind })
const sections: BlockField[] = [field('image', '配图', 'image'), field('title', '小标题'), field('text', '正文', 'rich')]
const array = (key: string, label: string, fields: BlockField[]): BlockField => ({ key, label, kind: 'array', fields, max: 30 })
const intro = field('intro', '引言', 'long'), date = field('event_date', '活动时间'), location = field('event_location', '活动地点')
export const newsTemplateFields: Record<string, BlockField[]> = {
  article: [intro], simple: [intro], cross_dept: [field('dept', '发布部门'), intro],
  exhibition: [field('event_name', '展会名称'), date, location, field('event_booth', '展位'), intro, array('sections', '图文段落', sections)],
  event: [field('event_name', '活动名称'), date, location, intro, array('recap_sections', '现场回顾', sections)],
  brand: [field('brand_name', '品牌名'), field('brand_tagline', '品牌标语'), field('story', '品牌故事', 'rich'), array('highlights', '数据亮点', [field('value', '数值'), field('label', '标签')])],
  team: [field('occasion', '活动场合'), field('message', '感言与寄语', 'rich')],
  product: [field('brand', '品牌'), field('product_name', '产品名'), field('tagline', '一句话卖点'), array('highlights', '产品卖点', [field('icon', '图标'), field('title', '标题'), field('text', '说明')]), array('specs', '规格参数', [field('label', '参数名'), field('value', '参数值')]), field('extra_text', '补充说明', 'rich')]
}
/** 新闻模板旧 wire 字段保持不变；只转换成预览所需块。 */
export function newsPreviewBlocks(type: string, b: Record<string, any>): HrBlock[] {
  const out: HrBlock[] = []
  if (b.intro) out.push({ type: 'text', text: b.intro })
  if (type === 'article') out.push(...(Array.isArray(b.content_blocks) ? b.content_blocks : []))
  if (['simple', 'cross_dept'].includes(type)) out.push(...(b.paragraphs ?? []).map((text: string) => ({ type: 'text', text })))
  if (type === 'brand') { if (b.story) out.push({ type: 'text', text: b.story }); if (b.highlights?.length) out.push({ type: 'metrics', items: b.highlights }) }
  for (const section of [...(b.sections ?? []), ...(b.recap_sections ?? [])]) out.push({ type: 'imageText', ...section })
  if (type === 'team') out.push({ type: 'text', text: b.message ?? '' })
  if (type === 'product') { out.push({ type: 'cards', columns: 3, items: b.highlights ?? [] }); out.push({ type: 'keypoints', items: (b.specs ?? []).map((v: any) => ({ title: v.label, desc: v.value })) }); if (b.extra_text) out.push({ type: 'text', text: b.extra_text }) }
  for (const image of b.gallery ?? []) out.push({ type: 'image', image, caption: '' })
  return out
}
