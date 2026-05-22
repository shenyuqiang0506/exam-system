<template>
  <div class="student-manage">
    <!-- 搜索栏 -->
    <el-card shadow="hover" class="search-card">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input v-model="keyword" placeholder="搜索用户名/姓名" clearable @keyup.enter="loadStudents">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="16">
          <el-button type="primary" @click="loadStudents">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="keyword = ''; loadStudents()">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 学生列表 -->
    <el-card shadow="hover">
      <el-table :data="students" v-loading="loading" stripe border>
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="账号" width="110" />
        <el-table-column prop="realName" label="姓名" width="90" />
        <el-table-column prop="studentNo" label="学号" width="110" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button :type="row.status === 1 ? 'warning' : 'success'" link @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-popconfirm title="确定重置该用户密码为学号后6位？" @confirm="resetPassword(row.id)">
              <template #reference>
                <el-button type="primary" link>重置密码</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadStudents"
        @current-change="loadStudents"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const keyword = ref('')
const students = ref([])

const pagination = reactive({ page: 1, size: 10, total: 0 })

const loadStudents = async () => {
  loading.value = true
  try {
    // 教师只能查看自己班级的学生
    const res = await request.get('/api/admin/teacher/students', {
      params: {
        page: pagination.page,
        size: pagination.size,
        keyword: keyword.value
      }
    })
    students.value = res.data.records || []
    pagination.total = Number(res.data.total) || 0
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const toggleStatus = async (user) => {
  const newStatus = user.status === 1 ? 0 : 1
  try {
    await request.put(`/api/admin/users/${user.id}/status?status=${newStatus}`)
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    loadStudents()
  } catch {
    ElMessage.error('操作失败')
  }
}

const resetPassword = async (id) => {
  try {
    await request.put(`/api/admin/users/${id}/reset-password`)
    ElMessage.success('密码已重置')
  } catch {
    ElMessage.error('重置失败')
  }
}

onMounted(loadStudents)
</script>

<style scoped>
.student-manage { padding: 10px; }
.search-card { margin-bottom: 20px; }
</style>
