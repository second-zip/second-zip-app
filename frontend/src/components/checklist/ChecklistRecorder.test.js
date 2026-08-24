import { mount } from '@vue/test-utils';
import { nextTick, ref } from 'vue';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import ChecklistRecorder from './ChecklistRecorder.vue';

let state;
vi.mock('@/composables/checklist/useChecklistRecorder', () => ({
  useChecklistRecorder: () => state,
}));

const makeState = () => ({
  beginRecording: vi.fn(),
  deleteErrorMessage: ref(''),
  elapsedSeconds: ref(0),
  errorMessage: ref(''),
  finishRecording: vi.fn(),
  isDeleteModalOpen: ref(false),
  isDeleting: ref(false),
  isFinishing: ref(false),
  isProcessing: ref(false),
  isRecording: ref(false),
  isLoadingRecording: ref(false),
  isStarting: ref(false),
  isTextLoading: ref(false),
  isTextModalOpen: ref(false),
  openTextModal: vi.fn(),
  ensureFreshUrl: vi.fn(),
  remove: vi.fn(),
  savedRecording: ref(null),
  textErrorMessage: ref(''),
  waveformLevels: ref(Array(8).fill(0.1)),
});

const mountRecorder = () => mount(ChecklistRecorder, {
  props: { reportChecklistId: 5 },
  global: {
    stubs: {
      RecordingDeleteModal: true,
      RecordingTextModal: true,
    },
  },
});

describe('ChecklistRecorder', () => {
  beforeEach(() => {
    state = makeState();
  });

  test('저장된 녹음이 없으면 녹음 시작 UI를 제공한다', async () => {
    const wrapper = mountRecorder();

    expect(wrapper.text()).toContain('녹음으로 계약 내용을 확인해 보세요.');
    await wrapper.get('.recording-idle__start').trigger('click');
    expect(state.beginRecording).toHaveBeenCalledOnce();
  });

  test('녹음 중에는 waveform과 종료 후 반영 안내를 표시한다', async () => {
    state.isRecording.value = true;
    state.elapsedSeconds.value = 84;
    const wrapper = mountRecorder();

    expect(wrapper.text()).toContain('01:24');
    expect(wrapper.text()).toContain('체크리스트에 반영해 드려요.');
    expect(wrapper.findAll('.recording-waveform span')).toHaveLength(8);
    await wrapper.get('.recording-active__stop').trigger('click');
    expect(state.finishRecording).toHaveBeenCalledOnce();
  });

  test('분석 중과 저장 완료 상태를 우선순위에 맞게 전환한다', async () => {
    state.isProcessing.value = true;
    const wrapper = mountRecorder();
    expect(wrapper.text()).toContain('녹음 내용을 분석하고 있어요.');

    state.isProcessing.value = false;
    state.savedRecording.value = { url: 'blob:test', duration: 3 };
    await nextTick();
    expect(wrapper.text()).toContain('녹음 삭제');
    expect(wrapper.text()).toContain('텍스트 읽기');

    await wrapper.get('.recording-saved__delete').trigger('click');
    await wrapper.get('.recording-saved__text').trigger('click');
    expect(state.isDeleteModalOpen.value).toBe(true);
    expect(state.openTextModal).toHaveBeenCalledOnce();
  });

  test('녹음 오류를 접근 가능한 경고로 표시한다', () => {
    state.errorMessage.value = '마이크를 사용할 수 없어요.';
    const wrapper = mountRecorder();

    expect(wrapper.get('[role="alert"]').text()).toBe(
      '마이크를 사용할 수 없어요.',
    );
  });
});
