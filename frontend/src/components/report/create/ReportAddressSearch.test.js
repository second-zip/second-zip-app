import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import ReportAddressSearch from './ReportAddressSearch.vue';

const ADDRESS = {
  id: 'address-1',
  roadAddress: '서울특별시 강남구 테헤란로 1',
  jibunAddress: '서울특별시 강남구 역삼동 1',
};

const mountSearch = (props = {}) =>
  mount(ReportAddressSearch, {
    props: { modelValue: '', results: [], ...props },
  });

describe('ReportAddressSearch', () => {
  it('검색 버튼과 Enter로 trim된 검색어를 emit한다', async () => {
    const wrapper = mountSearch({ modelValue: '  테헤란로  ' });

    await wrapper.get('[aria-label="주소 검색"]').trigger('click');
    await wrapper.get('input').trigger('keydown.enter');

    expect(wrapper.emitted('search')).toEqual([['테헤란로'], ['테헤란로']]);
  });

  it('검색 후 로딩·오류·결과 없음 상태를 표시한다', async () => {
    const wrapper = mountSearch({ modelValue: '테헤란로' });
    await wrapper.get('[aria-label="주소 검색"]').trigger('click');

    await wrapper.setProps({ isLoading: true });
    expect(wrapper.text()).toContain('주소를 검색하고 있어요.');

    await wrapper.setProps({ isLoading: false, errorMessage: '검색 실패' });
    expect(wrapper.get('[role="alert"]').text()).toBe('검색 실패');

    await wrapper.setProps({ errorMessage: '' });
    expect(wrapper.text()).toContain('검색 결과가 없습니다.');
  });

  it('도로명과 지번을 표시하고 선택 후 결과창을 닫는다', async () => {
    const wrapper = mountSearch({ modelValue: '테헤란로', results: [ADDRESS] });
    await wrapper.get('[aria-label="주소 검색"]').trigger('click');

    expect(wrapper.text()).toContain(ADDRESS.roadAddress);
    expect(wrapper.text()).toContain(ADDRESS.jibunAddress);
    await wrapper.get('.address-search__result').trigger('click');

    expect(wrapper.emitted('update:modelValue')).toEqual([[ADDRESS.roadAddress]]);
    expect(wrapper.emitted('select')).toEqual([[ADDRESS]]);
    expect(wrapper.find('[role="listbox"]').exists()).toBe(false);
  });

  it('지번만 있는 결과는 한 줄로 표시하고 clear를 emit한다', async () => {
    const jibunOnly = { ...ADDRESS, roadAddress: '' };
    const wrapper = mountSearch({ modelValue: '역삼동', results: [jibunOnly] });
    await wrapper.get('[aria-label="주소 검색"]').trigger('click');

    expect(wrapper.find('.address-search__jibun').exists()).toBe(false);
    expect(wrapper.get('.address-search__road').text()).toBe(ADDRESS.jibunAddress);

    await wrapper.get('[aria-label="주소 검색어 지우기"]').trigger('click');
    expect(wrapper.emitted('clear')).toHaveLength(1);
    expect(wrapper.find('[role="listbox"]').exists()).toBe(false);
  });

  it('빈 검색어는 API 검색 이벤트를 emit하지 않는다', async () => {
    const wrapper = mountSearch({ modelValue: '   ' });
    await wrapper.get('[aria-label="주소 검색"]').trigger('click');
    expect(wrapper.emitted('search')).toBeUndefined();
  });
});
