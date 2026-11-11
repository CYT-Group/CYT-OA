<!--
EmojiPickerPopover — 精选 emoji 网格弹层（HR 块编辑 cards/keypoints 的 icon 快捷填入）。

只做快捷选择，不是唯一入口：icon 输入框仍可自由输入任意文字/emoji。
精选 88 个常用项而不引第三方全量选择器——HR 文档场景够用，还省一个依赖。

约定：
- 点选 emit('select', emoji)；「清除」emit('select', '')；点外 / Esc emit('close')
- 开关状态由父级持有（v-if 挂载）；本组件 absolute 挂在触发处下方，
  父级容器需 position: relative
- 触发按钮自己 @click.stop，否则打开的同一次点击会冒泡到 document 立刻关闭
-->
<template>
  <div ref="rootEl" class="epp" @click.stop>
    <button type="button" class="epp-clear" @click="emit('select', '')">✕ 清除图标</button>
    <template v-for="g in GROUPS" :key="g.label">
      <div class="epp-cap">{{ g.label }}</div>
      <div class="epp-grid">
        <button v-for="e in g.list" :key="e" type="button" class="epp-cell" :title="e" @click="emit('select', e)">{{ e }}</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits(['select', 'close'])

// 分组小标题帮助扫视定位；挑选标准 = HR 文档（制度/流程/福利/公告）里用得上
const GROUPS = [
  {
    label: '工作',
    list: ['📝', '📋', '📌', '📎', '🗂️', '📊', '📈', '📉', '📅', '⏰', '⏳', '💼', '💻', '🖨️', '📞', '📧', '🔍', '🛠️', '⚙️', '🔑', '🔒', '📦', '🧾', '📚'],
  },
  {
    label: '状态',
    list: ['✅', '❌', '⚠️', '⛔', '🚫', '❗', '❓', '💡', '🔔', '🚀', '🎯', '🚩', '⭐', '🏆', '🎉', '👍', '👀', '💪', '🤝', '❤️', '🔥', '⚡', '✨', '♻️'],
  },
  {
    label: '物件',
    list: ['🏢', '🏠', '🏥', '🏦', '✈️', '🚗', '🌱', '🌈', '☀️', '🌙', '☕', '🎁', '🎓', '📖', '💰', '💳', '🧭', '📍', '🧰', '🩺', '👥', '👤', '🗣️', '💬'],
  },
  {
    label: '符号',
    list: ['🔴', '🟠', '🟡', '🟢', '🔵', '🟣', '⚪', '⚫', '🔺', '🔻', '▶️', '⏸️', '➕', '➖', '🔷', '🔶'],
  },
]

const rootEl = ref(null)

function onDocClick(e) {
  if (rootEl.value && !rootEl.value.contains(e.target)) emit('close')
}
function onDocKeydown(e) {
  if (e.key === 'Escape') emit('close')
}
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
.epp {
  position: absolute; top: calc(100% + 6px); left: 0; z-index: 50;
  width: min(324px, calc(100vw - 48px));
  max-height: 300px; overflow-y: auto;
  padding: 10px; background: #fff;
  border: 1px solid rgba(99,102,241,0.2); border-radius: 12px;
  box-shadow: 0 14px 44px rgba(13,16,38,0.16);
}
.epp-clear {
  width: 100%; padding: 6px 10px; margin-bottom: 4px;
  border: 1px dashed rgba(99,102,241,0.3); border-radius: 8px;
  background: transparent; color: var(--t3, #4A5072); font-size: 12px; cursor: pointer;
  font-family: inherit; text-align: left;
}
.epp-clear:hover { background: rgba(99,102,241,0.06); color: var(--v, #6366F1); }
.epp-cap {
  margin: 8px 2px 4px; font-size: 10.5px; font-weight: 700; letter-spacing: .1em;
  color: var(--t4, #8C92AB);
}
.epp-grid { display: grid; grid-template-columns: repeat(8, 1fr); gap: 2px; }
.epp-cell {
  height: 32px; border: none; border-radius: 7px; background: transparent;
  font-size: 17px; line-height: 1; cursor: pointer; padding: 0;
  display: flex; align-items: center; justify-content: center;
}
.epp-cell:hover { background: rgba(99,102,241,0.1); }
</style>
