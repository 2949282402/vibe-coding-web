<script setup>
import { onMounted, ref } from 'vue';
import { fetchDashboardApi } from '../../api/admin';
import { usePreferencesStore } from '../../stores/preferences';

const preferences = usePreferencesStore();
const loading = ref(true);
const dashboard = ref({
  postCount: 0,
  categoryCount: 0,
  tagCount: 0,
  pendingCommentCount: 0,
  approvedCommentCount: 0,
  recentPosts: [],
  recentComments: []
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchDashboardApi();
    dashboard.value = res.data;
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);
</script>

<template>
  <div v-loading="loading" class="stack">
    <section class="card-grid">
      <article class="section-card metric-card">
        <span class="muted">{{ preferences.t('dashboard.totalPosts') }}</span>
        <strong>{{ dashboard.postCount }}</strong>
      </article>
      <article class="section-card metric-card">
        <span class="muted">{{ preferences.t('dashboard.categories') }}</span>
        <strong>{{ dashboard.categoryCount }}</strong>
      </article>
      <article class="section-card metric-card">
        <span class="muted">{{ preferences.t('dashboard.tags') }}</span>
        <strong>{{ dashboard.tagCount }}</strong>
      </article>
      <article class="section-card metric-card">
        <span class="muted">{{ preferences.t('dashboard.pendingComments') }}</span>
        <strong>{{ dashboard.pendingCommentCount }}</strong>
      </article>
    </section>

    <section class="split-grid">
      <div class="section-card panel">
        <div class="section-heading">
          <h2>{{ preferences.t('dashboard.recentPosts') }}</h2>
        </div>
        <div class="list-item" v-for="post in dashboard.recentPosts" :key="post.id">
          <strong>{{ post.title }}</strong>
          <span class="muted">{{ preferences.formatDateTime(post.updatedAt) }}</span>
        </div>
      </div>

      <div class="section-card panel">
        <div class="section-heading">
          <h2>{{ preferences.t('dashboard.latestComments') }}</h2>
        </div>
        <div class="list-item" v-for="comment in dashboard.recentComments" :key="comment.id">
          <strong>{{ comment.nickname }}</strong>
          <span class="muted">{{ comment.postTitle }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.stack {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.metric-card,
.panel {
  padding: 22px;
}

.metric-card strong {
  display: block;
  font-size: 2.4rem;
  margin-top: 12px;
}

.list-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--line);
}

@media (max-width: 960px) {
  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
