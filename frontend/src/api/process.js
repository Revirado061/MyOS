import request from './config'

// 创建进程
export function createProcess(data) {
  return request({
    url: '/process',
    method: 'post',
    data
  })
}

// 获取所有进程
export function getAllProcesses() {
  return request({
    url: '/process',
    method: 'get'
  })
}

// 获取当前运行进程
export function getCurrentProcess() {
  return request({
    url: '/process/current',
    method: 'get'
  })
}

// 获取特定状态的进程
export function getProcessesByState(state) {
  return request({
    url: `/process/${state}`,
    method: 'get'
  })
} 