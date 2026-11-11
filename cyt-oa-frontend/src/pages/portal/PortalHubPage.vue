<template>
  <div class="min-w-0 space-y-5">
    <PageHeader
      :title="toolsView ? '管理工具' : '管理中心'"
      :description="
        toolsView
          ? '查找内容维护、数据分析和系统配置工具。'
          : '按业务进入管理模块，辅助功能集中在管理工具中。'
      "
    >
      <template #actions>
        <Button
          v-if="toolsView"
          variant="outline"
          @click="router.push('/portal')"
        >
          返回管理中心
        </Button>
        <Button
          v-else-if="hasTools"
          variant="outline"
          @click="router.push('/portal?view=tools')"
        >
          管理工具
        </Button>
        <Button
          v-if="session.isAdmin && toolsView"
          variant="outline"
          @click="router.push('/portal/rollout')"
        >
          入口上线管理
        </Button>
      </template>
    </PageHeader>
    <Callout
      v-if="portalRollout.error"
      title="入口状态未加载"
      tone="destructive"
    >
      {{ portalRollout.error }}
      <template #action>
        <Button variant="outline" size="sm" @click="retryRollout">
          重新加载
        </Button>
      </template>
    </Callout>
    <div v-if="toolsView" class="flex flex-wrap items-center gap-3">
      <Input
        v-model="query"
        class="sm:max-w-sm"
        aria-label="搜索管理工具"
        placeholder="搜索工具名称或用途"
      />
      <p class="text-xs text-muted-foreground">
        也可通过顶部搜索查找已授权功能
      </p>
    </div>
    <EmptyState
      v-if="!groups.length && !portalRollout.error"
      :title="query ? '没有匹配的管理工具' : '暂无可用管理入口'"
      :description="
        query
          ? '试试其他名称或清空搜索条件。'
          : '入口按当前账号权限和上线状态展示。'
      "
    />
    <section v-for="group in groups" :key="group.title" class="space-y-3">
      <h2 class="text-sm font-semibold">{{ group.title }}</h2>
      <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <RouterLink
          v-for="item in group.items"
          :key="item.key"
          :to="item.to"
          class="panel flex items-center gap-4 p-5 transition-colors hover:border-primary/40 focus-visible:outline-2 focus-visible:outline-primary"
        >
          <span class="rounded-lg bg-primary/10 p-2.5 text-primary">
            <NavIcon :name="item.icon" :glyph="item.glyph ?? ''" />
          </span>
          <span class="min-w-0 flex-1">
            <b class="block text-sm font-medium">{{ item.title }}</b>
            <small class="mt-1 block text-xs leading-5 text-muted-foreground">
              {{ descriptions[item.key || ''] || '查看与维护' }}
            </small>
          </span>
          <ArrowRight class="size-4 shrink-0 text-muted-foreground" />
        </RouterLink>
      </div>
    </section>
  </div>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowRight } from '@lucide/vue';
import { navZones, type NavSub } from '../../configs/nav';
import { filterPortalNavigation } from '../../lib/portal-access';
import { sidebarNavigation } from '../../lib/navigation';
import { session } from '../../lib/session';
import { loadPortalRollout, portalRollout } from '../../lib/portal-rollout';
import NavIcon from '../../components/NavIcon.vue';
import PageHeader from '../../ui/PageHeader.vue';
import Button from '../../ui/Button.vue';
import Callout from '../../ui/Callout.vue';
import EmptyState from '../../ui/EmptyState.vue';
import Input from '../../ui/Input.vue';
const router = useRouter(),
  route = useRoute(),
  query = ref('');
const toolsView = computed(() => route.query.view === 'tools');
const catalog = computed(() =>
  filterPortalNavigation(navZones).filter((zone) => zone.adminOnly)
);
const compact = computed(() =>
  sidebarNavigation(catalog.value).flatMap((zone) => zone.items)
);
const tools = computed(
  () =>
    catalog.value
      .flatMap((zone) => zone.items)
      .find((item) => item.key === 'management-tools')
      ?.children?.filter((item) => item.key !== 'portal-hub') ?? []
);
const hasTools = computed(() => tools.value.length > 0);
const groups = computed(() => {
  if (!toolsView.value)
    return [
      {
        title: '业务管理',
        items: compact.value.filter((item) => item.key !== 'management-tools')
      },
      ...(hasTools.value
        ? [
            {
              title: '更多工具',
              items: compact.value.filter(
                (item) => item.key === 'management-tools'
              )
            }
          ]
        : [])
    ].filter((group) => group.items.length);
  const buckets = new Map<string, NavSub[]>();
  const term = query.value.trim().toLowerCase();
  for (const item of tools.value) {
    const section = item.section || '系统设置';
    if (
      term &&
      !`${item.title}${section}${descriptions[item.key || ''] || ''}`
        .toLowerCase()
        .includes(term)
    )
      continue;
    if (!buckets.has(section)) buckets.set(section, []);
    buckets.get(section)!.push(item);
  }
  return [...buckets].map(([title, items]) => ({ title, items }));
});
watch(toolsView, () => {
  query.value = '';
});
async function retryRollout() {
  try {
    await loadPortalRollout(true);
  } catch {
    /* 保留恢复入口和读取错误。 */
  }
}
const descriptions: Record<string, string> = {
  permissions: '账号分配与角色授权',
  'approval-templates': '审批流程模板',
  'attendance-rules': '考勤规则、打卡与节假日',
  'office-materials': '用品目录、库存与领用记录',
  'sample-materials': '样品目录、借还与盘点',
  'company-news': '发布和管理公司动态',
  documents: '制度编辑、发布与签收统计',
  'business-rhythm': '活动排期和新品维护',
  celebrations: '生日、周年与员工祝贺偏好',
  'voice-management': '建议、CEO 来信、匿名反馈与满意度调研',
  'people-management': '招聘、花名册、入离职与员工历程',
  'management-tools': '内容与培训、数据与 AI、系统设置',
  'portal-wiki': '维护知识文档与外链',
  'portal-brand-docs': '手册上传、预览与版本',
  'portal-training': '课程、课时与学习统计',
  'portal-categories': '培训和物资的内容分类',
  'portal-ai': '模型、密钥和提示词',
  'portal-analytics': '浏览趋势与热门页面',
  'portal-api-costs': '调用量与估算费用',
  'portal-import': '文件校验与批量导入',
  'portal-rollout': '管理入口的分批上线',
  org: '组织层级与通讯录同步',
  'sys-dict': '系统字典维护',
  console: '企业工作台',
  'oa-announcement': '原有公告台账',
  'oa-supply-item': '原有办公用品台账',
  'oa-supply-issue': '原有领用发放台账'
};
</script>
