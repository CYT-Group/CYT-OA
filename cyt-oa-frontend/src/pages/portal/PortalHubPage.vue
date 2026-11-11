<template>
  <div class="space-y-6">
    <PageHeader title="门户管理中心" description="维护门户内容、资源和 HR 事务">
      <template #actions
        ><Button
          v-if="session.isAdmin"
          variant="outline"
          @click="router.push('/portal/rollout')"
          >入口上线管理</Button
        ><Button variant="outline" @click="router.push('/me/overview')"
          >我的工作台</Button
        ></template
      >
    </PageHeader>
    <Callout
      v-if="portalRollout.error"
      title="入口状态未加载"
      tone="destructive"
      >{{ portalRollout.error
      }}<template #action
        ><Button variant="outline" size="sm" @click="retryRollout"
          >重新加载</Button
        ></template
      ></Callout
    >
    <EmptyState
      v-if="!groups.length && !portalRollout.error"
      title="暂未开放管理功能"
      description="功能将按批次开放，开放后会显示在这里。"
    />
    <section v-for="group in groups" :key="group.group" class="space-y-3">
      <h2 class="text-sm font-semibold">{{ group.group }}</h2>
      <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <router-link
          v-for="item in group.items"
          :key="item.key"
          :to="item.to ?? `/page/${item.key}`"
          class="panel flex items-center gap-4 p-5 transition-colors hover:border-primary/40 focus-visible:outline-2 focus-visible:outline-primary"
        >
          <span class="rounded-lg bg-primary/10 p-2.5 text-primary"
            ><UiIcon :name="item.icon" :size="20"
          /></span>
          <span class="min-w-0 flex-1"
            ><b class="block text-sm font-medium">{{ item.title }}</b
            ><small class="mt-1 block text-xs text-muted-foreground">{{
              descriptions[item.key] ?? "查看与维护"
            }}</small></span
          ><ArrowRight class="size-4 text-muted-foreground" />
        </router-link>
      </div>
    </section>
  </div>
</template>
<script setup lang="ts">
import { computed } from "vue";
import { useRouter } from "vue-router";
import { ArrowRight } from "@lucide/vue";
import { portalMenus } from "../../configs/portal";
import { allowed } from "../../lib/portal-access";
import UiIcon from "../../components/UiIcon.vue";
import PageHeader from "../../ui/PageHeader.vue";
import Button from "../../ui/Button.vue";
import Callout from "../../ui/Callout.vue";
import EmptyState from "../../ui/EmptyState.vue";
import { session } from "../../lib/session";
import { loadPortalRollout, portalRollout } from "../../lib/portal-rollout";
async function retryRollout() {
  try {
    await loadPortalRollout(true);
  } catch {
    /* 继续显示读取失败并保留恢复入口。 */
  }
}
const router = useRouter();
const groups = computed(() =>
  portalMenus
    .map((g) => ({
      ...g,
      items: g.items.filter((i) => i.key !== "portal-hub" && allowed(i)),
    }))
    .filter((g) => g.items.length),
);
const descriptions: Record<string, string> = {
  "portal-news": "发布、审批公司动态",
  "portal-wiki": "维护知识文档与外链",
  "portal-roles": "角色与能力授权",
  "portal-rollout": "按上线批次开放管理入口",
  "portal-ai": "模型、密钥、提示词和版本",
  "portal-brand-docs": "上传手册、预览和版本回滚",
  "portal-samples": "规格库存、借用审批和归还",
  "portal-supplies": "商品库存、领用发放和负责人",
  "portal-training": "课程、课时、导入和学习统计",
  "portal-hr-documents": "文档编辑、发布与历史版本",
  "portal-signoff": "制度签收覆盖率与催签",
  "portal-contracts": "填写信息并生成 Word 合同",
  "portal-celebrations": "生日、入职周年和祝福设置",
  "portal-employee-info": "维护员工生日、入职日和庆祝偏好",
  "portal-resumes": "上传原件、AI 识别与人工校对",
  "portal-import": "文件校验与批量导入",
  "portal-analytics": "浏览趋势与热门页面",
  "portal-api-costs": "调用量与估算费用",
};
</script>
