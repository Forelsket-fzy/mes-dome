<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, RefreshRight } from '@element-plus/icons-vue'
import {
  fetchIntegrationLogs,
  retryIntegrationLog,
  type IntegrationLogItem,
} from '@/api/integration'

const loading = ref(false)
const rows = ref<IntegrationLogItem[]>([])
const total = ref(0)

const query = reactive({
  current: 1,
  size: 10,
  status: null as number | null,
  bizType: '' as string,
})

const statusMap: Record<number, { label: string; type: 'info' | 'success' | 'danger' | 'warning' }> = {
  0: { label: '待发送', type: 'info' },
  1: { label: '成功', type: 'success' },
  2: { label: '失败', type: 'danger' },
  3: { label: '重试中', type: 'warning' },
}

async function load() {
  loading.value = true
  try {
    const page = await fetchIntegrationLogs({
      current: query.current,
      size: query.size,
      status: query.status,
      bizType: query.bizType || undefined,
    })
    rows.value = page.records ?? []
    total.value = page.total ?? 0
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleRetry(row: IntegrationLogItem) {
  try {
    await retryIntegrationLog(row.id)
    ElMessage.success('已触发重试')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重试失败')
  }
}

onMounted(load)
</script>

<template>
  <section class="integration-page">
    <article class="page-card hero">
      <div>
        <p class="eyebrow">D30~D31 · 集成对账</p>
        <h2>IntegrationLog</h2>
        <p class="muted-text">报工回传 ERP 的对账日志，失败可手动重试（D29）。</p>
      </div>
      <el-button type="primary" class="cursor-pointer" :icon="Refresh" :loading="loading" @click="load">
        刷新
      </el-button>
    </article>

    <article class="page-card">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option :value="0" label="待发送" />
            <el-option :value="1" label="成功" />
            <el-option :value="2" label="失败" />
            <el-option :value="3" label="重试中" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型">
          <el-input v-model="query.bizType" clearable placeholder="REPORT_CALLBACK" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="cursor-pointer" @click="() => { query.current = 1; load() }">
            查询
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="bizType" label="业务类型" width="140" />
        <el-table-column prop="bizId" label="业务ID" width="100" />
        <el-table-column prop="idempotentKey" label="幂等键" min-width="140" />
        <el-table-column prop="targetSystem" label="目标" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status as number]?.type || 'info'">
              {{ statusMap[row.status as number]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试" width="70" />
        <el-table-column prop="errorMsg" label="错误" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 1"
              type="primary"
              link
              class="cursor-pointer"
              :icon="RefreshRight"
              @click="handleRetry(row)"
            >
              重试
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          background
          layout="total, prev, pager, next"
          :total="total"
          @current-change="load"
        />
      </div>
    </article>
  </section>
</template>

<style scoped>
.integration-page {
  display: grid;
  gap: 16px;
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 24px;
}

.page-card {
  padding: 16px 20px;
}

.eyebrow {
  margin: 0;
  color: var(--el-color-primary);
  font-size: 13px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.muted-text {
  color: var(--el-text-color-secondary);
}
</style>
