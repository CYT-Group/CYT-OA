<template>
  <div class="space-y-6">
    <OverviewView />
    <section class="space-y-5" aria-label="全公司数据">
      <h2 class="flex items-center gap-2 text-base font-semibold">
        <span aria-hidden="true">🛡️</span>全公司数据
      </h2>
      <div class="grid gap-4 sm:grid-cols-3">
        <router-link
          v-for="card in statCards"
          :key="card.key"
          :to="card.to"
          class="block rounded-xl focus-visible:outline-2 focus-visible:outline-primary"
        >
          <StatCard
            :title="card.label"
            :value="stats[card.key] ?? '…'"
            :hint="card.desc"
            :icon="card.icon"
            :glyph="card.glyph"
            :tone="card.tone"
            class="hover:border-primary/35"
          />
        </router-link>
      </div>
    </section>
    <div class="grid items-start gap-5 xl:grid-cols-2">
      <section class="panel overflow-hidden">
        <div class="panel-header">
          <h2 class="panel-title">常用应用</h2>
          <span class="text-[11px] text-muted-foreground"
            >企业日常，一步直达</span
          >
        </div>
        <div class="quick-apps">
          <router-link
            v-for="tile in tiles"
            :key="tile.key"
            :to="tile.to"
            class="quick-app group"
          >
            <span class="quick-app-icon"
              ><UiIcon :name="tile.icon" :size="20"
            /></span>
            <span class="min-w-0"
              ><b
                class="block text-[13px] font-medium group-hover:text-primary"
                >{{ tile.title }}</b
              ><small class="mt-1 block text-[11px] text-muted-foreground">{{
                tile.desc
              }}</small></span
            >
          </router-link>
        </div>
      </section>

      <section class="panel overflow-hidden">
        <div class="panel-header">
          <h2 class="panel-title">
            <Megaphone class="size-4 text-primary" />公司公告
          </h2>
          <Button
            variant="ghost"
            size="sm"
            @click="router.push('/page/oa-announcement')"
            >全部公告<ArrowRight class="size-3.5"
          /></Button>
        </div>
        <div v-if="announcementsLoading" class="space-y-4 p-5">
          <Skeleton v-for="i in 3" :key="i" class="h-10 w-full" />
        </div>
        <Callout
          v-else-if="announcementsError"
          class="m-5"
          tone="warning"
          title="公告暂未加载"
          ><span>{{ announcementsError }}</span
          ><template #action
            ><Button variant="ghost" size="sm" @click="loadAnnouncements"
              >重试</Button
            ></template
          ></Callout
        >
        <EmptyState
          v-else-if="!announcements.length"
          title="暂无公司公告"
          description="已发布的公司通知会展示在这里"
          :icon="Megaphone"
          compact
        />
        <button
          v-for="row in announcements"
          :key="row.id"
          type="button"
          class="list-row"
          @click="selectedAnnouncement = row"
        >
          <span
            class="grid size-9 shrink-0 place-items-center rounded-lg bg-primary/8 text-primary"
            ><FileText class="size-4"
          /></span>
          <span class="min-w-0 flex-1"
            ><b class="block truncate text-[13px] font-medium">{{
              row.title
            }}</b
            ><small class="mt-1 block text-[11px] text-muted-foreground"
              >{{ row.senderName || "公司公告" }} ·
              {{
                (row.publishTime || row.createTime || "").slice(0, 10)
              }}</small
            ></span
          >
          <ChevronRight class="size-4 text-muted-foreground" />
        </button>
      </section>
    </div>

    <Dialog
      :open="selectedAnnouncement !== null"
      :title="selectedAnnouncement?.title || '公告详情'"
      :description="selectedAnnouncement?.senderName"
      @update:open="!$event && (selectedAnnouncement = null)"
    >
      <p class="whitespace-pre-wrap text-sm leading-7 text-muted-foreground">
        {{
          selectedAnnouncement?.content || "此公告的正文请前往公告管理查看。"
        }}
      </p>
      <template #footer
        ><Button variant="outline" @click="selectedAnnouncement = null"
          >关闭</Button
        ><Button @click="router.push('/page/oa-announcement')"
          >公告管理</Button
        ></template
      >
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  ArrowRight,
  ChevronRight,
  FileText,
  Megaphone,
  Package,
  Users,
} from "@lucide/vue";
import UiIcon from "../components/UiIcon.vue";
import OverviewView from "./me/views/OverviewView.vue";
import Button from "../ui/Button.vue";
import StatCard from "../ui/StatCard.vue";
import EmptyState from "../ui/EmptyState.vue";
import Skeleton from "../ui/Skeleton.vue";
import Callout from "../ui/Callout.vue";
import Dialog from "../ui/Dialog.vue";
import { api } from "../lib/api";
import { errText } from "../lib/toast";

const router = useRouter();
const stats = ref<Record<string, string>>({});
const statCards = [
  {
    key: "emp",
    label: "在册员工",
    desc: "人事档案 · 查看员工",
    icon: Users,
    glyph: "👥",
    tone: "primary" as const,
    to: "/page/hrm-employee",
  },
  {
    key: "ann",
    label: "已发布公告",
    desc: "信息传达 · 公司通知",
    icon: Megaphone,
    glyph: "📰",
    tone: "success" as const,
    to: "/page/oa-announcement",
  },
  {
    key: "supply",
    label: "办公用品",
    desc: "物资管理 · 在库条目",
    icon: Package,
    glyph: "📦",
    tone: "info" as const,
    to: "/page/oa-supply-item",
  },
];
const tiles = [
  {
    key: "employee",
    icon: "user",
    title: "员工档案",
    desc: "人员信息与任职",
    to: "/page/hrm-employee",
  },
  {
    key: "org",
    icon: "users",
    title: "组织架构",
    desc: "部门与团队成员",
    to: "/org",
  },
  {
    key: "supply",
    icon: "box",
    title: "办公用品",
    desc: "物资台账与入库",
    to: "/page/oa-supply-item",
  },
  {
    key: "issue",
    icon: "truck",
    title: "领用发放",
    desc: "领用与归还登记",
    to: "/page/oa-supply-issue",
  },
  {
    key: "dict",
    icon: "book",
    title: "数据字典",
    desc: "统一基础数据",
    to: "/page/sys-dict",
  },
];
interface Announcement {
  id: string;
  title: string;
  senderName?: string;
  publishTime?: string;
  createTime?: string;
  content?: string;
}
const announcements = ref<Announcement[]>([]);
const selectedAnnouncement = ref<Announcement | null>(null);
const announcementsLoading = ref(true);
const announcementsError = ref("");
async function loadAnnouncements() {
  announcementsLoading.value = true;
  announcementsError.value = "";
  try {
    const result = await api.get<{ list: Announcement[]; total: number }>(
      "/admin-api/oa/announcement/published-page",
      { pageNo: 1, pageSize: 4 },
    );
    announcements.value = result.list ?? [];
    stats.value.ann = String(result.total ?? 0);
  } catch (e) {
    announcementsError.value = errText(e);
    stats.value.ann = "–";
  } finally {
    announcementsLoading.value = false;
  }
}
onMounted(() => {
  void loadAnnouncements();
  const fetchers = {
    emp: "/admin-api/hrm/employee/page",
    supply: "/admin-api/oa/supply-item/page",
  };
  void Promise.allSettled(
    Object.entries(fetchers).map(async ([key, url]) => {
      try {
        const data = await api.get<{ total: number }>(url, {
          pageNo: 1,
          pageSize: 1,
        });
        stats.value[key] = String(data?.total ?? 0);
      } catch {
        stats.value[key] = "–";
      }
    }),
  );
});
</script>
