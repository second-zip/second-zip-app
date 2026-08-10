package com.secondzip.backend.record.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//여러 LiveRecordingContext를 recordingSessionId별로 생성/조회/삭제하는 관리자
@Component
public class LiveRecordingContextManager {

    private final Map<Long, LiveRecordingContext> contexts =
            new ConcurrentHashMap<>();


    public LiveRecordingContext create(
            Long recordingSessionId,
            String category
    ) {

        LiveRecordingContext context =
                new LiveRecordingContext(category);

        contexts.put(
                recordingSessionId,
                context
        );

        return context;
    }


    public LiveRecordingContext get(
            Long recordingSessionId
    ) {

        LiveRecordingContext context =
                contexts.get(recordingSessionId);

        if (context == null) {

            throw new IllegalStateException(
                    "실시간 녹음 Context가 존재하지 않습니다. recordingSessionId="
                            + recordingSessionId
            );
        }

        return context;
    }


    public LiveRecordingContext remove(
            Long recordingSessionId
    ) {

        return contexts.remove(
                recordingSessionId
        );
    }
}