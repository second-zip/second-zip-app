import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import App from './App.vue';

describe('App', () => {
  it('현재 라우트 화면을 렌더링한다', () => {
    const wrapper = mount(App, {
      global: {
        stubs: { RouterView: { template: '<main data-test="route-view" />' } },
      },
    });

    expect(wrapper.get('[data-test="route-view"]').exists()).toBe(true);
  });
});
