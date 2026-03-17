import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(router)
app.use(ElementPlus)

app.config.errorHandler = (err, instance, info) => {
  const errorDiv = document.createElement('div');
  errorDiv.style = "color:red;font-size:20px;padding:50px;position:fixed;top:0;left:0;z-index:99999;background:white;width:100%;height:100%;overflow:auto;"
  errorDiv.innerHTML = '<h1>Vue Config Error</h1><pre>' + err.message + '\n\n' + err.stack + '\n\n' + info + '</pre>';
  document.body.appendChild(errorDiv);
  console.error(err);
}

router.onError((err) => {
  const errorDiv = document.createElement('div');
  errorDiv.style = "color:blue;font-size:20px;padding:50px;position:fixed;top:0;left:0;z-index:99998;background:lightgray;width:100%;height:100%;overflow:auto;"
  errorDiv.innerHTML = '<h1>Vue Router Error</h1><pre>' + err.message + '\n\n' + err.stack + '</pre>';
  document.body.appendChild(errorDiv);
  console.error(err);
})

app.mount('#app')
