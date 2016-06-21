package com.dao.mydebts.settlement

import com.dao.mydebts.entities.StoredDebt;

/**
 * Engine interface for mutual settlement detection
 *
 * @author Oleg Chernovskiy
 */
interface SettlementStrategy {

    /**
     * Performs one settlement detection and resolution between this debt and all others in database
     * @param debt newly approved debt to track cycles from
     */
    boolean relax(StoredDebt debt);

}
