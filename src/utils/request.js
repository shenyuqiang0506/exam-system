import axios from 'axios'
import {ElMessage} from 'element-plus'
import router from '@/router'

const request = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    response => {
        // 文件流直接返回
        if (response.config.responseType === 'blob') {
            return response
        }
        const res = response.data

        // 成功响应
        if (res.code === 200) {
            return res
        }

        // 业务错误（非200）
        // 对于登录等接口，弹出错误提示
        ElMessage.error(res.message || '请求失败')

        const error = new Error(res.message || '请求失败')
        error.code = res.code
        error.data = res.data
        return Promise.reject(error)
    },
    error => {
        // HTTP 状态码错误
        if (error.response) {
            const status = error.response.status
            const currentPath = router.currentRoute.value.path

            // 如果是登录接口的 401，不弹出"登录已过期"
            if (status === 401 && currentPath !== '/login') {
                localStorage.removeItem('token')
                localStorage.removeItem('userInfo')
                router.push('/login')
                ElMessage.error('登录已过期，请重新登录')
            } else if (status === 403) {
                ElMessage.error('没有权限访问')
            } else if (status === 500) {
                ElMessage.error('服务器内部错误')
            } else if (status !== 401) {
                ElMessage.error(error.response.data?.message || '请求失败')
            }
        } else {
            ElMessage.error('网络连接失败，请检查网络')
        }
        return Promise.reject(error)
    }
)

export default request
