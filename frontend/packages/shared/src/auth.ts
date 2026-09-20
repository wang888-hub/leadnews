import { defineStore } from 'pinia'
import type { UserType } from './types'

const key = 'leadnews.auth'
interface AuthState { token:string; user:Record<string,unknown>|null; userType:UserType|null }
function restore():AuthState { try { return JSON.parse(localStorage.getItem(key) || '') as AuthState } catch { return {token:'',user:null,userType:null} } }
export const useAuthStore = defineStore('auth', {
  state: restore,
  getters: { isAuthenticated: s => Boolean(s.token) },
  actions: {
    login(token:string,user:Record<string,unknown>,userType:UserType){this.token=token;this.user=user;this.userType=userType;this.persist()},
    logout(){this.token='';this.user=null;this.userType=null;localStorage.removeItem(key)},
    persist(){localStorage.setItem(key,JSON.stringify({token:this.token,user:this.user,userType:this.userType}))}
  }
})
export function readToken(){try{return (JSON.parse(localStorage.getItem(key)||'{}') as AuthState).token||''}catch{return ''}}
export function clearAuth(){localStorage.removeItem(key)}
