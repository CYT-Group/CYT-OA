import axios, { AxiosError } from 'axios'
import router from '../router'
import { session } from './session'

/** 后端统一响应 {code, message, msg, data}，code=0 成功。 */
export interface ApiEnvelope<T = unknown> {
  code: number
  message: string
  msg?: string
  data?: T
}

export class ApiError extends Error {
  code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

/** 统一超时：后端偶发不返回时，按钮不能一直停在「处理中」让人只能刷新页面 */
export const http = axios.create({ baseURL: '/', timeout: 20000 })

/** 已经在登录页就沿用原来的 from，否则并发 401 会把 /login?from=/login?from=… 越套越长 */
function toLogin() {
  const cur = router.currentRoute.value
  const from = String(cur.query.from ?? '')
  if (cur.path === '/login') {
    if (!from) return
    router.replace({ path: '/login', query: { from } })
    return
  }
  const target = cur.fullPath === '/login' ? '' : cur.fullPath
  router.push(target ? { path: '/login', query: { from: target } } : { path: '/login' })
}

http.interceptors.request.use((cfg) => {
  if (session.token) cfg.headers['cyt-token'] = session.token
  return cfg
})

http.interceptors.response.use(
  (res): any => {
    // blob 下载保留原始响应（含 headers），不做信封解包
    if (res.config.responseType === 'blob') return res
    const body = res.data as ApiEnvelope
    if (res.status >= 400) {
      const code = body?.code ?? res.status
      const message = body?.message || (res.status === 401 ? '登录状态已失效，请重新登录' : '请求失败')
      if (res.status === 401) {
        session.clear()
        toLogin()
      }
      throw new ApiError(code, message)
    }
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      if (body.code === 401) {
        session.clear()
        toLogin()
      }
      throw new ApiError(body.code, body.message || '请求失败')
    }
    return body
  },
  (err: AxiosError) => {
    const status = err.response?.status
    const text = status === 401 ? '登录状态已失效，请重新登录'
      : status === 403 ? '没有该操作权限'
      : err.code === 'ECONNABORTED' ? '请求超时，请重试'
      : '网络异常，请稍后再试'
    if (status === 401) {
      session.clear()
      toLogin()
    }
    throw new ApiError(status ?? -1, text)
  }
)

export const api = {
  get: <T = unknown>(url: string, params?: Record<string, unknown>) =>
    http.get(url, { params }) as Promise<T>,
  post: <T = unknown>(url: string, body?: unknown, timeout?: number) => http.post(url, body, { timeout }) as Promise<T>,
  put: <T = unknown>(url: string, body?: unknown) => http.put(url, body) as Promise<T>,
  del: <T = unknown>(url: string, params?: Record<string, unknown>) =>
    http.delete(url, { params }) as Promise<T>,
  /** 文件下载：带鉴权头拉取 blob 并触发浏览器保存 */
  download: async (url: string, params?: Record<string, unknown>, fallbackName = 'download') => {
    // 导出可能要生成大文件，单独放宽超时，不受普通请求的 20s 限制
    const res = await http.get(url, { params, responseType: 'blob', timeout: 60000 })
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data as BlobPart])
    const cd = (res.headers['content-disposition'] as string) || ''
    const m = /filename\*=UTF-8''([^;]+)/i.exec(cd) || /filename="?([^";]+)"?/i.exec(cd)
    const name = m ? decodeURIComponent(m[1]) : fallbackName
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = name
    a.click()
    URL.revokeObjectURL(a.href)
  }
}
