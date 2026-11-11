import { reactive } from 'vue'

export interface LoginInfo {
  token: string
  userId: number
  name: string
  roles: string[]
  permissions: string[]
}

const KEY = 'cyt-oa-session'

const state = reactive<{ info: LoginInfo | null }>({
  info: JSON.parse(localStorage.getItem(KEY) || 'null')
})

const hrWorkspacePaths = new Set([
  '/login', '/forbidden', '/portal/unavailable',
  '/hr/workbench', '/hr/recruit', '/hr/employment', '/hr/templates', '/hr/lifecycle',
  '/page/hrm-employee', '/portal/resumes', '/portal/contracts'
])

export const session = {
  get token() {
    return state.info?.token ?? ''
  },
  get user(): LoginInfo | null {
    return state.info
  },
  get name() {
    return state.info?.name ?? ''
  },
  has(permission: string): boolean {
    const p = state.info?.permissions ?? []
    return p.includes('*:*:*') || p.includes(permission)
  },
  /** 管理区只对管理员开放；员工登录同一个前端也只看到个人区 */
  get isAdmin(): boolean {
    const info = state.info
    if (!info) return false
    return info.permissions.includes('*:*:*') || info.roles.includes('admin')
  },
  get canManagePortal(): boolean {
    return this.has('can_enter_admin')
  },
  get isHrOnly(): boolean {
    const roles = state.info?.roles ?? []
    return !this.isAdmin && roles.includes('hr') && roles.every(role => role === 'hr' || role === 'employee')
  },
  get canAccessConsole(): boolean {
    return this.isAdmin || this.canManagePortal
  },
  get consoleHome(): string {
    if (this.isHrOnly) {
      if (!this.canAccessConsole) return '/forbidden'
      const entries: [string, string][] = [
        ['hrm:home:hr-query', '/hr/workbench'],
        ['hrm:recruit:candidate:query', '/hr/recruit?tab=candidates'],
        ['hrm:recruit:interview:query', '/hr/recruit?tab=interviews'],
        ['hrm:recruit:post:query', '/hr/recruit?tab=posts'],
        ['hrm:employee:query', '/page/hrm-employee?roster=active'],
        ['hrm:contract-template:query', '/hr/templates'],
        ['can_manage_resumes', '/portal/resumes'],
        ['can_generate_contracts', '/portal/contracts']
      ]
      return entries.find(([permission]) => this.has(permission))?.[1] ?? '/forbidden'
    }
    return this.isAdmin ? '/admin' : '/portal'
  },
  /** 专用 HR 身份的旧书签和登录回跳也留在人事工作区；业务权限仍由路由守卫校验。 */
  hrOnlyRedirect(path: string): string | undefined {
    const normalized = path.toLowerCase().replace(/\/+$/, '') || '/'
    return this.isHrOnly && !hrWorkspacePaths.has(normalized) ? this.consoleHome : undefined
  },
  save(info: LoginInfo) {
    state.info = info
    localStorage.setItem(KEY, JSON.stringify(info))
  },
  clear() {
    state.info = null
    localStorage.removeItem(KEY)
  }
}
