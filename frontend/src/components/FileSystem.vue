<template>
  <div class="file-system">
    <div class="file-control">
      <!-- 第一行：当前目录 -->
      <div style="margin-bottom: 12px; font-size: 16px;">
        当前目录：{{ currentPath }}
      </div>
      <!-- 第二行：切换目录 -->
      <div style="margin-bottom: 12px; display: flex; align-items: center;">
        <span style="margin-right: 8px;">切换目录：</span>
        <el-input
          v-model="changeDirInput"
          placeholder="/ .. 目录名称"
          style="width: 300px; margin-right: 8px;"
          size="small"
        ></el-input>
        <el-button type="primary" size="small" @click="handleChangeDirectory">切换</el-button>
      </div>
      <!-- 第三行：创建目录/文件按钮 -->
      <div style="margin-bottom: 12px; display: flex; gap: 12px;">
        <el-button type="success" size="small" @click="showCreateDir = !showCreateDir">创建目录</el-button>
        <el-button type="primary" size="small" @click="showCreateFileDialog = true">创建文件</el-button>
      </div>
      <!-- 第四行：创建目录输入框 -->
      <div v-if="showCreateDir" style="margin-bottom: 12px; display: flex; align-items: center; gap: 8px;">
        <span>目录名称：</span>
        <el-input v-model="createDirName" size="small" style="width: 200px;"></el-input>
        <el-button type="success" size="small" @click="handleCreateDirectory">提交</el-button>
        <el-button size="small" @click="showCreateDir = false">取消</el-button>
      </div>
      <!-- 创建文件对话框 -->
      <el-dialog title="创建文件" :visible.sync="showCreateFileDialog" width="800px" @open="handleDialogOpen" @close="handleDialogClose">
        <el-form label-width="80px">
          <el-form-item label="文件名称">
            <el-input v-model="createFileName"></el-input>
          </el-form-item>
          <el-form-item label="文件内容">
            <el-input type="textarea" v-model="createFileContent" :rows="6"></el-input>
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button @click="showCreateFileDialog = false">取消</el-button>
          <el-button type="primary" @click="handleCreateFile">创建</el-button>
        </div>
      </el-dialog>
      <!-- 文件树结构保持不变 -->
      <div class="file-tree">
        <h3>文件系统结构</h3>
        <el-tree
          :data="fileTree"
          :props="defaultProps"
          @node-click="handleNodeClick"
          default-expand-all
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

      <!-- 添加文件内容显示和编辑区域 -->
      <div class="file-content-area">
        <div class="content-header">
          <h3>当前文件: {{ currentFile || '未选择文件' }}</h3>
          <div class="content-actions">
            <el-button 
              type="primary" 
              size="small" 
              :disabled="!currentFile"
              @click="isEditing = !isEditing"
            >
              {{ isEditing ? '保存' : '修改' }}
            </el-button>
            <el-button 
              type="info" 
              size="small" 
              :disabled="!currentFile"
              @click="closeCurrentFile"
            >
              关闭文件
            </el-button>
          </div>
        </div>
        <div class="content-body">
          <el-input
            v-if="isEditing"
            type="textarea"
            v-model="fileContent"
            :rows="10"
            placeholder="请输入文件内容"
          ></el-input>
          <div v-else class="content-display">
            {{ fileContent || '暂无文件内容' }}
          </div>
        </div>
      </div>
    </div>
    
  </div>
</template>

<script>
import {
  getCurrentPath,
  getDirectoryTree,
  createDirectory,
  createFile,
  deleteDirectory,
  deleteFile,
  openFile,
  closeFile,
  readFileContent,
  writeFileContent,
  changeDirectory
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
      fileTree: [],
      defaultProps: {
        children: 'children',
        label: 'name'
      },
      totalSpace: 8192,
      usedSpace: 0,
      freeSpace: 8192,
      blockSize: 32,
      diskRows: [],
      blocksPerRow: 32,
      currentPath: '/',
      loading: false,
      currentFile: null,
      fileContent: '',
      isEditing: false,
      showCreateDir: false,
      createDirName: '',
      showCreateFileDialog: false,
      createFileName: '',
      createFileContent: '',
      changeDirInput: ''
    }
  },
  created() {
    this.initializeDisk()
    this.fetchFileTree()
    this.fetchCurrentPath()
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

    getBlockTooltip(block) {
      if (block.status === 'ALLOCATED') {
        return `文件: ${block.fileId}\n大小: ${block.size}MB`
      }
      return '空闲块'
    },

    async fetchFileTree() {
      try {
        const response = await getDirectoryTree()
        if (response.success) {
          this.fileTree = [response.tree]
        }
      } catch (error) {
        this.$message.error('获取文件树失败')
      }
    },

    async fetchCurrentPath() {
      try {
        const response = await getCurrentPath()
        this.currentPath = response
        console.log(this.currentPath)
      } catch (error) {
        this.$message.error('获取当前路径失败')
      }
    },

    async createFileOrDir() {
      if (!this.fileForm.name) {
        this.$message.warning('请输入名称')
        return
      }

      try {
        if (this.fileForm.type === 'directory') {
          await createDirectory(this.fileForm.name)
        } else {
          await createFile(this.fileForm.name)
        }
        this.$message.success(`${this.fileForm.type === 'directory' ? '目录' : '文件'}创建成功`)
        this.fileForm.name = ''
        this.fetchFileTree()
      } catch (error) {
        this.$message.error(`${this.fileForm.type === 'directory' ? '目录' : '文件'}创建失败`)
      }
    },

    async handleNodeClick(data) {
      if (data.type === 'directory') {
        this.currentFile = null
        this.fileContent = ''
      } else if (data.type === 'file') {
        try {
          await openFile(data.name)
          const response = await readFileContent(data.name)
          if (response.success) {
            this.currentFile = data.name
            this.fileContent = response.content
            console.log(this.fileContent)
          }
        } catch (error) {
          this.$message.error('打开文件失败')
        }
      }
    },

    async deleteNode(node, data) {
      try {
        if (data.type === 'directory') {
          await deleteDirectory(data.name)
        } else {
          await deleteFile(data.name)
        }
        this.$message.success(`${data.type === 'directory' ? '目录' : '文件'}删除成功`)
        this.fetchFileTree()
      } catch (error) {
        this.$message.error(`${data.type === 'directory' ? '目录' : '文件'}删除失败`)
      }
    },

    async closeCurrentFile() {
      if (!this.currentFile) return
      
      try {
        await closeFile(this.currentFile)
        this.currentFile = null
        this.fileContent = ''
        this.isEditing = false
        this.$message.success('文件已关闭')
      } catch (error) {
        this.$message.error('关闭文件失败')
      }
    },

    async saveFileContent() {
      if (!this.currentFile) return
      
      try {
        await writeFileContent(this.currentFile, this.fileContent)
        this.isEditing = false
        this.$message.success('文件保存成功')
      } catch (error) {
        this.$message.error('文件保存失败')
      }
    },

    async handleChangeDirectory() {
      try {
        console.log('切换目录请求参数:',this.changeDirInput)
        console.log('切换目录请求参数类型:', typeof this.changeDirInput)
        const response = await changeDirectory(String(this.changeDirInput).trim())
        console.log('切换目录响应:', response)
        this.fetchFileTree()
        this.fetchCurrentPath()
        this.changeDirInput = ''
      } catch (error) {
        console.error('切换目录错误:', error)
        this.$message.error('切换目录失败')
      }
    },

    async handleCreateDirectory() {
      try {
        await createDirectory(this.createDirName)
        this.showCreateDir = false
        this.fetchFileTree()
        this.fetchCurrentPath()
      } catch (error) {
        this.$message.error('创建目录失败')
      }
    },

    handleDialogOpen() {
      console.log('对话框打开时的内容:', this.createFileContent)
    },

    handleDialogClose() {
      console.log('对话框关闭时的内容:', this.createFileContent)
      this.resetCreateFileDialog()
    },

    async handleCreateFile() {
      console.log('创建文件前的名称:', this.createFileName)
      console.log('创建文件前的内容:', this.createFileContent)
      try {
        await createFile(this.createFileName)
        const content = this.createFileContent || ' '  // 确保内容不为空
        await writeFileContent(this.createFileName, content)
        console.log('创建文件后的内容:', content)
        this.showCreateFileDialog = false
        this.fetchFileTree()
        this.fetchCurrentPath()
      } catch (error) {
        console.error('创建文件错误:', error)
        this.$message.error('创建文件失败')
      }
    },

    resetCreateFileDialog() {
      this.createFileName = ''
      this.createFileContent = ''
    }
  },
  watch: {
    isEditing(newVal) {
      if (!newVal && this.currentFile) {
        this.saveFileContent()
      }
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
  height: fit-content;
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

.file-content-area {
  margin-top: 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 15px;
  background-color: #ffffff;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.content-header h3 {
  margin: 0;
  font-size: 16px;
}

.content-body {
  min-height: 200px;
}

.content-display {
  padding: 10px;
  min-height: 200px;
  background-color: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}

:deep(.el-textarea__inner) {
  font-size: 14px;
  font-family: monospace;
}
</style> 