<template>
  <div class="space-y-6">
    <OverviewView />
    <section class="space-y-5" aria-label="全公司数据">
      <div>
        <h2 class="flex items-center gap-2 text-base font-semibold">
          <span aria-hidden="true">🛡️</span>全公司数据
        </h2>
        <p class="mt-2 text-xs leading-6 text-muted-foreground">
          审批与报销仅统计门户申请；企业微信审批请从「申请与审批」查看。
          本月报销按北京时间 {{ currentMonth }} 的申请创建月份汇总已通过金额。
        </p>
      </div>
      <div class="grid grid-cols-2 items-start gap-4 xl:grid-cols-4">
        <div
          v-for="card in statCards"
          :key="card.key"
          class="min-w-0 space-y-2"
        >
          <router-link :to="card.to" class="block rounded-xl focus-visible:outline-2 focus-visible:outline-primary">
          <StatCard
            :title="card.label"
            :value="statsLoading[card.key] ? '…' : (stats[card.key] ?? '—')"
            :hint="card.desc"
            :icon="card.icon"
            :glyph="card.glyph"
            :tone="card.tone"
            class="hover:border-primary/35"
            :class="card.key === 'expense' ? '[&>p:first-of-type]:break-all [&>p:first-of-type]:text-xl' : ''"
          />
          </router-link>
          <Callout v-if="statsErrors[card.key]" tone="destructive" :title="`${card.label}未读取`">
            {{ statsErrors[card.key] }}
            <template #action><Button variant="ghost" size="sm" :disabled="statsLoading[card.key]" @click="retryStat(card.key)">重试</Button></template>
          </Callout>
        </div>
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
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import {
  ArrowRight,
  ChevronRight,
  FileText,
  Megaphone,
  CircleCheck,
  Receipt,
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
import { api, ApiError } from "../lib/api";
import { errText } from "../lib/toast";
import { workspaceApi } from "../lib/workspace-api";
import { todayShanghai } from "../lib/hr-workspace";

const router = useRouter();
type StatKey = 'emp' | 'pending' | 'expense' | 'ann';
const currentMonth = todayShanghai().slice(0, 7);
const stats = reactive<Record<StatKey, string | null>>({ emp: null, pending: null, expense: null, ann: null });
const statsLoading = reactive<Record<StatKey, boolean>>({ emp: true, pending: true, expense: true, ann: true });
const statsErrors = reactive<Record<StatKey, string>>({ emp: '', pending: '', expense: '', ann: '' });
const statCards = [
  {
    key: "emp",
    label: "在职员工",
    desc: "当前在职人数，包含待离职仍在职员工",
    icon: Users,
    glyph: "👥",
    tone: "primary" as const,
    to: "/page/hrm-employee?roster=active",
  },
  {
    key: "pending",
    label: "门户审批中",
    desc: "全公司处于审批中的门户申请，不含企业微信审批",
    icon: CircleCheck,
    glyph: "📝",
    tone: "warning" as const,
    to: "/me/requests?view=company&status=pending",
  },
  {
    key: "expense",
    label: "本月已批门户报销",
    desc: `申请创建于北京时间 ${currentMonth} 且已通过的门户报销金额，不含企业微信报销`,
    icon: Receipt,
    glyph: "💰",
    tone: "info" as const,
    to: `/me/requests?view=company&type=expense&status=approved&month=${currentMonth}`,
  },
  {
    key: "ann",
    label: "已发布公告",
    desc: "已发布的公司通知总数",
    icon: Megaphone,
    glyph: "📰",
    tone: "success" as const,
    to: "/page/oa-announcement",
  },
] satisfies { key: StatKey; label: string; desc: string; icon: unknown; glyph: string; tone: 'primary' | 'warning' | 'info' | 'success'; to: string }[];
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
  statsLoading.ann = true;
  statsErrors.ann = '';
  try {
    const result = await api.get<{ list: Announcement[]; total: number }>(
      "/admin-api/oa/announcement/published-page",
      { pageNo: 1, pageSize: 4 },
    );
    announcements.value = result.list ?? [];
    if (result.total === null || result.total === undefined) throw new ApiError(-1, '公告统计未返回总数');
    stats.ann = String(result.total);
  } catch (e) {
    announcementsError.value = errText(e);
    stats.ann = null;
    statsErrors.ann = errText(e);
  } finally {
    announcementsLoading.value = false;
    statsLoading.ann = false;
  }
}
async function loadEmployees() {
  statsLoading.emp = true;
  statsErrors.emp = '';
  try {
    const data = await api.get<{ employeeSurvey: { activeCount: number } }>('/admin-api/hrm/home/hr-statistics-summary');
    const count = data.employeeSurvey?.activeCount;
    if (count === null || count === undefined) throw new ApiError(-1, '在职统计未返回人数');
    stats.emp = String(count);
  } catch (e) { stats.emp = null; statsErrors.emp = errText(e); }
  finally { statsLoading.emp = false; }
}
function requestMonth(value: string) {
  if (!value) throw new ApiError(-1, '报销申请缺少创建时间');
  // 无偏移的 LocalDateTime 来自业务库北京时间；带偏移时转换为北京时间再归月。
  if (!/(Z|[+-]\d{2}:?\d{2})$/i.test(value)) return value.slice(0, 7);
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) throw new ApiError(-1, '报销申请创建时间无效');
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit' }).format(date);
}
function moneyInCents(value: unknown) {
  const match = /^(\d+)(?:\.(\d+))?$/.exec(String(value));
  if (!match) throw new ApiError(-1, '报销金额格式无效');
  const fraction = (match[2] || '').padEnd(3, '0');
  return BigInt(match[1]) * 100n + BigInt(fraction.slice(0, 2)) + (fraction[2] >= '5' ? 1n : 0n);
}
function formatMoney(cents: bigint) {
  const whole = (cents / 100n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return `¥${whole}.${(cents % 100n).toString().padStart(2, '0')}`;
}
async function loadRequests() {
  statsLoading.pending = true; statsLoading.expense = true;
  statsErrors.pending = ''; statsErrors.expense = '';
  try {
    const requests = await workspaceApi.requests('company');
    stats.pending = String(requests.filter(row => row.status === 'pending').length);
    try {
      const total = requests.filter(row => row.data.type === 'expense' && row.status === 'approved' && requestMonth(row.createdAt) === currentMonth)
        .reduce((sum, row) => sum + moneyInCents(row.data.amount), 0n);
      stats.expense = formatMoney(total);
    } catch (e) { stats.expense = null; statsErrors.expense = errText(e); }
  } catch (e) {
    stats.pending = null; stats.expense = null;
    statsErrors.pending = errText(e); statsErrors.expense = errText(e);
  } finally { statsLoading.pending = false; statsLoading.expense = false; }
}
function retryStat(key: StatKey) {
  if (key === 'emp') void loadEmployees();
  else if (key === 'ann') void loadAnnouncements();
  else void loadRequests();
}
onMounted(() => {
  void loadAnnouncements();
  void loadEmployees();
  void loadRequests();
});
</script>
