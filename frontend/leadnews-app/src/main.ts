import {createApp} from 'vue';import {createPinia} from 'pinia';import ElementPlus from 'element-plus';import 'element-plus/dist/index.css';import './style.css';import App from './App.vue';import {router} from './router';
const app=createApp(App);app.config.errorHandler=(error)=>{if(import.meta.env.DEV)console.error(error)};app.use(createPinia()).use(router).use(ElementPlus).mount('#app')
