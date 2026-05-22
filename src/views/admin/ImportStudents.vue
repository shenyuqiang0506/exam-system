<template>
  <div class="import-students">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>批量导入学生</span>
          <el-button type="primary" link @click="downloadTemplate">
            <el-icon><Download /></el-icon>下载导入模板
          </el-button>
        </div>
      </template>

      <!-- 导入说明 -->
      <el-alert title="导入说明" type="info" :closable="false" style="margin-bottom: 20px">
        <template #default>
          <p>1. 请先下载导入模板，按照模板格式填写数据</p>
          <p>2. 模板格式：学号、真实姓名、班级、手机号、邮箱</p>
          <p>3. 必填字段：学号（作为登录账号）；其他字段可选</p>
          <p>4. <strong>初始密码 = 学号后6位</strong>（如学号 2024001，密码为 400001）</p>
          <p>5. 支持 .xlsx 和 .xls 格式</p>
          <p>6. 学号不能重复</p>
        </template>
      </el-alert>

      <!-- 上传区域 -->
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        :auto-upload="false"
        :limit="1"
        :on-exceed="handleExceed"
        :on-change="handleChange"
        :on-remove="handleRemove"
        accept=".xlsx,.xls"
      >
        <el-icon class="el-icon--upload"><Upload /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            只能上传 xlsx/xls 文件，且不超过 10MB
          </div>
        </template>
      </el-upload>

      <!-- 导入按钮 -->
      <div class="import-actions">
        <el-button type="primary" :loading="loading" :disabled="!file" @click="handleImport">
          <el-icon><Upload /></el-icon>开始导入
        </el-button>
      </div>

      <!-- 导入结果 -->
      <el-card v-if="result" shadow="hover" class="result-card">
        <template #header>
          <span>导入结果</span>
        </template>
        <el-alert :title="result" :type="result.includes('失败') ? 'warning' : 'success'" :closable="false" />
      </el-card>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Download } from '@element-plus/icons-vue'
import request from '@/utils/request'

const uploadRef = ref(null)
const loading = ref(false)
const file = ref(null)
const result = ref(null)

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先移除已选文件')
}

const handleChange = (uploadFile) => {
  file.value = uploadFile.raw
}

const handleRemove = () => {
  file.value = null
}

const downloadTemplate = async () => {
  try {
    const res = await request.get('/api/admin/students/template', { responseType: 'blob' })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '学生导入模板.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('模板下载成功')
  } catch {
    ElMessage.error('模板下载失败')
  }
}

const handleImport = async () => {
  if (!file.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  loading.value = true
  result.value = null

  try {
    const formData = new FormData()
    formData.append('file', file.value)

    const res = await request.post('/api/admin/students/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    result.value = res.data
    ElMessage.success('导入完成')
    file.value = null
    uploadRef.value?.clearFiles()
  } catch (err) {
    result.value = err.message || '导入失败'
    ElMessage.error('导入失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.import-students {
  padding: 10px;
  max-width: 700px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-area {
  width: 100%;
  margin-bottom: 20px;
}

.import-actions {
  text-align: center;
  margin-bottom: 20px;
}

.result-card {
  margin-top: 20px;
}
</style>
