<template>
  <div class="teacher-manage">
    <!-- 搜索栏 -->
    <el-card shadow="hover" class="search-card">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input v-model="keyword" placeholder="搜索用户名/姓名" clearable @keyup.enter="loadTeachers">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="16">
          <el-button type="primary" @click="loadTeachers">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="keyword = ''; loadTeachers()">重置</el-button>
          <el-button type="success" @click="showAddDialog">
            <el-icon><Plus /></el-icon>添加教师
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 教师列表 -->
    <el-card shadow="hover">
      <el-table :data="teachers" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="100" />
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button :type="row.status === 1 ? 'warning' : 'success'" link @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-popconfirm title="确定重置该教师密码为 123456？" @confirm="resetPassword(row.id)">
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
        @size-change="loadTeachers"
        @current-change="loadTeachers"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <!-- 添加教师弹窗 -->
    <el-dialog v-model="dialogVisible" title="添加教师" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入登录账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdd" :loading="adding">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const adding = ref(false)
const keyword = ref('')
const teachers = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)

const pagination = reactive({ page: 1, size: 10, total: 0 })

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const loadTeachers = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/admin/teachers', {
      params: {
        page: pagination.page,
        size: pagination.size,
        keyword: keyword.value
      }
    })
    teachers.value = res.data.records
    pagination.total = Number(res.data.total) || 0
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  Object.assign(form, { username: '', password: '', realName: '', phone: '', email: '' })
  dialogVisible.value = true
}

const handleAdd = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    adding.value = true
    try {
      await request.post('/api/admin/teachers', {
        username: form.username,
        password: form.password,
        realName: form.realName,
        phone: form.phone,
        email: form.email,
        role: 1
      })
      ElMessage.success('添加成功')
      dialogVisible.value = false
      loadTeachers()
    } catch (err) {
      ElMessage.error(err.response?.data?.message || '添加失败')
    } finally {
      adding.value = false
    }
  })
}

const toggleStatus = async (user) => {
  const newStatus = user.status === 1 ? 0 : 1
  try {
    await request.put(`/api/admin/users/${user.id}/status?status=${newStatus}`)
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    loadTeachers()
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

onMounted(loadTeachers)
</script>

<style scoped>
.teacher-manage {
  padding: 10px;
}
.search-card {
  margin-bottom: 20px;
}
</style>
