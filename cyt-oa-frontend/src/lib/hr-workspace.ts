import { api } from './api';
import { portalFile, savePortalFile } from './portal-files';

export const hrBase = '/admin-api/hrm/workspace';
export interface HrTemplate {
  id: number;
  name: string;
  version: string;
  kind: 'onboard' | 'offboard';
  contractType: number;
  scope: string;
  content: string;
  status: 'pending' | 'approved' | 'rejected';
  reviewNote?: string;
  creatorName?: string;
  createdAt?: string;
}
export interface HrEmployee {
  id: string;
  name: string;
  jobNumber: string;
  deptId: string | null;
  deptName: string;
  postName: string;
  entryStatus: number;
  status: number;
  type: number;
  entryTime: string;
  probation: number;
  leaderEmployeeId: string | null;
  postLevel: string | null;
  workAddress: string | null;
  userId?: string;
  mobile?: string;
  email?: string;
}
export interface Candidate {
  id: string;
  name: string;
  postId: string;
  postName: string;
  deptName: string;
  channelId?: string;
  channelName?: string;
  status: number;
  stageNumber?: number;
  mobile: string;
  email?: string;
  education: number;
  sex: number;
  remark?: string;
  employeeId?: string;
}
export interface Interview {
  id: string;
  candidateId: string;
  candidateName: string;
  postName: string;
  stageNumber: number;
  type: number;
  interviewTime: string;
  address: string;
  result: number;
  interviewEmployeeName: string;
  evaluate?: string;
  cancelReason?: string;
}
export interface HrPage<T> {
  list: T[];
  total: number;
}
export const todayShanghai = () =>
  new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(new Date());
export const candidateStages: Record<number, string> = {
  1: '简历筛选',
  2: '初选通过',
  3: '面试中',
  4: '面试通过',
  5: '已发 offer',
  6: '待入职',
  7: '已淘汰',
  8: '已入职'
};
export const stageActions: Record<number, number[]> = {
  1: [2, 4],
  2: [1, 4],
  4: [5, 1, 2],
  7: [1]
};
export const interviewResults: Record<number, string> = {
  1: '待面试',
  2: '通过',
  3: '未通过',
  4: '已取消'
};
export const dateText = (s: unknown) =>
  s ? String(s).replace('T', ' ').slice(0, 16) : '—';
export const textDownload = (name: string, content: string) => {
  const url = URL.createObjectURL(
    new Blob(['\uFEFF', content], { type: 'text/plain;charset=utf-8' })
  );
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
};
export const getTemplates = () => api.get<HrTemplate[]>(`${hrBase}/templates`);

export async function downloadHrFile(
  url: string,
  params: Record<string, unknown> = {},
  fallbackName = '下载文件'
) {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params))
    if (value !== undefined && value !== null) query.set(key, String(value));
  const file = await portalFile(url + (query.size ? `?${query}` : ''));
  savePortalFile(file.blob, file.filename || fallbackName);
}
