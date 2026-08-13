import { describe, expect, test } from 'vitest';

import router from './index';

describe('checklist routes', () => {
  test('목록과 상세를 /checklist 부모의 children으로 구성한다', () => {
    const list = router.resolve({ name: 'checklist-list' });
    const detail = router.resolve({
      name: 'checklist-detail', params: { reportChecklistId: 25 },
    });

    expect(list.matched.map(({ path }) => path))
      .toEqual(['/', '/checklist', '/checklist']);
    expect(detail.matched.map(({ path }) => path))
      .toEqual(['/', '/checklist', '/checklist/:reportChecklistId(\\d+)']);
  });

  test('목록과 숫자 ID 상세 경로를 올바르게 해석한다', () => {
    const list = router.resolve({ name: 'checklist-list' });
    const detail = router.resolve({
      name: 'checklist-detail',
      params: { reportChecklistId: 25 },
    });

    expect(list.path).toBe('/checklist');
    expect(detail.path).toBe('/checklist/25');
    expect(list.meta.requiresAuth).toBe(true);
    expect(detail.meta.requiresAuth).toBe(true);
  });
});
