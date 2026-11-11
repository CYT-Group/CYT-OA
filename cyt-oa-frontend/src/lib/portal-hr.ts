import { api } from './api'

export interface HrBlock { type: string; [key: string]: any }
export interface HrDocument { id: number; title: string; category: string; topic: string; status: string; version: string; draftTitle?: string | null; draftVersion?: string | null; contentBlocks?: HrBlock[] | null; draftBlocks?: HrBlock[] | null; contentHtml?: string; draftContent?: string | null; requiresSignoff?: boolean; draftRequiresSignoff?: boolean | null; deptScope?: string[] | null; draftDeptScope?: string[] | null; revision?: string; publishedAt?: string; offlineAt?: string; deletedAt?: string; slug?: string; updatedAt?: string; [key: string]: any }
export interface HrVersion { id: number; version?: string; title?: string; publishedAt?: string; publishedBy?: string; savedBy?: string; createdAt?: string; summary?: string; contentBlocks?: HrBlock[] | null; contentHtml?: string }
export interface SignoffStats { version: string; total_active: number; signed_count: number; not_signed_count: number; coverage_pct: number; not_signed: { username: string; display_name: string; department: string | null }[] }
export const hrApi = {
  signoffDocuments: () => api.get<HrDocument[]>('/admin-api/portal/handbook/documents/options'),
  page: (params: Record<string, unknown>) => api.get<{ list: HrDocument[]; total: number }>('/admin-api/portal/hrdoc/page', params),
  get: (id: number) => api.get<HrDocument>('/admin-api/portal/hrdoc/get', { id }),
  create: (body: unknown) => api.post<HrDocument>('/admin-api/portal/hrdoc/create', body),
  update: (body: unknown) => api.put<HrDocument>('/admin-api/portal/hrdoc/update', body),
  publish: (id: number, revision: string, version: string) => api.post(`/admin-api/portal/hrdoc/publish?id=${id}`, { expectedRevision: revision, version }),
  action: (id: number, action: string, body?: unknown) => api.post(`/admin-api/portal/hrdoc/${action}?id=${id}`, body),
  remove: (id: number) => api.del('/admin-api/portal/hrdoc/delete', { id }),
  versions: (id: number, draft: boolean) => api.get<HrVersion[]>(`/admin-api/portal/hrdoc/${draft ? 'draft-versions' : 'versions'}`, { id }),
  version: (id: number, version: string | number, draft: boolean) => api.get<HrVersion>(`/admin-api/portal/hrdoc/${draft ? 'draft-versions' : 'versions'}/${encodeURIComponent(version)}`, { id }),
  deleteDraft: (id: number, version: number) => api.del(`/admin-api/portal/hrdoc/draft-versions/${version}`, { id }),
  stats: (params: Record<string, unknown>) => api.get<SignoffStats>('/admin-api/portal/handbook/signoff-stats', params),
  notify: (body: unknown) => api.post<{ targetCount: number; sent: number; failed: number; errors?: string[] }>('/admin-api/portal/handbook/notify-unsigned', body, 120000)
}
export const hrCategories = [
  { value: 'handbook', label: '员工手册' }, { value: 'incentive', label: '奖惩制度' },
  { value: 'hr-sop', label: 'HR SOP' }, { value: 'hr', label: '人事制度' }, { value: 'policy', label: '公司制度' }
]
export const hrTopics = [
  { value: 'admin', label: '行政管理' }, { value: 'performance', label: '绩效管理' },
  { value: 'attendance', label: '考勤管理' }, { value: 'salary', label: '薪酬管理' },
  { value: 'benefits', label: '福利关怀' }, { value: 'growth', label: '学习成长' }
]
export const blockTypes = [
  ['text', '文字段'], ['section', '小节标题'], ['image', '图片'], ['imageText', '图文段'],
  ['callout', '提示块'], ['keypoints', '要点列表'], ['cards', '卡片组'], ['steps', '步骤'],
  ['divider', '分隔线'], ['quote', '引用'], ['docref', '文档引用'], ['metrics', '指标卡片'],
  ['compare', '对比卡'], ['status', '状态列表'], ['alert', '警示横幅'], ['bento', '彩边网格'],
  ['numsec', '章节标题'], ['tabcard', '标签指标卡'], ['accordion', '折叠分组'], ['rawhtml', 'HTML 块']
].map(([value, label]) => ({ value: value!, label: label! }))
export function newBlock(type: string): HrBlock {
  const templates: Record<string, object> = {
    text: { text: '' }, section: { title: '' }, image: { image: '', caption: '' }, imageText: { image: '', title: '', text: '' },
    callout: { variant: 'info', text: '' }, keypoints: { items: [{ icon: '•', title: '', desc: '' }] },
    cards: { columns: 2, items: [{ icon: '', title: '', text: '' }] }, steps: { items: [{ title: '', desc: '' }] }, divider: { text: '' },
    rawhtml: { html: '' }, quote: { text: '', color: '', bg: '' }, docref: { title: '', text: '', button_text: '', button_url: '' },
    metrics: { items: [{ value: '', label: '' }] }, compare: { left: { title: '', color: '#16a34a', items: [] }, right: { title: '', color: '#dc2626', items: [] } },
    status: { items: [{ status: 'pass', text: '', tag: '' }] }, alert: { variant: 'info', title: '', text: '' },
    bento: { cards: [{ icon: '', title: '', desc: '', color: '#2563eb' }] }, numsec: { num: '', title: '', text: '', color: '' },
    tabcard: { activeTab: 0, tabs: [{ name: '', header: '', subHeader: '', metrics: [], note: '' }] },
    accordion: { items: [{ icon: '', title: '', lines: [], open: false }] }
  }
  return { type, ...JSON.parse(JSON.stringify(templates[type] ?? {})) }
}
export interface BlockField { key: string; label: string; kind?: 'rich' | 'long' | 'number' | 'bool' | 'lines' | 'select' | 'array' | 'object' | 'image'; options?: { value: string | number; label: string }[]; fields?: BlockField[]; max?: number }
const f = (key: string, label: string, kind?: BlockField['kind']): BlockField => ({ key, label, kind })
const rich = f('text', '正文', 'rich'), title = f('title', '标题'), icon = f('icon', '图标'), color = f('color', '颜色（#RRGGBB）')
const itemFields: Record<string, BlockField[]> = {
  keypoints: [icon, title, f('desc', '说明', 'long')], cards: [icon, title, f('text', '正文', 'long')],
  steps: [title, f('desc', '说明', 'long')], metrics: [f('value', '数值'), f('label', '标签')],
  status: [{ key: 'status', label: '状态', kind: 'select', options: [{ value: 'pass', label: '达标' }, { value: 'fail', label: '未达标' }, { value: 'warn', label: '待改进' }] }, f('text', '事项'), f('tag', '标签')],
  bento: [icon, title, { ...f('desc', '说明', 'rich') }, color],
  accordion: [icon, title, f('lines', '要点（每行一条）', 'lines'), f('open', '默认展开', 'bool')]
}
const arr = (key: string, label: string, fields: BlockField[], max = 30): BlockField => ({ key, label, kind: 'array', fields, max })
const side: BlockField[] = [title, color, f('items', '要点（每行一条）', 'lines')]
export const blockFields: Record<string, BlockField[]> = {
  text: [rich], section: [title], image: [f('image', '图片', 'image'), f('caption', '图说')], imageText: [f('image', '图片', 'image'), title, rich],
  callout: [{ key: 'variant', label: '提示类型', kind: 'select', options: ['info', 'success', 'warning', 'danger'].map((value, i) => ({ value, label: ['信息', '成功', '警告', '危险'][i]! })) }, rich],
  cards: [{ key: 'columns', label: '列数', kind: 'select', options: [{ value: 2, label: '两列' }, { value: 3, label: '三列' }] }, arr('items', '卡片', itemFields.cards!)],
  keypoints: [arr('items', '要点', itemFields.keypoints!)], steps: [arr('items', '步骤', itemFields.steps!)], divider: [f('text', '分隔短语')],
  rawhtml: [f('html', 'HTML 源码（保存时由服务端清洗）', 'long')], quote: [rich, color, f('bg', '背景色（#RRGGBB）')],
  docref: [f('document_id', '引用文档 ID', 'number'), f('slug', '旧文档标识（可空）'), title, rich, f('button_text', '按钮文字'), f('button_url', '按钮地址（可空）')],
  metrics: [arr('items', '指标', itemFields.metrics!)], compare: [{ key: 'left', label: '左栏', kind: 'object', fields: side }, { key: 'right', label: '右栏', kind: 'object', fields: side }],
  status: [arr('items', '状态条目', itemFields.status!)], alert: [{ key: 'variant', label: '横幅类型', kind: 'select', options: ['info', 'success', 'warn', 'danger'].map((value, i) => ({ value, label: ['信息', '成功', '警告', '危险'][i]! })) }, title, rich],
  bento: [arr('cards', '卡片', itemFields.bento!)], numsec: [title, rich, color],
  tabcard: [f('activeTab', '默认页签下标（从 0 开始）', 'number'), arr('tabs', '页签', [f('name', '页签名称'), f('header', '标题'), f('subHeader', '副标题'), arr('metrics', '指标', itemFields.metrics!), f('note', '备注', 'rich')], 8)],
  accordion: [arr('items', '折叠分组', itemFields.accordion!)]
}
export function newFieldObject(fields: BlockField[]): Record<string, any> {
  return Object.fromEntries(fields.map(field => [field.key, field.kind === 'array' || field.kind === 'lines' ? [] : field.kind === 'bool' ? false : field.kind === 'number' ? 0 : field.kind === 'select' ? field.options?.[0]?.value : field.kind === 'object' ? newFieldObject(field.fields ?? []) : field.key === 'color' ? '#2563eb' : '']))
}
/** 属性只允许 HTTPS 和站内绝对路径，阻止伪协议与协议相对地址。 */
export function hrSafeUrl(value: unknown): string {
  const text = typeof value === 'string' ? value.trim() : ''
  return /^(https:\/\/|\/(?!\/))/.test(text) && !/[\u0000-\u001f]/.test(text) ? text : ''
}
export function csvDownload(filename: string, rows: unknown[][]) {
  const cell = (value: unknown) => {
    let text = String(value ?? '')
    // CSV 在 Excel 打开时也应把姓名和账号当文本，避免公式注入。
    if (/^[=+\-@\t\r]/.test(text)) text = "'" + text
    return '"' + text.replace(/"/g, '""') + '"'
  }
  const url = URL.createObjectURL(new Blob(['\uFEFF' + rows.map(row => row.map(cell).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a'); a.href = url; a.download = filename; a.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
