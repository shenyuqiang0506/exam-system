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
        // 假设后端 Result 结构: { code, message, data }
        if (res.code !== 200) {
            // Token 失效
            if (res.code === 401) {
                localStorage.removeItem('token')
                router.push('/login')
                ElMessage.error('登录已过期，请重新登录')
                return Promise.reject(new Error(res.message))
            }
            // 403 业务错误：不弹出提示，让组件自行处理
            if (res.code === 403) {
                const error = new Error(res.message)
                error.code = 403
                error.data = res.data
                return Promise.reject(error)
            }
            // 其他业务错误
            ElMessage.error(res.message || '请求失败')
            return Promise.reject(new Error(res.message))
        }
        return res
    },
    error => {
        // HTTP 状态码错误
        if (error.response) {
            const status = error.response.status
            if (status === 401) {
                localStorage.removeItem('token')
                router.push('/login')
                ElMessage.error('登录已过期，请重新登录')
            } else if (status === 403) {
                // HTTP 403：不弹出提示
            } else if (status === 500) {
                ElMessage.error('服务器内部错误')
            } else {
                ElMessage.error(error.response.data?.message || '请求失败')
            }
        } else {
            ElMessage.error('网络连接失败，请检查网络')
        }
        return Promise.reject(error)
    }
)

export default request
