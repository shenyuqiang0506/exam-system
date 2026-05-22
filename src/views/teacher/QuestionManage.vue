<template>
  <div class="question-manage">
    <!-- 搜索区域 -->
    <el-card shadow="hover" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="科目">
          <el-select v-model="searchForm.subjectName" placeholder="请选择科目" clearable style="width: 150px">
            <el-option
                v-for="subject in subjectList"
                :key="subject"
                :label="subject"
                :value="subject"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="searchForm.type" placeholder="请选择题型" clearable style="width: 120px">
            <el-option label="单选题" :value="1"/>
            <el-option label="多选题" :value="2"/>
            <el-option label="判断题" :value="3"/>
            <el-option label="主观题" :value="4"/>
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="searchForm.difficulty" placeholder="请选择" clearable style="width: 120px">
            <el-option label="简单" :value="0.3"/>
            <el-option label="中等" :value="0.5"/>
            <el-option label="困难" :value="0.8"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon>
              <Search/>
            </el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon>
              <Plus/>
            </el-icon>
            新增题目
          </el-button>
          <el-button @click="downloadTemplate">
            <el-icon>
              <Download/>
            </el-icon>
            下载 Excel 模板
          </el-button>
          <el-button type="warning" @click="importDialogVisible = true">
            <el-icon>
              <Upload/>
            </el-icon>
            批量导入题目
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="hover">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="80"/>
        <el-table-column prop="subjectName" label="科目" width="120"/>
        <el-table-column prop="type" label="题型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)">{{ getTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题目内容" show-overflow-tooltip min-width="200">
          <template #default="{ row }">{{ getDisplayContent(row) }}</template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="80"/>
        <el-table-column prop="difficulty" label="难度" width="120">
          <template #default="{ row }">
            <el-progress
                :percentage="row.difficulty * 100"
                :stroke-width="12"
                :show-text="false"
                :color="getDifficultyColor(row.difficulty)"
            />
            <span class="difficulty-text">{{ row.difficulty?.toFixed(1) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该题目？" @confirm="handleDelete(row.id)">
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
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
          style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑题目' : '新增题目'" width="700px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="科目" prop="subjectName">
          <el-select
              v-model="formData.subjectName"
              placeholder="请选择或输入科目"
              style="width: 100%"
              filterable
              allow-create
              default-first-option
          >
            <el-option
                v-for="subject in subjectList"
                :key="subject"
                :label="subject"
                :value="subject"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="题型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择题型" style="width: 100%">
            <el-option label="单选题" :value="1"/>
            <el-option label="多选题" :value="2"/>
            <el-option label="判断题" :value="3"/>
            <el-option label="主观题" :value="4"/>
          </el-select>
        </el-form-item>
        <el-form-item label="题目内容" prop="contentTitle">
          <el-input v-model="formData.contentTitle" type="textarea" :rows="3" placeholder="请输入题目正文"/>
        </el-form-item>

        <!-- 单选/多选题：选项输入区域 -->
        <template v-if="formData.type === 1 || formData.type === 2">
          <el-form-item v-for="opt in ['A','B','C','D']" :key="opt" :label="`选项 ${opt}`">
            <el-input v-model="formData.options[opt]" :placeholder="`请输入选项 ${opt} 的内容`"/>
          </el-form-item>
        </template>
        <el-form-item label="标准答案" prop="standardAnswer">
          <el-input v-model="formData.standardAnswer" type="textarea" :rows="2" placeholder="请输入标准答案"/>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分值" prop="score">
              <el-input-number v-model="formData.score" :min="1" :max="100" style="width: 100%"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度系数" prop="difficulty">
              <el-slider v-model="formData.difficulty" :min="0.1" :max="1" :step="0.1"
                         :format-tooltip="v => v.toFixed(1)"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="formData.type === 4" label="给分关键词" prop="pointsKeyword">
          <el-input v-model="formData.pointsKeyword" placeholder="多个关键词用逗号分隔"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="批量导入题目" width="500px">
      <el-upload
          drag
          action="http://localhost:8080/api/question/import"
          :headers="uploadHeaders"
          accept=".xlsx"
          :on-success="handleImportSuccess"
          :on-error="handleImportError"
      >
        <el-icon class="el-icon--upload">
          <Upload/>
        </el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            请先下载模板，严格按照模板格式填写后上传（仅支持 .xlsx 格式）
          </div>
        </template>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {Search, Plus, Download, Upload} from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const searchForm = reactive({subjectName: '', type: null, difficulty: null})
const pagination = reactive({page: 1, size: 10, total: 0})
const tableData = ref([])
const subjectList = ref([])

const formData = reactive({
  id: null, subjectName: '', type: 1,
  contentTitle: '',  // 题目正文（对应UI输入）
  content: '',       // 最终存储的JSON或纯文本
  options: {A: '', B: '', C: '', D: ''},  // 选择题选项
  standardAnswer: '',
  score: 5, difficulty: 0.5, pointsKeyword: ''
})

const rules = {
  subjectName: [{required: true, message: '请选择科目', trigger: 'change'}],
  type: [{required: true, message: '请选择题型', trigger: 'change'}],
  contentTitle: [{required: true, message: '请输入题目内容', trigger: 'blur'}],
  standardAnswer: [{required: true, message: '请输入标准答案', trigger: 'blur'}]
}

const getTypeName = (type) => ({1: '单选', 2: '多选', 3: '判断', 4: '主观'}[type])
const getTypeTag = (type) => ({1: '', 2: 'warning', 3: 'success', 4: 'danger'}[type])
const getDifficultyColor = (d) => d <= 0.3 ? '#67c23a' : d <= 0.6 ? '#e6a23c' : '#f56c6c'

// 表格中展示题目正文（去掉选项JSON）
const getDisplayContent = (row) => {
  if (row.type === 1 || row.type === 2) {
    try {
      const parsed = JSON.parse(row.content)
      return parsed.title || row.content
    } catch (e) {
      return row.content
    }
  }
  return row.content
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/question/page', {
      params: {page: pagination.page, size: pagination.size, ...searchForm}
    })
    tableData.value = res.data.records
    // 确保 total 是数字类型（Long 精度问题可能导致返回字符串）
    pagination.total = Number(res.data.total) || 0
    console.log('分页数据:', {page: pagination.page, size: pagination.size, total: pagination.total})
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1;
  loadData()
}
const handleReset = () => {
  searchForm.subjectName = ''
  searchForm.type = null
  searchForm.difficulty = null
  handleSearch()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formData, {
    id: null, subjectName: '', type: 1,
    contentTitle: '', content: '',
    options: {A: '', B: '', C: '', D: ''},
    standardAnswer: '', score: 5, difficulty: 0.5, pointsKeyword: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  // 解析 content，还原 contentTitle 和 options
  let contentTitle = row.content
  let options = {A: '', B: '', C: '', D: ''}
  if (row.type === 1 || row.type === 2) {
    try {
      const parsed = JSON.parse(row.content)
      if (parsed && parsed.title) {
        contentTitle = parsed.title
        ;(parsed.options || []).forEach(opt => {
          options[opt.value] = opt.label
        })
      }
    } catch (e) { /* 纯文本，不处理 */
    }
  }
  Object.assign(formData, {...row, contentTitle, options})
  dialogVisible.value = true
}

const handleDelete = async (id) => {
  try {
    await request.delete(`/api/question/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      // 序列化 content：选择题存 JSON，其他存纯文本
      let contentToSave = formData.contentTitle
      if (formData.type === 1 || formData.type === 2) {
        const opts = ['A', 'B', 'C', 'D']
            .filter(k => formData.options[k])
            .map(k => ({value: k, label: formData.options[k]}))
        contentToSave = JSON.stringify({title: formData.contentTitle, options: opts})
      }
      const payload = {
        id: formData.id,
        subjectName: formData.subjectName,
        type: formData.type,
        content: contentToSave,
        standardAnswer: formData.standardAnswer,
        score: formData.score,
        difficulty: formData.difficulty,
        pointsKeyword: formData.pointsKeyword
      }
      await request.post('/api/question/save', payload)
      ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
      dialogVisible.value = false
      loadData()
    } catch {
      ElMessage.error('操作失败')
    }
  })
}

// 下载模板（使用 request 携带 Token）
const downloadTemplate = async () => {
  try {
    const res = await request.get('/api/question/template', {
      responseType: 'blob'
    })
    const blob = new Blob([res.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '题目导入模板.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载模板失败')
  }
}

// 上传请求头（携带 Token）
const uploadHeaders = {
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`
}

// 导入成功回调
const handleImportSuccess = (res) => {
  if (res.code === 200) {
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    loadData() // 刷新列表
  } else {
    ElMessage.error(res.message || '导入失败')
  }
}

// 导入失败回调
const handleImportError = () => {
  ElMessage.error('上传失败，请检查文件格式或网络连接')
}

// 加载科目列表（从现有题目中提取）
const loadSubjects = async () => {
  try {
    const res = await request.get('/api/question/subjects')
    subjectList.value = res.data || []
  } catch {
    // 如果接口失败，使用默认科目
    subjectList.value = ['高等数学', '数据结构', '计算机网络', 'Java程序设计']
  }
}

onMounted(() => {
  loadSubjects()
  loadData()
})
</script>

<style scoped>
.question-manage {
  padding: 10px;
}

.search-card {
  margin-bottom: 20px;
}

.difficulty-text {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
