<script setup>
import {ref, reactive, computed, onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {Plus, MagicStick, Download} from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const searchSubject = ref('')
const paperList = ref([])
const detailVisible = ref(false)
const editVisible = ref(false)
const currentPaper = ref({})
const editFormRef = ref(null)

const editForm = reactive({
  id: null,
  title: '',
  subjectName: '',
  totalScore: 100,
  examTime: []
})

const editRules = {
  title: [{required: true, message: '请输入试卷名称', trigger: 'blur'}]
}

const filteredPapers = computed(() => {
  if (!searchSubject.value) return paperList.value
  return paperList.value.filter(p => p.subjectName === searchSubject.value)
})

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return timeStr.replace('T', ' ').substring(0, 16)
}

// 获取考试状态
const getStatusText = (paper) => {
  if (!paper.startTime || !paper.endTime) return '无限制'
  const now = new Date()
  const start = new Date(paper.startTime)
  const end = new Date(paper.endTime)
  if (now < start) return '未开始'
  if (now > end) return '已结束'
  return '进行中'
}

const getStatusType = (paper) => {
  const status = getStatusText(paper)
  return {
    '未开始': 'info',
    '进行中': 'success',
    '已结束': 'danger',
    '无限制': 'warning'
  }[status] || 'info'
}

const loadPapers = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/paper/page', {params: {page: 1, size: 100}})
    paperList.value = res.data.records
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleArchive = async (id) => {
  try {
    await request.put(`/api/paper/archive/${id}`)
    ElMessage.success('归档成功')
    loadPapers()
  } catch {
    ElMessage.error('归档失败')
  }
}

const exportScore = (paperId) => {
  window.open(`http://localhost:8080/api/record/export/${paperId}`)
}

const viewDetail = async (row) => {
  try {
    const res = await request.get(`/api/paper/detail/${row.id}`)
    currentPaper.value = {...row, questionIds: res.data.questionIds?.map(id => ({questionId: id})) || []}
    detailVisible.value = true
  } catch {
    ElMessage.error('获取详情失败')
  }
}

// 编辑试卷
const handleEdit = (row) => {
  editForm.id = row.id
  editForm.title = row.title
  editForm.subjectName = row.subjectName
  editForm.totalScore = row.totalScore
  editForm.examTime = row.startTime && row.endTime ? [row.startTime, row.endTime] : []
  editVisible.value = true
}

// 保存编辑
const handleSave = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await request.put(`/api/paper/${editForm.id}`, {
        title: editForm.title,
        totalScore: editForm.totalScore,
        startTime: editForm.examTime?.[0] || null,
        endTime: editForm.examTime?.[1] || null
      })
      ElMessage.success('保存成功')
      editVisible.value = false
      loadPapers()
    } catch {
      ElMessage.error('保存失败')
    } finally {
      saving.value = false
    }
  })
}

// 删除试卷
const handleDelete = async (id) => {
  try {
    await request.delete(`/api/paper/${id}`)
    ElMessage.success('删除成功')
    loadPapers()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(loadPapers)
</script>
