<template>
  <div class="manual-paper">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>手动组卷</span>
        </div>
      </template>

      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="试卷名称" prop="title">
              <el-input v-model="formData.title" placeholder="请输入试卷名称"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="科目" prop="subjectName">
              <el-select
                  v-model="formData.subjectName"
                  placeholder="请选择科目"
                  style="width: 100%"
                  @change="loadQuestions"
              >
                <el-option
                    v-for="subject in subjectList"
                    :key="subject"
                    :label="subject"
                    :value="subject"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 考试时间 -->
        <el-form-item label="考试时间" prop="examTime">
          <el-date-picker
              v-model="formData.examTime"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>

        <!-- 题目选择区域 -->
        <el-form-item label="选择题目">
          <el-transfer
              v-model="formData.questionIds"
              :data="questionData"
              :titles="['题库列表', '已选题目']"
              :props="{ key: 'id', label: 'displayContent' }"
              filterable
              filter-placeholder="搜索题目"
              @change="handleTransferChange"
          >
            <template #default="{ option }">
              <div class="transfer-item">
                <el-tag :type="getTypeTag(option.type)" size="small" style="margin-right: 8px">
                  {{ getTypeName(option.type) }}
                </el-tag>
                <span>{{ option.content?.substring(0, 40) }}...</span>
                <span class="item-score">{{ option.score }}分</span>
              </div>
            </template>
          </el-transfer>
        </el-form-item>

        <!-- 统计信息 -->
        <el-form-item label="组卷统计">
          <el-descriptions :column="4" border size="small">
            <el-descriptions-item label="单选题">{{ stats.singleChoice }} 道</el-descriptions-item>
            <el-descriptions-item label="多选题">{{ stats.multiChoice }} 道</el-descriptions-item>
            <el-descriptions-item label="主观题">{{ stats.subjective }} 道</el-descriptions-item>
            <el-descriptions-item label="总分">{{ stats.totalScore }} 分</el-descriptions-item>
          </el-descriptions>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="loading">
            <el-icon>
              <Check/>
            </el-icon>
            创建试卷
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {Check} from '@element-plus/icons-vue'
import request from '@/utils/request'
import {useRouter} from 'vue-router'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const questionData = ref([])
const subjectList = ref([])

const formData = reactive({
  title: '',
  subjectName: '',
  questionIds: [],
  examTime: []
})

const rules = {
  title: [{required: true, message: '请输入试卷名称', trigger: 'blur'}],
  subjectName: [{required: true, message: '请选择科目', trigger: 'change'}]
}

const getTypeName = (type) => ({1: '单选', 2: '多选', 3: '判断', 4: '主观'}[type])
const getTypeTag = (type) => ({1: '', 2: 'warning', 3: 'success', 4: 'danger'}[type])

// 统计已选题目
const stats = computed(() => {
  const selected = questionData.value.filter(q => formData.questionIds.includes(q.id))
  return {
    singleChoice: selected.filter(q => q.type === 1).length,
    multiChoice: selected.filter(q => q.type === 2).length,
    subjective: selected.filter(q => q.type === 4).length,
    totalScore: selected.reduce((sum, q) => sum + (q.score || 0), 0)
  }
})

const loadQuestions = async () => {
  if (!formData.subjectName) {
    questionData.value = []
    return
  }
  try {
    const res = await request.get('/api/question/page', {
      params: {subjectName: formData.subjectName, page: 1, size: 100}
    })
    questionData.value = res.data.records.map(q => {
      let title = q.content || ''
      if (q.type === 1 || q.type === 2) {
        try {
          const parsed = JSON.parse(q.content)
          if (parsed && parsed.title) title = parsed.title
        } catch (e) { /* 纯文本，不处理 */
        }
      }
      return {
        ...q,
        displayContent: `[${getTypeName(q.type)}] ${title.substring(0, 30)}`
      }
    })
  } catch {
    ElMessage.error('加载题目失败')
  }
}

const handleTransferChange = () => {
  // 穿梭框变化时触发
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (formData.questionIds.length === 0) {
      ElMessage.warning('请至少选择一道题目')
      return
    }
    loading.value = true
    try {
      const paperData = {
        title: formData.title,
        subjectName: formData.subjectName,
        startTime: formData.examTime?.[0] || null,
        endTime: formData.examTime?.[1] || null
      }
      await request.post('/api/paper/manual-create', {
        paper: paperData,
        questionIds: formData.questionIds
      })
      ElMessage.success('创建成功')
      router.push('/teacher/papers')
    } catch {
      ElMessage.error('创建失败')
    } finally {
      loading.value = false
    }
  })
}

const resetForm = () => {
  formRef.value?.resetFields()
  questionData.value = []
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
  loadSubjects()
})
</script>

<style scoped>
.manual-paper {
  padding: 10px;
  max-width: 1000px;
  margin: 0 auto;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}

.transfer-item {
  display: flex;
  align-items: center;
}

.item-score {
  margin-left: auto;
  color: #f56c6c;
  font-size: 12px;
}
</style>
