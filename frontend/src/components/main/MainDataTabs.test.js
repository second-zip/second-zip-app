import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import MainDataTabs from './MainDataTabs.vue';

describe('MainDataTabs', () => {
  it('피해주택 탭을 기본 활성 상태로 표시한다', () => {
    const wrapper = mount(MainDataTabs);
    const buttons = wrapper.findAll('button');

    expect(buttons).toHaveLength(2);
    expect(buttons[0].classes()).toContain('data-tabs__button--damage');
    expect(buttons[1].classes()).not.toContain('data-tabs__button--price');
  });

  it.each([
    [0, 'fraud-damage'],
    [1, 'price-index'],
  ])('%i번째 탭 클릭 시 %s 값을 emit한다', async (index, value) => {
    const wrapper = mount(MainDataTabs, {
      props: { modelValue: 'price-index' },
    });

    await wrapper.findAll('button')[index].trigger('click');

    expect(wrapper.emitted('update:modelValue')).toEqual([[value]]);
  });

  it('전세가격 탭 값에 맞는 활성 클래스와 아이콘 투명도를 적용한다', () => {
    const wrapper = mount(MainDataTabs, {
      props: { modelValue: 'price-index' },
    });
    const buttons = wrapper.findAll('button');
    const icons = wrapper.findAll('img');

    expect(buttons[1].classes()).toContain('data-tabs__button--price');
    expect(icons[0].classes()).toContain('opacity-50');
    expect(icons[1].classes()).toContain('opacity-100');
  });
});
