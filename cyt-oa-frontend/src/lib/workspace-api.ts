import { api } from './api';

export const workspaceBase = '/api/workspace';
export interface WorkspaceRecord<T = Record<string, unknown>> {
  id: string;
  ownerId: string;
  ownerName?: string;
  parentId?: string | null;
  title: string;
  status: string;
  data: T;
  version: number;
  createdAt: string;
  updatedAt: string;
  editable?: boolean;
}
export interface WorkspaceContext {
  userId: string;
  name: string;
  admin: boolean;
  canManageRules: boolean;
  canManageVoice: boolean;
  canApproveRequests?: boolean;
  teamCount: number;
  deptId: string | null;
}
export interface KeyResult { title: string; target: number; current: number; weight: number; unit: string }
export type Objective = WorkspaceRecord<{ period: string; scope: string; description: string; keyResults: KeyResult[] }> & { progress: number };
export interface WorkspaceVoice { id: string; channel: string; anonymous: boolean; author: string; content: string; status: string; createdAt: string; reply: string; moderation: string; repliedAt?: string }
export interface SurveyQuestion { id: string; title: string; type: string; required: boolean; options: string[]; counts?: Record<string, number>; texts?: string[]; average?: number | null }
export type Survey = WorkspaceRecord<{ description: string; startAt: string; endAt: string; scope: string; targetIds?: string[]; anonymous: boolean; questions: SurveyQuestion[] }> & { active: boolean; answered: boolean; audienceCount: number; responseCount: number };
export type WorkspaceRequest = WorkspaceRecord<Record<string, unknown>> & { typeText: string; canApprove: boolean; steps: { stepNo: number; approverId: string; approverName: string; status: string; comment?: string }[] };
export const workspaceStatus: Record<string, string> = { draft: '草稿', pending: '审批中', approved: '已通过', rejected: '已驳回', cancelled: '已撤销', active: '进行中', completed: '已完成', archived: '已归档', published: '已发布', closed: '已关闭', open: '待处理', handling: '处理中', done: '已回复' };
export const workspaceApi = {
  context: () => api.get<WorkspaceContext>(`${workspaceBase}/context`),
  users: () => api.get<{ value: string; label: string }[]>(`${workspaceBase}/users`),
  objectives: (period: string, alignment = false) => api.get<Objective[]>(`${workspaceBase}/objectives`, { period, alignment }),
  saveObjective: (id: string | undefined, body: unknown) => id ? api.put<Objective>(`${workspaceBase}/objectives/${id}`, body) : api.post<Objective>(`${workspaceBase}/objectives`, body),
  deleteObjective: (id: string) => api.del(`${workspaceBase}/objectives/${id}`),
  policy: (key: string) => api.get<{ version: number; data: Record<string, unknown> }>(`${workspaceBase}/policies/${key}`),
  savePolicy: (key: string, body: unknown) => api.put<{ version: number; data: Record<string, unknown> }>(`${workspaceBase}/policies/${key}`, body),
  voices: (channel: string, manage: boolean) => api.get<WorkspaceVoice[]>(`${workspaceBase}/voices`, { channel, manage }),
  submitVoice: (body: unknown) => api.post(`${workspaceBase}/voices`, body),
  replyVoice: (id: string, body: unknown) => api.put(`${workspaceBase}/voices/${id}/reply`, body),
  surveys: (manage: boolean) => api.get<Survey[]>(`${workspaceBase}/surveys`, { manage }),
  saveSurvey: (id: string | undefined, body: unknown) => id ? api.put<Survey>(`${workspaceBase}/surveys/${id}`, body) : api.post<Survey>(`${workspaceBase}/surveys`, body),
  surveyAction: (id: string, action: 'publish' | 'close' | 'remind') => api.post(`${workspaceBase}/surveys/${id}/${action}`),
  answerSurvey: (id: string, answers: Record<string, unknown>) => api.post(`${workspaceBase}/surveys/${id}/answers`, answers),
  surveyResults: (id: string) => api.get<{ survey: Survey; questions: SurveyQuestion[]; responses: number }>(`${workspaceBase}/surveys/${id}/results`),
  requests: (view: string, type = '', status = '') => api.get<WorkspaceRequest[]>(`${workspaceBase}/requests`, { view, type, status }),
  request: (id: string) => api.get<WorkspaceRequest>(`${workspaceBase}/requests/${id}`),
  saveRequest: (id: string | undefined, body: unknown, submit: boolean) => id ? api.put<WorkspaceRequest>(`${workspaceBase}/requests/${id}?submit=${submit}`, body) : api.post<WorkspaceRequest>(`${workspaceBase}/requests?submit=${submit}`, body),
  requestAction: (id: string, action: 'approve' | 'reject' | 'cancel', comment = '') => api.post(`${workspaceBase}/requests/${id}/${action}`, { comment })
};
