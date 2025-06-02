<template>
  <div class="file-system">
    <div class="file-control">
      <el-form :model="fileForm" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="fileForm.name"></el-input>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="fileForm.type">
            <el-radio label="file">文件</el-radio>
            <el-radio label="directory">目录</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="fileForm.type === 'file'" label="文件大小(MB)" label-width="165px">
          <el-input-number v-model="fileForm.size" :min="1" :max="1024"></el-input-number>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="createFileOrDir">创建</el-button>
        </el-form-item>
      </el-form>

      <div class="file-tree">
        <h3>文件系统结构</h3>
        <el-tree
          :data="fileTree"
          :props="defaultProps"
          @node-click="handleNodeClick"
        >
          <span class="custom-tree-node" slot-scope="{ node, data }">
            <span>
              <i :class="data.type === 'directory' ? 'el-icon-folder' : 'el-icon-document'"></i>
              {{ node.label }}
            </span>
            <span>
              <el-button
                type="text"
                size="mini"
                @click="() => deleteNode(node, data)">
                删除
              </el-button>
            </span>
          </span>
        </el-tree>
      </div>
    </div>

    <div class="disk-blocks">
      <h2>磁盘空间分配</h2>
      <div class="disk-info">
        <div>总空间: {{ totalSpace }}MB</div>
        <div>已用空间: {{ usedSpace }}MB</div>
        <div>空闲空间: {{ freeSpace }}MB</div>
      </div>
      
      <div class="disk-blocks-grid">
        <div v-for="(row, rowIndex) in diskRows" :key="rowIndex" class="disk-row">
          <div
            v-for="(block, blockIndex) in row"
            :key="blockIndex"
            class="disk-block"
            :class="{
              'allocated': block.status === 'ALLOCATED',
              'free': block.status === 'FREE'
            }"
            :title="getBlockTooltip(block)"
          ></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FileSystem',
  data() {
    return {
      fileForm: {
        name: '',
        type: 'file',
        size: 1
      },
      fileTree: [{
        label: '根目录',
        type: 'directory',
        children: []
      }],
      defaultProps: {
        children: 'children',
        label: 'label'
      },
      totalSpace: 4096,
      usedSpace: 0,
      freeSpace: 4096,
      blockSize: 8,
      diskRows: [],
      blocksPerRow: 32,
      updateInterval: null
    }
  },
  created() {
    this.initializeDisk()
  },
  methods: {
    initializeDisk() {
      const totalBlocks = Math.ceil(this.totalSpace / this.blockSize)
      const rows = Math.ceil(totalBlocks / this.blocksPerRow)
      
      this.diskRows = Array(rows).fill().map(() => 
        Array(this.blocksPerRow).fill().map(() => ({
          status: 'FREE',
          fileId: null,
          size: this.blockSize
        }))
      )
    },
    createFileOrDir() {
      const newNode = {
        label: this.fileForm.name,
        type: this.fileForm.type,
        children: this.fileForm.type === 'directory' ? [] : undefined,
        size: this.fileForm.type === 'file' ? this.fileForm.size : 0
      }
      
      if (this.fileForm.type === 'file') {
        this.allocateDiskSpace(newNode)
      }
      
      this.fileTree[0].children.push(newNode)
      this.fileForm.name = ''
    },
    deleteNode(node, data) {
      if (data.type === 'file') {
        this.freeDiskSpace(data)
      }
      const parent = node.parent
      const children = parent.data.children || parent.data
      const index = children.findIndex(d => d.label === data.label)
      children.splice(index, 1)
    },
    handleNodeClick(data) {
      console.log(data)
    },
    getBlockTooltip(block) {
      if (block.status === 'ALLOCATED') {
        return `文件: ${block.fileId}\n大小: ${block.size}MB`
      }
      return '空闲块'
    },
    allocateDiskSpace(file) {
      const blocksNeeded = Math.ceil(file.size / this.blockSize)
      let allocatedBlocks = 0
      
      for (let row of this.diskRows) {
        for (let block of row) {
          if (block.status === 'FREE') {
            block.status = 'ALLOCATED'
            block.fileId = file.label
            allocatedBlocks++
            
            if (allocatedBlocks === blocksNeeded) {
              this.usedSpace += file.size
              this.freeSpace -= file.size
              return true
            }
          }
        }
      }
      return false
    },
    freeDiskSpace(file) {
      let freedSize = 0
      
      for (let row of this.diskRows) {
        for (let block of row) {
          if (block.status === 'ALLOCATED' && block.fileId === file.label) {
            block.status = 'FREE'
            block.fileId = null
            freedSize += block.size
          }
        }
      }
      
      this.usedSpace -= freedSize
      this.freeSpace += freedSize
    }
  }
}
</script>

<style scoped>
.file-system {
  display: flex;
  gap: 20px;
  padding: 20px;
  height: calc(100vh - 120px); /* 减去头部和padding的高度 */
}

.file-control {
  flex: 4;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #dcdfe6;
  padding-right: 20px;
  font-size: 16px;
}

/* 添加表单相关样式 */
:deep(.el-form-item__label) {
  font-size: 16px !important;
}

:deep(.el-input__inner) {
  font-size: 16px !important;
}

:deep(.el-radio__label) {
  font-size: 16px !important;
}

:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  font-size: 16px !important;
}

:deep(.el-input-number__input) {
  font-size: 16px !important;
}

:deep(.el-button) {
  font-size: 16px !important;
}

.file-tree {
  margin-top: 20px;
  flex: 1;
  overflow-y: auto;
}

.disk-blocks {
  flex: 6;
  display: flex;
  flex-direction: column;
}

.disk-info {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
  font-size: 16px;
}

.disk-blocks-grid {
  border: 1px solid #dcdfe6;
  padding: 15px;
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow-y: auto;
  max-height: calc(100vh - 250px);
}

.disk-row {
  display: flex;
  gap: 2px;
  margin-bottom: 2px;
  justify-content: center;
}

.disk-block {
  width: 28px;
  height: 28px;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  transition: all 0.3s ease;
}

.disk-block:hover {
  transform: scale(1.1);
  z-index: 1;
}

.disk-block.allocated {
  background-color: #409eff;
}

.disk-block.free {
  background-color: #f0f9eb;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}
</style> 