<script setup>
import { onMounted, ref } from 'vue';
import { fetchSiteHomeApi } from '../api/blog';
import { usePreferencesStore } from '../stores/preferences';

const preferences = usePreferencesStore();
const loading = ref(true);
const site = ref({
  categories: [],
  tags: []
});

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
  <div class="taxonomy-shell split-grid" v-loading="loading">
    <section class="section-card panel category-panel">
      <div class="taxonomy-head">
        <div>
          <span class="taxonomy-kicker muted">Explore</span>
          <h2>{{ preferences.t('categories.title') }}</h2>
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
            <strong>{{ category.name }}</strong>
            <p class="muted">{{ category.description || preferences.t('categories.noDescription') }}</p>
          </div>
          <span class="taxonomy-count">{{ category.postCount }} {{ preferences.t('categories.postsSuffix') }}</span>
        </router-link>
      </div>
    </section>

    <section class="section-card panel tag-panel">
      <div class="taxonomy-head">
        <div>
          <span class="taxonomy-kicker muted">Signals</span>
          <h2>{{ preferences.t('categories.tagCloud') }}</h2>
        </div>
      </div>

      <div class="chip-list tag-cloud">
        <router-link
          v-for="tag in site.tags"
          :key="tag.slug"
          class="chip tag-chip"
          :to="{ path: '/archives', query: { tag: tag.slug } }"
        >
          # {{ tag.name }} / {{ tag.postCount }}
        </router-link>
      </div>
    </section>
  </div>
</template>

<style scoped>
.taxonomy-shell {
  align-items: start;
}

.panel {
  padding: 32px;
}

.taxonomy-head {
  margin-bottom: 22px;
}

.taxonomy-kicker {
  display: inline-block;
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 0.76rem;
}

.taxonomy-head h2 {
  margin: 0;
  font-size: clamp(1.55rem, 2.6vw, 2.2rem);
  letter-spacing: -0.04em;
}

.taxonomy-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.taxonomy-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
  border-radius: 22px;
  border: 1px solid var(--line);
  background: rgba(255, 248, 233, 0.035);
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.taxonomy-item:hover {
  transform: translateY(-2px);
  border-color: var(--line-strong);
  background: rgba(255, 248, 233, 0.06);
}

.taxonomy-copy {
  min-width: 0;
}

.taxonomy-copy strong {
  display: block;
  font-size: 1.04rem;
  line-height: 1.45;
}

.taxonomy-copy p {
  margin: 8px 0 0;
  line-height: 1.7;
}

.taxonomy-count {
  white-space: nowrap;
  align-self: flex-start;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid var(--line);
  color: var(--text-secondary);
  font-size: 0.8rem;
  background: var(--accent-soft);
}

.tag-cloud {
  gap: 10px;
}

.tag-chip {
  min-height: 42px;
  padding: 10px 14px;
}

@media (max-width: 720px) {
  .panel {
    padding: 22px;
  }

  .taxonomy-item {
    flex-direction: column;
  }
}
</style>
