import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import PlusDisableIcon from '@/assets/icons/report/plus-gray-14.svg';
import PlusIcon from '@/assets/icons/report/plus-white-14.svg';
import ReportCreateForm from './ReportCreateForm.vue';

const addressApi = vi.hoisted(() => ({
  searchKakaoAddress: vi.fn(),
}));

vi.mock('@/api/address', () => ({
  KAKAO_API_KEY_MISSING_ERROR: 'KAKAO_API_KEY_MISSING',
  searchKakaoAddress: addressApi.searchKakaoAddress,
}));

const ADDRESS = {
  id: 'address-1',
  roadAddress: '서울특별시 강남구 테헤란로 1',
  jibunAddress: '서울특별시 강남구 역삼동 1',
};

const mountForm = () =>
  mount(ReportCreateForm, {
    global: { stubs: { RouterLink: true } },
  });

describe('ReportCreateForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    addressApi.searchKakaoAddress.mockResolvedValue([ADDRESS]);
  });

  it('주소 검색·선택과 상세 입력값을 submit payload로 전달한다', async () => {
    const wrapper = mountForm();

    await wrapper.get('[aria-label="주소 검색어"]').setValue('  테헤란로  ');
    await wrapper.get('[aria-label="주소 검색"]').trigger('click');
    await flushPromises();
    expect(addressApi.searchKakaoAddress).toHaveBeenCalledWith('테헤란로');

    await wrapper.get('.address-search__result').trigger('click');
    await wrapper.get('[aria-label="동 입력"]').setValue(' 202 ');
    await wrapper.get('[aria-label="호수 입력"]').setValue(' 303 ');
    await wrapper.get('[aria-label="보증금"]').setValue('25000');
    await wrapper.get('form').trigger('submit');

    expect(wrapper.emitted('submit')).toEqual([
      [
        {
          address: ADDRESS,
          addressKeyword: ADDRESS.roadAddress,
          dong: '202',
          ho: '303',
          deposit: 25_000,
          validateReport: true,
        },
      ],
    ]);
  });

  it('주소를 선택하고 0보다 큰 보증금을 입력해야 submit 버튼을 활성화한다', async () => {
    const wrapper = mountForm();
    const submitButton = wrapper.get('.report-create-form__submit');

    expect(submitButton.attributes('disabled')).toBeDefined();
    expect(submitButton.get('img').attributes('src')).toBe(PlusDisableIcon);

    await wrapper.get('[aria-label="보증금"]').setValue('10000');
    expect(submitButton.attributes('disabled')).toBeDefined();

    await wrapper.get('[aria-label="주소 검색어"]').setValue('테헤란로');
    await wrapper.get('[aria-label="주소 검색"]').trigger('click');
    await flushPromises();
    await wrapper.get('.address-search__result').trigger('click');

    expect(submitButton.attributes('disabled')).toBeUndefined();
    expect(submitButton.get('img').attributes('src')).toBe(PlusIcon);
  });

  it.each(['', '0', '-1', 'Infinity', 'NaN'])(
    '선택된 주소가 있어도 보증금 %j은 무효하다',
    async (deposit) => {
      const wrapper = mountForm();
      await wrapper.get('[aria-label="주소 검색어"]').setValue('테헤란로');
      await wrapper.get('[aria-label="주소 검색"]').trigger('click');
      await flushPromises();
      await wrapper.get('.address-search__result').trigger('click');

      wrapper
        .getComponent({ name: 'ReportDepositInput' })
        .vm.$emit('update:modelValue', deposit);
      await wrapper.vm.$nextTick();

      expect(
        wrapper.get('.report-create-form__submit').attributes('disabled'),
      ).toBeDefined();
      await wrapper.get('form').trigger('submit');
      expect(wrapper.emitted('submit')[0][0].validateReport).toBe(false);
    },
  );

  it('유효한 값을 만든 후 주소를 수정하면 다시 비활성화한다', async () => {
    const wrapper = mountForm();
    await wrapper.get('[aria-label="보증금"]').setValue('10000');
    await wrapper.get('[aria-label="주소 검색어"]').setValue('테헤란로');
    await wrapper.get('[aria-label="주소 검색"]').trigger('click');
    await flushPromises();
    await wrapper.get('.address-search__result').trigger('click');
    expect(
      wrapper.get('.report-create-form__submit').attributes('disabled'),
    ).toBeUndefined();

    await wrapper.get('[aria-label="주소 검색어"]').setValue('수정된 주소');

    expect(
      wrapper.get('.report-create-form__submit').attributes('disabled'),
    ).toBeDefined();
  });

  it('선택 후 검색어를 수정하면 선택 주소만 해제한다', async () => {
    const wrapper = mountForm();
    await wrapper.get('[aria-label="주소 검색어"]').setValue('테헤란로');
    await wrapper.get('[aria-label="주소 검색"]').trigger('click');
    await flushPromises();
    await wrapper.get('.address-search__result').trigger('click');

    await wrapper.get('[aria-label="주소 검색어"]').setValue('새 주소');
    await wrapper.get('form').trigger('submit');

    expect(wrapper.emitted('submit')[0][0]).toMatchObject({
      address: null,
      addressKeyword: '새 주소',
    });
  });

  it.each([
    ['KAKAO_API_KEY_MISSING', '주소 검색 설정을 확인해주세요.'],
    ['NETWORK_ERROR', '주소를 검색하지 못했습니다.'],
  ])('API %s 오류를 사용자 메시지로 표시한다', async (code, message) => {
    addressApi.searchKakaoAddress.mockRejectedValue({ code });
    const wrapper = mountForm();
    await wrapper.get('[aria-label="주소 검색어"]').setValue('테헤란로');
    await wrapper.get('[aria-label="주소 검색"]').trigger('click');
    await flushPromises();

    expect(wrapper.get('[role="alert"]').text()).toBe(message);
    expect(wrapper.get('[aria-label="주소 검색어"]').element.value).toBe(
      '테헤란로',
    );
  });

  it('clear 후 늦게 완료된 검색 응답을 무시한다', async () => {
    let resolveSearch;
    addressApi.searchKakaoAddress.mockReturnValue(
      new Promise((resolve) => {
        resolveSearch = resolve;
      }),
    );
    const wrapper = mountForm();
    await wrapper.get('[aria-label="주소 검색어"]').setValue('테헤란로');
    await wrapper.get('[aria-label="주소 검색"]').trigger('click');
    expect(wrapper.text()).toContain('주소를 검색하고 있어요.');

    await wrapper.get('[aria-label="주소 검색어 지우기"]').trigger('click');
    resolveSearch([ADDRESS]);
    await flushPromises();

    expect(wrapper.get('[aria-label="주소 검색어"]').element.value).toBe('');
    expect(wrapper.find('.address-search__result').exists()).toBe(false);
    expect(wrapper.find('[role="alert"]').exists()).toBe(false);
  });
});
