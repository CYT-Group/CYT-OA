<template>
  <div class="min-w-0 space-y-5">
    <PageHeader :title="flowMode ? '门户审批流程' : scopeMode ? '数据权限' : '考勤与报销规则'" :description="flowMode ? '配置门户申请的审批节点，按顺序流转到负责人或指定员工。' : scopeMode ? '配置不同角色在目标与门户审批中的数据范围。' : '维护门户申请使用的工作时间、请假额度和报销标准。'" />
    <Callout v-if="!flowMode && !scopeMode">工作时间与额度用于门户申请校验。实际打卡排班继续由考勤分组管理。</Callout>
    <Callout v-if="scopeMode">以下范围用于目标与门户审批；首页和工作报告继续依据现有组织关系展示。</Callout>
    <nav class="flex flex-wrap gap-1 rounded-xl border bg-card p-1" aria-label="规则分类"><RouterLink v-for="tab in tabs" :key="tab.value" :to="{ path: route.path, query: { section: section, tab: tab.value } }" class="rounded-lg px-3 py-2 text-xs font-medium focus-visible:ring-2 focus-visible:ring-ring" :class="selected === tab.value ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-accent'" :aria-current="selected === tab.value ? 'page' : undefined">{{ tab.label }}</RouterLink></nav>
    <Callout v-if="error" tone="destructive">{{ error }}<template #action><Button size="sm" variant="outline" @click="load">重新加载</Button></template></Callout>
    <p v-if="loading" class="panel p-8 text-sm text-muted-foreground" role="status">正在读取配置…</p>
    <form v-else-if="loaded" class="panel space-y-5 p-5 sm:p-6" @submit.prevent="save">
      <template v-if="flowMode"><div class="flex items-center gap-3"><Switch id="flow-enabled" v-model="form.enabled" /><label for="flow-enabled" class="text-sm font-medium">启用{{ currentTitle }}流程</label></div><div><label for="flow-description" class="field-label">办理说明</label><Textarea id="flow-description" v-model="form.description" :rows="3" maxlength="2000" /></div><fieldset class="space-y-3"><legend class="mb-2 text-sm font-semibold">审批顺序</legend><p v-if="!nodes.length" class="text-sm text-muted-foreground">添加至少一个节点后可启用流程。</p><div v-for="(node, index) in nodes" :key="index" class="flex flex-wrap items-end gap-3 rounded-lg border p-4"><div class="min-w-32 flex-1"><label :for="`node-type-${index}`" class="field-label">第 {{ index + 1 }} 级审批</label><Select :id="`node-type-${index}`" v-model="node.type" :options="[{ value: 'leader', label: '申请人的直属上级' }, { value: 'user', label: '指定员工' }]" /></div><div v-if="node.type === 'user'" class="min-w-40 flex-1"><label :for="`node-user-${index}`" class="field-label">审批人 *</label><Select :id="`node-user-${index}`" v-model="node.userId" :options="users" placeholder="选择审批人" /></div><Button variant="ghost" size="sm" :aria-label="`移除第 ${index + 1} 级审批`" @click="nodes.splice(index, 1)">移除</Button></div><Button v-if="nodes.length < 10" variant="outline" size="sm" @click="nodes.push({ type: 'leader', userId: '' })">添加审批节点</Button></fieldset></template>
      <template v-else-if="scopeMode"><fieldset v-for="(rule, index) in scopeRules" :key="index" class="space-y-3 rounded-lg border p-4"><legend class="px-1 text-sm font-semibold">角色 {{ index + 1 }}</legend><div class="grid gap-4 sm:grid-cols-3"><div><label :for="`scope-role-${index}`" class="field-label">角色标识 *</label><Input :id="`scope-role-${index}`" v-model.trim="rule.role" required pattern="[a-z][a-z0-9_-]{1,63}" /></div><div v-for="key in ['okr', 'approvals'] as const" :key="key"><label :for="`scope-${index}-${key}`" class="field-label">{{ key === 'okr' ? '目标' : '门户审批' }}</label><Select :id="`scope-${index}-${key}`" v-model="rule[key]" :options="scopeOptions" /></div></div><Button variant="ghost" size="sm" @click="scopeRules.splice(index, 1)">移除此角色规则</Button></fieldset><Button variant="outline" size="sm" @click="scopeRules.push({ role: '', okr: 'self', approvals: 'self', home: 'self', reports: 'self' })">添加角色规则</Button></template>
      <template v-else-if="selected === 'attendance'"><div class="grid gap-4 sm:grid-cols-2"><div><label for="rule-start" class="field-label">上班时间 *</label><Input id="rule-start" v-model="form.startTime" type="time" required /></div><div><label for="rule-end" class="field-label">下班时间 *</label><Input id="rule-end" v-model="form.endTime" type="time" required /></div><div><label for="rule-late" class="field-label">迟到宽限（分钟）</label><Input id="rule-late" v-model="form.lateMinutes" type="number" min="0" max="120" required /></div><div><label for="rule-early" class="field-label">早退宽限（分钟）</label><Input id="rule-early" v-model="form.earlyMinutes" type="number" min="0" max="120" required /></div></div><div class="flex flex-wrap gap-6"><Checkbox v-model="form.allowRemote">允许远程办公</Checkbox><Checkbox v-model="form.overtimeApproval">加班需要审批</Checkbox></div></template>
      <template v-else><div class="grid gap-4 sm:grid-cols-2"><div v-for="field in fields" :key="field.key"><label :for="`policy-${field.key}`" class="field-label">{{ field.label }}{{ selected === 'leave' ? '（天/年）' : '（元/次）' }}</label><Input :id="`policy-${field.key}`" v-model="form[field.key]" type="number" min="0" :max="selected === 'leave' ? 366 : 100000" :step="selected === 'leave' ? 0.5 : 0.01" required /></div></div><p v-if="selected === 'expense'" class="text-xs text-muted-foreground">币种：人民币；标准用于门户单次报销申请校验。</p></template>
      <Callout v-if="saveError" tone="destructive">{{ saveError }}</Callout><div class="flex justify-end gap-2 border-t pt-5"><Button variant="outline" :disabled="saving" @click="load">重新读取</Button><Button type="submit" :loading="saving">保存配置</Button></div>
    </form>
  </div>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { workspaceApi } from '../../lib/workspace-api';
import { errText, toast } from '../../lib/toast';
import PageHeader from '../../ui/PageHeader.vue';
import Button from '../../ui/Button.vue';
import Input from '../../ui/Input.vue';
import Select from '../../ui/Select.vue';
import Switch from '../../ui/Switch.vue';
import Checkbox from '../../ui/Checkbox.vue';
import Textarea from '../../ui/Textarea.vue';
import Callout from '../../ui/Callout.vue';
const route = useRoute();
const section = computed(() => route.query.section === 'flow' ? 'flow' : route.query.section === 'scope' ? 'scope' : 'policies');
const flowMode = computed(() => section.value === 'flow'), scopeMode = computed(() => section.value === 'scope');
const flowTabs = [{ value: 'leave', label: '请假' }, { value: 'expense', label: '报销' }, { value: 'travel', label: '出差' }, { value: 'purchase', label: '采购' }, { value: 'seal', label: '用章' }, { value: 'overtime', label: '加班' }];
const tabs = computed(() => flowMode.value ? flowTabs : scopeMode.value ? [{ value: 'data-scope', label: '角色数据范围' }] : [{ value: 'attendance', label: '考勤规则' }, { value: 'leave', label: '请假额度' }, { value: 'expense', label: '报销标准' }]);
const selected = computed(() => tabs.value.some(tab => tab.value === route.query.tab) ? String(route.query.tab) : tabs.value[0]!.value);
const policyKey = computed(() => flowMode.value ? `flow-${selected.value}` : selected.value);
const currentTitle = computed(() => tabs.value.find(tab => tab.value === selected.value)?.label);
const scopeOptions = [{ value: 'self', label: '仅本人' }, { value: 'team', label: '本人及直属团队' }, { value: 'department', label: '同部门' }, { value: 'company', label: '全公司' }];
const fields = computed(() => selected.value === 'leave' ? [{ key: 'annual', label: '年假' }, { key: 'sick', label: '病假' }, { key: 'personal', label: '事假' }, { key: 'compensatory', label: '调休' }] : [{ key: 'hotel', label: '差旅住宿' }, { key: 'transport', label: '差旅交通' }, { key: 'cityTransport', label: '市内交通' }, { key: 'hospitality', label: '业务招待' }]);
const form = reactive<Record<string, any>>({}), version = ref(0), nodes = ref<{ type: string; userId: string }[]>([]), scopeRules = ref<{ role: string; okr: string; approvals: string; home: string; reports: string }[]>([]);
const users = ref<{ value: string; label: string }[]>([]), loading = ref(false), loaded = ref(false), saving = ref(false), error = ref(''), saveError = ref('');
let request = 0;
async function load() { const id = ++request; loading.value = true; loaded.value = false; error.value = ''; saveError.value = ''; try { const [result, options] = await Promise.all([workspaceApi.policy(policyKey.value), flowMode.value ? workspaceApi.users() : Promise.resolve([])]); if (id !== request) return; for (const key of Object.keys(form)) delete form[key]; Object.assign(form, result.data); version.value = Number(result.version); users.value = options; nodes.value = ((result.data.nodes || []) as { type: string; userId?: string }[]).map(node => ({ type: node.type, userId: String(node.userId || '') })); scopeRules.value = scopeMode.value ? Object.entries(result.data).map(([role, raw]) => ({ role, ...(raw as { okr: string; approvals: string; home: string; reports: string }) })) : []; loaded.value = true; } catch (e) { if (id === request) error.value = errText(e); } finally { if (id === request) loading.value = false; } }
async function save() {
  if (saving.value) return;
  saveError.value = '';
  let data = { ...form };
  if (flowMode.value) data.nodes = nodes.value.map(node => ({ type: node.type, ...(node.type === 'user' ? { userId: node.userId } : {}) }));
  else if (scopeMode.value) {
    if (new Set(scopeRules.value.map(rule => rule.role)).size !== scopeRules.value.length) { saveError.value = '角色标识不能重复'; return; }
    data = Object.fromEntries(scopeRules.value.map(({ role, ...rule }) => [role, rule]));
  } else for (const field of selected.value === 'attendance' ? ['lateMinutes', 'earlyMinutes'] : fields.value.map(item => item.key)) data[field] = Number(data[field]);
  const key = policyKey.value;
  saving.value = true;
  try {
    await workspaceApi.savePolicy(key, { version: version.value, data });
    toast('配置已保存');
    if (key === policyKey.value) await load();
  } catch (e) { saveError.value = errText(e); }
  finally { saving.value = false; }
}
watch(policyKey, load, { immediate: true });
</script>
