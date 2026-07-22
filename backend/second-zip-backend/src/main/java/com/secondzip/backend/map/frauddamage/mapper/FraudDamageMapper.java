package com.secondzip.backend.map.frauddamage.mapper;

import com.secondzip.backend.map.frauddamage.domain.FraudDamageRegionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface FraudDamageMapper {
    LocalDate selectLatestBaseDate(); //피해주택 통계 가장 최신 기준일 조회
    //시도별 피해주택 수 조회
    List<FraudDamageRegionVO> selectSidoFraudDamages(
            @Param("baseDate") LocalDate baseDate
    );
    //시군구 피해 주택 수 조회
    List<FraudDamageRegionVO> selectSigunguFraudDamages(
            @Param("baseDate") LocalDate baseDate,
            @Param("parentRegionCode") String parentRegionCode
    );

}
