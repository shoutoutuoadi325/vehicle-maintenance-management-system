<template>
  <div class="grand-prize-page">
    <header class="page-header">
      <button class="back-btn" @click="$router.back()">返回</button>
      <h1>我的通关奖励</h1>
      <span class="tag">年度环保车主</span>
    </header>

    <section class="card hero" :class="statusClass">
      <h2>{{ heroTitle }}</h2>
      <p>{{ heroSubtitle }}</p>
      <div class="status-pill">{{ shippingStatusText }}</div>
    </section>

    <section class="card" v-if="status.journeyCompleted">
      <h3>奖励详情</h3>
      <div class="grid">
        <div><strong>实体车贴：</strong>{{ status.stickerClaimed ? '已申领' : '未申领' }}</div>
        <div><strong>终极商业大奖：</strong>{{ status.grandPrizeGranted ? '已激活' : '未激活' }}</div>
        <div><strong>通关时间：</strong>{{ status.completedAt || '-' }}</div>
        <div><strong>发货时间：</strong>{{ status.shippedAt || '-' }}</div>
        <div><strong>物流单号：</strong>{{ status.shipmentTrackingNo || '待更新' }}</div>
      </div>
    </section>

    <section class="card" v-if="status.stickerClaimed">
      <h3>收货信息</h3>
      <div class="grid">
        <div><strong>收货人：</strong>{{ status.consigneeName || '-' }}</div>
        <div><strong>联系电话：</strong>{{ status.consigneePhone || '-' }}</div>
        <div class="address"><strong>收货地址：</strong>{{ status.shippingAddress || '-' }}</div>
      </div>
    </section>

    <section class="card" v-if="!status.journeyCompleted">
      <h3>尚未通关</h3>
      <p>完成零碳公路最后节点（拉萨）后，即可解锁“年度环保车主”证书与实体奖励申领资格。</p>
      <button class="primary-btn" @click="$router.push('/customer/journey')">前往继续挑战</button>
    </section>

    <section class="card" v-if="status.journeyCompleted && !status.stickerClaimed">
      <h3>可立即申领</h3>
      <p>你已达成通关条件，前往零碳公路页面可填写收货地址并完成申领。</p>
      <button class="primary-btn" @click="$router.push('/customer/journey')">去填写地址</button>
    </section>
  </div>
</template>

<script>
export default {
  name: 'GrandPrizeStatus',
  data() {
    return {
      status: {
        userId: null,
        journeyCompleted: false,
        stickerClaimed: false,
        grandPrizeGranted: false,
        shippingStatus: 'NOT_CLAIMED',
        consigneeName: null,
        consigneePhone: null,
        shippingAddress: null,
        shipmentTrackingNo: null,
        completedAt: null,
        shippedAt: null
      }
    };
  },
  computed: {
    shippingStatusText() {
      const map = {
        NOT_CLAIMED: '待申领',
        PREPARING: '奖励已准备发货',
        SHIPPED: '已发货',
        DELIVERED: '已签收'
      };
      return map[this.status.shippingStatus] || '待申领';
    },
    heroTitle() {
      if (!this.status.journeyCompleted) return '通关后解锁年度环保车主荣誉';
      if (this.status.shippingStatus === 'DELIVERED') return '奖励已签收，感谢你的绿色出行';
      return '年度环保车主奖励进度';
    },
    heroSubtitle() {
      if (!this.status.journeyCompleted) return '继续完成零碳公路之旅，终点将有实体车贴与商业大奖。';
      if (!this.status.stickerClaimed) return '你已通关，等待你填写收货信息。';
      return '你的奖励正在履约中，可在此持续查看发货状态。';
    },
    statusClass() {
      return `status-${String(this.status.shippingStatus || 'NOT_CLAIMED').toLowerCase()}`;
    }
  },
  async created() {
    await this.loadStatus();
  },
  methods: {
    gamificationApi(path) {
      const baseURL = this.$axios?.defaults?.baseURL || '';
      if (baseURL.endsWith('/api')) {
        return `/gamification${path}`;
      }
      return `/api/gamification${path}`;
    },
    async loadStatus() {
      try {
        const response = await this.$axios.get(this.gamificationApi('/journey/grand-prize/me'));
        this.status = { ...this.status, ...(response.data || {}) };
      } catch (error) {
        console.error('加载通关奖励状态失败:', error);
      }
    }
  }
};
</script>

<style scoped>
.grand-prize-page {
  min-height: 100vh;
  padding: 18px;
  background: linear-gradient(180deg, #f0fdf4, #ecfeff);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
}

.back-btn {
  border: 1px solid #a7f3d0;
  border-radius: 8px;
  background: #fff;
  padding: 6px 12px;
  cursor: pointer;
}

.tag {
  margin-left: auto;
  background: #dcfce7;
  color: #15803d;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
}

.card {
  margin-top: 14px;
  border-radius: 14px;
  border: 1px solid #d1fae5;
  background: #ffffff;
  padding: 14px;
}

.hero {
  border-width: 2px;
}

.status-pill {
  margin-top: 8px;
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  background: #f1f5f9;
  font-weight: 700;
}

.grid {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.address {
  grid-column: 1 / -1;
}

.primary-btn {
  border: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #22c55e, #14b8a6);
  color: #fff;
  padding: 10px 16px;
  font-weight: 700;
  cursor: pointer;
}

.status-preparing {
  border-color: #2dd4bf;
}

.status-shipped {
  border-color: #60a5fa;
}

.status-delivered {
  border-color: #22c55e;
}

@media (max-width: 640px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
