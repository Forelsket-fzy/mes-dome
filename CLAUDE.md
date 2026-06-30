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
| `application.yml`（直连 MySQL/Redis，无 local profile） | ✅ |
| `mes_db.sql`（表结构 + 种子数据） | ✅ |
| `SecurityConfig` + JWT 过滤器链 | 🟡 `common/config/SecurityConfig.java`，白名单硬编码 |
| `docker-compose.yml`（MySQL + Redis） | ✅ |
| Entity / Mapper（13 实体 + 14 Mapper） | ✅ |
| 实体 Jakarta Validation（`Create`/`Update` 分组） | ✅ D2 遗留；**新 API 校验放 DTO**（见 §11.4） |
| `Result<T>` + `GlobalExceptionHandler` | 🟡 含登录校验/认证异常 |
| JWT 登录（`POST /api/auth/login`） | 🟡 Filter + `AuthService` 已通；RBAC/Redis 缓存/双令牌待做 |
| CORS 配置 | ❌ |
| Redis / RocketMQ 配置骨架 | ❌ |
| Vue 前端 | ❌ |
| RocketMQ Producer/Consumer | ❌ |

**当前开发阶段**：D3 收尾（CORS）+ D4 中间件 + D5 JWT 收尾（RBAC、Redis 会话、可选双令牌）并行推进。

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
│       │   ├── common/          # Result、SecurityConfig、JWT Filter、异常
│       │   └── module/          # auth/workorder/dispatch/report/integration/quality
│       └── resources/
│           ├── application.yml
│           └── sql/mes_db.sql
└── frontend/                   # 计划：Vue 单项目（尚未创建）
```

### 计划中的后端包结构

```
com.fzy.mes/
├── common/          # R<T>、BusinessException、SecurityConfig、JWT Filter
├── config/          # Redis、RocketMQ、CORS、MyBatis-Plus（计划，尚未建包）
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

| Profile | 用途 |
|---------|------|
| 默认（无 profile） | 本地开发，直连 MySQL + Redis |
| `prod` | 生产：收紧日志、替换 JWT 密钥 |

### 启动命令

```bash
# MySQL + Redis
docker compose up -d

# 后端
cd backend && mvn spring-boot:run
```

- 默认端口：**8080**
- 健康检查：`GET /actuator/health`
- 日志文件：`backend/logs/mes-dome.log`
- SLF4J + Logback **已内置**，无需额外 pom 依赖

### 自定义配置前缀 `mes.*`

- ~~`mes.jwt.*`~~ — **未使用**；JWT secret / 过期时间在 `JwtUtil` 硬编码（当前 access 7 天）
- ~~`mes.security.permit-urls`~~ — **未使用**；白名单在 `common/config/SecurityConfig.java` 硬编码
- `mes.rocketmq.*` — NameServer、Topic、Group
- `mes.redis.*` — Key 前缀、TTL（计划用于幂等、LoginUser 缓存、双令牌会话）

### JWT 鉴权（当前实现）

| 项 | 说明 |
|----|------|
| 登录 | `POST /api/auth/login` → `{ token }` |
| 请求头 | `Authorization: <token>`（Bearer 前缀待统一，见优化计划） |
| Filter | `JwtAuthenticationFilter`：验签 + 解析 username，LoginUser 暂用占位字段 |
| 待做 | RBAC 查 role、Redis 缓存 LoginUser、双令牌 + 服务端会话校验、Filter 错误响应改 `Result` JSON |

---

## 11. 编码规范与分工

> **项目目标**：不是快速完工，而是**开发者本人理解并手写核心逻辑**（面试能讲清楚）。AI 以讲解、拆步骤、Review、排错为主，**不代写核心业务代码**。

### 11.1 AI 禁止代写（必须开发者手写）

与 [40天开发计划.md §3.1](./40天开发计划.md) 对齐，以下模块 **AI 不得生成完整实现**（类、Service、状态机、Consumer 等）：

| 模块 | 开发者手写内容 | AI 只允许 |
|------|----------------|-----------|
| Entity + Mapper | 对齐 `mes_db.sql`，`@Version` | 字段清单、包结构、编写顺序 |
| 工单状态机 | `WorkOrderStateMachine`，7 态流转 | 状态图、非法跃迁说明、Review |
| ERP Mock | 推单/关单 + Redis 幂等 + 状态映射 | 幂等 Key 设计、流程拆解 |
| 派工 | 派工 Service + `DispatchAuditLog` | 审计字段含义、事务边界建议 |
| 报工 | 校验 + Redis 去重 + Redisson 锁 + MQ 发送 | 三层防重复时序图、伪代码级步骤 |
| 报工 Consumer | 事务落库、状态机、发 ERP Topic | 消费幂等策略、失败重试说明 |
| RocketMQ | Producer/Consumer 配置（原生 client） | 配置项含义、Topic 划分对照 yml |
| Security + JWT | 登录、过滤器、RBAC | 白名单配置位置、Security 链说明 |
| IntegrationLog | 重试 API + 业务对账 | 状态枚举、重试边界 |

**AI 对上述模块的请求应：**

- ✅ 讲「为什么这样设计」「先写哪个类」「方法职责怎么拆」
- ✅ Review 开发者已写的代码，指出 bug / 边界问题
- ✅ 帮看报错堆栈、配置、依赖冲突
- ❌ 不要直接输出可粘贴的 Service / Controller / StateMachine / Consumer 完整代码
- ❌ 不要「顺手帮你实现」核心业务，除非开发者明确要求破例

### 11.2 AI 可以代劳（非核心 / 基础设施）

| 内容 | 说明 |
|------|------|
| Vue 页面 | 管理端/工人端 UI（计划内交给 AI） |
| 配置文件 | `application.yml`、`docker-compose.yml`、pom 依赖修复 |
| 文档 | README、API 说明（开发者要求时） |
| 脚手架说明 | `Result`、CORS、全局异常等**仅当开发者要求**或纯配置类 |

### 11.3 推荐提问方式

```
❌ 「帮我写 ERP 推单 Service」
✅ 「ERP 推单我要建哪几个类？push 方法里逻辑顺序是什么？」
✅ 「我写了 WorkOrderStateMachine，帮我 Review 有没有漏掉非法跃迁」
✅ 「报工 Redis 幂等和 DB 唯一键会不会重复？我这段思路对吗？」
```

### 11.4 代码约定

- 包名：`com.fzy.mes`
- 统一响应：`Result<T>`（code / message / data）
- **参数校验（API 边界）**：写在 **Request DTO** 上，Controller 用 `@Valid` / `@Validated`；**不要**把 Entity 当入参暴露
- **Entity 校验**：D2 已在实体上加了 `Create`/`Update` 分组，属历史遗留；新接口逐步迁到 DTO，实体只保留持久化字段（`@TableId`、`@Version` 等）
- **业务规则**：状态机跃迁、数量守恒等放在 Service，不靠 `@NotNull` 代替
- 业务异常：`BusinessException` + 全局 `@RestControllerAdvice`
- 表名映射：`sys_user`（**不是** `user`，MySQL 保留字）
- Git commit：`feat(workorder)`、`fix(report-mq)` 等规范前缀

---

## 12. AI 协作注意事项

1. **最高优先级**：遵守 §11.1，**不代写核心业务代码**；开发者目标是理解逻辑，不是赶进度
2. **不要**把 `spring-boot-starter-aop` 写进 pom（SB4 不存在）
3. **启动前**先 `docker compose up -d` 确保 MySQL、Redis 可用
4. **不要** scope 膨胀：新需求记 backlog，40 天内保主链路
5. 改 sql 时同步 Entity；工单 status 枚举与 sql COMMENT 保持一致（0~6）
6. 报工 `production_report.status`：0处理中 / 1成功 / 2失败（不是 QMS 审批语义）
7. Jackson 配置遵循 SB4 / Jackson 3 路径
8. 前端报工页必须生成 UUID 作为 `X-Request-Id`

---

## 13. 参考文档

| 文件 | 内容 |
|------|------|
| [MES-设计摘要.md](./MES-设计摘要.md) | 工单/派工/报工业务设计、集成架构、常见坑 |
| [40天开发计划.md](./40天开发计划.md) | 按天排期、API、检查表、面试脚本 |
| [backend/src/main/resources/sql/mes_db.sql](./backend/src/main/resources/sql/mes_db.sql) | 完整 DDL + 种子数据 |
| [backend/src/main/resources/application.yml](./backend/src/main/resources/application.yml) | 运行配置 |

---

## 14. 下一步（开发者手写导向）

1. **D3 剩余**：CORS 配置（`WebMvcConfigurer`，允许 `localhost:5173`）
2. **D4**：RedisTemplate / Redisson 配置类 → 连通性自测；为 LoginUser 缓存与双令牌会话打基础
3. **D5 收尾**：RBAC 查 role、Redis 缓存、`Bearer` 前缀、Filter 错误响应统一为 `Result`；可选双令牌（见 [优化计划.md](./优化计划.md) §2.5）
4. **D6+**：ERP Mock → 状态机 → 派工 → 报工 MQ（均按 §11.1 手写，AI Review）

每完成一个模块，用「帮我 Review + 面试怎么讲」的方式验收，而不是让 AI 生成下一块代码。
