<template>
  <div class="device-management">
    <div class="device-control">
      <h3>设备管理</h3>
      <el-table :data="devices" style="width: 100%">
        <el-table-column prop="id" label="设备ID" align="center" header-align="center"></el-table-column>
        <el-table-column prop="name" label="设备名称" align="center" header-align="center"></el-table-column>
        <el-table-column prop="type" label="设备类型" align="center" header-align="center"></el-table-column>
        <el-table-column prop="status" label="状态" align="center" header-align="center">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentProcessId" label="当前进程" align="center" header-align="center">
          <template slot-scope="scope">
            {{ scope.row.currentProcessId || '无' }}
          </template>
        </el-table-column>
        <el-table-column prop="remainingTime" label="剩余时间" align="center" header-align="center">
          <template slot-scope="scope">
            {{ scope.row.remainingTime || 0 }}秒
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" header-align="center">
          <template slot-scope="scope">
            <div class="operation-buttons">
              <el-button
                size="mini"
                type="primary"
                @click="showAllocateDialog(scope.row)"
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
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分配设备对话框 -->
    <el-dialog
      title="分配设备"
      :visible.sync="allocateDialogVisible"
      width="30%"
      :close-on-click-modal="false">
      <el-form :model="allocateForm" label-width="100px">
        <el-form-item label="设备名称">
          <span>{{ selectedDevice ? selectedDevice.name : '' }}</span>
        </el-form-item>
        <el-form-item label="运行时间">
          <el-input-number 
            v-model="allocateForm.timeout" 
            :min="1" 
            :max="60"
            label="秒">
          </el-input-number>
          <span class="unit">秒</span>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="allocateDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleAllocate">确 定</el-button>
      </span>
    </el-dialog>

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
import { 
  getAllDevices, 
  getDeviceStatus, 
  allocateDevice, 
  releaseDevice } from '@/api/device'

export default {
  name: 'DeviceManagement',
  data() {
    return {
      devices: [],
      interrupts: [],
      timer: null,
      allocateDialogVisible: false,
      selectedDevice: null,
      allocateForm: {
        timeout: 5
      }
    }
  },
  created() {
    // 组件创建时获取设备列表
    this.fetchDevices()
    // 设置定时器，每秒更新一次设备状态
    this.timer = setInterval(this.fetchDevices, 1000)
  },
  beforeDestroy() {
    // 组件销毁前清除定时器
    if (this.timer) {
      clearInterval(this.timer)
    }
  },
  methods: {
    async fetchDevices() {
      try {
        const response = await getAllDevices()
        if (response && response.data) {  // 检查response.data是否存在
          this.devices = response.data    // 使用response.data作为设备列表
        } else {
          this.devices = []  // 如果没有数据，设置为空数组
        }
      } catch (error) {
        console.error('获取设备列表失败:', error)
        this.$message.error('获取设备列表失败')
        this.devices = []  // 发生错误时设置为空数组
      }
    },
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
        'ERROR': 'danger',
        'DEVICE': 'warning',
        'PROCESS': 'info',
        'CLOCK': 'primary'
      }
      return types[type] || 'info'
    },
    showAllocateDialog(device) {
      this.selectedDevice = device
      this.allocateForm.timeout = 5
      this.allocateDialogVisible = true
    },
    async handleAllocate() {
      if (!this.selectedDevice) return
      
      try {
        const data = {
          deviceId: this.selectedDevice.id,
          processId: 1, // 这里需要根据实际情况设置进程ID
          timeout: this.allocateForm.timeout
        }
        const response = await allocateDevice(data)
        if (response) {
          this.$message.success('设备分配成功')
          this.addInterrupt({
            type: 'DEVICE',
            message: `设备 ${this.selectedDevice.name} 已被分配给进程 ${data.processId}，运行时间 ${data.timeout} 秒`,
            time: new Date().toLocaleTimeString()
          })
          this.allocateDialogVisible = false
          await this.fetchDevices()
        }
      } catch (error) {
        console.error('设备分配失败:', error)
        this.$message.error('设备分配失败')
      }
    },
    async handleRelease(device) {
      try {
        const data = {
          deviceId: device.id,
          processId: device.currentProcessId
        }
        const response = await releaseDevice(data)
        if (response) {
          this.$message.success('设备释放成功')
          this.addInterrupt({
            type: 'DEVICE',
            message: `设备 ${device.name} 已被释放`,
            time: new Date().toLocaleTimeString()
          })
          // 更新设备列表
          await this.fetchDevices()
        }
      } catch (error) {
        console.error('设备释放失败:', error)
        this.$message.error('设备释放失败')
      }
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

.operation-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.unit {
  margin-left: 8px;
  color: #606266;
}
</style> 