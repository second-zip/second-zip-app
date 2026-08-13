import { mount } from '@vue/test-utils';
import { nextTick } from 'vue';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { useSavedRecording } from './useSavedRecording';

const mocks = vi.hoisted(() => ({
  deleteRecording: vi.fn(),
  getRecording: vi.fn(),
  getTranscript: vi.fn(),
}));

vi.mock('@/api/recording', () => ({
  deleteRecording: mocks.deleteRecording,
  getRecording: mocks.getRecording,
  getRecordingTranscript: mocks.getTranscript,
}));

const setup = () => {
  const emit = vi.fn();
  let state;
  const wrapper = mount({
    setup() {
      state = useSavedRecording(emit);
      return () => null;
    },
  });
  return { emit, state, wrapper };
};

describe('useSavedRecording', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    URL.createObjectURL = vi.fn(() => 'blob:recording');
    URL.revokeObjectURL = vi.fn();
    mocks.getRecording.mockResolvedValue({ status: 'COMPLETED', fileSize: 8 });
    mocks.getTranscript.mockResolvedValue({ transcript: '계약 내용을 확인했어요.' });
    mocks.deleteRecording.mockResolvedValue(undefined);
  });

  test('로컬 음원과 서버 상세 정보를 하나의 저장 상태로 구성한다', async () => {
    const { state } = setup();
    const blob = new Blob(['audio']);

    await state.save({ blob, duration: 5 }, {
      recordingSessionId: 7,
      transcript: '초기 내용',
    });

    expect(mocks.getRecording).toHaveBeenCalledWith(7);
    expect(state.savedRecording.value).toMatchObject({
      blob, duration: 5, fileSize: 8, recordingSessionId: 7,
      status: 'COMPLETED', transcript: '초기 내용', url: 'blob:recording',
    });
  });

  test('텍스트 모달을 열 때 실제 transcript를 조회한다', async () => {
    const { emit, state } = setup();
    await state.save({ blob: new Blob(), duration: 1 }, { recordingSessionId: 7 });

    await state.openTextModal();
    await nextTick();

    expect(mocks.getTranscript).toHaveBeenCalledWith(7);
    expect(state.savedRecording.value.transcript).toBe('계약 내용을 확인했어요.');
    expect(emit).toHaveBeenCalledWith('modal-visibility-change', true);
  });

  test('삭제 성공 시 음원 URL과 저장 상태만 정리한다', async () => {
    const { state } = setup();
    await state.save({ blob: new Blob(), duration: 1 }, { recordingSessionId: 7 });
    state.isDeleteModalOpen.value = true;

    await state.remove();

    expect(mocks.deleteRecording).toHaveBeenCalledWith(7);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:recording');
    expect(state.savedRecording.value).toBeNull();
    expect(state.isDeleteModalOpen.value).toBe(false);
  });

  test('삭제 실패 시 녹음은 유지하고 오류를 표시한다', async () => {
    mocks.deleteRecording.mockRejectedValue({
      response: { data: { message: '삭제할 수 없어요.' } },
    });
    const { state } = setup();
    await state.save({ blob: new Blob(), duration: 1 }, { recordingSessionId: 7 });

    await state.remove();

    expect(state.savedRecording.value).not.toBeNull();
    expect(state.deleteErrorMessage.value).toBe('삭제할 수 없어요.');
  });
});
