import request from '@/api/request'
import type { ApiResult } from '@/api/types'
import type {
  PageResult,
  WorkOrderDetail,
  WorkOrderListItem,
  WorkOrderQueryParams,
  WorkOrderStats,
} from '@/api/workOrder.types'

export async function fetchWorkOrderPage(
  params: WorkOrderQueryParams,
): Promise<PageResult<WorkOrderListItem>> {
  const { data } = await request.get<ApiResult<PageResult<WorkOrderListItem>>>(
    '/api/work-orders',
    { params },
  )
  if (!data.data) {
    throw new Error(data.message || '查询工单失败')
  }
  return data.data
}

export async function fetchWorkOrderDetail(id: number): Promise<WorkOrderDetail> {
  const { data } = await request.get<ApiResult<WorkOrderDetail>>(`/api/work-orders/${id}`)
  if (!data.data) {
    throw new Error(data.message || '查询工单详情失败')
  }
  return data.data
}

export async function fetchWorkOrderStats(): Promise<WorkOrderStats> {
  const { data } = await request.get<ApiResult<WorkOrderStats>>('/api/work-orders/stats')
  if (!data.data) {
    throw new Error(data.message || '查询看板统计失败')
  }
  return data.data
}
