export type FieldType = 'text' | 'number' | 'textarea' | 'select' | 'members' | 'date' | 'datetime' | 'switch' | 'password'

export interface FieldConfig {
  key: string
  label: string
  type?: FieldType
  dictType?: string
  options?: { label: string; value: string }[]
  /** 选项来自接口（如部门负责人要选员工）：按 url 缓存，只拉一次 */
  optionsApi?: string
  /** 从接口每行取哪个字段当 label / value，默认取 label、value */
  optionLabel?: string
  optionValue?: string
  required?: boolean
  hint?: string
  placeholder?: string
  readonly?: boolean
  /** 出现在搜索条 */
  searchable?: boolean
  /** 搜索时用 select（静态选项或字典） */
  searchType?: FieldType
  /** 搜索条上那个空选项的文案：不填就是「全部」，值省了但语义有默认时要写准（如「已发布」） */
  blankLabel?: string
  /** 对象/数组字段：编辑框按 JSON 文本回填与解析，非法 JSON 当场报错不提交 */
  json?: boolean
}

export type Tone = 'ok' | 'warn' | 'bad' | 'info'

export interface ColumnConfig {
  key: string
  label: string
  width?: string
  /** 静态值映射，如 {0:'开启',1:'关闭'} */
  map?: Record<string, string>
  /** 状态徽章映射：值 → {文本, 色调} */
  statusMap?: Record<string, { text: string; tone: Tone }>
  dictType?: string
  /** 列宽自适应省略 */
  ellipsis?: boolean
}

export interface RowAction {
  permission?: string
  label: string
  api: string
  method?: 'get' | 'post' | 'put' | 'delete' | 'download'
  /** 需要填写的补充内容：{key,label,type}，会先弹输入框 */
  prompt?: { key: string; label: string; type?: FieldType; required?: boolean }
  confirm?: string
  tone?: 'pri' | 'dan' | 'gho'
  /** 自定义请求体（默认 {id: row.id} + prompt 值） */
  body?: (row: Record<string, unknown>) => Record<string, unknown>
  /** 只在符合条件的行上出现，比如草稿才能提交、审批中才能同步 */
  when?: (row: Record<string, unknown>) => boolean
  successMsg?: string
}

export interface PageAction {
  label: string
  api: string
  method?: 'get' | 'post' | 'put'
  prompt?: { key: string; label: string; type?: FieldType; required?: boolean }
  confirm?: string
  tone?: 'pri' | 'gho' | 'dan'
  body?: () => Record<string, unknown>
  successMsg?: string
}

export interface PageConfig {
  permission?: string
  createPermission?: string
  editPermission?: string
  deletePermission?: string
  key: string
  title: string
  desc?: string
  /** 分页查询接口（GET，参数 pageNo/pageSize + 搜索字段） */
  api: string
  search?: FieldConfig[]
  columns: ColumnConfig[]
  /** 新增/编辑表单字段；为空则不支持新增编辑 */
  fields?: FieldConfig[]
  createApi?: string
  updateApi?: string
  deleteApi?: string
  getApi?: string
  canCreate?: boolean
  canEdit?: boolean
  canDelete?: boolean
  rowActions?: RowAction[]
  pageActions?: PageAction[]
}

export interface MenuLeaf {
  permission?: string
  anyPermissions?: string[]
  key: string
  title: string
  icon: string
  /** 不写走通用的 /page/<key>；专用组件页面（看板一类）在这指自己的路由 */
  to?: string
}

export interface MenuGroup {
  portal?: boolean
  group: string
  items: MenuLeaf[]
}

/** 页签指向 pageConfigs 里的哪一份表格配置 */
export interface PageTab {
  key: string
  title: string
  config: string
}

/** 同一套流程/数据的多个视角：合成一页多页签，而不是拆成多个菜单项 */
export interface TabbedPage {
  title: string
  desc?: string
  tabs: PageTab[]
}
