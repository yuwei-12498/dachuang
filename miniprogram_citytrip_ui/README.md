# 微信小程序前端迁移版

本目录是将原 `frontend` 网页前端迁移后的原生微信小程序实现（`WXML + WXSS + JS`）。

## 目录说明

- `app.js / app.json / app.wxss`：小程序入口与全局样式
- `config/env.js`：后端服务域名配置
- `utils/request.js`：`wx.request` 统一封装（自动带 `Authorization`、401 处理、错误提示）
- `api/*.js`：与原网页一致的接口定义（路径、方法、参数保持不变）
- `store/*.js`：登录态、行程快照、聊天状态
- `pages/*`：页面迁移结果
- `components/chat-widget`：全局聊天浮窗
- `components/home-ai-panel`：首页右侧 AI 面板

## 已迁移页面

- `pages/home/index`：首页（Hero + 行程表单 + 场景/功能/示例模块）
- `pages/auth/index`：登录/注册页
- `pages/result/index`：行程结果页（候选方案、收藏、重排、地图、时间线）
- `pages/history/index`：历史与收藏页
- `pages/detail/index`：点位详情页（替换当前站点）
- `pages/admin-users/index`：后台用户管理
- `pages/admin-pois/index`：后台 POI 管理

## 后端接口保持不变

已按原项目改为 `wx.request` 调用，核心接口包括：

- `/api/users` `POST`
- `/api/sessions` `POST`
- `/api/sessions/current` `DELETE`
- `/api/users/me` `GET`
- `/api/itineraries` `POST/GET`
- `/api/itineraries/{id}` `GET`
- `/api/itineraries/{id}/favorite` `PUT/DELETE`
- `/api/itineraries/{id}/replan` `PATCH`
- `/api/itineraries/{id}/nodes/{targetPoiId}/replacement` `PATCH`
- `/api/pois` `GET`
- `/api/pois/{id}` `GET`
- `/api/chat/messages` `POST`
- `/api/chat/messages/status` `GET`
- `/api/admin/users` `GET`
- `/api/admin/users/{id}/status` `PATCH`
- `/api/admin/pois` `GET/PUT`

## 运行前配置

1. 打开 `config/env.js`，把 `apiBaseUrl` 改成你的后端可访问地址（例如线上 HTTPS 域名）。
2. 在微信开发者工具里把该域名加入小程序 `request` 合法域名。
3. 导入目录 `miniprogram_citytrip_ui` 作为小程序项目运行。
