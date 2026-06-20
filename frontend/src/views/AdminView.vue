<template>
  <main class="admin">
    <section class="admin-head">
      <div>
        <p class="eyebrow">Management Console</p>
        <h1>监管后台</h1>
      </div>
      <el-tag size="large">{{ auth.role || '未登录' }}</el-tag>
    </section>

    <section class="dashboard-section">
      <h2 class="section-title">概览总览</h2>
      <div class="metric-grid">
        <div v-for="item in metrics" :key="item.label" class="metric">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
    </section>

    <section class="dashboard-section">
      <h2 class="section-title">商家审核状态分布</h2>
      <div class="metric-grid">
        <div v-for="item in merchantStatusMetrics" :key="item.label" class="metric status-metric" :class="item.type">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
    </section>

    <section class="dashboard-section">
      <h2 class="section-title">商品审核统计</h2>
      <div class="metric-grid">
        <div class="metric primary">
          <span>审核通过率</span>
          <strong>{{ dashboard.productApprovalRate ?? 0 }}%</strong>
        </div>
        <div class="metric">
          <span>已通过商品</span>
          <strong>{{ dashboard.approvedProducts ?? 0 }}</strong>
        </div>
        <div class="metric">
          <span>已审核总数</span>
          <strong>{{ dashboard.totalReviewedProducts ?? 0 }}</strong>
        </div>
        <div class="metric warning">
          <span>整改中商品</span>
          <strong>{{ dashboard.rectifyingProducts ?? 0 }}</strong>
        </div>
      </div>
    </section>

    <section class="dashboard-section">
      <h2 class="section-title">投诉状态分布</h2>
      <div class="metric-grid">
        <div v-for="item in complaintStatusMetrics" :key="item.label" class="metric status-metric" :class="item.type">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
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

      <el-tab-pane label="审核记录" name="audit-records">
        <div class="filter-bar">
          <el-input
            v-model="searchProductName"
            placeholder="输入商品名称搜索"
            clearable
            style="width: 220px"
            @clear="loadAuditRecords"
            @keyup.enter="searchAuditRecords"
          />
          <el-select
            v-model="searchMerchantId"
            placeholder="选择商家"
            clearable
            style="width: 200px"
            @change="searchAuditRecords"
            @clear="loadAuditRecords"
          >
            <el-option
              v-for="m in merchants"
              :key="m.id"
              :label="m.name"
              :value="m.id"
            />
          </el-select>
          <el-select
            v-model="searchToStatus"
            placeholder="审核结果"
            clearable
            style="width: 160px"
            @change="searchAuditRecords"
            @clear="loadAuditRecords"
          >
            <el-option label="通过" value="APPROVED" />
            <el-option label="整改" value="RECTIFYING" />
            <el-option label="驳回" value="REJECTED" />
            <el-option label="下架" value="OFF_SHELF" />
            <el-option label="待审核" value="PENDING" />
          </el-select>
          <el-button type="primary" @click="searchAuditRecords">查询</el-button>
          <el-button @click="resetAuditSearch">重置</el-button>
        </div>
        <el-table :data="auditRecords" border>
          <el-table-column prop="id" label="记录ID" width="90" />
          <el-table-column prop="productName" label="商品" min-width="170" />
          <el-table-column prop="merchantName" label="商家" min-width="150" />
          <el-table-column label="原状态" width="110">
            <template #default="{ row }"><el-tag :type="statusType(row.fromStatus)" size="small">{{ statusText(row.fromStatus) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="目标状态" width="110">
            <template #default="{ row }"><el-tag :type="statusType(row.toStatus)" size="small">{{ statusText(row.toStatus) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="remark" label="审核意见" min-width="200" show-overflow-tooltip />
          <el-table-column prop="operator" label="操作人" width="120" />
          <el-table-column prop="operateTime" label="操作时间" width="180" />
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
const dashboard = ref<any>({})
const products = ref<any[]>([])
const merchants = ref<any[]>([])
const complaints = ref<any[]>([])
const users = ref<any[]>([])
const auditRecords = ref<any[]>([])
const searchProductName = ref('')
const searchMerchantId = ref<number | null>(null)
const searchToStatus = ref('')

const metrics = computed(() => [
  { label: '商品总数', value: dashboard.value.products ?? 0 },
  { label: '待处理事项', value: dashboard.value.pendingTotal ?? 0, type: 'pending' },
  { label: '投诉总数', value: dashboard.value.complaints ?? 0 },
  { label: '黑名单商家', value: dashboard.value.blacklistedMerchants ?? 0 },
  { label: '平台账号', value: dashboard.value.users ?? 0 }
])

const merchantStatusMetrics = computed(() => {
  const dist = (dashboard.value.merchantStatusDistribution as Record<string, number>) || {}
  const statusMap: Record<string, { label: string; type: string }> = {
    DRAFT: { label: '草稿', type: 'info' },
    PENDING: { label: '待审核', type: 'pending' },
    APPROVED: { label: '已通过', type: 'success' },
    REJECTED: { label: '已驳回', type: 'danger' },
    RECTIFYING: { label: '整改中', type: 'warning' },
    OFF_SHELF: { label: '已下架', type: 'danger' }
  }
  return Object.entries(statusMap).map(([key, info]) => ({
    label: info.label,
    value: dist[key] ?? 0,
    type: info.type
  }))
})

const complaintStatusMetrics = computed(() => {
  const dist = (dashboard.value.complaintStatusDistribution as Record<string, number>) || {}
  const statusMap: Record<string, { label: string; type: string }> = {
    PENDING: { label: '待处理', type: 'pending' },
    PROCESSING: { label: '处理中', type: 'warning' },
    RESOLVED: { label: '已办结', type: 'success' },
    REJECTED: { label: '已驳回', type: 'danger' }
  }
  return Object.entries(statusMap).map(([key, info]) => ({
    label: info.label,
    value: dist[key] ?? 0,
    type: info.type
  }))
})

async function loadAll() {
  const [dash, productRes, merchantRes, complaintRes, userRes, auditRes] = await Promise.all([
    http.get('/admin/dashboard'),
    http.get('/admin/products'),
    http.get('/admin/merchants'),
    http.get('/admin/complaints'),
    http.get('/admin/users'),
    http.get('/admin/audit-records')
  ])
  dashboard.value = dash.data
  products.value = productRes.data
  merchants.value = merchantRes.data
  complaints.value = complaintRes.data
  users.value = userRes.data
  auditRecords.value = auditRes.data
}

async function loadAuditRecords() {
  const res = await http.get('/admin/audit-records')
  auditRecords.value = res.data
}

async function searchAuditRecords() {
  const params: Record<string, any> = {}
  if (searchMerchantId.value) params.merchantId = searchMerchantId.value
  if (searchToStatus.value) params.toStatus = searchToStatus.value
  if (searchProductName.value) {
    const all = await http.get('/admin/audit-records', { params })
    auditRecords.value = all.data.filter((r: any) =>
      r.productName.includes(searchProductName.value)
    )
  } else {
    const res = await http.get('/admin/audit-records', { params })
    auditRecords.value = res.data
  }
}

function resetAuditSearch() {
  searchProductName.value = ''
  searchMerchantId.value = null
  searchToStatus.value = ''
  loadAuditRecords()
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
  return { DRAFT: '草稿', PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', RECTIFYING: '整改中', OFF_SHELF: '已下架' }[status] || status
}

function statusType(status: string) {
  return { DRAFT: 'info', PENDING: 'info', APPROVED: 'success', REJECTED: 'danger', RECTIFYING: 'warning', OFF_SHELF: 'danger' }[status] || 'info'
}

function complaintText(status: string) {
  return { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已办结', REJECTED: '已驳回' }[status] || status
}

function complaintType(status: string) {
  return { PENDING: 'info', PROCESSING: 'warning', RESOLVED: 'success', REJECTED: 'danger' }[status] || 'info'
}

onMounted(loadAll)
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.dashboard-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.metric {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: all 0.2s;
}

.metric:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.metric span {
  font-size: 13px;
  color: #909399;
}

.metric strong {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.metric.primary strong {
  color: #409eff;
}

.metric.success strong {
  color: #67c23a;
}

.metric.warning strong {
  color: #e6a23c;
}

.metric.danger strong {
  color: #f56c6c;
}

.metric.pending strong {
  color: #909399;
}

.metric.info strong {
  color: #909399;
}

.status-metric {
  position: relative;
}

.status-metric::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  border-radius: 8px 0 0 8px;
}

.status-metric.success::before {
  background: #67c23a;
}

.status-metric.warning::before {
  background: #e6a23c;
}

.status-metric.danger::before {
  background: #f56c6c;
}

.status-metric.pending::before {
  background: #909399;
}

.status-metric.info::before {
  background: #909399;
}
</style>
