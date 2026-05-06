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
        ragTitle: '知识中枢',
        ragDescription: '直接基于站内已发布文章完成检索、引用和追问，适合快速回顾既有知识资产。',
        ragButton: '进入知识页',
        searchTitle: '快速检索',
        searchDescription: '像在一份结构化知识刊物里提问，先找准文章，再回到原文阅读。',
        searchPlaceholder: '输入问题，检索站内文章与知识片段',
        searchButton: '立即检索',
        searchEmpty: '输入问题后，这里会显示结构化答案和引用来源。',
        sources: '引用来源',
        explore: '继续浏览',
        spotlight: '编辑精选',
        latest: '最新发布',
        archives: '查看全部文章',
        categoriesTitle: '栏目导航',
        tagsTitle: '主题标签',
        searchSignal: '站内搜索'
      }
    : {
        ragTitle: 'Knowledge Desk',
        ragDescription: 'Search, cite, and revisit published knowledge directly from the site archive before returning to the full article.',
        ragButton: 'Open Knowledge',
        searchTitle: 'Quick Retrieval',
        searchDescription: 'Ask like you are browsing a structured digital magazine: find the right article, then go back to the source.',
        searchPlaceholder: 'Ask a question across on-site posts and knowledge chunks',
        searchButton: 'Search Now',
        searchEmpty: 'Ask a question and the structured answer with citations will appear here.',
        sources: 'Sources',
        explore: 'Keep Exploring',
        spotlight: 'Editor Picks',
        latest: 'Latest Releases',
        archives: 'Browse all articles',
        categoriesTitle: 'Sections',
        tagsTitle: 'Topics',
        searchSignal: 'On-site Search'
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
const spotlightPost = computed(() => pageData.value.featuredPosts?.[0] || pageData.value.latestPosts?.[0] || null);
const featuredGridPosts = computed(() => (pageData.value.featuredPosts || []).slice(1, 5));
const latestGridPosts = computed(() => pageData.value.latestPosts || []);

const spotlightCoverStyle = computed(() => ({
  backgroundImage: spotlightPost.value?.coverImage
    ? `linear-gradient(180deg, rgba(6, 8, 11, 0.06), rgba(6, 8, 11, 0.82)), url(${spotlightPost.value.coverImage})`
    : 'linear-gradient(135deg, rgba(255, 255, 255, 0.13), rgba(110, 110, 110, 0.08) 44%, rgba(9, 10, 13, 0.92))'
}));

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
  <div v-loading="loading" class="home-editorial-page">
    <section class="hero section-card editorial-hero">
      <div class="hero-copy">
        <span class="hero-kicker">{{ pageData.siteName }}</span>
        <h1 class="hero-title">{{ pageData.heroTitle }}</h1>
        <p class="hero-desc muted">{{ pageData.heroSubtitle }}</p>

        <div class="hero-actions">
          <router-link to="/archives" class="hero-primary-link">{{ homeCopy.archives }}</router-link>
          <router-link to="/knowledge" class="hero-secondary-link">{{ homeCopy.ragButton }}</router-link>
        </div>

        <div class="hero-stat-row glass-panel">
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
      </div>

      <router-link v-if="spotlightPost" :to="`/posts/${spotlightPost.slug}`" class="hero-spotlight">
        <div class="hero-spotlight-cover" :style="spotlightCoverStyle"></div>
        <div class="hero-spotlight-panel glass-panel">
          <div class="hero-spotlight-meta">
            <span>{{ homeCopy.spotlight }}</span>
            <span>{{ preferences.formatDate(spotlightPost.publishedAt) }}</span>
          </div>
          <h2>{{ spotlightPost.title }}</h2>
          <p>{{ spotlightPost.summary }}</p>
          <div class="hero-spotlight-footer">
            <span>{{ spotlightPost.categoryName || preferences.t('post.uncategorized') }}</span>
            <span>{{ preferences.t('post.views', { count: spotlightPost.viewCount }) }}</span>
          </div>
        </div>
      </router-link>
    </section>

    <section class="section-card retrieval-panel">
      <div class="retrieval-head">
        <div>
          <span class="retrieval-signal">{{ homeCopy.searchSignal }}</span>
          <h2>{{ homeCopy.searchTitle }}</h2>
          <p class="muted">{{ homeCopy.searchDescription }}</p>
        </div>
        <router-link to="/knowledge" class="retrieval-link">{{ homeCopy.explore }}</router-link>
      </div>

      <div class="retrieval-row">
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

    <section class="home-grid split-grid">
      <div class="home-main-stack">
        <div class="section-heading refined-heading editorial-heading">
          <h2>{{ homeCopy.spotlight }}</h2>
          <router-link to="/archives" class="muted all-link">{{ homeCopy.archives }}</router-link>
        </div>

        <div class="featured-grid">
          <PostCard v-for="post in featuredGridPosts" :key="post.id" :post="post" />
        </div>

        <div class="section-heading refined-heading editorial-heading">
          <h2>{{ homeCopy.latest }}</h2>
        </div>

        <div class="latest-grid">
          <PostCard v-for="post in latestGridPosts" :key="post.id" :post="post" />
        </div>
      </div>

      <aside class="home-side-stack">
        <section class="section-card side-editorial-card knowledge-card">
          <div class="side-card-head">
            <span class="side-kicker">Desk</span>
            <h2>{{ homeCopy.ragTitle }}</h2>
          </div>
          <p class="muted">{{ homeCopy.ragDescription }}</p>
          <router-link to="/knowledge" class="side-card-link">{{ homeCopy.ragButton }}</router-link>
        </section>

        <section class="section-card side-editorial-card">
          <div class="side-card-head">
            <span class="side-kicker">Browse</span>
            <h2>{{ homeCopy.categoriesTitle }}</h2>
          </div>
          <div class="editorial-pill-list">
            <router-link
              v-for="category in pageData.categories"
              :key="category.slug"
              class="editorial-pill"
              :to="{ path: '/archives', query: { category: category.slug } }"
            >
              <span>{{ category.name }}</span>
              <strong>{{ category.postCount }}</strong>
            </router-link>
          </div>
        </section>

        <section class="section-card side-editorial-card">
          <div class="side-card-head">
            <span class="side-kicker">Index</span>
            <h2>{{ homeCopy.tagsTitle }}</h2>
          </div>
          <div class="chip-list topic-chip-list">
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
.home-editorial-page {
  display: grid;
  gap: 32px;
}

.editorial-hero {
  position: relative;
  isolation: isolate;
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(360px, 0.98fr);
  gap: 24px;
  padding: 34px;
  overflow: hidden;
}

.editorial-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.08), transparent 34%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.hero-copy {
  display: grid;
  align-content: start;
  gap: 18px;
  min-width: 0;
}

.hero-desc {
  max-width: 660px;
  margin: 0;
  font-size: 1.02rem;
  line-height: 1.9;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 4px;
}

.hero-primary-link,
.hero-secondary-link,
.retrieval-link,
.side-card-link,
.all-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.hero-primary-link,
.side-card-link {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(192, 192, 192, 0.92));
  color: #111214;
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.18);
}

html[data-theme='light'] .hero-primary-link,
html[data-theme='light'] .side-card-link {
  background: linear-gradient(135deg, rgba(29, 29, 31, 0.95), rgba(68, 68, 74, 0.94));
  color: #ffffff;
  border-color: rgba(20, 20, 20, 0.14);
  box-shadow: 0 12px 24px rgba(17, 17, 17, 0.1);
}

.hero-secondary-link,
.retrieval-link,
.all-link {
  border: 1px solid var(--line);
  background: var(--bg-panel);
  color: var(--text-main);
}

.hero-primary-link:hover,
.hero-secondary-link:hover,
.retrieval-link:hover,
.side-card-link:hover,
.all-link:hover {
  transform: translateY(-1px);
}

.hero-stat-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
  padding: 14px;
  border-radius: 26px;
}

.hero-stat-row div {
  display: grid;
  gap: 8px;
  padding: 18px 16px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), rgba(255, 255, 255, 0.018)),
    rgba(255, 255, 255, 0.03);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.hero-stat-row strong {
  font-family: var(--font-display);
  font-size: clamp(1.8rem, 3vw, 2.5rem);
  line-height: 1;
}

.hero-stat-row span {
  color: var(--text-secondary);
  font-size: 0.74rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-spotlight {
  position: relative;
  min-height: 100%;
  border-radius: 28px;
  overflow: hidden;
  border: 1px solid var(--line);
  display: flex;
  align-items: end;
  padding: 18px;
  min-height: 520px;
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.22);
}

.hero-spotlight-cover {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  transform: scale(1.02);
}

.hero-spotlight-panel {
  position: relative;
  width: 100%;
  display: grid;
  gap: 14px;
  padding: 20px;
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(7, 8, 12, 0.38), rgba(7, 8, 12, 0.78)),
    rgba(7, 8, 12, 0.62);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(14px);
}

html[data-theme='light'] .hero-spotlight-panel {
  background: rgba(255, 255, 255, 0.84);
}

.hero-spotlight-meta,
.hero-spotlight-footer,
.retrieval-signal,
.side-kicker {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  color: var(--text-secondary);
  font-size: 0.74rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.hero-spotlight-panel h2,
.retrieval-head h2,
.side-card-head h2 {
  margin: 0;
  font-family: var(--font-display);
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.hero-spotlight-panel h2 {
  font-size: clamp(2rem, 3.6vw, 3rem);
}

.hero-spotlight-panel p,
.retrieval-head p,
.side-editorial-card p {
  margin: 0;
  line-height: 1.82;
}

.retrieval-panel {
  position: relative;
  overflow: hidden;
  padding: 28px;
  display: grid;
  gap: 18px;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.05), transparent 30%),
    radial-gradient(circle at bottom left, rgba(255, 255, 255, 0.06), transparent 28%),
    var(--bg-panel);
}

.retrieval-panel::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  border: 1px solid rgba(255, 255, 255, 0.04);
  pointer-events: none;
}

.retrieval-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
}

.retrieval-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: stretch;
}

.retrieval-row :deep(.el-input__wrapper) {
  min-height: 50px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.05);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.05);
}

.retrieval-row :deep(.el-button) {
  min-height: 50px;
  padding-inline: 20px;
  border-radius: 18px;
}

.search-result-panel {
  padding: 22px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02)),
    rgba(255, 255, 255, 0.03);
  min-height: 172px;
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
  color: var(--text-secondary);
  font-size: 0.76rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.home-grid {
  align-items: start;
}

.home-main-stack,
.home-side-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.editorial-heading {
  margin-top: 4px;
}

.featured-grid,
.latest-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.side-editorial-card {
  position: relative;
  overflow: hidden;
  padding: 24px;
  display: grid;
  gap: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.035), rgba(255, 255, 255, 0.015)),
    var(--bg-panel);
}

.side-editorial-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.3), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.knowledge-card {
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.08), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0.015)),
    var(--bg-panel);
}

.side-card-head {
  display: grid;
  gap: 10px;
}

.editorial-pill-list {
  display: grid;
  gap: 10px;
}

.editorial-pill {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.015)),
    rgba(255, 255, 255, 0.03);
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.editorial-pill:hover {
  transform: translateY(-1px);
  border-color: var(--line-strong);
  background: rgba(255, 255, 255, 0.05);
  box-shadow: 0 18px 30px rgba(0, 0, 0, 0.12);
}

.editorial-pill strong {
  font-family: var(--font-display);
  font-size: 1.12rem;
}

.topic-chip-list {
  gap: 10px;
}

.topic-chip-list :deep(.chip) {
  background: rgba(255, 255, 255, 0.035);
  border-color: rgba(255, 255, 255, 0.08);
}

@media (max-width: 1080px) {
  .editorial-hero,
  .featured-grid,
  .latest-grid {
    grid-template-columns: 1fr;
  }

  .hero-spotlight {
    min-height: 420px;
  }

  .hero-stat-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .editorial-hero,
  .retrieval-panel,
  .side-editorial-card {
    padding: 22px;
  }

  .retrieval-head,
  .retrieval-row {
    display: grid;
    grid-template-columns: 1fr;
  }

  .hero-stat-row {
    grid-template-columns: 1fr;
  }

  .hero-spotlight {
    min-height: 360px;
  }
}
</style>
