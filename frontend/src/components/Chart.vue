<template>
  <div class="chart-container">
    <h4 v-if="title" class="chart-title">{{ title }}</h4>
    <canvas ref="chartCanvas" :width="width" :height="height"></canvas>
  </div>
</template>

<script>
export default {
  name: 'Chart',
  props: {
    title: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      default: 'bar', // 'bar', 'pie', 'line'
      validator: value => ['bar', 'pie', 'line'].includes(value)
    },
    data: {
      type: Array,
      required: true,
      default: () => []
    },
    width: {
      type: Number,
      default: 400
    },
    height: {
      type: Number,
      default: 300
    },
    size: {
      type: Number,
      default: 200
    }
  },
  mounted() {
    this.renderChart();
  },
  watch: {
    data: {
      handler() {
        this.renderChart();
      },
      deep: true
    }
  },
  methods: {
    renderChart() {
      const canvas = this.$refs.chartCanvas;
      const ctx = canvas.getContext('2d');
      
      // 清空画布
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      
      if (!this.data || this.data.length === 0) {
        this.drawNoDataMessage(ctx, canvas);
        return;
      }
      
      if (this.type === 'bar') {
        this.drawBarChart(ctx, canvas);
      } else if (this.type === 'pie') {
        this.drawPieChart(ctx, canvas);
      }
    },
    
    drawNoDataMessage(ctx, canvas) {
      ctx.fillStyle = '#6b7280';
      ctx.font = '16px Arial';
      ctx.textAlign = 'center';
      ctx.fillText('暂无数据', canvas.width / 2, canvas.height / 2);
    },
    
    drawBarChart(ctx, canvas) {
      const padding = 60;
      const chartWidth = canvas.width - padding * 2;
      const chartHeight = canvas.height - padding * 2;
      
      // 获取最大值用于缩放
      const maxValue = Math.max(...this.data.map(d => d.value));
      if (maxValue === 0) {
        this.drawNoDataMessage(ctx, canvas);
        return;
      }
      
      const barWidth = chartWidth / this.data.length * 0.8;
      const barSpacing = chartWidth / this.data.length * 0.2;
      
      // 绘制坐标轴
      ctx.strokeStyle = '#e5e7eb';
      ctx.lineWidth = 1;
      
      // Y轴
      ctx.beginPath();
      ctx.moveTo(padding, padding);
      ctx.lineTo(padding, padding + chartHeight);
      ctx.stroke();
      
      // X轴
      ctx.beginPath();
      ctx.moveTo(padding, padding + chartHeight);
      ctx.lineTo(padding + chartWidth, padding + chartHeight);
      ctx.stroke();
      
      // 绘制柱状图
      const maxTick = this.drawYAxisTicks(ctx, padding, chartWidth, chartHeight, maxValue);
      
      this.data.forEach((item, index) => {
        const barHeight = (item.value / maxTick) * chartHeight;
        const x = padding + index * (barWidth + barSpacing) + barSpacing / 2;
        const y = padding + chartHeight - barHeight;
        
        // 绘制柱子
        const gradient = ctx.createLinearGradient(0, y, 0, y + barHeight);
        gradient.addColorStop(0, '#3b82f6');
        gradient.addColorStop(1, '#1e40af');
        
        ctx.fillStyle = gradient;
        ctx.fillRect(x, y, barWidth, barHeight);
        
        // 绘制边框
        ctx.strokeStyle = '#1e40af';
        ctx.lineWidth = 1;
        ctx.strokeRect(x, y, barWidth, barHeight);
        
        // 绘制数值标签
        ctx.fillStyle = '#374151';
        ctx.font = '12px Arial';
        ctx.textAlign = 'center';
        ctx.fillText(item.value.toString(), x + barWidth / 2, y - 5);
        
        // 绘制标签（旋转）
        ctx.save();
        ctx.translate(x + barWidth / 2, padding + chartHeight + 15);
        ctx.rotate(-Math.PI / 4);
        ctx.fillStyle = '#6b7280';
        ctx.font = '11px Arial';
        ctx.textAlign = 'right';
        ctx.fillText(item.label, 0, 0);
        ctx.restore();
      });
    },
    
    drawPieChart(ctx, canvas) {
      const centerX = canvas.width / 2;
      const centerY = canvas.height / 2;
      const radius = Math.min(centerX, centerY) - 40;
      
      const total = this.data.reduce((sum, d) => sum + d.value, 0);
      if (total === 0) {
        this.drawNoDataMessage(ctx, canvas);
        return;
      }
      
      let currentAngle = -Math.PI / 2; // 从顶部开始
      
      // 颜色数组
      const colors = [
        '#3b82f6', '#ef4444', '#10b981', '#f59e0b', 
        '#8b5cf6', '#06b6d4', '#84cc16', '#f97316'
      ];
      
      this.data.forEach((item, index) => {
        const sliceAngle = (item.value / total) * 2 * Math.PI;
        
        // 绘制扇形
        ctx.beginPath();
        ctx.moveTo(centerX, centerY);
        ctx.arc(centerX, centerY, radius, currentAngle, currentAngle + sliceAngle);
        ctx.closePath();
        
        ctx.fillStyle = colors[index % colors.length];
        ctx.fill();
        
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 2;
        ctx.stroke();
        
        // 绘制标签
        const labelAngle = currentAngle + sliceAngle / 2;
        const labelX = centerX + Math.cos(labelAngle) * (radius + 25);
        const labelY = centerY + Math.sin(labelAngle) * (radius + 25);
        
        ctx.fillStyle = '#374151';
        ctx.font = '12px Arial';
        ctx.textAlign = 'center';
        ctx.fillText(`${item.label}`, labelX, labelY);
        ctx.fillText(`${item.value}`, labelX, labelY + 15);
        
        currentAngle += sliceAngle;
      });
    },
    
    drawYAxisTicks(ctx, padding, chartWidth, chartHeight, maxValue) {
      // 智能计算刻度间隔和数量
      let tickInterval = 1;
      let maxTicks = 5;
      
      // 根据最大值确定合适的刻度间隔
      if (maxValue <= 5) {
        tickInterval = 1;
      } else if (maxValue <= 10) {
        tickInterval = 2;
      } else if (maxValue <= 25) {
        tickInterval = 5;
      } else if (maxValue <= 50) {
        tickInterval = 10;
      } else if (maxValue <= 100) {
        tickInterval = 20;
      } else {
        // 对于更大的值，使用动态计算
        tickInterval = Math.ceil(maxValue / 5);
        // 将间隔调整为"好看"的数字（1, 2, 5, 10, 20, 50, 100...）
        const magnitude = Math.pow(10, Math.floor(Math.log10(tickInterval)));
        const normalized = tickInterval / magnitude;
        if (normalized <= 1) {
          tickInterval = magnitude;
        } else if (normalized <= 2) {
          tickInterval = 2 * magnitude;
        } else if (normalized <= 5) {
          tickInterval = 5 * magnitude;
        } else {
          tickInterval = 10 * magnitude;
        }
      }
      
      // 计算实际的最大刻度值（向上取整到刻度间隔的倍数）
      const maxTick = Math.ceil(maxValue / tickInterval) * tickInterval;
      
      // 绘制刻度
      for (let value = 0; value <= maxTick; value += tickInterval) {
        const y = padding + chartHeight - (value / maxTick) * chartHeight;
        
        // 绘制刻度文字
        ctx.fillStyle = '#6b7280';
        ctx.font = '11px Arial';
        ctx.textAlign = 'right';
        ctx.fillText(value.toString(), padding - 10, y + 3);
        
        // 绘制网格线（除了底线）
        if (value > 0) {
          ctx.strokeStyle = '#f3f4f6';
          ctx.lineWidth = 1;
          ctx.beginPath();
          ctx.moveTo(padding, y);
          ctx.lineTo(padding + chartWidth, y);
          ctx.stroke();
        }
      }
      
      // 更新maxValue用于柱子高度计算
      return maxTick;
    }
  }
}
</script>

<style scoped>
.chart-container {
  background: white;
  border-radius: 8px;
  padding: 1rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.chart-title {
  text-align: center;
  margin: 0 0 1rem 0;
  color: #374151;
  font-size: 1rem;
  font-weight: 600;
}

canvas {
  display: block;
  margin: 0 auto;
}
</style> 