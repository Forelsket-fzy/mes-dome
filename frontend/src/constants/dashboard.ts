/** 与 7 态工单 status 顺序对应，用于 ECharts 配色 */
export const WORK_ORDER_CHART_COLORS = [
  '#64748B',
  '#3B82F6',
  '#F59E0B',
  '#F97316',
  '#22C55E',
  '#94A3B8',
  '#EF4444',
]

export function sumCountsByStatuses(
  items: Array<{ status: number; count: number }>,
  statuses: number[],
): number {
  return items
    .filter((item) => statuses.includes(item.status))
    .reduce((sum, item) => sum + item.count, 0)
}
