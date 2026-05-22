<template>
  <div class="paper-preview-edit">
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <div class="left">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <span class="paper-title">{{ paper.title }}</span>
        <el-tag>{{ paper.subjectName }}</el-tag>
        <el-tag type="success">总分：{{ paper.totalScore }} 分</el-tag>
      </div>
      <div class="right">
        <el-button type="primary" @click="handleSave" :loading="saving">
          <el-icon><Check /></el-icon>保存修改
        </el-button>
      </div>
    </div>

    <!-- 试卷内容 -->
    <div class="paper-content">
      <!-- 按题型分组显示 -->
      <div v-for="(group, type) in groupedQuestions" :key="type" class="question-group">
        <h3 class="group-title">{{ getTypeName(Number(type)) }} ({{ group.length }} 题，共 {{ getGroupScore(group) }} 分)</h3>

        <div v-for="(question, index) in group" :key="question.id" class="question-card">
          <div class="question-header">
            <div class="question-info">
              <span class="question-index">{{ index + 1 }}.</span>
              <el-tag :type="getTypeTag(question.type)" size="small">{{ getTypeName(question.type) }}</el-tag>
              <el-input-number
                v-model="question.score"
                :min="1"
                :max="50"
                size="small"
                style="width: 100px; margin-left: 10px"
                @change="handleScoreChange"
              />
              <span class="score-unit">分</span>
            </div>
            <div class="question-actions">
              <el-popconfirm title="确定移除该题目？" @confirm="removeQuestion(question.id)">
                <template #reference>
                  <el-button type="danger" size="small" link>
                    <el-icon><Delete /></el-icon>移除
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>

          <!-- 题目内容（可编辑） -->
          <div class="question-body">
            <el-input
              v-model="question.content"
              type="textarea"
              :rows="3"
              placeholder="题目内容"
              @change="handleContentChange(question)"
            />
          </div>

          <!-- 选项（单选/多选） -->
          <div v-if="question.type === 1 || question.type === 2" class="question-options">
            <div v-for="(opt, optIndex) in parseOptions(question)" :key="optIndex" class="option-item">
              <span class="option-label">{{ opt.value }}.</span>
              <el-input v-model="opt.label" size="small" style="width: 300px" />
            </div>
          </div>

          <!-- 标准答案 -->
          <div class="question-answer">
            <span class="answer-label">标准答案：</span>
            <el-input
              v-model="question.standardAnswer"
              type="textarea"
              :rows="2"
              placeholder="请输入标准答案"
              style="width: 500px"
            />
          </div>

          <!-- 难度 -->
          <div class="question-difficulty">
            <span class="difficulty-label">难度系数：</span>
            <el-slider
              v-model="question.difficulty"
              :min="0.1"
              :max="1.0"
              :step="0.1"
              :format-tooltip="val => val.toFixed(1)"
              style="width: 200px"
            />
          </div>
        </div>
      </div>

      <!-- 添加题目 -->
      <div class="add-question">
        <el-button type="dashed" @click="showAddDialog" style="width: 100%">
          <el-icon><Plus /></el-icon>添加题目
        </el-button>
      </div>
    </div>

    <!-- 添加题目弹窗 -->
    <el-dialog v-model="addDialogVisible" title="添加题目" width="800px">
      <div class="add-question-dialog">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="科目">
            <el-input v-model="paper.subjectName" disabled style="width: 150px" />
          </el-form-item>
          <el-form-item label="题型">
            <el-select v-model="searchForm.type" placeholder="全部" clearable style="width: 120px">
              <el-option label="单选题" :value="1" />
              <el-option label="多选题" :value="2" />
              <el-option label="主观题" :value="4" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadBankQuestions">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="bankQuestions" stripe @selection-change="handleSelectionChange" style="margin-top: 15px">
          <el-table-column type="selection" width="50" />
          <el-table-column prop="id" label="ID" width="100" />
          <el-table-column prop="type" label="题型" width="80">
            <template #default="{ row }">
              <el-tag :type="getTypeTag(row.type)" size="small">{{ getTypeName(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="题目内容" show-overflow-tooltip />
          <el-table-column prop="score" label="分值" width="80" />
          <el-table-column prop="difficulty" label="难度" width="80">
            <template #default="{ row }">{{ row.difficulty?.toFixed(1) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addSelectedQuestions">添加选中题目</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, Plus, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const paperId = route.params.id

const paper = ref({})
const questions = ref([])
const saving = ref(false)
const addDialogVisible = ref(false)
const bankQuestions = ref([])
const selectedQuestions = ref([])
const searchForm = reactive({ type: null })

// 按题型分组
const groupedQuestions = computed(() => {
  const groups = {}
  questions.value.forEach(q => {
    if (!groups[q.type]) groups[q.type] = []
    groups[q.type].push(q)
  })
  return groups
})

const getTypeName = (type) => ({ 1: '单选题', 2: '多选题', 3: '判断题', 4: '主观题' }[type] || '未知')
const getTypeTag = (type) => ({ 1: '', 2: 'warning', 3: 'success', 4: 'danger' }[type] || 'info')

const getGroupScore = (group) => {
  return group.reduce((sum, q) => sum + (q.score || 0), 0)
}

const parseOptions = (question) => {
  try {
    const parsed = JSON.parse(question.content)
    return parsed.options || []
  } catch {
    return []
  }
}

// 加载试卷详情
const loadPaperDetail = async () => {
  try {
    const res = await request.get(`/api/paper/detail/${paperId}`)
    paper.value = res.data.paper || {}
    questions.value = (res.data.questions || []).map(q => ({
      ...q,
      difficulty: q.difficulty || 0.5
    }))
  } catch {
    ElMessage.error('加载试卷失败')
  }
}

// 加载题库题目
const loadBankQuestions = async () => {
  try {
    const res = await request.get('/api/question/page', {
      params: {
        subjectName: paper.value.subjectName,
        type: searchForm.type,
        page: 1,
        size: 100
      }
    })
    // 过滤掉已存在的题目
    const existingIds = new Set(questions.value.map(q => q.id))
    bankQuestions.value = res.data.records.filter(q => !existingIds.has(q.id))
  } catch {
    ElMessage.error('加载题库失败')
  }
}

// 显示添加弹窗
const showAddDialog = () => {
  searchForm.type = null
  loadBankQuestions()
  addDialogVisible.value = true
}

// 选择题目
const handleSelectionChange = (selection) => {
  selectedQuestions.value = selection
}

// 添加选中题目
const addSelectedQuestions = () => {
  questions.value.push(...selectedQuestions.value)
  addDialogVisible.value = false
  ElMessage.success(`已添加 ${selectedQuestions.value.length} 道题目`)
}

// 移除题目
const removeQuestion = (questionId) => {
  questions.value = questions.value.filter(q => q.id !== questionId)
  ElMessage.success('已移除')
}

// 分值变化
const handleScoreChange = () => {
  // 重新计算总分
  paper.value.totalScore = questions.value.reduce((sum, q) => sum + (q.score || 0), 0)
}

// 内容变化
const handleContentChange = (question) => {
  // 可以在这里做自动保存或其他处理
}

// 保存修改
const handleSave = async () => {
  saving.value = true
  try {
    // 更新试卷信息
    await request.put(`/api/paper/${paperId}`, {
      title: paper.value.title,
      totalScore: paper.value.totalScore
    })

    // 更新题目信息
    for (const q of questions.value) {
      await request.post('/api/question/save', {
        id: q.id,
        content: q.content,
        standardAnswer: q.standardAnswer,
        score: q.score,
        difficulty: q.difficulty
      })
    }

    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 返回
const goBack = () => {
  router.back()
}

onMounted(loadPaperDetail)
</script>

<style scoped>
.paper-preview-edit {
  min-height: 100vh;
  background: #f5f7fa;
}

.top-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  padding: 15px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.top-bar .left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.paper-title {
  font-size: 18px;
  font-weight: bold;
}

.paper-content {
  max-width: 1000px;
  margin: 20px auto;
  padding: 0 20px;
}

.question-group {
  margin-bottom: 30px;
}

.group-title {
  font-size: 16px;
  color: #303133;
  border-bottom: 2px solid #409eff;
  padding-bottom: 10px;
  margin-bottom: 20px;
}

.question-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.question-index {
  font-weight: bold;
  font-size: 16px;
  color: #409eff;
}

.score-unit {
  color: #909399;
  font-size: 14px;
}

.question-body {
  margin-bottom: 15px;
}

.question-options {
  margin-bottom: 15px;
  padding-left: 20px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.option-label {
  font-weight: bold;
  width: 30px;
}

.question-answer {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 15px;
}

.answer-label {
  white-space: nowrap;
  font-weight: bold;
  color: #67c23a;
}

.question-difficulty {
  display: flex;
  align-items: center;
  gap: 10px;
}

.difficulty-label {
  white-space: nowrap;
  color: #909399;
}

.add-question {
  margin-top: 20px;
  margin-bottom: 40px;
}

.add-question-dialog {
  max-height: 500px;
  overflow-y: auto;
}
</style>
