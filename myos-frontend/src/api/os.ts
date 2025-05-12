import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/'
})

// 设备类型枚举
export enum DeviceType {
  PRINTER = 'PRINTER',    // 打印机
  DISK = 'DISK',         // 磁盘
  KEYBOARD = 'KEYBOARD', // 键盘
  MOUSE = 'MOUSE',       // 鼠标
  USB = 'USB',          // USB设备
  OTHER = 'OTHER'        // 其他设备
}

// 设备状态枚举
export enum DeviceStatus {
  IDLE = 'IDLE',   // 空闲
  BUSY = 'BUSY',   // 忙碌
  ERROR = 'ERROR'  // 错误
}

// 设备类型显示名称映射
export const DeviceTypeNames: Record<DeviceType, string> = {
  [DeviceType.PRINTER]: '打印机',
  [DeviceType.DISK]: '磁盘',
  [DeviceType.KEYBOARD]: '键盘',
  [DeviceType.MOUSE]: '鼠标',
  [DeviceType.USB]: 'USB设备',
  [DeviceType.OTHER]: '其他设备'
}

// 设备状态显示名称映射
export const DeviceStatusNames: Record<DeviceStatus, string> = {
  [DeviceStatus.IDLE]: '空闲',
  [DeviceStatus.BUSY]: '忙碌',
  [DeviceStatus.ERROR]: '错误'
}

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
  id: number
  name: string
  type: DeviceType
  status: DeviceStatus
  currentProcessId: number | null
  remainingTime: number
  waitQueue: number[]
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

interface ApiResponse<T> {
  status: string
  message: string
  data: T
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
  getAllDevices() {
    return api.get<ApiResponse<Device[]>>('/api/devices')
  },

  getDevice(id: number) {
    return api.get<ApiResponse<Device>>(`/api/devices/${id}`)
  },

  getDevicesByType(type: DeviceType) {
    return api.get<ApiResponse<Device[]>>(`/api/devices/type/${type}`)
  },

  getAvailableDevices() {
    return api.get<ApiResponse<Device[]>>('/api/devices/available')
  },

  allocateDevice(deviceId: number, processId: number, taskDuration: number) {
    return api.post<ApiResponse<Device>>(`/api/devices/${deviceId}/request`, {
      processId,
      taskDuration
    })
  },

  releaseDevice(deviceId: number) {
    return api.post<ApiResponse<Device>>(`/api/devices/${deviceId}/release`)
  },

  simulateDeviceInterrupt(deviceId: number) {
    return api.post<ApiResponse<Device>>(`/api/devices/${deviceId}/interrupt`)
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