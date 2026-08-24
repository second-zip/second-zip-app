package com.secondzip.backend.report.record.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nbp.cdncp.nest.grpc.proto.v1.NestRequest;
import com.nbp.cdncp.nest.grpc.proto.v1.RequestType;
import com.secondzip.backend.record.client.ClovaRealtimeSpeechClient;
import com.secondzip.backend.record.grpc.ClovaGrpcSession;
import com.secondzip.backend.record.grpc.ClovaGrpcSessionManager;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClovaRealtimeSpeechClientTest {

    @Mock
    private ClovaGrpcSessionManager sessionManager;

    @Mock
    private ClovaGrpcSession grpcSession;

    @Mock
    private StreamObserver<NestRequest> requestObserver;

    private ClovaRealtimeSpeechClient client;

    @BeforeEach
    void setUp() {
        client = new ClovaRealtimeSpeechClient(
                new ObjectMapper(),
                sessionManager
        );
    }

    @Test
    @DisplayName("음성 Chunk를 DATA 요청으로 CLOVA에 전달한다")
    void sendAudio_success() {
        // given
        byte[] audio = {1, 2, 3, 4};

        when(sessionManager.get(10L))
                .thenReturn(grpcSession);

        when(grpcSession.nextSequence())
                .thenReturn(1);

        when(grpcSession.getRequestObserver())
                .thenReturn(requestObserver);

        // when
        client.sendAudio(
                10L,
                audio
        );

        // then
        ArgumentCaptor<NestRequest> captor =
                ArgumentCaptor.forClass(
                        NestRequest.class
                );

        verify(requestObserver)
                .onNext(captor.capture());

        NestRequest request =
                captor.getValue();

        assertEquals(
                RequestType.DATA,
                request.getType()
        );

        assertArrayEquals(
                audio,
                request.getData()
                        .getChunk()
                        .toByteArray()
        );

        assertTrue(
                request.getData()
                        .getExtraContents()
                        .contains("\"seqId\": 1")
        );
    }

    @Test
    @DisplayName("빈 음성 Chunk는 CLOVA로 보내지 않는다")
    void sendAudio_empty_doesNothing() {
        // when
        client.sendAudio(
                10L,
                new byte[0]
        );

        // then
        verifyNoInteractions(sessionManager);
    }

    @Test
    @DisplayName("null 음성 Chunk는 CLOVA로 보내지 않는다")
    void sendAudio_null_doesNothing() {
        client.sendAudio(10L, null);

        verifyNoInteractions(sessionManager);
    }

    @Test
    @DisplayName("실시간 STT 종료 시 요청 종료, 응답 대기, Channel 종료를 순서대로 수행한다")
    void finish_success() {
        // given
        when(sessionManager.remove(10L))
                .thenReturn(grpcSession);

        // when
        client.finish(10L);

        // then
        InOrder inOrder =
                inOrder(grpcSession);

        inOrder.verify(grpcSession)
                .completeRequest();

        inOrder.verify(grpcSession)
                .awaitCompletion();

        inOrder.verify(grpcSession)
                .shutdown();
    }

    @Test
    @DisplayName("종료할 gRPC 세션이 없으면 아무 작업도 수행하지 않는다")
    void finish_missingSession_doesNothing() {
        // given
        when(sessionManager.remove(10L))
                .thenReturn(null);

        // when
        client.finish(10L);

        // then
        verifyNoInteractions(grpcSession);
    }
}