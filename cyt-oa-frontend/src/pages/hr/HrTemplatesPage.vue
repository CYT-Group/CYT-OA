<template>
  <div class="min-w-0 space-y-4">
    <PageHeader
      title="合同模板"
      description="按版本维护入职合同与离职协议，审核通过后用于生成文书。"
    >
      <template #actions>
        <Button
          v-if="session.has('hrm:contract-template:create')"
          @click="create"
        >
          提交新模板
        </Button>
        <Button variant="outline" @click="load">刷新</Button>
      </template>
    </PageHeader>
    <HrEmploymentNavigation />
    <Callout v-if="error" tone="destructive">{{ error }}</Callout>
    <p v-if="loading" class="panel p-8 text-sm text-muted-foreground">
      正在读取模板…
    </p>
    <EmptyState
      v-else-if="!templates.length && !error"
      title="尚无合同模板"
      description="提交公司现行模板，审核通过后即可办理入离职。"
    />
    <div v-else class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      <article
        v-for="t in templates"
        :key="t.id"
        class="panel flex flex-col p-5"
      >
        <div class="flex items-start justify-between gap-2">
          <div>
            <h2 class="text-base font-semibold">{{ t.name }}</h2>
            <p class="mt-1 text-xs text-muted-foreground">
              {{ t.version }} ·
              {{ t.kind === 'onboard' ? '入职合同' : '离职协议' }}
            </p>
          </div>
          <Badge>{{ statusLabels[t.status] }}</Badge>
        </div>
        <p class="mt-4 flex-1 text-sm text-muted-foreground">{{ t.scope }}</p>
        <p class="mt-3 text-xs text-muted-foreground">
          {{ t.creatorName || '—' }} · {{ dateText(t.createdAt) }}
        </p>
        <p v-if="t.reviewNote" class="mt-2 text-xs">
          审核意见：{{ t.reviewNote }}
        </p>
        <div class="mt-4 flex flex-wrap gap-2 border-t pt-3">
          <Button variant="outline" size="sm" @click="selected = t">
            预览
          </Button>
          <Button
            variant="ghost"
            size="sm"
            @click="textDownload(`${t.name}-${t.version}.txt`, t.content)"
          >
            下载
          </Button>
          <Button
            v-if="
              t.status === 'pending' &&
              session.isAdmin &&
              session.has('hrm:contract-template:review')
            "
            size="sm"
            @click="
              reviewTarget = t;
              reviewNote = '';
              modalError = '';
            "
          >
            审核
          </Button>
        </div>
      </article>
    </div>
    <Dialog
      :open="editing"
      title="提交合同模板"
      size="lg"
      @update:open="!busy && (editing = $event)"
    >
      <form id="template-form" class="space-y-4" @submit.prevent="submit">
        <div class="grid gap-4 sm:grid-cols-2">
          <div>
            <label for="tpl-name" class="field-label">模板名称 *</label>
            <Input id="tpl-name" v-model="form.name" required maxlength="100" />
          </div>
          <div>
            <label for="tpl-version" class="field-label">版本 *</label>
            <Input
              id="tpl-version"
              v-model="form.version"
              required
              maxlength="40"
              placeholder="如 V1.0"
            />
          </div>
          <div>
            <label for="tpl-kind" class="field-label">用途 *</label>
            <Select
              id="tpl-kind"
              v-model="form.kind"
              :options="[
                { value: 'onboard', label: '入职合同' },
                { value: 'offboard', label: '离职协议' }
              ]"
            />
          </div>
          <div v-if="form.kind === 'onboard'">
            <label for="tpl-contract-type" class="field-label">
              合同类型 *
            </label>
            <Select
              id="tpl-contract-type"
              v-model="form.contractType"
              :options="[
                { value: '1', label: '固定期限劳动合同' },
                { value: '4', label: '实习协议' },
                { value: '5', label: '劳务合同' },
                { value: '9', label: '其他' }
              ]"
            />
          </div>
          <div>
            <label for="tpl-scope" class="field-label">适用范围 *</label>
            <Input
              id="tpl-scope"
              v-model="form.scope"
              required
              maxlength="255"
            />
          </div>
        </div>
        <div>
          <label for="tpl-upload" class="field-label">
            从文件读取正文（DOCX / PDF / TXT，≤ 10MB）
          </label>
          <Input
            id="tpl-upload"
            type="file"
            accept=".docx,.pdf,.txt"
            :disabled="busy"
            @change="readFile"
          />
          <p class="mt-1 text-xs text-muted-foreground">
            文件正文转为文本模板，生成后可下载文本或打印为 PDF。
          </p>
        </div>
        <div>
          <label for="tpl-content" class="field-label">模板正文 *</label>
          <Textarea
            id="tpl-content"
            v-model="form.content"
            :rows="14"
            required
            maxlength="50000"
            class="font-mono text-xs"
          />
          <p class="mt-2 text-xs leading-6 text-muted-foreground">
            变量使用双花括号，如
            <code v-pre>{{ name }}</code>
            。可用变量：{{
              variableNames
            }}。审核通过后保留该版本；修改请提交新版本。
          </p>
        </div>
        <Callout v-if="modalError" tone="destructive">{{ modalError }}</Callout>
      </form>
      <template #footer>
        <Button variant="outline" :disabled="busy" @click="editing = false">
          取消
        </Button>
        <Button type="submit" form="template-form" :loading="busy">
          提交审核
        </Button>
      </template>
    </Dialog>
    <Dialog
      :open="!!selected"
      :title="selected ? `${selected.name} · ${selected.version}` : '模板预览'"
      size="lg"
      @update:open="!$event && (selected = null)"
    >
      <pre
        class="whitespace-pre-wrap break-words font-sans text-sm leading-7"
        >{{ selected?.content }}</pre
      >
      <template #footer>
        <Button
          variant="outline"
          @click="
            selected &&
            textDownload(
              `${selected.name}-${selected.version}.txt`,
              selected.content
            )
          "
        >
          下载模板
        </Button>
        <Button @click="selected = null">关闭</Button>
      </template>
    </Dialog>
    <Dialog
      :open="!!reviewTarget"
      title="审核合同模板"
      @update:open="!busy && !$event && (reviewTarget = null)"
    >
      <p class="text-sm">
        {{ reviewTarget?.name }} · {{ reviewTarget?.version }}
      </p>
      <pre
        class="mt-3 max-h-64 overflow-auto whitespace-pre-wrap break-words rounded-lg border p-3 text-xs"
        >{{ reviewTarget?.content }}</pre
      >
      <label for="tpl-review-note" class="field-label mt-4">
        审核意见（驳回时必填）
      </label>
      <Textarea id="tpl-review-note" v-model="reviewNote" maxlength="500" />
      <Callout v-if="modalError" class="mt-3" tone="destructive">
        {{ modalError }}
      </Callout>
      <template #footer>
        <Button variant="outline" :disabled="busy" @click="review(false)">
          驳回
        </Button>
        <Button :loading="busy" @click="review(true)">审核通过</Button>
      </template>
    </Dialog>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { api } from '../../lib/api';
import {
  hrBase,
  getTemplates,
  dateText,
  textDownload,
  type HrTemplate
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
import HrEmploymentNavigation from '../../components/HrEmploymentNavigation.vue';
import Select from '../../ui/Select.vue';
import Textarea from '../../ui/Textarea.vue';
const templates = ref<HrTemplate[]>([]),
  selected = ref<HrTemplate | null>(null),
  reviewTarget = ref<HrTemplate | null>(null),
  editing = ref(false),
  busy = ref(false),
  loading = ref(false),
  error = ref(''),
  modalError = ref(''),
  reviewNote = ref('');
const form = reactive({
  name: '',
  version: '',
  kind: 'onboard',
  contractType: '1',
  scope: '',
  content: ''
});
const statusLabels = {
  pending: '待审核',
  approved: '审核通过',
  rejected: '已驳回'
};
const variableNames =
  'name 姓名、jobNumber 工号、department 部门、postName 岗位、entryDate 入职日、probation 试用月数、salary 月薪、company 公司、companyCode 信用代码、companyAddress 注册地址、signDate 签订日、endDate 合同结束日、leaveDate 离职日、leaveType 离职类型、reason 离职原因';
function create() {
  Object.assign(form, {
    name: '',
    version: '',
    kind: 'onboard',
    contractType: '1',
    scope: '',
    content: ''
  });
  modalError.value = '';
  editing.value = true;
}
async function load() {
  loading.value = true;
  error.value = '';
  try {
    templates.value = await getTemplates();
  } catch (e) {
    error.value = errText(e);
    templates.value = [];
  } finally {
    loading.value = false;
  }
}
async function submit() {
  if (busy.value) return;
  busy.value = true;
  modalError.value = '';
  try {
    await api.post(`${hrBase}/templates`, {
      ...form,
      contractType: Number(form.contractType)
    });
    editing.value = false;
    toast('模板已提交审核');
    await load();
  } catch (e) {
    modalError.value = errText(e);
  } finally {
    busy.value = false;
  }
}
async function readFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  busy.value = true;
  modalError.value = '';
  try {
    if (file.size > 10 * 1024 * 1024) throw new Error('模板文件不能超过 10MB');
    const body = new FormData();
    body.append('file', file);
    const r = await api.post<{ content: string }>(
      `${hrBase}/template-text`,
      body
    );
    form.content = r.content;
    if (!form.name) form.name = file.name.replace(/\.[^.]+$/, '');
  } catch (e) {
    modalError.value = errText(e);
  } finally {
    busy.value = false;
    (e.target as HTMLInputElement).value = '';
  }
}
async function review(approved: boolean) {
  if (busy.value || !reviewTarget.value) return;
  busy.value = true;
  modalError.value = '';
  try {
    if (!approved && !reviewNote.value.trim())
      throw new Error('请填写驳回原因');
    await api.put(`${hrBase}/templates/${reviewTarget.value.id}/review`, {
      approved,
      note: reviewNote.value
    });
    reviewTarget.value = null;
    toast(approved ? '模板审核通过' : '模板已驳回');
    await load();
  } catch (e) {
    modalError.value = errText(e);
  } finally {
    busy.value = false;
  }
}
onMounted(load);
</script>
