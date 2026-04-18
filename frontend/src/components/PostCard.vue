<script setup>
import { usePreferencesStore } from '../stores/preferences';

defineProps({
  post: {
    type: Object,
    required: true
  }
});

const preferences = usePreferencesStore();
</script>

<template>
  <router-link :to="`/posts/${post.slug}`" class="post-card section-card">
    <div class="cover" :style="{ backgroundImage: `url(${post.coverImage || ''})` }">
      <div class="cover-overlay"></div>
    </div>
    <div class="body">
      <div class="meta muted">
        <span>{{ post.categoryName || preferences.t('post.uncategorized') }}</span>
        <span>{{ preferences.formatDate(post.publishedAt) }}</span>
        <span>{{ preferences.t('post.views', { count: post.viewCount }) }}</span>
      </div>
      <h3>{{ post.title }}</h3>
      <p>{{ post.summary }}</p>
      <div class="chip-list">
        <span v-for="tag in post.tags" :key="tag" class="chip"># {{ tag }}</span>
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.post-card {
  overflow: hidden;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  min-height: 240px;
  transition: transform 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease;
}

.post-card:hover {
  transform: translateY(-4px);
  border-color: var(--line-strong);
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.45);
}

.cover {
  min-height: 240px;
  position: relative;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(0, 0, 0, 0.84)),
    #111;
  background-size: cover;
  background-position: center;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.55)),
    linear-gradient(90deg, rgba(255, 255, 255, 0.08), transparent 32%);
}

.body {
  padding: 26px;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 0.84rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

h3 {
  margin: 18px 0 12px;
  font-size: 1.6rem;
  letter-spacing: -0.03em;
}

p {
  margin: 0 0 18px;
  line-height: 1.75;
  color: var(--text-secondary);
}

@media (max-width: 720px) {
  .post-card {
    grid-template-columns: 1fr;
  }
}
</style>
