export interface StatusMeta {
  label: string
  tagType: 'info' | 'primary' | 'warning' | 'success' | 'danger'
}

export const WORK_ORDER_STATUS_MAP: Record<number, StatusMeta> = {
  0: { label: '已下发', tagType: 'info' },
  1: { label: '已派工', tagType: 'primary' },
  2: { label: '执行中', tagType: 'warning' },
  3: { label: '部分完工', tagType: 'warning' },
  4: { label: '已完工', tagType: 'success' },
  5: { label: '已关闭', tagType: 'info' },
  6: { label: '已取消', tagType: 'danger' },
}

export const WORK_ORDER_STATUS_OPTIONS = Object.entries(WORK_ORDER_STATUS_MAP).map(
  ([value, meta]) => ({
    value: Number(value),
    label: meta.label,
  }),
)

export const TASK_STATUS_MAP: Record<number, StatusMeta> = {
  0: { label: '待派工', tagType: 'info' },
  1: { label: '生产中', tagType: 'warning' },
  2: { label: '已完工', tagType: 'success' },
}

export function getWorkOrderStatusMeta(status: number): StatusMeta {
  return WORK_ORDER_STATUS_MAP[status] ?? { label: `未知(${status})`, tagType: 'info' }
}

export function getTaskStatusMeta(status: number): StatusMeta {
  return TASK_STATUS_MAP[status] ?? { label: `未知(${status})`, tagType: 'info' }
}

export function formatDateTime(value?: string): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

export function calcProgress(completed: number, plan: number): number {
  if (!plan) {
    return 0
  }
  return Math.min(100, Math.round((completed / plan) * 100))
}
