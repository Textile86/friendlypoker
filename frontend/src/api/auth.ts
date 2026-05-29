import api from './client'

export interface AuthResponse {
  token: string
  username: string
}

export const register = (data: { username: string; email: string; password: string }) =>
  api.post<AuthResponse>('/auth/register', data).then((r) => r.data)

export const login = (data: { username: string; password: string }) =>
  api.post<AuthResponse>('/auth/login', data).then((r) => r.data)
