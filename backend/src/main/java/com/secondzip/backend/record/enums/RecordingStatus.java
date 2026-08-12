package com.secondzip.backend.record.enums;

public enum RecordingStatus {
    RECORDING,      // 실시간 녹음 중
    UPLOADED, //녹음 파일 업로드 완료
    TRANSCRIBING, //STT 변환 중
    ANALYZING, //AI 체크리스트 분석 중
    COMPLETED, //분석 완료
    FAILED //처리 실패
}