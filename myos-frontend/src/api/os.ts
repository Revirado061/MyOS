import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/'
})

export interface Process {
  id?: number
  name: string
  priority: number
  state: string
  memorySize: number
  createTime?: string
  lastUpdateTime?: string
}

export interface MemoryBlock {
  start: number
  size: number
  isFree: boolean
}

export interface Device {
  name: string
  type: string
  currentProcess: Process | null
}

export interface File {
  name: string
  size: number
  createTime?: string
  lastModified?: string
}

export interface Directory {
  name: string
  createTime?: string
  lastModified?: string
}

interface FileSystemResponse {
  files: File[]
  directories: string[]
}

export const osApi = {
  // 进程管理
  createProcess(process: Process) {
    return api.post<Process>('/process', process)
  },
  
  getAllProcesses() {
    return api.get<Process[]>('/process')
  },
  
  getCurrentProcess() {
    return api.get<Process>('/process/current')
  },

  deleteProcess(id: number) {
    return api.delete(`/process/${id}`)
  },

  updateProcessState(id: number, state: string) {
    return api.put(`/process/${id}/state`, null, {
      params: { state }
    })
  },
  
  // 内存管理
  allocateMemory(processId: number, size: number) {
    return api.post<boolean>('/memory/allocate', null, {
      params: { processId, size }
    })
  },
  
  getFreeMemory() {
    return api.get<MemoryBlock[]>('/memory/free')
  },

  freeMemory(processId: number) {
    return api.post<boolean>('/memory/free', null, {
      params: { processId }
    })
  },
  
  // 文件系统
  createFile(name: string) {
    return api.post<boolean>('/file', null, {
      params: { name }
    })
  },
  
  listFiles() {
    return api.get<FileSystemResponse>('/file')
  },

  deleteFile(name: string) {
    return api.delete(`/file/${name}`)
  },

  readFile(name: string) {
    return api.get<string>(`/file/${name}/content`)
  },

  writeFile(name: string, content: string) {
    return api.post<boolean>(`/file/${name}/content`, content, {
      headers: { 'Content-Type': 'text/plain' }
    })
  },
  
  // 设备管理
  requestDevice(processId: number, deviceName: string) {
    return api.post<boolean>('/device/request', null, {
      params: { processId, deviceName }
    })
  },
  
  getAvailableDevices() {
    return api.get<Device[]>('/device/available')
  },

  releaseDevice(deviceName: string) {
    return api.post<boolean>('/device/release', null, {
      params: { deviceName }
    })
  },

  createDirectory(name: string) {
    return api.post<boolean>('/file/directory', null, {
      params: { name }
    })
  },

  deleteDirectory(name: string) {
    return api.delete(`/file/directory/${name}`)
  },

  changeDirectory(name: string) {
    return api.post('/file/change-directory', null, { params: { name } })
  }
} 