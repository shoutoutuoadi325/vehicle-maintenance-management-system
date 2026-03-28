<template>
  <div class="dispatch-board">
    <header class="board-header">
      <div class="header-left">
        <button @click="$router.push('/admin')" class="btn btn-outline btn-small">
          <i class="fas fa-arrow-left"></i> 返回
        </button>
        <h1><i class="fas fa-tasks"></i> 智能调度看板</h1>
      </div>
      <div class="header-right">
        <button @click="loadData" class="btn btn-outline btn-small">
          <i class="fas fa-sync"></i> 刷新
        </button>
      </div>
    </header>

    <div v-if="loading" class="board-loading">
      <i class="fas fa-spinner fa-spin"></i> 加载中...
    </div>

    <div v-else class="board-content">
      <!-- 汇总统计 -->
      <div class="board-stats">
        <div class="stat-card">
          <div class="stat-icon"><i class="fas fa-users-cog"></i></div>
          <div class="stat-info">
            <h3>{{ technicians.length }}</h3>
            <p>技师总数</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon orange"><i class="fas fa-clipboard-list"></i></div>
          <div class="stat-info">
            <h3>{{ pendingOrders.length }}</h3>
            <p>待处理工单</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon blue"><i class="fas fa-cogs"></i></div>
          <div class="stat-info">
            <h3>{{ inProgressOrders.length }}</h3>
            <p>进行中工单</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon purple"><i class="fas fa-robot"></i></div>
          <div class="stat-info">
            <h3>{{ autoAssignedCount }}</h3>
            <p>AI自动分配</p>
          </div>
        </div>
      </div>

      <!-- 技师看板 -->
      <div class="kanban-container">
        <!-- 未分配工单列 -->
        <div class="kanban-column unassigned-column">
          <div class="column-header">
            <h3><i class="fas fa-inbox"></i> 待分配</h3>
            <span class="order-count">{{ pendingOrders.length }}</span>
          </div>
          <div class="column-body">
            <div v-if="pendingOrders.length === 0" class="empty-column">
              <i class="fas fa-check-circle"></i>
              <p>暂无待分配工单</p>
            </div>
            <div
              v-for="order in pendingOrders"
              :key="order.id"
              class="order-card pending-card"
              :draggable="true"
              @dragstart="onDragStart(order)"
            >
              <div class="order-card-header">
                <span class="order-number">{{ order.orderNumber }}</span>
                <span class="order-status pending">待分配</span>
              </div>
              <div class="order-desc">{{ order.description }}</div>
              <div class="order-meta">
                <span><i class="fas fa-car"></i> {{ getVehicleDisplay(order) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 各技师列 -->
        <div
          v-for="tech in technicians"
          :key="tech.id"
          class="kanban-column technician-column"
          @dragover.prevent
          @drop="onDrop(tech)"
        >
          <div class="column-header">
            <div class="tech-info">
              <div class="tech-avatar">
                <i class="fas fa-user-hard-hat"></i>
              </div>
              <div>
                <h3>{{ tech.name }}</h3>
                <span class="skill-badge">{{ getSkillLabel(tech.skillType) }}</span>
              </div>
            </div>
            <div class="workload-info">
              <span class="workload-count" :class="getWorkloadClass(getTechOrders(tech.id).length)">
                {{ getTechOrders(tech.id).length }} 单
              </span>
            </div>
          </div>

          <div class="workload-bar-wrap">
            <div class="workload-bar" :style="{ width: Math.min(getTechOrders(tech.id).length * 20, 100) + '%' }"
              :class="getWorkloadClass(getTechOrders(tech.id).length)"></div>
          </div>

          <div class="column-body">
            <div v-if="getTechOrders(tech.id).length === 0" class="empty-column">
              <p>拖拽工单至此分配</p>
            </div>
            <div
              v-for="order in getTechOrders(tech.id)"
              :key="order.id"
              class="order-card"
              :class="{ 'auto-assigned': order.assignmentType === 'AUTO' }"
              :draggable="true"
              @dragstart="onDragStart(order)"
            >
              <div class="order-card-header">
                <span class="order-number">{{ order.orderNumber }}</span>
                <div class="order-badges">
                  <span v-if="order.assignmentType === 'AUTO'" class="ai-badge">
                    <i class="fas fa-robot"></i> AI
                  </span>
                  <span :class="['order-status', order.status.toLowerCase()]">
                    {{ getStatusText(order.status) }}
                  </span>
                </div>
              </div>
              <div class="order-desc">{{ order.description }}</div>
              <div class="order-meta">
                <span><i class="fas fa-car"></i> {{ getVehicleDisplay(order) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 重新分配确认弹窗 -->
    <div v-if="showReassignModal" class="modal-overlay" @click="cancelReassign">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>确认重新分配</h3>
          <button @click="cancelReassign" class="modal-close">&times;</button>
        </div>
        <div class="modal-body">
          <p>
            将工单 <strong>{{ draggedOrder && draggedOrder.orderNumber }}</strong>
            重新分配给技师 <strong>{{ targetTechnician && targetTechnician.name }}</strong>？
          </p>
          <p class="reassign-note">
            <i class="fas fa-info-circle"></i>
            此操作将覆盖 AI 的原始派单决定。
          </p>
        </div>
        <div class="modal-footer">
          <button @click="confirmReassign" class="btn btn-primary" :disabled="reassigning">
            <i v-if="reassigning" class="fas fa-spinner fa-spin"></i>
            {{ reassigning ? '分配中...' : '确认分配' }}
          </button>
          <button @click="cancelReassign" class="btn btn-outline">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DispatchBoard',
  data() {
    return {
      loading: true,
      technicians: [],
      allOrders: [],
      draggedOrder: null,
      targetTechnician: null,
      showReassignModal: false,
      reassigning: false
    };
  },
  computed: {
    pendingOrders() {
      return this.allOrders.filter(o => o.status === 'PENDING');
    },
    inProgressOrders() {
      return this.allOrders.filter(o => o.status === 'IN_PROGRESS');
    },
    autoAssignedCount() {
      return this.allOrders.filter(o => o.assignmentType === 'AUTO').length;
    }
  },
  created() {
    this.checkAuth();
    this.loadData();
  },
  methods: {
    checkAuth() {
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      if (!user || user.role?.toLowerCase() !== 'admin') {
        this.$router.push('/');
      }
    },
    async loadData() {
      this.loading = true;
      try {
        const [techRes, orderRes] = await Promise.all([
          this.$axios.get('/technicians'),
          this.$axios.get('/repair-orders')
        ]);
        this.technicians = techRes.data || [];
        this.allOrders = orderRes.data || [];
      } catch (err) {
        console.error('加载看板数据失败', err);
      } finally {
        this.loading = false;
      }
    },
    getTechOrders(techId) {
      return this.allOrders.filter(o =>
        o.technicians && o.technicians.some(t => t.id === techId) &&
        (o.status === 'ASSIGNED' || o.status === 'IN_PROGRESS')
      );
    },
    getVehicleDisplay(order) {
      if (order.vehicle) {
        return `${order.vehicle.licensePlate} ${order.vehicle.brand}`;
      }
      return '未知车辆';
    },
    getStatusText(status) {
      const map = {
        PENDING: '待处理', ASSIGNED: '已分配',
        IN_PROGRESS: '进行中', COMPLETED: '已完成', CANCELLED: '已取消'
      };
      return map[status] || status;
    },
    getSkillLabel(skill) {
      const map = {
        MECHANIC: '机械', ELECTRICIAN: '电工',
        BODY_WORK: '钣金', PAINT: '喷漆', DIAGNOSTIC: '诊断'
      };
      return map[skill] || skill;
    },
    getWorkloadClass(count) {
      if (count === 0) return 'workload-none';
      if (count <= 2) return 'workload-low';
      if (count <= 4) return 'workload-medium';
      return 'workload-high';
    },
    onDragStart(order) {
      this.draggedOrder = order;
    },
    onDrop(technician) {
      if (!this.draggedOrder) return;
      this.targetTechnician = technician;
      this.showReassignModal = true;
    },
    cancelReassign() {
      this.showReassignModal = false;
      this.draggedOrder = null;
      this.targetTechnician = null;
    },
    async confirmReassign() {
      if (!this.draggedOrder || !this.targetTechnician) return;
      this.reassigning = true;
      try {
        await this.$axios.put(
          `/repair-orders/${this.draggedOrder.id}/reassign`,
          null,
          { params: { technicianId: this.targetTechnician.id } }
        );
        await this.loadData();
        this.showReassignModal = false;
        this.draggedOrder = null;
        this.targetTechnician = null;
      } catch (err) {
        console.error('重新分配失败', err);
        alert('重新分配失败：' + (err.response?.data?.message || err.message));
      } finally {
        this.reassigning = false;
      }
    }
  }
};
</script>

<style scoped>
.dispatch-board {
  min-height: 100vh;
  background: #f8fafc;
  font-family: 'Segoe UI', sans-serif;
}

.board-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.board-header h1 {
  margin: 0;
  font-size: 1.3rem;
  color: #1e293b;
}

.board-header h1 i {
  color: #6366f1;
  margin-right: 0.4rem;
}

.board-loading {
  text-align: center;
  padding: 4rem;
  color: #6366f1;
  font-size: 1.1rem;
}

.board-content {
  padding: 1.5rem 2rem;
}

.board-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stat-card {
  background: white;
  border-radius: 10px;
  padding: 1rem 1.25rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.1rem;
}

.stat-icon.orange { background: linear-gradient(135deg, #f59e0b, #d97706); }
.stat-icon.blue   { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.stat-icon.purple { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }

.stat-info h3 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
}

.stat-info p {
  margin: 0;
  font-size: 0.85rem;
  color: #64748b;
}

.kanban-container {
  display: flex;
  gap: 1rem;
  overflow-x: auto;
  padding-bottom: 1rem;
  align-items: flex-start;
}

.kanban-column {
  min-width: 240px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.07);
  flex: 1;
}

.unassigned-column {
  min-width: 220px;
  max-width: 260px;
  background: #fef3c7;
  border: 1px solid #f59e0b;
}

.column-header {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.column-header h3 {
  margin: 0;
  font-size: 0.95rem;
  color: #1e293b;
}

.column-header h3 i {
  margin-right: 0.35rem;
  color: #f59e0b;
}

.order-count {
  background: #e5e7eb;
  border-radius: 999px;
  padding: 0.1rem 0.5rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: #374151;
}

.tech-info {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.tech-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 0.85rem;
}

.tech-info h3 {
  margin: 0;
  font-size: 0.9rem;
  color: #1e293b;
}

.skill-badge {
  font-size: 0.72rem;
  background: #e0e7ff;
  color: #4f46e5;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.workload-count {
  font-size: 0.8rem;
  font-weight: 700;
  padding: 0.1rem 0.45rem;
  border-radius: 4px;
}

.workload-none  { background: #f0fdf4; color: #166534; }
.workload-low   { background: #d1fae5; color: #065f46; }
.workload-medium{ background: #fef3c7; color: #92400e; }
.workload-high  { background: #fee2e2; color: #991b1b; }

.workload-bar-wrap {
  height: 4px;
  background: #e5e7eb;
}

.workload-bar {
  height: 4px;
  transition: width 0.3s;
}

.workload-bar.workload-low   { background: #10b981; }
.workload-bar.workload-medium{ background: #f59e0b; }
.workload-bar.workload-high  { background: #ef4444; }

.column-body {
  padding: 0.75rem;
  min-height: 120px;
}

.empty-column {
  text-align: center;
  color: #9ca3af;
  padding: 1.5rem 0.5rem;
  font-size: 0.85rem;
  border: 2px dashed #e5e7eb;
  border-radius: 8px;
}

.empty-column i {
  font-size: 1.5rem;
  display: block;
  margin-bottom: 0.3rem;
}

.order-card {
  background: #f8faff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 0.75rem;
  margin-bottom: 0.6rem;
  cursor: grab;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.order-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  border-color: #a5b4fc;
}

.order-card.auto-assigned {
  border-left: 3px solid #6366f1;
}

.pending-card {
  background: #fffbeb;
  border-color: #fbbf24;
}

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.4rem;
}

.order-number {
  font-size: 0.78rem;
  font-weight: 600;
  color: #4f46e5;
}

.order-badges {
  display: flex;
  gap: 0.3rem;
  align-items: center;
}

.ai-badge {
  font-size: 0.7rem;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.ai-badge i {
  margin-right: 0.15rem;
}

.order-status {
  font-size: 0.72rem;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  font-weight: 600;
}

.order-status.pending   { background: #fef3c7; color: #92400e; }
.order-status.assigned  { background: #dbeafe; color: #1e40af; }
.order-status.in_progress{ background: #dcfce7; color: #166534; }

.order-desc {
  font-size: 0.85rem;
  color: #374151;
  margin-bottom: 0.35rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-meta {
  font-size: 0.78rem;
  color: #9ca3af;
}

.order-meta i {
  margin-right: 0.2rem;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 420px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  margin: 0;
  color: #1e293b;
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.3rem;
  cursor: pointer;
  color: #9ca3af;
}

.modal-body {
  padding: 1.25rem;
  color: #374151;
}

.reassign-note {
  color: #92400e;
  background: #fef3c7;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  font-size: 0.88rem;
  margin-top: 0.5rem;
}

.reassign-note i {
  margin-right: 0.4rem;
}

.modal-footer {
  display: flex;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid #e5e7eb;
  justify-content: flex-end;
}

.btn {
  padding: 0.5rem 1.25rem;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
  border: none;
}

.btn-primary {
  background: #6366f1;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #4f46e5;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-outline {
  background: transparent;
  border: 1px solid #d1d5db;
  color: #374151;
}

.btn-outline:hover {
  background: #f3f4f6;
}

.btn-small {
  padding: 0.35rem 0.75rem;
  font-size: 0.8rem;
}
</style>
