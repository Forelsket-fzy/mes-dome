import request from '@/api/request'
import type { ApiResult } from '@/api/types'
import type { PageResult } from '@/api/workOrder.types'

export interface IntegrationLogItem {
  id: number
  bizType: string
  bizId: string
  idempotentKey: string
  targetSystem: string
  status: number
  payload?: string
  retryCount: number
  errorMsg?: string
  createdAt?: string
  updatedAt?: string
}

export interface IntegrationLogQuery {
  current?: number
  size?: number
  bizType?: string
  targetSystem?: string
  status?: number | null
}

export async function fetchIntegrationLogs(
  params: IntegrationLogQuery,
): Promise<PageResult<IntegrationLogItem>> {
  const { data } = await request.get<ApiResult<PageResult<IntegrationLogItem>>>(
    '/api/integration/logs',
    { params },
  )
  if (!data.data) {
    throw new Error(data.message || '查询集成日志失败')
  }
  return data.data
}

export async function retryIntegrationLog(id: number): Promise<IntegrationLogItem> {
  const { data } = await request.post<ApiResult<IntegrationLogItem>>(
    `/api/integration/logs/${id}/retry`,
  )
  if (!data.data) {
    throw new Error(data.message || '重试失败')
  }
  return data.data
}
