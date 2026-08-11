import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import SecretaryCharacter from './SecretaryCharacter.vue';

describe('SecretaryCharacter', () => {
  it.each([
    ['CAT', 'cat-main'],
    ['MAN', 'man-main'],
    ['WOMAN', 'woman-main'],
  ])('%s 타입에 맞는 캐릭터 이미지를 표시한다', (characterType, fileName) => {
    const wrapper = mount(SecretaryCharacter, { props: { characterType } });

    expect(wrapper.get('img').attributes('src')).toContain(fileName);
  });

  it('지원하지 않는 캐릭터는 CAT으로 대체한다', () => {
    const wrapper = mount(SecretaryCharacter, {
      props: { characterType: 'UNKNOWN' },
    });

    expect(wrapper.get('img').attributes('src')).toContain('cat-main');
  });

  it('접힌 상태와 클릭 이벤트를 제공한다', async () => {
    const wrapper = mount(SecretaryCharacter, { props: { collapsed: true } });
    const button = wrapper.get('button');

    expect(button.classes()).toContain('secretary-character--collapsed');
    expect(button.attributes('aria-label')).toBe('안내 메시지 열기');
    await button.trigger('click');
    expect(wrapper.emitted('click')).toHaveLength(1);
  });
});
