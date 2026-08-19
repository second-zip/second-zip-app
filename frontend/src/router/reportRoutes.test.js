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
});
