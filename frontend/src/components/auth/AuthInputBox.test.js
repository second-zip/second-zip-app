import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import AuthInputBox from './AuthInputBox.vue';

const mountInput = (props = {}) =>
  mount(AuthInputBox, {
    props: {
      id: 'email',
      label: '이메일',
      ...props,
    },
    attrs: {
      autocomplete: 'email',
      placeholder: '이메일 입력',
    },
  });

describe('AuthInputBox', () => {
  it('label과 입력 속성을 렌더링한다', () => {
    const wrapper = mountInput({ modelValue: 'user@example.com', type: 'email' });
    const input = wrapper.get('input');

    expect(wrapper.get('label').text()).toBe('이메일');
    expect(wrapper.get('label').attributes('for')).toBe('email');
    expect(input.element.value).toBe('user@example.com');
    expect(input.attributes()).toMatchObject({
      id: 'email',
      type: 'email',
      autocomplete: 'email',
      placeholder: '이메일 입력',
    });
  });

  it('사용자 입력과 focus, blur 이벤트를 부모에 전달한다', async () => {
    const wrapper = mountInput();
    const input = wrapper.get('input');

    await input.setValue('changed@example.com');
    await input.trigger('focus');
    await input.trigger('blur');

    expect(wrapper.emitted('update:modelValue')[0]).toEqual(['changed@example.com']);
    expect(wrapper.emitted('focus')).toHaveLength(1);
    expect(wrapper.emitted('blur')).toHaveLength(1);
  });

  it.each([
    ['default', 'auth-input__status-message-default'],
    ['correct', 'auth-input__status-message-correct'],
    ['wrong', 'auth-input__status-message-wrong'],
  ])('%s 상태에 맞는 안내 문구 스타일을 표시한다', (status, className) => {
    const wrapper = mountInput({ message: '상태 안내', status });

    expect(wrapper.get('.auth-input__status-message').text()).toBe('상태 안내');
    expect(wrapper.get('.auth-input__status-message').classes()).toContain(className);
    expect(wrapper.get('.auth-input__status-icon').attributes('src')).toBeTruthy();
  });

  it('안내 문구가 없으면 상태 영역을 렌더링하지 않는다', () => {
    const wrapper = mountInput();

    expect(wrapper.find('.auth-input__status-message').exists()).toBe(false);
  });
});
