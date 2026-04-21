<template>
  <section class="filter-shell">
    <div class="sort-group">
      <button
        v-for="item in sortOptions"
        :key="item.value"
        type="button"
        class="sort-pill"
        :class="{ active: modelSort === item.value }"
        @click="$emit('update:sort', item.value); $emit('search')"
      >
        {{ item.label }}
      </button>
    </div>

    <div class="search-group">
      <el-input
        :model-value="modelKeyword"
        placeholder="搜索标题、分享语或标签"
        clearable
        @update:model-value="$emit('update:keyword', $event)"
        @keyup.enter="$emit('search')"
      />
      <el-button round @click="$emit('search')">筛选</el-button>
    </div>

    <div class="theme-group">
      <button
        type="button"
        class="theme-pill"
        :class="{ active: !modelTheme }"
        @click="$emit('update:theme', ''); $emit('search')"
      >
        全部
      </button>
      <button
        v-for="item in themes"
        :key="item"
        type="button"
        class="theme-pill"
        :class="{ active: modelTheme === item }"
        @click="$emit('update:theme', item); $emit('search')"
      >
        {{ item }}
      </button>
    </div>
  </section>
</template>

<script setup>
defineProps({
  modelSort: {
    type: String,
    default: 'latest'
  },
  modelKeyword: {
    type: String,
    default: ''
  },
  modelTheme: {
    type: String,
    default: ''
  },
  themes: {
    type: Array,
    default: () => []
  }
})

defineEmits(['update:sort', 'update:keyword', 'update:theme', 'search'])

const sortOptions = [
  { label: '最新优先', value: 'latest' },
  { label: '热度优先', value: 'hot' }
]
</script>

<style scoped>
.filter-shell {
  display: grid;
  gap: 16px;
  margin-top: 26px;
  padding: 22px;
  border-radius: 28px;
  background: rgba(255, 251, 244, 0.88);
  border: 1px solid rgba(220, 210, 194, 0.76);
  box-shadow: 0 16px 40px rgba(25, 37, 54, 0.06);
}

.sort-group,
.theme-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.search-group {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}

.sort-pill,
.theme-pill {
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid rgba(194, 179, 154, 0.45);
  background: #fffaf3;
  color: #5b6572;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sort-pill.active,
.theme-pill.active {
  border-color: rgba(28, 51, 78, 0.22);
  background: #1b2a3c;
  color: #f8efe1;
  box-shadow: 0 10px 24px rgba(17, 32, 49, 0.18);
}

@media (max-width: 800px) {
  .search-group {
    grid-template-columns: 1fr;
  }
}
</style>