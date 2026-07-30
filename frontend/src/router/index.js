import MainLayout from '@/layouts/MainLayout.vue';
import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: 'mypage',
        name: 'mypage',
        component: () => import('@/views/MyPageView.vue'),
        meta: {
          requiresAuth: true,
        },
      },
      {
        path: 'test',
        name: 'test',
        component: () => import('@/views/test/TestView.vue'),
      },
      // 로그인·회원가입에는 하단 네비게이션을 사용하지 않는 경우
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
