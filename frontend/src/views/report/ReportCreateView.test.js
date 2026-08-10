import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ReportCreateForm from '@/components/report/create/ReportCreateForm.vue';
import ReportCreateView from './ReportCreateView.vue';

const router = vi.hoisted(() => ({
  push: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRouter: () => router,
}));

describe('ReportCreateView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    router.push.mockResolvedValue();
  });

  it('실제 입력값을 분석 요청 형태로 변환해 진행 화면으로 전달한다', async () => {
    const wrapper = mount(ReportCreateView, {
      global: { stubs: { RouterLink: true } },
    });

    wrapper.getComponent(ReportCreateForm).vm.$emit('submit', {
      address: {
        roadAddress: '서울특별시 강남구 테헤란로 1',
        jibunAddress: '서울특별시 강남구 역삼동 1',
      },
      addressKeyword: '사용하지 않는 주소',
      dong: '202',
      ho: '303호',
      deposit: 25_000,
    });
    await flushPromises();

    expect(router.push).toHaveBeenCalledOnce();
    expect(router.push).toHaveBeenCalledWith({
      name: 'analysis-progress',
      state: {
        analysisRequest: {
          roadAddress: '서울특별시 강남구 테헤란로 1',
          detailAddress: '202동 303호',
          deposit: 250_000_000,
        },
      },
    });
  });

  it.each([
    [
      {
        address: { roadAddress: '', jibunAddress: '서울 역삼동 1' },
        addressKeyword: '무시됨',
        dong: '',
        ho: '303',
        deposit: 10_000,
      },
      {
        roadAddress: '서울 역삼동 1',
        detailAddress: '303호',
        deposit: 100_000_000,
      },
    ],
    [
      {
        address: null,
        addressKeyword: '  사용자 입력 주소  ',
        dong: '101동',
        ho: '',
        deposit: 5_000,
      },
      {
        roadAddress: '사용자 입력 주소',
        detailAddress: '101동',
        deposit: 50_000_000,
      },
    ],
  ])('도로명이 없을 때 fallback 주소와 선택 동/호만 전달한다', async (
    formData,
    analysisRequest,
  ) => {
    const wrapper = mount(ReportCreateView, {
      global: { stubs: { RouterLink: true } },
    });

    wrapper.getComponent(ReportCreateForm).vm.$emit('submit', formData);
    await flushPromises();

    expect(router.push).toHaveBeenCalledWith({
      name: 'analysis-progress',
      state: { analysisRequest },
    });
  });

  it('라우터 이동 중에는 중복 submit을 무시한다', async () => {
    let resolvePush;
    router.push.mockReturnValue(
      new Promise((resolve) => {
        resolvePush = resolve;
      }),
    );
    const wrapper = mount(ReportCreateView, {
      global: { stubs: { RouterLink: true } },
    });
    const payload = {
      address: null,
      addressKeyword: '서울',
      dong: '',
      ho: '',
      deposit: 1,
    };

    wrapper.getComponent(ReportCreateForm).vm.$emit('submit', payload);
    wrapper.getComponent(ReportCreateForm).vm.$emit('submit', payload);
    await flushPromises();

    expect(router.push).toHaveBeenCalledOnce();
    resolvePush();
    await flushPromises();
  });
});
