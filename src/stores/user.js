import {defineStore} from 'pinia'
import {ref} from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')
    const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

    function setToken(newToken) {
        token.value = newToken
        localStorage.setItem('token', newToken)
    }

    function setUserInfo(info) {
        userInfo.value = info
        localStorage.setItem('userInfo', JSON.stringify(info))
    }

    function clearUser() {
        token.value = ''
        userInfo.value = {}
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    function isLoggedIn() {
        return !!token.value
    }

    function isTeacher() {
        return userInfo.value.role === 1
    }

    function isStudent() {
        return userInfo.value.role === 0
    }

    return {
        token,
        userInfo,
        setToken,
        setUserInfo,
        clearUser,
        isLoggedIn,
        isTeacher,
        isStudent
    }
})
