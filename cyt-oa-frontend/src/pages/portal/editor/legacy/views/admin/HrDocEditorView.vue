<!--
HrDocEditorView — HR 文档编辑器（新建 + 编辑），Confluence 风格文档画布

双模式（口径-HR文档块编辑-前端.md §2）：
- 块模式：正文 = HrBlockListEditor 块列表（11 种块）；保存/提交发 blocks 字段。
  文档有 blocks 草稿或块快照、以及新建文档，走这条。
- 整篇模式：老格式文档（纯 content_html）现行为保留——sticky 工具栏 / 大纲 /
  表格 hack 全部不变；顶栏「转换为块模式」可一键把 HTML 无损包进单个 text 块
  （转换不可自动回退，保存前离开可放弃）。

布局：sticky 顶栏（状态 + 操作）/ 左侧大纲（整篇=正文 H1-H4；块=section 块
     标题 + text 块内 h2-h4）/ 居中「一张纸」/ 右侧「文档设置」抽屉（元信息）。
编辑区排版与 HrDocRenderer 员工端渲染一致，不再需要预览切换。

工作流（v1.9：审批环节已下线，草稿直接发布）：
- 新建：填元信息 + 富文本 → 保存草稿 → 点「发布」直接对员工可见
- 编辑：拉出文档 → 改 draft_content → 保存 → 发布
- admin 改稿期间 content_html（已发布快照）不动，员工读不到草稿

字段约束：
- category：handbook / incentive / hr / hr-sop / policy
- dept_scope：限定部门可见（多选）；空 = 全员可见
- draft_version：目标发布版本号；发布前必填
-->
<template>
  <div class="doc-ed" :inert="publicationLocked" :aria-busy="submitting || saving">
    <div class="doc-page-heading">
      <Button class="tb-back" variant="ghost" size="icon" @click="goBack" title="返回列表" aria-label="返回文档列表"><ArrowLeft /></Button>
      <PageHeader :title="isNew ? '新建 HR 文档' : '编辑 HR 文档'" :description="form.title || '编写制度内容，配置阅读与签收要求。'">
        <template #badge><Badge v-if="doc" :variant="displayStatus === 'published' ? 'success' : 'secondary'">{{ ({draft:'草稿',published:'已发布',offline:'已下架',pending:'待审',rejected:'已驳回'})[displayStatus] || displayStatus }}</Badge><Badge v-else variant="secondary">新建草稿</Badge></template>
        <template #actions>
          <span v-if="dirty" class="tb-dirty" role="status"><span></span>未保存</span>
          <span v-else-if="!isNew && !saving && !loading && !submitting" class="tb-save-state" role="status"><Check />已保存</span>
          <Button variant="outline" class="doc-save-action" :disabled="publicationLocked" :loading="saving" @click="saveDraft"><Save v-if="!saving" />{{ saveBtnLabel }}</Button>
          <Button v-if="!isNew && (doc?.status !== 'published' || isOffline)" :disabled="publicationLocked" :loading="submitting" @click="isOffline && canRestore ? restoreNow() : publishNow()"><Send v-if="!submitting" />{{ submitting ? '处理中…' : (isOffline && canRestore ? '重新上架' : '发布') }}</Button>
          <Button ref="settingsToggleRef" variant="outline" :aria-expanded="settingsOpen" aria-controls="doc-settings-panel" :class="{ 'doc-setting-active': settingsOpen }" @click="settingsOpen = !settingsOpen"><Settings2 />设置</Button>
        </template>
      </PageHeader>
    </div>

    <div ref="docToolbarEl" class="doc-topbar" role="toolbar" aria-label="文档编辑工具">
      <div class="vm-toggle" role="group" aria-label="视图模式">
        <button type="button" :class="{ active: viewMode === 'edit' }" :aria-pressed="viewMode === 'edit'" @click="viewMode = 'edit'" title="编辑（原布局：左大纲 + 纸面 + 右侧设置）"><Pencil />编辑</button>
        <button type="button" :class="{ active: viewMode === 'split' }" :aria-pressed="viewMode === 'split'" @click="viewMode = 'split'" title="分栏：左边编辑，右边实时预览，中间可拖动调宽"><Columns2 />分栏</button>
        <button type="button" :class="{ active: viewMode === 'preview' }" :aria-pressed="viewMode === 'preview'" @click="viewMode = 'preview'" title="只看预览"><Eye />预览</button>
      </div>
      <span class="doc-context-meta">{{ mode === 'blocks' ? form.blocks.length + ' 个内容块' : '富文本' }}<span>{{ wordCount }} 字</span></span>
      <div class="doc-tools-right">
        <Button variant="ghost" size="sm" :disabled="loading" @click="layoutOpen = true" title="上传文档或粘贴全文，AI 自动拆章节并匹配块类型"><Sparkles />智能排版</Button>
        <HrLayoutJobsLauncher class="doc-jobs-inline" hide-when-empty />
        <Button v-if="!isNew && !loading" variant="ghost" size="sm" :aria-expanded="historyOpen" @click="historyOpen = !historyOpen"><History />草稿历史</Button>
        <DropdownMenu>
          <template #trigger><Button variant="ghost" size="icon-sm" aria-label="更多编辑操作"><Ellipsis /></Button></template>
          <DropdownMenuItem v-if="!isNew && doc?.id" :disabled="testPushing" @select="testPushToMe"><Smartphone />{{ testPushing ? '正在推送…' : '测试推送给自己' }}</DropdownMenuItem>
          <DropdownMenuItem v-if="mode === 'html' && !loading" @select="convertOpen = true"><Blocks />转换为块模式</DropdownMenuItem>
          <DropdownMenuItem v-if="layoutUndo" @select="undoLayoutApply"><Undo2 />撤销智能排版</DropdownMenuItem>
          <DropdownMenuItem :disabled="loading" @select="settingsOpen = !settingsOpen"><Settings2 />文档设置</DropdownMenuItem>
        </DropdownMenu>
      </div>
      <div v-if="mode === 'html' && rteEditor" class="doc-html-toolbar"><RteToolbar :editor="rteEditor" :img-busy="!!rteRef?.imgUploading" @link="rteRef?.openLink()" @image="rteRef?.pickImage()" /></div>
    </div>

    <div v-if="loading" class="loading-state">加载中…</div>

    <template v-else>
      <div v-if="doc?.offline_at" class="doc-banner offline">
        ⏸ 本文档<strong>已下架</strong>，员工端不可见
        <template v-if="doc.offline_reason">（原因：{{ doc.offline_reason }}）</template>。
        内容没改 → 点右上角「重新上架」即可恢复；改了内容 → 保存后点「发布」，会发布新版本并同时恢复可见。
      </div>
      <div v-if="doc?.status === 'pending'" class="doc-banner warn">
        ⚠️ 这篇文档停留在历史遗留的「待审」状态（<strong>审批环节已下线</strong>）。点右上角「发布」即可直接对员工可见。
      </div>
      <div v-if="doc?.status === 'rejected'" class="doc-banner reject">
        ❌ 这篇文档此前被驳回过。修改后点右上角「发布」直接发布（不再需要送审）。
      </div>

      <div class="doc-main" :class="[`vm-${viewMode}`, { dragging }]">
        <!-- ── 左侧大纲（仅编辑态；分栏/预览要让出空间）── -->
        <aside v-if="viewMode === 'edit'" class="doc-outline">
          <div class="ol-title">大纲</div>
          <div v-if="!outline.length" class="ol-empty">{{ mode === 'blocks' ? '「小节标题」块和文字段里的标题（2/3/4 级）会自动出现在这里' : '正文里的标题（标题 2/3/4）会自动出现在这里' }}</div>
          <button
            v-for="(h, i) in outline" :key="i"
            class="ol-item" :class="`lv-${h.level}`"
            @click="jumpToHeading(i)"
          >{{ h.text || '（空标题）' }}</button>
        </aside>

        <!-- ── 文档纸面（预览态用 v-show 隐藏：v-if 会卸载 TipTap 实例、丢编辑态）── -->
        <div v-show="viewMode !== 'preview'" ref="paperEl" class="doc-paper" :style="paperStyle">
          <input
            v-model="form.title" class="doc-title" type="text"
            placeholder="无标题文档" maxlength="120"
          >
          <button class="doc-subline" type="button" @click="settingsOpen = true" title="点击打开文档设置">
            <span>{{ CATEGORY_LABEL[form.category] || form.category }}</span>
            <span class="dot">·</span>
            <span>目标 {{ form.draft_version || '（发布时填写）' }}</span>
            <span class="dot">·</span>
            <span>{{ form.requires_signoff ? '需签收' : '普通可读' }}</span>
            <span class="dot">·</span>
            <span>{{ deptScopeLabel }}</span>
            <span class="dot">·</span>
            <span>{{ wordCount }} 字</span>
            <template v-if="doc?.updated_by">
              <span class="dot">·</span>
              <span>{{ doc.updated_by }} 编辑于 {{ (doc.updated_at || '').slice(0, 16).replace('T', ' ') }}</span>
            </template>
          </button>
          <!-- 引言（v1.5）：渲染在正文最上方一句话，对齐参考「制度编辑器」 -->
          <input
            v-model="form.intro" class="doc-intro" type="text" maxlength="500"
            placeholder="引言（可选）—— 一句话概括重点，会显示在正文最上方"
          >
          <RichTextEditor v-if="mode === 'html'" ref="rteRef" v-model="form.content_html" :theme="form.theme" class="doc-rte" hide-toolbar placeholder="直接开始写正文，支持标题 / 表格 / 图片 / 列表…" />
          <HrBlockListEditor
            v-else
            v-model:blocks="form.blocks"
            :errors="blockErrors"
            :hide-preview="viewMode !== 'edit'"
            :theme="form.theme"
            class="doc-blocks"
          />
        </div>

        <!-- ── 内联预览（v1.6）：分栏时与纸面并排、中间可拖；预览态独占整宽 ── -->
        <div
          v-if="viewMode === 'split'"
          class="pv-resizer" title="拖动调整左右宽度"
          @mousedown="startSplitDrag"
        ></div>
        <aside v-if="viewMode !== 'edit'" ref="previewEl" class="doc-preview-pane">
          <div class="pv-head">
            <span class="pv-dot"></span>
            <span>预览效果</span>
            <span class="pv-hint">实时更新</span>
          </div>
          <div class="pv-body">
            <!-- 整屏封面：与阅读页/预览页同款组件（compact 档收字号） -->
            <HrDocCover
              compact :title="form.title || '（未填标题）'"
              :cover="form.cover" :theme="form.theme"
            />
            <!-- 目录 + 侧栏工具列 + 正文：与员工阅读页同款两栏（左/右由「目录位置」决定）。
                 预览栏窄于 560px 时自动退回单栏 —— 用**容器查询**而不是视口媒体查询，
                 因为这里要判断的是「预览栏自己有多宽」，不是整个窗口。
                 v1.19f：下载 PDF + 签收状态挪进侧栏列（目录下面），与阅读页摆位一致
                 （用户拍板「下载 PDF 放目录下面」，不再是正文下方的横向小条）。 -->
            <div
              class="pv-layout"
              :class="{ 'has-toc': paneTwoCols, 'toc-right': form.toc_position === 'right' }"
            >
              <aside v-if="form.toc_enabled || paneHasRail" class="pv-side">
                <HrDocOutlineNav
                  v-if="form.toc_enabled"
                  :container="paneContentEl"
                  :content-key="paneOutlineKey"
                  :fab="false"
                  @count="paneOutlineCount = $event"
                />
                <!-- 下载 PDF + 签收状态（签收惰性；PDF 真实可用），放目录下面 -->
                <HrDocSideRail
                  v-if="paneHasRail"
                  :pdf="!!form.download_pdf_enabled"
                  :signoff="!!form.requires_signoff"
                  preview
                  @download="printDoc"
                />
              </aside>
              <!-- v1.19i：正文列 = 正文（paneContentEl，供目录扫描）+ 签收卡 + 标签，
                   与员工端 .hr-doc-body 同构 —— 签收卡跟正文同宽、左对齐（不再通栏）。
                   ⚠️ 签收卡仍放在 paneContentEl **之外**（否则会被 HrDocOutlineNav 扫成目录条目）。 -->
              <div class="pv-main-col" :style="previewThemeVars">
                <div ref="paneContentEl" class="pv-content">
                  <p v-if="form.intro" class="pv-intro">{{ form.intro }}</p>
                  <HrBlocksRenderer v-if="mode === 'blocks'" :blocks="form.blocks" :theme="form.theme" />
                  <HrDocRenderer v-else :html="form.content_html" />
                </div>
                <HrDocSignoff
                  v-if="form.requires_signoff"
                  :signoff="mergeSignoff(form.signoff)"
                  :title="form.title || '本文档'"
                  :version="form.draft_version || form.version || ''"
                  :chapters-text="previewChapters"
                  preview
                />
                <div v-if="form.tags.length" class="pv-tags">
                  <span class="pv-tags-label">标签</span>
                  <span v-for="t in form.tags" :key="t" class="pv-tag">{{ t }}</span>
                </div>
              </div>


          </div>
          </div>
        </aside>

        <!-- ── 右侧文档设置：推拉面板（展开时挤压正文，非浮层）──
             三态都保留：分栏/预览的意义就是「边改边看」，把设置藏了就改不了封面/主题/标签了。
             它收起时 width:0 + 负 margin 抵消 gap，不占空间，所以留着没有代价。 -->
        <aside id="doc-settings-panel" class="doc-settings" :class="{ open: settingsOpen }" aria-label="文档设置">
          <div class="st-inner">
          <div class="st-head">
            <div><h2>文档设置</h2><p>管理发布信息与阅读体验</p></div>
            <button type="button" class="st-close" aria-label="关闭文档设置" @click="closeSettings">✕</button>
          </div>
          <div class="st-body">
            <section class="st-card" aria-label="归属与版本"><div class="st-card-title"><h3>归属与版本</h3></div>
            <div class="field">
              <label>一级分类 <span class="req">*</span></label>
              <SelectField v-model="form.category" :options="categoryOptions" aria-label="一级分类" />
            </div>
            <!-- 二级分类只有「HR 制度」有（对应制度中心 /hr 的 6 个 tab）；
                 员工手册 / 奖惩制度 / SOP 是单篇或独立列表，没有二级。 -->
            <div v-if="form.category === 'hr'" class="field">
              <label>二级分类</label>
              <SelectField v-model="form.topic" :options="topicOptions" aria-label="二级分类" />
            </div>
            <div class="field-row">
              <div class="field">
                <label for="doc-setting-published-version">已发布版本</label>
                <input id="doc-setting-published-version" :value="publishedVersion || '尚未发布'" type="text" disabled>
              </div>
              <div class="field">
                <label for="doc-setting-draft_version">目标版本号</label>
                <input
                  id="doc-setting-draft_version" v-model="form.draft_version" type="text"
                  placeholder="如 v1.1"
                >
              </div>
            </div>
            <small class="hint">保存草稿不要求改号；发布时再确认版本，可在确认框内修改。</small>
            </section>
            <section class="st-card" aria-label="阅读与权限"><div class="st-card-title"><h3>阅读与权限</h3></div>
            <div class="field">
              <label class="toggle">
                <input v-model="form.requires_signoff" type="checkbox" role="switch" aria-label="需要签收" :aria-checked="form.requires_signoff"><i class="st-switch-track" aria-hidden="true"></i>
                <span>需要签收 — {{ form.requires_signoff ? '员工需点签收' : '普通可读' }}</span>
              </label>
            </div>
            <div class="field">
              <small v-if="doc?.legacy_slug" class="hint">
                📲 发布<b>不会</b>自动推送：推送改在<b>文章列表页</b>点「推送企微」，
                可选按适用范围 / 指定员工（单次上限 50 人）。
                同一文档 + 版本 24 小时内最多推一次（防误连点）。
              </small>
              <small v-else class="hint">📲 发布不会自动推送；发布后可在文档列表手动推送企微。</small>
            </div>
            <div class="field">
              <label>限定部门（不选 = 全员可见）</label>
              <DeptScopePicker v-model="form.dept_scope" />
              <small class="hint">仅这些部门 + admin 能读到内容；签收/统计的分母也按此过滤</small>
            </div>

            <!-- v1.19：阅读页「下载 PDF」开关（默认关；走浏览器打印导出） -->
            <div class="field">
              <label class="toggle">
                <input v-model="form.download_pdf_enabled" type="checkbox" role="switch" aria-label="阅读页显示下载 PDF" :aria-checked="form.download_pdf_enabled"><i class="st-switch-track" aria-hidden="true"></i>
                <span>阅读页显示「下载 PDF」 — {{ form.download_pdf_enabled ? '员工可导出 PDF' : '不显示' }}</span>
              </label>
              <small class="hint">默认关闭；开启后工具列出现「下载 PDF」，点了走浏览器打印再「另存为 PDF」</small>
            </div>

            <div class="field">
              <label class="toggle">
                <input v-model="form.toc_enabled" type="checkbox" role="switch" aria-label="显示本文目录" :aria-checked="form.toc_enabled"><i class="st-switch-track" aria-hidden="true"></i>
                <span>显示本文目录 — {{ form.toc_enabled ? '阅读页显示目录栏' : '不显示' }}</span>
              </label>
            </div>
            <div v-if="form.toc_enabled" class="field">
              <label>目录位置</label>
              <div class="toc-pos">
                <button type="button" :aria-pressed="form.toc_position === 'left'" :class="{ active: form.toc_position === 'left' }" @click="form.toc_position = 'left'">左侧</button>
                <button type="button" :aria-pressed="form.toc_position === 'right'" :class="{ active: form.toc_position === 'right' }" @click="form.toc_position = 'right'">右侧</button>
              </div>
              <small class="hint">目录自动从「小节标题」与「文字段内的 H2-H4」提取</small>
            </div>

            <!-- ── 文档级展示字段（v1.5）：对齐参考「制度编辑器」的主题 / 标签 / 目录 ── -->
            </section>
            <section class="st-card" aria-label="主题与标签"><div class="st-card-title"><h3>主题与标签</h3></div>
            <div class="field">
              <label>主题配色</label>
              <div class="theme-row">
                <button
                  v-for="t in HR_THEMES" :key="t.key" type="button" class="theme-swatch"
                  :class="{ active: form.theme === t.key }" :style="{ '--swatch': t.primary }"
                  :title="t.name" :aria-label="t.name" :aria-pressed="form.theme === t.key" @click="form.theme = t.key"
                ><span class="theme-name">{{ t.name }}</span><span v-if="form.theme === t.key" class="theme-check" aria-hidden="true">✓</span></button>
                <button
                  type="button" class="theme-swatch theme-swatch-default"
                  :class="{ active: form.theme === '' }" :aria-pressed="form.theme === ''" title="默认（品牌紫）"
                  @click="form.theme = ''"
                >品牌紫<span v-if="form.theme === ''" aria-hidden="true"> ✓</span></button>
                <label class="theme-custom" title="自由取色">
                  <input v-model="customThemeColor" type="color" aria-label="自定义主题色">
                  <span>自选色</span>
                </label>
              </div>
              <small class="hint">当前：{{ resolveTheme(form.theme).name }} · 作用于封面与正文配色</small>
            </div>
            <div class="field">
              <label>标签</label>
              <div class="tags-input">
                <span v-for="(t, i) in form.tags" :key="t + i" class="tag-chip">
                  {{ t }}<button type="button" class="tc-x" title="移除" @click="form.tags.splice(i, 1)">×</button>
                </span>
                <input
                  v-model="tagDraft" aria-label="添加正文标签" type="text" class="tag-input" maxlength="20"
                  placeholder="添加标签…" @keydown.enter.prevent="addTag"
                >
              </div>
              <small class="hint">最多 10 个、每个 ≤20 字；会显示在正文末尾</small>
            </div>
            </section>
            <section class="st-card" aria-label="封面"><div class="st-card-title"><h3>封面</h3></div>
            <!-- ── 整屏封面（v1.6）：对齐参考更新后的 pc-fullcover，10 个键整体提交 ── -->
            <p class="st-card-hint">预览会随设置实时更新</p>
            <div class="field">
              <label>背景风格</label>
              <div class="toc-pos">
                <button type="button" class="cover-style-choice gradient" :aria-pressed="form.cover.bg_type === 'gradient'" :class="{ active: form.cover.bg_type === 'gradient' }" @click="form.cover.bg_type = 'gradient'">主题渐变</button>
                <button type="button" class="cover-style-choice dark" :aria-pressed="form.cover.bg_type === 'dark'" :class="{ active: form.cover.bg_type === 'dark' }" @click="form.cover.bg_type = 'dark'">深色</button>
                <button type="button" class="cover-style-choice image" :aria-pressed="form.cover.bg_type === 'image'" :class="{ active: form.cover.bg_type === 'image' }" @click="form.cover.bg_type = 'image'">图片</button>
              </div>
            </div>
            <div v-if="form.cover.bg_type === 'image'" class="field">
              <label>封面背景图</label>
              <!-- 封面保留原图比例；只提示清晰度，不因横图/竖图比例不同要求换图。 -->
              <ImageUpload
                class="cover-image-upload"
                v-model="form.cover.image" folder="hr/doc-cover" small
                :min-width="1200"
                recommend="宽度 ≥1200px，16:9 横图如 1600×900；其它比例也会完整显示"
                hint="JPG/PNG/WebP，上传自动压缩；建议宽度 ≥1200px，16:9 横图如 1600×900"
              />
              <small class="hint">原图完整显示、等比缩放，不裁切；支持其它比例，阅读页和预览保留相同画面。</small>
            </div>
            <div class="field-row">
              <div class="field">
                <label for="doc-setting-cover-label_en">英文标识</label>
                <input id="doc-setting-cover-label_en" v-model="form.cover.label_en" type="text" maxlength="40" placeholder="ADMIN POLICY">
              </div>
              <div class="field">
                <label for="doc-setting-cover-label_cn">中文标识</label>
                <input id="doc-setting-cover-label_cn" v-model="form.cover.label_cn" type="text" maxlength="40" placeholder="行政制度">
              </div>
            </div>
            <div class="field">
              <label>封面图标 <small class="hint">再点一次取消</small></label>
              <div class="cover-icons">
                <button
                  v-for="ic in COVER_ICONS" :key="ic" type="button"
                  class="cover-icon" :class="{ active: form.cover.icon === ic }"
                  @click="form.cover.icon = form.cover.icon === ic ? '' : ic"
                >{{ ic }}</button>
              </div>
            </div>
            <div class="field">
              <label for="doc-setting-cover-desc">封面说明</label>
              <textarea
                id="doc-setting-cover-desc" v-model="form.cover.desc" class="cover-desc" maxlength="300" rows="2"
                placeholder="一句话概括制度目的"
              ></textarea>
            </div>
            <div class="field">
              <label for="doc-setting-cover-date">执行日期</label>
              <input id="doc-setting-cover-date" v-model="form.cover.date" type="text" maxlength="60" placeholder="如 2026年10月1日起执行">
            </div>
            <div class="field">
              <label for="doc-setting-cover-scope">适用范围</label>
              <input id="doc-setting-cover-scope" v-model="form.cover.scope" type="text" maxlength="60" placeholder="如 适用全体员工">
            </div>
            <div class="field">
              <label for="doc-setting-cover-dept">发布部门</label>
              <input id="doc-setting-cover-dept" v-model="form.cover.dept" type="text" maxlength="60" placeholder="如 行政人事部">
            </div>
            </section>
            <!-- v1.19：签收卡文案配置（整张卡可改，每项留空 = 回线上默认模板）；仅在「需要签收」开启时出现。
                 lead/tip 的两个富文本编辑器挂在 settingsOpen 上：设置面板是 visibility+width 过渡展开的，
                 TipTap 在隐藏容器里挂载会不出预览——打开设置时再挂，关了就卸（值在 form.signoff 上，不丢）。 -->
            <section v-if="form.requires_signoff" class="st-card" aria-label="签收设置">
              <div class="st-card-title"><h3>签收设置</h3></div><p class="st-card-hint">留空使用默认签收文案</p><details class="st-details" :open="signoffDetailsOpen" @toggle="signoffDetailsOpen = $event.target.open"><summary>自定义签收卡与实时预览</summary><div class="st-details-body">
              <!-- v1.19：签收卡是页面级元素、正文里看不到 → 这里放同一张卡的实时预览
                   （HrDocSignoff 组件、preview 态：随下面的输入即时变，交互惰性不真签）。
                   改文案/勾选框/按钮，预览马上跟着变——编辑所见即发布所见。 -->
              <div class="sg-preview-wrap">
                <HrDocSignoff
                  :signoff="mergeSignoff(form.signoff)"
                  :title="form.title || '本文档'"
                  :version="form.draft_version || form.version || ''"
                  :chapters-text="previewChapters"
                  preview
                />
              </div>
              <div class="field-row">
                <div class="field">
                  <label for="doc-setting-signoff-icon">标题图标</label>
                  <input id="doc-setting-signoff-icon" v-model="form.signoff.icon" type="text" maxlength="8" placeholder="📝">
                </div>
                <div class="field">
                  <label for="doc-setting-signoff-title">卡标题</label>
                  <input id="doc-setting-signoff-title" v-model="form.signoff.title" type="text" maxlength="60" placeholder="员工签收">
                </div>
              </div>
              <div class="field">
                <label>引导语 / 承诺几条（富文本，留空 = 默认三条）</label>
                <RichTextEditor v-if="settingsOpen && signoffDetailsOpen" v-model="form.signoff.lead" :theme="form.theme" compact modern-toolbar-selects placeholder="留空自动生成：① 已完整阅读《标题》版本 全部 N 章 ② 理解约束力 ③ 存入人事档案" />
              </div>
              <div class="field">
                <label for="doc-setting-signoff-ack_label">勾选框文字</label>
                <input id="doc-setting-signoff-ack_label" v-model="form.signoff.ack_label" type="text" maxlength="200" placeholder="我确认已完整阅读并理解本文档全部内容">
              </div>
              <div class="field">
                <label for="doc-setting-signoff-button_text">签收按钮文字</label>
                <input id="doc-setting-signoff-button_text" v-model="form.signoff.button_text" type="text" maxlength="200" placeholder="✓ 我已阅读并同意（线上签收）">
              </div>
              <div class="field">
                <label>底部留痕小字（富文本，留空 = 默认）</label>
                <RichTextEditor v-if="settingsOpen && signoffDetailsOpen" v-model="form.signoff.tip" :theme="form.theme" compact modern-toolbar-selects placeholder="留空则用默认：⚖️ 线上签收用于人事档案存档…" />
              </div>
              <div class="field">
                <label for="doc-setting-signoff-signed_note">已签收状态的存档说明</label>
                <input id="doc-setting-signoff-signed_note" v-model="form.signoff.signed_note" type="text" maxlength="300" placeholder="签收记录已存档。本文档发布新版本时，您需要重新签收。">
              </div>
              </div></details>
            </section>
          </div>
          </div>
        </aside>
      </div>
    </template>

    <!-- 一键转块确认：转换只改本地编辑态，保存才落库；不保存离开即放弃 -->
    <ConfirmDialog
      v-model:open="convertOpen"
      title="转换为块模式？"
      message="现有正文会原样包进一个「文字段」块，之后可拆分并添加结构化块。转换后不能自动转回整篇模式；保存前离开页面可放弃本次转换。"
      confirm-text="转换"
      tone="warn"
      @confirm="convertToBlocks"
    />

    <!-- 草稿先保存，只有确认新版本后才发布；输入和取消均由弹框显式处理。 -->
    <HrDocVersionDialog
      v-model:open="versionDialogOpen"
      :current-version="publicationContext?.state.current_version || ''"
      :published-before="!!publicationContext?.state.published_before"
      :body-changed="!!publicationContext?.state.body_changed"
      :initial-version="versionInitial"
      :used-versions="usedPublishedVersions"
      :busy="submitting"
      :error="publicationError"
      @confirm="onVersionConfirm"
    />

    <!-- 草稿历史抽屉：列表/预览/删除都在组件内自理；恢复回调只负责填充编辑器 -->
    <HrDocDraftHistory
      v-if="!isNew"
      v-model:open="historyOpen"
      :document-id="doc?.id || 0"
      :dirty="dirty"
      :current="historyCurrent"
      @restore="applyDraftVersion"
    />

    <!-- v1.23 智能排版：粘贴 / 上传 / 直接用当前文档内容 → 预览 → 应用。只改内存，不落库 -->
    <HrLayoutDialog
      v-model:open="layoutOpen"
      :seed-text="layoutSeedText"
      :seed-block-count="mode === 'blocks' ? form.blocks.length : 0"
      @submitted="onLayoutJobSubmitted"
    />

    <!-- v1.23 新建文档先选开始方式：上传文档（自动 AI 排版）或 手动创建（空白编辑器） -->
    <HrNewDocDialog
      v-if="isNew"
      v-model:open="newStartOpen"
      @submitted="onNewJobSubmitted"
      @manual="newStartOpen = false"
      @cancel="router.push('/portal/hr-documents')"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick, provide } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { PageHeader, Button, Badge, DropdownMenu, DropdownMenuItem } from '../../../bridge/oa-ui'
import { ArrowLeft, Check, Save, Send, Settings2, Pencil, Columns2, Eye, Sparkles, History, Ellipsis, Smartphone, Blocks, Undo2 } from '@lucide/vue'
import RichTextEditor from '../../components/ui/RichTextEditor.vue'
import HrDocSignoff from '../../components/HrDocSignoff.vue'
import HrDocSideRail from '../../components/HrDocSideRail.vue'
import RteToolbar from '../../components/ui/RteToolbar.vue'
import { Select as SelectField } from '../../../bridge/oa-ui'
import DeptScopePicker from '../../components/ui/DeptScopePicker.vue'
import ConfirmDialog from '../../components/ui/ConfirmDialog.vue'
import HrBlockListEditor from '../../components/HrBlockListEditor.vue'
import HrDocDraftHistory from '../../components/HrDocDraftHistory.vue'
import HrBlocksRenderer from '../../components/HrBlocksRenderer.vue'
import HrDocRenderer from '../../components/HrDocRenderer.vue'
import HrDocCover from '../../components/HrDocCover.vue'
import HrDocOutlineNav from '../../components/HrDocOutlineNav.vue'
import HrDocVersionDialog from '../../components/hr/HrDocVersionDialog.vue'
import HrLayoutDialog from '../../components/ui/HrLayoutDialog.vue'
import HrLayoutJobsLauncher from '../../components/ui/HrLayoutJobsLauncher.vue'
import HrNewDocDialog from '../../components/ui/HrNewDocDialog.vue'
import ImageUpload from '../../components/news/ImageUpload.vue'
import { articleApi, hrDocApi } from '../../../bridge/api'
import { HR_THEMES, resolveTheme, themeCssVars } from '../../data/hr-themes'
import { COVER_ICONS, DEFAULT_COVER, normalizeCover } from '../../data/hr-cover'
import { DEFAULT_SIGNOFF, mergeSignoff } from '../../utils/hr-signoff'
import { publishVersionError, suggestNextVersion } from '../../utils/hr-version'
import { isHrDocumentListHistory } from '../../utils/hr-admin-navigation'
import { useToast } from '../../composables/useToast'
import { confirmDialog } from '../../composables/useConfirm'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const CATEGORY_LABEL = { handbook: '员工手册', incentive: '奖惩制度', hr: 'HR 制度', 'hr-sop': '行政人事 SOP', policy: '其他政策' }

// 二级分类：与「制度中心」(/hr) 的 6 个主题导航一一对应（v1.23，2026-10-27）。
// 顺序即下拉顺序；决定文档落在制度中心哪个 tab。只有「HR 制度」有二级。
const TOPIC_LABEL = {
  performance: '绩效考核',
  attendance: '考勤假期',
  salary: '薪资晋升',
  benefits: '员工福利',
  admin: '行政规范',
  growth: '成长发展',
}

const categoryOptions = Object.entries(CATEGORY_LABEL).map(([value, label]) => ({ value, label }))
const topicOptions = Object.entries(TOPIC_LABEL).map(([value, label]) => ({ value, label }))

const isNew = computed(() => route.path.endsWith('/new'))
const documentId = computed(() => Number(route.params.documentId || doc.value?.id || 0))
const saveBtnLabel = computed(() => {
  if (saving.value) return '保存中…'
  if (isNew.value) return '创建草稿'
  return '保存草稿'
})

const doc = ref(null)
const rteRef = ref(null)
const rteEditor = computed(() => rteRef.value?.editor || null)
// 编辑模式初始即 loading：让编辑器只在数据就绪后挂载一次。
// 否则会先空挂载一帧再卸载重挂，Teleport 出去的工具栏在这种快速挂/卸下会触发 Vue 卸载崩溃
const loading = ref(!route.path.endsWith('/new'))
const saving = ref(false)
const submitting = ref(false)
const testPushing = ref(false)
const settingsOpen = ref(false)
const settingsToggleRef = ref(null)
const docToolbarEl = ref(null)
let toolbarObserver
onMounted(() => {
  if (!docToolbarEl.value) return
  toolbarObserver = new ResizeObserver(() => {
    const toolbar = docToolbarEl.value
    toolbar?.closest('.doc-ed')?.style.setProperty('--portal-editor-toolbar-height', toolbar.getBoundingClientRect().height + 'px')
  })
  toolbarObserver.observe(docToolbarEl.value)
})
onUnmounted(() => toolbarObserver?.disconnect())
const signoffDetailsOpen = ref(false)
watch(settingsOpen, async (open) => {
  if (!open || !window.matchMedia('(max-width: 1100px)').matches) return
  await nextTick()
  const panel = document.getElementById('doc-settings-panel')
  if (!panel) return
  const toolbarHeight = docToolbarEl.value?.getBoundingClientRect().height || 0
  const shellHeight = parseFloat(getComputedStyle(panel).getPropertyValue('--portal-shell-sticky-top')) || 0
  window.scrollTo({ top: window.scrollY + panel.getBoundingClientRect().top - toolbarHeight - shellHeight - 12, behavior: 'auto' })
})
function closeSettings() {
  settingsOpen.value = false
  nextTick(() => (settingsToggleRef.value?.$el || settingsToggleRef.value)?.focus({ preventScroll: true }))
}

// ── 双模式 ──
// 'html' = 整篇富文本（老格式，现行为保留）；'blocks' = 块列表编辑器。
// 新建默认块模式；老文档在 loadDoc 按草稿/快照格式判定；转换单向 html → blocks。
const mode = ref(route.path.endsWith('/new') ? 'blocks' : 'html')
const convertOpen = ref(false)
// v1.23 新建文档先问「上传文档 / 手动创建」—— 上传是最常见路径，不该让人先面对空白编辑器
// 从任务中心跳过来（?job=N）时不要弹「新建方式」，直接进编辑器并应用结果
const newStartOpen = ref(isNew.value && !route.query.job)
watch(isNew, (v) => { if (v) newStartOpen.value = true })

// 新建时选了「上传文档」：只是提交了任务，关弹窗 + 打开任务中心看进度
// （v1.24 改异步：长文档要跑 2 分钟，不能卡在弹窗里）
function onNewJobSubmitted() {
  newStartOpen.value = false
  // 复用现有任务中心的打开逻辑（onJobSubmitted 是早期草稿里的名字，已不存在）
  onLayoutJobSubmitted()
}

// v1.24 智能排版弹窗（选来源 → 提交后台任务）
const layoutOpen = ref(false)

// 任务列表与进度轮询已挪到 HrLayoutJobsLauncher（挂在 AdminLayout，全局可见）。
// 这里只负责「把任务结果灌进编辑器」——通过 provide 给全局 launcher 用。
function onLayoutJobApply(blocks) {
  if (publicationLocked.value) { toast.show('请先完成保存或发布，再应用排版', 'err'); return }
  if (!Array.isArray(blocks) || !blocks.length) {
    toast.show('排版结果为空，无法应用', 'err')
    return
  }
  applyLayout(blocks)
  newStartOpen.value = false
  toast.show(`已应用 ${blocks.length} 个块；检查后再保存`, 'ok')
}
provide('hrLayoutApply', onLayoutJobApply)

// 全局任务中心在编辑器页点「应用」时派发事件（不依赖 provide 链，跨页也稳）
function onExternalLayoutApply(e) {
  const blocks = e.detail
  if (Array.isArray(blocks) && blocks.length) onLayoutJobApply(blocks)
}
onMounted(() => window.addEventListener('hr-layout-apply', onExternalLayoutApply))
onUnmounted(() => window.removeEventListener('hr-layout-apply', onExternalLayoutApply))

// 从任务中心「应用到编辑器」跳过来：读 ?job= 取结果并灌进正文
async function consumeJobFromQuery() {
  const jobId = Number(route.query.job || 0)
  if (!jobId) return
  try {
    const r = await hrDocApi.getLayoutJob(jobId)
    const blocks = r.data?.blocks || []
    if (blocks.length) {
      newStartOpen.value = false
      onLayoutJobApply(blocks)
    } else {
      toast.show('该任务没有可应用的内容', 'err')
    }
  } catch {
    toast.show('读取排版结果失败，请到「排版任务」里重试', 'err')
  } finally {
    // 清掉参数：否则刷新页面会重复应用一次
    router.replace({ query: {} })
  }
}

function onLayoutJobSubmitted() {
  toast.show('已提交，可在右上角「排版任务」查看进度', 'ok')
}

// 应用 AI 排版的块：切块模式 + 灌入编辑器。只改内存，不落库（用户点保存才写）。
// dirty 跟踪表单快照；版本号只在用户确认发布时改变。
// 应用 AI 排版前的快照。AI 重排不保证更好看（尤其对已有 49 块的老文档），
// 必须给一条退路：保存前可以一键还原，否则用户只能「不保存」把别的修改也一起丢掉。
const layoutUndo = ref(null)

function applyLayout(blocks) {
  if (!Array.isArray(blocks) || !blocks.length) return
  layoutUndo.value = {
    mode: mode.value,
    blocks: JSON.parse(JSON.stringify(form.blocks || [])),
    html: form.content_html || '',
  }
  mode.value = 'blocks'
  form.blocks = JSON.parse(JSON.stringify(blocks))
  form.content_html = ''
}

function undoLayoutApply() {
  const snap = layoutUndo.value
  if (!snap) return
  mode.value = snap.mode
  form.blocks = snap.blocks
  form.content_html = snap.html
  layoutUndo.value = null
  toast.show('已还原到应用 AI 排版前的版本', 'ok')
}

// 把 HTML 片段转纯文本（给「智能排版 · 当前文档」入口用）
// 用 DOMParser 而不是 div.innerHTML —— 项目 ESLint 禁 innerHTML 直接赋值（XSS 面）
function stripHtml(html) {
  if (!html) return ''
  const doc = new DOMParser().parseFromString(String(html), 'text/html')
  return (doc.body.textContent || '').replace(/\u00a0/g, ' ').trim()
}

// 编辑器当前内容的纯文本表示：智能排版打开时直接拿它排版，用户不用重新粘贴一遍
const layoutSeedText = computed(() => {
  if (mode.value === 'blocks') {
    const parts = []
    for (const b of form.blocks || []) {
      for (const f of ['num', 'title', 'text', 'desc', 'html', 'caption']) {
        if (b[f]) parts.push(stripHtml(b[f]))
      }
      for (const it of b.items || []) {
        for (const f of ['title', 'text', 'desc', 'label', 'value', 'tag']) {
          if (it[f]) parts.push(stripHtml(it[f]))
        }
        if (Array.isArray(it.lines)) parts.push(...it.lines)
      }
      for (const c of b.cards || []) {
        for (const f of ['title', 'desc', 'text']) {
          if (c[f]) parts.push(stripHtml(c[f]))
        }
      }
    }
    return parts.filter(Boolean).join('\n')
  }
  return stripHtml(form.content_html)
})
// 草稿历史抽屉（口径 §6）
const historyOpen = ref(false)
// 预校验错误：{ 块下标: 文案 }，传给 HrBlockListEditor 做卡片红框 + 就地红字
const blockErrors = ref({})

const form = reactive({
  title: '',
  category: 'handbook',
  // 二级分类（制度中心 6 主题）；存量文档后端可能返回 null → 加载时回落 admin
  topic: 'admin',
  requires_signoff: false,
  version: 'v1.0',
  draft_version: 'v1.0',
  content_html: '',
  blocks: [],
  dept_scope: [],
  // ── 文档级展示字段（v1.5）：对齐参考「制度编辑器」──
  intro: '',            // 引言（纯文本 ≤500 字）
  theme: '',            // '' 默认品牌紫 / 预设 key / #RRGGBB
  tags: [],             // 标签
  toc_enabled: true,    // 本文目录开关（默认开，与既有阅读页一致）
  toc_position: 'left', // 目录位置 left / right
  // 整屏封面配置（v1.6）：10 个键整体提交，后端整体替换
  cover: { ...DEFAULT_COVER, nav_tabs: [] },
  // v1.19：阅读页「下载 PDF」开关（默认关）+ 签收卡配置（每项可改，空值回默认）
  download_pdf_enabled: false,
  signoff: { ...DEFAULT_SIGNOFF },
})


// 展示字段的 payload 片段：与标题/签收同套路走 draft_*，approve 时才生效
function metaPayload() {
  return {
    intro: form.intro,
    theme: form.theme,
    tags: form.tags,
    toc_enabled: form.toc_enabled,
    toc_position: form.toc_position,
    cover: form.cover,
    // v1.19：下载 PDF 开关 + 签收卡配置（signoff 整体提交，后端整体替换）
    download_pdf_enabled: form.download_pdf_enabled,
    signoff: form.signoff,
  }
}

// 标签输入：回车加一个（去重、限 10 个）
const tagDraft = ref('')
// 签收卡实时预览用的「全部 N 章」：章数 = 正文里的 section 块个数（与阅读页 HrDocPage 数 h2 同源口径）
const previewSectionCount = computed(() => (form.blocks || []).filter(b => b && b.type === 'section').length)
const previewChapters = computed(() => (previewSectionCount.value > 0 ? `全部 ${previewSectionCount.value} 章内容` : '全部内容'))
function addTag() {
  const t = tagDraft.value.trim()
  tagDraft.value = ''
  if (!t) return
  if (form.tags.length >= 10) { toast.show('标签最多 10 个', 'err'); return }
  if (form.tags.includes(t)) return
  form.tags.push(t)
}

// 自由取色：form.theme 可能是预设 key（不是十六进制），不能直接绑 <input type="color">。
// 取值时回落成当前主题的实际主色，赋值时写入 #RRGGBB（后端只认预设 key 或严格 #RRGGBB）。
const customThemeColor = computed({
  get: () => (/^#[0-9a-fA-F]{6}$/.test(form.theme) ? form.theme : resolveTheme(form.theme).primary),
  set: (v) => { form.theme = v },
})

/** 预览里的「下载 PDF」= 走浏览器打印（对话框里可另存为 PDF）；打印时把标题换成「标题 版本 - 日期」当文件名。 */
function printDoc() {
  const oldTitle = document.title
  const stamp = new Date().toISOString().slice(0, 10)
  document.title = `${form.title || 'HR 文档'} ${form.draft_version || form.version || ''} - ${stamp}`.trim()
  window.print()
  setTimeout(() => { document.title = oldTitle }, 500)
}

// ── 视图模式（v1.6，对齐参考的 编辑 / 分栏 / 预览 三态）──
// 默认 'edit' = 与从前完全一致的布局；分栏/预览是**新增的可选态**，
// 打开时才隐藏左大纲与右侧设置面板，所以对既有编辑流程零影响。
const viewMode = ref('edit')
const splitPct = ref(52)          // 分栏时纸面占的百分比
const dragging = ref(false)

const previewThemeVars = computed(() => themeCssVars(form.theme))

// ── 分栏预览栏里的目录 ──
// 预览的定位是「预览所见 = 员工所见」，阅读页有目录栏，这里也得有。
const paneContentEl = ref(null)
const paneOutlineCount = ref(0)
const paneHasToc = computed(() => form.toc_enabled && paneOutlineCount.value > 0)
// v1.19f：侧栏工具列（下载 PDF / 签收状态）也要进两栏的侧栏列 → 有工具列时同样切两栏
const paneHasRail = computed(() => !!(form.download_pdf_enabled || form.requires_signoff))
const paneTwoCols = computed(() => paneHasToc.value || paneHasRail.value)
// 目录组件按 contentKey 变化重扫 DOM。直接喂实时内容会每次击键都重建
// IntersectionObserver（高亮会闪），所以**防抖 300ms** 后再换 key：
// 既保证边打字边更新目录，又不会每敲一个字重扫一次。
const paneOutlineKey = ref('')
let outlineKeyTimer = null
watch(
  () => JSON.stringify([form.title, form.intro, form.blocks, form.content_html, mode.value]),
  (v) => {
    clearTimeout(outlineKeyTimer)
    outlineKeyTimer = setTimeout(() => { paneOutlineKey.value = v }, 300)
  },
  { immediate: true },
)
// 分栏宽度用 CSS 变量传（不用行内 flex）：这样窄屏的媒体查询能直接改规则覆盖，
// 不需要 !important 去压行内样式
const paperStyle = computed(() =>
  viewMode.value === 'split' ? { '--split-pct': `${splitPct.value}%` } : {})

// ── 分栏：编辑区 ↔ 预览区滚动同步（v1.21）──
// 用户 2026-10-27：「预览拉到最底下了，编辑还得再拖一遍」。
// 做法：分栏时 .doc-paper 也变成独立滚动容器（与 .doc-preview-pane 同高，见样式），
// 两边按「块」双向同步（见 syncByBlocks）—— 滚任意一边，另一边对齐到同一块；
// 用 syncing 锁 + rAF 防止两边互相触发抖动。
const paperEl = ref(null)
const previewEl = ref(null)
// 记录「我们刚给某个容器写进去的 scrollTop」：它的 scroll 事件是回声，必须忽略，
// 否则两栏会互相回推、越滚越偏（实测：预览第 10 块 → 编辑区被推到第 12 块）。
const lastSet = new WeakMap()

function scrollRatio(el) {
  const max = el.scrollHeight - el.clientHeight
  return max > 0 ? el.scrollTop / max : 0
}

// 元素相对滚动容器「内容顶部」的偏移。
// 不能用 offsetTop：它的 offsetParent 不一定是滚动容器（中间隔着定位元素就会算错）。
function topIn(el, box) {
  return el.getBoundingClientRect().top - box.getBoundingClientRect().top + box.scrollTop
}

// 按「块」对齐，而不是按整页比例。
// 用户 2026-10-27 截图：同一段「周末/节假日出差计入调休」在编辑区底部、预览却在顶部。
// 原因是预览多了封面/目录、两边块高也不同，按 0~1 比例映射必然错位。
// 编辑区 .blk-card 与预览 .hb 都由 form.blocks 顺序渲染、一一对应，
// 所以找「源容器顶部所在的块」→ 目标滚到同一个块 + 块内同样的进度。
function computeTarget(src, dst) {
  const srcBlocks = [...src.querySelectorAll('.blk-card')]
  const dstBlocks = [...dst.querySelectorAll('.hb')]
  if (srcBlocks.length && srcBlocks.length === dstBlocks.length) {
    const srcTop = src.scrollTop
    // 源容器顶部落在哪个块里（最后一个 top <= srcTop 的块）
    let i = 0
    for (let k = 0; k < srcBlocks.length; k++) {
      if (topIn(srcBlocks[k], src) <= srcTop + 1) i = k
      else break
    }
    const sb = srcBlocks[i]
    const db = dstBlocks[i]
    const sbTop = topIn(sb, src)
    const dbTop = topIn(db, dst)
    const frac = Math.min(1, Math.max(0, (srcTop - sbTop) / (sb.offsetHeight || 1)))
    return dbTop + frac * (db.offsetHeight || 1)
  }
  // 兜底：html 模式没有 .blk-card / .hb 结构，按比例
  const max = dst.scrollHeight - dst.clientHeight
  return max > 0 ? scrollRatio(src) * max : null
}

function onPaneScroll(src, dst) {
  // 这次 scroll 是我们刚给 src 写 scrollTop 产生的回声 → 忽略，别回推
  const expect = lastSet.get(src)
  if (expect != null && Math.abs(src.scrollTop - expect) <= 1) {
    lastSet.delete(src)
    return
  }
  const target = computeTarget(src, dst)
  if (target == null) return
  const max = dst.scrollHeight - dst.clientHeight
  dst.scrollTop = Math.min(max, Math.max(0, target))
  // 记录「实际写入值」：target 可能被 clamp，回声要按实际值比对
  lastSet.set(dst, dst.scrollTop)
}
const onPaperScroll = () => { if (paperEl.value && previewEl.value) onPaneScroll(paperEl.value, previewEl.value) }
const onPreviewScroll = () => { if (paperEl.value && previewEl.value) onPaneScroll(previewEl.value, paperEl.value) }

function unbindSplitScroll() {
  paperEl.value?.removeEventListener('scroll', onPaperScroll)
  previewEl.value?.removeEventListener('scroll', onPreviewScroll)
}
async function bindSplitScroll() {
  unbindSplitScroll()
  if (viewMode.value !== 'split') return
  await nextTick()   // 预览栏是 v-if，切到分栏后才挂载；等 DOM 就绪再绑
  paperEl.value?.addEventListener('scroll', onPaperScroll, { passive: true })
  previewEl.value?.addEventListener('scroll', onPreviewScroll, { passive: true })
}
watch(viewMode, bindSplitScroll, { immediate: true })

// 拖拽分隔条调宽。监听挂在 window 上（鼠标滑出分隔条也要跟手），
// 松手即摘；组件卸载时兜底再摘一次，避免拖到一半切页留下野监听。
let dragMove = null
let dragUp = null
function startSplitDrag(e) {
  const main = document.querySelector('.doc-main')
  if (!main) return
  const rect = main.getBoundingClientRect()
  dragging.value = true
  dragMove = (ev) => {
    const pct = ((ev.clientX - rect.left) / rect.width) * 100
    splitPct.value = Math.min(72, Math.max(28, pct))
  }
  dragUp = () => {
    dragging.value = false
    document.body.style.userSelect = ''
    window.removeEventListener('mousemove', dragMove)
    window.removeEventListener('mouseup', dragUp)
    dragMove = null
    dragUp = null
  }
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', dragMove)
  window.addEventListener('mouseup', dragUp)
  e.preventDefault()
}

const deptScopeLabel = computed(() => {
  const n = Array.isArray(form.dept_scope) ? form.dept_scope.length : 0
  return n ? `限 ${n} 个部门` : '全员可见'
})

// 草稿历史抽屉「当前」标记的比对基准 = 编辑器实时内容三元组（标题 + 格式 + 正文）
const historyCurrent = computed(() => ({
  title: form.title,
  format: mode.value,
  html: form.content_html,
  blocks: form.blocks,
}))

// ── 大纲 / 字数 ──
// 整篇模式：从 v-model HTML 实时提取 h1-h4（现行为不变）。
// 块模式：section 块标题 + text 块内 h2-h4（bi=块下标、hi=块内第几个标题，跳转用）。
const outline = computed(() => {
  if (mode.value === 'blocks') {
    const out = []
    form.blocks.forEach((b, bi) => {
      if (b.type === 'section') {
        out.push({ level: 2, text: (b.title || '').trim(), bi, hi: -1 })
      } else if (b.type === 'numsec') {
        // v1.3：编号章节同样进大纲（对齐参考「自动从 H2/编号章节 提取目录」）
        // 编号已下线（2026-10-20）：大纲只取标题，与阅读页目录（.hb-numsec-title）一致
        out.push({ level: 2, text: (b.title || '').trim(), bi, hi: -1 })
      } else if (b.type === 'text' && b.text) {
        const doc_ = new DOMParser().parseFromString(b.text, 'text/html')
        Array.from(doc_.body.querySelectorAll('h2, h3, h4')).forEach((el, j) => {
          out.push({ level: Number(el.tagName[1]), text: (el.textContent || '').trim(), bi, hi: j })
        })
      }
    })
    return out
  }
  if (!form.content_html) return []
  const doc_ = new DOMParser().parseFromString(form.content_html, 'text/html')
  return Array.from(doc_.body.querySelectorAll('h1, h2, h3, h4')).map(el => ({
    level: Number(el.tagName[1]),
    text: (el.textContent || '').trim(),
  }))
})
const wordCount = computed(() => {
  const stripLen = (html) => {
    if (!html) return 0
    const doc_ = new DOMParser().parseFromString(html, 'text/html')
    return (doc_.body.textContent || '').replace(/\s/g, '').length
  }
  if (mode.value === 'blocks') {
    const plainLen = (s) => (typeof s === 'string' ? s.replace(/\s/g, '').length : 0)
    const listLen = (arr) => (Array.isArray(arr) ? arr.reduce((n, s) => n + plainLen(s), 0) : 0)
    let total = 0
    for (const b of form.blocks) {
      if (b.type === 'text') total += stripLen(b.text)
      else if (b.type === 'imageText') total += plainLen(b.title) + stripLen(b.text)
      else if (b.type === 'callout') total += stripLen(b.text)
      else if (b.type === 'rawhtml') total += stripLen(b.html)
      else if (b.type === 'section') total += plainLen(b.title)
      else if (b.type === 'image') total += plainLen(b.caption)
      else if (b.type === 'divider') total += plainLen(b.text)
      // ── v1.3 新增 9 种（metrics/status/accordion 走下面的 items 通用分支）──
      // v1.4：正文类字段（text / desc / note）改富文本 HTML，字数要剥标签后再算
      else if (b.type === 'quote') total += stripLen(b.text)
      else if (b.type === 'docref') total += plainLen(b.title)
      else if (b.type === 'alert') total += plainLen(b.title) + stripLen(b.text)
      else if (b.type === 'numsec') total += plainLen(b.title) + stripLen(b.text)
      else if (b.type === 'compare') {
        for (const s of [b.left, b.right]) {
          if (s) total += plainLen(s.title) + listLen(s.items)
        }
      } else if (b.type === 'tabcard') {
        for (const t of b.tabs || []) {
          total += plainLen(t.name) + plainLen(t.header) + plainLen(t.subHeader) + stripLen(t.note)
          for (const m of t.metrics || []) total += plainLen(m.value) + plainLen(m.label)
        }
      } else if (Array.isArray(b.items)) {
        for (const it of b.items) for (const v of Object.values(it)) total += Array.isArray(v) ? listLen(v) : plainLen(v)
      } else if (Array.isArray(b.cards)) {
        // bento：desc 是富文本，其余（icon/title/color）是短字段
        for (const c of b.cards) total += plainLen(c.title) + stripLen(c.desc)
      }
    }
    return total
  }
  return stripLen(form.content_html)
})

function jumpToHeading(i) {
  if (mode.value === 'blocks') {
    const h = outline.value[i]
    if (!h) return
    const card = document.querySelector(`.doc-blocks .blk-card[data-blk-index="${h.bi}"]`)
    if (!card) return
    if (h.hi >= 0) {
      // text 块内标题：折叠时 ProseMirror 不在 DOM，退回定位到块卡片
      const nodes = card.querySelectorAll('.ProseMirror h2, .ProseMirror h3, .ProseMirror h4')
      if (nodes[h.hi]) { nodes[h.hi].scrollIntoView({ behavior: 'smooth', block: 'center' }); return }
    }
    card.scrollIntoView({ behavior: 'smooth', block: 'center' })
    return
  }
  // 大纲第 i 个标题 = 编辑器 DOM 里第 i 个标题（DOMParser 与 ProseMirror 顺序一致）
  const nodes = document.querySelectorAll('.doc-rte .ProseMirror h1, .doc-rte .ProseMirror h2, .doc-rte .ProseMirror h3, .doc-rte .ProseMirror h4')
  nodes[i]?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

// ── 一键转块（单向）：整篇 HTML 无损包进单个 text 块 ──
// 只改本地编辑态：保存才把 blocks 落库（后端会同时清空 HTML 草稿）；
// 不保存离开 = 放弃转换，dirty 守卫会拦一道确认
async function convertToBlocks() {
  // 正文里还有未上传的本地图（blob:）：必须先传成正式 URL——
  // 转换会卸载整篇编辑器并回收 blob 地址，不先传这些图就永久坏了
  if (rteRef.value?.hasPendingImages()) {
    try {
      await rteRef.value.flushPendingImages()
    } catch (e) {
      convertOpen.value = false
      toast.show('本地图片上传失败：' + (e.message || '未知错误') + '，请重试后再转换', 'err')
      return
    }
  }
  form.blocks = [{ type: 'text', text: form.content_html || '' }]
  form.content_html = ''
  mode.value = 'blocks'
  convertOpen.value = false
  toast.show('已转换为块模式 — 保存后生效；不保存离开可放弃', 'ok')
}

// ── 载入块时补缺键（v1.12）──
// 后端 _BLOCK_FIELDS 是 fail-loud（缺键 422），但**老草稿/老快照**里的 numsec 块
// 没有后来的 `color` 键 → 直接载入再保存会被 422 拦下。这里在**载入边界**补默认值，
// 而不是在模板里兜（模板读 undefined 只是不渲染，根因还在数据里）。
// 颜色空串都表示「跟随文章主题色」；载入旧草稿时补齐，避免原样保存被契约拦截。
// 后加的块字段（老草稿里没有这些键）→ 载入时补空串，否则「打开旧文档 → 保存」会被后端 422。
// 空串的语义是「未自定义，渲染端回落默认」，不是「清空」（与 numsec.color 同口径）。
const BLOCK_KEY_DEFAULTS = {
  numsec: { color: '' },
  quote: { color: '', bg: '' },
  docref: { text: '', button_text: '', button_url: '' },
}
function normalizeBlocks(blocks) {
  if (!Array.isArray(blocks)) return []
  for (const b of blocks) {
    const def = b && BLOCK_KEY_DEFAULTS[b.type]
    if (!def) continue
    for (const [k, v] of Object.entries(def)) {
      if (!(k in b)) b[k] = v
    }
  }
  return blocks
}

// ── 恢复草稿历史（口径 §6）──
// 只填充编辑器：snapshot 对比会自动亮 dirty，用户点「保存草稿」才落库。
// HTML 快照恢复 = 编辑模式自然切回整篇（这就是块 → 整篇的回退路径，不做单独按钮）
function applyDraftVersion(v) {
  form.title = v.title || ''
  if (v.content_blocks != null || v.format === 'blocks') {
    mode.value = 'blocks'
    // 深拷贝：编辑时不污染抽屉里缓存的快照详情（不拷贝的话再点「预览」会看到改过的内容）
    form.blocks = normalizeBlocks(JSON.parse(JSON.stringify(v.content_blocks || [])))
    form.content_html = ''
  } else {
    mode.value = 'html'
    form.blocks = []
    form.content_html = v.content_html || ''
  }
  blockErrors.value = {}
  historyOpen.value = false
  toast.show('已载入该版本 — 点「保存草稿」后才会生效', 'ok')
}

// ── 未保存修改守卫 ──
const savedSnapshot = ref('')
function snapshot() {
  return JSON.stringify({ ...form, dept_scope: [...(form.dept_scope || [])] })
}
const dirty = computed(() => savedSnapshot.value !== '' && snapshot() !== savedSnapshot.value)

// ── 下架态（v1.20）──
// offline_at 与 status 正交：下架只关员工端可见性，status/version 都不动。
const isOffline = computed(() => !!doc.value?.offline_at)
// 顶栏徽标：按可见性显示（员工端看到的是上次发布快照）。
// 🔴 已发布过的文档一保存草稿 status 就变 'draft'，但员工仍能看 —— 不能显示成草稿。
const displayStatus = computed(() => {
  const d = doc.value
  if (!d) return ''
  if (d.offline_at) return 'offline'
  return d.published_at ? 'published' : d.status
})
// 只有「下架后没改过内容」才给一键重新上架（restore 不升版本）；
// 改过内容必须走发布，否则新内容不会生效、版本号也不会推进。
const canRestore = computed(() =>
  isOffline.value && doc.value?.status === 'published' && !dirty.value)

// 发布判定以刚保存的服务端草稿相对正式快照为准，不能用本次打开页面的基线。
const wasEverPublished = computed(() => !!(doc.value?.published_at || doc.value?.publication_state?.published_before))
const publishedVersion = computed(() => wasEverPublished.value ? String(doc.value?.publication_state?.current_version ?? doc.value?.version ?? '').trim() : '')
const versionDialogOpen = ref(false)
const versionInitial = ref('')
const usedPublishedVersions = ref([])
const publicationContext = ref(null)
const publicationError = ref('')
const publicationLocked = computed(() => saving.value || submitting.value || versionDialogOpen.value)
watch(versionDialogOpen, open => {
  if (!open) { publicationContext.value = null; publicationError.value = '' }
})

function requirePublicationState(data) {
  const state = data?.publication_state
  if (!state || typeof state.published_before !== 'boolean' || typeof state.body_changed !== 'boolean'
    || typeof state.current_version !== 'string' || !state.revision || typeof state.revision !== 'string'
    || (state.published_before && !state.current_version.trim())) {
    throw new Error('草稿已保存，但无法确认发布状态，请稍后重新点击发布。')
  }
  return { ...state, current_version: state.current_version.trim() }
}

function publicationFailure(error) {
  return error.response?.data?.detail || error.message || '请稍后重试'
}

async function onVersionConfirm(value) {
  if (submitting.value || !versionDialogOpen.value || !publicationContext.value) return
  const context = publicationContext.value
  const target = String(value || '').trim()
  const invalid = publishVersionError(target, {
    publishedBefore: context.state.published_before,
    currentVersion: context.state.current_version,
    usedVersions: usedPublishedVersions.value,
  })
  if (invalid) { publicationError.value = invalid; return }
  submitting.value = true
  publicationError.value = ''
  // 只有用户确认才把弹窗里的值写回表单；失败留在框里，方便纠正后重试。
  form.draft_version = target
  try {
    // 目标号与草稿修订指纹一起交服务端，在同一事务内校验并发布。
    await articleApi.adminPublish(context.id, { expected_revision: context.state.revision, version: target })
    savedSnapshot.value = snapshot()
    versionDialogOpen.value = false
    toast.show('已发布，员工端立即可见', 'ok')
    goBack()
  } catch (error) {
    publicationError.value = publicationFailure(error)
  } finally {
    submitting.value = false
  }
}

function onBeforeUnload(e) {
  if (dirty.value) { e.preventDefault(); e.returnValue = '' }
}
onBeforeRouteLeave(async () => {
  if (!dirty.value) return true
  // 路由守卫支持返回 Promise：等用户在本站弹框里做完选择再决定去留
  return await confirmDialog({
    title: '离开编辑器？',
    message: '有未保存的修改，离开后会丢失。',
    confirmText: '仍然离开',
    cancelText: '留在本页',
    tone: 'warn',
  })
})

async function loadDoc() {
  if (isNew.value) {
    savedSnapshot.value = snapshot()
    settingsOpen.value = true // 新建时先填版本与分类
    return
  }
  loading.value = true
  try {
    const r = await articleApi.adminGet(documentId.value)
    // api interceptor 已 unwrap envelope → r === {success, data, error}；doc 对象在 r.data
    const d = r.data
    doc.value = d
    form.category = d.category
    form.topic = d.topic || 'admin'
    form.version = d.version
    form.draft_version = d.draft_version != null ? d.draft_version : d.version
    // 元信息：draft_* 优先（admin 改过但尚未发布），否则用正式列
    form.title = d.draft_title != null ? d.draft_title : d.title
    form.requires_signoff = d.draft_requires_signoff != null ? d.draft_requires_signoff : d.requires_signoff
    // 展示字段（v1.5）：与 title/dept_scope 同一套三态 —— draft_* 非 NULL 优先，否则读正式列
    form.intro = d.draft_intro != null ? d.draft_intro : (d.intro || '')
    form.theme = d.draft_theme != null ? d.draft_theme : (d.theme || '')
    form.tags = JSON.parse(JSON.stringify(d.draft_tags != null ? d.draft_tags : (d.tags || [])))
    form.toc_enabled = d.draft_toc_enabled != null ? d.draft_toc_enabled : (d.toc_enabled !== false)
    form.toc_position = d.draft_toc_position != null ? d.draft_toc_position : (d.toc_position || 'left')
    // 封面：draft_cover 非 NULL 优先；normalizeCover 补全缺键（旧数据是 {}）
    const storedCover = d.draft_cover != null ? d.draft_cover : d.cover
    form.cover = normalizeCover(storedCover)
    // v1.19：下载 PDF 开关 + 签收卡配置（三态：draft_* 优先，mergeSignoff 补缺键、空→默认）
    form.download_pdf_enabled = d.draft_download_pdf_enabled != null
      ? d.draft_download_pdf_enabled : !!d.download_pdf_enabled
    form.signoff = mergeSignoff(d.draft_signoff != null ? d.draft_signoff : d.signoff)
    // v1.5 的 cover_image（单张封面图）已被 v1.6 的 cover.image 取代。
    // 只在「从未配过封面配置」时把它迁过来当图片背景，避免覆盖用户已选的渐变/深色。
    const legacyCover = d.draft_cover_image != null ? d.draft_cover_image : d.cover_image
    if (legacyCover && !(storedCover && Object.keys(storedCover).length)) {
      form.cover.image = legacyCover
      form.cover.bg_type = 'image'
    }
    // 双格式判定（与 HrDocPreviewView eff 口径一致）：块草稿 > HTML 草稿 > 块快照 > HTML 快照。
    // 后端保证两种草稿互斥；深拷贝块数组，编辑时不污染 doc.value 原始数据
    if (d.draft_blocks != null) {
      mode.value = 'blocks'
      form.blocks = normalizeBlocks(JSON.parse(JSON.stringify(d.draft_blocks)))
      form.content_html = ''
    } else if (d.draft_content != null) {
      mode.value = 'html'
      form.blocks = []
      form.content_html = d.draft_content
    } else if (d.content_blocks != null) {
      mode.value = 'blocks'
      form.blocks = normalizeBlocks(JSON.parse(JSON.stringify(d.content_blocks)))
      form.content_html = ''
    } else {
      mode.value = 'html'
      form.blocks = []
      form.content_html = d.content_html || ''
    }
    blockErrors.value = {}
    form.dept_scope = d.draft_dept_scope != null ? d.draft_dept_scope : (d.dept_scope || [])
    savedSnapshot.value = snapshot()
  } catch (e) {
    toast.show('加载失败：' + (e.response?.data?.detail || e.message), 'err')
    router.push('/portal/hr-documents')
  } finally {
    loading.value = false
  }
}

function parseDeptScope() {
  // DeptScopePicker v-model 直接给数组：空 = 「全员可见」；非空 = 限定部门列表
  // 后端用空数组 vs 非空数组区分这两种意图（不能返 null，会变"未变更"语义）
  return Array.isArray(form.dept_scope) ? form.dept_scope : []
}

function validate() {
  if (!form.title.trim()) { toast.show('请填写标题', 'err'); return false }
  if (mode.value === 'blocks' && !precheckBlocks()) return false
  return true
}

// ── 块预校验（与后端 domain.validate_blocks 同构：口径 §2 上限）──
// 后端 422 只带一条错误路径；这里提前把每块第一条错误标到卡片上，省来回试错。
// UI 已锁死 type/variant/columns 取值与键集，这里只查上限与 image URL
function precheckBlocks() {
  const errs = {}
  const htmlBytes = (s) => new Blob([s || '']).size  // 与后端按 UTF-8 字节算对齐
  const short = (v, name) => (v.length > 200 ? `${name}超长（上限 200 字，当前 ${v.length}）` : '')
  const plain = (v, name) => (v.length > 2000 ? `${name}超长（上限 2000 字，当前 ${v.length}）` : '')
  const icon = (v) => (v.length > 16 ? `图标超长（上限 16 字）` : '')
  const html = (v, name) => (htmlBytes(v) > 200 * 1024 ? `${name}超长（单块上限 200KB）` : '')
  const url = (v) => {
    if (!v) return '请上传图片'
    if (v.length > 500) return '图片地址超长（上限 500 字）'
    if (!/^(https:\/\/|\/)/.test(v)) return '图片地址必须以 https:// 或 / 开头'
    return ''
  }
  // 结构化块条目逐条查：返回第一条错误（带条目序号方便定位）
  // 容器可能是 items（多数块）或 cards（bento），故由调用方把数组传进来
  const listLen = (arr, name) => {
    if (!Array.isArray(arr)) return `${name}必须是数组`
    if (arr.length > 30) return `${name}最多 30 条（当前 ${arr.length}）`
    for (const s of arr) if (typeof s === 'string' && s.length > 2000) return `${name}有单条超长（上限 2000 字）`
    return ''
  }
  // 与后端 domain kind=color 同构：只认 #RRGGBB
  const color = (v, name) => (/^#[0-9a-fA-F]{6}$/.test(v || '') ? '' : `${name}必须是 #RRGGBB 形式的颜色`)
  const optionalColor = (v, name) => (!v || /^#[0-9a-fA-F]{6}$/.test(v) ? '' : `${name}必须留空或使用 #RRGGBB 形式`)
  const docref = (b) => {
    if (!(Number.isInteger(b.document_id) && b.document_id > 0) && !(typeof b.slug === 'string' && b.slug.trim())) return '请选择引用文档'
    if (typeof b.title !== 'string' || !b.title.trim()) return '引用文档标题不能为空'
    return short(b.title, '引用文档标题')
  }
  const kindMsg = (v, kind, name) => {
    if (kind === 'short') return short(v, name)
    if (kind === 'plain') return plain(v, name)
    if (kind === 'html') return html(v, name)
    if (kind === 'icon') return icon(v)
    if (kind === 'color') return color(v, name)
    if (kind === 'list') return listLen(v, name)
    return ''
  }
  const items = (arr, fields) => {
    if (arr.length > 30) return `条目最多 30 条（当前 ${arr.length}）`
    for (let j = 0; j < arr.length; j++) {
      const it = arr[j]
      for (const [key, kind] of Object.entries(fields)) {
        const msg = kindMsg(it[key], kind, `第 ${j + 1} 条 ${key}`)
        if (msg) return msg
      }
    }
    return ''
  }
  // compare 单侧（{title, color, items}）
  const side = (s, name) => {
    if (!s) return `${name}缺失`
    return short(s.title, `${name}标题`) || color(s.color, `${name}颜色`) || listLen(s.items, `${name}要点`)
  }
  // tabcard 页签数组
  const tabs = (blk) => {
    if (!Array.isArray(blk.tabs) || !blk.tabs.length) return '至少需要一个页签'
    if (blk.tabs.length > 8) return `页签最多 8 个（当前 ${blk.tabs.length}）`
    if (!Number.isInteger(blk.activeTab) || blk.activeTab < 0 || blk.activeTab >= blk.tabs.length) return '默认页签越界'
    for (let j = 0; j < blk.tabs.length; j++) {
      const t = blk.tabs[j]
      const p = `第 ${j + 1} 个页签`
      const msg = short(t.name, `${p}名称`) || short(t.header, `${p}大标题`) || short(t.subHeader, `${p}副标题`)
        || items(t.metrics || [], { value: 'short', label: 'short' }) || html(t.note, `${p}备注`)
      if (msg) return msg
    }
    return ''
  }

  if (form.blocks.length > 100) {
    toast.show(`最多 100 块（当前 ${form.blocks.length}），请精简或拆成多篇文档`, 'err')
    return false
  }
  form.blocks.forEach((b, i) => {
    let msg = ''
    if (b.type === 'text') msg = html(b.text, '正文')
    else if (b.type === 'section') msg = short(b.title, '标题')
    else if (b.type === 'image') msg = url(b.image) || short(b.caption, '图说')
    else if (b.type === 'imageText') msg = url(b.image) || short(b.title, '小标题') || html(b.text, '正文')
    else if (b.type === 'callout') msg = html(b.text, '内容')
    else if (b.type === 'keypoints') msg = items(b.items, { icon: 'icon', title: 'short', desc: 'plain' })
    else if (b.type === 'cards') msg = items(b.items, { icon: 'icon', title: 'short', text: 'plain' })
    else if (b.type === 'steps') msg = items(b.items, { title: 'short', desc: 'plain' })
    else if (b.type === 'divider') msg = plain(b.text, '短语')
    else if (b.type === 'rawhtml') msg = html(b.html, 'HTML')
    // ── v1.3 新增 9 种（对齐参考「制度编辑器」）；v1.4 正文类字段改富文本走 html ──
    else if (b.type === 'quote') msg = html(b.text, '引用') || optionalColor(b.color, '引用颜色') || optionalColor(b.bg, '引用背景色')
    else if (b.type === 'docref') msg = docref(b)
    else if (b.type === 'metrics') msg = items(b.items, { value: 'short', label: 'short' })
    else if (b.type === 'compare') msg = side(b.left, '左栏') || side(b.right, '右栏')
    else if (b.type === 'status') msg = items(b.items, { status: 'short', text: 'short', tag: 'short' })
    else if (b.type === 'alert') msg = short(b.title, '标题') || html(b.text, '正文')
    else if (b.type === 'bento') msg = items(b.cards, { icon: 'icon', title: 'short', desc: 'html', color: 'color' })
    // 编号已下线（2026-10-20）：界面没有编号输入框，不能再拿 num 报错（用户改不了）
    else if (b.type === 'numsec') msg = short(b.title, '标题') || html(b.text, '正文')
    else if (b.type === 'tabcard') msg = tabs(b)
    else if (b.type === 'accordion') msg = items(b.items, { icon: 'icon', title: 'short', lines: 'list', open: 'bool' })
    if (msg) errs[i] = msg
  })
  blockErrors.value = errs
  const bad = Object.keys(errs)
  if (!bad.length) return true
  const first = Number(bad[0])
  toast.show(`第 ${first + 1} 块（共 ${bad.length} 块有问题）：${errs[first]}`, 'err')
  // 滚到第一个出错的块卡片，红框 + 就地红字由子组件渲染
  document.querySelector(`.doc-blocks .blk-card[data-blk-index="${first}"]`)
    ?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  return false
}

// 保存载荷：JSON 往返剥掉 reactive 代理，等于按契约形状做一次浅净化
function blocksPayload() {
  return JSON.parse(JSON.stringify(form.blocks))
}

function draftPayload() {
  return {
    title: form.title,
    category: form.category,
    topic: form.topic,
    ...(mode.value === 'blocks' ? { blocks: blocksPayload() } : { content_html: form.content_html }),
    draft_version: form.draft_version,
    requires_signoff: form.requires_signoff,
    dept_scope: parseDeptScope(),
    ...metaPayload(),
  }
}
function rememberSavedDraft(data) {
  if (data) doc.value = { ...doc.value, ...data }
  savedSnapshot.value = snapshot()
}
async function saveDraft() {
  if (publicationLocked.value || !validate()) return
  await doSaveDraft()
}

async function doSaveDraft() {
  saving.value = true
  const dept_scope = parseDeptScope()
  try {
    // 先把正文里的本地图片传上去（blob: → 正式 URL），失败则中止保存。
    // 块模式没有整篇编辑器（rteRef 为 null，可选链直接跳过）：块内图片走
    // ImageUpload 即传即压，text 块 compact 工具栏没有图片按钮，不存在待传 blob
    await rteRef.value?.flushPendingImages()
    if (isNew.value) {
      const created = await articleApi.adminCreate({
        title: form.title,
        category: form.category,
        topic: form.topic,
        // 块模式：create 端点只收 content_html，先建空文档再补一发 blocks——
        // 否则空串会被当成 HTML 草稿，重新打开就掉回整篇模式
        content_html: mode.value === 'blocks' ? '' : form.content_html,
        version: form.draft_version,
        requires_signoff: form.requires_signoff,
        dept_scope,
        ...metaPayload(),
      })
      const newId = created.data.id
      let blocksSaveFailed = false
      if (mode.value === 'blocks') {
        try {
          await articleApi.adminUpdate(newId, { blocks: blocksPayload() })
        } catch (e2) {
          // 文档已创建成功，只有块内容没存上——照常进编辑页让用户重存，
          // 文档已经创建，进入 ID 编辑页让用户重试保存内容。
          blocksSaveFailed = true
          toast.show('文档已创建，但内容保存失败：' + (e2.response?.data?.detail || e2.message) + '，请点「保存草稿」重试', 'err')
        }
      }
      if (!blocksSaveFailed) toast.show('已创建草稿', 'ok')
      savedSnapshot.value = snapshot() // 先标记已保存，路由守卫不拦
      await router.replace(`/portal/hr-documents/id/${newId}/edit`)
      if (blocksSaveFailed) {
        // 服务端此刻只有空 HTML 草稿，直接 loadDoc 会把本地块内容冲掉——
        // 拉完元信息后把编辑区恢复成块模式，dirty 亮起提醒用户重存
        const keep = form.blocks
        await loadDoc()
        mode.value = 'blocks'
        form.blocks = keep
        form.content_html = ''
      } else {
        await loadDoc()
      }
    } else {
      const saved = await articleApi.adminUpdate(documentId.value, draftPayload())
      rememberSavedDraft(saved.data)
      toast.show('草稿已保存', 'ok')
    }
  } catch (e) {
    toast.show('保存失败：' + (e.response?.data?.detail || e.message), 'err')
  } finally {
    saving.value = false
  }
}

// ── 重新上架（v1.20）：只清下架标记，不升版本、不写新快照 ──
async function restoreNow() {
  const ok = await confirmDialog({
    title: '重新上架？',
    message: `版本仍为 ${doc.value?.version}，不产生新版本；上架后员工端立即可见。`,
    confirmText: '重新上架',
  })
  if (!ok) return
  submitting.value = true
  try {
    await articleApi.adminRestore(documentId.value)
    toast.show('已重新上架，员工端立即可见', 'ok')
    goBack()
  } catch (e) {
    toast.show('上架失败：' + (e.response?.data?.detail || e.message), 'err')
  } finally {
    submitting.value = false
  }
}

async function publishNow() {
  if (publicationLocked.value || !validate()) return
  submitting.value = true
  publicationError.value = ''
  try {
    await rteRef.value?.flushPendingImages()
    const id = documentId.value
    // 所有元信息与正文一起保存，避免分类/二级分类只改了界面却没随发布落库。
    const saved = await articleApi.adminUpdate(id, draftPayload())
    rememberSavedDraft(saved.data)
    const state = requirePublicationState(saved.data)
    const target = String(form.draft_version || '').trim()
    const newVersion = !state.published_before || state.body_changed || (!!target && target !== state.current_version)
    if (newVersion) {
      usedPublishedVersions.value = []
      if (state.published_before) {
        const history = await articleApi.adminVersions(id)
        if (!Array.isArray(history.data)) throw new Error('无法读取已发布版本记录，请稍后重新点击发布。')
        usedPublishedVersions.value = history.data.map(item => String(item.version || '').trim()).filter(Boolean)
      }
      versionInitial.value = !state.published_before
        ? (target || state.current_version || String(form.version || 'v1.0'))
        : (target && target !== state.current_version ? target : suggestNextVersion(state.current_version))
      publicationContext.value = { id, state }
      versionDialogOpen.value = true
      return
    }
    const ok = await confirmDialog({
      title: '发布文档设置？',
      message: `草稿已保存。本次只更新设置，版本保持 ${state.current_version}，已有签收记录保留。确认后立即对员工可见。`,
      confirmText: '确认发布设置',
    })
    if (!ok) return
    await articleApi.adminPublish(id, { expected_revision: state.revision, version: state.current_version })
    form.draft_version = state.current_version
    savedSnapshot.value = snapshot()
    toast.show('设置已发布，版本保持不变', 'ok')
    goBack()
  } catch (error) {
    toast.show('发布失败：' + publicationFailure(error), 'err')
  } finally {
    submitting.value = false
  }
}

function goBack() {
  // 从列表页进来时原路返回（保留列表 URL 上的搜索/筛选 query）；直达本页时兜底跳列表
  if (isHrDocumentListHistory(window.history.state?.back, window.location.origin)) {
    router.back()
  } else {
    router.push('/portal/hr-documents')
  }
}

async function testPushToMe() {
  if (!doc.value?.id) return
  // 用真实推送模板推送给当前 admin 一个人（不广播）；force 绕 cooldown
  // **不调 adminUpdate** — 避免把 pending 文档撤回 draft（reviewer 第十二轮 #1）。
  // 把当前表单值作 preview payload 传给端点，端点用 payload 临时覆盖 doc 字段
  // 生成推送内容，不写库。
  testPushing.value = true
  try {
    const r = await articleApi.adminTestPushToMe(doc.value.id, {
      title: form.title,
      version: form.draft_version,
      requires_signoff: form.requires_signoff,
    })
    const data = r.data || {}
    if (data.sent > 0) {
      // 消息里的链接指向员工端正式页：文档发布前点开是「暂未发布」占位页（草稿员工不可见，by design）
      toast.show(doc.value?.status === 'published'
        ? `✓ 已推送给你 — 请到企微查看（1-3 秒到达）`
        : `✓ 已推送给你 — 注意：文档尚未发布，消息里的链接会打开「暂未发布」页，发布后即正常`, 'ok')
    } else if (data.failed > 0) {
      // 后端在 push 结果里带回企微/代理层真实 errors（口径-企微消息推送走代理 §3）；
      // 空串（如网络超时 str(e) 为空）过滤掉，兜底提示查推送日志而不是瞎猜原因
      const reason = (data.errors || []).filter((s) => s && s.trim()).join('；')
      toast.show(reason
        ? `推送失败 (${data.failed})：${reason}`
        : `推送失败 (${data.failed})；企微未返回具体原因（多为网络超时/代理不可达），可查推送日志`, 'err')
    } else {
      toast.show('推送已发送但未确认到达', 'ok')
    }
  } catch (e) {
    toast.show('测试推送失败：' + (e.message || '未知错误'), 'err')
  } finally {
    testPushing.value = false
  }
}

onMounted(() => {
  loadDoc()
  consumeJobFromQuery()
  window.addEventListener('beforeunload', onBeforeUnload)
})
onUnmounted(() => {
  window.removeEventListener('beforeunload', onBeforeUnload)
  // 拖分栏拖到一半切页：兜底摘掉挂在 window 上的野监听
  if (dragMove) window.removeEventListener('mousemove', dragMove)
  if (dragUp) window.removeEventListener('mouseup', dragUp)
  document.body.style.userSelect = ''
  // 目录重扫的防抖定时器也要清，否则切页后还会写一次已卸载组件的 ref
  clearTimeout(outlineKeyTimer)
  // 分栏滚动同步的监听与 rAF 一并清理
  unbindSplitScroll()
})
</script>

<style scoped>
.doc-ed { position: relative; z-index: 1; padding: var(--nav-h) 0 80px; min-height: 100vh; }
/* 工具栏内的任务入口：跟随 tb-btn 的尺寸与间距，不抢眼但可点 */
/* 撤销按钮：琥珀色，和普通操作区分开，但不抢「发布」的视觉优先级 */
.tb-undo {
  color: #b45309;
  border-color: rgba(217, 119, 6, 0.32);
  background: rgba(217, 119, 6, 0.06);
}
.tb-undo:hover { border-color: rgba(217, 119, 6, 0.55); background: rgba(217, 119, 6, 0.1); }

.doc-jobs-inline :deep(.ljl-btn) {
  height: 30px;
  padding: 0 10px;
  font-size: 12.5px;
  border-radius: 8px;
}
.doc-jobs-inline :deep(.ljl-text) { display: inline; }
.loading-state { text-align: center; padding: 60px; color: var(--t4); }

/* ── 顶栏：返回 + 工具栏（靠左）+ 操作（靠右）── */
.doc-topbar {
  position: sticky; top: var(--nav-h); z-index: 30;
  display: flex; align-items: center; gap: 12px;
  min-height: 52px; padding: 6px 20px;
  background: rgba(255,255,255,0.97);
  border-bottom: 1px solid rgba(99,102,241,0.14);
}
.tb-back {
  width: 32px; height: 32px; border-radius: 8px; border: 1px solid rgba(99,102,241,0.2);
  background: #fff; color: var(--t3); cursor: pointer; font-size: 15px; flex-shrink: 0;
}
.tb-back:hover { color: var(--v); border-color: var(--v); }
/* 工具栏 Teleport 落点：去掉编辑器自带的底色/边框，融入顶栏 */
.tb-toolbar { flex: 1; min-width: 0; }
.tb-toolbar :deep(.rte-toolbar) { background: transparent; border-bottom: none; padding: 0; }
.tb-new { font-size: 12px; color: var(--t4); white-space: nowrap; }
/* 块模式顶栏标识：占住原工具栏位置，说明工具栏都在各文字块里 */
.tb-mode { font-size: 12px; font-weight: 700; color: var(--v); white-space: nowrap; }
.tb-dirty { font-size: 11px; color: #D97706; white-space: nowrap; }
.tb-push-pending { font-size: 11px; color: var(--t4); white-space: nowrap; }
.tb-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.tb-btn {
  padding: 7px 16px; border-radius: 9px; border: 1px solid rgba(99,102,241,0.2);
  background: #fff; color: var(--t2); font-size: 13px; font-weight: 600; font-family: inherit; cursor: pointer;
  transition: all .15s; white-space: nowrap;
}
.tb-btn:hover:not(:disabled) { border-color: var(--v); color: var(--v); }
.tb-btn.save { background: rgba(99,102,241,0.1); color: var(--v); border-color: transparent; }
.tb-btn.save:hover:not(:disabled) { background: rgba(99,102,241,0.18); }
.tb-btn.submit { background: var(--grad); color: #fff; border-color: transparent; }
.tb-btn.submit:hover:not(:disabled) { filter: brightness(1.08); }
.tb-btn.gear.on { background: rgba(99,102,241,0.1); color: var(--v); }
/* 草稿历史按钮开着抽屉时的激活态（与设置齿轮一致） */
.tb-btn.on { background: rgba(99,102,241,0.1); color: var(--v); }
.tb-btn:disabled { opacity: .5; cursor: not-allowed; }

/* ── 提示横幅 ── */
.doc-banner {
  max-width: 860px; margin: 16px auto 0; padding: 12px 16px;
  border-radius: 10px; font-size: 13px; line-height: 1.6;
}
.doc-banner.warn { background: rgba(245,158,11,0.12); border: 1px solid rgba(245,158,11,0.35); color: #92400e; }
.doc-banner.warn u { text-decoration-style: dotted; }
.doc-banner.reject { background: rgba(244,63,94,0.08); border: 1px solid rgba(244,63,94,0.2); color: #b91c1c; }
/* v1.20 已下架：灰调（不是错误，是「暂时关掉可见性」） */
.doc-banner.offline { background: rgba(140,146,171,0.12); border: 1px solid rgba(140,146,171,0.32); color: #4A5278; }

/* ── 主布局：大纲（贴左）+ 纸面（居中）+ 设置（推拉）── */
.doc-main {
  display: flex; gap: 24px; align-items: flex-start;
  padding: 20px 24px 0;
}
.doc-outline {
  position: sticky; top: calc(var(--nav-h) + 68px);
  width: 210px; flex-shrink: 0; max-height: calc(100vh - var(--nav-h) - 90px); overflow-y: auto;
  padding: 4px 0;
}
.ol-title { font-family: var(--fm); font-size: 11px; letter-spacing: .12em; text-transform: uppercase; color: var(--t4); margin-bottom: 8px; padding-left: 10px; }
.ol-empty { font-size: 12px; color: var(--t5); line-height: 1.6; padding: 0 10px; }
.ol-item {
  display: block; width: 100%; text-align: left; border: none; background: transparent;
  font-size: 12.5px; color: var(--t3); padding: 5px 10px; border-radius: 7px; cursor: pointer;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-family: inherit;
  border-left: 2px solid transparent; transition: all .12s;
}
.ol-item:hover { background: rgba(99,102,241,0.07); color: var(--v); }
.ol-item.lv-1 { font-weight: 600; color: var(--t2); }
.ol-item.lv-2 { font-weight: 600; color: var(--t2); }
.ol-item.lv-3 { padding-left: 24px; }
.ol-item.lv-4 { padding-left: 38px; font-size: 12px; }

/* ── 纸面 ── */
.doc-paper {
  flex: 1; min-width: 0; max-width: 920px; margin: 0 auto;
  background: rgba(255,255,255,0.96);
  border: 1px solid rgba(99,102,241,0.13);
  border-radius: 16px;
  padding: 36px 44px 48px;
  box-shadow: 0 8px 32px rgba(99,102,241,0.06);
}
.doc-title {
  width: 100%; border: none; background: transparent; outline: none;
  /* 🔴 必须用 --fn（中文字体在前），不能用 --fs。
     --fs = 'Outfit', 'Noto Sans SC', sans-serif —— Outfit 是**只有拉丁字形**的几何字体。
     标题是用户自己输入的内容，中英混排很常见：用 --fs 时「发过t」会变成
     「发过」走 Noto Sans SC、「t」走 Outfit —— **一行字里两套字体**，
     Outfit 的 t 又细又平（没有常规的底部弯钩），夹在汉字里就显得很诡异。
     --fn = 'Noto Sans SC', 'Outfit', sans-serif → 拉丁字母也由 Noto Sans SC 出，
     与汉字同一套字体，风格一致。 */
  font-family: var(--fn); font-size: 30px; font-weight: 600; color: var(--t);
  padding: 0 0 6px; box-sizing: border-box;
}
.doc-title::placeholder { color: var(--t5); font-weight: 400; }
.doc-subline {
  display: flex; align-items: center; gap: 7px; flex-wrap: wrap;
  border: none; background: transparent; padding: 0 0 14px; margin-bottom: 6px;
  font-size: 12px; color: var(--t4); cursor: pointer; font-family: inherit;
  border-bottom: 1px solid rgba(99,102,241,0.1); width: 100%;
}
.doc-subline:hover span { color: var(--v); }
.doc-subline .dot { color: var(--t5); }

/* ── 块列表编辑器：与整篇编辑区同一落位 ── */
.doc-blocks { margin-top: 14px; min-height: 55vh; }

/* ── 编辑器：无边框纸面化 + 员工端同款排版（工具栏已 Teleport 到顶栏）── */
.doc-rte { border: none !important; background: transparent !important; box-shadow: none !important; overflow: visible !important; }
.doc-rte :deep(.rte-content) {
  border: none; background: transparent; padding: 10px 2px 0;
  min-height: 55vh; max-height: none; overflow: visible;
  font-size: 15px; line-height: 1.75; color: var(--t);
}
.doc-rte :deep(.ProseMirror) { min-height: 55vh; }
/* 排版对齐 HrDocRenderer（员工端所见） */
.doc-rte :deep(.ProseMirror h1),
.doc-rte :deep(.ProseMirror h2),
.doc-rte :deep(.ProseMirror h3),
.doc-rte :deep(.ProseMirror h4) { color: var(--t); font-weight: 600; margin: 1.6em 0 0.6em; line-height: 1.3; }
.doc-rte :deep(.ProseMirror h1) { font-size: 28px; font-weight: 300; }
.doc-rte :deep(.ProseMirror h2) { font-size: 22px; }
.doc-rte :deep(.ProseMirror h3) { font-size: 17px; }
.doc-rte :deep(.ProseMirror h4) { font-size: 15px; }
.doc-rte :deep(.ProseMirror p) { margin: 0.6em 0; color: var(--t2); }
.doc-rte :deep(.ProseMirror ul),
.doc-rte :deep(.ProseMirror ol) { margin: 0.6em 0; padding-left: 1.4em; color: var(--t2); }
.doc-rte :deep(.ProseMirror li) { margin: 0.3em 0; }
.doc-rte :deep(.ProseMirror table) { border-collapse: collapse; width: 100%; margin: 1em 0; font-size: 14px; }
.doc-rte :deep(.ProseMirror th),
.doc-rte :deep(.ProseMirror td) { border: 1px solid var(--bd); padding: 8px 12px; text-align: left; vertical-align: top; }
.doc-rte :deep(.ProseMirror th) { background: rgba(99,102,241,0.05); font-weight: 600; }
.doc-rte :deep(.ProseMirror a) { color: var(--v); text-decoration: none; border-bottom: 1px solid rgba(99,102,241,0.25); }
.doc-rte :deep(.ProseMirror strong) { color: var(--t); font-weight: 600; }
/* 🔴 作者给加粗文字选过颜色时（Color 标记 = 外层 span[style*="color:"]），上面的显式 color
   会盖掉继承来的那个色，整篇编辑模式里会看到"选色没反应"。与 HrDocRenderer 同步处理。 */
.doc-rte :deep(.ProseMirror span[style*="color:"] :is(strong, b)) { color: inherit; }
.doc-rte :deep(.ProseMirror code) { font-family: var(--fm); background: rgba(99,102,241,0.08); padding: 2px 6px; border-radius: 4px; font-size: 13px; }
/* 块内引用：竖线色（border-left-color）/ 底色（background-color）可被工具栏「引用色 / 引用底色」
   选的 inline style 覆盖（见 BlockquoteWithColor）；这里给未选色时的默认观感。 */
.doc-rte :deep(.ProseMirror blockquote) { border-left: 4px solid var(--v); padding: .4em 0 .4em 16px; margin: 1em 0; border-radius: 0 8px 8px 0; background: rgba(99,102,241,0.05); color: var(--t2); }

/* ── 设置面板：推拉式（宽度过渡，展开时挤压正文而非浮层遮盖）── */
.doc-settings {
  position: sticky; top: calc(var(--nav-h) + 68px);
  flex-shrink: 0; width: 0; margin-left: -24px; /* 收起时抵消 .doc-main 的 gap */
  max-height: calc(100vh - var(--nav-h) - 90px);
  overflow: hidden; visibility: hidden;
  transition: width .25s ease, margin-left .25s ease, visibility .25s;
}
.doc-settings.open { width: 340px; margin-left: 0; visibility: visible; }
.st-inner {
  width: 340px; max-height: calc(100vh - var(--nav-h) - 90px);
  display: flex; flex-direction: column;
  background: #fff;
  border: 1px solid rgba(99,102,241,0.16); border-radius: 14px;
  box-shadow: 0 8px 32px rgba(99,102,241,0.08);
}
.st-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px; border-bottom: 1px solid rgba(99,102,241,0.12);
  font-size: 14px; font-weight: 700; color: var(--t);
}
.st-close {
  width: 28px; height: 28px; border-radius: 7px; border: 1px solid rgba(99,102,241,0.18);
  background: #fff; color: var(--t4); cursor: pointer; font-size: 12px;
}
.st-close:hover { color: #DC2626; border-color: rgba(220,38,38,0.3); }
.st-body { flex: 1; overflow-y: auto; overflow-x: hidden; padding: 16px 18px 28px; display: flex; flex-direction: column; gap: 14px; }
.field { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.field label { font-size: 13px; color: var(--t2); font-weight: 500; }
.field .req { color: #b91c1c; }
.field input[type="text"], .field select {
  width: 100%; box-sizing: border-box; min-width: 0;
  padding: 8px 12px; border: 1px solid rgba(99,102,241,0.22); border-radius: 8px;
  background: white; font-size: 14px; color: var(--t); font-family: inherit;
}
.field input:disabled { background: #f5f6fa; color: var(--t4); }
.field input:focus, .field select:focus { outline: 2px solid var(--v); outline-offset: -1px; }
.hint { font-size: 12px; color: var(--t4); }

.toggle { display: flex; align-items: center; gap: 8px; padding: 4px 0; cursor: pointer; font-size: 13.5px !important; font-weight: 500; color: var(--t2); }
.toggle input { width: 17px; height: 17px; cursor: pointer; flex-shrink: 0; }

@media (max-width: 1100px) {
  .doc-outline { display: none; }
}
/* ── v1.5 文档级展示字段（封面 / 主题 / 标签 / 目录）── */
/* 纸面上的引言：正文最上方一句话，编辑态给一条淡淡的底边提示它属于「正文之前」 */
.doc-intro {
  width: 100%;
  margin: 4px 0 14px;
  padding: 10px 14px;
  border: 1px dashed var(--bd);
  border-radius: 10px;
  background: transparent;
  font-family: inherit;
  font-size: 14.5px;
  line-height: 1.7;
  color: var(--t2);
}
.doc-intro:focus { outline: none; border-color: var(--v); border-style: solid; }

.st-sep {
  margin: 18px 0 10px;
  padding-top: 14px;
  border-top: 1px solid var(--bd);
  font-family: var(--fm);
  font-size: 10.5px;
  letter-spacing: 0.14em;
  color: var(--t4);
}
/* v1.19：设置里的签收卡实时预览 —— 收一下卡片自带的上边距，帖着「签收卡」分隔条 */
.sg-preview-wrap :deep(.hr-doc-signoff) { margin-top: 12px; }
/* v1.19i：正文列里的签收卡 —— 与正文同宽对齐（列已由 .pv-layout 内距收边），只留上下间距 */
.pv-main-col :deep(.hr-doc-signoff) { margin: 24px 0 8px; }
.st-mini-btn {
  display: block;
  margin-top: 8px;
  padding: 5px 10px;
  border: 1px solid var(--bd);
  border-radius: 7px;
  background: transparent;
  color: var(--t3);
  font-family: inherit;
  font-size: 11.5px;
  cursor: pointer;
}
.st-mini-btn:hover { border-color: var(--v); color: var(--v); }

.theme-row { display: flex; align-items: center; flex-wrap: wrap; gap: 7px; }
.theme-swatch {
  width: 26px;
  height: 26px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.12s, border-color 0.12s;
}
.theme-swatch:hover { transform: scale(1.08); }
.theme-swatch.active { border-color: var(--t); box-shadow: 0 0 0 2px #fff, 0 0 0 3px var(--bd); }
/* 默认主题是品牌紫渐变，用一枚小渐变块表示（不是纯色） */
.theme-swatch-default {
  width: auto;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #6366f1 0%, #a78bfa 100%);
  color: #fff;
  font-family: inherit;
  font-size: 11px;
  font-weight: 600;
}
.theme-custom {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 9px 3px 4px;
  border: 1px solid var(--bd);
  border-radius: 999px;
  cursor: pointer;
  font-size: 11.5px;
  color: var(--t3);
}
.theme-custom input[type='color'] {
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
}

.tags-input {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  padding: 6px 8px;
  border: 1px solid var(--bd);
  border-radius: 9px;
}
.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 6px 3px 9px;
  border-radius: 999px;
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.1);
  color: var(--v);
  font-size: 12px;
  font-weight: 600;
}
.tag-chip .tc-x {
  padding: 0 3px;
  border: 0;
  background: transparent;
  color: inherit;
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  opacity: 0.65;
}
.tag-chip .tc-x:hover { opacity: 1; }
.tag-input {
  flex: 1;
  min-width: 90px;
  border: 0;
  outline: none;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  color: var(--t);
}

.cover-icons { display: flex; flex-wrap: wrap; gap: 4px; }
.cover-image-upload :deep(.preview) { background-size: contain; }
.cover-icon {
  width: 28px; height: 28px; padding: 0;
  border: 1px solid var(--bd); border-radius: 8px;
  background: transparent; font-size: 15px; line-height: 1; cursor: pointer;
}
.cover-icon:hover { border-color: var(--v); }
.cover-icon.active { border-color: var(--v); background: rgba(99, 102, 241, 0.1); }
.cover-desc {
  width: 100%; padding: 7px 10px;
  border: 1px solid var(--bd); border-radius: 8px;
  background: transparent; font-family: inherit; font-size: 13px;
  color: var(--t); line-height: 1.6; resize: vertical;
}
.cover-desc:focus { outline: none; border-color: var(--v); }

.toc-pos { display: inline-flex; gap: 6px; }
.toc-pos button {
  padding: 5px 14px;
  border: 1px solid var(--bd);
  border-radius: 8px;
  background: transparent;
  color: var(--t3);
  font-family: inherit;
  font-size: 12.5px;
  cursor: pointer;
}
.toc-pos button.active { border-color: var(--v); background: rgba(99, 102, 241, 0.08); color: var(--v); font-weight: 600; }

/* ── 视图模式切换（v1.6，对齐参考的 编辑/分栏/预览）── */
.vm-toggle { display: inline-flex; border: 1px solid var(--bd); border-radius: 9px; overflow: hidden; }
.vm-toggle button {
  padding: 6px 11px; border: 0; background: transparent; color: var(--t3);
  font-family: inherit; font-size: 12.5px; cursor: pointer; white-space: nowrap;
}
.vm-toggle button + button { border-left: 1px solid var(--bd); }
.vm-toggle button.active { background: rgba(99, 102, 241, 0.1); color: var(--v); font-weight: 600; }

/* ── 分栏 / 预览 ── */
.doc-main.vm-split,
.doc-main.vm-preview { gap: 0; }
.doc-main.vm-split .doc-paper { flex: 0 0 var(--split-pct, 52%); max-width: none; margin: 0; }
/* v1.21：分栏时编辑区也变成独立滚动容器（与预览栏同高），两边滚动同步。
   不加这层时 .doc-paper 跟着整页滚、预览却是内部滚 → 滚完预览还得再拖编辑一遍。
   sticky 与 .doc-preview-pane 对齐，保证两栏顶端始终平齐。 */
.doc-main.vm-split .doc-paper {
  position: sticky;
  top: calc(var(--nav-h) + 68px);
  max-height: calc(100vh - var(--nav-h) - 90px);
  overflow-y: auto;
  /* overflow-y:auto 会把另一轴也算成 auto → 窄栏里工具栏可横向滚动，左侧被切掉一截。
     锁死横向：只保留纵向滚动（宽表格有 .tableWrapper 自己滚，不受影响）。 */
  overflow-x: hidden;
}
.doc-main.vm-split.dragging { cursor: col-resize; }
.pv-resizer {
  flex: 0 0 10px; align-self: stretch; cursor: col-resize;
  border-radius: 5px; background: transparent; transition: background 0.12s;
}
.pv-resizer:hover,
.doc-main.dragging .pv-resizer { background: rgba(99, 102, 241, 0.22); }
.doc-preview-pane {
  flex: 1 1 auto; min-width: 0;
  position: sticky; top: calc(var(--nav-h) + 68px);
  max-height: calc(100vh - var(--nav-h) - 90px); overflow-y: auto;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid var(--bd); border-radius: 14px;
}
.doc-main.vm-preview .doc-preview-pane { margin: 0 auto; max-width: 920px; }
.pv-head {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-bottom: 1px solid var(--bd);
  font-size: 12px; color: var(--t3);
}
.pv-dot { width: 7px; height: 7px; border-radius: 50%; background: #22c55e; }
.pv-hint { margin-left: auto; color: var(--t5); font-size: 11.5px; }
.pv-body { padding: 0 0 18px; }
/* 目录 + 正文两栏：与阅读页同款（左/右靠 grid-template-columns + order 换列）。
   预览栏是窄栏，所以目录列收到 150px；窄于 560px 时用容器查询退回单栏，
   免得预览被挤成一条。容器查询判断的是「预览栏自己有多宽」，视口媒体查询做不到。 */
.pv-body { container-type: inline-size; }
.pv-layout { padding: 0 18px 18px; }
/* 侧栏列：目录 + 下载 PDF + 签收状态 竖排（与阅读页 aside.hr-doc-side 同款） */
.pv-side { display: flex; flex-direction: column; gap: 16px; min-width: 0; }
/* 🔴 分栏 / 预览态：预览面板是**自己滚动的容器**（.doc-preview-pane 有 overflow-y:auto），
   阅读页那套「页面滚动 + 侧栏 sticky」在这里不成立 —— .pv-side 是 static 时，
   面板一滚目录就跟着走（实测滚 500px 后 top=-25、滚 1600px 后 -1125，等于没有目录）。
   改成让整列相对面板粘住：top 留 12px 不贴面板顶边（面板 14px 圆角，贴边会压角）。
   ⚠️ 这里**不要**给整列设 max-height + overflow —— 矮窗口下整列会变成滚动容器，
   一滚就把目录顶部推出去（用户 2026-10-20 截图：「第一章 总则」被裁掉）。
   限高与滚动交给目录自己（见下面 .hr-outline 那条）。 */
.pv-layout.has-toc > .pv-side {
  position: sticky;
  top: 12px;
  align-self: start;
}
/* 正文列（正文 + 签收卡 + 标签），与员工端 .hr-doc-body 同构 */
.pv-main-col { min-width: 0; }
/* 🔴 目录组件自带 position:sticky → 在本侧栏列里往下滚时目录吸顶、下方 PDF 按钮撞上来重叠。
   与阅读页同款处理：把目录压成静态（见 HrDocPage 的 .hr-doc-side .hr-outline）。
   ⚠️ 但**限高 + 自滚保留在目录自己身上**（不是整列）：矮窗口/高缩放下目录装不下时，
   滚的是列表本身、顶部条目不会被推出可视区，滚动条也明确告诉用户「这里可以滚」。 */
.pv-side :deep(.hr-outline) {
  position: static;
  max-height: calc(100vh - var(--nav-h) - 90px - 24px);
  overflow-y: auto;
  overscroll-behavior: contain;
}
/* 目录列只有 150px，按阅读页那套 30px 行高太占竖向空间：11 条就是 371px，
   而 500px 高的窗口里面板可视只有 ~354px → 目录被迫变成滚动区，滚轮经过侧栏时
   被它截留、顶部条目被推走（用户 2026-10-20 截图：「第一章 总则」消失）。
   压紧到 ~24px 一行（圆点同步上移保持居中）后，常见文档 10~15 条一屏放得下，
   滚动只发生在正文。 */
.pv-side :deep(.ol-item) { padding: 0 0 0 26px; }
.pv-side :deep(.ol-item.lv-2) { padding-left: 42px; }
.pv-side :deep(.ol-item.lv-3) { padding-left: 58px; }
.pv-side :deep(.ol-link) { padding: 2px 0; }
.pv-side :deep(.ol-item::before) { top: 6px; }
.pv-side :deep(.ol-item.lv-2::before),
.pv-side :deep(.ol-item.lv-3::before) { top: 8px; }
.pv-layout.has-toc {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
.pv-layout.has-toc.toc-right { grid-template-columns: minmax(0, 1fr) 150px; }
.pv-layout.has-toc.toc-right > :first-child { order: 2; }
@container (max-width: 559px) {
  .pv-layout.has-toc,
  .pv-layout.has-toc.toc-right { display: block; }
  .pv-layout.has-toc > :first-child { display: none; }
}
.pv-content { padding: 0; }
.pv-intro {
  margin: 14px 0 0; padding-bottom: 12px; border-bottom: 1px solid var(--bd);
  font-size: 13.5px; line-height: 1.75; color: var(--t3);
}
.pv-tags {
  margin-top: 22px; padding-top: 14px; border-top: 1px solid var(--bd);
  display: flex; align-items: center; flex-wrap: wrap; gap: 7px;
}
.pv-tags-label { font-size: 11.5px; color: var(--t4); }
.pv-tag {
  font-size: 11.5px; font-weight: 600; padding: 2px 9px; border-radius: 999px;
  background: rgba(var(--hr-primary-rgb, 99, 102, 241), 0.1);
  color: var(--hr-primary, var(--v));
}

@media (max-width: 900px) {
  /* 窄屏放不下并排：分栏退化成上下堆叠，分隔条没意义就藏掉 */
  .doc-main.vm-split { flex-direction: column; gap: 16px; }
  .doc-main.vm-split .doc-paper { flex: 1 1 auto; width: 100%; }
  .pv-resizer { display: none; }
}

@media (max-width: 768px) {
  .doc-paper { padding: 24px 18px 36px; }
  .doc-title { font-size: 24px; }
}


/* 文档设置：分组白卡、明确的控件状态与窄屏上下布局。 */
.doc-settings.open { width: 376px; scroll-margin-top: calc(var(--nav-h) + 88px); }
.st-inner { width: 376px; box-sizing: border-box; background: #F0F2F8; border-radius: 18px; }
.st-head { padding: 20px 20px 18px; align-items: flex-start; background: linear-gradient(135deg,#F5F3FF,#F0F2F8); border-radius: 18px 18px 0 0; }
.st-eyebrow { display: block; font-family: var(--fm); font-size: 10px; letter-spacing: .13em; color: var(--v); margin-bottom: 5px; }
.st-head h2 { margin: 0; color: var(--t); font-size: 19px; line-height: 1.4; }
.st-head p { margin: 5px 0 0; color: var(--t3); font-size: 12px; font-weight: 400; }
.st-close { flex: none; width: 30px; height: 30px; border-radius: 9px; }
.st-body { padding: 14px; gap: 12px; min-height: 0; overscroll-behavior: contain; scrollbar-width: thin; }
.st-card { min-width: 0; padding: 17px; background: rgba(255,255,255,.92); border: 1px solid var(--bd, #E2E3F0); border-radius: 13px; display: flex; flex-direction: column; gap: 15px; }
.st-card-title { display: flex; align-items: center; gap: 9px; margin-bottom: 1px; }
.st-card-title > span { width: 25px; height: 25px; display: inline-flex; align-items: center; justify-content: center; border-radius: 8px; background: #EEF0FF; color: var(--v); font-family: var(--fm); font-size: 11px; font-weight: 600; }
.st-card-title h3 { font-size: 14px; color: var(--t); margin: 0; font-weight: 650; }
.st-card-hint { margin: -8px 0 0; font-size: 12px; color: var(--t3); line-height: 1.6; }
.st-card .field { gap: 7px; }
.st-card .field-row { gap: 10px; grid-template-columns: minmax(0,1fr) minmax(0,1fr); }
.st-card .field > label { font-size: 12px; font-weight: 600; color: var(--t2); }
.st-card .field input[type='text'], .st-card .field textarea { min-height: 38px; border-radius: 9px; border-color: var(--bd); background: #FCFCFF; font-size: 13px; padding: 9px 11px; }
.st-card .field input[type='text']:focus, .st-card .field textarea:focus { outline: none; border-color: var(--v); box-shadow: 0 0 0 3px rgba(99,102,241,.10); background: #fff; }
.st-card .field input:disabled { background: #F1F3F8; color: var(--t3); }
.st-card .hint { display: block; font-size: 11.5px; line-height: 1.6; color: var(--t3); }
.st-card :deep(.sf-trigger) { height: 40px; border-radius: 9px; background: #FCFCFF; font-size: 13px; }
.st-card :deep(.sf-panel), .st-card :deep(.dsp-panel) { position: relative; top: auto; margin-top: 6px; box-shadow: none; max-height: 250px; }
.st-card :deep(.sf-option) { min-height: 35px; font-size: 12.5px; }
.st-card :deep(.dsp) { border-radius: 9px; }
.st-card :deep(.dsp-empty) { font-size: 12px; line-height: 1.6; }
.st-card :deep(.dsp-panel) { margin: 6px; border-color: var(--bd); }
.st-card :deep(.dsp-search) { width: 0; min-width: 0; }
.st-card :deep(.dsp-panel-hd) { padding: 8px; }
.st-card :deep(.dsp-panel-ft) { line-height: 1.6; }
.st-card label.toggle { position: relative; padding: 0; min-height: 34px; display: flex; align-items: center; gap: 10px; font-weight: 500; }
.st-card .toggle > span { flex: 1; min-width: 0; font-size: 12px; line-height: 1.65; }
.st-card .toggle input { position: absolute; opacity: 0; width: 36px; height: 22px; margin: 0; left: 0; }
.st-switch-track { display: inline-block; position: relative; flex: 0 0 36px; height: 22px; border-radius: 20px; background: #CBD0E1; transition: background .16s; pointer-events: none; }
.st-switch-track::after { content: ''; position: absolute; left: 3px; top: 3px; width: 16px; height: 16px; border-radius: 50%; background: white; box-shadow: 0 1px 3px rgba(20,25,60,.18); transition: transform .16s; }
.toggle input:checked + .st-switch-track { background: var(--v); }
.toggle input:checked + .st-switch-track::after { transform: translateX(14px); }
.toggle input:focus-visible + .st-switch-track { outline: 3px solid rgba(99,102,241,.28); outline-offset: 3px; }
.st-card .toc-pos { display: flex; padding: 3px; gap: 4px; border-radius: 10px; background: #F0F2F8; }
.st-card .toc-pos button { flex: 1; min-width: 0; padding: 7px 5px; border: 1px solid transparent; border-radius: 7px; background: transparent; color: var(--t3); font-size: 12px; }
.st-card .toc-pos button.active { color: var(--v); background: #fff; border-color: rgba(99,102,241,.20); box-shadow: 0 1px 4px rgba(20,25,60,.06); font-weight: 600; }
.st-card .toc-pos .cover-style-choice { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 7px 3px; }
.cover-style-choice::before { content: ''; display: block; width: 100%; height: 35px; border: 1px solid rgba(99,102,241,.12); border-radius: 5px; box-sizing: border-box; }
.cover-style-choice.gradient::before { background: linear-gradient(125deg,#6366F1,#C4B5FD); }
.cover-style-choice.dark::before { background: linear-gradient(125deg,#1E293B,#475569); }
.cover-style-choice.image::before { background: linear-gradient(145deg,transparent 45%,#BDD4D7 46% 62%,transparent 63%),linear-gradient(35deg,#C4C4ED 45%,#EEF1FB 46%); }
.st-card button:focus-visible, .st-details summary:focus-visible { outline: 2px solid var(--v); outline-offset: 3px; }
.st-card .theme-row { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 7px; }
.st-card .theme-swatch { width: auto; height: 58px; box-sizing: border-box; border-radius: 9px; padding: 7px 4px; display: flex; align-items: center; justify-content: center; gap: 4px; flex-wrap: wrap; background: #fff; color: var(--t2); border: 1px solid var(--bd); font-size: 11px; font-family: inherit; position: relative; }
.st-card .theme-swatch::before { content: ''; display: block; width: 100%; height: 15px; border-radius: 4px; background: var(--swatch,linear-gradient(135deg,#6366F1,#A78BFA)); }
.st-card .theme-swatch.active { border-color: var(--v); background: #F5F3FF; box-shadow: 0 0 0 1px var(--v); }
.theme-check { color: var(--v); font-weight: 700; }
.st-card .theme-custom { grid-column: span 2; height: 58px; box-sizing: border-box; justify-content: center; padding: 8px; border-radius: 9px; background: #FCFCFF; font-size: 12px; }
.st-card .theme-custom input[type='color'] { width: 25px; height: 25px; }
.st-card .tags-input { border-radius: 9px; min-height: 42px; background: #FCFCFF; }
.st-card .tag-input { min-width: 80px; width: 80px; font-size: 12px; }
.st-card .cover-icons { gap: 6px; }
.st-card .cover-icon { width: 31px; height: 31px; border-radius: 8px; }
.st-details { border-top: 1px solid var(--bd); padding-top: 12px; min-width: 0; }
.st-details summary { cursor: pointer; font-size: 12px; color: var(--v); font-weight: 600; line-height: 1.7; }
.st-details-body { display: flex; flex-direction: column; gap: 15px; padding-top: 15px; }
.st-details .sg-preview-wrap { max-width: 100%; overflow-wrap: anywhere; }
@media (max-width: 1250px) { .doc-main:has(.doc-settings.open) .doc-outline { display: none; } }
@media (max-width: 1100px) {
  .doc-main:has(.doc-settings.open) { flex-wrap: wrap; }
  .doc-main .doc-settings.open { width: 100%; flex: 0 0 100%; position: static; max-height: none; }
  .doc-settings.open .st-inner { width: 100%; max-height: none; }
  .doc-settings.open .st-body { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); align-items: start; max-height: none; overflow: visible; }
}
@media (max-width: 900px) {
  .doc-topbar { gap: 8px; padding-left: 12px; padding-right: 12px; }
  .tb-right { min-width: 0; flex: 1; max-width: 100%; overflow-x: auto; padding: 4px 2px; scrollbar-width: thin; }
  .tb-right > * { flex-shrink: 0; }
  .doc-main, .doc-main.vm-split { flex-direction: column; gap: 16px; }
  .doc-main .doc-paper, .doc-main .doc-preview-pane { flex: none; width: 100%; box-sizing: border-box; position: static; max-height: none; }
  .doc-main .doc-settings { display: none; margin-left: 0; }
  .doc-main .doc-settings.open { display: block; width: 100%; }
  .doc-settings.open .st-body { display: flex; }
  .doc-main .doc-settings.open .st-inner { width: 100%; }
  .doc-main { padding-left: 12px; padding-right: 12px; }
}
@media (prefers-reduced-motion: reduce) { .st-switch-track, .st-switch-track::after, .doc-settings { transition: none; } }

/* ── 打印 / 导出 PDF（预览态「下载 PDF」）：保留封面 + 目录 + 正文的样式，只去掉编辑器外壳与「签收卡 / 侧栏工具列 / 标签」── */
@media print {
  @page { size: A4; margin: 14mm; }
  /* 🔴 关键：让浏览器**保留背景色/渐变**（默认打印会丢掉背景 → 封面渐变、卡片底色、引用底色全没 = 「样式全丢了」） */
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

  /* 编辑器外壳全隐：顶栏 / 设置抽屉 / 大纲 / 纸面 / 拖拽条 / 预览栏标题 */
  .doc-topbar,
  .doc-settings,
  .doc-outline,
  .doc-paper,
  .pv-resizer,
  .pv-head { display: none !important; }
  /* 只去「签收卡 / 侧栏工具列 / 标签」；封面 + 目录 + 正文样式保留 */
  .hr-side-rail,
  .hr-doc-signoff,
  .pv-tags { display: none !important; }

  .doc-ed { background: #fff !important; min-height: 0 !important; padding: 0 !important; }
  .doc-main { display: block !important; gap: 0 !important; padding: 0 !important; }
  /* 预览栏去掉滚动/投影，让全部内容进入打印；卡片样式保留 */
  .doc-preview-pane {
    position: static !important;
    max-width: 100% !important;
    max-height: none !important;
    overflow: visible !important;
    box-shadow: none !important;
  }
  .pv-side { position: static !important; max-height: none !important; overflow: visible !important; }
  /* 🔴 打印时**不能用 grid/flex 排版目录 + 正文**：这两者在分页上下文里是不可分片的
     （整行被当成一个整体），内容稍微超出第 1 页剩余空间 → 整块被推到下一页，
     第 1 页只剩封面 + 一大段空白。改成 block + 浮动侧栏：可正常跨页流动。 */
  .pv-body { container-type: normal !important; padding: 0 !important; }
  .pv-layout.has-toc {
    /* flow-root：建 BFC 挡住正文首块的 margin 塌陷（否则整体被顶下 ~35px） */
    display: flow-root !important;
    padding: 0 !important;
  }
  .pv-layout.has-toc > .pv-side {
    float: left !important;
    width: 180px !important;
    margin: 0 20px 0 0 !important;
  }
  .pv-layout.has-toc > .pv-main-col { margin-left: 200px !important; }
  .pv-layout.has-toc.toc-right > .pv-side {
    float: right !important;
    margin: 0 0 0 20px !important;
  }
  .pv-layout.has-toc.toc-right > .pv-main-col { margin-left: 0 !important; margin-right: 200px !important; }
  .pv-layout.has-toc::after { content: ''; display: block; clear: both; }
  .pv-layout.has-toc > :first-child { display: block !important; }
  /* 🔴 目录组件自带 `@media (max-width:1099.98px){ .hr-outline{display:none} }`，A4 页宽 ~794px 会命中 → 打印里强制显示 */
  .pv-side :deep(.hr-outline) { display: block !important; }
  /* 块尽量不跨页断开：只对「原子小件」生效（表格行 / 图 / 引用 / 提示 / 指标）。
     🔴 绝不能套到 `.hb`（所有块的通用类）上：一个卡片块可能比整页还高，
     一旦 break-inside:avoid 就整块被推到下一页 → 上一页只剩封面 + 一大段空白。 */
  .pv-content :deep(tr),
  .pv-content :deep(img),
  .pv-content :deep(figure),
  .pv-content :deep(pre),
  .pv-content :deep(.hb-quote),
  .pv-content :deep(.hb-alert),
  .pv-content :deep(.hb-metrics) { break-inside: avoid; }
  /* 段落别留孤行/寡行 */
  .pv-content :deep(p) { orphans: 2; widows: 2; }
}
</style>
