package com.secondzip.backend.record.service;

//실시간 STT 세션 시작 → WebSocket에서 받은 byte[] 음성 전달 → 종료 시 transcript 반환(실시간 방식 전용)
public interface LiveTranscriptionService {

    void start(Long recordingSessionId,String category);

    void acceptAudio(Long recordingSessionId, byte[] audioChunk);

    String finish(Long recordingSessionId);
}