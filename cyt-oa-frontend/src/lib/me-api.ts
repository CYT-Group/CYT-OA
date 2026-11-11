import { api } from './api'

/**
 * 个人中心接口层。后端的 Long 走 JSON 字符串，所以编号一律按 string 处理，
 * 只用于回传和拼 URL，不参与运算。
 */

export interface PageResult<T> {
  list: T[]
  total: string | number
}

export interface Greeting {
  text: string
  date: string
  weekday: string
  name: string
}

export interface DeptAction {
  dept: string
  items: string[]
}

export interface RhythmNode {
  id: string
  name: string
  kind: 'sale' | 'new' | 'event' | string
  kindText: string
  startDate: string
  endDate: string
  state: 'upcoming' | 'ongoing' | 'past' | string
  daysLeft: number
  note: string
  deptActions: DeptAction[]
  linkedRefs: string[]
  followed: boolean
}

export interface ReportBrief {
  id: string
  title: string
  periodKey: string
  status: number
  statusText: string
  submittedTime: string
  authorName: string
}

export interface TodoGroup {
  key: string
  title: string
  count: number
  link: string
  items: { title: string; desc: string; link: string }[]
}

export interface Overview {
  greeting: Greeting
  currentRhythm: RhythmNode | null
  todos: TodoGroup[]
  recentReports: ReportBrief[]
  latestReminders: Reminder[]
}

export interface Reminder {
  id: string
  category: string
  type: string
  title: string
  link: string
  read: boolean
  channel: string
  createdAt: string
}

export interface ReminderPreference {
  reportDue: boolean
  overdue: boolean
  milestone: boolean
  corpNews: boolean
}

export interface WorkItem {
  content: string
  progress: number
}

export interface ReportComment {
  id: string
  reviewerId: string
  reviewerName: string
  grade: string
  reviewText: string
  reply: string
  replyTime: string
  createTime: string
}

export interface Report {
  id: string
  no: string
  type: number
  typeText: string
  status: number
  statusText: string
  title: string
  periodKey: string
  startTime: string
  endTime: string
  createTime: string
  summary: string
  plan: string
  problem: string
  remark: string
  workItems: WorkItem[]
  planItems: string[]
  fileUrls: string[]
  authorName: string
  comments: ReportComment[]
}

export interface TrackCell {
  periodKey: string
  label: string
  state: 'submitted' | 'draft' | 'missing' | string
  reportId: string | null
}

export interface TrackWeek {
  periodKey: string
  label: string
  weekStart: string
}

export interface ApplyItem {
  type: string
  typeText: string
  id: string
  no: string
  title: string
  status: number
  statusText: string
  amount: string
  createTime: string
  startTime: string
  endTime: string
  processInstanceId: string | null
}

export interface Footprint {
  reports: number
  submittedReports: number
  applies: number
  recognitions: number
  milestones: number
  attendanceRecords: number
  voices: number
}

export interface Profile {
  name: string
  jobNumber: string
  deptName: string
  postName: string
  postLevel: string
  birthday: string
  entryTime: string
  workAge: number
  workCity: string
  status: number
  entryStatus: number
  probation: number
  regularTime: string
  managerName: string
  subCount: number
  footprint: Footprint
  preference: ReminderPreference
}

export interface MilestoneView {
  id: string
  type: string
  typeText: string
  date: string
  title: string
  daysLeft: number | null
}

/** 同事 / 下属临近的里程碑。blessed 表示我这一天的祝福已送出；action 决定按钮是送祝福还是去安排 */
export interface ColleagueMilestone {
  userId: string
  name: string
  deptName: string
  type: string
  typeText: string
  date: string
  daysLeft: number
  relation: string
  blessed: boolean
  action: 'bless' | 'arrange' | string
}

export interface RecognitionView {
  id: string
  type: string
  typeText: string
  fromId: string
  fromName: string
  scope: string
  note: string
  period: string
  createdAt: string
}

export interface VoiceView {
  id: string
  channel: string
  channelText: string
  anonymous: boolean
  authorName: string
  content: string
  status: string
  createdAt: string
}

export interface TeamMember {
  userId: string
  employeeId: string
  name: string
  postName: string
  deptName: string
  entryTime: string
  cells: TrackCell[]
  /** 本周期我有没有给过他认可：赞按周、之星按月 */
  liked: boolean
  starred: boolean
}

export interface TeamBoard {
  me: { userId: string; name: string }
  members: TeamMember[]
  weeks: TrackWeek[]
  pendingReviews: ReportBrief[]
  submittedThisWeek: number
  star: { userId: string; name: string } | null
}

/** 下拉选项：领用的用品、审批人等 */
export interface Option {
  value: string
  label: string
  hint?: string
}

/**
 * 公司动态走 portal 的登录态展示面 /api/portal/news/**：
 * 编号一律用查询参数传，点赞幂等，评论由后端决定能不能删。
 */
export interface NewsRow {
  id: string
  title: string
  summary: string
  category: string
  author: string
  status: string
  publishedAt: string
}

export interface NewsComment {
  id: string
  displayName: string
  content: string
  createdAt: string
  canDelete: boolean
}

export interface NewsEngagement {
  /** 点赞数是 Long，后端全局序列化成字符串；前端只展示，不参与运算 */
  likeCount: number | string
  liked: boolean
  commentCount: number
  comments: NewsComment[]
}

export interface NewsLikeState {
  likeCount: number | string
  liked: boolean
}

/** 报告状态：后端只有草稿与已提交两态 */
export const REPORT_DRAFT = 1
export const REPORT_SUBMITTED = 2

export const meApi = {
  overview: () => api.get<Overview>('/api/me/overview'),

  rhythmNodes: (params?: { from?: string; to?: string; kind?: string }) =>
    api.get<RhythmNode[]>('/api/me/rhythm/nodes', params),
  followNode: (nodeId: string, remindBefore?: number) =>
    api.post<boolean>('/api/me/rhythm/follow', { nodeId, remindBefore }),
  unfollowNode: (nodeId: string) => api.post<boolean>('/api/me/rhythm/unfollow', { nodeId }),

  reminders: (params: { cat?: string; unreadOnly?: boolean; pageNo?: number; pageSize?: number }) =>
    api.get<PageResult<Reminder>>('/api/me/reminders', params),
  markRead: (id: string) => api.put<boolean>(`/api/me/reminders/${id}/read`),
  markAllRead: (cat?: string) =>
    api.put<{ count: number }>(`/api/me/reminders/read-all${cat ? `?cat=${cat}` : ''}`),
  preferences: () => api.get<ReminderPreference>('/api/me/reminders/preferences'),
  savePreferences: (cmd: Partial<ReminderPreference>) => api.put<boolean>('/api/me/reminders/preferences', cmd),

  reports: (params: { type?: number; status?: number; pageNo?: number; pageSize?: number }) =>
    api.get<PageResult<Report>>('/api/me/reports', params),
  reportDrafts: () => api.get<Report[]>('/api/me/reports/drafts'),
  reportTrack: (params?: { type?: number; weeks?: number }) => api.get<TrackCell[]>('/api/me/reports/track', params),
  reportDetail: (id: string) => api.get<Report>(`/api/me/reports/${id}`),
  saveDraft: (cmd: Record<string, unknown>) => api.post<string>('/api/me/reports/draft', cmd),
  submitReport: (id: string) => api.put<boolean>(`/api/me/reports/${id}/submit`),
  cancelReport: (id: string) => api.put<boolean>(`/api/me/reports/${id}/cancel`),
  comment: (reportId: string, cmd: { grade?: string; reviewText: string }) =>
    api.post<string>(`/api/me/reports/${reportId}/comment`, cmd),
  reply: (commentId: string, reply: string) =>
    api.put<boolean>(`/api/me/reports/comments/${commentId}/reply`, { reply }),
  exportReport: (id: string, no: string) =>
    api.download(`/api/me/reports/${id}/export`, undefined, `工作汇报-${no || id}.pdf`),

  applies: (params: { type: string; status?: number; pageNo?: number; pageSize?: number }) =>
    api.get<PageResult<ApplyItem>>('/api/me/applies', params),
  applyTypes: () => api.get<string[]>('/api/me/applies/types'),
  supplyItems: () => api.get<Option[]>('/api/me/applies/supply-items'),
  recentApplies: (limit = 6) => api.get<ApplyItem[]>('/api/me/applies/recent', { limit }),
  cancelApply: (type: string, id: string, reason?: string) =>
    api.del<boolean>(`/api/me/applies/${type}/${id}`, { reason }),
  createApply: (type: string, body: Record<string, unknown>) =>
    api.post<string>(`/api/me/applies/${type}`, body),
  submitApply: (type: string, body: Record<string, unknown>) =>
    api.put<boolean>(`/api/me/applies/${type}/submit`, body),
  /** 用品的提交只认编号，没有额外表单 */
  submitApplyById: (type: string, id: string) =>
    api.put<boolean>(`/api/me/applies/${type}/submit?id=${id}`),
  /** 请假自选审批人的候选人：在职且有企微账号的同事 */
  approverOptions: () => api.get<Option[]>('/api/me/applies/approvers'),
  /** 审批结论只在企业微信，点一次回读一次；请假也走这里，统一端点 */
  syncApply: (type: string, id: string) =>
    api.put<ApplyItem>(`/api/me/applies/${type}/sync`, { id }),

  profile: () => api.get<Profile>('/api/me/profile'),
  footprint: () => api.get<Footprint>('/api/me/footprint'),
  milestones: () => api.get<MilestoneView[]>('/api/me/milestones'),
  colleagueMilestones: () => api.get<ColleagueMilestone[]>('/api/me/milestones/colleagues'),
  bless: (cmd: { toUserId: string; type: string; date: string }) =>
    api.post<boolean>('/api/me/milestones/blessing', cmd),
  recognitions: (period?: string) => api.get<RecognitionView[]>('/api/me/recognitions', { period }),
  voices: () => api.get<VoiceView[]>('/api/me/voices'),
  submitVoice: (cmd: { channel: string; content: string; anonymous?: boolean }) =>
    api.post<string>('/api/me/voices', cmd),

  teamBoard: () => api.get<TeamBoard>('/api/me/team/board'),
  pendingReviews: () => api.get<ReportBrief[]>('/api/me/team/pending-reviews'),
  teamMilestones: () => api.get<ColleagueMilestone[]>('/api/me/team/milestones'),
  recognize: (cmd: { toUserId: string; type: string; note?: string }) =>
    api.post<boolean>('/api/me/team/recognition', cmd),

  newsPage: (params: { pageNo: number; pageSize: number }) =>
    api.get<PageResult<NewsRow>>('/api/portal/news/page', params),
  newsEngagement: (id: string) =>
    api.get<NewsEngagement>(`/api/portal/news/engagement?id=${encodeURIComponent(id)}`),
  newsLike: (id: string) =>
    api.post<NewsLikeState>(`/api/portal/news/like?id=${encodeURIComponent(id)}`),
  newsUnlike: (id: string) => api.del<NewsLikeState>('/api/portal/news/like', { id }),
  newsComment: (id: string, content: string) =>
    api.post<NewsComment>(`/api/portal/news/comment?id=${encodeURIComponent(id)}`, { content }),
  newsDeleteComment: (id: string, commentId: string) =>
    api.del<boolean>('/api/portal/news/comment', { id, commentId })
}
