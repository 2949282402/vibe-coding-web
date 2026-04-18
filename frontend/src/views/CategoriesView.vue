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
  <div class="split-grid" v-loading="loading">
    <section class="section-card panel">
      <div class="section-heading">
        <h2>{{ preferences.t('categories.title') }}</h2>
      </div>

      <router-link
        v-for="category in site.categories"
        :key="category.slug"
        class="taxonomy-item"
        :to="{ path: '/archives', query: { category: category.slug } }"
      >
        <div>
          <strong>{{ category.name }}</strong>
          <p class="muted">{{ category.description || preferences.t('categories.noDescription') }}</p>
        </div>
        <span>{{ category.postCount }} {{ preferences.t('categories.postsSuffix') }}</span>
      </router-link>
    </section>

    <section class="section-card panel">
      <div class="section-heading">
        <h2>{{ preferences.t('categories.tagCloud') }}</h2>
      </div>

      <div class="chip-list">
        <router-link
          v-for="tag in site.tags"
          :key="tag.slug"
          class="chip"
          :to="{ path: '/archives', query: { tag: tag.slug } }"
        >
          # {{ tag.name }} / {{ tag.postCount }}
        </router-link>
      </div>
    </section>
  </div>
</template>

<style scoped>
.panel {
  padding: 28px;
}

.taxonomy-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 12px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.025);
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.taxonomy-item:hover {
  transform: translateY(-2px);
  border-color: var(--line-strong);
}

.taxonomy-item p {
  margin: 8px 0 0;
}
</style>
