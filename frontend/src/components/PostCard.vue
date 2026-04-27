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
    <div class="cover" :style="{ backgroundImage: `url(${post.coverImage || ''})` }"></div>
    <div class="body">
      <div class="meta muted">
        <span>{{ post.categoryName || preferences.t('post.uncategorized') }}</span>
        <span>{{ preferences.formatDate(post.publishedAt) }}</span>
      </div>
      <div class="copy">
        <h3>{{ post.title }}</h3>
        <p>{{ post.summary }}</p>
      </div>
      <div class="post-footer">
        <span class="views muted">{{ preferences.t('post.views', { count: post.viewCount }) }}</span>
        <div class="chip-list">
          <span v-for="tag in post.tags" :key="tag" class="chip"># {{ tag }}</span>
        </div>
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.post-card {
  overflow: hidden;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  min-height: 248px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.post-card:hover {
  border-color: var(--line-strong);
  box-shadow: 0 18px 34px rgba(0, 0, 0, 0.14);
}

.cover {
  min-height: 248px;
  position: relative;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), rgba(0, 0, 0, 0.18)),
    linear-gradient(135deg, #242529, #131417);
  background-size: cover;
  background-position: center;
  border-right: 1px solid var(--line);
}

html[data-theme='light'] .cover {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(0, 0, 0, 0.04)),
    linear-gradient(135deg, #d9d9d6, #f6f6f4);
}

.body {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
  padding: 26px 28px;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 0.74rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.copy {
  display: grid;
  gap: 12px;
}

h3 {
  margin: 0;
  font-size: 1.6rem;
  line-height: 1.16;
  letter-spacing: -0.04em;
}

p {
  margin: 0;
  line-height: 1.75;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-footer {
  display: grid;
  gap: 12px;
}

.views {
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.chip-list {
  margin-top: auto;
}

@media (max-width: 720px) {
  .post-card {
    grid-template-columns: 1fr;
  }

  .cover {
    min-height: 190px;
    border-right: 0;
    border-bottom: 1px solid var(--line);
  }
}
</style>
