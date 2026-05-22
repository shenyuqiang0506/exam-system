<template>
  <div class="exam-list">
    <div v-for="paper in papers" :key="paper.id" class="exam-card">
      <div class="exam-card-left">
        <div class="exam-icon" :class="getStatusClass(paper.status)">
          {{ getStatusIcon(paper.status) }}
        </div>
      </div>

      <div class="exam-card-body">
        <h3 class="exam-title">{{ paper.title }}</h3>
        <div class="exam-meta">
          <span><el-icon><Collection /></el-icon> {{ paper.subjectName }}</span>
          <span><el-icon><Document /></el-icon> {{ paper.questionCount }} 题</span>
          <span><el-icon><Trophy /></el-icon> {{ paper.totalScore }} 分</span>
        </div>
        <div class="exam-time" v-if="paper.startTime">
          <el-icon><Clock /></el-icon>
          {{ formatTime(paper.startTime) }} ~ {{ formatTime(paper.endTime) }}
        </div>
        <div class="exam-status">
          <el-tag :type="getStatusTagType(paper.status)" size="small" effect="dark">
            {{ paper.status }}
          </el-tag>
          <span v-if="paper.status === '进行中' && Number(paper.remainingSeconds) > 0" class="countdown">
            剩余：{{ getCountdown(paper) }}
          </span>
          <span v-if="paper.status === '进行中' && Number(paper.remainingSeconds) <= 0" class="countdown unlimited">
            无时间限制
          </span>
          <span v-if="paper.status === '未开始'" class="countdown start">
            {{ formatStartCountdown(paper.startTime) }} 后开始
          </span>
        </div>
      </div>

      <div class="exam-card-right">
        <!-- 进度 -->
        <div class="exam-progress">
          <el-progress
            :percentage="getProgress(paper)"
            :status="getProgressStatus(paper)"
            :stroke-width="8"
          />
          <span class="progress-text">{{ paper.answeredCount }}/{{ paper.questionCount }}</span>
        </div>

        <!-- 得分 -->
        <div v-if="paper.myScore !== null" class="exam-score">
          <span class="score-value">{{ paper.myScore }}</span>
          <span class="score-unit">分</span>
        </div>

        <!-- 操作按钮 -->
        <el-button
          :type="getButtonType(paper)"
          :disabled="isButtonDisabled(paper)"
          @click="handleAction(paper)"
          size="large"
        >
          {{ getButtonText(paper) }}
        </el-button>
      </div>
    </div>

    <el-empty v-if="papers.length === 0" :description="emptyText" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Collection, Document, Trophy, Clock } from '@element-plus/icons-vue'

const props = defineProps({
  papers: {
    type: Array,
    default: () => []
  },
  type: {
    type: String,
    default: 'active'
  }
})

const emit = defineEmits(['refresh'])
const router = useRouter()

// 存储每个试卷的剩余时间（实时更新）
const countdowns = ref({})
let timer = null

const emptyText = computed(() => {
  return props.type === 'active' ? '暂无活跃题目集' : '暂无题目集'
})

// 初始化倒计时
const initCountdowns = () => {
  const newCountdowns = {}
  props.papers.forEach(paper => {
    if (paper.status === '进行中') {
      const seconds = Number(paper.remainingSeconds)
      if (seconds > 0) {
        newCountdowns[paper.id] = seconds
      }
    }
  })
  countdowns.value = newCountdowns
}

// 监听 papers 变化
watch(() => props.papers, () => {
  initCountdowns()
}, { immediate: true })

// 启动定时器
onMounted(() => {
  timer = setInterval(() => {
    Object.keys(countdowns.value).forEach(id => {
      if (countdowns.value[id] > 0) {
        countdowns.value[id]--
      }
    })
  }, 1000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})

// 获取实时倒计时
const getCountdown = (paper) => {
  const seconds = countdowns.value[paper.id]
  if (seconds === undefined || seconds === null) {
    return formatCountdown(paper.remainingSeconds)
  }
  return formatCountdown(seconds)
}

// 状态样式
const getStatusClass = (status) => {
  return {
    '未开始': 'status-pending',
    '进行中': 'status-active',
    '已结束': 'status-ended',
    '已归档': 'status-archived'
  }[status] || 'status-active'
}

const getStatusIcon = (status) => {
  return {
    '未开始': '⏰',
    '进行中': '📝',
    '已结束': '✅',
    '已归档': '📦'
  }[status] || '📝'
}

const getStatusTagType = (status) => {
  return {
    '未开始': 'info',
    '进行中': 'success',
    '已结束': 'danger',
    '已归档': 'warning'
  }[status] || 'info'
}

// 进度
const getProgress = (paper) => {
  if (!paper.questionCount) return 0
  return Math.round((paper.answeredCount / paper.questionCount) * 100)
}

const getProgressStatus = (paper) => {
  if (paper.myScore !== null) return 'success'
  if (paper.answeredCount > 0) return ''
  return ''
}

// 按钮状态
const getButtonType = (paper) => {
  if (paper.status === '进行中') {
    if (paper.recordStatus === 0) return 'warning'
    return 'primary'
  }
  if (paper.status === '已结束' && paper.myScore !== null) return 'success'
  if (paper.status === '已归档' && paper.myScore !== null) return 'success'
  return 'info'
}

const isButtonDisabled = (paper) => {
  if (paper.status === '未开始') return true
  if (paper.status === '已结束' && paper.myScore === null) return true
  if (paper.status === '已归档' && paper.myScore === null) return true
  return false
}

const getButtonText = (paper) => {
  if (paper.status === '未开始') return '未开始'
  if (paper.status === '进行中') {
    if (paper.recordStatus === 0) return '继续考试'
    return '开始考试'
  }
  if (paper.status === '已结束') {
    if (paper.myScore !== null) return '查看成绩'
    return '已结束'
  }
  if (paper.status === '已归档') {
    if (paper.myScore !== null) return '查看成绩'
    return '已归档'
  }
  return '开始考试'
}

// 操作
const handleAction = async (paper) => {
  if (paper.status === '进行中') {
    if (paper.recordStatus === 0) {
      router.push(`/student/exam/${paper.id}`)
    } else {
      await ElMessageBox.confirm(
        `确定开始【${paper.title}】考试？`,
        '开始考试',
        { confirmButtonText: '开始', cancelButtonText: '取消', type: 'warning' }
      )
      router.push(`/student/exam/${paper.id}`)
    }
  } else if (paper.status === '已结束' && paper.myScore !== null) {
    router.push(`/student/result/${paper.recordId}`)
  }
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

// 格式化倒计时
const formatCountdown = (seconds) => {
  if (!seconds || seconds <= 0) return '00:00:00'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// 格式化开始倒计时
const formatStartCountdown = (startTime) => {
  if (!startTime) return ''
  const now = new Date()
  const start = new Date(startTime)
  const diff = Math.floor((start - now) / 1000)

  if (diff <= 0) return '即将开始'
  if (diff < 3600) return `${Math.ceil(diff / 60)} 分钟`
  if (diff < 86400) return `${Math.ceil(diff / 3600)} 小时`
  return `${Math.ceil(diff / 86400)} 天`
}
</script>

<style scoped>
.exam-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.exam-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;
}

.exam-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.exam-card-left {
  margin-right: 20px;
}

.exam-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.status-pending {
  background: #e6f7ff;
}

.status-active {
  background: #f6ffed;
}

.status-ended {
  background: #fff7e6;
}

.status-archived {
  background: #f5f5f5;
}

.exam-card-body {
  flex: 1;
}

.exam-title {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #303133;
}

.exam-meta {
  display: flex;
  gap: 20px;
  color: #909399;
  font-size: 14px;
  margin-bottom: 8px;
}

.exam-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.exam-time {
  color: #606266;
  font-size: 14px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.exam-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.countdown {
  color: #67c23a;
  font-weight: bold;
  font-family: monospace;
  font-size: 16px;
}

.countdown.start {
  color: #409eff;
}

.countdown.unlimited {
  color: #909399;
  font-weight: normal;
}

.exam-card-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-width: 120px;
}

.exam-progress {
  width: 100%;
  text-align: center;
}

.progress-text {
  font-size: 12px;
  color: #909399;
}

.exam-score {
  text-align: center;
}

.score-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

.score-unit {
  font-size: 14px;
  color: #909399;
  margin-left: 4px;
}
</style>
