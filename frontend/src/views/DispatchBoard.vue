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

    <div class="board-grid" v-if="!loading">
      <section v-for="tech in board" :key="tech.id" class="tech-column">
        <div class="tech-head">
          <h3>{{ tech.name }}</h3>
          <div class="meta">
            <span>{{ tech.skillType || '-' }}</span>
            <span>{{ tech.activeOrderCount || 0 }} 单</span>
          </div>
        </div>

        <draggable
          :list="tech.orders"
          group="orders"
          item-key="id"
          class="order-list"
          @change="onOrderDrop($event, tech)"
        >
          <template #item="{ element }">
            <article class="order-card">
              <div class="order-title">{{ element.orderNumber || ('工单 #' + element.id) }}</div>
              <div class="order-desc">{{ element.description || '无描述' }}</div>
              <div class="order-meta">
                <span>{{ element.status }}</span>
                <span v-if="element.aiAssigned" class="ai-tag">AI 分配</span>
              </div>
            </article>
          </template>
        </draggable>
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
      error: ''
    }
  },
  created() {
    this.loadBoard()
  },
  methods: {
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
    async onOrderDrop(evt, targetTech) {
      const movedOrder = evt && evt.added ? evt.added.element : null
      if (!movedOrder || !targetTech) {
        return
      }
      try {
        await this.$axios.put(`/repair-orders/${movedOrder.id}/reassign`, [targetTech.id], {
          params: { isManual: true }
        })
        await this.loadBoard()
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
.board-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 1rem; }
.tech-column { background: white; border-radius: 0.8rem; padding: 0.8rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.tech-head h3 { margin: 0; font-size: 1rem; }
.meta { margin-top: 0.25rem; display: flex; gap: 0.5rem; color: #718096; font-size: 0.8rem; }
.order-list { min-height: 120px; margin-top: 0.8rem; display: grid; gap: 0.6rem; }
.order-card { border: 1px solid #e2e8f0; border-radius: 0.6rem; padding: 0.6rem; background: #f8fafc; }
.order-title { font-weight: 700; font-size: 0.9rem; }
.order-desc { margin-top: 0.25rem; font-size: 0.82rem; color: #4a5568; }
.order-meta { margin-top: 0.5rem; display: flex; justify-content: space-between; font-size: 0.75rem; color: #718096; }
.ai-tag { background: #ebf8ff; color: #2b6cb0; border-radius: 999px; padding: 0.1rem 0.45rem; }
.board-error { color: #c53030; margin-bottom: 0.75rem; }
.loading { color: #4a5568; }
</style>
