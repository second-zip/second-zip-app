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
});
