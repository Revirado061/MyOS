<template>
  <div class="process">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>进程管理</span>
          <div>
            <el-button type="primary" @click="showCreateDialog">创建进程</el-button>
            <el-button type="success" @click="scheduleNextProcess">调度进程</el-button>
          </div>
        </div>
      </template>

      <!-- 当前运行进程 -->
      <el-card v-if="currentProcess" class="current-process-card">
        <template #header>
          <div class="card-subheader">
            <span>当前运行进程</span>
          </div>
        </template>
        <div class="current-process">
          <div>
            <h3>{{ currentProcess.name }}</h3>
            <p>ID: {{ currentProcess.id }}</p>
            <p>优先级: {{ currentProcess.priority }}</p>
            <p>内存大小: {{ currentProcess.memorySize }}MB</p>
            <p>
              内存状态: 
              <el-tag :type="currentProcess.inMemory ? 'success' : 'danger'">
                {{ currentProcess.inMemory ? '在内存中' : '已交换出' }}
              </el-tag>
            </p>
          </div>
          <div class="process-actions">
            <el-button-group>
              <el-button type="warning" @click="blockProcess(currentProcess)">阻塞</el-button>
              <el-button type="danger" @click="terminateProcess(currentProcess)">终止</el-button>
            </el-button-group>
          </div>
        </div>
      </el-card>

      <!-- 进程状态标签页 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部进程" name="all">
          <process-table 
            :processes="allProcesses" 
            @delete="deleteProcess" 
            @update-state="updateProcessState"
            @swap-in="swapInProcess"
            @swap-out="swapOutProcess"
          />
        </el-tab-pane>
        <el-tab-pane label="就绪进程" name="ready">
          <process-table 
            :processes="readyProcesses" 
            @delete="deleteProcess" 
            @update-state="updateProcessState"
            @swap-in="swapInProcess"
            @swap-out="swapOutProcess"
          />
        </el-tab-pane>
        <el-tab-pane label="等待进程" name="waiting">
          <process-table 
            :processes="waitingProcesses" 
            @delete="deleteProcess" 
            @update-state="updateProcessState"
            @swap-in="swapInProcess"
            @swap-out="swapOutProcess"
          />
        </el-tab-pane>
        <el-tab-pane label="已终止进程" name="terminated">
          <process-table 
            :processes="terminatedProcesses" 
            @delete="deleteProcess" 
            @update-state="updateProcessState"
            @swap-in="swapInProcess"
            @swap-out="swapOutProcess"
          />
        </el-tab-pane>
        <el-tab-pane label="已交换进程" name="swapped">
          <process-table 
            :processes="swappedProcesses" 
            @delete="deleteProcess" 
            @update-state="updateProcessState"
            @swap-in="swapInProcess"
            @swap-out="swapOutProcess"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="dialogVisible" title="创建进程" width="500px">
      <el-form :model="newProcess" label-width="100px">
        <el-form-item label="进程名">
          <el-input v-model="newProcess.name" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="newProcess.priority" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="内存大小(MB)">
          <el-input-number v-model="newProcess.memorySize" :min="1" :max="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="createProcess">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { osApi, type Process, ProcessState, ProcessStateNames, ProcessStateTagTypes } from '../api/os'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProcessTable from '../components/ProcessTable.vue'

const allProcesses = ref<Process[]>([])
const readyProcesses = ref<Process[]>([])
const waitingProcesses = ref<Process[]>([])
const terminatedProcesses = ref<Process[]>([])
const swappedProcesses = ref<Process[]>([])
const currentProcess = ref<Process | null>(null)
const dialogVisible = ref(false)
const activeTab = ref('all')

// 用于创建新进程的表单数据
const newProcess = ref<Process>({
  name: '',
  priority: 1,
  state: ProcessState.NEW,
  memorySize: 64 // 默认64MB内存
})

// 获取不同状态的进程列表
const fetchProcesses = async () => {
  try {
    // 获取所有进程
    const response = await osApi.getAllProcesses()
    allProcesses.value = response.data
    
    // 获取就绪进程
    const readyResponse = await osApi.getReadyProcesses()
    readyProcesses.value = readyResponse.data
    
    // 获取等待进程
    const waitingResponse = await osApi.getWaitingProcesses()
    waitingProcesses.value = waitingResponse.data
    
    // 获取已终止进程
    const terminatedResponse = await osApi.getTerminatedProcesses()
    terminatedProcesses.value = terminatedResponse.data
    
    // 获取已交换进程
    const swappedResponse = await osApi.getSwappedProcesses()
    swappedProcesses.value = swappedResponse.data
    
    // 获取当前运行进程
    const currentResponse = await osApi.getCurrentProcess()
    currentProcess.value = currentResponse.data
  } catch (error) {
    console.error('Error fetching processes:', error)
    ElMessage.error('获取进程列表失败')
  }
}

// 显示创建进程对话框
const showCreateDialog = () => {
  newProcess.value = {
    name: `进程${Math.floor(Math.random() * 1000)}`,
    priority: Math.floor(Math.random() * 10) + 1,
    state: ProcessState.NEW,
    memorySize: Math.floor(Math.random() * 100) + 50 // 随机50-150MB内存
  }
  dialogVisible.value = true
}

// 创建新进程
const createProcess = async () => {
  try {
    await osApi.createProcess(newProcess.value)
    ElMessage.success('创建进程成功')
    dialogVisible.value = false
    fetchProcesses()
  } catch (error) {
    console.error('Error creating process:', error)
    ElMessage.error('创建进程失败')
  }
}

// 删除进程
const deleteProcess = async (process: Process) => {
  if (!process.id) return
  
  try {
    await ElMessageBox.confirm(
      `确定要删除进程 ${process.name} 吗？`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await osApi.deleteProcess(process.id)
    ElMessage.success('删除进程成功')
    fetchProcesses()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Error deleting process:', error)
      ElMessage.error('删除进程失败')
    }
  }
}

// 更新进程状态
const updateProcessState = async (process: Process, newState: ProcessState) => {
  if (!process.id) return
  
  try {
    await osApi.updateProcessState(process.id, newState)
    ElMessage.success(`已将进程 ${process.name} 状态更新为 ${ProcessStateNames[newState]}`)
    fetchProcesses()
  } catch (error) {
    console.error('Error updating process state:', error)
    ElMessage.error('更新进程状态失败')
  }
}

// 阻塞进程
const blockProcess = async (process: Process) => {
  if (!process.id) return
  
  try {
    await osApi.blockProcess(process.id)
    ElMessage.success(`已阻塞进程 ${process.name}`)
    fetchProcesses()
  } catch (error) {
    console.error('Error blocking process:', error)
    ElMessage.error('阻塞进程失败')
  }
}

// 唤醒进程
const wakeUpProcess = async (process: Process) => {
  if (!process.id) return
  
  try {
    await osApi.wakeUpProcess(process.id)
    ElMessage.success(`已唤醒进程 ${process.name}`)
    fetchProcesses()
  } catch (error) {
    console.error('Error waking up process:', error)
    ElMessage.error('唤醒进程失败')
  }
}

// 终止进程
const terminateProcess = async (process: Process) => {
  if (!process.id) return
  
  try {
    await osApi.terminateProcess(process.id)
    ElMessage.success(`已终止进程 ${process.name}`)
    fetchProcesses()
  } catch (error) {
    console.error('Error terminating process:', error)
    ElMessage.error('终止进程失败')
  }
}

// 调度下一个进程
const scheduleNextProcess = async () => {
  try {
    await osApi.scheduleNextProcess()
    ElMessage.success('进程调度完成')
    fetchProcesses()
  } catch (error) {
    console.error('Error scheduling process:', error)
    ElMessage.error('进程调度失败')
  }
}

// 将进程交换到磁盘
const swapOutProcess = async (process: Process) => {
  if (!process.id) return
  
  try {
    await osApi.swapOutProcess(process.id)
    ElMessage.success(`已将进程 ${process.name} 交换到磁盘`)
    fetchProcesses()
  } catch (error) {
    console.error('Error swapping out process:', error)
    ElMessage.error('交换进程到磁盘失败')
  }
}

// 将进程从磁盘加载回内存
const swapInProcess = async (process: Process) => {
  if (!process.id) return
  
  try {
    await osApi.swapInProcess(process.id)
    ElMessage.success(`已将进程 ${process.name} 加载到内存`)
    fetchProcesses()
  } catch (error) {
    console.error('Error swapping in process:', error)
    ElMessage.error('加载进程到内存失败')
  }
}

// 切换标签页时刷新数据
const handleTabChange = (tab: string) => {
  fetchProcesses()
}

onMounted(() => {
  fetchProcesses()
  
  // 设置定时刷新（每5秒刷新一次）
  setInterval(fetchProcesses, 5000)
})
</script>

<style scoped>
.process {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-subheader {
  font-weight: bold;
  color: #409EFF;
}

.current-process-card {
  margin-bottom: 20px;
}

.current-process {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.process-actions {
  display: flex;
  gap: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style> 