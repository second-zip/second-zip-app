package com.secondzip.backend.report.record.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.record.controller.RecordingController;
import com.secondzip.backend.record.dto.response.RecordingFileUrlResponseDTO;
import com.secondzip.backend.record.dto.response.RecordingLiveStartResponseDTO;
import com.secondzip.backend.record.dto.response.RecordingSessionResponseDTO;
import com.secondzip.backend.record.dto.response.RecordingStatusResponseDTO;
import com.secondzip.backend.record.dto.response.RecordingTranscriptResponseDTO;
import com.secondzip.backend.record.enums.RecordingStatus;
import com.secondzip.backend.record.service.RecordingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecordingControllerTest {

    @Mock
    private RecordingService recordingService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        RecordingController controller =
                new RecordingController(recordingService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        accountIdArgumentResolver()
                )
                .build();

        objectMapper = new ObjectMapper();
    }


    /*
     * =========================================================
     * 녹음 파일 업로드
     * =========================================================
     */
    @Nested
    @DisplayName("녹음 파일 업로드")
    class UploadRecording {

        @Test
        @DisplayName("녹음 파일을 업로드하면 202 Accepted와 세션 정보를 반환한다")
        void uploadRecording_success() throws Exception {

            // given
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "recording.mp3",
                            "audio/mpeg",
                            "audio-data".getBytes()
                    );

            RecordingSessionResponseDTO response =
                    RecordingSessionResponseDTO.builder()
                            .recordingSessionId(10L)
                            .status(RecordingStatus.UPLOADED)
                            .build();

            when(recordingService.createSession(
                    eq(1L),
                    eq(100L),
                    any()
            )).thenReturn(response);


            // when
            MvcResult result =
                    mockMvc.perform(
                                    multipart("/api/recordings")
                                            .file(file)
                                            .param(
                                                    "reportChecklistId",
                                                    "100"
                                            )
                            )
                            .andExpect(
                                    status().isAccepted()
                            )
                            .andReturn();


            // then
            JsonNode body = readBody(result);

            assertEquals(
                    10L,
                    body.get("recordingSessionId")
                            .asLong()
            );

            assertEquals(
                    "UPLOADED",
                    body.get("status").asText()
            );

            verify(recordingService)
                    .createSession(
                            eq(1L),
                            eq(100L),
                            argThat(uploadedFile ->
                                    uploadedFile
                                            .getOriginalFilename()
                                            .equals("recording.mp3")
                            )
                    );
        }


        @Test
        @DisplayName("파일이 없으면 400 Bad Request를 반환한다")
        void uploadRecording_missingFile_returnsBadRequest()
                throws Exception {

            // when & then
            mockMvc.perform(
                            multipart("/api/recordings")
                                    .param(
                                            "reportChecklistId",
                                            "100"
                                    )
                    )
                    .andExpect(
                            status().isBadRequest()
                    );

            verifyNoInteractions(
                    recordingService
            );
        }


        @Test
        @DisplayName("reportChecklistId가 없으면 400 Bad Request를 반환한다")
        void uploadRecording_missingChecklistId_returnsBadRequest()
                throws Exception {

            // given
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "recording.mp3",
                            "audio/mpeg",
                            "audio-data".getBytes()
                    );

            // when & then
            mockMvc.perform(
                            multipart("/api/recordings")
                                    .file(file)
                    )
                    .andExpect(
                            status().isBadRequest()
                    );

            verifyNoInteractions(
                    recordingService
            );
        }
    }


    /*
     * =========================================================
     * 파일 방식 STT 시작
     * =========================================================
     */
    @Nested
    @DisplayName("Transcription 시작")
    class StartTranscription {

        @Test
        @DisplayName("녹음 세션의 STT를 시작하면 202 Accepted를 반환한다")
        void transcribe_success() throws Exception {

            // when & then
            mockMvc.perform(
                            post(
                                    "/api/recordings/{recordingSessionId}/transcribe",
                                    10L
                            )
                    )
                    .andExpect(
                            status().isAccepted()
                    );

            verify(recordingService)
                    .startTranscription(
                            1L,
                            10L
                    );
        }
    }


    /*
     * =========================================================
     * 녹음 상태 조회
     * =========================================================
     */
    @Nested
    @DisplayName("녹음 상태 조회")
    class GetRecordingStatus {

        @Test
        @DisplayName("녹음 분석 상태를 조회한다")
        void getRecordingStatus_success()
                throws Exception {

            // given
            RecordingStatusResponseDTO response =
                    RecordingStatusResponseDTO.builder()
                            .recordingSessionId(10L)
                            .status(
                                    RecordingStatus.COMPLETED
                            )
                            .transcript(
                                    "계약 내용을 확인했습니다."
                            )
                            .summary(
                                    "체크리스트 분석 완료"
                            )
                            .failureReason(null)
                            .build();

            when(recordingService
                    .getRecordingStatus(
                            1L,
                            10L
                    ))
                    .thenReturn(response);


            // when
            MvcResult result =
                    mockMvc.perform(
                                    get(
                                            "/api/recordings/{recordingSessionId}",
                                            10L
                                    )
                            )
                            .andExpect(
                                    status().isOk()
                            )
                            .andReturn();


            // then
            JsonNode body =
                    readBody(result);

            assertEquals(
                    10L,
                    body.get("recordingSessionId")
                            .asLong()
            );

            assertEquals(
                    "COMPLETED",
                    body.get("status")
                            .asText()
            );

            assertEquals(
                    "계약 내용을 확인했습니다.",
                    body.get("transcript")
                            .asText()
            );

            assertEquals(
                    "체크리스트 분석 완료",
                    body.get("summary")
                            .asText()
            );

            verify(recordingService)
                    .getRecordingStatus(
                            1L,
                            10L
                    );
        }
    }


    /*
     * =========================================================
     * 실시간 녹음 시작
     * =========================================================
     */
    @Nested
    @DisplayName("실시간 녹음 시작")
    class StartLiveRecording {

        @Test
        @DisplayName("실시간 녹음을 시작하면 201 Created와 세션 정보를 반환한다")
        void startLiveRecording_success()
                throws Exception {

            // given
            RecordingLiveStartResponseDTO response =
                    RecordingLiveStartResponseDTO.builder()
                            .recordingSessionId(20L)
                            .status(
                                    RecordingStatus.RECORDING
                            )
                            .build();

            when(recordingService
                    .startLiveRecording(
                            1L,
                            100L
                    ))
                    .thenReturn(response);


            // when
            MvcResult result =
                    mockMvc.perform(
                                    post(
                                            "/api/recordings/live"
                                    )
                                            .contentType(
                                                    MediaType.APPLICATION_JSON
                                            )
                                            .content("""
                                                    {
                                                      "reportChecklistId": 100
                                                    }
                                                    """)
                            )
                            .andExpect(
                                    status().isCreated()
                            )
                            .andReturn();


            // then
            JsonNode body =
                    readBody(result);

            assertEquals(
                    20L,
                    body.get("recordingSessionId")
                            .asLong()
            );

            assertEquals(
                    "RECORDING",
                    body.get("status")
                            .asText()
            );

            verify(recordingService)
                    .startLiveRecording(
                            1L,
                            100L
                    );
        }
    }


    /*
     * =========================================================
     * 실시간 녹음 종료
     * =========================================================
     */
    @Nested
    @DisplayName("실시간 녹음 종료")
    class StopLiveRecording {

        @Test
        @DisplayName("최종 녹음 파일과 함께 실시간 녹음을 종료하면 202 Accepted를 반환한다")
        void stopLiveRecording_withFile_success()
                throws Exception {

            // given
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "final-recording.mp3",
                            "audio/mpeg",
                            "final-audio".getBytes()
                    );


            // when & then
            mockMvc.perform(
                            multipart(
                                    "/api/recordings/{recordingSessionId}/stop",
                                    20L
                            )
                                    .file(file)
                    )
                    .andExpect(
                            status().isAccepted()
                    );


            verify(recordingService)
                    .stopLiveRecording(
                            eq(1L),
                            eq(20L),
                            argThat(uploadedFile ->
                                    uploadedFile
                                            .getOriginalFilename()
                                            .equals(
                                                    "final-recording.mp3"
                                            )
                            )
                    );
        }


        @Test
        @DisplayName("파일 없이 실시간 녹음을 종료할 수도 있다")
        void stopLiveRecording_withoutFile_success()
                throws Exception {

            // when & then
            mockMvc.perform(
                            multipart(
                                    "/api/recordings/{recordingSessionId}/stop",
                                    20L
                            )
                    )
                    .andExpect(
                            status().isAccepted()
                    );


            verify(recordingService)
                    .stopLiveRecording(
                            1L,
                            20L,
                            null
                    );
        }
    }


    /*
     * =========================================================
     * 녹음 파일 URL 조회
     * =========================================================
     */
    @Nested
    @DisplayName("녹음 파일 URL 조회")
    class GetRecordingFileUrl {

        @Test
        @DisplayName("녹음 파일 재생용 임시 URL을 조회한다")
        void getRecordingFileUrl_success()
                throws Exception {

            // given
            RecordingFileUrlResponseDTO response =
                    RecordingFileUrlResponseDTO.builder()
                            .url(
                                    "https://storage.example.com/recording.mp3"
                            )
                            .originalFileName(
                                    "recording.mp3"
                            )
                            .contentType(
                                    "audio/mpeg"
                            )
                            .fileSize(
                                    12345L
                            )
                            .expiresIn(
                                    300L
                            )
                            .build();

            when(recordingService
                    .getRecordingFileUrl(
                            1L,
                            10L
                    ))
                    .thenReturn(response);


            // when
            MvcResult result =
                    mockMvc.perform(
                                    get(
                                            "/api/recordings/{recordingSessionId}/file-url",
                                            10L
                                    )
                            )
                            .andExpect(
                                    status().isOk()
                            )
                            .andReturn();


            // then
            JsonNode body =
                    readBody(result);

            assertEquals(
                    "https://storage.example.com/recording.mp3",
                    body.get("url")
                            .asText()
            );

            assertEquals(
                    "recording.mp3",
                    body.get("originalFileName")
                            .asText()
            );

            assertEquals(
                    "audio/mpeg",
                    body.get("contentType")
                            .asText()
            );

            assertEquals(
                    12345L,
                    body.get("fileSize")
                            .asLong()
            );

            assertEquals(
                    300L,
                    body.get("expiresIn")
                            .asLong()
            );

            verify(recordingService)
                    .getRecordingFileUrl(
                            1L,
                            10L
                    );
        }
    }


    /*
     * =========================================================
     * Transcript 조회
     * =========================================================
     */
    @Nested
    @DisplayName("Transcript 조회")
    class GetTranscript {

        @Test
        @DisplayName("녹음 세션의 전체 Transcript를 조회한다")
        void getTranscript_success()
                throws Exception {

            // given
            RecordingTranscriptResponseDTO response =
                    RecordingTranscriptResponseDTO.builder()
                            .recordingSessionId(10L)
                            .transcript(
                                    "전체 계약 녹취 내용입니다."
                            )
                            .build();

            when(recordingService.getTranscript(
                    1L,
                    10L
            )).thenReturn(response);


            // when
            MvcResult result =
                    mockMvc.perform(
                                    get(
                                            "/api/recordings/{recordingSessionId}/transcript",
                                            10L
                                    )
                            )
                            .andExpect(
                                    status().isOk()
                            )
                            .andReturn();


            // then
            JsonNode body =
                    readBody(result);

            assertEquals(
                    10L,
                    body.get("recordingSessionId")
                            .asLong()
            );

            assertEquals(
                    "전체 계약 녹취 내용입니다.",
                    body.get("transcript")
                            .asText()
            );

            verify(recordingService)
                    .getTranscript(
                            1L,
                            10L
                    );
        }
    }


    /*
     * =========================================================
     * 녹음 삭제
     * =========================================================
     */
    @Nested
    @DisplayName("녹음 삭제")
    class DeleteRecording {

        @Test
        @DisplayName("녹음 세션을 삭제하면 204 No Content를 반환한다")
        void deleteRecording_success()
                throws Exception {

            // when & then
            mockMvc.perform(
                            delete(
                                    "/api/recordings/{recordingSessionId}",
                                    10L
                            )
                    )
                    .andExpect(
                            status().isNoContent()
                    );


            verify(recordingService)
                    .deleteRecording(
                            1L,
                            10L
                    );
        }
    }


    /*
     * =========================================================
     * @AuthenticationPrincipal 테스트용 Resolver
     * =========================================================
     */
    private HandlerMethodArgumentResolver
    accountIdArgumentResolver() {

        return new HandlerMethodArgumentResolver() {

            @Override
            public boolean supportsParameter(
                    MethodParameter parameter
            ) {

                return parameter
                        .hasParameterAnnotation(
                                AuthenticationPrincipal.class
                        );
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {

                return 1L;
            }
        };
    }


    /*
     * =========================================================
     * JSON 응답 읽기
     * =========================================================
     */
    private JsonNode readBody(
            MvcResult result
    ) throws Exception {

        return objectMapper.readTree(
                result.getResponse()
                        .getContentAsString(
                                StandardCharsets.UTF_8
                        )
        );
    }
}