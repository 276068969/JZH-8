import { defineStore } from 'pinia'
import { http } from '../api/http'

type Role = 'ADMIN' | 'REGULATOR' | 'MERCHANT' | 'USER'

interface AuthState {
  token: string
  username: string
  displayName: string
  role: Role | ''
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    displayName: localStorage.getItem('displayName') || '',
    role: (localStorage.getItem('role') as Role) || ''
  }),
  actions: {
    async login(username: string, password: string) {
      const { data } = await http.post('/auth/login', { username, password })
      this.token = data.token
      this.username = data.username
      this.displayName = data.displayName
      this.role = data.role
      localStorage.setItem('token', data.token)
      localStorage.setItem('username', data.username)
      localStorage.setItem('displayName', data.displayName)
      localStorage.setItem('role', data.role)
    },
    logout() {
      this.token = ''
      this.username = ''
      this.displayName = ''
      this.role = ''
      localStorage.clear()
    }
  }
})
