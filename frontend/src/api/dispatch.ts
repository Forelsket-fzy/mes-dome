import request from '@/api/request'
import type { ApiResult } from '@/api/types'
import type { PageResult } from '@/api/workOrder.types'
import type {
  AuditListItem,
  AuditQueryParams,
  DispatchRequest,
  DispatchResponse,
  MyTaskItem,
  WorkerListItem,
  WorkerQueryParams,
} from '@/api/dispatch.types'

export async function fetchWorkers(
  params: WorkerQueryParams,
): Promise<PageResult<WorkerListItem>> {
  const { data } = await request.get<ApiResult<PageResult<WorkerListItem>>>('/api/workers', {
    params,
  })
  if (!data.data) {
    throw new Error(data.message || '查询工人失败')
  }
  return data.data
}

export async function dispatchTask(body: DispatchRequest): Promise<DispatchResponse> {
  const { data } = await request.post<ApiResult<DispatchResponse>>('/api/dispatch', body)
  if (!data.data) {
    throw new Error(data.message || '派工失败')
  }
  return data.data
}

export async function fetchDispatchAudits(
  params: AuditQueryParams,
): Promise<PageResult<AuditListItem>> {
  const { data } = await request.get<ApiResult<PageResult<AuditListItem>>>('/api/dispatch/audit', {
    params,
  })
  if (!data.data) {
    throw new Error(data.message || '查询派工审计失败')
  }
  return data.data
}

export async function fetchMyTasks(): Promise<MyTaskItem[]> {
  const { data } = await request.get<ApiResult<MyTaskItem[]>>('/api/tasks/my')
  return data.data ?? []
}
