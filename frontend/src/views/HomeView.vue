<script setup>
import { computed, onMounted, ref } from 'vue';
import { fetchSiteHomeApi, searchKnowledgePublicApi } from '../api/blog';
import PostCard from '../components/PostCard.vue';
import { renderMarkdown } from '../utils/markdown';
import { usePreferencesStore } from '../stores/preferences';

const preferences = usePreferencesStore();
const loading = ref(true);
const searchLoading = ref(false);
const searchQuestion = ref('');
const searchResult = ref(null);

const homeCopy = computed(() =>
  preferences.locale === 'zh-CN'
    ? {
        ragTitle: '站内知识检索',
        ragDescription: '不接入大模型，直接基于当前站内文章做 RAG 检索与引用展示。',
        ragButton: '打开聊天页',
        searchTitle: '首页快速检索',
        searchPlaceholder: '输入问题，直接检索站内文章',
        searchButton: '开始检索',
        searchEmpty: '检索结果会显示在这里。',
        sources: '参考来源'
      }
    : {
        ragTitle: 'On-site Knowledge Retrieval',
        ragDescription: 'Run RAG retrieval directly on published posts without using a chat model.',
        ragButton: 'Open Chat',
        searchTitle: 'Quick Search',
        searchPlaceholder: 'Ask a question against on-site posts',
        searchButton: 'Search',
        searchEmpty: 'Search results will appear here.',
        sources: 'Sources'
      }
);

const pageData = ref({
  siteName: 'HeJulian Blog',
  heroTitle: '',
  heroSubtitle: '',
  latestPosts: [],
  featuredPosts: [],
  categories: [],
  tags: [],
  stats: {}
});

const renderedSearchAnswer = computed(() => renderMarkdown(searchResult.value?.answer || ''));

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchSiteHomeApi();
    pageData.value = res.data;
  } finally {
    loading.value = false;
  }
};

const searchKnowledge = async () => {
  const question = searchQuestion.value.trim();
  if (!question) {
    return;
  }

  searchLoading.value = true;
  try {
    const res = await searchKnowledgePublicApi(question);
    searchResult.value = res.data;
  } finally {
    searchLoading.value = false;
  }
};

onMounted(loadData);
</script>

<template>
  <div v-loading="loading">
    <section class="hero section-card hero-panel">
      <div class="hero-copy">
        <span class="hero-kicker">{{ pageData.siteName }}</span>
        <h1 class="hero-title">{{ pageData.heroTitle }}</h1>
        <p class="hero-desc muted">{{ pageData.heroSubtitle }}</p>
      </div>

      <div class="hero-stats glass-panel">
        <div>
          <strong>{{ pageData.stats.postCount || 0 }}</strong>
          <span>{{ preferences.t('home.posts') }}</span>
        </div>
        <div>
          <strong>{{ pageData.stats.categoryCount || 0 }}</strong>
          <span>{{ preferences.t('home.categories') }}</span>
        </div>
        <div>
          <strong>{{ pageData.stats.tagCount || 0 }}</strong>
          <span>{{ preferences.t('home.tags') }}</span>
        </div>
        <div>
          <strong>{{ pageData.stats.commentCount || 0 }}</strong>
          <span>{{ preferences.t('home.comments') }}</span>
        </div>
      </div>
    </section>

    <section class="section-card search-panel">
      <div class="section-heading refined-heading">
        <h2>{{ homeCopy.searchTitle }}</h2>
      </div>
      <div class="search-row">
        <el-input
          v-model="searchQuestion"
          :placeholder="homeCopy.searchPlaceholder"
          @keyup.enter="searchKnowledge"
        />
        <el-button type="primary" :loading="searchLoading" @click="searchKnowledge">{{ homeCopy.searchButton }}</el-button>
      </div>
      <div class="search-result-panel">
        <div v-if="searchResult" class="search-answer">
          <div class="markdown-body" v-html="renderedSearchAnswer"></div>
          <div v-if="searchResult.sources?.length" class="source-list">
            <strong>{{ homeCopy.sources }}</strong>
            <div class="chip-list">
              <router-link
                v-for="source in searchResult.sources"
                :key="`${source.slug || source.title}-${source.citationIndex}`"
                class="chip"
                :to="source.slug ? `/posts/${source.slug}` : '/archives'"
              >
                [{{ source.citationIndex }}] {{ source.title }}
              </router-link>
            </div>
          </div>
        </div>
        <p v-else class="muted">{{ homeCopy.searchEmpty }}</p>
      </div>
    </section>

    <section class="split-grid content-section">
      <div class="stack">
        <div class="section-heading refined-heading">
          <h2>{{ preferences.t('home.featuredPosts') }}</h2>
          <router-link to="/archives" class="muted all-link">{{ preferences.t('home.browseAll') }}</router-link>
        </div>

        <div class="post-stack">
          <PostCard v-for="post in pageData.featuredPosts" :key="post.id" :post="post" />
        </div>

        <div class="section-heading refined-heading">
          <h2>{{ preferences.t('home.latestReleases') }}</h2>
        </div>

        <div class="post-stack">
          <PostCard v-for="post in pageData.latestPosts" :key="post.id" :post="post" />
        </div>
      </div>

      <aside class="side-stack">
        <section class="section-card sidebar-card rag-card">
          <div class="section-heading refined-heading">
            <h2>{{ homeCopy.ragTitle }}</h2>
          </div>
          <p class="muted rag-copy">{{ homeCopy.ragDescription }}</p>
          <router-link to="/knowledge" class="rag-link">{{ homeCopy.ragButton }}</router-link>
        </section>

        <section class="section-card sidebar-card">
          <div class="section-heading refined-heading">
            <h2>{{ preferences.t('home.categories') }}</h2>
          </div>
          <div class="chip-list">
            <router-link
              v-for="category in pageData.categories"
              :key="category.slug"
              class="chip"
              :to="{ path: '/archives', query: { category: category.slug } }"
            >
              {{ category.name }} / {{ category.postCount }}
            </router-link>
          </div>
        </section>

        <section class="section-card sidebar-card">
          <div class="section-heading refined-heading">
            <h2>{{ preferences.t('home.signalTags') }}</h2>
          </div>
          <div class="chip-list">
            <router-link
              v-for="tag in pageData.tags"
              :key="tag.slug"
              class="chip"
              :to="{ path: '/archives', query: { tag: tag.slug } }"
            >
              # {{ tag.name }}
            </router-link>
          </div>
        </section>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.hero-panel {
  padding: 40px;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.76fr);
  gap: 24px;
  align-items: stretch;
}

.hero-copy {
  display: grid;
  gap: 16px;
  align-content: start;
  min-width: 0;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 7px 12px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
  color: var(--text-secondary);
  letter-spacing: 0.14em;
  font-size: 0.72rem;
  text-transform: uppercase;
}

.hero-title {
  margin: 0;
  max-width: 720px;
  font-size: clamp(2.4rem, 5vw, 4.4rem);
  line-height: 0.98;
  letter-spacing: -0.06em;
}

.hero-desc {
  max-width: 640px;
  margin: 0;
  font-size: 1.02rem;
  line-height: 1.85;
}

.hero-stats {
  padding: 16px;
  border-radius: 28px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-self: stretch;
}

.hero-stats div {
  display: grid;
  gap: 8px;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
}

.hero-stats strong {
  display: block;
  font-size: clamp(1.9rem, 3.5vw, 2.6rem);
  line-height: 1;
  letter-spacing: -0.07em;
}

.hero-stats span {
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.76rem;
}

.search-panel {
  margin-top: 22px;
  padding: 28px;
}

.search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  margin-top: 14px;
}

.search-result-panel {
  margin-top: 18px;
  padding: 20px;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
}

.search-answer {
  display: grid;
  gap: 18px;
}

.search-answer :deep(p) {
  margin: 0;
  line-height: 1.85;
}

.source-list {
  display: grid;
  gap: 12px;
}

.source-list strong {
  font-size: 0.78rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.content-section {
  margin-top: 28px;
  align-items: start;
}

.stack,
.side-stack,
.post-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.sidebar-card {
  padding: 24px;
}

.rag-card {
  display: grid;
  gap: 12px;
}

.rag-copy {
  margin: 0;
  line-height: 1.8;
}

.rag-link,
.all-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.rag-link {
  min-height: 42px;
  width: fit-content;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid var(--line-strong);
  background: var(--accent);
  color: var(--bg-main);
  font-weight: 600;
}

html[data-theme='light'] .rag-link {
  color: #ffffff;
}

@media (max-width: 960px) {
  .hero-panel {
    grid-template-columns: 1fr;
    padding: 30px;
  }
}

@media (max-width: 720px) {
  .search-row {
    grid-template-columns: 1fr;
  }

  .hero-panel,
  .search-panel,
  .sidebar-card {
    padding: 22px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }
}
</style>
