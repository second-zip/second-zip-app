package com.secondzip.backend.map.frauddamage.service;

import com.secondzip.backend.map.common.domain.Region;
import com.secondzip.backend.map.common.enums.RegionLevel;
import com.secondzip.backend.map.common.mapper.RegionMapper;
import com.secondzip.backend.map.frauddamage.domain.FraudDamageRegion;
import com.secondzip.backend.map.frauddamage.dto.response.FraudDamageMapResponse;
import com.secondzip.backend.map.frauddamage.mapper.FraudDamageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FraudDamageServiceImpl implements FraudDamageService {

    private final RegionMapper regionMapper;
    private final FraudDamageMapper fraudDamageMapper;

    @Override
    public FraudDamageMapResponse getFraudDamages(
            RegionLevel regionLevel,
            String parentRegionCode
    ) {
        validateRegionLevel(regionLevel); //지역 단계 검증
        LocalDate latestBaseDate = getLatestBaseDate(); //최신 통계 기준일 조회

        if (regionLevel == RegionLevel.SIDO){
            return getSidoFraudDamages(latestBaseDate);
        }

        return getSigunguFraudDamages(
                latestBaseDate,
                parentRegionCode
        );
    }

    //'시도'별 피해 현황 조회
    private FraudDamageMapResponse getSidoFraudDamages(LocalDate baseDate){
        List<FraudDamageRegion> fraudDamages = fraudDamageMapper.selectSidoFraudDamages(baseDate);
        long totalDamageHouseCount = calculateTotalDamageHouseCount(fraudDamages);

        return FraudDamageMapResponse.of(
                RegionLevel.SIDO,
                null,
                null,
                baseDate,
                totalDamageHouseCount,
                fraudDamages
        );
    }

    //'시군구' 피해 현황 조회
    private FraudDamageMapResponse getSigunguFraudDamages(
            LocalDate baseDate,
            String parentRegionCode
    ){
        validateParentRegionCode(parentRegionCode);
        Region parentRegion = getSidoRegion(parentRegionCode);

        List<FraudDamageRegion> fraudDamages = fraudDamageMapper.selectSigunguFraudDamages(
                baseDate, parentRegionCode
        );

        long totalDamageHouseCount = calculateTotalDamageHouseCount(fraudDamages);

        return FraudDamageMapResponse.of(
                RegionLevel.SIGUNGU,
                parentRegion.getRegionCode(),
                parentRegion.getRegionName(),
                baseDate,
                totalDamageHouseCount,
                fraudDamages
        );
    }

    //피해현황 데이터의 가장 최신 기준일 조회
    private LocalDate getLatestBaseDate() {
        LocalDate latestBaseDate = fraudDamageMapper.selectLatestBaseDate();

        if (latestBaseDate == null) {
            throw new IllegalStateException(
                    "전세사기 피해현황 데이터가 존재하지 않습니다."
            );
        }

        return latestBaseDate;
    }

    //상위 지역 코드에 해당하는 시도 조회
    private Region getSidoRegion(String parentRegionCode) {
        Region region = regionMapper.selectByRegionCode(parentRegionCode);

        if (region == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 행정구역 코드입니다."
            );
        }

        if (region.getRegionLevel() != RegionLevel.SIDO) {
            throw new IllegalArgumentException(
                    "상위 행정구역 코드는 시도 코드여야 합니다."
            );
        }

        return region;
    }

    //조회 범위 내 피해주택 수 합계 계산
    private long calculateTotalDamageHouseCount(List<FraudDamageRegion> fraudDamages) {
        return fraudDamages.stream()
                .map(FraudDamageRegion::getDamageHouseCount)
                .filter(count -> count != null)
                .mapToLong(Long::longValue)
                .sum();
    }

    //행정구역 단계 검증
    private void validateRegionLevel(RegionLevel regionLevel) {
        if (regionLevel == null) {
            throw new IllegalArgumentException(
                    "행정구역 단계를 입력해 주세요."
            );
        }
    }

    //시군구 조회 시 상위 시도 코드 검증
    private void validateParentRegionCode(String parentRegionCode) {
        if (!StringUtils.hasText(parentRegionCode)) {
            throw new IllegalArgumentException(
                    "시군구 조회 시 상위 시도 코드가 필요합니다."
            );
        }
    }
}
