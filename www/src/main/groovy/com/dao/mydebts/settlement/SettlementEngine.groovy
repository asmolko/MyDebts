package com.dao.mydebts.settlement

import com.dao.mydebts.entities.StoredDebt;

/**
 * Engine interface settlement detection
 *
 * @author Oleg Chernovskiy
 */
interface SettlementEngine {

    /**
     * Performs all known settlement strategies between this debt and all others in database.
     * @param debt newly approved debt to track cycles from
     */
    void relax(StoredDebt debt);

}
