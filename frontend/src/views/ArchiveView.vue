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

const groupedPosts = computed(() => {
  return pageData.value.records.reduce((acc, post) => {
    const key = preferences.formatMonth(post.publishedAt);
    if (!acc[key]) {
      acc[key] = [];
    }
    acc[key].push(post);
    return acc;
  }, {});
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
    <div class="section-heading">
      <div>
        <h2>{{ preferences.t('archive.title') }}</h2>
        <p class="muted summary-text">{{ preferences.t('archive.summary') }}</p>
      </div>
      <span class="muted">{{ preferences.t('archive.postsIndexed', { count: pageData.total }) }}</span>
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

    <div v-if="currentCategory || currentTag || route.query.keyword" class="chip-list filter-summary">
      <span v-if="route.query.keyword" class="chip">{{ preferences.t('archive.keyword') }}: {{ route.query.keyword }}</span>
      <span v-if="currentCategory" class="chip">{{ preferences.t('archive.category') }}: {{ currentCategory }}</span>
      <span v-if="currentTag" class="chip">{{ preferences.t('archive.tag') }}: {{ currentTag }}</span>
    </div>

    <div v-for="(items, period) in groupedPosts" v-if="pageData.records.length" :key="period" class="archive-group">
      <h3>{{ period }}</h3>
      <router-link
        v-for="item in items"
        :key="item.id"
        :to="`/posts/${item.slug}`"
        class="archive-item"
      >
        <div class="archive-copy">
          <strong>{{ item.title }}</strong>
          <p class="muted item-summary">{{ item.summary }}</p>
        </div>
        <div class="archive-meta muted">
          <span>{{ preferences.formatDate(item.publishedAt) }}</span>
          <span>{{ preferences.t('post.views', { count: item.viewCount }) }}</span>
        </div>
      </router-link>
    </div>

    <div v-else class="empty muted">
      {{ preferences.t('archive.noPosts') }}
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
  padding: 30px;
}

.summary-text {
  margin: 8px 0 0;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 12px;
  margin-bottom: 18px;
}

.filter-summary {
  margin-bottom: 10px;
}

.archive-group + .archive-group {
  margin-top: 28px;
}

.archive-group h3 {
  margin: 0 0 14px;
  font-size: 1rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.archive-item {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.025);
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.archive-item + .archive-item {
  margin-top: 12px;
}

.archive-item:hover {
  transform: translateY(-2px);
  border-color: var(--line-strong);
  background: rgba(255, 255, 255, 0.04);
}

.archive-copy {
  min-width: 0;
}

.archive-copy strong {
  display: block;
  font-size: 1.08rem;
}

.archive-meta {
  min-width: 130px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.item-summary {
  margin: 8px 0 0;
}

.empty {
  padding: 28px 0;
  text-align: center;
}

.pager {
  margin-top: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.pager-actions {
  display: flex;
  gap: 12px;
}

@media (max-width: 720px) {
  .toolbar,
  .pager {
    grid-template-columns: 1fr;
    display: grid;
  }

  .archive-item {
    flex-direction: column;
  }

  .archive-meta {
    min-width: 0;
    align-items: flex-start;
  }

  .pager-actions {
    justify-content: flex-start;
  }
}
</style>
