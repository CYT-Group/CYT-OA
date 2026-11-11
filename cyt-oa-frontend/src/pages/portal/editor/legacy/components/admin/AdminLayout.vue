<template>
  <div class="admin-wrap" :style="{ maxWidth: maxWidthCss }">
    <div class="admin-header">
      <div class="admin-header-main">
        <div class="admin-tag">{{ tag }}</div>
        <h1 class="admin-title">
          <slot name="title">{{ title }}</slot>
        </h1>
        <p class="admin-sub" v-if="subtitle">{{ subtitle }}</p>
      </div>
      <!-- v1.24 排版任务入口：所有后台页都能看到任务进度（原来只在编辑器工具栏里）。
           hideJobs：与 HR 文档排版无关的页面（如生日贺礼）传 true 隐藏，避免无关入口干扰。 -->
      <HrLayoutJobsLauncher v-if="!hideJobs" class="admin-header-actions" />
    </div>
    <slot></slot>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import HrLayoutJobsLauncher from '../ui/HrLayoutJobsLauncher.vue'

// title 默认走文本插值。需要带 <strong> 等格式时用 #title slot：
//   <AdminLayout tag="..."><template #title>发布<strong>公司动态</strong></template></AdminLayout>
//
// 默认 max-width=900px；分栏编辑器/宽数据表场景传 :max-width="1280" 或 "100%" 等。
const props = defineProps({
  tag: { type: String, default: 'Admin' },
  title: { type: String, default: '' },
  subtitle: String,
  maxWidth: { type: [Number, String], default: 900 },
  /** 隐藏 header 右侧的「排版任务」入口（与 HR 文档排版无关的页面传 true） */
  hideJobs: { type: Boolean, default: false },
})

const maxWidthCss = computed(() =>
  typeof props.maxWidth === 'number' ? `${props.maxWidth}px` : props.maxWidth
)
</script>

<style scoped>
.admin-wrap {
  margin: 0 auto;
  padding: calc(var(--nav-h) + 28px) 32px 80px;
  position: relative; z-index: 1;
}
/* header 右侧放全局操作（排版任务入口） */
.admin-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}
.admin-header-main { flex: 1; min-width: 0; }
.admin-header-actions { flex: 0 0 auto; margin-top: 4px; }

.admin-tag {
  font-family: var(--fm); font-size: 11px; letter-spacing: .14em;
  text-transform: uppercase; color: var(--v); margin-bottom: 8px;
  display: flex; align-items: center; gap: 8px;
}
.admin-tag::before { content: '——'; color: var(--t4); }
.admin-title {
  font-size: 24px; font-weight: 300; margin-bottom: 6px;
}
.admin-title :deep(strong) {
  font-weight: 700; background: var(--grad);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
}
.admin-sub { font-size: 13px; color: var(--t3); margin-bottom: 28px; }
@media (max-width: 768px) {
  .admin-wrap { padding: calc(var(--nav-h) + 16px) 16px 60px; }
  .admin-title { font-size: 20px; }
}
</style>
