import type { AxiosResponse } from 'axios'
import { ApiError, http } from './api'

/** 二进制端点失败时也可能回 JSON 信封；不能把错误消息保存成合同或预览文档。 */
export async function portalFile(url: string, body?: unknown, method: 'get' | 'post' = 'get'): Promise<{ blob: Blob; filename: string }> {
  const response = (method === 'post'
    ? await http.post(url, body, { responseType: 'blob', timeout: 180000 })
    : await http.get(url, { responseType: 'blob', timeout: 60000 })) as AxiosResponse<Blob>
  const blob = response.data
  if (String(response.headers['content-type'] ?? blob.type ?? '').includes('json')) {
    const data = JSON.parse(await blob.text()) as { code?: number; message?: string; msg?: string }
    throw new ApiError(data.code ?? -1, data.message || data.msg || '文件请求失败')
  }
  const disposition = String(response.headers['content-disposition'] ?? '')
  const name = /filename\*=UTF-8''([^;]+)/i.exec(disposition) || /filename="?([^";]+)"?/i.exec(disposition)
  let filename = name?.[1] ?? ''
  try { filename = decodeURIComponent(filename) } catch { /* 保留服务器提供的原始名称 */ }
  return { blob, filename }
}

export function savePortalFile(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob), link = document.createElement('a')
  link.href = url; link.download = filename; document.body.appendChild(link); link.click(); link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
