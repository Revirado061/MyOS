<template>
  <div class="filesystem">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文件系统</span>
          <el-button type="warning" @click="goBackDir" style="margin-right: 10px">返回上一级</el-button>
          <el-button type="primary" @click="showCreateDialog">创建文件</el-button>
          <el-button type="success" @click="showCreateDirDialog">创建文件夹</el-button>
        </div>
      </template>

      <el-table :data="allItems" style="width: 100%">
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="name" label="文件名" width="180" />
        <el-table-column label="大小(MB)" width="120">
          <template #default="{ row }">
            <span v-if="row.type === '文件'">{{ row.size }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            <span v-if="row.type === '文件'">{{ row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button-group>
              <el-button v-if="row.type === '文件'" type="primary" size="small" @click="openFile(row)">打开</el-button>
              <el-button v-if="row.type === '文件夹'" type="primary" size="small" @click="enterDirectory(row)">进入</el-button>
              <el-button type="danger" size="small" @click="deleteItem(row)">删除</el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="创建文件" width="500px">
      <el-form :model="newFile" label-width="100px">
        <el-form-item label="文件名">
          <el-input v-model="newFile.name" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="createFile">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="fileContentVisible" title="文件内容" width="600px">
      <div class="file-content">
        <el-input
          v-model="currentFileContent"
          type="textarea"
          :rows="10"
          :readonly="!isEditing"
        />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="fileContentVisible = false">关闭</el-button>
          <el-button
            type="primary"
            @click="isEditing ? saveFileContent() : (isEditing = true)"
          >
            {{ isEditing ? '保存' : '编辑' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogDirVisible" title="创建文件夹" width="500px">
      <el-form :model="newDir" label-width="100px">
        <el-form-item label="文件夹名">
          <el-input v-model="newDir.name" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogDirVisible = false">取消</el-button>
          <el-button type="primary" @click="createDirectory">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { osApi, type File } from '../api/os'
import { ElMessage, ElMessageBox } from 'element-plus'

interface FileItem {
  type: '文件' | '文件夹'
  name: string
  size?: number
  createTime?: string
}

const files = ref<File[]>([])
const directories = ref<string[]>([])
const dialogVisible = ref(false)
const fileContentVisible = ref(false)
const isEditing = ref(false)
const currentFile = ref<File | null>(null)
const currentFileContent = ref('')
const newFile = ref<{ name: string }>({
  name: ''
})
const dialogDirVisible = ref(false)
const newDir = ref<{ name: string }>({ name: '' })

const allItems = computed(() => [
  ...directories.value.map(name => ({ type: '文件夹', name })),
  ...files.value.map(f => ({ ...f, type: '文件' }))
])

const fetchFiles = async () => {
  try {
    const response = await osApi.listFiles()
    console.log('API Response:', response)
    
    if (!response || !response.data) {
      throw new Error('Invalid API response')
    }
    
    const { files: fileList, directories: dirList } = response.data
    
    if (!Array.isArray(fileList) || !Array.isArray(dirList)) {
      throw new Error('Invalid data format')
    }
    
    files.value = fileList
    directories.value = dirList
  } catch (error) {
    console.error('Error fetching files:', error)
    ElMessage.error(`获取文件列表失败: ${error instanceof Error ? error.message : '未知错误'}`)
  }
}

const showCreateDialog = () => {
  newFile.value = {
    name: ''
  }
  dialogVisible.value = true
}

const createFile = async () => {
  try {
    await osApi.createFile(newFile.value.name)
    ElMessage.success('创建文件成功')
    dialogVisible.value = false
    fetchFiles()
  } catch (error) {
    console.error('Error creating file:', error)
    ElMessage.error('创建文件失败')
  }
}

const openFile = async (file: File) => {
  try {
    currentFile.value = file
    const response = await osApi.readFile(file.name)
    currentFileContent.value = response.data
    fileContentVisible.value = true
    isEditing.value = false
  } catch (error) {
    console.error('Error reading file:', error)
    ElMessage.error('读取文件失败')
  }
}

const saveFileContent = async () => {
  if (!currentFile.value) return

  try {
    await osApi.writeFile(currentFile.value.name, currentFileContent.value)
    ElMessage.success('保存文件成功')
    isEditing.value = false
  } catch (error) {
    console.error('Error saving file:', error)
    ElMessage.error('保存文件失败')
  }
}

const deleteItem = async (item: FileItem) => {
  if (item.type === '文件') {
    await deleteFile(item as File)
  } else {
    await deleteDirectory(item)
  }
}

const deleteFile = async (file: File) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件 ${file.name} 吗？`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await osApi.deleteFile(file.name)
    ElMessage.success('删除文件成功')
    fetchFiles()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Error deleting file:', error)
      ElMessage.error('删除文件失败')
    }
  }
}

const deleteDirectory = async (item: FileItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件夹 ${item.name} 吗？`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await osApi.deleteDirectory(item.name)
    ElMessage.success('删除文件夹成功')
    fetchFiles()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Error deleting directory:', error)
      ElMessage.error('删除文件夹失败')
    }
  }
}

const showCreateDirDialog = () => {
  newDir.value = { name: '' }
  dialogDirVisible.value = true
}

const createDirectory = async () => {
  try {
    await osApi.createDirectory(newDir.value.name)
    ElMessage.success('创建文件夹成功')
    dialogDirVisible.value = false
    fetchFiles()
  } catch (error) {
    console.error('Error creating directory:', error)
    ElMessage.error('创建文件夹失败')
  }
}

const enterDirectory = async (row: FileItem) => {
  try {
    await osApi.changeDirectory(row.name)
    fetchFiles()
  } catch (error) {
    ElMessage.error('进入文件夹失败')
  }
}

const goBackDir = async () => {
  try {
    await osApi.changeDirectory('..')
    fetchFiles()
  } catch (error) {
    ElMessage.error('返回上一级目录失败')
  }
}

onMounted(() => {
  fetchFiles()
})
</script>

<style scoped>
.filesystem {
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