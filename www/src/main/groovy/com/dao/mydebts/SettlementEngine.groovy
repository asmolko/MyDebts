package com.dao.mydebts

import com.dao.mydebts.entities.StoredDebt;

/**
 * Engine interface for mutual settlement detection
 *
 * @author Oleg Chernovskiy
 */
interface SettlementEngine {

    /**
     * Performs debt cycles detection and resolution between this debt and all others in database.
     * @param debt newly approved debt to track cycles from
     */
    void relax(StoredDebt debt);

}
