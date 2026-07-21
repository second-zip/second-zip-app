package com.secondzip.backend.report.dto;

import lombok.Data;

@Data
public class PriceData {
    private Long recentSalePrice;  // 최근 매매가 (null = 거래 없음 → 확인 불가)
    private Long officialPrice;    // 공시가격 (null 가능)
}