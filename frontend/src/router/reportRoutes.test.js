import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import router from './index';

describe('report routes', () => {
  beforeEach(() => {
    vi.stubGlobal('localStorage', {
      getItem: vi.fn(() => null),
      removeItem: vi.fn(),
      setItem: vi.fn(),
    });
    setActivePinia(createPinia());
  });

  test.each(['/report/analysis', '/analysis'])(
    'ID 없는 분석 경로 %s는 분석 생성 화면으로 보낸다',
    async (path) => {
      await router.push(path);
      await router.isReady();

      expect(router.currentRoute.value.name).toBe('report-create');
    },
  );

  test('공유 토큰을 비로그인 열람 경로로 해석한다', () => {
    const shared = router.resolve({
      name: 'analysis-shared',
      params: { shareToken: 'share-token' },
    });

    expect(shared.path).toBe('/report/shared/share-token');
    expect(shared.meta.analysisShared).toBe(true);
    expect(shared.meta.requiresAuth).toBeUndefined();
  });

  test('비로그인 사용자는 인증 필수 화면에서 로그인으로 이동한다', async () => {
    await router.push('/mypage');

    expect(router.currentRoute.value.name).toBe('login');
    expect(router.currentRoute.value.query.redirect).toBe('/mypage');
  });

  test('모든 지연 로딩 화면 모듈을 정상적으로 불러온다', async () => {
    const loaders = router
      .getRoutes()
      .map((route) => route.components?.default)
      .filter((component) => typeof component === 'function');

    const modules = await Promise.all(loaders.map((load) => load()));

    expect(loaders.length).toBeGreaterThan(15);
    expect(modules.every((module) => module.default)).toBe(true);
  });

  test('이전 분석 상세 URL의 ID를 현재 라우트로 전달한다', () => {
    const legacyRoute = router.getRoutes().find(
      ({ path }) => path === '/analysis/:analysisReportId(\\d+)',
    );

    expect(legacyRoute.redirect({ params: { analysisReportId: '27' } })).toEqual({
      name: 'analysis',
      params: { analysisReportId: '27' },
    });
  });
});
