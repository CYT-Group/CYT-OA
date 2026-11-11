<template>
  <div class="oa-hr-preview">
    <PageHeader :title="doc ? effTitle : '文档预览'" description="检查草稿正文、目录与签收内容。">
      <template #actions>
        <Button variant="outline" @click="goBack"><ArrowLeft />返回列表</Button>
        <Button v-if="doc && effPdf" variant="outline" @click="printDoc"><Download />导出 PDF</Button>
        <Button v-if="doc" @click="router.push('/portal/hr-documents/id/' + doc.id + '/edit')"><Pencil />编辑文档</Button>
      </template>
    </PageHeader>
    <div v-if="loading" class="pv-state" role="status">正在加载文档…</div>
    <div v-else-if="error" class="pv-state" role="alert"><h2>预览加载失败</h2><p>{{ error }}</p><Button variant="outline" @click="goBack">返回列表</Button></div>
    <template v-else-if="doc">
      <div class="pv-banner"><Eye /><span>草稿预览</span><Badge variant="secondary">{{ effVersion || '未设置版本' }}</Badge><span class="pv-live">{{ liveHint }}</span></div>
      <main class="pv-reading-shell">
        <PortalDocumentReader :blocks="effBlocks" :html="effContent" :theme="effTheme" :content-key="doc.id">
          <template #before><HrDocCover class="pv-cover" :title="effTitle" :cover="effCover" :theme="effTheme" compact /><p v-if="effIntro" class="hr-doc-intro">{{ effIntro }}</p></template>
          <template #after>
            <HrDocSignoff v-if="effSignoff" :signoff="effSignoffCfg" :title="effTitle" :version="effVersion" :chapters-text="effChapters" preview />
            <div v-if="effTags.length" class="hr-doc-tags"><span class="hr-doc-tags-label">标签</span><span v-for="tag in effTags" :key="tag" class="hr-doc-tag">{{ tag }}</span></div>
          </template>
        </PortalDocumentReader>
      </main>
    </template>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { PageHeader, Button, Badge } from '../../../bridge/oa-ui'
import { ArrowLeft, Download, Pencil, Eye } from '@lucide/vue'
import PortalDocumentReader from '../../../components/PortalDocumentReader.vue'
import HrDocCover from '../../components/HrDocCover.vue'
import HrDocSignoff from '../../components/HrDocSignoff.vue'
import { withLegacyCoverImage } from '../../data/hr-cover'
import { mergeSignoff } from '../../utils/hr-signoff'
import { articleApi } from '../../../bridge/api'
import { isHrDocumentListHistory } from '../../utils/hr-admin-navigation'

const route = useRoute()
const router = useRouter()
function goBack() {
  if (isHrDocumentListHistory(window.history.state?.back, window.location.origin)) router.back()
  else router.push('/portal/hr-documents')
}
const doc = ref(null)
const loading = ref(true)
const error = ref('')
// effective 口径与编辑器 loadDoc 一致：draft_* 非 null 优先，否则用正式列
const effTitle = computed(() => (doc.value?.draft_title != null ? doc.value.draft_title : (doc.value?.title || '')))
const effVersion = computed(() => doc.value?.draft_version || doc.value?.version || '')
// 展示字段同样走 effective 口径（与编辑器 loadDoc 一致）
const effCover = computed(() => {
  const d = doc.value
  if (!d) return withLegacyCoverImage(null)
  const cover = d.draft_cover != null ? d.draft_cover : d.cover
  const legacy = d.draft_cover_image != null ? d.draft_cover_image : d.cover_image
  return withLegacyCoverImage(cover, legacy)
})
const effTheme = computed(() => (doc.value?.draft_theme != null ? doc.value.draft_theme : (doc.value?.theme || '')))
// 草稿优先读取引言与标签；管理阅读器的目录独立于员工端目录设置。
const effIntro = computed(() => (doc.value?.draft_intro != null ? doc.value.draft_intro : (doc.value?.intro || '')))
const effTags = computed(() => (doc.value?.draft_tags != null ? doc.value.draft_tags : (doc.value?.tags || [])))
const effContent = computed(() => (doc.value?.draft_content != null ? doc.value.draft_content : (doc.value?.content_html || '')))
// 块格式 effective 口径：draft_blocks 非 null → 块草稿优先；
// draft_content 非 null 说明草稿是 HTML 格式（后端保证两种草稿互斥），此时不走块；
// 两种草稿皆无 → 回落已发布 content_blocks（与员工端分发一致）
const effBlocks = computed(() => {
  const d = doc.value
  if (!d) return null
  if (d.draft_blocks != null) return d.draft_blocks   // 块草稿（空数组也算块模式=空草稿）
  if (d.draft_content != null) return null            // HTML 草稿
  return Array.isArray(d.content_blocks) && d.content_blocks.length > 0 ? d.content_blocks : null
})
const effSignoff = computed(() => (doc.value?.draft_requires_signoff != null ? doc.value.draft_requires_signoff : !!doc.value?.requires_signoff))
// 下载 PDF 开关（v1.19e）：draft_download_pdf_enabled 优先、否则正式列
const effPdf = computed(() => (doc.value?.draft_download_pdf_enabled != null ? doc.value.draft_download_pdf_enabled : !!doc.value?.download_pdf_enabled))
// 签收卡配置（v1.19）：draft_signoff 优先、否则正式列；mergeSignoff 补缺键、空值回落默认模板
const effSignoffCfg = computed(() => {
  const d = doc.value
  if (!d) return mergeSignoff(null)
  return mergeSignoff(d.draft_signoff != null ? d.draft_signoff : d.signoff)
})
// 「全部 N 章」：块文档数 section 块个数（与员工端 HrDocPage / 编辑器预览同口径的近似——HTML 旧格式无 section 块则回落「全部内容」）
const effChapters = computed(() => {
  const n = (effBlocks.value || []).filter(b => b && b.type === 'section').length
  return n > 0 ? `全部 ${n} 章内容` : '全部内容'
})
// 员工端现状：已发布快照（content_html 或 content_blocks）非空才对员工可见（草稿员工永远读不到）
const liveHint = computed(() => {
  const d = doc.value
  if (d?.deleted_at) return '员工端不可见（已删除）'
  if (d?.offline_at) return '员工端不可见（已下架）'
  const published = (d?.content_html || '').trim().length > 0 ||
    (Array.isArray(d?.content_blocks) && d.content_blocks.length > 0)
  return published ? `员工端可见：${d.version}` : '员工端不可见（尚未发布）'
})

// 下载 PDF（v1.19e）：走浏览器打印（对话框里可另存为 PDF）。打印期间把 document.title 换成
// 「标题 版本 - 日期」，浏览器打印的页眉/文件名就用它；打完恢复。
function printDoc() {
  const oldTitle = document.title
  const stamp = new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')
  document.title = `${effTitle.value || 'HR 文档'} ${effVersion.value || ''} - ${stamp}`.trim()
  window.print()
  setTimeout(() => { document.title = oldTitle }, 500)
}

onMounted(async () => {
  try {
    const r = await articleApi.adminGet(String(route.params.documentId))
    doc.value = r.data
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.pv-reading-shell { height:calc(100dvh - 260px); min-height:480px; margin-top:16px; overflow:hidden; border:1px solid var(--border); border-radius:10px; background:var(--card); }
.pv-state { display:flex; min-height:300px; flex-direction:column; justify-content:center; align-items:center; gap:16px; color:var(--muted-foreground); font-size:13px; }
.pv-cover { margin-bottom:28px; border-radius:7px; overflow:hidden; }
.pv-cover:not(.has-image) { min-height:220px; }
.pv-cover :deep(.fc-title) { font-size:28px; }
.pv-cover :deep(.fc-scroll-hint) { display:none; }
.pv-banner { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.pv-live { color:var(--muted-foreground); }
.hr-doc-intro { margin:0 0 20px; padding-bottom:16px; border-bottom:1px solid #e2e8f0; color:#738095; font-size:14px; line-height:1.8; }
.hr-doc-tags { display:flex; flex-wrap:wrap; align-items:center; gap:8px; margin-top:28px; padding-top:18px; border-top:1px solid #e2e8f0; font-size:12px; }
.hr-doc-tags-label { color:#738095; }
.hr-doc-tag { padding:3px 9px; border-radius:5px; background:#f1f5f9; color:#475569; }
.pv-reading-shell :deep(.hr-doc-signoff) { margin-top:28px; }
@media (max-width:767px) { .pv-reading-shell { height:calc(100dvh - 280px); min-height:520px; } .pv-cover:not(.has-image) { min-height:180px; } .pv-cover :deep(.fc-title) { font-size:23px; } }
@media print {
  @page { size:A4; margin:14mm; }
  :global(html), :global(body) { -webkit-print-color-adjust:exact!important; print-color-adjust:exact!important; }
  .pv-banner, .hr-doc-tags, .pv-reading-shell :deep(.hr-doc-signoff) { display:none!important; }
  .pv-reading-shell { height:auto; min-height:0; overflow:visible; margin:0; border:0; border-radius:0; background:#fff; }
  .pv-reading-shell :deep(tr), .pv-reading-shell :deep(img), .pv-reading-shell :deep(figure), .pv-reading-shell :deep(pre) { break-inside:avoid; }
  .pv-reading-shell :deep(p) { orphans:2; widows:2; }
}
</style>
