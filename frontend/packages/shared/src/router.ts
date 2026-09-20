import type { Router } from 'vue-router'
import { useAuthStore } from './auth'
import type { UserType } from './types'
export function installAuthGuard(router:Router,expected:UserType){router.beforeEach(to=>{const auth=useAuthStore();if(to.meta.public)return true;if(!auth.isAuthenticated)return {path:'/login',query:{redirect:to.fullPath}};if(auth.userType!==expected)return {path:'/forbidden'};return true})}
