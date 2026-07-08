# MES-dome Frontend

Vue 3 + Element Plus + Vite 单项目，管理端与工人端通过路由区分。

## 快速启动

```bash
cd frontend
npm install
npm run dev
```

- 前端：http://localhost:5173
- 后端：http://localhost:8081（Vite 已将 `/api` 代理到后端）

## 演示账号

| 用户名 | 密码 | 跳转 |
|--------|------|------|
| admin / planner | 123456 | `/admin` |
| worker1 | 123456 | `/terminal` |

## 目录结构

```
src/
├── api/          # axios 封装、login/refresh/logout
├── stores/       # Pinia 登录态
├── router/       # 路由守卫
├── layouts/      # AdminLayout / TerminalLayout
└── views/        # login、admin、terminal 页面
```

## 设计规范

见项目根目录 [design-system/mes-dome/MASTER.md](../design-system/mes-dome/MASTER.md)（由 ui-ux-pro-max skill 生成）。

## 后续页面（排期）

| 天 | 页面 |
|----|------|
| D12 | `/admin/work-orders` 工单列表 + 详情 | **✅** |
| D13 | `/admin/dashboard` ECharts 看板 | **✅** |
| D19 | `/terminal/report` 扫码报工 |
