<template>
  <div class="paper-list">
    <!-- 操作栏 -->
    <el-card shadow="hover" class="action-card">
      <el-row :gutter="10">
        <el-col :span="6">
          <el-select v-model="searchSubject" placeholder="按科目筛选" clearable style="width: 100%">
            <el-option label="高等数学" value="高等数学" />
            <el-option label="数据结构" value="数据结构" />
            <el-option label="计算机网络" value="计算机网络" />
          </el-select>
        </el-col>
        <el-col :span="18">
          <el-button type="primary" @click="$router.push('/teacher/manual-paper')">
            <el-icon><Plus /></el-icon>
            手动组卷
          </el-button>
          <el-button type="success" @click="$router.push('/teacher/auto-paper')">
            <el-icon><MagicStick /></el-icon>
            智能组卷
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 试卷列表 -->
    <el-card shadow="hover">
      <el-table :data="filteredPapers" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="试卷名称" min-width="180" />
        <el-table-column prop="subjectName" label="科目" width="120" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column prop="targetDifficulty" label="目标难度" width="100">
          <template #default="{ row }">{{ row.targetDifficulty?.toFixed(1) }}</template>
        </el-table-column>
        <el-table-column prop="isArchived" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isArchived === 0 ? 'success' : 'info'">
              {{ row.isArchived === 0 ? '正常' : '已归档' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">查看详情</el-button>
            <el-popconfirm
              v-if="row.isArchived === 0"
              title="确定归档该试卷？"
              @confirm="handleArchive(row.id)"
            >
              <template #reference>
                <el-button type="warning" link>归档</el-button>
              </template>
            </el-popconfirm>
            <el-button type="success" link @click="exportScore(row.id)">
              <el-icon><Download /></el-icon>
              导出成绩
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="试卷详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="试卷名称">{{ currentPaper.title }}</el-descriptions-item>
        <el-descriptions-item label="科目">{{ currentPaper.subjectName }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ currentPaper.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentPaper.isArchived === 0 ? 'success' : 'info'">
            {{ currentPaper.isArchived === 0 ? '正常' : '已归档' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-divider />
      <h4>包含题目 ({{ currentPaper.questionIds?.length || 0 }} 道)</h4>
      <el-table :data="currentPaper.questionIds" stripe size="small" style="margin-top: 10px">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="questionId" label="题目ID" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, MagicStick, Download } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const searchSubject = ref('')
const paperList = ref([])
const detailVisible = ref(false)
const currentPaper = ref({})

const filteredPapers = computed(() => {
  if (!searchSubject.value) return paperList.value
  return paperList.value.filter(p => p.subjectName === searchSubject.value)
})

const loadPapers = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/paper/page', { params: { page: 1, size: 100 } })
    paperList.value = res.data.records
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const handleArchive = async (id) => {
  try {
    await request.put(`/api/paper/archive/${id}`)
    ElMessage.success('归档成功')
    loadPapers()
  } catch { ElMessage.error('归档失败') }
}

const exportScore = (paperId) => {
  window.open(`http://localhost:8080/api/record/export/${paperId}`)
}

const viewDetail = async (row) => {
  try {
    const res = await request.get(`/api/paper/detail/${row.id}`)
    currentPaper.value = { ...row, questionIds: res.data.questionIds?.map(id => ({ questionId: id })) || [] }
    detailVisible.value = true
  } catch { ElMessage.error('获取详情失败') }
}

onMounted(loadPapers)
</script>

<style scoped>
.paper-list { padding: 10px; }
.action-card { margin-bottom: 20px; }
</style>
