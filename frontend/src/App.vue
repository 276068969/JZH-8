<template>
  <div class="app-shell">
    <header class="topbar">
      <router-link class="brand" to="/">
        <span class="brand-mark">T</span>
        <span>玩具出售监管平台</span>
      </router-link>
      <nav>
        <router-link to="/">前台查询</router-link>
        <router-link to="/admin">监管后台</router-link>
        <router-link v-if="!auth.token" to="/login">登录</router-link>
        <button v-else class="link-button" @click="logout">{{ auth.displayName }} 退出</button>
      </nav>
    </header>
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.push('/')
}
</script>
