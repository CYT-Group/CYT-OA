<template>
  <div class="space-y-6">
    <section class="welcome-banner">
      <div class="relative z-1">
        <p class="welcome-kicker"><span class="h-1.5 w-1.5 rounded-full bg-primary" />企业管理 · OVERVIEW</p>
        <h1 class="welcome-title">
          {{ greeting }}，{{ session.name || '管理员' }}<span class="ml-2 text-primary">。</span>
        </h1>
        <p class="mt-2 text-[13px] leading-relaxed text-muted-foreground">
          从这里掌握企业日常，让每一件工作井然有序。
        </p>
      </div>
      <div class="relative z-1 flex flex-wrap items-center gap-2">
        <Button variant="outline" @click="router.push('/me/overview')"
          ><House class="size-4" />我的工作台</Button
        >
        <Button @click="router.push('/org')"><Network class="size-4" />查看组织</Button>
      </div>
    </section>

    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
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
          :tone="card.tone"
          class="hover:border-primary/35"
        />
      </router-link>
    </div>

    <div class="dashboard-grid">
      <div class="dashboard-primary">
        <section class="panel overflow-hidden">
          <div class="panel-header">
            <h2 class="panel-title">常用应用</h2>
            <span class="text-[11px] text-muted-foreground">企业日常，一步直达</span>
          </div>
          <div class="quick-apps">
            <router-link v-for="tile in tiles" :key="tile.key" :to="tile.to" class="quick-app group">
              <span class="quick-app-icon"><UiIcon :name="tile.icon" :size="20" /></span>
              <span class="min-w-0"
                ><b class="block text-[13px] font-medium group-hover:text-primary">{{ tile.title }}</b
                ><small class="mt-1 block text-[11px] text-muted-foreground">{{ tile.desc }}</small></span
              >
            </router-link>
          </div>
        </section>

        <section class="panel overflow-hidden">
          <div class="panel-header">
            <h2 class="panel-title"><Megaphone class="size-4 text-primary" />公司公告</h2>
            <Button variant="ghost" size="sm" @click="router.push('/page/oa-announcement')"
              >全部公告<ArrowRight class="size-3.5"
            /></Button>
          </div>
          <div v-if="announcementsLoading" class="space-y-4 p-5">
            <Skeleton v-for="i in 3" :key="i" class="h-10 w-full" />
          </div>
          <Callout v-else-if="announcementsError" class="m-5" tone="warning" title="公告暂未加载"
            ><span>{{ announcementsError }}</span
            ><template #action
              ><Button variant="ghost" size="sm" @click="loadAnnouncements">重试</Button></template
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
            <span class="grid size-9 shrink-0 place-items-center rounded-lg bg-primary/8 text-primary"
              ><FileText class="size-4"
            /></span>
            <span class="min-w-0 flex-1"
              ><b class="block truncate text-[13px] font-medium">{{ row.title }}</b
              ><small class="mt-1 block text-[11px] text-muted-foreground"
                >{{ row.senderName || '公司公告' }} ·
                {{ (row.publishTime || row.createTime || '').slice(0, 10) }}</small
              ></span
            >
            <ChevronRight class="size-4 text-muted-foreground" />
          </button>
        </section>
      </div>

      <div class="dashboard-secondary">
        <WorkCalendar
          ><router-link
            to="/page/oa-schedule"
            class="flex items-center justify-between text-xs text-muted-foreground hover:text-primary"
            ><span>安排日程，让工作更从容</span><ArrowRight class="size-3.5" /></router-link
        ></WorkCalendar>
        <section class="panel overflow-hidden">
          <div class="panel-header">
            <h2 class="panel-title">我的待处理</h2>
            <span class="text-[11px] text-muted-foreground">个人事务</span>
          </div>
          <router-link v-for="item in personalItems" :key="item.to" :to="item.to" class="list-row">
            <component :is="item.icon" class="size-4 text-muted-foreground" /><span
              class="flex-1 text-[13px]"
              >{{ item.title }}</span
            ><span
              class="min-w-6 rounded-md bg-muted px-1.5 py-0.5 text-center text-xs font-medium tabular-nums"
              >{{ item.count }}</span
            ><ChevronRight class="size-3 text-muted-foreground" />
          </router-link>
        </section>
        <div class="rounded-xl border border-primary/10 bg-primary/5 p-5">
          <div class="flex items-center gap-2 text-sm font-medium text-primary">
            <BriefcaseBusiness class="size-4" />协作，从连接开始
          </div>
          <p class="mt-2 text-xs leading-6 text-muted-foreground">
            组织、档案与办公台账集中管理，审批进展在企业微信中协同处理。
          </p>
          <router-link
            to="/page/hrm-employee"
            class="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary"
            >查看员工档案<ArrowRight class="size-3.5"
          /></router-link>
        </div>
      </div>
    </div>
    <Dialog
      :open="selectedAnnouncement !== null"
      :title="selectedAnnouncement?.title || '公告详情'"
      :description="selectedAnnouncement?.senderName"
      @update:open="!$event && (selectedAnnouncement = null)"
    >
      <p class="whitespace-pre-wrap text-sm leading-7 text-muted-foreground">
        {{ selectedAnnouncement?.content || '此公告的正文请前往公告管理查看。' }}
      </p>
      <template #footer
        ><Button variant="outline" @click="selectedAnnouncement = null">关闭</Button
        ><Button @click="router.push('/page/oa-announcement')">公告管理</Button></template
      >
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Bell,
  BriefcaseBusiness,
  ChevronRight,
  FileText,
  House,
  Megaphone,
  Network,
  Package,
  Stamp,
  Users
} from '@lucide/vue'
import UiIcon from '../components/UiIcon.vue'
import WorkCalendar from '../components/WorkCalendar.vue'
import Button from '../ui/Button.vue'
import StatCard from '../ui/StatCard.vue'
import EmptyState from '../ui/EmptyState.vue'
import Skeleton from '../ui/Skeleton.vue'
import Callout from '../ui/Callout.vue'
import Dialog from '../ui/Dialog.vue'
import { session } from '../lib/session'
import { meSummary, meSummaryLoaded } from '../lib/me'
import { api } from '../lib/api'
import { errText } from '../lib/toast'

const router = useRouter()
const stats = ref<Record<string, string>>({})
const statCards = [
  {
    key: 'emp',
    label: '在册员工',
    desc: '人事档案 · 查看员工',
    icon: Users,
    tone: 'primary' as const,
    to: '/page/hrm-employee'
  },
  {
    key: 'ann',
    label: '已发布公告',
    desc: '信息传达 · 公司通知',
    icon: Megaphone,
    tone: 'success' as const,
    to: '/page/oa-announcement'
  },
  {
    key: 'seal',
    label: '企业印章',
    desc: '印章台账 · 统一管理',
    icon: Stamp,
    tone: 'warning' as const,
    to: '/page/oa-seal'
  },
  {
    key: 'supply',
    label: '办公用品',
    desc: '物资管理 · 在库条目',
    icon: Package,
    tone: 'info' as const,
    to: '/page/oa-supply-item'
  }
]
const tiles = [
  { key: 'employee', icon: 'user', title: '员工档案', desc: '人员信息与任职', to: '/page/hrm-employee' },
  { key: 'org', icon: 'users', title: '组织架构', desc: '部门与团队成员', to: '/org' },
  { key: 'seal', icon: 'stamp', title: '印章管理', desc: '印章与用印台账', to: '/page/oa-seal' },
  { key: 'supply', icon: 'box', title: '办公用品', desc: '物资台账与入库', to: '/page/oa-supply-item' },
  { key: 'issue', icon: 'truck', title: '领用发放', desc: '领用与归还登记', to: '/page/oa-supply-issue' },
  { key: 'dict', icon: 'book', title: '数据字典', desc: '统一基础数据', to: '/page/sys-dict' }
]
const personalItems = computed(() => [
  { title: '未读提醒', count: meSummaryLoaded.value ? meSummary.counts.reminder : '–', icon: Bell, to: '/me/reminder' },
  { title: '报告待办', count: meSummaryLoaded.value ? meSummary.counts.report : '–', icon: FileText, to: '/me/report' }
])
const greeting = computed(() => {
  const h = new Date().getHours()
  return h < 6 ? '夜深了' : h < 12 ? '早上好' : h < 18 ? '下午好' : '晚上好'
})
interface Announcement {
  id: string
  title: string
  senderName?: string
  publishTime?: string
  createTime?: string
  content?: string
}
const announcements = ref<Announcement[]>([])
const selectedAnnouncement = ref<Announcement | null>(null)
const announcementsLoading = ref(true)
const announcementsError = ref('')
async function loadAnnouncements() {
  announcementsLoading.value = true
  announcementsError.value = ''
  try {
    const result = await api.get<{ list: Announcement[]; total: number }>(
      '/admin-api/oa/announcement/published-page',
      { pageNo: 1, pageSize: 4 }
    )
    announcements.value = result.list ?? []
    stats.value.ann = String(result.total ?? 0)
  } catch (e) {
    announcementsError.value = errText(e)
    stats.value.ann = '–'
  } finally {
    announcementsLoading.value = false
  }
}
onMounted(() => {
  void loadAnnouncements()
  const fetchers = {
    seal: '/admin-api/oa/seal/page',
    emp: '/admin-api/hrm/employee/page',
    supply: '/admin-api/oa/supply-item/page'
  }
  void Promise.allSettled(
    Object.entries(fetchers).map(async ([key, url]) => {
      try {
        const data = await api.get<{ total: number }>(url, { pageNo: 1, pageSize: 1 })
        stats.value[key] = String(data?.total ?? 0)
      } catch {
        stats.value[key] = '–'
      }
    })
  )
})
</script>
