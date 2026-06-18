<template>
  <main class="portal">
    <section class="hero-band">
      <div>
        <p class="eyebrow">Toy Safety Supervision</p>
        <h1>玩具售卖全链路监管</h1>
        <p>面向消费者查询、商家合规上架、监管审核与投诉处置的一体化平台。</p>
      </div>
      <el-button type="primary" size="large" @click="$router.push('/login')">进入后台</el-button>
    </section>

    <section class="toolbar">
      <el-input v-model="keyword" placeholder="搜索商品或商家" clearable />
      <el-select v-model="category" placeholder="全部分类" clearable>
        <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
      </el-select>
      <el-button :icon="Search" type="primary" @click="loadProducts">查询</el-button>
    </section>

    <section class="product-grid">
      <article v-for="item in products" :key="item.id" class="product-card">
        <img :src="item.imageUrl" :alt="item.name">
        <div class="product-body">
          <div class="row-between">
            <h2>{{ item.name }}</h2>
            <el-tag type="success">已认证</el-tag>
          </div>
          <p>{{ item.category }} · {{ item.merchantName }}</p>
          <p class="cert">认证编号：{{ item.certificationNo }}</p>
          <div class="row-between">
            <strong>￥{{ item.price }}</strong>
            <el-button size="small" @click="openComplaint(item)">投诉</el-button>
          </div>
        </div>
      </article>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { http } from '../api/http'

interface Product {
  id: number
  name: string
  category: string
  merchantName: string
  price: string
  certificationNo: string
  imageUrl: string
}

const products = ref<Product[]>([])
const notices = ref<any[]>([])
const keyword = ref('')
const category = ref('')
const categories = ['益智玩具', '运动玩具', '毛绒玩具', '手工玩具']
const dialogVisible = ref(false)
const complaint = reactive({ productId: 0, productName: '', reporter: 'user', reason: '' })

async function loadProducts() {
  const { data } = await http.get('/public/products', { params: { keyword: keyword.value, category: category.value } })
  products.value = data
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

onMounted(async () => {
  await loadProducts()
  const { data } = await http.get('/public/notices')
  notices.value = data
})
</script>
