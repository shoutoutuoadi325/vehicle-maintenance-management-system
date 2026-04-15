<template>
  <div class="ai-diagnosis-client-page">
    <header class="page-header">
      <div>
        <h1>AI 故障问诊</h1>
        <p>像聊天一样描述车辆异常，获取结构化诊断建议。</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-outline" @click="$router.push('/customer')">返回客户控制台</button>
      </div>
    </header>

    <section class="chat-panel">
      <div class="chat-history" ref="chatHistory">
        <div
          v-for="item in messages"
          :key="item.id"
          class="chat-item"
          :class="item.role"
        >
          <div class="bubble">
            <div class="role-label">{{ item.role === 'user' ? '你' : 'AI 诊断助手' }}</div>
            <template v-if="item.role === 'user'">
              <div class="content">{{ item.text || '（仅上传了故障图片）' }}</div>
              <div v-if="item.images && item.images.length" class="message-image-list">
                <img
                  v-for="(image, index) in item.images"
                  :key="`${item.id}-image-${index}`"
                  :src="image"
                  alt="故障图片"
                />
              </div>
            </template>
            <div class="content markdown" v-else v-html="renderMarkdown(item.text)"></div>
          </div>
        </div>
        <div v-if="loading" class="chat-item ai">
          <div class="bubble">
            <div class="role-label">AI 诊断助手</div>
            <div class="content">正在分析中，请稍候...</div>
          </div>
        </div>
      </div>

      <form class="composer" @submit.prevent="submitDiagnosis">
        <textarea
          v-model="problemDescription"
          rows="4"
          class="composer-input"
          :disabled="loading"
          placeholder="例如：冷车启动抖动，怠速不稳，伴随发动机故障灯偶发点亮。"
        ></textarea>
        <div class="composer-media">
          <label class="media-upload-btn">
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp"
              multiple
              :disabled="loading"
              @change="onImageSelected"
            >
            <span>上传图片</span>
          </label>
          <span class="media-upload-hint">最多 {{ maxImageCount }} 张，单张不超过 {{ maxImageSizeMB }}MB</span>
        </div>
        <div v-if="selectedImages.length" class="media-preview-grid">
          <div
            v-for="(image, index) in selectedImages"
            :key="image.id"
            class="media-preview-item"
          >
            <img :src="image.previewUrl" :alt="`附件${index + 1}`">
            <button
              type="button"
              class="media-remove-btn"
              :disabled="loading"
              @click="removeSelectedImage(index)"
              aria-label="删除图片"
            >
              ×
            </button>
          </div>
        </div>
        <div class="composer-actions">
          <button class="btn btn-primary" type="submit" :disabled="loading || (!problemDescription.trim() && !selectedImages.length)">
            {{ loading ? '诊断中...' : '发送诊断' }}
          </button>
        </div>
      </form>
    </section>

    <section v-if="diagnosisResult" class="result-panel">
      <h2>结构化诊断结果</h2>
      <div class="result-grid">
        <article class="result-card">
          <h3>可能原因</h3>
          <ul>
            <li v-for="(cause, index) in diagnosisResult.possibleCauses" :key="index">{{ cause }}</li>
            <li v-if="!diagnosisResult.possibleCauses.length">AI 未返回明确原因，请参考下方建议。</li>
          </ul>
        </article>

        <article class="result-card">
          <h3>严重等级</h3>
          <p :class="['severity', (diagnosisResult.severityLevel || 'LOW').toLowerCase()]">
            {{ diagnosisResult.severityLevel || 'LOW' }}
          </p>
        </article>

        <article class="result-card">
          <h3>预估维修费用</h3>
          <p>
            ¥{{ diagnosisResult.estimatedCostMin || 0 }}
            <span v-if="diagnosisResult.estimatedCostMax && diagnosisResult.estimatedCostMax !== diagnosisResult.estimatedCostMin">
              - ¥{{ diagnosisResult.estimatedCostMax }}
            </span>
          </p>
          <h3 style="margin-top: 0.6rem;">预估维修时长</h3>
          <p>
            {{ diagnosisResult.estimatedHoursMin || 0 }}h
            <span v-if="diagnosisResult.estimatedHoursMax && diagnosisResult.estimatedHoursMax !== diagnosisResult.estimatedHoursMin">
              - {{ diagnosisResult.estimatedHoursMax }}h
            </span>
          </p>
        </article>
      </div>

      <div class="result-actions">
        <button class="btn btn-primary" @click="convertToRepairOrder">一键转化为维修工单</button>
        <button class="btn btn-outline" @click="resetAll">重新开始</button>
      </div>
    </section>

    <div v-if="error" class="error-banner">{{ error }}</div>
  </div>
</template>

<script>
import { marked } from 'marked'

export default {
  name: 'AIDiagnosisClient',
  data() {
    return {
      problemDescription: '',
      loading: false,
      error: '',
      messages: [],
      diagnosisResult: null,
      lastDiagnosisInput: '',
      selectedImages: [],
      maxImageCount: 3,
      maxImageSizeMB: 4
    }
  },
  methods: {
    renderMarkdown(text) {
      return marked.parse(text || '')
    },
    async onImageSelected(event) {
      const files = Array.from(event.target.files || [])
      event.target.value = ''
      if (!files.length) {
        return
      }

      const remainingSlots = this.maxImageCount - this.selectedImages.length
      if (remainingSlots <= 0) {
        this.error = `最多上传 ${this.maxImageCount} 张图片`
        return
      }

      const acceptedFiles = files.slice(0, remainingSlots)
      if (files.length > remainingSlots) {
        this.error = `最多上传 ${this.maxImageCount} 张图片，超出部分已忽略`
      }

      for (const file of acceptedFiles) {
        if (!file.type.startsWith('image/')) {
          this.error = `文件 ${file.name} 不是图片格式`
          continue
        }
        if (file.size > this.maxImageSizeMB * 1024 * 1024) {
          this.error = `文件 ${file.name} 超过 ${this.maxImageSizeMB}MB 限制`
          continue
        }

        try {
          const dataUrl = await this.toDataUrl(file)
          this.selectedImages.push({
            id: `${Date.now()}-${Math.random()}`,
            name: file.name,
            dataUrl,
            previewUrl: dataUrl
          })
        } catch (err) {
          this.error = `文件 ${file.name} 读取失败`
        }
      }
    },
    removeSelectedImage(index) {
      this.selectedImages.splice(index, 1)
    },
    clearSelectedImages() {
      this.selectedImages = []
    },
    toDataUrl(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = () => resolve(reader.result)
        reader.onerror = () => reject(new Error('read-failed'))
        reader.readAsDataURL(file)
      })
    },
    async submitDiagnosis() {
      const input = this.problemDescription.trim()
      const imageDataUrls = this.selectedImages.map(item => item.dataUrl)
      if ((!input && !imageDataUrls.length) || this.loading) {
        return
      }

      this.loading = true
      this.error = ''
      this.lastDiagnosisInput = input || `用户上传了 ${imageDataUrls.length} 张故障图片（未填写文字描述）`

      this.messages.push({
        id: Date.now() + '-u',
        role: 'user',
        text: input || '（仅上传了故障图片）',
        images: imageDataUrls
      })

      try {
        const response = await this.$axios.post('/ai-diagnosis/diagnose', {
          problemDescription: input,
          role: 'customer',
          imageDataUrls
        })

        const payload = response.data || {}
        if (!payload.success) {
          throw new Error(payload.errorMessage || 'AI 诊断失败')
        }

        this.diagnosisResult = {
          faultType: payload.faultType || '综合诊断',
          suggestion: payload.suggestion || '',
          severityLevel: payload.severityLevel || 'LOW',
          possibleCauses: Array.isArray(payload.possibleCauses) ? payload.possibleCauses : [],
          estimatedCostMin: payload.estimatedCostMin,
          estimatedCostMax: payload.estimatedCostMax,
          estimatedHoursMin: payload.estimatedHoursMin,
          estimatedHoursMax: payload.estimatedHoursMax
        }

        const aiMarkdown = [
          `## ${this.diagnosisResult.faultType}`,
          this.diagnosisResult.suggestion || '暂无建议。'
        ].join('\n\n')

        this.messages.push({
          id: Date.now() + '-a',
          role: 'ai',
          text: aiMarkdown
        })

        this.problemDescription = ''
        this.clearSelectedImages()
        this.$nextTick(this.scrollToBottom)
      } catch (err) {
        this.error = err.response?.data?.errorMessage || err.message || 'AI 诊断服务暂不可用'
      } finally {
        this.loading = false
      }
    },
    scrollToBottom() {
      const el = this.$refs.chatHistory
      if (el) {
        el.scrollTop = el.scrollHeight
      }
    },
    convertToRepairOrder() {
      if (!this.diagnosisResult) {
        return
      }

      const orderDescription = [
        this.lastDiagnosisInput,
        '',
        'AI诊断结果：',
        `故障类型：${this.diagnosisResult.faultType}`,
        `建议：${this.diagnosisResult.suggestion}`
      ].join('\n')

      this.$router.push({
        path: '/customer',
        query: {
          createOrder: '1',
          diagnosisDesc: encodeURIComponent(orderDescription)
        }
      })
    },
    resetAll() {
      this.problemDescription = ''
      this.diagnosisResult = null
      this.error = ''
      this.messages = []
      this.lastDiagnosisInput = ''
      this.clearSelectedImages()
    }
  }
}
</script>

<style scoped>
.ai-diagnosis-client-page {
  min-height: 100vh;
  background: #f8fafc;
  padding: 1.2rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.page-header h1 {
  margin: 0;
  color: #1f2937;
}

.page-header p {
  margin-top: 0.35rem;
  color: #64748b;
}

.chat-panel,
.result-panel {
  background: #ffffff;
  border-radius: 0.9rem;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
  padding: 1rem;
  margin-bottom: 1rem;
}

.chat-history {
  max-height: 350px;
  overflow-y: auto;
  padding: 0.6rem;
  border: 1px solid #e2e8f0;
  border-radius: 0.7rem;
  background: #f8fafc;
}

.chat-item {
  display: flex;
  margin-bottom: 0.7rem;
}

.chat-item.user {
  justify-content: flex-end;
}

.chat-item.ai {
  justify-content: flex-start;
}

.bubble {
  max-width: 82%;
  padding: 0.7rem;
  border-radius: 0.7rem;
  background: #ffffff;
  border: 1px solid #e5e7eb;
}

.chat-item.user .bubble {
  background: #dbeafe;
  border-color: #bfdbfe;
}

.role-label {
  font-size: 0.78rem;
  color: #475569;
  margin-bottom: 0.3rem;
}

.content {
  color: #1f2937;
  line-height: 1.5;
  white-space: pre-wrap;
}

.content.markdown :deep(h1),
.content.markdown :deep(h2),
.content.markdown :deep(h3) {
  margin: 0.5rem 0;
}

.composer {
  margin-top: 0.8rem;
}

.composer-input {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 0.6rem;
  padding: 0.7rem;
  resize: vertical;
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.6rem;
}

.composer-media {
  margin-top: 0.6rem;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.media-upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.55rem;
  padding: 0.35rem 0.75rem;
  color: #334155;
  background: #ffffff;
  cursor: pointer;
  font-size: 0.85rem;
}

.media-upload-btn input {
  display: none;
}

.media-upload-hint {
  font-size: 0.78rem;
  color: #64748b;
}

.media-preview-grid {
  margin-top: 0.6rem;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(82px, 1fr));
  gap: 0.5rem;
}

.media-preview-item {
  position: relative;
  border: 1px solid #dbe2ea;
  border-radius: 0.5rem;
  overflow: hidden;
  background: #ffffff;
}

.media-preview-item img {
  width: 100%;
  height: 76px;
  object-fit: cover;
  display: block;
}

.media-remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  border: none;
  border-radius: 999px;
  width: 20px;
  height: 20px;
  line-height: 20px;
  text-align: center;
  background: rgba(15, 23, 42, 0.7);
  color: #ffffff;
  cursor: pointer;
}

.message-image-list {
  margin-top: 0.5rem;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(76px, 1fr));
  gap: 0.4rem;
}

.message-image-list img {
  width: 100%;
  height: 70px;
  object-fit: cover;
  border-radius: 0.45rem;
  border: 1px solid #dbe2ea;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.7rem;
}

.result-card {
  border: 1px solid #e2e8f0;
  border-radius: 0.7rem;
  padding: 0.7rem;
  background: #f8fafc;
}

.result-card h3 {
  margin: 0 0 0.4rem;
  color: #1f2937;
  font-size: 0.98rem;
}

.result-card ul {
  margin: 0;
  padding-left: 1.1rem;
}

.severity {
  font-weight: 700;
  letter-spacing: 0.02em;
}

.severity.low { color: #059669; }
.severity.medium { color: #d97706; }
.severity.high { color: #b45309; }
.severity.critical { color: #dc2626; }

.result-actions {
  display: flex;
  gap: 0.6rem;
  margin-top: 0.9rem;
}

.btn {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #1f2937;
  padding: 0.5rem 1rem;
  border-radius: 0.55rem;
  cursor: pointer;
}

.btn-primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #ffffff;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-banner {
  background: #fee2e2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  border-radius: 0.6rem;
  padding: 0.7rem;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.6rem;
  }

  .result-actions {
    flex-direction: column;
  }
}
</style>
