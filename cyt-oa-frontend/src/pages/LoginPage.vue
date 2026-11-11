<template>
  <div class="login-reference">
    <Button
      variant="ghost"
      size="icon-sm"
      class="absolute right-6 top-6"
      :aria-label="isDark ? '切换到浅色模式' : '切换到深色模式'"
      @click="toggleTheme"
      ><Sun v-if="isDark" /><Moon v-else
    /></Button>
    <main class="login-reference-main">
      <header class="login-reference-heading">
        <span class="brand-mark">OA</span>
        <h1>CYT</h1>
        <p>个人办公与企业管理</p>
      </header>
      <section
        class="login-reference-card panel"
        :aria-busy="booting || exchanging"
      >
        <template v-if="exchanging">
          <div
            class="flex min-h-64 flex-col items-center justify-center gap-5 text-center"
            role="status"
          >
            <LoaderCircle class="size-8 animate-spin text-primary" />
            <div>
              <h2 class="text-lg font-bold">正在进入工作空间</h2>
              <p class="mt-2 text-xs text-muted-foreground">
                正在确认身份，请稍候
              </p>
            </div>
          </div>
        </template>
        <template v-else>
          <h2 class="text-lg font-bold">
            {{ method === "password" ? "登录工作空间" : "企业微信扫码登录" }}
          </h2>
          <p class="mt-2 text-xs text-muted-foreground">
            {{
              method === "password"
                ? "使用本机开发账号继续"
                : "使用企业微信扫一扫，安全进入工作空间"
            }}
          </p>
          <Callout v-if="notice" class="mt-5" role="status">{{
            notice
          }}</Callout>
          <Callout
            v-if="err"
            class="mt-5"
            tone="destructive"
            role="alert"
            aria-live="polite"
            >{{ err }}</Callout
          >
          <div
            v-if="booting"
            class="flex min-h-64 items-center justify-center gap-2 text-xs text-muted-foreground"
            role="status"
          >
            <LoaderCircle class="size-4 animate-spin" />正在加载登录方式
          </div>
          <template v-else-if="method === 'wecom' && policy?.wecomEnabled">
            <template v-if="insideWecom">
              <div
                class="my-8 grid size-16 place-items-center rounded-2xl bg-primary/8 text-primary"
              >
                <ScanLine class="size-8" />
              </div>
              <Button class="mt-2 w-full" size="lg" @click="openWecom"
                >使用企业微信登录<ArrowRight class="size-4"
              /></Button>
            </template>
            <template v-else>
              <div
                class="oa-wecom-panel relative mx-auto mt-5 overflow-hidden rounded-xl border border-border bg-white"
                :aria-busy="qrLoading"
              >
                <div ref="qrElement" class="h-[380px] w-full" />
                <div
                  v-if="qrLoading || qrError || qrExpired"
                  class="absolute inset-0 flex flex-col items-center justify-center gap-4 bg-background/95 px-5 text-center text-xs text-muted-foreground"
                  role="status"
                >
                  <LoaderCircle
                    v-if="qrLoading"
                    class="size-6 animate-spin text-primary"
                  />
                  <ScanLine v-else class="size-8 text-primary" />
                  <p>
                    {{
                      qrLoading
                        ? "正在生成登录二维码"
                        : qrExpired
                          ? "二维码已过期，请刷新后扫码"
                          : qrError
                    }}
                  </p>
                  <Button
                    v-if="!qrLoading"
                    variant="outline"
                    size="sm"
                    @click="loadQr"
                    ><RefreshCw class="size-3.5" />重新获取二维码</Button
                  >
                </div>
              </div>
              <div class="mt-3 flex items-center justify-center gap-2 text-xs">
                <Button
                  variant="ghost"
                  size="sm"
                  :disabled="qrLoading"
                  @click="loadQr"
                  ><RefreshCw class="size-3.5" />刷新二维码</Button
                >
                <span class="text-border">|</span
                ><Button
                  variant="ghost"
                  size="sm"
                  :disabled="qrLoading"
                  @click="openWecom"
                  >前往企业微信登录<ArrowUpRight class="size-3.5"
                /></Button>
              </div>
            </template>
          </template>
          <form
            v-else-if="method === 'password' && policy?.passwordLoginEnabled"
            class="mt-7 space-y-5"
            @submit.prevent="doLogin"
          >
            <div>
              <Label for="login-username">账号</Label>
              <div class="relative">
                <UserRound
                  class="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                /><Input
                  id="login-username"
                  name="username"
                  v-model.trim="username"
                  class="h-10 pl-9"
                  placeholder="请输入账号"
                  autocomplete="username"
                  :spellcheck="false"
                />
              </div>
            </div>
            <div>
              <Label for="login-password">密码</Label>
              <div class="relative">
                <LockKeyhole
                  class="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                /><Input
                  id="login-password"
                  name="password"
                  v-model="password"
                  :type="showPassword ? 'text' : 'password'"
                  class="h-10 px-9"
                  placeholder="请输入密码"
                  autocomplete="current-password"
                /><button
                  type="button"
                  class="absolute right-1.5 top-1/2 grid size-7 -translate-y-1/2 place-items-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground"
                  :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                  @click="showPassword = !showPassword"
                >
                  <EyeOff v-if="showPassword" class="size-4" /><Eye
                    v-else
                    class="size-4"
                  />
                </button>
              </div>
            </div>
            <Button type="submit" size="lg" class="w-full" :loading="loading">{{
              loading ? "登录中…" : "登录"
            }}</Button>
          </form>
          <div
            v-else
            class="py-8 text-center text-xs leading-6 text-muted-foreground"
          >
            <ScanLine class="mx-auto mb-4 size-8 text-primary" />
            <p>企业微信登录暂未就绪</p>
            <p>请联系管理员检查统一登录配置。</p>
            <Button variant="outline" size="sm" class="mt-4" @click="loadPolicy"
              >重新检查</Button
            >
          </div>
          <div
            v-if="
              !booting && policy?.passwordLoginEnabled && policy.wecomEnabled
            "
            class="mt-5 text-center"
          >
            <Button variant="ghost" size="sm" @click="switchMethod">{{
              method === "wecom" ? "使用本机开发账号" : "返回企业微信扫码"
            }}</Button>
          </div>
          <Button
            v-if="!booting && !policy && err"
            variant="outline"
            size="sm"
            class="mt-4 w-full"
            @click="loadPolicy"
            >重新加载登录方式</Button
          >
          <div class="mt-7 flex items-center gap-3">
            <span class="h-px flex-1 bg-border" /><span
              class="text-[11px] text-muted-foreground"
              >企业内部协同平台</span
            ><span class="h-px flex-1 bg-border" />
          </div>
          <p class="mt-4 text-center text-xs leading-6 text-muted-foreground">
            账号问题请联系企业管理员
          </p>
        </template>
      </section>
      <p class="mt-7 text-center text-[11px] text-muted-foreground">
        © {{ year }} CYT
      </p>
    </main>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onBeforeUnmount, ref } from "vue";
import { useRouter, useRoute } from "vue-router";
import {
  Eye,
  EyeOff,
  LockKeyhole,
  Sun,
  Moon,
  UserRound,
  LoaderCircle,
  ScanLine,
  RefreshCw,
  ArrowRight,
  ArrowUpRight,
} from "@lucide/vue";
import type { WWLoginInstance, WWLoginParams } from "@wecom/jssdk";
import { api } from "../lib/api";
import { session, type LoginInfo } from "../lib/session";
import { resetMeSummary } from "../lib/me";
import { resetPortalRollout } from "../lib/portal-rollout";
import {
  safeLoginTarget,
  parseLoginTarget,
  selectLoginTarget,
  hasLoginHandoff,
  validWecomPanel,
  type WecomConfiguration,
} from "../lib/login-target";
import { errText } from "../lib/toast";
import { isDark, toggleTheme } from "../lib/theme";
import Button from "../ui/Button.vue";
import Callout from "../ui/Callout.vue";
import Input from "../ui/Input.vue";
import Label from "../ui/Label.vue";

interface Policy {
  passwordLoginEnabled: boolean;
  wecomEnabled: boolean;
}
const router = useRouter();
const route = useRoute();
const username = ref("");
const password = ref("");
const showPassword = ref(false);
const err = ref("");
const notice = ref("");
const loading = ref(false);
const booting = ref(true);
const exchanging = ref(false);
const policy = ref<Policy | null>(null);
const method = ref<"wecom" | "password">("wecom");
const qrElement = ref<HTMLElement | null>(null);
const qrLoading = ref(false);
const qrError = ref("");
const qrExpired = ref(false);
const insideWecom = /wxwork/i.test(navigator.userAgent);
const year = new Date().getFullYear();
let returnTo = safeLoginTarget(route.query.from);
let qr: WWLoginInstance | undefined;
let expiry: ReturnType<typeof setTimeout> | undefined;
let generation = 0;
let disposed = false;
let panelQueue: Promise<unknown> = Promise.resolve();

function clearIdentity() {
  session.clear();
  resetMeSummary();
  resetPortalRollout();
}
function stopQr() {
  generation++;
  if (expiry) clearTimeout(expiry);
  qr?.unmount();
  qr = undefined;
  qrLoading.value = false;
}
async function finishLogin(data: LoginInfo) {
  if (disposed) return;
  if (
    !data.token ||
    !Array.isArray(data.roles) ||
    !Array.isArray(data.permissions)
  )
    throw new Error("登录结果不完整，请重新扫码");
  resetMeSummary();
  resetPortalRollout();
  session.save(data);
  await router.replace(returnTo);
}
async function loadPolicy() {
  stopQr();
  booting.value = true;
  policy.value = null;
  try {
    const value = await api.get<Policy>("/api/auth/policy");
    if (disposed) return;
    if (
      typeof value?.passwordLoginEnabled !== "boolean" ||
      typeof value?.wecomEnabled !== "boolean"
    )
      throw new Error("登录方式配置不完整");
    policy.value = value;
    if (notice.value && !value.wecomEnabled)
      notice.value = "企业微信登录尚未启用，请使用当前可用的登录方式。";
    method.value = value.wecomEnabled ? "wecom" : "password";
  } catch (error) {
    if (!disposed) err.value = errText(error, "登录方式加载失败，请重试");
  } finally {
    if (!disposed) booting.value = false;
  }
  if (disposed) return;
  await nextTick();
  if (policy.value?.wecomEnabled && !insideWecom) await loadQr();
}
async function loadQr() {
  stopQr();
  if (
    disposed ||
    method.value !== "wecom" ||
    !policy.value?.wecomEnabled ||
    insideWecom
  )
    return;
  const request = generation;
  qrLoading.value = true;
  qrError.value = "";
  qrExpired.value = false;
  try {
    const configuration = await api.get<WecomConfiguration>(
      "/api/wecom/configuration",
    );
    if (disposed || request !== generation) return;
    if (!configuration.available)
      throw new Error(configuration.message || "企业微信登录暂不可用");
    // 刷新请求顺序执行，旧请求不能晚到服务端而覆盖新二维码绑定的事务。
    const pending = panelQueue
      .catch(() => {})
      .then(() => {
        if (disposed || request !== generation) return null;
        return api.get<Record<string, string>>("/api/wecom/panel", {
          scope: "auto",
          from: returnTo,
        });
      });
    panelQueue = pending;
    const params = await pending;
    if (!params || disposed || request !== generation) return;
    if (!validWecomPanel(configuration, params, window.location.origin))
      throw new Error("登录配置与当前访问地址不一致，请联系管理员");
    const sdk = await import("@wecom/jssdk");
    if (disposed || request !== generation || !qrElement.value) return;
    qr = sdk.createWWLoginPanel({
      el: qrElement.value,
      params: params as WWLoginParams,
      onLoginFail() {
        if (!disposed && request === generation)
          qrError.value = "企业微信登录未完成，请刷新二维码后重试";
      },
    });
    qr.el.title = "企业微信登录二维码";
    qr.el.setAttribute("referrerpolicy", "no-referrer");
    expiry = setTimeout(() => {
      if (disposed || request !== generation) return;
      qrExpired.value = true;
      qr?.unmount();
      qr = undefined;
    }, 290000);
  } catch (error) {
    if (!disposed && request === generation)
      qrError.value = errText(error, "二维码加载失败，请重新获取");
  } finally {
    if (!disposed && request === generation) qrLoading.value = false;
  }
}
async function switchMethod() {
  stopQr();
  method.value = method.value === "wecom" ? "password" : "wecom";
  await nextTick();
  if (method.value === "wecom") await loadQr();
}
function openWecom() {
  stopQr();
  window.location.assign(
    "/api/wecom/login?" +
      new URLSearchParams({ scope: "auto", from: returnTo }),
  );
}
async function doLogin() {
  if (loading.value || !policy.value?.passwordLoginEnabled) return;
  if (!username.value || !password.value) {
    err.value = "请输入账号和密码";
    document
      .getElementById(username.value ? "login-password" : "login-username")
      ?.focus();
    return;
  }
  err.value = "";
  loading.value = true;
  try {
    clearIdentity();
    await finishLogin(
      await api.post<LoginInfo>("/api/auth/login", {
        username: username.value,
        password: password.value,
        device: "auto",
      }),
    );
  } catch (error) {
    if (!disposed) err.value = errText(error, "登录失败，请稍后再试");
  } finally {
    if (!disposed) loading.value = false;
  }
}
onMounted(async () => {
  const requestedFrom = route.query.from;
  const handoff = hasLoginHandoff(route.query);
  const rawCode = route.query.wecom_code;
  const failed = route.query.wecom_error;
  const portalFallback = route.query.sso_error;
  if (handoff) {
    clearIdentity();
    // 在加载二维码 SDK 之前清掉一次性交接码，避免跟随后续请求的 Referer。
    await router.replace({
      path: "/login",
      query: returnTo !== "/" ? { from: returnTo } : {},
    });
    if (disposed) return;
  }
  if (rawCode !== undefined) {
    exchanging.value = true;
    try {
      if (typeof rawCode !== "string" || !/^[a-f0-9]{32}$/.test(rawCode))
        throw new Error("登录凭据无效，请重新扫码");
      await finishLogin(
        await api.post<LoginInfo>("/api/wecom/exchange", { code: rawCode }),
      );
      return;
    } catch (error) {
      if (!disposed) err.value = errText(error, "登录凭据已过期，请重新扫码");
    } finally {
      if (!disposed) exchanging.value = false;
    }
  } else if (failed !== undefined)
    err.value = "企业微信登录未完成或已过期，请重新扫码";
  else if (portalFallback !== undefined) {
    notice.value = "本次身份交接未完成，请使用企业微信扫码继续。";
    if (parseLoginTarget(requestedFrom) === null) {
      try {
        const context = await api.get<{ returnTo: string }>(
          "/api/wecom/portal/context",
        );
        returnTo = selectLoginTarget(requestedFrom, context.returnTo);
        await router.replace({
          path: "/login",
          query: returnTo !== "/" ? { from: returnTo } : {},
        });
      } catch {
        /* 上次事务已失效时，扫码后进入自己的工作空间。 */
      }
    }
  }
  if (!disposed) await loadPolicy();
});
onBeforeUnmount(() => {
  disposed = true;
  stopQr();
});
</script>

<style scoped>
.oa-wecom-panel {
  width: 320px;
  max-width: 100%;
  min-height: 380px;
}
.oa-wecom-panel :deep(iframe) {
  width: 100% !important;
  height: 380px !important;
  border: 0;
}
@media (max-width: 479px) {
  .login-reference-card {
    padding: 20px;
  }
}
</style>
