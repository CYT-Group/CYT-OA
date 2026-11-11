<template>
  <div v-if="session.has('hrm:employee:update')" class="mt-4">
    <div class="flex flex-wrap gap-2">
      <Button v-if="employee.entryStatus === 2" variant="outline" size="sm" @click="startEmployment('onboard')">确认入职</Button>
      <Button v-if="employee.entryStatus === 1 && employee.status === 2" variant="outline" size="sm" @click="begin('regular')">办理转正</Button>
      <Button v-if="employee.entryStatus === 1" variant="outline" size="sm" @click="begin('transfer')">登记任职调整</Button>
      <Button v-if="employee.entryStatus === 1" variant="outline" size="sm" @click="begin('promote')">办理晋升</Button>
      <Button v-if="employee.entryStatus === 1" variant="outline" size="sm" @click="startEmployment('offboard')">办理离职</Button>
      <Button v-if="employee.entryStatus === 3" variant="outline" size="sm" @click="begin('cancel-quit')">取消待离职</Button>
      <Button v-if="employee.entryStatus === 4" variant="outline" size="sm" @click="begin('rehire')">办理再入职</Button>
    </div>

    <form
      v-if="action"
      class="mt-4 rounded-xl border bg-muted/30 p-4"
      @submit.prevent="submit"
    >
      <h3 class="text-sm font-semibold">{{ labels[action] }}</h3>
      <p v-if="positionChange" class="mt-1 text-xs leading-relaxed text-muted-foreground">
        部门和岗位以企微同步结果为准，此处记录职级及工作地点调整。直属上级在档案资料中单独维护。
      </p>
      <p v-if="action === 'quit'" class="mt-1 text-xs leading-relaxed text-muted-foreground">
        离职流程维护人事状态。登录权限请在账号管理中另行核对。
      </p>

      <div class="mt-4 grid gap-4 sm:grid-cols-2">
        <div v-if="action !== 'cancel-quit' && action !== 'confirm-entry'" class="flex flex-col">
          <Label required>{{ action === 'rehire' ? '再入职日期' : action === 'quit' ? '计划离职日期' : '生效日期' }}</Label>
          <Input v-model="effective" type="date" required />
        </div>

        <template v-if="action === 'quit'">
          <div class="flex flex-col">
            <Label required>申请离职日期</Label>
            <Input v-model="applyDate" type="date" required />
          </div>
          <div class="flex flex-col">
            <Label required>薪资结算日期</Label>
            <Input v-model="settlementDate" type="date" required />
          </div>
          <div class="flex flex-col">
            <Label>离职类型</Label>
            <Select v-model="quitType" :options="QUIT_TYPE_OPTIONS" @update:model-value="reason = ''" />
          </div>
        </template>

        <div v-if="positionChange || (action === 'quit' && quitType !== '3')" class="flex flex-col">
          <Label required>原因</Label>
          <Select v-model="reason" :options="reasonOptions" placeholder="请选择原因" />
        </div>

        <template v-if="positionChange">
          <div class="flex flex-col">
            <Label>调整后职级</Label>
            <Input v-model="postLevel" maxlength="255" />
          </div>
          <div class="flex flex-col">
            <Label>调整后工作地点</Label>
            <Input v-model="workAddress" maxlength="255" />
          </div>
        </template>

        <template v-if="action === 'rehire' || action === 'confirm-entry'">
          <div class="flex flex-col">
            <Label required>聘用形式</Label>
            <Select v-model="type" :options="TYPE_OPTIONS" />
          </div>
          <div v-if="type === '1'" class="flex flex-col">
            <Label required>试用期（月，0 表示无试用期）</Label>
            <Input v-model="probation" type="number" min="0" max="6" required />
          </div>
          <div v-else class="flex flex-col">
            <Label required>员工类型</Label>
            <Select v-model="status" :options="STATUS_OPTIONS" />
          </div>
        </template>

        <div class="flex flex-col sm:col-span-2">
          <Label>备注</Label>
          <Textarea v-model="remark" :rows="2" maxlength="500" />
        </div>
      </div>

      <Callout v-if="error" class="mt-4" tone="destructive">{{ error }}</Callout>

      <div class="mt-4 flex items-center justify-end gap-2">
        <Button type="button" variant="outline" :disabled="busy" @click="action = ''">取消</Button>
        <Button type="submit" :disabled="busy">
          <LoaderCircle v-if="busy" class="size-4 animate-spin" />
          {{ busy ? '提交中…' : '确认办理' }}
        </Button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { LoaderCircle } from '@lucide/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { session } from '../lib/session'
import Button from '../ui/Button.vue'
import Callout from '../ui/Callout.vue'
import Input from '../ui/Input.vue'
import Label from '../ui/Label.vue'
import Select from '../ui/Select.vue'
import Textarea from '../ui/Textarea.vue'

interface Employee { id: string; entryStatus: number | null; status: number | null; type: number | null; entryTime: string | null; postLevel: string | null; workAddress: string | null; probation?: number | null; deptId?: string | null; postName: string | null; leaderEmployeeId: string | null }

const props = defineProps<{ employee: Employee }>()
const emit = defineEmits<{ updated: [] }>()
const router = useRouter()
function startEmployment(mode: 'onboard' | 'offboard') {
  if (session.has('hrm:contract-template:query')) void router.push({ path: '/hr/employment', query: { mode, employeeId: props.employee.id } })
  else begin(mode === 'onboard' ? 'confirm-entry' : 'quit')
}

const labels: Record<string, string> = {
  'confirm-entry': '确认入职',
  regular: '办理转正',
  transfer: '登记任职调整',
  promote: '办理晋升',
  quit: '办理离职',
  rehire: '办理再入职',
  'cancel-quit': '取消待离职'
}

const QUIT_TYPE_OPTIONS = [
  { label: '主动离职', value: '1' },
  { label: '被动离职', value: '2' },
  { label: '退休', value: '3' }
]
const TYPE_OPTIONS = [
  { label: '正式', value: '1' },
  { label: '非正式', value: '2' }
]
const STATUS_OPTIONS = [
  { label: '实习', value: '3' },
  { label: '兼职', value: '4' },
  { label: '劳务', value: '5' },
  { label: '顾问', value: '6' },
  { label: '返聘', value: '7' },
  { label: '外包', value: '8' }
]

const action = ref('')
const effective = ref('')
const applyDate = ref('')
const settlementDate = ref('')
const reason = ref('')
const quitType = ref('1')
const postLevel = ref('')
const workAddress = ref('')
const remark = ref('')
const type = ref('1')
const status = ref('3')
const probation = ref('0')
const busy = ref(false)
const error = ref('')

const positionChange = computed(() => ['regular', 'transfer', 'promote'].includes(action.value))

const reasonOptions = computed(() => {
  const pairs: [number, string][] = action.value === 'quit'
    ? quitType.value === '1'
      ? [[1, '家庭原因'], [2, '身体原因'], [3, '薪资原因'], [4, '交通不便'], [5, '工作压力'], [6, '管理问题'], [7, '无晋升机会'], [8, '职业规划'], [9, '合同到期放弃续签'], [10, '其他个人原因']]
      : [[11, '试用期内辞退'], [12, '违反公司条例'], [13, '组织调整/裁员'], [14, '绩效不达标'], [15, '合同到期不续签'], [16, '其他原因']]
    : [[1, '组织架构调整'], [2, '个人申请'], [3, '工作安排']]
  return pairs.map(([value, label]) => ({ label, value: String(value) }))
})

function begin(key: string) {
  action.value = key
  effective.value = ''
  applyDate.value = ''
  settlementDate.value = ''
  reason.value = ''
  remark.value = ''
  error.value = ''
  postLevel.value = props.employee.postLevel || ''
  workAddress.value = props.employee.workAddress || ''
  type.value = String(props.employee.type || 1)
  status.value = String(props.employee.status && props.employee.status > 2 ? props.employee.status : 3)
  probation.value = String(props.employee.probation || 0)
}

const timestamp = (v: string) => v + 'T00:00:00'

async function submit() {
  busy.value = true
  error.value = ''
  try {
    const e = props.employee
    let body: Record<string, unknown> = { employeeId: e.id, remark: remark.value || null }
    if (positionChange.value) body = { ...body, reason: Number(reason.value), effectTime: timestamp(effective.value), newDeptId: e.deptId, newPostName: e.postName, newPostLevel: postLevel.value || null, newWorkAddress: workAddress.value || null, newLeaderEmployeeId: e.leaderEmployeeId }
    if (action.value === 'quit') body = { ...body, type: Number(quitType.value), reason: quitType.value === '3' ? null : Number(reason.value), planQuitTime: timestamp(effective.value), applyQuitTime: timestamp(applyDate.value), salarySettlementTime: timestamp(settlementDate.value) }
    if (action.value === 'confirm-entry' || action.value === 'rehire') body = { ...e, ...body, id: e.id, entryStatus: action.value === 'rehire' ? 1 : e.entryStatus, type: Number(type.value), status: type.value === '1' ? 1 : Number(status.value), probation: type.value === '1' ? Number(probation.value) : 0, entryTime: action.value === 'rehire' ? timestamp(effective.value) : e.entryTime, companyAgeStartTime: action.value === 'rehire' ? timestamp(effective.value) : undefined }
    if (action.value === 'cancel-quit' || action.value === 'confirm-entry') await api.put(`/admin-api/hrm/employee/${action.value}`, body)
    else await api.post(`/admin-api/hrm/employee/${action.value}`, body)
    action.value = ''
    emit('updated')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '办理失败'
  } finally {
    busy.value = false
  }
}
</script>
