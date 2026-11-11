import { api } from './api'
export interface MaterialRow { id: number; name?: string; brand?: string; category?: string; categoryId?: number; description?: string; imageUrls?: string[]; variantDims?: string[]; variantValues?: Record<string, string>; productId?: number; site?: string; sku?: string; spec?: string; stockQty?: number; unitPrice?: number | null; location?: string; ownerName?: string; ownerUsername?: string; currentBorrowers?: string[]; note?: string; archivedAt?: string; applyNo?: string; applicantName?: string; applicantUsername?: string; purpose?: string; remark?: string; status?: string; expectReturnDate?: string; actualReturnDate?: string; createdAt?: string; canHandle?: boolean; [key: string]: any }
export interface MaterialOptions { currentUsername?: string; users: { username: string; name: string }[]; owners: MaterialRow[]; products: MaterialRow[]; categories: { id: number; name: string }[] }
export const materialApi = {
  page: (params: Record<string, unknown>) => api.get<{ list: MaterialRow[]; total: number }>('/admin-api/portal/materials/page', params),
  options: (kind: string) => api.get<MaterialOptions>('/admin-api/portal/materials/options', { kind }),
  save: (resource: string, body: unknown) => api.post<number>(`/admin-api/portal/materials/${resource}/save`, body),
  archive: (resource: string, id: number, restore: boolean) => api.post('/admin-api/portal/materials/archive', { resource, id, restore }),
  remove: (resource: string, id: number) => api.del('/admin-api/portal/materials/delete', { resource, id }),
  owner: (body: unknown) => api.put('/admin-api/portal/materials/owner', body),
  detail: (id: number) => api.get<MaterialRow>('/admin-api/portal/materials/apply', { id }),
  createApply: (body: unknown) => api.post<number>('/admin-api/portal/materials/applies/create', body),
  action: (body: unknown) => api.post('/admin-api/portal/materials/applies/action', body)
}
