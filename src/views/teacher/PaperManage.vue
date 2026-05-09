<template>
  <div class="paper-manage">
    <!-- 操作按钮 -->
    <el-card shadow="hover" class="action-card">
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        手动组卷
      </el-button>
      <el-button type="success" @click="$router.push('/teacher/auto-paper')">
        <el-icon><MagicStick /></el-icon>
        智能组卷
      </el-button>
    </el-card>

    <!-- 试卷列表 -->
    <el-card shadow="hover">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="试卷名称" />
        <el-table-column prop="subjectName" label="科目" width="120" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column prop="targetDifficulty" label="目标难度" width="100">
          <template #default="{ row }">
            {{ row.targetDifficulty?.toFixed(1) }}
          </template>
        </el-table-column>
        <el-table-column prop="isArchived" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isArchived === 0 ? 'success' : 'info'">
              {{ row.isArchived === 0 ? '正常' : '已归档' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              v-if="row.isArchived === 0"
              title="确定归档该试卷？"
              @confirm="handleArchive(row.id)"
            >
              <template #reference>
                <el-button type="warning" link>归档</el-button>
              </template>
            </el-popconfirm>
            <el-button type="primary" link @click="exportScore(row.id)">
              导出成绩
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 手动组卷对话框 -->
    <el-dialog v-model="dialogVisible" title="手动组卷" width="800px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="试卷名称" prop="title">
          <el-input v-model="formData.title" placeholder="请输入试卷名称" />
        </el-form-item>
        <el-form-item label="科目" prop="subjectName">
          <el-select v-model="formData.subjectName" placeholder="请选择科目" @change="loadQuestions" style="width: 100%">
            <el-option
              v-for="subject in subjectList"
              :key="subject"
              :label="subject"
              :value="subject"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择题目" prop="questionIds">
          <el-transfer
            v-model="formData.questionIds"
            :data="allQuestions"
            :titles="['题库', '已选']"
            :props="{ key: 'id', label: 'content' }"
            filterable
            filter-placeholder="搜索题目"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">创建试卷</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, MagicStick } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)
const tableData = ref([])
const allQuestions = ref([])
const subjectList = ref([])

const formData = reactive({
  title: '',
  subjectName: '',
  questionIds: []
})

const rules = {
  title: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  subjectName: [{ required: true, message: '请选择科目', trigger: 'change' }],
  questionIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一道题目', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    // 教师查看所有试卷（包含已归档），使用分页接口
    const res = await request.get('/api/paper/page', { params: { page: 1, size: 100 } })
    tableData.value = res.data.records
  } catch (err) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadQuestions = async () => {
  if (!formData.subjectName) return
  try {
    const res = await request.get('/api/question/page', {
      params: { subjectName: formData.subjectName, page: 1, size: 100 }
    })
    allQuestions.value = res.data.records.map(q => ({
      ...q,
      content: `[${q.type === 1 ? '单选' : q.type === 2 ? '多选' : '主观'}] ${q.content.substring(0, 30)}...`
    }))
  } catch (err) {
    ElMessage.error('加载题目失败')
  }
}

const handleCreate = () => {
  Object.assign(formData, { title: '', subjectName: '', questionIds: [] })
  allQuestions.value = []
  dialogVisible.value = true
}

const handleArchive = async (id) => {
  try {
    await request.put(`/api/paper/archive/${id}`)
    ElMessage.success('归档成功')
    loadData()
  } catch (err) {
    ElMessage.error('归档失败')
  }
}

const exportScore = async (paperId) => {
  try {
    const res = await request.get(`/api/record/export/${paperId}`, {
      responseType: 'blob'
    })
    // 从响应头获取文件名，或使用默认名称
    const blob = new Blob([res.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    // 查找试卷标题作为文件名
    const paper = tableData.value.find(p => p.id === paperId)
    link.download = `${paper?.title || '成绩表'}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      // 后端 ManualCreateDTO 期望 { paper: {...}, questionIds: [...] }
      await request.post('/api/paper/manual-create', {
        paper: { title: formData.title, subjectName: formData.subjectName },
        questionIds: formData.questionIds
      })
      ElMessage.success('创建成功')
      dialogVisible.value = false
      loadData()
    } catch (err) {
      ElMessage.error('创建失败')
    }
  })
}

// 加载科目列表
const loadSubjects = async () => {
  try {
    const res = await request.get('/api/question/subjects')
    subjectList.value = res.data || []
  } catch {
    subjectList.value = ['高等数学', '数据结构', '计算机网络', 'Java程序设计']
  }
}

onMounted(() => {
  loadData()
  loadSubjects()
})
</script>

<style scoped>
.paper-manage {
  padding: 10px;
}

.action-card {
  margin-bottom: 20px;
}
</style>
