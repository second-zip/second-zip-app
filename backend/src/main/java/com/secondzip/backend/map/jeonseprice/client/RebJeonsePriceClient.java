package com.secondzip.backend.map.jeonseprice.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.map.jeonseprice.dto.external.RebJeonsePriceApiResponse;
import com.secondzip.backend.map.jeonseprice.dto.external.RebJeonsePriceRowDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RebJeonsePriceClient {

    private static final String API_URL =
            "https://www.reb.or.kr/r-one/openapi/SttsApiTblData.do";

    private static final String STAT_TABLE_ID =
            "A_2024_00019";

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMM");

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Value("${OPEN_API_KEY}")
    private String openApiKey;

    public List<RebJeonsePriceRowDTO> getMonthlyPriceIndices(YearMonth baseMonth) {
        URI uri = createUri(baseMonth);
        try {
            String jsonResponse = restTemplate.getForObject(uri, String.class);

            if (jsonResponse == null || jsonResponse.isBlank()) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR, "한국부동산원 API 응답이 비어 있습니다."
                );
            }

            RebJeonsePriceApiResponse response = objectMapper.readValue(jsonResponse, RebJeonsePriceApiResponse.class);

            List<RebJeonsePriceRowDTO> rows = response.getRows();

            if (rows.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "해당 기준월의 전세가격지수 데이터가 없습니다: " + baseMonth
                );
            }

            return rows;

        } catch (RestClientException exception) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR, "한국부동산원 API 호출에 실패했습니다."
            );

        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR, "한국부동산원 API 응답 처리에 실패했습니다."
            );
        }
    }

    private URI createUri(YearMonth baseMonth) {
        return UriComponentsBuilder
                .fromHttpUrl(API_URL)
                .queryParam("KEY", openApiKey)
                .queryParam("Type", "json")
                .queryParam("pIndex", 1)
                .queryParam("pSize", 1000)
                .queryParam("STATBL_ID", STAT_TABLE_ID)
                .queryParam("DTACYCLE_CD", "MM")
                .queryParam(
                        "WRTTIME_IDTFR_ID",
                        baseMonth.format(MONTH_FORMATTER)
                )
                .build()
                .encode()
                .toUri();
    }
}