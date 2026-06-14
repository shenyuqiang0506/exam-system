<template>
  <div class="ai-generate-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>AI智能出题</span>
          <el-tag type="success">DeepSeek</el-tag>
        </div>
      </template>

      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="科目名称" prop="subject">
              <el-input v-model="form.subject" placeholder="如：Java程序设计" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="知识点" prop="knowledge">
              <el-input v-model="form.knowledge" placeholder="如：面向对象、多态" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="题型" prop="type">
              <el-select v-model="form.type" placeholder="请选择题型">
                <el-option label="单选题" :value="1" />
                <el-option label="多选题" :value="2" />
                <el-option label="判断题" :value="3" />
                <el-option label="主观题" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度" prop="difficulty">
              <el-rate v-model="form.difficulty" :max="5" show-text :texts="['简单', '较易', '中等', '较难', '困难']" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数量" prop="count">
              <el-input-number v-model="form.count" :min="1" :max="10" :step="1" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" @click="handleGenerate" :loading="loading" :icon="MagicStick">
            AI出题
          </el-button>
          <el-button @click="handleReset" :icon="RefreshRight">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 生成结果 -->
      <template v-if="questions.length > 0">
        <el-divider content-position="left">
          生成结果（共 {{ questions.length }} 题）
        </el-divider>

        <div class="save-bar">
          <el-button type="success" @click="handleSaveAll" :loading="saving" :icon="Collection">
            全部保存到题库
          </el-button>
        </div>

        <div class="questions-list">
          <el-card v-for="(q, index) in questions" :key="index" class="question-card" shadow="hover">
            <div class="question-header">
              <span class="question-index">第 {{ index + 1 }} 题</span>
              <el-tag :type="getTypeTag(form.type)">{{ getTypeName(form.type) }}</el-tag>
            </div>

            <div class="question-content">{{ q.content }}</div>

            <!-- 选项 -->
            <div v-if="q.options && q.options.length > 0" class="question-options">
              <div v-for="(opt, optIndex) in q.options" :key="optIndex" class="option-item">
                <span class="option-label">{{ getOptionLabel(optIndex) }}.</span>
                <span>{{ cleanOption(opt) }}</span>
              </div>
            </div>

            <!-- 答案 -->
            <el-divider content-position="left">答案</el-divider>
            <div class="question-answer">
              <el-tag type="success">正确答案：{{ q.answer }}</el-tag>
            </div>

            <!-- 解析 -->
            <div v-if="q.analysis" class="question-analysis">
              <div class="analysis-label">解析：</div>
              <div class="analysis-content">{{ q.analysis }}</div>
            </div>
          </el-card>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, RefreshRight, Collection } from '@element-plus/icons-vue'
import { aiGenerateQuestions, aiSaveQuestions } from '@/api/ai'

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const questions = ref([])

const form = ref({
  subject: '',
  knowledge: '',
  type: 1,
  difficulty: 3,
  count: 5
})

const rules = {
  subject: [{ required: true, message: '请输入科目名称', trigger: 'blur' }],
  knowledge: [{ required: true, message: '请输入知识点', trigger: 'blur' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  count: [{ required: true, message: '请输入数量', trigger: 'blur' }]
}

// 获取题型名称
const getTypeName = (type) => {
  const map = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '主观题' }
  return map[type] || '未知'
}

// 获取题型标签类型
const getTypeTag = (type) => {
  const map = { 1: 'primary', 2: 'success', 3: 'warning', 4: 'danger' }
  return map[type] || 'info'
}

// 获取选项标签
const getOptionLabel = (index) => {
  return String.fromCharCode(65 + index)
}

// 清理选项前缀（去掉A. B. C. D.等）
const cleanOption = (opt) => {
  if (!opt) return ''
  // 去掉开头的字母前缀，如 "A. xxx" -> "xxx"
  return opt.replace(/^[A-Da-d][.、:：]\s*/, '').trim()
}

// AI出题
const handleGenerate = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  questions.value = []

  try {
    const res = await aiGenerateQuestions(form.value)
    questions.value = res.data
    ElMessage.success(`成功生成 ${questions.value.length} 道题目`)
  } catch (error) {
    console.error('AI出题失败:', error)
  } finally {
    loading.value = false
  }
}

// 全部保存到题库
const handleSaveAll = async () => {
  try {
    await ElMessageBox.confirm(
      `确定将 ${questions.value.length} 道题目保存到题库吗？`,
      '确认保存',
      { type: 'info' }
    )
  } catch {
    return
  }

  saving.value = true

  try {
    const res = await aiSaveQuestions({
      questions: questions.value,
      subjectName: form.value.subject,
      type: form.value.type
    })
    ElMessage.success(`成功保存 ${res.data} 道题目到题库`)
  } catch (error) {
    console.error('保存题目失败:', error)
  } finally {
    saving.value = false
  }
}

// 重置
const handleReset = () => {
  formRef.value.resetFields()
  questions.value = []
}
</script>

<style scoped>
.ai-generate-container {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.save-bar {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-card {
  border-left: 4px solid #409eff;
}

.question-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.question-index {
  font-weight: bold;
  color: #303133;
}

.question-content {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 12px;
}

.question-options {
  padding-left: 20px;
  margin-bottom: 12px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
  line-height: 1.6;
}

.option-label {
  font-weight: bold;
  color: #409eff;
  min-width: 20px;
}

.question-answer {
  margin-bottom: 12px;
}

.question-analysis {
  background-color: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
}

.analysis-label {
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.analysis-content {
  color: #606266;
  line-height: 1.8;
}
</style>
