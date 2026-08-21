package com.secondzip.backend.report.account.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.checklist.controller.ChecklistController;
import com.secondzip.backend.checklist.dto.request.ChecklistCheckRequestDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistDetailResponseDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistListResponseDTO;
import com.secondzip.backend.checklist.service.ChecklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChecklistControllerTest {

    @Mock
    private ChecklistService checklistService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ChecklistController controller =
                new ChecklistController(checklistService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        accountIdArgumentResolver()
                )
                .build();

        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("체크리스트 목록 조회")
    class GetChecklistList {

        @Test
        @DisplayName("인증된 회원의 체크리스트 목록을 조회한다")
        void getChecklistList_success() throws Exception {
            // given
            ChecklistListResponseDTO checklist =
                    new ChecklistListResponseDTO();

            ReflectionTestUtils.setField(
                    checklist,
                    "analysisReportId",
                    10L
            );

            ReflectionTestUtils.setField(
                    checklist,
                    "reportChecklistId",
                    100L
            );

            ReflectionTestUtils.setField(
                    checklist,
                    "roadAddress",
                    "서울특별시 강남구"
            );

            when(checklistService.getChecklistList(1L))
                    .thenReturn(List.of(checklist));

            // when
            MvcResult result = mockMvc.perform(
                            get("/api/checklists")
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            JsonNode body = readBody(result);

            assertEquals(1, body.size());

            assertEquals(
                    10L,
                    body.get(0)
                            .get("analysisReportId")
                            .asLong()
            );

            assertEquals(
                    100L,
                    body.get(0)
                            .get("reportChecklistId")
                            .asLong()
            );

            verify(checklistService)
                    .getChecklistList(1L);
        }
    }

    @Nested
    @DisplayName("체크리스트 생성")
    class CreateChecklist {

        @Test
        @DisplayName("리포트 체크리스트를 생성하면 201 Created와 체크리스트 ID를 반환한다")
        void createChecklist_success() throws Exception {
            // given
            when(checklistService.createChecklist(
                    1L,
                    10L
            )).thenReturn(100L);

            // when
            MvcResult result = mockMvc.perform(
                            post(
                                    "/api/checklists/reports/{analysisReportId}",
                                    10L
                            )
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            // then
            JsonNode body = readBody(result);

            assertEquals(
                    100L,
                    body.get("reportChecklistId")
                            .asLong()
            );

            verify(checklistService)
                    .createChecklist(1L, 10L);
        }
    }

    @Nested
    @DisplayName("체크리스트 상세조회")
    class GetChecklist {

        @Test
        @DisplayName("체크리스트 주소, 항목, 녹음 세션 ID를 조회한다")
        void getChecklist_success() throws Exception {
            // given
            ChecklistDetailResponseDTO response =
                    ChecklistDetailResponseDTO.builder()
                            .roadAddress(
                                    "서울특별시 강남구 테헤란로"
                            )
                            .detailAddress("101동 101호")
                            .items(List.of())
                            .recordingSessionId(200L)
                            .build();

            when(checklistService.getChecklist(
                    1L,
                    100L
            )).thenReturn(response);

            // when
            MvcResult result = mockMvc.perform(
                            get(
                                    "/api/checklists/{reportChecklistId}",
                                    100L
                            )
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            JsonNode body = readBody(result);

            assertEquals(
                    "서울특별시 강남구 테헤란로",
                    body.get("roadAddress").asText()
            );

            assertEquals(
                    "101동 101호",
                    body.get("detailAddress").asText()
            );

            assertEquals(
                    200L,
                    body.get("recordingSessionId")
                            .asLong()
            );

            verify(checklistService)
                    .getChecklist(1L, 100L);
        }
    }

    @Nested
    @DisplayName("체크 상태 변경")
    class UpdateChecklist {

        @Test
        @DisplayName("체크리스트 항목의 상태를 변경하면 204 No Content를 반환한다")
        void updateChecklist_success() throws Exception {
            // given
            String requestBody = """
                    {
                      "checked": true
                    }
                    """;

            // when & then
            mockMvc.perform(
                            patch(
                                    "/api/checklists/{reportChecklistId}/items/{checklistItemId}",
                                    100L,
                                    1000L
                            )
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content(requestBody)
                    )
                    .andExpect(status().isNoContent());

            verify(checklistService)
                    .updateCheckStatus(
                            eq(1L),
                            eq(100L),
                            eq(1000L),
                            argThat(dto ->
                                    Boolean.TRUE.equals(
                                            dto.getChecked()
                                    )
                            )
                    );
        }

        @Test
        @DisplayName("checked 값이 없으면 400 Bad Request를 반환한다")
        void updateChecklist_missingChecked_returnsBadRequest()
                throws Exception {

            // when & then
            mockMvc.perform(
                            patch(
                                    "/api/checklists/{reportChecklistId}/items/{checklistItemId}",
                                    100L,
                                    1000L
                            )
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content("{}")
                    )
                    .andExpect(status().isBadRequest());

            verify(checklistService, never())
                    .updateCheckStatus(
                            anyLong(),
                            anyLong(),
                            anyLong(),
                            any(ChecklistCheckRequestDTO.class)
                    );
        }
    }

    @Nested
    @DisplayName("체크리스트 초기화")
    class ResetChecklist {

        @Test
        @DisplayName("체크리스트를 초기화하면 204 No Content를 반환한다")
        void resetChecklist_success() throws Exception {
            // when & then
            mockMvc.perform(
                            patch(
                                    "/api/checklists/{reportChecklistId}/reset",
                                    100L
                            )
                    )
                    .andExpect(status().isNoContent());

            verify(checklistService)
                    .resetChecklist(1L, 100L);
        }
    }

    private HandlerMethodArgumentResolver
    accountIdArgumentResolver() {

        return new HandlerMethodArgumentResolver() {

            @Override
            public boolean supportsParameter(
                    MethodParameter parameter
            ) {
                return parameter.hasParameterAnnotation(
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

    private JsonNode readBody(MvcResult result)
            throws Exception {

        return objectMapper.readTree(
                result.getResponse()
                        .getContentAsString(
                                StandardCharsets.UTF_8
                        )
        );
    }
}