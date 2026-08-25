import { mount } from '@vue/test-utils';
import { describe, expect, test, vi } from 'vitest';

import RecordingDeleteModal from './RecordingDeleteModal.vue';
import RecordingTextModal from './RecordingTextModal.vue';

vi.mock('./RecordingModalShell.vue', () => ({
  default: {
    props: ['open', 'title', 'titleId'],
    emits: ['close'],
    template: `
      <section v-if="open" class="modal-shell">
        <h2>{{ title }}</h2><slot /><footer><slot name="footer" /></footer>
        <button class="shell-close" @click="$emit('close')">shell close</button>
      </section>
    `,
  },
}));

describe('RecordingDeleteModal', () => {
  test('삭제 영향 안내와 확인 이벤트를 제공한다', async () => {
    const wrapper = mount(RecordingDeleteModal, { props: { open: true } });

    expect(wrapper.text()).toContain('삭제한 녹음 기록은 복구할 수 없어요.');
    expect(wrapper.text()).toContain('체크 상태는 변경되지 않아요.');
    await wrapper.get('.btn-danger').trigger('click');
    await wrapper.get('.shell-close').trigger('click');
    expect(wrapper.emitted('confirm')).toHaveLength(1);
    expect(wrapper.emitted('close')).toHaveLength(1);
  });

  test('삭제 중 중복 동작을 막고 서버 오류를 표시한다', () => {
    const wrapper = mount(RecordingDeleteModal, {
      props: { open: true, isDeleting: true, errorMessage: '삭제 실패' },
    });

    expect(wrapper.get('.btn-danger').attributes('disabled')).toBeDefined();
    expect(wrapper.get('.btn-light').attributes('disabled')).toBeDefined();
    expect(wrapper.get('.btn-danger').text()).toBe('삭제 중');
    expect(wrapper.get('[role="alert"]').text()).toBe('삭제 실패');
  });
});

describe('RecordingTextModal', () => {
  test.each([
    [{ isLoading: true }, '녹음 내용을 불러오고 있어요.'],
    [{ errorMessage: '조회 실패' }, '조회 실패'],
    [{ text: '' }, '변환된 녹음 텍스트가 아직 없어요.'],
    [{ text: '화자 정보 없는 녹음' }, '화자 정보 없는 녹음'],
  ])('조회 상태에 맞는 본문을 표시한다', (state, message) => {
    const wrapper = mount(RecordingTextModal, {
      props: { open: true, ...state },
    });

    expect(wrapper.text()).toContain(message);
  });

  test('백엔드 화자 번호를 A와 B로 바꾸고 화자마다 별도 줄로 표시한다', () => {
    const wrapper = mount(RecordingTextModal, {
      props: {
        open: true,
        text: '화자 1: 첫 번째 발화 이어지는 발화\n화자 2: 두 번째 발화\n화자 1: 다시 첫 번째 화자',
      },
    });

    expect(
      wrapper.findAll('.recording-text-modal__speaker-line').map((line) => line.text()),
    ).toEqual([
      'A: 첫 번째 발화 이어지는 발화',
      'B: 두 번째 발화',
      'A: 다시 첫 번째 화자',
    ]);
  });

  test('모달 shell 닫힘을 상위로 전달한다', async () => {
    const wrapper = mount(RecordingTextModal, { props: { open: true } });
    await wrapper.get('.shell-close').trigger('click');
    expect(wrapper.emitted('close')).toHaveLength(1);
  });
});
