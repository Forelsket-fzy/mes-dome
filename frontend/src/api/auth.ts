import request from '@/api/request'
import type { ApiResult, LoginPayload, TokenPair } from '@/api/types'

export async function login(payload: LoginPayload): Promise<TokenPair> {
  const { data } = await request.post<ApiResult<TokenPair>>('/api/auth/login', payload)
  if (!data.data) {
    throw new Error(data.message || '登录失败')
  }
  return data.data
}

export async function logout(accessToken: string, refreshToken: string): Promise<void> {
  await request.post(
    '/api/auth/logout',
    null,
    {
      headers: {
        Authorization: `Bearer ${refreshToken}`,
        'X-Access-Token': `Bearer ${accessToken}`,
      },
    },
  )
}
