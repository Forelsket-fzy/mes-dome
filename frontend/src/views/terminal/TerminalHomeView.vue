<script setup lang="ts">
import { useRouter } from 'vue-router'
import { List, Pointer } from '@element-plus/icons-vue'

const router = useRouter()

function goTasks() {
  router.push('/terminal/tasks')
}
</script>

<template>
  <section class="terminal-home">
    <article class="hero-card page-card">
      <p class="eyebrow">工人端</p>
      <h2>今日任务台</h2>
      <p class="muted-text">
        查看计划员派给您的工序任务；报工能力将在 D22 接入 Redis 幂等与 RocketMQ。
      </p>
    </article>

    <div class="action-grid">
      <button type="button" class="action-card page-card cursor-pointer" @click="goTasks">
        <el-icon><List /></el-icon>
        <span>查看我的任务</span>
        <small class="muted-text">GET /api/tasks/my</small>
      </button>

      <button type="button" class="action-card page-card cursor-pointer" disabled>
        <el-icon><Pointer /></el-icon>
        <span>扫码报工</span>
        <small class="muted-text">D22 开放</small>
      </button>
    </div>
  </section>
</template>

<style scoped>
.terminal-home {
  display: grid;
  gap: 20px;
}

.hero-card h2 {
  margin: 8px 0 12px;
  font-size: 1.75rem;
}

.eyebrow {
  margin: 0;
  color: var(--color-cta);
  font-size: 0.8125rem;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.action-card {
  min-height: 160px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 8px;
  text-align: left;
  font: inherit;
  transition: border-color var(--transition-fast), transform var(--transition-fast);
}

.action-card .el-icon {
  font-size: 28px;
  color: var(--color-cta);
}

.action-card:not(:disabled):hover {
  border-color: var(--color-cta);
  transform: translateY(-2px);
}

.action-card:disabled {
  opacity: 0.72;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .action-grid {
    grid-template-columns: 1fr;
  }
}
</style>
