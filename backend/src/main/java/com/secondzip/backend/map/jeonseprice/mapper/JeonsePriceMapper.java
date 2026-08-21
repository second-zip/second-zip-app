package com.secondzip.backend.map.jeonseprice.mapper;

import com.secondzip.backend.map.jeonseprice.domain.JeonsePriceIndex;
import com.secondzip.backend.map.jeonseprice.domain.JeonsePriceRegion;
import com.secondzip.backend.map.jeonseprice.domain.SigunguRegionMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface JeonsePriceMapper {

    int upsertJeonsePriceIndex(JeonsePriceIndex jeonsePriceIndex);
    List<SigunguRegionMapping> selectAllSigunguRegions();
    LocalDate selectLatestBaseMonth();
    List<JeonsePriceRegion> selectSidoJeonsePrices(@Param("baseMonth") LocalDate baseMonth);
    List<JeonsePriceRegion> selectSigunguJeonsePrices(
            @Param("baseMonth") LocalDate baseMonth,
            @Param("parentRegionCode") String parentRegionCode
    );
    int countSidoJeonsePriceIndicesByMonth(@Param("baseMonth") LocalDate baseMonth);
}