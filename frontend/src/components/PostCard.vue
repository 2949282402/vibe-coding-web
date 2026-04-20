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
  grid-template-columns: 280px minmax(0, 1fr);
  min-height: 272px;
  transition: transform 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease;
}

.post-card:hover {
  transform: translateY(-5px);
  border-color: var(--line-strong);
  box-shadow: 0 30px 72px rgba(36, 24, 9, 0.24);
}

.cover {
  min-height: 272px;
  position: relative;
  background:
    linear-gradient(135deg, rgba(255, 240, 214, 0.22), rgba(18, 14, 10, 0.8)),
    #1d1712;
  background-size: cover;
  background-position: center;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, transparent, rgba(17, 13, 9, 0.58)),
    linear-gradient(90deg, rgba(255, 244, 220, 0.1), transparent 34%);
}

.body {
  padding: 28px 30px;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 0.8rem;
  letter-spacing: 0.08em;
}

h3 {
  margin: 18px 0 14px;
  font-size: 1.72rem;
  line-height: 1.18;
  letter-spacing: -0.04em;
}

p {
  margin: 0 0 18px;
  line-height: 1.8;
  color: var(--text-secondary);
}

@media (max-width: 720px) {
  .post-card {
    grid-template-columns: 1fr;
  }
}
</style>
