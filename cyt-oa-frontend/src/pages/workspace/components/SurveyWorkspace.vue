<template>
  <div class="space-y-4">
    <div class="flex flex-wrap items-center justify-between gap-3"><h2 class="text-base font-semibold">{{ manage ? view === 'release' ? '问卷发布' : view === 'results' ? '调研结果' : '问卷配置' : '满意度调研' }}</h2><div class="flex gap-2"><Button variant="outline" :disabled="loading" @click="load">刷新问卷</Button><Button v-if="manage && view === 'questionnaire'" @click="edit()">新建问卷</Button></div></div>
    <Callout v-if="error" tone="destructive">{{ error }}</Callout>
    <p v-if="loading" class="panel p-8 text-sm text-muted-foreground" role="status">正在读取问卷…</p>
    <EmptyState v-else-if="!rows.length && !error" :title="manage ? '还没有问卷' : '暂无面向你的调研'" :description="manage ? '新建题目并保存草稿，再到发布管理发放。' : '已发布且在发放范围内的调研会显示在这里。'" />
    <article v-for="survey in rows" :key="survey.id" class="panel p-5"><div class="flex flex-wrap items-start justify-between gap-3"><div class="min-w-0 flex-1"><div class="flex flex-wrap items-center gap-2"><Badge>{{ survey.answered && !manage ? '已填写' : workspaceStatus[survey.status] }}</Badge><Badge v-if="survey.active" variant="secondary">收集中</Badge><span class="text-xs text-muted-foreground">{{ survey.data.anonymous ? '匿名调研' : '实名调研' }} · {{ survey.data.questions.length }} 题</span></div><h3 class="mt-3 break-words text-base font-semibold">{{ survey.title }}</h3><p class="mt-2 whitespace-pre-wrap break-words text-sm leading-6 text-muted-foreground">{{ survey.data.description }}</p><p class="mt-3 text-xs leading-6 text-muted-foreground">{{ dateText(survey.data.startAt) }} 至 {{ dateText(survey.data.endAt) }}</p><p v-if="manage" class="mt-1 text-xs text-muted-foreground">已回收 {{ survey.responseCount }} / {{ survey.audienceCount }} · 参与率 {{ survey.audienceCount ? Math.round(Number(survey.responseCount) / Number(survey.audienceCount) * 100) : 0 }}%</p></div><div class="flex flex-wrap gap-2"><template v-if="manage"><Button v-if="survey.status === 'draft' && view === 'questionnaire'" size="sm" variant="outline" @click="edit(survey)">编辑问卷</Button><Button v-if="survey.status === 'draft' && view === 'release'" size="sm" @click="confirmAction(survey, 'publish')">发布问卷</Button><template v-if="survey.status === 'published' && view === 'release'"><Button size="sm" variant="outline" :disabled="!survey.active" @click="confirmAction(survey, 'remind')">提醒未填写人员</Button><Button size="sm" variant="outline" @click="confirmAction(survey, 'close')">结束收集</Button></template><Button v-if="view === 'results'" size="sm" variant="outline" @click="results(survey)">查看结果</Button></template><Button v-else :disabled="!survey.active || survey.answered" @click="answer(survey)">{{ survey.answered ? '已填写' : survey.active ? '填写问卷' : '未在收集时间' }}</Button></div></div></article>
    <Dialog :open="opened" :title="editing ? '编辑问卷' : '新建问卷'" size="lg" @update:open="value => { if (!busy) opened = value }"><form id="survey-edit-form" class="space-y-4" @submit.prevent="save"><Callout v-if="formError" tone="destructive">{{ formError }}</Callout><div><label for="survey-title" class="field-label">问卷名称 *</label><Input id="survey-title" v-model.trim="form.title" required maxlength="255" /></div><div><label for="survey-description" class="field-label">说明</label><Textarea id="survey-description" v-model="form.description" :rows="3" maxlength="4000" /></div><div class="grid gap-4 sm:grid-cols-2"><div><label for="survey-start" class="field-label">开始时间 *</label><Input id="survey-start" v-model="form.startAt" type="datetime-local" required /></div><div><label for="survey-end" class="field-label">结束时间 *</label><Input id="survey-end" v-model="form.endAt" type="datetime-local" required /></div><div><label for="survey-scope" class="field-label">发放范围</label><Select id="survey-scope" v-model="form.scope" :options="[{ value: 'all', label: '全公司' }, { value: 'users', label: '指定员工' }]" /></div><Checkbox v-model="form.anonymous">匿名调研</Checkbox></div><div v-if="form.scope === 'users'"><label for="survey-users" class="field-label">受访员工 *</label><MultiSelect id="survey-users" v-model="form.targetIds" :options="users" /></div><fieldset class="space-y-3"><legend class="mb-2 text-sm font-semibold">题目</legend><div v-for="(q, index) in form.questions" :key="q.id" class="space-y-3 rounded-lg border p-4"><div class="flex items-start gap-2"><Input v-model.trim="q.title" :aria-label="`题目 ${index + 1}`" placeholder="请输入题目" required maxlength="255" /><Button v-if="form.questions.length > 1" variant="ghost" size="sm" :aria-label="`移除题目 ${index + 1}`" @click="form.questions.splice(index, 1)">移除</Button></div><div class="flex flex-wrap items-center gap-4"><Select v-model="q.type" :aria-label="`题型 ${index + 1}`" :options="questionTypes" class="w-40" /><Checkbox v-model="q.required">必答</Checkbox></div><div v-if="['single', 'multiple'].includes(q.type)"><label :for="`survey-options-${index}`" class="field-label">选项（每行一项）*</label><Textarea :id="`survey-options-${index}`" v-model="q.optionsText" :rows="3" required /></div></div><Button v-if="form.questions.length < 30" variant="outline" size="sm" @click="form.questions.push(newQuestion())">添加题目</Button></fieldset></form><template #footer><Button variant="outline" :disabled="busy" @click="opened = false">取消</Button><Button type="submit" form="survey-edit-form" :loading="busy">保存草稿</Button></template></Dialog>
    <Dialog :open="!!answering" :title="answering?.title || '填写问卷'" size="lg" @update:open="value => { if (!value && !busy) answering = null }"><form id="survey-answer-form" class="space-y-5" @submit.prevent="submitAnswers"><p class="text-sm leading-6 text-muted-foreground">{{ answering?.data.description }}</p><fieldset v-for="(q, index) in answering?.data.questions" :key="q.id" class="space-y-3"><legend class="mb-2 text-sm font-medium">{{ index + 1 }}. {{ q.title }}{{ q.required ? ' *' : '' }}</legend><Textarea v-if="q.type === 'text'" v-model="answers[q.id]" :aria-label="q.title" :required="q.required" maxlength="4000" :rows="3" /><Select v-else-if="q.type !== 'multiple'" v-model="answers[q.id]" :aria-label="q.title" placeholder="请选择" :options="q.type === 'rating' ? [1, 2, 3, 4, 5].map(value => ({ value: String(value), label: `${value} 分` })) : q.options.map(value => ({ value, label: value }))" /><div v-else class="flex flex-col gap-3"><Checkbox v-for="option in q.options" :key="option" :model-value="(answers[q.id] || []).includes(option)" @update:model-value="checked => selectAnswer(q.id, option, checked)">{{ option }}</Checkbox></div></fieldset><Callout v-if="formError" tone="destructive">{{ formError }}</Callout></form><template #footer><Button variant="outline" :disabled="busy" @click="answering = null">取消</Button><Button type="submit" form="survey-answer-form" :loading="busy">提交问卷</Button></template></Dialog>
    <Dialog :open="!!pendingAction" title="确认问卷操作" @update:open="value => { if (!value && !busy) pendingAction = null }"><p class="text-sm leading-7">{{ actionDescription }}</p><Callout v-if="formError" tone="destructive" class="mt-3">{{ formError }}</Callout><template #footer><Button variant="outline" :disabled="busy" @click="pendingAction = null">取消</Button><Button :loading="busy" @click="runAction">确认</Button></template></Dialog>
    <Dialog :open="resultOpen" :title="result?.survey.title || '调研结果'" size="lg" @update:open="resultOpen = $event"><p v-if="resultLoading" role="status" class="text-sm text-muted-foreground">正在读取调研结果…</p><Callout v-if="resultError" tone="destructive">{{ resultError }}</Callout><template v-if="result"><p class="mb-4 text-sm text-muted-foreground">共 {{ result.responses }} 份有效答卷</p><section v-for="(q, index) in result.questions" :key="q.id" class="border-t py-4"><h3 class="text-sm font-semibold">{{ index + 1 }}. {{ q.title }}</h3><p v-if="q.average != null" class="mt-2 text-sm text-primary">平均 {{ q.average }} 分</p><ul v-if="q.type === 'text'" class="mt-3 list-inside list-disc space-y-2 text-sm"><li v-for="(text, i) in q.texts" :key="i" class="whitespace-pre-wrap break-words leading-6">{{ text }}</li><li v-if="!q.texts?.length" class="text-muted-foreground">暂无文字回答</li></ul><ul v-else class="mt-3 space-y-3"><li v-for="option in resultOptions(q)" :key="option" class="flex items-center gap-3 text-sm"><span class="min-w-0 flex-1 break-words">{{ option }}</span><b class="tabular-nums">{{ q.counts?.[option] || 0 }}</b></li></ul></section></template></Dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { workspaceApi, workspaceStatus, type Survey, type SurveyQuestion } from '../../../lib/workspace-api';
import { dateText, todayShanghai } from '../../../lib/hr-workspace';
import { errText, toast } from '../../../lib/toast';
import Button from '../../../ui/Button.vue';
import Input from '../../../ui/Input.vue';
import Select from '../../../ui/Select.vue';
import MultiSelect from '../../../ui/MultiSelect.vue';
import Textarea from '../../../ui/Textarea.vue';
import Checkbox from '../../../ui/Checkbox.vue';
import Badge from '../../../ui/Badge.vue';
import Callout from '../../../ui/Callout.vue';
import EmptyState from '../../../ui/EmptyState.vue';
import Dialog from '../../../ui/Dialog.vue';
const props = defineProps<{ manage: boolean; view: string }>();
const rows = ref<Survey[]>([]), loading = ref(false), error = ref(''), busy = ref(false), formError = ref('');
const users = ref<{ value: string; label: string }[]>([]), opened = ref(false), editing = ref<Survey | null>(null), answering = ref<Survey | null>(null);
const answers = reactive<Record<string, any>>({});
const questionTypes = [{ value: 'rating', label: '评分（1–5分）' }, { value: 'single', label: '单选题' }, { value: 'multiple', label: '多选题' }, { value: 'text', label: '问答题' }];
let questionId = 0;
const newQuestion = () => ({ id: `q_${Date.now()}_${++questionId}`, title: '', type: 'rating', required: true, optionsText: '' });
const form = reactive({ title: '', description: '', startAt: '', endAt: '', scope: 'all', targetIds: [] as string[], anonymous: true, questions: [newQuestion()] });
const pendingAction = ref<{ survey: Survey; action: 'publish' | 'close' | 'remind' } | null>(null);
const actionDescription = computed(() => pendingAction.value?.action === 'publish' ? `发布「${pendingAction.value.survey.title}」后题目将锁定，并通知范围内的员工。` : pendingAction.value?.action === 'close' ? '结束收集后，员工将无法继续提交答卷。' : '向尚未填写此问卷的员工发送站内提醒。');
const result = ref<Awaited<ReturnType<typeof workspaceApi.surveyResults>> | null>(null), resultOpen = ref(false), resultLoading = ref(false), resultError = ref('');
let loadRequest = 0;
async function load() { const id = ++loadRequest; loading.value = true; error.value = ''; try { const data = await workspaceApi.surveys(props.manage); if (id === loadRequest) rows.value = data; } catch (e) { if (id === loadRequest) { rows.value = []; error.value = errText(e); } } finally { if (id === loadRequest) loading.value = false; } }
async function edit(survey?: Survey) { editing.value = survey || null; formError.value = ''; const day = todayShanghai(); Object.assign(form, survey ? { title: survey.title, ...survey.data, targetIds: [...(survey.data.targetIds || [])], questions: survey.data.questions.map(q => ({ ...q, optionsText: q.options.join('\n') })) } : { title: '', description: '', startAt: `${day}T09:00`, endAt: `${day}T18:00`, scope: 'all', targetIds: [], anonymous: true, questions: [newQuestion()] }); opened.value = true; try { users.value = await workspaceApi.users(); } catch (e) { formError.value = errText(e); } }
async function save() { if (busy.value) return; busy.value = true; formError.value = ''; try { await workspaceApi.saveSurvey(editing.value?.id, { ...form, version: editing.value?.version, questions: form.questions.map(q => ({ id: q.id, title: q.title, type: q.type, required: q.required, options: q.optionsText.split('\n').map(value => value.trim()).filter(Boolean) })) }); opened.value = false; toast('问卷草稿已保存'); await load(); } catch (e) { formError.value = errText(e); } finally { busy.value = false; } }
function answer(survey: Survey) { for (const key of Object.keys(answers)) delete answers[key]; answering.value = survey; formError.value = ''; }
function selectAnswer(id: string, option: string, checked: boolean) { const values = new Set<string>(answers[id] || []); if (checked) values.add(option); else values.delete(option); answers[id] = [...values]; }
async function submitAnswers() {
  if (!answering.value || busy.value) return;
  formError.value = '';
  const body: Record<string, unknown> = {};
  for (const q of answering.value.data.questions) {
    const value = answers[q.id];
    if (q.required && (value == null || value === '' || Array.isArray(value) && !value.length)) {
      formError.value = `请回答：${q.title}`;
      return;
    }
    if (value != null && value !== '') body[q.id] = q.type === 'rating' ? Number(value) : value;
  }
  busy.value = true;
  try {
    await workspaceApi.answerSurvey(answering.value.id, body);
    answering.value = null;
    toast('问卷已提交');
    await load();
  } catch (e) { formError.value = errText(e); }
  finally { busy.value = false; }
}
function confirmAction(survey: Survey, action: 'publish' | 'close' | 'remind') { pendingAction.value = { survey, action }; formError.value = ''; }
async function runAction() { if (!pendingAction.value || busy.value) return; busy.value = true; formError.value = ''; try { const { survey, action } = pendingAction.value; const count = await workspaceApi.surveyAction(survey.id, action); pendingAction.value = null; toast(action === 'publish' ? '问卷已发布' : action === 'close' ? '问卷已结束收集' : `已提醒 ${count} 名未填写员工`); await load(); } catch (e) { formError.value = errText(e); } finally { busy.value = false; } }
let resultRequest = 0;
async function results(survey: Survey) { const id = ++resultRequest; result.value = null; resultOpen.value = true; resultLoading.value = true; resultError.value = ''; try { const data = await workspaceApi.surveyResults(survey.id); if (id === resultRequest) result.value = data; } catch (e) { if (id === resultRequest) resultError.value = errText(e); } finally { if (id === resultRequest) resultLoading.value = false; } }
const resultOptions = (q: SurveyQuestion) => q.type === 'rating' ? ['1', '2', '3', '4', '5'] : q.options;
watch(() => props.manage, load, { immediate: true });
</script>
