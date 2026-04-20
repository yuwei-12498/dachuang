# Community Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成社区精品化重构，补齐作者/管理员治理能力，重构社区首页与详情页，并把“公开到社区”升级为“发布路线帖”。

**Architecture:** 后端继续沿用 `controller -> service -> application service -> repository/mapper` 结构，在 `saved_itinerary` 上补齐社区治理字段，并通过社区查询/交互服务集中输出展示与权限信息。前端继续沿用 Vue 3 + Element Plus，在社区首页、详情页、结果页、历史页和后台管理页上拆分出更清晰的 UI 结构与状态流，优先保证 P0 权限闭环和核心浏览体验。

**Tech Stack:** Spring Boot, MyBatis-Plus, Vue 3, Element Plus, Axios, JUnit 5, Mockito

---

## File Map

### Backend

**Modify:**
- `F:/dachuang/backend/src/main/java/com/citytrip/model/entity/SavedItinerary.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/mapper/SavedItineraryMapper.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/service/persistence/itinerary/SavedItineraryRepository.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/service/ItineraryService.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/service/impl/ItineraryServiceImpl.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/service/application/community/CommunityItineraryQueryService.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/service/application/community/CommunityInteractionService.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/service/application/itinerary/SavedItineraryCommandService.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/controller/ItineraryController.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/service/AdminService.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/service/impl/AdminServiceImpl.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/controller/AdminController.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/model/dto/PublicStatusReqDTO.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/model/vo/CommunityItineraryVO.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/model/vo/CommunityItineraryDetailVO.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/model/vo/CommunityCommentVO.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/assembler/ItinerarySummaryAssembler.java`
- `F:/dachuang/backend/src/test/java/com/citytrip/service/application/community/CommunityItineraryQueryServiceTest.java`
- `F:/dachuang/backend/src/test/java/com/citytrip/service/application/itinerary/SavedItineraryCommandServiceTest.java`

**Create:**
- `F:/dachuang/backend/src/main/java/com/citytrip/model/dto/CommunityCommentPinReqDTO.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/model/dto/AdminCommunityPostQueryDTO.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/model/vo/AdminCommunityPostVO.java`
- `F:/dachuang/backend/src/main/java/com/citytrip/model/vo/AdminCommunityPostPageVO.java`
- `F:/dachuang/backend/sql/upgrade_community_redesign_20260421.sql`
- `F:/dachuang/backend/src/test/java/com/citytrip/service/application/community/CommunityInteractionServiceTest.java`
- `F:/dachuang/backend/src/test/java/com/citytrip/service/impl/AdminServiceImplCommunityTest.java`

### Frontend

**Modify:**
- `F:/dachuang/frontend/src/api/itinerary.js`
- `F:/dachuang/frontend/src/router/index.js`
- `F:/dachuang/frontend/src/views/Community.vue`
- `F:/dachuang/frontend/src/views/CommunityDetail.vue`
- `F:/dachuang/frontend/src/views/Result.vue`
- `F:/dachuang/frontend/src/views/History.vue`
- `F:/dachuang/frontend/src/views/admin/AdminLayout.vue`

**Create:**
- `F:/dachuang/frontend/src/api/adminCommunity.js`
- `F:/dachuang/frontend/src/components/community/CommunityHero.vue`
- `F:/dachuang/frontend/src/components/community/CommunityPinnedSection.vue`
- `F:/dachuang/frontend/src/components/community/CommunityFilterBar.vue`
- `F:/dachuang/frontend/src/components/community/CommunityFeedCard.vue`
- `F:/dachuang/frontend/src/components/community/PublishRouteDialog.vue`
- `F:/dachuang/frontend/src/components/community/CommentComposer.vue`
- `F:/dachuang/frontend/src/views/admin/CommunityManage.vue`

### Docs

**Create:**
- `F:/dachuang/docs/community/community-redesign-changelog.md`

---

### Task 1: 补齐社区帖子状态与查询模型

**Files:**
- Create: `F:/dachuang/backend/sql/upgrade_community_redesign_20260421.sql`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/model/entity/SavedItinerary.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/mapper/SavedItineraryMapper.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/service/persistence/itinerary/SavedItineraryRepository.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/model/vo/CommunityItineraryVO.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/model/vo/CommunityItineraryDetailVO.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/model/vo/CommunityCommentVO.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/assembler/ItinerarySummaryAssembler.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/service/application/community/CommunityItineraryQueryService.java`
- Test: `F:/dachuang/backend/src/test/java/com/citytrip/service/application/community/CommunityItineraryQueryServiceTest.java`

- [ ] **Step 1: 先写查询层失败测试，覆盖“删除帖不可见、置顶信息可返回”**

```java
@Test
void getPublicDetailIncludesPinnedAndPermissionFlags() {
    SavedItinerary entity = new SavedItinerary();
    entity.setId(9L);
    entity.setUserId(5L);
    entity.setIsPublic(1);
    entity.setIsDeleted(0);
    entity.setIsGlobalPinned(1);
    entity.setPinnedCommentId(101L);

    when(repository.requirePublic(9L)).thenReturn(entity);
    when(commentMapper.selectById(101L)).thenReturn(pinnedCommentEntity());

    CommunityItineraryDetailVO detail = service.getPublicDetail(9L, 5L);

    assertThat(detail.getGlobalPinned()).isTrue();
    assertThat(detail.getPinnedCommentId()).isEqualTo(101L);
    assertThat(detail.getCanDelete()).isTrue();
    assertThat(detail.getCanPinComment()).isTrue();
}
```

- [ ] **Step 2: 跑测试，确认它因字段/逻辑缺失而失败**

Run: `mvn test -Dtest=CommunityItineraryQueryServiceTest`
Expected: FAIL，提示 `SavedItinerary` / `CommunityItineraryDetailVO` 缺少新字段或断言不成立。

- [ ] **Step 3: 增加数据库字段与实体字段**

```sql
ALTER TABLE `saved_itinerary`
  ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN `deleted_at` DATETIME NULL,
  ADD COLUMN `deleted_by` BIGINT NULL,
  ADD COLUMN `is_global_pinned` TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN `global_pinned_at` DATETIME NULL,
  ADD COLUMN `global_pinned_by` BIGINT NULL,
  ADD COLUMN `pinned_comment_id` BIGINT NULL;
```

```java
private Integer isDeleted;
private LocalDateTime deletedAt;
private Long deletedBy;
private Integer isGlobalPinned;
private LocalDateTime globalPinnedAt;
private Long globalPinnedBy;
private Long pinnedCommentId;
```

- [ ] **Step 4: 在 repository 中统一过滤已删除帖子，并拆出置顶/普通流查询**

```java
public SavedItinerary requirePublic(Long itineraryId) {
    SavedItinerary entity = itineraryId == null ? null : savedItineraryMapper.selectById(itineraryId);
    if (entity == null || !isPublicVisible(entity)) {
        throw new NotFoundException("Public itinerary not found");
    }
    return entity;
}

public List<SavedItinerary> listPublicPinned(int limit) { ... }
public List<SavedItinerary> listPublicFeed(int page, int size, String sort, String keyword, String theme) { ... }
```

- [ ] **Step 5: 扩展社区 VO，补齐 pinned / permission / governance 信息**

```java
private Boolean globalPinned;
private Long pinnedCommentId;
private Boolean canDelete;
private Boolean canPinComment;
private Boolean canManage;
private Boolean pinned;
private Boolean canPin;
```

- [ ] **Step 6: 在查询服务中同时返回 pinned 区和普通 feed 区，并给 detail 注入权限状态**

```java
result.setPinnedRecords(loadPinnedSummaries());
result.setRecords(loadFeedSummaries(page, size, sort, keyword, theme));

detail.setCanDelete(canDelete(entity, currentUserId));
detail.setCanPinComment(canPinComment(entity, currentUserId));
detail.setCanManage(isAdmin(currentUserId));
```

- [ ] **Step 7: 跑测试验证查询模型变绿**

Run: `mvn test -Dtest=CommunityItineraryQueryServiceTest`
Expected: PASS

- [ ] **Step 8: 提交查询模型底座**

```bash
git add F:/dachuang/backend/src/main/java/com/citytrip/model/entity/SavedItinerary.java \
        F:/dachuang/backend/src/main/java/com/citytrip/service/persistence/itinerary/SavedItineraryRepository.java \
        F:/dachuang/backend/src/main/java/com/citytrip/service/application/community/CommunityItineraryQueryService.java \
        F:/dachuang/backend/src/main/java/com/citytrip/model/vo/CommunityItineraryVO.java \
        F:/dachuang/backend/src/main/java/com/citytrip/model/vo/CommunityItineraryDetailVO.java \
        F:/dachuang/backend/src/main/java/com/citytrip/model/vo/CommunityCommentVO.java \
        F:/dachuang/backend/sql/upgrade_community_redesign_20260421.sql

git commit -m "feat: add community post governance state"
```

### Task 2: 完成作者侧删帖与置顶评论能力

**Files:**
- Create: `F:/dachuang/backend/src/main/java/com/citytrip/model/dto/CommunityCommentPinReqDTO.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/service/application/community/CommunityInteractionService.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/service/ItineraryService.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/service/impl/ItineraryServiceImpl.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/controller/ItineraryController.java`
- Test: `F:/dachuang/backend/src/test/java/com/citytrip/service/application/community/CommunityInteractionServiceTest.java`

- [ ] **Step 1: 先写作者能力测试，覆盖“只能删自己的帖子、只能置顶一级评论”**

```java
@Test
void pinCommentRejectsNestedReply() {
    when(repository.requireOwnedForUpdate(5L, 9L)).thenReturn(publicOwnedItinerary());
    when(commentMapper.selectById(101L)).thenReturn(replyComment());

    assertThatThrownBy(() -> service.pinComment(5L, 9L, 101L))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("only root comments can be pinned");
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn test -Dtest=CommunityInteractionServiceTest`
Expected: FAIL，提示 `pinComment` / `deletePost` 不存在。

- [ ] **Step 3: 增加作者删帖实现，使用软删除并清理置顶状态**

```java
@Transactional
public void deletePost(Long userId, Long itineraryId) {
    SavedItinerary entity = savedItineraryRepository.requireOwnedForUpdate(userId, itineraryId);
    entity.setIsDeleted(1);
    entity.setDeletedAt(LocalDateTime.now());
    entity.setDeletedBy(userId);
    entity.setIsPublic(0);
    entity.setIsGlobalPinned(0);
    entity.setPinnedCommentId(null);
    savedItineraryRepository.saveOrUpdate(entity);
    communityCacheInvalidationService.markDirty();
}
```

- [ ] **Step 4: 增加作者置顶评论能力**

```java
@Transactional
public CommunityItineraryDetailVO pinComment(Long userId, Long itineraryId, Long commentId) {
    SavedItinerary entity = savedItineraryRepository.requireOwnedForUpdate(userId, itineraryId);
    CommunityComment comment = requireRootComment(itineraryId, commentId);
    entity.setPinnedCommentId(comment.getId());
    savedItineraryRepository.saveOrUpdate(entity);
    return communityItineraryQueryService.getPublicDetail(itineraryId, userId);
}
```

- [ ] **Step 5: 暴露接口并给评论列表打上 `pinned` / `canPin`**

```java
@DeleteMapping("/community/{id}")
public ResponseEntity<Void> deleteCommunityPost(@PathVariable("id") Long id, HttpServletRequest request) { ... }

@PatchMapping("/community/{id}/comments/{commentId}/pin")
public CommunityItineraryDetailVO pinCommunityComment(...) { ... }
```

- [ ] **Step 6: 重新排序评论，确保置顶评论永远位于列表顶部**

```java
return roots.stream()
        .sorted(Comparator.comparing((CommunityCommentVO vo) -> !Boolean.TRUE.equals(vo.getPinned())))
        .toList();
```

- [ ] **Step 7: 跑测试**

Run: `mvn test -Dtest=CommunityInteractionServiceTest,SavedItineraryCommandServiceTest`
Expected: PASS

- [ ] **Step 8: 提交作者能力**

```bash
git add F:/dachuang/backend/src/main/java/com/citytrip/service/application/community/CommunityInteractionService.java \
        F:/dachuang/backend/src/main/java/com/citytrip/controller/ItineraryController.java \
        F:/dachuang/backend/src/main/java/com/citytrip/service/impl/ItineraryServiceImpl.java \
        F:/dachuang/backend/src/main/java/com/citytrip/service/ItineraryService.java \
        F:/dachuang/backend/src/test/java/com/citytrip/service/application/community/CommunityInteractionServiceTest.java

git commit -m "feat: support author community moderation actions"
```

### Task 3: 完成管理员社区治理接口与后台数据模型

**Files:**
- Create: `F:/dachuang/backend/src/main/java/com/citytrip/model/dto/AdminCommunityPostQueryDTO.java`
- Create: `F:/dachuang/backend/src/main/java/com/citytrip/model/vo/AdminCommunityPostVO.java`
- Create: `F:/dachuang/backend/src/main/java/com/citytrip/model/vo/AdminCommunityPostPageVO.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/service/AdminService.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/service/impl/AdminServiceImpl.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/controller/AdminController.java`
- Test: `F:/dachuang/backend/src/test/java/com/citytrip/service/impl/AdminServiceImplCommunityTest.java`

- [ ] **Step 1: 写管理员治理失败测试**

```java
@Test
void globalPinMarksPostAndReturnsUpdatedState() {
    SavedItinerary entity = publicPost();
    when(repository.requireExisting(9L)).thenReturn(entity);

    service.updateCommunityPostPin(99L, 9L, true);

    assertThat(entity.getIsGlobalPinned()).isEqualTo(1);
    assertThat(entity.getGlobalPinnedBy()).isEqualTo(99L);
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn test -Dtest=AdminServiceImplCommunityTest`
Expected: FAIL，提示 `AdminService` 缺少社区治理方法。

- [ ] **Step 3: 给 AdminService 增加社区帖子分页查询、全站置顶、删帖接口**

```java
Page<AdminCommunityPostVO> getCommunityPostPage(int page, int size, String keyword, Integer pinned, Integer deleted);
void updateCommunityPostPin(Long adminUserId, Long itineraryId, boolean pinned);
void deleteCommunityPost(Long adminUserId, Long itineraryId);
```

- [ ] **Step 4: 在实现层复用 repository 和 assembler，返回后台管理页需要的字段**

```java
vo.setId(entity.getId());
vo.setTitle(assembler.buildTitle(req, itinerary));
vo.setGlobalPinned(entity.getIsGlobalPinned() == 1);
vo.setDeleted(entity.getIsDeleted() == 1);
vo.setCommentCount(commentCountMap.getOrDefault(entity.getId(), 0L));
```

- [ ] **Step 5: 在控制器下新增管理接口**

```java
@GetMapping("/community/posts")
public Page<AdminCommunityPostVO> getCommunityPostPage(...) { ... }

@PatchMapping("/community/posts/{id}/pin")
public void updateCommunityPostPin(@PathVariable Long id, @RequestParam boolean pinned, HttpServletRequest request) { ... }

@DeleteMapping("/community/posts/{id}")
public void deleteCommunityPost(@PathVariable Long id, HttpServletRequest request) { ... }
```

- [ ] **Step 6: 跑测试**

Run: `mvn test -Dtest=AdminServiceImplCommunityTest`
Expected: PASS

- [ ] **Step 7: 提交管理员治理能力**

```bash
git add F:/dachuang/backend/src/main/java/com/citytrip/service/AdminService.java \
        F:/dachuang/backend/src/main/java/com/citytrip/service/impl/AdminServiceImpl.java \
        F:/dachuang/backend/src/main/java/com/citytrip/controller/AdminController.java \
        F:/dachuang/backend/src/main/java/com/citytrip/model/vo/AdminCommunityPostVO.java \
        F:/dachuang/backend/src/test/java/com/citytrip/service/impl/AdminServiceImplCommunityTest.java

git commit -m "feat: add admin community governance endpoints"
```

### Task 4: 重构社区首页与后台社区管理页

**Files:**
- Create: `F:/dachuang/frontend/src/api/adminCommunity.js`
- Create: `F:/dachuang/frontend/src/components/community/CommunityHero.vue`
- Create: `F:/dachuang/frontend/src/components/community/CommunityPinnedSection.vue`
- Create: `F:/dachuang/frontend/src/components/community/CommunityFilterBar.vue`
- Create: `F:/dachuang/frontend/src/components/community/CommunityFeedCard.vue`
- Create: `F:/dachuang/frontend/src/views/admin/CommunityManage.vue`
- Modify: `F:/dachuang/frontend/src/api/itinerary.js`
- Modify: `F:/dachuang/frontend/src/views/Community.vue`
- Modify: `F:/dachuang/frontend/src/views/admin/AdminLayout.vue`
- Modify: `F:/dachuang/frontend/src/router/index.js`

- [ ] **Step 1: 先给前端 API 层补齐社区列表筛选与后台治理请求**

```js
export function reqListCommunityItineraries(params = {}) {
  return request({ url: '/api/itineraries/community', method: 'get', params, skipErrorMessage: true })
}

export function reqDeleteCommunityPost(id) {
  return request({ url: `/api/itineraries/community/${id}`, method: 'delete' })
}
```

```js
export function reqAdminCommunityPosts(params = {}) {
  return request({ url: '/api/admin/community/posts', method: 'get', params })
}
```

- [ ] **Step 2: 用独立组件重写社区首页骨架，先做结构不做细节**

```vue
<CommunityHero @publish="goPublish" @refresh="loadPage" />
<CommunityPinnedSection :items="pinnedRecords" @open="openDetail" />
<CommunityFilterBar v-model:sort="sort" v-model:keyword="keyword" v-model:theme="theme" />
<div class="community-feed-grid">
  <CommunityFeedCard v-for="item in records" :key="item.id" :item="item" @open="openDetail(item.id)" />
</div>
```

- [ ] **Step 3: 用“高级清爽”方向完成视觉细化**

```css
.community-page {
  background: linear-gradient(180deg, #f7f4ec 0%, #fbfaf7 45%, #f6f8fb 100%);
}

.community-feed-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
}
```

- [ ] **Step 4: 新增后台社区管理页，并挂到 admin 菜单与路由**

```js
{
  path: 'community',
  name: 'AdminCommunity',
  component: () => import('@/views/admin/CommunityManage.vue'),
  meta: { title: '社区内容治理' }
}
```

- [ ] **Step 5: 后台列表支持搜索、分页、全站置顶、取消置顶、删除**

```vue
<el-table-column label="操作">
  <template #default="{ row }">
    <el-button link type="primary" @click="togglePin(row)">{{ row.globalPinned ? '取消置顶' : '设为置顶' }}</el-button>
    <el-button link type="danger" @click="removePost(row)">删除</el-button>
  </template>
</el-table-column>
```

- [ ] **Step 6: 手工验证首页与后台页**

Run: `npm run build`
Expected: 前端构建通过；社区首页显示 Hero / 置顶 / 筛选 / 双列流；后台菜单出现“社区内容治理”。

- [ ] **Step 7: 提交首页与后台页重构**

```bash
git add F:/dachuang/frontend/src/views/Community.vue \
        F:/dachuang/frontend/src/components/community \
        F:/dachuang/frontend/src/views/admin/CommunityManage.vue \
        F:/dachuang/frontend/src/router/index.js \
        F:/dachuang/frontend/src/views/admin/AdminLayout.vue \
        F:/dachuang/frontend/src/api/adminCommunity.js

git commit -m "feat: redesign community landing and admin governance ui"
```

### Task 5: 重构社区详情页，接入评论置顶与权限动作

**Files:**
- Create: `F:/dachuang/frontend/src/components/community/CommentComposer.vue`
- Modify: `F:/dachuang/frontend/src/views/CommunityDetail.vue`
- Modify: `F:/dachuang/frontend/src/api/itinerary.js`

- [ ] **Step 1: 先补齐详情页 API**

```js
export function reqPinCommunityComment(itineraryId, commentId) {
  return request({ url: `/api/itineraries/community/${itineraryId}/comments/${commentId}/pin`, method: 'patch' })
}

export function reqDeleteCommunityPost(id) {
  return request({ url: `/api/itineraries/community/${id}`, method: 'delete' })
}
```

- [ ] **Step 2: 把详情页改成“主列 + 侧列”的双栏阅读布局**

```vue
<div class="detail-grid">
  <main class="detail-main">...</main>
  <aside class="detail-side">...</aside>
</div>
```

- [ ] **Step 3: 在评论区增加 pinned banner 和作者动作按钮**

```vue
<div v-if="pinnedComment" class="pinned-comment-banner">
  <span class="banner-label">作者置顶</span>
  <p>{{ pinnedComment.content }}</p>
</div>

<el-button v-if="comment.canPin" link type="primary" @click="pinComment(comment)">置顶此评论</el-button>
```

- [ ] **Step 4: 根据权限显示“删除帖子 / 管理操作”菜单**

```vue
<el-dropdown v-if="detail.canDelete || detail.canManage">
  <el-dropdown-menu>
    <el-dropdown-item v-if="detail.canDelete" @click="deletePost">删除帖子</el-dropdown-item>
    <el-dropdown-item v-if="detail.canManage" @click="toggleAdminPin">{{ detail.globalPinned ? '取消全站置顶' : '设为全站置顶' }}</el-dropdown-item>
  </el-dropdown-menu>
</el-dropdown>
```

- [ ] **Step 5: 跑前端构建验证详情页状态联动**

Run: `npm run build`
Expected: 前端构建通过；详情页存在置顶评论 banner，作者/管理员看到正确动作菜单。

- [ ] **Step 6: 提交详情页重构**

```bash
git add F:/dachuang/frontend/src/views/CommunityDetail.vue \
        F:/dachuang/frontend/src/components/community/CommentComposer.vue \
        F:/dachuang/frontend/src/api/itinerary.js

git commit -m "feat: redesign community detail moderation experience"
```

### Task 6: 把“公开到社区”升级为“发布路线帖”流程

**Files:**
- Create: `F:/dachuang/frontend/src/components/community/PublishRouteDialog.vue`
- Modify: `F:/dachuang/frontend/src/views/Result.vue`
- Modify: `F:/dachuang/frontend/src/views/History.vue`
- Modify: `F:/dachuang/frontend/src/api/itinerary.js`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/model/dto/PublicStatusReqDTO.java`
- Modify: `F:/dachuang/backend/src/main/java/com/citytrip/service/application/itinerary/SavedItineraryCommandService.java`
- Modify: `F:/dachuang/backend/src/test/java/com/citytrip/service/application/itinerary/SavedItineraryCommandServiceTest.java`

- [ ] **Step 1: 先写发布请求测试，覆盖标题/分享语/标签写回**

```java
@Test
void updatePublicStatusPersistsPublishMetadata() {
    PublicStatusReqDTO req = new PublicStatusReqDTO();
    req.setIsPublic(true);
    req.setTitle("周末路线帖");
    req.setShareNote("适合第一次来武汉的人");
    req.setThemes(List.of("Citywalk", "拍照"));

    ItineraryVO result = service.updatePublicStatus(1L, 2L, req);

    assertThat(result.getIsPublic()).isTrue();
    verify(savedItineraryRepository).saveOrUpdate(argThat(entity -> "周末路线帖".equals(entity.getCustomTitle())));
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn test -Dtest=SavedItineraryCommandServiceTest`
Expected: FAIL，提示 `PublicStatusReqDTO` 缺少 `themes` 字段或未写回请求元数据。

- [ ] **Step 3: 扩展发布 DTO，让用户在发布时可编辑标题、分享语、标签、选中版本**

```java
private List<String> themes;
private String selectedOptionKey;
```

```java
if (CollectionUtils.isNotEmpty(req.getThemes())) {
    originalReq.setThemes(req.getThemes().stream().map(String::trim).limit(3).toList());
}
entity.setRequestJson(savedItineraryCodec.writeJson(originalReq));
```

- [ ] **Step 4: 在 Result 页接入三步发布弹窗**

```vue
<PublishRouteDialog
  v-model="publishDialogVisible"
  :itinerary="itinerary"
  @published="handlePublished"
/>
```

- [ ] **Step 5: 在 History 页给公开路线增加“重新发布/查看帖子”入口**

```vue
<el-button v-if="item.isPublic" round @click="openCommunity(item.id)">查看帖子</el-button>
<el-button v-else type="primary" round @click="openPublish(item.id)">发布路线帖</el-button>
```

- [ ] **Step 6: 发布成功后给出明确去向**

```js
ElMessageBox.confirm('路线帖已发布，是否立即查看？', '发布成功', {
  confirmButtonText: '查看帖子',
  cancelButtonText: '回社区首页'
})
```

- [ ] **Step 7: 跑测试与构建**

Run: `mvn test -Dtest=SavedItineraryCommandServiceTest`
Expected: PASS

Run: `npm run build`
Expected: PASS

- [ ] **Step 8: 提交发布流程重构**

```bash
git add F:/dachuang/frontend/src/components/community/PublishRouteDialog.vue \
        F:/dachuang/frontend/src/views/Result.vue \
        F:/dachuang/frontend/src/views/History.vue \
        F:/dachuang/backend/src/main/java/com/citytrip/model/dto/PublicStatusReqDTO.java \
        F:/dachuang/backend/src/main/java/com/citytrip/service/application/itinerary/SavedItineraryCommandService.java \
        F:/dachuang/backend/src/test/java/com/citytrip/service/application/itinerary/SavedItineraryCommandServiceTest.java

git commit -m "feat: upgrade public share flow to route publishing"
```

### Task 7: 最终联调、验证与交付说明

**Files:**
- Create: `F:/dachuang/docs/community/community-redesign-changelog.md`
- Modify: `F:/dachuang/docs/superpowers/specs/2026-04-21-community-redesign-design.md`
- Modify: `F:/dachuang/docs/superpowers/plans/2026-04-21-community-redesign.md`

- [ ] **Step 1: 跑后端社区相关测试集**

Run: `mvn test -Dtest=CommunityItineraryQueryServiceTest,CommunityInteractionServiceTest,SavedItineraryCommandServiceTest,AdminServiceImplCommunityTest`
Expected: PASS

- [ ] **Step 2: 跑前端构建**

Run: `npm run build`
Expected: PASS

- [ ] **Step 3: 做一次 requirements checklist，对照 spec 逐项核验**

```markdown
- [x] 管理员可删帖
- [x] 管理员可全站置顶
- [x] 作者可删自己帖子
- [x] 作者可置顶 1 条一级评论
- [x] 首页含 Hero / 置顶 / 双列流 / 轻筛选
- [x] 详情页有置顶评论与权限菜单
- [x] 发布流程升级为“发布路线帖”
```

- [ ] **Step 4: 写重构说明文档，明确“改了哪里、如何体现”**

```markdown
## 更新范围
- 后端权限与治理：新增软删除、全站置顶、作者置顶评论
- 前端社区 UI：首页双列卡片流、详情双栏布局、后台社区管理页
- 发布流程：从即时公开改为三步发布路线帖
```

- [ ] **Step 5: 最终提交**

```bash
git add F:/dachuang/docs/community/community-redesign-changelog.md

git commit -m "docs: add community redesign changelog"
```
