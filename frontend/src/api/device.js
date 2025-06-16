import request from './config'

// 获取可用设备
export function getAvailableDevices() {
  return request({
    url: '/device/available',
    method: 'get'
  })
}

// 获取全部设备
export function getAllDevices() {
  return request({
    url: '/device',
    method: 'get'
  })
}

// 获取设备状态
export function getDeviceStatus(deviceId) {
  return request({
    url: '/device/status',
    method: 'get',
    params: { deviceId }
  })
}

// 请求设备
export function requestDevice(processId, deviceType) {
  return request({
    url: `/process/${processId}/request-device`,
    method: 'post',
    data: { deviceType }
  })
}

// 释放设备
export function releaseDevice(processId, deviceType) {
  return request({
    url: `/process/${processId}/release-device`,
    method: 'post',
    data: { deviceType }
  })
}

// 按类型获取设备列表
export function getDevicesByType(type) {
  return request({
    url: `/device/type/${type}`,
    method: 'get'
  })
}

// 获取设备详细信息
export function getDeviceInfo(deviceId) {
  return request({
    url: `/device/${deviceId}`,
    method: 'get'
  })
}
