import { mount } from '@vue/test-utils';
import { describe, expect, test } from 'vitest';

import { REPORT_ACTION_ICONS } from '@/constants/report/list';
import ReportListItemActions from './ReportListItemActions.vue';

const RouterLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a class="router-link-stub"><slot /></a>',
};
const report = { analysisReportId: 7, favorite: false };

describe('ReportListItemActions', () => {
  test('즐겨찾기와 삭제 이벤트를 리포트와 함께 전달한다', async () => {
    const wrapper = mount(ReportListItemActions, {
      props: { report, address: '서울시 송파구' },
      global: { stubs: { RouterLink: RouterLinkStub } },
    });
    const buttons = wrapper.findAll('button');

    await buttons[0].trigger('click');
    await buttons[1].trigger('click');

    expect(wrapper.emitted('toggle-favorite')[0]).toEqual([report]);
    expect(wrapper.emitted('delete')[0]).toEqual([report]);
  });

  test('즐겨찾기·삭제 활성 아이콘과 상세 route를 반영한다', async () => {
    const wrapper = mount(ReportListItemActions, {
      props: { report, address: '서울시 송파구' },
      global: { stubs: { RouterLink: RouterLinkStub } },
    });
    const buttons = wrapper.findAll('button');

    expect(buttons[0].get('img').attributes('src')).toBe(
      REPORT_ACTION_ICONS.favorite,
    );
    await wrapper.setProps({ report: { ...report, favorite: true } });
    expect(buttons[0].get('img').attributes('src')).toBe(
      REPORT_ACTION_ICONS.favoriteActive,
    );

    await buttons[1].trigger('mouseenter');
    expect(buttons[1].get('img').attributes('src')).toBe(
      REPORT_ACTION_ICONS.deleteActive,
    );
    await buttons[1].trigger('mouseleave');
    expect(buttons[1].get('img').attributes('src')).toBe(
      REPORT_ACTION_ICONS.delete,
    );
    await buttons[1].trigger('focus');
    expect(buttons[1].get('img').attributes('src')).toBe(
      REPORT_ACTION_ICONS.deleteActive,
    );
    await buttons[1].trigger('blur');
    expect(buttons[1].get('img').attributes('src')).toBe(
      REPORT_ACTION_ICONS.delete,
    );
    expect(wrapper.getComponent(RouterLinkStub).props('to')).toEqual({
      name: 'analysis',
      params: { analysisReportId: 7 },
    });
  });
});
