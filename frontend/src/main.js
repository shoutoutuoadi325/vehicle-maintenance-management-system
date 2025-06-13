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

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
