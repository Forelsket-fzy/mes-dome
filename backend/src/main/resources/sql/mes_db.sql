/*
 MES 数据库初始化脚本
 技术栈：MySQL 8.0 + MyBatis-Plus
 对齐：MES-设计摘要.md / 40天开发计划.md
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `mes_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `mes_db`;

-- ----------------------------
-- sys_user 用户基本信息（避免使用 MySQL 保留字 user）
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `skill_level` tinyint NOT NULL DEFAULT 1 COMMENT '技能等级 1-5',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基本信息';

-- ----------------------------
-- role 角色表
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` varchar(30) NOT NULL COMMENT 'ROLE_ADMIN / ROLE_PLANNER / ROLE_WORKER / ROLE_QC',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

-- ----------------------------
-- user_auth 用户认证表
-- ----------------------------
DROP TABLE IF EXISTS `user_auth`;
CREATE TABLE `user_auth` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '认证ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '加密密码（BCrypt）',
  `user_id` bigint NOT NULL COMMENT '关联 sys_user.id',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '1-启用 0-禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_user_auth_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户认证表';

-- ----------------------------
-- user_role 用户角色关联
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联';

-- ----------------------------
-- work_order 工单表
-- ----------------------------
DROP TABLE IF EXISTS `work_order`;
CREATE TABLE `work_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `erp_order_no` varchar(50) NOT NULL COMMENT 'ERP订单号（业务唯一标识）',
  `erp_status` varchar(30) DEFAULT NULL COMMENT 'ERP原始状态（RELEASED/CLOSED/CANCELLED等）',
  `product_code` varchar(50) NOT NULL COMMENT '产品编码',
  `product_name` varchar(100) DEFAULT NULL COMMENT '产品名称',
  `plan_qty` int NOT NULL COMMENT '计划生产数量',
  `completed_qty` int NOT NULL DEFAULT 0 COMMENT '累计完成数量（末工序良品汇总）',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-已下发 1-已派工 2-执行中 3-部分完工 4-已完工 5-已关闭 6-已取消',
  `due_date` datetime DEFAULT NULL COMMENT '计划交期',
  `cancel_reason` varchar(200) DEFAULT NULL COMMENT '取消/关闭原因',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_erp_order_no` (`erp_order_no`),
  KEY `idx_status` (`status`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_created_by` (`created_by`),
  CONSTRAINT `fk_work_order_created_by` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单表';

-- ----------------------------
-- operation_task 工序任务表
-- ----------------------------
DROP TABLE IF EXISTS `operation_task`;
CREATE TABLE `operation_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工序任务ID',
  `work_order_id` bigint NOT NULL COMMENT '关联工单ID',
  `operation_code` varchar(30) NOT NULL COMMENT '工序编码（如 OP001）',
  `operation_name` varchar(100) DEFAULT NULL COMMENT '工序名称',
  `seq` int NOT NULL COMMENT '工序序号（1,2,3...）',
  `plan_qty` int NOT NULL COMMENT '工序计划数量',
  `completed_qty` int NOT NULL DEFAULT 0 COMMENT '工序已完成数量',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-待派工 1-生产中 2-已完工',
  `priority` int NOT NULL DEFAULT 0 COMMENT '优先级（越大越优先）',
  `planned_start` datetime DEFAULT NULL COMMENT '计划开始时间',
  `assigned_to` bigint DEFAULT NULL COMMENT '派工工人ID',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_order_seq` (`work_order_id`, `seq`),
  KEY `idx_status` (`status`),
  KEY `idx_assigned_to` (`assigned_to`),
  KEY `idx_priority_planned` (`priority`, `planned_start`),
  CONSTRAINT `fk_operation_task_work_order` FOREIGN KEY (`work_order_id`) REFERENCES `work_order` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_operation_task_assignee` FOREIGN KEY (`assigned_to`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工序任务表';

-- ----------------------------
-- dispatch_record 派工记录表
-- ----------------------------
DROP TABLE IF EXISTS `dispatch_record`;
CREATE TABLE `dispatch_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '派工记录ID',
  `task_id` bigint NOT NULL COMMENT '工序任务ID',
  `operator_id` bigint NOT NULL COMMENT '操作人ID（计划员/班组长）',
  `assignee_id` bigint NOT NULL COMMENT '被派工人ID',
  `mode` tinyint NOT NULL DEFAULT 1 COMMENT '派工模式：1-手动派工 2-自动派工 3-抢单',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_assignee_id` (`assignee_id`),
  CONSTRAINT `fk_dispatch_record_task` FOREIGN KEY (`task_id`) REFERENCES `operation_task` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_dispatch_record_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_dispatch_record_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='派工记录表';

-- ----------------------------
-- dispatch_audit_log 派工审计日志表
-- ----------------------------
DROP TABLE IF EXISTS `dispatch_audit_log`;
CREATE TABLE `dispatch_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `dispatch_id` bigint NOT NULL COMMENT '派工记录ID',
  `action_by` bigint NOT NULL COMMENT '操作人ID',
  `snapshot_json` json DEFAULT NULL COMMENT '变更前快照（JSON）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_dispatch_id` (`dispatch_id`),
  KEY `idx_action_by` (`action_by`),
  CONSTRAINT `fk_dispatch_audit_dispatch` FOREIGN KEY (`dispatch_id`) REFERENCES `dispatch_record` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_dispatch_audit_action_by` FOREIGN KEY (`action_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='派工审计日志表';

-- ----------------------------
-- defect_reason 不良原因字典表
-- ----------------------------
DROP TABLE IF EXISTS `defect_reason`;
CREATE TABLE `defect_reason` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '原因ID',
  `code` varchar(30) NOT NULL COMMENT '原因编码',
  `name` varchar(100) NOT NULL COMMENT '原因名称',
  `defect_type` varchar(30) NOT NULL COMMENT '不良类型（DIMENSION/APPEARANCE/MATERIAL）',
  `operation_code` varchar(30) DEFAULT NULL COMMENT '适用工序编码（NULL表示通用）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_operation_code` (`operation_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不良原因字典表';

-- ----------------------------
-- production_report 报工记录表
-- ----------------------------
DROP TABLE IF EXISTS `production_report`;
CREATE TABLE `production_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报工记录ID',
  `task_id` bigint NOT NULL COMMENT '工序任务ID',
  `request_id` varchar(50) NOT NULL COMMENT '请求唯一ID（幂等控制，对应 X-Request-Id）',
  `good_qty` int NOT NULL COMMENT '良品数量',
  `defect_qty` int NOT NULL DEFAULT 0 COMMENT '不良品数量',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-处理中 1-成功 2-失败',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因（MQ消费失败时）',
  `reported_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '报工时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_id` (`request_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_reported_at` (`reported_at`),
  CONSTRAINT `fk_production_report_task` FOREIGN KEY (`task_id`) REFERENCES `operation_task` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_production_report_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报工记录表';

-- ----------------------------
-- defect_record 不良记录明细表
-- ----------------------------
DROP TABLE IF EXISTS `defect_record`;
CREATE TABLE `defect_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `report_id` bigint NOT NULL COMMENT '报工记录ID',
  `reason_id` bigint NOT NULL COMMENT '不良原因ID',
  `qty` int NOT NULL COMMENT '该原因对应数量',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_reason_id` (`reason_id`),
  CONSTRAINT `fk_defect_record_report` FOREIGN KEY (`report_id`) REFERENCES `production_report` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_defect_record_reason` FOREIGN KEY (`reason_id`) REFERENCES `defect_reason` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不良记录明细表';

-- ----------------------------
-- integration_log 系统集成日志表
-- ----------------------------
DROP TABLE IF EXISTS `integration_log`;
CREATE TABLE `integration_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `biz_type` varchar(30) NOT NULL COMMENT '业务类型（ERP_ORDER/REPORT_CALLBACK/QUALITY_RESULT）',
  `biz_id` varchar(50) NOT NULL COMMENT '业务ID（如 erp_order_no、reportId）',
  `idempotent_key` varchar(100) NOT NULL COMMENT '幂等键（固定业务键，如 report:123，不含时间戳）',
  `target_system` varchar(30) NOT NULL COMMENT '目标系统（ERP/WMS/QMS）',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-待发送 1-发送成功 2-发送失败 3-重试中',
  `payload` json DEFAULT NULL COMMENT '请求数据（JSON）',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
  KEY `idx_biz_type_biz_id` (`biz_type`, `biz_id`),
  KEY `idx_target_system` (`target_system`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统集成日志表';

-- ----------------------------
-- quality_inspection_task 质检任务表（QMS Mock，D32）
-- ----------------------------
DROP TABLE IF EXISTS `quality_inspection_task`;
CREATE TABLE `quality_inspection_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '检验任务ID',
  `report_id` bigint NOT NULL COMMENT '触发的报工记录ID',
  `work_order_id` bigint NOT NULL COMMENT '工单ID',
  `task_id` bigint NOT NULL COMMENT '工序任务ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-待检验 1-合格 2-不合格',
  `inspector_id` bigint DEFAULT NULL COMMENT '质检员ID',
  `result_remark` varchar(500) DEFAULT NULL COMMENT '检验备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_work_order_id` (`work_order_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_qc_report` FOREIGN KEY (`report_id`) REFERENCES `production_report` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_qc_work_order` FOREIGN KEY (`work_order_id`) REFERENCES `work_order` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_qc_task` FOREIGN KEY (`task_id`) REFERENCES `operation_task` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_qc_inspector` FOREIGN KEY (`inspector_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='质检任务表';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 种子数据（演示 / 开发用）
-- 默认密码均为 123456 的 BCrypt 哈希（请在生产环境修改）
-- =============================================================================

-- 角色
INSERT INTO `role` (`id`, `role_code`, `role_name`) VALUES
(1, 'ROLE_ADMIN',   '系统管理员'),
(2, 'ROLE_PLANNER', '计划员'),
(3, 'ROLE_WORKER',  '生产工人'),
(4, 'ROLE_QC',      '质检员');

-- 用户（skill_level 1-5）
INSERT INTO `sys_user` (`id`, `real_name`, `email`, `phone`, `skill_level`) VALUES
(1, '管理员',   'admin@mes.local',   '13800000001', 5),
(2, '李计划',   'planner@mes.local', '13800000002', 4),
(3, '张三',     NULL,                '13800000003', 3),
(4, '李四',     NULL,                '13800000004', 4),
(5, '王五',     NULL,                '13800000005', 2),
(6, '赵质检',   'qc@mes.local',      '13800000006', 4);

-- 认证（密码均为 123456，BCrypt 加密）
INSERT INTO `user_auth` (`username`, `password`, `user_id`, `enabled`) VALUES
('admin',   '$2b$10$qS8I3jf65imKKhunLsGpYu6J5Us9xY9mgtlLBxBLcsN93F5REhSl2', 1, 1),
('planner', '$2b$10$qS8I3jf65imKKhunLsGpYu6J5Us9xY9mgtlLBxBLcsN93F5REhSl2', 2, 1),
('worker1', '$2b$10$qS8I3jf65imKKhunLsGpYu6J5Us9xY9mgtlLBxBLcsN93F5REhSl2', 3, 1),
('worker2', '$2b$10$qS8I3jf65imKKhunLsGpYu6J5Us9xY9mgtlLBxBLcsN93F5REhSl2', 4, 1),
('worker3', '$2b$10$qS8I3jf65imKKhunLsGpYu6J5Us9xY9mgtlLBxBLcsN93F5REhSl2', 5, 1),
('qc1',     '$2b$10$qS8I3jf65imKKhunLsGpYu6J5Us9xY9mgtlLBxBLcsN93F5REhSl2', 6, 1);

-- 用户角色
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES
(1, 1), (2, 2), (3, 3), (4, 3), (5, 3), (6, 4);

-- 不良原因字典
INSERT INTO `defect_reason` (`code`, `name`, `defect_type`, `operation_code`) VALUES
('DIM-001', '尺寸超差',   'DIMENSION',  NULL),
('DIM-002', '孔径不符',   'DIMENSION',  'OP020'),
('APP-001', '外观划痕',   'APPEARANCE', NULL),
('APP-002', '表面污渍',   'APPEARANCE', 'OP030'),
('MAT-001', '材料缺陷',   'MATERIAL',   NULL),
('ASM-001', '装配松动',   'DIMENSION',  'OP040');

-- 演示工单（已下发，含两道工序）
INSERT INTO `work_order` (`id`, `erp_order_no`, `erp_status`, `product_code`, `product_name`, `plan_qty`, `completed_qty`, `status`, `due_date`, `created_by`) VALUES
(1, 'ERP-2026-0001', 'RELEASED', 'P-10086', '电机外壳', 100, 0, 0, DATE_ADD(NOW(), INTERVAL 7 DAY), 2),
(2, 'ERP-2026-0002', 'RELEASED', 'P-10087', '控制面板',  50, 0, 0, DATE_ADD(NOW(), INTERVAL 3 DAY), 2);

INSERT INTO `operation_task` (`work_order_id`, `operation_code`, `operation_name`, `seq`, `plan_qty`, `completed_qty`, `status`, `priority`, `planned_start`) VALUES
(1, 'OP010', '冲压', 1, 100, 0, 0, 10, DATE_ADD(NOW(), INTERVAL 1 DAY)),
(1, 'OP020', '焊接', 2, 100, 0, 0,  5, DATE_ADD(NOW(), INTERVAL 2 DAY)),
(2, 'OP010', '贴片', 1,  50, 0, 0, 20, NOW()),
(2, 'OP030', '测试', 2,  50, 0, 0, 15, DATE_ADD(NOW(), INTERVAL 1 DAY));
