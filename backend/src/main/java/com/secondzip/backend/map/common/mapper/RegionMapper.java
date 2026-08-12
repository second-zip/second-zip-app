package com.secondzip.backend.map.common.mapper;

import com.secondzip.backend.map.common.domain.RegionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RegionMapper {
    RegionVO selectByRegionCode(
            @Param("regionCode") String regionCode
    );
}
