<template>
  <div class="record-list">
    <el-card shadow="hover">
      <el-table :data="recordList" v-loading="loading" stripe border>
        <el-table-column prop="id" label="记录ID" width="100"/>
        <el-table-column prop="paperTitle" label="试卷名称" min-width="200"/>
        <el-table-column prop="totalScore" label="总得分" width="100">
          <template #default="{ row }">
            <span class="score-text">{{ row.totalScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="objectiveScore" label="客观题" width="100"/>
        <el-table-column prop="subjectiveScore" label="主观题" width="100"/>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <div>
              <el-tag :type="getStatusType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
              <div v-if="row.aiGradeStatusDesc" class="ai-status-text">
                {{ row.aiGradeStatusDesc }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="考试时间" width="180"/>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              @click="viewDetail(row.id)"
              :disabled="row.status === 2"
            >
              {{ row.status === 2 ? '判分中...' : '查看详情' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const recordList = ref([])

const loadRecords = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/record/my-records')
    recordList.value = res.data
    
    // 检查是否有正在判分的记录，自动刷新
    const hasGrading = res.data.some(r => r.status === 2 || r.aiGradeStatus === 1)
    if (hasGrading) {
      setTimeout(loadRecords, 3000) // 3秒后自动刷新
    }
  } catch {
    ElMessage.error('加载记录失败')
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  switch (status) {
    case 0: return 'warning'
    case 1: return 'success'
    case 2: return 'info'
    default: return 'info'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 0: return '考试中'
    case 1: return '已批阅'
    case 2: return '待批阅'
    default: return '未知'
  }
}

const viewDetail = (id) => {
  router.push(`/student/result/${id}`)
}

onMounted(loadRecords)
</script>

<style scoped>
.record-list {
  padding: 10px;
}

.score-text {
  font-weight: bold;
  color: #409eff;
  font-size: 16px;
}

.ai-status-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
