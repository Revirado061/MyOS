<template>
  <div class="process-management">
    <div class="process-control">
      <el-form class="input-form" :model="processForm" label-width="80px" inline>
        <el-form-item label="进程名称">
          <el-input v-model="processForm.name" placeholder="请输入进程名称"></el-input>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="processForm.priority" placeholder="请选择">
            <el-option label="高" :value="1"></el-option>
            <el-option label="中" :value="2"></el-option>
            <el-option label="低" :value="3"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="内存大小">
          <el-input-number v-model="processForm.memorySize" :min="1" :max="1024" :step="64"></el-input-number>
          <span class="unit">MB</span>
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
            <div class="process-info">
              <span>{{ process.name }}</span>
              <el-tag size="small" type="info">P{{ process.priority }}</el-tag>
              <el-tag size="small" type="info">{{ process.memorySize }}MB</el-tag>
            </div>
          </el-tag>
        </div>
      </div>

      <div class="queue-container">
        <h3>运行队列</h3>
        <div class="queue running-queue">
          <el-tag v-for="process in runningQueue" :key="process.id" type="success" class="process-tag">
            <div class="process-info">
              <span>{{ process.name }}</span>
              <el-tag size="small" type="info">P{{ process.priority }}</el-tag>
              <el-tag size="small" type="info">{{ process.memorySize }}MB</el-tag>
            </div>
          </el-tag>
        </div>
      </div>

      <div class="queue-container">
        <h3>阻塞队列</h3>
        <div class="queue blocked-queue">
          <el-tag v-for="process in blockedQueue" :key="process.id" type="warning" class="process-tag">
            <div class="process-info">
              <span>{{ process.name }}</span>
              <el-tag size="small" type="info">P{{ process.priority }}</el-tag>
              <el-tag size="small" type="info">{{ process.memorySize }}MB</el-tag>
            </div>
          </el-tag>
        </div>
      </div>

      <div class="queue-container">
        <h3>终止队列</h3>
        <div class="queue terminated-queue">
          <el-tag v-for="process in terminatedQueue" :key="process.id" type="info" class="process-tag">
            <div class="process-info">
              <span>{{ process.name }}</span>
              <el-tag size="small" type="info">P{{ process.priority }}</el-tag>
              <el-tag size="small" type="info">{{ process.memorySize }}MB</el-tag>
            </div>
          </el-tag>
        </div>
      </div>
    </div>

    <div class="process-list">
      <h3>进程列表</h3>
      <el-table 
        :data="allProcesses" 
        style="width: 100%" 
        border
        height="280"
      >
        <el-table-column prop="id" label="进程ID" width="100" align="center"></el-table-column>
        <el-table-column prop="name" label="进程名称" width="100" align="center"></el-table-column>
        <el-table-column prop="priority" label="优先级" width="80" align="center">
          <template slot-scope="scope">
            <el-tag :type="getPriorityType(scope.row.priority)">
              P{{ scope.row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="memorySize" label="内存大小" width="120" align="center">
          <template slot-scope="scope">
            {{ scope.row.memorySize }}MB
          </template>
        </el-table-column>
        <el-table-column prop="state" label="状态" width="120" align="center">
          <template slot-scope="scope">
            <el-tag :type="getStateType(scope.row.state)">
              {{ getStateText(scope.row.state) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center">
          <template slot-scope="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="200">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="primary"
              @click="handleEnableDevice(scope.row)"
              :disabled="scope.row.state === 'TERMINATED'"
              style="margin-left: 10px;"
            >启用设备</el-button> <!-- 添加启用设备按钮 -->
            <el-button
              size="mini"
              type="danger"
              @click="handleTerminate(scope.row)"
              :disabled="scope.row.state === 'TERMINATED'"
            >终止</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 进程相关内容 -->
    <el-dialog
      title="设备管理"
      :visible.sync="deviceDialogVisible"
      width="80%"
      :close-on-click-modal="false">
      <div class="device-control">
        <el-table :data="devices" style="width: 100%">
          <el-table-column prop="id" label="设备ID" align="center" header-align="center" />
          <el-table-column prop="name" label="设备名称" align="center" header-align="center" />
          <el-table-column prop="type" label="设备类型" align="center" header-align="center" />
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
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

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
          <span class="unit" style="margin-left: 10px;">秒</span>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="allocateDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleAllocate">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { createProcess, getAllProcesses, getCurrentProcess, getProcessesByState } from '../api/process'
import { getAllDevices, allocateDevice, releaseDevice } from '../api/device'
import axios from 'axios'

export default {
  name: 'ProcessManagement',
  data() {
    return {
      processForm: {
        name: '',
        priority: 2,
        memorySize: 64
      },
      readyQueue: [],
      runningQueue: [],
      blockedQueue: [],
      terminatedQueue: [],
      allProcesses: [],
      timer: null,

      // 设备管理相关
      devices: [],                 // 设备列表
      deviceDialogVisible: false, // 控制设备管理弹窗显示
      allocateDialogVisible: false,
      selectedDevice: null,        // 当前选中用于分配的设备
      allocateForm: {
        timeout: 5
      },
    }
  },
  created() {
    // 组件创建时立即获取一次数据
    this.fetchProcesses()
    // 设置定时器，每1秒更新一次数据
    this.timer = setInterval(this.fetchProcesses, 1000)
  },
  beforeDestroy() {
    // 组件销毁前清除定时器
    if (this.timer) {
      clearInterval(this.timer)
    }
  },
  methods: {
    // 处理前端显示细节
    getPriorityType(priority) {
      const types = {
        1: 'danger',
        2: 'warning',
        3: 'info'
      }
      return types[priority] || 'info'
    },
    getStateType(state) {
      const types = {
        'READY': 'info',
        'RUNNING': 'success',
        'BLOCKED': 'warning',
        'TERMINATED': 'info'
      }
      return types[state] || 'info'
    },
    getStateText(state) {
      const texts = {
        'READY': '就绪',
        'RUNNING': '运行中',
        'BLOCKED': '阻塞',
        'TERMINATED': '已终止'
      }
      return texts[state] || state
    },
    formatTime(timestamp) {
      if (!timestamp) return '-'
      const date = new Date(timestamp)
      return date.toLocaleString()
    },
    // 前后端连接-进程
    async handleTerminate(process) {
      try {
        // 这里需要调用后端API来终止进程
        // const response = await terminateProcess(process.id)
        this.$message.success(`进程 ${process.name} 已终止`)
        await this.fetchProcesses()
      } catch (error) {
        // console.error('终止进程失败:', error)
        this.$message.error('终止进程失败')
      }
    },
    // 获取特定状态的进程
    async getProcessesByState(state) {
      try {
        const response = await getProcessesByState(state)
        if (response && response.success) {
          return response.data
        }
        return []
      } catch (error) {
        console.error(`获取${state}状态进程失败:`, error)
        return []
      }
    },
    // 更新后的fetchProcesses方法
    async fetchProcesses() {
      try {
        // 并行获取所有状态的进程
        const [readyProcesses, runningProcesses, blockedProcesses, terminatedProcesses] = await Promise.all([
          this.getProcessesByState('READY'),
          this.getProcessesByState('RUNNING'),
          this.getProcessesByState('WAITING'),
          this.getProcessesByState('TERMINATED')
        ])

        // 更新各个队列
        this.readyQueue = readyProcesses
        this.runningQueue = runningProcesses
        this.blockedQueue = blockedProcesses
        this.terminatedQueue = terminatedProcesses

        // 合并所有进程用于表格显示
        this.allProcesses = [
          ...readyProcesses,
          ...runningProcesses,
          ...blockedProcesses,
          ...terminatedProcesses
        ]
      } catch (error) {
        console.error('获取进程数据失败:', error)
        this.$message.error('获取进程数据失败')
        // 清空所有队列
        this.readyQueue = []
        this.runningQueue = []
        this.blockedQueue = []
        this.terminatedQueue = []
        this.allProcesses = []
      }
    },
    async createProcess() {
      try {
        const newProcess = {
          name: this.processForm.name,
          priority: this.processForm.priority,
          memorySize: this.processForm.memorySize, //* 1024
        }
        console.log('创建进程数据:', newProcess)
        
        // 调用后端 API 创建进程
        const response = await createProcess(newProcess)
        console.log('创建进程响应:', response)
        
        // 如果创建成功，将进程添加到就绪队列
        if (response) {
          this.readyQueue.push(response)
          this.processForm.name = ''
          this.$message.success('进程创建成功')
        }
      } catch (error) {
        // console.error('创建进程失败:', error)
        this.$message.error('创建进程失败，请重试')
      }
    },
    
    // 启动设备调用
    async handleEnableDevice(row) {
      // 你也可以根据row.id传入进程ID来筛选相关设备
      await this.fetchDevices()
      this.deviceDialogVisible = true
    },
    // 获取设备列表
    async fetchDevices() {
      try {
        const response = await getAllDevices()
        if (response && response.data) {
          this.devices = response.data
        } else {
          this.devices = []
        }
      } catch (error) {
        // console.error('获取设备列表失败:', error)
        this.$message.error('获取设备列表失败')
        this.devices = []
      }
    },

    // 获取状态标签类型
    getStatusType(status) {
      const types = {
        'IDLE': 'success',
        'BUSY': 'warning',
        'ERROR': 'danger'
      }
      return types[status] || 'info'
    },

    // 获取中断类型标签
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

    // 弹出设备分配对话框
    showAllocateDialog(device) {
      this.selectedDevice = device
      this.allocateForm.timeout = 5
      this.allocateDialogVisible = true
    },

    // 确认分配设备
    async handleAllocate() {
      if (!this.selectedDevice) return

      try {
        const data = {
          deviceId: this.selectedDevice.id,
          processId: 1, // TODO: 这里请改为当前选中进程的ID
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
        // console.error('设备分配失败:', error)
        this.$message.error('设备分配失败')
      }
    },

    // 释放设备
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
          await this.fetchDevices()
        }
      } catch (error) {
        // console.error('设备释放失败:', error)
        this.$message.error('设备释放失败')
      }
    },

    // 添加中断日志（最多保留10条）
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
.process-management {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 900px;
  padding: 20px;
  gap: 20px;
}

.process-control {
  background-color: #fff;
  padding: 20px 0 20px 20px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.process-control .el-form {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-right: 10px;
}

.process-control .el-form-item {
  margin-bottom: 0;
  margin-right: 0;
}

.process-control .el-form-item__label {
  padding-right: 8px;
}

.process-control .el-input {
  width: 150px;
}

.process-control .el-select {
  width: 120px;
}

.process-control .el-input-number {
  width: 150px;
}

.process-control .unit {
  margin-left: 5px;
  color: #606266;
}

.process-control .el-button {
  margin-left: 10px;
}

.process-queues {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  width: 100%;
  margin-bottom: 20px;
}

.queue-container {
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  display: flex;
  flex-direction: column;
  height: 240px;
}

.queue-container h3 {
  margin: 0;
  padding: 10px 15px;
  border-bottom: 1px solid #ebeef5;
  background-color: #f5f7fa;
  border-radius: 4px 4px 0 0;
  font-size: 14px;
}

.queue {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.queue::-webkit-scrollbar {
  width: 6px;
}

.queue::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 3px;
}

.queue::-webkit-scrollbar-track {
  background-color: #f5f7fa;
}

.process-tag {
  margin: 0;
  padding: 6px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.process-tag .process-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.process-tag .el-tag {
  margin: 0;
}

.process-tag .el-tag--small {
  height: 20px;
  line-height: 18px;
  padding: 0 6px;
  font-size: 12px;
}

.process-list {
  margin-bottom: 10px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  height: 350px;
  overflow: hidden;
}

.process-list h3 {
  margin: 0;
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
  color: #303133;
  font-size: 16px;
}
.process-list el-table {
  margin-bottom: 10px;
}
/* 自定义表格滚动条样式 */
.process-list .el-table__body-wrapper::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.process-list .el-table__body-wrapper::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 3px;
}

.process-list .el-table__body-wrapper::-webkit-scrollbar-track {
  background-color: #f5f7fa;
}
</style> 