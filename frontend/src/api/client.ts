import axios, { AxiosError, AxiosResponse } from 'axios'
import { useAuthStore } from '@/store/authStore'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Request interceptor: attach access token
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: handle 401 → try refresh
api.interceptors.response.use(
  (res: AxiosResponse) => res,
  async (error: AxiosError) => {
    const original = error.config as any
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      const refreshToken = useAuthStore.getState().refreshToken
      if (refreshToken) {
        try {
          const { data } = await axios.post(
            `${import.meta.env.VITE_API_URL || '/api'}/auth/refresh`,
            { refreshToken }
          )
          const payload = data.data
          useAuthStore.getState().setTokens(payload.accessToken, payload.refreshToken)
          original.headers.Authorization = `Bearer ${payload.accessToken}`
          return api(original)
        } catch {
          useAuthStore.getState().logout()
        }
      } else {
        useAuthStore.getState().logout()
      }
    }
    return Promise.reject(error)
  }
)

export default api
