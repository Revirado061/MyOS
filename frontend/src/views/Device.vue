<template>
  <div class="device-container">
    <h2>设备管理</h2>

    <!-- 设备操作按钮 -->
    <div class="operation-buttons">
      <el-button type="primary" @click="requestDevice">申请设备</el-button>
      <el-button type="success" @click="releaseDevice">释放设备</el-button>
    </div>

    <!-- 设备列表 -->
    <el-table :data="deviceList" style="width: 100%; margin-top: 20px">
      <el-table-column prop="id" label="设备ID" width="100"></el-table-column>
      <el-table-column prop="name" label="设备名称" width="150"></el-table-column>
      <el-table-column prop="type" label="设备类型" width="120"></el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '空闲' ? 'success' : 'danger'">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="processId" label="占用进程" width="120"></el-table-column>
      <el-table-column prop="requestTime" label="申请时间" width="180"></el-table-column>
      <el-table-column label="操作">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="primary"
            @click="handleRequest(scope.row)"
            :disabled="scope.row.status !== '空闲'">
            申请
          </el-button>
          <el-button
            size="mini"
            type="danger"
            @click="handleRelease(scope.row)"
            :disabled="scope.row.status === '空闲'">
            释放
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 申请设备对话框 -->
    <el-dialog title="申请设备" :visible.sync="dialogVisible" width="30%">
      <el-form :model="deviceForm" label-width="100px">
        <el-form-item label="进程ID">
          <el-input-number v-model="deviceForm.processId" :min="1"></el-input-number>
        </el-form-item>
        <el-form-item label="使用时长(秒)">
          <el-input-number v-model="deviceForm.duration" :min="1" :max="3600"></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitRequest">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Device',
  data() {
    return {
      deviceList: [
        {
          id: 1,
          name: '打印机',
          type: '输出设备',
          status: '空闲',
          processId: null,
          requestTime: null
        },
        {
          id: 2,
          name: '扫描仪',
          type: '输入设备',
          status: '空闲',
          processId: null,
          requestTime: null
        },
        {
          id: 3,
          name: '键盘',
          type: '输入设备',
          status: '空闲',
          processId: null,
          requestTime: null
        },
        {
          id: 4,
          name: '显示器',
          type: '输出设备',
          status: '空闲',
          processId: null,
          requestTime: null
        }
      ],
      dialogVisible: false,
      deviceForm: {
        processId: 1,
        duration: 60
      },
      selectedDevice: null
    }
  },
  methods: {
    requestDevice() {
      this.dialogVisible = true
    },
    releaseDevice() {
      this.$message.success('执行设备释放')
    },
    handleRequest(row) {
      this.selectedDevice = row
      this.dialogVisible = true
    },
    handleRelease(row) {
      this.$confirm('确认释放该设备?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const device = this.deviceList.find(d => d.id === row.id)
        if (device) {
          device.status = '空闲'
          device.processId = null
          device.requestTime = null
          this.$message.success('设备释放成功')
        }
      }).catch(() => {})
    },
    submitRequest() {
      if (this.selectedDevice) {
        const device = this.deviceList.find(d => d.id === this.selectedDevice.id)
        if (device) {
          device.status = '使用中'
          device.processId = this.deviceForm.processId
          device.requestTime = new Date().toLocaleString()
          this.$message.success('设备申请成功')
        }
      }
      this.dialogVisible = false
      this.selectedDevice = null
    }
  }
}
</script>

<style scoped>
.device-container {
  padding: 20px;
}

.operation-buttons {
  margin-bottom: 20px;
}

.operation-buttons .el-button {
  margin-right: 10px;
}
</style> 