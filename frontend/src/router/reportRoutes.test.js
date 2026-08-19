import { describe, expect, test } from 'vitest';

import router from './index';

describe('report routes', () => {
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
