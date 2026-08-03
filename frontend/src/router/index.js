import { createRouter, createWebHistory } from 'vue-router';

import MainLayout from '@/layouts/MainLayout.vue';
import { useAuthStore } from '@/stores/auth';

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'main',
        component: () => import('@/views/MainPageView.vue'),
      },
      {
        path: 'mypage',
        name: 'mypage',
        component: () => import('@/views/MyPageView.vue'),
        meta: {
          requiresAuth: true,
        },
      },
      {
        path: 'report/analysis/preview',
        redirect: {
          name: 'analysis-preview',
          params: { scenario: 'a' },
        },
      },
      {
        path: 'report/analysis/preview/:scenario([a-f])',
        name: 'analysis-preview',
        component: () => import('@/views/report/AnalysisView.vue'),
        meta: {
          analysisPreview: true,
        },
      },
      {
        path: 'report/analysis',
        name: 'analysis-create',
        component: () => import('@/views/report/AnalysisView.vue'),
        meta: {
          requiresAuth: true,
        },
      },
      {
        path: 'report/analysis/:analysisReportId(\\d+)',
        name: 'analysis',
        component: () => import('@/views/report/AnalysisView.vue'),
        meta: {
          requiresAuth: true,
        },
      },
      {
        path: 'analysis',
        redirect: {
          name: 'analysis-create',
        },
      },
      {
        path: 'analysis/:analysisReportId(\\d+)',
        redirect: (to) => ({
          name: 'analysis',
          params: { analysisReportId: to.params.analysisReportId },
        }),
      },
      {
        path: '/login',
        name: 'login',
        component: () => import('@/views/auth/LoginView.vue'),
      },
      {
        path: '/signup',
        name: 'signup',
        component: () => import('@/views/auth/SignupView.vue'),
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

router.beforeEach((to) => {
  const authStore = useAuthStore();

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    // 인증이 필요한 화면은 로그인 후 원래 경로로 돌아가도록 처리
    return {
      name: 'login',
      query: {
        redirect: to.fullPath,
      },
    };
  }

  return true;
});

export default router;
