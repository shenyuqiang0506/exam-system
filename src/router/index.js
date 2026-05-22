import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录' }
    },

    // ========== 教师端路由 ==========
    {
      path: '/teacher',
      component: () => import('@/views/teacher/TeacherLayout.vue'),
      redirect: '/teacher/dashboard',
      meta: { requiresAuth: true, role: 1 },
      children: [
        {
          path: 'dashboard',
          name: 'TeacherDashboard',
          component: () => import('@/views/teacher/Dashboard.vue'),
          meta: { title: '首页' }
        },
        {
          path: 'questions',
          name: 'QuestionManage',
          component: () => import('@/views/teacher/QuestionManage.vue'),
          meta: { title: '题库管理' }
        },
        {
          path: 'papers',
          name: 'PaperManage',
          component: () => import('@/views/teacher/PaperManage.vue'),
          meta: { title: '试卷管理' }
        },
        {
          path: 'paper-list',
          name: 'PaperList',
          component: () => import('@/views/teacher/PaperList.vue'),
          meta: { title: '试卷列表' }
        },
        {
          path: 'manual-paper',
          name: 'ManualPaper',
          component: () => import('@/views/teacher/ManualPaper.vue'),
          meta: { title: '手动组卷' }
        },
        {
          path: 'auto-paper',
          name: 'AutoPaperGenerate',
          component: () => import('@/views/teacher/AutoPaperGenerate.vue'),
          meta: { title: '智能组卷' }
        },
        {
          path: 'records',
          name: 'TeacherRecords',
          component: () => import('@/views/teacher/TeacherRecords.vue'),
          meta: { title: '成绩管理' }
        }
      ]
    },

    // ========== 学生端路由 ==========
    {
      path: '/student',
      component: () => import('@/views/student/StudentLayout.vue'),
      redirect: '/student/exams',
      meta: { requiresAuth: true, role: 0 },
      children: [
        {
          path: 'exams',
          name: 'ExamCenter',
          component: () => import('@/views/student/ExamCenter.vue'),
          meta: { title: '考试中心' }
        },
        {
          path: 'records',
          name: 'MyRecords',
          component: () => import('@/views/student/MyRecords.vue'),
          meta: { title: '我的成绩' }
        },
        {
          path: 'result/:recordId',
          name: 'ExamResult',
          component: () => import('@/views/student/ExamResultDetail.vue'),
          meta: { title: '成绩详情' }
        }
      ]
    },

    // ========== 考试页面（独立布局） ==========
    {
      path: '/student/exam/:paperId',
      name: 'TakeExam',
      component: () => import('@/views/student/TakeExam.vue'),
      meta: { requiresAuth: true, role: 0, fullscreen: true }
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 在线考试系统`
  }

  // 不需要登录的页面
  if (to.path === '/login') {
    next()
    return
  }

  // 需要登录的页面
  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }

  // 角色权限检查
  if (to.meta.role !== undefined && userInfo.role !== to.meta.role) {
    if (userInfo.role === 1) {
      next('/teacher/dashboard')
    } else {
      next('/student/exams')
    }
    return
  }

  next()
})

export default router
