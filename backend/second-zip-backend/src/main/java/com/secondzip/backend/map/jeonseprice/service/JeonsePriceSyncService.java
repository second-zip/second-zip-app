package com.secondzip.backend.map.jeonseprice.service;

import java.time.YearMonth;

public interface JeonsePriceSyncService {

    int sync(YearMonth targetMonth);
}