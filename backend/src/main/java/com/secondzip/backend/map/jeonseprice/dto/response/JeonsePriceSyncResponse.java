package com.secondzip.backend.map.jeonseprice.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JeonsePriceSyncResponse {

    private String targetMonth;
    private int savedCount;

    public static JeonsePriceSyncResponse of(
            String targetMonth,
            int savedCount
    ) {
        return JeonsePriceSyncResponse.builder()
                .targetMonth(targetMonth)
                .savedCount(savedCount)
                .build();
    }
}
