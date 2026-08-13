import { mount } from '@vue/test-utils';
import { describe, expect, test } from 'vitest';

import ReportListStatusIcon from '@/components/report/list/ReportListStatusIcon.vue';
import ChecklistReportItem from './ChecklistReportItem.vue';
import CircularProgress from './CircularProgress.vue';

const makeReport = (overrides = {}) => ({
  analysisReportId: 2,
  checklistCreated: true,
  detailAddress: '101호',
  progressPercentage: 43,
  reportChecklistId: 9,
  reportCreatedAt: '2026-08-13T09:00:00',
  riskLevel: 'CAUTION',
  roadAddress: '서울시 마포구 합정동',
  ...overrides,
});

const mountItem = (report) => mount(ChecklistReportItem, {
  props: { report },
  global: {
    stubs: {
      RouterLink: {
        name: 'RouterLink',
        props: ['to'],
        template: '<a class="router-link"><slot /></a>',
      },
    },
  },
});

describe('ChecklistReportItem', () => {
  test('목록 API의 위험도·주소·진행률 필드를 표시한다', () => {
    const wrapper = mountItem(makeReport());

    expect(wrapper.findComponent(ReportListStatusIcon).props('result'))
      .toBe('CAUTION');
    expect(wrapper.text()).toContain('서울시 마포구 합정동 101호');
    expect(wrapper.text()).toContain('43%');
    expect(wrapper.findComponent(CircularProgress).props())
      .toMatchObject({ value: 43, size: 20 });
    expect(wrapper.findComponent({ name: 'RouterLink' }).props('to'))
      .toEqual({ name: 'checklist-detail', params: { reportChecklistId: 9 } });
  });

  test('진행률을 0~100 범위로 제한한다', () => {
    expect(mountItem(makeReport({ progressPercentage: 150 })).text())
      .toContain('100%');
    expect(mountItem(makeReport({ progressPercentage: -10 })).text())
      .toContain('0%');
  });

  test('미생성 리포트는 생성 버튼으로 상위 이벤트를 전달한다', async () => {
    const report = makeReport({ checklistCreated: false });
    const wrapper = mountItem(report);

    expect(wrapper.find('.checklist-report-item__progress').exists()).toBe(false);
    await wrapper.get('.checklist-create-button').trigger('click');
    expect(wrapper.emitted('create')).toEqual([[report]]);
  });
});
