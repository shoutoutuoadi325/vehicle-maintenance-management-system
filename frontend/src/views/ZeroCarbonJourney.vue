<template>
  <div class="journey-root">
    <header class="topbar">
      <button class="back-btn" @click="goBack">
        <span class="arrow">←</span>
        <span>返回上一页</span>
      </button>
      <h1 class="title">Zero-Carbon Road Trip</h1>
      <div class="topbar-right">绿色公路挑战</div>
    </header>

    <main class="stage">
      <section class="map-card">
        <div class="map-caption">
          <h2>零碳公路地图</h2>
          <p>解锁城市节点，完成环保问答，持续积累绿色能量</p>
        </div>

        <svg class="road-map" viewBox="0 0 1000 560" preserveAspectRatio="xMidYMid meet">
          <defs>
            <filter id="nodeGlow" x="-60%" y="-60%" width="220%" height="220%">
              <feGaussianBlur stdDeviation="6" result="blur" />
              <feMerge>
                <feMergeNode in="blur" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
          </defs>

          <path
            :d="roadPath"
            class="road-base"
          />
          <path
            :d="roadPath"
            class="road-dash"
          />

          <g v-for="(city, idx) in cities" :key="city.id">
            <line :x1="city.x" :x2="city.x" :y1="city.y" :y2="city.y + 42" class="city-guide" />

            <g v-if="city.brandServiceArea" class="brand-service-marker" :transform="`translate(${city.x + 18}, ${city.y - 30})`">
              <rect x="0" y="0" width="88" height="24" rx="12" class="brand-pill" />
              <text x="44" y="16" text-anchor="middle" class="brand-pill-text">{{ brandTag(city.brandName) }}</text>
            </g>

            <circle
              :cx="city.x"
              :cy="city.y"
              r="14"
              :class="[
                'city-node',
                isReached(city) ? 'reached' : 'locked',
                isLatestUnlockAndUnchecked(idx) ? 'unlocking' : '',
                isCheckedIn(idx) ? 'checked' : ''
              ]"
              :filter="isReached(city) ? 'url(#nodeGlow)' : null"
              @click="handleNodeClick(idx)"
            />

            <circle
              v-if="isLatestUnlockAndUnchecked(idx)"
              :cx="city.x"
              :cy="city.y"
              r="22"
              class="pulse-ring"
              @click="handleNodeClick(idx)"
            />

            <text :x="city.x" :y="city.y + 68" text-anchor="middle" class="city-name">{{ city.name }}</text>
            <text :x="city.x" :y="city.y + 88" text-anchor="middle" class="city-mile">{{ city.mileage }} km</text>
          </g>
        </svg>
      </section>
    </main>

    <footer class="status-bar">
      <div class="status-item">
        <span class="status-label">当前能量</span>
        <strong class="status-value energy">{{ totalEnergy }}</strong>
      </div>
      <div class="status-item">
        <span class="status-label">当前里程</span>
        <strong class="status-value mileage">{{ currentMileage }} km</strong>
      </div>
      <div class="status-item">
        <span class="status-label">已打卡城市</span>
        <strong class="status-value">{{ checkedCount }}/{{ cities.length }}</strong>
      </div>
      <div class="status-item">
        <span class="status-label">终极奖励状态</span>
        <strong class="status-value reward-state">{{ rewardShippingStatusText }}</strong>
      </div>
    </footer>

    <div v-if="showModal" class="modal-mask" @click.self="closeModal">
      <div class="modal-card">
        <div class="modal-head">
          <h3>{{ activeCityName }} · 环保问答</h3>
          <button class="close-btn" @click="closeModal">×</button>
        </div>

        <div v-if="quizLoading" class="quiz-loading">题目加载中...</div>

        <template v-else-if="quiz">
          <div class="event-card" :class="eventThemeClass">
            <div class="event-icon">{{ eventThemeIcon }}</div>
            <div class="event-content">
              <h4>{{ eventTitle }}</h4>
              <p>{{ eventDescription }}</p>
            </div>
          </div>

          <p class="question">{{ quiz.question }}</p>

          <div class="options">
            <button
              v-for="(opt, key) in parsedOptions"
              :key="key"
              class="option-btn"
              :disabled="answering || selectedKey !== ''"
              @click="submitAnswer(String(key))"
            >
              <span class="option-key">{{ key }}</span>
              <span class="option-text">{{ opt }}</span>
            </button>
          </div>

          <p v-if="feedbackMsg" class="feedback" :class="{ success: lastCorrect, fail: !lastCorrect }">
            {{ feedbackMsg }}
          </p>
        </template>

        <div v-else class="quiz-loading">暂无题目，请稍后重试</div>
      </div>

      <div v-if="showConfetti" class="confetti-layer">
        <span v-for="n in 24" :key="n" class="confetti" :style="confettiStyle(n)" />
      </div>
    </div>

    <div v-if="showDrawModal" class="modal-mask" @click.self="dismissDrawModal">
      <div class="draw-modal-card" :class="{ won: drawResult?.won }">
        <h3>{{ drawResult?.won ? '恭喜获得品牌福利' : '本次盲盒未中奖' }}</h3>
        <p class="draw-subtitle">
          {{ drawResult?.won ? `${drawResult.brandName || '合作品牌'} 服务区专属权益已发放` : '再前往下一座合作服务区，中奖概率更高' }}
        </p>

        <div v-if="drawResult?.won" class="draw-prize-card">
          <div class="draw-brand-logo">{{ brandTag(drawResult.brandName) }}</div>
          <div class="draw-prize-content">
            <strong>{{ drawResult.couponTitle }}</strong>
            <p>{{ drawResult.couponDescription }}</p>
          </div>
        </div>

        <div class="draw-actions">
          <button class="wallet-btn" @click="putIntoWallet">
            {{ drawResult?.won ? '放入卡包' : '继续挑战' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showGrandPrizeModal" class="grand-prize-mask" @click.self="dismissGrandPrizeModal">
      <div class="grand-prize-certificate">
        <div class="certificate-header">
          <span>Annual Green Driver Award</span>
          <h2>年度环保车主 荣誉证书</h2>
          <p>恭喜你完成零碳公路之旅，成功解锁终极商业大奖与实体车贴权益</p>
        </div>

        <form class="claim-form" @submit.prevent="submitGrandPrizeClaim">
          <label>
            收货人姓名
            <input v-model.trim="grandPrizeForm.consigneeName" maxlength="100" required />
          </label>
          <label>
            联系电话
            <input v-model.trim="grandPrizeForm.consigneePhone" maxlength="30" required />
          </label>
          <label>
            详细收货地址
            <textarea v-model.trim="grandPrizeForm.shippingAddress" maxlength="500" rows="3" required />
          </label>

          <p v-if="grandPrizeMsg" class="grand-prize-msg">{{ grandPrizeMsg }}</p>

          <div class="certificate-actions">
            <button type="button" class="ghost-btn" @click="dismissGrandPrizeModal">稍后填写</button>
            <button type="submit" class="claim-btn" :disabled="claimingGrandPrize">
              {{ claimingGrandPrize ? '提交中...' : '提交领奖信息' }}
            </button>
          </div>
        </form>
      </div>

      <div class="confetti-layer">
        <span v-for="n in 24" :key="`g-${n}`" class="confetti" :style="confettiStyle(n + 7)" />
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ZeroCarbonJourney',
  data() {
    return {
      userId: null,
      totalEnergy: 0,
      currentMileage: 0,
      cities: [],
      showModal: false,
      quizLoading: false,
      answering: false,
      quiz: null,
      selectedKey: '',
      feedbackMsg: '',
      lastCorrect: false,
      activeNodeIndex: -1,
      showConfetti: false,
      confettiTimer: null,
      nodeStateMap: {},
      openingQuiz: false,
      quizRequestSeq: 0,
      lastSubmitAt: 0,
      submitDebounceMs: 800,
      showDrawModal: false,
      drawResult: null,
      pendingGrandPrize: false,
      showGrandPrizeModal: false,
      claimingGrandPrize: false,
      grandPrizeMsg: '',
      rewardShippingStatus: 'NOT_CLAIMED',
      grandPrizeForm: {
        consigneeName: '',
        consigneePhone: '',
        shippingAddress: ''
      }
    };
  },
  computed: {
    checkedCount() {
      return Object.values(this.nodeStateMap).filter(state => state === 'CHECKED_IN').length;
    },
    activeCityName() {
      const activeCity = this.activeNodeIndex >= 0 ? this.cities[this.activeNodeIndex] : null;
      return activeCity ? activeCity.name : '城市';
    },
    parsedOptions() {
      if (!this.quiz || !this.quiz.options) {
        return {};
      }
      try {
        return JSON.parse(this.quiz.options);
      } catch (error) {
        console.error('题目选项JSON解析失败:', error);
        return {};
      }
    },
    eventTitle() {
      return this.quiz?.eventTitle || `抵达${this.activeCityName}`;
    },
    eventDescription() {
      return this.quiz?.eventDescription || '完成本次绿色知识挑战即可打卡前进。';
    },
    eventThemeClass() {
      const theme = String(this.quiz?.eventTheme || 'default').toLowerCase();
      const supported = ['sandstorm', 'rain', 'coldwave', 'mountain', 'traffic', 'default'];
      return `theme-${supported.includes(theme) ? theme : 'default'}`;
    },
    eventThemeIcon() {
      const theme = String(this.quiz?.eventTheme || 'default').toLowerCase();
      const iconMap = {
        sandstorm: 'SA',
        rain: 'RN',
        coldwave: 'CW',
        mountain: 'MT',
        traffic: 'TF',
        default: 'EV'
      };
      return iconMap[theme] || iconMap.default;
    },
    latestUnlockedIndex() {
      return this.cities.findIndex((city, index) => this.nodeState(index) === 'UNLOCKED');
    },
    roadPath() {
      if (!this.cities.length) {
        return 'M 70 470 L 940 95';
      }

      return this.buildSmoothRoadPath(this.cities);
    },
    rewardShippingStatusText() {
      const map = {
        NOT_CLAIMED: '待申领',
        PREPARING: '奖励已准备发货',
        SHIPPED: '已发货',
        DELIVERED: '已签收'
      };
      return map[this.rewardShippingStatus] || '待申领';
    }
  },
  async mounted() {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    if (!user || !user.id) {
      this.$router.push('/');
      return;
    }

    this.userId = user.id;
    await this.loadJourneyConfig();
    await this.loadJourneyState();
  },
  methods: {
    buildSmoothRoadPath(cities) {
      const points = cities.map(city => ({
        x: Number(city.x) || 0,
        y: Number(city.y) || 0
      }));

      if (points.length === 1) {
        return `M ${points[0].x} ${points[0].y}`;
      }

      if (points.length === 2) {
        return `M ${points[0].x} ${points[0].y} L ${points[1].x} ${points[1].y}`;
      }

      let path = `M ${points[0].x} ${points[0].y}`;
      for (let i = 0; i < points.length - 1; i += 1) {
        const p0 = points[i - 1] || points[i];
        const p1 = points[i];
        const p2 = points[i + 1];
        const p3 = points[i + 2] || points[i + 1];

        const cp1x = p1.x + (p2.x - p0.x) / 6;
        const cp1y = p1.y + (p2.y - p0.y) / 6;
        const cp2x = p2.x - (p3.x - p1.x) / 6;
        const cp2y = p2.y - (p3.y - p1.y) / 6;

        path += ` C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${p2.x} ${p2.y}`;
      }

      return path;
    },
    gamificationApi(path) {
      const baseURL = this.$axios?.defaults?.baseURL || '';
      if (baseURL.endsWith('/api')) {
        return `/gamification${path}`;
      }
      return `/api/gamification${path}`;
    },
    goBack() {
      this.$router.back();
    },
    nodeState(index) {
      return this.nodeStateMap[index] || 'LOCKED';
    },
    isReached(city) {
      const index = this.cities.findIndex(item => item.id === city.id);
      return this.nodeState(index) !== 'LOCKED';
    },
    isCheckedIn(index) {
      return this.nodeState(index) === 'CHECKED_IN';
    },
    isLatestUnlockAndUnchecked(index) {
      return index === this.latestUnlockedIndex && !this.isCheckedIn(index);
    },
    async loadJourneyConfig() {
      try {
        const response = await this.$axios.get(this.gamificationApi('/journey/config'));
        const data = response.data || {};
        const nodes = Array.isArray(data.nodes) ? data.nodes : [];
        this.cities = nodes
          .slice()
          .sort((a, b) => (a.cityIndex || 0) - (b.cityIndex || 0))
          .map(node => ({
            id: (node.cityIndex || 0) + 1,
            name: node.cityName || '',
            mileage: node.requiredMileage || 0,
            x: node.x || 0,
            y: node.y || 0,
            cityIndex: node.cityIndex || 0,
            brandServiceArea: !!node.brandServiceArea,
            brandName: node.brandName || '',
            brandLogoUrl: node.brandLogoUrl || ''
          }));
      } catch (error) {
        console.error('加载零碳路线配置失败:', error);
      }
    },
    async loadJourneyState() {
      try {
        const response = await this.$axios.get(this.gamificationApi('/journey/state/me'));
        const data = response.data || {};

        this.totalEnergy = data.totalEnergy || 0;
        this.currentMileage = data.currentMileage || 0;

        const map = {};
        (data.nodes || []).forEach(node => {
          map[node.cityIndex] = node.nodeState;
        });
        this.nodeStateMap = map;
      } catch (error) {
        console.error('加载零碳状态失败:', error);
      }
    },
    async fetchQuiz(cityIndex) {
      const requestSeq = ++this.quizRequestSeq;
      this.quizLoading = true;
      try {
        const response = await this.$axios.get(this.gamificationApi('/journey/quiz'), {
          params: { cityIndex }
        });
        if (requestSeq === this.quizRequestSeq) {
          this.quiz = response.data;
        }
      } catch (error) {
        console.error('获取城市事件题失败:', error);
        if (requestSeq === this.quizRequestSeq) {
          this.quiz = null;
        }
      } finally {
        if (requestSeq === this.quizRequestSeq) {
          this.quizLoading = false;
        }
      }
    },
    async handleNodeClick(index) {
      if (!this.isLatestUnlockAndUnchecked(index) || this.showModal || this.quizLoading || this.openingQuiz) {
        return;
      }

      this.openingQuiz = true;
      this.activeNodeIndex = index;
      this.selectedKey = '';
      this.feedbackMsg = '';
      this.lastCorrect = false;
      this.showModal = true;
      try {
        const cityIndex = this.cities[index]?.cityIndex ?? index;
        await this.fetchQuiz(cityIndex);
      } finally {
        this.openingQuiz = false;
      }
    },
    closeModal() {
      this.showModal = false;
      this.quiz = null;
      this.selectedKey = '';
      this.feedbackMsg = '';
      this.lastCorrect = false;
    },
    dismissDrawModal() {
      this.showDrawModal = false;
      if (this.pendingGrandPrize) {
        this.showGrandPrizeModal = true;
      }
    },
    dismissGrandPrizeModal() {
      this.showGrandPrizeModal = false;
      this.pendingGrandPrize = false;
      this.closeModal();
    },
    putIntoWallet() {
      this.showDrawModal = false;
      if (this.pendingGrandPrize) {
        this.showGrandPrizeModal = true;
        this.grandPrizeMsg = '';
        return;
      }
      this.closeModal();
      this.drawResult = null;
    },
    async submitGrandPrizeClaim() {
      if (this.claimingGrandPrize) {
        return;
      }

      this.claimingGrandPrize = true;
      this.grandPrizeMsg = '';
      try {
        const response = await this.$axios.post(this.gamificationApi('/journey/claim-grand-prize'), {
          consigneeName: this.grandPrizeForm.consigneeName,
          consigneePhone: this.grandPrizeForm.consigneePhone,
          shippingAddress: this.grandPrizeForm.shippingAddress
        });
        const data = response.data || {};
        this.rewardShippingStatus = data.shippingStatus || 'PREPARING';
        this.grandPrizeMsg = '领奖成功，奖励已准备发货。';
        this.pendingGrandPrize = false;

        setTimeout(() => {
          this.showGrandPrizeModal = false;
          this.drawResult = null;
          this.closeModal();
        }, 700);
      } catch (error) {
        this.grandPrizeMsg = error?.response?.data?.message || '领奖信息提交失败，请稍后重试';
      } finally {
        this.claimingGrandPrize = false;
      }
    },
    brandTag(brandName) {
      if (!brandName) {
        return 'BRAND';
      }
      return String(brandName).slice(0, 6).toUpperCase();
    },
    async submitAnswer(optionKey) {
      if (!this.quiz || this.answering || this.selectedKey) {
        return;
      }

      const now = Date.now();
      if (now - this.lastSubmitAt < this.submitDebounceMs) {
        return;
      }
      this.lastSubmitAt = now;

      this.selectedKey = optionKey;

      this.answering = true;

      try {
        const response = await this.$axios.post(this.gamificationApi('/journey/checkin'), {
          cityIndex: this.cities[this.activeNodeIndex]?.cityIndex ?? this.activeNodeIndex,
          quizId: this.quiz.id,
          selectedAnswer: optionKey
        });

        const result = response.data || {};
        this.lastCorrect = !!result.correct;

        if (!result.correct) {
          this.feedbackMsg = '回答不正确，再试试下一座城市挑战吧。';
          return;
        }

        this.feedbackMsg = '回答正确！能量与里程已更新';
        this.showConfetti = true;
        this.drawResult = result.couponDrawResult || { won: false };
        this.pendingGrandPrize = !!result.journeyCompleted && !result.grandPrizeStickerClaimed;
        this.rewardShippingStatus = result.grandPrizeShipmentStatus || this.rewardShippingStatus;
        await this.loadJourneyState();

        this.confettiTimer = setTimeout(() => {
          this.showConfetti = false;
          this.showDrawModal = true;
          this.confettiTimer = null;
        }, 1100);
      } catch (error) {
        this.feedbackMsg = error?.response?.data?.message || '奖励结算失败，请稍后重试';
        this.showConfetti = false;
        this.selectedKey = '';
      } finally {
        this.answering = false;
      }
    },
    confettiStyle(n) {
      const left = (n * 37) % 100;
      const delay = (n % 9) * 0.06;
      const dur = 1.4 + (n % 5) * 0.12;
      const hue = (n * 43) % 360;
      return {
        left: `${left}%`,
        animationDelay: `${delay}s`,
        animationDuration: `${dur}s`,
        background: `hsl(${hue}, 90%, 58%)`
      };
    }
  }
  ,
  beforeDestroy() {
    if (this.confettiTimer) {
      clearTimeout(this.confettiTimer);
      this.confettiTimer = null;
    }
  }
};
</script>

<style scoped>
.journey-root {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 12%, #dff6e6 0%, transparent 35%),
    radial-gradient(circle at 88% 20%, #d9f0ff 0%, transparent 34%),
    linear-gradient(180deg, #f4f8f5, #e8f2eb);
  color: #1f2937;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 68px;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #e5e7eb;
  backdrop-filter: blur(4px);
}

.title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  text-align: center;
  letter-spacing: 0.02em;
}

.topbar-right {
  justify-self: end;
  color: #6b7280;
  font-size: 13px;
}

.back-btn {
  justify-self: start;
  border: 1px solid #d1d5db;
  background: #ffffff;
  color: #111827;
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 14px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.back-btn:hover {
  border-color: #9ca3af;
  transform: translateY(-1px);
}

.arrow {
  font-size: 16px;
}

.stage {
  flex: 1;
  padding: 24px 24px 116px;
  display: flex;
  justify-content: center;
}

.map-card {
  width: min(1120px, 100%);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.08);
  padding: 20px;
}

.map-caption h2 {
  margin: 0;
  font-size: 22px;
}

.map-caption p {
  margin: 8px 0 0;
  color: #6b7280;
}

.road-map {
  width: 100%;
  height: 460px;
  margin-top: 14px;
}

.road-base {
  fill: none;
  stroke: #cbd5e1;
  stroke-width: 18;
  stroke-linecap: round;
}

.road-dash {
  fill: none;
  stroke: #64748b;
  stroke-width: 4;
  stroke-dasharray: 12 11;
  stroke-linecap: round;
  opacity: 0.75;
}

.city-guide {
  stroke: #cbd5e1;
  stroke-width: 1.5;
  stroke-dasharray: 4 4;
}

.city-node {
  cursor: default;
  transition: all 0.25s ease;
}

.city-node.locked {
  fill: #9ca3af;
  stroke: #6b7280;
  stroke-width: 2;
}

.city-node.reached {
  fill: #22c55e;
  stroke: #16a34a;
  stroke-width: 2.5;
}

.city-node.unlocking {
  cursor: pointer;
}

.city-node.unlocking:hover {
  transform: scale(1.08);
}

.city-node.checked {
  fill: #10b981;
}

.pulse-ring {
  fill: none;
  stroke: rgba(34, 197, 94, 0.55);
  stroke-width: 3;
  animation: pulse 1.8s infinite ease-out;
  cursor: pointer;
}

@keyframes pulse {
  0% {
    opacity: 0.85;
    transform: scale(1);
  }
  100% {
    opacity: 0;
    transform: scale(1.2);
  }
}

.city-name {
  font-size: 15px;
  font-weight: 700;
  fill: #334155;
}

.city-mile {
  font-size: 12px;
  fill: #64748b;
}

.brand-service-marker {
  pointer-events: none;
}

.brand-pill {
  fill: rgba(16, 185, 129, 0.92);
  stroke: rgba(5, 150, 105, 1);
  stroke-width: 1;
}

.brand-pill-text {
  font-size: 11px;
  fill: #ffffff;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.status-bar {
  position: fixed;
  left: 16px;
  right: 16px;
  bottom: 14px;
  height: 84px;
  border-radius: 16px;
  border: 1px solid #d1fae5;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.12);
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  align-items: center;
  z-index: 18;
}

.status-item {
  text-align: center;
}

.status-label {
  display: block;
  color: #6b7280;
  font-size: 12px;
}

.status-value {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.2;
}

.status-value.energy {
  color: #16a34a;
}

.status-value.mileage {
  color: #0f766e;
}

.status-value.reward-state {
  font-size: 16px;
  color: #0ea5a3;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
  display: grid;
  place-items: center;
  z-index: 60;
}

.modal-card {
  width: min(620px, calc(100vw - 28px));
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.25);
  border: 1px solid #e5e7eb;
  padding: 18px 18px 20px;
  position: relative;
}

.draw-modal-card {
  width: min(520px, calc(100vw - 28px));
  border-radius: 18px;
  background: linear-gradient(160deg, #f8fafc, #ecfeff);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.25);
  border: 1px solid #cbd5e1;
  padding: 22px;
}

.grand-prize-mask {
  position: fixed;
  inset: 0;
  background: radial-gradient(circle at 20% 20%, rgba(16, 185, 129, 0.3), rgba(15, 23, 42, 0.8));
  display: grid;
  place-items: center;
  z-index: 88;
}

.grand-prize-certificate {
  width: min(760px, calc(100vw - 24px));
  border-radius: 22px;
  border: 1px solid #86efac;
  background: linear-gradient(160deg, #f0fdf4, #ecfeff);
  box-shadow: 0 24px 64px rgba(2, 44, 34, 0.45);
  padding: 24px;
  position: relative;
  z-index: 89;
}

.certificate-header span {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  background: #dcfce7;
  color: #15803d;
  font-size: 12px;
  font-weight: 700;
}

.certificate-header h2 {
  margin: 10px 0 6px;
  font-size: 30px;
  color: #14532d;
}

.certificate-header p {
  margin: 0;
  color: #334155;
}

.claim-form {
  margin-top: 16px;
  display: grid;
  gap: 10px;
}

.claim-form label {
  display: grid;
  gap: 6px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.claim-form input,
.claim-form textarea {
  border: 1px solid #86efac;
  background: #ffffff;
  border-radius: 10px;
  padding: 10px;
  font-size: 14px;
}

.claim-form input:focus,
.claim-form textarea:focus {
  outline: 0;
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.16);
}

.grand-prize-msg {
  margin: 0;
  color: #166534;
  font-size: 13px;
  font-weight: 600;
}

.certificate-actions {
  margin-top: 6px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.ghost-btn {
  border: 1px solid #a7f3d0;
  background: #ffffff;
  color: #065f46;
  border-radius: 10px;
  padding: 9px 14px;
  font-weight: 600;
  cursor: pointer;
}

.claim-btn {
  border: 0;
  background: linear-gradient(135deg, #22c55e, #14b8a6);
  color: #ffffff;
  border-radius: 10px;
  padding: 9px 16px;
  font-weight: 700;
  cursor: pointer;
}

.claim-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.draw-modal-card.won {
  background: linear-gradient(160deg, #fef9c3, #dcfce7);
  border-color: #84cc16;
}

.draw-modal-card h3 {
  margin: 0;
  font-size: 22px;
}

.draw-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #334155;
}

.draw-prize-card {
  margin-top: 14px;
  border: 1px solid #a7f3d0;
  background: rgba(255, 255, 255, 0.72);
  border-radius: 14px;
  padding: 12px;
  display: grid;
  grid-template-columns: 76px 1fr;
  gap: 12px;
}

.draw-brand-logo {
  border-radius: 10px;
  background: #0f766e;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  display: grid;
  place-items: center;
  letter-spacing: 0.05em;
}

.draw-prize-content strong {
  font-size: 16px;
}

.draw-prize-content p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #475569;
}

.draw-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.wallet-btn {
  border: 0;
  background: linear-gradient(135deg, #059669, #10b981);
  color: #ffffff;
  border-radius: 10px;
  padding: 10px 18px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.wallet-btn:hover {
  filter: brightness(1.05);
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-head h3 {
  margin: 0;
  font-size: 20px;
}

.close-btn {
  border: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  cursor: pointer;
  background: #f3f4f6;
  font-size: 20px;
}

.close-btn:hover {
  background: #e5e7eb;
}

.quiz-loading {
  padding: 30px 0;
  text-align: center;
  color: #6b7280;
}

.question {
  margin-top: 14px;
  margin-bottom: 14px;
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.6;
}

.event-card {
  margin-top: 14px;
  border-radius: 12px;
  border: 1px solid transparent;
  padding: 12px;
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: 12px;
}

.event-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  background: rgba(255, 255, 255, 0.45);
}

.event-content h4 {
  margin: 0;
  font-size: 16px;
}

.event-content p {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.55;
  color: #475569;
}

.event-card.theme-sandstorm {
  background: linear-gradient(135deg, #fff8e1, #ffe9b3);
  border-color: #f59e0b;
}

.event-card.theme-rain {
  background: linear-gradient(135deg, #e0f2fe, #dbeafe);
  border-color: #38bdf8;
}

.event-card.theme-coldwave {
  background: linear-gradient(135deg, #ecfeff, #e0e7ff);
  border-color: #60a5fa;
}

.event-card.theme-mountain {
  background: linear-gradient(135deg, #ecfccb, #dcfce7);
  border-color: #65a30d;
}

.event-card.theme-traffic {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border-color: #d97706;
}

.event-card.theme-default {
  background: linear-gradient(135deg, #f1f5f9, #e2e8f0);
  border-color: #94a3b8;
}

.options {
  display: grid;
  gap: 10px;
}

.option-btn {
  border: 1px solid #d1d5db;
  background: #ffffff;
  border-radius: 12px;
  text-align: left;
  padding: 12px;
  display: grid;
  grid-template-columns: 28px 1fr;
  gap: 10px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.option-btn:hover:not(:disabled) {
  border-color: #22c55e;
  transform: translateY(-1px);
}

.option-btn:disabled {
  opacity: 0.72;
  cursor: not-allowed;
}

.option-key {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #ecfeff;
  color: #0f766e;
  font-weight: 700;
}

.option-text {
  align-self: center;
}

.feedback {
  margin-top: 14px;
  font-size: 14px;
  font-weight: 600;
}

.feedback.success {
  color: #15803d;
}

.feedback.fail {
  color: #b91c1c;
}

.confetti-layer {
  pointer-events: none;
  position: fixed;
  inset: 0;
  overflow: hidden;
  z-index: 70;
}

.confetti {
  position: absolute;
  top: -20px;
  width: 10px;
  height: 16px;
  border-radius: 3px;
  animation-name: fall;
  animation-timing-function: ease-in;
  animation-fill-mode: both;
}

@keyframes fall {
  0% {
    transform: translateY(-20px) rotate(0deg);
    opacity: 1;
  }
  100% {
    transform: translateY(110vh) rotate(560deg);
    opacity: 0.95;
  }
}

@media (max-width: 900px) {
  .topbar {
    grid-template-columns: 1fr auto;
    row-gap: 4px;
  }

  .topbar-right {
    display: none;
  }

  .title {
    justify-self: end;
    font-size: 16px;
  }

  .road-map {
    height: 380px;
  }

  .status-value {
    font-size: 18px;
  }
}

@media (max-width: 640px) {
  .stage {
    padding: 12px 12px 106px;
  }

  .map-card {
    padding: 12px;
    border-radius: 14px;
  }

  .map-caption h2 {
    font-size: 18px;
  }

  .map-caption p {
    font-size: 13px;
  }

  .road-map {
    height: 300px;
  }
}
</style>
