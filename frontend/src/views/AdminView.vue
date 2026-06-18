<template>
  <main class="admin">
    <section class="admin-head">
      <div>
        <p class="eyebrow">Management Console</p>
        <h1>监管后台</h1>
      </div>
      <el-tag size="large">{{ auth.role || '未登录' }}</el-tag>
    </section>

    <section class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <el-tabs v-model="active">
      <el-tab-pane label="商品审核" name="products">
        <el-table :data="products" border>
          <el-table-column prop="name" label="商品" min-width="170" />
          <el-table-column prop="category" label="分类" width="110" />
          <el-table-column prop="merchantName" label="商家" min-width="150" />
          <el-table-column prop="certificationNo" label="认证编号" min-width="180" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="auditProduct(row.id, 'APPROVED', '审核通过')">通过</el-button>
              <el-button size="small" type="warning" @click="auditProduct(row.id, 'RECTIFYING', '需补充报告或警示标签')">整改</el-button>
              <el-button size="small" type="danger" @click="auditProduct(row.id, 'OFF_SHELF', '违规下架')">下架</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="商家资质" name="merchants">
        <el-table :data="merchants" border>
          <el-table-column prop="name" label="商家名称" min-width="170" />
          <el-table-column prop="licenseNo" label="营业/资质编号" min-width="190" />
          <el-table-column prop="contact" label="联系方式" min-width="170" />
          <el-table-column prop="status" label="审核状态" width="120">
            <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="黑名单" width="100">
            <template #default="{ row }"><el-switch v-model="row.blacklisted" @change="toggleBlacklist(row)" /></template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="auditMerchant(row.id, 'APPROVED', '资质通过')">通过</el-button>
              <el-button size="small" type="warning" @click="auditMerchant(row.id, 'RECTIFYING', '资质材料需补正')">整改</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="投诉处理" name="complaints">
        <el-table :data="complaints" border>
          <el-table-column prop="productName" label="商品" min-width="160" />
          <el-table-column prop="reporter" label="投诉人" width="110" />
          <el-table-column prop="reason" label="投诉原因" min-width="240" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }"><el-tag :type="complaintType(row.status)">{{ complaintText(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="处理记录" min-width="180">
            <template #default="{ row }">{{ row.records.join(' / ') }}</template>
          </el-table-column>
          <el-table-column label="操作" width="190">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="processComplaint(row.id, 'PROCESSING', '监管人员已介入核查')">受理</el-button>
              <el-button size="small" type="success" @click="processComplaint(row.id, 'RESOLVED', '投诉已处理并反馈')">办结</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="用户管理" name="users">
        <el-table :data="users" border>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="账号" />
          <el-table-column prop="displayName" label="名称" />
          <el-table-column prop="role" label="角色" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const active = ref('products')
const dashboard = ref<Record<string, number>>({})
const products = ref<any[]>([])
const merchants = ref<any[]>([])
const complaints = ref<any[]>([])
const users = ref<any[]>([])

const metrics = computed(() => [
  { label: '商品总数', value: dashboard.value.products ?? 0 },
  { label: '待审商品', value: dashboard.value.pendingProducts ?? 0 },
  { label: '投诉数量', value: dashboard.value.complaints ?? 0 },
  { label: '黑名单商家', value: dashboard.value.blacklistedMerchants ?? 0 },
  { label: '平台账号', value: dashboard.value.users ?? 0 }
])

async function loadAll() {
  const [dash, productRes, merchantRes, complaintRes, userRes] = await Promise.all([
    http.get('/admin/dashboard'),
    http.get('/admin/products'),
    http.get('/admin/merchants'),
    http.get('/admin/complaints'),
    http.get('/admin/users')
  ])
  dashboard.value = dash.data
  products.value = productRes.data
  merchants.value = merchantRes.data
  complaints.value = complaintRes.data
  users.value = userRes.data
}

async function auditProduct(id: number, status: string, remark: string) {
  await http.patch(`/admin/products/${id}/audit`, { status, remark })
  ElMessage.success('商品状态已更新')
  await loadAll()
}

async function auditMerchant(id: number, status: string, remark: string) {
  await http.patch(`/admin/merchants/${id}/audit`, { status, remark })
  ElMessage.success('商家资质已更新')
  await loadAll()
}

async function toggleBlacklist(row: any) {
  await http.patch(`/admin/merchants/${row.id}/blacklist`, null, { params: { enabled: row.blacklisted } })
  ElMessage.success(row.blacklisted ? '已加入黑名单' : '已移出黑名单')
  await loadAll()
}

async function processComplaint(id: number, status: string, record: string) {
  await http.patch(`/admin/complaints/${id}`, { status, record })
  ElMessage.success('投诉处理状态已更新')
  await loadAll()
}

function statusText(status: string) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', RECTIFYING: '整改中', OFF_SHELF: '已下架' }[status] || status
}

function statusType(status: string) {
  return { PENDING: 'info', APPROVED: 'success', REJECTED: 'danger', RECTIFYING: 'warning', OFF_SHELF: 'danger' }[status] || 'info'
}

function complaintText(status: string) {
  return { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已办结', REJECTED: '已驳回' }[status] || status
}

function complaintType(status: string) {
  return { PENDING: 'info', PROCESSING: 'warning', RESOLVED: 'success', REJECTED: 'danger' }[status] || 'info'
}

onMounted(loadAll)
</script>
