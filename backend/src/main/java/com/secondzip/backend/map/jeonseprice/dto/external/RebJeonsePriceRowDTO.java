package com.secondzip.backend.map.jeonseprice.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RebJeonsePriceRowDTO {
    @JsonProperty("WRTTIME_IDTFR_ID")
    private String baseMonth;

    @JsonProperty("CLS_ID")
    private Long rebClassId;

    @JsonProperty("CLS_NM")
    private String regionName;

    @JsonProperty("CLS_FULLNM")
    private String fullRegionName;

    @JsonProperty("ITM_ID")
    private Long itemId;

    @JsonProperty("ITM_NM")
    private String itemName;

    @JsonProperty("DTA_VAL")
    private BigDecimal priceIndex;
}
