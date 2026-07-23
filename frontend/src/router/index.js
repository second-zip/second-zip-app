import { createRouter, createWebHistory } from 'vue-router';

import { useAuthStore } from '@/stores/auth';

const routes = [
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

// 보호된 화면은 Access Token이 있을 때만 접근을 허용한다.
router.beforeEach((to) => {
  const authStore = useAuthStore();

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    // TODO: 로그인 기능과 /login 라우트 명세가 확정되면 로그인 화면으로 이동한다.
    return false;
  }

  return true;
});

export default router;
