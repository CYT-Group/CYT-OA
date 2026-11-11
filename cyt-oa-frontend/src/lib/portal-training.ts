import { api } from './api'

export interface PortalCategory {
  id: number
  scope: string
  name: string
  code: string | null
  parentId: number | null
  depth: number
  sortOrder: number
  attrs: Record<string, unknown>
  archivedAt?: string | null
  refCount?: number
  childrenCount?: number
}

export interface TrainingCourse {
  id: number
  title: string
  description?: string
  level: string
  instructor?: string
  coverImage?: string
  category: string
  categoryId?: number
  deptScope?: string[] | null
  sortOrder: number
  status: string
  lessonCount: number
  kindCounts?: Record<string, number>
  updatedAt?: string
}

export interface TrainingLesson {
  id: number
  title: string
  kind: string
  sortOrder: number
  sizeBytes?: number
  durationSec?: number
  pageCount?: number
  hasPdf?: boolean
  url?: string
}

export interface TrainingStats { totalCourses: number; usersStarted: number; completions: number }
export interface TrainingBatchResult {
  published?: number
  unpublished?: number
  skipped: { id: number; title: string; reason: string }[]
}
export interface TrainingImportResult {
  categoriesCreated: number
  coursesCreated: number
  coursesSeen: number
  lessonsCreated: number
  lessonsUpdated: number
  skipped: string[]
}

const base = '/admin-api/portal/training'
export const trainingApi = {
  page: (params: Record<string, unknown>) => api.get<{ list: TrainingCourse[]; total: number }>(`${base}/page`, params),
  get: (id: number) => api.get<TrainingCourse>(`${base}/get`, { id }),
  detail: (id: number) => api.get<TrainingCourse & { lessons: TrainingLesson[] }>('/api/portal/training/get', { id }),
  save: (body: Record<string, unknown>) => body.id
    ? api.put<TrainingCourse>(`${base}/update`, body) : api.post<TrainingCourse>(`${base}/create`, body),
  delete: (id: number) => api.del(`${base}/delete`, { id }),
  state: (id: number, action: 'publish' | 'unpublish') => api.post(`${base}/${action}?id=${id}`),
  batch: (ids: number[], action: 'publish' | 'unpublish') => api.post<TrainingBatchResult>(`${base}/batch-${action}`, { ids }),
  saveLesson: (body: Record<string, unknown>) => body.id
    ? api.put<TrainingLesson>(`${base}/lesson/update`, body) : api.post<TrainingLesson>(`${base}/lesson/create`, body),
  deleteLesson: (id: number) => api.del(`${base}/lesson/delete`, { id }),
  uploadUrl: (courseId: number, filename: string, kind: string) =>
    api.post<{ uploadUrl: string; objectKey: string; expiresIn: number }>(`${base}/upload-url`, { courseId, filename, kind }),
  media: (id: number) => api.get<{ url?: string; pdfUrl?: string; pages?: string[] }>(`/api/portal/training/lessons/${id}/media`),
  stats: () => api.get<TrainingStats>(`${base}/stats`),
  scan: () => api.post<TrainingImportResult>(`${base}/import/scan`, undefined, 180000)
}

const categoryBase = '/admin-api/portal/categories'
export const categoryApi = {
  list: (scope: string) => api.get<PortalCategory[]>(`${categoryBase}/list`, { scope }),
  adminList: (scope: string) => api.get<PortalCategory[]>(`${categoryBase}/admin-list`, { scope }),
  save: (id: number | null, body: Record<string, unknown>) => id
    ? api.put<PortalCategory>(`${categoryBase}/update?id=${id}`, body)
    : api.post<PortalCategory>(`${categoryBase}/create`, body),
  state: (id: number, action: 'archive' | 'restore') => api.post<{ detached?: number }>(`${categoryBase}/${action}?id=${id}`),
  delete: (id: number) => api.del(`${categoryBase}/delete`, { id }),
  reorder: (scope: string, parentId: number | null, orderedIds: number[]) => api.post(`${categoryBase}/reorder`, { scope, parentId, orderedIds })
}

export const trainingKinds = [
  { value: 'video', label: '视频' }, { value: 'pdf', label: 'PDF 文档' },
  { value: 'article', label: '图文' }, { value: 'cards', label: '图片卡片' }, { value: 'link', label: '外部链接' }
]

export function kindLabel(value: string) {
  return trainingKinds.find(k => k.value === value)?.label ?? value
}

export function safeMediaUrl(value: string | undefined): string | undefined {
  if (!value) return undefined
  try {
    const url = new URL(value, window.location.origin)
    return ['http:', 'https:'].includes(url.protocol) ? url.href : undefined
  } catch { return undefined }
}
