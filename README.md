# MES-dome

面向面试的精简 **制造执行系统（MES）**：跑通车间主链路

```
ERP Mock 推单 → 工单状态机 → 主动派工 → 扫码报工（RocketMQ 异步）→ 自动完工 → 回传 ERP
```

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 4.1 · Java 17 · MyBatis-Plus · Redis/Redisson · RocketMQ 5.5 |
| 前端 | Vue 3 · Element Plus · Vite |
| 中间件 | MySQL 8 · Redis 7 · RocketMQ（Docker Compose） |

## 快速启动

```bash
# 1. 中间件（MySQL / Redis / RocketMQ）
docker compose up -d mysql redis namesrv broker

# 2. 后端
cd backend && mvn spring-boot:run

# 3. 前端
cd frontend && npm install && npm run dev
```

- 后端：http://localhost:8081  
- 前端：http://localhost:5173（`/api` 代理到 8081）  
- RocketMQ Dashboard：http://localhost:8082（若已启动 dashboard）

种子账号密码均为 `123456`：`admin` / `planner` / `worker1` / `qc1`。

> 若数据库是早期初始化的，种子工单可能只有 2 条。需要 10 条演示数据时可重建 MySQL 卷后重新 `docker compose up`，或手工执行 `mes_db.sql` 中工单相关 INSERT。

## 架构（简图）

```
Worker/Admin (Vue)
        │ JWT
        ▼
 Spring Boot API ── Redis（幂等 / 锁 / 看板缓存）
        │
        ├── MySQL（工单/派工/报工/集成日志）
        └── RocketMQ
              ├─ mes_report_submit  → 落库 + 状态机 + 质检任务
              └─ mes_erp_callback   → IntegrationLog + Mock ERP
```

## 演示路径（约 5 分钟）

1. `planner` 登录 → 派工给 `worker1`
2. `worker1` → 我的任务 → 扫码报工（前端生成 UUID 作为 `X-Request-Id`）
3. 同一 UUID 连点 → 幂等返回已有 reportId
4. 轮询至成功 → 工单状态自动流转；末工序满额 → 已完工
5. 管理端「集成对账」查看回传日志；失败可重试

## 目录

- `backend/` Spring Boot
- `frontend/` Vue 管理端 + 工人端
- `docs/面试讲稿.md` 短提纲
- `docs/面试完整问答.md` 完整面试问答（功能/技术/问题/深挖）
- `docs/D28-自测清单.md` 压测/防重检查表
- `MES-设计摘要.md` / `40天开发计划.md` 设计与排期

## 简历一句话

> 基于 Spring Boot 4 + Vue3 的 MES 车间执行系统，手写工单 7 态状态机与派工审计；报工采用 Redis 幂等 + Redisson 分布式锁 + RocketMQ 异步消费防重复；MyBatis-Plus 乐观锁保证并发安全；Mock ERP 验证推单、回传与 IntegrationLog 重试闭环。
