package com.secondzip.backend.report.record.service;

import com.secondzip.backend.record.service.LiveRecordingContext;
import com.secondzip.backend.record.service.LiveRecordingContextManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LiveRecordingContextManagerTest {

    private LiveRecordingContextManager contextManager;

    @BeforeEach
    void setUp() {
        contextManager =
                new LiveRecordingContextManager();
    }


    @Nested
    @DisplayName("Context 생성")
    class Create {

        @Test
        @DisplayName("recordingSessionId로 새로운 Context를 생성한다")
        void create_success() {
            // given
            Long recordingSessionId = 100L;

            // when
            LiveRecordingContext result =
                    contextManager.create(
                            recordingSessionId
                    );

            // then
            assertNotNull(result);

            assertSame(
                    result,
                    contextManager.get(
                            recordingSessionId
                    )
            );
        }

        @Test
        @DisplayName("동일한 recordingSessionId로 다시 생성하면 기존 Context를 교체한다")
        void create_sameSessionId_replacesContext() {
            // given
            Long recordingSessionId = 100L;

            LiveRecordingContext first =
                    contextManager.create(
                            recordingSessionId
                    );

            // when
            LiveRecordingContext second =
                    contextManager.create(
                            recordingSessionId
                    );

            // then
            assertNotSame(
                    first,
                    second
            );

            assertSame(
                    second,
                    contextManager.get(
                            recordingSessionId
                    )
            );
        }
    }


    @Nested
    @DisplayName("Context 조회")
    class Get {

        @Test
        @DisplayName("존재하는 Context를 조회한다")
        void get_existingContext_returnsContext() {
            // given
            LiveRecordingContext expected =
                    contextManager.create(100L);

            // when
            LiveRecordingContext result =
                    contextManager.get(100L);

            // then
            assertSame(
                    expected,
                    result
            );
        }

        @Test
        @DisplayName("Context가 존재하지 않으면 예외가 발생한다")
        void get_missingContext_throwsException() {
            // given
            Long recordingSessionId = 999L;

            // when
            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> contextManager.get(
                                    recordingSessionId
                            )
                    );

            // then
            assertEquals(
                    "실시간 녹음 Context가 존재하지 않습니다. recordingSessionId=999",
                    exception.getMessage()
            );
        }
    }


    @Nested
    @DisplayName("Context 안전 조회")
    class Find {

        @Test
        @DisplayName("존재하는 Context이면 해당 Context를 반환한다")
        void find_existingContext_returnsContext() {
            // given
            LiveRecordingContext expected =
                    contextManager.create(100L);

            // when
            LiveRecordingContext result =
                    contextManager.find(100L);

            // then
            assertSame(
                    expected,
                    result
            );
        }

        @Test
        @DisplayName("Context가 존재하지 않으면 null을 반환한다")
        void find_missingContext_returnsNull() {
            // when
            LiveRecordingContext result =
                    contextManager.find(999L);

            // then
            assertNull(result);
        }
    }


    @Nested
    @DisplayName("Context 삭제")
    class Remove {

        @Test
        @DisplayName("Context를 삭제하고 삭제된 객체를 반환한다")
        void remove_existingContext_returnsRemovedContext() {
            // given
            LiveRecordingContext expected =
                    contextManager.create(100L);

            // when
            LiveRecordingContext removed =
                    contextManager.remove(100L);

            // then
            assertSame(
                    expected,
                    removed
            );

            assertNull(
                    contextManager.find(100L)
            );
        }

        @Test
        @DisplayName("존재하지 않는 Context를 삭제하면 null을 반환한다")
        void remove_missingContext_returnsNull() {
            // when
            LiveRecordingContext result =
                    contextManager.remove(999L);

            // then
            assertNull(result);
        }

        @Test
        @DisplayName("Context 삭제 후 get으로 조회하면 예외가 발생한다")
        void remove_thenGet_throwsException() {
            // given
            contextManager.create(100L);

            // when
            contextManager.remove(100L);

            // then
            assertThrows(
                    IllegalStateException.class,
                    () -> contextManager.get(100L)
            );
        }
    }
}