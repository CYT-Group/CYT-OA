import type { PageConfig, MenuGroup, TabbedPage } from './types'
import { portalMenus, portalPages } from './portal'

/** 业务单审批状态（BpmProcessInstanceStatusEnum） */
const APPLY_STATUS = {
  '-1': { text: '未提交', tone: 'info' },
  0: { text: '未提交', tone: 'info' },
  1: { text: '审批中', tone: 'warn' },
  2: { text: '已通过', tone: 'ok' },
  3: { text: '不通过', tone: 'bad' },
  4: { text: '已取消', tone: 'info' }
} as const

/** Portal 搬来的内容审批状态：draft 是自己没提，pending 等人审，rejected 是被打回 */
const PORTAL_STATUS = {
  draft: { text: '草稿', tone: 'info' },
  pending: { text: '待审批', tone: 'warn' },
  published: { text: '已发布', tone: 'ok' },
  rejected: { text: '已驳回', tone: 'bad' }
} as const

/** 状态下拉的 all 是后端专门为控制台加的一档：审阅者看全量，普通人只看到「已发布 + 自己的」 */
const PORTAL_STATUS_OPTIONS = [
  { label: '草稿', value: 'draft' },
  { label: '待审批', value: 'pending' },
  { label: '已发布', value: 'published' },
  { label: '已驳回', value: 'rejected' },
  { label: '全部（含他人草稿）', value: 'all' }
]

const portalStatusSearch = {
  key: 'status', label: '状态', type: 'select' as const, searchable: true,
  blankLabel: '已发布（默认）', options: PORTAL_STATUS_OPTIONS
}

const urgencySearch = { key: 'title', label: '标题', searchable: true, placeholder: '按标题搜索' }
const statusSearch = { key: 'status', label: '状态', type: 'select' as const, options: [
  { label: '未提交', value: '-1' }, { label: '审批中', value: '1' }, { label: '已通过', value: '2' },
  { label: '不通过', value: '3' }, { label: '已取消', value: '4' }
], searchable: true }

const urgencyField = { key: 'urgency', label: '紧急程度', type: 'select' as const, dictType: 'oa_apply_urgency', required: true }
const timeField = (key: string, label: string) => ({ key, label, type: 'datetime' as const, required: true })
const reasonField = { key: 'reason', label: '申请原因', type: 'textarea' as const, required: true }

export const pageConfigs: Record<string, PageConfig> = {
  ...portalPages,

  // ==================== 协同办公 ====================
  'oa-overtime': {
    key: 'oa-overtime', title: '加班申请', desc: '本人加班申请；审批在企业微信，结论点「同步状态」回读', api: '/admin-api/oa/overtime-apply/page',
    search: [urgencySearch, statusSearch],
    columns: [
      { key: 'title', label: '标题', ellipsis: true },
      { key: 'type', label: '加班类型', dictType: 'oa_overtime_type' },
      { key: 'startTime', label: '开始时间' },
      { key: 'endTime', label: '结束时间' },
      { key: 'days', label: '天数' },
      { key: 'status', label: '状态', statusMap: APPLY_STATUS },
      { key: 'createTime', label: '创建时间' }
    ],
    fields: [
      { key: 'title', label: '标题', required: true },
      urgencyField,
      { key: 'type', label: '加班类型', type: 'select', dictType: 'oa_overtime_type', required: true },
      timeField('startTime', '开始时间'), timeField('endTime', '结束时间'), reasonField
    ],
    createApi: '/admin-api/oa/overtime-apply/create',
    updateApi: '/admin-api/oa/overtime-apply/update',
    rowActions: [
      { label: '提交', api: '/admin-api/oa/overtime-apply/submit', method: 'post', tone: 'pri',
        confirm: '确认提交到企业微信审批？', successMsg: '已提交企业微信审批' },
      { label: '同步状态', api: '/api/me/applies/overtime/sync', method: 'put', tone: 'gho',
        successMsg: '已同步企业微信的审批结论', when: (row) => Number(row.status) === 1 }
    ]
  },
  'oa-leave': {
    key: 'oa-leave', title: '请假申请', desc: '本人请假申请；审批单发在企业微信，结论要点「同步状态」回读',
    api: '/admin-api/oa/leave-apply/page',
    search: [urgencySearch, statusSearch],
    columns: [
      { key: 'title', label: '标题', ellipsis: true },
      { key: 'type', label: '请假类型', dictType: 'oa_leave_type' },
      { key: 'startTime', label: '开始时间' },
      { key: 'endTime', label: '结束时间' },
      { key: 'days', label: '天数' },
      { key: 'status', label: '状态', statusMap: APPLY_STATUS },
      { key: 'wecomApprovalNo', label: '企微单号', ellipsis: true },
      { key: 'createTime', label: '创建时间' }
    ],
    fields: [
      { key: 'title', label: '标题', required: true },
      urgencyField,
      { key: 'type', label: '请假类型', type: 'select', dictType: 'oa_leave_type', required: true },
      timeField('startTime', '开始时间'), timeField('endTime', '结束时间'), reasonField,
      {
        key: 'approvers', label: '审批人', type: 'members', required: true,
        optionsApi: '/api/me/applies/approvers',
        hint: '可选择多位审批人，按选择顺序依次审批'
      }
    ],
    createApi: '/admin-api/oa/leave-apply/create',
    updateApi: '/admin-api/oa/leave-apply/update',
    rowActions: [
      { label: '提交', api: '/admin-api/oa/leave-apply/submit', method: 'post', tone: 'pri',
        confirm: '确认提交到企业微信审批？', successMsg: '已提交企业微信审批',
        when: (row) => Number(row.status) <= 0 },
      { label: '同步状态', api: '/api/me/applies/leave/sync', method: 'put', tone: 'gho',
        successMsg: '已同步企业微信的审批结论', when: (row) => Number(row.status) === 1 }
    ]
  },
  'oa-reimbursement': {
    key: 'oa-reimbursement', title: '费用报销', desc: '本人报销申请；审批在企业微信，结论点「同步状态」回读', api: '/admin-api/oa/reimbursement/page',
    search: [urgencySearch, statusSearch],
    columns: [
      { key: 'title', label: '标题', ellipsis: true },
      { key: 'paymentMethod', label: '支付方式', dictType: 'oa_reimbursement_payment_method' },
      { key: 'totalPrice', label: '报销总额' },
      { key: 'invoiceCount', label: '发票数' },
      { key: 'status', label: '状态', statusMap: APPLY_STATUS },
      { key: 'createTime', label: '创建时间' }
    ],
    fields: [
      { key: 'title', label: '标题', required: true },
      urgencyField,
      { key: 'witnessUserId', label: '见证人编号', type: 'number', hint: '可选' },
      { key: 'customerName', label: '客户名称' },
      { key: 'paymentMethod', label: '支付方式', type: 'select', dictType: 'oa_reimbursement_payment_method', required: true },
      reasonField
    ],
    createApi: '/admin-api/oa/reimbursement/create',
    updateApi: '/admin-api/oa/reimbursement/update',
    rowActions: [
      { label: '提交', api: '/admin-api/oa/reimbursement/submit', method: 'post', tone: 'pri',
        confirm: '确认提交到企业微信审批？', successMsg: '已提交企业微信审批' },
      { label: '同步状态', api: '/api/me/applies/reimbursement/sync', method: 'put', tone: 'gho',
        successMsg: '已同步企业微信的审批结论', when: (row) => Number(row.status) === 1 }
    ]
  },
  'oa-seal-apply': {
    key: 'oa-seal-apply', title: '用印申请', desc: '本人用印申请；审批在企业微信，结论点「同步状态」回读', api: '/admin-api/oa/seal-apply/page',
    search: [urgencySearch, statusSearch],
    columns: [
      { key: 'title', label: '标题', ellipsis: true },
      { key: 'useType', label: '用印类型', dictType: 'oa_seal_apply_type' },
      { key: 'useMode', label: '用印方式', dictType: 'oa_seal_use_mode' },
      { key: 'expectedCount', label: '用印次数' },
      { key: 'status', label: '状态', statusMap: APPLY_STATUS },
      { key: 'createTime', label: '创建时间' }
    ],
    fields: [
      { key: 'title', label: '标题', required: true },
      urgencyField,
      { key: 'useType', label: '用印类型', type: 'select', dictType: 'oa_seal_apply_type', required: true },
      { key: 'useMode', label: '用印方式', type: 'select', dictType: 'oa_seal_use_mode', required: true },
      { key: 'expectedCount', label: '用印次数', type: 'number', required: true },
      { key: 'expectedUseTime', label: '预计用印时间', type: 'datetime', required: true },
      reasonField
    ],
    createApi: '/admin-api/oa/seal-apply/create',
    updateApi: '/admin-api/oa/seal-apply/update',
    rowActions: [
      { label: '提交', api: '/admin-api/oa/seal-apply/submit', method: 'put', tone: 'pri',
        confirm: '确认提交到企业微信审批？', successMsg: '已提交企业微信审批' },
      { label: '同步状态', api: '/api/me/applies/seal/sync', method: 'put', tone: 'gho',
        successMsg: '已同步企业微信的审批结论', when: (row) => Number(row.status) === 1 }
    ]
  },
  'oa-seal': {
    key: 'oa-seal', title: '印章管理', desc: '公司印章台账', api: '/admin-api/oa/seal/page',
    search: [
      { key: 'name', label: '印章名称', searchable: true },
      { key: 'type', label: '类型', type: 'select', dictType: 'oa_seal_type', searchType: 'select', searchable: true }
    ],
    columns: [
      { key: 'name', label: '名称' },
      { key: 'no', label: '编号' },
      { key: 'type', label: '类型', dictType: 'oa_seal_type' },
      { key: 'category', label: '分类', dictType: 'oa_seal_category' },
      { key: 'keeperName', label: '保管人' },
      { key: 'status', label: '状态', dictType: 'oa_seal_status' },
      { key: 'enableTime', label: '启用时间' }
    ],
    fields: [
      { key: 'name', label: '名称', required: true },
      { key: 'no', label: '编号', required: true },
      { key: 'type', label: '类型', type: 'select', dictType: 'oa_seal_type', required: true },
      { key: 'category', label: '分类', type: 'select', dictType: 'oa_seal_category', required: true },
      { key: 'keeperUserId', label: '保管人编号', type: 'number', required: true },
      { key: 'status', label: '状态', type: 'select', dictType: 'oa_seal_status' },
      { key: 'enableTime', label: '启用时间', type: 'datetime' },
      { key: 'remark', label: '备注', type: 'textarea' }
    ],
    createApi: '/admin-api/oa/seal/create',
    updateApi: '/admin-api/oa/seal/update',
    deleteApi: '/admin-api/oa/seal/delete'
  },
  'oa-supply-apply': {
    key: 'oa-supply-apply', title: '用品领用申请', desc: '本人领用申请；审批在企业微信，结论点「同步状态」回读', api: '/admin-api/oa/supply-apply/page',
    search: [statusSearch],
    columns: [
      { key: 'no', label: '申请编号', ellipsis: true },
      { key: 'useType', label: '用途', dictType: 'oa_supply_category' },
      { key: 'pickupMethod', label: '领取方式', dictType: 'oa_supply_pickup_method' },
      { key: 'applyTime', label: '申请时间' },
      { key: 'status', label: '状态', statusMap: APPLY_STATUS },
      { key: 'createTime', label: '创建时间' }
    ],
    fields: [
      { key: 'applyTime', label: '申请时间', type: 'datetime', required: true },
      { key: 'useType', label: '用途', type: 'select', dictType: 'oa_supply_category', required: true },
      { key: 'pickupMethod', label: '领取方式', type: 'select', dictType: 'oa_supply_pickup_method', required: true },
      reasonField
    ],
    createApi: '/admin-api/oa/supply-apply/create',
    updateApi: '/admin-api/oa/supply-apply/update',
    rowActions: [
      { label: '提交', api: '/admin-api/oa/supply-apply/submit', method: 'put', tone: 'pri',
        confirm: '确认提交到企业微信审批？', successMsg: '已提交企业微信审批' },
      { label: '同步状态', api: '/api/me/applies/supply/sync', method: 'put', tone: 'gho',
        successMsg: '已同步企业微信的审批结论', when: (row) => Number(row.status) === 1 }
    ]
  },
  'oa-supply-issue': {
    key: 'oa-supply-issue', title: '领用发放', desc: '领用发放与归还管理', api: '/admin-api/oa/supply-issue/page',
    search: [
      { key: 'itemName', label: '用品名称', searchable: true },
      { key: 'status', label: '状态', searchable: true, type: 'select', options: [
        { label: '待发放', value: '0' }, { label: '已发放', value: '1' }, { label: '已归还', value: '2' }
      ] }
    ],
    columns: [
      { key: 'itemName', label: '用品' },
      { key: 'applyQuantity', label: '申请数量' },
      { key: 'issuedQuantity', label: '已发数量' },
      { key: 'returnedQuantity', label: '已还数量' },
      { key: 'status', label: '状态', statusMap: { 0: { text: '待发放', tone: 'warn' }, 1: { text: '已发放', tone: 'ok' }, 2: { text: '已归还', tone: 'info' } } },
      { key: 'issueUserName', label: '发放人' },
      { key: 'issueTime', label: '发放时间' }
    ],
    rowActions: [
      { label: '发放', api: '/admin-api/oa/supply-issue/issue', method: 'put', tone: 'pri',
        prompt: { key: 'issuedQuantity', label: '发放数量', type: 'number', required: true } },
      { label: '归还', api: '/admin-api/oa/supply-issue/return', method: 'put', tone: 'gho',
        prompt: { key: 'quantity', label: '归还数量', type: 'number', required: true } }
    ]
  },
  'oa-supply-item': {
    key: 'oa-supply-item', title: '办公用品', desc: '办公用品台账', api: '/admin-api/oa/supply-item/page',
    search: [
      { key: 'name', label: '名称', searchable: true },
      { key: 'category', label: '分类', type: 'select', dictType: 'oa_supply_category', searchType: 'select', searchable: true }
    ],
    columns: [
      { key: 'name', label: '名称' },
      { key: 'no', label: '编号' },
      { key: 'category', label: '分类', dictType: 'oa_supply_category' },
      { key: 'model', label: '型号' },
      { key: 'unit', label: '单位' },
      { key: 'stockQuantity', label: '库存' },
      { key: 'referencePrice', label: '参考价' },
      { key: 'status', label: '状态', map: { '0': '开启', '1': '禁用' } }
    ],
    fields: [
      { key: 'name', label: '名称', required: true },
      { key: 'no', label: '编号', required: true },
      { key: 'category', label: '分类', type: 'select', dictType: 'oa_supply_category', required: true },
      { key: 'model', label: '型号' },
      { key: 'unit', label: '单位' },
      { key: 'referencePrice', label: '参考价', type: 'number' },
      { key: 'stockQuantity', label: '库存数量', type: 'number' },
      { key: 'minStockQuantity', label: '最低库存', type: 'number' },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '开启', value: '0' }, { label: '禁用', value: '1' }] },
      { key: 'remark', label: '备注', type: 'textarea' }
    ],
    createApi: '/admin-api/oa/supply-item/create',
    updateApi: '/admin-api/oa/supply-item/update',
    deleteApi: '/admin-api/oa/supply-item/delete',
    rowActions: [
      { label: '入库', api: '/admin-api/oa/supply-item/stock-in', method: 'put', tone: 'pri',
        prompt: { key: 'quantity', label: '入库数量', type: 'number', required: true } }
    ]
  },
  'oa-travel-apply': {
    key: 'oa-travel-apply', title: '出差申请', desc: '本人出差申请；审批在企业微信，结论点「同步状态」回读', api: '/admin-api/oa/travel-apply/page',
    search: [statusSearch],
    columns: [
      { key: 'no', label: '编号', ellipsis: true },
      { key: 'startTime', label: '开始时间' },
      { key: 'endTime', label: '结束时间' },
      { key: 'days', label: '天数' },
      { key: 'estimatedPrice', label: '预计费用' },
      { key: 'status', label: '状态', statusMap: APPLY_STATUS },
      { key: 'createTime', label: '创建时间' }
    ],
    fields: [
      timeField('startTime', '开始时间'), timeField('endTime', '结束时间'),
      { key: 'companion', label: '同行人' },
      { key: 'estimatedPrice', label: '预计费用', type: 'number' },
      reasonField,
      { key: 'remark', label: '备注', type: 'textarea' }
    ],
    createApi: '/admin-api/oa/travel-apply/create',
    updateApi: '/admin-api/oa/travel-apply/update',
    rowActions: [
      { label: '提交', api: '/admin-api/oa/travel-apply/submit', method: 'post', tone: 'pri',
        confirm: '确认提交到企业微信审批？', successMsg: '已提交企业微信审批' },
      { label: '同步状态', api: '/api/me/applies/travel/sync', method: 'put', tone: 'gho',
        successMsg: '已同步企业微信的审批结论', when: (row) => Number(row.status) === 1 }
    ]
  },
  'oa-travel-reimbursement': {
    key: 'oa-travel-reimbursement', title: '出差报销', desc: '本人出差报销；审批在企业微信，结论点「同步状态」回读', api: '/admin-api/oa/travel-reimbursement/page',
    search: [statusSearch],
    columns: [
      { key: 'no', label: '编号', ellipsis: true },
      { key: 'transportType', label: '交通方式', dictType: 'oa_transport_type' },
      { key: 'totalPrice', label: '报销总额' },
      { key: 'status', label: '状态', statusMap: APPLY_STATUS },
      { key: 'createTime', label: '创建时间' }
    ],
    fields: [
      { key: 'travelApplyId', label: '关联出差申请编号', type: 'number', required: true, hint: '出差申请列表的编号' },
      { key: 'transportType', label: '交通方式', type: 'select', dictType: 'oa_transport_type', required: true },
      reasonField
    ],
    createApi: '/admin-api/oa/travel-reimbursement/create',
    updateApi: '/admin-api/oa/travel-reimbursement/update',
    rowActions: [
      { label: '提交', api: '/admin-api/oa/travel-reimbursement/submit', method: 'post', tone: 'pri',
        confirm: '确认提交到企业微信审批？', successMsg: '已提交企业微信审批' },
      { label: '同步状态', api: '/api/me/applies/travel-reimbursement/sync', method: 'put', tone: 'gho',
        successMsg: '已同步企业微信的审批结论', when: (row) => Number(row.status) === 1 }
    ]
  },
  'oa-announcement': {
    key: 'oa-announcement', title: '公告管理', desc: '公司公告发布', api: '/admin-api/oa/announcement/published-page',
    search: [{ key: 'title', label: '标题', searchable: true }],
    columns: [
      { key: 'title', label: '标题', ellipsis: true },
      { key: 'senderName', label: '发布人' },
      { key: 'publishTime', label: '发布时间' },
      { key: 'createTime', label: '创建时间' }
    ],
    fields: [
      { key: 'title', label: '标题', required: true },
      { key: 'content', label: '内容', type: 'textarea', required: true }
    ],
    createApi: '/admin-api/oa/announcement/create',
    updateApi: '/admin-api/oa/announcement/update',
    deleteApi: '/admin-api/oa/announcement/delete'
  },
  'oa-schedule': {
    key: 'oa-schedule', title: '日程', desc: '我的日程', api: '/admin-api/oa/schedule/my-page',
    columns: [
      { key: 'title', label: '标题', ellipsis: true },
      { key: 'type', label: '类型', map: { '1': '日程提醒', '2': '假日安排' } },
      { key: 'priority', label: '优先级', map: { '1': '一般', '2': '重要', '3': '紧急' } },
      { key: 'startTime', label: '开始时间' },
      { key: 'endTime', label: '结束时间' },
      { key: 'createTime', label: '创建时间' }
    ],
    fields: [
      { key: 'title', label: '标题', required: true },
      { key: 'type', label: '类型', type: 'select', required: true, options: [
        { label: '日程提醒', value: '1' }, { label: '假日安排', value: '2' }
      ] },
      { key: 'priority', label: '优先级', type: 'select', required: true, options: [
        { label: '一般', value: '1' }, { label: '重要', value: '2' }, { label: '紧急', value: '3' }
      ] },
      timeField('startTime', '开始时间'), timeField('endTime', '结束时间'),
      { key: 'description', label: '描述', type: 'textarea' },
      { key: 'remind', label: '是否提醒', type: 'switch' }
    ],
    createApi: '/admin-api/oa/schedule/create',
    updateApi: '/admin-api/oa/schedule/update',
    deleteApi: '/admin-api/oa/schedule/delete'
  },
  'oa-work-report': {
    key: 'oa-work-report', title: '工作汇报', desc: '我的工作汇报', api: '/admin-api/oa/work-report/page',
    search: [
      { key: 'type', label: '类型', type: 'select', options: [{ label: '日报', value: '1' }, { label: '周报', value: '2' }, { label: '月报', value: '3' }], searchable: true },
      { key: 'status', label: '状态', searchable: true, type: 'select', options: [
        { label: '草稿', value: '0' }, { label: '审批中', value: '1' }, { label: '已通过', value: '2' }, { label: '不通过', value: '3' }
      ] }
    ],
    columns: [
      { key: 'title', label: '标题', ellipsis: true },
      { key: 'no', label: '编号' },
      { key: 'type', label: '类型', map: { '1': '日报', '2': '周报', '3': '月报' } },
      { key: 'startTime', label: '开始时间' },
      { key: 'endTime', label: '结束时间' },
      { key: 'status', label: '状态', statusMap: APPLY_STATUS },
      { key: 'createTime', label: '创建时间' }
    ],
    fields: [
      { key: 'type', label: '类型', type: 'select', options: [{ label: '日报', value: '1' }, { label: '周报', value: '2' }, { label: '月报', value: '3' }], required: true },
      { key: 'title', label: '标题', required: true },
      timeField('startTime', '开始时间'), timeField('endTime', '结束时间'),
      { key: 'summary', label: '工作总结', type: 'textarea' },
      { key: 'plan', label: '工作计划', type: 'textarea' },
      { key: 'problem', label: '问题与风险', type: 'textarea' }
    ],
    createApi: '/admin-api/oa/work-report/create',
    updateApi: '/admin-api/oa/work-report/update',
    deleteApi: '/admin-api/oa/work-report/delete',
    rowActions: [
      { label: '提交', api: '/admin-api/oa/work-report/submit', method: 'put', tone: 'pri',
        confirm: '确认提交？', successMsg: '已提交' },
      { label: '导出 PDF', api: '/admin-api/oa/work-report/export', method: 'download', tone: 'gho' }
    ]
  },
  'oa-attendance': {
    key: 'oa-attendance', title: 'OA 考勤', desc: '我的考勤记录', api: '/admin-api/oa/attendance/my-page',
    columns: [
      { key: 'clockTime', label: '打卡时间' },
      { key: 'status', label: '状态', map: { '0': '正常', '1': '迟到', '2': '早退', '3': '缺卡' } },
      { key: 'remark', label: '备注', ellipsis: true },
      { key: 'createTime', label: '创建时间' }
    ],
    rowActions: [
      { label: '打卡', api: '/admin-api/oa/attendance/clock', method: 'post', tone: 'pri',
        confirm: '确认现在打卡？', successMsg: '打卡成功', body: (row) => ({ id: row.id }) }
    ]
  },

  // ==================== 人事管理 ====================
  'hrm-employee': {
    key: 'hrm-employee', title: '员工档案', desc: '企微身份与人事档案关联管理，任职状态与账号状态独立维护',
    api: '/admin-api/hrm/employee/page',
    search: [
      { key: 'name', label: '姓名', searchable: true },
      { key: 'jobNumber', label: '工号', searchable: true },
      { key: 'mobile', label: '手机号', searchable: true },
      { key: 'status', label: '状态', type: 'select', dictType: 'hrm_employee_status', searchType: 'select', searchable: true }
    ],
    columns: [
      { key: 'name', label: '姓名' },
      { key: 'jobNumber', label: '工号' },
      { key: 'mobile', label: '手机号' },
      { key: 'postName', label: '岗位' },
      { key: 'deptName', label: '部门' },
      { key: 'entryTime', label: '入职日期' },
      { key: 'status', label: '状态', dictType: 'hrm_employee_status' }
    ],
    fields: [
      { key: 'name', label: '姓名', required: true, hint: '以企微通讯录为准，同步后会被覆盖' },
      { key: 'mobile', label: '手机号', required: true, hint: '以企微通讯录为准，同步后会被覆盖' },
      { key: 'entryStatus', label: '入职状态', type: 'select', dictType: 'hrm_employee_entry_status', required: true },
      { key: 'type', label: '聘用形式', type: 'select', dictType: 'hrm_employee_type', required: true },
      { key: 'entryTime', label: '入职日期', type: 'date', required: true },
      { key: 'jobNumber', label: '工号' },
      { key: 'probation', label: '试用期（月）', type: 'number' },
      { key: 'regularTime', label: '转正日期', type: 'date' },
      { key: 'leaveTime', label: '离职日期', type: 'date' },
      { key: 'postName', label: '职位名称' },
      { key: 'postLevel', label: '岗位职级' },
      { key: 'workCity', label: '工作城市' },
      { key: 'workAddress', label: '工作地址' },
      { key: 'birthday', label: '生日', type: 'date' },
      { key: 'idType', label: '证件类型', type: 'select', dictType: 'hrm_employee_id_type' },
      { key: 'idNumber', label: '证件号码' },
      { key: 'highestEducation', label: '最高学历', type: 'select', dictType: 'hrm_employee_education' },
      { key: 'nativePlace', label: '籍贯' },
      { key: 'address', label: '户籍地址' },
      { key: 'status', label: '状态', type: 'select', dictType: 'hrm_employee_status' },
      { key: 'remark', label: '备注', type: 'textarea' }
    ],
    updateApi: '/admin-api/hrm/employee/update',
    canCreate: false, canDelete: false
  },
  'hrm-clock': {
    key: 'hrm-clock', title: '打卡记录', desc: '员工打卡明细', api: '/admin-api/hrm/attendance/clock/page',
    search: [
      { key: 'employeeName', label: '员工', searchable: true },
      { key: 'status', label: '状态', type: 'select', dictType: 'hrm_attendance_clock_status', searchType: 'select', searchable: true }
    ],
    columns: [
      { key: 'employeeName', label: '员工' },
      { key: 'jobNumber', label: '工号' },
      { key: 'deptName', label: '部门' },
      { key: 'clockTime', label: '打卡时间' },
      { key: 'type', label: '类型', dictType: 'hrm_attendance_clock_type' },
      { key: 'status', label: '状态', dictType: 'hrm_attendance_clock_status' },
      { key: 'address', label: '地点', ellipsis: true }
    ],
    fields: [
      { key: 'employeeId', label: '员工编号', type: 'number', required: true },
      timeField('clockTime', '打卡时间'),
      { key: 'type', label: '类型', type: 'select', dictType: 'hrm_attendance_clock_type', required: true },
      { key: 'address', label: '地点' },
      { key: 'remark', label: '备注', type: 'textarea' }
    ],
    createApi: '/admin-api/hrm/attendance/clock/create',
    updateApi: '/admin-api/hrm/attendance/clock/update',
    deleteApi: '/admin-api/hrm/attendance/clock/delete'
  },
  'hrm-group': {
    key: 'hrm-group', title: '考勤组', desc: '考勤组配置', api: '/admin-api/hrm/attendance/group/page',
    search: [{ key: 'name', label: '名称', searchable: true }],
    columns: [
      { key: 'name', label: '名称' },
      { key: 'deptNames', label: '部门范围', ellipsis: true },
      { key: 'employeeNames', label: '员工范围', ellipsis: true },
      { key: 'defaultStatus', label: '默认组', map: { 'true': '是', 'false': '否' } },
      { key: 'createTime', label: '创建时间' }
    ],
    fields: [
      { key: 'name', label: '名称', required: true },
      { key: 'openWifiCard', label: 'WiFi 打卡', type: 'switch' },
      { key: 'openPointCard', label: '定位打卡', type: 'switch' },
      { key: 'rest', label: '法定节假日休息', type: 'switch' }
    ],
    createApi: '/admin-api/hrm/attendance/group/create',
    updateApi: '/admin-api/hrm/attendance/group/update',
    deleteApi: '/admin-api/hrm/attendance/group/delete'
  },
  'hrm-holiday': {
    key: 'hrm-holiday', title: '考勤节假日', desc: '节假日与调休配置', api: '/admin-api/hrm/attendance/holiday/page',
    columns: [
      { key: 'date', label: '日期' },
      { key: 'type', label: '类型', dictType: 'hrm_attendance_holiday_type' },
      { key: 'createTime', label: '创建时间' }
    ],
    fields: [
      { key: 'date', label: '日期', type: 'date', required: true },
      { key: 'type', label: '类型', type: 'select', dictType: 'hrm_attendance_holiday_type', required: true }
    ],
    createApi: '/admin-api/hrm/attendance/holiday/create',
    updateApi: '/admin-api/hrm/attendance/holiday/update',
    deleteApi: '/admin-api/hrm/attendance/holiday/delete'
  },

  // ==================== 系统管理 ====================
  'sys-users': {
    key: 'sys-users', title: '账号管理', desc: '员工账号（由企微通讯录同步产生）', api: '/admin-api/system/users',
    search: [
      { key: 'username', label: '账号', searchable: true },
      { key: 'name', label: '姓名', searchable: true }
    ],
    columns: [
      { key: 'username', label: '账号' },
      { key: 'name', label: '姓名' },
      { key: 'position', label: '职位' },
      { key: 'mainDeptName', label: '部门' },
      { key: 'mobile', label: '手机号' },
      { key: 'enabled', label: '状态', statusMap: { 'true': { text: '启用', tone: 'ok' }, 'false': { text: '停用', tone: 'bad' } } },
      { key: 'lastLoginAt', label: '最近登录' }
    ],
    canCreate: false, canEdit: false, canDelete: false
  },
  'sys-dict': {
    key: 'sys-dict', title: '数据字典', desc: '字典类型与数据项', api: '/admin-api/system/dict/types',
    columns: [
      { key: 'name', label: '类型名称' },
      { key: 'type', label: '类型标识' },
      { key: 'remark', label: '备注', ellipsis: true }
    ],
    canCreate: false, canEdit: false, canDelete: false
  },

  // ==================== 门户内容（迁自 Portal 管理面） ====================
  'portal-news': {
    key: 'portal-news', title: '公司动态', desc: 'Portal 搬来的公司动态：草稿、待审队列与发布',
    api: '/admin-api/portal/news/page', getApi: '/admin-api/portal/news/get',
    search: [
      { key: 'q', label: '关键词', searchable: true, placeholder: '标题 / 摘要 / 正文' },
      portalStatusSearch,
      { key: 'category', label: '分类', searchable: true, placeholder: '精确匹配，如 company' }
    ],
    columns: [
      { key: 'title', label: '标题', ellipsis: true },
      { key: 'category', label: '分类' },
      { key: 'templateType', label: '稿型' },
      { key: 'author', label: '作者' },
      { key: 'status', label: '状态', statusMap: PORTAL_STATUS },
      { key: 'isFeatured', label: '精选', map: { true: '★ 置顶', false: '—' } },
      { key: 'publishedAt', label: '发布时间' },
      { key: 'createdAt', label: '创建时间' }
    ],
    fields: [
      { key: 'title', label: '标题', required: true },
      { key: 'category', label: '分类', hint: '文本分类，与分类树无关', placeholder: 'company' },
      { key: 'templateType', label: '稿型', placeholder: 'simple' },
      { key: 'coverImage', label: '封面地址' },
      { key: 'summary', label: '摘要', type: 'textarea', hint: '列表页展示，留空时前端回退正文首段' },
      { key: 'content', label: '正文', type: 'textarea', hint: '模板化稿件可空，内容在数据块里' },
      { key: 'blocks', label: '模板数据块', type: 'textarea', json: true, hint: 'JSON 对象，前端按 blocks.dept 之类取值' },
      { key: 'isFeatured', label: '精选（前端兼作置顶）', type: 'switch' },
      { key: 'notifyWecom', label: '发布时推送企微', type: 'switch', hint: '仅新建时生效；走审批的稿存成待确认意图' },
      { key: 'notifyDeptScope', label: '推送部门范围', type: 'textarea', json: true, hint: '仅新建时生效；JSON 数组，留空 = 全员' }
    ],
    createApi: '/admin-api/portal/news/create',
    updateApi: '/admin-api/portal/news/update',
    deleteApi: '/admin-api/portal/news/delete',
    rowActions: [
      { label: '通过', api: '/admin-api/portal/news/approve', tone: 'pri',
        prompt: { key: 'comment', label: '审批意见' }, successMsg: '已通过并发布' },
      { label: '驳回', api: '/admin-api/portal/news/reject', tone: 'dan',
        prompt: { key: 'comment', label: '驳回原因', required: true }, successMsg: '已驳回' }
    ]
  },
  'portal-wiki': {
    key: 'portal-wiki', title: '知识库', desc: 'Portal 搬来的制度与流程文档',
    api: '/admin-api/portal/wiki/page', getApi: '/admin-api/portal/wiki/get',
    search: [
      { key: 'q', label: '关键词', searchable: true, placeholder: '标题 / 正文' },
      portalStatusSearch,
      { key: 'category', label: '分类', searchable: true, placeholder: '精确匹配，如 finance' },
      { key: 'department', label: '归属部门', searchable: true, placeholder: '精确匹配' }
    ],
    columns: [
      { key: 'title', label: '标题', ellipsis: true },
      { key: 'category', label: '分类' },
      { key: 'department', label: '归属部门' },
      { key: 'tags', label: '标签' },
      { key: 'authorUsername', label: '作者' },
      { key: 'viewCount', label: '浏览量' },
      { key: 'status', label: '状态', statusMap: PORTAL_STATUS },
      { key: 'createdAt', label: '创建时间' }
    ],
    fields: [
      { key: 'title', label: '标题', required: true },
      { key: 'category', label: '分类', placeholder: 'finance' },
      { key: 'department', label: '归属部门', hint: '可见范围口径，不是分类树的行' },
      { key: 'docUrl', label: '外链文档地址' },
      { key: 'tags', label: '标签', type: 'textarea', json: true, hint: 'JSON 数组，如 ["报销","差旅"]' },
      { key: 'content', label: '正文', type: 'textarea', hint: '只给外链时可以留空' },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '存草稿', value: 'draft' }],
        blankLabel: '按角色派发（审阅者直接发布，其余进待审批）', hint: '只有「存草稿」这档生效' }
    ],
    createApi: '/admin-api/portal/wiki/create',
    updateApi: '/admin-api/portal/wiki/update',
    deleteApi: '/admin-api/portal/wiki/delete',
    rowActions: [
      { label: '通过', api: '/admin-api/portal/wiki/approve', tone: 'pri',
        prompt: { key: 'comment', label: '审批意见' }, successMsg: '已通过' },
      { label: '驳回', api: '/admin-api/portal/wiki/reject', tone: 'dan',
        prompt: { key: 'comment', label: '驳回原因', required: true }, successMsg: '已驳回' }
    ]
  },
  'portal-training': {
    key: 'portal-training', title: '培训课程', desc: 'Portal 搬来的培训课程：手建草稿、课时与发布下架',
    api: '/admin-api/portal/training/page', getApi: '/admin-api/portal/training/get',
    search: [
      { key: 'q', label: '关键词', searchable: true, placeholder: '标题 / 简介' },
      { key: 'status', label: '状态', type: 'select', searchable: true, blankLabel: '全部（含草稿）', options: [
        { label: '草稿', value: 'draft' }, { label: '已发布', value: 'published' }
      ] },
      { key: 'category', label: '分类', searchable: true, placeholder: '精确匹配小类名' }
    ],
    columns: [
      { key: 'id', label: 'ID' },
      { key: 'title', label: '标题', ellipsis: true },
      { key: 'category', label: '分类' },
      { key: 'level', label: '级别', map: { required: '必修', elective: '选修' } },
      { key: 'lessonCount', label: '课时数' },
      { key: 'status', label: '状态', statusMap: PORTAL_STATUS },
      { key: 'createdAt', label: '创建时间' }
    ],
    fields: [
      { key: 'title', label: '标题', required: true },
      { key: 'category', label: '分类', required: true, hint: '不存在会自动建小类' },
      { key: 'level', label: '级别', type: 'select', options: [
        { label: '必修', value: 'required' }, { label: '选修', value: 'elective' }
      ] },
      { key: 'deptScope', label: '可见部门', type: 'textarea', json: true, hint: '部门名 JSON 数组，留空 = 全员' },
      { key: 'description', label: '简介', type: 'textarea' },
      { key: 'coverImage', label: '封面地址' },
      { key: 'instructor', label: '讲师' },
      { key: 'sortOrder', label: '排序', type: 'number' }
    ],
    createApi: '/admin-api/portal/training/create',
    updateApi: '/admin-api/portal/training/update',
    deleteApi: '/admin-api/portal/training/delete',
    rowActions: [
      { label: '发布', api: '/admin-api/portal/training/publish', method: 'post', tone: 'pri',
        confirm: '课程至少要有一个课时才能发布', successMsg: '已发布' },
      { label: '下架', api: '/admin-api/portal/training/unpublish', method: 'post', tone: 'gho',
        successMsg: '已下架' }
    ]
  },
  'portal-events': {
    key: 'portal-events', title: '展会日历', desc: '维护展会与活动安排，方便员工了解时间、地点和参与信息。',
    api: '/admin-api/portal/events/page', getApi: '/admin-api/portal/events/get',
    search: [
      { key: 'q', label: '关键词', searchable: true, placeholder: '标题 / 地点' }
    ],
    columns: [
      { key: 'title', label: '标题', ellipsis: true },
      { key: 'location', label: '地点' },
      { key: 'startDate', label: '开始日期' },
      { key: 'endDate', label: '结束日期' },
      { key: 'eventType', label: '活动类型', map: { exhibition: '展会', holiday: '节假日', company: '公司活动', training: '培训' } },
      { key: 'createdAt', label: '创建时间' }
    ],
    fields: [
      { key: 'title', label: '标题', required: true },
      { key: 'location', label: '地点' },
      { key: 'startDate', label: '开始日期', type: 'date', hint: '新建缺省今天' },
      { key: 'endDate', label: '结束日期', type: 'date' },
      { key: 'eventType', label: '活动类型', placeholder: '如 exhibition（展会）' },
      { key: 'description', label: '介绍', type: 'textarea' }
    ],
    createApi: '/admin-api/portal/events/create',
    updateApi: '/admin-api/portal/events/update',
    deleteApi: '/admin-api/portal/events/delete'
  },
  'portal-products': {
    key: 'portal-products', title: '新品动态', desc: '维护新品资料、上市日期和详情链接，设置员工关注的精选产品。',
    api: '/admin-api/portal/products/page', getApi: '/admin-api/portal/products/get',
    search: [
      { key: 'q', label: '关键词', searchable: true, placeholder: '名称 / 介绍' },
      { key: 'brand', label: '品牌', searchable: true, placeholder: '精确匹配' }
    ],
    columns: [
      { key: 'name', label: '名称', ellipsis: true },
      { key: 'brand', label: '品牌' },
      { key: 'category', label: '分类' },
      { key: 'launchDate', label: '发布日期' },
      { key: 'isFeatured', label: '精选', map: { true: '★ 置顶', false: '—' } },
      { key: 'createdAt', label: '创建时间' }
    ],
    fields: [
      { key: 'name', label: '名称', required: true },
      { key: 'brand', label: '品牌' },
      { key: 'category', label: '分类' },
      { key: 'launchDate', label: '发布日期', type: 'date' },
      { key: 'imageUrl', label: '图片地址' },
      { key: 'linkUrl', label: '详情链接' },
      { key: 'isFeatured', label: '精选置顶', type: 'switch' },
      { key: 'description', label: '介绍', type: 'textarea' }
    ],
    createApi: '/admin-api/portal/products/create',
    updateApi: '/admin-api/portal/products/update',
    deleteApi: '/admin-api/portal/products/delete'
  }
}

/**
 * 页签组：把同一套数据的多个视角合成一页（DataPage 里切页签）。
 * 目前没有页签组，机制留给以后需要多视角的台账页复用。
 */
export const tabbedPages: Record<string, TabbedPage> = {}

/**
 * 管理区菜单分组。个人视角的页面（请假/加班/报销/出差/用印/领用/日程/汇报）不在这里——
 * 那些都在「我要办事」里，管理区只放台账、配置与人事职责页。
 */
export const menuGroups: MenuGroup[] = [
  {
    group: '办公台账',
    items: [
      { key: 'oa-announcement', title: '公告管理', icon: 'megaphone' },
      { key: 'oa-seal', title: '印章管理', icon: 'stamp' },
      { key: 'oa-supply-issue', title: '领用发放', icon: 'truck' },
      { key: 'oa-supply-item', title: '办公用品', icon: 'box' }
    ]
  },
  {
    group: '人事管理',
    items: [
      { key: 'org', title: '组织架构', icon: 'building', to: '/org' },
      { key: 'hrm-employee', title: '员工档案', icon: 'user' },
      { key: 'hrm-clock', title: '打卡记录', icon: 'clock' },
      { key: 'hrm-group', title: '考勤组', icon: 'users' },
      { key: 'hrm-holiday', title: '节假日', icon: 'gift' }
    ]
  },
  ...portalMenus,
  {
    group: '系统管理',
    items: [
      { key: 'sys-users', title: '账号管理', icon: 'key' },
      { key: 'wecom-templates', title: '审批模板', icon: 'clipboard', to: '/wecom-templates' },
      { key: 'sys-dict', title: '数据字典', icon: 'book' }
    ]
  }
]

for (const [key, permission] of Object.entries({
  'portal-news': 'can_enter_admin', 'portal-wiki': 'can_enter_admin',
  'portal-training': 'can_manage_training', 'portal-events': 'can_manage_events', 'portal-products': 'can_manage_products'
})) {
  const config = pageConfigs[key]!
  config.permission = permission
  config.createPermission = ['portal-news', 'portal-wiki'].includes(key) ? 'can_create_content' : permission
  config.deletePermission = 'can_delete'
  for (const action of config.rowActions ?? []) {
    action.permission = ['portal-news', 'portal-wiki'].includes(key) ? 'can_review' : permission
    if (['portal-news', 'portal-wiki'].includes(key)) action.when = (row) => row.status === 'pending'
  }
}
