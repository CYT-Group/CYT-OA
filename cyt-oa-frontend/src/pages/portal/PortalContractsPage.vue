<template>
  <div class="min-w-0 space-y-4">
    <PageHeader title="劳动合同生成" description="选择签约主体，填写员工、岗位及薪酬信息，生成 Word 合同。"><template #actions><Button variant="outline" size="sm" :disabled="busy" @click="openEntity()"><Building2 />新增签约主体</Button></template></PageHeader>
    <HrEmploymentNavigation />
    <Callout v-if="loadError" tone="destructive" title="合同配置加载失败">{{ loadError }}<template #action><Button variant="outline" size="sm" @click="loadConfig">重试</Button></template></Callout>
    <p v-if="loading" class="panel p-6 text-sm text-muted-foreground">正在加载合同模板配置…</p>
    <nav v-if="!loading" aria-label="合同填写分区" class="flex flex-wrap gap-1 border-b pb-3"><a v-for="section in contractSections" :key="section.id" :href="`#${section.id}`" class="rounded-lg px-3 py-2 text-xs text-muted-foreground transition-colors hover:bg-accent hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">{{ section.label }}</a></nav>
    <form v-if="!loading" id="contract-generate-form" class="grid items-start gap-4 xl:grid-cols-[minmax(0,1fr)_300px]" @submit.prevent="generate">
      <div class="min-w-0 space-y-4">
        <section id="contract-company-section" class="panel scroll-mt-24 p-5">
          <h2 class="panel-title mb-4">签约主体</h2>
          <div class="flex flex-wrap items-end gap-2"><div class="min-w-48 flex-1"><label for="contract-company" class="field-label">签约公司 *</label><Select id="contract-company" v-model="companyId" :options="entities.map(e => ({ value: e.id, label: e.name }))" placeholder="请选择签约主体" /></div><Button variant="outline" :disabled="!company" @click="company && openEntity(company)">编辑主体</Button></div>
          <div v-if="company" class="mt-3 rounded-lg bg-muted/40 p-3 text-xs leading-6 text-muted-foreground"><p>注册地址：{{ company.address }}</p><p>法定代表人：{{ company.legalRep }}</p></div>
          <Callout v-else class="mt-3" tone="warning">请先新增或选择签约主体。</Callout>
        </section>
        <section id="contract-employee-section" class="panel scroll-mt-24 p-5">
          <h2 class="panel-title mb-4">员工信息</h2>
          <div class="grid gap-4 sm:grid-cols-2">
            <div v-for="field in employeeFields" :key="field.key" :class="field.key === 'homeAddress' ? 'sm:col-span-2' : ''"><label :for="`contract-${field.key}`" class="field-label">{{ field.label }} *</label><Input :id="`contract-${field.key}`" v-model="employee[field.key]" :type="field.key === 'phone' ? 'tel' : 'text'" required :maxlength="field.max" :pattern="field.key === 'idNumber' ? '[0-9]{17}[0-9Xx]' : undefined" :placeholder="field.key === 'emergencyContact' ? '联系人姓名及联系电话' : undefined" autocomplete="off" /></div>
          </div>
        </section>
        <section id="contract-dates-section" class="panel scroll-mt-24 p-5">
          <div class="mb-4 flex flex-wrap items-center justify-between gap-2"><h2 class="panel-title">合同日期</h2><Button size="sm" variant="ghost" @click="calculateDates">按开始日计算期限</Button></div>
          <div class="grid gap-4 sm:grid-cols-2"><div v-for="field in dateFields" :key="field.key"><label :for="`contract-${field.key}`" class="field-label">{{ field.label }} *</label><Input :id="`contract-${field.key}`" v-model="dates[field.key]" type="date" required /></div></div>
          <p class="mt-3 text-xs text-muted-foreground">默认合同期限 {{ defaults.contractYears ?? 3 }} 年，试用期 {{ defaults.probationMonths ?? 3 }} 个月，可根据实际签约情况调整。</p>
        </section>
        <section id="contract-position-section" class="panel scroll-mt-24 p-5">
          <h2 class="panel-title mb-4">岗位与工作安排</h2>
          <div class="grid gap-4 sm:grid-cols-2"><div><label for="contract-position" class="field-label">岗位名称 *</label><Input id="contract-position" v-model="position.title" required maxlength="100" /></div><div><label for="contract-report-to" class="field-label">汇报对象 *</label><Input id="contract-report-to" v-model="position.reportTo" required maxlength="50" /></div><div class="sm:col-span-2"><label for="contract-city" class="field-label">工作城市 *</label><Input id="contract-city" v-model="position.workCity" list="contract-cities" required maxlength="50" /><datalist id="contract-cities"><option v-for="city in defaults.workCities ?? []" :key="city" :value="city" /></datalist></div><div class="sm:col-span-2"><label for="contract-schedule" class="field-label">工作时间安排 *</label><Textarea id="contract-schedule" v-model="position.workSchedule" required maxlength="2000" :rows="4" /></div><div class="sm:col-span-2"><label for="contract-duties" class="field-label">岗位职责 *</label><Textarea id="contract-duties" v-model="dutiesText" required :rows="6" placeholder="每行一条岗位职责，最多 20 条，每条最多 500 字" /></div></div>
        </section>
        <section id="contract-salary-section" class="panel scroll-mt-24 p-5">
          <h2 class="panel-title mb-4">薪酬约定 <span class="text-xs font-normal text-muted-foreground">元 / 月</span></h2>
          <div class="grid gap-4 sm:grid-cols-2"><div v-for="field in salaryFields" :key="field.key"><label :for="`contract-${field.key}`" class="field-label">{{ field.label }} *</label><Input :id="`contract-${field.key}`" v-model="salary[field.key]" class="text-right tabular-nums" type="number" :min="field.key === 'regularBase' ? 1 : 0" max="2147483647" step="1" required /></div></div>
        </section>
      </div>
      <aside class="panel space-y-4 p-5 xl:sticky xl:top-24">
        <h2 class="panel-title">生成前核对</h2>
        <dl class="space-y-3 text-sm"><div><dt class="text-xs text-muted-foreground">签约主体</dt><dd class="mt-1 font-medium">{{ company?.name || '未选择' }}</dd></div><div><dt class="text-xs text-muted-foreground">员工 / 岗位</dt><dd class="mt-1">{{ employee.name || '未填写' }} · {{ position.title || '未填写' }}</dd></div><div><dt class="text-xs text-muted-foreground">合同期限</dt><dd class="mt-1 text-xs">{{ dates.startDate }} 至 {{ dates.endDate }}</dd></div><div><dt class="text-xs text-muted-foreground">试用期固定月薪</dt><dd class="mt-1 tabular-nums">{{ money(totalProbation) }} 元</dd></div><div><dt class="text-xs text-muted-foreground">转正固定月薪</dt><dd class="mt-1 tabular-nums">{{ money(totalRegular) }} 元</dd></div><div><dt class="text-xs text-muted-foreground">绩效工资</dt><dd class="mt-1 tabular-nums">{{ money(Number(salary.performance)) }} 元</dd></div></dl>
        <p class="border-t pt-3 text-xs leading-6 text-muted-foreground">员工个人资料与薪酬仅用于本次生成，页面不会自动保存。下载后请核对合同内容。</p>
        <Callout v-if="generateError" tone="destructive">{{ generateError }}</Callout>
        <Button type="submit" class="w-full" :loading="busy" :disabled="!companyId || !!loadError"><FileDown />生成并下载 Word</Button>
      </aside>
    </form>
    <Dialog :open="entityOpen" :title="entityForm.id ? '编辑签约主体' : '新增签约主体'" @update:open="!entityBusy && (entityOpen = $event)">
      <form id="contract-entity-form" class="space-y-4" @submit.prevent="saveEntity"><Callout v-if="entityError" tone="destructive">{{ entityError }}</Callout><div><label for="contract-entity-name" class="field-label">公司名称 *</label><Input id="contract-entity-name" v-model="entityForm.name" required maxlength="100" /></div><div><label for="contract-entity-address" class="field-label">注册地址 *</label><Textarea id="contract-entity-address" v-model="entityForm.address" required maxlength="200" :rows="3" /></div><div><label for="contract-entity-rep" class="field-label">法定代表人 *</label><Input id="contract-entity-rep" v-model="entityForm.legalRep" required maxlength="50" /></div></form>
      <template #footer><Button variant="outline" :disabled="entityBusy" @click="entityOpen = false">取消</Button><Button type="submit" form="contract-entity-form" :loading="entityBusy">保存签约主体</Button></template>
    </Dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import HrEmploymentNavigation from '../../components/HrEmploymentNavigation.vue'
import { Building2, FileDown } from '@lucide/vue'
import { api } from '../../lib/api'
import { portalFile, savePortalFile } from '../../lib/portal-files'
import { errText, toast } from '../../lib/toast'
import Button from '../../ui/Button.vue'
import Callout from '../../ui/Callout.vue'
import Dialog from '../../ui/Dialog.vue'
import Input from '../../ui/Input.vue'
import PageHeader from '../../ui/PageHeader.vue'
import Select from '../../ui/Select.vue'
import Textarea from '../../ui/Textarea.vue'
interface Entity { id: number; name: string; address: string; legalRep: string }
interface Defaults { reportTo?: string; workCity?: string; workCities?: string[]; workSchedule?: string; contractYears?: number; probationMonths?: number }
const contractSections = [{ id: 'contract-company-section', label: '签约主体' }, { id: 'contract-employee-section', label: '员工信息' }, { id: 'contract-dates-section', label: '合同日期' }, { id: 'contract-position-section', label: '岗位安排' }, { id: 'contract-salary-section', label: '薪酬约定' }]
const base = '/admin-api/portal/contracts'
const entities = ref<Entity[]>([]), defaults = ref<Defaults>({}), companyId = ref<number | null>(null), loading = ref(false), loadError = ref(''), busy = ref(false), generateError = ref('')
const company = computed(() => entities.value.find(e => String(e.id) === String(companyId.value)))
const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
const employee = reactive({ name: '', idNumber: '', phone: '', homeAddress: '', emergencyContact: '' })
const employeeFields: { key: keyof typeof employee; label: string; max: number }[] = [{ key: 'name', label: '员工姓名', max: 50 }, { key: 'idNumber', label: '身份证号码', max: 18 }, { key: 'phone', label: '联系电话', max: 50 }, { key: 'homeAddress', label: '家庭住址', max: 200 }, { key: 'emergencyContact', label: '紧急联系人', max: 200 }]
const dates = reactive({ signDate: today, startDate: today, endDate: '', probationEnd: '' })
const dateFields: { key: keyof typeof dates; label: string }[] = [{ key: 'signDate', label: '签署日期' }, { key: 'startDate', label: '合同开始日' }, { key: 'endDate', label: '合同结束日' }, { key: 'probationEnd', label: '试用期截止日' }]
const position = reactive({ title: '', reportTo: '', workCity: '', workSchedule: '' }), dutiesText = ref('')
const salary = reactive({ probationBase: '0', probationAllowance: '0', regularBase: '', regularAllowance: '0', performance: '0' })
const salaryFields: { key: keyof typeof salary; label: string }[] = [{ key: 'probationBase', label: '试用期基本工资' }, { key: 'probationAllowance', label: '试用期岗位津贴' }, { key: 'regularBase', label: '转正基本工资' }, { key: 'regularAllowance', label: '转正岗位津贴' }, { key: 'performance', label: '绩效工资' }]
const totalProbation = computed(() => Number(salary.probationBase) + Number(salary.probationAllowance)), totalRegular = computed(() => Number(salary.regularBase) + Number(salary.regularAllowance))
const money = (value: number) => Number.isFinite(value) ? value.toLocaleString('zh-CN') : '–'
const entityOpen = ref(false), entityBusy = ref(false), entityError = ref(''), entityForm = reactive({ id: null as number | null, name: '', address: '', legalRep: '' })
let initialized = false
async function loadConfig() {
  loading.value = true; loadError.value = ''
  try { const result = await api.get<{ entities: Entity[]; defaults: Defaults }>(`${base}/config`); entities.value = result.entities; defaults.value = result.defaults; if (!companyId.value) companyId.value = result.entities[0]?.id ?? null; if (!initialized) { position.reportTo = result.defaults.reportTo ?? ''; position.workCity = result.defaults.workCity ?? ''; position.workSchedule = result.defaults.workSchedule ?? ''; calculateDates(); initialized = true } }
  catch (e) { loadError.value = errText(e) }
  finally { loading.value = false }
}
function calculateDates() {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dates.startDate)
  if (!match) return
  const y = Number(match[1]), m = Number(match[2]) - 1, d = Number(match[3])
  const after = (months: number) => { const date = new Date(Date.UTC(y, m + months, 1)); const last = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + 1, 0)).getUTCDate(); date.setUTCDate(Math.min(d, last)); date.setUTCDate(date.getUTCDate() - 1); return date.toISOString().slice(0, 10) }
  dates.endDate = after((defaults.value.contractYears ?? 3) * 12); dates.probationEnd = after(defaults.value.probationMonths ?? 3)
}
function openEntity(entity?: Entity) { Object.assign(entityForm, entity ? { ...entity, id: entity.id } : { id: null, name: '', address: '', legalRep: '' }); entityError.value = ''; entityOpen.value = true }
async function saveEntity() {
  if (entityBusy.value) return
  entityBusy.value = true; entityError.value = ''
  try { const body = { name: entityForm.name.trim(), address: entityForm.address.trim(), legalRep: entityForm.legalRep.trim() }; const saved = entityForm.id ? await api.put<Entity>(`${base}/entities/${entityForm.id}`, body) : await api.post<Entity>(`${base}/entities`, body); entityOpen.value = false; companyId.value = saved.id; await loadConfig(); toast('签约主体已保存') }
  catch (e) { entityError.value = errText(e) }
  finally { entityBusy.value = false }
}
async function generate() {
  if (busy.value || !companyId.value) return
  generateError.value = ''
  const duties = dutiesText.value.split(/\r?\n/).map(x => x.trim()).filter(Boolean)
  if (!duties.length || duties.length > 20 || duties.some(x => x.length > 500)) { generateError.value = '岗位职责须为 1 至 20 条，每条最多 500 字'; return }
  if (dates.endDate < dates.startDate || dates.probationEnd < dates.startDate || dates.probationEnd > dates.endDate) { generateError.value = '日期须满足：合同开始日 ≤ 试用期截止日 ≤ 合同结束日'; return }
  const salaryBody = Object.fromEntries(salaryFields.map(f => [f.key, Number(salary[f.key])]))
  if (Object.values(salaryBody).some(value => !Number.isInteger(value) || value < 0 || value > 2147483647) || salaryBody.regularBase < 1) { generateError.value = '请填写有效的非负整数薪酬，转正基本工资须大于零'; return }
  busy.value = true
  try { const result = await portalFile(`${base}/generate`, { companyId: companyId.value, employee: { ...employee }, dates: { ...dates }, position: { ...position, duties }, salary: salaryBody }, 'post'); savePortalFile(result.blob, result.filename || `劳动合同-${employee.name}-${dates.signDate}.docx`); toast('劳动合同已生成') }
  catch (e) { generateError.value = errText(e) }
  finally { busy.value = false }
}
onMounted(() => { void loadConfig() })
</script>
