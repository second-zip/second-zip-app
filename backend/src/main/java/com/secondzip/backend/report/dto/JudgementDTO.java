package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.RiskLevel;

/**
 * 개별 판정 결과. 위험도와 데이터 상태를 한 쌍으로 묶는다.
 *
 * <p>값 자체가 정체성인 불변 객체(Value Object)다. 식별자가 없고,
 * 두 필드가 같으면 같은 판정으로 취급한다.
 *
 * <p>이 둘을 따로 반환하면 "CAUTION인데 데이터 상태를 안 실어 보내는" 실수가 나기 쉬워
 * 항상 함께 다니도록 묶었다.
 */
public record JudgementDTO(RiskLevel riskLevel, DataStatus dataStatus) {

    public JudgementDTO {
        if (riskLevel == null || dataStatus == null) {
            throw new IllegalArgumentException("위험도와 데이터 상태는 모두 필요합니다.");
        }
    }

    /** 데이터를 확보해 실제로 판정한 경우. */
    public static JudgementDTO verified(RiskLevel riskLevel) {
        return new JudgementDTO(riskLevel, DataStatus.VERIFIED);
    }

    /**
     * 데이터를 확보하지 못한 경우.
     * 위험도는 CAUTION으로 두되, 화면에서는 "확인 불가"로 구분해 보여줄 수 있다.
     */
    public static JudgementDTO unverified() {
        return new JudgementDTO(RiskLevel.CAUTION, DataStatus.UNVERIFIED);
    }

    /**
     * 이 매물에 해당하지 않는 항목.
     *
     * <p>집계에서는 제외되므로 여기 담긴 위험도는 <b>표시용</b>일 뿐이다.
     * SAFE로 두면 {@code dataStatus}를 읽지 않는 클라이언트에서
     * "확인하지도 않은 항목"이 "안전"으로 표시되어 잘못된 안심을 준다.
     * 그래서 기존 화면과 동일하게 CAUTION으로 둔다.
     *
     * <p>클라이언트가 {@code NOT_APPLICABLE}을 "해당 없음"으로 따로 표시하게 되면
     * 이 값은 무의미해진다.
     */
    public static JudgementDTO notApplicable() {
        return new JudgementDTO(RiskLevel.CAUTION, DataStatus.NOT_APPLICABLE);
    }

    public boolean isApplicable() {
        return dataStatus != DataStatus.NOT_APPLICABLE;
    }
}
