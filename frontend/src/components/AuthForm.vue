<template>
  <div class="auth-form-container">
    <div class="auth-form">
      <!-- 头部 -->
      <div class="form-header">
        <button @click="goBack" class="back-btn">
          <i class="fas fa-arrow-left"></i>
        </button>
        <div class="role-info">
          <div class="role-icon">
            <i :class="roleConfig[role].icon"></i>
          </div>
          <div>
            <h2>{{ isLogin ? '登录' : '注册' }}</h2>
            <p>{{ roleConfig[role].title }}{{ isLogin ? '登录' : '注册' }}</p>
          </div>
        </div>
      </div>

      <!-- 表单 -->
      <form @submit.prevent="submitForm" class="form-content">
        <!-- 姓名字段 (注册时) -->
        <div v-if="!isLogin" class="form-group">
          <label class="form-label">
            <i class="fas fa-user"></i>
            姓名
          </label>
          <input 
            v-model="formData.name" 
            class="form-input" 
            placeholder="请输入您的真实姓名" 
            required
          >
        </div>

        <!-- 顾客注册字段 -->
        <template v-if="!isLogin && role === 'customer'">
          <div class="form-group">
            <label class="form-label">
              <i class="fas fa-phone"></i>
              电话号码
            </label>
            <input 
              v-model="formData.phone" 
              class="form-input" 
              placeholder="请输入手机号码" 
              required
            >
          </div>
          <div class="form-group">
            <label class="form-label">
              <i class="fas fa-envelope"></i>
              邮箱地址
            </label>
            <input 
              v-model="formData.email" 
              class="form-input" 
              placeholder="请输入邮箱地址" 
              type="email" 
              required
            >
          </div>
          <div class="form-group">
            <label class="form-label">
              <i class="fas fa-map-marker-alt"></i>
              联系地址
            </label>
            <input 
              v-model="formData.address" 
              class="form-input" 
              placeholder="请输入联系地址" 
              required
            >
          </div>
        </template>

        <!-- 管理员注册字段 -->
        <template v-if="!isLogin && role === 'admin'">
          <div class="form-group">
            <label class="form-label">
              <i class="fas fa-phone"></i>
              电话号码
            </label>
            <input 
              v-model="formData.phone" 
              class="form-input" 
              placeholder="请输入手机号码" 
              required
            >
          </div>
          <div class="form-group">
            <label class="form-label">
              <i class="fas fa-envelope"></i>
              邮箱地址
            </label>
            <input 
              v-model="formData.email" 
              class="form-input" 
              placeholder="请输入邮箱地址" 
              type="email" 
              required
            >
          </div>
          <div class="form-group">
            <label class="form-label">
              <i class="fas fa-user-tag"></i>
              管理员角色
            </label>
            <select v-model="formData.role" class="form-input" required>
              <option value="">请选择管理员角色</option>
              <option value="SUPER_ADMIN">超级管理员</option>
              <option value="MANAGER">经理</option>
              <option value="STAFF">员工</option>
            </select>
          </div>
        </template>

        <!-- 技师注册字段 -->
        <template v-if="!isLogin && role === 'technician'">
          <div class="form-group">
            <label class="form-label">
              <i class="fas fa-id-badge"></i>
              员工ID
            </label>
            <input 
              v-model="formData.employeeId" 
              class="form-input" 
              placeholder="请输入员工ID" 
              required
            >
            <div v-if="employeeIdError" class="field-error">
              <i class="fas fa-exclamation-triangle"></i>
              {{ employeeIdError }}
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">
              <i class="fas fa-phone"></i>
              电话号码
            </label>
            <input 
              v-model="formData.phone" 
              class="form-input" 
              placeholder="请输入手机号码" 
              required
            >
          </div>
          <div class="form-group">
            <label class="form-label">
              <i class="fas fa-envelope"></i>
              邮箱地址
            </label>
            <input 
              v-model="formData.email" 
              class="form-input" 
              placeholder="请输入邮箱地址" 
              type="email" 
              required
            >
          </div>
          <div class="form-group"> 
            <label class="form-label">
              <i class="fas fa-cogs"></i>
              技能类型
            </label>
            <select v-model="formData.skillType" class="form-input" required>
              <option value="">请选择技能类型</option>
              <option value="MECHANIC">机械维修</option>
              <option value="ELECTRICIAN">电气维修</option>
              <option value="BODY_WORK">车身维修</option>
              <option value="PAINT">喷漆</option>
              <option value="DIAGNOSTIC">诊断</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">
              <i class="fas fa-dollar-sign"></i>
              小时费率 (元)
            </label>
            <input 
              v-model="formData.hourlyRate" 
              class="form-input" 
              placeholder="请输入小时费率" 
              type="number" 
              step="0.01" 
              min="0"
              required
            >
          </div>
        </template>

        <!-- 用户名 -->
        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-user-circle"></i>
            用户名
          </label>
          <input 
            v-model="formData.username" 
            class="form-input" 
            placeholder="请输入用户名" 
            required
          >
          <div v-if="usernameError" class="field-error">
            <i class="fas fa-exclamation-triangle"></i>
            {{ usernameError }}
          </div>
        </div>

        <!-- 密码 -->
        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-lock"></i>
            密码
          </label>
          <div class="password-input">
            <input 
              v-model="formData.password" 
              :type="showPassword ? 'text' : 'password'"
              class="form-input" 
              placeholder="请输入密码" 
              required
            >
            <button 
              type="button" 
              @click="togglePassword" 
              class="password-toggle"
            >
              <i :class="showPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
            </button>
          </div>
        </div>

        <!-- 提交按钮 -->
        <button type="submit" :disabled="loading" class="submit-btn">
          <div v-if="loading" class="btn-spinner"></div>
          <i v-else :class="isLogin ? 'fas fa-sign-in-alt' : 'fas fa-user-plus'"></i>
          {{ loading ? '处理中...' : (isLogin ? '登录' : '注册') }}
        </button>
      </form>

      <!-- 切换模式 -->
      <div class="form-footer">
        <p @click="toggleMode" class="mode-toggle">
          {{ isLogin ? '没有账号？点击注册' : '已有账号？点击登录' }}
          <i class="fas fa-exchange-alt"></i>
        </p>
      </div>

      <!-- 错误消息 -->
      <transition name="error">
        <div v-if="errorMessage" class="error-message">
          <i class="fas fa-exclamation-circle"></i>
          {{ errorMessage }}
          <button @click="clearError" class="error-close">&times;</button>
        </div>
      </transition>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AuthForm',
  props: {
    role: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      isLogin: true,
      loading: false,
      showPassword: false,
      errorMessage: '',
      employeeIdError: '',
      usernameError: '',
      formData: {
        name: '',
        username: '',
        password: '',
        phone: '',
        email: '',
        address: '',
        role: '',
        employeeId: '',
        skillType: '',
        hourlyRate: ''
      },
      roleConfig: {
        customer: {
          title: '顾客',
          icon: 'fas fa-user'
        },
        technician: {
          title: '技师',
          icon: 'fas fa-tools'
        },
        admin: {
          title: '管理员',
          icon: 'fas fa-user-cog'
        }
      }
    }
  },
  methods: {
    goBack() {
      this.$router.push('/');
    },
    toggleMode() {
      this.isLogin = !this.isLogin;
      this.clearErrors();
      this.resetForm();
    },
    togglePassword() {
      this.showPassword = !this.showPassword;
    },
    resetForm() {
      this.formData = {
        name: '',
        username: '',
        password: '',
        phone: '',
        email: '',
        address: '',
        role: '',
        employeeId: '',
        skillType: '',
        hourlyRate: ''
      };
    },
    clearErrors() {
      this.errorMessage = '';
      this.employeeIdError = '';
      this.usernameError = '';
    },
    clearError() {
      this.errorMessage = '';
    },
    async submitForm() {
      this.loading = true;
      this.clearErrors();
      
      try {
        if (this.isLogin) {
          await this.handleLogin();
        } else {
          await this.handleRegister();
        }
      } catch (error) {
        console.error('认证失败:', error);
        this.handleError(error);
      } finally {
        this.loading = false;
      }
    },
    async handleLogin() {
      const apiPath = this.getApiPath();
      const response = await this.$axios.post(`${apiPath}/login`, null, {
        params: {
          username: this.formData.username,
          password: this.formData.password
        }
      });
      
      if (response.data) {
        localStorage.setItem('user', JSON.stringify(response.data));
        localStorage.setItem('userRole', this.role);
        this.$emit('auth-success', { ...response.data, role: this.role });
      }
    },
    async handleRegister() {
      const apiPath = this.getApiPath();
      const requestData = this.prepareRegisterData();
      
      const response = await this.$axios.post(apiPath, requestData);
      
      if (response.data) {
        // 注册成功后自动登录
        await this.handleLogin();
      }
    },
    getApiPath() {
      const roleMapping = {
        'customer': '/users',
        'admin': '/admins',
        'technician': '/technicians'
      };
      return roleMapping[this.role];
    },
    prepareRegisterData() {
      const baseData = {
        username: this.formData.username,
        password: this.formData.password
      };

      switch (this.role) {
        case 'customer':
          return {
            ...baseData,
            name: this.formData.name,
            phone: this.formData.phone,
            email: this.formData.email,
            address: this.formData.address
          };
        case 'admin':
          return {
            ...baseData,
            name: this.formData.name,
            phone: this.formData.phone,
            email: this.formData.email,
            role: this.formData.role
          };
        case 'technician':
          return {
            name: this.formData.name,
            employeeId: this.formData.employeeId,
            username: this.formData.username,
            password: this.formData.password,
            phone: this.formData.phone,
            email: this.formData.email,
            skillType: this.formData.skillType,
            hourlyRate: parseFloat(this.formData.hourlyRate)
          };
        default:
          return baseData;
      }
    },
    handleError(error) {
      const errorMessage = error.response?.data?.message || error.message;
      
      // 处理特定错误
      if (errorMessage.includes('员工ID已存在')) {
        this.employeeIdError = '该员工ID已被使用，请尝试其他ID';
        return;
      }
      
      if (errorMessage.includes('用户名已存在')) {
        this.usernameError = '该用户名已被使用，请尝试其他用户名';
        return;
      }
      
      // 通用错误处理
      if (error.response?.status === 401) {
        this.errorMessage = '用户名或密码错误';
      } else if (error.response?.status === 403) {
        this.errorMessage = '访问被拒绝，请检查权限';
      } else if (error.response?.status === 500) {
        this.errorMessage = '服务器错误，请稍后重试';
      } else if (error.code === 'ERR_NETWORK') {
        this.errorMessage = '网络连接失败，请检查网络';
      } else {
        this.errorMessage = errorMessage || '操作失败，请重试';
      }
    }
  }
}
</script>

<style scoped>
.auth-form-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.auth-form {
  width: 100%;
  max-width: 500px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.form-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 30px 30px 20px;
  background: linear-gradient(135deg, #42b983, #369970);
  color: white;
}

.back-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  transition: background-color 0.2s;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.role-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.role-icon {
  width: 50px;
  height: 50px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

.role-info h2 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
}

.role-info p {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}

.form-content {
  padding: 30px;
}

.form-group {
  margin-bottom: 24px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-weight: 500;
  color: #374151;
  font-size: 14px;
}

.form-input {
  width: 100%;
  padding: 14px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  font-size: 14px;
  transition: all 0.2s ease;
  background: #f9fafb;
}

.form-input:focus {
  outline: none;
  border-color: #42b983;
  box-shadow: 0 0 0 3px rgba(66, 185, 131, 0.1);
  background: white;
}

.password-input {
  position: relative;
}

.password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 4px;
  transition: color 0.2s;
}

.password-toggle:hover {
  color: #42b983;
}

.field-error {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  color: #ef4444;
  font-size: 12px;
}

.submit-btn {
  width: 100%;
  padding: 16px 24px;
  background: linear-gradient(135deg, #42b983, #369970);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(66, 185, 131, 0.3);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.form-footer {
  padding: 0 30px 30px;
  text-align: center;
}

.mode-toggle {
  color: #42b983;
  cursor: pointer;
  font-size: 14px;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: color 0.2s;
}

.mode-toggle:hover {
  color: #369970;
}

.error-message {
  margin: 20px 30px;
  padding: 16px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  color: #dc2626;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.error-close {
  background: none;
  border: none;
  color: #dc2626;
  font-size: 18px;
  cursor: pointer;
  margin-left: auto;
  padding: 0;
}

.error-enter-active, .error-leave-active {
  transition: all 0.3s ease;
}

.error-enter {
  opacity: 0;
  transform: translateY(-10px);
}

.error-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 响应式设计 */
@media (max-width: 640px) {
  .auth-form-container {
    padding: 10px;
  }
  
  .auth-form {
    max-width: 100%;
  }
  
  .form-header {
    padding: 20px 20px 15px;
  }
  
  .form-content {
    padding: 20px;
  }
  
  .form-footer {
    padding: 0 20px 20px;
  }
  
  .error-message {
    margin: 15px 20px;
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>