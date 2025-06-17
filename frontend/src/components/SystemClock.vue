<template>
  <div class="system-clock">
    <span class="clock-display">{{ formatTime(currentTime) }}  / {{ currentTime }}</span>
  </div>
</template>

<script>
import { getCurrentTime } from '@/api/clock'

export default {
  name: 'SystemClock',
  data() {
    return {
      currentTime: 0,
      timer: null
    }
  },
  methods: {
    formatTime(seconds) {
      const hours = Math.floor(seconds / 3600)
      const minutes = Math.floor((seconds % 3600) / 60)
      const secs = seconds % 60
      return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
    },
    async updateTime() {
      try {
        const response = await getCurrentTime()
        this.currentTime = response.currentTime
      } catch (error) {
        console.error('获取时间失败:', error)
      }
    },
    startTimer() {
      this.updateTime()
      this.timer = setInterval(this.updateTime, 1000)
    },
    clearTimer() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    }
  },
  created() {
    this.startTimer()
  },
  beforeDestroy() {
    this.clearTimer()
  }
}
</script>

<style scoped>
.system-clock {
  position: absolute;
  top: 10px;
  right: 20px;
  display: flex;
  align-items: center;
  z-index: 1;
}

.clock-display {
  font-size: 18px;
  font-weight: bold;
  font-family: monospace;
}
</style> 