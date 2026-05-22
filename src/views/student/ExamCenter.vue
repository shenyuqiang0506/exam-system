<template>
  <div class="exam-center">
    <!-- 顶部统计 -->
    <div class="stats-bar">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <el-statistic title="活跃题目集" :value="activeCount">
              <template #icon>
                <el-icon style="color: #67c23a">
                  <VideoPlay/>
                </el-icon>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <el-statistic title="待完成" :value="pendingCount">
              <template #icon>
                <el-icon style="color: #e6a23c">
                  <Clock/>
                </el-icon>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <el-statistic title="已完成" :value="completedCount">
              <template #icon>
                <el-icon style="color: #409eff">
                  <CircleCheck/>
                </el-icon>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="exam-tabs">
      <el-tab-pane name="active">
        <template #label>
          <span class="tab-label">
            <el-icon><VideoPlay/></el-icon>
            活跃题目集
            <el-badge :value="activeCount" :max="99" class="badge"/>
          </span>
        </template>
        <ExamList :papers="activePapers" type="active" @refresh="loadActivePapers"/>
      </el-tab-pane>

      <el-tab-pane name="all">
        <template #label>
          <span class="tab-label">
            <el-icon><List/></el-icon>
            所有题目集
          </span>
        </template>
        <ExamList :papers="allPapers" type="all" @refresh="loadAllPapers"/>
        <!-- 分页组件 -->
        <div class="pagination-wrapper">
          <el-pagination
              v-model:current-page="pagination.page"
              v-model:page-size="pagination.size"
              :total="pagination.total"
              :page-sizes="[5, 10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadAllPapers"
              @current-change="loadAllPapers"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {VideoPlay, List, CircleCheck, Clock} from '@element-plus/icons-vue'
import request from '@/utils/request'
import ExamList from './ExamList.vue'

const activeTab = ref('active')
const activePapers = ref([])
const allPapers = ref([])
const loading = ref(false)

// 分页参数
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const activeCount = computed(() => activePapers.value.length)
const pendingCount = computed(() => allPapers.value.filter(p => p.status === '未开始').length)
const completedCount = computed(() => allPapers.value.filter(p => p.myScore !== null).length)

// 加载活跃题目集
const loadActivePapers = async () => {
  try {
    const res = await request.get('/api/paper/active')
    activePapers.value = res.data || []
  } catch (err) {
    ElMessage.error('加载活跃题目集失败')
  }
}

// 加载所有题目集（分页）
const loadAllPapers = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/paper/all', {
      params: {
        page: pagination.page,
        size: pagination.size
      }
    })
    allPapers.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (err) {
    ElMessage.error('加载所有题目集失败')
  } finally {
    loading.value = false
  }
}

// 加载所有数据
const loadData = async () => {
  await Promise.all([loadActivePapers(), loadAllPapers()])
}

const handleTabChange = (tab) => {
  // Tab 切换时可以重新加载数据
}

onMounted(loadData)
</script>

<style scoped>
.exam-center {
  padding: 10px;
}

.stats-bar {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-card :deep(.el-statistic__head) {
  font-size: 14px;
}

.stat-card :deep(.el-statistic__content) {
  font-size: 28px;
}

.exam-tabs {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.badge {
  margin-left: 5px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding: 10px 0;
}
</style>
