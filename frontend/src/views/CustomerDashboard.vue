<template>
  <div class="customer-dashboard">
    <!-- 顶部导航栏 -->
    <header class="dashboard-header">
      <div class="header-left">
        <div class="logo">
          <i class="fas fa-car"></i>
          <span>维修系统</span>
        </div>
        <nav class="nav-menu">
          <a href="#" @click="activeTab = 'overview'" :class="{ active: activeTab === 'overview' }">
            <i class="fas fa-home"></i> 概览
          </a>
          <a href="#" @click="activeTab = 'vehicles'" :class="{ active: activeTab === 'vehicles' }">
            <i class="fas fa-car"></i> 我的车辆
          </a>
          <a href="#" @click="activeTab = 'orders'" :class="{ active: activeTab === 'orders' }">
            <i class="fas fa-wrench"></i> 维修记录
          </a>
          <a href="#" @click="activeTab = 'feedback'" :class="{ active: activeTab === 'feedback' }">
            <i class="fas fa-comment"></i> 反馈
          </a>
        </nav>
      </div>
      <div class="header-right">
        <div class="user-menu" @click="toggleUserMenu">
          <div class="user-avatar">
            <i class="fas fa-user"></i>
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
          <h1>欢迎回来，{{ user.name || user.username }}！</h1>
          <p>管理您的车辆和维修服务</p>
        </div>

        <!-- 统计卡片 -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon">
              <i class="fas fa-car"></i>
            </div>
            <div class="stat-content">
              <h3>{{ statistics.vehicleCount }}</h3>
              <p>我的车辆</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">
              <i class="fas fa-wrench"></i>
            </div>
            <div class="stat-content">
              <h3>{{ statistics.repairCount }}</h3>
              <p>维修次数</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">
              <i class="fas fa-clock"></i>
            </div>
            <div class="stat-content">
              <h3>{{ statistics.pendingCount }}</h3>
              <p>待处理</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">
              <i class="fas fa-dollar-sign"></i>
            </div>
            <div class="stat-content">
              <h3>¥{{ formatCurrency(statistics.totalCost) }}</h3>
              <p>总费用</p>
            </div>
          </div>
        </div>

        <!-- 快速操作 -->
        <div class="quick-actions">
          <h2>快速操作</h2>
          <div class="action-grid">
            <button class="action-card" @click="activeTab = 'vehicles'">
              <i class="fas fa-plus-circle"></i>
              <span>添加车辆</span>
            </button>
            <button class="action-card" @click="activeTab = 'orders'">
              <i class="fas fa-calendar-plus"></i>
              <span>预约维修</span>
            </button>
            <button class="action-card" @click="activeTab = 'orders'">
              <i class="fas fa-history"></i>
              <span>查看记录</span>
            </button>
            <button class="action-card" @click="activeTab = 'feedback'">
              <i class="fas fa-star"></i>
              <span>评价反馈</span>
            </button>
          </div>
        </div>

        <!-- 最近维修记录 -->
        <div class="recent-section">
          <h2>最近维修记录</h2>
          <div class="recent-orders">
            <div v-if="recentOrders.length === 0" class="empty-state">
              <i class="fas fa-inbox"></i>
              <p>暂无维修记录</p>
            </div>
            <div v-for="order in recentOrders" :key="order.id" class="order-item">
              <div class="order-info">
                <h4>{{ getVehicleDisplay(order) }}</h4>
                <p>{{ order.description }}</p>
                <small>{{ formatDate(order.createdAt) }}</small>
              </div>
              <div class="order-status">
                <span :class="['status', order.status.toLowerCase()]">
                  {{ getStatusText(order.status) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 车辆管理页面 -->
      <div v-if="activeTab === 'vehicles'" class="tab-content">
        <div class="section-header">
          <h2>我的车辆</h2>
          <button @click="showAddVehicle = true" class="btn btn-primary">
            <i class="fas fa-plus"></i> 添加车辆
          </button>
        </div>
        
        <div class="vehicles-grid">
          <div v-if="vehicles.length === 0" class="empty-state">
            <i class="fas fa-car"></i>
            <h3>还没有车辆</h3>
            <p>添加您的第一辆车开始管理</p>
            <button @click="showAddVehicle = true" class="btn btn-primary">
              <i class="fas fa-plus"></i> 添加车辆
            </button>
          </div>
          <div v-for="vehicle in vehicles" :key="vehicle.id" class="vehicle-card">
            <div class="vehicle-header">
              <h3>{{ vehicle.licensePlate || '未知车牌' }}</h3>
              <div class="vehicle-actions">
                <button @click="editVehicle(vehicle)" class="btn-icon">
                  <i class="fas fa-edit"></i>
                </button>
                <button @click="deleteVehicle(vehicle.id)" class="btn-icon danger">
                  <i class="fas fa-trash"></i>
                </button>
              </div>
            </div>
            <div class="vehicle-info">
              <p><strong>品牌:</strong> {{ vehicle.brand || '未知' }}</p>
              <p><strong>车型:</strong> {{ vehicle.model || '未知' }}</p>
              <p><strong>年份:</strong> {{ vehicle.year || '未知' }}</p>
              <p><strong>颜色:</strong> {{ vehicle.color || '未设置' }}</p>
              <p><strong>维修次数:</strong> {{ (vehicle.repairOrders && vehicle.repairOrders.length) || 0 }}</p>
            </div>
            <button @click="createRepairOrder(vehicle)" class="btn btn-outline">
              <i class="fas fa-wrench"></i> 预约维修
            </button>
          </div>
        </div>
      </div>

      <!-- 维修记录页面 -->
      <div v-if="activeTab === 'orders'" class="tab-content">
        <div class="section-header">
          <h2>维修记录</h2>
          <button @click="showCreateOrder = true" class="btn btn-primary">
            <i class="fas fa-plus"></i> 新建维修单
          </button>
        </div>

        <div class="orders-container">
          <div v-if="repairOrders.length === 0" class="empty-state">
            <i class="fas fa-wrench"></i>
            <h3>暂无维修记录</h3>
            <p>预约您的第一次维修服务</p>
            <button @click="showCreateOrder = true" class="btn btn-primary">
              <i class="fas fa-plus"></i> 预约维修
            </button>
          </div>
          <div v-for="order in repairOrders" :key="order.id" class="order-card">
            <div class="order-header">
              <div>
                <h3>维修单 #{{ order.id }}</h3>
                <p class="order-vehicle">{{ getVehicleDisplay(order) }}</p>
              </div>
              <span :class="['status-badge', order.status.toLowerCase()]">
                {{ getStatusText(order.status) }}
              </span>
            </div>
            <div class="order-body">
              <p><strong>故障描述:</strong> {{ order.description }}</p>
              <p><strong>开始时间:</strong> {{ formatDate(order.createdAt) }}</p>
              <p><strong>预估费用:</strong> ¥{{ (order.laborCost || 0) + (order.materialCost || 0) }}</p>
              <p v-if="order.totalCost"><strong>实际费用:</strong> ¥{{ order.totalCost }}</p>
            </div>
            <div class="order-footer">
              <button v-if="order.status === 'COMPLETED'" @click="addFeedback(order)" class="btn btn-outline">
                <i class="fas fa-star"></i> 评价
              </button>
              <button v-if="order.status === 'IN_PROGRESS' && order.urgeStatus === 'NOT_URGED'" 
                      @click="urgeOrder(order)" 
                      class="btn btn-warning">
                <i class="fas fa-bell"></i> 催单
              </button>
              <button v-if="order.status === 'IN_PROGRESS' && order.urgeStatus === 'URGED'" 
                      class="btn btn-warning" 
                      disabled>
                <i class="fas fa-bell"></i> 已催单
              </button>
              <button @click="viewOrderDetail(order)" class="btn btn-primary">
                <i class="fas fa-eye"></i> 查看详情
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 反馈页面 -->
      <div v-if="activeTab === 'feedback'" class="tab-content">
        <div class="section-header">
          <h2>我的反馈</h2>
        </div>
        
        <div class="feedback-container">
          <div v-if="completedOrdersWithoutFeedback.length === 0" class="empty-state">
            <i class="fas fa-comments"></i>
            <h3>暂无待评价订单</h3>
            <p>完成维修后可以提交反馈</p>
          </div>
          
          <!-- 显示可反馈的维修单 -->
          <div v-for="order in completedOrdersWithoutFeedback" :key="'order-' + order.id" class="feedback-prompt">
            <div class="feedback-header">
              <small>{{ formatDate(order.completedAt || order.createdAt) }}</small>
            </div>
            <p class="feedback-prompt-text">维修单 #{{ order.id }} 等待您的评价</p>
            <button @click="openFeedbackModal(order)" class="btn btn-outline">
              <i class="fas fa-star"></i> 立即评价
            </button>
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
              <label class="form-label">地址</label>
              <input v-model="profileForm.address" class="form-input" required>
            </div>
            <button type="submit" class="btn btn-primary">
              <i class="fas fa-save"></i> 保存更改
            </button>
          </form>
        </div>
      </div>
    </main>

    <!-- 添加车辆模态框 -->
    <div v-if="showAddVehicle" class="modal-overlay" @click="showAddVehicle = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>添加车辆</h3>
          <button @click="showAddVehicle = false" class="modal-close">&times;</button>
        </div>
        <form @submit.prevent="addVehicle" class="modal-body">
          <div class="form-group">
            <label class="form-label">车牌号 <span class="required">*</span></label>
            <input v-model="vehicleForm.licensePlate" class="form-input" placeholder="例：京A12345" required>
          </div>
          <div class="form-group">
            <label class="form-label">品牌 <span class="required">*</span></label>
            <input v-model="vehicleForm.brand" class="form-input" placeholder="例：大众" required>
          </div>
          <div class="form-group">
            <label class="form-label">车型 <span class="required">*</span></label>
            <input v-model="vehicleForm.model" class="form-input" placeholder="例：速腾" required>
          </div>
          <div class="form-group">
            <label class="form-label">年份 <span class="required">*</span></label>
            <input v-model="vehicleForm.year" type="number" min="1980" :max="new Date().getFullYear()" class="form-input" required>
          </div>
          <div class="form-group">
            <label class="form-label">颜色</label>
            <input v-model="vehicleForm.color" class="form-input" placeholder="例：白色">
          </div>
          <div class="form-group">
            <label class="form-label">车架号</label>
            <input v-model="vehicleForm.vin" class="form-input" placeholder="17位车架号">
          </div>
          <div class="modal-footer">
            <button type="button" @click="showAddVehicle = false" class="btn btn-outline">
              取消
            </button>
            <button type="submit" class="btn btn-primary" :disabled="isSubmitting">
              <i class="fas fa-plus"></i> {{ isSubmitting ? '添加中...' : '添加车辆' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 预约维修模态框 -->
    <div v-if="showCreateOrder" class="modal-overlay" @click="showCreateOrder = false">
      <div class="modal large" @click.stop>
        <div class="modal-header">
          <h3>预约维修</h3>
          <button @click="showCreateOrder = false" class="modal-close">&times;</button>
        </div>
        <form @submit.prevent="submitRepairOrder" class="modal-body">
          <div class="form-group">
            <label class="form-label">选择车辆 <span class="required">*</span></label>
            <select v-model="repairOrderForm.vehicleId" class="form-input" required>
              <option value="">请选择车辆</option>
              <option v-for="vehicle in vehicles" :key="vehicle.id" :value="vehicle.id">
                {{ vehicle.licensePlate }} - {{ vehicle.brand }} {{ vehicle.model }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">故障描述 <span class="required">*</span></label>
            <textarea v-model="repairOrderForm.description" class="form-input" rows="4" 
                      placeholder="请详细描述车辆故障情况，如：发动机异响、刹车片磨损等" required></textarea>
            <button type="button" @click="getIntelligentDiagnosis" class="btn btn-ai" 
                    :disabled="!repairOrderForm.description || !repairOrderForm.vehicleId || isDiagnosing"
                    style="margin-top: 8px;">
              <i class="fas fa-brain"></i> {{ isDiagnosing ? 'AI诊断中...' : 'AI智能诊断' }}
            </button>
          </div>
          
          <!-- AI诊断结果展示 -->
          <div v-if="diagnosisResult" class="diagnosis-result">
            <div class="diagnosis-header">
              <i class="fas fa-robot"></i>
              <h4>AI诊断结果</h4>
            </div>
            <div class="diagnosis-content">
              <div class="diagnosis-item">
                <strong>故障类型:</strong> {{ diagnosisResult.faultType }}
              </div>
              <div class="diagnosis-item">
                <strong>可能原因:</strong> {{ diagnosisResult.possibleCause }}
              </div>
              <div class="diagnosis-item">
                <strong>严重程度:</strong> 
                <span :class="getSeverityClass(diagnosisResult.estimatedSeverity)">
                  {{ diagnosisResult.estimatedSeverity }}
                </span>
              </div>
              <div class="diagnosis-item">
                <strong>预估费用:</strong> ¥{{ diagnosisResult.estimatedCost }}
              </div>
              <div class="diagnosis-item">
                <strong>建议维修类型:</strong> {{ getSkillTypeName(diagnosisResult.skillTypeRequired) }}
              </div>
              <div class="diagnosis-item">
                <strong>建议措施:</strong>
                <ul class="recommendations">
                  <li v-for="(action, index) in diagnosisResult.recommendedActions" :key="index">
                    {{ action }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
          
          <div class="form-group">
            <label class="form-label">维修类型 <span class="required">*</span></label>
            <select v-model="repairOrderForm.requiredSkillType" class="form-input" required>
              <option value="">请选择维修类型</option>
              <option value="MECHANIC">机械维修</option>
              <option value="ELECTRICIAN">电气维修</option>
              <option value="BODY_WORK">车身维修</option>
              <option value="PAINT">喷漆</option>
              <option value="DIAGNOSTIC">故障诊断</option>
            </select>
            <small class="form-help">
              <i class="fas fa-info-circle"></i> 
              我们会根据您选择的维修类型自动分配最合适的技师。如暂无对应技师，请您稍后再试或选择其他维修类型。
            </small>
          </div>
          <div class="form-group">
            <label class="form-label">预约时间</label>
            <input v-model="repairOrderForm.preferredDate" type="datetime-local" class="form-input"
                   :min="new Date().toISOString().slice(0, 16)">
          </div>
          <div class="form-group">
            <label class="form-label">联系方式</label>
            <input v-model="repairOrderForm.contactPhone" type="tel" class="form-input" 
                   :placeholder="user.phone">
          </div>
          <div class="modal-footer">
            <button type="button" @click="showCreateOrder = false" class="btn btn-outline">
              取消
            </button>
            <button type="submit" class="btn btn-primary" :disabled="isSubmitting">
              <i class="fas fa-calendar-plus"></i> {{ isSubmitting ? '提交中...' : '提交预约' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 维修详情模态框 -->
    <div v-if="showOrderDetail" class="modal-overlay" @click="showOrderDetail = false">
      <div class="modal large" @click.stop>
        <div class="modal-header">
          <h3>维修单详情</h3>
          <button @click="showOrderDetail = false" class="modal-close">&times;</button>
        </div>
        <div class="modal-body" v-if="selectedOrder">
          <div class="detail-section">
            <h4>基本信息</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">维修单号:</span>
                <span class="detail-value">#{{ selectedOrder.id }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">状态:</span>
                <span :class="['status-badge', selectedOrder.status.toLowerCase()]">
                  {{ getStatusText(selectedOrder.status) }}
                </span>
              </div>
              <div class="detail-item">
                <span class="detail-label">车辆:</span>
                <span class="detail-value">{{ getVehicleDisplay(selectedOrder) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">创建时间:</span>
                <span class="detail-value">{{ formatDate(selectedOrder.createdAt) }}</span>
              </div>
            </div>
          </div>
          
          <div class="detail-section">
            <h4>故障描述</h4>
            <p class="fault-description">{{ selectedOrder.description }}</p>
          </div>

          <div class="detail-section" v-if="selectedOrder.technicians && selectedOrder.technicians.length > 0">
            <h4>负责技师</h4>
            <div class="technician-list">
              <div v-for="tech in selectedOrder.technicians" :key="tech.id" class="technician-item">
                <div class="tech-avatar">
                  <i class="fas fa-user-hard-hat"></i>
                </div>
                <div class="tech-info">
                  <span class="tech-name">{{ tech.name }}</span>
                  <span class="tech-skill">{{ getSkillTypeName(tech.skillType) }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h4>费用信息</h4>
            <div class="cost-breakdown">
              <div class="cost-item">
                <span>人工费:</span>
                <span>¥{{ selectedOrder.laborCost || 0 }}</span>
              </div>
              <div class="cost-item">
                <span>材料费:</span>
                <span>¥{{ selectedOrder.materialCost || 0 }}</span>
              </div>
              <div class="cost-item total">
                <span>总计:</span>
                <span>¥{{ selectedOrder.totalCost || selectedOrder.laborCost + selectedOrder.materialCost || 0 }}</span>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button v-if="selectedOrder.status === 'COMPLETED' && !hasUserFeedback(selectedOrder)" 
                    @click="openFeedbackModal(selectedOrder)" class="btn btn-primary">
              <i class="fas fa-star"></i> 评价服务
            </button>
            <button @click="showOrderDetail = false" class="btn btn-outline">
              关闭
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 评价反馈模态框 -->
    <div v-if="showFeedback" class="modal-overlay" @click="showFeedback = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>评价服务</h3>
          <button @click="showFeedback = false" class="modal-close">&times;</button>
        </div>
        <form @submit.prevent="submitFeedback" class="modal-body" v-if="feedbackOrder">
          <div class="feedback-order-info">
            <h4>维修单 #{{ feedbackOrder.id }}</h4>
            <p>{{ getVehicleDisplay(feedbackOrder) }} - {{ feedbackOrder.description }}</p>
          </div>
          
          <!-- 服务满意度评分 -->
          <div class="form-group">
            <label class="form-label">服务满意度</label>
            <div class="rating-container">
              <div class="star-rating">
                <span 
                  v-for="star in 5" 
                  :key="star" 
                  class="star" 
                  :class="{ 'active': star <= feedbackForm.rating }" 
                  @click="feedbackForm.rating = star"
                >
                  <i class="fas fa-star"></i>
                </span>
              </div>
              <span class="rating-text">{{ getRatingText(feedbackForm.rating) }}</span>
            </div>
          </div>
          
          <div class="form-group">
            <label class="form-label">评价内容</label>
            <textarea v-model="feedbackForm.comment" class="form-input" rows="4" 
                      placeholder="请分享您对本次维修服务的感受和建议..."></textarea>
          </div>
          
          <div class="modal-footer">
            <button type="button" @click="showFeedback = false" class="btn btn-outline">
              取消
            </button>
            <button type="submit" class="btn btn-primary" :disabled="isSubmitting || !isValidFeedback">
              <i class="fas fa-paper-plane"></i> {{ isSubmitting ? '提交中...' : '提交评价' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CustomerDashboard',
  data() {
    return {
      user: null,
      vehicles: [],
      repairOrders: [],
      recentOrders: [],
      feedbacks: [],
      completedOrdersWithoutFeedback: [],
      activeTab: 'dashboard',
      showUserMenu: false,
      showAddVehicle: false,
      showCreateOrder: false,
      showOrderDetail: false,
      showFeedback: false,
      selectedOrder: null,
      feedbackOrder: null,
      isSubmitting: false,
      isDiagnosing: false,
      diagnosisResult: null,
      vehicleForm: {
        licensePlate: '',
        brand: '',
        model: '',
        year: '',
        color: '',
        vin: ''
      },
      repairOrderForm: {
        vehicleId: '',
        description: '',
        preferredDate: '',
        contactPhone: '',
        requiredSkillType: ''
      },
      feedbackForm: {
        comment: '',
        rating: 0
      },
      profileForm: {
        name: '',
        phone: '',
        email: '',
        address: ''
      },
      statistics: {
        vehicleCount: 0,
        repairCount: 0,
        pendingCount: 0,
        totalCost: 0
      }
    }
  },
  computed: {
    isValidFeedback() {
      return this.feedbackForm.rating > 0 && this.feedbackForm.comment.trim().length > 0;
    }
  },
  created() {
    this.loadUserInfo();
    this.loadData();
  },
  methods: {
    loadUserInfo() {
      const userData = localStorage.getItem('user');
      console.log('从localStorage读取用户数据:', userData);
      
      if (userData) {
        this.user = JSON.parse(userData);
        this.profileForm = { ...this.user };
        console.log('解析后的用户数据:', this.user);
        console.log('用户ID:', this.user.id);
        
        // 检查用户ID是否存在
        if (!this.user.id) {
          console.error('用户数据中缺少ID字段:', this.user);
          this.$emit('message', '用户数据错误，请重新登录', 'error');
          this.logout();
          return;
        }
        
        console.log('用户信息加载成功，准备加载相关数据');
      } else {
        console.error('localStorage中没有用户数据');
        this.$emit('message', '未找到用户信息，请重新登录', 'error');
        this.logout();
      }
    },
    async loadData() {
      try {
        await Promise.all([
          this.loadVehicles(),
          this.loadRepairOrders(),
          this.loadFeedbacks()
        ]);
        this.calculateStatistics();
      } catch (error) {
        console.error('加载数据失败:', error);
        this.$emit('message', '加载数据失败', 'error');
      }
    },
    async loadVehicles() {
      try {
        console.log('开始加载车辆数据，用户ID:', this.user.id);
        const response = await this.$axios.get(`/vehicles/user/${this.user.id}`);
        console.log('车辆API响应:', response.data);
        
        if (Array.isArray(response.data)) {
          this.vehicles = response.data;
          console.log('成功设置车辆数据，共', this.vehicles.length, '辆车');
          this.vehicles.forEach((vehicle, index) => {
            console.log(`车辆${index + 1}:`, {
              id: vehicle.id,
              licensePlate: vehicle.licensePlate,
              brand: vehicle.brand,
              model: vehicle.model,
              repairOrders: vehicle.repairOrders?.length || 0
            });
          });
          
          // 更新统计信息
          this.calculateStatistics();
        } else {
          console.error('API返回的数据不是数组:', response.data);
          this.vehicles = [];
        }
      } catch (error) {
        console.error('加载车辆失败:', error);
        console.error('错误详情:', error.response?.data);
        this.$emit('message', `加载车辆失败: ${error.response?.data?.message || error.message}`, 'error');
        this.vehicles = [];
      }
    },
    async loadRepairOrders() {
      try {
        console.log('开始加载维修记录，用户ID:', this.user.id);
        const response = await this.$axios.get(`/repair-orders/user/${this.user.id}`);
        console.log('维修记录API响应:', response.data);
        
        this.repairOrders = response.data || [];
        this.recentOrders = this.repairOrders.slice(0, 5);
        
        console.log('设置维修记录数据:', this.repairOrders);
        
        // 检查每条记录的车辆信息
        this.repairOrders.forEach((order, index) => {
          console.log(`维修记录${index + 1}:`, {
            id: order.id,
            vehicleId: order.vehicleId,
            vehicle: order.vehicle,
            vehicleLicensePlate: order.vehicle?.licensePlate
          });
        });
        
        if (this.repairOrders.length === 0) {
          console.log('没有找到维修记录');
        } else {
          console.log(`成功加载 ${this.repairOrders.length} 条维修记录`);
        }
      } catch (error) {
        console.error('加载维修记录失败:', error);
        console.error('错误详情:', error.response?.data);
        this.$emit('message', `加载维修记录失败: ${error.response?.data?.message || error.message}`, 'error');
      }
    },
    async loadFeedbacks() {
      try {
        const response = await this.$axios.get(`/feedbacks/user/${this.user.id}`);
        this.feedbacks = response.data;
        this.completedOrdersWithoutFeedback = this.repairOrders.filter(order => order.status === 'COMPLETED' && !this.hasUserFeedback(order));
      } catch (error) {
        console.error('加载反馈失败:', error);
      }
    },
    calculateStatistics() {
      this.statistics = {
        vehicleCount: this.vehicles.length,
        repairCount: this.repairOrders.length,
        pendingCount: this.repairOrders.filter(order => 
          order.status === 'PENDING' || order.status === 'IN_PROGRESS' || order.status === 'ASSIGNED'
        ).length,
        totalCost: this.formatCurrency(
          this.repairOrders.reduce((sum, order) => 
            sum + (order.totalCost || order.laborCost + order.materialCost || 0), 0
          )
        )
      };
      
      // 调试信息
      console.log('所有维修订单:', this.repairOrders);
      console.log('已完成订单:', this.repairOrders.filter(order => order.status === 'COMPLETED'));
      console.log('所有反馈:', this.feedbacks);
      
      // 更新可反馈的维修单列表
      this.completedOrdersWithoutFeedback = this.repairOrders
        .filter(order => order.status === 'COMPLETED' && !this.hasUserFeedback(order));
      
      console.log('可反馈的订单:', this.completedOrdersWithoutFeedback);
    },
    
    formatCurrency(amount) {
      return Math.round((amount || 0) * 100) / 100;
    },
    
    toggleUserMenu() {
      this.showUserMenu = !this.showUserMenu;
    },
    async addVehicle() {
      try {
        this.isSubmitting = true;
        const vehicleData = {
          licensePlate: this.vehicleForm.licensePlate,
          brand: this.vehicleForm.brand,
          model: this.vehicleForm.model,
          year: parseInt(this.vehicleForm.year),
          color: this.vehicleForm.color,
          vin: this.vehicleForm.vin,
          userId: this.user.id
        };
        
        const response = await this.$axios.post('/vehicles', vehicleData);
        this.vehicles.push(response.data);
        this.showAddVehicle = false;
        this.vehicleForm = { licensePlate: '', brand: '', model: '', year: '', color: '', vin: '' };
        this.calculateStatistics();
        this.$emit('message', '车辆添加成功', 'success');
      } catch (error) {
        console.error('添加车辆失败:', error);
        const errorMessage = error.response?.data?.message || error.message || '添加车辆失败';
        this.$emit('message', errorMessage, 'error');
      } finally {
        this.isSubmitting = false;
      }
    },
    async updateProfile() {
      try {
        // 检查用户ID是否存在
        if (!this.user.id) {
          throw new Error('用户ID不存在，请重新登录');
        }
        
        console.log('更新用户资料，用户ID:', this.user.id);
        
        // 构建完整的用户更新请求数据
        const updateData = {
          username: this.user.username, // 保持原用户名
          password: this.user.password || '', // 如果没有密码字段，发送空字符串
          name: this.profileForm.name,
          phone: this.profileForm.phone,
          email: this.profileForm.email,
          address: this.profileForm.address,
          vehicles: this.user.vehicles || [],
          repairOrders: this.user.repairOrders || []
        };
        
        console.log('发送的更新数据:', updateData);
        
        const response = await this.$axios.put(`/users/${this.user.id}`, updateData);
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
    getStatusText(status) {
      const statusMap = {
        'PENDING': '待处理',
        'IN_PROGRESS': '进行中',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消'
      };
      return statusMap[status] || status;
    },
    formatDate(dateString) {
      return new Date(dateString).toLocaleDateString('zh-CN');
    },
    editVehicle(vehicle) {
      // 实现编辑车辆功能
      this.$emit('message', '编辑功能开发中', 'info');
    },
    async deleteVehicle(vehicleId) {
      if (confirm('确定要删除这辆车吗？')) {
        try {
          await this.$axios.delete(`/vehicles/${vehicleId}`);
          this.loadVehicles();
          this.$emit('message', '车辆删除成功', 'success');
        } catch (error) {
          console.error('删除车辆失败:', error);
          this.$emit('message', '删除车辆失败', 'error');
        }
      }
    },
    createRepairOrder(vehicle) {
      console.log('开始创建维修单，车辆信息:', vehicle);
      if (!vehicle || !vehicle.id) {
        console.error('车辆信息无效:', vehicle);
        this.$emit('message', '车辆信息无效，无法预约维修', 'error');
        return;
      }
      
      this.repairOrderForm.vehicleId = vehicle.id;
      this.repairOrderForm.contactPhone = this.user.phone || '';
      console.log('设置维修单表单:', this.repairOrderForm);
      this.showCreateOrder = true;
    },
    async submitRepairOrder() {
      try {
        this.isSubmitting = true;
        
        const orderData = {
          orderNumber: `RO${Date.now()}`, // 生成订单号
          status: 'PENDING',
          description: this.repairOrderForm.description,
          createdAt: new Date(),
          updatedAt: new Date(),
          completedAt: null,
          laborCost: 0,
          materialCost: 0,
          totalCost: 0,
          userId: this.user.id,
          vehicleId: this.repairOrderForm.vehicleId,
          technicianIds: [],
          requiredSkillType: this.repairOrderForm.requiredSkillType
        };
        
        const response = await this.$axios.post('/repair-orders', orderData);
        this.repairOrders.push(response.data);
        this.showCreateOrder = false;
        this.repairOrderForm = { vehicleId: '', description: '', preferredDate: '', contactPhone: '', requiredSkillType: '' };
        this.diagnosisResult = null; // 清空诊断结果
        this.calculateStatistics();
        
        // 检查订单状态，给出相应提示
        if (response.data.status === 'ASSIGNED') {
          this.$emit('message', '预约维修成功！系统已为您分配技师，我们将尽快为您安排维修服务。', 'success');
        } else {
          this.$emit('message', '预约提交成功！我们会尽快为您安排合适的技师。', 'success');
        }
        
      } catch (error) {
        console.error('预约维修失败:', error);
        
        // 解析错误消息
        let errorMessage = '预约维修失败';
        
        if (error.response && error.response.data) {
          if (typeof error.response.data === 'string') {
            errorMessage = error.response.data;
          } else if (error.response.data.message) {
            errorMessage = error.response.data.message;
          }
        } else if (error.message) {
          errorMessage = error.message;
        }
        
        // 如果是技师不可用的错误，使用警告级别
        if (errorMessage.includes('暂时没有可用的') || errorMessage.includes('技师')) {
          this.$emit('message', errorMessage, 'warning');
        } else {
          this.$emit('message', errorMessage, 'error');
        }
      } finally {
        this.isSubmitting = false;
      }
    },
    async getIntelligentDiagnosis() {
      try {
        this.isDiagnosing = true;
        this.diagnosisResult = null;
        
        // 获取选中的车辆信息
        const selectedVehicle = this.vehicles.find(v => v.id === this.repairOrderForm.vehicleId);
        
        const diagnosisData = {
          description: this.repairOrderForm.description,
          vehicleBrand: selectedVehicle?.brand || '',
          vehicleModel: selectedVehicle?.model || '',
          mileage: selectedVehicle?.mileage || 0
        };
        
        const response = await this.$axios.post('/diagnosis/analyze', diagnosisData);
        this.diagnosisResult = response.data;
        
        // 自动设置推荐的维修类型（AI推荐优先）
        if (this.diagnosisResult.skillTypeRequired) {
          this.repairOrderForm.requiredSkillType = this.diagnosisResult.skillTypeRequired;
        }
        
        this.$emit('message', 'AI诊断完成！请查看诊断结果', 'success');
      } catch (error) {
        console.error('AI诊断失败:', error);
        const errorMessage = error.response?.data?.message || error.message || 'AI诊断服务暂时不可用';
        this.$emit('message', errorMessage, 'error');
      } finally {
        this.isDiagnosing = false;
      }
    },
    getSeverityClass(severity) {
      const severityMap = {
        '低': 'severity-low',
        '中': 'severity-medium',
        '高': 'severity-high',
        '极高': 'severity-critical'
      };
      return severityMap[severity] || 'severity-medium';
    },
    getSkillTypeName(skillType) {
      const skillTypeMap = {
        'MECHANIC': '机械维修',
        'ELECTRICIAN': '电气维修',
        'BODY_WORK': '车身维修',
        'PAINT': '喷漆',
        'DIAGNOSTIC': '故障诊断'
      };
      return skillTypeMap[skillType] || skillType;
    },
    viewOrderDetail(order) {
      this.selectedOrder = order;
      this.showOrderDetail = true;
    },
    addFeedback(order) {
      this.openFeedbackModal(order);
    },
    openFeedbackModal(order) {
      this.feedbackOrder = order;
      this.feedbackForm = { comment: '', rating: 0 };
      this.showFeedback = true;
    },
    async submitFeedback() {
      try {
        if (!this.isValidFeedback) {
          this.$emit('message', '请至少填写评分和评价内容', 'warning');
          return;
        }
        
        this.isSubmitting = true;
        
        const feedbackData = {
          rating: this.feedbackForm.rating,
          comment: this.feedbackForm.comment,
          createdAt: new Date(),
          repairOrderId: this.feedbackOrder.id,
          userId: this.user.id
        };
        
        const response = await this.$axios.post('/feedbacks', feedbackData);
        this.feedbacks.push(response.data);
        this.showFeedback = false;
        this.feedbackForm = { comment: '', rating: 0 };
        this.feedbackOrder = null;
        
        // 重新计算统计数据，更新可反馈的订单列表
        this.calculateStatistics();
        
        this.$emit('message', '反馈提交成功', 'success');
      } catch (error) {
        console.error('提交反馈失败:', error);
        const errorMessage = error.response?.data?.message || error.message || '提交反馈失败';
        this.$emit('message', errorMessage, 'error');
      } finally {
        this.isSubmitting = false;
      }
    },
    hasUserFeedback(order) {
      return this.feedbacks.some(feedback => feedback.repairOrder && feedback.repairOrder.id === order.id);
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

    getVehicleDisplay(order) {
      // 首先尝试从订单中获取车辆信息
      if (order.vehicle && order.vehicle.licensePlate) {
        return `${order.vehicle.licensePlate} - ${order.vehicle.brand} ${order.vehicle.model}`;
      }
      
      // 如果订单中没有车辆信息，尝试通过vehicleId在vehicles数组中查找
      const vehicleId = order.vehicleId || (order.vehicle && order.vehicle.id);
      if (vehicleId && this.vehicles.length > 0) {
        const vehicle = this.vehicles.find(v => v.id === vehicleId);
        if (vehicle) {
          return `${vehicle.licensePlate} - ${vehicle.brand} ${vehicle.model}`;
        }
      }
      
      return `未知车辆 (ID: ${vehicleId || 'null'})`;
    },
    logout() {
      localStorage.removeItem('user');
      localStorage.removeItem('userRole');
      this.$router.push('/');
    },
    getRatingText(rating) {
      const texts = ['请评分', '很差', '一般', '满意', '不错', '非常满意'];
      return texts[rating] || texts[0];
    },
    async urgeOrder(order) {
      try {
        const response = await this.$axios.post(`/repair-orders/${order.id}/urge`);
        if (response.status === 200) {
          this.$emit('message', '催单成功', 'success');
          // 更新订单状态
          const index = this.repairOrders.findIndex(o => o.id === order.id);
          if (index !== -1) {
            this.repairOrders[index] = response.data;
          }
        }
      } catch (error) {
        this.$emit('message', '催单失败：' + (error.response?.data?.message || '未知错误'), 'error');
      }
    }
  }
}
</script>

<style scoped>
.customer-dashboard {
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
  color: #42b983;
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
  color: #42b983;
  border-bottom-color: #42b983;
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
  background: #42b983;
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
  margin-bottom: 0.5rem;
}

.welcome-section p {
  color: #6b7280;
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
  background: linear-gradient(135deg, #42b983, #369970);
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

.quick-actions {
  margin-bottom: 2rem;
}

.quick-actions h2 {
  margin-bottom: 1rem;
  color: #1f2937;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.action-card {
  background: white;
  padding: 1.5rem;
  border-radius: 1rem;
  border: none;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: transform 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.action-card:hover {
  transform: translateY(-2px);
}

.action-card i {
  font-size: 2rem;
  color: #42b983;
}

.action-card span {
  font-weight: 500;
  color: #374151;
}

.recent-section {
  margin-bottom: 2rem;
}

.recent-section h2 {
  margin-bottom: 1rem;
  color: #1f2937;
}

.recent-orders {
  background: white;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 1px solid #e5e7eb;
}

.order-item:last-child {
  border-bottom: none;
}

.order-info h4 {
  margin: 0 0 0.25rem 0;
  color: #1f2937;
}

.order-info p {
  margin: 0;
  color: #6b7280;
  font-size: 0.875rem;
}

.order-info small {
  color: #9ca3af;
  font-size: 0.75rem;
}

.status {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 500;
  text-transform: uppercase;
}

.status.pending {
  background: #fef3c7;
  color: #d97706;
}

.status.in_progress {
  background: #dbeafe;
  color: #2563eb;
}

.status.completed {
  background: #d1fae5;
  color: #059669;
}

.status.cancelled {
  background: #fee2e2;
  color: #dc2626;
}

.vehicles-grid,
.orders-container,
.feedback-container {
  display: grid;
  gap: 1.5rem;
}

.vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
  padding: 1.5rem;
  grid-auto-rows: minmax(min-content, max-content);
}

.vehicle-card {
  display: flex;
  flex-direction: column;
  background: white;
  padding: 1.5rem;
  border-radius: 1rem;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  height: 100%;
}

.vehicle-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.vehicle-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-icon {
  width: 2rem;
  height: 2rem;
  border: none;
  border-radius: 0.5rem;
  background: #f3f4f6;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-icon:hover {
  background: #e5e7eb;
}

.btn-icon.danger:hover {
  background: #fee2e2;
  color: #dc2626;
}

.status-badge {
  padding: 0.5rem 1rem;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 500;
}

.order-footer {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
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

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  border-radius: 1rem;
  max-width: 500px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}

.modal.large {
  max-width: 700px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #6b7280;
}

.modal-body {
  padding: 1.5rem;
}

.modal-footer {
  display: flex;
  gap: 1rem;
  margin-top: 1.5rem;
  justify-content: flex-end;
}

.required {
  color: #ef4444;
}

.form-help {
  display: block;
  margin-top: 0.5rem;
  color: #6b7280;
  font-size: 0.875rem;
  line-height: 1.4;
}

.form-help i {
  color: #3b82f6;
  margin-right: 0.25rem;
}

.btn {
  /* Add any necessary styles for the btn class */
}

.detail-section {
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.detail-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.detail-section h4 {
  color: #1f2937;
  margin-bottom: 1rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.detail-label {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
}

.detail-value {
  color: #1f2937;
  font-weight: 500;
}

.fault-description {
  background: #f3f4f6;
  padding: 1rem;
  border-radius: 0.5rem;
  border-left: 4px solid #42b983;
  margin: 0;
}

.technician-list {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.technician-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: #f9fafb;
  padding: 0.75rem;
  border-radius: 0.5rem;
}

.tech-avatar {
  width: 2.5rem;
  height: 2.5rem;
  background: #42b983;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.tech-info {
  display: flex;
  flex-direction: column;
}

.tech-name {
  font-weight: 500;
  color: #1f2937;
}

.tech-skill {
  font-size: 0.875rem;
  color: #6b7280;
}

.cost-breakdown {
  background: #f9fafb;
  padding: 1rem;
  border-radius: 0.5rem;
}

.cost-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid #e5e7eb;
}

.cost-item:last-child {
  border-bottom: none;
}

.cost-item.total {
  font-weight: 600;
  font-size: 1.1rem;
  border-top: 2px solid #42b983;
  padding-top: 0.75rem;
  margin-top: 0.5rem;
}

.feedback-order-info {
  background: #f3f4f6;
  padding: 1rem;
  border-radius: 0.5rem;
  margin-bottom: 1rem;
}

.feedback-order-info h4 {
  margin: 0 0 0.5rem 0;
  color: #1f2937;
}

.feedback-order-info p {
  margin: 0;
  color: #6b7280;
  font-size: 0.875rem;
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

/* AI诊断样式 */
.btn-ai {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  justify-content: center;
}

.btn-ai:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-ai:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.diagnosis-result {
  margin-top: 1rem;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 1rem;
  padding: 1.5rem;
  border: 2px solid #667eea;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.diagnosis-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 2px solid rgba(102, 126, 234, 0.3);
}

.diagnosis-header i {
  font-size: 1.5rem;
  color: #667eea;
}

.diagnosis-header h4 {
  margin: 0;
  color: #2d3748;
  font-size: 1.1rem;
}

.diagnosis-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.diagnosis-item {
  background: white;
  padding: 1rem;
  border-radius: 0.5rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.diagnosis-item strong {
  display: block;
  color: #4a5568;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
}

.severity-low {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: #d1fae5;
  color: #065f46;
  border-radius: 0.375rem;
  font-weight: 600;
  font-size: 0.875rem;
}

.severity-medium {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: #fed7aa;
  color: #92400e;
  border-radius: 0.375rem;
  font-weight: 600;
  font-size: 0.875rem;
}

.severity-high {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: #fecaca;
  color: #991b1b;
  border-radius: 0.375rem;
  font-weight: 600;
  font-size: 0.875rem;
}

.severity-critical {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: #fca5a5;
  color: #7f1d1d;
  border-radius: 0.375rem;
  font-weight: 600;
  font-size: 0.875rem;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

.recommendations {
  margin: 0.5rem 0 0 0;
  padding-left: 1.5rem;
}

.recommendations li {
  padding: 0.5rem 0;
  color: #4a5568;
  line-height: 1.6;
}

.recommendations li::marker {
  color: #667eea;
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
  
  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .section-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
  
  .modal {
    margin: 1rem;
    width: calc(100% - 2rem);
  }
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
  
  .technician-list {
    flex-direction: column;
  }
  
  .modal-footer {
    flex-direction: column;
  }
}

/* 评分组件样式 */
.rating-container {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.star-rating {
  display: flex;
  gap: 0.25rem;
}

.star {
  cursor: pointer;
  font-size: 1.5rem;
  color: #d1d5db;
  transition: color 0.2s;
}

.star:hover, .star.active {
  color: #f59e0b;
}

.rating-text {
  font-size: 0.875rem;
  color: #4b5563;
}

/* 反馈提示样式优化 */
.feedback-prompt {
  background: #f0f9ff;
  border-left: 4px solid #0ea5e9;
  padding: 1.25rem;
  border-radius: 0.5rem;
  margin-bottom: 1rem;
  transition: transform 0.2s;
}

.feedback-prompt:hover {
  transform: translateX(2px);
}

.feedback-prompt-text {
  margin: 0.5rem 0;
  font-weight: 500;
}
</style>