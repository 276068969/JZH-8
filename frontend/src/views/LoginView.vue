<template>
  <main class="login-page">
    <section class="login-panel">
      <h1>平台登录</h1>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="测试账号">
          <el-select v-model="form.username">
            <el-option label="系统管理员 admin" value="admin" />
            <el-option label="监管人员 regulator" value="regulator" />
            <el-option label="商家用户 merchant" value="merchant" />
            <el-option label="普通用户 user" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">登录</el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'regulator', password: '123456' })

async function submit() {
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    if (auth.role === 'MERCHANT') {
      router.push('/merchant')
    } else if (auth.role === 'ADMIN' || auth.role === 'REGULATOR') {
      router.push('/admin')
    } else {
      router.push('/')
    }
  } catch {
    ElMessage.error('账号或密码错误')
  } finally {
    loading.value = false
  }
}
</script>
