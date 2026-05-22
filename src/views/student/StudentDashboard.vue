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
            <p>
              <el-icon>
                <Timer/>
              </el-icon>
              考试时长：120 分钟
            </p>
            <p>
              <el-icon>
                <Trophy/>
              </el-icon>
              总分：{{ paper.totalScore }} 分
            </p>
            <p>
              <el-icon>
                <Calendar/>
              </el-icon>
              创建时间：{{ paper.createTime }}
            </p>
          </div>

          <!-- 根据考试状态显示不同按钮 -->
          <el-button
              v-if="paper.completed"
              type="info"
              style="width: 100%"
              disabled
          >
            <el-icon>
              <CircleCheck/>
            </el-icon>
            已完成考试
          </el-button>
          <el-button
              v-else-if="paper.ongoing"
              type="warning"
              style="width: 100%"
              @click="continueExam(paper)"
          >
            <el-icon>
              <RefreshRight/>
            </el-icon>
            继续考试
          </el-button>
          <el-button
              v-else
              type="primary"
              style="width: 100%"
              @click="startExam(paper)"
          >
            <el-icon>
              <CaretRight/>
            </el-icon>
            开始考试
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="paperList.length === 0 && !loading" description="暂无可考试卷"/>
  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Timer, Trophy, Calendar, CaretRight, CircleCheck, RefreshRight} from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const paperList = ref([])

const loadPapers = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/paper/list-active')
    const papers = res.data || []

    // 获取每张试卷的考试状态
    for (let paper of papers) {
      try {
        const recordRes = await request.get(`/api/record/check/${paper.id}`)
        if (recordRes.data) {
          paper.completed = recordRes.data.status === 1
          paper.ongoing = recordRes.data.status === 0
          paper.recordId = recordRes.data.id
        } else {
          paper.completed = false
          paper.ongoing = false
        }
      } catch (err) {
        // 如果返回 403，说明已完成考试
        if (err.response?.status === 403 || err.response?.data?.code === 403) {
          paper.completed = true
          paper.ongoing = false
        } else {
          paper.completed = false
          paper.ongoing = false
        }
      }
    }

    paperList.value = papers
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
      {confirmButtonText: '开始', cancelButtonText: '取消', type: 'warning'}
  )
  router.push(`/student/exam/${paper.id}`)
}

const continueExam = (paper) => {
  router.push(`/student/exam/${paper.id}`)
}

onMounted(loadPapers)
</script>

<style scoped>
.student-dashboard {
  padding: 10px;
}

.paper-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.paper-title {
  font-weight: bold;
  font-size: 16px;
}

.paper-info {
  margin-bottom: 15px;
}

.paper-info p {
  margin: 8px 0;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
