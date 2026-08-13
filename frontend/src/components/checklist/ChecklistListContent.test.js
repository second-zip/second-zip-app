import { mount } from '@vue/test-utils';
import { describe, expect, test } from 'vitest';

import ChecklistListContent from './ChecklistListContent.vue';

const reports = [
  { analysisReportId: 1, roadAddress: '서울시 마포구' },
  { analysisReportId: 2, roadAddress: '서울시 강남구' },
];

const ItemStub = {
  name: 'ChecklistReportItem',
  props: ['report', 'isCreating'],
  emits: ['create'],
  template: `
    <button class="item-stub" :data-loading="isCreating"
      @click="$emit('create', report)">{{ report.analysisReportId }}</button>
  `,
};

const mountContent = (props = {}) => mount(ChecklistListContent, {
  props,
  global: { stubs: { ChecklistReportItem: ItemStub } },
});

describe('ChecklistListContent', () => {
  test.each([
    [{ isLoading: true }, '체크리스트 목록을 불러오는 중입니다.', 'status'],
    [{ errorMessage: '목록 조회 실패' }, '목록 조회 실패', 'alert'],
    [{ checklists: [] }, '체크리스트를 만들 리포트가 없습니다.', null],
  ])('목록 피드백 상태를 표시한다', (props, message, role) => {
    const wrapper = mountContent(props);
    expect(wrapper.text()).toContain(message);
    if (role) expect(wrapper.get(`[role="${role}"]`).exists()).toBe(true);
  });

  test('실제 개수와 리포트별 생성 진행 상태를 전달한다', () => {
    const wrapper = mountContent({
      checklists: reports,
      creatingReportIds: [2],
    });

    expect(wrapper.text()).toContain('2건');
    const rendered = wrapper.findAll('.item-stub');
    expect(rendered).toHaveLength(2);
    expect(rendered[0].attributes('data-loading')).toBe('false');
    expect(rendered[1].attributes('data-loading')).toBe('true');
  });

  test('리포트 생성 이벤트와 생성 오류를 상위로 전달한다', async () => {
    const wrapper = mountContent({
      checklists: reports,
      creationErrorMessage: '체크리스트 생성 실패',
    });
    await wrapper.findAll('.item-stub')[0].trigger('click');

    expect(wrapper.emitted('create')).toEqual([[reports[0]]]);
    expect(wrapper.get('.checklist-list__error').text())
      .toBe('체크리스트 생성 실패');
  });
});
