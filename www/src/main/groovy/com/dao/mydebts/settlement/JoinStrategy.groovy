package com.dao.mydebts.settlement

import com.dao.mydebts.entities.StoredAuditEntry
import com.dao.mydebts.entities.StoredDebt
import com.dao.mydebts.repos.StoredAuditEntryRepo
import com.dao.mydebts.repos.StoredDebtRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller

/**
 * Engine implementation based on simple join of two debts with the same src and dest
 *
 * @author Alexander Smolko
 */
@Controller
class JoinStrategy implements SettlementStrategy {

    @Autowired
    StoredDebtRepo sdRepo;

    @Autowired
    StoredAuditEntryRepo auditEntryRepo

    @Override
    boolean relax(StoredDebt debt) {
        def all = sdRepo.findAllNotSettled()
        all.find { it.id != debt.id && it.src == debt.src && it.dest == debt.dest }.each {
            def uuid = UUID.randomUUID()

            auditEntryRepo.save(new StoredAuditEntry(amount: +it.amount,
                    created: new Date(), settled: it, settleId: uuid))
            auditEntryRepo.save(new StoredAuditEntry(amount: -it.amount,
                    created: new Date(), settled: debt, settleId: uuid))

            debt.amount += it.amount
            it.amount = 0.0

            sdRepo.saveAndFlush debt
            sdRepo.saveAndFlush it
        }
    }
}
