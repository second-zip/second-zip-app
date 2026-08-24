import { mount } from '@vue/test-utils';
import { nextTick, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { Modal } from 'bootstrap';
import { useBootstrapModal } from './useBootstrapModal';

const mocks = vi.hoisted(() => ({
  modal: {
    dispose: vi.fn(),
    hide: vi.fn(),
    show: vi.fn(),
  },
}));

vi.mock('bootstrap', () => ({
  Modal: vi.fn(function ModalMock() {
    return mocks.modal;
  }),
}));

const mountModal = (initialOpen = false, onClose = vi.fn()) => {
  const open = ref(initialOpen);
  const wrapper = mount({
    setup() {
      return { open, ...useBootstrapModal(open, onClose) };
    },
    template: '<div ref="modalElement" />',
  });

  return { onClose, open, wrapper };
};

describe('useBootstrapModal', () => {
  beforeEach(() => vi.clearAllMocks());

  it('마운트 시 Bootstrap Modal을 생성하고 열린 상태를 반영한다', () => {
    const { wrapper } = mountModal(true);

    expect(Modal).toHaveBeenCalledWith(wrapper.element);
    expect(mocks.modal.show).toHaveBeenCalledOnce();
  });

  it('open 변경에 따라 모달을 열고 닫는다', async () => {
    const { open } = mountModal();

    open.value = true;
    await nextTick();
    expect(mocks.modal.show).toHaveBeenCalledOnce();

    open.value = false;
    await nextTick();
    expect(mocks.modal.hide).toHaveBeenCalledOnce();
  });

  it('숨김 이벤트와 hide 함수를 연결하고 언마운트 시 해제한다', () => {
    const { onClose, wrapper } = mountModal();

    wrapper.element.dispatchEvent(new Event('hidden.bs.modal'));
    wrapper.vm.hide();

    expect(onClose).toHaveBeenCalledOnce();
    expect(mocks.modal.hide).toHaveBeenCalledOnce();

    wrapper.unmount();
    expect(mocks.modal.dispose).toHaveBeenCalledOnce();
  });
});
