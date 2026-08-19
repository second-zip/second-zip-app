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
            Long recordingSessionId
    ) {

        LiveRecordingContext context =
                new LiveRecordingContext();

        contexts.put(
                recordingSessionId,
                context
        );

        System.out.println(
                "[Context CREATE] manager="
                        + System.identityHashCode(this)
                        + ", recordingSessionId="
                        + recordingSessionId
                        + ", size="
                        + contexts.size()
        );

        return context;
    }


    public LiveRecordingContext get(
            Long recordingSessionId
    ) {

        System.out.println(
                "[Context GET] manager="
                        + System.identityHashCode(this)
                        + ", recordingSessionId="
                        + recordingSessionId
                        + ", size="
                        + contexts.size()
                        + ", keys="
                        + contexts.keySet()
        );

        LiveRecordingContext context =
                contexts.get(
                        recordingSessionId
                );

        if (context == null) {
            throw new IllegalStateException(
                    "실시간 녹음 Context가 존재하지 않습니다. recordingSessionId="
                            + recordingSessionId
            );
        }

        return context;
    }

    // async GPT가 끝나는 순간 사용자가 이미 STOP해서 Context가 삭제
    public LiveRecordingContext find(
            Long recordingSessionId
    ) {

        return contexts.get(recordingSessionId);
    }

    public LiveRecordingContext remove(
            Long recordingSessionId
    ) {

        return contexts.remove(recordingSessionId);
    }
}