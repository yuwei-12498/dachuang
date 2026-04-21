# 社区重构说明文档

## 1. 本次重构目标

本次社区重构围绕“精品社区流”方案推进，目标不是简单补几个按钮，而是把原本零散的“公开到社区”能力升级为一套完整的社区内容体系：

- 帖子有明确的发布入口与发布内容
- 作者有删帖与作者置顶评论能力
- 管理员有全站置顶与社区删帖能力
- 社区首页、帖子详情页、历史页、结果页、后台治理页形成完整闭环
- UI 从信息拥挤、层级混乱，升级为更清爽、更适合阅读的卡片化旅行社区体验

---

## 2. 更新范围总览

### 前端

- 社区首页重构
- 社区详情页重构
- 结果页发布流程重构
- 历史页新增社区入口
- 后台新增社区内容治理页
- 新增发布弹窗、Hero、筛选条、置顶专区、卡片流、评论输入等组件

### 后端

- 社区帖子状态模型扩展
- 社区查询模型扩展
- 作者删帖 / 作者置顶评论能力
- 管理员社区分页治理能力
- 发布接口支持标题、分享语、主题标签

### 数据库

- `saved_itinerary` 增加社区治理字段
- 增加面向社区查询的索引

---

## 3. 功能上具体更新了什么

### 3.1 发帖与发布流程升级

#### 更新位置

- `frontend/src/views/Result.vue`
- `frontend/src/components/community/PublishRouteDialog.vue`
- `frontend/src/views/History.vue`
- `backend/src/main/java/com/citytrip/service/application/itinerary/SavedItineraryCommandService.java`
- `backend/src/main/java/com/citytrip/model/dto/PublicStatusReqDTO.java`

#### 体现方式

- 原来的“公开到社区”被升级为“发布路线帖”
- 发布时不再只改一个公开状态，而是补充：
  - 帖子标题
  - 分享语
  - 主题标签
  - 选中的路线版本
- `Result.vue` 中新增发布弹窗联动：
  - 未公开时点击按钮直接打开发布弹窗
  - 已公开时点击按钮走“撤回社区展示”确认流
- 支持通过 `/result?id=xxx&publish=1` 自动打开发布弹窗
- 发布成功后，支持直接跳转帖子详情或回到社区大厅
- 历史页为未公开路线新增“发布路线帖”入口

---

### 3.2 作者权限补齐

#### 更新位置

- `backend/src/main/java/com/citytrip/service/application/community/CommunityInteractionService.java`
- `backend/src/main/java/com/citytrip/controller/CommunityController.java`
- `frontend/src/views/CommunityDetail.vue`

#### 体现方式

- 作者可以删除自己发布的帖子（软删除）
- 作者可以在自己帖子下，将一条一级评论置顶
- 社区详情页会根据后端返回的权限信息动态显示：
  - 删除帖子按钮
  - 置顶评论按钮
  - 作者置顶评论 banner

---

### 3.3 管理员权限补齐

#### 更新位置

- `backend/src/main/java/com/citytrip/service/AdminService.java`
- `backend/src/main/java/com/citytrip/service/impl/AdminServiceImpl.java`
- `backend/src/main/java/com/citytrip/controller/AdminController.java`
- `frontend/src/api/adminCommunity.js`
- `frontend/src/views/admin/CommunityManage.vue`
- `frontend/src/views/admin/AdminLayout.vue`
- `frontend/src/router/index.js`

#### 体现方式

- 管理员可分页查看社区帖子
- 管理员可按关键词、置顶状态、删除状态筛选帖子
- 管理员可执行：
  - 全站置顶
  - 取消全站置顶
  - 删除帖子
- 后台导航新增“社区内容治理”入口
- 社区详情页中，管理员也可直接在帖子详情中执行全站置顶

---

### 3.4 社区首页重构

#### 更新位置

- `frontend/src/views/Community.vue`
- `frontend/src/components/community/CommunityHero.vue`
- `frontend/src/components/community/CommunityPinnedSection.vue`
- `frontend/src/components/community/CommunityFilterBar.vue`
- `frontend/src/components/community/CommunityFeedCard.vue`

#### 体现方式

- 社区首页改为“Hero + 筛选条 + 顶部置顶专区 + 双列动态流”结构
- 全站置顶帖子单独放在顶部专区，不与普通流混排
- 普通帖子区采用双列卡片流，更适合旅行路线内容浏览
- 支持按：
  - 最新 / 热门排序
  - 关键词搜索
  - 主题标签筛选
- 卡片内容信息更聚焦：封面、标题、分享语、主题标签、时长、预算、互动数据

---

### 3.5 社区详情页重构

#### 更新位置

- `frontend/src/views/CommunityDetail.vue`
- `frontend/src/components/community/CommentComposer.vue`

#### 体现方式

- 详情页改为“顶部大图 Hero + 主内容列 + 侧边栏”结构
- 主内容包含：
  - 路线摘要
  - 作者置顶评论
  - 路线时间线
  - 评论讨论区
- 评论区支持：
  - 评论
  - 回复
  - 作者置顶评论
- 详情页按钮按权限动态展示，避免游客看到无效操作
- 修复了管理员全站置顶成功提示反向显示的问题

---

### 3.6 结果页与历史页重构

#### 更新位置

- `frontend/src/views/Result.vue`
- `frontend/src/views/History.vue`

#### 体现方式

##### Result.vue

- 重写页面文案，清除原有乱码问题
- 保留路线结果页的方案对比、时间线、地图、海报导出能力
- 新增社区状态说明卡
- 新增“查看社区帖子”入口
- 新增自动打开发布弹窗的发布链路

##### History.vue

- 每条路线增加社区状态识别
- 已发布路线显示“查看帖子”按钮
- 未发布路线显示“发布路线帖”按钮
- 操作区从单一路线查看，升级为“查看路线 / 发布或查看帖子 / 继续调整”的闭环操作

---

## 4. 后端数据与接口更新在哪里

### 4.1 数据模型扩展

#### 更新位置

- `backend/src/main/java/com/citytrip/model/entity/SavedItinerary.java`
- `backend/sql/upgrade_community_redesign_20260421.sql`

#### 新增字段

- `isDeleted`
- `deletedAt`
- `deletedBy`
- `isGlobalPinned`
- `globalPinnedAt`
- `globalPinnedBy`
- `pinnedCommentId`

#### 体现方式

这些字段让社区帖子具备：

- 软删除能力
- 全站置顶能力
- 作者置顶评论能力
- 清晰的治理追踪信息

---

### 4.2 社区查询模型扩展

#### 更新位置

- `backend/src/main/java/com/citytrip/service/application/community/CommunityItineraryQueryService.java`
- `backend/src/main/java/com/citytrip/model/vo/CommunityItineraryVO.java`
- `backend/src/main/java/com/citytrip/model/vo/CommunityItineraryDetailVO.java`
- `backend/src/main/java/com/citytrip/model/vo/CommunityCommentVO.java`
- `backend/src/main/java/com/citytrip/model/vo/CommunityItineraryPageVO.java`

#### 体现方式

- 列表页返回 `pinnedRecords` + 普通 `records`
- 支持 `latest/hot` 排序、关键词、主题筛选
- 详情页返回治理与权限信息：
  - `canDelete`
  - `canPinComment`
  - `canManage`
  - `globalPinned`
  - `pinnedCommentId`
  - `pinnedComment`
- 评论返回 `pinned` / `canPin`

---

### 4.3 管理端 VO 与接口扩展

#### 更新位置

- `backend/src/main/java/com/citytrip/model/vo/AdminCommunityPostVO.java`
- `backend/src/main/java/com/citytrip/controller/AdminController.java`
- `backend/src/main/java/com/citytrip/service/impl/AdminServiceImpl.java`

#### 体现方式

后台社区治理页现在拿到的是面向治理的帖子视图，包含：

- 帖子标题
- 作者信息
- 封面图
- 分享语
- 主题标签
- 互动数据
- 是否全站置顶
- 是否已删除
- 更新时间 / 置顶时间

---

## 5. UI 重构具体体现在哪里

### 原问题

- 信息过于紧凑
- 阅读顺序不清晰
- 按钮功能杂糅
- 缺少社区氛围和内容感

### 本次优化后的体现

- 卡片圆角、留白、信息分层更清晰
- 社区首页变成可浏览的内容流，而不是列表堆叠
- 详情页更像“可读的路线帖”，不是纯数据展示
- 发布动作不再突兀，而是有明确步骤和预览
- 后台治理页从通用后台风格中独立出“社区治理”模块，角色边界更明确

---

## 6. 新增 / 重点修改文件清单

### 前端

- `frontend/src/views/Community.vue`
- `frontend/src/views/CommunityDetail.vue`
- `frontend/src/views/Result.vue`
- `frontend/src/views/History.vue`
- `frontend/src/views/admin/AdminLayout.vue`
- `frontend/src/views/admin/CommunityManage.vue`
- `frontend/src/components/community/CommunityHero.vue`
- `frontend/src/components/community/CommunityPinnedSection.vue`
- `frontend/src/components/community/CommunityFilterBar.vue`
- `frontend/src/components/community/CommunityFeedCard.vue`
- `frontend/src/components/community/PublishRouteDialog.vue`
- `frontend/src/components/community/CommentComposer.vue`
- `frontend/src/api/adminCommunity.js`
- `frontend/src/api/itinerary.js`
- `frontend/src/router/index.js`

### 后端

- `backend/src/main/java/com/citytrip/model/entity/SavedItinerary.java`
- `backend/src/main/java/com/citytrip/model/dto/PublicStatusReqDTO.java`
- `backend/src/main/java/com/citytrip/model/vo/CommunityItineraryVO.java`
- `backend/src/main/java/com/citytrip/model/vo/CommunityItineraryDetailVO.java`
- `backend/src/main/java/com/citytrip/model/vo/CommunityCommentVO.java`
- `backend/src/main/java/com/citytrip/model/vo/CommunityItineraryPageVO.java`
- `backend/src/main/java/com/citytrip/model/vo/AdminCommunityPostVO.java`
- `backend/src/main/java/com/citytrip/service/application/community/CommunityItineraryQueryService.java`
- `backend/src/main/java/com/citytrip/service/application/community/CommunityInteractionService.java`
- `backend/src/main/java/com/citytrip/service/application/itinerary/SavedItineraryCommandService.java`
- `backend/src/main/java/com/citytrip/service/impl/AdminServiceImpl.java`
- `backend/src/main/java/com/citytrip/controller/AdminController.java`
- `backend/src/main/java/com/citytrip/controller/CommunityController.java`
- `backend/src/main/java/com/citytrip/controller/ItineraryController.java`
- `backend/sql/upgrade_community_redesign_20260421.sql`

---

## 7. 验证结果

### 前端验证

执行目录：`frontend`

命令：

```bash
npm run build
```

结果：

- 构建成功
- 新增页面 `CommunityManage`、重写后的 `Result`、改造后的 `History` 均已进入产物

### 后端验证

执行目录：`backend`

命令：

```bash
mvn test "-Dtest=CommunityItineraryQueryServiceTest,CommunityInteractionServiceTest,SavedItineraryCommandServiceTest,AdminServiceImplCommunityTest"
```

结果：

- `BUILD SUCCESS`
- 共执行 8 个社区相关测试
- 0 Failures / 0 Errors / 0 Skipped

---

## 8. 最终效果总结

这次社区重构，核心不是“换了个皮肤”，而是完成了三件事：

1. **把发布流程产品化了**：从简单公开，升级为真正的路线帖发布。  
2. **把权限体系补齐了**：作者可删帖、可置顶自己帖子的评论；管理员可删帖、可全站置顶。  
3. **把社区体验做顺了**：首页可浏览、详情可阅读、历史页可回流、后台可治理。  

现在这套社区已经具备继续做内容增长、运营活动和管理扩展的基础。 