import request from './config'

// 目录操作相关接口
export const getCurrentPath = () => {
  return request({
    url: '/filesystem/current-path',
    method: 'get'
  })
}

export const getDirectoryTree = () => {
  return request({
    url: '/filesystem/tree',
    method: 'get'
  })
}

export const createDirectory = (name) => {
  return request({
    url: '/filesystem/directory',
    method: 'post',
    params: { name }
  })
}

export const changeDirectory = (path) => {
  return request({
    url: '/filesystem/change-directory',
    method: 'post',
    params: { path }
  })
}

export const listDirectory = () => {
  return request({
    url: '/filesystem/list',
    method: 'get'
  })
}

export const deleteDirectory = (name) => {
  return request({
    url: '/filesystem/directory',
    method: 'delete',
    data: { name }
  })
}

// 文件操作相关接口
export const createFile = (name) => {
  return request({
    url: '/filesystem/file',
    method: 'post',
    params: { name }
  })
}

export const openFile = (name) => {
  return request({
    url: '/filesystem/file/open',
    method: 'post',
    params: { name }
  })
}

export const closeFile = (name) => {
  return request({
    url: '/filesystem/file/close',
    method: 'post',
    params: { name }
  })
}

export const readFileContent = (name) => {
  return request({
    url: '/filesystem/file/content',
    method: 'get',
    params: { name }
  })
}

export const writeFileContent = (name, content) => {
  return request({
    url: '/filesystem/file/content',
    method: 'post',
    params: { name },
    data: content, // 直接传字符串
    headers: {
      'Content-Type': 'text/plain'
    }
  })
}

export const deleteFile = (name) => {
  return request({
    url: '/filesystem/file',
    method: 'delete',
    data: { name }
  })
}