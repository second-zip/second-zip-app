import { mount } from '@vue/test-utils';
import { describe, expect, test } from 'vitest';

import CheckIcon from '@/assets/icons/checklist/check-white-14.svg';
import ChecklistItem from './ChecklistItem.vue';

const makeItem = (checked = false) => ({
  id: 3,
  title: '등기부등본 확인',
  description: '권리 관계를 확인해 보세요.',
  checked,
});

describe('ChecklistItem', () => {
  test('미체크 상태를 접근 가능한 checkbox로 표시한다', () => {
    const wrapper = mount(ChecklistItem, { props: { item: makeItem() } });
    const input = wrapper.get('input[type="checkbox"]');

    expect(input.attributes('id')).toBe('checklist-item-3');
    expect(input.element.checked).toBe(false);
    expect(wrapper.classes()).not.toContain('checklist-item--checked');
    expect(wrapper.find('.checklist-item__checkbox img').exists()).toBe(false);
  });

  test('체크 상태에서 강조 클래스와 체크 아이콘을 표시한다', () => {
    const wrapper = mount(ChecklistItem, {
      props: { item: makeItem(true) },
    });

    expect(wrapper.classes()).toContain('checklist-item--checked');
    expect(wrapper.get('input').element.checked).toBe(true);
    expect(wrapper.get('.checklist-item__checkbox').classes())
      .toContain('checklist-item__checkbox--checked');
    expect(wrapper.get('.checklist-item__checkbox img').attributes('src'))
      .toBe(CheckIcon);
  });

  test('checkbox 변경은 toggle 이벤트만 전달한다', async () => {
    const wrapper = mount(ChecklistItem, { props: { item: makeItem() } });
    await wrapper.get('input').trigger('change');

    expect(wrapper.emitted('toggle')).toHaveLength(1);
    expect(wrapper.emitted('show-description')).toBeUndefined();
  });

  test('물음표 클릭은 체크를 변경하지 않고 설명 이벤트만 전달한다', async () => {
    const wrapper = mount(ChecklistItem, { props: { item: makeItem() } });
    const help = wrapper.get('.checklist-item__help');
    expect(help.attributes('aria-label')).toContain('등기부등본 확인');

    await help.trigger('click');
    expect(wrapper.emitted('show-description')).toHaveLength(1);
    expect(wrapper.emitted('toggle')).toBeUndefined();
  });
});
