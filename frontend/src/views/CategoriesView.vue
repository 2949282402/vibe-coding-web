<script setup>
import { computed, onMounted, ref } from 'vue';
import { fetchSiteHomeApi } from '../api/blog';
import { usePreferencesStore } from '../stores/preferences';

const preferences = usePreferencesStore();
const loading = ref(true);
const site = ref({
  categories: [],
  tags: []
});

const copy = computed(() =>
  preferences.locale === 'zh-CN'
    ? {
        kicker: '主题浏览',
        title: '栏目与标签',
        summary: '把内容按栏目和主题拆开浏览，适合按知识方向快速建立阅读路径。',
        categorySection: '栏目分区',
        categoryHint: '从稳定的主题结构进入文章归档。',
        tagSection: '标签索引',
        tagHint: '适合快速跳到具体话题与关键词。',
        noDescription: '该栏目暂未补充描述。',
        tagSuffix: '篇内容'
      }
    : {
        kicker: 'Topic Browse',
        title: 'Sections and Tags',
        summary: 'Browse the archive through sections and topical tags to build a faster reading path by knowledge direction.',
        categorySection: 'Sections',
        categoryHint: 'Enter the archive through stable thematic structure.',
        tagSection: 'Tag Index',
        tagHint: 'Use tags to jump straight into specific topics and keywords.',
        noDescription: 'This section does not have a description yet.',
        tagSuffix: 'posts'
      }
);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchSiteHomeApi();
    site.value = res.data;
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);
</script>

<template>
  <div class="taxonomy-shell" v-loading="loading">
    <section class="section-card taxonomy-hero">
      <span class="taxonomy-kicker">{{ copy.kicker }}</span>
      <h2>{{ copy.title }}</h2>
      <p class="muted">{{ copy.summary }}</p>
      <div class="taxonomy-stats">
        <div class="glass-panel">
          <span>{{ preferences.t('home.categories') }}</span>
          <strong>{{ site.categories.length }}</strong>
        </div>
        <div class="glass-panel">
          <span>{{ preferences.t('home.tags') }}</span>
          <strong>{{ site.tags.length }}</strong>
        </div>
      </div>
    </section>

    <div class="taxonomy-grid split-grid">
      <section class="section-card panel category-panel">
        <div class="taxonomy-head">
          <div>
            <span class="section-kicker">{{ copy.categorySection }}</span>
            <h3>{{ preferences.t('categories.title') }}</h3>
            <p class="muted">{{ copy.categoryHint }}</p>
          </div>
        </div>

        <div class="taxonomy-list">
          <router-link
            v-for="category in site.categories"
            :key="category.slug"
            class="taxonomy-item"
            :to="{ path: '/archives', query: { category: category.slug } }"
          >
            <div class="taxonomy-copy">
              <span class="taxonomy-item-kicker">{{ preferences.t('archive.category') }}</span>
              <strong>{{ category.name }}</strong>
              <p class="muted">{{ category.description || copy.noDescription }}</p>
            </div>
            <div class="taxonomy-count">
              <span>{{ preferences.t('categories.postsSuffix') }}</span>
              <strong>{{ category.postCount }}</strong>
            </div>
          </router-link>
        </div>
      </section>

      <section class="section-card panel tag-panel">
        <div class="taxonomy-head">
          <div>
            <span class="section-kicker">{{ copy.tagSection }}</span>
            <h3>{{ preferences.t('categories.tagCloud') }}</h3>
            <p class="muted">{{ copy.tagHint }}</p>
          </div>
        </div>

        <div class="tag-cloud">
          <router-link
            v-for="tag in site.tags"
            :key="tag.slug"
            class="tag-chip"
            :to="{ path: '/archives', query: { tag: tag.slug } }"
          >
            <span># {{ tag.name }}</span>
            <strong>{{ tag.postCount }} {{ copy.tagSuffix }}</strong>
          </router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.taxonomy-shell {
  display: grid;
  gap: 28px;
}

.taxonomy-hero,
.panel {
  padding: 32px;
}

.taxonomy-hero {
  position: relative;
  overflow: hidden;
  display: grid;
  gap: 14px;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.07), transparent 32%),
    radial-gradient(circle at right center, rgba(255, 255, 255, 0.04), transparent 24%),
    var(--bg-panel);
}

.taxonomy-hero::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  border: 1px solid rgba(255, 255, 255, 0.04);
  pointer-events: none;
}

.taxonomy-kicker,
.section-kicker,
.taxonomy-item-kicker {
  display: inline-flex;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 0.74rem;
}

.taxonomy-hero h2,
.taxonomy-head h3 {
  margin: 0;
  font-family: var(--font-display);
  letter-spacing: -0.04em;
}

.taxonomy-hero h2 {
  font-size: clamp(2rem, 3.6vw, 3rem);
  line-height: 1.02;
}

.taxonomy-hero p,
.taxonomy-head p,
.taxonomy-copy p {
  margin: 0;
  line-height: 1.82;
}

.taxonomy-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(160px, 1fr));
  gap: 12px;
  max-width: 420px;
  margin-top: 6px;
}

.taxonomy-stats div {
  padding: 18px 20px;
  border-radius: 22px;
  display: grid;
  gap: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), rgba(255, 255, 255, 0.018)),
    rgba(255, 255, 255, 0.03);
}

.taxonomy-stats span {
  color: var(--text-secondary);
  font-size: 0.74rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.taxonomy-stats strong,
.taxonomy-count strong {
  font-family: var(--font-display);
  font-size: 2rem;
  line-height: 1;
}

.taxonomy-grid {
  align-items: start;
}

.panel {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.035), rgba(255, 255, 255, 0.015)),
    var(--bg-panel);
}

.panel::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.24), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.taxonomy-head {
  margin-bottom: 22px;
}

.taxonomy-head > div {
  display: grid;
  gap: 10px;
}

.taxonomy-list {
  display: grid;
  gap: 12px;
}

.taxonomy-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  padding: 22px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.018)),
    rgba(255, 255, 255, 0.03);
  transition: border-color 0.2s ease, background-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.taxonomy-item:hover {
  border-color: var(--line-strong);
  background: rgba(255, 255, 255, 0.05);
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(0, 0, 0, 0.12);
}

.taxonomy-copy {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.taxonomy-copy strong {
  display: block;
  font-family: var(--font-display);
  font-size: clamp(1.24rem, 1.9vw, 1.6rem);
  line-height: 1.12;
  letter-spacing: -0.03em;
}

.taxonomy-count {
  display: grid;
  gap: 8px;
  align-content: start;
  min-width: 110px;
  padding: 14px 16px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), rgba(255, 255, 255, 0.018)),
    var(--bg-panel);
}

.taxonomy-count span {
  color: var(--text-secondary);
  font-size: 0.72rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.tag-cloud {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.tag-chip {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.05), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.018)),
    rgba(255, 255, 255, 0.03);
  transition: border-color 0.2s ease, background-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.tag-chip:hover {
  border-color: var(--line-strong);
  background: rgba(255, 255, 255, 0.05);
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(0, 0, 0, 0.12);
}

.tag-chip span {
  color: var(--text-main);
}

.tag-chip strong {
  color: var(--text-secondary);
  font-size: 0.74rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

@media (max-width: 960px) {
  .taxonomy-stats {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .taxonomy-hero,
  .panel {
    padding: 22px;
  }

  .taxonomy-stats,
  .taxonomy-item {
    grid-template-columns: 1fr;
  }

  .taxonomy-count {
    min-width: 0;
  }

  .tag-chip {
    width: 100%;
    min-width: 0;
  }
}

@media (max-width: 640px) {
  .tag-cloud {
    grid-template-columns: 1fr;
  }
}
</style>
