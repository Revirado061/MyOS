<template>
  <div class="device">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备管理</span>
          <div class="header-actions">
            <el-select v-model="selectedType" placeholder="选择设备类型" @change="handleTypeChange">
              <el-option
                v-for="type in deviceTypes"
                :key="type"
                :label="DeviceTypeNames[type as DeviceType]"
                :value="type"
              />
            </el-select>
            <el-button type="primary" @click="showRequestDialog">请求设备</el-button>
          </div>
        </div>
      </template>

      <el-table :data="devices" style="width: 100%">
        <el-table-column prop="id" label="设备ID" width="80" />
        <el-table-column prop="name" label="设备名称" width="180" />
        <el-table-column label="设备类型" width="120">
          <template #default="{ row }">
            {{ DeviceTypeNames[row.type as DeviceType] }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status as DeviceStatus)">
              {{ DeviceStatusNames[row.status as DeviceStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前进程" width="120">
          <template #default="{ row }">
            {{ row.currentProcessId || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="剩余时间" width="120">
          <template #default="{ row }">
            {{ row.remainingTime > 0 ? row.remainingTime + '秒' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="等待队列" width="180">
          <template #default="{ row }">
            {{ row.waitQueue.length > 0 ? row.waitQueue.join(', ') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :disabled="row.status !== DeviceStatus.BUSY"
              @click="releaseDevice(row)"
            >
              释放设备
            </el-button>
            <el-button
              type="warning"
              size="small"
              :disabled="row.status !== DeviceStatus.BUSY"
              @click="simulateInterrupt(row)"
            >
              模拟中断
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
        <el-form-item label="设备">
          <el-select v-model="requestForm.deviceId" placeholder="请选择设备">
            <el-option
              v-for="device in availableDevices"
              :key="device.id"
              :label="`${device.name} (${DeviceTypeNames[device.type as DeviceType]})`"
              :value="device.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="使用时长">
          <el-input-number v-model="requestForm.taskDuration" :min="1" :max="3600" />
          <span class="unit">秒</span>
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
import { osApi, type Device, DeviceType, DeviceStatus, DeviceTypeNames, DeviceStatusNames } from '../api/os'
import { ElMessage, ElMessageBox } from 'element-plus'

const devices = ref<Device[]>([])
const dialogVisible = ref(false)
const selectedType = ref<DeviceType | ''>('')
const deviceTypes = Object.values(DeviceType)

const requestForm = ref({
  processId: 1,
  deviceId: undefined as number | undefined,
  taskDuration: 5
})

const availableDevices = computed(() => {
  return devices.value.filter(device => device.status === DeviceStatus.IDLE)
})

const getStatusType = (status: DeviceStatus) => {
  switch (status) {
    case DeviceStatus.IDLE:
      return 'success'
    case DeviceStatus.BUSY:
      return 'warning'
    case DeviceStatus.ERROR:
      return 'danger'
    default:
      return 'info'
  }
}

const fetchDevices = async () => {
  try {
    const response = await osApi.getAllDevices()
    devices.value = response.data.data
    console.log('获取到的设备列表:', devices.value)
  } catch (error) {
    console.error('Error fetching devices:', error)
    ElMessage.error('获取设备列表失败')
  }
}

const handleTypeChange = async (type: DeviceType) => {
  try {
    const response = await osApi.getDevicesByType(type)
    devices.value = response.data.data
  } catch (error) {
    console.error('Error fetching devices by type:', error)
    ElMessage.error('获取设备列表失败')
  }
}

const showRequestDialog = () => {
  requestForm.value = {
    processId: 1,
    deviceId: undefined,
    taskDuration: 5
  }
  dialogVisible.value = true
}

const requestDevice = async () => {
  if (!requestForm.value.deviceId) {
    ElMessage.warning('请选择设备')
    return
  }

  try {
    const response = await osApi.allocateDevice(
      requestForm.value.deviceId,
      requestForm.value.processId,
      requestForm.value.taskDuration
    )
    if (response.data.status === 'success') {
      ElMessage.success(response.data.message)
      dialogVisible.value = false
      fetchDevices()
    } else {
      ElMessage.warning(response.data.message)
    }
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
    const response = await osApi.releaseDevice(device.id)
    if (response.data.status === 'success') {
      ElMessage.success(response.data.message)
      fetchDevices()
    } else {
      ElMessage.warning(response.data.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Error releasing device:', error)
      ElMessage.error('设备释放失败')
    }
  }
}

const simulateInterrupt = async (device: Device) => {
  try {
    const response = await osApi.simulateDeviceInterrupt(device.id)
    if (response.data.status === 'success') {
      ElMessage.success(response.data.message)
      fetchDevices()
    } else {
      ElMessage.warning(response.data.message)
    }
  } catch (error) {
    console.error('Error simulating device interrupt:', error)
    ElMessage.error('设备中断模拟失败')
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

.header-actions {
  display: flex;
  gap: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.unit {
  margin-left: 8px;
  color: #666;
}
</style> 