package com.dao.mydebts.settlement

import com.dao.mydebts.entities.StoredDebt
import groovy.util.logging.Log4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller

/**
 * Engine implementation based on simple DFS
 *
 * @author Oleg Chernovskiy
 */
@Controller
@Log4j
class SettlementEngineImpl implements SettlementEngine {

    @Autowired
    List<SettlementStrategy> strategies

    @Override
    void relax(StoredDebt debt) {
        log.info "Starting $debt relaxation"
        while (true) {
            def entries = strategies.collectEntries { [(it): it.relax(debt)] }

            def relaxed = entries.find { k, v -> v }
            if (!relaxed) {
                log.info "All strategies returned false => nothing more to relax"
                return
            }
            log.info "Next strategies relaxed: $relaxed.key"
        }
    }
}
