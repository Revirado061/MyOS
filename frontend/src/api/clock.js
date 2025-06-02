import request from './config'

// 启动系统时钟
export function startSystemClock() {
  return request({
    url: '/timer/start',
    method: 'post'
  })
}

// 停止系统时钟
export function stopSystemClock() {
  return request({
    url: '/timer/stop',
    method: 'post'
  })
}

// 获取当前时间
export function getCurrentTime() {
  return request({
    url: '/timer/current',
    method: 'get'
  })
}
