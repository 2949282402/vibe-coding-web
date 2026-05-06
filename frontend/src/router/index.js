import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../views/HomeView.vue') },
      {
        path: 'knowledge',
        name: 'knowledge',
        component: () => import('../views/KnowledgeView.vue'),
        meta: { immersive: true, hideFooter: true }
      },
      { path: 'posts/:slug', name: 'post-detail', component: () => import('../views/PostDetailView.vue') },
      { path: 'archives', name: 'archives', component: () => import('../views/ArchiveView.vue') },
      { path: 'categories', name: 'categories', component: () => import('../views/CategoriesView.vue') }
    ]
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'admin-dashboard', component: () => import('../views/admin/DashboardView.vue') },
      { path: 'posts', name: 'admin-posts', component: () => import('../views/admin/PostManageView.vue') },
      { path: 'posts/new', name: 'admin-post-create', component: () => import('../views/admin/PostEditorView.vue') },
      { path: 'posts/:id/edit', name: 'admin-post-edit', component: () => import('../views/admin/PostEditorView.vue') },
      { path: 'taxonomies', name: 'admin-taxonomies', component: () => import('../views/admin/TaxonomyManageView.vue') },
      { path: 'comments', name: 'admin-comments', component: () => import('../views/admin/CommentManageView.vue') },
      { path: 'rag-feedback', name: 'admin-rag-feedback', component: () => import('../views/admin/RagFeedbackManageView.vue') },
      { path: 'agent-drafts', name: 'admin-agent-drafts', component: () => import('../views/admin/AgentDraftReviewView.vue') },
      { path: 'agents', name: 'admin-agents', component: () => import('../views/admin/AgentOpsView.vue') },
      { path: 'agent-tool-calls', name: 'admin-agent-tool-calls', component: () => import('../views/admin/AgentToolCallsView.vue') }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return { name: 'home' };
  }
  if (to.name === 'login' && authStore.isAuthenticated) {
    return authStore.isAdmin ? { name: 'admin-dashboard' } : { name: 'home' };
  }
  return true;
});

export default router;