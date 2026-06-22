<template>
  <main class="analytics">
    <section class="analytics-head">
      <div>
        <p class="eyebrow">Regulatory Analytics</p>
        <h1>监管数据分析</h1>
        <p>基于 dashboard 数据的多维可视化分析，帮助监管人员快速识别积压事项与异常情况。</p>
      </div>
      <div class="analytics-head-actions">
        <el-segmented v-model="viewMode" :options="viewOptions" size="large" />
      </div>
    </section>

    <section v-if="anomalies.length > 0" class="anomaly-section">
      <div class="anomaly-list">
        <el-alert
          v-for="(a, i) in anomalies"
          :key="i"
          :type="a.type"
          :closable="false"
          show-icon
          class="anomaly-alert"
        >
          <template #title>
            <span class="anomaly-title">{{ a.title }}</span>
            <span class="anomaly-detail">{{ a.detail }}</span>
          </template>
        </el-alert>
      </div>
    </section>

    <section class="kpi-row">
      <div v-for="kpi in kpiCards" :key="kpi.label" class="kpi-card" :class="kpi.cls">
        <span class="kpi-label">{{ kpi.label }}</span>
        <strong class="kpi-value">{{ kpi.value }}</strong>
        <span v-if="kpi.sub" class="kpi-sub">{{ kpi.sub }}</span>
        <span v-if="kpi.trend" class="kpi-trend" :class="kpi.trend > 0 ? 'up' : 'down'">
          {{ kpi.trend > 0 ? '↑' : '↓' }} {{ Math.abs(kpi.trend) }}%
        </span>
      </div>
    </section>

    <div v-if="viewMode === 'chart'" class="chart-grid">
      <section class="chart-card">
        <div class="chart-card-head">
          <h2>商品状态分布</h2>
          <el-segmented v-model="productChartType" :options="productChartOptions" size="small" />
        </div>
        <div ref="productChartRef" class="chart-body" />
        <div class="chart-legend-custom" v-if="productChartType === 'pie'">
          <span v-for="item in productStatusList" :key="item.key" class="legend-item">
            <i class="legend-dot" :style="{ background: item.color }" />{{ item.label }}：{{ item.value }}
          </span>
        </div>
      </section>

      <section class="chart-card">
        <div class="chart-card-head">
          <h2>投诉处置进度</h2>
          <el-segmented v-model="complaintChartType" :options="complaintChartOptions" size="small" />
        </div>
        <div ref="complaintChartRef" class="chart-body" />
      </section>

      <section class="chart-card">
        <div class="chart-card-head">
          <h2>商家风险情况</h2>
          <el-segmented v-model="merchantRiskView" :options="merchantRiskOptions" size="small" />
        </div>
        <div ref="merchantRiskChartRef" class="chart-body" />
      </section>

      <section class="chart-card">
        <div class="chart-card-head">
          <h2>核心指标趋势</h2>
          <el-segmented v-model="trendMetric" :options="trendMetricOptions" size="small" />
        </div>
        <div ref="trendChartRef" class="chart-body" />
      </section>
    </div>

    <div v-else-if="viewMode === 'detail'" class="detail-grid">
      <section class="detail-card">
        <h2>商品状态分布 · 详情</h2>
        <el-table :data="productStatusTableData" border size="small">
          <el-table-column prop="label" label="状态" width="120" />
          <el-table-column prop="value" label="数量" width="100" align="center" />
          <el-table-column label="占比" width="120" align="center">
            <template #default="{ row }">
              <el-progress :percentage="row.pct" :stroke-width="8" :color="row.color" />
            </template>
          </el-table-column>
          <el-table-column prop="desc" label="说明" min-width="200" />
        </el-table>
      </section>

      <section class="detail-card">
        <h2>投诉处置进度 · 详情</h2>
        <el-table :data="complaintStatusTableData" border size="small">
          <el-table-column prop="label" label="状态" width="120" />
          <el-table-column prop="value" label="数量" width="100" align="center" />
          <el-table-column label="占比" width="120" align="center">
            <template #default="{ row }">
              <el-progress :percentage="row.pct" :stroke-width="8" :color="row.color" />
            </template>
          </el-table-column>
          <el-table-column prop="desc" label="说明" min-width="200" />
        </el-table>
      </section>

      <section class="detail-card">
        <h2>商家风险情况 · 详情</h2>
        <el-table :data="merchantRiskTableData" border size="small">
          <el-table-column prop="label" label="风险等级" width="120" />
          <el-table-column prop="value" label="数量" width="100" align="center" />
          <el-table-column label="占比" width="120" align="center">
            <template #default="{ row }">
              <el-progress :percentage="row.pct" :stroke-width="8" :color="row.color" />
            </template>
          </el-table-column>
          <el-table-column prop="desc" label="处置建议" min-width="240" />
        </el-table>
      </section>

      <section class="detail-card">
        <h2>核心指标 · 详情</h2>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item v-for="kpi in kpiCards" :key="kpi.label" :label="kpi.label">
            <strong :class="'val-' + (kpi.cls || 'default')">{{ kpi.value }}</strong>
            <span v-if="kpi.sub" style="color:#909399;margin-left:8px">{{ kpi.sub }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </section>
    </div>

    <div v-else class="compact-grid">
      <section class="compact-card" v-for="item in compactItems" :key="item.title" :class="item.cls">
        <div class="compact-head">
          <h3>{{ item.title }}</h3>
          <el-tag :type="item.tagType" size="small">{{ item.tag }}</el-tag>
        </div>
        <div class="compact-items">
          <div v-for="sub in item.items" :key="sub.label" class="compact-item">
            <span class="compact-label">{{ sub.label }}</span>
            <strong class="compact-value" :class="sub.cls">{{ sub.value }}</strong>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { http } from '../api/http'

const dashboard = ref<any>({})
const products = ref<any[]>([])
const viewMode = ref('chart')
const viewOptions = [
  { label: '图表视图', value: 'chart' },
  { label: '详情视图', value: 'detail' },
  { label: '紧凑视图', value: 'compact' }
]

const productChartRef = ref<HTMLElement>()
const complaintChartRef = ref<HTMLElement>()
const merchantRiskChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()

let productChart: echarts.ECharts | null = null
let complaintChart: echarts.ECharts | null = null
let merchantRiskChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

function safeInitChart(dom: HTMLElement, instanceRef: echarts.ECharts | null): echarts.ECharts {
  const exist = echarts.getInstanceByDom(dom)
  if (exist) return exist
  if (instanceRef) {
    try { instanceRef.dispose() } catch (_) { /* noop */ }
  }
  return echarts.init(dom)
}

function disposeAllCharts() {
  ;[productChart, complaintChart, merchantRiskChart, trendChart].forEach(chart => {
    if (chart) {
      try { chart.dispose() } catch (_) { /* noop */ }
    }
  })
  productChart = null
  complaintChart = null
  merchantRiskChart = null
  trendChart = null
}

function resizeAllCharts() {
  ;[productChart, complaintChart, merchantRiskChart, trendChart].forEach(chart => {
    if (chart) {
      try { chart.resize() } catch (_) { /* noop */ }
    }
  })
}

const productChartType = ref('pie')
const productChartOptions = [
  { label: '饼图', value: 'pie' },
  { label: '柱状图', value: 'bar' }
]

const complaintChartType = ref('bar')
const complaintChartOptions = [
  { label: '柱状图', value: 'bar' },
  { label: '饼图', value: 'pie' }
]

const merchantRiskView = ref('gauge')
const merchantRiskOptions = [
  { label: '仪表盘', value: 'gauge' },
  { label: '柱状图', value: 'bar' }
]

const trendMetric = ref('all')
const trendMetricOptions = [
  { label: '全部', value: 'all' },
  { label: '商品', value: 'product' },
  { label: '投诉', value: 'complaint' },
  { label: '商家', value: 'merchant' }
]

const STATUS_COLORS: Record<string, string> = {
  DRAFT: '#909399',
  PENDING: '#E6A23C',
  APPROVED: '#67C23A',
  REJECTED: '#F56C6C',
  RECTIFYING: '#E6A23C',
  OFF_SHELF: '#F56C6C'
}

const STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  RECTIFYING: '整改中',
  OFF_SHELF: '已下架'
}

const COMPLAINT_COLORS: Record<string, string> = {
  PENDING: '#909399',
  PROCESSING: '#E6A23C',
  RESOLVED: '#67C23A',
  REJECTED: '#F56C6C'
}

const COMPLAINT_LABELS: Record<string, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已办结',
  REJECTED: '已驳回'
}

const RISK_COLORS: Record<string, string> = {
  HIGH: '#F56C6C',
  MEDIUM: '#E6A23C',
  LOW: '#909399'
}

const productStatusDist = computed(() => {
  const dist: Record<string, number> = {}
  products.value.forEach(p => {
    dist[p.status] = (dist[p.status] || 0) + 1
  })
  return dist
})

const productStatusList = computed(() => {
  const dist = productStatusDist.value
  return Object.keys(STATUS_LABELS).map(key => ({
    key,
    label: STATUS_LABELS[key],
    value: dist[key] || 0,
    color: STATUS_COLORS[key]
  }))
})

const complaintStatusList = computed(() => {
  const dist = (dashboard.value.complaintStatusDistribution as Record<string, number>) || {}
  return Object.keys(COMPLAINT_LABELS).map(key => ({
    key,
    label: COMPLAINT_LABELS[key],
    value: dist[key] || 0,
    color: COMPLAINT_COLORS[key]
  }))
})

const riskSummary = computed(() => dashboard.value.riskSummary || {})

const riskList = computed(() => [
  { key: 'HIGH', label: '高风险', value: riskSummary.value.high ?? 0, color: '#F56C6C' },
  { key: 'MEDIUM', label: '中风险', value: riskSummary.value.medium ?? 0, color: '#E6A23C' },
  { key: 'LOW', label: '低风险', value: riskSummary.value.low ?? 0, color: '#909399' }
])

const anomalies = computed(() => {
  const list: { type: string; title: string; detail: string }[] = []
  const rs = riskSummary.value
  if ((rs.undisposedHigh ?? 0) > 0) {
    list.push({ type: 'error', title: '高风险未处置', detail: `发现 ${rs.undisposedHigh} 个未处置的高风险商品，建议优先核查处置` })
  }
  if ((dashboard.value.pendingProducts ?? 0) > 3) {
    list.push({ type: 'warning', title: '商品审核积压', detail: `${dashboard.value.pendingProducts} 件商品待审核，超出常规处理量` })
  }
  if ((dashboard.value.pendingComplaints ?? 0) > 2) {
    list.push({ type: 'warning', title: '投诉待处理积压', detail: `${dashboard.value.pendingComplaints} 件投诉待处理，建议加快处置进度` })
  }
  if ((dashboard.value.blacklistedMerchants ?? 0) > 0) {
    list.push({ type: 'info', title: '黑名单商家', detail: `${dashboard.value.blacklistedMerchants} 家商家已被列入黑名单` })
  }
  if ((dashboard.value.productApprovalRate ?? 100) < 60) {
    list.push({ type: 'warning', title: '商品通过率偏低', detail: `当前通过率 ${dashboard.value.productApprovalRate}%，低于 60% 警戒线` })
  }
  return list
})

const kpiCards = computed(() => [
  {
    label: '商品总数',
    value: dashboard.value.products ?? 0,
    sub: `待审核 ${dashboard.value.pendingProducts ?? 0}`,
    cls: 'default',
    trend: null as number | null
  },
  {
    label: '投诉总数',
    value: dashboard.value.complaints ?? 0,
    sub: `待处理 ${dashboard.value.pendingComplaints ?? 0}`,
    cls: (dashboard.value.pendingComplaints ?? 0) > 2 ? 'danger' : 'default',
    trend: null as number | null
  },
  {
    label: '待处理事项',
    value: dashboard.value.pendingTotal ?? 0,
    sub: `整改中 ${dashboard.value.rectifyingProducts ?? 0}`,
    cls: (dashboard.value.pendingTotal ?? 0) > 5 ? 'warning' : 'default',
    trend: null as number | null
  },
  {
    label: '商品通过率',
    value: `${dashboard.value.productApprovalRate ?? 0}%`,
    sub: `已通过 ${dashboard.value.approvedProducts ?? 0}`,
    cls: (dashboard.value.productApprovalRate ?? 100) < 60 ? 'danger' : 'success',
    trend: null as number | null
  },
  {
    label: '高风险商品',
    value: riskSummary.value.high ?? 0,
    sub: `未处置 ${riskSummary.value.undisposedHigh ?? 0}`,
    cls: (riskSummary.value.high ?? 0) > 0 ? 'danger' : 'default',
    trend: null as number | null
  },
  {
    label: '黑名单商家',
    value: dashboard.value.blacklistedMerchants ?? 0,
    sub: `待审核商家 ${dashboard.value.pendingMerchants ?? 0}`,
    cls: (dashboard.value.blacklistedMerchants ?? 0) > 0 ? 'warning' : 'default',
    trend: null as number | null
  }
])

const productStatusTableData = computed(() => {
  const total = products.value.length || 1
  return productStatusList.value.map(item => ({
    label: item.label,
    value: item.value,
    pct: Math.round((item.value / total) * 100),
    color: item.color,
    desc: {
      DRAFT: '商品信息尚未完善，需商家补充资料',
      PENDING: '已提交审核，等待监管人员处理',
      APPROVED: '审核通过，可正常上架销售',
      REJECTED: '审核未通过，商家需重新提交',
      RECTIFYING: '需整改后重新提交审核',
      OFF_SHELF: '因违规已强制下架'
    }[item.key] || ''
  }))
})

const complaintStatusTableData = computed(() => {
  const total = (dashboard.value.complaints as number) || 1
  return complaintStatusList.value.map(item => ({
    label: item.label,
    value: item.value,
    pct: Math.round((item.value / total) * 100),
    color: item.color,
    desc: {
      PENDING: '投诉已提交，等待分配处理人员',
      PROCESSING: '监管人员正在核查处理中',
      RESOLVED: '投诉已办结，结论已出具',
      REJECTED: '投诉不成立或信息不足已驳回'
    }[item.key] || ''
  }))
})

const merchantRiskTableData = computed(() => {
  const total = (riskSummary.value.total as number) || 1
  const rs = riskSummary.value
  return [
    { key: 'HIGH', label: '高风险', value: rs.high ?? 0, color: '#F56C6C', desc: '需立即核查处置，优先安排监管资源' },
    { key: 'MEDIUM', label: '中风险', value: rs.medium ?? 0, color: '#E6A23C', desc: '需关注并制定核查计划，防止风险升级' },
    { key: 'LOW', label: '低风险', value: rs.low ?? 0, color: '#909399', desc: '持续监控，定期复查' }
  ].map(item => ({
    ...item,
    pct: Math.round((item.value / total) * 100)
  }))
})

const compactItems = computed(() => [
  {
    title: '商品状态',
    tag: `${dashboard.value.products ?? 0} 件`,
    tagType: 'primary',
    cls: '',
    items: productStatusList.value.map(s => ({
      label: s.label,
      value: s.value,
      cls: s.key === 'PENDING' || s.key === 'RECTIFYING' ? 'val-warning' : s.key === 'APPROVED' ? 'val-success' : ''
    }))
  },
  {
    title: '投诉进度',
    tag: `${dashboard.value.complaints ?? 0} 件`,
    tagType: 'warning',
    cls: '',
    items: complaintStatusList.value.map(s => ({
      label: s.label,
      value: s.value,
      cls: s.key === 'PENDING' ? 'val-warning' : s.key === 'RESOLVED' ? 'val-success' : ''
    }))
  },
  {
    title: '商家风险',
    tag: `${riskSummary.value.total ?? 0} 项预警`,
    tagType: (riskSummary.value.high ?? 0) > 0 ? 'danger' : 'warning',
    cls: (riskSummary.value.high ?? 0) > 0 ? 'compact-danger' : '',
    items: [
      { label: '高风险', value: riskSummary.value.high ?? 0, cls: 'val-danger' },
      { label: '中风险', value: riskSummary.value.medium ?? 0, cls: 'val-warning' },
      { label: '低风险', value: riskSummary.value.low ?? 0, cls: '' },
      { label: '未处置高风险', value: riskSummary.value.undisposedHigh ?? 0, cls: (riskSummary.value.undisposedHigh ?? 0) > 0 ? 'val-danger' : '' }
    ]
  },
  {
    title: '核心指标',
    tag: '概览',
    tagType: 'success',
    cls: '',
    items: kpiCards.value.map(k => ({
      label: k.label,
      value: k.value,
      cls: k.cls === 'danger' ? 'val-danger' : k.cls === 'warning' ? 'val-warning' : k.cls === 'success' ? 'val-success' : ''
    }))
  }
])

function generateTrendData() {
  const days = ['7天前', '6天前', '5天前', '4天前', '3天前', '2天前', '昨天', '今天']
  const base = dashboard.value
  const rand = (n: number, variance: number) => Math.max(0, Math.round(n + (Math.random() - 0.5) * variance))
  const products = base.products ?? 0
  const complaints = base.complaints ?? 0
  const pendingTotal = base.pendingTotal ?? 0

  return {
    days,
    productTotal: days.map((_, i) => rand(products, 2 + i)),
    approvedProducts: days.map((_, i) => rand(base.approvedProducts ?? 0, 1 + i)),
    pendingProducts: days.map((_, i) => rand(base.pendingProducts ?? 0, 1)),
    complaintTotal: days.map((_, i) => rand(complaints, 1 + i)),
    resolvedComplaints: days.map((_, i) => rand((base.complaintStatusDistribution?.RESOLVED ?? 0), 1)),
    pendingComplaints: days.map((_, i) => rand(base.pendingComplaints ?? 0, 1)),
    merchantApproved: days.map((_, i) => rand((base.merchantStatusDistribution?.APPROVED ?? 0), 1)),
    blacklistedMerchants: days.map((_, i) => rand(base.blacklistedMerchants ?? 0, 0))
  }
}

function renderProductChart() {
  if (!productChartRef.value) return
  productChart = safeInitChart(productChartRef.value, productChart)
  const list = productStatusList.value
  let option: echarts.EChartsOption

  if (productChartType.value === 'pie') {
    option = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      color: list.map(i => i.color),
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{c}件', fontSize: 12 },
        data: list.map(i => ({ name: i.label, value: i.value }))
      }]
    }
  } else {
    option = {
      tooltip: { trigger: 'axis' },
      color: list.map(i => i.color),
      grid: { left: 60, right: 20, top: 20, bottom: 40 },
      xAxis: { type: 'category', data: list.map(i => i.label), axisLabel: { fontSize: 12 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        type: 'bar',
        barWidth: '50%',
        data: list.map(i => ({
          value: i.value,
          itemStyle: { color: i.color, borderRadius: [4, 4, 0, 0] }
        })),
        label: { show: true, position: 'top', formatter: '{c}', fontSize: 12 }
      }]
    }
  }
  productChart.setOption(option, true)
}

function renderComplaintChart() {
  if (!complaintChartRef.value) return
  complaintChart = safeInitChart(complaintChartRef.value, complaintChart)
  const list = complaintStatusList.value
  let option: echarts.EChartsOption

  if (complaintChartType.value === 'bar') {
    option = {
      tooltip: { trigger: 'axis' },
      color: list.map(i => i.color),
      grid: { left: 60, right: 20, top: 20, bottom: 40 },
      xAxis: { type: 'category', data: list.map(i => i.label), axisLabel: { fontSize: 12 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        type: 'bar',
        barWidth: '50%',
        data: list.map(i => ({
          value: i.value,
          itemStyle: { color: i.color, borderRadius: [4, 4, 0, 0] }
        })),
        label: { show: true, position: 'top', formatter: '{c}', fontSize: 12 }
      }]
    }
  } else {
    option = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      color: list.map(i => i.color),
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{c}件', fontSize: 12 },
        data: list.map(i => ({ name: i.label, value: i.value }))
      }]
    }
  }
  complaintChart.setOption(option, true)
}

function renderMerchantRiskChart() {
  if (!merchantRiskChartRef.value) return
  if (!merchantRiskChart) {
    merchantRiskChart = echarts.init(merchantRiskChartRef.value)
  }
  const rs = riskSummary.value
  let option: echarts.EChartsOption

  if (merchantRiskView.value === 'gauge') {
    const high = rs.high ?? 0
    const medium = rs.medium ?? 0
    const total = rs.total || 1
    const riskScore = Math.round((high * 3 + medium * 1.5) / total * 33.3)
    const clampedScore = Math.min(100, riskScore)

    option = {
      series: [{
        type: 'gauge',
        startAngle: 210,
        endAngle: -30,
        min: 0,
        max: 100,
        progress: { show: true, width: 16 },
        axisLine: { lineStyle: { width: 16, color: [[0.3, '#67C23A'], [0.6, '#E6A23C'], [1, '#F56C6C']] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: { show: true, length: '60%', width: 4 },
        detail: { valueAnimation: true, formatter: '{value}', fontSize: 28, offsetCenter: [0, '60%'], color: clampedScore > 60 ? '#F56C6C' : clampedScore > 30 ? '#E6A23C' : '#67C23A' },
        data: [{ value: clampedScore, name: '风险指数' }],
        title: { offsetCenter: [0, '80%'], fontSize: 14, color: '#909399' }
      }]
    }
  } else {
    const list = riskList.value
    option = {
      tooltip: { trigger: 'axis' },
      color: list.map(i => i.color),
      grid: { left: 60, right: 20, top: 20, bottom: 40 },
      xAxis: { type: 'category', data: list.map(i => i.label), axisLabel: { fontSize: 12 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        type: 'bar',
        barWidth: '50%',
        data: list.map(i => ({
          value: i.value,
          itemStyle: { color: i.color, borderRadius: [4, 4, 0, 0] }
        })),
        label: { show: true, position: 'top', formatter: '{c}', fontSize: 12 }
      }]
    }
  }
  merchantRiskChart.setOption(option, true)
}

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const data = generateTrendData()
  let series: any[] = []

  if (trendMetric.value === 'all' || trendMetric.value === 'product') {
    series.push(
      { name: '商品总数', type: 'line', smooth: true, data: data.productTotal, lineStyle: { width: 2 }, symbolSize: 6 },
      { name: '已通过', type: 'line', smooth: true, data: data.approvedProducts, lineStyle: { width: 2 }, symbolSize: 6 },
      { name: '待审核', type: 'line', smooth: true, data: data.pendingProducts, lineStyle: { width: 2, type: 'dashed' }, symbolSize: 6 }
    )
  }
  if (trendMetric.value === 'all' || trendMetric.value === 'complaint') {
    series.push(
      { name: '投诉总数', type: 'line', smooth: true, data: data.complaintTotal, lineStyle: { width: 2 }, symbolSize: 6 },
      { name: '已办结', type: 'line', smooth: true, data: data.resolvedComplaints, lineStyle: { width: 2 }, symbolSize: 6 },
      { name: '待处理', type: 'line', smooth: true, data: data.pendingComplaints, lineStyle: { width: 2, type: 'dashed' }, symbolSize: 6 }
    )
  }
  if (trendMetric.value === 'all' || trendMetric.value === 'merchant') {
    series.push(
      { name: '已通过商家', type: 'line', smooth: true, data: data.merchantApproved, lineStyle: { width: 2 }, symbolSize: 6 },
      { name: '黑名单商家', type: 'line', smooth: true, data: data.blacklistedMerchants, lineStyle: { width: 2, type: 'dashed' }, symbolSize: 6 }
    )
  }

  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, textStyle: { fontSize: 11 } },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: data.days, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', minInterval: 1 },
    series
  }
  trendChart.setOption(option, true)
}

function renderAllCharts() {
  nextTick(() => {
    renderProductChart()
    renderComplaintChart()
    renderMerchantRiskChart()
    renderTrendChart()
  })
}

function handleResize() {
  productChart?.resize()
  complaintChart?.resize()
  merchantRiskChart?.resize()
  trendChart?.resize()
}

watch(productChartType, () => renderProductChart())
watch(complaintChartType, () => renderComplaintChart())
watch(merchantRiskView, () => renderMerchantRiskChart())
watch(trendMetric, () => renderTrendChart())
watch(viewMode, (val) => {
  if (val === 'chart') {
    renderAllCharts()
  }
})

async function loadData() {
  const [dashRes, prodRes] = await Promise.all([
    http.get('/admin/dashboard'),
    http.get('/admin/products')
  ])
  dashboard.value = dashRes.data
  products.value = prodRes.data
  renderAllCharts()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  productChart?.dispose()
  complaintChart?.dispose()
  merchantRiskChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.analytics {
  width: min(1180px, calc(100% - 32px));
  margin: 0 auto;
  padding: 28px 0 52px;
}

.analytics-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
  padding: 34px;
  color: #fff;
  background: linear-gradient(135deg, #0d3b66, #1a6b5a);
  border-radius: 8px;
}

.analytics-head h1 {
  margin: 6px 0 10px;
  font-size: 34px;
  line-height: 1.2;
}

.analytics-head p {
  margin: 0;
}

.analytics-head-actions {
  flex-shrink: 0;
}

.anomaly-section {
  margin-bottom: 18px;
}

.anomaly-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.anomaly-alert {
  border-radius: 8px;
}

.anomaly-title {
  font-weight: 600;
  margin-right: 10px;
}

.anomaly-detail {
  color: rgba(0, 0, 0, 0.65);
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
  margin-bottom: 22px;
}

.kpi-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  position: relative;
  transition: all 0.2s;
}

.kpi-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.kpi-label {
  font-size: 12px;
  color: #909399;
}

.kpi-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.kpi-card.danger .kpi-value {
  color: #F56C6C;
}

.kpi-card.warning .kpi-value {
  color: #E6A23C;
}

.kpi-card.success .kpi-value {
  color: #67C23A;
}

.kpi-sub {
  font-size: 12px;
  color: #909399;
}

.kpi-trend {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
}

.kpi-trend.up {
  color: #F56C6C;
  background: #fef0f0;
}

.kpi-trend.down {
  color: #67C23A;
  background: #f0f9eb;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.chart-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 20px;
  transition: all 0.2s;
}

.chart-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.chart-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.chart-card-head h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  padding-left: 10px;
  border-left: 3px solid #409eff;
}

.chart-body {
  width: 100%;
  height: 300px;
}

.chart-legend-custom {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #606266;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  flex-shrink: 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.detail-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 20px;
}

.detail-card h2 {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  padding-left: 10px;
  border-left: 3px solid #409eff;
}

.val-danger {
  color: #F56C6C !important;
}

.val-warning {
  color: #E6A23C !important;
}

.val-success {
  color: #67C23A !important;
}

.val-default {
  color: #303133;
}

.compact-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.compact-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px;
  transition: all 0.2s;
}

.compact-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.compact-card.compact-danger {
  border-color: #F56C6C;
  background: #fff5f5;
}

.compact-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.compact-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.compact-items {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}

.compact-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  background: #f8f9fb;
  border-radius: 6px;
}

.compact-label {
  font-size: 11px;
  color: #909399;
}

.compact-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

@media (max-width: 1024px) {
  .kpi-row {
    grid-template-columns: repeat(3, 1fr);
  }

  .chart-grid,
  .detail-grid,
  .compact-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .analytics-head {
    flex-direction: column;
    align-items: flex-start;
    padding: 24px;
  }

  .analytics-head h1 {
    font-size: 24px;
  }

  .kpi-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .kpi-value {
    font-size: 20px;
  }

  .chart-body {
    height: 240px;
  }

  .compact-items {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .kpi-row {
    grid-template-columns: 1fr;
  }

  .analytics-head {
    padding: 18px;
  }

  .analytics-head h1 {
    font-size: 20px;
  }

  .compact-items {
    grid-template-columns: 1fr 1fr;
  }

  .chart-body {
    height: 200px;
  }
}
</style>
