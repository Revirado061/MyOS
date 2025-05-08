<template>
  <div class="memory">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>内存管理</span>
          <el-button type="primary" @click="showAllocateDialog">分配内存</el-button>
        </div>
      </template>

      <div class="memory-visualization">
        <div class="memory-blocks">
          <div
            v-for="(block, index) in memoryBlocks"
            :key="index"
            class="memory-block"
            :class="{ 'free': block.isFree }"
            :style="{ width: `${(block.size / totalMemory) * 100}%` }"
          >
            <span class="block-info">
              {{ block.isFree ? '空闲' : '已用' }}<br>
              {{ block.size }}MB
            </span>
          </div>
        </div>
      </div>

      <el-table :data="memoryBlocks" style="width: 100%; margin-top: 20px">
        <el-table-column prop="start" label="起始地址" width="120" />
        <el-table-column prop="size" label="大小(MB)" width="120" />
        <el-table-column prop="isFree" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.isFree ? 'success' : 'danger'">
              {{ row.isFree ? '空闲' : '已用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="分配内存" width="500px">
      <el-form :model="allocationForm" label-width="100px">
        <el-form-item label="进程ID">
          <el-input-number v-model="allocationForm.processId" :min="1" />
        </el-form-item>
        <el-form-item label="内存大小(MB)">
          <el-input-number 
            v-model="allocationForm.size" 
            :min="1" 
            :max="maxAllocatable"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="allocateMemory">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { osApi, type MemoryBlock } from '../api/os'
import { ElMessage } from 'element-plus'

const memoryBlocks = ref<MemoryBlock[]>([])
const dialogVisible = ref(false)
const allocationForm = ref({
  processId: 1,
  size: 1
})

const totalMemory = 1024 // 假设总内存为1024MB

const maxAllocatable = computed(() => {
  return memoryBlocks.value
    .filter(block => block.isFree)
    .reduce((sum, block) => sum + block.size, 0)
})

const fetchMemoryBlocks = async () => {
  try {
    const response = await osApi.getFreeMemory()
    memoryBlocks.value = response.data
  } catch (error) {
    console.error('Error fetching memory blocks:', error)
    ElMessage.error('获取内存信息失败')
  }
}

const showAllocateDialog = () => {
  allocationForm.value = {
    processId: 1,
    size: 1
  }
  dialogVisible.value = true
}

const allocateMemory = async () => {
  try {
    await osApi.allocateMemory(
      allocationForm.value.processId,
      allocationForm.value.size
    )
    ElMessage.success('内存分配成功')
    dialogVisible.value = false
    fetchMemoryBlocks()
  } catch (error) {
    console.error('Error allocating memory:', error)
    ElMessage.error('内存分配失败')
  }
}

onMounted(() => {
  fetchMemoryBlocks()
})
</script>

<style scoped>
.memory {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.memory-visualization {
  margin: 20px 0;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.memory-blocks {
  display: flex;
  height: 60px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.memory-block {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #409eff;
  color: white;
  transition: all 0.3s;
}

.memory-block.free {
  background-color: #67c23a;
}

.block-info {
  font-size: 12px;
  text-align: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style> 