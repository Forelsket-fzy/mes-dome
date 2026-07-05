const ADMIN_USERS = new Set(['admin', 'planner'])
const TERMINAL_USERS = new Set(['worker1', 'worker2', 'worker3', 'qc'])

export function resolveHomePath(username: string): string {
  if (ADMIN_USERS.has(username)) {
    return '/admin'
  }
  if (TERMINAL_USERS.has(username) || username.startsWith('worker')) {
    return '/terminal'
  }
  return '/admin'
}

export function canAccessAdmin(username: string): boolean {
  return ADMIN_USERS.has(username)
}

export function canAccessTerminal(username: string): boolean {
  return TERMINAL_USERS.has(username) || username.startsWith('worker') || ADMIN_USERS.has(username)
}
