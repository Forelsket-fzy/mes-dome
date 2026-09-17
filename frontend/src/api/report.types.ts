export interface DefectItem {
  reasonId: number
  qty: number
}

export interface ReportSubmitBody {
  taskId: number
  goodQty: number
  defectQty: number
  defects?: DefectItem[]
}

export interface ReportAcceptResponse {
  reportId: number
  requestId: string
  status: number
  message: string
}

export interface ReportStatus {
  reportId: number
  requestId: string
  taskId: number
  goodQty: number
  defectQty: number
  status: number
  errorMsg?: string
  reportedAt?: string
}

export interface DefectReason {
  id: number
  code: string
  name: string
  defectType: string
  operationCode?: string
}
