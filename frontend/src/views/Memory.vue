<template>
  <div class="memory-container">
    <h2>内存管理</h2>

    <!-- 内存状态概览 -->
    <el-row :gutter="20" class="memory-overview">
      <el-col :span="8">
        <el-card>
          <div slot="header">总内存</div>
          <div class="memory-value">1024 MB</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div slot="header">已使用</div>
          <div class="memory-value">{{ usedMemory }} MB</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div slot="header">可用内存</div>
          <div class="memory-value">{{ freeMemory }} MB</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 内存分配操作 -->
    <div class="memory-operations">
      <el-button type="primary" @click="allocateMemory">分配内存</el-button>
      <el-button type="danger" @click="freeMemory">释放内存</el-button>
    </div>

    <!-- 内存使用情况表格 -->
    <el-table :data="memoryBlocks" style="width: 100%; margin-top: 20px">
      <el-table-column prop="id" label="块ID" width="100"></el-table-column>
      <el-table-column prop="processId" label="进程ID" width="100"></el-table-column>
      <el-table-column prop="size" label="大小(MB)" width="120"></el-table-column>
      <el-table-column prop="startAddress" label="起始地址" width="150"></el-table-column>
      <el-table-column prop="status" label="状态" width="120"></el-table-column>
      <el-table-column label="操作">
        <template slot-scope="scope">
          <el-button size="mini" type="danger" @click="handleFree(scope.row)">释放</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分配内存对话框 -->
    <el-dialog title="分配内存" :visible.sync="dialogVisible" width="30%">
      <el-form :model="memoryForm" label-width="100px">
        <el-form-item label="进程ID">
          <el-input-number v-model="memoryForm.processId" :min="1"></el-input-number>
        </el-form-item>
        <el-form-item label="内存大小(MB)">
          <el-input-number v-model="memoryForm.size" :min="1" :max="freeMemory"></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitAllocation">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Memory',
  data() {
    return {
      totalMemory: 1024,
      memoryBlocks: [],
      dialogVisible: false,
      memoryForm: {
        processId: 1,
        size: 100
      }
    }
  },
  computed: {
    usedMemory() {
      return this.memoryBlocks.reduce((sum, block) => sum + block.size, 0)
    },
    freeMemory() {
      return this.totalMemory - this.usedMemory
    }
  },
  methods: {
    allocateMemory() {
      this.dialogVisible = true
    },
    freeMemory() {
      this.$message.success('执行内存释放')
    },
    handleFree(row) {
      this.$confirm('确认释放该内存块?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const index = this.memoryBlocks.findIndex(block => block.id === row.id)
        if (index !== -1) {
          this.memoryBlocks.splice(index, 1)
          this.$message.success('内存释放成功')
        }
      }).catch(() => {})
    },
    submitAllocation() {
      if (this.memoryForm.size > this.freeMemory) {
        this.$message.error('内存不足')
        return
      }

      const newBlock = {
        id: this.memoryBlocks.length + 1,
        processId: this.memoryForm.processId,
        size: this.memoryForm.size,
        startAddress: this.calculateStartAddress(),
        status: '已分配'
      }

      this.memoryBlocks.push(newBlock)
      this.dialogVisible = false
      this.$message.success('内存分配成功')
    },
    calculateStartAddress() {
      if (this.memoryBlocks.length === 0) {
        return '0x0000'
      }
      const lastBlock = this.memoryBlocks[this.memoryBlocks.length - 1]
      const lastAddress = parseInt(lastBlock.startAddress, 16)
      const newAddress = lastAddress + lastBlock.size
      return '0x' + newAddress.toString(16).padStart(4, '0')
    }
  }
}
</script>

<style scoped>
.memory-container {
  padding: 20px;
}

.memory-overview {
  margin-bottom: 20px;
}

.memory-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  text-align: center;
}

.memory-operations {
  margin: 20px 0;
}

.memory-operations .el-button {
  margin-right: 10px;
}
</style> 