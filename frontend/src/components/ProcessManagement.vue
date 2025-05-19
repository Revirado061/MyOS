<template>
  <div class="process-management">
    <div class="process-control">
      <el-form :model="processForm" label-width="100px">
        <el-form-item label="进程名称">
          <el-input v-model="processForm.name"></el-input>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="processForm.priority">
            <el-option label="高" :value="1"></el-option>
            <el-option label="中" :value="2"></el-option>
            <el-option label="低" :value="3"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="内存大小(MB)">
          <el-input-number v-model="processForm.memorySize" :min="1" :max="1024"></el-input-number>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="createProcess">创建进程</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="process-queues">
      <div class="queue-container">
        <h3>就绪队列</h3>
        <div class="queue ready-queue">
          <el-tag v-for="process in readyQueue" :key="process.id" class="process-tag">
            {{ process.name }}
          </el-tag>
        </div>
      </div>

      <div class="queue-container">
        <h3>运行队列</h3>
        <div class="queue running-queue">
          <el-tag v-for="process in runningQueue" :key="process.id" type="success" class="process-tag">
            {{ process.name }}
          </el-tag>
        </div>
      </div>

      <div class="queue-container">
        <h3>阻塞队列</h3>
        <div class="queue blocked-queue">
          <el-tag v-for="process in blockedQueue" :key="process.id" type="warning" class="process-tag">
            {{ process.name }}
          </el-tag>
        </div>
      </div>

      <div class="queue-container">
        <h3>终止队列</h3>
        <div class="queue terminated-queue">
          <el-tag v-for="process in terminatedQueue" :key="process.id" type="info" class="process-tag">
            {{ process.name }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProcessManagement',
  data() {
    return {
      processForm: {
        name: '',
        priority: 2,
        memorySize: 256
      },
      readyQueue: [],
      runningQueue: [],
      blockedQueue: [],
      terminatedQueue: []
    }
  },
  methods: {
    createProcess() {
      const newProcess = {
        id: Date.now(),
        name: this.processForm.name,
        priority: this.processForm.priority,
        memorySize: this.processForm.memorySize,
        state: 'NEW'
      }
      this.readyQueue.push(newProcess)
      this.processForm.name = ''
    }
  }
}
</script>

<style scoped>
.process-management {
  display: flex;
  gap: 20px;
  padding: 20px;
}

.process-control {
  width: 300px;
}

.process-queues {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.queue-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
}

.queue {
  min-height: 100px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.process-tag {
  margin: 5px;
}
</style> 