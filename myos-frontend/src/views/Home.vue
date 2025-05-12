<template>
  <div class="home">
    <el-container>
      <el-header>
        <h1>操作系统模拟程序</h1>
      </el-header>
      
      <el-main>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>进程管理</span>
                </div>
              </template>
              <div class="card-content">
                <p>当前进程数：{{ processCount }}</p>
                <el-button type="primary" @click="$router.push('/process')">
                  查看详情
                </el-button>
              </div>
            </el-card>
          </el-col>
          
          <el-col :span="6">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>内存管理</span>
                </div>
              </template>
              <div class="card-content">
                <p>空闲内存：{{ freeMemory }}MB</p>
                <el-button type="primary" @click="$router.push('/memory')">
                  查看详情
                </el-button>
              </div>
            </el-card>
          </el-col>
          
          <el-col :span="6">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>文件系统</span>
                </div>
              </template>
              <div class="card-content">
                <p>文件数量：{{ fileCount }}</p>
                <el-button type="primary" @click="$router.push('/filesystem')">
                  查看详情
                </el-button>
              </div>
            </el-card>
          </el-col>
          
          <el-col :span="6">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>设备管理</span>
                </div>
              </template>
              <div class="card-content">
                <p>可用设备：{{ availableDevices }}</p>
                <el-button type="primary" @click="$router.push('/device')">
                  查看详情
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { osApi } from '../api/os'

const processCount = ref(0)
const freeMemory = ref(0)
const fileCount = ref(0)
const availableDevices = ref(0)

const fetchData = async () => {
  try {
    const [processes, memory, files, devices] = await Promise.all([
      osApi.getAllProcesses(),
      osApi.getFreeMemory(),
      osApi.listFiles(),
      osApi.getAvailableDevices()
    ])
    
    processCount.value = processes.data.length
    freeMemory.value = memory.data.reduce((sum, block) => sum + block.size, 0)
    fileCount.value = files.data.files.length
    availableDevices.value = devices.data.data.length
  } catch (error) {
    console.error('Error fetching data:', error)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.home {
  padding: 20px;
}

.el-header {
  text-align: center;
  line-height: 60px;
}

.box-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-content {
  text-align: center;
}

.el-button {
  margin-top: 10px;
}
</style> 