import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import ReportDetailAddressInput from './ReportDetailAddressInput.vue';

const mountInput = (props = {}) =>
  mount(ReportDetailAddressInput, {
    props: { dong: '', ho: '', ...props },
  });

describe('ReportDetailAddressInput', () => {
  it('동과 호수를 서로 다른 v-model 이벤트로 전달한다', async () => {
    const wrapper = mountInput();

    await wrapper.get('[aria-label="동 입력"]').setValue('101A');
    await wrapper.get('[aria-label="호수 입력"]').setValue('1203');

    expect(wrapper.emitted('update:dong')).toEqual([['101A']]);
    expect(wrapper.emitted('update:ho')).toEqual([['1203']]);
  });

  it('각 필드를 다른 값에 영향 없이 초기화한다', async () => {
    const wrapper = mountInput({ dong: '101', ho: '1203' });

    await wrapper.get('[aria-label="동 지우기"]').trigger('click');
    await wrapper.get('[aria-label="호수 지우기"]').trigger('click');

    expect(wrapper.emitted('update:dong')).toEqual([['']]);
    expect(wrapper.emitted('update:ho')).toEqual([['']]);
  });

  it('선택 안내와 모바일 숫자 키패드 속성을 제공한다', () => {
    const wrapper = mountInput();
    const inputs = wrapper.findAll('input');

    expect(wrapper.text()).toContain('상세 주소');
    expect(wrapper.text()).toContain('선택');
    expect(inputs.every((input) => input.attributes('type') === 'text')).toBe(true);
    expect(inputs.every((input) => input.attributes('inputmode') === 'numeric')).toBe(
      true,
    );
  });
});
