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
    </el-tabs>
  </div>
</template>

<script>
import SystemClock from './components/SystemClock.vue'
import ProcessManagement from './components/ProcessManagement.vue'
import MemoryManagement from './components/MemoryManagement.vue'
import FileSystem from './components/FileSystem.vue'

export default {
  name: 'App',
  components: {
    SystemClock,
    ProcessManagement,
    MemoryManagement,
    FileSystem,
  },
  data() {
    return {
      activeTab: 'process'
    }
  }
}
</script>

<style>
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
}

#app {
  font-family: 'Microsoft YaHei', Arial, sans-serif;
  color: #2c3e50;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.el-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.el-tabs__content {
  flex: 1;
  overflow: auto;
}

.el-tab-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.tab-content {
  flex: 1;
  padding: 20px;
  box-sizing: border-box;
  margin-top: 10px;
  margin-left: 10px;
  margin-right: 10px;
  margin-bottom: 40px;
}

.management-container {
  display: flex;
  gap: 20px;
  width: 100%;
  height: 100%;
}

.left-panel {
  flex: 6;
  min-width: 0;

}

.right-panel {
  flex: 4;
  min-width: 0;
  overflow: auto;
}

</style> 