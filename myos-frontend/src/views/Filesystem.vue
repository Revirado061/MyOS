<template>
  <div class="filesystem">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文件系统</span>
          <el-button type="primary" @click="showCreateDialog">创建文件</el-button>
        </div>
      </template>

      <el-table :data="files" style="width: 100%">
        <el-table-column prop="name" label="文件名" width="180" />
        <el-table-column prop="size" label="大小(MB)" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="lastModified" label="最后修改时间" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button-group>
              <el-button type="primary" size="small" @click="openFile(row)">
                打开
              </el-button>
              <el-button type="danger" size="small" @click="deleteFile(row)">
                删除
              </el-button>
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
        <el-form-item label="文件大小(MB)">
          <el-input-number v-model="newFile.size" :min="1" :max="1000" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { osApi, type File } from '../api/os'
import { ElMessage, ElMessageBox } from 'element-plus'

const files = ref<File[]>([])
const dialogVisible = ref(false)
const fileContentVisible = ref(false)
const isEditing = ref(false)
const currentFile = ref<File | null>(null)
const currentFileContent = ref('')
const newFile = ref<File>({
  name: '',
  size: 1
})

const fetchFiles = async () => {
  try {
    const response = await osApi.listFiles()
    files.value = response.data
  } catch (error) {
    console.error('Error fetching files:', error)
    ElMessage.error('获取文件列表失败')
  }
}

const showCreateDialog = () => {
  newFile.value = {
    name: '',
    size: 1
  }
  dialogVisible.value = true
}

const createFile = async () => {
  try {
    await osApi.createFile(newFile.value.name, newFile.value.size)
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