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
          <el-button type="success" @click="$router.push('/admin/import')">
            <el-icon><Upload /></el-icon>批量导入
          </el-button>
          <el-button type="danger" :disabled="selectedIds.length === 0" @click="batchDelete">
            <el-icon><Delete /></el-icon>批量删除
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 学生列表 -->
    <el-card shadow="hover">
      <el-table :data="students" v-loading="loading" stripe border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="账号" width="110" />
        <el-table-column prop="realName" label="姓名" width="90" />
        <el-table-column prop="studentNo" label="学号" width="110" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="email" label="邮箱" width="160" show-overflow-tooltip />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button :type="row.status === 1 ? 'warning' : 'success'" link @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-popconfirm title="确定重置该用户密码为 123456？" @confirm="resetPassword(row.id)">
              <template #reference>
                <el-button type="primary" link>重置密码</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm title="确定删除该用户？" @confirm="deleteUser(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
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
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadStudents"
        @current-change="loadStudents"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const keyword = ref('')
const students = ref([])
const selectedIds = ref([])

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const loadStudents = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/admin/students', {
      params: {
        page: pagination.page,
        size: pagination.size,
        keyword: keyword.value
      }
    })
    students.value = res.data.records
    pagination.total = Number(res.data.total) || 0
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(s => s.id)
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
    ElMessage.success('密码已重置为 123456')
  } catch {
    ElMessage.error('重置失败')
  }
}

const deleteUser = async (id) => {
  try {
    await request.delete(`/api/admin/users/${id}`)
    ElMessage.success('删除成功')
    loadStudents()
  } catch {
    ElMessage.error('删除失败')
  }
}

const batchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个学生？`, '提示', { type: 'warning' })
  try {
    for (const id of selectedIds.value) {
      await request.delete(`/api/admin/users/${id}`)
    }
    ElMessage.success('批量删除成功')
    loadStudents()
  } catch {
    ElMessage.error('批量删除失败')
  }
}

onMounted(loadStudents)
</script>

<style scoped>
.student-manage {
  padding: 10px;
}
.search-card {
  margin-bottom: 20px;
}
</style>
