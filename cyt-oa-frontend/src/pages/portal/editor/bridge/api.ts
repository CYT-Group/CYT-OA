/**
 * 保留 Portal 原编辑器的 API 契约，身份、持久化及工作任务全部由 OA 提供。
 * blocks/cover/signoff 是原有混合命名的内容格式，不能递归改键。
 */
import { api } from '../../../../lib/api'
import type { HrDocPublishedItem } from '../legacy/types'

type Params = Record<string, any>
export interface LegacyEnvelope<T = any> { success: true; data: T; total?: number }
const BASE = '/admin-api/portal/hrdoc'
const opaque = new Set(['blocks', 'content_blocks', 'draft_blocks', 'contentBlocks', 'draftBlocks', 'cover', 'draft_cover', 'draftCover', 'signoff', 'draft_signoff', 'draftSignoff'])
// OA 的 Long 以字符串返回；原编辑器用数字 ID 严格比较，并直接累加人数。
const numericMetadata = new Set(['id', 'document_id', 'user_id', 'job_id', 'log_id', 'user_count', 'block_count', 'input_chars', 'recipient_count', 'failed_count', 'target_count', 'sent', 'failed', 'recipients', 'progress'])
const snake = (key: string) => key.replace(/[A-Z]/g, letter => '_' + letter.toLowerCase())
const camel = (key: string) => key.replace(/_([a-z])/g, (_, letter: string) => letter.toUpperCase())

export function toLegacy(value: any): any {
  if (Array.isArray(value)) return value.map(toLegacy)
  if (!value || typeof value !== 'object') return value
  return Object.fromEntries(Object.entries(value).map(([key, item]) => {
    const name = snake(key)
    const numeric = numericMetadata.has(name) && typeof item === 'string' && /^\d+$/.test(item) && Number.isSafeInteger(Number(item))
    return [name, opaque.has(key) ? item : numeric ? Number(item) : toLegacy(item)]
  }))
}
export function toOa(value: any): any {
  if (Array.isArray(value)) return value.map(toOa)
  if (!value || typeof value !== 'object') return value
  return Object.fromEntries(Object.entries(value).map(([key, item]) => [camel(key), opaque.has(key) ? item : toOa(item)]))
}

function legacyError(error: unknown): Error {
  const source = error instanceof Error ? error : new Error('请求失败')
  const code = Number((source as any).code)
  const status = code >= 400 && code <= 599 ? code : 422
  Object.assign(source, { response: { status, data: { detail: source.message } } })
  return source
}
async function envelope<T = any>(request: Promise<any>, transform: (value: any) => T = toLegacy): Promise<LegacyEnvelope<T>> {
  try { return { success: true, data: transform(await request) } }
  catch (error) { throw legacyError(error) }
}
function documentView(raw: Params): Params {
  const doc = toLegacy(raw)
  doc.slug ??= doc.legacy_slug ?? null
  if (doc.id) {
    const display = String(import.meta.env.VITE_PORTAL_DISPLAY_BASE_URL || '').replace(/\/$/, '')
    doc.canonical_url = display ? `${display}/article/${doc.id}` : `/portal/hr-documents/id/${doc.id}/preview`
  }
  if (doc.publication_state && doc.revision && !doc.publication_state.revision) doc.publication_state.revision = doc.revision
  return doc
}

async function allDocuments(params: Params = {}): Promise<LegacyEnvelope<Params[]>> {
  const query = toOa(params)
  if ('deleted' in query) query.deleted = Boolean(Number(query.deleted))
  const rows: Params[] = []
  let total = 0
  try {
    for (let pageNo = 1; ; pageNo++) {
      const response = await api.get<{ list: Params[]; total: number }>(`${BASE}/page`, { ...query, pageNo, pageSize: 200 })
      const page = response.list ?? []
      total = Number(response.total)
      rows.push(...page.map(documentView))
      if (!page.length || rows.length >= total) break
    }
    return { success: true, data: rows, total }
  } catch (error) { throw legacyError(error) }
}
export async function resolveDocumentId(idOrSlug: number | string): Promise<number> {
  if (/^\d+$/.test(String(idOrSlug))) return Number(idOrSlug)
  try {
    const found = await api.get<{ id: number | string }>(`${BASE}/resolve`, { slug: idOrSlug })
    const id = Number(found.id)
    if (!Number.isSafeInteger(id) || id <= 0) throw new Error('文档不存在')
    return id
  } catch (error) { throw legacyError(error) }
}
const documentId = resolveDocumentId
async function getDocument(id: number | string): Promise<LegacyEnvelope<Params>> {
  return envelope(api.get(`${BASE}/get`, { id: await documentId(id) }), documentView)
}
async function action(id: number | string, name: string, body?: Params): Promise<LegacyEnvelope<Params>> {
  const resolved = await documentId(id)
  return envelope(api.post(`${BASE}/${name}?id=${resolved}`, body === undefined ? undefined : toOa(body)), documentView)
}

export const articleApi = {
  adminList: allDocuments,
  adminGet: getDocument,
  adminCreate: (body: Params) => envelope(api.post(`${BASE}/create`, toOa(body)), documentView),
  adminUpdate: async (id: number | string, body: Params) => envelope(api.put(`${BASE}/update`, { ...toOa(body), id: await documentId(id) }), documentView),
  adminPublish: async (id: number | string, body?: Params) => {
    if (!body) {
      const doc = (await getDocument(id)).data
      body = { expected_revision: doc.publication_state?.revision || doc.revision, version: doc.draft_version || doc.version }
    }
    return action(id, 'publish', body)
  },
  adminOffline: (id: number | string, reason?: string) => action(id, 'offline', { reason: reason || null }),
  adminRestore: (id: number | string) => action(id, 'restore'),
  adminDelete: async (id: number | string) => envelope(api.del(`${BASE}/delete`, { id: await documentId(id) }), documentView),
  adminUndelete: (id: number | string) => action(id, 'undelete'),
  adminVersions: async (id: number | string) => envelope(api.get(`${BASE}/versions`, { id: await documentId(id) })),
  adminVersion: async (id: number | string, version: number | string) => envelope(api.get(`${BASE}/versions/${encodeURIComponent(String(version))}`, { id: await documentId(id) })),
  adminDraftVersions: async (id: number | string) => envelope(api.get(`${BASE}/draft-versions`, { id: await documentId(id) })),
  adminDraftVersion: async (id: number | string, version: number | string) => envelope(api.get(`${BASE}/draft-versions/${encodeURIComponent(String(version))}`, { id: await documentId(id) })),
  adminDraftVersionDelete: async (id: number | string, version: number | string) => envelope(api.del(`${BASE}/draft-versions/${encodeURIComponent(String(version))}`, { id: await documentId(id) })),
  adminPush: async (id: number | string, body: Params) => envelope(api.post(`${BASE}/compat/notify?id=${await documentId(id)}`, toOa(body), 180000)),
  adminTestPushToMe: async (id: number | string, body: Params = {}) => envelope(api.post(`${BASE}/compat/test-push-to-me?id=${await documentId(id)}`, toOa(body), 120000)),
  listPublished: async (): Promise<LegacyEnvelope<HrDocPublishedItem[]>> => {
    return envelope(api.get('/api/portal/hrdoc/published'), rows => rows.map(documentView))
  },
  get: getDocument,
}

export const hrDocApi = {
  ...articleApi,
  adminGet: getDocument,
  get: getDocument,
  adminSlugAvailable: (slug: string, exclude?: string) => envelope(api.get(`${BASE}/slug-available`, { slug, exclude })),
  adminLayout: (text: string) => envelope(api.post(`${BASE}/layout`, { text }, 180000)),
  adminLayoutFile: (file: File) => {
    const data = new FormData(); data.append('file', file)
    return envelope(api.post(`${BASE}/layout-file`, data, 180000))
  },
  createLayoutJob: (body: Params) => envelope(api.post(`${BASE}/layout/jobs`, toOa(body))),
  createLayoutJobFile: (file: File) => {
    const data = new FormData(); data.append('file', file)
    return envelope(api.post(`${BASE}/layout/jobs/file`, data, 90000))
  },
  listLayoutJobs: () => envelope<any[]>(api.get(`${BASE}/layout/jobs`)),
  getLayoutJob: (id: number) => envelope(api.get(`${BASE}/layout/jobs/${id}`)),
  retryLayoutJob: (id: number) => envelope(api.post(`${BASE}/layout/jobs/${id}/retry`)),
  deleteLayoutJob: (id: number) => envelope(api.del(`${BASE}/layout/jobs/${id}`)),
  adminSearchPushUsers: (q: string) => envelope<any[]>(api.get(`${BASE}/users/search`, { q })),
}

export const directoryApi = {
  departments: () => envelope<any[]>(api.get(`${BASE}/departments/stats`)),
}
export const uploadApi = {
  uploadFile: (file: File, _folder = 'hrdoc') => {
    const data = new FormData(); data.append('file', file); data.append('folder', 'hrdoc')
    return envelope<{ url: string }>(api.post('/admin-api/portal/media/upload', data, 90000))
  },
}
