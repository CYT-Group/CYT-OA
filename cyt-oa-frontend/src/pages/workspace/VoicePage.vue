<template>
  <div class="min-w-0 space-y-5">
    <PageHeader :title="manage ? '发声管理' : '我要发声'" :description="manage ? '处理员工建议与来信，发布问卷并查看调研结果。' : '分享建议、向 CEO 写信、匿名反馈，参与公司满意度调研。'" />
    <nav class="flex flex-wrap gap-1 rounded-xl border bg-card p-1" :aria-label="manage ? '发声管理分类' : '发声渠道'">
      <RouterLink v-for="item in tabs" :key="item.value" :to="{ path: route.path, query: { [manage ? 'tab' : 'channel']: item.value } }" class="rounded-lg px-3 py-2 text-xs font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring" :class="selected === item.value ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-accent'" :aria-current="selected === item.value ? 'page' : undefined">{{ item.label }}</RouterLink>
    </nav>
    <SurveyWorkspace v-if="isSurvey" :manage="manage" :view="selected" />
    <template v-else>
      <section v-if="!manage" class="panel p-5">
        <h2 class="text-base font-semibold">{{ channelName }}</h2>
        <p class="mt-2 text-xs leading-6 text-muted-foreground">{{ selected === 'anon' ? '本渠道以匿名方式展示来信，请勿在正文中填写个人敏感信息。' : selected === 'ceo' ? '来信由具有 CEO 信箱权限的管理员查阅和回复。' : '写清问题、影响和建议，帮助负责同事跟进处理。' }}</p>
        <form class="mt-4 space-y-4" @submit.prevent="submit"><div><label for="voice-content" class="field-label">{{ channelName }}内容 *</label><Textarea id="voice-content" v-model.trim="content" :rows="5" required maxlength="2000" :placeholder="`写下你的${selected === 'suggestion' ? '建议' : '想法'}…`" /><p class="mt-1 text-right text-xs text-muted-foreground">{{ content.length }} / 2000</p></div><Callout v-if="submitError" tone="destructive">{{ submitError }}</Callout><div class="flex flex-wrap items-center justify-between gap-3"><Checkbox v-if="selected !== 'anon'" v-model="anonymous">匿名提交</Checkbox><span v-else class="text-xs text-muted-foreground">此渠道自动匿名</span><Button type="submit" :loading="busy" :disabled="!content">提交{{ channelName === 'CEO 信箱' ? '来信' : '反馈' }}</Button></div></form>
      </section>
      <section class="panel overflow-hidden">
        <div class="panel-header"><h2 class="panel-title">{{ manage ? channelName : '我的提交记录' }}</h2><Button variant="ghost" size="sm" :disabled="loading" @click="load">刷新记录</Button></div>
        <Callout v-if="error" class="m-5" tone="destructive">{{ error }}<template #action><Button size="sm" variant="outline" @click="load">重试</Button></template></Callout>
        <p v-if="loading" role="status" class="p-8 text-sm text-muted-foreground">正在读取发声记录…</p>
        <EmptyState v-else-if="!rows.length && !error" :title="manage ? '暂无待处理来信' : '还没有提交记录'" :description="manage ? '新提交的内容会显示在这里。' : '提交反馈后，可在这里查看处理状态与回复。'" compact />
        <ul v-else class="divide-y"><li v-for="item in rows" :key="item.id" class="space-y-3 p-5"><div class="flex flex-wrap items-center justify-between gap-2"><div class="flex items-center gap-2"><Badge>{{ workspaceStatus[item.status] || item.status }}</Badge><span v-if="manage" class="text-xs text-muted-foreground">{{ item.author }}</span><span class="text-xs text-muted-foreground">{{ dateText(item.createdAt) }}</span></div><Button v-if="manage" size="sm" variant="outline" @click="edit(item)">处理与回复</Button></div><p class="whitespace-pre-wrap break-words text-sm leading-7">{{ item.content }}</p><div v-if="item.reply" class="rounded-lg bg-muted p-4"><p class="text-xs font-medium text-primary">处理回复</p><p class="mt-2 whitespace-pre-wrap break-words text-sm leading-6">{{ item.reply }}</p></div><p v-if="manage && item.channel === 'anon'" class="text-xs text-muted-foreground">审核状态：{{ moderationLabels[item.moderation] }}</p></li></ul>
      </section>
    </template>
    <Dialog :open="!!editing" title="处理与回复" @update:open="value => { if (!value && !busy) editing = null }"><form id="voice-reply-form" class="space-y-4" @submit.prevent="reply"><p class="whitespace-pre-wrap break-words rounded-lg bg-muted p-3 text-sm leading-6">{{ editing?.content }}</p><div><label for="voice-state" class="field-label">处理状态</label><Select id="voice-state" v-model="replyForm.status" :options="Object.entries({ open: '待处理', handling: '处理中', done: '已回复', closed: '已关闭' }).map(([value, label]) => ({ value, label }))" /></div><div v-if="editing?.channel === 'anon'"><label for="voice-moderation" class="field-label">内容审核</label><Select id="voice-moderation" v-model="replyForm.moderation" :options="Object.entries(moderationLabels).map(([value, label]) => ({ value, label }))" /></div><div><label for="voice-reply" class="field-label">回复内容{{ replyForm.status === 'done' ? ' *' : '' }}</label><Textarea id="voice-reply" v-model.trim="replyForm.reply" :required="replyForm.status === 'done'" maxlength="2000" :rows="4" /></div><Callout v-if="submitError" tone="destructive">{{ submitError }}</Callout></form><template #footer><Button variant="outline" :disabled="busy" @click="editing = null">取消</Button><Button type="submit" form="voice-reply-form" :loading="busy">保存回复</Button></template></Dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { session } from '../../lib/session';
import { workspaceApi, workspaceStatus, type WorkspaceVoice } from '../../lib/workspace-api';
import { dateText } from '../../lib/hr-workspace';
import { errText, toast } from '../../lib/toast';
import SurveyWorkspace from './components/SurveyWorkspace.vue';
import PageHeader from '../../ui/PageHeader.vue';
import Button from '../../ui/Button.vue';
import Select from '../../ui/Select.vue';
import Textarea from '../../ui/Textarea.vue';
import Checkbox from '../../ui/Checkbox.vue';
import Badge from '../../ui/Badge.vue';
import Callout from '../../ui/Callout.vue';
import EmptyState from '../../ui/EmptyState.vue';
import Dialog from '../../ui/Dialog.vue';
const route = useRoute();
const manage = computed(() => route.path.startsWith('/portal/'));
const tabs = computed(() => manage.value ? [{ value: 'suggestion', label: '建议箱管理' }, ...(session.isAdmin ? [{ value: 'ceo', label: 'CEO 信箱管理' }] : []), { value: 'anon', label: '吐槽审核' }, { value: 'questionnaire', label: '问卷配置' }, { value: 'release', label: '发布管理' }, { value: 'results', label: '调研结果' }] : [{ value: 'suggestion', label: '建议箱' }, { value: 'ceo', label: 'CEO 信箱' }, { value: 'anon', label: '匿名吐槽' }, { value: 'survey', label: '满意度调研' }]);
const selected = computed(() => { const key = String(route.query[manage.value ? 'tab' : 'channel'] || 'suggestion'); return tabs.value.some(item => item.value === key) ? key : 'suggestion'; });
const isSurvey = computed(() => ['survey', 'questionnaire', 'release', 'results'].includes(selected.value));
const channelName = computed(() => ({ suggestion: '建议箱', ceo: 'CEO 信箱', anon: '匿名吐槽' })[selected.value as 'suggestion'] || '发声');
const moderationLabels: Record<string, string> = { pending: '待审核', approved: '已通过', rejected: '未通过' };
const rows = ref<WorkspaceVoice[]>([]), content = ref(''), anonymous = ref(false), busy = ref(false), loading = ref(false), error = ref(''), submitError = ref(''), editing = ref<WorkspaceVoice | null>(null);
const replyForm = reactive({ status: 'handling', reply: '', moderation: 'pending' });
let request = 0;
async function load() { const id = ++request; rows.value = []; error.value = ''; if (isSurvey.value) return; loading.value = true; try { const data = await workspaceApi.voices(selected.value, manage.value); if (id === request) rows.value = data; } catch (e) { if (id === request) error.value = errText(e); } finally { if (id === request) loading.value = false; } }
async function submit() { if (busy.value) return; busy.value = true; submitError.value = ''; try { await workspaceApi.submitVoice({ channel: selected.value, content: content.value, anonymous: selected.value === 'anon' || anonymous.value }); content.value = ''; toast('反馈已提交'); await load(); } catch (e) { submitError.value = errText(e); } finally { busy.value = false; } }
function edit(item: WorkspaceVoice) { editing.value = item; Object.assign(replyForm, { status: item.status, reply: item.reply, moderation: item.moderation }); submitError.value = ''; }
async function reply() { if (!editing.value || busy.value) return; busy.value = true; submitError.value = ''; try { await workspaceApi.replyVoice(editing.value.id, replyForm); editing.value = null; toast('回复已保存'); await load(); } catch (e) { submitError.value = errText(e); } finally { busy.value = false; } }
watch(() => [selected.value, manage.value], () => { content.value = ''; anonymous.value = false; submitError.value = ''; editing.value = null; void load(); }, { immediate: true });
</script>
