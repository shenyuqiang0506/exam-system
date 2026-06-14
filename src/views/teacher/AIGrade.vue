<template>
  <div class="ai-grade-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>AI智能判分</span>
          <el-tag type="success">DeepSeek</el-tag>
        </div>
      </template>

      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="题目内容" prop="question">
          <el-input v-model="form.question" type="textarea" :rows="3" placeholder="请输入题目内容" />
        </el-form-item>

        <el-form-item label="标准答案" prop="standardAnswer">
          <el-input v-model="form.standardAnswer" type="textarea" :rows="3" placeholder="请输入标准答案" />
        </el-form-item>

        <el-form-item label="学生答案" prop="studentAnswer">
          <el-input v-model="form.studentAnswer" type="textarea" :rows="3" placeholder="请输入学生答案" />
        </el-form-item>

        <el-form-item label="满分" prop="fullScore">
          <el-input-number v-model="form.fullScore" :min="1" :max="100" :step="1" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleGrade" :loading="loading" :icon="MagicStick">
            AI判分
          </el-button>
          <el-button @click="handleReset" :icon="RefreshRight">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 判分结果 -->
      <template v-if="result">
        <el-divider content-position="left">判分结果</el-divider>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-statistic title="AI评分" :value="result.score" :precision="1">
              <template #suffix>
                <span class="score-suffix">/ {{ form.fullScore }}</span>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="8">
            <el-statistic title="得分率" :value="scoreRate" :precision="1" suffix="%" />
          </el-col>
          <el-col :span="8">
            <div class="score-level">
              <span class="label">评级</span>
              <el-tag :type="scoreLevel.type" size="large">{{ scoreLevel.text }}</el-tag>
            </div>
          </el-col>
        </el-row>

        <el-descriptions :column="1" border class="result-descriptions">
          <el-descriptions-item label="判分依据">
            <div class="reason-text">{{ result.reason }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <el-row :gutter="20" class="keywords-row">
          <el-col :span="12">
            <el-card shadow="hover" class="keyword-card success-card">
              <template #header>
                <span>答对的关键点</span>
              </template>
              <el-tag v-for="(item, index) in result.keywords" :key="index" type="success" class="keyword-tag">
                {{ item }}
              </el-tag>
              <el-empty v-if="!result.keywords || result.keywords.length === 0" description="无" :image-size="60" />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover" class="keyword-card warning-card">
              <template #header>
                <span>遗漏的关键点</span>
              </template>
              <el-tag v-for="(item, index) in result.missing" :key="index" type="warning" class="keyword-tag">
                {{ item }}
              </el-tag>
              <el-empty v-if="!result.missing || result.missing.length === 0" description="无" :image-size="60" />
            </el-card>
          </el-col>
        </el-row>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, RefreshRight } from '@element-plus/icons-vue'
import { aiGrade } from '@/api/ai'

const formRef = ref(null)
const loading = ref(false)
const result = ref(null)

const form = ref({
  question: '',
  standardAnswer: '',
  studentAnswer: '',
  fullScore: 10
})

const rules = {
  question: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  standardAnswer: [{ required: true, message: '请输入标准答案', trigger: 'blur' }],
  studentAnswer: [{ required: true, message: '请输入学生答案', trigger: 'blur' }],
  fullScore: [{ required: true, message: '请输入满分', trigger: 'blur' }]
}

// 得分率
const scoreRate = computed(() => {
  if (!result.value || !form.value.fullScore) return 0
  return (result.value.score / form.value.fullScore) * 100
})

// 评级
const scoreLevel = computed(() => {
  const rate = scoreRate.value
  if (rate >= 90) return { type: 'success', text: '优秀' }
  if (rate >= 80) return { type: 'primary', text: '良好' }
  if (rate >= 60) return { type: 'warning', text: '及格' }
  return { type: 'danger', text: '不及格' }
})

// AI判分
const handleGrade = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  result.value = null

  try {
    const res = await aiGrade(form.value)
    result.value = res.data
    ElMessage.success('AI判分完成')
  } catch (error) {
    console.error('AI判分失败:', error)
  } finally {
    loading.value = false
  }
}

// 重置
const handleReset = () => {
  formRef.value.resetFields()
  result.value = null
}
</script>

<style scoped>
.ai-grade-container {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-descriptions {
  margin-top: 20px;
}

.reason-text {
  line-height: 1.8;
  color: #606266;
}

.score-suffix {
  font-size: 14px;
  color: #909399;
}

.score-level {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.score-level .label {
  font-size: 14px;
  color: #909399;
}

.keywords-row {
  margin-top: 20px;
}

.keyword-card {
  height: 100%;
}

.success-card {
  border-color: #67c23a;
}

.warning-card {
  border-color: #e6a23c;
}

.keyword-tag {
  margin: 4px;
}
</style>
