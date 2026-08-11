import { mount } from '@vue/test-utils';
import { describe, expect, test } from 'vitest';

import ReportCreateBox from './ReportCreateBox.vue';

const RouterLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a><slot /></a>',
};

describe('ReportCreateBox', () => {
  test('리포트 생성 안내와 이동 링크를 표시한다', () => {
    const wrapper = mount(ReportCreateBox, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    });

    expect(wrapper.text()).toContain('리포트를 만들어 보세요');
    expect(wrapper.text()).toContain('리포트 생성하기');
    expect(wrapper.getComponent(RouterLinkStub).props('to')).toBe(
      '/reports/create',
    );
    expect(wrapper.get('.report-create-box').classes()).toContain('w-100');
  });
});
