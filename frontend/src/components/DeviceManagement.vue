<template>
  <div class="device-management">
    <div class="device-control">
      <h3>设备管理</h3>
      <el-table :data="devices" style="width: 100%">
        <el-table-column prop="deviceCode" label="设备编号"></el-table-column>
        <el-table-column prop="type" label="设备类型"></el-table-column>
        <el-table-column prop="status" label="状态">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="primary"
              @click="handleAllocate(scope.row)"
              :disabled="scope.row.status !== 'IDLE'">
              分配
            </el-button>
            <el-button
              size="mini"
              type="danger"
              @click="handleRelease(scope.row)"
              :disabled="scope.row.status === 'IDLE'">
              释放
            </el-button>
          </template>
        </el-table-column>
      </el-table>
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
export default {
  name: 'DeviceManagement',
  data() {
    return {
      devices: [
        {
          deviceCode: 'A1',
          type: 'PRINTER',
          status: 'IDLE',
          version: 1
        },
        {
          deviceCode: 'A2',
          type: 'SCANNER',
          status: 'IDLE',
          version: 1
        },
        {
          deviceCode: 'A3',
          type: 'DISK',
          status: 'IDLE',
          version: 1
        }
      ],
      interrupts: []
    }
  },
  methods: {
    getStatusType(status) {
      const types = {
        'IDLE': 'success',
        'BUSY': 'warning',
        'ERROR': 'danger'
      }
      return types[status] || 'info'
    },
    getInterruptType(type) {
      const types = {
        'IO': 'warning',
        'TIMER': 'primary',
        'ERROR': 'danger'
      }
      return types[type] || 'info'
    },
    handleAllocate(device) {
      device.status = 'BUSY'
      this.addInterrupt({
        type: 'IO',
        message: `设备 ${device.deviceCode} 已被分配`,
        time: new Date().toLocaleTimeString()
      })
    },
    handleRelease(device) {
      device.status = 'IDLE'
      this.addInterrupt({
        type: 'IO',
        message: `设备 ${device.deviceCode} 已被释放`,
        time: new Date().toLocaleTimeString()
      })
    },
    addInterrupt(interrupt) {
      this.interrupts.unshift(interrupt)
      if (this.interrupts.length > 10) {
        this.interrupts.pop()
      }
    }
  }
}
</script>

<style scoped>
.device-management {
  display: flex;
  gap: 20px;
  padding: 20px;
}

.device-control {
  flex: 1;
}

.interrupt-handling {
  flex: 1;
}

.interrupt-list {
  height: 400px;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 20px;
}
</style> 