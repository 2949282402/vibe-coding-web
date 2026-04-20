<script setup>
import { computed, onMounted, ref } from 'vue';
import { fetchSiteHomeApi } from '../api/blog';
import PostCard from '../components/PostCard.vue';
import { usePreferencesStore } from '../stores/preferences';

const preferences = usePreferencesStore();
const loading = ref(true);
const ragCopy = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return {
      title: '知识问答',
      description: '基于已发布文章做检索增强问答，适合快速定位方案、设计思路和交付细节。',
      cta: '进入 RAG'
    };
  }

  return {
    title: 'Knowledge Assistant',
    description: 'Ask retrieval-augmented questions against published blog content to find designs, steps, and delivery details.',
    cta: 'Open RAG'
  };
});
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

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchSiteHomeApi();
    pageData.value = res.data;
  } finally {
    loading.value = false;
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
            <h2>{{ ragCopy.title }}</h2>
          </div>
          <p class="muted rag-copy">{{ ragCopy.description }}</p>
          <router-link to="/knowledge" class="rag-link">{{ ragCopy.cta }}</router-link>
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
  padding: 46px;
  display: grid;
  grid-template-columns: 1.34fr 0.8fr;
  gap: 28px;
  overflow: hidden;
}

.hero-panel::after {
  content: "";
  position: absolute;
  right: -60px;
  top: -40px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(220, 193, 136, 0.22), transparent 68%);
  pointer-events: none;
}

.hero-copy {
  position: relative;
  z-index: 1;
  max-width: 760px;
}

.hero-desc {
  max-width: 640px;
  margin-top: 18px;
  font-size: 1.1rem;
  line-height: 1.95;
}

.hero-stats {
  padding: 26px;
  border-radius: var(--radius-xl);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  align-self: stretch;
}

.hero-stats div {
  padding: 22px 20px;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: rgba(255, 248, 233, 0.05);
}

.hero-stats strong {
  display: block;
  font-size: 2.6rem;
  margin-bottom: 10px;
  letter-spacing: -0.06em;
}

.hero-stats span {
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.8rem;
}

.content-section {
  margin-top: 30px;
}

.stack,
.side-stack,
.post-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.refined-heading h2 {
  letter-spacing: -0.03em;
}

.sidebar-card {
  padding: 24px;
}

.rag-card {
  gap: 18px;
  background:
    linear-gradient(180deg, rgba(220, 193, 136, 0.12), rgba(255, 248, 233, 0.04)),
    rgba(21, 18, 14, 0.94);
}

.rag-copy {
  margin: 0 0 18px;
  line-height: 1.8;
}

.rag-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: var(--accent-soft);
  color: var(--text-main);
  letter-spacing: 0.08em;
  font-size: 0.8rem;
}

.rag-link:hover {
  background: rgba(220, 193, 136, 0.2);
  border-color: var(--line-strong);
}

.all-link {
  letter-spacing: 0.08em;
  font-size: 0.82rem;
}

@media (max-width: 960px) {
  .hero-panel {
    grid-template-columns: 1fr;
  }
}
</style>
