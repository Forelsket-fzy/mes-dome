export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages?: number
}

export interface WorkOrderListItem {
  id: number
  erpOrderNo: string
  productCode: string
  productName: string
  planQty: number
  completedQty: number
  status: number
  createdAt?: string
  dueDate?: string
}

export interface OperationTaskItem {
  id: number
  seq: number
  operationCode: string
  operationName: string
  planQty: number
  completedQty: number
  status: number
  priority: number
  plannedStart?: string
  assignedTo?: number
}

export interface WorkOrderDetail extends WorkOrderListItem {
  erpStatus?: string
  version?: number
  cancelReason?: string
  updatedAt?: string
  operations: OperationTaskItem[]
}

export interface WorkOrderQueryParams {
  pageNum?: number
  pageSize?: number
  status?: number
  erpOrderNo?: string
  productCode?: string
}

export interface WorkOrderStatusStatItem {
  status: number
  statusLabel: string
  count: number
}

export interface WorkOrderStats {
  total: number
  items: WorkOrderStatusStatItem[]
}
