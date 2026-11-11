<template>
  <div class="min-w-0 space-y-4">
    <PageHeader
      title="入离职与合同"
      description="填写资料、核对文书并确认签订，办理结果写入员工档案。"
    />
    <HrEmploymentNavigation />
    <ol class="grid grid-cols-3 gap-2 text-xs" aria-label="办理步骤">
      <li
        v-for="(label, i) in ['填写资料', '生成并核对文书', '确认签订与归档']"
        :key="label"
        class="rounded-lg border px-3 py-3"
        :class="
          step >= i + 1
            ? 'border-primary/40 bg-primary/5 text-primary'
            : 'text-muted-foreground'
        "
      >
        {{ i + 1 }}. {{ label }}
      </li>
    </ol>
    <Callout v-if="error" tone="destructive">
      {{ error }}
      <template #action>
        <Button variant="outline" size="sm" @click="loadConfig">
          重新读取配置
        </Button>
      </template>
    </Callout>
    <section v-if="result" class="panel space-y-4 p-6">
      <h2 class="text-xl font-semibold">
        {{ mode === 'onboard' ? '入职已完成' : '离职手续已登记' }}
      </h2>
      <p class="text-sm text-muted-foreground">
        文书已归档至员工档案。{{
          mode === 'offboard'
            ? '账号将在离职生效时停用，未来日期由定时任务处理。'
            : '可在花名册查看员工任职状态。'
        }}
      </p>
      <div class="flex flex-wrap gap-2">
        <Button
          @click="
            router.push(`/page/hrm-employee?employeeId=${result.employeeId}`)
          "
        >
          查看员工档案
        </Button>
        <Button
          variant="outline"
          @click="router.push(`/hr/lifecycle?employeeId=${result.employeeId}`)"
        >
          查看生命周期
        </Button>
        <Button variant="outline" @click="downloadArchived">
          下载归档文书
        </Button>
        <Button variant="ghost" @click="reset">继续办理</Button>
      </div>
    </section>
    <form v-else class="space-y-4" @submit.prevent="preview">
      <section class="panel p-5">
        <h2 class="panel-title">
          {{ mode === 'onboard' ? '员工信息' : '离职员工' }}
        </h2>
        <div v-if="mode === 'onboard'" class="mt-4">
          <label for="employment-source" class="field-label">档案来源</label>
          <Select
            id="employment-source"
            v-model="source"
            :disabled="busy"
            :options="sourceOptions"
            @update:model-value="clearPerson"
          />
        </div>
        <div v-if="source !== 'new' || mode === 'offboard'" class="mt-4">
          <label for="employment-search" class="field-label">
            {{
              source === 'candidate' && mode === 'onboard'
                ? '搜索已通过面试 / 已发 offer 的候选人'
                : '搜索员工姓名或工号'
            }}
          </label>
          <div class="flex gap-2">
            <Input id="employment-search" v-model="personSearch" />
            <Button variant="outline" :disabled="busy" @click="findPeople">
              搜索
            </Button>
          </div>
          <Select
            v-model="personId"
            class="mt-2"
            :disabled="busy"
            :options="personOptions"
            placeholder="请选择办理对象"
            aria-label="办理对象"
            @update:model-value="choosePerson"
          />
        </div>
        <div
          v-if="mode === 'onboard'"
          class="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-3"
        >
          <div>
            <label for="employment-name" class="field-label">员工姓名 *</label>
            <Input
              id="employment-name"
              v-model="form.name"
              required
              maxlength="255"
              :disabled="source !== 'new'"
            />
          </div>
          <div>
            <label for="employment-number" class="field-label">工号 *</label>
            <Input
              id="employment-number"
              v-model="form.jobNumber"
              required
              maxlength="64"
            />
          </div>
          <div>
            <label for="employment-mobile" class="field-label">手机号</label>
            <Input
              id="employment-mobile"
              v-model="form.mobile"
              type="tel"
              :disabled="source === 'pending'"
            />
          </div>
          <div>
            <label for="employment-dept" class="field-label">所属部门 *</label>
            <Select
              id="employment-dept"
              v-model="form.deptId"
              :options="departments"
              :disabled="source === 'pending'"
            />
          </div>
          <div>
            <label for="employment-post" class="field-label">入职岗位 *</label>
            <Input
              id="employment-post"
              v-model="form.postName"
              required
              maxlength="255"
              :disabled="source === 'pending'"
            />
          </div>
          <div>
            <label for="employment-entry" class="field-label">入职日期 *</label>
            <Input
              id="employment-entry"
              v-model="form.entryDate"
              type="date"
              required
              :max="today"
              :disabled="source === 'pending'"
            />
          </div>
          <div>
            <label for="employment-type" class="field-label">聘用形式 *</label>
            <Select
              id="employment-type"
              v-model="form.employeeType"
              :options="[
                { value: '1', label: '正式聘用' },
                { value: '2', label: '非正式聘用' }
              ]"
            />
          </div>
          <div v-if="form.employeeType === '2'">
            <label for="employment-status" class="field-label">
              员工类型 *
            </label>
            <Select
              id="employment-status"
              v-model="form.employeeStatus"
              :options="[
                { value: '3', label: '实习' },
                { value: '4', label: '兼职' },
                { value: '5', label: '劳务' },
                { value: '6', label: '顾问' },
                { value: '7', label: '返聘' },
                { value: '8', label: '外包' }
              ]"
            />
          </div>
          <div v-if="form.employeeType === '1'">
            <label for="employment-probation" class="field-label">
              试用期（月）*
            </label>
            <Input
              id="employment-probation"
              v-model="form.probation"
              type="number"
              min="0"
              max="6"
              step="1"
              required
            />
          </div>
          <div>
            <label for="employment-salary" class="field-label">
              税前月薪（元）*
            </label>
            <Input
              id="employment-salary"
              v-model="form.salary"
              type="number"
              min="0.01"
              max="99999999.99"
              step="0.01"
              required
            />
          </div>
          <div>
            <label for="employment-end" class="field-label">
              合同结束日期 *
            </label>
            <Input
              id="employment-end"
              v-model="form.endDate"
              type="date"
              :min="form.entryDate"
              required
            />
          </div>
        </div>
        <div v-else class="mt-4 grid gap-4 sm:grid-cols-2">
          <div>
            <label for="offboard-date" class="field-label">离职日期 *</label>
            <Input
              id="offboard-date"
              v-model="form.leaveDate"
              type="date"
              required
            />
          </div>
          <div>
            <label for="offboard-apply" class="field-label">
              申请离职日期 *
            </label>
            <Input
              id="offboard-apply"
              v-model="form.applyDate"
              type="date"
              :max="form.leaveDate"
              required
            />
          </div>
          <div>
            <label for="offboard-settlement" class="field-label">
              薪资结算日期 *
            </label>
            <Input
              id="offboard-settlement"
              v-model="form.settlementDate"
              type="date"
              :min="form.leaveDate"
              required
            />
          </div>
          <div>
            <label for="offboard-type" class="field-label">离职类型 *</label>
            <Select
              id="offboard-type"
              v-model="form.leaveType"
              :options="[
                { value: '1', label: '主动离职' },
                { value: '2', label: '被动离职' },
                { value: '3', label: '退休' }
              ]"
              @update:model-value="form.reasonCode = ''"
            />
          </div>
          <div v-if="form.leaveType !== '3'">
            <label for="offboard-reason-code" class="field-label">
              原因分类 *
            </label>
            <Select
              id="offboard-reason-code"
              v-model="form.reasonCode"
              :options="quitReasons"
            />
          </div>
          <div class="sm:col-span-2">
            <label for="offboard-reason" class="field-label">离职原因 *</label>
            <Textarea
              id="offboard-reason"
              v-model="form.reason"
              required
              maxlength="500"
            />
          </div>
        </div>
      </section>
      <section class="panel p-5">
        <h2 class="panel-title">签约信息与合同模板</h2>
        <p v-if="loading" class="mt-3 text-sm text-muted-foreground">
          正在读取配置…
        </p>
        <div class="mt-4 grid gap-4 sm:grid-cols-2">
          <div>
            <label for="employment-company" class="field-label">
              公司名称 *
            </label>
            <Input
              id="employment-company"
              v-model="form.company"
              required
              maxlength="100"
            />
          </div>
          <div>
            <label for="employment-template" class="field-label">
              合同模板 *
            </label>
            <Select
              id="employment-template"
              v-model="form.templateId"
              :options="
                approvedTemplates.map((t) => ({
                  value: String(t.id),
                  label: `${t.name} · ${t.version}`
                }))
              "
              placeholder="选择审核通过的模板"
            />
          </div>
          <template v-if="mode === 'onboard'">
            <div>
              <label for="employment-code" class="field-label">
                统一社会信用代码 *
              </label>
              <Input
                id="employment-code"
                v-model="form.companyCode"
                required
                maxlength="100"
              />
            </div>
            <div>
              <label for="employment-address" class="field-label">
                注册地址 *
              </label>
              <Input
                id="employment-address"
                v-model="form.companyAddress"
                required
                maxlength="255"
              />
            </div>
          </template>
          <div>
            <label for="employment-sign" class="field-label">签订日期 *</label>
            <Input
              id="employment-sign"
              v-model="form.signDate"
              type="date"
              required
            />
          </div>
        </div>
        <Callout
          v-if="!loading && !error && !approvedTemplates.length"
          class="mt-4"
          tone="warning"
        >
          尚无审核通过的{{ mode === 'onboard' ? '入职合同' : '离职协议' }}模板。
          <RouterLink to="/hr/templates" class="underline">
            前往合同模板
          </RouterLink>
        </Callout>
      </section>
      <div class="flex flex-wrap gap-3">
        <Button
          type="submit"
          :loading="busy"
          :disabled="loading || !approvedTemplates.length"
        >
          生成并核对{{ mode === 'onboard' ? '合同' : '离职协议' }}
        </Button>
        <p class="self-center text-xs text-muted-foreground">
          确认签订后同步更新花名册，并保存本次文书。
        </p>
      </div>
    </form>
    <Dialog
      :open="previewOpen"
      :title="mode === 'onboard' ? '入职合同预览' : '离职协议预览'"
      size="lg"
      @update:open="!busy && (previewOpen = $event)"
    >
      <pre
        class="whitespace-pre-wrap break-words font-sans text-sm leading-7"
        >{{ previewContent }}</pre
      >
      <Checkbox v-model="signed" class="mt-5">
        已核对文书，并已完成双方签订
      </Checkbox>
      <Callout v-if="previewError" class="mt-3" tone="destructive">
        {{ previewError }}
      </Callout>
      <template #footer>
        <Button
          variant="outline"
          @click="
            textDownload(
              `${form.name || '员工'}-${mode === 'onboard' ? '合同' : '离职协议'}.txt`,
              previewContent
            )
          "
        >
          下载
        </Button>
        <Button variant="outline" @click="printDocument">
          打印 / 存为 PDF
        </Button>
        <Button :loading="busy" :disabled="!signed" @click="confirm">
          确认签订并归档
        </Button>
      </template>
    </Dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../../lib/api';
import { session } from '../../lib/session';
import {
  getTemplates,
  hrBase,
  textDownload,
  todayShanghai,
  type Candidate,
  type HrEmployee,
  type HrPage,
  type HrTemplate
} from '../../lib/hr-workspace';
import { errText } from '../../lib/toast';

import Button from '../../ui/Button.vue';
import Callout from '../../ui/Callout.vue';
import Checkbox from '../../ui/Checkbox.vue';
import Dialog from '../../ui/Dialog.vue';
import Input from '../../ui/Input.vue';
import PageHeader from '../../ui/PageHeader.vue';
import HrEmploymentNavigation from '../../components/HrEmploymentNavigation.vue';
import Select from '../../ui/Select.vue';
import Textarea from '../../ui/Textarea.vue';
const route = useRoute(),
  router = useRouter(),
  mode = computed(() =>
    route.query.mode === 'offboard' ? 'offboard' : 'onboard'
  ),
  today = todayShanghai();
const source = ref('new'),
  sourceOptions = computed(() =>
    [
      {
        value: 'new',
        label: '新增未建档员工',
        permission: 'hrm:employee:create'
      },
      {
        value: 'pending',
        label: '已有待入职档案',
        permission: 'hrm:employee:query'
      },
      {
        value: 'candidate',
        label: '招聘候选人转入职',
        permission: 'hrm:recruit:candidate:update'
      }
    ].filter((s) => session.has(s.permission))
  );
const defaults = () => ({
  name: '',
  jobNumber: '',
  mobile: '',
  deptId: '',
  postName: '',
  entryDate: today,
  probation: '3',
  employeeType: '1',
  employeeStatus: '3',
  salary: '',
  endDate: `${Number(today.slice(0, 4)) + 3}${today.slice(4)}`,
  company: '',
  companyCode: '',
  companyAddress: '',
  templateId: '',
  signDate: today,
  leaveDate: today,
  applyDate: today,
  settlementDate: today,
  leaveType: '1',
  reasonCode: '',
  reason: ''
});
const form = reactive(defaults()),
  templates = ref<HrTemplate[]>([]),
  departments = ref<{ value: string; label: string }[]>([]),
  people = ref<(HrEmployee | Candidate)[]>([]),
  original = ref<HrEmployee | null>(null),
  personId = ref(''),
  personSearch = ref(''),
  reasonDict = ref<{ value: string; label: string }[]>([]);
const approvedTemplates = computed(() =>
  templates.value.filter(
    (t) => t.status === 'approved' && t.kind === mode.value
  )
);
const quitReasons = computed(() =>
  reasonDict.value.filter((d) =>
    form.leaveType === '1' ? Number(d.value) <= 10 : Number(d.value) > 10
  )
);
const personOptions = computed(() =>
  people.value.map((p) => ({
    value: String(p.id),
    label: `${p.name} · ${p.postName || p.deptName || ''}`
  }))
);
const loading = ref(false),
  busy = ref(false),
  error = ref(''),
  previewError = ref(''),
  previewOpen = ref(false),
  previewContent = ref(''),
  signed = ref(false),
  step = ref(1),
  result = ref<{ employeeId: string; documentId: string } | null>(null);
let requestKey = crypto.randomUUID(),
  previewFingerprint = '',
  selectionRequest = 0;
async function loadConfig() {
  loading.value = true;
  error.value = '';
  const results = await Promise.allSettled([
    getTemplates(),
    api.get<typeof departments.value>(`${hrBase}/departments`),
    api.get<typeof reasonDict.value>(hrBase + '/quit-reasons')
  ]);
  for (const [i, r] of results.entries()) {
    if (r.status === 'rejected') error.value = errText(r.reason);
    else if (i === 0) templates.value = r.value as HrTemplate[];
    else if (i === 1) departments.value = r.value as typeof departments.value;
    else reasonDict.value = r.value as typeof reasonDict.value;
  }
  loading.value = false;
}
function reset() {
  Object.assign(form, defaults());
  result.value = null;
  step.value = 1;
  previewContent.value = '';
  previewFingerprint = '';
  original.value = null;
  personId.value = '';
  personSearch.value = '';
  people.value = [];
  requestKey = crypto.randomUUID();
  source.value = sourceOptions.value[0]?.value || 'pending';
  error.value = '';
}
function clearPerson() {
  ++selectionRequest;
  original.value = null;
  personId.value = '';
  people.value = [];
  form.name = '';
  form.mobile = '';
  form.jobNumber = '';
  form.deptId = '';
  form.postName = '';
  if (source.value !== 'new' || mode.value === 'offboard') void findPeople();
}
async function findPeople() {
  const request = ++selectionRequest;
  error.value = '';
  try {
    const candidate = mode.value === 'onboard' && source.value === 'candidate';
    const r = await api.get<HrPage<HrEmployee | Candidate>>(
      candidate
        ? '/admin-api/hrm/recruit/candidate/page'
        : '/admin-api/hrm/employee/page',
      {
        pageNo: 1,
        pageSize: 50,
        search: personSearch.value || undefined,
        entryStatus: candidate ? undefined : mode.value === 'onboard' ? 2 : 1
      }
    );
    if (request === selectionRequest)
      people.value = candidate
        ? (r.list as Candidate[]).filter((c) => [4, 5].includes(c.status))
        : r.list;
  } catch (e) {
    if (request === selectionRequest) error.value = errText(e);
  }
}
async function choosePerson(value: unknown) {
  const id = String(value || '');
  if (!id) return;
  const request = ++selectionRequest;
  error.value = '';
  try {
    if (mode.value === 'onboard' && source.value === 'candidate') {
      const c = await api.get<Candidate>(
        '/admin-api/hrm/recruit/candidate/get',
        { id }
      );
      if (request !== selectionRequest) return;
      if (![4, 5].includes(c.status))
        throw new Error('该候选人尚未通过面试或已办理入职');
      form.name = c.name;
      form.mobile = c.mobile;
      form.postName = c.postName;
      const post = await api.get<{ deptId: string }>(
        '/admin-api/hrm/recruit/post/get',
        { id: c.postId }
      );
      if (request === selectionRequest) form.deptId = String(post.deptId || '');
    } else {
      const e = await api.get<HrEmployee>('/admin-api/hrm/employee/get', {
        id
      });
      if (request !== selectionRequest) return;
      original.value = e;
      Object.assign(form, {
        name: e.name,
        jobNumber: e.jobNumber || '',
        mobile: e.mobile || '',
        deptId: String(e.deptId || ''),
        postName: e.postName || '',
        entryDate: e.entryTime?.slice(0, 10) || today,
        probation: String(e.probation ?? 3),
        employeeType: String(e.type || 1),
        employeeStatus: String(e.status && e.status > 2 ? e.status : 3)
      });
    }
  } catch (e) {
    if (request === selectionRequest) {
      original.value = null;
      error.value = errText(e);
    }
  }
}
function payload() {
  if (!form.templateId) throw new Error('请选择审核通过的模板');
  if (mode.value === 'offboard') {
    if (!original.value || original.value.id !== personId.value)
      throw new Error('请选择在职员工');
    if (form.leaveType !== '3' && !form.reasonCode)
      throw new Error('请选择离职原因分类');
    return {
      requestKey,
      templateId: Number(form.templateId),
      company: form.company,
      reason: form.reason,
      signDate: form.signDate,
      quit: {
        employeeId: personId.value,
        type: Number(form.leaveType),
        reason: form.leaveType === '3' ? null : Number(form.reasonCode),
        planQuitTime: `${form.leaveDate}T00:00:00`,
        applyQuitTime: `${form.applyDate}T00:00:00`,
        salarySettlementTime: `${form.settlementDate}T00:00:00`
      }
    };
  }
  if (source.value !== 'new' && !personId.value)
    throw new Error('请选择员工或候选人');
  if (
    source.value === 'pending' &&
    (!original.value || original.value.id !== personId.value)
  )
    throw new Error('请重新选择待入职员工');
  if (!form.deptId || !form.postName) throw new Error('请补全部门和岗位');
  return {
    requestKey,
    employee: {
      ...(source.value === 'pending' ? original.value : {}),
      id: source.value === 'pending' ? personId.value : null,
      candidateId: source.value === 'candidate' ? personId.value : undefined,
      name: form.name,
      jobNumber: form.jobNumber,
      mobile: form.mobile || null,
      deptId: form.deptId,
      postName: form.postName,
      entryTime:
        source.value === 'pending'
          ? original.value?.entryTime
          : `${form.entryDate}T00:00:00`,
      probation: form.employeeType === '1' ? Number(form.probation) : 0,
      entryStatus: 1,
      type: Number(form.employeeType),
      status:
        form.employeeType === '1'
          ? Number(form.probation)
            ? 2
            : 1
          : Number(form.employeeStatus)
    },
    document: {
      templateId: Number(form.templateId),
      company: form.company,
      companyCode: form.companyCode,
      companyAddress: form.companyAddress,
      signDate: form.signDate,
      endDate: form.endDate,
      salary: form.salary
    }
  };
}
async function preview() {
  if (busy.value) return;
  busy.value = true;
  error.value = '';
  previewError.value = '';
  try {
    const body = payload(),
      fingerprint = JSON.stringify(body);
    const r = await api.post<{ content: string }>(
      `${hrBase}/${mode.value}/preview`,
      body
    );
    if (fingerprint !== JSON.stringify(payload()))
      throw new Error('资料已修改，请重新生成文书');
    previewContent.value = r.content;
    previewFingerprint = fingerprint;
    signed.value = false;
    previewOpen.value = true;
    step.value = 2;
  } catch (e) {
    error.value = errText(e);
  } finally {
    busy.value = false;
  }
}
async function confirm() {
  if (busy.value || !signed.value) return;
  busy.value = true;
  previewError.value = '';
  try {
    const body = payload();
    if (JSON.stringify(body) !== previewFingerprint)
      throw new Error('资料已变化，请关闭预览并重新生成文书');
    result.value = await api.post(`${hrBase}/${mode.value}`, body);
    previewOpen.value = false;
    step.value = 3;
  } catch (e) {
    previewError.value = errText(e);
  } finally {
    busy.value = false;
  }
}
async function downloadArchived() {
  if (!result.value) return;
  try {
    const doc = await api.get<{ content: string }>(
      `${hrBase}/documents/${result.value.documentId}`
    );
    textDownload(`${form.name}-归档文书.txt`, doc.content);
  } catch (e) {
    error.value = errText(e);
  }
}
function printDocument() {
  const w = window.open('', '_blank', 'width=850,height=1000');
  if (!w) {
    previewError.value = '请允许弹出打印窗口后重试';
    return;
  }
  w.opener = null;
  w.document.title = `${form.name}-文书`;
  const pre = w.document.createElement('pre');
  pre.style.cssText =
    'white-space:pre-wrap;overflow-wrap:anywhere;font:14px/2 sans-serif;margin:40px';
  pre.textContent = previewContent.value;
  w.document.body.append(pre);
  w.focus();
  w.print();
}
watch(mode, () => {
  reset();
  if (mode.value === 'offboard') void findPeople();
});
onMounted(async () => {
  source.value = sourceOptions.value[0]?.value || 'pending';
  await loadConfig();
  if (route.query.employeeId) {
    source.value = 'pending';
    personId.value = String(route.query.employeeId);
    await choosePerson(personId.value);
    if (original.value) people.value = [original.value];
  } else if (route.query.candidateId && mode.value === 'onboard') {
    source.value = 'candidate';
    personId.value = String(route.query.candidateId);
    await choosePerson(personId.value);
    const c = await api
      .get<Candidate>('/admin-api/hrm/recruit/candidate/get', {
        id: personId.value
      })
      .catch(() => null);
    if (c) people.value = [c];
  } else if (mode.value === 'offboard' || source.value === 'pending')
    await findPeople();
});
</script>
