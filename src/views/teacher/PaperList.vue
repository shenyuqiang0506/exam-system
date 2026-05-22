<template>
  <div class="paper-list">
    <el-card shadow="hover" class="action-card">
      <el-row :gutter="10">
        <el-col :span="6">
          <el-select v-model="searchSubject" placeholder="按科目筛选" clearable style="width: 100%">
            <el-option label="高等数学" value="高等数学" />
            <el-option label="数据结构" value="数据结构" />
            <el-option label="计算机网络" value="计算机网络" />
          </el-select>
        </el-col>
        <el-col :span="18">
          <el-button type="primary" @click="$router.push('/teacher/manual-paper')">
            <el-icon><Plus /></el-icon>手动组卷
          </el-button>
          <el-button type="success" @click="$router.push('/teacher/auto-paper')">
            <el-icon><MagicStick /></el-icon>智能组卷
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="hover">
      <el-table :data="filteredPapers" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="试卷名称" min-width="180" />
        <el-table-column prop="subjectName" label="科目" width="100" />
        <el-table-column prop="totalScore" label="总分" width="70" />
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="160">
          <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" size="small">{{ getStatusText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
            <el-button type="info" link @click="handlePreview(row)">预览</el-button>
            <el-button type="info" link @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm v-if="row.isArchived === 0" title="确定归档？" @confirm="handleArchive(row.id)">
              <template #reference><el-button type="warning" link>归档</el-button></template>
            </el-popconfirm>
            <el-button type="success" link @click="exportScore(row.id)">成绩</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference><el-button type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <PaperPreview v-model="previewVisible" :paper-id="previewPaperId" />

    <el-dialog v-model="detailVisible" title="试卷详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="试卷名称">{{ currentPaper.title }}</el-descriptions-item>
        <el-descriptions-item label="科目">{{ currentPaper.subjectName }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ currentPaper.totalScore }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑试卷" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="试卷名称" prop="title">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="总分" prop="totalScore">
          <el-input-number v-model="editForm.totalScore" :min="0" :max="200" style="width: 100%" />
        </el-form-item>
        <el-form-item label="考试时间">
          <el-date-picker v-model="editForm.examTime" type="datetimerange" range-separator="至"
            start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {Plus, MagicStick, Download} from '@element-plus/icons-vue'
import request from '@/utils/request'
import PaperPreview from '@/components/PaperPreview.vue'

const loading = ref(false)
const saving = ref(false)
const searchSubject = ref('')
const paperList = ref([])
const detailVisible = ref(false)
const editVisible = ref(false)
const previewVisible = ref(false)
const previewPaperId = ref(null)
const currentPaper = ref({})
const editFormRef = ref(null)

const editForm = reactive({id: null, title: '', subjectName: '', totalScore: 100, examTime: []})
const editRules = {title: [{required: true, message: '请输入试卷名称', trigger: 'blur'}]}

const filteredPapers = computed(() => {
  if (!searchSubject.value) return paperList.value
  return paperList.value.filter(p => p.subjectName === searchSubject.value)
})

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : '-'

const getStatusText = (p) => {
  if (!p.startTime || !p.endTime) return '无限制'
  const now = new Date()
  if (now < new Date(p.startTime)) return '未开始'
  if (now > new Date(p.endTime)) return '已结束'
  return '进行中'
}

const getStatusType = (p) => ({'未开始':'info','进行中':'success','已结束':'danger','无限制':'warning'}[getStatusText(p)])

const loadPapers = async () => {
  loading.value = true
  try { paperList.value = (await request.get('/api/paper/page', {params:{page:1,size:100}})).data.records }
  catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const handleArchive = async (id) => {
  try { await request.put(`/api/paper/archive/${id}`); ElMessage.success('归档成功'); loadPapers() }
  catch { ElMessage.error('归档失败') }
}

const exportScore = (id) => window.open(`http://localhost:8080/api/record/export/${id}`)

const viewDetail = async (row) => {
  try {
    const res = await request.get(`/api/paper/detail/${row.id}`)
    currentPaper.value = {...row, questionIds: res.data.questionIds?.map(id => ({questionId: id})) || []}
    detailVisible.value = true
  } catch { ElMessage.error('获取详情失败') }
}

const handlePreview = (row) => { previewPaperId.value = row.id; previewVisible.value = true }

const handleEdit = (row) => {
  editForm.id = row.id; editForm.title = row.title; editForm.subjectName = row.subjectName
  editForm.totalScore = row.totalScore; editForm.examTime = row.startTime && row.endTime ? [row.startTime, row.endTime] : []
  editVisible.value = true
}

const handleSave = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await request.put(`/api/paper/${editForm.id}`, {title: editForm.title, totalScore: editForm.totalScore, startTime: editForm.examTime?.[0], endTime: editForm.examTime?.[1]})
      ElMessage.success('保存成功'); editVisible.value = false; loadPapers()
    } catch { ElMessage.error('保存失败') }
    finally { saving.value = false }
  })
}

const handleDelete = async (id) => {
  try { await request.delete(`/api/paper/${id}`); ElMessage.success('删除成功'); loadPapers() }
  catch { ElMessage.error('删除失败') }
}

onMounted(loadPapers)
</script>

<style scoped>
.paper-list { padding: 10px; }
.action-card { margin-bottom: 20px; }
</style>
