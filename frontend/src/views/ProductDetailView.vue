<template>
  <main class="portal">
    <div class="detail-page">
      <div class="back-bar">
        <el-button :icon="ArrowLeft" @click="goBack">返回查询</el-button>
      </div>

      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="10" animated />
      </div>

      <div v-else-if="!product" class="empty-state">
        <el-empty description="未找到该商品信息">
          <el-button type="primary" @click="goBack">返回查询</el-button>
        </el-empty>
      </div>

      <div v-else class="detail-container">
        <section class="product-gallery">
          <img :src="product.imageUrl" :alt="product.name" class="main-image">
        </section>

        <section class="product-info">
          <div class="info-header">
            <h1 class="product-name">{{ product.name }}</h1>
            <div class="status-tags">
              <el-tag :type="product.stock > 0 ? 'success' : 'info'" effect="plain">
                {{ product.stock > 0 ? '有货' : '缺货' }}
              </el-tag>
              <el-tag :type="statusTagType" effect="dark">
                {{ statusText }}
              </el-tag>
            </div>
          </div>

          <div class="price-row">
            <span class="price-label">售价</span>
            <span class="price-value">￥{{ product.price }}</span>
          </div>

          <el-descriptions :column="2" border class="info-descriptions">
            <el-descriptions-item label="所属分类">
              <el-tag type="info" effect="light">{{ product.category }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="商家名称">
              {{ product.merchantName }}
            </el-descriptions-item>
            <el-descriptions-item label="库存数量">
              <span :class="{ 'text-danger': product.stock === 0 }">{{ product.stock }} 件</span>
            </el-descriptions-item>
            <el-descriptions-item label="商品编号">
              #{{ product.id }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="cert-section">
            <h3 class="section-title">
              <el-icon class="title-icon"><CircleCheckFilled /></el-icon>
              商品安全认证
            </h3>
            <el-descriptions :column="1" border class="cert-descriptions">
              <el-descriptions-item label="认证编号">
                <template v-if="product.certificationNo">
                  <span class="cert-no">{{ product.certificationNo }}</span>
                </template>
                <span v-else class="text-muted">暂无认证编号</span>
              </el-descriptions-item>
              <el-descriptions-item label="检测报告">
                <template v-if="product.reportName">
                  <el-icon class="doc-icon"><Document /></el-icon>
                  <span class="report-name">{{ product.reportName }}</span>
                </template>
                <span v-else class="text-muted">暂无检测报告</span>
              </el-descriptions-item>
              <el-descriptions-item label="认证状态">
                <div class="status-detail">
                  <el-tag :type="statusTagType" effect="dark" size="large">
                    {{ statusText }}
                  </el-tag>
                  <span class="audit-remark" v-if="product.auditRemark">
                    备注：{{ product.auditRemark }}
                  </span>
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="action-row">
            <el-button type="primary" size="large" :icon="Warning" @click="openComplaint">
              发起投诉
            </el-button>
          </div>
        </section>
      </div>
    </div>

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
import { ArrowLeft, Document, Warning, CircleCheckFilled } from '@element-plus/icons-vue'
import { http } from '../api/http'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

interface ProductDetail {
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

const product = ref<ProductDetail | null>(null)
const loading = ref(true)
const dialogVisible = ref(false)
const complaint = reactive({ productId: 0, productName: '', reporter: 'user', reason: '' })

const statusText = computed(() => {
  if (!product.value) return ''
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PENDING: '待审核',
    APPROVED: '已认证',
    REJECTED: '已驳回',
    RECTIFYING: '整改中',
    OFF_SHELF: '已下架'
  }
  return map[product.value.status] || product.value.status
})

const statusTagType = computed(() => {
  if (!product.value) return 'info'
  const map: Record<string, string> = {
    DRAFT: 'info',
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    RECTIFYING: 'warning',
    OFF_SHELF: 'info'
  }
  return map[product.value.status] || 'info'
})

async function loadProduct() {
  loading.value = true
  try {
    const id = route.params.id
    const { data } = await http.get(`/public/products/${id}`)
    product.value = data
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/')
}

function openComplaint() {
  if (!product.value) return
  complaint.productId = product.value.id
  complaint.productName = product.value.name
  complaint.reason = ''
  dialogVisible.value = true
}

async function submitComplaint() {
  await http.post('/public/complaints', complaint)
  ElMessage.success('投诉已提交，等待监管处理')
  dialogVisible.value = false
}

onMounted(() => {
  loadProduct()
})
</script>

<style scoped>
.back-bar {
  margin-bottom: 16px;
}

.loading-state {
  background: #fff;
  padding: 32px;
  border-radius: 8px;
  border: 1px solid #e1e6ed;
}

.detail-container {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 28px;
  background: #fff;
  border: 1px solid #e1e6ed;
  border-radius: 8px;
  padding: 28px;
}

.product-gallery .main-image {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  border-radius: 8px;
  background: #dfe6ee;
  display: block;
}

.info-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.product-name {
  margin: 0;
  font-size: 24px;
  line-height: 1.3;
}

.status-tags {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 16px 20px;
  background: #fff7f6;
  border-radius: 8px;
  margin-bottom: 20px;
}

.price-label {
  color: #65758b;
  font-size: 14px;
}

.price-value {
  color: #d7442e;
  font-size: 32px;
  font-weight: 700;
}

.info-descriptions {
  margin-bottom: 24px;
}

.cert-section {
  background: #f4f8ff;
  border: 1px solid #d9ecff;
  border-radius: 8px;
  padding: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 16px;
  color: #1565c0;
}

.title-icon {
  font-size: 18px;
}

.cert-no {
  font-family: "SF Mono", Consolas, monospace;
  font-weight: 600;
  color: #1565c0;
}

.doc-icon {
  color: #1565c0;
  margin-right: 6px;
  vertical-align: -2px;
}

.report-name {
  color: #1565c0;
}

.status-detail {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.audit-remark {
  color: #65758b;
  font-size: 13px;
}

.action-row {
  margin-top: 24px;
}

.text-danger {
  color: #d7442e;
  font-weight: 600;
}

.text-muted {
  color: #909399;
}

@media (max-width: 860px) {
  .detail-container {
    grid-template-columns: 1fr;
  }
}
</style>
