import Vue from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'

Vue.config.productionTip = false

// 配置axios
Vue.prototype.$axios = axios.create({
  baseURL: '/api', // 使用代理，简化baseURL
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json'
  }
})

Vue.prototype.$axios.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem('accessToken')

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }

  return config
})

let isRefreshing = false
let refreshQueue = []

function clearAuthState() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  localStorage.removeItem('userRole')
  // Keep deviceId for device-level session analytics and continuity.
}

function enqueueRequest(resolve, reject) {
  refreshQueue.push({ resolve, reject })
}

function flushQueue(error, token) {
  refreshQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else {
      resolve(token)
    }
  })
  refreshQueue = []
}

Vue.prototype.$axios.interceptors.response.use(
  response => response,
  async (error) => {
    const originalRequest = error.config || {}
    const status = error.response?.status

    if (status !== 401 || originalRequest._retry || originalRequest.url?.includes('/auth/refresh')) {
      return Promise.reject(error)
    }

    const refreshToken = localStorage.getItem('refreshToken')
    if (!refreshToken) {
      clearAuthState()
      if (router.currentRoute.path !== '/') {
        router.push('/')
      }
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        enqueueRequest(
          (newToken) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            resolve(Vue.prototype.$axios(originalRequest))
          },
          reject
        )
      })
    }

    originalRequest._retry = true
    isRefreshing = true

    try {
      const refreshResponse = await Vue.prototype.$axios.post('/auth/refresh', { refreshToken })
      const payload = refreshResponse.data || {}
      const newAccessToken = payload.accessToken
      const newRefreshToken = payload.refreshToken

      if (!newAccessToken || !newRefreshToken) {
        throw new Error('刷新令牌响应不完整')
      }

      localStorage.setItem('accessToken', newAccessToken)
      localStorage.setItem('refreshToken', newRefreshToken)

      flushQueue(null, newAccessToken)
      originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
      return Vue.prototype.$axios(originalRequest)
    } catch (refreshError) {
      flushQueue(refreshError)
      clearAuthState()
      if (router.currentRoute.path !== '/') {
        router.push('/')
      }
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  }
)

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
