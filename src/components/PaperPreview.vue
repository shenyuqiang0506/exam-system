<template>
  <el-dialog v-model="visible" title="试卷预览" width="800px" @close="handleClose">
    <div class="paper-preview">
      <!-- 试卷头部 -->
      <div class="paper-header">
        <h2>{{ paper.title }}</h2>
        <div class="paper-info">
          <span>科目：{{ paper.subjectName }}</span>
          <span>总分：{{ paper.totalScore }} 分</span>
          <span v-if="paper.startTime">时间：{{ paper.startTime }} ~ {{ paper.endTime }}</span>
        </div>
      </div>

      <!-- 题目列表 -->
      <div v-for="(group, type) in groupedQuestions" :key="type" class="question-group">
        <h3>{{ getTypeName(Number(type)) }} ({{ group.length }} 题)</h3>
        <div v-for="(q, index) in group" :key="q.id" class="question-item">
          <div class="question-title">
            <span class="question-index">{{ index + 1 }}.</span>
            <span class="question-score">({{ q.score }}分)</span>
            <span>{{ getQuestionContent(q) }}</span>
          </div>

          <!-- 单选/多选选项 -->
          <div v-if="q.type === 1 || q.type === 2" class="question-options">
            <div v-for="opt in getOptions(q)" :key="opt.value" class="option">
              {{ opt.value }}. {{ opt.label }}
            </div>
          </div>

          <!-- 主观题答题区 -->
          <div v-if="q.type === 4" class="answer-area">
            <div class="answer-line" v-for="i in 5" :key="i"></div>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="handlePrint">打印</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import request from '@/utils/request'

const props = defineProps({
  modelValue: Boolean,
  paperId: [Number, String]
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const paper = ref({})
const questions = ref([])

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.paperId) {
    loadPaperDetail()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const groupedQuestions = computed(() => {
  const groups = {}
  questions.value.forEach(q => {
    if (!groups[q.type]) {
      groups[q.type] = []
    }
    groups[q.type].push(q)
  })
  return groups
})

const loadPaperDetail = async () => {
  try {
    const res = await request.get(`/api/paper/detail/${props.paperId}`)
    paper.value = res.data.paper || {}
    questions.value = res.data.questions || []
  } catch {
    console.error('加载试卷详情失败')
  }
}

const getTypeName = (type) => {
  return { 1: '一、单选题', 2: '二、多选题', 3: '三、判断题', 4: '四、主观题' }[type] || '未知题型'
}

const getQuestionContent = (q) => {
  if (q.type === 1 || q.type === 2) {
    try {
      const parsed = JSON.parse(q.content)
      return parsed.title || q.content
    } catch {
      return q.content
    }
  }
  return q.content
}

const getOptions = (q) => {
  try {
    const parsed = JSON.parse(q.content)
    return parsed.options || []
  } catch {
    return []
  }
}

const handleClose = () => {
  questions.value = []
  paper.value = {}
}

const handlePrint = () => {
  window.print()
}
</script>

<style scoped>
.paper-preview {
  max-height: 600px;
  overflow-y: auto;
  padding: 20px;
}

.paper-header {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #333;
}

.paper-header h2 {
  font-size: 22px;
  margin-bottom: 10px;
}

.paper-info {
  display: flex;
  justify-content: center;
  gap: 20px;
  color: #666;
  font-size: 14px;
}

.question-group {
  margin-bottom: 25px;
}

.question-group h3 {
  font-size: 16px;
  margin-bottom: 15px;
  color: #333;
  border-bottom: 1px solid #eee;
  padding-bottom: 8px;
}

.question-item {
  margin-bottom: 20px;
  padding-left: 10px;
}

.question-title {
  font-size: 15px;
  line-height: 1.8;
  margin-bottom: 10px;
}

.question-index {
  font-weight: bold;
  margin-right: 5px;
}

.question-score {
  color: #999;
  margin-right: 10px;
}

.question-options {
  padding-left: 25px;
}

.option {
  margin-bottom: 8px;
  font-size: 14px;
}

.answer-area {
  margin-top: 10px;
  padding: 10px;
}

.answer-line {
  border-bottom: 1px solid #ddd;
  height: 30px;
  margin-bottom: 5px;
}
</style>
