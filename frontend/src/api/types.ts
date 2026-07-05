export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface TokenPair {
  accessToken: string
  refreshToken: string
  expiresIn: string
}

export interface LoginPayload {
  username: string
  password: string
}
