package com.secondzip.backend.report.record.grpc;

import com.nbp.cdncp.nest.grpc.proto.v1.NestRequest;
import com.secondzip.backend.record.grpc.ClovaGrpcSession;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClovaGrpcSessionTest {

    @Mock
    private ManagedChannel channel;

    @Mock
    private StreamObserver<NestRequest> requestObserver;

    private ClovaGrpcSession session;

    @BeforeEach
    void setUp() {
        session = new ClovaGrpcSession(
                channel,
                requestObserver,
                new CountDownLatch(0)
        );
    }

    @Test
    @DisplayName("음성 Chunk sequence는 1부터 순차적으로 증가한다")
    void nextSequence_increments() {
        assertEquals(1, session.nextSequence());
        assertEquals(2, session.nextSequence());
        assertEquals(3, session.nextSequence());
    }

    @Test
    @DisplayName("요청 완료 시 gRPC 요청 스트림을 종료한다")
    void completeRequest_callsOnCompleted() {
        // when
        session.completeRequest();

        // then
        verify(requestObserver).onCompleted();
    }

    @Test
    @DisplayName("Channel이 정상 종료되면 강제 종료하지 않는다")
    void shutdown_success() throws Exception {
        // given
        when(channel.shutdown())
                .thenReturn(channel);

        when(channel.awaitTermination(
                3,
                TimeUnit.SECONDS
        )).thenReturn(true);

        // when
        session.shutdown();

        // then
        verify(channel).shutdown();

        verify(channel, never())
                .shutdownNow();
    }

    @Test
    @DisplayName("Channel이 제한 시간 내 종료되지 않으면 강제 종료한다")
    void shutdown_timeout_callsShutdownNow()
            throws Exception {

        // given
        when(channel.shutdown())
                .thenReturn(channel);

        when(channel.awaitTermination(
                3,
                TimeUnit.SECONDS
        )).thenReturn(false);

        // when
        session.shutdown();

        // then
        verify(channel).shutdownNow();
    }
}