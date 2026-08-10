<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search, User } from '@element-plus/icons-vue'
import { dispatchTask, fetchDispatchAudits, fetchWorkers } from '@/api/dispatch'
import type { AuditListItem, WorkerListItem } from '@/api/dispatch.types'
import { fetchWorkOrderDetail, fetchWorkOrderPage } from '@/api/workOrder'
import type { OperationTaskItem, WorkOrderDetail, WorkOrderListItem } from '@/api/workOrder.types'
import {
  formatDateTime,
  getTaskStatusMeta,
  getWorkOrderStatusMeta,
} from '@/constants/workOrderStatus'

const activeTab = ref('dispatch')
const loadingOrders = ref(false)
const loadingDetail = ref(false)
const loadingWorkers = ref(false)
const dispatching = ref(false)
const loadingAudits = ref(false)

const orderQuery = reactive({
  pageNum: 1,
  pageSize: 8,
  erpOrderNo: '',
  status: undefined as number | undefined,
})

const orders = ref<WorkOrderListItem[]>([])
const orderTotal = ref(0)
const selectedOrderId = ref<number | null>(null)
const detail = ref<WorkOrderDetail | null>(null)

const dispatchDialogVisible = ref(false)
const currentTask = ref<OperationTaskItem | null>(null)
const selectedWorkerId = ref<number | null>(null)
const workerKeyword = ref('')
const workers = ref<WorkerListItem[]>([])
const workerTotal = ref(0)
const workerPage = reactive({ pageNum: 1, pageSize: 8 })

const auditQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  erpFilter: '',
})
const audits = ref<AuditListItem[]>([])
const auditTotal = ref(0)

const pendingTasks = computed(() =>
  (detail.value?.operations ?? []).filter((task) => task.assignedTo == null && task.status !== 2),
)

const assignedTasks = computed(() =>
  (detail.value?.operations ?? []).filter((task) => task.assignedTo != null),
)

async function loadOrders() {
  loadingOrders.value = true
  try {
    const page = await fetchWorkOrderPage({
      pageNum: orderQuery.pageNum,
      pageSize: orderQuery.pageSize,
      erpOrderNo: orderQuery.erpOrderNo.trim() || undefined,
      status: orderQuery.status,
    })
    orders.value = page.records ?? []
    orderTotal.value = page.total ?? 0

    if (selectedOrderId.value == null && orders.value.length > 0) {
      await selectOrder(orders.value[0].id)
    } else if (
      selectedOrderId.value != null &&
      !orders.value.some((item) => item.id === selectedOrderId.value)
    ) {
      selectedOrderId.value = null
      detail.value = null
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载工单失败')
  } finally {
    loadingOrders.value = false
  }
}

async function selectOrder(id: number) {
  selectedOrderId.value = id
  loadingDetail.value = true
  try {
    detail.value = await fetchWorkOrderDetail(id)
  } catch (error) {
    detail.value = null
    ElMessage.error(error instanceof Error ? error.message : '加载工序失败')
  } finally {
    loadingDetail.value = false
  }
}

function handleOrderSearch() {
  orderQuery.pageNum = 1
  loadOrders()
}

function orderRowClassName({ row }: { row: WorkOrderListItem }) {
  return row.id === selectedOrderId.value ? 'is-selected' : ''
}

async function openDispatchDialog(task: OperationTaskItem) {
  currentTask.value = task
  selectedWorkerId.value = null
  workerKeyword.value = ''
  workerPage.pageNum = 1
  dispatchDialogVisible.value = true
  await loadWorkers()
}

async function loadWorkers() {
  loadingWorkers.value = true
  try {
    const page = await fetchWorkers({
      pageNum: workerPage.pageNum,
      pageSize: workerPage.pageSize,
      keyword: workerKeyword.value.trim() || undefined,
    })
    workers.value = page.records ?? []
    workerTotal.value = page.total ?? 0
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载工人失败')
  } finally {
    loadingWorkers.value = false
  }
}

async function confirmDispatch() {
  if (!currentTask.value || selectedWorkerId.value == null) {
    ElMessage.warning('请选择工人')
    return
  }
  dispatching.value = true
  try {
    const result = await dispatchTask({
      taskId: currentTask.value.id,
      assigneeId: selectedWorkerId.value,
    })
    ElMessage.success(`派工成功，工单状态 → ${getWorkOrderStatusMeta(result.workOrderStatus).label}`)
    dispatchDialogVisible.value = false
    await Promise.all([loadOrders(), selectedOrderId.value ? selectOrder(selectedOrderId.value) : Promise.resolve()])
    if (activeTab.value === 'audit') {
      await loadAudits()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '派工失败')
  } finally {
    dispatching.value = false
  }
}

async function loadAudits() {
  loadingAudits.value = true
  try {
    const page = await fetchDispatchAudits({
      pageNum: auditQuery.pageNum,
      pageSize: auditQuery.pageSize,
    })
    const records = page.records ?? []
    const keyword = auditQuery.erpFilter.trim().toLowerCase()
    audits.value = keyword
      ? records.filter((item) => (item.erpOrderNo ?? '').toLowerCase().includes(keyword))
      : records
    auditTotal.value = page.total ?? 0
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载审计失败')
  } finally {
    loadingAudits.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'audit') {
    loadAudits()
  }
})

onMounted(loadOrders)
</script>

<template>
  <section class="dispatch-page">
    <el-tabs v-model="activeTab" class="dispatch-tabs">
      <el-tab-pane label="主动派工" name="dispatch">
        <div class="dispatch-grid">
          <article class="page-card order-panel">
            <div class="panel-header">
              <div>
                <h3>可派工工单</h3>
                <p class="muted-text">建议筛选「已下发 / 已派工」后选择工序派工</p>
              </div>
              <el-button class="cursor-pointer" :icon="Refresh" @click="loadOrders">刷新</el-button>
            </div>

            <el-form :inline="true" class="filter-form" @submit.prevent="handleOrderSearch">
              <el-form-item label="ERP单号">
                <el-input
                  v-model="orderQuery.erpOrderNo"
                  clearable
                  placeholder="精确匹配"
                  style="width: 160px"
                />
              </el-form-item>
              <el-form-item label="状态">
                <el-select
                  v-model="orderQuery.status"
                  clearable
                  placeholder="全部"
                  style="width: 120px"
                >
                  <el-option label="已下发" :value="0" />
                  <el-option label="已派工" :value="1" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" class="cursor-pointer" :icon="Search" @click="handleOrderSearch">
                  查询
                </el-button>
              </el-form-item>
            </el-form>

            <el-table
              v-loading="loadingOrders"
              :data="orders"
              highlight-current-row
              :row-class-name="orderRowClassName"
              empty-text="暂无可派工工单"
              @row-click="(row: WorkOrderListItem) => selectOrder(row.id)"
            >
              <el-table-column prop="erpOrderNo" label="ERP单号" min-width="140" />
              <el-table-column prop="productName" label="产品" min-width="120" show-overflow-tooltip />
              <el-table-column label="状态" width="96">
                <template #default="{ row }">
                  <el-tag :type="getWorkOrderStatusMeta(row.status).tagType" effect="light">
                    {{ getWorkOrderStatusMeta(row.status).label }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>

            <div class="pager">
              <el-pagination
                v-model:current-page="orderQuery.pageNum"
                v-model:page-size="orderQuery.pageSize"
                layout="total, prev, pager, next"
                :total="orderTotal"
                @current-change="loadOrders"
              />
            </div>
          </article>

          <article v-loading="loadingDetail" class="page-card task-panel">
            <template v-if="detail">
              <div class="panel-header">
                <div>
                  <p class="eyebrow">当前工单</p>
                  <h3>{{ detail.erpOrderNo }}</h3>
                  <p class="muted-text">
                    {{ detail.productName }}（{{ detail.productCode }}）·
                    {{ getWorkOrderStatusMeta(detail.status).label }}
                  </p>
                </div>
              </div>

              <div class="section-block">
                <div class="section-title">
                  <h4>待派工序</h4>
                  <el-tag type="warning" effect="plain">{{ pendingTasks.length }}</el-tag>
                </div>
                <el-table :data="pendingTasks" empty-text="该工单工序均已派工或已完工">
                  <el-table-column prop="seq" label="序号" width="70" />
                  <el-table-column prop="operationCode" label="工序" min-width="100" />
                  <el-table-column prop="operationName" label="名称" min-width="120" />
                  <el-table-column label="计划量" width="90">
                    <template #default="{ row }">{{ row.planQty }}</template>
                  </el-table-column>
                  <el-table-column prop="priority" label="优先级" width="80" />
                  <el-table-column label="操作" width="110" fixed="right">
                    <template #default="{ row }">
                      <el-button
                        type="primary"
                        class="cursor-pointer"
                        size="small"
                        @click="openDispatchDialog(row)"
                      >
                        派工
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <div class="section-block">
                <div class="section-title">
                  <h4>已派工序</h4>
                  <el-tag type="success" effect="plain">{{ assignedTasks.length }}</el-tag>
                </div>
                <el-table :data="assignedTasks" empty-text="暂无已派工序">
                  <el-table-column prop="seq" label="序号" width="70" />
                  <el-table-column prop="operationCode" label="工序" min-width="100" />
                  <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                      <el-tag :type="getTaskStatusMeta(row.status).tagType" effect="light">
                        {{ getTaskStatusMeta(row.status).label }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="工人ID" width="100">
                    <template #default="{ row }">{{ row.assignedTo }}</template>
                  </el-table-column>
                  <el-table-column label="计划开始" min-width="160">
                    <template #default="{ row }">{{ formatDateTime(row.plannedStart) }}</template>
                  </el-table-column>
                </el-table>
              </div>
            </template>
            <el-empty v-else description="请选择左侧工单" />
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="派工审计" name="audit">
        <article class="page-card audit-panel">
          <div class="panel-header">
            <div>
              <h3>派工审计日志</h3>
              <p class="muted-text">谁在何时把哪道工序派给了谁</p>
            </div>
            <div class="audit-actions">
              <el-input
                v-model="auditQuery.erpFilter"
                clearable
                placeholder="本页按 ERP 单号过滤"
                style="width: 200px"
                @change="loadAudits"
              />
              <el-button class="cursor-pointer" :icon="Refresh" @click="loadAudits">刷新</el-button>
            </div>
          </div>

          <el-table v-loading="loadingAudits" :data="audits" empty-text="暂无审计记录">
            <el-table-column prop="createdAt" label="时间" min-width="160">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column prop="erpOrderNo" label="ERP单号" min-width="130" />
            <el-table-column prop="operationCode" label="工序" width="100" />
            <el-table-column prop="operatorName" label="操作人" min-width="100">
              <template #default="{ row }">{{ row.operatorName || row.operatorId }}</template>
            </el-table-column>
            <el-table-column prop="assigneeName" label="被派工人" min-width="100">
              <template #default="{ row }">{{ row.assigneeName || row.assigneeId }}</template>
            </el-table-column>
            <el-table-column prop="taskId" label="任务ID" width="90" />
            <el-table-column label="模式" width="90">
              <template #default="{ row }">
                {{ row.mode === 1 ? '主动' : row.mode ?? '-' }}
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              v-model:current-page="auditQuery.pageNum"
              v-model:page-size="auditQuery.pageSize"
              layout="total, prev, pager, next"
              :total="auditTotal"
              @current-change="loadAudits"
            />
          </div>
        </article>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="dispatchDialogVisible"
      title="选择工人派工"
      width="640px"
      destroy-on-close
    >
      <p v-if="currentTask" class="dialog-task muted-text">
        工序 {{ currentTask.operationCode }} · {{ currentTask.operationName }} · 计划
        {{ currentTask.planQty }}
      </p>

      <el-form :inline="true" @submit.prevent="loadWorkers">
        <el-form-item label="关键字">
          <el-input
            v-model="workerKeyword"
            clearable
            placeholder="姓名 / 账号 / 手机"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item>
          <el-button class="cursor-pointer" :icon="Search" @click="loadWorkers">搜索工人</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loadingWorkers"
        :data="workers"
        highlight-current-row
        empty-text="暂无工人"
        @row-click="(row: WorkerListItem) => (selectedWorkerId = row.id)"
      >
        <el-table-column width="56">
          <template #default="{ row }">
            <el-radio v-model="selectedWorkerId" :label="row.id">&nbsp;</el-radio>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="username" label="账号" min-width="100" />
        <el-table-column prop="skillLevel" label="技能" width="80" />
        <el-table-column prop="phone" label="手机" min-width="120" />
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="workerPage.pageNum"
          v-model:page-size="workerPage.pageSize"
          layout="total, prev, pager, next"
          :total="workerTotal"
          @current-change="loadWorkers"
        />
      </div>

      <template #footer>
        <el-button class="cursor-pointer" @click="dispatchDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          class="cursor-pointer"
          :icon="User"
          :loading="dispatching"
          :disabled="selectedWorkerId == null"
          @click="confirmDispatch"
        >
          确认派工
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.dispatch-page {
  display: grid;
  gap: 16px;
}

.dispatch-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(420px, 1.1fr);
  gap: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-header h3 {
  margin: 0;
}

.panel-header p,
.eyebrow {
  margin: 4px 0 0;
}

.eyebrow {
  color: var(--color-cta);
  font-size: 0.8125rem;
  font-weight: 600;
}

.filter-form {
  margin-bottom: 8px;
}

.section-block + .section-block {
  margin-top: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.section-title h4 {
  margin: 0;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.audit-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.dialog-task {
  margin: 0 0 12px;
}

:deep(.is-selected > td) {
  background: rgb(249 115 22 / 8%) !important;
}

@media (max-width: 1100px) {
  .dispatch-grid {
    grid-template-columns: 1fr;
  }
}
</style>
