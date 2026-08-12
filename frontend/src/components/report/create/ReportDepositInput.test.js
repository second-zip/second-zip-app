import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import ReportDepositInput from './ReportDepositInput.vue';

describe('ReportDepositInput', () => {
  it('입력값에서 숫자만 v-model로 전달한다', async () => {
    const wrapper = mount(ReportDepositInput, {
      props: { modelValue: '' },
    });

    await wrapper.get('[aria-label="보증금"]').setValue('1,000만원');

    expect(wrapper.emitted('update:modelValue')).toEqual([['1000']]);
    expect(wrapper.get('input').attributes('inputmode')).toBe('numeric');
  });

  it('빠른 금액을 현재 값에 누적한다', async () => {
    const wrapper = mount(ReportDepositInput, {
      props: { modelValue: '100' },
    });
    const amountButtons = wrapper.findAll('.quick-amount-button');

    await amountButtons[2].trigger('click');

    expect(wrapper.emitted('update:modelValue')).toEqual([['1100']]);
    expect(amountButtons.map((button) => button.text())).toEqual([
      '10',
      '100',
      '1000',
      '10000',
    ]);
  });

  it('값이 있을 때만 clear 버튼을 표시하고 빈 문자열을 emit한다', async () => {
    const wrapper = mount(ReportDepositInput, {
      props: { modelValue: '5000' },
    });

    await wrapper.get('[aria-label="보증금 지우기"]').trigger('click');
    expect(wrapper.emitted('update:modelValue')).toEqual([['']]);

    await wrapper.setProps({ modelValue: '' });
    expect(wrapper.find('[aria-label="보증금 지우기"]').exists()).toBe(false);
  });
});
