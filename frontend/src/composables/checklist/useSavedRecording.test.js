import { flushPromises, mount } from '@vue/test-utils';
import { nextTick, ref } from 'vue';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { useSavedRecording } from './useSavedRecording';

const mocks = vi.hoisted(() => ({
  deleteRecording: vi.fn(),
  getFileUrl: vi.fn(),
  getTranscript: vi.fn(),
}));

vi.mock('@/api/recording', () => ({
  deleteRecording: mocks.deleteRecording,
  getRecordingFileUrl: mocks.getFileUrl,
  getRecordingTranscript: mocks.getTranscript,
}));

const setup = (recordingSessionId = ref(null)) => {
  const emit = vi.fn();
  let state;
  const wrapper = mount({
    setup() {
      state = useSavedRecording(emit, recordingSessionId);
      return () => null;
    },
  });
  return { emit, recordingSessionId, state, wrapper };
};

describe('useSavedRecording', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    URL.createObjectURL = vi.fn(() => 'blob:recording');
    URL.revokeObjectURL = vi.fn();
    mocks.getFileUrl.mockResolvedValue({
      url: 'https://storage.test/recording.wav?signature=old',
      originalFileName: 'recording-7.wav',
      contentType: 'audio/wav',
      fileSize: 8,
      expiresIn: 600,
    });
    mocks.getTranscript.mockResolvedValue({ transcript: '계약 내용을 확인했어요.' });
    mocks.deleteRecording.mockResolvedValue(undefined);
  });

  test('방금 완료한 녹음은 서버 URL을 다시 조회하지 않고 로컬 Blob으로 재생한다', () => {
    const { state } = setup();
    const blob = new Blob(['audio']);

    state.save({ blob, duration: 5 }, {
      recordingSessionId: 7,
      transcript: '초기 내용',
    });

    expect(mocks.getFileUrl).not.toHaveBeenCalled();
    expect(state.savedRecording.value).toMatchObject({
      blob, duration: 5, recordingSessionId: 7, transcript: '초기 내용',
      url: 'blob:recording', urlKind: 'blob',
    });
  });

  test('체크리스트 재조회가 같은 세션 ID를 반환하면 로컬 Blob을 유지한다', async () => {
    const recordingSessionId = ref(null);
    const { state } = setup(recordingSessionId);
    state.save({ blob: new Blob(['audio']), duration: 5 }, {
      recordingSessionId: 7,
    });

    recordingSessionId.value = 7;
    await nextTick();

    expect(mocks.getFileUrl).not.toHaveBeenCalled();
    expect(state.savedRecording.value.url).toBe('blob:recording');
  });

  test('체크리스트 응답의 세션 ID로 저장된 녹음 URL을 불러온다', async () => {
    const { state } = setup(ref(7));

    await flushPromises();

    expect(mocks.getFileUrl).toHaveBeenCalledWith(7);
    expect(state.savedRecording.value).toMatchObject({
      recordingSessionId: 7,
      url: 'https://storage.test/recording.wav?signature=old',
      contentType: 'audio/wav',
      fileSize: 8,
      urlKind: 'remote',
    });
    expect(state.savedRecording.value.expiresAt).toBeGreaterThan(Date.now());
  });

  test('저장 파일이 없는 404 응답은 녹음 없음 상태로 처리한다', async () => {
    mocks.getFileUrl.mockRejectedValue({ response: { status: 404 } });
    const { state } = setup(ref(7));

    await flushPromises();

    expect(state.savedRecording.value).toBeNull();
    expect(state.errorMessage.value).toBe('');
    expect(state.isLoadingRecording.value).toBe(false);
  });

  test('presigned URL 만료가 임박하면 재생 전에 URL을 재발급한다', async () => {
    const { state } = setup(ref(7));
    await flushPromises();
    state.savedRecording.value.expiresAt = Date.now() - 1;
    mocks.getFileUrl.mockResolvedValueOnce({
      url: 'https://storage.test/recording.wav?signature=new',
      expiresIn: 600,
    });

    await state.ensureFreshUrl();

    expect(mocks.getFileUrl).toHaveBeenCalledTimes(2);
    expect(state.savedRecording.value.url).toContain('signature=new');
    expect(state.savedRecording.value.expiresAt).toBeGreaterThan(Date.now());
  });

  test('텍스트 모달을 열 때 실제 transcript를 조회한다', async () => {
    const { emit, state } = setup();
    state.save({ blob: new Blob(), duration: 1 }, { recordingSessionId: 7 });

    await state.openTextModal();
    await nextTick();

    expect(mocks.getTranscript).toHaveBeenCalledWith(7);
    expect(state.savedRecording.value.transcript).toBe('계약 내용을 확인했어요.');
    expect(emit).toHaveBeenCalledWith('modal-visibility-change', true);
  });

  test('삭제 성공 시 로컬 URL을 정리하고 체크리스트 재조회를 요청한다', async () => {
    const { emit, state } = setup();
    state.save({ blob: new Blob(), duration: 1 }, { recordingSessionId: 7 });
    state.isDeleteModalOpen.value = true;

    await state.remove();

    expect(mocks.deleteRecording).toHaveBeenCalledWith(7);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:recording');
    expect(state.savedRecording.value).toBeNull();
    expect(state.isDeleteModalOpen.value).toBe(false);
    expect(emit).toHaveBeenCalledWith('processed');
  });

  test('삭제 실패 시 녹음은 유지하고 오류를 표시한다', async () => {
    mocks.deleteRecording.mockRejectedValue({
      response: { data: { message: '삭제할 수 없어요.' } },
    });
    const { state } = setup();
    state.save({ blob: new Blob(), duration: 1 }, { recordingSessionId: 7 });

    await state.remove();

    expect(state.savedRecording.value).not.toBeNull();
    expect(state.deleteErrorMessage.value).toBe('삭제할 수 없어요.');
  });
});
