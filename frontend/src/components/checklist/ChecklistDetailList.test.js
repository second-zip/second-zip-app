import { mount } from '@vue/test-utils';
import { describe, expect, test } from 'vitest';

import ChecklistItem from './ChecklistItem.vue';
import ChecklistDetailList from './ChecklistDetailList.vue';

const items = [
  { id: 1, category: 'HOUSE', title: '유형 1', checked: false },
  { id: 2, category: 'HOUSE', title: '유형 2', checked: false },
  { id: 3, category: 'COMMON', title: '공통 1', checked: true },
  { id: 4, category: 'COMMON', title: '공통 2', checked: false },
];

describe('ChecklistDetailList', () => {
  test.each([
    [{ isLoading: true }, '체크리스트를 불러오는 중입니다.', 'status'],
    [{ loadErrorMessage: '조회 실패' }, '조회 실패', 'alert'],
    [{ items: [] }, '확인할 체크리스트 항목이 없습니다.', null],
  ])('목록 피드백 상태를 표시한다', (props, message, role) => {
    const wrapper = mount(ChecklistDetailList, { props });
    expect(wrapper.text()).toContain(message);
    if (role) expect(wrapper.get(`[role="${role}"]`).exists()).toBe(true);
  });

  test('백엔드 순서를 유지하며 유형별과 공통 라벨을 한 번씩 표시한다', () => {
    const wrapper = mount(ChecklistDetailList, { props: { items } });

    expect(wrapper.findAll('.checklist-detail-list__label').map((label) =>
      label.text())).toEqual(['유형별', '공통']);
    expect(wrapper.findAllComponents(ChecklistItem).map((item) =>
      item.props('item').id)).toEqual([1, 2, 3, 4]);
  });

  test('항목 이벤트를 ID와 원본 항목으로 상위에 전달한다', async () => {
    const wrapper = mount(ChecklistDetailList, { props: { items } });
    const first = wrapper.findAllComponents(ChecklistItem)[0];

    first.vm.$emit('toggle');
    first.vm.$emit('show-description');
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted('toggle')).toEqual([[1]]);
    expect(wrapper.emitted('show-description')).toEqual([[items[0]]]);
  });

  test('항목 동작 오류를 목록 아래에 표시한다', () => {
    const wrapper = mount(ChecklistDetailList, {
      props: { items, actionErrorMessage: '변경 실패' },
    });
    expect(wrapper.get('[role="alert"]').text()).toBe('변경 실패');
  });
});
