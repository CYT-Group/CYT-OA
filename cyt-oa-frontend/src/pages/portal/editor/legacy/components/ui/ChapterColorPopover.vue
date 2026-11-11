<!--
ChapterColorPopover — 选色弹层（HR 块编辑「编号章节 numsec」的章色选择器 + 富文本工具栏的「文字颜色」）。

用户 2026-10-14：「章节的颜色可以自定义，包括目录的也是」+「参考表格这样设计不好吗」
→ 颜色**放在块内**（不跳后台页），点标题行右侧的小色点打开本弹层。
用户 2026-10-15：「能编辑字体的地方都加上编辑颜色这样，就是添加块那里」
→ 同一个弹层复用到富文本工具栏，选中的文字上色（TipTap TextStyle 的 color mark）。

两个场景共用一套 UI，只有文案不同 → 文案走 props，默认值保持章色原样（零变化）：

约定（与 EmojiPickerPopover 同构，保持项目内一致）：
- 选色 emit('select', '#RRGGBB')；「清空」emit('select', '')；点外 / Esc emit('close')
- 开关状态由父级持有（v-if 挂载）；absolute 挂在触发处下方，父级容器需 position: relative
- 触发按钮自己 @click.stop，否则打开的同一次点击会冒泡到 document 立刻关闭
- 章色场景**空串 = 跟随文档主题**（后端 themecolor 契约放行空串），不是「无色」；
  文字颜色场景空串 = 不设置颜色。核心语义由调用方决定，本组件只负责传话。
-->
<template>
  <div ref="rootEl" class="ccp" @click.stop>
    <button type="button" class="ccp-clear" @click="emit('select', '')">
      <span class="ccp-clear-dot"></span> {{ clearLabel }}
    </button>

    <div class="ccp-cap">预设</div>
    <div class="ccp-grid">
      <button
        v-for="p in PRESETS" :key="p.hex" type="button" class="ccp-cell"
        :class="{ on: same(p.hex) }" :title="p.name + ' ' + p.hex"
        :style="{ background: p.hex }" @click="emit('select', p.hex)"
      ></button>
    </div>

    <div class="ccp-cap">自定义</div>
    <div class="ccp-row">
      <input
        type="color" class="ccp-picker" :value="hexOrFallback"
        @input="onPicker" title="取色器"
      >
      <input
        type="text" class="ccp-hex" maxlength="7" placeholder="#16A34A"
        :value="modelValue" @input="onHex"
      >
    </div>
    <div class="ccp-hint">{{ modelValue ? setHint : clearHint }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },   // 当前色，'' = 见 clearLabel 的语义
  // 「清空」项文案（章色 = 跟随文章主题；文字颜色 = 不加颜色）
  clearLabel: { type: String, default: '跟随文章主题色' },
  // 底部提示：未设色 / 已设色 两种。默认值 = 章色语境原样
  clearHint: { type: String, default: '当前：跟随文章主题色' },
  setHint: { type: String, default: '当前：本章固定用此色（章头 + 目录联动）' },
})
const emit = defineEmits(['select', 'close'])

// 与 data/hr-themes.ts 的 6 套预设同源（用户已熟悉这几色）+ 品牌默认
const PRESETS = [
  { name: '品牌紫', hex: '#6366f1' },
  { name: '优雅紫', hex: '#7c3aed' },
  { name: '商务蓝', hex: '#2563eb' },
  { name: '暖橙', hex: '#ea580c' },
  { name: '森林绿', hex: '#16a34a' },
  { name: '玫红', hex: '#e11d48' },
  { name: '极简灰', hex: '#334155' },
  { name: '正红', hex: '#dc2626' },
]

const HEX6 = /^#[0-9a-fA-F]{6}$/
const same = (hex) => !!props.modelValue && props.modelValue.toLowerCase() === hex.toLowerCase()
const hexOrFallback = computed(() => (HEX6.test(props.modelValue) ? props.modelValue : '#6366f1'))

function onPicker(e) { emit('select', e.target.value) }
function onHex(e) {
  let v = e.target.value.trim()
  if (v && !v.startsWith('#')) v = '#' + v       // 手输省 # 也认
  emit('select', v)                               // 非法值交给后端 fail-loud，前端不静默吞
}

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
</script>

<style scoped>
.ccp {
  position: absolute; top: calc(100% + 6px); right: 0; z-index: 50;
  width: 224px; padding: 10px; background: #fff;
  border: 1px solid rgba(99,102,241,0.2); border-radius: 12px;
  box-shadow: 0 14px 44px rgba(13,16,38,0.16);
}
.ccp-clear {
  width: 100%; padding: 7px 10px; margin-bottom: 6px;
  display: flex; align-items: center; gap: 8px;
  border: 1px dashed rgba(99,102,241,0.3); border-radius: 8px;
  background: transparent; color: var(--t3, #4A5072); font-size: 12px; cursor: pointer;
  font-family: inherit; text-align: left;
}
.ccp-clear:hover { background: rgba(99,102,241,0.06); color: var(--v, #6366F1); }
.ccp-clear-dot {
  width: 12px; height: 12px; border-radius: 50%; flex: none;
  background: conic-gradient(#6366f1, #16a34a, #ea580c, #e11d48, #6366f1);
}
.ccp-cap {
  margin: 8px 2px 4px; font-size: 10.5px; font-weight: 700; letter-spacing: .1em;
  color: var(--t4, #8C92AB);
}
.ccp-grid { display: grid; grid-template-columns: repeat(8, 1fr); gap: 4px; }
.ccp-cell {
  height: 20px; border: 1px solid rgba(13,16,38,0.08); border-radius: 6px;
  cursor: pointer; padding: 0;
}
.ccp-cell:hover { transform: scale(1.12); }
.ccp-cell.on { box-shadow: 0 0 0 2px #fff, 0 0 0 3.5px var(--v, #6366F1); }
.ccp-row { display: flex; align-items: center; gap: 6px; }
.ccp-picker {
  width: 32px; height: 30px; flex: none; padding: 0; border: 1px solid rgba(13,16,38,0.14);
  border-radius: 7px; background: #fff; cursor: pointer;
}
.ccp-hex {
  flex: 1; min-width: 0; height: 30px; padding: 0 8px;
  border: 1px solid rgba(13,16,38,0.14); border-radius: 7px;
  font-family: var(--fm, monospace); font-size: 12px; color: var(--t, #0d1026);
}
.ccp-hex:focus { outline: none; border-color: var(--v, #6366F1); }
.ccp-hint { margin-top: 7px; font-size: 11px; color: var(--t4, #8C92AB); line-height: 1.4; }
</style>
