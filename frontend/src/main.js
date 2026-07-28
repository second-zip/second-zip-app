import { createApp, h } from 'vue';
import { createPinia } from 'pinia';
import { RouterView } from 'vue-router';

import { AUTH_UNAUTHORIZED_EVENT } from './api/instance';
import router from './router';
import { useAuthStore } from './stores/auth';

import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap';

import '@/assets/styles/colors.css';
import '@/assets/styles/fonts.css';
import '@/assets/styles/global.css';

const app = createApp({
  render: () => h(RouterView),
});
const pinia = createPinia();

app.use(pinia);
app.use(router);

window.addEventListener(AUTH_UNAUTHORIZED_EVENT, () => {
  const authStore = useAuthStore();

  authStore.clearAuth();

  if (router.currentRoute.value.name !== 'login') {
    router.replace({ name: 'login' });
  }
});

app.mount('#app');
