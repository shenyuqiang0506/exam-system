<template>
  <div class="student-dashboard">
    <el-row :gutter="20">
      <el-col :span="8" v-for="paper in paperList" :key="paper.id">
        <el-card shadow="hover" class="paper-card">
          <template #header>
            <div class="card-header">
              <span class="paper-title">{{ paper.title }}</span>
              <el-tag type="success">{{ paper.subjectName }}</el-tag>
            </div>
          </template>
          <div class="paper-info">
            <p><el-icon><Timer /></el-icon> 考试时长：120 分钟</p>
            <p><el-icon><Trophy /></el-icon> 总分：{{ paper.totalScore }} 分</p>
            <p><el-icon><Calendar /></el-icon> 创建时间：{{ paper.createTime }}</p>
          </div>
          <el-button type="primary" style="width: 100%" @click="startExam(paper)">
            <el-icon><CaretRight /></el-icon>
            开始考试
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="paperList.length === 0 && !loading" description="暂无可考试卷" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Timer, Trophy, Calendar, CaretRight } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const paperList = ref([])

const loadPapers = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/paper/list-active')
    paperList.value = res.data
  } catch {
    ElMessage.error('加载试卷列表失败')
  } finally {
    loading.value = false
  }
}

const startExam = async (paper) => {
  await ElMessageBox.confirm(
    `确定开始【${paper.title}】考试？开始后不可暂停。`,
    '开始考试',
    { confirmButtonText: '开始', cancelButtonText: '取消', type: 'warning' }
  )
  router.push(`/student/exam/${paper.id}`)
}

onMounted(loadPapers)
</script>

<style scoped>
.student-dashboard { padding: 10px; }
.paper-card { margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.paper-title { font-weight: bold; font-size: 16px; }
.paper-info { margin-bottom: 15px; }
.paper-info p { margin: 8px 0; color: #606266; display: flex; align-items: center; gap: 6px; }
</style>
