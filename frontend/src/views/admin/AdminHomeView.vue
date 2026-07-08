<script setup lang="ts">
import { Connection, Document, Timer } from '@element-plus/icons-vue'
</script>

<template>
  <section class="admin-home">
    <div class="stats-grid">
      <article class="stat-card page-card">
        <el-icon class="stat-icon"><Document /></el-icon>
        <div>
          <p class="muted-text">工单模块</p>
          <h3>D8 已就绪</h3>
        </div>
      </article>

      <article class="stat-card page-card">
        <el-icon class="stat-icon"><Timer /></el-icon>
        <div>
          <p class="muted-text">状态机</p>
          <h3>D9 开发中</h3>
        </div>
      </article>

      <article class="stat-card page-card">
        <el-icon class="stat-icon"><Connection /></el-icon>
        <div>
          <p class="muted-text">后端联调</p>
          <h3>8081 / JWT</h3>
        </div>
      </article>
    </div>

    <article class="welcome-card page-card">
      <h3>D12/D13 管理端页面已接入</h3>
      <p class="muted-text">
        工单列表、详情与 ECharts 看板已对接 D8/D11 API。
      </p>
      <el-alert
        title="联调提示"
        type="success"
        show-icon
        :closable="false"
        description="请确保 backend 运行在 http://localhost:8081，前端 dev 会通过 Vite 代理转发 /api 请求。"
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

@media (max-width: 960px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
