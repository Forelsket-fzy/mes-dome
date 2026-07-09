<script setup lang="ts">
import { Connection, Document, DataAnalysis } from '@element-plus/icons-vue'
</script>

<template>
  <section class="admin-home">
    <div class="stats-grid">
      <article class="stat-card page-card">
        <el-icon class="stat-icon"><Document /></el-icon>
        <div>
          <p class="muted-text">工单模块</p>
          <h3>D8 ~ D14 已就绪</h3>
        </div>
      </article>

      <article class="stat-card page-card">
        <el-icon class="stat-icon"><DataAnalysis /></el-icon>
        <div>
          <p class="muted-text">看板统计</p>
          <h3>Redis 60s 缓存</h3>
        </div>
      </article>

      <article class="stat-card page-card">
        <el-icon class="stat-icon"><Connection /></el-icon>
        <div>
          <p class="muted-text">推单全流程</p>
          <h3>推单 → 查询 → 关单</h3>
        </div>
      </article>
    </div>

    <article class="welcome-card page-card">
      <h3>D14 工单 Demo 链路</h3>
      <p class="muted-text">
        ERP 推单写入工单与工序，管理端可分页查询、查看详情与 7 态统计看板；ERP 关单同步 MES 状态，推单/关单后自动刷新 stats 缓存。
      </p>
      <el-steps :active="4" finish-status="success" align-center class="demo-steps">
        <el-step title="ERP 推单" description="POST /api/erp/work-orders" />
        <el-step title="工单列表" description="/admin/work-orders" />
        <el-step title="看板统计" description="/admin/dashboard" />
        <el-step title="ERP 关单" description="POST .../close" />
      </el-steps>
      <el-alert
        title="联调提示"
        type="success"
        show-icon
        :closable="false"
        description="backend 8081 + 前端 Vite 代理 /api；登录后使用 planner/admin 角色验证推单与关单。"
        class="demo-alert"
      />
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
  color: var(--color-primary);
}

.stat-card h3,
.welcome-card h3 {
  margin: 4px 0 0;
}

.welcome-card p {
  line-height: 1.7;
}

.demo-steps {
  margin: 24px 0;
}

.demo-alert {
  margin-top: 8px;
}

@media (max-width: 960px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
