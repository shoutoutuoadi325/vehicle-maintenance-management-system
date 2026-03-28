<template>
  <div class="ai-diagnosis-client">
    <div class="diagnosis-intro">
      <div class="intro-card">
        <i class="fas fa-robot"></i>
        <h3>AI智能问诊助手</h3>
        <p>详细描述您车辆的异常现象，AI将为您分析可能原因、严重等级和预估维修费用</p>
      </div>
    </div>

    <div class="diagnosis-form-section">
      <form @submit.prevent="submitDiagnosis" class="diagnosis-form">
        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-clipboard-list"></i> 故障描述
            <span class="required">*</span>
          </label>
          <textarea
            v-model="form.problemDescription"
            class="form-input diagnosis-textarea"
            rows="6"
            placeholder="请详细描述车辆的故障现象，例如：
- 启动困难、发动机异响
- 刹车异常、方向盘抖动
- 油耗增加、动力不足
描述越详细，诊断结果越准确。"
            required
            :disabled="loading"
          ></textarea>
        </div>

        <button
          type="submit"
          class="btn btn-primary btn-large"
          :disabled="loading || !form.problemDescription.trim()"
        >
          <i :class="loading ? 'fas fa-spinner fa-spin' : 'fas fa-search'"></i>
          {{ loading ? 'AI诊断中...' : '开始诊断' }}
        </button>
      </form>
    </div>

    <!-- 诊断结果 -->
    <div v-if="result" class="diagnosis-result">
      <div class="result-header">
        <i class="fas fa-check-circle"></i>
        <h3>诊断结果</h3>
      </div>

      <div class="result-content">
        <div class="result-item">
          <div class="result-label">
            <i class="fas fa-exclamation-triangle"></i> 故障类型
          </div>
          <div class="result-value fault-type">{{ result.faultType }}</div>
        </div>

        <div v-if="result.severityLevel" class="result-item">
          <div class="result-label">
            <i class="fas fa-tachometer-alt"></i> 严重等级
          </div>
          <div class="result-value">
            <span :class="['severity-badge', getSeverityClass(result.severityLevel)]">
              {{ result.severityLevel }}
            </span>
          </div>
        </div>

        <div v-if="result.possibleCauses && result.possibleCauses.length" class="result-item">
          <div class="result-label">
            <i class="fas fa-search"></i> 可能原因
          </div>
          <ul class="causes-list">
            <li v-for="(cause, i) in result.possibleCauses" :key="i">
              <i class="fas fa-dot-circle"></i> {{ cause }}
            </li>
          </ul>
        </div>

        <div v-if="result.estimatedCost" class="result-item">
          <div class="result-label">
            <i class="fas fa-dollar-sign"></i> 预估维修费用
          </div>
          <div class="result-value cost-value">{{ result.estimatedCost }}</div>
        </div>

        <div v-if="result.estimatedTime" class="result-item">
          <div class="result-label">
            <i class="fas fa-clock"></i> 预估维修工时
          </div>
          <div class="result-value">{{ result.estimatedTime }}</div>
        </div>

        <div v-if="result.suggestion" class="result-item">
          <div class="result-label">
            <i class="fas fa-lightbulb"></i> 维修建议
          </div>
          <div class="result-value suggestion">{{ result.suggestion }}</div>
        </div>
      </div>

      <div class="result-actions">
        <button @click="$emit('create-order', result, form.problemDescription)" class="btn btn-primary">
          <i class="fas fa-plus"></i> 一键转化为维修工单
        </button>
        <button @click="clearDiagnosis" class="btn btn-outline">
          <i class="fas fa-redo"></i> 重新诊断
        </button>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="diagnosis-error">
      <i class="fas fa-exclamation-circle"></i>
      <p>{{ error }}</p>
      <button @click="error = null" class="btn btn-outline btn-small">关闭</button>
    </div>

    <!-- 历史记录 -->
    <div v-if="history.length > 0" class="diagnosis-history">
      <h3><i class="fas fa-history"></i> 最近诊断记录</h3>
      <div class="history-list">
        <div v-for="(item, index) in history" :key="index" class="history-item">
          <div class="history-header">
            <span class="history-time">{{ formatDate(item.timestamp) }}</span>
            <span v-if="item.severityLevel" :class="['severity-badge', getSeverityClass(item.severityLevel)]">
              {{ item.severityLevel }}
            </span>
          </div>
          <div class="history-problem">
            <strong>问题：</strong>{{ item.problemDescription }}
          </div>
          <div class="history-result">
            <strong>诊断：</strong>{{ item.faultType }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'AIDiagnosisClient',
  emits: ['create-order'],
  data() {
    return {
      form: {
        problemDescription: ''
      },
      result: null,
      error: null,
      loading: false,
      history: []
    };
  },
  created() {
    this.loadHistory();
  },
  methods: {
    async submitDiagnosis() {
      if (!this.form.problemDescription.trim()) return;
      this.loading = true;
      this.result = null;
      this.error = null;

      try {
        const token = localStorage.getItem('token');
        const response = await axios.post('/api/ai-diagnosis/diagnose', {
          problemDescription: this.form.problemDescription
        }, {
          headers: token ? { Authorization: `Bearer ${token}` } : {}
        });

        if (response.data.success) {
          this.result = response.data;
          this.saveToHistory(response.data);
        } else {
          this.error = response.data.errorMessage || 'AI诊断失败，请稍后再试';
        }
      } catch (err) {
        this.error = err.response?.data?.errorMessage || 'AI诊断服务暂时不可用，请稍后再试';
      } finally {
        this.loading = false;
      }
    },

    clearDiagnosis() {
      this.result = null;
      this.error = null;
      this.form.problemDescription = '';
    },

    saveToHistory(diagResult) {
      const record = {
        timestamp: new Date().toISOString(),
        problemDescription: this.form.problemDescription,
        faultType: diagResult.faultType,
        severityLevel: diagResult.severityLevel,
        suggestion: diagResult.suggestion
      };

      this.history.unshift(record);
      if (this.history.length > 10) {
        this.history = this.history.slice(0, 10);
      }

      try {
        localStorage.setItem('diagnosisHistory', JSON.stringify(this.history));
      } catch (e) {
        // ignore storage errors
      }
    },

    loadHistory() {
      try {
        const stored = localStorage.getItem('diagnosisHistory');
        if (stored) {
          this.history = JSON.parse(stored);
        }
      } catch (e) {
        this.history = [];
      }
    },

    getSeverityClass(level) {
      if (!level) return '';
      const l = level.trim();
      if (l === '低') return 'severity-low';
      if (l === '中') return 'severity-medium';
      if (l === '高') return 'severity-high';
      if (l === '紧急') return 'severity-critical';
      return 'severity-medium';
    },

    formatDate(dateStr) {
      if (!dateStr) return '';
      const d = new Date(dateStr);
      return d.toLocaleString('zh-CN');
    }
  }
};
</script>

<style scoped>
.ai-diagnosis-client {
  max-width: 800px;
}

.intro-card {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.intro-card i {
  font-size: 2rem;
}

.intro-card h3 {
  margin: 0 0 0.25rem;
  font-size: 1.1rem;
}

.intro-card p {
  margin: 0;
  font-size: 0.9rem;
  opacity: 0.9;
}

.diagnosis-textarea {
  width: 100%;
  resize: vertical;
  font-size: 0.95rem;
  line-height: 1.5;
  box-sizing: border-box;
}

.diagnosis-result {
  background: #f8faff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1.5rem;
  margin-top: 1.5rem;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #10b981;
  margin-bottom: 1.25rem;
}

.result-header i {
  font-size: 1.25rem;
}

.result-header h3 {
  margin: 0;
  font-size: 1.1rem;
}

.result-item {
  margin-bottom: 1rem;
}

.result-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 0.35rem;
}

.result-label i {
  margin-right: 0.3rem;
}

.result-value {
  color: #1e293b;
  font-size: 0.95rem;
  line-height: 1.5;
}

.fault-type {
  font-weight: 600;
  color: #dc2626;
  font-size: 1rem;
}

.cost-value {
  color: #d97706;
  font-weight: 600;
}

.suggestion {
  background: #fff;
  border-left: 3px solid #3b82f6;
  padding: 0.75rem 1rem;
  border-radius: 0 8px 8px 0;
  white-space: pre-wrap;
}

.causes-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.causes-list li {
  padding: 0.3rem 0;
  font-size: 0.95rem;
  color: #374151;
}

.causes-list li i {
  color: #6366f1;
  margin-right: 0.4rem;
  font-size: 0.7rem;
}

.severity-badge {
  display: inline-block;
  padding: 0.2rem 0.7rem;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
}

.severity-low {
  background: #d1fae5;
  color: #065f46;
}

.severity-medium {
  background: #fef3c7;
  color: #92400e;
}

.severity-high {
  background: #fee2e2;
  color: #991b1b;
}

.severity-critical {
  background: #dc2626;
  color: white;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.result-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1.25rem;
  flex-wrap: wrap;
}

.diagnosis-error {
  background: #fef2f2;
  border: 1px solid #fca5a5;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  margin-top: 1rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: #dc2626;
}

.diagnosis-error i {
  font-size: 1.1rem;
  flex-shrink: 0;
}

.diagnosis-error p {
  margin: 0;
  flex: 1;
}

.diagnosis-history {
  margin-top: 2rem;
}

.diagnosis-history h3 {
  font-size: 1rem;
  color: #374151;
  margin-bottom: 0.75rem;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.history-item {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 0.85rem 1rem;
  font-size: 0.9rem;
}

.history-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.4rem;
}

.history-time {
  font-size: 0.8rem;
  color: #9ca3af;
}

.history-problem,
.history-result {
  color: #374151;
  margin-bottom: 0.25rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.required {
  color: #ef4444;
  margin-left: 0.2rem;
}

.btn-large {
  padding: 0.75rem 2rem;
  font-size: 1rem;
  margin-top: 0.75rem;
}
</style>
