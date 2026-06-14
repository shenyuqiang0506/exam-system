<template>
  <div class="ai-logs-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>AI判分日志</span>
          <el-button type="primary" :icon="Refresh" @click="fetchLogs">刷新</el-button>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :shortcuts="dateShortcuts"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchLogs">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志列表 -->
      <el-table :data="logs" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="questionId" label="题目ID" width="120" />
        <el-table-column prop="aiScore" label="AI评分" width="100">
          <template #default="{ row }">
            <el-tag :type="getScoreType(row.aiScore)">{{ row.aiScore }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="aiReason" label="判分依据" min-width="200" show-overflow-tooltip />
        <el-table-column prop="model" label="AI模型" width="150" />
        <el-table-column prop="createTime" label="判分时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-if="total > 0"
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        @current-change="fetchLogs"
        @size-change="fetchLogs"
      />

      <!-- 空状态 -->
      <el-empty v-if="!loading && logs.length === 0" description="暂无判分日志" />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="判分详情" width="700px">
      <template v-if="currentLog">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="日志ID">{{ currentLog.id }}</el-descriptions-item>
          <el-descriptions-item label="考试记录ID">{{ currentLog.recordId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="题目ID">{{ currentLog.questionId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="AI模型">{{ currentLog.model }}</el-descriptions-item>
          <el-descriptions-item label="AI评分">
            <el-tag :type="getScoreType(currentLog.aiScore)" size="large">{{ currentLog.aiScore }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="判分时间">{{ currentLog.createTime }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">学生答案</el-divider>
        <div class="answer-box">{{ currentLog.studentAnswer || '无' }}</div>

        <el-divider content-position="left">标准答案</el-divider>
        <div class="answer-box">{{ currentLog.standardAnswer || '无' }}</div>

        <el-divider content-position="left">判分依据</el-divider>
        <div class="reason-box">{{ currentLog.aiReason || '无' }}</div>

        <el-divider content-position="left">关键点分析</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="keyword-section">
              <div class="keyword-label">答对的关键点：</div>
              <el-tag v-for="(item, index) in parseKeywords(currentLog.keywords)" :key="index" type="success" class="keyword-tag">
                {{ item }}
              </el-tag>
              <span v-if="!currentLog.keywords" class="empty-text">无</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="keyword-section">
              <div class="keyword-label">遗漏的关键点：</div>
              <el-tag v-for="(item, index) in parseKeywords(currentLog.missing)" :key="index" type="warning" class="keyword-tag">
                {{ item }}
              </el-tag>
              <span v-if="!currentLog.missing" class="empty-text">无</span>
            </div>
          </el-col>
        </el-row>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const dateRange = ref(null)
const detailVisible = ref(false)
const currentLog = ref(null)

// 日期快捷选项
const dateShortcuts = [
  {
    text: '最近一周',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 7 * 24 * 3600 * 1000)
      return [start, end]
    }
  },
  {
    text: '最近一个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 30 * 24 * 3600 * 1000)
      return [start, end]
    }
  }
]

// 获取评分类型
const getScoreType = (score) => {
  if (score >= 9) return 'success'
  if (score >= 6) return 'primary'
  if (score >= 3) return 'warning'
  return 'danger'
}

// 解析关键词JSON
const parseKeywords = (json) => {
  if (!json) return []
  try {
    return JSON.parse(json)
  } catch {
    return []
  }
}

// 获取日志列表
const fetchLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0].toISOString()
      params.endTime = dateRange.value[1].toISOString()
    }
    
    // 注意：后端需要实现这个接口
    const res = await request.get('/api/ai/logs', { params })
    logs.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取日志失败:', error)
    // 如果接口不存在，显示空状态
    logs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 重置查询
const handleReset = () => {
  dateRange.value = null
  currentPage.value = 1
  fetchLogs()
}

// 显示详情
const showDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(() => {
  fetchLogs()
})
</script>

<style scoped>
.ai-logs-container {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.answer-box,
.reason-box {
  background-color: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

.keyword-section {
  margin-bottom: 12px;
}

.keyword-label {
  font-weight: bold;
  margin-bottom: 8px;
  color: #303133;
}

.keyword-tag {
  margin: 4px;
}

.empty-text {
  color: #909399;
  font-size: 14px;
}
</style>
