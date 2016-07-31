package com.dao.mydebts.settlement

import com.dao.mydebts.entities.StoredAuditEntry
import com.dao.mydebts.entities.StoredDebt
import com.dao.mydebts.repos.StoredAuditEntryRepo
import com.dao.mydebts.repos.StoredDebtRepo
import groovy.util.logging.Log4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller

import static com.dao.mydebts.entities.StoredAuditEntry.EventType.JOIN

/**
 * Engine implementation based on simple join of two debts with the same src and dest
 *
 * @author Alexander Smolko
 */
@Controller
@Log4j
class JoinStrategy implements SettlementStrategy {

    @Autowired
    StoredDebtRepo sdRepo;

    @Autowired
    StoredAuditEntryRepo auditEntryRepo

    @Override
    boolean relax(StoredDebt debt) {
        log.debug "Starting $debt relaxation"
        def all = sdRepo.findAllNotSettled()
        log.trace "Found $all.size non-empty debts"

        def sameDirectionDebts = all.find {
            it.id != debt.id && it.src == debt.src && it.dest == debt.dest
        }
        log.trace "Found $sameDirectionDebts"
        sameDirectionDebts.each { // for each old debt to be merged into new
            def uuid = UUID.randomUUID()

            // old amount is decreased
            auditEntryRepo.save(new StoredAuditEntry(type: JOIN, amount: -it.amount, settled: it, settleId: uuid))
            // new amount is increased
            auditEntryRepo.save(new StoredAuditEntry(type: JOIN, amount: +it.amount, settled: debt, settleId: uuid))

            debt.amount += it.amount
            it.amount = 0.0

            sdRepo.saveAndFlush debt
            sdRepo.saveAndFlush it
        }
    }
}
