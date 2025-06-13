import request from './config'

// 获取当前目录路径
export function getCurrentPath() {
  return request({
    url: '/filesystem/current-path',
    method: 'get'
  })
}

// 创建目录
export function createDirectory(data) {
  return request({
    url: '/filesystem/directory',
    method: 'post',
    data
  })
}

// 切换目录
export function changeDirectory(data) {
  return request({
    url: '/filesystem/change-directory',
    method: 'post',
    data
  })
}

// 列出目录内容
export function listDirectory() {
  return request({
    url: '/filesystem/list',
    method: 'get'
  })
}

// 获取目录详细内容
export function getDirectoryContent() {
  return request({
    url: '/filesystem/directory-content',
    method: 'get'
  })
}

// 删除目录
export function deleteDirectory(data) {
  return request({
    url: '/filesystem/directory',
    method: 'delete',
    data
  })
}

// 创建文件
export function createFile(data) {
  return request({
    url: '/filesystem/file',
    method: 'post',
    data
  })
}

// 打开文件
export function openFile(data) {
  return request({
    url: '/filesystem/file/open',
    method: 'post',
    data
  })
}

// 关闭文件
export function closeFile(data) {
  return request({
    url: '/filesystem/file/close',
    method: 'post',
    data
  })
}

// 读取文件内容
export function getFileContent(params) {
  return request({
    url: '/filesystem/file/content',
    method: 'get',
    params
  })
}

// 写入文件内容
export function writeFileContent(name, content) {
  return request({
    url: '/filesystem/file/content',
    method: 'post',
    params: { name },
    data: content,
    headers: {
      'Content-Type': 'text/plain'
    }
  })
}

// 删除文件
export function deleteFile(data) {
  return request({
    url: '/filesystem/file',
    method: 'delete',
    data
  })
}