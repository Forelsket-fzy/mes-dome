<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { fetchWorkOrderStats } from '@/api/workOrder'
import type { WorkOrderStats } from '@/api/workOrder.types'
import { WORK_ORDER_CHART_COLORS, sumCountsByStatuses } from '@/constants/dashboard'
import { getWorkOrderStatusMeta } from '@/constants/workOrderStatus'

const router = useRouter()
const loading = ref(false)
const stats = ref<WorkOrderStats | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
const barChartRef = ref<HTMLElement | null>(null)
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

const summaryCards = computed(() => {
  const items = stats.value?.items ?? []
  return [
    {
      label: '工单总数',
      value: stats.value?.total ?? 0,
      hint: '全部 MES 工单',
      accent: '#64748B',
    },
    {
      label: '进行中',
      value: sumCountsByStatuses(items, [0, 1, 2, 3]),
      hint: '已下发 ~ 部分完工',
      accent: '#F97316',
    },
    {
      label: '已完工',
      value: sumCountsByStatuses(items, [4]),
      hint: '待 ERP 归档',
      accent: '#22C55E',
    },
    {
      label: '终态',
      value: sumCountsByStatuses(items, [5, 6]),
      hint: '已关闭 + 已取消',
      accent: '#94A3B8',
    },
  ]
})

const visibleItems = computed(() =>
  (stats.value?.items ?? []).filter((item) => item.count > 0),
)

async function loadStats() {
  loading.value = true
  try {
    stats.value = await fetchWorkOrderStats()
    await nextTick()
    renderCharts()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载看板失败')
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  renderPieChart()
  renderBarChart()
}

function renderPieChart() {
  if (!pieChartRef.value) {
    return
  }
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }

  const data = visibleItems.value.map((item) => ({
    name: item.statusLabel,
    value: item.count,
    itemStyle: { color: WORK_ORDER_CHART_COLORS[item.status] ?? '#64748B' },
  }))

  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      right: 0,
      top: 'center',
    },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['38%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 600,
          },
        },
        data,
      },
    ],
  })
}

function renderBarChart() {
  if (!barChartRef.value || !stats.value) {
    return
  }
  if (!barChart) {
    barChart = echarts.init(barChartRef.value)
  }

  const items = stats.value.items
  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    grid: {
      left: 24,
      right: 16,
      top: 24,
      bottom: 24,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: items.map((item) => item.statusLabel),
      axisLabel: {
        interval: 0,
        rotate: items.length > 5 ? 20 : 0,
      },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
    },
    series: [
      {
        type: 'bar',
        data: items.map((item) => ({
          value: item.count,
          itemStyle: { color: WORK_ORDER_CHART_COLORS[item.status] ?? '#64748B' },
        })),
        barMaxWidth: 42,
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
        },
      },
    ],
  })
}

function handleResize() {
  pieChart?.resize()
  barChart?.resize()
}

function goWorkOrders(status?: number) {
  router.push({
    path: '/admin/work-orders',
    query: status === undefined ? {} : { status: String(status) },
  })
}

watch(visibleItems, () => {
  renderCharts()
})

onMounted(() => {
  loadStats()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  barChart?.dispose()
  pieChart = null
  barChart = null
})
</script>

<template>
  <section v-loading="loading" class="dashboard-page">
    <div class="toolbar">
      <el-alert
        title="统计数据来自 D11 API，Redis 缓存 60 秒"
        type="info"
        show-icon
        :closable="false"
      />
      <el-button class="cursor-pointer" @click="loadStats">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="summary-grid">
      <article
        v-for="card in summaryCards"
        :key="card.label"
        class="summary-card page-card cursor-pointer"
        @click="goWorkOrders()"
      >
        <p class="muted-text">{{ card.label }}</p>
        <h3 :style="{ color: card.accent }">{{ card.value }}</h3>
        <small class="muted-text">{{ card.hint }}</small>
      </article>
    </div>

    <div class="charts-grid">
      <article class="chart-card page-card">
        <div class="chart-header">
          <h3>工单状态分布</h3>
          <p class="muted-text">环形图 · 仅展示 count &gt; 0 的状态</p>
        </div>
        <div v-if="visibleItems.length" ref="pieChartRef" class="chart-box" />
        <el-empty v-else description="暂无工单数据" />
      </article>

      <article class="chart-card page-card">
        <div class="chart-header">
          <h3>各状态数量对比</h3>
          <p class="muted-text">柱状图 · 含 count=0 的完整 7 态</p>
        </div>
        <div ref="barChartRef" class="chart-box" />
      </article>
    </div>

    <article class="table-card page-card">
      <div class="chart-header">
        <h3>状态明细</h3>
        <p class="muted-text">点击行可跳转工单列表并按状态筛选</p>
      </div>
      <el-table :data="stats?.items ?? []" stripe>
        <el-table-column prop="status" label="状态码" width="90" />
        <el-table-column label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getWorkOrderStatusMeta(row.status).tagType" effect="light">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="count" label="数量" width="100" />
        <el-table-column label="占比" min-width="180">
          <template #default="{ row }">
            <div class="ratio-cell">
              <span>
                {{
                  stats?.total
                    ? `${Math.round((row.count / stats.total) * 100)}%`
                    : '0%'
                }}
              </span>
              <el-progress
                :percentage="
                  stats?.total ? Math.round((row.count / stats.total) * 100) : 0
                "
                :stroke-width="8"
                :show-text="false"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              class="cursor-pointer"
              @click="goWorkOrders(row.status)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </article>
  </section>
</template>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card h3 {
  margin: 8px 0;
  font-size: 2rem;
  line-height: 1;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-card,
.table-card {
  min-height: 360px;
}

.chart-header h3 {
  margin: 0 0 4px;
}

.chart-header p {
  margin: 0 0 16px;
}

.chart-box {
  width: 100%;
  height: 300px;
}

.ratio-cell {
  display: grid;
  gap: 6px;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
