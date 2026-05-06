<script setup>
import { computed } from 'vue';
import { usePreferencesStore } from '../stores/preferences';

const props = defineProps({
  post: {
    type: Object,
    required: true
  }
});

const preferences = usePreferencesStore();

const copy = computed(() =>
  preferences.locale === 'zh-CN'
    ? {
        tagCount: (count) => `${count} 个标签`
      }
    : {
        tagCount: (count) => `${count} tags`
      }
);

const tagPreview = computed(() => (Array.isArray(props.post.tags) ? props.post.tags.slice(0, 3) : []));
const coverStyle = computed(() => ({
  backgroundImage: props.post.coverImage
    ? `linear-gradient(180deg, rgba(7, 8, 11, 0.06), rgba(7, 8, 11, 0.72)), url(${props.post.coverImage})`
    : 'linear-gradient(135deg, rgba(255, 255, 255, 0.12), rgba(110, 110, 110, 0.08) 48%, rgba(15, 16, 19, 0.92))'
}));
</script>

<template>
  <router-link :to="`/posts/${post.slug}`" class="post-card section-card">
    <div class="post-card-cover" :style="coverStyle">
      <div class="post-card-overlay">
        <span class="post-card-category">{{ post.categoryName || preferences.t('post.uncategorized') }}</span>
        <span class="post-card-date">{{ preferences.formatDate(post.publishedAt) }}</span>
      </div>
    </div>

    <div class="post-card-body">
      <div class="post-card-main">
        <h3>{{ post.title }}</h3>
        <p>{{ post.summary }}</p>
      </div>

      <div class="post-card-footer">
        <div class="post-card-meta muted">
          <span>{{ preferences.t('post.views', { count: post.viewCount }) }}</span>
          <span v-if="post.tags?.length">{{ copy.tagCount(post.tags.length) }}</span>
        </div>
        <div class="chip-list post-card-tags">
          <span v-for="tag in tagPreview" :key="tag" class="chip"># {{ tag }}</span>
        </div>
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.post-card {
  display: grid;
  min-height: 100%;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.03)),
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.08), transparent 24%),
    radial-gradient(circle at 18% 0%, rgba(255, 255, 255, 0.04), transparent 22%),
    linear-gradient(180deg, rgba(16, 20, 28, 0.72), rgba(10, 13, 18, 0.78));
  transition: transform 0.34s var(--ease-liquid), border-color 0.34s var(--ease-soft), box-shadow 0.34s var(--ease-liquid), background 0.34s var(--ease-soft);
  backdrop-filter: blur(28px) saturate(138%);
  -webkit-backdrop-filter: blur(28px) saturate(138%);
  animation: post-card-rise 0.72s var(--ease-soft);
  box-shadow: var(--glass-edge), 0 24px 50px rgba(2, 5, 12, 0.22);
}

.post-card:hover {
  transform: translateY(-8px) scale(1.01);
  border-color: var(--line-strong);
  box-shadow: var(--glass-edge), 0 28px 56px rgba(2, 5, 12, 0.28);
}

.post-card-cover {
  min-height: 236px;
  display: flex;
  align-items: flex-end;
  padding: 18px;
  background-position: center;
  background-size: cover;
  border-bottom: 1px solid var(--line);
}

.post-card-overlay {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.05)),
    rgba(10, 13, 19, 0.42);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(22px) saturate(135%);
  -webkit-backdrop-filter: blur(22px) saturate(135%);
  box-shadow: var(--glass-edge);
}

html[data-theme='light'] .post-card-overlay {
  background: rgba(255, 255, 255, 0.82);
  border-color: rgba(0, 0, 0, 0.08);
}

.post-card-category,
.post-card-date,
.post-card-meta {
  font-size: 0.74rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.post-card-body {
  display: grid;
  gap: 20px;
  padding: 24px 24px 26px;
}

.post-card-main {
  display: grid;
  gap: 12px;
}

.post-card-main h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(1.48rem, 2.4vw, 2rem);
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.post-card-main p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.86;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-card-footer {
  display: grid;
  gap: 12px;
}

.post-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
}

.post-card-tags {
  gap: 8px;
}

.post-card-tags :deep(.chip) {
  padding-inline: 12px;
}

@keyframes post-card-rise {
  0% {
    opacity: 0;
    transform: translateY(22px) scale(0.985);
  }

  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 720px) {
  .post-card-cover {
    min-height: 208px;
  }

  .post-card-body {
    padding: 18px 18px 20px;
  }
}
</style>
