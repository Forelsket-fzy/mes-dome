<script setup lang="ts">
import { List, Pointer } from '@element-plus/icons-vue'
</script>

<template>
  <section class="terminal-home">
    <article class="hero-card page-card">
      <p class="eyebrow">D7 · 工人端壳子</p>
      <h2>今日任务清单</h2>
      <p class="muted-text">
        工人端采用大按钮、高对比布局，便于车间触控操作。D19 将接入扫码报工与 X-Request-Id 幂等提交。
      </p>
    </article>

    <div class="action-grid">
      <button type="button" class="action-card page-card cursor-pointer" disabled>
        <el-icon><List /></el-icon>
        <span>查看任务</span>
        <small class="muted-text">D19 开放</small>
      </button>

      <button type="button" class="action-card page-card cursor-pointer" disabled>
        <el-icon><Pointer /></el-icon>
        <span>扫码报工</span>
        <small class="muted-text">D19 开放</small>
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
  transition: border-color var(--transition-fast), opacity var(--transition-fast);
}

.action-card .el-icon {
  font-size: 28px;
  color: var(--color-cta);
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
