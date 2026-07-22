import { createApp, h } from 'vue';
import { createPinia } from 'pinia';
import { RouterView } from 'vue-router';

import { AUTH_UNAUTHORIZED_EVENT } from './api/instance';
import router from './router';
import { useAuthStore } from './stores/auth';

// App.vue를 수정하지 않고 현재 경로의 화면을 루트에 렌더링한다.
const app = createApp({
  render: () => h(RouterView),
});
const pinia = createPinia();

app.use(pinia);
app.use(router);

// 401 응답을 한 곳에서 처리해 Pinia와 localStorage의 인증 상태를 초기화한다.
window.addEventListener(AUTH_UNAUTHORIZED_EVENT, () => {
  const authStore = useAuthStore();

  authStore.clearAuth();

  // TODO: 로그인 기능과 /login 라우트 명세가 확정되면 router.replace로 이동한다.
});

app.mount('#app');
