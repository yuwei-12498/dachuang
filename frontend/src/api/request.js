import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 Axios 实例，复用配置
const service = axios.create({
  baseURL: '', // Vite 的 proxy 会处理 /api 前缀拦截
  timeout: 15000
})

// 响应拦截器：统一处理后端返回格式 Result<T>
service.interceptors.response.use(
  response => {
    const res = response.data
    // 约定的成功状态码为 200
    if (res.code !== 200) {
      ElMessage.error(res.msg || '服务器错误')
      return Promise.reject(new Error(res.msg || 'Error'))
    } else {
      // 成功则直接抛出包裹的 data
      return res.data
    }
  },
  error => {
    console.error('API Error: ', error)
    ElMessage.error('网络连接异常或后端服务未启动')
    return Promise.reject(error)
  }
)

export default service
