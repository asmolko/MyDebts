package com.dao.mydebts.settlement

import com.dao.mydebts.entities.StoredDebt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller

/**
 * Engine implementation based on simple DFS
 *
 * @author Oleg Chernovskiy
 */
@Controller
class SettlementEngineImpl implements SettlementEngine {

    @Autowired
    List<SettlementStrategy> strategies

    @Override
    void relax(StoredDebt debt) {
        while (true) {
            def entries = strategies.collectEntries { [(it): it.relax(debt)] }
            if (!entries.find { k, v -> v })
                return
        }
    }
}
