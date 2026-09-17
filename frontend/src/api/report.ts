import request from '@/api/request'
import type { ApiResult } from '@/api/types'
import type {
  DefectReason,
  ReportAcceptResponse,
  ReportStatus,
  ReportSubmitBody,
} from '@/api/report.types'

export async function submitReport(
  body: ReportSubmitBody,
  requestId: string,
): Promise<ReportAcceptResponse> {
  const { data } = await request.post<ApiResult<ReportAcceptResponse>>('/api/reports', body, {
    headers: { 'X-Request-Id': requestId },
  })
  if (!data.data) {
    throw new Error(data.message || '报工受理失败')
  }
  return data.data
}

export async function fetchReportStatus(reportId: number): Promise<ReportStatus> {
  const { data } = await request.get<ApiResult<ReportStatus>>(`/api/reports/${reportId}`)
  if (!data.data) {
    throw new Error(data.message || '查询报工状态失败')
  }
  return data.data
}

export async function fetchDefectReasons(operationCode?: string): Promise<DefectReason[]> {
  const { data } = await request.get<ApiResult<DefectReason[]>>('/api/defect-reasons', {
    params: operationCode ? { operationCode } : undefined,
  })
  return data.data ?? []
}

export function createRequestId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `req-${Date.now()}-${Math.random().toString(16).slice(2)}`
}
