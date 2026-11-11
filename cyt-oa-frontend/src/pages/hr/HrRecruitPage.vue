<template>
  <div class="min-w-0 space-y-4">
    <PageHeader
      title="招聘管理"
      description="从简历筛选、面试到 offer 与入职，持续跟进每一位候选人。"
    >
      <template #actions>
        <Button
          v-if="
            tab === 'candidates' && session.has('hrm:recruit:candidate:create')
          "
          @click="newCandidate"
        >
          新增候选人
        </Button>
        <Button
          v-if="
            tab === 'interviews' && session.has('hrm:recruit:interview:create')
          "
          @click="arrange()"
        >
          安排面试
        </Button>
        <Button
          v-if="tab === 'posts' && session.has('hrm:recruit:post:create')"
          @click="openPost"
        >
          新增职位
        </Button>
      </template>
    </PageHeader>
    <HrRecruitNavigation />
    <HrRecruitOverview ref="overview" :selected-status="tab === 'candidates' ? appliedStatus : ''" @select="selectStage" />
    <Callout v-if="error" tone="destructive">
      {{ error }}
      <template #action>
        <Button variant="outline" size="sm" @click="load">重试</Button>
      </template>
    </Callout>
    <section class="panel overflow-hidden">
      <form
        class="table-toolbar flex flex-wrap gap-2"
        @submit.prevent="applyFilters"
      >
        <Input
          v-model="search"
          aria-label="搜索招聘记录"
          placeholder="姓名、手机号或职位"
          class="w-56"
        />
        <Select
          v-if="tab === 'candidates'"
          v-model="status"
          :options="[
            { value: '', label: '全部阶段' },
            ...Object.entries(candidateStages).map(([value, label]) => ({
              value,
              label
            }))
          ]"
          aria-label="招聘阶段"
          class="w-40"
        />
        <Select
          v-if="tab === 'interviews'"
          v-model="resultFilter"
          :options="[
            { value: '', label: '全部结果' },
            ...Object.entries(interviewResults).map(([value, label]) => ({
              value,
              label
            }))
          ]"
          aria-label="面试结果"
          class="w-40"
        />
        <Select
          v-if="tab === 'posts'"
          v-model="status"
          :options="[{ value: '', label: '全部职位' }, { value: '1', label: '招聘中' }, { value: '0', label: '停止招聘' }]"
          aria-label="招聘职位状态"
          class="w-40"
        />
        <Button type="submit" variant="outline">查询</Button>
      </form>
      <p v-if="loading" class="p-8 text-sm text-muted-foreground">
        正在读取招聘记录…
      </p>
      <EmptyState
        v-else-if="!rows.length && !error"
        title="暂无招聘记录"
        description="可新增职位、登记候选人并安排面试。"
      />
      <div v-else class="overflow-x-auto">
        <table class="data-table min-w-[760px]">
          <thead>
            <tr v-if="tab === 'candidates'">
              <th>候选人</th>
              <th>应聘职位 / 部门</th>
              <th>来源渠道</th>
              <th>阶段</th>
              <th>操作</th>
            </tr>
            <tr v-else-if="tab === 'interviews'">
              <th>面试时间</th>
              <th>候选人 / 职位</th>
              <th>面试官</th>
              <th>轮次 / 地点</th>
              <th>结果</th>
              <th>操作</th>
            </tr>
            <tr v-else>
              <th>职位</th>
              <th>部门</th>
              <th>计划招聘</th>
              <th>已入职</th>
              <th>负责人</th>
              <th>招聘状态</th>
            </tr>
          </thead>
          <tbody v-if="tab === 'candidates'">
            <tr v-for="c in rows as Candidate[]" :key="c.id">
              <td>
                <b>{{ c.name }}</b>
                <p class="text-xs text-muted-foreground">{{ c.mobile }}</p>
              </td>
              <td>
                {{ c.postName }}
                <p class="text-xs text-muted-foreground">
                  {{ c.deptName || '—' }}
                </p>
              </td>
              <td>{{ c.channelName || '未指定' }}</td>
              <td>
                <Badge>
                  {{
                    c.status === 3
                      ? `第 ${c.stageNumber || 1} 轮面试`
                      : candidateStages[c.status]
                  }}
                </Badge>
              </td>
              <td>
                <Button variant="ghost" size="sm" @click="view(c)">
                  档案与跟进
                </Button>
              </td>
            </tr>
          </tbody>
          <tbody v-else-if="tab === 'interviews'">
            <tr v-for="i in rows as Interview[]" :key="i.id">
              <td>{{ dateText(i.interviewTime) }}</td>
              <td>
                {{ i.candidateName }}
                <p class="text-xs text-muted-foreground">{{ i.postName }}</p>
              </td>
              <td>{{ i.interviewEmployeeName || '—' }}</td>
              <td>
                第 {{ i.stageNumber }} 轮
                <p class="text-xs text-muted-foreground">
                  {{ i.address || '—' }}
                </p>
              </td>
              <td>
                <Badge>{{ interviewResults[i.result] }}</Badge>
              </td>
              <td>
                <Button size="sm" variant="ghost" @click="feedback(i)">
                  {{ i.result === 1 ? '填写反馈' : '查看反馈' }}
                </Button>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr v-for="p in rows as Post[]" :key="p.id">
              <td class="font-medium">{{ p.postName }}</td>
              <td>{{ p.deptName || '—' }}</td>
              <td>{{ p.recruitNum ?? '—' }}</td>
              <td>{{ p.hasEntryNum ?? 0 }}</td>
              <td>{{ p.ownerEmployeeName || '—' }}</td>
              <td><Badge>{{ p.status === 1 ? '招聘中' : p.status === 0 ? '停止招聘' : '—' }}</Badge></td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination
        class="border-t px-4 py-3"
        :page="page"
        :page-size="20"
        :total="total"
        @update:page="changePage"
      />
    </section>

    <Dialog
      :open="modal === 'candidate'"
      title="新增候选人"
      size="lg"
      @update:open="closeModal"
    >
      <form
        id="candidate-form"
        class="grid gap-4 sm:grid-cols-2"
        @submit.prevent="saveCandidate"
      >
        <div v-for="f in candidateFields" :key="f.key">
          <label :for="`cand-${f.key}`" class="field-label">
            {{ f.label }}{{ f.required ? ' *' : '' }}
          </label>
          <Input
            :id="`cand-${f.key}`"
            v-model="candidateForm[f.key]"
            :required="f.required"
            :type="f.key === 'email' ? 'email' : 'text'"
            maxlength="255"
          />
        </div>
        <div>
          <label for="cand-sex" class="field-label">性别 *</label>
          <Select
            id="cand-sex"
            v-model="candidateForm.sex"
            :options="sexOptions"
          />
        </div>
        <div>
          <label for="cand-education" class="field-label">学历 *</label>
          <Select
            id="cand-education"
            v-model="candidateForm.education"
            :options="educationOptions"
          />
        </div>
        <div class="sm:col-span-2">
          <label for="cand-post-search" class="field-label">搜索招聘职位</label>
          <div class="flex gap-2">
            <Input
              id="cand-post-search"
              v-model="postSearch"
              placeholder="输入职位名称"
            />
            <Button variant="outline" @click="loadPosts">搜索</Button>
          </div>
          <Select
            id="cand-post"
            v-model="candidateForm.postId"
            :options="
              posts.map((p) => ({
                value: p.id,
                label: `${p.postName} · ${p.deptName || '未分配部门'}`
              }))
            "
            aria-label="应聘职位"
            placeholder="选择应聘职位"
            class="mt-2"
          />
        </div>
        <div>
          <label for="cand-channel" class="field-label">来源渠道</label>
          <Select
            id="cand-channel"
            v-model="candidateForm.channelId"
            :options="[
              { value: '', label: '未指定' },
              ...channels.map((c) => ({ value: c.id, label: c.name }))
            ]"
          />
        </div>
        <div>
          <label for="cand-resume" class="field-label">
            简历附件（PDF / DOCX，每份 ≤ 10MB）
          </label>
          <Input
            id="cand-resume"
            type="file"
            accept=".pdf,.docx"
            multiple
            @change="selectFiles"
          />
        </div>
        <div class="sm:col-span-2">
          <label for="cand-remark" class="field-label">备注</label>
          <Textarea
            id="cand-remark"
            v-model="candidateForm.remark"
            maxlength="255"
          />
        </div>
        <Callout v-if="modalError" tone="destructive" class="sm:col-span-2">
          {{ modalError }}
        </Callout>
      </form>
      <template #footer>
        <Button variant="outline" :disabled="busy" @click="modal = ''">
          取消
        </Button>
        <Button type="submit" form="candidate-form" :loading="busy">
          保存候选人
        </Button>
      </template>
    </Dialog>

    <Dialog
      :open="modal === 'detail'"
      :title="`候选人档案 · ${selected?.name || ''}`"
      size="lg"
      @update:open="closeModal"
    >
      <template v-if="selected">
        <dl class="grid gap-4 sm:grid-cols-2 text-sm">
          <div>
            <dt class="text-xs text-muted-foreground">应聘职位</dt>
            <dd>{{ selected.postName }}</dd>
          </div>
          <div>
            <dt class="text-xs text-muted-foreground">当前阶段</dt>
            <dd>{{ candidateStages[selected.status] }}</dd>
          </div>
          <div>
            <dt class="text-xs text-muted-foreground">联系方式</dt>
            <dd>{{ selected.mobile }} / {{ selected.email || '—' }}</dd>
          </div>
          <div>
            <dt class="text-xs text-muted-foreground">来源渠道</dt>
            <dd>{{ selected.channelName || '未指定' }}</dd>
          </div>
        </dl>
        <section class="mt-5 border-t pt-4">
          <h3 class="text-sm font-semibold">简历附件</h3>
          <p
            v-if="!attachments.length"
            class="mt-2 text-xs text-muted-foreground"
          >
            尚未上传简历
          </p>
          <Button
            v-for="a in attachments"
            :key="a.id"
            variant="link"
            @click="downloadResume(a)"
          >
            {{ a.filename }}
          </Button>
          <div v-if="session.has('hrm:recruit:candidate:update')" class="mt-3">
            <label for="resume-more" class="field-label">补充简历</label>
            <Input
              id="resume-more"
              type="file"
              accept=".pdf,.docx"
              :disabled="busy"
              @change="uploadMore"
            />
          </div>
        </section>
        <section class="mt-5 border-t pt-4">
          <h3 class="text-sm font-semibold">面试记录</h3>
          <p v-if="historyLoading" class="mt-2 text-xs">正在读取…</p>
          <p
            v-else-if="!history.length"
            class="mt-2 text-xs text-muted-foreground"
          >
            暂无面试记录
          </p>
          <ul class="divide-y">
            <li v-for="i in history" :key="i.id" class="py-3 text-sm">
              第 {{ i.stageNumber }} 轮 · {{ dateText(i.interviewTime) }} ·
              {{ i.interviewEmployeeName }}
              <Badge class="ml-2">{{ interviewResults[i.result] }}</Badge>
              <p class="mt-1 text-xs text-muted-foreground whitespace-pre-wrap">
                {{ i.evaluate || i.cancelReason || i.address }}
              </p>
            </li>
          </ul>
        </section>
        <div
          v-if="session.has('hrm:recruit:candidate:update')"
          class="mt-5 flex flex-wrap gap-2 border-t pt-4"
        >
          <Button
            v-for="s in stageActions[selected.status] || []"
            :key="s"
            variant="outline"
            size="sm"
            :disabled="busy"
            @click="changeStage(s)"
          >
            {{ s === 5 ? '标记已发 offer' : candidateStages[s] }}
          </Button>
          <Button
            v-if="
              [1, 2, 3, 4].includes(selected.status) &&
              session.has('hrm:recruit:interview:create')
            "
            size="sm"
            @click="arrange(selected)"
          >
            安排面试
          </Button>
          <Button
            v-if="
              [4, 5].includes(selected.status) &&
              session.has('hrm:employee:update')
            "
            size="sm"
            @click="
              router.push(
                `/hr/employment?mode=onboard&candidateId=${selected.id}`
              )
            "
          >
            办理入职
          </Button>
        </div>
        <form
          v-if="
            [1, 2, 3, 4, 5, 6].includes(selected.status) &&
            session.has('hrm:recruit:candidate:update')
          "
          class="mt-4 flex gap-2"
          @submit.prevent="eliminate"
        >
          <Input
            v-model="eliminateReason"
            aria-label="淘汰原因"
            placeholder="填写淘汰原因"
            required
            maxlength="255"
          />
          <Button variant="outline" type="submit" :disabled="busy">淘汰</Button>
        </form>
        <Callout v-if="modalError" class="mt-4" tone="destructive">
          {{ modalError }}
        </Callout>
      </template>
    </Dialog>

    <Dialog
      :open="modal === 'interview'"
      title="安排面试"
      @update:open="closeModal"
    >
      <form
        id="interview-form"
        class="space-y-4"
        @submit.prevent="saveInterview"
      >
        <div>
          <label for="int-candidate-search" class="field-label">
            搜索候选人
          </label>
          <div class="flex gap-2">
            <Input id="int-candidate-search" v-model="candidateSearch" />
            <Button variant="outline" @click="loadCandidateOptions">
              搜索
            </Button>
          </div>
          <Select
            v-model="interviewForm.candidateId"
            :options="
              candidateOptions.map((c) => ({
                value: c.id,
                label: `${c.name} · ${c.postName}`
              }))
            "
            aria-label="面试候选人"
            class="mt-2"
          />
        </div>
        <div>
          <label for="int-employee-search" class="field-label">
            搜索面试官
          </label>
          <div class="flex gap-2">
            <Input id="int-employee-search" v-model="employeeSearch" />
            <Button variant="outline" @click="loadInterviewers">搜索</Button>
          </div>
          <Select
            v-model="interviewForm.interviewEmployeeId"
            :options="
              interviewers.map((e) => ({
                value: e.id,
                label: `${e.name} · ${e.deptName || e.jobNumber || ''}`
              }))
            "
            aria-label="主面试官"
            class="mt-2"
          />
        </div>
        <div>
          <label for="int-time" class="field-label">面试时间 *</label>
          <Input
            id="int-time"
            v-model="interviewForm.interviewTime"
            type="datetime-local"
            required
          />
        </div>
        <div>
          <label for="int-type" class="field-label">面试方式 *</label>
          <Select
            id="int-type"
            v-model="interviewForm.type"
            :options="interviewTypes"
          />
        </div>
        <div>
          <label for="int-address" class="field-label">地点 / 会议链接</label>
          <Input
            id="int-address"
            v-model="interviewForm.address"
            maxlength="255"
          />
        </div>
        <Callout v-if="modalError" tone="destructive">{{ modalError }}</Callout>
      </form>
      <template #footer>
        <Button variant="outline" :disabled="busy" @click="modal = ''">
          取消
        </Button>
        <Button type="submit" form="interview-form" :loading="busy">
          保存面试安排
        </Button>
      </template>
    </Dialog>

    <Dialog
      :open="modal === 'feedback'"
      title="面试反馈"
      @update:open="closeModal"
    >
      <form id="feedback-form" class="space-y-4" @submit.prevent="saveFeedback">
        <p class="text-sm">
          {{ feedbackTarget?.candidateName }} · 第
          {{ feedbackTarget?.stageNumber }} 轮
        </p>
        <div>
          <label for="feedback-result" class="field-label">面试结果 *</label>
          <Select
            id="feedback-result"
            v-model="feedbackForm.result"
            :disabled="feedbackTarget?.result !== 1"
            :options="
              Object.entries(interviewResults)
                .filter(([v]) => v !== '1')
                .map(([value, label]) => ({ value, label }))
            "
          />
        </div>
        <div>
          <label for="feedback-text" class="field-label">
            评价 / 取消原因 *
          </label>
          <Textarea
            id="feedback-text"
            v-model="feedbackForm.evaluate"
            :disabled="feedbackTarget?.result !== 1"
            required
            maxlength="255"
          />
        </div>
        <Callout v-if="modalError" tone="destructive">{{ modalError }}</Callout>
      </form>
      <template #footer>
        <Button variant="outline" @click="modal = ''">关闭</Button>
        <Button
          v-if="
            feedbackTarget?.result === 1 &&
            session.has('hrm:recruit:interview:update')
          "
          type="submit"
          form="feedback-form"
          :loading="busy"
        >
          保存反馈
        </Button>
      </template>
    </Dialog>

    <Dialog
      :open="modal === 'post'"
      title="新增招聘职位"
      @update:open="closeModal"
    >
      <form id="post-form" class="space-y-4" @submit.prevent="savePost">
        <div>
          <label for="post-name" class="field-label">职位名称 *</label>
          <Input
            id="post-name"
            v-model="postForm.postName"
            required
            maxlength="255"
          />
        </div>
        <div>
          <label for="post-dept" class="field-label">部门 *</label>
          <Select
            id="post-dept"
            v-model="postForm.deptId"
            :options="departments"
          />
        </div>
        <div>
          <label for="post-number" class="field-label">计划招聘人数 *</label>
          <Input
            id="post-number"
            v-model="postForm.recruitNum"
            type="number"
            required
            min="1"
            max="10000"
          />
        </div>
        <div>
          <label for="post-nature" class="field-label">工作性质 *</label>
          <Select
            id="post-nature"
            v-model="postForm.jobNature"
            :options="jobNatures"
          />
        </div>
        <div>
          <label for="post-description" class="field-label">职位描述</label>
          <Textarea
            id="post-description"
            v-model="postForm.description"
            maxlength="4000"
          />
        </div>
        <Callout v-if="modalError" tone="destructive">{{ modalError }}</Callout>
      </form>
      <template #footer>
        <Button variant="outline" @click="modal = ''">取消</Button>
        <Button type="submit" form="post-form" :loading="busy">保存职位</Button>
      </template>
    </Dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../../lib/api';
import { dictOptions } from '../../lib/dict';
import {
  hrBase,
  candidateStages,
  stageActions,
  interviewResults,
  dateText,
  downloadHrFile,
  type Candidate,
  type Interview,
  type HrEmployee,
  type HrPage
} from '../../lib/hr-workspace';
import { session } from '../../lib/session';
import { errText, toast } from '../../lib/toast';
import Badge from '../../ui/Badge.vue';
import Button from '../../ui/Button.vue';
import Callout from '../../ui/Callout.vue';
import Dialog from '../../ui/Dialog.vue';
import EmptyState from '../../ui/EmptyState.vue';
import Input from '../../ui/Input.vue';
import PageHeader from '../../ui/PageHeader.vue';
import Pagination from '../../ui/Pagination.vue';
import HrRecruitNavigation from '../../components/HrRecruitNavigation.vue';
import HrRecruitOverview from '../../components/HrRecruitOverview.vue';
import Select from '../../ui/Select.vue';
import Textarea from '../../ui/Textarea.vue';
interface Post {
  id: string;
  postName: string;
  deptName?: string;
  recruitNum?: number;
  hasEntryNum?: number;
  ownerEmployeeName?: string;
  status?: number;
}
type Option = { value: string; label: string };
const route = useRoute(),
  router = useRouter();
const tabs = computed(() =>
  [
    {
      value: 'candidates',
      label: '候选人与简历',
      p: 'hrm:recruit:candidate:query'
    },
    {
      value: 'interviews',
      label: '面试安排与记录',
      p: 'hrm:recruit:interview:query'
    },
    { value: 'posts', label: '招聘职位', p: 'hrm:recruit:post:query' }
  ].filter((t) => session.has(t.p))
);
const tab = computed(() =>
  tabs.value.some((t) => t.value === route.query.tab)
    ? String(route.query.tab)
    : tabs.value[0]?.value || 'candidates'
);
const search = ref(''),
  status = ref(''),
  resultFilter = ref(''),
  page = ref(1),
  total = ref(0),
  rows = ref<(Candidate | Interview | Post)[]>([]),
  loading = ref(false),
  error = ref('');
const overview = ref<InstanceType<typeof HrRecruitOverview> | null>(null);
const appliedStatus = computed(() => String(route.query.status || ''));
function applyFilters() {
  const query = {
    tab: tab.value,
    search: search.value || undefined,
    status: tab.value !== 'interviews' ? status.value || undefined : undefined,
    result: tab.value === 'interviews' ? resultFilter.value || undefined : undefined
  };
  if (search.value === String(route.query.search || '') &&
      status.value === String(route.query.status || '') &&
      resultFilter.value === String(route.query.result || '') && page.value === 1) {
    void load();
    return;
  }
  void router.push({ query });
}
function selectStage(value: string) {
  void router.push({ path: '/hr/recruit', query: { tab: 'candidates', status: value || undefined } });
}
function changePage(value: number) {
  void router.push({ query: { ...route.query, page: value > 1 ? String(value) : undefined } });
}
const modal = ref(''),
  modalError = ref(''),
  busy = ref(false),
  selected = ref<Candidate | null>(null),
  history = ref<Interview[]>([]),
  historyLoading = ref(false),
  attachments = ref<{ id: number; filename: string }[]>([]);
const candidateForm = reactive<Record<string, string>>({}),
  candidateFields = [
    { key: 'name', label: '候选人姓名', required: true },
    { key: 'mobile', label: '手机号', required: true },
    { key: 'email', label: '邮箱', required: false },
    { key: 'graduateSchool', label: '毕业院校', required: false }
  ];
const sexOptions: Option[] = [
  { value: '1', label: '男' },
  { value: '2', label: '女' },
  { value: '0', label: '未知' }
];
const educationOptions = ref<Option[]>([]),
  interviewTypes = ref<Option[]>([]),
  jobNatures = ref<Option[]>([]),
  departments = ref<Option[]>([]),
  channels = ref<{ id: string; name: string }[]>([]),
  posts = ref<Post[]>([]),
  postSearch = ref(''),
  files = ref<File[]>([]),
  eliminateReason = ref('');
const candidateSearch = ref(''),
  employeeSearch = ref(''),
  candidateOptions = ref<Candidate[]>([]),
  interviewers = ref<HrEmployee[]>([]);
const interviewForm = reactive({
    candidateId: '',
    interviewEmployeeId: '',
    interviewTime: '',
    type: '',
    address: ''
  }),
  feedbackTarget = ref<Interview | null>(null),
  feedbackForm = reactive({ result: '2', evaluate: '' });
const postForm = reactive({
  postName: '',
  deptId: '',
  recruitNum: '1',
  jobNature: '',
  description: ''
});
let loadRequest = 0;
async function load() {
  const request = ++loadRequest;
  loading.value = true;
  error.value = '';
  try {
    const url =
      tab.value === 'candidates'
        ? '/admin-api/hrm/recruit/candidate/page'
        : tab.value === 'interviews'
          ? `${hrBase}/interviews`
          : '/admin-api/hrm/recruit/post/page';
    const result = await api.get<HrPage<Candidate | Interview | Post>>(url, {
      pageNo: page.value,
      pageSize: 20,
      search: search.value || undefined,
      postName: search.value || undefined,
      status:
        tab.value !== 'interviews' ? status.value || undefined : undefined,
      result:
        tab.value === 'interviews' ? resultFilter.value || undefined : undefined
    });
    if (request === loadRequest) {
      rows.value = result.list;
      total.value = Number(result.total);
    }
  } catch (e) {
    if (request === loadRequest) {
      error.value = errText(e);
      rows.value = [];
      total.value = 0;
    }
  } finally {
    if (request === loadRequest) loading.value = false;
  }
}
function closeModal(v: boolean) {
  if (!busy.value && !v) modal.value = '';
}
async function run(action: () => Promise<void>) {
  if (busy.value) return;
  busy.value = true;
  modalError.value = '';
  try {
    await action();
    void overview.value?.refresh();
  } catch (e) {
    modalError.value = errText(e);
  } finally {
    busy.value = false;
  }
}
async function loadPosts() {
  try {
    posts.value = (
      await api.get<HrPage<Post>>('/admin-api/hrm/recruit/post/page', {
        pageNo: 1,
        pageSize: 50,
        postName: postSearch.value || undefined
      })
    ).list;
  } catch (e) {
    modalError.value = errText(e);
  }
}
async function newCandidate() {
  Object.assign(candidateForm, {
    name: '',
    mobile: '',
    email: '',
    graduateSchool: '',
    sex: '0',
    education: '',
    postId: '',
    channelId: '',
    remark: ''
  });
  files.value = [];
  modalError.value = '';
  modal.value = 'candidate';
  await loadPosts();
  try {
    channels.value = await api.get(
      '/admin-api/hrm/recruit/channel/simple-list'
    );
  } catch (e) {
    modalError.value = errText(e);
  }
}
function selectFiles(e: Event) {
  files.value = Array.from((e.target as HTMLInputElement).files || []);
}
async function uploadFiles(id: string, list: File[]) {
  for (const file of list) {
    if (file.size > 10 * 1024 * 1024) throw new Error(`${file.name} 超过 10MB`);
    const body = new FormData();
    body.append('candidateId', id);
    body.append('file', file);
    await api.post(`${hrBase}/resumes`, body);
  }
}
async function saveCandidate() {
  await run(async () => {
    if (!candidateForm.postId || !candidateForm.education)
      throw new Error('请选择应聘职位和学历');
    if (
      files.value.length > 5 ||
      files.value.some((f) => f.size > 10 * 1024 * 1024)
    )
      throw new Error('最多上传 5 份简历，每份不超过 10MB');
    const id = await api.post<string>(
      '/admin-api/hrm/recruit/candidate/create',
      {
        ...candidateForm,
        sex: Number(candidateForm.sex),
        education: Number(candidateForm.education),
        channelId: candidateForm.channelId || null,
        resumeUrls: []
      }
    );
    try {
      await uploadFiles(String(id), files.value);
    } catch (e) {
      modal.value = '';
      await load();
      await view(
        await api.get<Candidate>('/admin-api/hrm/recruit/candidate/get', { id })
      );
      throw new Error(
        `候选人已保存，附件未全部上传，可在档案中补传：${errText(e)}`
      );
    }
    modal.value = '';
    toast('候选人已保存');
    await load();
  });
}
async function view(c: Candidate) {
  selected.value = c;
  modal.value = 'detail';
  modalError.value = '';
  eliminateReason.value = '';
  attachments.value = [];
  history.value = [];
  historyLoading.value = true;
  try {
    const results = await Promise.allSettled([
      api.get<typeof attachments.value>(`${hrBase}/resumes`, {
        candidateId: c.id
      }),
      session.has('hrm:recruit:interview:query')
        ? api.get<Interview[]>(
            '/admin-api/hrm/recruit/interview/list-by-candidate',
            { candidateId: c.id }
          )
        : Promise.resolve([])
    ]);
    if (selected.value?.id !== c.id) return;
    for (const [i, result] of results.entries()) {
      if (result.status === 'rejected')
        modalError.value = errText(result.reason);
      else if (i === 0)
        attachments.value = result.value as typeof attachments.value;
      else history.value = result.value as Interview[];
    }
  } finally {
    historyLoading.value = false;
  }
}
async function downloadResume(a: { id: number; filename: string }) {
  await run(async () => {
    await downloadHrFile(
      `${hrBase}/resumes/${a.id}/file`,
      undefined,
      a.filename
    );
  });
}
async function uploadMore(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file || !selected.value) return;
  await run(async () => {
    await uploadFiles(selected.value!.id, [file]);
    await view(selected.value!);
    toast('简历已上传');
  });
  (e.target as HTMLInputElement).value = '';
}
async function refreshCandidate() {
  if (!selected.value) return;
  await view(
    await api.get<Candidate>('/admin-api/hrm/recruit/candidate/get', {
      id: selected.value.id
    })
  );
  await load();
}
async function changeStage(s: number) {
  await run(async () => {
    await api.put('/admin-api/hrm/recruit/candidate/update-status', {
      id: selected.value!.id,
      status: s
    });
    await refreshCandidate();
    toast('招聘阶段已更新');
  });
}
async function eliminate() {
  await run(async () => {
    await api.put('/admin-api/hrm/recruit/candidate/eliminate', {
      id: selected.value!.id,
      eliminate: eliminateReason.value
    });
    await refreshCandidate();
    toast('候选人已淘汰');
  });
}
async function loadCandidateOptions() {
  try {
    const r = await api.get<HrPage<Candidate>>(
      '/admin-api/hrm/recruit/candidate/page',
      { pageNo: 1, pageSize: 50, search: candidateSearch.value || undefined }
    );
    const chosen = candidateOptions.value.find(
      (c) => c.id === interviewForm.candidateId
    );
    candidateOptions.value = r.list.filter((c) =>
      [1, 2, 3, 4].includes(c.status)
    );
    if (chosen && !candidateOptions.value.some((c) => c.id === chosen.id))
      candidateOptions.value.unshift(chosen);
  } catch (e) {
    modalError.value = errText(e);
  }
}
async function loadInterviewers() {
  try {
    interviewers.value = (
      await api.get<HrPage<HrEmployee>>('/admin-api/hrm/employee/simple-page', {
        pageNo: 1,
        pageSize: 50,
        search: employeeSearch.value || undefined,
        entryStatus: 1
      })
    ).list;
  } catch (e) {
    modalError.value = errText(e);
  }
}
async function arrange(c?: Candidate) {
  modal.value = 'interview';
  modalError.value = '';
  candidateSearch.value = c?.name || '';
  employeeSearch.value = '';
  Object.assign(interviewForm, {
    candidateId: c?.id || '',
    interviewEmployeeId: '',
    interviewTime: '',
    type: interviewTypes.value[0]?.value || '',
    address: ''
  });
  candidateOptions.value = c ? [c] : [];
  await Promise.all([loadCandidateOptions(), loadInterviewers()]);
}
async function saveInterview() {
  await run(async () => {
    if (
      !interviewForm.candidateId ||
      !interviewForm.interviewEmployeeId ||
      !interviewForm.type
    )
      throw new Error('请选择候选人、面试官和面试方式');
    await api.post('/admin-api/hrm/recruit/interview/create', {
      ...interviewForm,
      type: Number(interviewForm.type),
      interviewTime: `${interviewForm.interviewTime}:00`,
      otherInterviewEmployeeIds: []
    });
    modal.value = '';
    toast('面试已安排');
    await load();
  });
}
function feedback(i: Interview) {
  feedbackTarget.value = i;
  feedbackForm.result = String(i.result === 1 ? 2 : i.result);
  feedbackForm.evaluate = i.evaluate || i.cancelReason || '';
  modalError.value = '';
  modal.value = 'feedback';
}
async function saveFeedback() {
  await run(async () => {
    await api.put('/admin-api/hrm/recruit/interview/update-result', {
      id: feedbackTarget.value!.id,
      result: Number(feedbackForm.result),
      evaluate: feedbackForm.result === '4' ? null : feedbackForm.evaluate,
      cancelReason: feedbackForm.result === '4' ? feedbackForm.evaluate : null
    });
    modal.value = '';
    toast('面试反馈已保存');
    await load();
  });
}
async function openPost() {
  Object.assign(postForm, {
    postName: '',
    deptId: '',
    recruitNum: '1',
    jobNature: jobNatures.value[0]?.value || '',
    description: ''
  });
  modal.value = 'post';
  modalError.value = '';
  try {
    departments.value = await api.get('/admin-api/hrm/workspace/departments');
  } catch (e) {
    modalError.value = errText(e);
  }
}
async function savePost() {
  await run(async () => {
    if (!postForm.deptId || !postForm.jobNature)
      throw new Error('请选择部门和工作性质');
    await api.post('/admin-api/hrm/recruit/post/create', {
      ...postForm,
      recruitNum: Number(postForm.recruitNum),
      jobNature: Number(postForm.jobNature),
      interviewEmployeeIds: []
    });
    modal.value = '';
    toast('招聘职位已保存');
    await load();
  });
}
watch(() => [tab.value, route.query.search, route.query.status, route.query.result, route.query.page], () => {
  const requestedPage = Number(route.query.page || 1);
  page.value = Number.isSafeInteger(requestedPage) && requestedPage > 0 ? requestedPage : 1;
  search.value = String(route.query.search || '');
  status.value = tab.value !== 'interviews' ? String(route.query.status || '') : '';
  resultFilter.value = tab.value === 'interviews' ? String(route.query.result || '') : '';
  error.value = '';
  void load();
}, { immediate: true });
onMounted(async () => {
  const results = await Promise.allSettled(
    [
      'hrm_employee_education',
      'hrm_recruit_interview_type',
      'hrm_recruit_job_nature'
    ].map(dictOptions)
  );
  for (const [i, r] of results.entries()) {
    if (r.status === 'fulfilled')
      [educationOptions, interviewTypes, jobNatures][i]!.value = r.value.map(
        (d) => ({ value: d.value, label: d.label })
      );
    else error.value = `字典加载失败：${errText(r.reason)}`;
  }
});
</script>
