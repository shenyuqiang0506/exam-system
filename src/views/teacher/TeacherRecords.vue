<template>
  <div class="teacher-records">
    <!-- 筛选栏 -->
    <el-card shadow="hover" style="margin-bottom: 16px">
      <el-form inline>
        <el-form-item label="选择试卷">
          <el-select
            v-model="selectedPaperId"
            placeholder="请选择试卷"
            style="width: 280px"
            clearable
            @change="loadRecords"
          >
            <el-option
              v-for="paper in paperList"
              :key="paper.id"
              :label="paper.title"
              :value="paper.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="success"
            :disabled="!selectedPaperId"
            :loading="exportLoading"
            @click="handleExport"
          >
            <el-icon><Download /></el-icon>
            导出 Excel
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 成绩表格 -->
    <el-card shadow="hover">
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center">
          <span>成绩列表</span>
          <el-tag v-if="selectedPaperId" type="info">共 {{ recordList.length }} 条记录</el-tag>
        </div>
      </template>

      <el-table
        v-loading="tableLoading"
        :data="recordList"
        stripe
        border
        empty-text="请先选择试卷"
      >
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="studentId" label="学生ID" width="180" align="center" />
        <el-table-column label="总得分" align="center">
          <template #default="{ row }">
            <el-tag :type="getScoreTag(row.totalScore)" size="large">
              {{ row.totalScore ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="objectiveScore" label="客观题得分" align="center" />
        <el-table-column prop="subjectiveScore" label="主观题得分" align="center" />
        <el-table-column label="状态" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '已批阅' : '待批阅' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="考试时间" align="center" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import request from '@/utils/request'

const selectedPaperId = ref(null)
const paperList = ref([])
const recordList = ref([])
const tableLoading = ref(false)
const exportLoading = ref(false)

// 加载所有试卷供选择
const loadPapers = async () => {
  try {
    const res = await request.get('/api/paper/page', { params: { page: 1, size: 100 } })
    paperList.value = res.data.records || []
  } catch {
    ElMessage.error('加载试卷列表失败')
  }
}

// 加载选中试卷的成绩
const loadRecords = async () => {
  if (!selectedPaperId.value) {
    recordList.value = []
    return
  }
  tableLoading.value = true
  try {
    const res = await request.get(`/api/record/list/${selectedPaperId.value}`)
    recordList.value = res.data || []
  } catch {
    ElMessage.error('加载成绩失败')
  } finally {
    tableLoading.value = false
  }
}

// 导出 Excel
const handleExport = async () => {
  if (!selectedPaperId.value) return
  exportLoading.value = true
  try {
    const response = await request.get(`/api/record/export/${selectedPaperId.value}`, {
      responseType: 'blob'
    })
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    const paper = paperList.value.find(p => p.id === selectedPaperId.value)
    link.href = url
    link.download = `${paper?.title || '成绩表'}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

// 根据分数返回标签颜色
const getScoreTag = (score) => {
  if (score === null || score === undefined) return 'info'
  const s = Number(score)
  if (s >= 90) return 'success'
  if (s >= 60) return ''
  return 'danger'
}

onMounted(() => {
  loadPapers()
})
</script>

<style scoped>
.teacher-records {
  padding: 10px;
}
</style>
