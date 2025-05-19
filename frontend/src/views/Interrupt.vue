<template>
  <div class="interrupt-container">
    <h2>中断处理</h2>

    <!-- 中断操作按钮 -->
    <div class="operation-buttons">
      <el-button type="primary" @click="triggerInterrupt">触发中断</el-button>
      <el-button type="success" @click="handleInterrupt">处理中断</el-button>
    </div>

    <!-- 中断列表 -->
    <el-table :data="interruptList" style="width: 100%; margin-top: 20px">
      <el-table-column prop="id" label="中断ID" width="100"></el-table-column>
      <el-table-column prop="type" label="中断类型" width="150"></el-table-column>
      <el-table-column prop="priority" label="优先级" width="120"></el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="触发时间" width="180"></el-table-column>
      <el-table-column prop="handleTime" label="处理时间" width="180"></el-table-column>
      <el-table-column label="操作">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="primary"
            @click="handleInterruptItem(scope.row)"
            :disabled="scope.row.status !== '待处理'">
            处理
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 触发中断对话框 -->
    <el-dialog title="触发中断" :visible.sync="dialogVisible" width="30%">
      <el-form :model="interruptForm" label-width="100px">
        <el-form-item label="中断类型">
          <el-select v-model="interruptForm.type" placeholder="请选择中断类型">
            <el-option label="硬件中断" value="hardware"></el-option>
            <el-option label="软件中断" value="software"></el-option>
            <el-option label="时钟中断" value="timer"></el-option>
            <el-option label="I/O中断" value="io"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="interruptForm.priority" placeholder="请选择优先级">
            <el-option label="高" value="high"></el-option>
            <el-option label="中" value="medium"></el-option>
            <el-option label="低" value="low"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitInterrupt">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Interrupt',
  data() {
    return {
      interruptList: [],
      dialogVisible: false,
      interruptForm: {
        type: 'hardware',
        priority: 'medium'
      }
    }
  },
  methods: {
    triggerInterrupt() {
      this.dialogVisible = true
    },
    handleInterrupt() {
      this.$message.success('执行中断处理')
    },
    handleInterruptItem(row) {
      this.$confirm('确认处理该中断?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const interrupt = this.interruptList.find(i => i.id === row.id)
        if (interrupt) {
          interrupt.status = '已处理'
          interrupt.handleTime = new Date().toLocaleString()
          this.$message.success('中断处理成功')
        }
      }).catch(() => {})
    },
    submitInterrupt() {
      const newInterrupt = {
        id: this.interruptList.length + 1,
        type: this.interruptForm.type,
        priority: this.interruptForm.priority,
        status: '待处理',
        createTime: new Date().toLocaleString(),
        handleTime: null
      }
      this.interruptList.push(newInterrupt)
      this.dialogVisible = false
      this.$message.success('中断触发成功')
    },
    getStatusType(status) {
      switch (status) {
        case '待处理':
          return 'warning'
        case '已处理':
          return 'success'
        default:
          return 'info'
      }
    }
  }
}
</script>

<style scoped>
.interrupt-container {
  padding: 20px;
}

.operation-buttons {
  margin-bottom: 20px;
}

.operation-buttons .el-button {
  margin-right: 10px;
}
</style> 