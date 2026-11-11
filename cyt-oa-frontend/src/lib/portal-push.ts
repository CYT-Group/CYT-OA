import { api } from './api'
export interface ContentPushResult { jobId?: number; id?: number; status: 'queued' | 'sent' | 'partial' | 'failed' | 'cooldown' | 'no_recipients'; targetCount?: number; sent?: number; failed?: number; trigger?: string; targetKey?: string; errorMsg?: string | null; createdAt?: string; finishedAt?: string }
export const pushApi = {
  send: (kind: 'news' | 'hrdoc', id: number, body: unknown) => api.post<ContentPushResult>(`/admin-api/portal/${kind}/notify?id=${id}`, body),
  test: (kind: 'news' | 'hrdoc', id: number) => api.post<ContentPushResult>(`/admin-api/portal/${kind}/test-push-to-me?id=${id}`),
  latest: (trigger: string, targetKey: string) => api.get<ContentPushResult | null>('/admin-api/portal/push/latest', { trigger, targetKey })
}
