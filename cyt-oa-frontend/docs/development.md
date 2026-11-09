# 开发说明

## 本地启动

```sh
npm install
npm run dev      # http://localhost:4810
```

前端只做代理转发，不带后端地址的构建期常量：`vite.config.ts` 把 `/admin-api`、`/api`
代理到 `VITE_PROXY_TARGET`，默认 `http://127.0.0.1:48081`。

本服务固定使用 `48081`（后端）+ `4810`（前端），`strictPort` 已开启：端口被占用时直接报错，
不会静默跳到 4811。`8189/8190` 是 CYT-Common 的一对，`8080/8081` 属于网盘，不要混用。

## 检查

```sh
npx vue-tsc --noEmit    # 类型检查
npm run build           # 类型检查 + 构建，CI 里等价于这道命令
```

## 页面是怎么拼起来的

- `src/configs/pages.ts` 声明每一个台账页：接口地址、列、字段与操作按钮。
  新增一张后台表通常只在这里加一项，不需要写新组件。
- `src/configs/nav.ts` 声明左侧导航的区块、一级项与二级项；二级项是带查询参数的路由，
  所以同一个视图的不同标签在地址栏是可分享的。
- `src/pages/DataPage.vue` 消费上面两份声明，渲染表格、筛选与表单。
- 只有交互特殊的页面（个人中心各视图）才单独写组件。

字段的下拉选项来自后端字典接口，见 `src/lib/dict.ts`；不要在页面里硬编码枚举，
除非该枚举只用于前端展示分支。

## 接口与身份

请求统一走 `src/lib/api.ts`（管理端）与 `src/lib/me-api.ts`（个人中心）。
会话令牌、身份与权限判断集中在 `src/lib/session.ts`，路由守卫据此把非管理员挡在控制台外。

## 样式与组件

设计系统基于 Tailwind CSS 4 + Reka UI 无头组件，按 shadcn-vue 的组织方式维护：

- `src/styles/theme.css` 是唯一全局样式入口：设计令牌（颜色、圆角、动效）与
  `panel`、`data-table`、`field-label` 等组合类都在这里定义，深色模式走 `.dark` 类。
- `src/styles/workspace.css` 由主题入口导入，维护响应式外壳、工作台双栏、登录页与快捷入口。
  桌面侧栏支持收起和二级浮出菜单，移动端使用导航抽屉；个人工作台与管理控制台分别呈现导航。
- `src/ui/` 是基础组件（Button、Dialog、Select、Badge、Pagination、EmptyState…），
  用 `cn()`（clsx + tailwind-merge）合并类名、`cva` 声明变体。
- 图标统一用 `@lucide/vue`，提示统一用 `src/lib/toast.ts` 的函数式 toast。

新增页面优先组合 `src/ui/` 组件与 `theme.css` 的组合类，不要退回原生控件或另起一套
按钮/卡片样式；颜色一律引用语义令牌（`bg-card`、`text-muted-foreground` 等），
不要写死十六进制色值。

视觉参考 [Vben Admin](https://github.com/vbenjs/vue-vben-admin) 与
[Ant Design Pro](https://github.com/ant-design/ant-design-pro) 的后台导航和信息层次，
采用深色侧栏、浅灰内容底色、白色面板与蓝色强调。默认浅色，保留用户保存的主题偏好。
业务页面复用语义令牌，不叠加装饰性光晕；指标必须来自接口，加载失败使用「–」或错误提示，
不可把失败结果显示成零。工作日历 `WorkCalendar.vue` 提供月份切换和回到今天，
它只展示日期，不隐含业务事件或考勤安排。

表单控件需要关联 label、输入 ID 与校验消息；Select 和 MultiSelect 将附加属性转发到实际触发按钮。
界面变更应检查 1440px 桌面与 390px 窄屏、深色模式、搜索跳转、二级菜单和弹窗关闭，
联调时不要为验证界面而提交真实审批、考勤或通讯录同步操作。

## 提交前

- 跑一次类型检查，并在浏览器手工走一遍受影响的流程（含空数据与报错分支）。
- 确认没有把临时调试代码、`console.log` 与 `dist/` 提交进去。
- 页面文案只描述现有能力，不写「暂不支持」「待开发」。
