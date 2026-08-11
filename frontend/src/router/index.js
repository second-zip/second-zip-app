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
        path: 'dictionary',
        name: 'dictionary',
        component: () => import('@/views/dictionary/DictionaryView.vue'),
      },
      {
        path: 'dictionary/words',
        name: 'dictionary-words',
        component: () => import('@/views/dictionary/WordDictionaryView.vue'),
      },
      {
        path: 'dictionary/fraud',
        name: 'dictionary-fraud',
        component: () => import('@/views/dictionary/FraudDictionaryView.vue'),
      },
      {
        path: 'dictionary/fraud/:typeId',
        name: 'dictionary-fraud-video',
        component: () => import('@/views/dictionary/FraudVideoView.vue'),
      },
      {
        path: 'dictionary/register',
        name: 'dictionary-register',
        component: () => import('@/views/dictionary/DictionaryGuideView.vue'),
        meta: { guideType: 'register' },
      },
      {
        path: 'dictionary/move-in',
        name: 'dictionary-move-in',
        component: () => import('@/views/dictionary/DictionaryGuideView.vue'),
        meta: { guideType: 'moveIn' },
      },
      {
        path: 'checklist',
        children: [
          {
            path: '',
            name: 'checklist-list',
            component: () => import('@/views/checklist/ChecklistListView.vue'),
            meta: { requiresAuth: true },
          },
          {
            path: ':analysisReportId(\\d+)',
            name: 'checklist-detail',
            component: () =>
              import('@/views/checklist/ChecklistDetailView.vue'),
            meta: { requiresAuth: true },
          },
        ],
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
        path: 'mypage/profile',
        name: 'mypage-profile',
        component: () => import('@/views/mypage/ProfileEditView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'mypage/secretary',
        name: 'mypage-secretary',
        component: () => import('@/views/mypage/SecretaryChangeView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'mypage/withdraw',
        name: 'mypage-withdraw',
        component: () => import('@/views/mypage/WithdrawView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'report',
        children: [
          {
            path: '',
            name: 'report-list',
            component: () => import('@/views/report/ReportListView.vue'),
          },
          {
            path: 'create',
            name: 'report-create',
            component: () => import('@/views/report/ReportCreateView.vue'),
          },
          {
            path: 'analysis/progress',
            name: 'analysis-progress',
            component: () =>
              import('@/views/report/AnalysisProgressView.vue'),
            meta: {
              requiresAuth: true,
            },
          },
          {
            path: 'analysis/preview',
            redirect: {
              name: 'analysis-preview',
              params: { scenario: 'a' },
            },
          },
          {
            path: 'analysis/preview/:scenario([a-f])',
            name: 'analysis-preview',
            component: () => import('@/views/report/AnalysisView.vue'),
            meta: {
              analysisPreview: true,
            },
          },
          {
            path: 'analysis',
            name: 'analysis-create',
            component: () => import('@/views/report/AnalysisView.vue'),
            meta: {
              requiresAuth: true,
            },
          },
          {
            path: 'analysis/:analysisReportId(\\d+)',
            name: 'analysis',
            component: () => import('@/views/report/AnalysisView.vue'),
            meta: {
              requiresAuth: true,
            },
          },
        ],
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
