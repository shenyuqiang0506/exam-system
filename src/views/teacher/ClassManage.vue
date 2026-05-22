<template>
  <div class="class-manage">
    <!-- 操作栏 -->
    <el-card shadow="hover" class="action-card">
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>创建班级
      </el-button>
    </el-card>

    <!-- 班级列表 -->
    <el-row :gutter="20">
      <el-col :span="8" v-for="cls in classList" :key="cls.id">
        <el-card shadow="hover" class="class-card">
          <template #header>
            <div class="card-header">
              <span>{{ cls.className }}</span>
              <el-dropdown @command="(cmd) => handleCommand(cmd, cls)">
                <el-icon><More /></el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="students">管理学生</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
          <p class="class-desc">{{ cls.description || '暂无描述' }}</p>
          <div class="class-footer">
            <el-button type="primary" link @click="viewStudents(cls)">查看学生</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 创建/编辑班级弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑班级' : '创建班级'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="form.className" placeholder="请输入班级名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入班级描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 学生管理弹窗 -->
    <el-dialog v-model="studentDialogVisible" :title="`${currentClass.className} - 学生管理`" width="700px">
      <div class="student-manage">
        <div class="add-student">
          <el-select
            v-model="selectedStudents"
            multiple
            filterable
            placeholder="选择要添加的学生"
            style="width: 70%"
          >
            <el-option
              v-for="s in allStudents"
              :key="s.id"
              :label="`${s.realName || s.username}`"
              :value="s.id"
            />
          </el-select>
          <el-button type="primary" @click="addStudents" :disabled="selectedStudents.length === 0">
            添加
          </el-button>
        </div>

        <el-table :data="classStudents" stripe style="margin-top: 15px">
          <el-table-column prop="id" label="ID" width="100" />
          <el-table-column prop="username" label="账号" width="120" />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-popconfirm title="确定移除该学生？" @confirm="removeStudent(row.id)">
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, More } from '@element-plus/icons-vue'
import request from '@/utils/request'

const classList = ref([])
const dialogVisible = ref(false)
const studentDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const allStudents = ref([])
const classStudents = ref([])
const selectedStudents = ref([])
const currentClass = reactive({ id: null, className: '' })

const form = reactive({
  id: null,
  className: '',
  description: ''
})

const rules = {
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }]
}

const loadClasses = async () => {
  try {
    const res = await request.get('/api/class/list')
    classList.value = res.data || []
  } catch {
    ElMessage.error('加载班级列表失败')
  }
}

const showCreateDialog = () => {
  isEdit.value = false
  form.id = null
  form.className = ''
  form.description = ''
  dialogVisible.value = true
}

const handleCommand = (cmd, cls) => {
  if (cmd === 'edit') {
    isEdit.value = true
    form.id = cls.id
    form.className = cls.className
    form.description = cls.description
    dialogVisible.value = true
  } else if (cmd === 'students') {
    currentClass.id = cls.id
    currentClass.className = cls.className
    loadClassStudents(cls.id)
    loadAllStudents()
    studentDialogVisible.value = true
  } else if (cmd === 'delete') {
    ElMessageBox.confirm('确定删除该班级？', '提示', { type: 'warning' }).then(async () => {
      try {
        await request.delete(`/api/class/${cls.id}`)
        ElMessage.success('删除成功')
        loadClasses()
      } catch {
        ElMessage.error('删除失败')
      }
    })
  }
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
    } catch {
      ElMessage.error('操作失败')
    }
  })
}

const loadAllStudents = async () => {
  try {
    const res = await request.get('/api/admin/students/all')
    allStudents.value = res.data || []
  } catch {
    ElMessage.error('加载学生列表失败')
  }
}

const loadClassStudents = async (classId) => {
  try {
    const res = await request.get(`/api/class/${classId}`)
    classStudents.value = res.data.students || []
  } catch {
    ElMessage.error('加载班级学生失败')
  }
}

const viewStudents = (cls) => {
  currentClass.id = cls.id
  currentClass.className = cls.className
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
  } catch {
    ElMessage.error('添加失败')
  }
}

const removeStudent = async (studentId) => {
  try {
    await request.delete(`/api/class/${currentClass.id}/students/${studentId}`)
    ElMessage.success('移除成功')
    loadClassStudents(currentClass.id)
  } catch {
    ElMessage.error('移除失败')
  }
}

onMounted(loadClasses)
</script>

<style scoped>
.class-manage {
  padding: 10px;
}
.action-card {
  margin-bottom: 20px;
}
.class-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.class-desc {
  color: #909399;
  font-size: 14px;
  min-height: 40px;
}
.class-footer {
  margin-top: 10px;
  text-align: right;
}
.add-student {
  display: flex;
  gap: 10px;
}
</style>
