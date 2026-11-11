<!--
HrBlockListEditor — HR 文档块列表编辑器（10 种块，契约见 doc_ww/口径-HR文档块编辑-后端.md §2；
v1.1 起 timeline 块已删除，见前端口径 §5）。

只管「块数组怎么编辑」：块卡片（折叠/上移/下移/删除）+ 块间 hover 插入条 +
类型选择面板 + 各块表单 + 结构化块内联实时预览（复用 HrBlocksRenderer 单块渲染）。
校验、保存、大纲等文档级逻辑都在父页面 HrDocEditorView。

数据流：v-model:blocks。结构变更（增/删/移）emit 新数组；块内字段直接
v-model 到共享对象引用上——父级 reactive 持有同一批对象，改动即时可见。

富文本一律 compact 模式（内建工具栏、无图片按钮）：图片只走图片块的
ImageUpload 即传即压，规避整篇模式的 blob 延迟上传（flushPendingImages）链路。
text/imageText 额外开 headings（h2-h4 下拉）——标题要进左侧大纲。
-->
<template>
  <div ref="rootEl" class="hbe">
    <div v-if="!blocks.length" class="hbe-empty">
      文档还没有内容块 — 点下方「＋ 添加块」开始搭建
    </div>

    <template v-for="(blk, i) in blocks" :key="blockKey(blk)">
      <!-- 块间插入条：hover 显形，点击弹类型面板在此位置插入 -->
      <div class="hbe-gap" :data-picker-anchor="i">
        <button type="button" class="gap-plus" title="在此插入块" @click.stop="openPicker(i, $event)">＋</button>
      </div>

      <div class="blk-card" :class="{ err: !!errors[i] }" :data-blk-index="i">
        <div class="blk-hd">
          <button type="button" class="blk-fold" :title="isCollapsed(blk) ? '展开' : '折叠'" @click="toggleCollapse(blk)">
            {{ isCollapsed(blk) ? '▸' : '▾' }}
          </button>
          <span class="blk-badge">{{ typeMeta(blk.type).icon }} {{ typeMeta(blk.type).label }}</span>
          <span v-if="blk.type === 'rawhtml'" class="blk-adv">高级</span>
          <span class="blk-no">#{{ i + 1 }}</span>
          <span v-if="isCollapsed(blk)" class="blk-summary">{{ blockSummary(blk) }}</span>
          <div class="blk-actions">
            <button type="button" class="blk-ord" :disabled="i === 0" title="上移" @click="move(i, -1)">↑</button>
            <button type="button" class="blk-ord" :disabled="i === blocks.length - 1" title="下移" @click="move(i, 1)">↓</button>
            <button type="button" class="blk-ord" :disabled="blocks.length >= 100" title="复制此块" @click="duplicateAt(i)">⧉</button>
            <button type="button" class="blk-del" title="删除此块" @click="removeAt(i)">✕</button>
          </div>
        </div>
        <div v-if="errors[i]" class="blk-errmsg">⚠ {{ errors[i] }}</div>

        <!-- v-show 而非 v-if：折叠时保住 TipTap 实例，展开不丢光标历史 -->
        <div v-show="!isCollapsed(blk)" class="blk-body">
          <!-- text：富文本段（headings：h2-h4 进左侧大纲，块编辑器专属开关） -->
          <RichTextEditor v-if="blk.type === 'text'" v-model="blk.text" :theme="theme" compact headings placeholder="写一段正文…" />

          <!-- section：小节标题 -->
          <input
            v-else-if="blk.type === 'section'" v-model="blk.title" type="text"
            class="hbe-input" maxlength="200" placeholder="小节标题（自动编号，会出现在左侧大纲）"
          >

          <!-- image：图片 + 图说 -->
          <template v-else-if="blk.type === 'image'">
            <ImageUpload v-model="blk.image" folder="hr/doc-img" small hint="正文完整显示不裁切 · 上传自动压缩" />
            <input v-model="blk.caption" type="text" class="hbe-input mt8" maxlength="200" placeholder="图说（可空）">
          </template>

          <!-- imageText：图 + 小标题 + 富文本 -->
          <template v-else-if="blk.type === 'imageText'">
            <ImageUpload v-model="blk.image" folder="hr/doc-img" small hint="桌面端左图右文 · 上传自动压缩" />
            <input v-model="blk.title" type="text" class="hbe-input mt8" maxlength="200" placeholder="小标题（可空）">
            <RichTextEditor v-model="blk.text" :theme="theme" compact headings class="mt8" placeholder="段落正文…" />
          </template>

          <!-- callout：变体 + 富文本 -->
          <template v-else-if="blk.type === 'callout'">
            <select v-model="blk.variant" class="hbe-select">
              <option value="info">💡 信息（蓝）</option>
              <option value="success">✅ 成功（绿）</option>
              <option value="warning">⚠️ 警告（黄）</option>
              <option value="danger">⛔ 危险（红）</option>
            </select>
            <RichTextEditor v-model="blk.text" :theme="theme" compact class="mt8" placeholder="提示内容…" />
          </template>

          <!-- keypoints：icon + 标题 + 说明 -->
          <template v-else-if="blk.type === 'keypoints'">
            <div v-for="(it, j) in blk.items" :key="j" class="hbe-item">
              <div class="hbe-item-hd">
                <span class="hbe-item-no">条目 {{ j + 1 }}</span>
                <div class="hbe-item-act">
                  <button type="button" :disabled="j === 0" title="上移" @click="moveItem(blk, j, -1)">↑</button>
                  <button type="button" :disabled="j === blk.items.length - 1" title="下移" @click="moveItem(blk, j, 1)">↓</button>
                  <button type="button" class="del" title="删除条目" @click="blk.items.splice(j, 1)">✕</button>
                </div>
              </div>
              <div class="hbe-grid ico">
                <div class="hbe-iconbox">
                  <input v-model="it.icon" type="text" class="hbe-input" maxlength="16" placeholder="emoji（可空）">
                  <button type="button" class="hbe-emoji-btn" title="选择 emoji（也可直接在左边输入文字）" @click.stop="toggleEmojiPop(it)">😀</button>
                  <EmojiPickerPopover v-if="emojiPopItem === it" @select="v => pickEmoji(it, v)" @close="emojiPopItem = null" />
                </div>
                <input v-model="it.title" type="text" class="hbe-input" maxlength="200" placeholder="要点标题">
              </div>
              <textarea v-model="it.desc" class="hbe-ta mt6" rows="2" maxlength="2000" placeholder="说明（可空，纯文本）"></textarea>
            </div>
            <button type="button" class="hbe-add-item" :disabled="blk.items.length >= 30" @click="addItem(blk)">＋ 添加条目</button>
          </template>

          <!-- cards：列数 + icon/标题/正文 -->
          <template v-else-if="blk.type === 'cards'">
            <select v-model.number="blk.columns" class="hbe-select">
              <option :value="2">2 列</option>
              <option :value="3">3 列</option>
            </select>
            <div v-for="(it, j) in blk.items" :key="j" class="hbe-item">
              <div class="hbe-item-hd">
                <span class="hbe-item-no">卡片 {{ j + 1 }}</span>
                <div class="hbe-item-act">
                  <button type="button" :disabled="j === 0" title="上移" @click="moveItem(blk, j, -1)">↑</button>
                  <button type="button" :disabled="j === blk.items.length - 1" title="下移" @click="moveItem(blk, j, 1)">↓</button>
                  <button type="button" class="del" title="删除卡片" @click="blk.items.splice(j, 1)">✕</button>
                </div>
              </div>
              <div class="hbe-grid ico">
                <div class="hbe-iconbox">
                  <input v-model="it.icon" type="text" class="hbe-input" maxlength="16" placeholder="emoji（可空）">
                  <button type="button" class="hbe-emoji-btn" title="选择 emoji（也可直接在左边输入文字）" @click.stop="toggleEmojiPop(it)">😀</button>
                  <EmojiPickerPopover v-if="emojiPopItem === it" @select="v => pickEmoji(it, v)" @close="emojiPopItem = null" />
                </div>
                <input v-model="it.title" type="text" class="hbe-input" maxlength="200" placeholder="卡片标题">
              </div>
              <textarea v-model="it.text" class="hbe-ta mt6" rows="2" maxlength="2000" placeholder="卡片正文（可空，纯文本）"></textarea>
            </div>
            <button type="button" class="hbe-add-item" :disabled="blk.items.length >= 30" @click="addItem(blk)">＋ 添加卡片</button>
          </template>

          <!-- steps：标题 + 说明，渲染端自动编号 -->
          <template v-else-if="blk.type === 'steps'">
            <div v-for="(it, j) in blk.items" :key="j" class="hbe-item">
              <div class="hbe-item-hd">
                <span class="hbe-item-no">第 {{ j + 1 }} 步</span>
                <div class="hbe-item-act">
                  <button type="button" :disabled="j === 0" title="上移" @click="moveItem(blk, j, -1)">↑</button>
                  <button type="button" :disabled="j === blk.items.length - 1" title="下移" @click="moveItem(blk, j, 1)">↓</button>
                  <button type="button" class="del" title="删除步骤" @click="blk.items.splice(j, 1)">✕</button>
                </div>
              </div>
              <input v-model="it.title" type="text" class="hbe-input" maxlength="200" placeholder="步骤标题">
              <textarea v-model="it.desc" class="hbe-ta mt6" rows="2" maxlength="2000" placeholder="说明（可空，纯文本）"></textarea>
            </div>
            <button type="button" class="hbe-add-item" :disabled="blk.items.length >= 30" @click="addItem(blk)">＋ 添加步骤</button>
          </template>

          <!-- divider：可空短语 -->
          <input
            v-else-if="blk.type === 'divider'" v-model="blk.text" type="text"
            class="hbe-input" maxlength="2000" placeholder="居中短语（可空 = 纯分隔线）"
          >

          <!-- rawhtml：高级块，服务端还会再清洗一遍 -->
          <template v-else-if="blk.type === 'rawhtml'">
            <div class="hbe-warn">⚠ 高级块：HTML 入库会经服务端白名单清洗（script / 事件属性等会被剥除），发布前请预览确认最终效果。</div>
            <textarea v-model="blk.html" class="hbe-ta mono mt8" rows="10" placeholder="<div>…</div>" spellcheck="false"></textarea>
          </template>

          <!-- ── v1.3 新增 9 种（对齐参考「制度编辑器」）── -->

          <!-- quote：富文本引文 + 配面色版（v1.19：竖线/背景二合一，实时预览 + 快捷色） -->
          <template v-else-if="blk.type === 'quote'">
            <div class="hbe-quote-toolbar">
              <div class="hbe-quote-color">
                <button
                  type="button"
                  class="hbe-quote-color-btn"
                  :class="{ active: !!(blk.color || blk.bg) }"
                  title="引用配色：竖线色 + 背景色"
                  @click.stop="toggleQuoteColor(blk)"
                >
                  <span class="hbe-quote-swatch" :style="quoteSwatch(blk)"></span>
                  配色
                </button>
                <QuoteColorPopover
                  v-if="quoteColorPop === blk"
                  :line="blk.color || ''"
                  :bg="blk.bg || ''"
                  @update="v => setQuoteColors(blk, v)"
                  @close="quoteColorPop === blk && (quoteColorPop = null)"
                />
              </div>
            </div>
            <RichTextEditor
              v-model="blk.text"
              :theme="theme"
              compact placeholder="引用内容（渲染时左侧加品牌色竖线）"
            />
          </template>

          <!-- docref：文档引用（v1.17 三层）——
               ① 默认文章标题（选完文档自动带出，就是选择器里那行）
               ② 段落富文本（可加粗 / 调字体颜色等；留空 → 目标文档封面说明 / 引言）
               ③ 触达按钮（文字与链接都可自定义；留空各自走默认） -->
          <template v-else-if="blk.type === 'docref'">
            <HrDocRefPicker
              :document-id="blk.document_id"
              :slug="blk.slug"
              :title="blk.title"
              @select="doc => setDocRef(blk, doc)"
            />
            <RichTextEditor
              v-model="blk.text"
              :theme="theme"
              compact
              placeholder="段落说明（可加粗、调颜色；留空则用被引用文档的封面说明 / 引言）"
            />
            <div class="hbe-grid two mt6">
              <input
                v-model="blk.button_text" type="text" class="hbe-input" maxlength="200"
                placeholder="按钮文字（留空 = 前往《标题》→）"
              >
              <input
                v-model="blk.button_url" type="text" class="hbe-input" maxlength="500"
                placeholder="按钮链接（留空 = /article/文档ID；可填 https:// 外链）"
              >
            </div>
            <div class="hbe-hint">① 标题取被引用文档；② 段落与 ③ 按钮可自行编辑。阅读页只有这个按钮能点击跳转。</div>
          </template>

          <!-- metrics：数值 + 标签，渲染端固定 3 列 -->
          <template v-else-if="blk.type === 'metrics'">
            <div v-for="(it, j) in blk.items" :key="j" class="hbe-item">
              <div class="hbe-item-hd">
                <span class="hbe-item-no">指标 {{ j + 1 }}</span>
                <div class="hbe-item-act">
                  <button type="button" :disabled="j === 0" title="上移" @click="moveIn(blk.items, j, -1)">↑</button>
                  <button type="button" :disabled="j === blk.items.length - 1" title="下移" @click="moveIn(blk.items, j, 1)">↓</button>
                  <button type="button" class="del" title="删除指标" @click="blk.items.splice(j, 1)">✕</button>
                </div>
              </div>
              <div class="hbe-grid two">
                <input v-model="it.value" type="text" class="hbe-input" maxlength="200" placeholder="数值，如 09:00 / 98.6%">
                <input v-model="it.label" type="text" class="hbe-input" maxlength="200" placeholder="标签，如 上班时间">
              </div>
            </div>
            <button type="button" class="hbe-add-item" :disabled="blk.items.length >= 30" @click="addItem(blk)">＋ 添加指标</button>
          </template>

          <!-- compare：左右两栏，每栏「标题 + 颜色 + 要点（一行一条）」 -->
          <template v-else-if="blk.type === 'compare'">
            <div v-for="(side, si) in [blk.left, blk.right]" :key="si" class="hbe-item">
              <div class="hbe-item-hd">
                <span class="hbe-item-no">{{ si === 0 ? '左栏（✓）' : '右栏（✗）' }}</span>
              </div>
              <div class="hbe-grid ico">
                <input v-model="side.title" type="text" class="hbe-input" maxlength="200" placeholder="栏标题，如 制度优势">
                <div class="hbe-colorbox">
                  <input v-model="side.color" type="color" class="hbe-color" title="栏颜色">
                  <input v-model="side.color" type="text" class="hbe-input" maxlength="7" placeholder="#22c55e">
                </div>
              </div>
              <textarea
                class="hbe-ta mt6" rows="3" :value="(side.items || []).join('\n')"
                placeholder="每行一条要点（最多 30 条，空行会被忽略）"
                @input="setLines(side, 'items', $event)"
              ></textarea>
            </div>
          </template>

          <!-- status：三档状态 + 事项 + 标签 -->
          <template v-else-if="blk.type === 'status'">
            <div v-for="(it, j) in blk.items" :key="j" class="hbe-item">
              <div class="hbe-item-hd">
                <span class="hbe-item-no">条目 {{ j + 1 }}</span>
                <div class="hbe-item-act">
                  <button type="button" :disabled="j === 0" title="上移" @click="moveIn(blk.items, j, -1)">↑</button>
                  <button type="button" :disabled="j === blk.items.length - 1" title="下移" @click="moveIn(blk.items, j, 1)">↓</button>
                  <button type="button" class="del" title="删除条目" @click="blk.items.splice(j, 1)">✕</button>
                </div>
              </div>
              <div class="hbe-grid three">
                <select v-model="it.status" class="hbe-select">
                  <option value="pass">✓ 达标（绿）</option>
                  <option value="fail">✗ 未达标（红）</option>
                  <option value="warn">! 待改进（黄）</option>
                </select>
                <input v-model="it.text" type="text" class="hbe-input" maxlength="200" placeholder="事项">
                <input v-model="it.tag" type="text" class="hbe-input" maxlength="200" placeholder="标签，如 已达标">
              </div>
            </div>
            <button type="button" class="hbe-add-item" :disabled="blk.items.length >= 30" @click="addItem(blk)">＋ 添加条目</button>
          </template>

          <!-- alert：四型 + 标题 + 正文 -->
          <template v-else-if="blk.type === 'alert'">
            <select v-model="blk.variant" class="hbe-select">
              <option value="info">ℹ 信息（品牌色）</option>
              <option value="warn">⚠ 警示（黄）</option>
              <option value="danger">✗ 危险（红）</option>
              <option value="success">✓ 成功（绿）</option>
            </select>
            <input v-model="blk.title" type="text" class="hbe-input mt8" maxlength="200" placeholder="横幅标题，如 重要提醒">
            <RichTextEditor v-model="blk.text" :theme="theme" compact class="mt8" placeholder="正文（可空）" />
          </template>

          <!-- bento：2 列彩边卡片 -->
          <template v-else-if="blk.type === 'bento'">
            <div v-for="(c, j) in blk.cards" :key="j" class="hbe-item">
              <div class="hbe-item-hd">
                <span class="hbe-item-no">卡片 {{ j + 1 }}</span>
                <div class="hbe-item-act">
                  <button type="button" :disabled="j === 0" title="上移" @click="moveIn(blk.cards, j, -1)">↑</button>
                  <button type="button" :disabled="j === blk.cards.length - 1" title="下移" @click="moveIn(blk.cards, j, 1)">↓</button>
                  <button type="button" class="del" title="删除卡片" @click="blk.cards.splice(j, 1)">✕</button>
                </div>
              </div>
              <div class="hbe-grid ico">
                <div class="hbe-iconbox">
                  <input v-model="c.icon" type="text" class="hbe-input" maxlength="16" placeholder="emoji（可空）">
                  <button type="button" class="hbe-emoji-btn" title="选择 emoji（也可直接在左边输入文字）" @click.stop="toggleEmojiPop(c)">😀</button>
                  <EmojiPickerPopover v-if="emojiPopItem === c" @select="v => pickEmoji(c, v)" @close="emojiPopItem = null" />
                </div>
                <div class="hbe-colorbox">
                  <input v-model="c.color" type="color" class="hbe-color" title="左边条颜色">
                  <input v-model="c.color" type="text" class="hbe-input" maxlength="7" placeholder="#7c3aed">
                </div>
              </div>
              <input v-model="c.title" type="text" class="hbe-input mt6" maxlength="200" placeholder="卡片标题">
              <RichTextEditor v-model="c.desc" :theme="theme" compact class="mt6" placeholder="说明（可空）" />
            </div>
            <button type="button" class="hbe-add-item" :disabled="blk.cards.length >= 30" @click="addItem(blk)">＋ 添加卡片</button>
          </template>

          <!-- numsec：章节标题 + 正文 + 章色
               🔴 编号输入已下线（用户 2026-10-20：不要 01/02 自动编号，内容自定义就好）——
               章号直接写进标题文字（如「第一章 公司简介」），渲染端也不再画徽标。
               wire 里 blk.num 字段保留（后端契约不变、老草稿不报错），只是没有输入口。 -->
          <template v-else-if="blk.type === 'numsec'">
            <div class="hbe-numsec-top">
              <input v-model="blk.title" type="text" class="hbe-input hbe-numsec-fields" maxlength="200" placeholder="章节标题（如 第一章 公司简介）">
              <!-- 章色：点开色板；章头横幅与该章在左侧目录的条目共用此色（未配=跟随文章主题） -->
              <div class="hbe-chapcolor">
                <button
                  type="button" class="hbe-chapcolor-btn"
                  :class="{ set: !!blk.color }"
                  :style="blk.color ? { background: blk.color } : undefined"
                  :title="blk.color ? `本章配色 ${blk.color}（章头 + 目录联动）— 点击修改` : '给本章配色（章头 + 目录联动）；不配则跟随文章主题色'"
                  @click.stop="toggleChapColor(blk)"
                >{{ blk.color ? '' : '🎨' }}</button>
                <ChapterColorPopover
                  v-if="chapColorPop === blk"
                  :model-value="blk.color || ''"
                  @select="v => setChapColor(blk, v)"
                  @close="chapColorPop === blk && (chapColorPop = null)"
                />
              </div>
            </div>
            <RichTextEditor v-model="blk.text" :theme="theme" compact class="mt8" placeholder="章节正文（可空）" />
          </template>

          <!-- tabcard：页签 + 每组指标 -->
          <template v-else-if="blk.type === 'tabcard'">
            <div class="hbe-hint">阅读页点页签切换；★ = 默认显示的那一页</div>
            <div v-for="(t, j) in blk.tabs" :key="j" class="hbe-item">
              <div class="hbe-item-hd">
                <span class="hbe-item-no">页签 {{ j + 1 }}<template v-if="blk.activeTab === j"> · 默认</template></span>
                <div class="hbe-item-act">
                  <button type="button" :disabled="blk.activeTab === j" title="设为默认页签" @click="blk.activeTab = j">★</button>
                  <button type="button" :disabled="j === 0" title="上移" @click="moveIn(blk.tabs, j, -1)">↑</button>
                  <button type="button" :disabled="j === blk.tabs.length - 1" title="下移" @click="moveIn(blk.tabs, j, 1)">↓</button>
                  <button type="button" class="del" title="删除页签" @click="delTab(blk, j)">✕</button>
                </div>
              </div>
              <div class="hbe-grid two">
                <input v-model="t.name" type="text" class="hbe-input" maxlength="200" placeholder="页签名（如 日韩）">
                <input v-model="t.header" type="text" class="hbe-input" maxlength="200" placeholder="大标题">
              </div>
              <input v-model="t.subHeader" type="text" class="hbe-input mt6" maxlength="200" placeholder="副标题（可空）">
              <div v-for="(m, k) in t.metrics" :key="k" class="hbe-subrow">
                <input v-model="m.value" type="text" class="hbe-input" maxlength="200" placeholder="数值">
                <input v-model="m.label" type="text" class="hbe-input" maxlength="200" placeholder="标签">
                <button type="button" class="hbe-subdel" title="删除指标" @click="t.metrics.splice(k, 1)">✕</button>
              </div>
              <button type="button" class="hbe-add-item" :disabled="t.metrics.length >= 30" @click="addTabMetric(t)">＋ 添加指标</button>
              <RichTextEditor v-model="t.note" :theme="theme" compact class="mt6" placeholder="备注（可空）" />
            </div>
            <button type="button" class="hbe-add-item" :disabled="blk.tabs.length >= 8" @click="addTab(blk)">＋ 添加页签</button>
          </template>

          <!-- accordion：可折叠分组，每组「图标 + 标题 + 要点（一行一条）」 -->
          <template v-else-if="blk.type === 'accordion'">
            <div v-for="(it, j) in blk.items" :key="j" class="hbe-item">
              <div class="hbe-item-hd">
                <span class="hbe-item-no">条目 {{ j + 1 }}</span>
                <div class="hbe-item-act">
                  <button
                    type="button" class="hbe-toggle" :class="{ on: it.open }"
                    :title="it.open ? '阅读页默认展开（点击改为默认收起）' : '阅读页默认收起（点击改为默认展开）'"
                    @click="it.open = !it.open"
                  >{{ it.open ? '默认展开' : '默认收起' }}</button>
                  <button type="button" :disabled="j === 0" title="上移" @click="moveIn(blk.items, j, -1)">↑</button>
                  <button type="button" :disabled="j === blk.items.length - 1" title="下移" @click="moveIn(blk.items, j, 1)">↓</button>
                  <button type="button" class="del" title="删除条目" @click="blk.items.splice(j, 1)">✕</button>
                </div>
              </div>
              <div class="hbe-grid ico">
                <div class="hbe-iconbox">
                  <input v-model="it.icon" type="text" class="hbe-input" maxlength="16" placeholder="emoji（可空）">
                  <button type="button" class="hbe-emoji-btn" title="选择 emoji（也可直接在左边输入文字）" @click.stop="toggleEmojiPop(it)">😀</button>
                  <EmojiPickerPopover v-if="emojiPopItem === it" @select="v => pickEmoji(it, v)" @close="emojiPopItem = null" />
                </div>
                <input v-model="it.title" type="text" class="hbe-input" maxlength="200" placeholder="分组标题">
              </div>
              <textarea
                class="hbe-ta mt6" rows="3" :value="(it.lines || []).join('\n')"
                placeholder="每行一条要点（最多 30 条，空行会被忽略）"
                @input="setLines(it, 'lines', $event)"
              ></textarea>
            </div>
            <button type="button" class="hbe-add-item" :disabled="blk.items.length >= 30" @click="addItem(blk)">＋ 添加条目</button>
          </template>

          <!-- 结构化块所见即所得：单块传入渲染端唯一真相，员工端长什么样这里就长什么样。
               传 :block（对象引用）而不是 :blocks="[blk]"（数组字面量每次渲染都是新引用）——
               后者会让全部内联预览跟着任意一次父级渲染重渲染。
               分栏/预览态由父页传 hidePreview 关掉：右侧已经有一份完整实时预览了，
               块卡片里再来一份纯属重复，还白占竖向空间（口径 §22）。 -->
          <div v-if="PREVIEW_TYPES.has(blk.type) && !hidePreview" class="blk-preview">
            <div class="blk-preview-cap">预览</div>
            <HrBlocksRenderer :block="blk" :theme="theme" disable-navigation />
          </div>
        </div>
      </div>
    </template>

    <!-- 底部常驻添加入口（末尾插入） -->
    <div class="hbe-add" :data-picker-anchor="blocks.length">
      <button type="button" class="add-block-btn" @click.stop="openPicker(blocks.length, $event)">＋ 添加块</button>
    </div>

    <!-- 类型选择面板：单例 + 自管理开合 + Teleport 到 body。
         刻意不读父级响应式状态：早先 picker.open 一变就会把整张块列表（20 卡片 + 16 预览）重渲染一遍。 -->
    <HrBlockPicker ref="pickerRef" :groups="blockGroups" @pick="onPick" @close="clearAnchor" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import RichTextEditor from './ui/RichTextEditor.vue'
import ImageUpload from './news/ImageUpload.vue'
import HrBlocksRenderer from './HrBlocksRenderer.vue'
import HrBlockPicker from './HrBlockPicker.vue'
import EmojiPickerPopover from './ui/EmojiPickerPopover.vue'
import ChapterColorPopover from './ui/ChapterColorPopover.vue'
import QuoteColorPopover from './ui/QuoteColorPopover.vue'
import HrDocRefPicker from './ui/HrDocRefPicker.vue'
import { confirmDialog } from '../composables/useConfirm'

const props = defineProps({
  // v-model:blocks — 块数组（与父页面共享对象引用，字段编辑就地生效）
  blocks: { type: Array, default: () => [] },
  // 父页面预校验结果：{ 块下标: 错误文案 }，卡片红框 + 就地红字
  errors: { type: Object, default: () => ({}) },
  // 分栏/预览态下关掉块卡片里的内联预览（右侧已有完整预览，重复且占竖向空间）
  hidePreview: { type: Boolean, default: false },
  // 文档主题：内联预览也要跟随，否则卡片里是品牌紫、右侧预览是主题色，两处对不上
  theme: { type: String, default: '' },
})
const emit = defineEmits(['update:blocks'])

// 块的元数据表；面板不再直接平铺它，而是按下面的分组渲染（顺序只在分组内生效）
const BLOCK_TYPES = [
  { type: 'text',      icon: '📝', label: '文字段',   desc: '富文本正文，支持标题/列表/表格' },
  { type: 'section',   icon: '🔖', label: '小节标题', desc: '小节标题，进左侧大纲（不自动编号）' },
  { type: 'image',     icon: '🖼️', label: '图片',     desc: '单张图片 + 图说' },
  { type: 'imageText', icon: '🗞️', label: '图文段',   desc: '左图右文（移动端上下排）' },
  { type: 'callout',   icon: '💡', label: '提示块',   desc: '信息/成功/警告/危险四色' },
  { type: 'keypoints', icon: '📌', label: '要点列表', desc: '图标+标题+说明，双列排布' },
  { type: 'cards',     icon: '🗂️', label: '卡片组',   desc: '2 或 3 列卡片' },
  { type: 'steps',     icon: '👣', label: '步骤',     desc: '自动编号的操作步骤' },
  { type: 'divider',   icon: '➖', label: '分隔线',   desc: '可带居中短语' },
  // ── v1.3：对齐参考「制度编辑器」补齐的 9 种块 ──
  { type: 'quote',     icon: '❝', label: '引用',       desc: '左侧竖线 + 引文，可自定义颜色' },
  { type: 'docref',    icon: '📄', label: '文档引用', desc: '嵌入另一篇 HR 文档的引用卡片' },
  { type: 'metrics',   icon: '📊', label: '指标卡片',   desc: '3 列大数字 + 标签' },
  { type: 'compare',   icon: '⚖️', label: '对比卡',     desc: '左右两栏逐项对比' },
  { type: 'status',    icon: '📋', label: '状态列表',   desc: '三档状态圆点 + 标签' },
  { type: 'alert',     icon: '⚠️', label: '警示横幅',   desc: '四型全宽横幅（带标题）' },
  { type: 'bento',     icon: '⊞', label: 'Bento 网格', desc: '2 列彩边卡片' },
  { type: 'numsec',    icon: '§',  label: '章节标题',   desc: '章节大标题 + 正文（章号写进标题）' },
  { type: 'tabcard',   icon: '🏷️', label: '标签指标卡', desc: '页签切换 + 指标组' },
  { type: 'accordion', icon: '📂', label: '下拉表格',   desc: '可折叠的要点列表' },
  { type: 'rawhtml',   icon: '⚙️', label: '原样 HTML', desc: '直接写 HTML（服务端会清洗）', advanced: true },
]
const TYPE_META = Object.fromEntries(BLOCK_TYPES.map(t => [t.type, t]))
const typeMeta = (t) => TYPE_META[t] || { icon: '❔', label: t }

// 面板分组（口径-HR文档块编辑-前端.md §9）：新增块时往对应分组里加一个 type 即可，
// 不用改面板结构 —— 这是「块类型会一直加」的伸缩位，避免再出现「一张卡一个说明」堆成一长条。
const BLOCK_GROUPS = [
  { name: '文字与图文', types: ['text', 'section', 'numsec', 'image', 'imageText', 'divider'] },
  { name: '提示与强调', types: ['callout', 'alert', 'quote', 'docref'] },
  { name: '列表与卡片', types: ['keypoints', 'cards', 'steps', 'bento'] },
  { name: '数据展示', types: ['metrics', 'compare', 'status', 'tabcard'] },
  { name: '折叠与高级', types: ['accordion', 'rawhtml'] },
]
// 只保留 BLOCK_TYPES 里真实存在的 type（写错分组名不会让面板炸掉，只是少一项）
const blockGroups = BLOCK_GROUPS
  .map(g => ({ name: g.name, items: g.types.map(t => TYPE_META[t]).filter(Boolean) }))
  .filter(g => g.items.length)

// 有「所见即所得」价值的结构化块才带内联预览；text/imageText 富文本编辑器本身
// 就是所见即所得，image 有缩略图，rawhtml 让用户去预览页看清洗后效果
const PREVIEW_TYPES = new Set([
  'section', 'callout', 'keypoints', 'cards', 'steps', 'divider',
  // v1.3 新增块全部是「结构化视觉块」，内联预览价值最高
  'quote', 'docref', 'metrics', 'compare', 'status', 'alert', 'bento', 'numsec', 'tabcard', 'accordion',
])

// 新块骨架：键集必须与后端 _BLOCK_FIELDS 完全一致（多键少键都 422），
// 「可空」字段一律给空串而不是省略键
function blankBlock(type) {
  switch (type) {
    case 'text': return { type, text: '' }
    case 'section': return { type, title: '' }
    case 'image': return { type, image: '', caption: '' }
    case 'imageText': return { type, image: '', title: '', text: '' }
    case 'callout': return { type, variant: 'info', text: '' }
    case 'keypoints': return { type, items: [blankItem(type)] }
    case 'cards': return { type, columns: 2, items: [blankItem(type), blankItem(type)] }
    case 'steps': return { type, items: [blankItem(type)] }
    case 'divider': return { type, text: '' }
    case 'rawhtml': return { type, html: '' }
    // ── v1.3 新增 9 种（键集必须与后端 _BLOCK_FIELDS 完全一致）──
    // v1.18：quote 加 bg（背景色），与 color（竖线色）都默认真空串 = 跟随文档主题
    case 'quote': return { type, text: '', color: '', bg: '' }
    case 'metrics': return { type, items: [blankItem(type), blankItem(type), blankItem(type)] }
    case 'compare': return { type, left: blankSide('优势', '#22c55e'), right: blankSide('不足', '#ef4444') }
    case 'status': return { type, items: [blankItem(type)] }
    case 'alert': return { type, variant: 'warn', title: '', text: '' }
    case 'bento': return { type, cards: [blankItem(type), blankItem(type)] }
    case 'numsec': return { type, num: '', title: '', text: '', color: '' }
    case 'tabcard': return { type, activeTab: 0, tabs: [blankTab()] }
    case 'accordion': return { type, items: [blankItem(type)] }
    // v1.17 三层：② 段落富文本 ③ 按钮文字/链接（空串 = 未自定义，渲染端回落默认）
    case 'docref': return { type, document_id: 0, title: '', text: '', button_text: '', button_url: '' }
    default: return { type: 'text', text: '' }
  }
}
// compare 的单侧：与后端 _SIDE_FIELDS 键集一致（title / color / items）
function blankSide(title, color) { return { title, color, items: [''] } }
// tabcard 的页签：与后端 _TAB_FIELDS 键集一致（name / header / subHeader / metrics / note）
function blankTab() {
  return { name: '', header: '', subHeader: '', metrics: [{ value: '', label: '' }], note: '' }
}
function blankItem(btype) {
  if (btype === 'keypoints') return { icon: '', title: '', desc: '' }
  if (btype === 'cards') return { icon: '', title: '', text: '' }
  if (btype === 'metrics') return { value: '', label: '' }
  if (btype === 'status') return { status: 'pass', text: '', tag: '' }
  if (btype === 'bento') return { icon: '', title: '', desc: '', color: '#7c3aed' }
  if (btype === 'accordion') return { icon: '', title: '', lines: [''], open: true }
  return { title: '', desc: '' } // steps
}

// ── 稳定 key：按对象引用发号 ──
// 移动块时 key 跟着对象走，Vue 直接搬 DOM 节点，TipTap 编辑器实例不销毁重建
let uidSeq = 0
const uids = new WeakMap()
function blockKey(blk) {
  if (!uids.has(blk)) uids.set(blk, ++uidSeq)
  return uids.get(blk)
}

// ── 折叠：按对象引用记（下标会随增删移漂移）──
const collapsed = ref(new Set())
const isCollapsed = (blk) => collapsed.value.has(blk)
function toggleCollapse(blk) {
  const next = new Set(collapsed.value)
  if (next.has(blk)) next.delete(blk)
  else next.add(blk)
  collapsed.value = next
}

// 折叠时给一行内容摘要，不展开也能认出是哪块
// ⚠️ DOMParser 每次约 1ms，而摘要在每次渲染时都要重算 → 按 HTML 字符串缓存。
// 缓存带上限（300 条），防长会话里无限涨。
const stripCache = new Map()
function stripHtml(h) {
  if (!h) return ''
  let v = stripCache.get(h)
  if (v === undefined) {
    v = (new DOMParser().parseFromString(h, 'text/html').body.textContent || '').trim()
    if (stripCache.size > 300) stripCache.clear()
    stripCache.set(h, v)
  }
  return v
}
function blockSummary(blk) {
  const strip = stripHtml
  let s = ''
  if (blk.type === 'text') s = strip(blk.text)
  else if (blk.type === 'section') s = blk.title
  else if (blk.type === 'image') s = blk.caption || blk.image
  else if (blk.type === 'imageText') s = blk.title || strip(blk.text)
  else if (blk.type === 'callout') s = strip(blk.text)
  else if (blk.type === 'divider') s = blk.text
  else if (blk.type === 'rawhtml') s = strip(blk.html)
  // ── v1.3 新增 9 种 ──
  else if (blk.type === 'quote') s = strip(blk.text)
  else if (blk.type === 'metrics') s = `${blk.items.length} 项：` + blk.items.map(it => it.value || '').filter(Boolean).join(' / ')
  else if (blk.type === 'compare') s = `${(blk.left && blk.left.title) || ''} vs ${(blk.right && blk.right.title) || ''}`
  else if (blk.type === 'status') s = `${blk.items.length} 条：` + blk.items.map(it => it.text || '').filter(Boolean).join(' / ')
  else if (blk.type === 'alert') s = blk.title || strip(blk.text)
  else if (blk.type === 'bento') s = `${blk.cards.length} 张：` + blk.cards.map(c => c.title || '').filter(Boolean).join(' / ')
  // 编号已下线（2026-10-20）：摘要只取标题，别再拼一个界面上看不到的 num
  else if (blk.type === 'numsec') s = blk.title || ''
  else if (blk.type === 'tabcard') s = `${blk.tabs.length} 个页签：` + blk.tabs.map(t => t.name || '').filter(Boolean).join(' / ')
  else if (blk.type === 'accordion') s = `${blk.items.length} 组：` + blk.items.map(it => it.title || '').filter(Boolean).join(' / ')
  else if (blk.type === 'docref') s = blk.title || (blk.document_id ? `文档 #${blk.document_id}` : blk.slug) || '(未选择)'
  else if (Array.isArray(blk.items)) s = `${blk.items.length} 条：` + blk.items.map(it => it.title || '').filter(Boolean).join(' / ')
  s = (s || '').replace(/\s+/g, ' ')
  return s.length > 40 ? s.slice(0, 40) + '…' : s
}

// ── 结构操作：增/删/移 emit 新数组（父级 v-model:blocks 接管）──
// 面板的开合与定位都在 HrBlockPicker 内部（见该组件顶部注释），这里只做两件事：
// 把点击点透传过去、给锚点加/去一个高亮类。
// ⚠️ 锚点高亮走**命令式 DOM 操作**而不是响应式 class 绑定 —— 绑进模板就等于让整张块列表
// 依赖「面板开着没」，开关一次就把 20 个块卡片 + 16 个内联预览全部重渲染（实测 28ms 掉帧）。
const rootEl = ref(null)
const pickerRef = ref(null)
let activeAnchorEl = null

function markAnchor(index) {
  clearAnchor()
  const el = rootEl.value && rootEl.value.querySelector(`[data-picker-anchor="${index}"]`)
  if (el) { el.classList.add('active'); activeAnchorEl = el }
}
function clearAnchor() {
  if (activeAnchorEl) { activeAnchorEl.classList.remove('active'); activeAnchorEl = null }
}
function openPicker(index, evt) {
  const p = pickerRef.value
  if (!p) return
  // 再点同一个入口 = 开关切换；
  // 点**另一个**入口 = 把面板挪到那个插入点（早先一律先关掉，得点两次才出来，
  // 眼睛看到的就是「面板一闪没了、再一闪出现在别处」= 抖）。
  if (p.isOpen() && p.currentIndex() === index) { closePicker(); return }
  markAnchor(index)
  p.open(index, evt)
}
function closePicker() {
  clearAnchor()
  if (pickerRef.value) pickerRef.value.close()
}
function onPick(type, index) {
  clearAnchor()
  insertAt(index, type)
}
function insertAt(index, type) {
  const next = props.blocks.slice()
  next.splice(index, 0, blankBlock(type))
  emit('update:blocks', next)
}
async function removeAt(index) {
  const blk = props.blocks[index]
  // 空骨架直接删；有内容的删除不可撤销，拦一道（用站内弹框，不用原生 confirm）
  if (blockSummary(blk)) {
    const ok = await confirmDialog({
      title: `删除第 ${index + 1} 块？`,
      message: `「${typeMeta(blk.type).label}」块里的内容会一并删除，不可恢复。`,
      confirmText: '删除',
      tone: 'danger',
    })
    if (!ok) return
  }
  const next = props.blocks.slice()
  next.splice(index, 1)
  emit('update:blocks', next)
}
// 复制块（对齐参考的 dupBlock）：深拷贝插到下一格。深拷贝是必须的——
// 直接塞同一个对象引用会让两个卡片编辑同一份数据
function duplicateAt(index) {
  if (props.blocks.length >= 100) return
  const copy = JSON.parse(JSON.stringify(props.blocks[index]))
  const next = props.blocks.slice()
  next.splice(index + 1, 0, copy)
  emit('update:blocks', next)
}
function move(index, dir) {
  const j = index + dir
  if (j < 0 || j >= props.blocks.length) return
  const next = props.blocks.slice()
  ;[next[index], next[j]] = [next[j], next[index]]
  emit('update:blocks', next)
}

// ── emoji 快捷面板：按条目对象引用记当前打开的（下标随增删移漂移，引用不会）──
// 面板只是快捷填入，icon 输入框仍可自由输入文字
const emojiPopItem = ref(null)
function toggleEmojiPop(it) {
  emojiPopItem.value = emojiPopItem.value === it ? null : it
}
function pickEmoji(it, v) {
  it.icon = v
  emojiPopItem.value = null
}

// ── 章节配色（numsec）：按块对象引用记当前打开的面板（同 emoji 面板的引用记法）──
// 空串 = 跟随文章主题色（后端 themecolor 契约放行空串），不是「无色」。
const chapColorPop = ref(null)
function toggleChapColor(blk) {
  chapColorPop.value = chapColorPop.value === blk ? null : blk
}
function setChapColor(blk, v) {
  blk.color = (v || '').trim()
  // 原生色板拖动会连续发 input；此时卸载面板会中断拖动，关闭留给外点 / Esc / 触发按钮。
}

// ── 独立引用块：选择器内部自持搜索状态，这里只把选择结果写回块契约 ──
function setDocRef(blk, doc) {
  blk.document_id = doc.document_id
  delete blk.slug
  blk.title = doc.title
}

// ── 独立 quote 块配色（v1.19）：竖线色 color / 背景色 bg 改用「配面色版」一个面板 ──
// 空串 = 跟随文章主题色（后端 themecolor 契约放行空串），不是「无色」。
const quoteColorPop = ref(null)
function toggleQuoteColor(blk) {
  quoteColorPop.value = quoteColorPop.value === blk ? null : blk
}
// 面板受控：每次改动写回块（面板保持打开，方便连调；点「完成 / 再点按钮」收起）
function setQuoteColors(blk, { line, bg }) {
  blk.color = (line || '').trim()
  blk.bg = (bg || '').trim()
}
// 「配色」按钮上的双色小样：优先竖线色、否则背景色；都没有就用 CSS 默认色
function quoteSwatch(blk) {
  const c = blk.color || blk.bg
  return c ? { background: c } : undefined
}

// 条目级操作直接改块内数组（对象与父级共享，reactive 即时生效）
// 条目容器键：bento 用 cards，其余结构化块用 items（与后端契约一致）
const itemKey = (blk) => (blk.type === 'bento' ? 'cards' : 'items')
function addItem(blk) {
  const key = itemKey(blk)
  if (blk[key].length >= 30) return
  blk[key].push(blankItem(blk.type))
}
function moveItem(blk, j, dir) {
  moveIn(blk[itemKey(blk)], j, dir)
}
// 通用数组内上下移（tabcard.tabs / bento.cards 等非 items 容器也用）
function moveIn(arr, j, dir) {
  const k = j + dir
  if (!Array.isArray(arr) || k < 0 || k >= arr.length) return
  ;[arr[j], arr[k]] = [arr[k], arr[j]]
}

// ── v1.3 新增块的辅助 ──
// 多行文本 ↔ 纯文本数组（compare.items / accordion.lines）：一行一条。
// 不做过滤（保留空行，避免输入时数组变化把光标顶回行尾）；空行由渲染端跳过。
function setLines(obj, key, e) {
  obj[key] = String(e.target.value).split('\n').slice(0, 30)
}
// tabcard 页签增删（删除时收敛 activeTab，避免越界）
function addTab(blk) {
  if (blk.tabs.length >= 8) return
  blk.tabs.push(blankTab())
}
function delTab(blk, j) {
  blk.tabs.splice(j, 1)
  if (blk.activeTab >= blk.tabs.length) blk.activeTab = Math.max(blk.tabs.length - 1, 0)
}
function addTabMetric(tab) {
  if (tab.metrics.length >= 30) return
  tab.metrics.push({ value: '', label: '' })
}

// 类型面板点外关闭（面板已 Teleport 到 body，面板内部自行 @click.stop）
function onDocClick() {
  if (pickerRef.value && pickerRef.value.isOpen()) closePicker()
}
function onDocKeydown(e) {
  if (e.key === 'Escape') onDocClick()
}
onMounted(() => {
  document.addEventListener('click', onDocClick)
  document.addEventListener('keydown', onDocKeydown)
})
onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onDocKeydown)
})
</script>

<style scoped>
.hbe { display: flex; flex-direction: column; }
.hbe-empty {
  padding: 36px 16px; text-align: center; font-size: 13px; color: var(--t4);
  border: 2px dashed rgba(99,102,241,0.18); border-radius: 12px;
}

/* ── 块间插入条：默认近隐形，hover / 面板打开时显形 ── */
.hbe-gap {
  position: relative; height: 18px; display: flex; align-items: center; justify-content: center;
}
.hbe-gap::before {
  content: ''; position: absolute; left: 0; right: 0; top: 50%; height: 2px;
  background: rgba(99,102,241,0.35); border-radius: 1px;
  opacity: 0; transition: opacity .12s;
}
.gap-plus {
  position: relative; z-index: 1;
  width: 22px; height: 22px; border-radius: 50%;
  border: 1px solid rgba(99,102,241,0.4); background: #fff; color: var(--v);
  font-size: 13px; line-height: 1; cursor: pointer;
  opacity: 0; transition: opacity .12s, transform .12s;
  display: flex; align-items: center; justify-content: center;
}
.hbe-gap:hover::before, .hbe-gap.active::before { opacity: 1; }
.hbe-gap:hover .gap-plus, .hbe-gap.active .gap-plus { opacity: 1; }
.gap-plus:hover { transform: scale(1.12); background: rgba(99,102,241,0.08); }

/* ── 类型选择面板：定位与内容都在 HrBlockPicker 里（Teleport 到 body，position:fixed）──
   父级只保留 .hbe-gap.active 的插入位置高亮（由 openPicker 命令式加类）。 */

/* ── 块卡片 ── */
.blk-card {
  background: rgba(255,255,255,0.9);
  border: 1px solid rgba(99,102,241,0.16); border-radius: 12px;
  padding: 0 0 2px;
}
.blk-card.err { border-color: rgba(220,38,38,0.55); box-shadow: 0 0 0 2px rgba(220,38,38,0.10); }
.blk-hd {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px;
}
.blk-fold {
  width: 24px; height: 24px; flex-shrink: 0; border: none; border-radius: 6px;
  background: transparent; color: var(--t4); cursor: pointer; font-size: 12px;
}
.blk-fold:hover { background: rgba(99,102,241,0.08); color: var(--v); }
.blk-badge {
  font-size: 12px; font-weight: 700; color: var(--v); white-space: nowrap;
  background: rgba(99,102,241,0.08); padding: 3px 10px; border-radius: 999px;
}
.blk-adv {
  font-size: 10px; font-weight: 700; color: #b45309; white-space: nowrap;
  background: rgba(245,158,11,0.14); padding: 2px 7px; border-radius: 999px;
}
.blk-no { font-family: var(--fm); font-size: 11px; color: var(--t5); }
.blk-summary {
  flex: 1; min-width: 0; font-size: 12px; color: var(--t4);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.blk-actions { margin-left: auto; display: flex; gap: 4px; flex-shrink: 0; }
.blk-ord, .blk-del {
  width: 26px; height: 26px; border-radius: 7px; border: 1px solid rgba(99,102,241,0.16);
  background: #fff; color: var(--t3); cursor: pointer; font-size: 12px;
}
.blk-ord:hover:not(:disabled) { color: var(--v); border-color: var(--v); }
.blk-ord:disabled { opacity: .35; cursor: not-allowed; }
.blk-del:hover { color: #DC2626; border-color: rgba(220,38,38,0.4); }
.blk-errmsg { padding: 0 12px 8px; font-size: 12px; color: #DC2626; font-weight: 600; }
.blk-body { padding: 2px 12px 12px; }

/* ── 表单元素 ── */
.hbe-input, .hbe-select, .hbe-ta {
  width: 100%; box-sizing: border-box; min-width: 0;
  padding: 8px 12px; border: 1px solid rgba(99,102,241,0.22); border-radius: 8px;
  background: #fff; font-size: 13.5px; color: var(--t); font-family: inherit;
}
.hbe-select { width: auto; min-width: 140px; }
.hbe-ta { resize: vertical; line-height: 1.6; }
.hbe-ta.mono { font-family: var(--fm, ui-monospace, monospace); font-size: 12.5px; line-height: 1.55; }
.hbe-input:focus, .hbe-select:focus, .hbe-ta:focus { outline: 2px solid var(--v); outline-offset: -1px; }
.mt6 { margin-top: 6px; }
.mt8 { margin-top: 8px; }

.hbe-warn {
  padding: 8px 12px; border-radius: 8px; font-size: 12px; line-height: 1.6;
  background: rgba(245,158,11,0.1); border: 1px solid rgba(245,158,11,0.3); color: #92400e;
}

/* ── 条目小表单 ── */
.hbe-item {
  margin-top: 10px; padding: 10px 12px;
  border: 1px solid rgba(99,102,241,0.12); border-radius: 10px;
  background: rgba(99,102,241,0.025);
}
.hbe-item-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; }
.hbe-item-no { font-size: 11.5px; font-weight: 700; color: var(--t4); letter-spacing: .04em; }
.hbe-item-act { display: flex; gap: 4px; }
/* 图标按钮（↑ ↓ ✕）：定宽方块。
   ⚠️ 必须排除 .hbe-toggle —— 它是文字按钮，被这条 22px 定宽压住会让「默认展开」
   四个字溢出边框（全局无 box-sizing: border-box，22px 是内容区，padding 另算，
   文字约 44px 而内容区只有 22px），溢出部分正好盖到旁边的 ↓ 上。 */
.hbe-item-act button:not(.hbe-toggle) {
  width: 22px; height: 22px; border-radius: 6px; border: 1px solid rgba(99,102,241,0.14);
  background: #fff; color: var(--t4); cursor: pointer; font-size: 11px;
}
.hbe-item-act button:hover:not(:disabled) { color: var(--v); border-color: var(--v); }
.hbe-item-act button.del:hover { color: #DC2626; border-color: rgba(220,38,38,0.4); }
.hbe-item-act button:disabled { opacity: .35; cursor: not-allowed; }
.hbe-grid { display: grid; gap: 6px; }
.hbe-grid.ico { grid-template-columns: 190px 1fr; }
/* icon 输入 + emoji 按钮 + 弹层的容器（弹层 absolute 以此为基准） */
.hbe-iconbox { position: relative; display: flex; gap: 4px; min-width: 0; }
.hbe-iconbox .hbe-input { flex: 1; min-width: 0; }
.hbe-emoji-btn {
  flex-shrink: 0; width: 36px; border-radius: 8px;
  border: 1px solid rgba(99,102,241,0.22); background: #fff;
  font-size: 15px; line-height: 1; cursor: pointer; padding: 0;
}
.hbe-emoji-btn:hover { border-color: var(--v); background: rgba(99,102,241,0.06); }
.hbe-add-item {
  margin-top: 10px; padding: 7px 14px; border-radius: 8px;
  border: 1px dashed rgba(99,102,241,0.35); background: transparent;
  color: var(--v); font-size: 12.5px; font-weight: 600; cursor: pointer; font-family: inherit;
}
.hbe-add-item:hover:not(:disabled) { background: rgba(99,102,241,0.06); }
.hbe-add-item:disabled { opacity: .4; cursor: not-allowed; }

/* ── v1.3 新增块的编辑器补充样式 ── */
/* 两列等分（metrics / numsec / compare / tabcard 的成对字段） */
.hbe-grid.two { grid-template-columns: repeat(2, minmax(0, 1fr)); }
/* 三列（status：状态下拉 + 事项 + 标签）——下拉给固定宽，其余等分 */
.hbe-grid.three { grid-template-columns: auto minmax(0, 1fr) minmax(0, 1fr); }
.hbe-grid.three .hbe-select { width: auto; min-width: 132px; }

/* 颜色选择：原生取色器 + 可手输 #RRGGBB（后端只收 6 位十六进制） */
.hbe-colorbox { position: relative; display: flex; gap: 6px; min-width: 0; }
.hbe-colorbox .hbe-input { flex: 1; min-width: 0; font-family: var(--fm, ui-monospace, monospace); }
.hbe-color {
  flex-shrink: 0; width: 38px; padding: 0; border: 1px solid rgba(99,102,241,0.22);
  border-radius: 8px; background: #fff; cursor: pointer;
}

/* numsec 章色：编号/标题一行 + 右侧小色点（色板弹层以此为基准 absolute） */
.hbe-numsec-top { display: flex; align-items: center; gap: 8px; }
.hbe-numsec-fields { flex: 1; min-width: 0; }
.hbe-chapcolor { position: relative; flex-shrink: 0; }
.hbe-chapcolor-btn {
  width: 36px; height: 34px; border-radius: 8px; padding: 0;
  border: 1px dashed rgba(99,102,241,0.35); background: #fff;
  font-size: 14px; line-height: 1; cursor: pointer;
}
.hbe-chapcolor-btn:hover { border-color: var(--v); }
/* 已配色的：实心色块 + 白描边，一眼看出本章有专属色 */
.hbe-chapcolor-btn.set { border: 1px solid rgba(13,16,38,0.14); box-shadow: inset 0 0 0 2px #fff; }

/* ── quote 颜色工具栏 ── */
.hbe-quote-toolbar {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 6px;
}
.hbe-quote-color { position: relative; }
/* v1.19 配面色版：相对「配色」按钮右上角弹出（该容器已 position:relative） */
.hbe-quote-color :deep(.qcp) { position: absolute; top: calc(100% + 8px); right: 0; z-index: 40; }
.hbe-quote-color-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 5px 9px; border: 1px solid rgba(99,102,241,.16); border-radius: 7px;
  background: #fff; color: var(--t4); font: inherit; font-size: 12px; cursor: pointer;
}
.hbe-quote-color-btn:hover,
.hbe-quote-color-btn.active { border-color: var(--v, #6366f1); color: var(--v, #6366f1); }
.hbe-quote-swatch {
  width: 14px; height: 14px; border-radius: 3px;
  background: var(--v, #6366F1);
  box-shadow: inset 0 0 0 1px rgba(13,16,38,0.15);
}
.hbe-quote-hex {
  font-family: var(--fm, ui-monospace, monospace);
  font-size: 11.5px; color: var(--t3);
}

/* tabcard 页签内的指标行：数值 / 标签 / 删除 */
.hbe-subrow { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto; gap: 6px; margin-top: 6px; }
.hbe-subdel {
  width: 32px; border-radius: 8px; border: 1px solid rgba(99,102,241,0.16);
  background: #fff; color: var(--t4); cursor: pointer; font-size: 12px;
}
.hbe-subdel:hover { color: #DC2626; border-color: rgba(220,38,38,0.4); }

/* accordion 的「默认展开 / 收起」开关按钮 */
.hbe-toggle {
  /* 宽度随文字自适应；flex 容器里不许被压缩，否则「默认展开」会挤变形 */
  flex: 0 0 auto;
  padding: 0 10px; height: 22px; border-radius: 6px; font-size: 11px; font-weight: 600;
  border: 1px solid rgba(99,102,241,0.16); background: #fff; color: var(--t4); cursor: pointer;
  font-family: inherit; white-space: nowrap;
}
.hbe-toggle.on { color: var(--v); border-color: rgba(99,102,241,0.45); background: rgba(99,102,241,0.08); }

.hbe-hint {
  margin-bottom: 10px; padding: 7px 12px; border-radius: 8px;
  font-size: 12px; line-height: 1.6; color: var(--t4);
  background: rgba(99,102,241,0.05); border: 1px solid rgba(99,102,241,0.12);
}

/* ── 内联实时预览 ── */
.blk-preview {
  margin-top: 12px; padding: 14px 16px 16px;
  border: 1px dashed rgba(99,102,241,0.25); border-radius: 10px;
  background: rgba(250,250,255,0.6);
}
.blk-preview-cap {
  font-family: var(--fm); font-size: 10px; letter-spacing: .14em; text-transform: uppercase;
  color: var(--t4); margin-bottom: 10px;
}

/* ── 底部常驻添加 ── */
.hbe-add { position: relative; margin-top: 14px; display: flex; justify-content: center; }
.add-block-btn {
  width: 100%; padding: 12px; border-radius: 12px;
  border: 2px dashed rgba(99,102,241,0.3); background: transparent;
  color: var(--v); font-size: 13.5px; font-weight: 700; cursor: pointer; font-family: inherit;
  transition: all .15s;
}
.add-block-btn:hover { border-color: var(--v); background: rgba(99,102,241,0.05); }

@media (max-width: 768px) {
  .hbe-grid.ico { grid-template-columns: 1fr; }
  .hbe-grid.two, .hbe-grid.three { grid-template-columns: 1fr; }
  .hbe-grid.three .hbe-select { width: 100%; }
  .hbe-subrow { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto; }
  .blk-summary { display: none; }
}
</style>
