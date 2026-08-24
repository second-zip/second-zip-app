import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';

import { useCounterStore } from './counter';

describe('counter store', () => {
  beforeEach(() => setActivePinia(createPinia()));

  it('카운트를 증가시키고 두 배 값을 계산한다', () => {
    const store = useCounterStore();

    store.increment();

    expect(store.count).toBe(1);
    expect(store.doubleCount).toBe(2);
  });
});
