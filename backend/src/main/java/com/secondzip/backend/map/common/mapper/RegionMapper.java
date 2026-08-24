package com.secondzip.backend.map.common.mapper;

import com.secondzip.backend.map.common.domain.Region;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RegionMapper {
    Region selectByRegionCode(
            @Param("regionCode") String regionCode
    );
}
