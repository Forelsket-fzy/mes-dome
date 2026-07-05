import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResult, TokenPair } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 15000,
})

let refreshing = false
let refreshQueue: Array<(token: string | null) => void> = []

function flushQueue(token: string | null) {
  refreshQueue.forEach((callback) => callback(token))
  refreshQueue = []
}

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const authStore = useAuthStore()
  if (authStore.accessToken) {
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResult<unknown>
    if (payload && typeof payload.code === 'number' && payload.code !== 200) {
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    return response
  },
  async (error: AxiosError<ApiResult<unknown>>) => {
    const authStore = useAuthStore()
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status !== 401 || !originalRequest || originalRequest._retry) {
      const message = error.response?.data?.message ?? error.message ?? '网络异常'
      return Promise.reject(new Error(message))
    }

    if (!authStore.refreshToken) {
      authStore.clearSession()
      await router.push('/login')
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }

    if (refreshing) {
      return new Promise((resolve, reject) => {
        refreshQueue.push((token) => {
          if (!token) {
            reject(new Error('登录已过期，请重新登录'))
            return
          }
          originalRequest.headers.Authorization = `Bearer ${token}`
          resolve(request(originalRequest))
        })
      })
    }

    originalRequest._retry = true
    refreshing = true

    try {
      const { data } = await axios.post<ApiResult<TokenPair>>(
        `${import.meta.env.VITE_API_BASE_URL ?? ''}/api/auth/refresh`,
        null,
        {
          headers: {
            Authorization: `Bearer ${authStore.refreshToken}`,
          },
        },
      )

      if (data.code !== 200 || !data.data) {
        throw new Error(data.message || '刷新 token 失败')
      }

      authStore.setTokens(data.data)
      flushQueue(data.data.accessToken)
      originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`
      return request(originalRequest)
    } catch (refreshError) {
      flushQueue(null)
      authStore.clearSession()
      await router.push('/login')
      return Promise.reject(refreshError)
    } finally {
      refreshing = false
    }
  },
)

export default request
