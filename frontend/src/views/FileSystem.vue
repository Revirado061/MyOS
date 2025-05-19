<template>
  <div class="filesystem-container">
    <h2>文件系统</h2>

    <!-- 文件系统操作按钮 -->
    <div class="operation-buttons">
      <el-button type="primary" @click="createFile">创建文件</el-button>
      <el-button type="success" @click="createDirectory">创建目录</el-button>
      <el-button type="danger" @click="deleteItem">删除</el-button>
    </div>

    <!-- 文件系统树形结构 -->
    <div class="filesystem-tree">
      <el-tree
        :data="fileSystem"
        :props="defaultProps"
        @node-click="handleNodeClick"
        default-expand-all>
        <span class="custom-tree-node" slot-scope="{ node, data }">
          <span>
            <i :class="data.type === 'directory' ? 'el-icon-folder' : 'el-icon-document'"></i>
            {{ node.label }}
          </span>
          <span>
            <el-button
              type="text"
              size="mini"
              @click="() => handleEdit(node, data)">
              编辑
            </el-button>
            <el-button
              type="text"
              size="mini"
              @click="() => handleDelete(node, data)">
              删除
            </el-button>
          </span>
        </span>
      </el-tree>
    </div>

    <!-- 创建文件对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="30%">
      <el-form :model="fileForm" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="fileForm.name"></el-input>
        </el-form-item>
        <el-form-item label="类型" v-if="isCreatingFile">
          <el-select v-model="fileForm.type" placeholder="请选择文件类型">
            <el-option label="文本文件" value="text"></el-option>
            <el-option label="二进制文件" value="binary"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="大小(KB)" v-if="isCreatingFile">
          <el-input-number v-model="fileForm.size" :min="1"></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'FileSystem',
  data() {
    return {
      fileSystem: [{
        id: 1,
        label: '根目录',
        type: 'directory',
        children: []
      }],
      defaultProps: {
        children: 'children',
        label: 'label'
      },
      dialogVisible: false,
      dialogTitle: '',
      isCreatingFile: false,
      fileForm: {
        name: '',
        type: 'text',
        size: 1
      },
      currentNode: null
    }
  },
  methods: {
    createFile() {
      this.dialogTitle = '创建文件'
      this.isCreatingFile = true
      this.dialogVisible = true
    },
    createDirectory() {
      this.dialogTitle = '创建目录'
      this.isCreatingFile = false
      this.dialogVisible = true
    },
    deleteItem() {
      if (!this.currentNode) {
        this.$message.warning('请先选择要删除的项目')
        return
      }
      this.handleDelete(this.currentNode, this.currentNode.data)
    },
    handleNodeClick(data) {
      this.currentNode = data
    },
    handleEdit(node, data) {
      console.log('编辑', node, data)
    },
    handleDelete(node, data) {
      this.$confirm('确认删除该' + (data.type === 'directory' ? '目录' : '文件') + '?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const parent = node.parent
        const children = parent.data.children || parent.data
        const index = children.findIndex(d => d.id === data.id)
        children.splice(index, 1)
        this.$message.success('删除成功')
      }).catch(() => {})
    },
    submitForm() {
      if (!this.fileForm.name) {
        this.$message.error('请输入名称')
        return
      }

      const newNode = {
        id: Date.now(),
        label: this.fileForm.name,
        type: this.isCreatingFile ? 'file' : 'directory',
        children: this.isCreatingFile ? [] : undefined
      }

      if (this.isCreatingFile) {
        newNode.size = this.fileForm.size
        newNode.fileType = this.fileForm.type
      }

      if (this.currentNode && this.currentNode.type === 'directory') {
        if (!this.currentNode.children) {
          this.$set(this.currentNode, 'children', [])
        }
        this.currentNode.children.push(newNode)
      } else {
        this.fileSystem[0].children.push(newNode)
      }

      this.dialogVisible = false
      this.$message.success('创建成功')
      this.resetForm()
    },
    resetForm() {
      this.fileForm = {
        name: '',
        type: 'text',
        size: 1
      }
    }
  }
}
</script>

<style scoped>
.filesystem-container {
  padding: 20px;
}

.operation-buttons {
  margin-bottom: 20px;
}

.operation-buttons .el-button {
  margin-right: 10px;
}

.filesystem-tree {
  margin-top: 20px;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}

.custom-tree-node i {
  margin-right: 5px;
}
</style> 