import axios from 'axios'

// 创建axios实例
const service = axios.create({
  baseURL: 'http://localhost:8080', // API的基础URL
  timeout: 5000 // 请求超时时间
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    return config
  },
  error => {
    console.error('请求错误：', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('响应错误：', error)
    return Promise.reject(error)
  }
)

export default service