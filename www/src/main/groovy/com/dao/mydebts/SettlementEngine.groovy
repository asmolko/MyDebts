package com.dao.mydebts

import com.dao.mydebts.entities.StoredDebt;

/**
 * Engine interface for mutual settlement detection
 *
 * @author Oleg Chernovskiy
 */
interface SettlementEngine {

    void relax(StoredDebt debt);

}
