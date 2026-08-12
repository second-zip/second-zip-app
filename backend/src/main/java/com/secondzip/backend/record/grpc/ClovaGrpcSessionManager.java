package com.secondzip.backend.record.grpc;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//recordingSessionId별로 여러 ClovaGrpcSession을 관리
@Component
public class ClovaGrpcSessionManager {

    private final Map<Long, ClovaGrpcSession> sessions = new ConcurrentHashMap<>(); //gRPC 세션


    public void put(
            Long recordingSessionId,
            ClovaGrpcSession session
    ) {

        ClovaGrpcSession previous =
                sessions.putIfAbsent( //key가 없을 때만 저장
                        recordingSessionId,
                        session
                );

        if (previous != null) {

            throw new IllegalStateException(
                    "이미 CLOVA 실시간 세션이 존재합니다. recordingSessionId="
                            + recordingSessionId
            );
        }
    }


    public ClovaGrpcSession get(
            Long recordingSessionId
    ) {

        ClovaGrpcSession session =
                sessions.get(
                        recordingSessionId
                );

        if (session == null) {

            throw new IllegalStateException(
                    "CLOVA 실시간 세션이 존재하지 않습니다. recordingSessionId="
                            + recordingSessionId
            );
        }

        return session;
    }


    public ClovaGrpcSession remove(
            Long recordingSessionId
    ) {

        return sessions.remove(
                recordingSessionId
        );
    }
}