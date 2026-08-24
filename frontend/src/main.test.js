import { beforeEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
  app: { mount: vi.fn(), use: vi.fn() },
  authStore: { clearAuth: vi.fn() },
  createApp: vi.fn(),
  h: vi.fn(),
  pinia: { id: 'pinia' },
  router: {
    currentRoute: { value: { name: 'main' } },
    replace: vi.fn(),
  },
}));

vi.mock('vue', () => ({
  createApp: mocks.createApp,
  h: mocks.h,
}));
vi.mock('pinia', () => ({ createPinia: vi.fn(() => mocks.pinia) }));
vi.mock('vue-router', () => ({ RouterView: { name: 'RouterView' } }));
vi.mock('./api/instance', () => ({
  AUTH_UNAUTHORIZED_EVENT: 'auth:unauthorized',
}));
vi.mock('./router', () => ({ default: mocks.router }));
vi.mock('./stores/auth', () => ({ useAuthStore: () => mocks.authStore }));
vi.mock('bootstrap', () => ({}));

describe('앱 진입점', () => {
  beforeEach(async () => {
    vi.resetModules();
    vi.clearAllMocks();
    mocks.createApp.mockReturnValue(mocks.app);
    mocks.router.currentRoute.value = { name: 'main' };
    await import('./main');
  });

  it('Pinia와 라우터를 등록하고 앱을 마운트한다', () => {
    expect(mocks.createApp).toHaveBeenCalledOnce();
    expect(mocks.app.use).toHaveBeenNthCalledWith(1, mocks.pinia);
    expect(mocks.app.use).toHaveBeenNthCalledWith(2, mocks.router);
    expect(mocks.app.mount).toHaveBeenCalledWith('#app');

    const rootComponent = mocks.createApp.mock.calls[0][0];
    rootComponent.render();
    expect(mocks.h).toHaveBeenCalled();
  });

  it('인증 만료 이벤트를 받으면 인증을 지우고 로그인으로 이동한다', () => {
    window.dispatchEvent(new CustomEvent('auth:unauthorized'));

    expect(mocks.authStore.clearAuth).toHaveBeenCalled();
    expect(mocks.router.replace).toHaveBeenCalledWith({ name: 'login' });
  });

  it('이미 로그인 화면이면 중복 이동하지 않는다', () => {
    mocks.router.currentRoute.value = { name: 'login' };

    window.dispatchEvent(new CustomEvent('auth:unauthorized'));

    expect(mocks.authStore.clearAuth).toHaveBeenCalled();
    expect(mocks.router.replace).not.toHaveBeenCalled();
  });
});
