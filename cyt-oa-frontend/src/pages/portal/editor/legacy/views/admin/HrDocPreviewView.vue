<!--
HrDocPreviewView — HR 文档草稿预览（admin 专用，只读）。

以员工视角渲染**草稿**内容：draft_* 字段优先、正式列兜底（effective 口径与
HrDocEditorView.loadDoc 一致）。数据走 adminGet（后端 can_edit_hr_documents 鉴权，
草稿不经员工端接口暴露）；正文渲染复用员工端同款 PageHeader + HrDocRenderer，
保证「预览所见 = 发布后员工所见」。签收区仅展示不可操作；本页不写任何数据。

入口：编辑器顶栏「👁 预览」（新标签页打开）。路由 requires: canEditHrDocs。
-->
<template>
  <div class="oa-hr-preview">
    <PageHeader title="文档预览" :description="doc ? effTitle : '检查草稿在员工阅读端的展示效果。'">
      <template #actions>
        <Button variant="outline" @click="goBack"><ArrowLeft />返回列表</Button>
        <Button v-if="doc && effPdf" variant="outline" @click="printDoc"><Download />导出 PDF</Button>
        <Button v-if="doc" @click="router.push('/portal/hr-documents/id/' + doc.id + '/edit')"><Pencil />编辑文档</Button>
      </template>
    </PageHeader>
    <div v-if="loading" class="pv-state"><p>加载中…</p></div>
    <div v-else-if="error" class="pv-state error">
      <h2>预览加载失败</h2>
      <p>{{ error }}</p>
      <button class="pv-back" @click="goBack">← 返回列表</button>
    </div>
    <template v-else-if="doc">
      <div class="pv-banner"><Eye /><span>草稿预览</span><Badge variant="secondary">{{ effVersion || '未设置版本' }}</Badge><span class="pv-live">{{ liveHint }}</span></div>
      <!-- 整屏封面：与员工端 HrDocPage 同款组件，取草稿优先的 effective 值 -->
      <HrDocCover :title="effTitle" :cover="effCover" :theme="effTheme" />
      <!-- 有大纲条目或侧栏工具列时切两栏网格（与员工端 HrDocPage 同款），预览所见 = 员工所见 -->
      <main
        class="pv-main"
        :class="{ 'has-outline': outlineCount > 0 || hasRail, 'toc-right': effTocPosition === 'right' }"
        :style="themeVars"
      >
        <p v-if="!hasContent" class="pv-empty">（草稿正文为空 — 回编辑器写点内容再来预览）</p>
        <template v-else>
          <!-- v1.19f：侧栏列 = 目录 + 下载 PDF + 签收状态（与员工端 aside.hr-doc-side 同款摆位：
               下载 PDF 在目录下面；preview 态惰性：不打印、不跳转） -->
          <aside v-if="effTocEnabled || hasRail" class="pv-side">
            <HrDocOutlineNav
              v-if="effTocEnabled"
              :container="contentEl"
              :content-key="doc"
              @count="outlineCount = $event"
            />
            <HrDocSideRail
              v-if="hasRail"
              :pdf="!!effPdf"
              :signoff="!!effSignoff"
              preview
              @download="printDoc"
            />
          </aside>
          <div class="pv-body">
            <!-- 引言：与员工端同款（正文最上方一句话，带下边框） -->
            <p v-if="effIntro" class="hr-doc-intro">{{ effIntro }}</p>
            <div ref="contentEl">
              <!-- 双格式分发（与员工端 HrDocPage 同款）：块格式走 HrBlocksRenderer -->
              <HrBlocksRenderer v-if="effBlocks" :blocks="effBlocks" :theme="effTheme" />
              <HrDocRenderer v-else :html="effContent" />
            </div>

            <!-- 签收卡：与员工端 HrDocPage **同款组件**（v1.19，读 effective 的 signoff 配置）；
                 之前这里是写死的白底小按钮桩，既不符合配置化、观感也和员工端对不上 → 换成 HrDocSignoff。
                 preview 态交互惰性（不可真签）。位于 contentEl（目录扫描容器）之外，不进目录。 -->
            <HrDocSignoff
              v-if="effSignoff"
              :signoff="effSignoffCfg"
              :title="effTitle"
              :version="effVersion"
              :chapters-text="effChapters"
              preview
            />

            <!-- 底部标签：与员工端同款 -->
            <div v-if="effTags.length" class="hr-doc-tags">
              <span class="hr-doc-tags-label">标签</span>
              <span v-for="t in effTags" :key="t" class="hr-doc-tag">{{ t }}</span>
            </div>
          </div>
        </template>
      </main>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { PageHeader, Button, Badge } from '../../../bridge/oa-ui'
import { ArrowLeft, Download, Pencil, Eye } from '@lucide/vue'
import HrDocRenderer from '../../components/HrDocRenderer.vue'
import HrBlocksRenderer from '../../components/HrBlocksRenderer.vue'
import HrDocOutlineNav from '../../components/HrDocOutlineNav.vue'
import HrDocCover from '../../components/HrDocCover.vue'
import HrDocSignoff from '../../components/HrDocSignoff.vue'
import HrDocSideRail from '../../components/HrDocSideRail.vue'
import { themeCssVars } from '../../data/hr-themes'
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
// 左侧导航：正文容器 ref 交给 HrDocOutlineNav 扫标题；条目数决定要不要切两栏
const contentEl = ref(null)
const outlineCount = ref(0)

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
const themeVars = computed(() => themeCssVars(effTheme.value))
// 引言 / 标签 / 目录开关与位置：全部走 effective 口径，保证「预览所见 = 员工所见」
const effIntro = computed(() => (doc.value?.draft_intro != null ? doc.value.draft_intro : (doc.value?.intro || '')))
const effTags = computed(() => (doc.value?.draft_tags != null ? doc.value.draft_tags : (doc.value?.tags || [])))
const effTocEnabled = computed(() =>
  doc.value?.draft_toc_enabled != null ? doc.value.draft_toc_enabled : doc.value?.toc_enabled !== false)
const effTocPosition = computed(() =>
  doc.value?.draft_toc_position != null ? doc.value.draft_toc_position : (doc.value?.toc_position || 'left'))
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
// 侧栏工具列（v1.19f）：有 PDF 或签收时侧栏列也要出现（与员工端「工具列常驻」同口径）
const hasRail = computed(() => !!(effPdf.value || effSignoff.value))
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
const hasContent = computed(() =>
  (effBlocks.value ? effBlocks.value.length > 0 : effContent.value.trim().length > 0))
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
/* 预览横幅：夹在导航和 PageHeader 之间，提醒这是草稿而非员工实际所见 */
.pv-banner {
  position: relative; z-index: 1;
  margin-top: var(--nav-h);
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  padding: 10px 24px;
  background: rgba(99,102,241,0.08);
  border-bottom: 1px solid rgba(99,102,241,0.18);
  font-size: 13px; color: var(--t2);
}
.pv-banner + :deep(.page-header) { margin-top: 0; }
.pv-tag { font-weight: 700; color: var(--v); }
.pv-live { color: var(--t3); }
.pv-edit {
  padding: 5px 14px; border-radius: 8px; text-decoration: none;
  background: #fff; border: 1px solid rgba(99,102,241,0.25);
  color: var(--v); font-size: 12.5px; font-weight: 600;
}
.pv-edit:hover { border-color: var(--v); }

.pv-main {
  max-width: 1100px; margin: 0 auto; padding: 24px 32px 80px;
  position: relative; z-index: 1;
}
/* 有大纲时的桌面两栏（与员工端 HrDocPage 同款断点与列宽） */
@media (min-width: 1100px) {
  .pv-main.has-outline {
    max-width: 1380px;
    display: grid;
    grid-template-columns: 240px minmax(0, 1fr);
    gap: 32px;
    align-items: start;
  }
  /* 目录放右侧：换列宽 + 用 order 把导航列排到第二列（与员工端同款） */
  .pv-main.has-outline.toc-right { grid-template-columns: minmax(0, 1fr) 240px; }
  .pv-main.has-outline.toc-right > :first-child { order: 2; }
  /* 整列 sticky：往下读时目录/下载/签收都在眼前（与员工端 .hr-doc-side 同款） */
  .pv-side {
    position: sticky;
    top: calc(var(--nav-h, 64px) + 24px);
    max-height: calc(100vh - var(--nav-h, 64px) - 48px);
    overflow-y: auto;
  }
}
.pv-empty { text-align: center; color: var(--t4); padding: 60px 0; }
/* 侧栏列：目录 + 下载 PDF + 签收状态 竖排（与员工端 aside.hr-doc-side 同款） */
.pv-side { display: flex; flex-direction: column; gap: 16px; min-width: 0; }
/* 🔴 目录组件自带 position:sticky → 往下滚时目录吸顶、下方 PDF 按钮撞上来重叠。
   与员工端同款处理：把目录压成静态（见 HrDocPage 的 .hr-doc-side .hr-outline）。 */
.pv-side :deep(.hr-outline) { position: static; max-height: none; overflow: visible; }

/* ── 正文卡片 ──
   与员工端 .hr-doc-body、编辑器 .doc-paper 同一套值：正文不再直接铺在页面底色上。
   预览页的用途就是「看员工端长什么样」，所以这里的边界感必须和员工端一致。 */
.pv-body {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(99, 102, 241, 0.13);
  border-radius: 16px;
  padding: 36px 44px 48px;
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.06);
}

/* ── 引言 / 底部标签：与员工端 HrDocPage 同名同值 ──
   ⚠️ 这三处（阅读页 / 预览页 / 编辑器分栏预览）的正文装饰样式必须一起改，
   否则「编辑所见 = 预览所见 = 员工所见」破功（同 .pv-body / .doc-paper / .hr-doc-body 那条约定）。 */
.hr-doc-intro {
  margin: 0 0 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--bd);
  font-size: 15px;
  line-height: 1.8;
  color: var(--t3);
}
.hr-doc-tags {
  margin-top: 36px;
  padding-top: 18px;
  border-top: 1px solid var(--bd);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.hr-doc-tags-label { font-size: 12px; color: var(--t4); margin-right: 2px; }
.hr-doc-tag {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.1);
  color: var(--hr-primary, var(--v));
}
.pv-state {
  margin-top: var(--nav-h); padding: 80px 24px; text-align: center;
  color: var(--t3); position: relative; z-index: 1;
}
.pv-state.error h2 { color: var(--t); font-size: 20px; margin-bottom: 8px; }
.pv-back {
  margin-top: 24px; padding: 10px 20px; background: var(--v); color: #fff;
  border: 0; border-radius: 8px; cursor: pointer; font-size: 13px;
}
.pv-banner .pv-back { margin-top: 0; padding: 5px 12px; }

/* 签收卡由 HrDocSignoff 组件自带样式（与员工端同款）；这里只给预览栏内的一点点边距 */
.pv-body :deep(.hr-doc-signoff) { margin: 28px 0 8px; }


/* ── 打印 / 导出 PDF：保留「封面 + 目录 + 正文」的完整样式，只去掉站点外壳、预览横幅、签收卡、侧栏工具列、标签 ── */
@media print {
  @page { size: A4; margin: 14mm; }
  /* 🔴 让浏览器保留背景色/渐变（默认会丢掉 → 封面渐变、卡片底色全没 = 「样式全丢了」） */
  :global(html),
  :global(body) { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }
  :global(.cyt-nav),
  :global(.portal-layout > footer),
  :global(.bg-canvas),
  :global(.aurora),
  :global(.light-streak),
  :global(.grid-pattern),
  :global(.particles),
  :global(.live-clock) { display: none !important; }
  :global(.portal-layout) { background: #fff !important; }

  /* 预览横幅 / 侧栏工具列 / 签收卡 / 标签：不进 PDF（封面保留） */
  .pv-banner,
  .hr-side-rail,
  .hr-doc-signoff,
  .hr-doc-tags { display: none !important; }

  .pv-main {
    max-width: 100% !important;
    margin: 0 !important;
    padding: 0 !important;
    /* flow-root：建 BFC 挡住正文首块的 margin 塌陷（否则整体被顶下 ~35px，短文档会溢出一页） */
    display: flow-root !important;
  }
  /* 🔴 打印时**不能用 grid/flex 排目录 + 正文**：分页上下文里它们不可分片，
     内容略超第 1 页剩余空间就会整块被推到下一页 → 第 1 页只剩封面 + 一大段空白。
     改成 block + 浮动侧栏，可正常跨页流动。 */
  .pv-main.has-outline { display: flow-root !important; }
  .pv-main.has-outline > .pv-side {
    float: left !important;
    width: 180px !important;
    margin: 0 20px 0 0 !important;
  }
  .pv-main.has-outline > .pv-body { margin-left: 200px !important; }
  .pv-main.has-outline.toc-right > .pv-side {
    float: right !important;
    margin: 0 0 0 20px !important;
  }
  .pv-main.has-outline.toc-right > .pv-body { margin-left: 0 !important; margin-right: 200px !important; }
  .pv-main.has-outline::after { content: ''; display: block; clear: both; }
  .pv-side { position: static !important; max-height: none !important; overflow: visible !important; }
  /* 🔴 目录组件自带 `@media (max-width:1099.98px){ .hr-outline{display:none} }`，A4 页宽会命中 → 强制显示 */
  .pv-side :deep(.hr-outline) { display: block !important; }
  .pv-body {
    background: none !important;
    border: 0 !important;
    border-radius: 0 !important;
    box-shadow: none !important;
    padding: 0 !important;
  }
  /* 打印时封面收一档（420 → 340）：屏幕上的整屏封面在纸上会白占近半页，
     短文档刚好被顶出第 2 页（只剩一条空白）。 */
  :deep(.hr-cover) { min-height: 320px !important; }
  /* 块尽量不跨页断开：只对「原子小件」生效。
     🔴 别套到 `.hb`（所有块通用类）上：卡片块可能比整页还高，avoid 会整块推到下一页 → 上一页留一大段空白。 */
  .pv-body :deep(tr),
  .pv-body :deep(img),
  .pv-body :deep(figure),
  .pv-body :deep(pre),
  .pv-body :deep(.hb-quote),
  .pv-body :deep(.hb-alert),
  .pv-body :deep(.hb-metrics) { break-inside: avoid; }
  .pv-body :deep(p) { orphans: 2; widows: 2; }
}

@media (max-width: 768px) {
  .pv-main { padding: 16px 16px 60px; }
  .pv-body { padding: 20px 18px 28px; border-radius: 14px; }
}

</style>
