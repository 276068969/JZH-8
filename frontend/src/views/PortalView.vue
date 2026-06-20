<template>
  <main class="portal">
    <section class="hero-band">
      <div>
        <p class="eyebrow">Toy Safety Supervision</p>
        <h1>玩具售卖全链路监管</h1>
        <p>面向消费者查询、商家合规上架、监管审核与投诉处置的一体化平台。</p>
      </div>
      <el-button type="primary" size="large" @click="goToBackend">{{ auth.token ? '进入后台' : '登录' }}</el-button>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <el-input v-model="keyword" placeholder="搜索商品或商家" clearable class="filter-item w-280" :prefix-icon="Search" />
        <el-select v-model="category" placeholder="全部分类" clearable class="filter-item w-180">
          <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="merchantName" placeholder="商家名称" clearable class="filter-item w-200" filterable>
          <el-option v-for="m in merchantList" :key="m.id" :label="m.name" :value="m.name" />
        </el-select>
      </div>
      <div class="filter-row">
        <div class="filter-item price-range">
          <el-input-number v-model="minPrice" :min="0" :precision="2" :step="10" placeholder="最低价" controls-position="right" />
          <span class="price-sep">—</span>
          <el-input-number v-model="maxPrice" :min="0" :precision="2" :step="10" placeholder="最高价" controls-position="right" />
          <span class="price-unit">元</span>
        </div>
        <el-select v-model="stockStatus" placeholder="库存状态" clearable class="filter-item w-160">
          <el-option label="有货" value="in_stock" />
          <el-option label="缺货" value="out_of_stock" />
        </el-select>
        <el-input v-model="certificationNo" placeholder="认证编号（如 CCC-2026）" clearable class="filter-item w-260" />
        <el-button type="primary" :icon="Search" @click="loadProducts">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
      </div>
    </section>

    <section class="active-filters" v-if="activeFilters.length > 0">
      <span class="filters-label">当前筛选：</span>
      <el-tag
        v-for="f in activeFilters"
        :key="f.key"
        closable
        type="info"
        effect="light"
        @close="removeFilter(f.key)"
      >
        {{ f.label }}：{{ f.value }}
      </el-tag>
    </section>

    <section class="result-summary">
      <span>共找到 <strong class="count">{{ products.length }}</strong> 件符合条件的玩具商品</span>
    </section>

    <section v-if="products.length > 0" class="product-grid">
      <article v-for="item in products" :key="item.id" class="product-card">
        <img :src="item.imageUrl" :alt="item.name">
        <div class="product-body">
          <div class="row-between">
            <h2>{{ item.name }}</h2>
            <div class="tags">
              <el-tag :type="item.stock > 0 ? 'success' : 'info'" size="small" effect="plain">
                {{ item.stock > 0 ? '有货' : '缺货' }}
              </el-tag>
              <el-tag type="success" size="small">已认证</el-tag>
            </div>
          </div>
          <p>{{ item.category }} · {{ item.merchantName }}</p>
          <p class="cert">认证编号：{{ item.certificationNo }}</p>
          <p class="stock">库存：{{ item.stock }} 件</p>
          <div class="row-between">
            <strong>￥{{ item.price }}</strong>
            <el-button size="small" @click="openComplaint(item)">投诉</el-button>
          </div>
        </div>
      </article>
    </section>

    <section v-else class="empty-state">
      <el-empty description="未找到符合条件的商品，请尝试调整筛选条件">
        <el-button type="primary" @click="resetFilters">重置筛选</el-button>
      </el-empty>
    </section>

    <section class="notice-list">
      <h2>监管公告</h2>
      <el-timeline>
        <el-timeline-item v-for="notice in notices" :key="notice.id" :timestamp="notice.publishedAt">
          <strong>{{ notice.title }}</strong>
          <p>{{ notice.content }}</p>
        </el-timeline-item>
      </el-timeline>
    </section>

    <el-dialog v-model="dialogVisible" title="提交投诉举报" width="520px">
      <el-form label-position="top">
        <el-form-item label="商品">
          <el-input v-model="complaint.productName" disabled />
        </el-form-item>
        <el-form-item label="投诉人">
          <el-input v-model="complaint.reporter" />
        </el-form-item>
        <el-form-item label="投诉原因">
          <el-input v-model="complaint.reason" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComplaint">提交</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { http } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

interface Product {
  id: number
  name: string
  category: string
  merchantName: string
  price: string
  stock: number
  certificationNo: string
  imageUrl: string
}

interface Merchant {
  id: number
  name: string
  licenseNo: string
}

interface ActiveFilter {
  key: string
  label: string
  value: string
}

const products = ref<Product[]>([])
const notices = ref<any[]>([])
const merchantList = ref<Merchant[]>([])

const keyword = ref('')
const category = ref('')
const merchantName = ref('')
const minPrice = ref<number | null>(null)
const maxPrice = ref<number | null>(null)
const stockStatus = ref('')
const certificationNo = ref('')

const categories = ['益智玩具', '运动玩具', '毛绒玩具', '手工玩具']
const dialogVisible = ref(false)
const complaint = reactive({ productId: 0, productName: '', reporter: 'user', reason: '' })

const activeFilters = computed<ActiveFilter[]>(() => {
  const list: ActiveFilter[] = []
  if (keyword.value) list.push({ key: 'keyword', label: '关键词', value: keyword.value })
  if (category.value) list.push({ key: 'category', label: '分类', value: category.value })
  if (merchantName.value) list.push({ key: 'merchantName', label: '商家', value: merchantName.value })
  if (minPrice.value != null || maxPrice.value != null) {
    const min = minPrice.value != null ? `￥${minPrice.value}` : '不限'
    const max = maxPrice.value != null ? `￥${maxPrice.value}` : '不限'
    list.push({ key: 'priceRange', label: '价格区间', value: `${min} ~ ${max}` })
  }
  if (stockStatus.value) {
    list.push({ key: 'stockStatus', label: '库存', value: stockStatus.value === 'in_stock' ? '有货' : '缺货' })
  }
  if (certificationNo.value) list.push({ key: 'certificationNo', label: '认证编号', value: certificationNo.value })
  return list
})

async function loadMerchants() {
  const { data } = await http.get('/public/merchants')
  merchantList.value = data
}

async function loadProducts() {
  const params: Record<string, any> = {
    keyword: keyword.value,
    category: category.value,
  }
  if (merchantName.value) params.merchantName = merchantName.value
  if (minPrice.value != null) params.minPrice = minPrice.value
  if (maxPrice.value != null) params.maxPrice = maxPrice.value
  if (stockStatus.value) params.stockStatus = stockStatus.value
  if (certificationNo.value) params.certificationNo = certificationNo.value

  const { data } = await http.get('/public/products', { params })
  products.value = data
}

function removeFilter(key: string) {
  switch (key) {
    case 'keyword': keyword.value = ''; break
    case 'category': category.value = ''; break
    case 'merchantName': merchantName.value = ''; break
    case 'priceRange': minPrice.value = null; maxPrice.value = null; break
    case 'stockStatus': stockStatus.value = ''; break
    case 'certificationNo': certificationNo.value = ''; break
  }
  loadProducts()
}

function resetFilters() {
  keyword.value = ''
  category.value = ''
  merchantName.value = ''
  minPrice.value = null
  maxPrice.value = null
  stockStatus.value = ''
  certificationNo.value = ''
  loadProducts()
}

function openComplaint(item: Product) {
  complaint.productId = item.id
  complaint.productName = item.name
  complaint.reason = ''
  dialogVisible.value = true
}

async function submitComplaint() {
  await http.post('/public/complaints', complaint)
  ElMessage.success('投诉已提交，等待监管处理')
  dialogVisible.value = false
}

function goToBackend() {
  if (!auth.token) {
    router.push('/login')
    return
  }
  if (auth.role === 'MERCHANT') {
    router.push('/merchant')
  } else if (auth.role === 'ADMIN' || auth.role === 'REGULATOR') {
    router.push('/admin')
  } else {
    router.push('/login')
  }
}

onMounted(async () => {
  await loadMerchants()
  await loadProducts()
  const { data } = await http.get('/public/notices')
  notices.value = data
})
</script>

<style scoped>
.filter-panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-row + .filter-row {
  margin-top: 14px;
}

.filter-item {
  flex-shrink: 0;
}

.w-160 { width: 160px; }
.w-180 { width: 180px; }
.w-200 { width: 200px; }
.w-260 { width: 260px; }
.w-280 { width: 280px; }

.price-range {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 4px;
}

.price-range :deep(.el-input-number) {
  width: 130px;
}

.price-sep {
  color: #909399;
  font-weight: 500;
}

.price-unit {
  color: #606266;
  font-size: 13px;
  margin-left: 2px;
}

.active-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  background: #f4f8ff;
  border: 1px solid #d9ecff;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 14px;
}

.filters-label {
  color: #409eff;
  font-size: 13px;
  font-weight: 500;
}

.result-summary {
  padding: 10px 4px 16px 4px;
  color: #606266;
  font-size: 14px;
}

.result-summary .count {
  color: #409eff;
  font-size: 18px;
  margin: 0 4px;
}

.empty-state {
  background: #fff;
  border: 1px dashed #dcdfe6;
  border-radius: 10px;
  padding: 60px 20px;
  margin-bottom: 20px;
}

.product-card .tags {
  display: flex;
  gap: 6px;
}

.product-card .stock {
  color: #909399;
  font-size: 12px;
  margin: 4px 0;
}
</style>
