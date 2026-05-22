<template>
  <div class="result-container">
    <el-result :icon="record.status===1?'success':'warning'" :title="record.status===1?'批阅完成':'批阅中'">
      <template #icon>
        <div class="score-circle">
          <span class="score-number">{{ record.totalScore || 0 }}</span>
          <span class="score-unit">分</span>
        </div>
      </template>
      <template #sub-title><p class="sub-title">你的最终得分</p></template>
    </el-result>

    <el-card class="summary-card" shadow="hover">
      <el-descriptions title="得分汇总" :column="2" border>
        <el-descriptions-item label="客观题得分"><span class="score-text">{{ record.objectiveScore || 0 }} 分</span>
        </el-descriptions-item>
        <el-descriptions-item label="主观题得分"><span class="score-text">{{ record.subjectiveScore || 0 }} 分</span>
        </el-descriptions-item>
        <el-descriptions-item label="考试状态">
          <el-tag :type="record.status===1?'success':'warning'">{{ record.status === 1 ? '已批阅' : '批阅中' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="试卷ID">{{ record.paperId }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="detail-card" shadow="hover">
      <template #header>
        <div class="detail-header">
          <el-icon>
            <Document/>
          </el-icon>
          <span>答题明细</span></div>
      </template>
      <el-collapse v-model="activeNames" accordion>
        <el-collapse-item v-for="(item,index) in details" :key="item.id" :name="index">
          <template #title>
            <div class="collapse-title">
              <el-tag :type="getTypeTag(item.questionType)" size="small" style="margin-right:10px">
                {{ getTypeName(item.questionType) }}
              </el-tag>
              <span class="title-content">{{ truncate(item.questionContent, 40) }}</span>
              <span class="title-score">{{ item.score }} / {{ item.fullScore }} 分</span>
            </div>
          </template>
          <div class="detail-body">
            <div class="detail-section">
              <h4>
                <el-icon>
                  <Document/>
                </el-icon>
                题目内容
              </h4>
              <p class="section-content">{{ item.questionContent }}</p></div>
            <div class="detail-section">
              <h4>
                <el-icon>
                  <Edit/>
                </el-icon>
                你的作答
              </h4>
              <p class="section-content student-answer">{{ item.studentAnswer || '未作答' }}</p></div>
            <div class="detail-section">
              <h4>
                <el-icon>
                  <CircleCheck/>
                </el-icon>
                标准答案
              </h4>
              <p class="section-content standard-answer">{{ item.standardAnswer }}</p></div>
            <div v-if="item.aiReason" class="detail-section">
              <h4>
                <el-icon>
                  <Cpu/>
                </el-icon>
                AI判分依据
              </h4>
              <el-alert :title="item.aiReason" type="info" :closable="false" show-icon/>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {useRoute} from 'vue-router'
import {Document, Edit, CircleCheck, Cpu} from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const activeNames = ref(0)
const record = reactive({})
const details = ref([])

const getTypeName = (t) => ({1: '单选题', 2: '多选题', 3: '判断题', 4: '主观题'}[t] || '未知')
const getTypeTag = (t) => ({1: '', 2: 'warning', 3: 'success', 4: 'danger'}[t] || 'info')
const truncate = (s, l) => s?.length > l ? s.substring(0, l) + '...' : s

const loadDetail = async () => {
  try {
    const res = await request.get(`/api/record/detail/${route.params.recordId}`)
    Object.assign(record, res.data.record)
    details.value = res.data.details || []
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.result-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px
}

.score-circle {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: linear-gradient(135deg, #67c23a, #409eff);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
  box-shadow: 0 8px 24px rgba(64, 158, 255, .3)
}

.score-number {
  font-size: 42px;
  font-weight: bold
}

.score-unit {
  font-size: 14px;
  opacity: .9
}

.sub-title {
  color: #909399;
  margin-top: 10px
}

.summary-card, .detail-card {
  margin-top: 20px
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold
}

.score-text {
  font-size: 18px;
  font-weight: bold;
  color: #409eff
}

.collapse-title {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 10px
}

.title-content {
  flex: 1;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap
}

.title-score {
  font-weight: bold;
  color: #f56c6c;
  white-space: nowrap;
  margin-left: auto
}

.detail-body {
  padding: 10px 20px
}

.detail-section {
  margin-bottom: 20px
}

.detail-section:last-child {
  margin-bottom: 0
}

.detail-section h4 {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 10px 0;
  padding-left: 10px;
  border-left: 3px solid #409eff;
  color: #303133
}

.section-content {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 6px;
  line-height: 1.8;
  margin: 0
}

.student-answer {
  background: #fdf6ec;
  border-left: 3px solid #e6a23c
}

.standard-answer {
  background: #f0f9eb;
  border-left: 3px solid #67c23a
}
</style>
