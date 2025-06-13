<template>
  <div id="app">
    <!-- 全局加载指示器 -->
    <div v-if="isLoading" class="global-loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    
    <!-- 全局消息提示 -->
    <transition name="message">
      <div v-if="globalMessage" :class="['global-message', messageType]">
        <i :class="messageIcon"></i>
        {{ globalMessage }}
        <button @click="clearMessage" class="close-btn">&times;</button>
      </div>
    </transition>
    
    <!-- 主内容区域 -->
    <transition name="page" mode="out-in">
      <router-view 
        @role-selected="handleRoleSelection" 
        @auth-success="handleAuthSuccess"
        @loading="setLoading"
        @message="showMessage"
      />
    </transition>
    
    <!-- 页脚 -->
    <footer class="app-footer">
      <p>&copy; 2024 汽车维修管理系统. All rights reserved.</p>
    </footer>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      isLoading: false,
      globalMessage: '',
      messageType: 'info',
      messageTimer: null
    }
  },
  computed: {
    messageIcon() {
      const icons = {
        success: 'fas fa-check-circle',
        error: 'fas fa-exclamation-circle',
        warning: 'fas fa-exclamation-triangle',
        info: 'fas fa-info-circle'
      };
      return icons[this.messageType] || icons.info;
    }
  },
  methods: {
    handleRoleSelection(role) {
      this.$router.push({ name: 'Auth', params: { role } });
    },
    handleAuthSuccess(userData) {
      const route = {
        customer: '/customer',
        technician: '/technician',
        admin: '/admin'
      }[userData.role];
      
      this.showMessage(`欢迎回来，${userData.name || userData.username}！`, 'success');
      this.$router.push(route);
    },
    setLoading(status) {
      this.isLoading = status;
    },
    showMessage(message, type = 'info', duration = 5000) {
      this.globalMessage = message;
      this.messageType = type;
      
      if (this.messageTimer) {
        clearTimeout(this.messageTimer);
      }
      
      this.messageTimer = setTimeout(() => {
        this.clearMessage();
      }, duration);
    },
    clearMessage() {
      this.globalMessage = '';
      if (this.messageTimer) {
        clearTimeout(this.messageTimer);
        this.messageTimer = null;
      }
    }
  }
}
</script>

<style>
/* 导入Font Awesome图标 */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #333;
  line-height: 1.6;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* 全局加载指示器 */
.global-loading {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #42b983;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 全局消息提示 */
.global-message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 16px 20px;
  border-radius: 8px;
  color: white;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 400px;
}

.global-message.success {
  background-color: #10b981;
}

.global-message.error {
  background-color: #ef4444;
}

.global-message.warning {
  background-color: #f59e0b;
}

.global-message.info {
  background-color: #3b82f6;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 20px;
  cursor: pointer;
  margin-left: auto;
  padding: 0;
  opacity: 0.8;
  transition: opacity 0.2s;
}

.close-btn:hover {
  opacity: 1;
}

/* 页脚样式 */
.app-footer {
  margin-top: auto;
  padding: 20px;
  text-align: center;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  backdrop-filter: blur(10px);
}

/* 页面过渡动画 */
.page-enter-active, .page-leave-active {
  transition: all 0.3s ease;
}

.page-enter {
  opacity: 0;
  transform: translateX(30px);
}

.page-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

/* 消息过渡动画 */
.message-enter-active, .message-leave-active {
  transition: all 0.3s ease;
}

.message-enter {
  opacity: 0;
  transform: translateX(100%);
}

.message-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

/* 通用按钮样式 */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;
}

.btn-primary {
  background-color: #42b983;
  color: white;
}

.btn-primary:hover {
  background-color: #369970;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(66, 185, 131, 0.3);
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background-color: #5a6268;
}

.btn-danger {
  background-color: #dc3545;
  color: white;
}

.btn-danger:hover {
  background-color: #c82333;
}

.btn-outline {
  background-color: transparent;
  border: 2px solid #42b983;
  color: #42b983;
}

.btn-outline:hover {
  background-color: #42b983;
  color: white;
}

/* 卡片样式 */
.card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.card:hover {
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.card-header {
  padding: 20px 24px 0;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 20px;
}

.card-body {
  padding: 0 24px 24px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

/* 表单样式 */
.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #374151;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.form-input:focus {
  outline: none;
  border-color: #42b983;
  box-shadow: 0 0 0 3px rgba(66, 185, 131, 0.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .global-message {
    top: 10px;
    right: 10px;
    left: 10px;
    max-width: none;
  }
  
  .app-footer {
    padding: 15px;
    font-size: 14px;
  }
}

/* 工具类 */
.text-center { text-align: center; }
.text-left { text-align: left; }
.text-right { text-align: right; }
.mt-1 { margin-top: 0.25rem; }
.mt-2 { margin-top: 0.5rem; }
.mt-3 { margin-top: 1rem; }
.mt-4 { margin-top: 1.5rem; }
.mb-1 { margin-bottom: 0.25rem; }
.mb-2 { margin-bottom: 0.5rem; }
.mb-3 { margin-bottom: 1rem; }
.mb-4 { margin-bottom: 1.5rem; }
.p-1 { padding: 0.25rem; }
.p-2 { padding: 0.5rem; }
.p-3 { padding: 1rem; }
.p-4 { padding: 1.5rem; }
</style>
