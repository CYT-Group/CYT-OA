<!--
QuoteColorPopover — 引用「竖线色 + 背景色」配版面版（v1.19）。

用户 2026-10-16：引用样式「太丑」、交互也要升级 → 把原来两个散按钮（竖线色 / 背景色）
换成一个面板：顶部**实时预览** + 一排**快捷配色** + 竖线/背景两个色行。

presentational 组件，受控：调用方传 `line` / `bg`，改动经 `@update` 冒上去，
值回流成 props 后预览即时刷新（不做内部一份 state，单一真相在父级）。
`themePrimary` 只用于「未选色」时预览/色板兜底展示，不决定落库值。
-->
<template>
  <div ref="rootEl" class="qcp" @click.stop>
    <!-- 实时预览：当前竖线 + 背景下的引文效果 -->
    <div class="qcp-preview" :style="previewStyle">
      <div class="qcp-preview-text">引用预览：这句话按你选的竖线色与背景色显示</div>
    </div>

    <div class="qcp-block">
      <span class="qcp-label">快捷配色</span>
      <div class="qcp-presets">
        <button
          v-for="p in presets" :key="p.name" type="button" class="qcp-preset"
          :style="{ borderLeftColor: p.line, background: p.bg }"
          :title="p.name"
          @click="applyPreset(p)"
        ></button>
      </div>
    </div>

    <div class="qcp-block">
      <div class="qcp-row">
        <span class="qcp-label">竖线色</span>
        <div class="qcp-ctl">
          <input type="color" class="qcp-color" :value="line || themePrimary" @input="onLine($event.target.value)">
          <input
            type="text" class="qcp-hex" maxlength="7" :value="line"
            placeholder="跟随主题"
            @input="onHex('line', $event.target.value)"
            @keydown.enter.prevent
          >
          <button type="button" class="qcp-clear" @click="clear('line')">清除</button>
        </div>
      </div>
      <div class="qcp-row">
        <span class="qcp-label">背景色</span>
        <div class="qcp-ctl">
          <input type="color" class="qcp-color" :value="bg || '#ffffff'" @input="onBg($event.target.value)">
          <input
            type="text" class="qcp-hex" maxlength="7" :value="bg"
            placeholder="跟随主题"
            @input="onHex('bg', $event.target.value)"
            @keydown.enter.prevent
          >
          <button type="button" class="qcp-clear" @click="clear('bg')">清除</button>
        </div>
      </div>
    </div>

    <div class="qcp-foot">
      <small class="qcp-hint">留空 = 跟随文章主题色；竖线与背景各自独立</small>
      <button type="button" class="qcp-done" @click="$emit('close')">完成</button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  line: { type: String, default: '' },   // 竖线色 #RRGGBB；空串 = 跟随主题
  bg: { type: String, default: '' },      // 背景色 #RRGGBB；空串 = 跟随主题浅底
  themePrimary: { type: String, default: '#6366f1' },
})
const emit = defineEmits(['update', 'close'])

const rootEl = ref(null)
function onDocClick(e) {
  if (rootEl.value && !rootEl.value.contains(e.target)) emit('close')
}
function onDocKeydown(e) { if (e.key === 'Escape') emit('close') }
onMounted(() => {
  document.addEventListener('click', onDocClick)
  document.addEventListener('keydown', onDocKeydown)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onDocKeydown)
})

const HEX6 = /^#[0-9a-fA-F]{6}$/

// 快捷配色（line + bg 配套，观感统一；名字给 title 提示）
const presets = [
  { name: '墨绿浅草', line: '#16a34a', bg: '#f0fdf4' },
  { name: '朱砂浅粉', line: '#ef4444', bg: '#fef2f2' },
  { name: '靛蓝浅蓝', line: '#6366f1', bg: '#eef2ff' },
  { name: '琥珀浅米', line: '#f59e0b', bg: '#fffbeb' },
  { name: '玫红浅紫', line: '#d946ef', bg: '#fdf4ff' },
  { name: '石墨浅灰', line: '#475569', bg: '#f8fafc' },
]

const previewStyle = computed(() => ({
  borderLeft: `4px solid ${props.line || props.themePrimary}`,
  background: props.bg || 'rgba(99,102,241,0.05)',
}))

function applyPreset(p) { emit('update', { line: p.line, bg: p.bg }) }
function onLine(v) { if (HEX6.test(v)) emit('update', { line: v, bg: props.bg }) }
function onBg(v) { if (HEX6.test(v)) emit('update', { line: props.line, bg: v }) }
// hex 文本：敲空串 = 清掉该项；敲完整 hex 才生效，半成品不动（避免输入中乱跳）
function onHex(which, text) {
  const v = (text || '').trim()
  if (v === '') emit('update', which === 'line' ? { line: '', bg: props.bg } : { line: props.line, bg: '' })
  else if (HEX6.test(v)) emit('update', which === 'line' ? { line: v, bg: props.bg } : { line: props.line, bg: v })
}
function clear(which) { onHex(which, '') }
</script>

<style scoped>
.qcp {
  width: 268px;
  padding: 12px;
  border: 1px solid rgba(99, 102, 241, 0.22);
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 12px 40px rgba(13, 16, 38, 0.16);
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-family: inherit;
}
/* 实时预览：右上角小标签 + 一块引文样式 */
.qcp-preview {
  position: relative;
  padding: 12px 14px;
  border-radius: 0 10px 10px 0;
  min-height: 44px;
}
.qcp-preview-text { font-size: 13px; line-height: 1.6; color: #2d3258; }
.qcp-preview::after {
  content: '预览';
  position: absolute; top: 6px; right: 8px;
  font-size: 10px; letter-spacing: .08em; color: #8c92ab; opacity: .8;
}
.qcp-block { display: flex; flex-direction: column; gap: 8px; }
.qcp-label { font-size: 11.5px; font-weight: 700; color: #5a5f7a; }
.qcp-presets { display: flex; flex-wrap: wrap; gap: 7px; }
.qcp-preset {
  width: 30px; height: 24px; padding: 0;
  border: 1px solid rgba(13,16,38,.12);
  border-left-width: 4px;
  border-radius: 0 6px 6px 0;
  cursor: pointer;
  transition: transform .12s, box-shadow .12s;
}
.qcp-preset:hover { transform: translateY(-1px); box-shadow: 0 3px 10px rgba(13,16,38,.14); }
.qcp-row { display: flex; align-items: center; gap: 8px; }
.qcp-row .qcp-label { flex: none; width: 52px; }
.qcp-ctl { display: flex; align-items: center; gap: 6px; flex: 1; min-width: 0; }
.qcp-color {
  width: 30px; height: 26px; padding: 0; border: 1px solid rgba(13,16,38,.15);
  border-radius: 7px; background: #fff; cursor: pointer; flex: none;
}
.qcp-hex {
  flex: 1; min-width: 0; height: 26px; padding: 0 8px;
  border: 1px solid rgba(99,102,241,.2); border-radius: 7px;
  font-family: var(--fm, ui-monospace, monospace); font-size: 12px; color: #2d3258;
}
.qcp-hex:focus { outline: none; border-color: var(--v, #6366f1); }
.qcp-clear {
  flex: none; padding: 3px 8px; border: 1px solid rgba(13,16,38,.14);
  border-radius: 7px; background: #fff; color: #8c92ab; font-size: 11px; cursor: pointer;
}
.qcp-clear:hover { color: #475569; border-color: rgba(13,16,38,.3); }
.qcp-foot { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.qcp-hint { color: #8c92ab; line-height: 1.4; }
.qcp-done {
  flex: none; padding: 6px 14px; border: 0; border-radius: 8px;
  background: var(--v, #6366f1); color: #fff; font-size: 12px; font-weight: 700; cursor: pointer;
}
.qcp-done:hover { filter: brightness(1.05); }
</style>
