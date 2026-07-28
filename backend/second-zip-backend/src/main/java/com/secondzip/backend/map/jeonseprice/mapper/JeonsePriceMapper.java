package com.secondzip.backend.map.jeonseprice.mapper;

import com.secondzip.backend.map.jeonseprice.domain.JeonsePriceIndexVO;
import com.secondzip.backend.map.jeonseprice.domain.JeonsePriceRegionVO;
import com.secondzip.backend.map.jeonseprice.domain.SigunguRegionMappingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface JeonsePriceMapper {

    int upsertJeonsePriceIndex(JeonsePriceIndexVO jeonsePriceIndex);
    List<SigunguRegionMappingVO> selectAllSigunguRegions();
    List<JeonsePriceRegionVO> selectSidoJeonsePrices(@Param("baseMonth") LocalDate baseMonth);
    List<JeonsePriceRegionVO> selectSigunguJeonsePrices(
            @Param("baseMonth") LocalDate baseMonth,
            @Param("parentRegionCode") String parentRegionCode
    );
}