<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Warning } from '@element-plus/icons-vue'
import { fetchMyTasks } from '@/api/dispatch'
import type { MyTaskItem } from '@/api/dispatch.types'
import {
  calcProgress,
  formatDateTime,
  getTaskStatusMeta,
  getWorkOrderStatusMeta,
} from '@/constants/workOrderStatus'

const router = useRouter()
const loading = ref(false)
const tasks = ref<MyTaskItem[]>([])

const sortedHint = computed(() =>
  tasks.value.length > 0
    ? '已按优先级降序、计划开始时间升序排列'
    : '暂无派给您的未完工任务，请等待计划员派工',
)

async function loadTasks() {
  loading.value = true
  try {
    tasks.value = await fetchMyTasks()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载任务失败')
  } finally {
    loading.value = false
  }
}

function remainingQty(task: MyTaskItem) {
  return Math.max(0, (task.planQty ?? 0) - (task.completedQty ?? 0))
}

function goReport(task: MyTaskItem) {
  router.push(`/terminal/report/${task.taskId}`)
}

onMounted(loadTasks)
</script>

<template>
  <section class="my-tasks">
    <article class="hero-card page-card">
      <div class="hero-copy">
        <p class="eyebrow">D20 · 工人任务台</p>
        <h2>我的任务</h2>
        <p class="muted-text">{{ sortedHint }}</p>
      </div>
      <el-button
        type="primary"
        class="cursor-pointer refresh-btn"
        size="large"
        :icon="Refresh"
        :loading="loading"
        @click="loadTasks"
      >
        刷新任务
      </el-button>
    </article>

    <div v-loading="loading" class="task-grid">
      <article
        v-for="task in tasks"
        :key="task.taskId"
        class="task-card page-card"
      >
        <div class="task-top">
          <el-tag type="warning" effect="dark" class="priority-tag">
            P{{ task.priority ?? 0 }}
          </el-tag>
          <el-tag :type="getTaskStatusMeta(task.taskStatus).tagType" effect="light">
            {{ getTaskStatusMeta(task.taskStatus).label }}
          </el-tag>
        </div>

        <h3>{{ task.operationName || task.operationCode }}</h3>
        <p class="task-meta">
          {{ task.erpOrderNo }} · {{ task.productName || task.productCode }}
        </p>

        <div class="qty-block">
          <div class="qty-row">
            <span>完成进度</span>
            <strong>{{ task.completedQty }} / {{ task.planQty }}</strong>
          </div>
          <el-progress
            :percentage="calcProgress(task.completedQty, task.planQty)"
            :stroke-width="12"
            :show-text="false"
            color="var(--color-cta)"
          />
          <p class="muted-text remain">剩余 {{ remainingQty(task) }} 件</p>
        </div>

        <dl class="info-grid">
          <div>
            <dt>工序编码</dt>
            <dd>{{ task.operationCode }}</dd>
          </div>
          <div>
            <dt>序号</dt>
            <dd>#{{ task.seq }}</dd>
          </div>
          <div>
            <dt>工单状态</dt>
            <dd>{{ getWorkOrderStatusMeta(task.workOrderStatus).label }}</dd>
          </div>
          <div>
            <dt>计划开始</dt>
            <dd>{{ formatDateTime(task.plannedStart) }}</dd>
          </div>
        </dl>

        <el-button
          class="report-btn cursor-pointer"
          type="primary"
          size="large"
          :disabled="remainingQty(task) <= 0"
          @click="goReport(task)"
        >
          扫码报工
        </el-button>
      </article>

      <el-empty
        v-if="!loading && tasks.length === 0"
        class="empty-state page-card"
        :image-size="96"
      >
        <template #description>
          <p>暂无任务</p>
          <p class="muted-text empty-hint">
            <el-icon><Warning /></el-icon>
            使用 planner 账号在「派工管理」将工序派给当前工人后，点刷新即可看到
          </p>
        </template>
      </el-empty>
    </div>
  </section>
</template>

<style scoped>
.my-tasks {
  display: grid;
  gap: 20px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  background:
    linear-gradient(135deg, rgb(249 115 22 / 8%), transparent 55%),
    var(--color-surface);
}

.hero-copy h2 {
  margin: 6px 0 8px;
  font-size: 1.75rem;
}

.eyebrow {
  margin: 0;
  color: var(--color-cta);
  font-size: 0.8125rem;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.refresh-btn {
  min-width: 140px;
  min-height: 48px;
  font-weight: 600;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  min-height: 180px;
}

.task-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-color: rgb(148 163 184 / 35%);
  transition: border-color var(--transition-fast), transform var(--transition-fast);
}

.task-card:hover {
  border-color: var(--color-cta);
  transform: translateY(-2px);
}

.task-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.priority-tag {
  font-weight: 700;
}

.task-card h3 {
  margin: 0;
  font-size: 1.35rem;
  line-height: 1.3;
}

.task-meta {
  margin: 0;
  color: var(--color-secondary);
  font-size: 0.9375rem;
}

.qty-block {
  display: grid;
  gap: 8px;
  padding: 12px;
  border-radius: 10px;
  background: var(--color-background);
}

.qty-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.remain {
  margin: 0;
  font-size: 0.875rem;
}

.info-grid {
  margin: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 12px;
}

.info-grid dt {
  margin: 0;
  color: var(--color-secondary);
  font-size: 0.75rem;
}

.info-grid dd {
  margin: 2px 0 0;
  font-weight: 600;
}

.report-btn {
  width: 100%;
  min-height: 52px;
  margin-top: auto;
  font-size: 1.05rem;
  font-weight: 600;
}

.soon-tag {
  margin-left: 8px;
}

.empty-state {
  grid-column: 1 / -1;
}

.empty-hint {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .hero-card {
    flex-direction: column;
    align-items: stretch;
  }

  .refresh-btn,
  .report-btn {
    width: 100%;
  }

  .task-grid {
    grid-template-columns: 1fr;
  }
}
</style>
