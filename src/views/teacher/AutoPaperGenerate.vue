<template>
  <div class="auto-paper-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><MagicStick /></el-icon>
          <span>遗传算法智能组卷</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="ruleForm"
        :rules="rules"
        label-width="120px"
        size="large"
      >
        <!-- 试卷名称 -->
        <el-form-item label="试卷名称" prop="title">
          <el-input
            v-model="ruleForm.title"
            placeholder="请输入试卷名称"
            clearable
          />
        </el-form-item>

        <!-- 科目名称 -->
        <el-form-item label="科目名称" prop="subjectName">
          <el-select
            v-model="ruleForm.subjectName"
            placeholder="请选择科目"
            style="width: 100%"
          >
            <el-option
              v-for="subject in subjectList"
              :key="subject"
              :label="subject"
              :value="subject"
            />
          </el-select>
        </el-form-item>

        <!-- 试卷总分 -->
        <el-form-item label="试卷总分" prop="totalScore">
          <el-input-number
            v-model="ruleForm.totalScore"
            :min="10"
            :max="200"
            :step="10"
            style="width: 100%"
          />
        </el-form-item>

        <!-- 考试时间 -->
        <el-form-item label="考试时间" prop="examTime">
          <el-date-picker
            v-model="ruleForm.examTime"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <!-- 期望难度 -->
        <el-form-item label="期望难度">
          <el-slider
            v-model="ruleForm.targetDifficulty"
            :min="0.1"
            :max="1.0"
            :step="0.1"
            :format-tooltip="val => val.toFixed(1)"
            show-stops
            style="width: 100%"
          />
        </el-form-item>

        <!-- 单选题配置 -->
        <el-form-item label="单选题">
          <el-row :gutter="20" style="width: 100%">
            <el-col :span="12">
              <el-input-number
                v-model="ruleForm.singleChoiceCount"
                :min="0"
                :max="50"
                placeholder="数量"
                style="width: 100%"
              />
              <span class="unit-label">题数</span>
            </el-col>
            <el-col :span="12">
              <el-input-number
                v-model="ruleForm.singleChoiceScore"
                :min="1"
                :max="10"
                placeholder="单题分值"
                style="width: 100%"
              />
              <span class="unit-label">分/题</span>
            </el-col>
          </el-row>
        </el-form-item>

        <!-- 多选题配置 -->
        <el-form-item label="多选题">
          <el-row :gutter="20" style="width: 100%">
            <el-col :span="12">
              <el-input-number
                v-model="ruleForm.multiChoiceCount"
                :min="0"
                :max="50"
                placeholder="数量"
                style="width: 100%"
              />
              <span class="unit-label">题数</span>
            </el-col>
            <el-col :span="12">
              <el-input-number
                v-model="ruleForm.multiChoiceScore"
                :min="1"
                :max="10"
                placeholder="单题分值"
                style="width: 100%"
              />
              <span class="unit-label">分/题</span>
            </el-col>
          </el-row>
        </el-form-item>

        <!-- 主观题配置 -->
        <el-form-item label="主观题">
          <el-row :gutter="20" style="width: 100%">
            <el-col :span="12">
              <el-input-number
                v-model="ruleForm.subjectiveCount"
                :min="0"
                :max="10"
                placeholder="数量"
                style="width: 100%"
              />
              <span class="unit-label">题数</span>
            </el-col>
            <el-col :span="12">
              <el-input-number
                v-model="ruleForm.subjectiveScore"
                :min="1"
                :max="30"
                placeholder="单题分值"
                style="width: 100%"
              />
              <span class="unit-label">分/题</span>
            </el-col>
          </el-row>
        </el-form-item>

        <!-- 分值汇总 -->
        <el-form-item label="分值汇总">
          <el-tag type="info" size="large">
            单选 {{ ruleForm.singleChoiceCount * ruleForm.singleChoiceScore }} 分
            + 多选 {{ ruleForm.multiChoiceCount * ruleForm.multiChoiceScore }} 分
            + 主观 {{ ruleForm.subjectiveCount * ruleForm.subjectiveScore }} 分
            = 总计 {{ totalCalcScore }} 分
          </el-tag>
        </el-form-item>

        <!-- 提交按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleSubmit"
            size="large"
            style="width: 100%"
          >
            <el-icon><MagicStick /></el-icon>
            开始智能生成
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import request from '@/utils/request'

const formRef = ref(null)
const loading = ref(false)
const subjectList = ref([])

const ruleForm = reactive({
  title: '',
  subjectName: '',
  totalScore: 100,
  targetDifficulty: 0.5,
  singleChoiceCount: 10,
  singleChoiceScore: 2,
  multiChoiceCount: 5,
  multiChoiceScore: 4,
  subjectiveCount: 3,
  subjectiveScore: 10,
  examTime: []
})

const rules = reactive({
  title: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  subjectName: [{ required: true, message: '请选择科目', trigger: 'change' }],
  totalScore: [{ required: true, message: '请设置试卷总分', trigger: 'blur' }]
})

const totalCalcScore = computed(() => {
  return ruleForm.singleChoiceCount * ruleForm.singleChoiceScore +
         ruleForm.multiChoiceCount * ruleForm.multiChoiceScore +
         ruleForm.subjectiveCount * ruleForm.subjectiveScore
})

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const submitData = {
        ...ruleForm,
        startTime: ruleForm.examTime?.[0] || null,
        endTime: ruleForm.examTime?.[1] || null
      }
      await request.post('/api/paper/auto-create', submitData)
      ElMessage.success('智能组卷成功！')
      formRef.value.resetFields()
    } catch (err) {
      ElMessage.error('组卷失败，请检查题库是否充足')
    } finally {
      loading.value = false
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
  loadSubjects()
})
</script>

<style scoped>
.auto-paper-container {
  max-width: 700px;
  margin: 30px auto;
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: bold;
}

.unit-label {
  display: block;
  text-align: center;
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
</style>
