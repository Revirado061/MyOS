<template>
  <div class="memory-management">
    <h2>内存管理</h2>
    <div class="memory-info">
      <div>总内存: {{ totalMemory }}MB</div>
      <div>已用内存: {{ usedMemory }}MB</div>
      <div>空闲内存: {{ freeMemory }}MB</div>
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
  </div>
</template>

<script>
export default {
  name: 'MemoryManagement',
  data() {
    return {
      totalMemory: 1024, // 总内存大小（MB）
      usedMemory: 0,
      freeMemory: 1024,
      blockSize: 4, // 每个块的大小（MB）
      memoryRows: [],
      blocksPerRow: 16
    }
  },
  created() {
    this.initializeMemory()
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
    freeMemory(processId) {
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
    }
  }
}
</script>

<style scoped>
.memory-management {
  padding: 20px;
}

.memory-info {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.memory-blocks {
  border: 1px solid #dcdfe6;
  padding: 10px;
  background-color: #f5f7fa;
}

.memory-row {
  display: flex;
  gap: 2px;
  margin-bottom: 2px;
}

.memory-block {
  width: 30px;
  height: 30px;
  border: 1px solid #dcdfe6;
  cursor: pointer;
}

.memory-block.allocated {
  background-color: #409eff;
}

.memory-block.free {
  background-color: #f0f9eb;
}
</style> 