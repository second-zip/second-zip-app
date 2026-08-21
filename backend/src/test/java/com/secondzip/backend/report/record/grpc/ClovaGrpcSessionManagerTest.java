package com.secondzip.backend.report.record.grpc;

import com.secondzip.backend.record.grpc.ClovaGrpcSession;
import com.secondzip.backend.record.grpc.ClovaGrpcSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ClovaGrpcSessionManagerTest {

    private ClovaGrpcSessionManager manager;

    @BeforeEach
    void setUp() {
        manager =
                new ClovaGrpcSessionManager();
    }

    @Test
    @DisplayName("gRPC 세션을 등록하고 조회한다")
    void putAndGet_success() {
        // given
        ClovaGrpcSession session =
                mock(ClovaGrpcSession.class);

        // when
        manager.put(10L, session);

        // then
        assertSame(
                session,
                manager.get(10L)
        );
    }

    @Test
    @DisplayName("동일한 recordingSessionId로 두 개의 gRPC 세션을 등록할 수 없다")
    void put_duplicate_throwsException() {
        // given
        manager.put(
                10L,
                mock(ClovaGrpcSession.class)
        );

        // when
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> manager.put(
                                10L,
                                mock(ClovaGrpcSession.class)
                        )
                );

        // then
        assertTrue(
                exception.getMessage()
                        .contains("이미 CLOVA 실시간 세션")
        );
    }

    @Test
    @DisplayName("존재하지 않는 gRPC 세션 조회 시 예외가 발생한다")
    void get_missing_throwsException() {
        assertThrows(
                IllegalStateException.class,
                () -> manager.get(999L)
        );
    }

    @Test
    @DisplayName("gRPC 세션을 제거하면 제거된 세션을 반환한다")
    void remove_success() {
        // given
        ClovaGrpcSession session =
                mock(ClovaGrpcSession.class);

        manager.put(10L, session);

        // when
        ClovaGrpcSession removed =
                manager.remove(10L);

        // then
        assertSame(session, removed);

        assertThrows(
                IllegalStateException.class,
                () -> manager.get(10L)
        );
    }
}