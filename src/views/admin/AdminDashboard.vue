<template>
  <div class="admin-dashboard">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #409eff">
            <el-icon :size="28" color="#fff"><User /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.studentCount }}</span>
            <span class="stat-label">学生总数</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #67c23a">
            <el-icon :size="28" color="#fff"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.teacherCount }}</span>
            <span class="stat-label">教师总数</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #e6a23c">
            <el-icon :size="28" color="#fff"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.paperCount }}</span>
            <span class="stat-label">试卷总数</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="quick-actions">
      <template #header>
        <span>快捷操作</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-button type="primary" size="large" style="width: 100%" @click="$router.push('/admin/students')">
            <el-icon><User /></el-icon>
            学生管理
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="success" size="large" style="width: 100%" @click="$router.push('/admin/teachers')">
            <el-icon><UserFilled /></el-icon>
            教师管理
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="warning" size="large" style="width: 100%" @click="$router.push('/admin/import')">
            <el-icon><Upload /></el-icon>
            批量导入
          </el-button>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { User, UserFilled, Document, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const stats = reactive({
  studentCount: 0,
  teacherCount: 0,
  paperCount: 0
})

const loadStats = async () => {
  try {
    const res = await request.get('/api/admin/stats')
    stats.studentCount = res.data.studentCount || 0
    stats.teacherCount = res.data.teacherCount || 0
    stats.paperCount = res.data.paperCount || 0
  } catch {
    ElMessage.error('加载统计数据失败')
  }
}

onMounted(loadStats)
</script>

<style scoped>
.admin-dashboard { padding: 10px; }
.stat-card { display: flex; align-items: center; padding: 20px; }
.stat-icon { width: 60px; height: 60px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-right: 15px; }
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 28px; font-weight: bold; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 5px; }
.quick-actions { margin-top: 20px; }
.quick-actions .el-button { height: 60px; font-size: 16px; }
</style>
