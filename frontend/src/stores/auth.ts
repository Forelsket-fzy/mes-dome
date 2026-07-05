import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { TokenPair } from '@/api/types'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import { resolveHomePath } from '@/utils/redirect'

const STORAGE_KEY = 'mes-auth'

interface StoredAuth {
  accessToken: string
  refreshToken: string
  expiresIn: string
  username: string
}

function readStorage(): StoredAuth | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as StoredAuth
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

function decodeUsername(accessToken: string): string | null {
  try {
    const payload = accessToken.split('.')[1]
    if (!payload) {
      return null
    }
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))) as {
      username?: string
      sub?: string
    }
    return decoded.username ?? decoded.sub ?? null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const stored = readStorage()
  const accessToken = ref(stored?.accessToken ?? '')
  const refreshToken = ref(stored?.refreshToken ?? '')
  const expiresIn = ref(stored?.expiresIn ?? '')
  const username = ref(stored?.username ?? '')

  const isAuthenticated = computed(() => Boolean(accessToken.value))

  function persist() {
    if (!accessToken.value || !refreshToken.value) {
      localStorage.removeItem(STORAGE_KEY)
      return
    }

    const payload: StoredAuth = {
      accessToken: accessToken.value,
      refreshToken: refreshToken.value,
      expiresIn: expiresIn.value,
      username: username.value,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
  }

  function setTokens(tokens: TokenPair) {
    accessToken.value = tokens.accessToken
    refreshToken.value = tokens.refreshToken
    expiresIn.value = tokens.expiresIn
    username.value = decodeUsername(tokens.accessToken) ?? username.value
    persist()
  }

  function clearSession() {
    accessToken.value = ''
    refreshToken.value = ''
    expiresIn.value = ''
    username.value = ''
    localStorage.removeItem(STORAGE_KEY)
  }

  async function login(inputUsername: string, password: string) {
    const tokens = await loginApi({ username: inputUsername, password })
    username.value = inputUsername
    setTokens(tokens)
    return resolveHomePath(inputUsername)
  }

  async function logout() {
    const currentAccess = accessToken.value
    const currentRefresh = refreshToken.value
    clearSession()
    if (currentAccess && currentRefresh) {
      try {
        await logoutApi(currentAccess, currentRefresh)
      } catch {
        // 本地会话已清理，忽略远端登出失败
      }
    }
  }

  return {
    accessToken,
    refreshToken,
    expiresIn,
    username,
    isAuthenticated,
    setTokens,
    clearSession,
    login,
    logout,
  }
})
