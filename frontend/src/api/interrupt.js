import request from './config'

// 触发中断
export function triggerInterrupt(data) {
  return request({
    url: '/interrupts/trigger',
    method: 'post',
    data
  })
}

// 获取中断队列
export function getInterruptQueue() {
  return request({
    url: '/interrupts/queue',
    method: 'get'
  })
}

// 获取中断日志
export function getInterruptLogs(params) {
  return request({
    url: '/interrupts/logs',
    method: 'get',
    params
  })
}
