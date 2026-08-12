package com.secondzip.backend.map.jeonseprice.service;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.map.common.domain.RegionVO;
import com.secondzip.backend.map.common.enums.RegionLevel;
import com.secondzip.backend.map.common.mapper.RegionMapper;
import com.secondzip.backend.map.jeonseprice.domain.JeonsePriceRegionVO;
import com.secondzip.backend.map.jeonseprice.dto.response.JeonsePriceMapResponse;
import com.secondzip.backend.map.jeonseprice.mapper.JeonsePriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JeonsePriceServiceImpl
        implements JeonsePriceService {

    private final JeonsePriceMapper jeonsePriceMapper;
    private final RegionMapper regionMapper;

    @Override
    public JeonsePriceMapResponse getJeonsePrices(
            YearMonth baseMonth,
            RegionLevel regionLevel,
            String parentRegionCode
    ) {
        List<JeonsePriceRegionVO> regions;

        if (regionLevel == RegionLevel.SIDO) {
            regions = jeonsePriceMapper.selectSidoJeonsePrices(baseMonth.atDay(1));

        } else if (regionLevel == RegionLevel.SIGUNGU) {
            validateParentRegionCode(parentRegionCode);
            validateSidoRegion(parentRegionCode);

            regions = jeonsePriceMapper.selectSigunguJeonsePrices(baseMonth.atDay(1), parentRegionCode);

        } else {
            throw new BusinessException(ErrorCode.INVALID_ENUM_VALUE, "지원하지 않는 행정구역 단계입니다.");
        }

        validateJeonsePriceData(regions, baseMonth);

        return JeonsePriceMapResponse.of(
                baseMonth,
                regionLevel,
                regionLevel == RegionLevel.SIGUNGU ? parentRegionCode : null, regions
        );
    }

    private void validateParentRegionCode(String parentRegionCode) {
        if (parentRegionCode == null || parentRegionCode.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_VALUE, "SIGUNGU 조회에는 parentRegionCode가 필요합니다.");
        }
    }

    private void validateSidoRegion(String parentRegionCode) {
        RegionVO parentRegion = regionMapper.selectByRegionCode(parentRegionCode);

        if (parentRegion == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 행정구역 코드입니다.");
        }

        if (parentRegion.getRegionLevel() != RegionLevel.SIDO) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "parentRegionCode는 시도 코드여야 합니다.");
        }
    }

    private void validateJeonsePriceData(List<JeonsePriceRegionVO> regions, YearMonth baseMonth) {
        if (regions.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND, "조회 가능한 행정구역이 없습니다."
            );
        }

        boolean allPriceDataMissing = regions.stream()
                        .allMatch(
                                region ->
                                        region.getPriceIndex()
                                                == null
                        );

        if (allPriceDataMissing) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND, "해당 기준월의 전세가격지수 데이터가 없습니다: " + baseMonth
            );
        }
    }
}