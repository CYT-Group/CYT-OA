# 畅盈通 OA · CYT OA

面向企业内部协同与人事管理的一体化系统。本目录为 **cyt-oa-frontend 前端**，
在同一套页面里承载员工「个人中心」与管理员「控制台」两个入口，
配合 `cyt-oa-backend` 后端使用。

## 主要功能

- **两级导航**：左侧按区块分组，一级项可展开二级项，二级项直接带查询参数落到对应视图。
- **个人中心**：概览摘要、大促与新品节奏日历、我的提醒、报告时间线与点评、
  自助办事、审批待办、我的团队、个人资料与足迹。
- **管理控制台**：组织与人事、薪酬与考勤、OA 台账、字典与权限等页面。
- **配置化台账页**：绝大多数增删改查由菜单项声明驱动，共用一个通用表格与表单组件，
  字段类型、字典下拉与权限按钮一并配置。
- **登录**：线上走企业微信扫码，本地开发保留账号密码入口；
  同一入口按身份自动落到工作台或控制台。

## 技术栈

Vue 3 · TypeScript · Vite 6 · Vue Router 4 · Tailwind CSS 4 · Reka UI · Lucide · Axios。
基础组件在 `src/ui/` 内维护，主题通过语义令牌统一，支持浅色和深色模式。

## 快速开始

```sh
npm install
npm run dev
```

访问 `http://localhost:4810`。开发服务默认把 `/admin-api`、`/api` 代理到
`http://127.0.0.1:48081`，联调其他后端实例时覆盖即可：

```sh
VITE_PROXY_TARGET=http://127.0.0.1:48082 npm run dev
```

## 构建

```sh
npm run build      # 先 vue-tsc 类型检查，再产出 dist/
npm run preview    # 本地预览构建产物
```

`dist/` 为静态产物，由后端或反向代理托管。

## 项目结构

```text
src/configs/       菜单与页面声明：控制台各板块、字段与路由都在这里配置
src/pages/         ShellPage 外壳、登录页、通用台账页；me/ 个人中心各视图
src/components/    导航、全局搜索、工作日历与通用表单
src/lib/           接口封装、会话、字典、格式化与提示
src/styles/        theme.css 设计令牌与公共组件，workspace.css 页面与外壳布局
```

## 文档

- [开发与验证](docs/development.md)：本地启动、类型检查与改动约定。
- 后端配置项含义见 `cyt-oa-backend/docs/configuration.md`。

## 参与开发

提交前至少跑一次 `npx vue-tsc --noEmit`，并在页面上手工走一遍受影响的流程。
不要把构建产物、本机环境变量与临时调试代码纳入提交。
