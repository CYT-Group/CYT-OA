/**
 * usePublishedHrDocs — 「已发布 HR 文档」列表的会话级共享缓存（v1.16）。
 *
 * 两个消费方共用同一份数据，一次会话只发一次请求：
 *   1. `HrDocRefPicker`（块编辑器里选被引用文档）—— 搜索候选；
 *   2. `HrBlocksRenderer`（块渲染端）—— 按 ID 或历史 slug 反查文档的 `theme` / `cover`，
 *      把 `docref` 块画成「封面预览」。
 *
 * 封面是目标文档的属性、不属于引用块。新块存 document_id，旧块仍存 slug，
 * 因此在渲染时按 ID 或历史别名反查。与其为每篇被引用文档各发一次请求，不如复用这个
 * 本来就要拉的已发布文章列表。
 *
 * 失败语义（照 useBrandDocs 的口径）：
 * - 加载失败只置 `failed`，**不**把空列表缓存成「已加载」——下次 `load()` 自动重试；
 * - 并发/乱序：只接受最新一次请求的结果，旧响应到达后丢弃；
 * - 消费方一律**静默降级**：拿不到就回落成朴素引用卡，不报错、不阻塞渲染。
 */
import { ref } from 'vue'
import { articleApi } from '../../bridge/api'
import type { HrDocPublishedItem } from '../types'

const docs = ref<HrDocPublishedItem[]>([])
const loaded = ref(false)   // 至少成功加载过一次
const failed = ref(false)   // 最近一次尝试失败（网络 / 5xx）
let inflight: Promise<void> | null = null
let latest: Promise<void> | null = null

function fetchDocs(): Promise<void> {
  const p: Promise<void> = articleApi
    .listPublished()
    .then((res) => {
      if (latest !== p) return // 已有更新的请求在途/完成，丢弃旧响应
      const rows = Array.isArray(res?.data) ? res.data : []
      docs.value = rows.filter(d => d && Number.isInteger(d.id) && d.id > 0 && d.title)
      loaded.value = true
      failed.value = false
    })
    .catch(() => {
      if (latest !== p) return
      failed.value = true // 不动 docs / loaded：失败不缓存为成功
    })
    .finally(() => {
      if (inflight === p) inflight = null
    })
  latest = p
  inflight = p
  return p
}

export function usePublishedHrDocs() {
  /** 成功加载过则复用；失败过则重试；在途则共享同一 promise */
  function load(): Promise<void> {
    if (loaded.value) return Promise.resolve()
    if (inflight) return inflight
    return fetchDocs()
  }

  /** 新引用按 ID 取，历史引用按 legacy slug 取。 */
  function find(documentId?: number | null, legacySlug?: string | null): HrDocPublishedItem | undefined {
    if (documentId) return docs.value.find(d => d.id === documentId)
    return legacySlug ? docs.value.find(d => d.legacy_slug === legacySlug) : undefined
  }

  return { docs, loaded, failed, load, find }
}
