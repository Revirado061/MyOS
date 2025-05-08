<template>
  <div class="process">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>进程管理</span>
          <el-button type="primary" @click="showCreateDialog">创建进程</el-button>
        </div>
      </template>

      <el-table :data="processes" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="进程名" width="120" />
        <el-table-column prop="priority" label="优先级" width="100" />
        <el-table-column prop="state" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStateType(row.state)">{{ row.state }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="memorySize" label="内存大小(MB)" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="lastUpdateTime" label="最后更新时间" width="180" />
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button-group>
              <el-button 
                type="primary" 
                size="small" 
                @click="updateProcessState(row)"
              >
                更新状态
              </el-button>
              <el-button 
                type="danger" 
                size="small" 
                @click="deleteProcess(row)"
              >
                删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
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
import { ref, onMounted } from 'vue'
import { osApi, type Process } from '../api/os'
import { ElMessage, ElMessageBox } from 'element-plus'

const processes = ref<Process[]>([])
const dialogVisible = ref(false)
const newProcess = ref<Process>({
  name: '',
  priority: 1,
  state: 'READY',
  memorySize: 1
})

const fetchProcesses = async () => {
  try {
    const response = await osApi.getAllProcesses()
    processes.value = response.data
  } catch (error) {
    console.error('Error fetching processes:', error)
    ElMessage.error('获取进程列表失败')
  }
}

const showCreateDialog = () => {
  newProcess.value = {
    name: '',
    priority: 1,
    state: 'READY',
    memorySize: 1
  }
  dialogVisible.value = true
}

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

const getStateType = (state: string) => {
  switch (state) {
    case 'RUNNING':
      return 'success'
    case 'READY':
      return 'warning'
    case 'BLOCKED':
      return 'danger'
    default:
      return 'info'
  }
}

const deleteProcess = async (process: Process) => {
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
    await osApi.deleteProcess(process.id!)
    ElMessage.success('删除进程成功')
    fetchProcesses()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Error deleting process:', error)
      ElMessage.error('删除进程失败')
    }
  }
}

const updateProcessState = async (process: Process) => {
  try {
    const states = ['READY', 'RUNNING', 'BLOCKED']
    const currentIndex = states.indexOf(process.state)
    const nextState = states[(currentIndex + 1) % states.length]
    
    await osApi.updateProcessState(process.id!, nextState)
    ElMessage.success('更新进程状态成功')
    fetchProcesses()
  } catch (error) {
    console.error('Error updating process state:', error)
    ElMessage.error('更新进程状态失败')
  }
}

onMounted(() => {
  fetchProcesses()
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style> 