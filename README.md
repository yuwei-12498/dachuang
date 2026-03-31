# 行城有数

基于 `Vue 3 + Spring Boot 3 + MyBatis-Plus + MySQL` 的城市旅游智能行程规划系统，当前聚焦成都一日游/短途游场景。

系统核心不是单纯调用大模型生成文案，而是采用“规则与优化算法主导 + LLM 辅助问答”的混合方案：

- 路线生成、重排、替换由后端候选点筛选、评分和路径搜索完成
- 大模型主要承担聊天问答与解释增强
- 当模型配置异常时，系统可以明确返回错误原因，便于排障

## 当前版本亮点

- 多方案路线生成：一次返回 3 条不同风格的候选路线，支持对比切换
- 解释型推荐：不仅告诉用户推荐什么，还会解释为什么选这条、不优先选别的
- 地图路线展示：结果页内嵌地图，路线与时间轴联动展示
- 动态重排与点位替换：支持“换一版路线”和针对单个站点的替换
- 历史行程与收藏：收藏当前选中的方案，并支持自定义路线名称
- RESTful 接口：认证、行程、景点、聊天接口均已按资源风格整理
- 聊天状态检查：提供聊天服务状态接口，便于判断当前是否具备真实模型调用条件

## 技术栈

### 前端

- Vue 3
- Vue Router 4
- Element Plus
- Axios
- Leaflet
- Vite

### 后端

- Spring Boot 3.1.5
- MyBatis-Plus 3.5.4.1
- MySQL 8.x
- Lombok

## 系统功能

### 用户侧

- 注册、登录、退出、获取当前登录用户
- 生成智能行程
- 查看多方案对比结果
- 地图路线展示
- 重排行程
- 替换单个景点
- 查看景点详情与营业状态
- AI 问答
- 历史行程查看
- 收藏/取消收藏路线
- 收藏路线自定义命名

### 系统侧

- POI 营业状态动态补充
- 候选景点筛选与评分
- 本地出行时间估算
- 路线搜索与排序
- 会话鉴权
- 聊天模型路由与状态检测

## 项目结构

```text
.
├─backend
│  ├─sql
│  │  ├─init.sql
│  │  ├─upgrade_dynamic_itinerary.sql
│  │  ├─upgrade_itinerary_custom_title.sql
│  │  └─upgrade_itinerary_history_favorite.sql
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

## 运行环境

- Node.js 18+
- npm 9+
- JDK 17
- Maven 3.9+
- MySQL 8.x

## 数据库初始化

首次初始化执行：

```sql
source F:/dachuang/backend/sql/init.sql;
```

如果你的数据库是旧版本，还需要按实际历史执行升级脚本：

- `backend/sql/upgrade_dynamic_itinerary.sql`
- `backend/sql/upgrade_itinerary_history_favorite.sql`
- `backend/sql/upgrade_itinerary_custom_title.sql`

## 配置说明

后端使用环境变量读取敏感信息，避免把数据库密码和模型密钥直接写进仓库。

当前 `backend/src/main/resources/application.yml` 的关键配置如下：

```yaml
server:
  port: 8081

spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://127.0.0.1:3306/city_trip_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}

app:
  cors:
    allowed-origin-patterns: ${APP_CORS_ALLOWED_ORIGIN_PATTERNS:http://localhost:3000,http://127.0.0.1:3000}

llm:
  provider: ${LLM_PROVIDER:real}
  fallback-to-mock: ${LLM_FALLBACK_TO_MOCK:false}
  timeout-seconds: ${LLM_TIMEOUT_SECONDS:20}
  openai:
    enabled: ${LLM_OPENAI_ENABLED:true}
    api-key: ${OPENAI_API_KEY:}
    base-url: ${OPENAI_BASE_URL:https://api.555615.xyz/v1}
    model: ${OPENAI_MODEL:gpt-5.1}
    temperature: ${OPENAI_TEMPERATURE:0.7}
```

### 建议的本地环境变量

Windows PowerShell 示例：

```powershell
$env:DB_PASSWORD="你的数据库密码"
$env:OPENAI_API_KEY="你的模型密钥"
$env:OPENAI_BASE_URL="你的模型网关地址"
$env:OPENAI_MODEL="你的模型名"
```

### LLM 路由说明

- `LLM_PROVIDER=mock`：只使用本地 Mock
- `LLM_PROVIDER=real`：只使用真实模型
- `LLM_PROVIDER=auto`：优先真实模型，不可用时回退到 Mock
- `LLM_FALLBACK_TO_MOCK=true`：真实模型运行失败时自动降级

如果你希望直接暴露真实失败原因，建议：

```text
LLM_PROVIDER=real
LLM_FALLBACK_TO_MOCK=false
```

## 启动方式

### 启动后端

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

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认开发环境：

- 前端地址：`http://127.0.0.1:3000`
- 后端地址：`http://127.0.0.1:8081`
- 前端通过 Vite 代理转发 `/api`

## 前端页面

- `/`：首页
- `/auth`：登录/注册
- `/result`：结果页
- `/history`：历史行程
- `/detail/:id`：景点详情

## RESTful API 概览

### 认证

- `POST /api/users`：注册
- `POST /api/sessions`：登录
- `DELETE /api/sessions/current`：退出登录
- `GET /api/users/me`：获取当前用户

### 行程

- `POST /api/itineraries`：生成行程
- `GET /api/itineraries`：行程列表
- `GET /api/itineraries/{id}`：行程详情
- `PATCH /api/itineraries/{id}/replan`：换一版路线
- `PATCH /api/itineraries/{id}/nodes/{poiId}/replacement`：替换某一站点
- `PUT /api/itineraries/{id}/favorite`：收藏并命名当前选中方案
- `DELETE /api/itineraries/{id}/favorite`：取消收藏

### 景点

- `GET /api/pois`：景点列表
- `GET /api/pois/{id}`：景点详情

### 聊天

- `POST /api/chat/messages`：发送聊天消息
- `GET /api/chat/messages/status`：查看聊天服务状态

## 鉴权说明

系统当前基于 `HttpSession` 做登录态维护，前端请求默认携带 `withCredentials`。

需要登录后才能访问的能力：

- 行程生成
- 行程详情查看
- 重排行程
- 景点替换
- AI 问答
- 历史与收藏

## 构建验证

本项目最近一次构建验证命令：

```bash
cd backend
mvn -q -DskipTests package

cd ../frontend
npm run build
```

## 已知工程化改进

- 配置文件已改为环境变量读取，避免敏感信息直接入库
- CORS 已改为按来源模式配置，不再对任意来源开放凭证
- 行程收藏支持自定义命名
- 聊天服务新增状态检查接口
- Vite 已做基础分包，首屏主入口体积已下降

## 仍可继续提升的方向

- 为聊天服务增加一个前端可见的状态面板，直接展示当前模型配置和连通性
- Element Plus 进一步按需引入，继续压缩前端包体积
- 增加接口测试和前端关键流程测试
- 增加模型连通性主动探测或管理端调试面板
- 增加分享页、导出海报/PDF 和数据看板

## 说明

- 项目默认样例数据为成都 POI
- 如果真实模型配置不可用，聊天能力表现取决于当前 `LLM_PROVIDER` 与 `LLM_FALLBACK_TO_MOCK` 配置
- 提交软著或公开仓库前，建议再次检查环境变量、数据库账号和第三方密钥配置
