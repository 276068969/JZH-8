<template>
  <main class="merchant-workbench">
    <section class="workbench-head">
      <div>
        <p class="eyebrow">Merchant Workbench</p>
        <h1>商家商品管理工作台</h1>
        <p class="sub-title">{{ merchantProfile?.name || '加载中...' }}</p>
      </div>
      <div class="head-actions">
        <el-tag size="large" type="success">{{ auth.role || 'MERCHANT' }}</el-tag>
        <el-button @click="handleLogout">退出登录</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <div v-for="item in metrics" :key="item.key" class="metric" :class="item.key">
        <span class="metric-label">{{ item.label }}</span>
        <strong class="metric-value">{{ item.value }}</strong>
      </div>
    </section>

    <section class="product-section">
      <div class="section-head">
        <h2>商品管理</h2>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建商品</el-button>
      </div>

      <el-tabs v-model="activeTab" @tab-change="loadProducts">
        <el-tab-pane label="全部商品" name="all" />
        <el-tab-pane label="草稿" name="DRAFT" />
        <el-tab-pane label="待审核" name="PENDING" />
        <el-tab-pane label="整改中" name="RECTIFYING" />
        <el-tab-pane label="已通过" name="APPROVED" />
        <el-tab-pane label="已下架" name="OFF_SHELF" />
      </el-tabs>

      <el-table :data="products" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="商品名称" min-width="180">
          <template #default="{ row }">
            <div class="product-name-cell">
              <img v-if="row.imageUrl" :src="row.imageUrl" class="product-thumb" />
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column prop="price" label="价格" width="110">
          <template #default="{ row }">￥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column prop="certificationNo" label="认证编号" min-width="170">
          <template #default="{ row }">
            <span v-if="row.certificationNo">{{ row.certificationNo }}</span>
            <span v-else class="muted">未填写</span>
          </template>
        </el-table-column>
        <el-table-column prop="reportName" label="检测报告" min-width="160">
          <template #default="{ row }">
            <span v-if="row.reportName">{{ row.reportName }}</span>
            <span v-else class="muted">未上传</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核备注" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.auditRemark">{{ row.auditRemark }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'RECTIFYING'"
              size="small"
              type="primary"
              @click="submitForAudit(row)"
            >
              提交审核
            </el-button>
            <el-button
              v-if="row.status === 'APPROVED'"
              size="small"
              type="warning"
              @click="handleOffShelf(row)"
            >
              下架
            </el-button>
            <el-button
              v-if="row.status === 'OFF_SHELF'"
              size="small"
              type="success"
              @click="handleReEdit(row)"
            >
              重新编辑
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              size="small"
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="products.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无商品，点击「新建商品」开始上架">
          <el-button type="primary" @click="openCreateDialog">新建商品</el-button>
        </el-empty>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新建商品'" width="720px">
      <el-form :model="productForm" label-position="top">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品名称">
              <el-input v-model="productForm.name" placeholder="请输入商品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品分类">
              <el-select v-model="productForm.category" placeholder="请选择分类" style="width: 100%">
                <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="价格（元）">
              <el-input-number v-model="productForm.price" :min="0" :precision="2" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存数量">
              <el-input-number v-model="productForm.stock" :min="0" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="认证编号">
              <el-input v-model="productForm.certificationNo" placeholder="如 CCC-2026-MAG-0088" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检测报告名称">
              <el-input v-model="productForm.reportName" placeholder="如 磁通量检测报告.pdf" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="商品图片 URL">
          <el-input v-model="productForm.imageUrl" placeholder="请输入商品图片链接" />
        </el-form-item>
        <div v-if="productForm.imageUrl" class="image-preview">
          <p class="preview-label">图片预览：</p>
          <img :src="productForm.imageUrl" alt="预览" class="preview-img" />
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProduct" :loading="saving">
          {{ isEdit ? '保存修改' : '创建草稿' }}
        </el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { http } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

interface Product {
  id: number
  name: string
  category: string
  merchantId: number
  merchantName: string
  price: string
  stock: number
  certificationNo: string
  reportName: string
  imageUrl: string
  status: string
  auditRemark: string
}

interface MerchantProfile {
  id: number
  name: string
  licenseNo: string
  contact: string
  status: string
}

const categories = ['益智玩具', '运动玩具', '毛绒玩具', '手工玩具']

const loading = ref(false)
const saving = ref(false)
const products = ref<Product[]>([])
const merchantProfile = ref<MerchantProfile | null>(null)
const dashboard = ref<Record<string, number>>({})
const activeTab = ref('all')
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)

const productForm = reactive({
  name: '',
  category: '益智玩具',
  price: 0,
  stock: 0,
  certificationNo: '',
  reportName: '',
  imageUrl: ''
})

const metrics = computed(() => [
  { key: 'total', label: '全部商品', value: dashboard.value.total ?? 0 },
  { key: 'draft', label: '草稿', value: dashboard.value.draft ?? 0 },
  { key: 'pending', label: '待审核', value: dashboard.value.pending ?? 0 },
  { key: 'rectifying', label: '整改中', value: dashboard.value.rectifying ?? 0 },
  { key: 'approved', label: '已通过', value: dashboard.value.approved ?? 0 },
  { key: 'offShelf', label: '已下架', value: dashboard.value.offShelf ?? 0 }
])

async function loadDashboard() {
  const { data } = await http.get('/merchant/dashboard')
  dashboard.value = data
}

async function loadProducts() {
  loading.value = true
  try {
    const params: Record<string, any> = {}
    if (activeTab.value !== 'all') {
      params.status = activeTab.value
    }
    const { data } = await http.get('/merchant/products', { params })
    products.value = data
  } finally {
    loading.value = false
  }
}

async function loadProfile() {
  try {
    const { data } = await http.get('/merchant/profile')
    merchantProfile.value = data
  } catch (e) {
    // ignore
  }
}

async function loadAll() {
  await Promise.all([loadDashboard(), loadProducts(), loadProfile()])
}

function openCreateDialog() {
  isEdit.value = false
  editingId.value = null
  productForm.name = ''
  productForm.category = '益智玩具'
  productForm.price = 0
  productForm.stock = 0
  productForm.certificationNo = ''
  productForm.reportName = ''
  productForm.imageUrl = ''
  dialogVisible.value = true
}

function openEditDialog(row: Product) {
  isEdit.value = true
  editingId.value = row.id
  productForm.name = row.name
  productForm.category = row.category
  productForm.price = Number(row.price)
  productForm.stock = row.stock
  productForm.certificationNo = row.certificationNo
  productForm.reportName = row.reportName
  productForm.imageUrl = row.imageUrl
  dialogVisible.value = true
}

async function saveProduct() {
  if (!productForm.name.trim()) {
    ElMessage.warning('请输入商品名称')
    return
  }

  saving.value = true
  try {
    if (isEdit.value && editingId.value != null) {
      await http.put(`/merchant/products/${editingId.value}`, productForm)
      ElMessage.success('商品已更新')
    } else {
      await http.post('/merchant/products', productForm)
      ElMessage.success('商品草稿已创建')
    }
    dialogVisible.value = false
    await loadAll()
  } finally {
    saving.value = false
  }
}

function validateSubmission(row: Product): string[] {
  const errors: string[] = []
  if (!row.name?.trim()) errors.push('商品名称不能为空')
  if (!row.category?.trim()) errors.push('商品分类不能为空')
  if (!row.price || Number(row.price) <= 0) errors.push('商品价格必须大于0')
  if (row.stock == null || row.stock < 0) errors.push('库存数量不能为负数')
  if (!row.certificationNo?.trim()) errors.push('认证编号不能为空，请填写3C等认证编号')
  if (!row.reportName?.trim()) errors.push('检测报告不能为空，请上传或填写检测报告名称')
  if (!row.imageUrl?.trim()) errors.push('商品图片不能为空，请上传商品图片')
  return errors
}

async function submitForAudit(row: Product) {
  const preCheckErrors = validateSubmission(row)
  if (preCheckErrors.length > 0) {
    try {
      await ElMessageBox.alert(
        `送审资料不完整，缺少以下内容：\n\n${preCheckErrors.map((e, i) => `${i + 1}. ${e}`).join('\n')}\n\n请先完善资料后再提交审核。`,
        '资料校验未通过',
        { type: 'warning', confirmButtonText: '去完善' }
      )
      openEditDialog(row)
    } catch {
      // user closed
    }
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要将「${row.name}」提交审核吗？提交后将无法修改，等待监管审核。`,
      '提交审核确认',
      { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '取消' }
    )
    await http.post(`/merchant/products/${row.id}/submit`)
    ElMessage.success('已提交审核，请等待监管处理')
    await loadAll()
  } catch (e: any) {
    if (e !== 'cancel' && e !== undefined) {
      const msg = e?.response?.data?.message || e?.message || '提交失败'
      ElMessage.error(msg)
    }
  }
}

async function handleOffShelf(row: Product) {
  try {
    await ElMessageBox.confirm(
      `确定要将「${row.name}」下架吗？下架后消费者将无法看到该商品。`,
      '下架确认',
      { type: 'warning', confirmButtonText: '确认下架', cancelButtonText: '取消' }
    )
    await http.post(`/merchant/products/${row.id}/off-shelf`)
    ElMessage.success('商品已下架')
    await loadAll()
  } catch (e: any) {
    if (e !== 'cancel' && e !== undefined) {
      const msg = e?.response?.data?.message || e?.message || '操作失败'
      ElMessage.error(msg)
    }
  }
}

async function handleReEdit(row: Product) {
  try {
    await ElMessageBox.confirm(
      `确定要将「${row.name}」转为草稿重新编辑吗？重新提交后需要再次审核。`,
      '重新编辑确认',
      { type: 'info', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
    await http.post(`/merchant/products/${row.id}/re-edit`)
    ElMessage.success('已转为草稿，可重新编辑后提交审核')
    await loadAll()
  } catch (e: any) {
    if (e !== 'cancel' && e !== undefined) {
      const msg = e?.response?.data?.message || e?.message || '操作失败'
      ElMessage.error(msg)
    }
  }
}

async function handleDelete(row: Product) {
  try {
    await ElMessageBox.confirm(
      `确定要删除草稿「${row.name}」吗？删除后无法恢复。`,
      '删除确认',
      { type: 'error', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await http.delete(`/merchant/products/${row.id}`)
    ElMessage.success('商品已删除')
    await loadAll()
  } catch (e: any) {
    if (e !== 'cancel' && e !== undefined) {
      const msg = e?.response?.data?.message || e?.message || '删除失败'
      ElMessage.error(msg)
    }
  }
}

function statusText(status: string) {
  return {
    DRAFT: '草稿',
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回',
    RECTIFYING: '整改中',
    OFF_SHELF: '已下架'
  }[status] || status
}

function statusType(status: string) {
  return {
    DRAFT: 'info',
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    RECTIFYING: 'warning',
    OFF_SHELF: 'info'
  }[status] || 'info'
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}

onMounted(loadAll)
</script>

<style scoped>
.merchant-workbench {
  padding: 24px;
}

.workbench-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.workbench-head .eyebrow {
  color: #409eff;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.5px;
  margin: 0 0 4px 0;
  text-transform: uppercase;
}

.workbench-head h1 {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 6px 0;
  color: #303133;
}

.workbench-head .sub-title {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.head-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.metric {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.metric-label {
  color: #909399;
  font-size: 13px;
  display: block;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 26px;
  font-weight: 600;
  color: #303133;
}

.metric.draft .metric-value {
  color: #909399;
}

.metric.pending .metric-value {
  color: #e6a23c;
}

.metric.rectifying .metric-value {
  color: #f56c6c;
}

.metric.approved .metric-value {
  color: #67c23a;
}

.metric.offShelf .metric-value {
  color: #909399;
}

.product-section {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-head h2 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #303133;
}

.product-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  background: #f5f7fa;
}

.muted {
  color: #c0c4cc;
}

.empty-state {
  padding: 40px 20px;
}

.image-preview {
  margin-top: 4px;
}

.preview-label {
  color: #909399;
  font-size: 13px;
  margin: 0 0 8px 0;
}

.preview-img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}
</style>
