<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createRequestId,
  fetchDefectReasons,
  fetchReportStatus,
  submitReport,
} from '@/api/report'
import type { DefectReason, ReportStatus } from '@/api/report.types'
import { fetchMyTasks } from '@/api/dispatch'
import type { MyTaskItem } from '@/api/dispatch.types'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const polling = ref(false)
const task = ref<MyTaskItem | null>(null)
const reasons = ref<DefectReason[]>([])
const lastStatus = ref<ReportStatus | null>(null)
const requestId = ref(createRequestId())
let pollTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  goodQty: 1,
  defectQty: 0,
  reasonId: null as number | null,
})

const remaining = computed(() => {
  if (!task.value) return 0
  return Math.max(0, (task.value.planQty ?? 0) - (task.value.completedQty ?? 0))
})

const statusLabel = computed(() => {
  const s = lastStatus.value?.status
  if (s === 0) return '处理中'
  if (s === 1) return '成功'
  if (s === 2) return '失败'
  return '未提交'
})

async function loadTask() {
  const taskId = Number(route.params.taskId)
  if (!taskId) {
    ElMessage.error('任务 ID 无效')
    return
  }
  loading.value = true
  try {
    const tasks = await fetchMyTasks()
    task.value = tasks.find((item) => item.taskId === taskId) ?? null
    if (!task.value) {
      ElMessage.warning('未找到该任务，可能未派工给您或已完工')
      return
    }
    reasons.value = await fetchDefectReasons(task.value.operationCode)
    form.goodQty = Math.min(1, remaining.value || 1)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  polling.value = false
}

async function pollOnce(reportId: number) {
  const status = await fetchReportStatus(reportId)
  lastStatus.value = status
  if (status.status === 1) {
    stopPoll()
    ElMessage.success('报工成功')
    await loadTask()
  } else if (status.status === 2) {
    stopPoll()
    ElMessage.error(status.errorMsg || '报工失败')
  }
}

function startPoll(reportId: number) {
  stopPoll()
  polling.value = true
  pollTimer = setInterval(() => {
    pollOnce(reportId).catch((error) => {
      ElMessage.error(error instanceof Error ? error.message : '轮询失败')
      stopPoll()
    })
  }, 1500)
  pollOnce(reportId).catch(() => undefined)
}

async function handleSubmit() {
  if (!task.value) return
  if (form.goodQty + form.defectQty <= 0) {
    ElMessage.warning('报工数量必须大于 0')
    return
  }
  if (form.goodQty + form.defectQty > remaining.value) {
    ElMessage.warning(`不能超过剩余 ${remaining.value}`)
    return
  }
  if (form.defectQty > 0 && !form.reasonId) {
    ElMessage.warning('有不良时请选择不良原因')
    return
  }

  submitting.value = true
  try {
    if (!requestId.value) {
      requestId.value = createRequestId()
    }
    const body = {
      taskId: task.value.taskId,
      goodQty: form.goodQty,
      defectQty: form.defectQty,
      defects:
        form.defectQty > 0 && form.reasonId
          ? [{ reasonId: form.reasonId, qty: form.defectQty }]
          : [],
    }
    const accepted = await submitReport(body, requestId.value)
    lastStatus.value = {
      reportId: accepted.reportId,
      requestId: accepted.requestId,
      taskId: task.value.taskId,
      goodQty: form.goodQty,
      defectQty: form.defectQty,
      status: accepted.status,
    }
    ElMessage.success(accepted.message || '已受理')
    startPoll(accepted.reportId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  } finally {
    submitting.value = false
  }
}

function renewRequestId() {
  requestId.value = createRequestId()
  lastStatus.value = null
  stopPoll()
  ElMessage.info('已生成新的 X-Request-Id')
}

onMounted(loadTask)
onBeforeUnmount(stopPoll)
</script>

<template>
  <section v-loading="loading" class="report-page">
    <article class="page-card hero">
      <div>
        <p class="eyebrow">D27 · 扫码报工</p>
        <h2>工序报工</h2>
        <p class="muted-text">
          提交后返回 202，前端用同一 UUID 作 X-Request-Id，并轮询直到成功/失败。
        </p>
      </div>
      <el-button class="cursor-pointer" @click="router.push('/terminal/tasks')">返回任务</el-button>
    </article>

    <article v-if="task" class="page-card form-card">
      <h3>{{ task.operationName || task.operationCode }}</h3>
      <p class="muted-text">{{ task.erpOrderNo }} · 剩余 {{ remaining }} 件</p>

      <el-form label-position="top" class="report-form">
        <el-form-item label="良品数量">
          <el-input-number v-model="form.goodQty" :min="0" :max="remaining" />
        </el-form-item>
        <el-form-item label="不良数量">
          <el-input-number v-model="form.defectQty" :min="0" :max="remaining" />
        </el-form-item>
        <el-form-item v-if="form.defectQty > 0" label="不良原因">
          <el-select v-model="form.reasonId" placeholder="选择原因" style="width: 100%">
            <el-option
              v-for="item in reasons"
              :key="item.id"
              :label="`${item.code} · ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="X-Request-Id">
          <div class="req-row">
            <el-input :model-value="requestId" readonly />
            <el-button class="cursor-pointer" @click="renewRequestId">换新</el-button>
          </div>
        </el-form-item>
        <div class="actions">
          <el-button
            type="primary"
            size="large"
            class="cursor-pointer"
            :loading="submitting || polling"
            @click="handleSubmit"
          >
            {{ polling ? '处理中…' : '提交报工' }}
          </el-button>
        </div>
      </el-form>

      <el-alert
        v-if="lastStatus"
        :title="`状态：${statusLabel}`"
        :type="lastStatus.status === 1 ? 'success' : lastStatus.status === 2 ? 'error' : 'info'"
        :description="`reportId=${lastStatus.reportId}${lastStatus.errorMsg ? ' · ' + lastStatus.errorMsg : ''}`"
        show-icon
        :closable="false"
        class="status-alert"
      />
    </article>

    <el-empty v-else description="任务不可用" />
  </section>
</template>

<style scoped>
.report-page {
  display: grid;
  gap: 16px;
}

.hero,
.form-card {
  padding: 20px 24px;
}

.eyebrow {
  margin: 0;
  color: var(--el-color-primary);
  font-size: 13px;
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.report-form {
  margin-top: 16px;
  max-width: 420px;
}

.req-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.actions {
  margin-top: 8px;
}

.status-alert {
  margin-top: 16px;
}

.muted-text {
  color: var(--el-text-color-secondary);
}
</style>
