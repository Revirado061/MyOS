<template>
  <div class="device">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备管理</span>
          <el-button type="primary" @click="showRequestDialog">请求设备</el-button>
        </div>
      </template>

      <el-table :data="devices" style="width: 100%">
        <el-table-column prop="name" label="设备名称" width="180" />
        <el-table-column prop="type" label="设备类型" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.currentProcess ? 'danger' : 'success'">
              {{ row.currentProcess ? '使用中' : '空闲' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前进程" width="180">
          <template #default="{ row }">
            {{ row.currentProcess ? row.currentProcess.name : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :disabled="!row.currentProcess"
              @click="releaseDevice(row)"
            >
              释放设备
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="请求设备" width="500px">
      <el-form :model="requestForm" label-width="100px">
        <el-form-item label="进程ID">
          <el-input-number v-model="requestForm.processId" :min="1" />
        </el-form-item>
        <el-form-item label="设备名称">
          <el-select v-model="requestForm.deviceName" placeholder="请选择设备">
            <el-option
              v-for="device in availableDevices"
              :key="device.name"
              :label="device.name"
              :value="device.name"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="requestDevice">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { osApi, type Device } from '../api/os'
import { ElMessage, ElMessageBox } from 'element-plus'

const devices = ref<Device[]>([])
const dialogVisible = ref(false)
const requestForm = ref({
  processId: 1,
  deviceName: ''
})

const availableDevices = computed(() => {
  return devices.value.filter(device => !device.currentProcess)
})

const fetchDevices = async () => {
  try {
    const response = await osApi.getAvailableDevices()
    devices.value = response.data
  } catch (error) {
    console.error('Error fetching devices:', error)
    ElMessage.error('获取设备列表失败')
  }
}

const showRequestDialog = () => {
  requestForm.value = {
    processId: 1,
    deviceName: ''
  }
  dialogVisible.value = true
}

const requestDevice = async () => {
  try {
    await osApi.requestDevice(
      requestForm.value.processId,
      requestForm.value.deviceName
    )
    ElMessage.success('设备请求成功')
    dialogVisible.value = false
    fetchDevices()
  } catch (error) {
    console.error('Error requesting device:', error)
    ElMessage.error('设备请求失败')
  }
}

const releaseDevice = async (device: Device) => {
  try {
    await ElMessageBox.confirm(
      `确定要释放设备 ${device.name} 吗？`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await osApi.releaseDevice(device.name)
    ElMessage.success('设备释放成功')
    fetchDevices()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Error releasing device:', error)
      ElMessage.error('设备释放失败')
    }
  }
}

onMounted(() => {
  fetchDevices()
})
</script>

<style scoped>
.device {
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