<script setup>
import { computed, onMounted, ref } from 'vue';
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
  ragFeedback: {
    answerCount: 0,
    feedbackCount: 0,
    helpfulCount: 0,
    needsWorkCount: 0,
    coverageRate: 0,
    helpfulRate: 0,
    recentFeedback: []
  },
  recentPosts: [],
  recentComments: []
});

const ragCopy = computed(() => {
  if (preferences.locale === 'zh-CN') {
    return {
      title: 'RAG 反馈',
      answers: 'RAG 回答数',
      captured: '已收集反馈',
      helpful: '有帮助',
      needsWork: '需改进',
      coverage: '反馈覆盖率',
      helpfulRate: '正向率',
      recent: '最近反馈',
      empty: '还没有 RAG 反馈记录',
      positive: '有帮助',
      negative: '需改进',
      noNote: '未填写备注'
    };
  }

  return {
    title: 'RAG Feedback',
    answers: 'RAG Answers',
    captured: 'Feedback Captured',
    helpful: 'Helpful',
    needsWork: 'Needs Work',
    coverage: 'Coverage',
    helpfulRate: 'Helpful Rate',
    recent: 'Recent Feedback',
    empty: 'No RAG feedback yet.',
    positive: 'Helpful',
    negative: 'Needs Work',
    noNote: 'No note provided'
  };
});

const ragFeedbackCards = computed(() => [
  {
    key: 'answers',
    label: ragCopy.value.answers,
    value: dashboard.value.ragFeedback.answerCount || 0
  },
  {
    key: 'captured',
    label: ragCopy.value.captured,
    value: dashboard.value.ragFeedback.feedbackCount || 0
  },
  {
    key: 'helpful',
    label: ragCopy.value.helpful,
    value: dashboard.value.ragFeedback.helpfulCount || 0
  },
  {
    key: 'needs-work',
    label: ragCopy.value.needsWork,
    value: dashboard.value.ragFeedback.needsWorkCount || 0
  }
]);

async function loadData() {
  loading.value = true;
  try {
    const res = await fetchDashboardApi();
    dashboard.value = res.data;
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>

<template>
  <div v-loading="loading" class="admin-page-stack">
    <section class="section-card admin-surface admin-panel">
      <div class="admin-page-head">
        <div>
          <span class="admin-kicker">Overview</span>
          <h2>{{ preferences.t('admin.menuOverview') }}</h2>
          <p class="muted">{{ preferences.t('dashboard.recentPosts') }} · {{ preferences.t('dashboard.latestComments') }}</p>
        </div>
        <span class="admin-badge">{{ preferences.t('dashboard.pendingComments') }} · {{ dashboard.pendingCommentCount }}</span>
      </div>
    </section>

    <section class="card-grid">
      <article class="section-card admin-surface admin-panel metric-card">
        <span class="muted">{{ preferences.t('dashboard.totalPosts') }}</span>
        <strong>{{ dashboard.postCount }}</strong>
      </article>
      <article class="section-card admin-surface admin-panel metric-card">
        <span class="muted">{{ preferences.t('dashboard.categories') }}</span>
        <strong>{{ dashboard.categoryCount }}</strong>
      </article>
      <article class="section-card admin-surface admin-panel metric-card">
        <span class="muted">{{ preferences.t('dashboard.tags') }}</span>
        <strong>{{ dashboard.tagCount }}</strong>
      </article>
      <article class="section-card admin-surface admin-panel metric-card metric-card-accent">
        <span class="muted">{{ preferences.t('dashboard.pendingComments') }}</span>
        <strong>{{ dashboard.pendingCommentCount }}</strong>
      </article>
    </section>

    <section class="split-grid">
      <div class="section-card admin-surface admin-panel">
        <div class="section-heading refined-heading">
          <h2>{{ preferences.t('dashboard.recentPosts') }}</h2>
        </div>
        <div class="admin-list">
          <div class="admin-list-row" v-for="post in dashboard.recentPosts" :key="post.id">
            <div class="admin-list-copy">
              <strong>{{ post.title }}</strong>
              <p class="muted">{{ preferences.formatDateTime(post.updatedAt) }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="section-card admin-surface admin-panel">
        <div class="section-heading refined-heading">
          <h2>{{ preferences.t('dashboard.latestComments') }}</h2>
        </div>
        <div class="admin-list">
          <div class="admin-list-row" v-for="comment in dashboard.recentComments" :key="comment.id">
            <div class="admin-list-copy">
              <strong>{{ comment.nickname }}</strong>
              <p class="muted">{{ comment.postTitle }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="section-card admin-surface admin-panel rag-panel">
      <div class="section-heading refined-heading rag-panel-head">
        <div>
          <span class="admin-kicker">{{ ragCopy.title }}</span>
          <h2>{{ ragCopy.title }}</h2>
        </div>
        <div class="rag-rates">
          <span class="admin-badge">{{ ragCopy.coverage }} {{ dashboard.ragFeedback.coverageRate }}%</span>
          <span class="admin-badge">{{ ragCopy.helpfulRate }} {{ dashboard.ragFeedback.helpfulRate }}%</span>
        </div>
      </div>

      <div class="rag-metric-grid">
        <article
          v-for="item in ragFeedbackCards"
          :key="item.key"
          class="rag-metric"
        >
          <span class="muted">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>

      <div class="section-heading refined-heading recent-feedback-head">
        <h2>{{ ragCopy.recent }}</h2>
      </div>
      <div v-if="dashboard.ragFeedback.recentFeedback.length" class="admin-list rag-feedback-list">
        <div
          v-for="feedback in dashboard.ragFeedback.recentFeedback"
          :key="feedback.id"
          class="admin-list-row rag-feedback-row"
        >
          <div class="admin-list-copy">
            <div class="feedback-row-head">
              <strong>{{ feedback.helpful ? ragCopy.positive : ragCopy.negative }}</strong>
              <span class="feedback-mode muted">{{ feedback.mode || 'retrieval' }}</span>
            </div>
            <p class="muted feedback-note">{{ feedback.note || ragCopy.noNote }}</p>
            <p class="feedback-preview">{{ feedback.answerPreview }}</p>
          </div>
          <div class="feedback-row-meta muted">
            <span>{{ feedback.sessionId }}</span>
            <span>{{ preferences.formatDateTime(feedback.feedbackAt) }}</span>
          </div>
        </div>
      </div>
      <p v-else class="muted">{{ ragCopy.empty }}</p>
    </section>
  </div>
</template>

<style scoped>
.card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.metric-card {
  min-height: 170px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.metric-card strong {
  display: block;
  font-size: clamp(2.3rem, 4vw, 3rem);
  margin-top: 18px;
  letter-spacing: -0.06em;
}

.metric-card-accent {
  border-color: var(--line-strong);
}

.refined-heading {
  margin-bottom: 18px;
}

.rag-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.rag-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.rag-rates {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.rag-metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.rag-metric {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.rag-metric strong {
  display: block;
  margin-top: 12px;
  font-size: clamp(1.8rem, 3vw, 2.4rem);
  letter-spacing: -0.05em;
}

.recent-feedback-head {
  margin-bottom: 0;
}

.rag-feedback-list {
  gap: 14px;
}

.rag-feedback-row {
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
}

.feedback-row-head,
.feedback-row-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 10px;
}

.feedback-mode {
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.78rem;
}

.feedback-note,
.feedback-preview {
  margin: 8px 0 0;
}

.feedback-preview {
  line-height: 1.7;
}

@media (max-width: 960px) {
  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .rag-metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .card-grid {
    grid-template-columns: 1fr;
  }

  .rag-panel-head,
  .feedback-row-head,
  .feedback-row-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .rag-metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
