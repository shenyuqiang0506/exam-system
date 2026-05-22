<template>
  <div class="exam-container" v-if="!blocked">
    <div class="exam-header">
      <div class="header-left">
        <span class="paper-title">{{ paperInfo.title }}</span>
        <el-divider direction="vertical" />
        <span class="paper-score">总分：{{ paperInfo.totalScore }} 分</span>
      </div>
      <div class="header-right">
        <span class="countdown-label">剩余时间</span>
        <div class="countdown-time" :class="{ warning: remainingSeconds <= 300 }">
          {{ formattedTime }}
        </div>
      </div>
    </div>

    <div class="exam-body">
      <el-card v-for="(question, index) in questions" :key="question.id" class="question-card" shadow="hover">
        <template #header>
          <div class="question-header">
            <el-tag :type="getTypeTag(question.type)" size="small">{{ getTypeName(question.type) }}</el-tag>
            <span class="question-index">第 {{ index + 1 }} 题</span>
            <span class="question-score">{{ question.score }} 分</span>
          </div>
        </template>
        <div class="question-content">{{ question.content }}</div>

        <div v-if="question.type === 1" class="question-options">
          <el-radio-group v-model="answers[question.id]">
            <el-radio v-for="opt in question.options" :key="opt.value" :value="opt.value" class="option-item">
              {{ opt.value }}. {{ opt.label }}
            </el-radio>
          </el-radio-group>
        </div>

        <div v-else-if="question.type === 2" class="question-options">
          <el-checkbox-group v-model="answers[question.id]">
            <el-checkbox v-for="opt in question.options" :key="opt.value" :label="opt.value" class="option-item">
              {{ opt.value }}. {{ opt.label }}
            </el-checkbox>
          </el-checkbox-group>
        </div>

        <div v-else-if="question.type === 4" class="question-textarea">
          <el-input v-model="answers[question.id]" type="textarea" :rows="4" placeholder="请输入你的作答内容..." />
        </div>
      </el-card>
    </div>

    <div class="exam-footer">
      <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">
        <el-icon><Check /></el-icon>交 卷
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const paperInfo = reactive({ title: '加载中...', totalScore: 100, duration: 120 })
const questions = ref([])
const answers = reactive({})
const submitting = ref(false)
const remainingSeconds = ref(7200)
const recordId = ref(null)
const blocked = ref(false)
let timer = null

const formattedTime = computed(() => {
  const h = Math.floor(remainingSeconds.value / 3600)
  const m = Math.floor((remainingSeconds.value % 3600) / 60)
  const s = remainingSeconds.value % 60
  return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`
})

const getTypeName = (type) => ({1:'单选题',2:'多选题',4:'主观题'}[type]||'未知')
const getTypeTag = (type) => ({1:'',2:'warning',4:'success'}[type]||'info')

const blockAndRedirect = (msg) => {
  blocked.value = true
  ElMessage.warning(msg || '您已完成该试卷考试，不可重复参加')
  setTimeout(() => router.push('/student/exams'), 1500)
}

const loadExamData = async () => {
  try {
    const res = await request.get(`/api/paper/detail/${route.params.paperId}`)

    if (res.data?.ongoingRecord) {
      recordId.value = res.data.ongoingRecord.id
      ElMessage.info('继续上次考试')
    }

    paperInfo.title = res.data.paper.title
    paperInfo.totalScore = res.data.paper.totalScore

    questions.value = (res.data.questions || []).map(q => {
      let displayContent = q.content
      let options = []
      if (q.type === 1 || q.type === 2) {
        try {
          const parsed = JSON.parse(q.content)
          if (parsed && parsed.title) {
            displayContent = parsed.title
            options = parsed.options || []
          }
        } catch (e) {
          displayContent = q.content
        }
      }
      return { ...q, content: displayContent, options }
    })

    questions.value.forEach(q => {
      answers[q.id] = q.type === 2 ? [] : ''
    })
    return true
  } catch (err) {
    // 403 业务错误（通过 error.code 判断）
    if (err.code === 403) {
      blockAndRedirect(err.message)
      return false
    }
    // 其他错误
    ElMessage.error('加载试卷失败')
    router.push('/student/exams')
    return false
  }
}

onMounted(async () => {
  const success = await loadExamData()
  if (!success) return

  timer = setInterval(() => {
    if (remainingSeconds.value > 0) remainingSeconds.value--
    else { clearInterval(timer); ElMessage.warning('时间到！'); doSubmit() }
  }, 1000)
})

onUnmounted(() => { if(timer) clearInterval(timer) })

const handleSubmit = async () => {
  await ElMessageBox.confirm('确定交卷？交卷后不可修改。','交卷确认',{confirmButtonText:'确定',cancelButtonText:'取消',type:'warning'})
  await doSubmit()
}

const doSubmit = async () => {
  submitting.value = true
  try {
    const submitData = {
      paperId: route.params.paperId,
      answers: Object.entries(answers).map(([qId,ans])=>({questionId:qId,answer:Array.isArray(ans)?ans.join(','):ans}))
    }
    const res = await request.post('/api/record/submit', submitData)
    ElMessage.success('交卷成功！')
    router.push(`/student/result/${res.data}`)
  } catch (err) {
    if (err.code === 403) {
      blockAndRedirect(err.message)
    } else {
      ElMessage.error('交卷失败')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.exam-container{min-height:100vh;background:#f5f7fa}
.exam-header{position:fixed;top:0;left:0;right:0;height:60px;background:#fff;box-shadow:0 2px 12px rgba(0,0,0,.1);display:flex;justify-content:space-between;align-items:center;padding:0 30px;z-index:1000}
.header-left{display:flex;align-items:center}
.paper-title{font-size:18px;font-weight:bold;color:#303133}
.paper-score{color:#909399}
.header-right{display:flex;align-items:center;gap:10px}
.countdown-label{font-size:14px;color:#909399}
.countdown-time{font-size:24px;font-weight:bold;color:#67c23a;font-family:monospace}
.countdown-time.warning{color:#f56c6c;animation:blink 1s infinite}
@keyframes blink{50%{opacity:.5}}
.exam-body{padding:80px 20px 100px;max-width:900px;margin:0 auto}
.question-card{margin-bottom:20px}
.question-header{display:flex;align-items:center;gap:12px}
.question-index{font-weight:bold;color:#409eff}
.question-score{margin-left:auto;color:#f56c6c;font-weight:bold}
.question-content{font-size:16px;line-height:1.8;margin-bottom:15px}
.option-item{display:block;margin-bottom:12px}
.exam-footer{position:fixed;bottom:0;left:0;right:0;height:70px;background:#fff;box-shadow:0 -2px 12px rgba(0,0,0,.1);display:flex;justify-content:center;align-items:center;z-index:1000}
.exam-footer .el-button{width:200px}
</style>
