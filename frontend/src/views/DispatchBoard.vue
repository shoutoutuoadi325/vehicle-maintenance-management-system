<template>
  <div class="dispatch-board-page">
    <header class="board-header">
      <div>
        <h1>调度看板</h1>
        <p>拖拽工单到目标技师列，快速完成重分配。</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-outline" @click="$router.push('/admin')">返回管理台</button>
        <button class="btn btn-primary" @click="loadBoard" :disabled="loading">
          {{ loading ? '加载中...' : '刷新' }}
        </button>
      </div>
    </header>

    <div v-if="error" class="board-error">{{ error }}</div>
    <div v-if="infoMessage" class="board-info">{{ infoMessage }}</div>

    <section v-if="focusedOrderId" class="focused-order-panel">
      <h2>当前待改派工单</h2>
      <div v-if="focusedOrder" class="focused-order-card">
        <div class="order-title">{{ focusedOrder.orderNumber || ('工单 #' + focusedOrder.id) }}</div>
        <div class="order-desc">{{ focusedOrder.description || '无描述' }}</div>
        <div class="order-meta">
          <span>状态：{{ focusedOrder.status || '-' }}</span>
            <span>所需工种：{{ focusedOrder.requiredSkillType || '-' }}</span>
          <span>
            当前技师：
            {{ focusedOrder.technicians && focusedOrder.technicians.length
              ? focusedOrder.technicians.map(t => t.name).join('、')
              : '未分配' }}
          </span>
        </div>
      </div>
      <p class="focused-order-tip">可点击任一技师列的“转派到此”按钮完成更换（无需拖拽）。</p>
    </section>

    <div class="board-grid" v-if="!loading">
      <section v-for="tech in board" :key="tech.id" class="tech-column">
        <div class="tech-head">
          <h3>{{ tech.name }}</h3>
          <div class="meta">
            <span>{{ tech.skillType || '-' }}</span>
            <span>{{ tech.activeOrderCount || 0 }} 单</span>
          </div>
          <button
            v-if="focusedOrderId"
            class="btn btn-primary btn-sm reassign-btn"
            :disabled="reassigningTechId !== null || isFocusedOrderAssignedToTech(tech.id) || !canAssignFocusedOrderToTech(tech)"
            @click="assignFocusedOrderToTech(tech)"
          >
            {{ getAssignButtonText(tech) }}
          </button>
        </div>

        <draggable
          :list="tech.orders"
          group="orders"
          item-key="id"
          class="order-list"
          @change="onOrderDrop($event, tech)"
        >
          <template #item="{ element }">
            <article :class="['order-card', { focused: isFocusedOrder(element) }]">
              <div class="order-title">{{ element.orderNumber || ('工单 #' + element.id) }}</div>
              <div class="order-desc">{{ element.description || '无描述' }}</div>
              <div class="order-meta">
                <span>{{ element.status }}</span>
                <span v-if="element.aiAssigned" class="ai-tag">AI 分配</span>
              </div>
            </article>
          </template>
        </draggable>

        <div v-if="!tech.orders || tech.orders.length === 0" class="empty-column">暂无工单</div>
      </section>
    </div>

    <div v-if="loading" class="loading">正在加载调度看板...</div>
  </div>
</template>

<script>
import draggable from 'vuedraggable'

export default {
  name: 'DispatchBoard',
  components: { draggable },
  data() {
    return {
      board: [],
      loading: false,
      error: '',
      infoMessage: '',
      focusedOrderId: null,
      focusedOrder: null,
      reassigningTechId: null
    }
  },
  async created() {
    const orderIdFromQuery = this.$route?.query?.orderId
    this.focusedOrderId = orderIdFromQuery ? Number(orderIdFromQuery) : null
    await this.loadBoard()
    if (this.focusedOrderId) {
      await this.loadFocusedOrder()
    }
  },
  methods: {
    isFocusedOrder(order) {
      return Boolean(this.focusedOrderId) && Number(order?.id) === this.focusedOrderId
    },
    isFocusedOrderAssignedToTech(techId) {
      if (!this.focusedOrder || !this.focusedOrder.technicians) {
        return false
      }
      return this.focusedOrder.technicians.some(tech => Number(tech.id) === Number(techId))
    },
    isSkillTypeMatched(orderSkillType, techSkillType) {
      if (!orderSkillType) {
        return true
      }
      return String(orderSkillType).toUpperCase() === String(techSkillType || '').toUpperCase()
    },
    canAssignFocusedOrderToTech(tech) {
      return this.isSkillTypeMatched(this.focusedOrder?.requiredSkillType, tech?.skillType)
    },
    getAssignButtonText(tech) {
      if (this.reassigningTechId === tech.id) {
        return '更换中...'
      }
      if (this.isFocusedOrderAssignedToTech(tech.id)) {
        return '当前技师'
      }
      if (!this.canAssignFocusedOrderToTech(tech)) {
        return '类型不匹配'
      }
      return '转派到此'
    },
    async loadBoard() {
      this.loading = true
      this.error = ''
      try {
        const response = await this.$axios.get('/admin/dispatch/board')
        this.board = response.data || []
      } catch (e) {
        this.error = e.response?.data?.message || '加载调度看板失败'
      } finally {
        this.loading = false
      }
    },
    async loadFocusedOrder() {
      if (!this.focusedOrderId) {
        return
      }

      try {
        const response = await this.$axios.get(`/repair-orders/${this.focusedOrderId}/details`)
        this.focusedOrder = response.data
      } catch (e) {
        try {
          const fallbackResponse = await this.$axios.get(`/repair-orders/${this.focusedOrderId}`)
          this.focusedOrder = fallbackResponse.data
        } catch (fallbackError) {
          this.focusedOrder = null
          this.error = fallbackError.response?.data?.message || '加载工单详情失败'
        }
      }
    },
    async assignFocusedOrderToTech(targetTech) {
      if (!this.focusedOrderId || !targetTech) {
        return
      }

      if (!this.canAssignFocusedOrderToTech(targetTech)) {
        this.error = '工单所需工种与目标技师不匹配，不能改派'
        return
      }

      this.reassigningTechId = targetTech.id
      this.error = ''
      this.infoMessage = ''

      try {
        await this.$axios.put(`/repair-orders/${this.focusedOrderId}/reassign`, [targetTech.id], {
          params: { isManual: true }
        })
        this.infoMessage = `工单已改派至技师：${targetTech.name}`
        await this.loadBoard()
        await this.loadFocusedOrder()
      } catch (e) {
        this.error = e.response?.data?.message || '改派失败，请稍后重试'
      } finally {
        this.reassigningTechId = null
      }
    },
    async onOrderDrop(evt, targetTech) {
      const movedOrder = evt && evt.added ? evt.added.element : null
      if (!movedOrder || !targetTech) {
        return
      }

      if (!this.isSkillTypeMatched(movedOrder.requiredSkillType, targetTech.skillType)) {
        this.error = '工单所需工种与目标技师不匹配，不能改派'
        await this.loadBoard()
        if (this.focusedOrderId) {
          await this.loadFocusedOrder()
        }
        return
      }

      try {
        await this.$axios.put(`/repair-orders/${movedOrder.id}/reassign`, [targetTech.id], {
          params: { isManual: true }
        })
        this.infoMessage = `工单已改派至技师：${targetTech.name}`
        await this.loadBoard()
        if (this.focusedOrderId) {
          await this.loadFocusedOrder()
        }
      } catch (e) {
        this.error = e.response?.data?.message || '重分配失败，已刷新列表'
        await this.loadBoard()
      }
    }
  }
}
</script>

<style scoped>
.dispatch-board-page { padding: 1.5rem; background: #f7fafc; min-height: 100vh; }
.board-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.board-header h1 { margin: 0; }
.board-header p { margin: 0.25rem 0 0; color: #4a5568; }
.header-actions { display: flex; gap: 0.5rem; }
.btn { border: 1px solid #cbd5e0; background: white; padding: 0.45rem 0.9rem; border-radius: 0.5rem; cursor: pointer; }
.btn-primary { background: #2b6cb0; border-color: #2b6cb0; color: white; }
.btn-sm { padding: 0.3rem 0.7rem; font-size: 0.78rem; }
.focused-order-panel { background: #edf2f7; border: 1px solid #cbd5e0; border-radius: 0.75rem; padding: 0.9rem; margin-bottom: 1rem; }
.focused-order-panel h2 { margin: 0 0 0.5rem 0; font-size: 0.95rem; }
.focused-order-card { background: #fff; border: 1px solid #dbe4ef; border-radius: 0.6rem; padding: 0.65rem; }
.focused-order-tip { margin: 0.55rem 0 0; font-size: 0.8rem; color: #4a5568; }
.board-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 1rem; }
.tech-column { background: white; border-radius: 0.8rem; padding: 0.8rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.tech-head h3 { margin: 0; font-size: 1rem; }
.meta { margin-top: 0.25rem; display: flex; gap: 0.5rem; color: #718096; font-size: 0.8rem; }
.reassign-btn { margin-top: 0.55rem; width: 100%; }
.order-list { min-height: 120px; margin-top: 0.8rem; display: grid; gap: 0.6rem; }
.order-card { border: 1px solid #e2e8f0; border-radius: 0.6rem; padding: 0.6rem; background: #f8fafc; }
.order-card.focused { border-color: #2b6cb0; box-shadow: 0 0 0 2px rgba(43, 108, 176, 0.2); }
.order-title { font-weight: 700; font-size: 0.9rem; }
.order-desc { margin-top: 0.25rem; font-size: 0.82rem; color: #4a5568; }
.order-meta { margin-top: 0.5rem; display: flex; justify-content: space-between; font-size: 0.75rem; color: #718096; }
.ai-tag { background: #ebf8ff; color: #2b6cb0; border-radius: 999px; padding: 0.1rem 0.45rem; }
.empty-column { margin-top: 0.55rem; font-size: 0.8rem; color: #718096; }
.board-error { color: #c53030; margin-bottom: 0.75rem; }
.board-info { color: #2f855a; margin-bottom: 0.75rem; }
.loading { color: #4a5568; }
</style>
