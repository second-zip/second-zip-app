import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import ReportDeleteModal from './ReportDeleteModal.vue';

const mocks = vi.hoisted(() => ({
  dispose: vi.fn(),
  hide: vi.fn(),
  show: vi.fn(),
}));

vi.mock('bootstrap', () => ({
  Modal: class {
    show() {
      mocks.show();
    }

    hide() {
      mocks.hide();
    }

    dispose() {
      mocks.dispose();
    }
  },
}));

describe('ReportDeleteModal', () => {
  beforeEach(() => vi.clearAllMocks());

  test('삭제 대상의 변경에 따라 모달을 열고 닫는다', async () => {
    const wrapper = mount(ReportDeleteModal);

    await wrapper.setProps({ report: { analysisReportId: 1 } });
    expect(mocks.show).toHaveBeenCalledOnce();

    await wrapper.setProps({ report: null });
    expect(mocks.hide).toHaveBeenCalledOnce();

    wrapper.unmount();
    expect(mocks.dispose).toHaveBeenCalledOnce();
  });

  test('확인과 Bootstrap 닫힘 이벤트를 상위로 전달한다', async () => {
    const wrapper = mount(ReportDeleteModal, {
      props: { report: { analysisReportId: 1 } },
    });

    await wrapper.get('.btn-danger').trigger('click');
    wrapper.get('.modal').element.dispatchEvent(new Event('hidden.bs.modal'));

    expect(wrapper.emitted('confirm')).toHaveLength(1);
    expect(wrapper.emitted('close')).toHaveLength(1);
  });

  test('삭제 진행 상태와 오류 메시지를 표시한다', () => {
    const wrapper = mount(ReportDeleteModal, {
      props: {
        report: { analysisReportId: 1 },
        isDeleting: true,
        errorMessage: '삭제 실패',
      },
    });

    expect(wrapper.get('.btn-danger').attributes()).toHaveProperty('disabled');
    expect(wrapper.get('.btn-danger').text()).toBe('삭제 중...');
    expect(wrapper.get('[role="alert"]').text()).toBe('삭제 실패');
  });
});
