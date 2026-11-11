<!--
  StatusBadge — 状态徽标
  与 docs/DATA_CONTRACT.md 第 3 节"状态机总览"对应：draft / pending / published / rejected

  用法：
    <StatusBadge status="pending" />
    <StatusBadge status="published" size="sm" />
-->
<template>
  <span class="status-badge" :class="[`s-${status}`, `size-${size}`]">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true },
  size: { type: String, default: 'md' },  // sm | md
  customLabel: { type: String, default: '' },
})

const LABELS = {
  draft: '草稿',
  pending: '待审批',
  published: '已发布',
  rejected: '已驳回',
  // v1.20：下架不是 status 列取值，是 offline_at 非空 → 由列表页传 status="offline" 渲染
  offline: '已下架',
  active: '已激活',
  inactive: '已禁用',
  started: '进行中',
  completed: '已完成',
}

const label = computed(() => props.customLabel || LABELS[props.status] || props.status)
</script>

<style scoped>
.status-badge {
  display: inline-flex; align-items: center; gap: 4px;
  font-family: var(--fm, 'JetBrains Mono', monospace);
  font-weight: 600; letter-spacing: 0.02em;
  border-radius: 999px; border: 1px solid transparent;
  white-space: nowrap;
}
.size-md { padding: 3px 10px; font-size: 11px; }
.size-sm { padding: 2px 8px; font-size: 10px; }

.s-draft     { background: rgba(99,102,241,0.10); color: #4338CA; border-color: rgba(99,102,241,0.20); }
.s-pending   { background: rgba(245,158,11,0.12); color: #92400E; border-color: rgba(245,158,11,0.30); }
.s-published { background: rgba(34,197,94,0.10);  color: #166534; border-color: rgba(34,197,94,0.25); }
.s-rejected  { background: rgba(239,68,68,0.10);  color: #991B1B; border-color: rgba(239,68,68,0.25); }
.s-offline   { background: rgba(140,146,171,0.14); color: #4A5278; border-color: rgba(140,146,171,0.32); }
.s-active    { background: rgba(34,197,94,0.10);  color: #166534; border-color: rgba(34,197,94,0.25); }
.s-inactive  { background: rgba(140,146,171,0.12); color: #4A5278; border-color: rgba(140,146,171,0.30); }
.s-started   { background: rgba(59,130,246,0.10); color: #1E40AF; border-color: rgba(59,130,246,0.25); }
.s-completed { background: rgba(34,197,94,0.10);  color: #166534; border-color: rgba(34,197,94,0.25); }
</style>
