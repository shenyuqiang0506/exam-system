<template>
  <div class="class-manage">
    <!-- 操作栏 -->
    <el-card shadow="hover" class="action-card">
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>创建班级
      </el-button>
    </el-card>

    <!-- 班级列表 -->
    <el-card shadow="hover">
      <el-table :data="classList" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="100" />
        <el-table-column prop="className" label="班级名称" min-width="150" />
        <el-table-column prop="teacherName" label="班主任" width="120" />
        <el-table-column prop="studentCount" label="学生数" width="80" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewStudents(row)">管理学生</el-button>
            <el-button type="info" link @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该班级？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑班级' : '创建班级'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="form.className" placeholder="请输入班级名称" />
        </el-form-item>
        <el-form-item label="班主任">
          <el-select v-model="form.teacherId" placeholder="选择班主任" clearable style="width: 100%">
            <el-option v-for="t in teacherList" :key="t.id" :label="t.realName || t.username" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="班级描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 学生管理弹窗 -->
    <el-dialog v-model="studentDialogVisible" :title="`${currentClass.className} - 学生管理`" width="800px">
      <div class="student-manage">
        <div class="add-student">
          <el-select v-model="selectedStudents" multiple filterable placeholder="选择要添加的学生" style="width: 70%">
            <el-option v-for="s in allStudents" :key="s.id" :label="`${s.realName || s.username}`" :value="s.id" />
          </el-select>
          <el-button type="primary" @click="addStudents" :disabled="selectedStudents.length === 0">添加</el-button>
        </div>
        <el-table :data="classStudents" stripe style="margin-top: 15px">
          <el-table-column prop="id" label="ID" width="100" />
          <el-table-column prop="username" label="账号" width="120" />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column prop="className" label="班级" width="120" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-popconfirm title="确定移除？" @confirm="removeStudent(row.id)">
                <template #reference>
                  <el-button type="danger" link>移除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const classList = ref([])
const teacherList = ref([])
const allStudents = ref([])
const classStudents = ref([])
const selectedStudents = ref([])
const dialogVisible = ref(false)
const studentDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const currentClass = reactive({ id: null, className: '' })

const form = reactive({ id: null, className: '', teacherId: null, description: '' })
const rules = { className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }] }

const loadClasses = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/admin/classes')
    classList.value = res.data || []
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const loadTeachers = async () => {
  try {
    const res = await request.get('/api/admin/teachers')
    teacherList.value = res.data?.records || []
  } catch {}
}

const showCreateDialog = () => {
  isEdit.value = false
  Object.assign(form, { id: null, className: '', teacherId: null, description: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, { id: row.id, className: row.className, teacherId: row.teacherId, description: row.description })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await request.put(`/api/class/${form.id}`, form)
      } else {
        await request.post('/api/class/create', form)
      }
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadClasses()
    } catch { ElMessage.error('操作失败') }
  })
}

const handleDelete = async (id) => {
  try {
    await request.delete(`/api/class/${id}`)
    ElMessage.success('删除成功')
    loadClasses()
  } catch { ElMessage.error('删除失败') }
}

const loadAllStudents = async () => {
  try {
    const res = await request.get('/api/admin/students/all')
    allStudents.value = res.data || []
  } catch {}
}

const loadClassStudents = async (classId) => {
  try {
    const res = await request.get(`/api/class/${classId}`)
    classStudents.value = res.data.students || []
  } catch { ElMessage.error('加载班级学生失败') }
}

const viewStudents = (cls) => {
  Object.assign(currentClass, cls)
  loadClassStudents(cls.id)
  loadAllStudents()
  studentDialogVisible.value = true
}

const addStudents = async () => {
  try {
    await request.post(`/api/class/${currentClass.id}/students`, selectedStudents.value)
    ElMessage.success('添加成功')
    selectedStudents.value = []
    loadClassStudents(currentClass.id)
    loadClasses()
  } catch { ElMessage.error('添加失败') }
}

const removeStudent = async (studentId) => {
  try {
    await request.delete(`/api/class/${currentClass.id}/students/${studentId}`)
    ElMessage.success('移除成功')
    loadClassStudents(currentClass.id)
    loadClasses()
  } catch { ElMessage.error('移除失败') }
}

onMounted(() => { loadClasses(); loadTeachers() })
</script>

<style scoped>
.class-manage { padding: 10px; }
.action-card { margin-bottom: 20px; }
.add-student { display: flex; gap: 10px; }
</style>
