<template>
  <div class="system-clock">
    <el-button type="primary" @click="toggleClock">
      {{ isRunning ? '暂停' : '启动' }}
    </el-button>
    <span class="clock-display">{{ currentTime }}</span>
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'

export default {
  name: 'SystemClock',
  computed: {
    ...mapGetters([
      'isClockRunning',
      'currentTime'
    ]),
    isRunning() {
      return this.isClockRunning
    }
  },
  methods: {
    ...mapActions([
      'startClock',
      'pauseClock'
    ]),
    toggleClock() {
      if (this.isRunning) {
        this.pauseClock()
      } else {
        this.startClock()
      }
    }
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
  gap: 10px;
  z-index: 1;
}

.clock-display {
  font-size: 18px;
  font-weight: bold;
  font-family: monospace;
}
</style> 