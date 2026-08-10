<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import { fetchWorkOrderDetail } from '@/api/workOrder'
import type { WorkOrderDetail } from '@/api/workOrder.types'
import {
  calcProgress,
  formatDateTime,
  getTaskStatusMeta,
  getWorkOrderStatusMeta,
} from '@/constants/workOrderStatus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<WorkOrderDetail | null>(null)

const workOrderId = computed(() => Number(route.params.id))

async function loadDetail() {
  if (!workOrderId.value || Number.isNaN(workOrderId.value)) {
    ElMessage.error('工单 ID 无效')
    await router.replace('/admin/work-orders')
    return
  }

  loading.value = true
  try {
    detail.value = await fetchWorkOrderDetail(workOrderId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/admin/work-orders')
}

onMounted(loadDetail)
</script>

<template>
  <section v-loading="loading" class="work-order-detail">
    <div class="detail-toolbar">
      <el-button class="cursor-pointer" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>
      <el-button class="cursor-pointer" @click="loadDetail">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
      <el-button type="primary" class="cursor-pointer" @click="router.push('/admin/dispatch')">
        去派工
      </el-button>
    </div>

    <template v-if="detail">
      <article class="summary-card page-card">
        <div class="summary-header">
          <div>
            <p class="eyebrow">工单详情</p>
            <h2>{{ detail.erpOrderNo }}</h2>
            <p class="muted-text">{{ detail.productName }}（{{ detail.productCode }}）</p>
          </div>
          <el-tag
            size="large"
            :type="getWorkOrderStatusMeta(detail.status).tagType"
            effect="light"
          >
            {{ getWorkOrderStatusMeta(detail.status).label }}
          </el-tag>
        </div>

        <el-descriptions :column="3" border class="summary-descriptions">
          <el-descriptions-item label="MES状态">
            {{ getWorkOrderStatusMeta(detail.status).label }} ({{ detail.status }})
          </el-descriptions-item>
          <el-descriptions-item label="ERP状态">
            {{ detail.erpStatus || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="计划数量">
            {{ detail.planQty }}
          </el-descriptions-item>
          <el-descriptions-item label="完工数量">
            {{ detail.completedQty }}
          </el-descriptions-item>
          <el-descriptions-item label="完成进度">
            {{ calcProgress(detail.completedQty, detail.planQty) }}%
          </el-descriptions-item>
          <el-descriptions-item label="乐观锁版本">
            {{ detail.version ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(detail.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDateTime(detail.updatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="交期">
            {{ formatDateTime(detail.dueDate) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.cancelReason" label="取消原因" :span="3">
            {{ detail.cancelReason }}
          </el-descriptions-item>
        </el-descriptions>
      </article>

      <article class="operations-card page-card">
        <div class="section-header">
          <h3>工序任务</h3>
          <p class="muted-text">按 seq 升序，共 {{ detail.operations?.length ?? 0 }} 道工序</p>
        </div>

        <el-table :data="detail.operations ?? []" stripe empty-text="暂无工序">
          <el-table-column prop="seq" label="序号" width="70" />
          <el-table-column prop="operationCode" label="工序编码" min-width="110" />
          <el-table-column prop="operationName" label="工序名称" min-width="140" />
          <el-table-column label="计划/完成" min-width="120">
            <template #default="{ row }">
              {{ row.completedQty }} / {{ row.planQty }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getTaskStatusMeta(row.status).tagType" effect="light">
                {{ getTaskStatusMeta(row.status).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="priority" label="优先级" width="90" />
          <el-table-column label="计划开始" min-width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.plannedStart) }}
            </template>
          </el-table-column>
          <el-table-column label="派工工人ID" width="110">
            <template #default="{ row }">
              {{ row.assignedTo ?? '-' }}
            </template>
          </el-table-column>
        </el-table>
      </article>
    </template>

    <el-empty v-else-if="!loading" description="未找到工单详情" />
  </section>
</template>

<style scoped>
.work-order-detail {
  display: grid;
  gap: 16px;
}

.detail-toolbar {
  display: flex;
  gap: 12px;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.eyebrow {
  margin: 0;
  color: var(--color-cta);
  font-size: 0.8125rem;
  font-weight: 600;
}

.summary-header h2 {
  margin: 6px 0 4px;
}

.summary-header p {
  margin: 0;
}

.section-header {
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0 0 4px;
}

@media (max-width: 768px) {
  .summary-header {
    flex-direction: column;
  }
}
</style>
