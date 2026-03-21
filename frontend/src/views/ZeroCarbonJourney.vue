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
            d="M 70 470 C 210 390, 260 340, 360 300 C 480 250, 560 260, 650 200 C 760 130, 820 170, 940 95"
            class="road-base"
          />
          <path
            d="M 70 470 C 210 390, 260 340, 360 300 C 480 250, 560 260, 650 200 C 760 130, 820 170, 940 95"
            class="road-dash"
          />

          <g v-for="(city, idx) in cities" :key="city.id">
            <line :x1="city.x" :x2="city.x" :y1="city.y" :y2="city.y + 42" class="city-guide" />

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
    </footer>

    <div v-if="showModal" class="modal-mask" @click.self="closeModal">
      <div class="modal-card">
        <div class="modal-head">
          <h3>{{ activeCityName }} · 环保问答</h3>
          <button class="close-btn" @click="closeModal">×</button>
        </div>

        <div v-if="quizLoading" class="quiz-loading">题目加载中...</div>

        <template v-else-if="quiz">
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
      cities: [
        { id: 1, name: '成都', mileage: 0, x: 70, y: 470 },
        { id: 2, name: '康定', mileage: 120, x: 285, y: 345 },
        { id: 3, name: '理塘', mileage: 260, x: 470, y: 270 },
        { id: 4, name: '林芝', mileage: 420, x: 680, y: 195 },
        { id: 5, name: '拉萨', mileage: 580, x: 940, y: 95 }
      ],
      showModal: false,
      quizLoading: false,
      answering: false,
      quiz: null,
      selectedKey: '',
      feedbackMsg: '',
      lastCorrect: false,
      activeNodeIndex: -1,
      showConfetti: false,
      checkedInMap: {}
    };
  },
  computed: {
    checkedCount() {
      return Object.values(this.checkedInMap).filter(Boolean).length;
    },
    activeCityName() {
      return this.activeNodeIndex >= 0 ? this.cities[this.activeNodeIndex].name : '城市';
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
    latestUnlockedIndex() {
      let latest = -1;
      this.cities.forEach((city, index) => {
        if (this.currentMileage >= city.mileage) {
          latest = index;
        }
      });
      return latest;
    }
  },
  async mounted() {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    if (!user || !user.id) {
      this.$router.push('/');
      return;
    }

    this.userId = user.id;
    this.loadCheckins();
    await this.loadEnergyAccount();
  },
  methods: {
    goBack() {
      this.$router.back();
    },
    isReached(city) {
      return this.currentMileage >= city.mileage;
    },
    isCheckedIn(index) {
      return !!this.checkedInMap[index];
    },
    isLatestUnlockAndUnchecked(index) {
      return index === this.latestUnlockedIndex && !this.isCheckedIn(index);
    },
    checkinStorageKey() {
      return `journey-checkins-${this.userId}`;
    },
    loadCheckins() {
      const raw = localStorage.getItem(this.checkinStorageKey());
      this.checkedInMap = raw ? JSON.parse(raw) : {};
    },
    persistCheckins() {
      localStorage.setItem(this.checkinStorageKey(), JSON.stringify(this.checkedInMap));
    },
    async loadEnergyAccount() {
      try {
        const response = await this.$axios.get(`/gamification/account/${this.userId}`);
        this.totalEnergy = response.data.totalEnergy || 0;
        this.currentMileage = response.data.currentMileage || 0;
      } catch (error) {
        console.error('加载零碳账户失败:', error);
      }
    },
    async fetchQuiz() {
      this.quizLoading = true;
      try {
        const response = await this.$axios.get('/gamification/quiz/random');
        this.quiz = response.data;
      } catch (error) {
        console.error('获取随机题目失败:', error);
        this.quiz = null;
      } finally {
        this.quizLoading = false;
      }
    },
    async handleNodeClick(index) {
      if (!this.isLatestUnlockAndUnchecked(index)) {
        return;
      }

      this.activeNodeIndex = index;
      this.selectedKey = '';
      this.feedbackMsg = '';
      this.lastCorrect = false;
      this.showModal = true;
      await this.fetchQuiz();
    },
    closeModal() {
      this.showModal = false;
      this.quiz = null;
      this.selectedKey = '';
      this.feedbackMsg = '';
      this.lastCorrect = false;
    },
    async submitAnswer(optionKey) {
      if (!this.quiz || this.answering || this.selectedKey) {
        return;
      }

      this.selectedKey = optionKey;
      const isCorrect = String(optionKey).trim() === String(this.quiz.correctAnswer).trim();
      this.lastCorrect = isCorrect;

      if (!isCorrect) {
        this.feedbackMsg = '回答不正确，再试试下一座城市挑战吧。';
        return;
      }

      this.answering = true;
      this.feedbackMsg = '回答正确！能量与里程已更新';
      this.showConfetti = true;

      try {
        await this.$axios.post('/gamification/quiz/answer', {
          userId: this.userId,
          quizId: this.quiz.id,
          isCorrect: true
        });

        this.$set(this.checkedInMap, this.activeNodeIndex, true);
        this.persistCheckins();
        await this.loadEnergyAccount();

        setTimeout(() => {
          this.closeModal();
          this.showConfetti = false;
        }, 1100);
      } catch (error) {
        this.feedbackMsg = '奖励结算失败，请稍后重试';
        this.showConfetti = false;
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
  grid-template-columns: repeat(3, 1fr);
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
