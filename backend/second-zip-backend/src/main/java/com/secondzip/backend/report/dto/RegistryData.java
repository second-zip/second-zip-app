package com.secondzip.backend.report.dto;

import lombok.Getter;

@Getter
public class RegistryData {
    private Long mortgageAmount;              // 근저당 총액
    private Boolean hasSeizure;               // 압류/가압류/경매 여부
    private Boolean hasTrustRegistration;     // 신탁등기 존재 여부
    private String ownerName;                 // 등기상 소유자명
    private String ownerType;                 // INDIVIDUAL / TRUST_COMPANY / CORPORATION
    private String landOwnerName;             // 토지 소유자명
    private Boolean hasPostTrustInfringement; // 신탁등기 이후 권리침해 여부
}