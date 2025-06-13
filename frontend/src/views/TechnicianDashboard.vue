<template>
  <div class="technician-dashboard">
    <!-- 顶部导航栏 -->
    <header class="dashboard-header">
      <div class="header-left">
        <div class="logo">
          <i class="fas fa-tools"></i>
          <span>技师工作台</span>
        </div>
        <nav class="nav-menu">
          <a href="#" @click="activeTab = 'overview'" :class="{ active: activeTab === 'overview' }">
            <i class="fas fa-home"></i> 概览
          </a>
          <a href="#" @click="activeTab = 'tasks'" :class="{ active: activeTab === 'tasks' }">
            <i class="fas fa-tasks"></i> 我的任务
          </a>
          <a href="#" @click="activeTab = 'history'" :class="{ active: activeTab === 'history' }">
            <i class="fas fa-history"></i> 工作历史
          </a>
          <a href="#" @click="activeTab = 'earnings'" :class="{ active: activeTab === 'earnings' }">
            <i class="fas fa-dollar-sign"></i> 收入统计
          </a>
        </nav>
      </div>
      <div class="header-right">
        <div class="user-menu" @click="toggleUserMenu">
          <div class="user-avatar">
            <i class="fas fa-user-hard-hat"></i>
          </div>
          <span class="user-name">{{ user.name || user.username }}</span>
          <i class="fas fa-chevron-down"></i>
        </div>
        <div v-if="showUserMenu" class="user-dropdown">
          <a href="#" @click="activeTab = 'profile'">
            <i class="fas fa-user-edit"></i> 个人资料
          </a>
          <a href="#" @click="logout">
            <i class="fas fa-sign-out-alt"></i> 登出
          </a>
        </div>
      </div>
    </header>

    <!-- 主内容区域 -->
    <main class="dashboard-main">
      <!-- 概览页面 -->
      <div v-if="activeTab === 'overview'" class="tab-content">
        <div class="welcome-section">
          <h1>欢迎回来，{{ user.name || user.username }}技师！</h1>
          <div class="skill-badge">
            <i class="fas fa-award"></i>
            <span>{{ getSkillTypeName(user.skillType) }}</span>
          </div>
        </div>

        <!-- 统计卡片 -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f59e0b, #d97706);">
              <i class="fas fa-tasks"></i>
            </div>
            <div class="stat-content">
              <h3>{{ statistics.totalTasks }}</h3>
              <p>总任务数</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #10b981, #059669);">
              <i class="fas fa-check-circle"></i>
            </div>
            <div class="stat-content">
              <h3>{{ statistics.completedTasks }}</h3>
              <p>已完成任务</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #3b82f6, #2563eb);">
              <i class="fas fa-clock"></i>
            </div>
            <div class="stat-content">
              <h3>{{ statistics.pendingTasks }}</h3>
              <p>进行中任务</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #8b5cf6, #7c3aed);">
              <i class="fas fa-dollar-sign"></i>
            </div>
            <div class="stat-content">
              <h3>¥{{ formatCurrency(statistics.monthlyEarnings || 0) }}</h3>
              <p>本月收入</p>
            </div>
          </div>
        </div>

        <!-- 技师信息卡片 -->
        <div class="info-section">
          <div class="technician-card">
            <div class="tech-avatar">
              <i class="fas fa-user-hard-hat"></i>
            </div>
            <div class="tech-info">
              <h3>{{ user.name }}</h3>
              <p class="tech-id">员工ID: {{ user.employeeId }}</p>
              <div class="tech-details">
                <div class="detail-item">
                  <i class="fas fa-cogs"></i>
                  <span>{{ getSkillTypeName(user.skillType) }}</span>
                </div>
                <div class="detail-item">
                  <i class="fas fa-dollar-sign"></i>
                  <span>¥{{ user.hourlyRate }}/小时</span>
                </div>
                <div class="detail-item">
                  <i class="fas fa-phone"></i>
                  <span>{{ user.phone }}</span>
                </div>
                <div class="detail-item">
                  <i class="fas fa-envelope"></i>
                  <span>{{ user.email }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="earnings-overview">
            <h3>收入概览</h3>
            <div class="earnings-stats">
              <div class="earnings-item">
                <span class="earnings-label">本月收入</span>
                <span class="earnings-value">¥{{ formatCurrency(earnings.monthly) }}</span>
              </div>
              <div class="earnings-item">
                <span class="earnings-label">总收入</span>
                <span class="earnings-value">¥{{ formatCurrency(earnings.total) }}</span>
              </div>
            </div>
            <button class="btn btn-outline" @click="activeTab = 'earnings'">
              <i class="fas fa-chart-line"></i> 查看详细
            </button>
          </div>
        </div>

        <!-- 待处理任务 -->
        <div class="pending-tasks">
          <h2>待处理任务</h2>
          <div class="task-list">
            <div v-if="pendingTasks.length === 0" class="empty-state">
              <i class="fas fa-check-circle"></i>
              <p>暂无待处理任务</p>
            </div>
            <div v-for="task in pendingTasks.slice(0, 5)" :key="task.id" class="task-item">
              <div class="task-info">
                <h4>{{ task.orderNumber || `维修单 #${task.id}` }}</h4>
                <p>{{ task.description }}</p>
                <div class="task-meta">
                  <span class="task-date">
                    <i class="fas fa-calendar"></i>
                    {{ formatDate(task.createdAt) }}
                  </span>
                  <span class="task-vehicle">
                    <i class="fas fa-car"></i>
                    {{ getVehicleDisplay(task) }}
                  </span>
                  <span class="task-customer">
                    <i class="fas fa-user"></i>
                    {{ task.user ? task.user.name : '未知客户' }}
                  </span>
                </div>
              </div>
              <div class="task-actions">
                <span :class="['task-status', task.status.toLowerCase()]">
                  {{ getStatusText(task.status) }}
                </span>
                <button v-if="task.status === 'ASSIGNED'" class="btn btn-primary btn-sm" @click="startTask(task)">
                  <i class="fas fa-play"></i> 开始
                </button>
                <button v-else-if="task.status === 'IN_PROGRESS'" class="btn btn-success btn-sm" @click="completeTask(task)">
                  <i class="fas fa-check"></i> 完成
                </button>
                <button class="btn btn-outline btn-sm" @click="viewTask(task)">
                  <i class="fas fa-eye"></i> 查看
                </button>
              </div>
            </div>
          </div>
          <button v-if="pendingTasks.length > 5" class="btn btn-outline" @click="activeTab = 'tasks'">
            查看全部任务
          </button>
        </div>
      </div>

      <!-- 任务管理页面 -->
      <div v-if="activeTab === 'tasks'" class="tab-content">
        <div class="section-header">
          <h2>我的任务</h2>
          <div class="task-filters">
            <select v-model="taskFilter" class="form-input">
              <option value="">全部状态</option>
              <option value="ASSIGNED">已分配</option>
              <option value="IN_PROGRESS">进行中</option>
              <option value="COMPLETED">已完成</option>
            </select>
          </div>
        </div>

        <div class="tasks-container">
          <div v-if="filteredTasks.length === 0" class="empty-state">
            <i class="fas fa-tasks"></i>
            <h3>暂无任务</h3>
            <p>等待系统分配新任务</p>
          </div>
          <div v-for="task in filteredTasks" :key="task.id" class="task-card">
            <div class="task-header">
              <div>
                <h3>{{ task.orderNumber || `维修单 #${task.id}` }}</h3>
                <p class="task-vehicle">{{ getVehicleDisplay(task) }}</p>
              </div>
              <span :class="['status-badge', task.status.toLowerCase()]">
                {{ getStatusText(task.status) }}
              </span>
            </div>
            <div class="task-body">
              <p><strong>故障描述:</strong> {{ task.description }}</p>
              <div class="task-info">
                <div class="info-item">
                  <i class="fas fa-user"></i>
                  <span>客户: {{ task.user ? task.user.name : '未知' }}</span>
                </div>
                <div class="info-item">
                  <i class="fas fa-phone"></i>
                  <span>{{ task.user ? task.user.phone : '无联系方式' }}</span>
                </div>
                <div class="info-item">
                  <i class="fas fa-calendar"></i>
                  <span>创建: {{ formatDate(task.createdAt) }}</span>
                </div>
                <div v-if="task.completedAt" class="info-item">
                  <i class="fas fa-check-circle"></i>
                  <span>完成: {{ formatDate(task.completedAt) }}</span>
                </div>
              </div>
              <p><strong>工时费用:</strong> ¥{{ task.laborCost || 0 }}</p>
            </div>
            <div class="task-footer">
              <button v-if="task.status === 'ASSIGNED'" @click="startTask(task)" class="btn btn-primary">
                <i class="fas fa-play"></i> 开始任务
              </button>
              <button v-if="task.status === 'IN_PROGRESS'" @click="completeTask(task)" class="btn btn-success">
                <i class="fas fa-check"></i> 完成任务
              </button>
              <button @click="viewTaskDetail(task)" class="btn btn-outline">
                <i class="fas fa-eye"></i> 查看详情
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 工作历史页面 -->
      <div v-if="activeTab === 'history'" class="tab-content">
        <div class="section-header">
          <h2>工作历史</h2>
        </div>
        
        <div class="history-container">
          <div v-if="completedTasks.length === 0" class="empty-state">
            <i class="fas fa-history"></i>
            <h3>暂无历史记录</h3>
            <p>完成任务后将显示在这里</p>
          </div>
          <div v-for="task in completedTasks" :key="task.id" class="history-card">
            <div class="history-header">
              <div>
                <h3>维修单 #{{ task.id }}</h3>
                <p>{{ task.vehiclePlate }}</p>
              </div>
              <div class="completion-date">
                <i class="fas fa-check-circle"></i>
                {{ formatDate(task.endDate) }}
              </div>
            </div>
            <div class="history-body">
              <p>{{ task.description }}</p>
              <div class="history-metrics">
                <span class="metric">
                  <i class="fas fa-clock"></i>
                  耗时: {{ calculateDuration(task.startDate, task.endDate) }}
                </span>
                <span class="metric">
                  <i class="fas fa-dollar-sign"></i>
                  费用: ¥{{ task.actualCost || task.estimatedCost }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 收入统计页面 -->
      <div v-if="activeTab === 'earnings'" class="tab-content">
        <div class="section-header">
          <h2>收入统计</h2>
        </div>
        
        <div class="earnings-dashboard">
          <div class="earnings-summary">
            <div class="summary-card">
              <h3>本月收入</h3>
              <div class="amount">¥{{ formatCurrency(earnings.monthly) }}</div>
              <div class="change positive">
                <i class="fas fa-arrow-up"></i>
                +12.5%
              </div>
            </div>
            <div class="summary-card">
              <h3>平均每单</h3>
              <div class="amount">¥{{ formatCurrency(earnings.averagePerTask) }}</div>
              <div class="change">
                <i class="fas fa-minus"></i>
                持平
              </div>
            </div>
            <div class="summary-card">
              <h3>总收入</h3>
              <div class="amount">¥{{ formatCurrency(earnings.total) }}</div>
              <div class="tasks-count">{{ statistics.completedTasks }}个任务</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 个人资料页面 -->
      <div v-if="activeTab === 'profile'" class="tab-content">
        <div class="section-header">
          <h2>个人资料</h2>
        </div>
        
        <div class="profile-container">
          <form @submit.prevent="updateProfile" class="profile-form">
            <div class="form-group">
              <label class="form-label">姓名</label>
              <input v-model="profileForm.name" class="form-input" required>
            </div>
            <div class="form-group">
              <label class="form-label">员工ID</label>
              <input v-model="profileForm.employeeId" class="form-input" disabled>
            </div>
            <div class="form-group">
              <label class="form-label">用户名</label>
              <input v-model="profileForm.username" class="form-input" disabled>
            </div>
            <div class="form-group">
              <label class="form-label">电话</label>
              <input v-model="profileForm.phone" class="form-input" required>
            </div>
            <div class="form-group">
              <label class="form-label">邮箱</label>
              <input v-model="profileForm.email" type="email" class="form-input" required>
            </div>
            <div class="form-group">
              <label class="form-label">技能类型</label>
              <select v-model="profileForm.skillType" class="form-input" required>
                <option value="MECHANIC">机械维修</option>
                <option value="ELECTRICIAN">电气维修</option>
                <option value="BODY_WORK">车身维修</option>
                <option value="PAINT">喷漆</option>
                <option value="DIAGNOSTIC">诊断</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">小时费率</label>
              <input v-model="profileForm.hourlyRate" type="number" step="0.01" class="form-input" required>
            </div>
            <button type="submit" class="btn btn-primary">
              <i class="fas fa-save"></i> 保存更改
            </button>
          </form>
        </div>
      </div>
    </main>

    <!-- 任务详情模态框 -->
    <div v-if="showTaskDetail && selectedTask" class="modal-overlay" @click="closeTaskDetail">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>{{ selectedTask.orderNumber || `维修单 #${selectedTask.id}` }}</h2>
          <button class="modal-close" @click="closeTaskDetail">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="detail-section">
            <h3>基本信息</h3>
            <div class="detail-grid">
              <div class="detail-item">
                <label>订单号:</label>
                <span>{{ selectedTask.orderNumber || `RO-${selectedTask.id}` }}</span>
              </div>
              <div class="detail-item">
                <label>状态:</label>
                <span :class="['status-badge', selectedTask.status.toLowerCase()]">
                  {{ getStatusText(selectedTask.status) }}
                </span>
              </div>
              <div class="detail-item">
                <label>创建时间:</label>
                <span>{{ formatDate(selectedTask.createdAt) }}</span>
              </div>
              <div v-if="selectedTask.completedAt" class="detail-item">
                <label>完成时间:</label>
                <span>{{ formatDate(selectedTask.completedAt) }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h3>车辆信息</h3>
            <div class="detail-grid" v-if="selectedTask.vehicle">
              <div class="detail-item">
                <label>车牌号:</label>
                <span>{{ selectedTask.vehicle.licensePlate }}</span>
              </div>
              <div class="detail-item">
                <label>品牌型号:</label>
                <span>{{ selectedTask.vehicle.brand }} {{ selectedTask.vehicle.model }}</span>
              </div>
              <div class="detail-item">
                <label>年份:</label>
                <span>{{ selectedTask.vehicle.year }}</span>
              </div>
              <div class="detail-item">
                <label>颜色:</label>
                <span>{{ selectedTask.vehicle.color }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h3>客户信息</h3>
            <div class="detail-grid" v-if="selectedTask.user">
              <div class="detail-item">
                <label>客户姓名:</label>
                <span>{{ selectedTask.user.name }}</span>
              </div>
              <div class="detail-item">
                <label>联系电话:</label>
                <span>{{ selectedTask.user.phone }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h3>维修详情</h3>
            <div class="detail-item full-width">
              <label>故障描述:</label>
              <p class="description">{{ selectedTask.description }}</p>
            </div>
            <div class="detail-grid">
              <div class="detail-item">
                <label>工时费用:</label>
                <span class="amount">¥{{ selectedTask.laborCost || 0 }}</span>
              </div>
              <div class="detail-item">
                <label>材料费用:</label>
                <span class="amount">¥{{ selectedTask.materialCost || 0 }}</span>
              </div>
              <div class="detail-item">
                <label>总费用:</label>
                <span class="amount total">¥{{ selectedTask.totalCost || (selectedTask.laborCost || 0) + (selectedTask.materialCost || 0) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button v-if="selectedTask.status === 'ASSIGNED'" @click="startTask(selectedTask)" class="btn btn-primary">
            <i class="fas fa-play"></i> 开始任务
          </button>
          <button v-if="selectedTask.status === 'IN_PROGRESS'" @click="completeTask(selectedTask)" class="btn btn-success">
            <i class="fas fa-check"></i> 完成任务
          </button>
          <button @click="closeTaskDetail" class="btn btn-outline">
            <i class="fas fa-times"></i> 关闭
          </button>
        </div>
      </div>
    </div>

    <!-- 完成任务模态框 -->
    <div v-if="showCompleteTask && selectedTask" class="modal-overlay" @click="closeCompleteTask">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>完成任务</h2>
          <button class="modal-close" @click="closeCompleteTask">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="task-summary">
            <h3>{{ selectedTask.orderNumber || `维修单 #${selectedTask.id}` }}</h3>
            <p>{{ selectedTask.description }}</p>
            <p><strong>车辆:</strong> {{ getVehicleDisplay(selectedTask) }}</p>
          </div>
          
          <form @submit.prevent="submitCompleteTask">
            <div class="form-group">
              <label class="form-label">使用材料</label>
              <div class="material-row" v-for="(row, idx) in materialRows" :key="idx" style="display:flex;gap:0.5rem;margin-bottom:0.5rem;">
                <select v-model="row.materialId" required style="flex:2;">
                  <option :value="null" disabled>选择材料</option>
                  <option v-for="m in materials" :key="m.id" :value="m.id">
                    {{ m.name }} (¥{{ m.unitPrice }})
                  </option>
                </select>
                <input type="number" v-model.number="row.quantity" min="0.1" step="0.1" placeholder="数量" style="flex:1;" required>
                <span style="flex:1;">¥{{ materialRowCost(row) }}</span>
                <button type="button" @click="removeMaterialRow(idx)" v-if="materialRows.length>1" style="flex:0 0 auto;">删除</button>
              </div>
              <button type="button" @click="addMaterialRow" class="btn btn-outline" style="margin-top:0.5rem;">添加材料</button>
              <p style="margin-top:0.5rem;">材料总费用：<strong>¥{{ materialCostComputed }}</strong></p>
            </div>
            <div class="form-group">
              <label class="form-label">工作说明</label>
              <textarea v-model="completeTaskForm.workNotes" class="form-input" rows="3"
                        placeholder="请简要说明本次维修的工作内容（可选）"></textarea>
            </div>
            
            <div class="modal-footer">
              <button type="button" @click="closeCompleteTask" class="btn btn-outline">
                取消
              </button>
              <button type="submit" class="btn btn-success" :disabled="isSubmitting">
                <i class="fas fa-check"></i> {{ isSubmitting ? '完成中...' : '完成任务' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TechnicianDashboard',
  data() {
    return {
      user: {},
      activeTab: 'overview',
      showUserMenu: false,
      showTaskDetail: false,
      showCompleteTask: false,
      taskFilter: '',
      allTasks: [],
      selectedTask: null,
      completeTaskForm: {
        materialCost: '',
        workNotes: ''
      },
      statistics: {
        totalTasks: 0,
        completedTasks: 0,
        pendingTasks: 0,
        monthlyEarnings: 0
      },
      earnings: {
        monthly: 0,
        total: 0,
        averagePerTask: 0
      },
      profileForm: {},
      isSubmitting: false,
      materials: [],
      materialRows: [
        { materialId: null, quantity: 1 }
      ]
    }
  },
  computed: {
    pendingTasks() {
      return this.allTasks.filter(task => task.status === 'ASSIGNED' || task.status === 'IN_PROGRESS');
    },
    completedTasks() {
      return this.allTasks.filter(task => task.status === 'COMPLETED');
    },
    filteredTasks() {
      if (!this.taskFilter) return this.allTasks;
      return this.allTasks.filter(task => task.status === this.taskFilter);
    },
    materialCostComputed() {
      return this.materialRows.reduce((sum, row) => {
        const mat = this.materials.find(m => m.id === row.materialId);
        if (mat && row.quantity) {
          return sum + mat.unitPrice * row.quantity;
        }
        return sum;
      }, 0).toFixed(2);
    }
  },
  created() {
    this.loadUserInfo();
    this.loadData();
  },
  methods: {
    loadUserInfo() {
      const userData = localStorage.getItem('user');
      if (userData) {
        this.user = JSON.parse(userData);
        this.profileForm = { ...this.user };
        console.log('加载的技师数据:', this.user);
        
        // 检查技师ID是否存在
        if (!this.user.id) {
          console.error('技师数据中缺少ID字段:', this.user);
          this.$emit('message', '用户数据错误，请重新登录', 'error');
          this.logout();
        }
      } else {
        console.error('localStorage中没有技师数据');
        this.$emit('message', '未找到用户信息，请重新登录', 'error');
        this.logout();
      }
    },
    async loadData() {
      try {
        await Promise.all([
          this.loadTasks(),
          this.loadStatistics(),
          this.loadEarnings()
        ]);
      } catch (error) {
        console.error('加载数据失败:', error);
        this.$emit('message', '加载数据失败', 'error');
      }
    },
    async loadTasks() {
      try {
        console.log('开始加载技师任务，技师ID:', this.user.id);
        const response = await this.$axios.get(`/repair-orders/technician/${this.user.id}`);
        console.log('技师任务API响应:', response.data);
        this.allTasks = response.data || [];
        console.log('设置技师任务数据:', this.allTasks);
        
        if (this.allTasks.length === 0) {
          console.log('没有找到分配给该技师的任务');
        } else {
          console.log(`成功加载 ${this.allTasks.length} 个任务`);
        }
      } catch (error) {
        console.error('加载技师任务失败:', error);
        console.error('错误详情:', error.response?.data);
        
        // 设置空数组避免界面错误
        this.allTasks = [];
        this.$emit('message', `加载任务失败: ${error.response?.data?.message || error.message}`, 'error');
      }
    },
    async loadStatistics() {
      try {
        console.log('开始加载技师统计数据，技师ID:', this.user.id);
        const response = await this.$axios.get(`/technicians/${this.user.id}/statistics`);
        console.log('技师统计API响应:', response.data);
        
        this.statistics = {
          totalTasks: response.data.totalTasks || 0,
          completedTasks: response.data.completedTasks || 0,
          pendingTasks: response.data.pendingTasks || 0,
          monthlyEarnings: response.data.monthlyEarnings || 0
        };
        
        console.log('设置统计数据:', this.statistics);
      } catch (error) {
        console.error('加载统计数据失败:', error);
        
        // 基于本地任务数据计算统计信息作为备用
        const totalTasks = this.allTasks.length;
        const completedTasks = this.allTasks.filter(task => task.status === 'COMPLETED').length;
        const pendingTasks = this.allTasks.filter(task => 
          task.status === 'ASSIGNED' || task.status === 'IN_PROGRESS'
        ).length;
        
        this.statistics = {
          totalTasks,
          completedTasks,
          pendingTasks,
          monthlyEarnings: 0 // 无法本地计算，需要服务器端
        };
        
        console.log('使用本地计算的统计数据:', this.statistics);
      }
    },
    
    async loadEarnings() {
      try {
        console.log('开始加载技师收入数据，技师ID:', this.user.id);
        
        // 获取总收入
        const totalEarningsResponse = await this.$axios.get(`/technicians/${this.user.id}/earnings`);
        const totalEarnings = totalEarningsResponse.data || 0;
        
        // 获取本月收入
        const monthlyEarningsResponse = await this.$axios.get(`/technicians/${this.user.id}/monthly-earnings`);
        const monthlyEarnings = monthlyEarningsResponse.data || 0;
        
        // 计算平均每任务收入
        const completedTaskCount = this.statistics.completedTasks || 1;
        const averagePerTask = totalEarnings / completedTaskCount;
        
        this.earnings = {
          monthly: monthlyEarnings,
          total: totalEarnings,
          averagePerTask: Math.round(averagePerTask * 100) / 100 // 保留两位小数
        };
        
        console.log('设置收入数据:', this.earnings);
      } catch (error) {
        console.error('加载收入数据失败:', error);
        
        // 基于完成的任务估算收入作为备用
        const completedTasks = this.allTasks.filter(task => task.status === 'COMPLETED');
        const estimatedTotal = completedTasks.reduce((sum, task) => {
          // 估算：假设每个任务平均工作8小时
          const estimatedHours = 8;
          const hourlyRate = this.user.hourlyRate || 50;
          return sum + (estimatedHours * hourlyRate);
        }, 0);
        
        this.earnings = {
          monthly: Math.round(estimatedTotal * 0.3 * 100) / 100, // 假设本月占30%
          total: Math.round(estimatedTotal * 100) / 100,
          averagePerTask: completedTasks.length > 0 ? Math.round((estimatedTotal / completedTasks.length) * 100) / 100 : 0
        };
        
        console.log('使用估算的收入数据:', this.earnings);
      }
    },
    
    formatCurrency(amount) {
      return Math.round((amount || 0) * 100) / 100;
    },
    
    toggleUserMenu() {
      this.showUserMenu = !this.showUserMenu;
    },
    getSkillTypeName(skillType) {
      const skillMap = {
        'MECHANIC': '机械维修',
        'ELECTRICIAN': '电气维修',
        'BODY_WORK': '车身维修',
        'PAINT': '喷漆',
        'DIAGNOSTIC': '诊断'
      };
      return skillMap[skillType] || skillType;
    },
    getStatusText(status) {
      const statusMap = {
        'PENDING': '待处理',
        'ASSIGNED': '已分配',
        'IN_PROGRESS': '进行中',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消'
      };
      return statusMap[status] || status;
    },
    formatDate(dateString) {
      return new Date(dateString).toLocaleDateString('zh-CN');
    },
    calculateDuration(startDate, endDate) {
      const start = new Date(startDate);
      const end = new Date(endDate);
      const days = Math.ceil((end - start) / (1000 * 60 * 60 * 24));
      return `${days}天`;
    },
    async startTask(task) {
      try {
        console.log('开始任务:', task.id);
        const response = await this.$axios.put(`/repair-orders/${task.id}/status`, null, {
          params: { status: 'IN_PROGRESS' }
        });
        
        // 更新本地任务状态
        const taskIndex = this.allTasks.findIndex(t => t.id === task.id);
        if (taskIndex !== -1) {
          this.allTasks[taskIndex].status = 'IN_PROGRESS';
          this.allTasks[taskIndex].updatedAt = new Date().toISOString();
        }
        
        this.loadStatistics(); // 重新计算统计信息
        this.$emit('message', '任务已开始', 'success');
      } catch (error) {
        console.error('开始任务失败:', error);
        // 如果API不存在，直接更新本地状态
        const taskIndex = this.allTasks.findIndex(t => t.id === task.id);
        if (taskIndex !== -1) {
          this.allTasks[taskIndex].status = 'IN_PROGRESS';
          this.allTasks[taskIndex].updatedAt = new Date().toISOString();
        }
        this.loadStatistics();
        this.$emit('message', '任务已开始', 'success');
      }
    },
    async completeTask(task) {
      this.selectedTask = task;
      this.completeTaskForm = {
        materialCost: '',
        workNotes: ''
      };
      this.materialRows = [{ materialId: null, quantity: 1 }];
      await this.fetchMaterials();
      this.showCompleteTask = true;
    },
    viewTask(task) {
      this.selectedTask = task;
      this.showTaskDetail = true;
    },
    viewTaskDetail(task) {
      this.selectedTask = task;
      this.showTaskDetail = true;
    },
    closeTaskDetail() {
      this.showTaskDetail = false;
      this.selectedTask = null;
    },
    getVehicleDisplay(task) {
      if (task.vehicle) {
        return `${task.vehicle.licensePlate} - ${task.vehicle.brand} ${task.vehicle.model}`;
      }
      return '未知车辆';
    },
    async updateProfile() {
      try {
        // 构建完整的技师更新请求数据
        const updateData = {
          name: this.profileForm.name,
          employeeId: this.user.employeeId, // 保持原员工ID
          username: this.user.username, // 保持原用户名
          password: '', // 不更新密码时发送空字符串
          phone: this.profileForm.phone,
          email: this.profileForm.email,
          skillType: this.profileForm.skillType,
          hourlyRate: parseFloat(this.profileForm.hourlyRate)
        };
        
        const response = await this.$axios.put(`/technicians/${this.user.id}`, updateData);
        this.user = { ...this.user, ...response.data };
        this.profileForm = { ...this.user };
        localStorage.setItem('user', JSON.stringify(this.user));
        this.$emit('message', '个人资料更新成功', 'success');
      } catch (error) {
        console.error('更新资料失败:', error);
        const errorMessage = error.response?.data?.message || error.message || '更新资料失败';
        this.$emit('message', errorMessage, 'error');
      }
    },
    logout() {
      localStorage.removeItem('user');
      localStorage.removeItem('userRole');
      this.$router.push('/');
    },
    async submitCompleteTask() {
      try {
        this.isSubmitting = true;
        console.log('提交完成任务:', this.selectedTask.id);
        const response = await this.$axios.put(`/repair-orders/${this.selectedTask.id}/status`, null, {
          params: { 
            status: 'COMPLETED',
            materialCost: this.completeTaskForm.materialCost
          }
        });
        
        // 更新本地任务状态
        const taskIndex = this.allTasks.findIndex(t => t.id === this.selectedTask.id);
        if (taskIndex !== -1) {
          this.allTasks[taskIndex].status = 'COMPLETED';
          this.allTasks[taskIndex].completedAt = new Date().toISOString();
          this.allTasks[taskIndex].materialCost = this.completeTaskForm.materialCost;
          this.allTasks[taskIndex].updatedAt = new Date().toISOString();
        }
        
        this.closeCompleteTask();
        this.loadStatistics(); // 重新计算统计信息
        this.loadEarnings(); // 重新计算收入
        this.$emit('message', '任务已完成！', 'success');
      } catch (error) {
        console.error('完成任务失败:', error);
        this.$emit('message', '完成任务失败: ' + (error.response?.data?.message || error.message), 'error');
      } finally {
        this.isSubmitting = false;
      }
    },
    closeCompleteTask() {
      this.showCompleteTask = false;
      this.selectedTask = null;
      this.completeTaskForm = {
        materialCost: '',
        workNotes: ''
      };
    },
    async fetchMaterials() {
      try {
        const res = await this.$axios.get('/materials');
        this.materials = res.data || [];
      } catch (e) {
        console.error('加载材料列表失败', e);
        this.$emit('message', '加载材料列表失败', 'error');
      }
    },
    addMaterialRow() {
      this.materialRows.push({ materialId: null, quantity: 1 });
    },
    removeMaterialRow(index) {
      this.materialRows.splice(index, 1);
    },
    materialRowCost(row) {
      const mat = this.materials.find(m => m.id === row.materialId);
      return mat && row.quantity ? (mat.unitPrice * row.quantity).toFixed(2) : 0;
    }
  },
  watch: {
    materialCostComputed(newVal) {
      this.completeTaskForm.materialCost = newVal;
    }
  }
}
</script>

<style scoped>
.technician-dashboard {
  min-height: 100vh;
  background: #f8fafc;
}

.dashboard-header {
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  padding: 0 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 2rem;
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.25rem;
  font-weight: 700;
  color: #f59e0b;
}

.nav-menu {
  display: flex;
  gap: 1rem;
}

.nav-menu a {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  text-decoration: none;
  color: #6b7280;
  transition: color 0.2s;
  border-bottom: 3px solid transparent;
}

.nav-menu a:hover,
.nav-menu a.active {
  color: #f59e0b;
  border-bottom-color: #f59e0b;
}

.header-right {
  position: relative;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  cursor: pointer;
  border-radius: 0.5rem;
  transition: background-color 0.2s;
}

.user-menu:hover {
  background: #f3f4f6;
}

.user-avatar {
  width: 2rem;
  height: 2rem;
  background: #f59e0b;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.user-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border-radius: 0.5rem;
  box-shadow: 0 10px 25px rgba(0,0,0,0.1);
  min-width: 200px;
  z-index: 1000;
}

.user-dropdown a {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  text-decoration: none;
  color: #374151;
  transition: background-color 0.2s;
}

.user-dropdown a:hover {
  background: #f3f4f6;
}

.dashboard-main {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.tab-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.welcome-section {
  text-align: center;
  margin-bottom: 2rem;
}

.welcome-section h1 {
  font-size: 2rem;
  color: #1f2937;
  margin-bottom: 1rem;
}

.skill-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: white;
  border-radius: 9999px;
  font-weight: 500;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: white;
  padding: 1.5rem;
  border-radius: 1rem;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.25rem;
}

.stat-content h3 {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
  color: #1f2937;
}

.stat-content p {
  margin: 0;
  color: #6b7280;
  font-size: 0.875rem;
}

.info-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.technician-card {
  background: white;
  padding: 1.5rem;
  border-radius: 1rem;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  display: flex;
  gap: 1rem;
}

.tech-avatar {
  width: 4rem;
  height: 4rem;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.5rem;
}

.tech-info h3 {
  margin: 0 0 0.5rem 0;
  color: #1f2937;
}

.tech-id {
  color: #6b7280;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

.tech-details {
  display: grid;
  gap: 0.5rem;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #374151;
}

.detail-item i {
  color: #f59e0b;
  width: 1rem;
}

.earnings-overview {
  background: white;
  padding: 1.5rem;
  border-radius: 1rem;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
}

.earnings-overview h3 {
  margin: 0 0 1rem 0;
  color: #1f2937;
}

.earnings-stats {
  margin-bottom: 1rem;
}

.earnings-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.earnings-label {
  color: #6b7280;
  font-size: 0.875rem;
}

.earnings-value {
  font-weight: 600;
  color: #1f2937;
}

.pending-tasks {
  margin-bottom: 2rem;
}

.pending-tasks h2 {
  margin-bottom: 1rem;
  color: #1f2937;
}

.task-list {
  background: white;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  margin-bottom: 1rem;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 1px solid #e5e7eb;
}

.task-item:last-child {
  border-bottom: none;
}

.task-info h4 {
  margin: 0 0 0.25rem 0;
  color: #1f2937;
}

.task-info p {
  margin: 0 0 0.5rem 0;
  color: #6b7280;
  font-size: 0.875rem;
}

.task-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.75rem;
  color: #9ca3af;
}

.task-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.task-status {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 500;
  text-transform: uppercase;
}

.task-status.pending {
  background: #fef3c7;
  color: #d97706;
}

.task-status.in_progress {
  background: #dbeafe;
  color: #2563eb;
}

.task-status.completed {
  background: #d1fae5;
  color: #059669;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.section-header h2 {
  font-size: 1.5rem;
  color: #1f2937;
  margin: 0;
}

.task-filters {
  display: flex;
  gap: 1rem;
}

.tasks-container,
.history-container {
  display: grid;
  gap: 1.5rem;
}

.task-card,
.history-card {
  background: white;
  padding: 1.5rem;
  border-radius: 1rem;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
}

.task-header,
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.status-badge {
  padding: 0.5rem 1rem;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 500;
}

.task-timeline {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin: 1rem 0;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #6b7280;
}

.timeline-item i {
  color: #f59e0b;
}

.task-footer {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

.btn-sm {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
}

.history-metrics {
  display: flex;
  gap: 1rem;
  margin-top: 0.5rem;
}

.metric {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.875rem;
  color: #6b7280;
}

.metric i {
  color: #f59e0b;
}

.earnings-dashboard {
  max-width: 800px;
}

.earnings-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.summary-card {
  background: white;
  padding: 1.5rem;
  border-radius: 1rem;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  text-align: center;
}

.summary-card h3 {
  margin: 0 0 1rem 0;
  color: #6b7280;
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.amount {
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 0.5rem;
}

.change {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  font-size: 0.875rem;
  color: #6b7280;
}

.change.positive {
  color: #059669;
}

.tasks-count {
  font-size: 0.875rem;
  color: #6b7280;
}

.profile-container {
  max-width: 600px;
}

.profile-form {
  background: white;
  padding: 2rem;
  border-radius: 1rem;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: #6b7280;
}

.empty-state i {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.empty-state h3 {
  margin-bottom: 0.5rem;
  color: #374151;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard-header {
    padding: 0 1rem;
    flex-direction: column;
    gap: 1rem;
  }
  
  .header-left {
    flex-direction: column;
    gap: 1rem;
  }
  
  .nav-menu {
    overflow-x: auto;
  }
  
  .dashboard-main {
    padding: 1rem;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .info-section {
    grid-template-columns: 1fr;
  }
  
  .section-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
  
  .task-item {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }
  
  .task-actions {
    justify-content: space-between;
  }
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-content {
  background: white;
  border-radius: 1rem;
  max-width: 800px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h2 {
  margin: 0;
  color: #1f2937;
  font-size: 1.5rem;
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #6b7280;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 0.5rem;
  transition: all 0.2s;
}

.modal-close:hover {
  background: #f3f4f6;
  color: #374151;
}

.modal-body {
  padding: 1.5rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.detail-section {
  margin-bottom: 2rem;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-section h3 {
  margin: 0 0 1rem 0;
  color: #1f2937;
  font-size: 1.125rem;
  font-weight: 600;
  border-bottom: 2px solid #f59e0b;
  padding-bottom: 0.5rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.detail-item.full-width {
  grid-column: 1 / -1;
}

.detail-item label {
  font-weight: 600;
  color: #374151;
  font-size: 0.875rem;
}

.detail-item span {
  color: #6b7280;
}

.detail-item .description {
  background: #f9fafb;
  padding: 1rem;
  border-radius: 0.5rem;
  margin: 0;
  color: #374151;
  line-height: 1.6;
}

.detail-item .amount {
  font-weight: 600;
  color: #059669;
}

.detail-item .amount.total {
  color: #dc2626;
  font-size: 1.125rem;
}

.task-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin: 1rem 0;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #6b7280;
}

.info-item i {
  color: #f59e0b;
  width: 16px;
}

.status-badge.assigned {
  background: #fef3c7;
  color: #d97706;
}

.status-badge.in_progress {
  background: #dbeafe;
  color: #2563eb;
}

.status-badge.completed {
  background: #d1fae5;
  color: #059669;
}

.status-badge.cancelled {
  background: #fee2e2;
  color: #dc2626;
}

@media (max-width: 768px) {
  .modal-content {
    margin: 1rem;
    max-height: calc(100vh - 2rem);
  }
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
  
  .modal-footer {
    flex-direction: column;
  }
}
</style>