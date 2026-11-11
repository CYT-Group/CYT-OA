<template>
  <div class="workspace-dashboard">
    <h1 class="text-xl font-bold">工作台</h1>
    <section class="welcome-banner">
      <template v-if="data?.currentRhythm">
        <div class="min-w-0">
          <h2 class="welcome-title">
            <span aria-hidden="true">🎉</span> {{ data.currentRhythm.name }}
          </h2>
          <p class="mt-2 text-[13px] text-muted-foreground">
            {{ bannerTag(data.currentRhythm) }} ·
            {{ brief(data.currentRhythm) || "关注业务关键节点" }}
          </p>
        </div>
        <Button variant="outline" @click="openNode(data.currentRhythm)"
          >查看详情 <ArrowRight
        /></Button>
      </template>
      <template v-else>
        <div class="relative z-1">
          <h2 class="welcome-title">{{ welcomeText }}</h2>
          <p class="mt-2 text-[13px] text-muted-foreground">
            {{
              data ? `${data.greeting.date} ${data.greeting.weekday} · ` : ""
            }}让今天的工作，从容有序。
          </p>
        </div>
        <div class="relative z-1 flex gap-2">
          <Button variant="outline" @click="router.push('/me/report')"
            ><FileText class="size-4" />写工作报告</Button
          ><Button @click="router.push('/me/apply')"
            ><ClipboardPen class="size-4" />我要办事</Button
          >
        </div>
      </template>
    </section>

    <Callout v-if="loadError" tone="destructive" title="工作台数据加载失败"
      >{{ loadError
      }}<template #action
        ><Button variant="outline" size="sm" @click="loadOverview"
          >重新加载</Button
        ></template
      ></Callout
    >
    <div class="grid grid-cols-2 gap-3.5 xl:grid-cols-4">
      <component
        :is="card.to.startsWith('#') ? 'a' : RouterLink"
        v-for="card in summaryCards"
        :key="card.title"
        :href="card.to.startsWith('#') ? card.to : undefined"
        :to="card.to.startsWith('#') ? undefined : card.to"
        class="block rounded-xl focus-visible:outline-2 focus-visible:outline-primary"
      >
        <StatCard
          :title="card.title"
          :value="card.value"
          :hint="card.hint"
          :icon="card.icon"
          :glyph="card.glyph"
          :tone="card.tone"
          class="hover:border-primary/35"
        />
      </component>
    </div>

    <TodayGreetings />

    <div class="dashboard-grid">
      <div class="dashboard-primary">
        <section id="my-todos" class="panel scroll-mt-24 overflow-hidden">
          <div class="panel-header">
            <h2 class="panel-title">
              <span aria-hidden="true">🔴</span>我的待办<Badge
                variant="secondary"
                >{{ todoTotal }}</Badge
              >
            </h2>
            <span class="text-[11px] text-muted-foreground"
              >聚焦需要你处理的事</span
            >
          </div>
          <div v-if="overviewLoading" class="space-y-3 p-5">
            <Skeleton v-for="i in 3" :key="i" class="h-12 w-full" />
          </div>
          <EmptyState
            v-else-if="!data"
            title="待办暂未加载"
            description="重新加载工作台以获取最新待办"
            :icon="ListChecks"
            compact
            ><Button variant="outline" size="sm" @click="loadOverview"
              >重新加载</Button
            ></EmptyState
          >
          <EmptyState
            v-else-if="!todoTotal"
            title="待办已清，专注向前"
            description="新的审批、报告与节点提醒会在这里汇总"
            :icon="CircleCheckBig"
            compact
          />
          <template v-if="data">
            <div
              v-for="group in data.todos.filter((g) => g.items.length)"
              :key="group.key"
            >
              <div
                class="flex items-center justify-between bg-muted/70 px-5 py-2 text-[11px] text-muted-foreground"
              >
                <span>{{ group.title }}</span
                ><span>{{ group.count }} 项</span>
              </div>
              <button
                v-for="(item, idx) in group.items"
                :key="group.key + idx"
                type="button"
                class="list-row"
                @click="router.push(item.link || group.link)"
              >
                <span
                  class="grid size-9 shrink-0 place-items-center rounded-lg bg-primary/8 text-primary"
                  ><UiIcon :name="group.key" :size="16"
                /></span>
                <span class="min-w-0 flex-1"
                  ><b class="block truncate text-[13px] font-medium">{{
                    item.title
                  }}</b
                  ><small
                    class="mt-1 block truncate text-xs text-muted-foreground"
                    >{{ item.desc }}</small
                  ></span
                >
                <span
                  class="hidden rounded-md border border-border px-3 py-1.5 text-xs text-primary sm:inline"
                  >{{ ACTION_TEXT[group.key] ?? "去处理" }}</span
                ><ChevronRight
                  class="size-3.5 text-muted-foreground sm:hidden"
                />
              </button>
            </div>
          </template>
        </section>

        <section
          v-if="data?.currentRhythm || strip.length"
          class="panel overflow-hidden"
        >
          <div class="panel-header">
            <h2 class="panel-title">
              <CalendarDays class="size-4 text-primary" />业务节奏
            </h2>
            <Button variant="ghost" size="sm" @click="router.push('/me/rhythm')"
              >全部节奏<ArrowRight class="size-3.5"
            /></Button>
          </div>
          <button
            v-if="data?.currentRhythm"
            type="button"
            class="mx-5 mt-5 flex w-[calc(100%-40px)] items-center gap-3 rounded-lg bg-primary/6 p-4 text-left"
            @click="openNode(data.currentRhythm)"
          >
            <Megaphone class="size-5 shrink-0 text-primary" /><span
              class="min-w-0 flex-1"
              ><small class="text-[11px] text-primary"
                >当前节奏 · {{ bannerTag(data.currentRhythm) }}</small
              ><b class="mt-1 block truncate text-sm font-medium">{{
                data.currentRhythm.name
              }}</b></span
            ><ChevronRight class="size-4 text-primary" />
          </button>
          <div class="divide-y divide-border p-5">
            <button
              v-for="node in strip"
              :key="node.id"
              type="button"
              class="flex w-full items-center gap-4 py-3 text-left first:pt-0 last:pb-0"
              @click="openNode(node)"
            >
              <span
                class="h-2 w-2 shrink-0 rounded-full"
                :class="
                  node.state === 'ongoing' ? 'bg-primary' : 'bg-border-strong'
                "
              /><span class="min-w-0 flex-1"
                ><b class="block text-[13px] font-medium">{{ node.name }}</b
                ><small class="mt-1 block text-[11px] text-muted-foreground">{{
                  brief(node) || node.kindText
                }}</small></span
              ><span
                class="shrink-0 text-[11px] tabular-nums text-muted-foreground"
                >{{ node.startDate.slice(5) }}</span
              >
            </button>
          </div>
        </section>

        <section v-if="data" class="panel overflow-hidden">
          <div class="panel-header">
            <h2 class="panel-title">
              <FileText class="size-4 text-primary" />最近的报告
            </h2>
            <Button variant="ghost" size="sm" @click="router.push('/me/report')"
              >全部报告<ArrowRight class="size-3.5"
            /></Button>
          </div>
          <EmptyState
            v-if="!data.recentReports.length"
            title="记录你的工作进展"
            description="提交日报或周报后，最近的记录会出现在这里"
            :icon="FileText"
            compact
            ><Button
              variant="outline"
              size="sm"
              @click="router.push('/me/report')"
              >写一份报告</Button
            ></EmptyState
          >
          <button
            v-for="row in data.recentReports"
            :key="row.id"
            type="button"
            class="list-row"
            @click="router.push('/me/report')"
          >
            <span
              class="grid size-9 shrink-0 place-items-center rounded-lg bg-muted text-muted-foreground"
              ><FileText class="size-4" /></span
            ><span class="min-w-0 flex-1"
              ><b class="block truncate text-[13px] font-medium">{{
                row.title || row.periodKey
              }}</b
              ><small class="mt-1 block text-[11px] text-muted-foreground">{{
                row.periodKey
              }}</small></span
            ><Badge :variant="toneBadge(reportTone(row.status))">{{
              row.statusText
            }}</Badge>
          </button>
        </section>
      </div>

      <div class="dashboard-secondary">
        <section class="panel overflow-hidden">
          <div class="panel-header">
            <h2 class="panel-title">
              <span aria-hidden="true">⚡</span>快捷入口
            </h2>
          </div>
          <div class="grid grid-cols-3 gap-1 p-3">
            <router-link
              v-for="app in quickApps"
              :key="app.to"
              :to="app.to"
              class="group flex flex-col items-center rounded-lg px-2 py-4 text-center transition-colors hover:bg-muted"
            >
              <span class="grid size-9 place-items-center"
                ><NavIcon :name="app.icon" class="quick-app-glyph" /></span
              ><b
                class="mt-3 text-[13px] font-medium group-hover:text-primary"
                >{{ app.title }}</b
              ><small class="mt-1 text-[10px] text-muted-foreground">{{
                app.desc
              }}</small>
            </router-link>
          </div>
        </section>

        <section class="panel overflow-hidden">
          <div class="panel-header">
            <h2 class="panel-title">
              <Clock3 class="size-4 text-primary" />今日考勤
            </h2>
            <span class="text-[11px] text-muted-foreground">日常打卡</span>
          </div>
          <div class="p-5">
            <p class="text-[13px] font-medium">
              {{
                clocks.length
                  ? "工作已开启，保持好状态"
                  : "准备好了，开启今天的工作"
              }}
            </p>
            <p class="mt-2 text-xs leading-6 text-muted-foreground">
              {{ clockText }}
            </p>
            <Button
              v-if="clockError"
              class="mt-4 w-full"
              variant="outline"
              @click="loadClocks"
              >刷新打卡记录</Button
            ><Button
              v-else
              class="mt-4 w-full"
              :loading="clockBusy"
              @click="clock"
              >{{ clockBusy ? "打卡中…" : "考勤打卡" }}</Button
            >
          </div>
        </section>
        <WorkCalendar
          ><router-link
            to="/me/rhythm"
            class="flex items-center justify-between text-xs text-muted-foreground hover:text-primary"
            ><span>查看业务节奏与关键日期</span
            ><ArrowRight class="size-3.5" /></router-link
        ></WorkCalendar>
        <Callout
          v-if="nextMilestone"
          tone="warning"
          :title="milestoneText(nextMilestone)"
          >{{ nextMilestone.title
          }}<template #action
            ><Button
              variant="ghost"
              size="sm"
              @click="router.push('/me/reminder')"
              >查看</Button
            ></template
          ></Callout
        >
        <Callout v-if="inFlight.length" tone="info" :title="mineTitle"
          >{{ mineSub
          }}<template #action
            ><Button variant="ghost" size="sm" @click="router.push('/me/apply')"
              >查看</Button
            ></template
          ></Callout
        >
        <section v-if="data" class="panel overflow-hidden">
          <div class="panel-header">
            <h2 class="panel-title">
              <Bell class="size-4 text-primary" />最新提醒
            </h2>
            <Button
              variant="ghost"
              size="sm"
              @click="router.push('/me/reminder')"
              >全部<ArrowRight class="size-3.5"
            /></Button>
          </div>
          <EmptyState
            v-if="!data.latestReminders.length"
            title="暂无新提醒"
            description="重要消息会及时出现在这里"
            :icon="Bell"
            compact
          />
          <button
            v-for="row in data.latestReminders"
            :key="row.id"
            type="button"
            class="list-row !items-start"
            @click="openReminder(row)"
          >
            <span
              class="mt-1.5 size-1.5 shrink-0 rounded-full"
              :class="row.read ? 'bg-border-strong' : 'bg-primary'"
            /><span class="min-w-0 flex-1"
              ><b
                class="block text-xs leading-6"
                :class="
                  row.read ? 'font-normal text-muted-foreground' : 'font-medium'
                "
                >{{ row.title }}</b
              ><small class="mt-1 block text-[10px] text-muted-foreground"
                >{{ CAT_TEXT[row.category] ?? row.category }} ·
                {{ relTime(row.createdAt) }}</small
              ></span
            >
          </button>
        </section>
      </div>
    </div>
    <Dialog
      :open="picked !== null"
      :title="picked?.name ?? '节点详情'"
      :description="picked ? range(picked) : ''"
      @update:open="!$event && (picked = null)"
    >
      <div v-if="picked" class="space-y-5">
        <dl class="kv-list">
          <div class="kv-item">
            <dt>类型</dt>
            <dd>{{ picked.kindText }}</dd>
          </div>
          <div class="kv-item">
            <dt>日期</dt>
            <dd>{{ range(picked) }}</dd>
          </div>
          <div class="kv-item">
            <dt>状态</dt>
            <dd>{{ bannerTag(picked) }}</dd>
          </div>
          <div class="kv-item">
            <dt>关注</dt>
            <dd>{{ picked.followed ? "已关注" : "未关注" }}</dd>
          </div>
        </dl>

        <div>
          <p class="section-title">作战说明</p>
          <p class="text-sm leading-relaxed text-muted-foreground">
            {{ picked.note || "暂无" }}
          </p>
        </div>

        <div v-if="picked.deptActions.length">
          <p class="section-title">各部门安排</p>
          <div class="space-y-1.5">
            <div
              v-for="act in picked.deptActions"
              :key="act.dept"
              class="flex gap-3 rounded-lg bg-muted/60 px-3 py-2 text-sm"
            >
              <span class="shrink-0 font-medium">{{ act.dept }}</span>
              <span class="text-muted-foreground">{{
                act.items.join("、")
              }}</span>
            </div>
          </div>
        </div>

        <div v-if="picked.linkedRefs.length">
          <p class="section-title">关联资料</p>
          <div class="flex flex-wrap gap-1.5">
            <Badge
              v-for="ref in picked.linkedRefs"
              :key="ref"
              variant="outline"
              >{{ ref }}</Badge
            >
          </div>
        </div>
      </div>
      <template #footer>
        <Button variant="outline" @click="picked = null">关闭</Button>
        <Button
          v-if="picked"
          :variant="picked.followed ? 'outline' : 'default'"
          @click="toggleFollow(picked)"
        >
          {{ picked.followed ? "取消关注" : "关注这个节点" }}
        </Button>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import {
  ArrowRight,
  CalendarDays,
  ChevronRight,
  Clock3,
  Send,
  Bell,
  CircleCheckBig,
  ClipboardPen,
  FileText,
  ListChecks,
  Megaphone,
} from "@lucide/vue";
import UiIcon from "../../../components/UiIcon.vue";
import TodayGreetings from "../components/TodayGreetings.vue";
import NavIcon from "../../../components/NavIcon.vue";
import {
  meApi,
  type ApplyItem,
  type MilestoneView,
  type Reminder,
  type RhythmNode,
  type Overview,
} from "../../../lib/me-api";
import {
  relTime,
  reportTone,
  daysBetween,
  todayISO,
} from "../../../lib/format";
import { toast, errText } from "../../../lib/toast";
import { loadMeSummary, meSummary, meSummaryLoaded } from "../../../lib/me";
import { api } from "../../../lib/api";
import { toneBadge } from "../../../lib/tone";
import WorkCalendar from "../../../components/WorkCalendar.vue";
import StatCard from "../../../ui/StatCard.vue";
import Skeleton from "../../../ui/Skeleton.vue";
import { session } from "../../../lib/session";
import Callout from "../../../ui/Callout.vue";
import Badge from "../../../ui/Badge.vue";
import Button from "../../../ui/Button.vue";
import EmptyState from "../../../ui/EmptyState.vue";
import Dialog from "../../../ui/Dialog.vue";

const router = useRouter();
const data = ref<Overview | null>(null);
const welcomeText = computed(() => {
  const name = data.value?.greeting.name || session.name;
  const text = data.value?.greeting.text || "欢迎回来";
  return name && !text.includes(name) ? `${text}，${name}` : text;
});
const strip = ref<RhythmNode[]>([]);
const nextMilestone = ref<MilestoneView | null>(null);
const picked = ref<RhythmNode | null>(null);
const loadError = ref("");
const overviewLoading = ref(true);
const quickApps = [
  { title: "请假", desc: "", icon: "clipboard", to: "/me/apply?new=leave" },
  {
    title: "报销",
    desc: "",
    icon: "receipt",
    to: "/me/apply?new=reimbursement",
  },
  { title: "出差", desc: "", icon: "plane", to: "/me/apply?new=travel" },
  { title: "用品领用", desc: "", icon: "box", to: "/me/apply?new=supply" },
  { title: "写周报", desc: "", icon: "trending-up", to: "/me/report?type=2" },
  { title: "我的资料", desc: "", icon: "user", to: "/me/profile" },
];
const summaryCards = computed(() => [
  {
    title: "待我处理",
    value: data.value ? todoTotal.value : "—",
    hint: "审批、报告与节点待办",
    icon: ListChecks,
    glyph: "📝",
    tone: "primary" as const,
    to: "#my-todos",
  },
  {
    title: "近期审批中",
    value: mine.value ? inFlight.value.length : "—",
    hint: "近期发起的审批申请",
    icon: Send,
    glyph: "🔄",
    tone: "warning" as const,
    to: "/me/apply",
  },
  {
    title: "未读提醒",
    value: meSummaryLoaded.value ? meSummary.counts.reminder : "–",
    hint: "关注重要消息与动态",
    icon: Bell,
    glyph: "🔔",
    tone: "success" as const,
    to: "/me/reminder",
  },
  {
    title: "报告待办",
    value: meSummaryLoaded.value ? meSummary.counts.report : "–",
    hint: "及时记录，让进展可见",
    icon: FileText,
    glyph: "📑",
    tone: "info" as const,
    to: "/me/report",
  },
]);
/** null 表示这次没取到，摘要栏宁可不显示也不写「没有」 */
const mine = ref<ApplyItem[] | null>(null);

const ACTION_TEXT: Record<string, string> = {
  report: "去填写",
  review: "去点评",
  rhythm: "看节奏",
};
const CAT_TEXT: Record<string, string> = {
  todo: "待我办",
  milestone: "里程碑",
  compliance: "规则提醒",
  corp: "公司动态",
};

const todoTotal = computed(() =>
  data.value ? data.value.todos.reduce((sum, g) => sum + g.count, 0) : 0,
);

/** 状态 1 = 审批中（单子在企微），其余都是已经落到终态的历史；日程没有审批状态，天然被排除 */
const inFlight = computed(() =>
  (mine.value ?? []).filter((row) => row.status === 1),
);

const mineTitle = computed(
  () => `我发起的 ${inFlight.value.length} 件还在企业微信审批中`,
);

/** 挑最久没动的那件说，摘要栏只负责提醒「你在等谁」，明细交给办事页 */
const mineSub = computed(() => {
  const rows = inFlight.value;
  if (!rows.length) return "";
  const oldest = rows.reduce(
    (min, row) => ((row.createTime || "") < (min.createTime || "") ? row : min),
    rows[0],
  );
  const day =
    daysBetween((oldest.createTime || "").slice(0, 10), todayISO()) + 1;
  return `最久的是${oldest.typeText}${oldest.title ? `「${oldest.title}」` : ""}，已等 ${day} 天`;
});

function range(node: RhythmNode) {
  return node.startDate === node.endDate
    ? node.startDate
    : `${node.startDate} ~ ${node.endDate}`;
}

function bannerTag(node: RhythmNode) {
  if (node.state === "ongoing") {
    const days = daysBetween(node.startDate, node.endDate) + 1;
    const done = daysBetween(node.startDate, todayISO()) + 1;
    return `进行中 第 ${done} 天 / 共 ${days} 天`;
  }
  if (node.state === "upcoming") return `倒计时 ${node.daysLeft} 天`;
  return "已结束";
}

function brief(node: RhythmNode) {
  if (node.deptActions.length)
    return node.deptActions
      .map((a) => a.dept + " " + a.items[0])
      .slice(0, 2)
      .join(" · ");
  return node.note || "";
}

function milestoneText(m: MilestoneView) {
  const days = m.daysLeft ?? 0;
  if (days <= 0) return `今天就是${m.typeText}`;
  return `还有 ${days} 天，${m.typeText}`;
}

function openNode(node: RhythmNode) {
  picked.value = node;
}

async function toggleFollow(node: RhythmNode) {
  try {
    if (node.followed) {
      await meApi.unfollowNode(node.id);
      node.followed = false;
      toast("已取消关注");
    } else {
      await meApi.followNode(node.id);
      node.followed = true;
      toast("关注后节点开始前会提醒你");
    }
    void loadMeSummary(true);
  } catch (e) {
    toast(errText(e), "bad");
  }
}

async function openReminder(row: Reminder) {
  if (!row.read) {
    try {
      await meApi.markRead(row.id);
      row.read = true;
      void loadMeSummary(true);
    } catch {
      /* 已读失败不拦跳转 */
    }
  }
  if (row.link) router.push(row.link);
}

async function loadOverview() {
  overviewLoading.value = true;
  loadError.value = "";
  try {
    data.value = await meApi.overview();
  } catch (e) {
    loadError.value = errText(e);
  } finally {
    overviewLoading.value = false;
  }
}

onMounted(() => {
  void loadOverview();
  void meApi
    .rhythmNodes()
    .then((nodes) => {
      strip.value = nodes.filter((n) => n.state !== "past").slice(0, 5);
    })
    .catch(() => {});
  void meApi
    .milestones()
    .then((milestones) => {
      nextMilestone.value =
        milestones
          .filter((m) => (m.daysLeft ?? -1) >= 0)
          .sort((a, b) => (a.daysLeft ?? 0) - (b.daysLeft ?? 0))[0] ?? null;
    })
    .catch(() => {});
  void loadMine();
  void loadClocks();
});

async function loadMine() {
  try {
    mine.value = await meApi.recentApplies(50);
  } catch {
    mine.value = null;
  }
}

const clocks = ref<{ attendanceTime: string; type: number; status: number }[]>([]);
const clockBusy = ref(false);
const clockError = ref(false);

const CLOCK_STATUS: Record<number, string> = {
  1: "正常",
  2: "迟到",
  3: "早退",
  4: "请假",
  5: "出差",
};

const clockText = computed(() => {
  if (clockError.value) return "打卡记录加载失败，请刷新重试";
  if (!clocks.value.length) return "今天还没有打卡记录";
  const latest = [...clocks.value].sort((a, b) => String(b.attendanceTime).localeCompare(String(a.attendanceTime)))[0];
  const time = (latest.attendanceTime || "").slice(11, 16);
  const status = CLOCK_STATUS[latest.status];
  return `已打卡 ${clocks.value.length} 次 · 最近 ${time}${status ? `（${status}）` : ""}`;
});

async function loadClocks() {
  clockError.value = false;
  try {
    clocks.value =
      (await api.get<{ attendanceTime: string; type: number; status: number }[]>(
        "/admin-api/oa/attendance/my-today-list",
      )) ?? [];
  } catch {
    clockError.value = true;
    clocks.value = [];
  }
}

async function clock() {
  if (clockBusy.value) return;
  clockBusy.value = true;
  try {
    await api.post("/admin-api/oa/attendance/clock");
    toast("打卡成功", "ok");
    await loadClocks();
  } catch (e) {
    toast(errText(e), "bad");
  } finally {
    clockBusy.value = false;
  }
}
</script>
