<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { fetchWorkOrderPage } from '@/api/workOrder'
import type { WorkOrderListItem } from '@/api/workOrder.types'
import {
  WORK_ORDER_STATUS_OPTIONS,
  calcProgress,
  formatDateTime,
  getWorkOrderStatusMeta,
} from '@/constants/workOrderStatus'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const tableData = ref<WorkOrderListItem[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined as number | undefined,
  erpOrderNo: '',
  productCode: '',
})

async function loadData() {
  loading.value = true
  try {
    const page = await fetchWorkOrderPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      status: query.status,
      erpOrderNo: query.erpOrderNo.trim() || undefined,
      productCode: query.productCode.trim() || undefined,
    })
    tableData.value = page.records ?? []
    total.value = page.total ?? 0
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载工单失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.pageNum = 1
  query.pageSize = 10
  query.status = undefined
  query.erpOrderNo = ''
  query.productCode = ''
  loadData()
}

function handlePageChange(page: number) {
  query.pageNum = page
  loadData()
}

function handleSizeChange(size: number) {
  query.pageSize = size
  query.pageNum = 1
  loadData()
}

function goDetail(row: WorkOrderListItem) {
  router.push(`/admin/work-orders/${row.id}`)
}

function applyRouteQuery() {
  const statusQuery = route.query.status
  if (statusQuery === undefined || statusQuery === '') {
    query.status = undefined
    return
  }
  const parsed = Number(statusQuery)
  query.status = Number.isNaN(parsed) ? undefined : parsed
}

watch(
  () => route.query.status,
  () => {
    applyRouteQuery()
    query.pageNum = 1
    loadData()
  },
)

onMounted(() => {
  applyRouteQuery()
  loadData()
})
</script>

<template>
  <section class="work-order-list">
    <article class="filter-card page-card">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="ERP单号">
          <el-input
            v-model="query.erpOrderNo"
            placeholder="精确匹配"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="产品编码">
          <el-input
            v-model="query.productCode"
            placeholder="模糊匹配"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="item in WORK_ORDER_STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="cursor-pointer" native-type="submit">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button class="cursor-pointer" @click="handleReset">重置</el-button>
          <el-button class="cursor-pointer" @click="loadData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-form-item>
      </el-form>
    </article>

    <article class="table-card page-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        class="cursor-pointer"
        @row-click="goDetail"
      >
        <el-table-column prop="erpOrderNo" label="ERP单号" min-width="140" />
        <el-table-column prop="productCode" label="产品编码" min-width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="进度" min-width="150">
          <template #default="{ row }">
            <div class="progress-cell">
              <span>{{ row.completedQty }} / {{ row.planQty }}</span>
              <el-progress
                :percentage="calcProgress(row.completedQty, row.planQty)"
                :stroke-width="8"
                :show-text="false"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getWorkOrderStatusMeta(row.status).tagType" effect="light">
              {{ getWorkOrderStatusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="交期" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.dueDate) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              class="cursor-pointer"
              @click.stop="goDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </article>
  </section>
</template>

<style scoped>
.work-order-list {
  display: grid;
  gap: 16px;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 12px;
}

.table-card {
  padding-bottom: 8px;
}

.progress-cell {
  display: grid;
  gap: 6px;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
