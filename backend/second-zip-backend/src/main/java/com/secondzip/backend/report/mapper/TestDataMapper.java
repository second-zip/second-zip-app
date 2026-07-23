package com.secondzip.backend.report.mapper;

import com.secondzip.backend.report.dto.BuildingData;
import com.secondzip.backend.report.dto.PriceData;
import com.secondzip.backend.report.dto.RegistryData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TestDataMapper {
    RegistryData findRegistryByAddress(@Param("roadAddress") String roadAddress);
    BuildingData findBuildingByAddress(@Param("roadAddress") String roadAddress);
    PriceData findPriceByAddress(@Param("roadAddress") String roadAddress);
}