<template>
  <section class="panel overflow-hidden" aria-labelledby="today-greetings-title">
    <div class="panel-header"><h2 id="today-greetings-title" class="panel-title"><span aria-hidden="true">🎉</span>今日祝贺</h2><Button variant="ghost" size="sm" :disabled="loading" @click="load">刷新</Button></div>
    <p v-if="loading" class="p-5 text-sm text-muted-foreground" role="status">正在读取今日祝贺…</p>
    <Callout v-else-if="error" class="m-4" title="今日祝贺未读取" tone="destructive">{{ error }}</Callout>
    <p v-else-if="!visibleRows.length" class="p-5 text-sm text-muted-foreground">{{ rows.length ? '今日祝贺已处理。' : '今天暂无同事生日或入职周年。' }}</p>
    <div v-else class="grid divide-y lg:grid-cols-2 lg:divide-y-0">
      <div v-for="row in visibleRows" :key="rowKey(row)" class="flex flex-wrap items-center gap-3 p-5">
        <span class="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-xl" aria-hidden="true">{{ row.type === 'birthday' ? '🎂' : '💐' }}</span>
        <div class="min-w-0 flex-1"><p class="text-sm font-medium">{{ row.name }} · {{ row.typeText }}</p><p class="mt-1 text-xs text-muted-foreground">{{ row.deptName }}<span v-if="row.relation"> · {{ row.relation }}</span></p></div>
        <div class="flex gap-2"><Badge v-if="row.blessed" variant="success">祝福已送达</Badge><template v-else><Button variant="ghost" size="sm" @click="ignore(row)">本次忽略</Button><Button size="sm" :loading="sending === rowKey(row)" @click="send(row)">送祝福</Button></template></div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { meApi, type ColleagueMilestone } from '../../../lib/me-api';
import { errText, toast } from '../../../lib/toast';
import Button from '../../../ui/Button.vue';
import Badge from '../../../ui/Badge.vue';
import Callout from '../../../ui/Callout.vue';

const rows = ref<ColleagueMilestone[]>([]), loading = ref(false), error = ref(''), sending = ref('');
const ignored = ref<string[]>([]);
const rowKey = (row: ColleagueMilestone) => `${row.userId}:${row.type}:${row.date}`;
const visibleRows = computed(() => rows.value.filter(row => !ignored.value.includes(rowKey(row))));
async function load() {
  loading.value = true; error.value = '';
  try { rows.value = (await meApi.colleagueMilestones()).filter(row => row.daysLeft === 0 && row.action === 'bless'); }
  catch (e) { rows.value = []; error.value = errText(e); }
  finally { loading.value = false; }
}
function ignore(row: ColleagueMilestone) { ignored.value = [...ignored.value, rowKey(row)]; }
async function send(row: ColleagueMilestone) {
  if (sending.value || row.blessed) return;
  sending.value = rowKey(row);
  try { await meApi.bless({ toUserId: row.userId, type: row.type, date: row.date }); row.blessed = true; toast(`已给${row.name}送上${row.typeText}祝福`); }
  catch (e) { toast(errText(e), 'bad'); }
  finally { sending.value = ''; }
}
onMounted(load);
</script>
