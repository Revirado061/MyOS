<template>
  <div class="process-container">
    <h2>进程管理</h2>
    
    <!-- 进程操作按钮 -->
    <div class="operation-buttons">
      <el-button type="primary" @click="createProcess">创建进程</el-button>
      <el-button type="success" @click="scheduleProcess">进程调度</el-button>
      <el-button type="warning" @click="blockProcess">进程阻塞</el-button>
      <el-button type="info" @click="wakeupProcess">进程唤醒</el-button>
      <el-button type="danger" @click="synchronizeProcess">进程同步</el-button>
    </div>

    <!-- 进程列表 -->
    <el-table :data="processList" style="width: 100%; margin-top: 20px">
      <el-table-column prop="id" label="进程ID" width="100"></el-table-column>
      <el-table-column prop="name" label="进程名称" width="150"></el-table-column>
      <el-table-column prop="state" label="状态" width="120"></el-table-column>
      <el-table-column prop="priority" label="优先级" width="100"></el-table-column>
      <el-table-column prop="memorySize" label="内存占用" width="120"></el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180"></el-table-column>
      <el-table-column label="操作">
        <template slot-scope="scope">
          <el-button size="mini" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建进程对话框 -->
    <el-dialog title="创建新进程" :visible.sync="dialogVisible" width="30%">
      <el-form :model="processForm" label-width="100px">
        <el-form-item label="进程名称">
          <el-input v-model="processForm.name"></el-input>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="processForm.priority" placeholder="请选择优先级">
            <el-option label="高" :value="1"></el-option>
            <el-option label="中" :value="2"></el-option>
            <el-option label="低" :value="3"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="内存需求">
          <el-input-number v-model="processForm.memorySize" :min="1" :max="1000"></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitProcess">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { createProcess, getAllProcesses, getCurrentProcess } from '@/api/process'

export default {
  name: 'Process',
  data() {
    return {
      processList: [],
      currentProcess: null,
      dialogVisible: false,
      processForm: {
        name: '',
        priority: 1,
        memorySize: 256
      }
    }
  },
  created() {
    this.fetchProcesses()
    this.fetchCurrentProcess()
  },
  methods: {
    async fetchProcesses() {
      try {
        const response = await getAllProcesses()
        this.processList = response
      } catch (error) {
        this.$message.error('获取进程列表失败')
        console.error('获取进程列表失败:', error)
      }
    },
    async fetchCurrentProcess() {
      try {
        const response = await getCurrentProcess()
        this.currentProcess = response
      } catch (error) {
        console.error('获取当前进程失败:', error)
      }
    },
    createProcess() {
      this.dialogVisible = true
    },
    async submitProcess() {
      try {
        const response = await createProcess(this.processForm)
        this.processList.push(response)
        this.dialogVisible = false
        this.$message.success('创建进程成功')
        // 重置表单
        this.processForm = {
          name: '',
          priority: 1,
          memorySize: 256
        }
      } catch (error) {
        this.$message.error('创建进程失败')
        console.error('创建进程失败:', error)
      }
    },
    scheduleProcess() {
      this.$message.success('执行进程调度')
    },
    blockProcess() {
      this.$message.warning('执行进程阻塞')
    },
    wakeupProcess() {
      this.$message.info('执行进程唤醒')
    },
    synchronizeProcess() {
      this.$message.success('执行进程同步')
    },
    handleEdit(row) {
      console.log('编辑进程', row)
    },
    handleDelete(row) {
      this.$confirm('确认删除该进程?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$message.success('删除成功')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.process-container {
  padding: 20px;
}

.operation-buttons {
  margin-bottom: 20px;
}

.operation-buttons .el-button {
  margin-right: 10px;
}
</style> 