package com.secondzip.backend.report.record.websocket;

import com.secondzip.backend.record.service.LiveTranscriptionService;
import com.secondzip.backend.record.websocket.RecordingWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordingWebSocketHandlerTest {

    @Mock
    private LiveTranscriptionService liveTranscriptionService;

    @Mock
    private WebSocketSession session;

    private RecordingWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new RecordingWebSocketHandler(
                        liveTranscriptionService
                );
    }

    @Test
    @DisplayName("WebSocket 연결 시 recordingSessionId의 실시간 STT를 시작한다")
    void afterConnectionEstablished_startsTranscription()
            throws Exception {

        // given
        when(session.getUri())
                .thenReturn(
                        new URI(
                                "ws://localhost/ws/recordings/10"
                        )
                );

        when(session.getId())
                .thenReturn("ws-1");

        // when
        handler.afterConnectionEstablished(
                session
        );

        // then
        verify(liveTranscriptionService)
                .start(10L);
    }

    @Test
    @DisplayName("Binary 음성 데이터를 실시간 STT 서비스로 전달한다")
    void handleBinaryMessage_acceptsAudio()
            throws Exception {

        // given
        when(session.getUri())
                .thenReturn(
                        new URI(
                                "ws://localhost/ws/recordings/10"
                        )
                );

        byte[] audio = {1, 2, 3};

        BinaryMessage message =
                new BinaryMessage(audio);

        // when
        handler.handleBinaryMessage(
                session,
                message
        );

        // then
        verify(liveTranscriptionService)
                .acceptAudio(
                        eq(10L),
                        aryEq(audio)
                );
    }

    @Test
    @DisplayName("WebSocket URI가 없으면 예외가 발생한다")
    void missingUri_throwsException() {
        // given
        when(session.getUri())
                .thenReturn(null);

        // when & then
        assertThrows(
                IllegalStateException.class,
                () ->
                        handler.afterConnectionEstablished(
                                session
                        )
        );

        verifyNoInteractions(
                liveTranscriptionService
        );
    }

    @Test
    @DisplayName("recordingSessionId가 숫자가 아니면 예외가 발생한다")
    void invalidRecordingSessionId_throwsException()
            throws Exception {

        // given
        when(session.getUri())
                .thenReturn(
                        new URI(
                                "ws://localhost/ws/recordings/abc"
                        )
                );

        // when & then
        assertThrows(
                IllegalStateException.class,
                () ->
                        handler.afterConnectionEstablished(
                                session
                        )
        );

        verifyNoInteractions(
                liveTranscriptionService
        );
    }
}