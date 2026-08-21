package com.secondzip.backend.report.dto.external;

import lombok.Data;

import java.util.List;

// 등기부등본 데이터
@Data
public class RegistryData {
    private Long mortgageAmount;              // 근저당 총액
    private Boolean hasSeizure;               // 압류/가압류/경매 여부
    private Boolean hasTrustRegistration;     // 신탁등기 존재 여부
    private String ownerName;                 // 등기상 소유자명
    private List<String> ownerNames;           // 공동소유자를 정렬·중복제거한 전체 집합
    private String ownerType;                 // INDIVIDUAL / TRUST_COMPANY / CORPORATION
    private String landOwnerName;             // 토지 소유자명
    private List<String> landOwnerNames;       // 토지 공동소유자 전체 집합
    private String landOwnerType;              // 토지 소유자 유형
    private String requestedDong;              // 집합등기 조회에 사용한 동 원문 토큰
    private String requestedHo;                // 집합등기 조회에 사용한 호 원문 토큰
    private Boolean targetIdentityVerified;    // 응답 식별자가 있을 때 대상 동·호 일치 여부
    private Boolean hasPostTrustInfringement; // 신탁등기 이후 권리침해 여부
}
