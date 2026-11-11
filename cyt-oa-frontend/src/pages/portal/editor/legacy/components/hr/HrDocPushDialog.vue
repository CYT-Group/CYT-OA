<!--
  HrDocPushDialog — 推送企微弹框（v1.8：推送与审核彻底拆开后新增）

  背景：原来「推送」藏在审核通过里（勾了 notify_on_publish 就自动推），
  范围只能是文档的 dept_scope。拆开后推送变成列表页上一个独立动作，
  且范围可选：全员 / 按文档适用范围 / 指定员工（按名称搜索）。

  设计要点：
    - 三档用单选卡片（不是下拉）：三档语义差异大，平铺才看得清
    - 「指定员工」才出搜索框 —— 不做「列出全部员工」，
      搜索接口本身就是空关键词返回空列表（后端口径）
    - 上次推送时间直接摆在顶部：24h 频控是这个弹框最容易踩的坑，
      提前告知 + 给「忽略频控」的明确出口，比推完报错好
    - persistent：填到一半点遮罩不该关掉（尤其已经选了一堆人时）
-->
<template>
  <BaseModal v-model:open="open" title="推送企微" size="md" persistent>
    <div class="pd-doc">
      <div class="pd-doc-title">{{ docTitle || '（未填标题）' }}</div>
      <div class="pd-doc-meta">
        <span class="pd-ver">{{ version }}</span>
        <span v-if="lastPush && lastPush.pushed_at" class="pd-last">
          上次推送 {{ fmtTime(lastPush.pushed_at) }} · 成功 {{ lastPush.recipient_count }} 人<template
            v-if="lastPush.failed_count">（失败 {{ lastPush.failed_count }}）</template>
          · {{ lastPush.pushed_by }}
        </span>
        <span v-else class="pd-last">尚未推送过</span>
      </div>
      <!-- 小字提示：本文档适用于谁（比「会被上限拦下」的告警友好得多） -->
      <div class="pd-scope">{{ scopeHint }}</div>
    </div>

    <div class="pd-opts">
      <label
        v-for="o in OPTIONS"
        :key="o.value"
        class="pd-opt"
        :class="{ on: audience === o.value }"
      >
        <input v-model="audience" type="radio" :value="o.value">
        <span class="pd-opt-body">
          <span class="pd-opt-name">{{ o.name }}</span>
          <span class="pd-opt-desc">{{ o.desc }}</span>
        </span>
      </label>
    </div>

    <!-- 指定员工：按名称搜索 + 已选 chips -->
    <div v-if="audience === 'users'" class="pd-picker">
      <input
        v-model="keyword" class="pd-search" type="text"
        placeholder="输入姓名搜索（不列出全部员工）" @input="onSearchInput"
      >
      <div v-if="searching" class="pd-tip">搜索中…</div>
      <ul v-else-if="candidates.length" class="pd-cands">
        <li v-for="c in candidates" :key="c.username">
          <button
            type="button" class="pd-cand" :class="{ off: !c.is_active }"
            :disabled="!c.is_active" @click="toggle(c)"
          >
            <span class="pd-cand-name">{{ c.display_name }}</span>
            <span class="pd-cand-dept">{{ c.department.join(' · ') || '未设部门' }}</span>
            <!-- 非在职的也搜出来并说明原因：企微同步会把「未关注/已禁用」写成非在职，
                 只搜在职的话用户只会看到「搜不到」，以为是系统坏了 -->
            <span v-if="!c.is_active" class="pd-cand-off">已离职 / 未关注企微 · 不可推送</span>
            <span v-else class="pd-cand-act">{{ isPicked(c) ? '✓ 已选' : '+ 添加' }}</span>
          </button>
        </li>
      </ul>
      <div v-else-if="keyword.trim()" class="pd-tip">
        没搜到这个人。请确认账号已开通，以及姓名是否与企微登记一致；
        已有员工的姓名和部门可以在员工信息管理获取企微资料、核对差异后保存。
        <a class="pd-tip-link" href="/portal/employee-info" target="_blank" rel="noopener">去「员工信息管理」核对 →</a>
      </div>
      <div v-else class="pd-tip">输入姓名或工号开始搜索</div>

      <div v-if="picked.length" class="pd-picked">
        <span class="pd-picked-label">已选 {{ picked.length }} 人</span>
        <span v-for="p in picked" :key="p.username" class="pd-chip">
          {{ p.display_name }}
          <button type="button" aria-label="移除" @click="toggle(p)">✕</button>
        </span>
      </div>
    </div>

    <!-- ⚠️ 上限告警（保留告警形态）。原话是「文档没设适用范围时选按适用范围会被拦」——
         现在那种情况显示的是「全体员工」且不受上限约束，原话已不成立，
         所以按当前逻辑重写：上限只约束「按适用范围」与「指定员工」两档。 -->
    <p v-if="capWarning" class="pd-warn">{{ capWarning }}</p>
    <p v-if="withinCooldown" class="pd-warn">
      ⚠️ 该文档这个版本 24 小时内已经推过（防误连点）。确认仍要推的话，下方按钮会忽略频控再发一次。
    </p>
    <p v-if="error" class="pd-err">{{ error }}</p>

    <template #foot>
      <button class="btn" @click="open = false">取消</button>
      <button class="btn btn-primary" :disabled="!canSubmit" @click="submit">
        {{ busy ? '推送中…' : (withinCooldown ? '仍要推送（忽略频控）' : '确认推送') }}
      </button>
    </template>
  </BaseModal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import BaseModal from '../ui/BaseModal.vue'
import { articleApi, hrDocApi } from '../../../bridge/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  documentId: { type: Number, required: true },
  docTitle: { type: String, default: '' },
  version: { type: String, default: '' },
  /** 文档的适用范围，用来在选项里如实说明「会圈到谁」 */
  deptScope: { type: Array, default: () => [] },
  lastPush: { type: Object, default: null },
})
const emit = defineEmits(['update:open', 'pushed'])

const open = computed({
  get: () => props.open,
  set: (v) => emit('update:open', v),
})

const audience = ref('users')
const keyword = ref('')
const candidates = ref([])
const picked = ref([])
const searching = ref(false)
const busy = ref(false)
const error = ref('')
let timer = null

const hasScope = computed(() => !!(props.deptScope && props.deptScope.length))

// 第一档随文档**自适应**：设了适用范围就显示「按文档适用范围」，没设就显示「全体员工」。
// 两档指向同一批人时只留一个，避免用户面对两个其实一样的选项。
const OPTIONS = computed(() => {
  const first = hasScope.value
    ? { value: 'scope', name: '按文档适用范围', desc: `推给 ${props.deptScope.join(' · ')}` }
    : { value: 'all', name: '全体员工', desc: '推给公司在职的每一个人' }
  return [
    first,
    { value: 'users', name: '指定员工', desc: '按姓名点名推送，可跨部门' },
  ]
})

// 小字提示：这篇文档到底适用于谁（替代原来那段「会被 50 人上限拦下」的告警 ——
// 上限已放宽到 ≥ 公司人数，正常不会触发，不该拿它吓用户）
const scopeHint = computed(() => hasScope.value
  ? `本文档适用于 ${props.deptScope.join(' · ')}`
  : '本文档适用于全体员工')

// ⚠️ 上限告警：「按适用范围」与「指定员工」两档单次最多 50 人，超了会被后端拦下。
// 「全体员工」是明确选择，不受该上限约束，所以不提示。
const CAP = 50
const capWarning = computed(() => {
  if (audience.value === 'all') return ''
  if (audience.value === 'users') {
    return picked.value.length > CAP
      ? `⚠️ 已选 ${picked.value.length} 人，超过单次上限 ${CAP} 人，推送会被后端拦下 —— 请减少点名，或改用「全体员工」。`
      : ''
  }
  return `⚠️ 「按文档适用范围」单次最多推 ${CAP} 人，超过会被后端拦下。`
    + '需要通知更多人，请改用「全体员工」。'
})

/** 24h 内推过 → 提示频控；此时按钮改为「仍要推送（忽略频控）」 */
const withinCooldown = computed(() => {
  const t = props.lastPush?.pushed_at
  if (!t) return false
  return Date.now() - new Date(t).getTime() < 24 * 3600 * 1000
})

const canSubmit = computed(() => {
  if (busy.value) return false
  if (audience.value === 'users') return picked.value.length > 0
  return true
})

function fmtTime(iso) {
  try {
    const d = new Date(iso)
    const p = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
  } catch { return iso }
}

function isPicked(c) {
  return picked.value.some((p) => p.username === c.username)
}

function toggle(c) {
  if (!c.is_active) return   // 非在职的推不了，后端也会拒，别让它进已选
  const i = picked.value.findIndex((p) => p.username === c.username)
  if (i >= 0) picked.value.splice(i, 1)
  else picked.value.push(c)
}

// 搜索防抖 300ms：边打字边发请求既费流量也容易乱序覆盖
function onSearchInput() {
  clearTimeout(timer)
  timer = setTimeout(runSearch, 300)
}

async function runSearch() {
  const q = keyword.value.trim()
  if (!q) { candidates.value = []; return }
  searching.value = true
  try {
    const res = await hrDocApi.adminSearchPushUsers(q)
    candidates.value = res?.data || []
  } catch {
    candidates.value = []
  } finally {
    searching.value = false
  }
}

async function submit() {
  if (!canSubmit.value) return
  busy.value = true
  error.value = ''
  try {
    const payload = { audience: audience.value }
    if (audience.value === 'users') payload.user_ids = picked.value.map((p) => p.username)
    // 24h 内推过 → 用户在按钮上已明确表达「忽略频控」
    if (withinCooldown.value) payload.force = true
    const res = await articleApi.adminPush(props.documentId, payload)
    emit('pushed', res?.data || {})
    open.value = false
  } catch (e) {
    error.value = e.response?.data?.detail || e.message || '推送失败'
  } finally {
    busy.value = false
  }
}

// 每次打开重置：上一次选的档位/人选不该带到这一次
// 弹框由父级 v-if 挂载，挂载时 open 已经是 true；immediate 保证每次打开都先回到点名模式。
watch(() => props.open, (v) => {
  if (!v) return
  audience.value = 'users'
  keyword.value = ''
  candidates.value = []
  picked.value = []
  error.value = ''
  busy.value = false
}, { immediate: true })
</script>

<style scoped>
.pd-doc {
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(99, 102, 241, 0.05);
  margin-bottom: 14px;
}
.pd-doc-title { font-size: 14px; font-weight: 700; color: var(--t); margin-bottom: 4px; }
.pd-doc-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; font-size: 12px; color: var(--t4); }
.pd-ver {
  padding: 1px 6px;
  border-radius: 5px;
  background: rgba(99, 102, 241, 0.12);
  color: var(--v);
  font-weight: 600;
}

.pd-scope {
  margin-top: 6px;
  font-size: 11px;
  color: var(--t4, #8C92AB);
}

.pd-opts { display: flex; flex-direction: column; gap: 8px; }
.pd-opt {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid rgba(99, 102, 241, 0.16);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
}
.pd-opt:hover { border-color: rgba(99, 102, 241, 0.35); }
.pd-opt.on { border-color: var(--v); background: rgba(99, 102, 241, 0.06); }
.pd-opt input { margin-top: 2px; accent-color: var(--v); }
.pd-opt-body { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.pd-opt-name { font-size: 13px; font-weight: 600; color: var(--t2); }
.pd-opt-desc { font-size: 12px; color: var(--t4); }

.pd-picker { margin-top: 12px; }
.pd-search {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 8px;
  font-size: 13px;
  font-family: inherit;
  outline: none;
}
.pd-search:focus { border-color: var(--v); }
.pd-tip { padding: 10px 2px; font-size: 12px; line-height: 1.7; color: var(--t4); }
.pd-tip-link { display: inline-block; margin-top: 2px; color: var(--v, #6366F1); font-weight: 600; }

.pd-cands { list-style: none; margin: 8px 0 0; padding: 0; max-height: 180px; overflow-y: auto; }
.pd-cand {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 7px 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}
.pd-cand:hover { background: rgba(99, 102, 241, 0.07); }
.pd-cand-name { font-size: 13px; font-weight: 600; color: var(--t2); }
.pd-cand-dept { flex: 1; min-width: 0; font-size: 12px; color: var(--t4); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pd-cand-act { flex: none; font-size: 12px; color: var(--v); font-weight: 600; }
.pd-cand.off { cursor: not-allowed; }
.pd-cand.off .pd-cand-name { color: var(--t4, #8C92AB); font-weight: 500; }
.pd-cand-off {
  flex: none;
  padding: 1px 7px;
  border-radius: 999px;
  background: rgba(140, 146, 171, 0.14);
  color: var(--t4, #8C92AB);
  font-size: 11px;
}

.pd-picked { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.pd-picked-label { font-size: 12px; color: var(--t4); }
.pd-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 4px 2px 9px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.1);
  color: var(--v);
  font-size: 12px;
  font-weight: 600;
}
.pd-chip button {
  border: none;
  background: transparent;
  color: inherit;
  font-size: 11px;
  cursor: pointer;
  padding: 0 3px;
  opacity: 0.7;
}
.pd-chip button:hover { opacity: 1; }

.pd-warn {
  margin: 12px 0 0;
  padding: 9px 12px;
  border-radius: 9px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.28);
  font-size: 12px;
  line-height: 1.6;
  color: #92400e;
}
.pd-err {
  margin: 12px 0 0;
  padding: 9px 12px;
  border-radius: 9px;
  background: rgba(244, 63, 94, 0.08);
  border: 1px solid rgba(244, 63, 94, 0.25);
  font-size: 12px;
  color: #b91c1c;
}
</style>
