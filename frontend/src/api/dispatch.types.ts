export interface WorkerListItem {
  id: number
  username: string
  realName: string
  phone?: string
  email?: string
  skillLevel?: number
}

export interface WorkerQueryParams {
  pageNum?: number
  pageSize?: number
  skillLevel?: number
  keyword?: string
}

export interface DispatchRequest {
  taskId: number
  assigneeId: number
}

export interface DispatchResponse {
  dispatchId: number
  taskId: number
  workOrderId: number
  workOrderStatus: number
}

export interface AuditQueryParams {
  pageNum?: number
  pageSize?: number
  taskId?: number
  assigneeId?: number
  operatorId?: number
}

export interface AuditListItem {
  auditId: number
  operatorId: number
  operatorName?: string
  dispatchId: number
  taskId: number
  workOrderId: number
  erpOrderNo?: string
  operationName?: string
  operationCode?: string
  assigneeId: number
  assigneeName?: string
  snapshotJson?: string
  createdAt?: string
  mode?: number
}

export interface MyTaskItem {
  taskId: number
  workOrderId: number
  erpOrderNo: string
  productCode: string
  productName?: string
  operationCode: string
  operationName?: string
  seq: number
  planQty: number
  completedQty: number
  taskStatus: number
  workOrderStatus: number
  priority: number
  plannedStart?: string
}
