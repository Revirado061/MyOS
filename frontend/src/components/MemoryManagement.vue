<template>
  <div class="memory-management">
    <!-- <h2 style="margin: 5px 0;">内存管理</h2> -->
    <div class="memory-info">
      <div class="info-item">总内存: {{ totalMemory }}MB</div>
      <div class="info-item">已用内存: {{ usedMemory }}MB</div>
      <div class="info-item">空闲内存: {{ freeMemory }}MB</div>
      <div class="memory-usage">
        <div class="usage-label">内存利用率: {{ memoryUsagePercentage }}%</div>
        <el-progress 
          :percentage="memoryUsagePercentage"
          :color="memoryUsageColor"
          :stroke-width="15"
          :show-text="false"
        ></el-progress>
      </div>
    </div>
    
    <div class="memory-blocks">
      <div v-for="(row, rowIndex) in memoryRows" :key="rowIndex" class="memory-row">
        <div
          v-for="(block, blockIndex) in row"
          :key="blockIndex"
          class="memory-block"
          :class="{
            'allocated': block.status === 'ALLOCATED',
            'free': block.status === 'FREE'
          }"
          :title="getBlockTooltip(block)"
        ></div>
      </div>
    </div>

    <div class="interrupt-handling">
      <h3>中断处理</h3>
      <div class="interrupt-list">
        <el-timeline>
          <el-timeline-item
            v-for="(interrupt, index) in interrupts"
            :key="index"
            :type="getInterruptType(interrupt.type)"
            :timestamp="interrupt.time">
            {{ interrupt.message }}
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>
  </div>
</template>

<script>
import { getFreeMemoryBlocks, getFreeMemorySize, getMemoryStatus, getMemoryUsage } from '../api/memory'

export default {
  name: 'MemoryManagement',
  data() {
    return {
      
      // 内存部分
      totalMemory: 1024, // 总内存大小（MB）
      usedMemory: 0,
      freeMemory: 1024,
      blockSize: 4, // 每个块的大小（MB）
      memoryRows: [],
      blocksPerRow: 16,

      // 中断部分
      updateInterval: null,
      interrupts: [], // 添加中断数组
    }
  },
  computed: {
    memoryUsagePercentage() {
      return Math.round((this.usedMemory / this.totalMemory) * 100)
    },
    memoryUsageColor() {
      const percentage = this.memoryUsagePercentage
      if (percentage < 60) return '#67C23A'
      if (percentage < 80) return '#E6A23C'
      return '#F56C6C'
    }
  },
  created() {
    this.initializeMemory()
    this.fetchMemoryData()
    // 每1秒更新一次内存数据
    this.updateInterval = setInterval(this.fetchMemoryData, 1000)
  },
  beforeDestroy() {
    if (this.updateInterval) {
      clearInterval(this.updateInterval)
    }
  },
  methods: {

    initializeMemory() {
      const totalBlocks = Math.ceil(this.totalMemory / this.blockSize)
      const rows = Math.ceil(totalBlocks / this.blocksPerRow)
      
      this.memoryRows = Array(rows).fill().map(() => 
        Array(this.blocksPerRow).fill().map(() => ({
          status: 'FREE',
          processId: null,
          size: this.blockSize
        }))
      )
    },
    getBlockTooltip(block) {
      if (block.status === 'ALLOCATED') {
        return `进程ID: ${block.processId}\n大小: ${block.size}MB`
      }
      return '空闲块'
    },
    allocateMemory(processId, size) {
      const blocksNeeded = Math.ceil(size / this.blockSize)
      let allocatedBlocks = 0
      
      for (let row of this.memoryRows) {
        for (let block of row) {
          if (block.status === 'FREE') {
            block.status = 'ALLOCATED'
            block.processId = processId
            allocatedBlocks++
            
            if (allocatedBlocks === blocksNeeded) {
              this.usedMemory += size
              this.freeMemory -= size
              return true
            }
          }
        }
      }
      return false
    },
    releaseMemory(processId) {
      let freedSize = 0
      
      for (let row of this.memoryRows) {
        for (let block of row) {
          if (block.status === 'ALLOCATED' && block.processId === processId) {
            block.status = 'FREE'
            block.processId = null
            freedSize += block.size
          }
        }
      }
      
      this.usedMemory -= freedSize
      this.freeMemory += freedSize
    },
    async fetchMemoryData() {
      try {
        // 获取空闲内存大小
        const freeSize = await getFreeMemorySize()
        this.freeMemory = freeSize
        this.usedMemory = this.totalMemory - freeSize

        // 获取内存状态
        const memoryStatus = await getMemoryStatus()
        this.updateMemoryBlocks(memoryStatus)

        // 获取空闲内存块列表
        const freeBlocks = await getFreeMemoryBlocks()

        // 获取内存使用率
        const usage = await getMemoryUsage()
      } catch (error) {
        this.$message.error('获取内存数据失败')
      }
    },
    updateMemoryBlocks(memoryStatus) {
      const totalBlocks = Math.ceil(this.totalMemory / this.blockSize)
      const rows = Math.ceil(totalBlocks / this.blocksPerRow)
      
      this.memoryRows = Array(rows).fill().map((_, rowIndex) => 
        Array(this.blocksPerRow).fill().map((_, blockIndex) => {
          const index = rowIndex * this.blocksPerRow + blockIndex
          const processId = index < memoryStatus.length ? memoryStatus[index] : 0
          return {
            status: processId === 0 ? 'FREE' : 'ALLOCATED',
            processId: processId === 0 ? null : processId,
            size: this.blockSize
          }
        })
      )
    },
    getInterruptType(type) {
      const types = {
        'MEMORY_ALLOCATION': 'success',
        'MEMORY_DEALLOCATION': 'warning',
        'MEMORY_ERROR': 'danger',
        'default': 'info'
      }
      return types[type] || types.default
    },
    addInterrupt(type, message) {
      const now = new Date()
      const time = `${now.getHours()}:${now.getMinutes()}:${now.getSeconds()}`
      this.interrupts.unshift({
        type,
        message,
        time
      })
    }
  }
}
</script>

<style scoped>
.memory-management {
  padding: 10px;
}

.memory-info {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 10px;
  flex-wrap: nowrap;
}

.info-item {
  white-space: nowrap;
}

.memory-blocks {
  border: 1px solid #dcdfe6;
  padding: 8px;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.memory-row {
  display: flex;
  gap: 1px;
  margin-bottom: 1px;
  justify-content: center;
}

.memory-block {
  width: 20px;
  height: 20px;
  border: 1px solid #dcdfe6;
  cursor: pointer;
}

.memory-block.allocated {
  background-color: #409eff;
}

.memory-block.free {
  background-color: #f0f9eb;
}

.memory-usage {
  flex: 1;
  min-width: 200px;
  margin-left: 20px;
}

.usage-label {
  margin-bottom: 5px;
  font-weight: bold;
  white-space: nowrap;
}

.interrupt-handling {
  margin-top: 20px;
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  height: 240px;
  display: flex;
  flex-direction: column;
}

.interrupt-handling h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  flex-shrink: 0;
}

.interrupt-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 10px;
}

.interrupt-list::-webkit-scrollbar {
  width: 6px;
}

.interrupt-list::-webkit-scrollbar-thumb {
  background-color: #909399;
  border-radius: 3px;
}

.interrupt-list::-webkit-scrollbar-track {
  background-color: #f5f7fa;
}
</style> 