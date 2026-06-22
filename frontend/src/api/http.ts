import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import router from '../router'

export const http = axios.create({
  baseURL: '/api',
  timeout: 8000
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const auth = useAuthStore()
    if (error.response?.status === 401) {
      auth.logout()
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else if (error.response?.status === 403) {
      const message = error.response.data?.message || '无权限访问该资源'
      ElMessage.error(message)
      if (message.includes('身份已失效')) {
        auth.logout()
        router.push('/login')
      }
    } else if (error.response?.status >= 500) {
      ElMessage.error('服务器错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)
