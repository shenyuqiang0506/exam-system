<template>
  <div class="monitor-container">
    <!-- 顶部信息栏 -->
    <el-card shadow="hover" class="header-card">
      <div class="header-content">
        <div class="paper-info">
          <h2>{{ paperInfo.title }} - 实时监控</h2>
          <el-tag :type="connected ? 'success' : 'danger'" size="small">
            {{ connected ? '已连接' : '未连接' }}
          </el-tag>
        </div>
        <div class="stats">
          <el-statistic title="在线人数" :value="onlineCount" class="stat-item" />
          <el-statistic title="已交卷" :value="submittedCount" class="stat-item" />
          <el-statistic title="切屏警告" :value="screenSwitchCount" class="stat-item" />
        </div>
      </div>
    </el-card>

    <!-- 学生列表 -->
    <el-card shadow="hover" class="table-card">
      <template #header>
        <div class="table-header">
          <span>学生状态列表</span>
          <el-button type="primary" size="small" @click="refreshList">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <el-table :data="studentList" stripe border>
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" size="small">
              {{ getStatusText(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="答题进度" width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="getProgress(row)"
              :status="getProgressStatus(row)"
              :stroke-width="10"
            />
            <span class="progress-text">{{ row.answeredCount || 0 }}/{{ row.totalQuestions || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="切屏次数" width="100">
          <template #default="{ row }">
            <el-tag :type="row.screenSwitchCount > 0 ? 'danger' : 'success'" size="small">
              {{ row.screenSwitchCount || 0 }} 次
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后心跳" width="180">
          <template #default="{ row }">
            {{ formatTime(row.lastHeartbeat) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-popconfirm title="确定强制该学生交卷？" @confirm="forceSubmit(row)">
              <template #reference>
                <el-button type="danger" size="small" :disabled="row.submitted">
                  强制交卷
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { connect, subscribe, send, disconnect } from '@/utils/websocket'
import request from '@/utils/request'

const route = useRoute()
const paperId = route.params.paperId

const paperInfo = ref({ title: '加载中...' })
const connected = ref(false)
const studentList = ref([])
const onlineCount = ref(0)
const submittedCount = ref(0)
const screenSwitchCount = ref(0)

let subscription = null

const loadPaperInfo = async () => {
  try {
    const res = await request.get(`/api/paper/detail/${paperId}`)
    paperInfo.value = res.data.paper
  } catch {
    ElMessage.error('加载试卷信息失败')
  }
}

const updateStats = () => {
  onlineCount.value = studentList.value.filter(s => s.status !== 'offline').length
  submittedCount.value = studentList.value.filter(s => s.submitted).length
  screenSwitchCount.value = studentList.value.reduce((sum, s) => sum + (s.screenSwitchCount || 0), 0)
}

const handleMessage = (message) => {
  console.log('收到消息:', message)
  const { type, studentId, studentName, screenSwitchCount: sc, answeredCount, totalQuestions } = message

  const existing = studentList.value.find(s => s.studentId === studentId)

  switch (type) {
    case 'STUDENT_ONLINE':
      if (!existing) {
        studentList.value.push({
          studentId,
          studentName,
          status: 'online',
          answeredCount: answeredCount || 0,
          totalQuestions: totalQuestions || 0,
          screenSwitchCount: sc || 0,
          lastHeartbeat: new Date().toLocaleTimeString(),
          submitted: false
        })
      } else {
        existing.status = 'online'
        existing.lastHeartbeat = new Date().toLocaleTimeString()
      }
      break

    case 'STUDENT_OFFLINE':
      if (existing) {
        existing.status = 'offline'
      }
      break

    case 'ANSWER_SAVE':
      if (existing) {
        existing.answeredCount = answeredCount || existing.answeredCount
        existing.totalQuestions = totalQuestions || existing.totalQuestions
        existing.lastHeartbeat = new Date().toLocaleTimeString()
      }
      break

    case 'SCREEN_SWITCH':
      if (existing) {
        existing.screenSwitchCount = sc || existing.screenSwitchCount + 1
        existing.lastHeartbeat = new Date().toLocaleTimeString()
        ElMessage.warning(`${studentName} 切屏！当前切屏次数：${existing.screenSwitchCount}`)
      } else {
        // 如果学生不在列表中，添加
        studentList.value.push({
          studentId,
          studentName,
          status: 'online',
          answeredCount: 0,
          totalQuestions: 0,
          screenSwitchCount: sc || 1,
          lastHeartbeat: new Date().toLocaleTimeString(),
          submitted: false
        })
        ElMessage.warning(`${studentName} 切屏！`)
      }
      break
  }

  updateStats()
}

const refreshList = async () => {
  try {
    const res = await request.get(`/api/monitor/students/${paperId}`)
    if (res.data && res.data.length > 0) {
      studentList.value = res.data.map(s => ({
        ...s,
        status: 'online',
        lastHeartbeat: s.lastHeartbeat ? new Date(s.lastHeartbeat).toLocaleTimeString() : '-'
      }))
      updateStats()
    }
    ElMessage.success('已刷新')
  } catch (err) {
    console.error('刷新失败:', err)
  }
}

const forceSubmit = async (student) => {
  try {
    const res = await request.post('/api/record/force-submit', {
      studentId: student.studentId,
      paperId: Number(paperId)
    })
    student.submitted = true
    ElMessage.success(res.message || '已强制交卷')
  } catch (err) {
    ElMessage.error(err.message || '强制交卷失败')
  }
}

const getStatusType = (student) => {
  if (student.submitted) return 'success'
  if (student.status === 'online') return 'primary'
  return 'info'
}

const getStatusText = (student) => {
  if (student.submitted) return '已交卷'
  if (student.status === 'online') return '在线'
  return '离线'
}

const getProgress = (student) => {
  if (!student.totalQuestions) return 0
  return Math.round((student.answeredCount / student.totalQuestions) * 100)
}

const getProgressStatus = (student) => {
  if (student.submitted) return 'success'
  if (student.answeredCount === student.totalQuestions) return ''
  return ''
}

const formatTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  return date.toLocaleTimeString()
}

onMounted(() => {
  loadPaperInfo()

  // 连接 WebSocket
  connect('/ws/exam', () => {
    connected.value = true
    // 订阅监控频道
    subscription = subscribe(`/topic/monitor/${paperId}`, handleMessage)
  }, () => {
    connected.value = false
  })
})

onUnmounted(() => {
  disconnect()
})
</script>

<style scoped>
.monitor-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header-card {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.paper-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.paper-info h2 {
  margin: 0;
  font-size: 20px;
}

.stats {
  display: flex;
  gap: 30px;
}

.stat-item {
  text-align: center;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
