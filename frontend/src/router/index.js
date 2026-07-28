import { createRouter, createWebHistory } from 'vue-router';

import { useAuthStore } from '@/stores/auth';

const routes = [
  {
    path: '/',
    name: 'main',
    component: () => import('@/App.vue'),
  },
  {
    // 세진: 로그인 화면 경로
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
  },
  {
    path: '/signup',
    name: 'signup',
    component: () => import('@/views/SignupView.vue'),
  },
  {
    path: '/mypage',
    name: 'mypage',
    component: () => import('@/views/MyPageView.vue'),
    meta: {
      requiresAuth: true,
    },
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

router.beforeEach((to) => {
  const authStore = useAuthStore();

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    // 세진: 인증이 필요한 화면은 로그인 후 원래 경로로 돌아가도록 처리한다.
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
