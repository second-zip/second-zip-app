package com.secondzip.backend.record.websocket;

import com.secondzip.backend.record.service.LiveTranscriptionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer;

@Slf4j
@Component
@AllArgsConstructor
public class RecordingWebSocketHandler
        extends BinaryWebSocketHandler {

    private final LiveTranscriptionService liveTranscriptionService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        Long recordingSessionId = resolveRecordingSessionId(session);

        log.info(
                "실시간 녹음 WebSocket 연결. recordingSessionId={}, wsSessionId={}",
                recordingSessionId,
                session.getId()
        );
        liveTranscriptionService.start(
                recordingSessionId
        );
    }

    @Override
    protected void handleBinaryMessage(
            WebSocketSession session,
            BinaryMessage message
    ) {

        Long recordingSessionId = resolveRecordingSessionId(session);

        ByteBuffer payload = message.getPayload(); //ByteBuffer 구현에 따라 array()를 직접 못 쓰는 경우 대비

        byte[] audioChunk = new byte[payload.remaining()];

        payload.get(audioChunk);

        log.info(
                "음성 chunk 수신. recordingSessionId={}, size={} bytes",
                recordingSessionId,
                audioChunk.length
        );

        liveTranscriptionService.acceptAudio(
                recordingSessionId,
                audioChunk
        );
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {

        log.info(
                "실시간 녹음 WebSocket 종료. wsSessionId={}, status={}",
                session.getId(),
                status
        );
    }

    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception
    ) {

        log.error(
                "실시간 녹음 WebSocket 오류. wsSessionId={}",
                session.getId(),
                exception
        );
    }

    private Long resolveRecordingSessionId(
            WebSocketSession session
    ) {

        if (session.getUri() == null) {
            throw new IllegalStateException(
                    "WebSocket URI가 없습니다."
            );
        }

        String path = session.getUri().getPath();

        String value = path.substring(path.lastIndexOf("/") + 1);

        try {

            return Long.valueOf(value);

        } catch (NumberFormatException e) {

            throw new IllegalStateException(
                    "올바르지 않은 recordingSessionId입니다."
            );
        }
    }
}