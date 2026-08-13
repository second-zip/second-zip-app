import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import ChecklistDescriptionModal from './ChecklistDescriptionModal.vue';

const mocks = vi.hoisted(() => ({
  dispose: vi.fn(), hide: vi.fn(), show: vi.fn(),
}));

vi.mock('bootstrap', () => ({
  Modal: class {
    show() { mocks.show(); }
    hide() { mocks.hide(); }
    dispose() { mocks.dispose(); }
  },
}));

const item = {
  id: 2,
  title: '확정일자 부여현황',
  description: '기존 임차인의 권리 관계를 확인할 수 있어요.',
};

describe('ChecklistDescriptionModal', () => {
  beforeEach(() => vi.clearAllMocks());

  test('선택 항목의 제목과 description을 표시한다', () => {
    const wrapper = mount(ChecklistDescriptionModal, {
      props: { item, open: true },
    });

    expect(wrapper.get('.modal-title').text()).toBe(item.title);
    expect(wrapper.get('.checklist-description-modal__text').text())
      .toBe(item.description);
    expect(mocks.show).toHaveBeenCalledOnce();
  });

  test('open prop과 확인 버튼으로 Bootstrap 모달을 제어한다', async () => {
    const wrapper = mount(ChecklistDescriptionModal, {
      props: { item, open: false },
    });
    await wrapper.setProps({ open: true });
    expect(mocks.show).toHaveBeenCalledOnce();

    await wrapper.get('.checklist-description-modal__confirm').trigger('click');
    expect(mocks.hide).toHaveBeenCalledOnce();

    await wrapper.setProps({ open: false });
    expect(mocks.hide).toHaveBeenCalledTimes(2);
  });

  test('Bootstrap 닫힘을 전달하고 unmount 시 인스턴스를 정리한다', () => {
    const wrapper = mount(ChecklistDescriptionModal, {
      props: { item, open: false },
    });
    wrapper.get('.modal').element.dispatchEvent(new Event('hidden.bs.modal'));
    expect(wrapper.emitted('close')).toHaveLength(1);

    wrapper.unmount();
    expect(mocks.dispose).toHaveBeenCalledOnce();
  });
});
