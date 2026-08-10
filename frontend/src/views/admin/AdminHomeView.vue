<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Connection, Document, DataAnalysis, User } from '@element-plus/icons-vue'

const router = useRouter()
</script>

<template>
  <section class="admin-home">
    <div class="stats-grid">
      <article class="stat-card page-card cursor-pointer" @click="router.push('/admin/work-orders')">
        <el-icon class="stat-icon"><Document /></el-icon>
        <div>
          <p class="muted-text">工单模块</p>
          <h3>D8 ~ D14 已就绪</h3>
        </div>
      </article>

      <article class="stat-card page-card cursor-pointer" @click="router.push('/admin/dispatch')">
        <el-icon class="stat-icon"><User /></el-icon>
        <div>
          <p class="muted-text">派工模块</p>
          <h3>D15 ~ D21 已就绪</h3>
        </div>
      </article>

      <article class="stat-card page-card cursor-pointer" @click="router.push('/admin/dashboard')">
        <el-icon class="stat-icon"><DataAnalysis /></el-icon>
        <div>
          <p class="muted-text">看板统计</p>
          <h3>Redis 60s 缓存</h3>
        </div>
      </article>
    </div>

    <article class="welcome-card page-card">
      <h3>D21 派工 Demo 链路</h3>
      <p class="muted-text">
        计划员在管理端主动派工 → 工单进入「已派工」→ 工人端刷新「我的任务」按优先级查看。
      </p>
      <el-steps :active="4" finish-status="success" align-center class="demo-steps">
        <el-step title="ERP 推单" description="POST /api/erp/work-orders" />
        <el-step title="主动派工" description="/admin/dispatch" />
        <el-step title="工人任务" description="/terminal/tasks" />
        <el-step title="审计可查" description="GET /api/dispatch/audit" />
      </el-steps>
      <el-alert
        title="联调提示"
        type="success"
        show-icon
        :closable="false"
        description="planner/123456 打开派工管理 → 选择工序派给 worker1 → worker1/123456 登录工人端查看任务。"
        class="demo-alert"
      />
      <div class="quick-links">
        <el-button type="primary" class="cursor-pointer" @click="router.push('/admin/dispatch')">
          进入派工管理
        </el-button>
        <el-button class="cursor-pointer" @click="router.push('/admin/work-orders')">
          工单列表
        </el-button>
      </div>
    </article>

    <article class="welcome-card page-card">
      <h3>
        <el-icon><Connection /></el-icon>
        后续报工周（D22+）
      </h3>
      <p class="muted-text">
        报工将接入 Redis 幂等、Redisson 锁与 RocketMQ 异步消费；工人端「扫码报工」按钮待 D22 开放。
      </p>
    </article>
  </section>
</template>

<style scoped>
.admin-home {
  display: grid;
  gap: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  display: flex;
  gap: 16px;
  align-items: center;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.stat-card:hover {
  border-color: var(--color-primary);
  box-shadow: 0 8px 24px rgb(15 23 42 / 6%);
}

.stat-icon {
  font-size: 28px;
  color: var(--color-cta);
}

.welcome-card h3 {
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.demo-steps {
  margin: 24px 0;
}

.demo-alert {
  margin-bottom: 16px;
}

.quick-links {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
