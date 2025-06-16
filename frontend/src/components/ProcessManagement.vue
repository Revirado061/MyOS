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
          <el-input-number v-model="processForm.memorySize" :min="1" :max="1024" :step="1"></el-input-number>
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
      <div class="process-list-header">
        <h3>进程列表</h3>
        <div class="scheduling-algorithm">
          <span class="label">调度算法：</span>
          <el-select 
            v-model="schedulingAlgorithm" 
            @change="handleSchedulingAlgorithmChange" 
            size="small"
            placeholder="请选择调度算法"
          >
            <el-option 
              v-for="item in schedulingOptions" 
              :key="item.value" 
              :label="item.label" 
              :value="item.value"
            ></el-option>
          </el-select>
        </div>
      </div>
      <el-table 
        :data="allProcesses" 
        style="width: 100%" 
        border
        height="280"
      >
        <el-table-column 
          prop="id" 
          label="进程ID" 
          width="100" 
          align="center"
          sortable
        ></el-table-column>
        <el-table-column prop="name" label="进程名称" width="100" align="center"></el-table-column>
        <el-table-column 
          prop="priority" 
          label="优先级" 
          width="80" 
          align="center"
          :filters="[
            { text: '高', value: 1 },
            { text: '中', value: 2 },
            { text: '低', value: 3 }
          ]"
          :filter-method="filterPriority"
          filter-placement="bottom"
        >
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
        <el-table-column 
          prop="state" 
          label="状态" 
          width="120" 
          align="center"
          :filters="[
            { text: '就绪', value: 'READY' },
            { text: '运行中', value: 'RUNNING' },
            { text: '阻塞', value: 'BLOCKED' },
            { text: '已终止', value: 'TERMINATED' }
          ]"
          :filter-method="filterState"
          filter-placement="bottom"
        >
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
              style="margin-left: 10px;"
              size="mini"
              type="primary"
              @click="handleEnableDevice(scope.row)"
            >启用设备</el-button>
            <!-- :disabled="scope.row.state !== 'RUNNING'" -->
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
import { processApi, deviceApi, interruptApi } from '@/api/process_interrupt_device'

export default {
  name: 'ProcessManagement',
  data() {
    return {
      processForm: {
        name: '',
        priority: 2,
        memorySize: 64
      },
      schedulingAlgorithm: '',
      schedulingOptions: [
        { label: '先到先服务 (FCFS)', value: 'FCFS' },
        { label: '优先级调度 (PRIORITY)', value: 'PRIORITY' }
      ],
      readyQueue: [],
      runningQueue: [],
      blockedQueue: [],
      terminatedQueue: [],
      allProcesses: [],
      timer: null,
      selectedState: 'ALL',
      searchKeyword: '', // 添加搜索关键词

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
  computed: {
    filteredProcesses() {
      let result = this.allProcesses
      
      // 状态筛选
      if (this.selectedState !== 'ALL') {
        result = result.filter(process => process.state === this.selectedState)
      }
      
      // 关键词搜索
      if (this.searchKeyword) {
        const keyword = this.searchKeyword.toLowerCase()
        result = result.filter(process => 
          process.name.toLowerCase().includes(keyword)
        )
      }
      
      return result
    },
  },
  async created() {
    try {
      // 先获取当前调度算法
      const response = await processApi.getSchedulingAlgorithm()
      if (response.success) {
        this.schedulingAlgorithm = response.data.algorithm
      }
    } catch (error) {
      console.error('获取调度算法失败:', error)
      this.schedulingAlgorithm = 'FCFS' // 如果获取失败，设置默认值
    }
    
    // 获取进程数据
    this.fetchProcesses()
    // 设置定时器，每30秒更新一次数据
    this.timer = setInterval(this.fetchProcesses, 30000)
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

    // 进程操作相关方法
    async handleTerminate(process) {
      try {
        const response = await processApi.terminateProcess(process.id)
        if (response.success) {
          this.$message.success(response.message || `进程 ${process.name} 已终止`)
          // 更新进程列表
          await this.fetchProcesses()
          // 如果当前进程在运行队列中，清空运行队列
          if (this.runningQueue.some(p => p.id === process.id)) {
            this.runningQueue = []
          }
        } else {
          this.$message.error(response.message || '终止进程失败')
        }
      } catch (error) {
        console.error('终止进程错误:', error)
        this.$message.error('终止进程失败')
      }
    },

    // 获取特定状态的进程
    async getProcessesByState(state) {
      try {
        const response = await processApi.getProcessesByState(state)
        return response.data || []
      } catch (error) {
        console.error(`获取${state}状态进程失败:`, error)
        return []
      }
    },

    // 更新后的fetchProcesses方法
    async fetchProcesses() {
      try {
        // 获取所有进程
        const allResponse = await processApi.getAllProcesses()
        this.allProcesses = allResponse.data || []

        // 获取各状态进程
        this.readyQueue = await this.getProcessesByState('ready')
        // 使用 getCurrentProcess 获取运行中的进程
        const currentProcessResponse = await processApi.getCurrentProcess()
        this.runningQueue = currentProcessResponse.data ? [currentProcessResponse.data] : []
        this.blockedQueue = await this.getProcessesByState('waiting')
        this.terminatedQueue = await this.getProcessesByState('terminated')
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
        const response = await processApi.createProcess({
          name: this.processForm.name,
          priority: this.processForm.priority,
          memorySize: this.processForm.memorySize,
        })
        
        if (response.success) {
          this.$message.success('进程创建成功')
          this.processForm.name = ''
          // 立即刷新数据
          await this.fetchProcesses()
        } else {
          this.$message.error(response.message || '创建进程失败')
        }
      } catch (error) {
        this.$message.error('创建进程失败，请重试')
      }
    },
    
    // 设备管理相关方法
    async handleEnableDevice(row) {
      try {
        await this.fetchDevices()
        this.deviceDialogVisible = true
      } catch (error) {
        this.$message.error('获取设备列表失败')
      }
    },

    async fetchDevices() {
      try {
        // 这里需要添加获取设备列表的API调用
        // const response = await deviceApi.getDevices()
        // this.devices = response.data || []
        this.devices = [] // 临时使用空数组，等待设备API实现
      } catch (error) {
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
        const response = await deviceApi.requestDevice(
          this.selectedDevice.id,
          this.selectedDevice.type
        )
        
        if (response.success) {
          this.$message.success('设备分配成功')
          this.allocateDialogVisible = false
          // 刷新设备和进程数据
          await Promise.all([
            this.fetchDevices(),
            this.fetchProcesses()
          ])
        } else {
          this.$message.error(response.message || '设备分配失败')
        }
      } catch (error) {
        this.$message.error('设备分配失败')
      }
    },

    // 释放设备
    async handleRelease(device) {
      try {
        const response = await deviceApi.releaseDevice(
          device.id,
          device.type
        )
        
        if (response.success) {
          this.$message.success('设备释放成功')
          // 刷新设备和进程数据
          await Promise.all([
            this.fetchDevices(),
            this.fetchProcesses()
          ])
        } else {
          this.$message.error(response.message || '设备释放失败')
        }
      } catch (error) {
        this.$message.error('设备释放失败')
      }
    },

    handleSearch() {
      // 可以在这里添加额外的搜索逻辑
    },

    // 添加状态筛选方法
    filterState(value, row) {
      return row.state === value
    },

    // 添加优先级筛选方法
    filterPriority(value, row) {
      return row.priority === value
    },

    // 处理调度算法变更

    async handleSchedulingAlgorithmChange(newValue) {
      console.log('用户选择的调度算法:', newValue);

      const originalValue = this.schedulingAlgorithm; // 保留原始值以便回退

      try {
        const response = await processApi.setSchedulingAlgorithm(newValue);

        if (response?.success) {
          this.$message.success(response.message || '调度算法设置成功');
          this.schedulingAlgorithm = newValue; // 显式设置为新值（以防同步问题）
          
          // 刷新进程数据，确保新算法生效
          await this.fetchProcesses();
        } else {
          this.$message.error(response.message || '调度算法设置失败');
          this.schedulingAlgorithm = originalValue; // 回退
        }

      } catch (error) {
        console.error('设置调度算法失败:', error);
        this.$message.error('调度算法设置失败，请稍后再试');
        this.schedulingAlgorithm = originalValue; // 出错也回退
      }
    },
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

.process-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
  background-color: #f5f7fa;
}

.process-list-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
}

.scheduling-algorithm {
  display: flex;
  align-items: center;
  gap: 10px;
}

.scheduling-algorithm .label {
  color: #606266;
  font-size: 14px;
}

.scheduling-algorithm .el-select {
  width: 180px;
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