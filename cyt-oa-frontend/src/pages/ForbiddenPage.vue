<template>
  <section class="panel mx-auto max-w-xl p-8 text-center">
    <ShieldAlert class="mx-auto mb-4 size-10 text-muted-foreground" />
    <h1 class="text-lg font-semibold">没有此页面的访问权限</h1>
    <p class="mt-2 text-sm text-muted-foreground">请联系管理员确认角色授权。</p>
    <Button
      class="mt-5"
      @click="
        router.push(
          session.canAccessConsole ? session.consoleHome : '/me/overview',
        )
      "
      >返回工作空间</Button
    >
  </section>
</template>
<script setup lang="ts">
import { ShieldAlert } from "@lucide/vue";
import { useRouter } from "vue-router";
import { session } from "../lib/session";
import Button from "../ui/Button.vue";
const router = useRouter();
</script>
