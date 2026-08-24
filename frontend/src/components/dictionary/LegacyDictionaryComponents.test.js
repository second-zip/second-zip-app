import { mount, RouterLinkStub } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import FFraudGuide from './fraud/F_FraudGuide.vue';
import FDictionaryGuide from './main/F_DictionaryGuide.vue';
import FWordGuide from './words/F_WordGuide.vue';
import DictSubHeader from '@/layouts/DictSubHeader.vue';

describe('도감 표시용 컴포넌트', () => {
  it.each([
    [FFraudGuide, 'aside'],
    [FDictionaryGuide, '.dictionary-guide'],
    [FWordGuide, '.word-guide'],
  ])('비서 이미지와 메시지를 표시한다', (component, selector) => {
    const wrapper = mount(component, {
      props: { image: '/character.png', message: '안내 메시지' },
    });

    expect(wrapper.get(selector).text()).toContain('안내 메시지');
    expect(wrapper.get('img').attributes('src')).toBe('/character.png');
  });

  it('하위 도감 헤더를 표시한다', () => {
    const wrapper = mount(DictSubHeader, {
      props: { title: '현재 도감' },
      global: { stubs: { RouterLink: RouterLinkStub } },
    });

    expect(wrapper.text()).toContain('전세사기 도감');
    expect(wrapper.text()).toContain('현재 도감');
  });
});
