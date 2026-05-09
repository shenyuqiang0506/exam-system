<template>
  <div class="dashboard">
    <!-- ===== 顶部摘要卡片（6格） ===== -->
    <el-row :gutter="16">
      <el-col :span="4" v-for="card in summaryCards" :key="card.key">
        <el-card shadow="hover" class="summary-card" v-loading="loading">
          <div class="card-inner">
            <div class="card-icon" :style="{ background: card.color }">
              <el-icon :size="24" color="#fff"><component :is="card.icon" /></el-icon>
            </div>
            <div class="card-body">
              <div class="card-value">{{ card.value }}</div>
              <div class="card-label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ===== 图表区（柱 12 + 饼 6 + 雷达 6） ===== -->
    <el-row :gutter="16" style="margin-top: 16px">
      <!-- 柱状图：成绩分布 -->
      <el-col :span="12">
        <el-card shadow="hover" v-loading="loading">
          <template #header>
            <div class="chart-header">
              <el-icon color="#409eff" style="margin-right:6px"><DataLine /></el-icon>
              <span>成绩分布区间</span>
              <el-tag size="small" type="info" style="margin-left:auto">柱状图</el-tag>
            </div>
          </template>
          <div ref="barChartRef" class="chart-box"></div>
        </el-card>
      </el-col>

      <!-- 环形图：及格率 -->
      <el-col :span="6">
        <el-card shadow="hover" v-loading="loading">
          <template #header>
            <div class="chart-header">
              <el-icon color="#67c23a" style="margin-right:6px"><PieChart /></el-icon>
              <span>整体及格率</span>
              <el-tag size="small" type="success" style="margin-left:auto">饼图</el-tag>
            </div>
          </template>
          <div ref="pieChartRef" class="chart-box"></div>
        </el-card>
      </el-col>

      <!-- 雷达图：科目均分 -->
      <el-col :span="6">
        <el-card shadow="hover" v-loading="loading">
          <template #header>
            <div class="chart-header">
              <el-icon color="#e6a23c" style="margin-right:6px"><DataAnalysis /></el-icon>
              <span>各科目均分掌握度</span>
              <el-tag size="small" type="warning" style="margin-left:auto">雷达图</el-tag>
            </div>
          </template>
          <div ref="radarChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ===== 快捷操作 ===== -->
    <el-card shadow="hover" style="margin-top: 16px">
      <template #header><span>快捷操作</span></template>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-button type="primary" size="large" style="width:100%" @click="$router.push('/teacher/questions')">
            <el-icon><Plus /></el-icon> 添加题目
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="success" size="large" style="width:100%" @click="$router.push('/teacher/papers')">
            <el-icon><DocumentAdd /></el-icon> 创建试卷
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="warning" size="large" style="width:100%" @click="$router.push('/teacher/auto-paper')">
            <el-icon><MagicStick /></el-icon> 智能组卷
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="info" size="large" style="width:100%" @click="$router.push('/teacher/records')">
            <el-icon><DataAnalysis /></el-icon> 查看成绩
          </el-button>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

// ─── 摘要卡片定义 ────────────────────────────────────────────────────────────
const summaryCards = reactive([
  { key: 'totalExams',   label: '总考试人次', value: '…', icon: 'DataAnalysis', color: '#409eff' },
  { key: 'maxScore',     label: '历史最高分', value: '…', icon: 'Odometer',     color: '#67c23a' },
  { key: 'avgScore',     label: '整体平均分', value: '…', icon: 'TrendCharts',  color: '#e6a23c' },
  { key: 'passRatePct',  label: '整体及格率', value: '…', icon: 'CircleCheck',  color: '#00c48f' },
  { key: 'questionCount',label: '题库总数',   value: '…', icon: 'Document',     color: '#9254de' },
  { key: 'paperCount',   label: '试卷总数',   value: '…', icon: 'Files',        color: '#f56c6c' },
])

// ─── 图表 DOM refs & 实例 ─────────────────────────────────────────────────────
const barChartRef   = ref(null)
const pieChartRef   = ref(null)
const radarChartRef = ref(null)
let barChart   = null
let pieChart   = null
let radarChart = null
const loading  = ref(true)

// ─── 柱状图：成绩分布 ─────────────────────────────────────────────────────────
function initBarChart(dist) {
  if (!barChartRef.value) return
  barChart = echarts.init(barChartRef.value)

  const d = dist || {}
  const values = [
    Number(d.below60 || 0),
    Number(d.s6070   || 0),
    Number(d.s7080   || 0),
    Number(d.s8090   || 0),
    Number(d.above90 || 0),
  ]
  const BAR_COLORS = ['#f56c6c', '#e6a23c', '#409eff', '#85ce61', '#67c23a']

  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => `${params[0].name}：<b>${params[0].value} 人</b>`,
    },
    grid: { top: '18%', left: '4%', right: '4%', bottom: '6%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['< 60分', '60~70分', '70~80分', '80~90分', '≥ 90分'],
      axisLabel: { color: '#606266', fontSize: 12 },
      axisTick: { alignWithLabel: true },
    },
    yAxis: {
      type: 'value',
      name: '人数（人）',
      nameTextStyle: { color: '#909399', fontSize: 11 },
      minInterval: 1,
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { type: 'dashed', color: '#ebeef5' } },
    },
    series: [{
      name: '人数',
      type: 'bar',
      barMaxWidth: 56,
      itemStyle: {
        color: (params) => BAR_COLORS[params.dataIndex],
        borderRadius: [6, 6, 0, 0],
      },
      label: {
        show: true,
        position: 'top',
        formatter: '{c} 人',
        color: '#606266',
        fontSize: 12,
      },
      data: values,
    }],
  })
}

// ─── 环形图：及格率 ───────────────────────────────────────────────────────────
function initPieChart(passData) {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)

  const p = passData || {}
  const passed = Number(p.passed || 0)
  const failed  = Number(p.failed  || 0)
  const total   = passed + failed
  const pct     = total > 0 ? ((passed / total) * 100).toFixed(1) : '0.0'

  // 同步更新摘要卡片
  const card = summaryCards.find(c => c.key === 'passRatePct')
  if (card) card.value = `${pct}%`

  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}：{c} 人 ({d}%)',
    },
    legend: {
      bottom: '2%',
      left: 'center',
      itemWidth: 12,
      itemHeight: 12,
      textStyle: { color: '#606266' },
    },
    graphic: [{
      type: 'text',
      left: 'center',
      top: '38%',
      style: {
        text: `及格率\n${pct}%`,
        textAlign: 'center',
        fill: '#303133',
        fontSize: 14,
        fontWeight: 'bold',
        lineHeight: 22,
      },
    }],
    series: [{
      name: '及格情况',
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: false,
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 13, fontWeight: 'bold' },
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,.15)' },
      },
      labelLine: { show: false },
      data: [
        { value: passed, name: '及格',   itemStyle: { color: '#67c23a' } },
        { value: failed,  name: '不及格', itemStyle: { color: '#f56c6c' } },
      ],
    }],
  })
}

// ─── 雷达图：科目均分 ─────────────────────────────────────────────────────────
function initRadarChart(subjectData) {
  if (!radarChartRef.value) return
  radarChart = echarts.init(radarChartRef.value)

  let subjects = (subjectData || []).map(s => ({
    name: String(s.subjectName || ''),
    avg:  Number(s.avgScore || 0),
  }))

  // 雷达图至少需要 3 个指标，不足时补位
  while (subjects.length < 3) {
    subjects.push({ name: '', avg: 0 })
  }

  const indicators = subjects.map(s => ({ name: s.name, max: 100 }))
  const values     = subjects.map(s => s.avg)

  radarChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p) => {
        const vals = p.value
        return subjects
          .map((s, i) => `${s.name || '—'}：<b>${vals[i]}</b> 分`)
          .join('<br/>')
      },
    },
    radar: {
      indicator: indicators,
      splitNumber: 4,
      shape: 'polygon',
      axisName: {
        color: '#606266',
        fontSize: 11,
        formatter: (val) => val.length > 5 ? val.slice(0, 4) + '…' : val,
      },
      splitLine:  { lineStyle: { color: 'rgba(64,158,255,0.25)' } },
      splitArea:  { areaStyle: { color: ['rgba(64,158,255,0.04)', 'rgba(64,158,255,0.1)'] } },
      axisLine:   { lineStyle: { color: 'rgba(64,158,255,0.3)' } },
    },
    series: [{
      name: '科目均分',
      type: 'radar',
      data: [{
        value: values,
        name: '平均分',
        areaStyle:  { color: 'rgba(64,158,255,0.18)' },
        lineStyle:  { color: '#409eff', width: 2 },
        itemStyle:  { color: '#409eff' },
        symbol:     'circle',
        symbolSize: 5,
      }],
    }],
  })
}

// ─── 窗口自适应 ───────────────────────────────────────────────────────────────
const handleResize = () => {
  barChart?.resize()
  pieChart?.resize()
  radarChart?.resize()
}

// ─── 生命周期 ─────────────────────────────────────────────────────────────────
onMounted(async () => {
  try {
    const res  = await request.get('/api/statistics/dashboard')
    const data = res.data || {}

    // 更新摘要卡片
    const sum = data.summary || {}
    ;[
      ['totalExams',    sum.totalExams    ?? 0],
      ['maxScore',      sum.maxScore      ?? '-'],
      ['avgScore',      sum.avgScore      ?? '-'],
      ['questionCount', sum.questionCount ?? 0],
      ['paperCount',    sum.paperCount    ?? 0],
    ].forEach(([key, val]) => {
      const card = summaryCards.find(c => c.key === key)
      if (card) card.value = val
    })

    // 等 DOM 渲染完毕再初始化 ECharts
    await nextTick()
    initBarChart(data.scoreDistribution)
    initPieChart(data.passRate)
    initRadarChart(data.subjectAvg)
  } catch (e) {
    ElMessage.warning('暂无统计数据，图表将显示空白')
    await nextTick()
    initBarChart(null)
    initPieChart(null)
    initRadarChart(null)
  } finally {
    loading.value = false
  }

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  pieChart?.dispose()
  radarChart?.dispose()
})
</script>

<style scoped>
.dashboard { padding: 10px; }

/* 摘要卡片 */
.summary-card .card-inner {
  display: flex;
  align-items: center;
  gap: 14px;
}
.card-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.card-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.1;
}
.card-label {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

/* 图表卡片 */
.chart-header {
  display: flex;
  align-items: center;
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
.chart-box {
  height: 300px;
  width: 100%;
}

/* 快捷操作按钮 */
:deep(.quick-actions .el-button) {
  height: 60px;
  font-size: 15px;
}
.el-button { height: 56px; font-size: 15px; }
</style>
