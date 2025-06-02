import request from './config'

/**
 * 获取空闲内存大小
 * @returns {Promise<number>} 返回空闲内存大小
 */
export function getFreeMemorySize() {
  return request({
    url: '/memory/free-size',
    method: 'get'
  })
}

/**
 * 获取内存状态
 * @returns {Promise<Array<number>>} 返回内存状态数组，非0值表示被进程占用（非0值为对应进程的id），0表示空闲
 */
export function getMemoryStatus() {
  return request({
    url: '/memory/status',
    method: 'get'
  })
}

/**
 * 获取空闲内存块列表
 * @returns {Promise<Array<{start: number, size: number}>>} 返回空闲内存块列表
 */
export function getFreeMemoryBlocks() {
  return request({
    url: '/memory/free-blocks',
    method: 'get'
  })
}

/**
 * 获取内存使用率
 * @returns {Promise<number>} 返回内存使用率（0-1之间的浮点数）
 */
export function getMemoryUsage() {
  return request({
    url: '/memory/usage',
    method: 'get'
  })
}
