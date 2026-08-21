package com.secondzip.backend.report.record.service;

import com.secondzip.backend.record.dto.response.ChecklistAnalysisResult;
import com.secondzip.backend.record.enums.ChecklistAnalysisStatus;
import com.secondzip.backend.record.service.LiveRecordingContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LiveRecordingContextTest {

    @Nested
    @DisplayName("Transcript 추가")
    class AppendTranscript {

        @Test
        @DisplayName("현재 transcript 끝 위치에 텍스트를 추가한다")
        void appendTranscript_endPosition_appendsText() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            // when
            context.appendTranscript(0, "안녕하세요");
            context.appendTranscript(5, "반갑습니다");

            // then
            assertEquals(
                    "안녕하세요반갑습니다",
                    context.getTranscript()
            );
        }

        @Test
        @DisplayName("기존 위치의 인식 결과가 수정되면 해당 구간을 교체한다")
        void appendTranscript_existingPosition_replacesText() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            context.appendTranscript(
                    0,
                    "안녕하세요"
            );

            // when
            context.appendTranscript(
                    2,
                    "가세"
            );

            // then
            assertEquals(
                    "안녕가세요",
                    context.getTranscript()
            );
        }

        @Test
        @DisplayName("null 텍스트는 transcript에 추가하지 않는다")
        void appendTranscript_nullText_doesNothing() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            context.appendTranscript(
                    0,
                    "테스트"
            );

            // when
            context.appendTranscript(
                    3,
                    null
            );

            // then
            assertEquals(
                    "테스트",
                    context.getTranscript()
            );
        }

        @Test
        @DisplayName("빈 문자열은 transcript에 추가하지 않는다")
        void appendTranscript_blankText_doesNothing() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            context.appendTranscript(
                    0,
                    "테스트"
            );

            // when
            context.appendTranscript(
                    3,
                    "   "
            );

            // then
            assertEquals(
                    "테스트",
                    context.getTranscript()
            );
        }

        @Test
        @DisplayName("position이 음수이면 transcript에 추가하지 않는다")
        void appendTranscript_negativePosition_doesNothing() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            // when
            context.appendTranscript(
                    -1,
                    "테스트"
            );

            // then
            assertEquals(
                    "",
                    context.getTranscript()
            );
        }

        @Test
        @DisplayName("현재 길이보다 큰 position이 들어오면 텍스트를 뒤에 추가한다")
        void appendTranscript_gapPosition_appendsText() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            context.appendTranscript(
                    0,
                    "안녕"
            );

            // when
            context.appendTranscript(
                    10,
                    "하세요"
            );

            // then
            assertEquals(
                    "안녕하세요",
                    context.getTranscript()
            );
        }
    }


    @Nested
    @DisplayName("분석되지 않은 Transcript 길이")
    class UnanalyzedLength {

        @Test
        @DisplayName("분석 전에는 전체 transcript 길이를 반환한다")
        void getUnanalyzedLength_beforeAnalysis_returnsFullLength() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            context.appendTranscript(
                    0,
                    "안녕하세요"
            );

            // when
            int result =
                    context.getUnanalyzedLength();

            // then
            assertEquals(
                    5,
                    result
            );
        }

        @Test
        @DisplayName("분석 완료 표시 후에는 미분석 길이가 0이 된다")
        void markAnalyzed_setsUnanalyzedLengthToZero() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            context.appendTranscript(
                    0,
                    "안녕하세요"
            );

            // when
            context.markAnalyzed();

            // then
            assertEquals(
                    0,
                    context.getUnanalyzedLength()
            );
        }

        @Test
        @DisplayName("분석 완료 이후 추가된 transcript 길이만 반환한다")
        void getUnanalyzedLength_afterNewTranscript_returnsAddedLength() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            context.appendTranscript(
                    0,
                    "안녕하세요"
            );

            context.markAnalyzed();

            // when
            context.appendTranscript(
                    5,
                    "반갑습니다"
            );

            // then
            assertEquals(
                    5,
                    context.getUnanalyzedLength()
            );
        }
    }


    @Nested
    @DisplayName("분석 실행 상태")
    class AnalysisState {

        @Test
        @DisplayName("분석 중이 아니면 분석 시작에 성공한다")
        void startAnalysis_notAnalyzing_returnsTrue() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            // when
            boolean result =
                    context.startAnalysis();

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("이미 분석 중이면 추가 분석 시작에 실패한다")
        void startAnalysis_alreadyAnalyzing_returnsFalse() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            context.startAnalysis();

            // when
            boolean result =
                    context.startAnalysis();

            // then
            assertFalse(result);
        }

        @Test
        @DisplayName("분석 종료 후에는 다시 분석을 시작할 수 있다")
        void finishAnalysis_allowsNextAnalysis() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            context.startAnalysis();

            // when
            context.finishAnalysis();

            // then
            assertTrue(
                    context.startAnalysis()
            );
        }
    }


    @Nested
    @DisplayName("실시간 임시 분석 결과")
    class ProvisionalResult {

        @Test
        @DisplayName("분석 결과를 PROVISIONAL 상태로 저장한다")
        void updateProvisional_savesProvisionalResult() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            ChecklistAnalysisResult.ResultItem item =
                    ChecklistAnalysisResult.ResultItem.builder()
                            .checklistItemId(10L)
                            .status(
                                    ChecklistAnalysisStatus.CHECKED
                            )
                            .confidenceScore(
                                    new BigDecimal("0.90")
                            )
                            .evidenceText(
                                    "등기부등본을 확인했습니다."
                            )
                            .reason(
                                    "계약 전 확인 완료"
                            )
                            .build();

            // when
            context.updateProvisional(item);

            // then
            Map<Long, ChecklistAnalysisResult.ResultItem>
                    results =
                    context.getProvisionalResults();

            ChecklistAnalysisResult.ResultItem saved =
                    results.get(10L);

            assertNotNull(saved);

            assertEquals(
                    10L,
                    saved.getChecklistItemId()
            );

            assertEquals(
                    ChecklistAnalysisStatus.PROVISIONAL,
                    saved.getStatus()
            );

            assertEquals(
                    new BigDecimal("0.90"),
                    saved.getConfidenceScore()
            );

            assertEquals(
                    "등기부등본을 확인했습니다.",
                    saved.getEvidenceText()
            );

            assertEquals(
                    "계약 전 확인 완료",
                    saved.getReason()
            );
        }

        @Test
        @DisplayName("같은 체크리스트 항목의 새 결과가 들어오면 기존 결과를 덮어쓴다")
        void updateProvisional_sameItem_replacesExistingResult() {
            // given
            LiveRecordingContext context =
                    new LiveRecordingContext();

            ChecklistAnalysisResult.ResultItem first =
                    ChecklistAnalysisResult.ResultItem.builder()
                            .checklistItemId(10L)
                            .confidenceScore(
                                    new BigDecimal("0.60")
                            )
                            .evidenceText("첫 번째")
                            .reason("첫 번째 분석")
                            .build();

            ChecklistAnalysisResult.ResultItem second =
                    ChecklistAnalysisResult.ResultItem.builder()
                            .checklistItemId(10L)
                            .confidenceScore(
                                    new BigDecimal("0.95")
                            )
                            .evidenceText("두 번째")
                            .reason("두 번째 분석")
                            .build();

            context.updateProvisional(first);

            // when
            context.updateProvisional(second);

            // then
            assertEquals(
                    1,
                    context.getProvisionalResults().size()
            );

            ChecklistAnalysisResult.ResultItem saved =
                    context.getProvisionalResults()
                            .get(10L);

            assertEquals(
                    new BigDecimal("0.95"),
                    saved.getConfidenceScore()
            );

            assertEquals(
                    "두 번째",
                    saved.getEvidenceText()
            );
        }
    }
}