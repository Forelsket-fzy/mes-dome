# CLAUDE.md — MES-dome 项目上下文

> 本文件供 AI 助手（Claude/Cursor 等）快速理解项目全貌。详细设计见 [MES-设计摘要.md](./MES-设计摘要.md)，开发排期见 [40天开发计划.md](./40天开发计划.md)。

---

## 1. 项目是什么

**MES-dome** 是一个面向面试展示的 **制造执行系统（MES）** 精简版，跑通车间执行主链路：

```
ERP Mock 推单 → 工单状态机 → 主动派工 → 扫码报工（MQ 异步）→ 自动完工 → 回传 ERP
```

**项目定位**：应届生 40 天面试项目。核心后端业务**手写**，Vue 管理端/看板 UI **交给 AI 生成**。

**一句话简历描述**：

> 基于 Spring Boot 4 + Vue3 的 MES 车间执行系统，手写工单 7 态状态机与派工审计；报工采用 Redis 幂等 + Redisson 分布式锁 + RocketMQ 异步消费防重复；MyBatis-Plus 乐观锁保证并发安全；Mock ERP 验证推单、回传与 IntegrationLog 重试闭环。

---

## 2. 技术栈（已确定，勿随意更换）

| 层 | 技术 | 版本/说明 |
|----|------|-----------|
| 后端 | Spring Boot | **4.1.0**（注意：Jackson 3、starter 命名有变） |
| 语言 | Java | 17 |
| ORM | MyBatis-Plus | 3.5.15，`mybatis-plus-spring-boot4-starter` |
| 数据库 | MySQL | 8.0，库名 `mes_db` |
| 缓存/锁 | Redis + Redisson | Redisson **4.6.1+**（SB4 必须此版本） |
| 消息队列 | RocketMQ | 5.5 原生 `rocketmq-client`（无 Spring Boot Starter） |
| 鉴权 | Spring Security + Auth0 JWT | `java-jwt` 4.4.0 |
| AOP | spring-boot-starter-**aspectj** | SB4 已改名，**没有** `spring-boot-starter-aop` |
| 前端（计划） | Vue 3 + Element Plus + Vite | 尚未初始化 |
| 部署 | Docker Compose | MySQL + Redis（RocketMQ 待补） |

### Spring Boot 4 踩坑备忘

- AOP 依赖：`spring-boot-starter-aspectj`（不写 version，交给 parent）
- Jackson 日期：用 `spring.jackson.datatype.datetime.write-dates-as-timestamps`，**不是** `serialization.write-dates-as-timestamps`
- Redisson 3.x 与 SB4 不兼容，启动报 `RedisProperties` ClassNotFound

---

## 3. 当前进度

| 项 | 状态 |
|----|------|
| Spring Boot 工程 + pom | ✅ |
| `application.yml` + local/dev/prod profile | ✅ |
| `mes_db.sql`（表结构 + 种子数据） | ✅ |
| `SecurityConfig`（local 放行全部） | ✅ |
| `docker-compose.yml`（MySQL + Redis） | ✅ |
| Entity / Mapper / 业务模块 | ❌ 未开始（下一步 D2） |
| Vue 前端 | ❌ 未开始 |
| RocketMQ Producer/Consumer | ❌ 未开始 |

**当前开发阶段**：D2 — 生成 Entity/Mapper，搭建 `R<T>` 框架层。

---

## 4. 仓库结构

```
MES-dome/
├── CLAUDE.md                 # 本文件（AI 上下文）
├── MES-设计摘要.md             # 业务设计基线
├── 40天开发计划.md             # 40 天排期
├── docker-compose.yml          # MySQL + Redis
├── backend/                    # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/fzy/mes/
│       │   ├── MesApplication.java
│       │   └── config/SecurityConfig.java
│       └── resources/
│           ├── application.yml
│           └── sql/mes_db.sql
└── frontend/                   # 计划：Vue 单项目（尚未创建）
```

### 计划中的后端包结构

```
com.fzy.mes/
├── common/          # R<T>、BusinessException、枚举
├── config/          # Redis、RocketMQ、Security、CORS、MyBatis-Plus
├── module/
│   ├── auth/        # 登录、JWT
│   ├── erp/         # Mock ERP 推单/关单
│   ├── workorder/   # 工单 + 状态机 ★手写核心
│   ├── dispatch/    # 派工 + 审计 ★手写核心
│   ├── report/      # 报工 + MQ ★手写核心
│   └── integration/ # IntegrationLog、重试
└── mq/              # Topic、消息 DTO、Producer/Consumer
```

---

## 5. 核心业务：三大环节

| 环节 | 职责 | MVP 范围 |
|------|------|----------|
| **工单** | 生产指令：做什么、做多少、何时交 | ERP Mock 同步、7 态状态机、自动流转 |
| **派工** | 任务分配给人/工位 | 仅**主动派工** + 审计日志 |
| **报工** | 现场反馈产量与质量 | 扫码报工、防错、MQ 异步、自动完工 |

**刻意不做（backlog）**：工单拆分/合并、抢单/自动派工、离线报工、PLC 自动报工、真实 ERP/WMS 对接。

---

## 6. 工单状态机（7 态，必须自动流转）

| status | 名称 | 触发条件 |
|--------|------|----------|
| 0 | 已下发 | ERP 推「已释放」 |
| 1 | 已派工 | 首次派工 |
| 2 | 执行中 | 首次报工 |
| 3 | 部分完工 | 有产出但未达 plan_qty |
| 4 | 已完工 | 末工序报工且 remaining_qty = 0（**自动，勿人工点**） |
| 5 | 已关闭 | ERP 确认或人工关闭 |
| 6 | 已取消 | ERP 取消；禁止再报工 |

**ERP 状态映射**：

- ERP「已释放」→ MES「已下发」
- ERP「已关闭-正常」→ 已完工/已关闭
- ERP「已关闭-取消」→ 已取消

---

## 7. 报工防重复（三层，面试亮点）

1. **API 层**：Redis `SET NX` on `mes:report:idempotent:{requestId}`，Header `X-Request-Id`
2. **并发层**：Redisson 锁 `mes:lock:task:{taskId}` + MyBatis-Plus `@Version` 乐观锁
3. **MQ 层**：RocketMQ 异步落库；DB 唯一键 `production_report.request_id`；Consumer 幂等

**报工 API 流程**：同步校验 → 202 已受理 + reportId → 前端轮询 `GET /api/reports/{id}`

**RocketMQ Topic**（见 `mes.rocketmq` 配置）：

| Topic | Tag | 用途 |
|-------|-----|------|
| `mes_report_submit` | `submit` | 报工落库 + 状态机 |
| `mes_erp_callback` | `callback` | 回传 Mock ERP |

---

## 8. 数据库（mes_db.sql）

### 核心表

| 表 | 要点 |
|----|------|
| `work_order` | `erp_order_no` UK；`status` 7 态；`version` 乐观锁 |
| `operation_task` | `uk(work_order_id, seq)`；`priority`、`planned_start` |
| `dispatch_record` / `dispatch_audit_log` | 派工 + 审计 |
| `production_report` | `request_id` UK；status 0处理中/1成功/2失败 |
| `defect_reason` / `defect_record` | 不良字典 + 明细 |
| `integration_log` | `idempotent_key` UK；ERP 回传对账 |
| `sys_user` / `user_auth` / `role` / `user_role` | 认证鉴权 |
| `quality_inspection_task` | QMS Mock（D32） |

### 种子账号（密码均为 `123456`）

| 用户名 | 角色 |
|--------|------|
| admin | 管理员 |
| planner | 计划员 |
| worker1~3 | 工人 |
| qc1 | 质检员 |

---

## 9. API 规划（MVP）

```
# 认证
POST   /api/auth/login
GET    /api/auth/me

# ERP Mock
POST   /api/erp/work-orders
POST   /api/erp/work-orders/{no}/close
GET    /api/erp/callback-logs

# 工单
GET    /api/work-orders
GET    /api/work-orders/{id}
GET    /api/work-orders/stats

# 派工
POST   /api/dispatch
GET    /api/dispatch/audit
GET    /api/tasks/my

# 报工
POST   /api/reports              # Header: X-Request-Id
GET    /api/reports/{reportId}
GET    /api/defect-reasons

# 集成
GET    /api/integration/logs
POST   /api/integration/logs/{id}/retry
```

---

## 10. 配置与启动

### Profile

| Profile | 用途 | Redis/Redisson |
|---------|------|----------------|
| `local`（**默认**） | 日常开发，Web + MySQL | 排除 Redisson 自动配置 |
| `dev` | 完整中间件联调 | 需 Redis 运行 |
| `prod` | 生产 | 需 Redis 运行 |

### 启动命令

```bash
# 后端（local，不需 Redis）
cd backend && mvn spring-boot:run

# 带 Redis/Redisson
docker-compose up -d redis
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev

# MySQL + Redis
docker-compose up -d
```

- 默认端口：**8080**（见 `application.yml` 中 `SERVER_PORT`）
- 健康检查：`GET /actuator/health`
- 日志文件：`backend/logs/mes-dome.log`
- SLF4J + Logback **已内置**，无需额外 pom 依赖

### 自定义配置前缀 `mes.*`

- `mes.jwt.*` — JWT 密钥、过期时间
- `mes.security.permit-urls` — Security 白名单
- `mes.rocketmq.*` — NameServer、Topic、Group
- `mes.redis.*` — Key 前缀、TTL

---

## 11. 编码规范与分工

### 必须手写（面试深挖，AI 不要代写核心逻辑）

- 工单状态机 `WorkOrderStateMachine`
- ERP Mock + Redis 幂等
- 派工服务 + 审计
- 报工校验 + Redis/MQ 三层防重
- RocketMQ Producer/Consumer
- IntegrationLog 重试

### 可交给 AI

- Vue 页面（表格、表单、ECharts 看板）
- 重复性 Entity/Mapper（需人工核对字段与 sql 一致）
- Docker/README 文档

### 代码约定

- 包名：`com.fzy.mes`
- 统一响应：`R<T>`（code / message / data）
- 业务异常：`BusinessException` + 全局 `@RestControllerAdvice`
- 表名映射：`sys_user`（**不是** `user`，MySQL 保留字）
- Git commit：`feat(workorder)`、`fix(report-mq)` 等规范前缀

---

## 12. AI 协作注意事项

1. **不要**把 `spring-boot-starter-aop` 写进 pom（SB4 不存在）
2. **不要**在没起 Redis 时启用 Redisson（用 `local` profile 或 `--spring.profiles.active=local`）
3. **不要** scope 膨胀：新需求记 backlog，40 天内保主链路
4. 改 sql 时同步 Entity；工单 status 枚举与 sql COMMENT 保持一致（0~6）
5. 报工 `production_report.status`：0处理中 / 1成功 / 2失败（不是 QMS 审批语义）
6. Jackson 配置遵循 SB4 / Jackson 3 路径
7. 前端报工页必须生成 UUID 作为 `X-Request-Id`

---

## 13. 参考文档

| 文件 | 内容 |
|------|------|
| [MES-设计摘要.md](./MES-设计摘要.md) | 工单/派工/报工业务设计、集成架构、常见坑 |
| [40天开发计划.md](./40天开发计划.md) | 按天排期、API、检查表、面试脚本 |
| [backend/src/main/resources/sql/mes_db.sql](./backend/src/main/resources/sql/mes_db.sql) | 完整 DDL + 种子数据 |
| [backend/src/main/resources/application.yml](./backend/src/main/resources/application.yml) | 运行配置 |

---

## 14. 下一步（D2）

1. 按 `mes_db.sql` 生成 Entity（`@TableName`、`@Version`）
2. 创建 Mapper 接口 + `@MapperScan("com.fzy.mes.**.mapper")`
3. 搭建 `common/result/R.java`、`common/exception/GlobalExceptionHandler`
4. 验证能查 `sys_user` 表
