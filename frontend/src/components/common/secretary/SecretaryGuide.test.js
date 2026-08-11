import { mount } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';

import SecretaryGuide from './SecretaryGuide.vue';

describe('SecretaryGuide', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('캐릭터와 줄바꿈이 포함된 안내 문구를 표시한다', () => {
    const wrapper = mount(SecretaryGuide, {
      props: {
        text: '첫 번째 줄\n두 번째 줄',
        characterType: 'WOMAN',
      },
    });

    expect(wrapper.get('.secretary-bubble').text()).toContain(
      '첫 번째 줄\n두 번째 줄',
    );
    expect(wrapper.get('img').attributes('src')).toContain('woman-main');
  });

  it('변경 버튼 클릭을 change 이벤트로 전달한다', async () => {
    const wrapper = mount(SecretaryGuide, {
      props: { text: '안내', changeBtn: true },
    });

    await wrapper.get('.secretary-guide__change-btn').trigger('click');

    expect(wrapper.emitted('change')).toHaveLength(1);
  });

  it('floating 안내는 4초 후 접히고 캐릭터 클릭으로 다시 열린다', async () => {
    vi.useFakeTimers();
    const wrapper = mount(SecretaryGuide, {
      props: { text: '안내', floating: true },
    });

    expect(wrapper.find('.secretary-bubble').exists()).toBe(true);
    await vi.advanceTimersByTimeAsync(4000);
    expect(wrapper.find('.secretary-bubble').exists()).toBe(false);

    await wrapper.get('.secretary-character').trigger('click');
    expect(wrapper.find('.secretary-bubble').exists()).toBe(true);
  });
});
