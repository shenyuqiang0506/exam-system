<template>
  <div class="profile-container">
    <el-card shadow="hover" class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
        </div>
      </template>

      <div class="profile-content">
        <!-- 头像区域 -->
        <div class="avatar-section">
          <el-avatar :size="100" :src="form.avatar" :icon="UserFilled" />
          <el-upload
            class="avatar-upload"
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleAvatarChange"
            accept="image/*"
          >
            <el-button type="primary" link>更换头像</el-button>
          </el-upload>
        </div>

        <!-- 信息表单 -->
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          class="profile-form"
        >
          <el-form-item label="账号">
            <el-input v-model="form.username" disabled />
          </el-form-item>

          <el-form-item label="学号" v-if="form.studentNo">
            <el-input v-model="form.studentNo" disabled />
          </el-form-item>

          <el-form-item label="班级" v-if="form.className">
            <el-input v-model="form.className" disabled />
          </el-form-item>

          <el-form-item label="角色">
            <el-tag :type="getRoleTag(form.role)">{{ getRoleName(form.role) }}</el-tag>
          </el-form-item>

          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" />
          </el-form-item>

          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSubmit" :loading="loading">
              保存修改
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  studentNo: '',
  className: '',
  role: 0,
  realName: '',
  phone: '',
  email: '',
  avatar: ''
})

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const getRoleName = (role) => ({ 0: '学生', 1: '教师', 2: '管理员' }[role] || '未知')
const getRoleTag = (role) => ({ 0: 'success', 1: 'warning', 2: 'danger' }[role] || 'info')

const loadProfile = async () => {
  try {
    const res = await request.get('/api/user/profile')
    const user = res.data
    Object.assign(form, {
      username: user.username || '',
      studentNo: user.studentNo || '',
      className: user.className || '',
      role: user.role || 0,
      realName: user.realName || '',
      phone: user.phone || '',
      email: user.email || '',
      avatar: user.avatar || ''
    })
  } catch {
    ElMessage.error('加载个人信息失败')
  }
}

const handleAvatarChange = (file) => {
  // 压缩图片后转 base64
  const reader = new FileReader()
  reader.onload = (e) => {
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const maxSize = 150
      let width = img.width
      let height = img.height
      if (width > height) {
        if (width > maxSize) { height = height * maxSize / width; width = maxSize }
      } else {
        if (height > maxSize) { width = width * maxSize / height; height = maxSize }
      }
      canvas.width = width
      canvas.height = height
      canvas.getContext('2d').drawImage(img, 0, 0, width, height)
      form.avatar = canvas.toDataURL('image/jpeg', 0.7)
    }
    img.src = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await request.put('/api/user/profile', {
        realName: form.realName,
        phone: form.phone,
        email: form.email,
        avatar: form.avatar
      })
      // 更新本地存储
      userStore.setUserInfo({
        ...userStore.userInfo,
        realName: form.realName,
        avatar: form.avatar
      })
      ElMessage.success('保存成功')
      // 刷新页面让头像生效
      window.location.reload()
    } catch {
      ElMessage.error('保存失败')
    } finally {
      loading.value = false
    }
  })
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-container {
  max-width: 700px;
  margin: 20px auto;
  padding: 0 20px;
}

.profile-content {
  display: flex;
  gap: 40px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  min-width: 150px;
}

.avatar-upload {
  text-align: center;
}

.profile-form {
  flex: 1;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}
</style>
