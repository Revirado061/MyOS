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
import {
  getCurrentPath,
  createDirectory,
  changeDirectory,
  listDirectory,
  getDirectoryContent,
  deleteDirectory,
  createFile,
  openFile,
  closeFile,
  getFileContent,
  writeFileContent,
  deleteFile
} from '@/api/file'

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
      currentPath: '/',
      loading: false
    }
  },
  created() {
    this.initializeDisk()
    this.loadDirectoryContent()
  },
  methods: {
    async loadDirectoryContent() {
      try {
        this.loading = true
        // 获取当前路径
        const pathRes = await getCurrentPath()
        this.currentPath = pathRes.data

        // 获取目录内容
        const contentRes = await getDirectoryContent()
        if (contentRes.success) {
          const { files, directories } = contentRes.content
          
          // 更新文件树
          this.fileTree[0].children = [
            ...directories.map(dir => ({
              label: dir,
              type: 'directory',
              children: []
            })),
            ...files.map(file => ({
              label: file.name,
              type: 'file',
              size: file.size,
              isOpen: file.isOpen,
              isAllocated: file.isAllocated
            }))
          ]

          // 更新磁盘使用情况
          this.usedSpace = files.reduce((sum, file) => sum + file.size, 0)
          this.freeSpace = this.totalSpace - this.usedSpace
        }
      } catch (error) {
        this.$message.error('加载目录内容失败：' + error.message)
      } finally {
        this.loading = false
      }
    },

    async createFileOrDir() {
      try {
        this.loading = true
        if (this.fileForm.type === 'directory') {
          const res = await createDirectory({ name: this.fileForm.name })
          if (res.success) {
            this.$message.success('目录创建成功')
            await this.loadDirectoryContent()
          }
        } else {
          const res = await createFile({ name: this.fileForm.name })
          if (res.success) {
            this.$message.success('文件创建成功')
            await this.loadDirectoryContent()
          }
        }
        this.fileForm.name = ''
      } catch (error) {
        this.$message.error('创建失败：' + error.message)
      } finally {
        this.loading = false
      }
    },

    async handleNodeClick(data) {
      if (data.type === 'directory') {
        try {
          this.loading = true
          const res = await changeDirectory({ path: data.label })
          if (res.success) {
            await this.loadDirectoryContent()
          }
        } catch (error) {
          this.$message.error('切换目录失败：' + error.message)
        } finally {
          this.loading = false
        }
      } else if (data.type === 'file') {
        try {
          this.loading = true
          const res = await openFile({ name: data.label })
          if (res.success) {
            const contentRes = await getFileContent({ name: data.label })
            if (contentRes.success) {
              this.$message.success('文件内容：' + contentRes.content)
            }
            await closeFile({ name: data.label })
          }
        } catch (error) {
          this.$message.error('读取文件失败：' + error.message)
        } finally {
          this.loading = false
        }
      }
    },

    async deleteNode(node, data) {
      try {
        this.loading = true
        if (data.type === 'directory') {
          const res = await deleteDirectory({ name: data.label })
          if (res.success) {
            this.$message.success('目录删除成功')
            await this.loadDirectoryContent()
          }
        } else {
          const res = await deleteFile({ name: data.label })
          if (res.success) {
            this.$message.success('文件删除成功')
            await this.loadDirectoryContent()
          }
        }
      } catch (error) {
        this.$message.error('删除失败：' + error.message)
      } finally {
        this.loading = false
      }
    },

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

    getBlockTooltip(block) {
      if (block.status === 'ALLOCATED') {
        return `文件: ${block.fileId}\n大小: ${block.size}MB`
      }
      return '空闲块'
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