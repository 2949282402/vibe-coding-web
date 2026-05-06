<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchPostListApi } from '../api/blog';
import { usePreferencesStore } from '../stores/preferences';

const preferences = usePreferencesStore();
const route = useRoute();
const router = useRouter();
const loading = ref(true);
const searchForm = reactive({
  keyword: ''
});
const pageData = ref({
  records: [],
  page: 1,
  pageSize: 8,
  total: 0,
  totalPages: 0,
  hasNext: false
});

const copy = computed(() =>
  preferences.locale === 'zh-CN'
    ? {
        kicker: '归档浏览',
        title: '文章索引',
        summary: '按照主题、标签和月份浏览所有已发布内容，像在数字刊物里翻阅完整目录。',
        searchTitle: '查找文章',
        pageLabel: '页码',
        monthlyIssue: '月度分组',
        filteredBy: '当前筛选',
        noPosts: '当前筛选条件下还没有文章。',
        readArticle: '阅读正文'
      }
    : {
        kicker: 'Archive Browse',
        title: 'Article Index',
        summary: 'Browse published writing by theme, tag, and month as if you were scanning the full table of contents of a digital journal.',
        searchTitle: 'Find Articles',
        pageLabel: 'Page',
        monthlyIssue: 'Monthly grouping',
        filteredBy: 'Current filters',
        noPosts: 'No articles match the current filters.',
        readArticle: 'Read article'
      }
);

const currentCategory = computed(() => route.query.category || '');
const currentTag = computed(() => route.query.tag || '');

const syncFormFromRoute = () => {
  searchForm.keyword = route.query.keyword || '';
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchPostListApi({
      keyword: route.query.keyword,
      categorySlug: route.query.category,
      tagSlug: route.query.tag,
      page: Number(route.query.page || 1),
      pageSize: 8
    });
    pageData.value = res.data;
  } finally {
    loading.value = false;
  }
};

const groupedPosts = computed(() => pageData.value.records.reduce((acc, post) => {
  const key = preferences.formatMonth(post.publishedAt);
  if (!acc[key]) {
    acc[key] = [];
  }
  acc[key].push(post);
  return acc;
}, {}));

const activeFilters = computed(() => {
  const filters = [];
  if (route.query.keyword) {
    filters.push(`${preferences.t('archive.keyword')}: ${route.query.keyword}`);
  }
  if (currentCategory.value) {
    filters.push(`${preferences.t('archive.category')}: ${currentCategory.value}`);
  }
  if (currentTag.value) {
    filters.push(`${preferences.t('archive.tag')}: ${currentTag.value}`);
  }
  return filters;
});

const updateQuery = (patch) => {
  const query = {
    keyword: route.query.keyword,
    category: route.query.category,
    tag: route.query.tag,
    page: route.query.page,
    ...patch
  };

  Object.keys(query).forEach((key) => {
    if (query[key] === '' || query[key] === undefined || query[key] === null) {
      delete query[key];
    }
  });

  router.push({ path: '/archives', query });
};

const search = () => {
  updateQuery({
    keyword: searchForm.keyword.trim() || undefined,
    page: 1
  });
};

const resetFilters = () => {
  searchForm.keyword = '';
  router.push({ path: '/archives' });
};

const prevPage = () => {
  if (pageData.value.page > 1) {
    updateQuery({ page: pageData.value.page - 1 });
  }
};

const nextPage = () => {
  if (pageData.value.hasNext) {
    updateQuery({ page: pageData.value.page + 1 });
  }
};

watch(() => route.fullPath, async () => {
  syncFormFromRoute();
  await loadData();
});

onMounted(async () => {
  syncFormFromRoute();
  await loadData();
});
</script>

<template>
  <section class="section-card archive-panel" v-loading="loading">
    <div class="archive-hero">
      <div class="archive-copy">
        <span class="archive-kicker">{{ copy.kicker }}</span>
        <h2>{{ copy.title }}</h2>
        <p class="muted summary-text">{{ copy.summary }}</p>
      </div>

      <div class="archive-side">
        <div class="archive-overview glass-panel">
          <span class="muted">{{ preferences.t('archive.postsIndexed', { count: pageData.total }) }}</span>
          <strong>{{ pageData.total }}</strong>
        </div>
        <div class="archive-overview glass-panel">
          <span class="muted">{{ copy.pageLabel }}</span>
          <strong>{{ pageData.page }}</strong>
        </div>
      </div>
    </div>

    <div class="archive-search-card">
      <div class="archive-search-head">
        <h3>{{ copy.searchTitle }}</h3>
        <span class="muted">{{ copy.monthlyIssue }}</span>
      </div>

      <div class="toolbar">
        <el-input
          v-model="searchForm.keyword"
          :placeholder="preferences.t('archive.searchPlaceholder')"
          @keyup.enter="search"
        />
        <el-button type="primary" @click="search">{{ preferences.t('postManage.apply') }}</el-button>
        <el-button @click="resetFilters">{{ preferences.t('postManage.reset') }}</el-button>
      </div>

      <div v-if="activeFilters.length" class="filter-summary">
        <span class="filter-label">{{ copy.filteredBy }}</span>
        <div class="chip-list">
          <span v-for="item in activeFilters" :key="item" class="chip">{{ item }}</span>
        </div>
      </div>
    </div>

    <div v-if="pageData.records.length" class="archive-groups">
      <div v-for="(items, period) in groupedPosts" :key="period" class="archive-group">
        <div class="archive-group-head">
          <div>
            <span class="group-kicker">{{ copy.monthlyIssue }}</span>
            <h3>{{ period }}</h3>
          </div>
          <span class="group-count">{{ items.length }}</span>
        </div>

        <div class="archive-list">
          <router-link
            v-for="item in items"
            :key="item.id"
            :to="`/posts/${item.slug}`"
            class="archive-item"
          >
            <div class="archive-item-main">
              <div class="archive-item-meta muted">
                <span>{{ item.categoryName || preferences.t('post.uncategorized') }}</span>
                <span>{{ preferences.formatDate(item.publishedAt) }}</span>
              </div>
              <strong>{{ item.title }}</strong>
              <p class="muted item-summary">{{ item.summary }}</p>
            </div>

            <div class="archive-item-side">
              <span class="views muted">{{ preferences.t('post.views', { count: item.viewCount }) }}</span>
              <span class="read-link">{{ copy.readArticle }}</span>
            </div>
          </router-link>
        </div>
      </div>
    </div>

    <div v-else class="empty muted">
      {{ copy.noPosts }}
    </div>

    <div v-if="pageData.totalPages > 0" class="pager">
      <span class="muted">{{ preferences.t('archive.pageInfo', { page: pageData.page, total: pageData.totalPages }) }}</span>
      <div class="pager-actions">
        <el-button :disabled="pageData.page <= 1" @click="prevPage">{{ preferences.t('postManage.previous') }}</el-button>
        <el-button :disabled="!pageData.hasNext" type="primary" @click="nextPage">{{ preferences.t('postManage.next') }}</el-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.archive-panel {
  position: relative;
  display: grid;
  gap: 28px;
  padding: 32px;
  overflow: hidden;
}

.archive-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.06), transparent 28%),
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.04), transparent 26%);
}

.archive-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: end;
  padding-bottom: 6px;
}

.archive-copy {
  display: grid;
  gap: 12px;
}

.archive-kicker,
.group-kicker {
  display: inline-flex;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 0.74rem;
}

.archive-hero h2,
.archive-search-head h3,
.archive-group-head h3 {
  margin: 0;
  font-family: var(--font-display);
  letter-spacing: -0.04em;
}

.archive-hero h2 {
  font-size: clamp(2rem, 3.6vw, 3rem);
  line-height: 1.02;
}

.summary-text {
  max-width: 720px;
  margin: 0;
  line-height: 1.84;
}

.archive-side {
  display: grid;
  grid-template-columns: repeat(2, minmax(140px, 1fr));
  gap: 12px;
}

.archive-overview {
  padding: 18px 20px;
  border-radius: 22px;
  display: grid;
  gap: 10px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.018)),
    rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.archive-overview strong {
  font-family: var(--font-display);
  font-size: 2.1rem;
  line-height: 1;
  letter-spacing: -0.06em;
}

.archive-search-card {
  position: relative;
  display: grid;
  gap: 16px;
  padding: 22px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.018)),
    rgba(255, 255, 255, 0.03);
  overflow: hidden;
}

.archive-search-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.archive-search-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 12px;
  align-items: stretch;
}

.toolbar :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.045);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.toolbar :deep(.el-button) {
  min-height: 48px;
  padding-inline: 18px;
  border-radius: 18px;
}

.filter-summary {
  display: grid;
  gap: 10px;
}

.filter-label {
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 0.72rem;
}

.archive-groups {
  display: grid;
  gap: 32px;
}

.archive-group {
  position: relative;
  display: grid;
  gap: 16px;
  padding-left: 18px;
}

.archive-group::before {
  content: '';
  position: absolute;
  top: 8px;
  bottom: 8px;
  left: 0;
  width: 2px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.38), rgba(255, 255, 255, 0.08));
}

.archive-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.group-count {
  display: inline-flex;
  min-width: 42px;
  min-height: 42px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.018)),
    var(--bg-panel);
  font-family: var(--font-display);
  font-size: 1.12rem;
}

.archive-list {
  display: grid;
  gap: 12px;
}

.archive-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  padding: 22px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.018)),
    rgba(255, 255, 255, 0.03);
  transition: border-color 0.2s ease, background-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.archive-item:hover {
  border-color: var(--line-strong);
  background: rgba(255, 255, 255, 0.05);
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(0, 0, 0, 0.12);
}

.archive-item-main {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.archive-item-main strong {
  display: block;
  font-family: var(--font-display);
  font-size: clamp(1.26rem, 2vw, 1.64rem);
  line-height: 1.12;
  letter-spacing: -0.03em;
}

.archive-item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  font-size: 0.74rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.item-summary {
  margin: 0;
  line-height: 1.78;
}

.archive-item-side {
  min-width: 124px;
  display: grid;
  align-content: space-between;
  justify-items: end;
  gap: 12px;
}

.views {
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  text-align: right;
}

.read-link {
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02)),
    var(--bg-panel);
  color: var(--text-main);
  font-size: 0.76rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.empty {
  padding: 34px 0;
  text-align: center;
}

.pager {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px 20px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.pager-actions {
  display: flex;
  gap: 12px;
}

@media (max-width: 960px) {
  .archive-hero,
  .archive-side {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .archive-panel,
  .archive-search-card {
    padding: 22px;
  }

  .toolbar,
  .archive-item,
  .pager {
    grid-template-columns: 1fr;
  }

  .archive-search-head,
  .archive-group-head,
  .pager {
    flex-direction: column;
    align-items: flex-start;
  }

  .archive-item-side {
    min-width: 0;
    justify-items: start;
  }

  .views {
    text-align: left;
  }

  .pager-actions {
    width: 100%;
  }
}
</style>
