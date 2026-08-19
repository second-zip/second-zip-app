import { mount } from '@vue/test-utils';
import { describe, expect, test } from 'vitest';

import ReportListItem from './ReportListItem.vue';

const RouterLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a><slot /></a>',
};
const report = {
  analysisReportId: 7,
  roadAddress: '서울특별시 송파구 송파대로 123',
  detailAddress: '101동 101호',
  result: 'DANGER',
  favorite: true,
  createdAt: [2026, 8, 6, 10, 30],
};

describe('ReportListItem', () => {
  test('주소 전체·생성일·상태·즐겨찾기를 실제 필드로 표시한다', () => {
    const wrapper = mount(ReportListItem, {
      props: { report },
      global: { stubs: { RouterLink: RouterLinkStub } },
    });

    expect(wrapper.get('.report-list-item__address').text()).toBe(
      '서울특별시 송파구 송파대로 123 101동 101호',
    );
    expect(wrapper.get('.report-list-item__address').classes())
      .not.toContain('text-truncate');
    expect(wrapper.get('time').text()).toBe('2026. 8. 6');
    expect(wrapper.get('time').attributes('datetime')).toBe('2026-08-06');
    expect(wrapper.get('.status').classes()).toContain('status--danger');
    expect(wrapper.get('[aria-label="즐겨찾기 해제"]')).toBeTruthy();
  });

  test('액션 이벤트를 상위 목록으로 전달한다', async () => {
    const wrapper = mount(ReportListItem, {
      props: { report },
      global: { stubs: { RouterLink: RouterLinkStub } },
    });

    await wrapper.get('[aria-label="즐겨찾기 해제"]').trigger('click');
    await wrapper.get('[aria-label="리포트 삭제"]').trigger('click');

    expect(wrapper.emitted('toggle-favorite')[0]).toEqual([report]);
    expect(wrapper.emitted('delete')[0]).toEqual([report]);
  });
});
