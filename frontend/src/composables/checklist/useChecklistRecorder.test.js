import { ref } from 'vue';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { useChecklistRecorder } from './useChecklistRecorder';

const mocks = vi.hoisted(() => ({
  abort: vi.fn(), finish: vi.fn(), liveStart: vi.fn(), onComplete: null,
  openText: vi.fn(), remove: vi.fn(), save: vi.fn(), sendChunk: vi.fn(),
  startRecording: vi.fn(), stopRecording: vi.fn(),
}));

const recorderState = {
  clearError: vi.fn(), elapsedSeconds: ref(6), errorMessage: ref(''),
  isRecording: ref(false), isStarting: ref(false),
  startRecording: mocks.startRecording, stopRecording: mocks.stopRecording,
  waveformLevels: ref([]),
};
const liveState = {
  abort: mocks.abort, errorMessage: ref(''), finish: mocks.finish,
  isProcessing: ref(false), recordingSessionId: ref(null), refresh: vi.fn(),
  sendChunk: mocks.sendChunk, start: mocks.liveStart, status: ref(null),
  transcript: ref(''),
};
const savedState = {
  deleteErrorMessage: ref(''), errorMessage: ref(''), isDeleteModalOpen: ref(false),
  isDeleting: ref(false), isTextLoading: ref(false), isTextModalOpen: ref(false),
  openTextModal: mocks.openText, remove: mocks.remove, save: mocks.save,
  savedRecording: ref(null), textErrorMessage: ref(''),
};

vi.mock('./useAudioRecorder', () => ({ useAudioRecorder: () => recorderState }));
vi.mock('./useLiveRecordingSession', () => ({
  useLiveRecordingSession: (_, onComplete) => {
    mocks.onComplete = onComplete;
    return liveState;
  },
}));
vi.mock('./useSavedRecording', () => ({ useSavedRecording: () => savedState }));

describe('useChecklistRecorder', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    recorderState.elapsedSeconds.value = 6;
    recorderState.errorMessage.value = '';
    liveState.errorMessage.value = '';
    savedState.errorMessage.value = '';
    mocks.startRecording.mockImplementation(async ({ beforeStart, onPcmChunk }) => {
      await beforeStart();
      onPcmChunk(new ArrayBuffer(2));
    });
    mocks.stopRecording.mockResolvedValue(new Blob(['audio']));
    mocks.finish.mockResolvedValue(undefined);
  });

  test('녹음 시작 시 서버 세션 연결 후 PCM chunk를 전달한다', async () => {
    const state = useChecklistRecorder(vi.fn(), ref(9));

    await state.beginRecording();

    expect(mocks.liveStart).toHaveBeenCalledOnce();
    expect(mocks.sendChunk).toHaveBeenCalledWith(expect.any(ArrayBuffer));
  });

  test('종료 분석이 완료되면 녹음을 저장하고 체크리스트 갱신을 요청한다', async () => {
    const emit = vi.fn();
    const state = useChecklistRecorder(emit, ref(9));

    await state.finishRecording();
    await mocks.onComplete({ recordingSessionId: 4, transcript: '완료' });

    expect(mocks.finish).toHaveBeenCalledOnce();
    expect(mocks.save).toHaveBeenCalledWith(
      expect.objectContaining({ duration: 6, blob: expect.any(Blob) }),
      { recordingSessionId: 4, transcript: '완료' },
    );
    expect(emit).toHaveBeenCalledWith('processed');
  });

  test('서버 종료 실패 시 완료 데이터를 저장하지 않는다', async () => {
    mocks.finish.mockRejectedValue(new Error('fail'));
    const state = useChecklistRecorder(vi.fn(), ref(9));

    await state.finishRecording();
    mocks.onComplete({ recordingSessionId: 4 });

    expect(mocks.save).not.toHaveBeenCalled();
    expect(state.isFinishing.value).toBe(false);
  });
});
