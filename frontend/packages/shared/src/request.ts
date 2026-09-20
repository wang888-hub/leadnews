import axios, { AxiosError, type AxiosInstance } from 'axios'
import { clearAuth, readToken } from './auth'
import type { ResponseResult } from './types'

export class ApiError extends Error { constructor(public status:number,message:string,public traceId?:string){super(message)} }
export const errorMessage=(status:number,fallback='请求失败')=>({400:fallback,401:'登录已失效，请重新登录',403:'权限不足，无法执行此操作',409:fallback||'当前业务状态冲突',429:'请求过于频繁，请稍后重试',500:'服务处理异常，请稍后重试',502:'上游服务响应异常',503:'服务暂不可用，请稍后重试'}[status]||fallback)
export function createRequest(baseURL=import.meta.env.VITE_API_BASE_URL as string):AxiosInstance {
 const client=axios.create({baseURL,timeout:15000})
 client.interceptors.request.use(config=>{const token=readToken();if(token)config.headers.Authorization=`Bearer ${token}`;return config})
 client.interceptors.response.use(response=>response, (error:AxiosError<ResponseResult<unknown>>)=>{
   const status=error.response?.status||0;const body=error.response?.data;const message=errorMessage(status,body?.message||error.message)
   if(status===401){clearAuth();if(location.pathname!=='/login')location.assign(`/login?redirect=${encodeURIComponent(location.pathname+location.search)}`)}
   return Promise.reject(new ApiError(status,message,body?.traceId))
 })
 return client
}
export const unwrap=<T>(response:{data:ResponseResult<T>})=>response.data.data
