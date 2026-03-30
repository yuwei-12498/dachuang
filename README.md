# 行“城”有数

基于 Vue 3、Spring Boot 3、MyBatis-Plus 和 MySQL 的智慧旅游行程推荐系统。  
系统面向普通用户提供成都本地行程生成、路线重排、点位替换、AI 问答和基于 `HttpSession` 的用户注册登录能力。

## 项目亮点

- AI 行程推荐：根据时间、预算、主题偏好、同行人群、步行强度、雨天/夜游偏好生成推荐路线
- 行程动态调整：支持一键重排路线，也支持进入地点详情后替换为相似点位
- AI 出行助手：首页助手和悬浮聊天框共享同一份对话记录
- 用户会话体系：新增注册、登录、退出、当前登录态查询，使用 `Session` 鉴权
- 登录后使用核心能力：AI 问答、行程生成、结果页和详情页都要求先登录
- 更统一的前端界面：登录页、首页、结果页、详情页采用统一的浅色卡片视觉风格

## 技术栈

### 前端

- Vue 3
- Vue Router 4
- Element Plus
- Axios
- Vite

### 后端

- Spring Boot 3.1.5
- MyBatis-Plus 3.5.4.1
- MySQL 8.x
- Lombok

## 目录结构

```text
.
├─backend
│  ├─sql
│  │  └─init.sql
│  └─src/main/java/com/citytrip
│     ├─annotation
│     ├─common
│     ├─config
│     ├─controller
│     ├─mapper
│     ├─model
│     ├─service
│     ├─util
│     └─CityTripApplication.java
├─frontend
│  └─src
│     ├─api
│     ├─components
│     ├─router
│     ├─store
│     └─views
└─README.md
```

## 当前功能

### 游客侧

- 首页浏览系统介绍
- 注册与登录
- AI 问答
- 行程生成
- 行程结果总览
- 智能重排路线
- 点位详情查看
- 相似点位替换

### 系统侧

- 本地 POI 数据筛选与排序
- 本地出行时间估算
- LLM 文案生成与问答
- Mock / Real / Auto 三种 LLM 路由模式
- `Session` 登录态拦截

## 本次更新内容

### 后端

- 新增用户表 `trip_user`
- 新增 `AuthController`
- 新增 `UserService`、`UserMapper`、`PasswordUtils`
- 新增 `@LoginRequired`、`AuthInterceptor`、`AuthWebMvcConfig`
- 行程生成、行程重排、点位替换、AI 问答接口改为登录后可用

### 前端

- 新增登录/注册页 `frontend/src/views/Auth.vue`
- 新增认证 API 与登录态 store
- 首页生成按钮与 AI 助手增加登录校验
- 结果页、详情页加入路由鉴权
- 首页 AI 面板与悬浮聊天框共享聊天记录
- 首页布局、结果页、详情页 UI 重新整理，交互更贴近普通用户

## 数据库初始化

项目初始化 SQL 位于：

- `backend/sql/init.sql`

该脚本会：

- 创建数据库 `city_trip_db`
- 创建用户表 `trip_user`
- 创建景点表 `poi`
- 插入一批成都本地 POI 测试数据

执行方式示例：

```sql
source F:/dachuang/backend/sql/init.sql;
```

## 后端配置

仓库中不提交 `yml` 配置文件，你需要自行创建：

- `backend/src/main/resources/application.yml`

示例配置：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/city_trip_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

llm:
  provider: auto
  fallback-to-mock: true
  timeout-seconds: 20
  openai:
    enabled: true
    api-key: your_api_key
    base-url: https://api.555615.xyz/v1
    model: gpt-5.1
    temperature: 0.7
```

`llm.provider` 支持：

- `mock`：只走本地 Mock
- `real`：只走真实模型
- `auto`：优先真实模型，失败后回退 Mock

## 运行方式

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

或者：

```bash
cd backend
mvn -q -DskipTests package
java -jar target/citytrip-backend-0.0.1-SNAPSHOT.jar
```

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认开发模式下，前端通过浏览器访问 Vite 地址，后端使用 `8080` 端口。

## 认证说明

系统采用 `HttpSession` 鉴权。

### 认证接口

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`

### 需要登录的能力

- `POST /api/itinerary/generate`
- `POST /api/itinerary/replan`
- `POST /api/itinerary/replace`
- `POST /api/chat/qa`
- 前端 `/result`
- 前端 `/detail/:id`

首页介绍仍可匿名浏览，但 AI 问答和行程推荐必须登录。

## 构建验证

本次提交前已完成：

```bash
cd frontend
npm run build

cd ../backend
mvn -q -DskipTests package
```

## 说明

- 前端 `dist` 和 `node_modules` 不再纳入版本管理
- 所有数据库与 LLM 密钥配置需要在本地自行补充
- 当前项目以成都 POI 数据为默认样例
