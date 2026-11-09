<template>
  <div class="login-layout">
    <section class="login-story">
      <div class="relative flex items-center gap-3">
        <span class="brand-mark"><LayoutGrid class="size-5" /></span
        ><span
          ><b class="block text-lg font-semibold tracking-wide">畅盈通 OA</b
          ><small class="text-[10px] tracking-[.2em] text-white/50">WORK TOGETHER, BETTER</small></span
        >
      </div>
      <div class="login-story-content">
        <p class="mb-5 flex items-center gap-2 text-[11px] tracking-[.2em] text-blue-300">
          <span class="h-px w-7 bg-blue-400" />连接团队 · 专注工作
        </p>
        <h1>每一份专注，<br />都有高效协作相伴。</h1>
        <p class="mt-5 max-w-sm text-[13px] leading-7 text-white/55">
          让繁杂的事务井然有序。把提醒、报告、办事与团队连接起来，开启更从容的工作方式。
        </p>
        <div class="login-illustration" aria-hidden="true">
          <div class="mb-5 flex items-center justify-between">
            <span class="flex items-center gap-2 text-sm font-medium"
              ><LayoutGrid class="size-4 text-blue-300" />我的工作空间</span
            ><span class="flex gap-1.5"
              ><i class="size-1.5 rounded-full bg-white/25" /><i
                class="size-1.5 rounded-full bg-white/25" /><i class="size-1.5 rounded-full bg-white/25"
            /></span>
          </div>
          <div class="illustration-row">
            <span class="grid size-8 place-items-center rounded-lg bg-blue-400/15 text-blue-300"
              ><FileText class="size-4" /></span
            ><span class="flex-1 text-xs text-white/80">工作报告，让进展清晰可见</span
            ><Check class="size-4 text-emerald-300" />
          </div>
          <div class="illustration-row">
            <span class="grid size-8 place-items-center rounded-lg bg-amber-400/15 text-amber-200"
              ><BellRing class="size-4" /></span
            ><span class="flex-1 text-xs text-white/80">重要提醒，时刻心中有数</span
            ><Check class="size-4 text-emerald-300" />
          </div>
          <div class="illustration-row !pb-0">
            <span class="grid size-8 place-items-center rounded-lg bg-emerald-400/15 text-emerald-300"
              ><Users class="size-4" /></span
            ><span class="flex-1 text-xs text-white/80">团队协作，让彼此更进一步</span
            ><ArrowRight class="size-4 text-white/40" />
          </div>
        </div>
      </div>
      <p class="relative text-[11px] text-white/40">© {{ year }} 畅盈通 · 企业协同办公平台</p>
    </section>
    <section class="login-form-section">
      <Button
        variant="ghost"
        size="icon-sm"
        class="absolute right-6 top-6"
        :aria-label="isDark ? '切换到浅色模式' : '切换到深色模式'"
        @click="toggleTheme"
        ><Sun v-if="isDark" /><Moon v-else
      /></Button>
      <div class="login-form-container">
        <div class="mb-10 flex items-center gap-3 lg:hidden">
          <span class="brand-mark"><LayoutGrid class="size-5" /></span
          ><b class="text-lg font-semibold">畅盈通 OA</b>
        </div>
        <p class="mb-3 text-[11px] font-medium tracking-[.16em] text-primary">WELCOME BACK</p>
        <h2 class="text-[28px] font-semibold tracking-tight">登录你的工作空间</h2>
        <p class="mt-3 text-[13px] leading-6 text-muted-foreground">新一天的有序工作，从这里开始。</p>
        <form class="mt-8 space-y-5" @submit.prevent="doLogin">
          <div>
            <Label for="login-username">账号</Label>
            <div class="relative">
              <UserRound
                class="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
              />
              <Input
                id="login-username"
                v-model.trim="username"
                class="h-10 pl-9"
                placeholder="请输入账号"
                autocomplete="username"
                autofocus
              />
            </div>
          </div>

          <div>
            <Label for="login-password">密码</Label>
            <div class="relative">
              <LockKeyhole
                class="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
              />
              <Input
                id="login-password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                class="h-10 px-9"
                placeholder="请输入密码"
                autocomplete="current-password"
              />
              <button
                type="button"
                class="absolute right-1.5 top-1/2 grid size-7 -translate-y-1/2 place-items-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                <EyeOff v-if="showPassword" class="size-4" />
                <Eye v-else class="size-4" />
              </button>
            </div>
          </div>

          <Callout v-if="err" tone="destructive">
            {{ err }}
          </Callout>

          <Button type="submit" size="lg" class="w-full" :loading="loading">
            {{ loading ? '登录中…' : '登录' }}
          </Button>
        </form>

        <div class="mt-7 flex items-center gap-3">
          <span class="h-px flex-1 bg-border" /><span class="text-[11px] text-muted-foreground"
            >企业内部协同平台</span
          ><span class="h-px flex-1 bg-border" />
        </div>
        <p class="mt-5 text-center text-xs leading-6 text-muted-foreground">
          线上环境请使用企业微信扫码登录<br /><span class="text-[11px]">账号问题请联系企业管理员</span>
        </p>
      </div>
      <p class="login-footer">畅盈通 OA · 协同办公 / 人事管理 / 团队连接</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  ArrowRight,
  BellRing,
  Check,
  Eye,
  EyeOff,
  FileText,
  LayoutGrid,
  LockKeyhole,
  Sun,
  Moon,
  UserRound,
  Users
} from '@lucide/vue'
import { api } from '../lib/api'
import { session } from '../lib/session'
import { errText } from '../lib/toast'
import { isDark, toggleTheme } from '../lib/theme'
import Button from '../ui/Button.vue'
import Callout from '../ui/Callout.vue'
import Input from '../ui/Input.vue'
import Label from '../ui/Label.vue'

const router = useRouter()
const route = useRoute()
const username = ref('')
const password = ref('')
const showPassword = ref(false)
const err = ref('')
const loading = ref(false)
const year = new Date().getFullYear()

async function doLogin() {
  if (!username.value || !password.value) {
    err.value = '请输入账号和密码'
    return
  }
  err.value = ''
  loading.value = true
  try {
    const data = await api.post<{
      token: string
      device: string
      userId: number
      name: string
      roles: string[]
      permissions: string[]
    }>('/api/auth/login', { username: username.value, password: password.value, device: 'admin' })
    session.save({
      token: data.token,
      userId: data.userId,
      name: data.name,
      roles: data.roles,
      permissions: data.permissions
    })
    router.push((route.query.from as string) || '/')
  } catch (e) {
    err.value = errText(e, '登录失败，请稍后再试')
  } finally {
    loading.value = false
  }
}
</script>
