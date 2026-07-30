package com.secondzip.backend.map.jeonseprice.scheduler;

import com.secondzip.backend.map.jeonseprice.service.JeonsePriceSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class JeonsePriceScheduler {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private final JeonsePriceSyncService jeonsePriceSyncService;

    /**
     * 매월 16일부터 20일까지 매일 새벽 3시에 실행
     *
     * 이미 지난달 데이터가 동기화되어 있으면 건너뛰고,
     * 아직 동기화되지 않았다면 한국부동산원 데이터를 조회하여 저장
     */
    @Scheduled(
            cron = "${JEONSE_PRICE_SYNC_CRON:0 0 3 16-20 * *}",
            zone = "Asia/Seoul"
    )
    public void syncPreviousMonth(){
        YearMonth targetMonth = YearMonth.now(SEOUL_ZONE).minusMonths(1);

        if (jeonsePriceSyncService.isAlreadySynced(targetMonth)){
            log.info(
                    "전세가격지수 동기화를 생략합니다. "
                            + "이미 저장된 기준월입니다. "
                            + "targetMonth={}",
                    targetMonth
            );
            return;
        }

        try {
            log.info(
                    "전세가격지수 자동 동기화를 시작합니다. "
                            + "targetMonth={}",
                    targetMonth
            );

            int savedCount = jeonsePriceSyncService.sync(targetMonth);

            log.info(
                    "전세가격지수 자동 동기화가 완료되었습니다. "
                            + "targetMonth={}, savedCount={}",
                    targetMonth,
                    savedCount
            );


        } catch (Exception exception){
            log.error(
                    "전세가격지수 자동 동기화에 실패했습니다. "
                            + "targetMonth={}",
                    targetMonth,
                    exception
            );
        }


    }
}
