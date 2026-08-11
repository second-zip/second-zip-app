package com.secondzip.backend.record.service;

//실시간 녹음 도중 누적된 transcript를 GPT로 중간 분석하는 기능의 인터페이스
public interface LiveChecklistAnalysisService {

    void analyzeProvisional(
            Long recordingSessionId,
            String transcript
    );
}