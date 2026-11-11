<template>
  <section class="panel mx-auto max-w-xl p-8 text-center">
    <component
      :is="failed ? CircleAlert : Clock3"
      class="mx-auto mb-4 size-10 text-muted-foreground"
    />
    <h1 class="text-lg font-semibold">
      {{ failed ? "暂时无法获取入口状态" : "该功能尚未开放" }}
    </h1>
    <p class="mt-3 text-sm leading-relaxed text-muted-foreground">
      {{
        failed
          ? "入口上线状态未能加载，请重试或返回工作空间。"
          : `${entry?.title || "该功能"}正在分批上线，请等待管理员开放入口。`
      }}
    </p>
    <p v-if="error" class="mt-3 text-xs text-destructive">{{ error }}</p>
    <div class="mt-6 flex flex-wrap justify-center gap-2">
      <Button v-if="failed" :loading="loading" @click="retry">重新加载</Button
      ><Button variant="outline" @click="router.push('/portal')"
        >返回管理中心</Button
      ><Button
        v-if="session.isAdmin"
        variant="ghost"
        @click="router.push('/portal/rollout')"
        >管理上线设置</Button
      >
    </div>
  </section>
</template>
<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Clock3, CircleAlert } from "@lucide/vue";
import {
  portalEntryForRoute,
  safePortalEntryTarget,
} from "../../lib/portal-entry-policy";
import { loadPortalRollout } from "../../lib/portal-rollout";
import { session } from "../../lib/session";
import { errText } from "../../lib/toast";
import Button from "../../ui/Button.vue";
const route = useRoute(),
  router = useRouter(),
  loading = ref(false),
  error = ref("");
const failed = computed(() => route.query.reason === "load");
const target = computed(() => safePortalEntryTarget(route.query.from));
const entry = computed(() => {
  const url = new URL(target.value, "http://oa.local");
  return portalEntryForRoute(
    url.pathname,
    Object.fromEntries(url.searchParams),
  );
});
async function retry() {
  loading.value = true;
  error.value = "";
  try {
    await loadPortalRollout(true);
    await router.replace(target.value);
  } catch (failure) {
    error.value = errText(failure);
  } finally {
    loading.value = false;
  }
}
</script>
