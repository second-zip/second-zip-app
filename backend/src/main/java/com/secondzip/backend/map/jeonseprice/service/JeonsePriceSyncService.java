package com.secondzip.backend.map.jeonseprice.service;

import java.time.YearMonth;

public interface JeonsePriceSyncService {

    int sync(YearMonth targetMonth); //REB API 호출 후 DB 저장
    boolean isAlreadySynced(YearMonth targetMonth); //해당 월이 이미 저장됐는지 DB에서 확인
}