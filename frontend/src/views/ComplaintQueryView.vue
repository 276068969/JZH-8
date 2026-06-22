<template>
  <main class="complaint-query">
    <section class="query-hero">
      <div>
        <p class="eyebrow">Complaint Progress Query</p>
        <h1>投诉进度查询</h1>
        <p>输入您的投诉查询码，实时了解投诉处理进展。</p>
      </div>
    </section>

    <section class="query-panel">
      <el-input
        v-model="queryCode"
        placeholder="请输入投诉查询码，如 CMP7A3B9X"
        size="large"
        clearable
        @keyup.enter="handleQuery"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" size="large" :loading="loading" @click="handleQuery">查询</el-button>
      <el-button size="large" @click="goBack">返回首页</el-button>
    </section>

    <section v-if="queryError" class="error-section">
      <el-alert :title="queryError" type="error" show-icon :closable="false" />
    </section>

    <section v-if="result" class="result-section">
      <div class="result-header">
        <div class="result-title">
          <h2>投诉查询结果</h2>
          <el-tag size="large" :type="complaintType(result.complaint.status)">
            {{ complaintText(result.complaint.status) }}
          </el-tag>
        </div>
        <div class="query-code-display">
          <span class="code-label">查询码</span>
          <span class="code-value">{{ result.complaint.queryCode }}</span>
        </div>
      </div>

      <div class="detail-grid">
        <section class="detail-card">
          <h3 class="card-title">投诉信息</h3>
          <div class="info-list">
            <div class="info-item">
              <span class="info-label">投诉编号</span>
              <span class="info-value">#{{ result.complaint.id }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">投诉人</span>
              <span class="info-value">{{ result.complaint.reporter }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">提交时间</span>
              <span class="info-value">{{ result.complaint.createdAt }}</span>
            </div>
            <div class="info-item full">
              <span class="info-label">投诉原因</span>
              <span class="info-value">{{ result.complaint.reason }}</span>
            </div>
            <div class="info-item" v-if="result.complaint.finalConclusion">
              <span class="info-label">核查结论</span>
              <el-tag :type="conclusionType(result.complaint.finalConclusion)">
                {{ conclusionText(result.complaint.finalConclusion) }}
              </el-tag>
            </div>
          </div>
        </section>

        <section class="detail-card" v-if="result.product">
          <h3 class="card-title">关联商品</h3>
          <div class="product-info">
            <img :src="result.product.imageUrl" :alt="result.product.name" class="product-thumb" />
            <div class="product-meta">
              <div class="product-name">{{ result.product.name }}</div>
              <div class="product-sub">分类：{{ result.product.category }}</div>
              <div class="product-sub">商家：{{ result.product.merchantName }}</div>
              <div class="product-sub">认证编号：{{ result.product.certificationNo || '—' }}</div>
            </div>
          </div>
        </section>
      </div>

      <section class="detail-card timeline-card">
        <h3 class="card-title">处理记录</h3>
        <el-timeline v-if="result.processRecords && result.processRecords.length > 0">
          <el-timeline-item
            v-for="(rec, idx) in result.processRecords"
            :key="rec.id || idx"
            :timestamp="rec.operateTime"
          >
            <div class="process-record">
              <div class="process-header">
                <span class="process-operator">{{ rec.operator }}</span>
                <el-tag size="small" :type="complaintType(rec.toStatus)">
                  {{ complaintText(rec.toStatus) }}
                </el-tag>
                <el-tag v-if="rec.conclusion" size="small" :type="conclusionType(rec.conclusion)" style="margin-left: 6px">
                  {{ conclusionText(rec.conclusion) }}
                </el-tag>
                <el-tag v-if="rec.productAction" type="warning" size="small" style="margin-left: 6px">
                  商品→{{ statusText(rec.productAction) }}
                </el-tag>
              </div>
              <div class="process-remark">{{ rec.remark }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-timeline v-else>
          <el-timeline-item :timestamp="result.complaint.createdAt">
            <div class="process-record">
              <div class="process-header">
                <span class="process-operator">系统</span>
                <el-tag size="small" type="info">待处理</el-tag>
              </div>
              <div class="process-remark">投诉已提交，等待监管人员处理</div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { http } from '../api/http'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const queryCode = ref('')
const loading = ref(false)
const result = ref<any>(null)
const queryError = ref('')

if (route.query.code) {
  queryCode.value = String(route.query.code)
  handleQuery()
}

async function handleQuery() {
  if (!queryCode.value.trim()) {
    ElMessage.warning('请输入投诉查询码')
    return
  }

  loading.value = true
  queryError.value = ''
  result.value = null

  try {
    const { data } = await http.get('/public/complaints/query', {
      params: { code: queryCode.value.trim().toUpperCase() }
    })
    result.value = data
  } catch (err: any) {
    queryError.value = err?.response?.data?.message || '未找到该投诉记录，请检查查询码是否正确'
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/')
}

function complaintText(status: string) {
  return { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已办结', REJECTED: '已驳回' }[status] || status
}

function complaintType(status: string) {
  return { PENDING: 'info', PROCESSING: 'warning', RESOLVED: 'success', REJECTED: 'danger' }[status] || 'info'
}

function conclusionText(conclusion: string) {
  return {
    UNDER_INVESTIGATION: '核查中',
    VERIFIED: '投诉属实',
    UNFOUNDED: '投诉不属实',
    NEEDS_RECTIFICATION: '需商品整改',
    PRODUCT_OFF_SHELF: '需商品下架'
  }[conclusion] || conclusion
}

function conclusionType(conclusion: string) {
  return {
    UNDER_INVESTIGATION: 'info',
    VERIFIED: 'danger',
    UNFOUNDED: 'success',
    NEEDS_RECTIFICATION: 'warning',
    PRODUCT_OFF_SHELF: 'danger'
  }[conclusion] || 'info'
}

function statusText(status: string) {
  return { DRAFT: '草稿', PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', RECTIFYING: '整改中', OFF_SHELF: '已下架' }[status] || status
}
</script>

<style scoped>
.complaint-query {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.query-hero {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  border-radius: 12px;
  padding: 36px 32px;
  color: #fff;
  margin-bottom: 24px;
}

.query-hero .eyebrow {
  font-size: 12px;
  letter-spacing: 2px;
  opacity: 0.8;
  margin: 0 0 8px 0;
}

.query-hero h1 {
  font-size: 28px;
  margin: 0 0 8px 0;
}

.query-hero p {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}

.query-panel {
  display: flex;
  gap: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.query-panel .el-input {
  flex: 1;
}

.error-section {
  margin-bottom: 20px;
}

.result-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-header {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.result-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-title h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.query-code-display {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.code-label {
  font-size: 12px;
  color: #909399;
}

.code-value {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
  font-family: 'Courier New', monospace;
  letter-spacing: 1px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.detail-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
  padding-left: 10px;
  border-left: 3px solid #409eff;
}

.info-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item.full {
  grid-column: 1 / -1;
}

.info-label {
  font-size: 12px;
  color: #909399;
}

.info-value {
  font-size: 14px;
  color: #303133;
}

.product-info {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.product-thumb {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  flex-shrink: 0;
}

.product-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.product-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.product-sub {
  font-size: 13px;
  color: #606266;
}

.timeline-card {
  grid-column: 1 / -1;
}

.process-record {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.process-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.process-operator {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.process-remark {
  font-size: 13px;
  color: #606266;
}

@media (max-width: 640px) {
  .query-panel {
    flex-direction: column;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .result-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .query-code-display {
    align-items: flex-start;
  }
}
</style>
