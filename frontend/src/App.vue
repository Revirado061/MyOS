<template>
  <div id="app">
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="进程与内存管理" name="process">
        <div class="tab-content">
          <system-clock></system-clock>
          <div class="management-container">
            <process-management class="left-panel"></process-management>
            <memory-management class="right-panel"></memory-management>
          </div>
        </div>
      </el-tab-pane>
      
      <el-tab-pane label="文件系统" name="file">
        <div class="tab-content">
          <system-clock></system-clock>
          <file-system></file-system>
        </div>
      </el-tab-pane>
      
      <el-tab-pane label="设备与中断" name="device">
        <div class="tab-content">
          <system-clock></system-clock>
          <device-management></device-management>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import SystemClock from './components/SystemClock.vue'
import ProcessManagement from './components/ProcessManagement.vue'
import MemoryManagement from './components/MemoryManagement.vue'
import FileSystem from './components/FileSystem.vue'
import DeviceManagement from './components/DeviceManagement.vue'

export default {
  name: 'App',
  components: {
    SystemClock,
    ProcessManagement,
    MemoryManagement,
    FileSystem,
    DeviceManagement
  },
  data() {
    return {
      activeTab: 'process'
    }
  }
}
</script>

<style>
#app {
  font-family: 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  margin: 0;
  padding: 20px;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.tab-content {
  position: relative;
  padding-top: 50px;
}

.management-container {
  display: flex;
  gap: 20px;
}

.left-panel {
  flex: 1;
}

.right-panel {
  flex: 1;
}

.el-tabs {
  margin-top: 20px;
}
</style> 