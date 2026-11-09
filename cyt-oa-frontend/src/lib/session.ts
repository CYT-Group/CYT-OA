import { reactive, computed } from 'vue'

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
  save(info: LoginInfo) {
    state.info = info
    localStorage.setItem(KEY, JSON.stringify(info))
  },
  clear() {
    state.info = null
    localStorage.removeItem(KEY)
  }
}
