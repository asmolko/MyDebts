package com.dao.mydebts

import com.dao.mydebts.entities.StoredActor
import com.dao.mydebts.entities.StoredAuditEntry;
import com.dao.mydebts.entities.StoredDebt
import com.dao.mydebts.repos.StoredAuditEntryRepo;
import com.dao.mydebts.repos.StoredDebtRepo
import groovy.transform.AutoClone
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Engine implementation based on simple DFS
 *
 * @author Oleg Chernovskiy
 */
@Controller
class SettlementEngineImpl implements SettlementEngine {

    @Autowired
    StoredDebtRepo sdRepo

    @Autowired
    StoredAuditEntryRepo auditEntryRepo

    @Override
    void relax(StoredDebt debt) {
        def root = debt.src
        while (true) {
            def start = new Path(chain: [debt], amount: debt.amount)
            def all = sdRepo.findAllNotSettled()
            def found = findCycle root, start, all - debt
            if(!found)
                break

            // break cycle
            settle found

            if(debt.amount == 0.0) // root debt depleted
                break
        }
    }

    private void settle(Path path) {
        UUID uuid = UUID.randomUUID()
        path.chain.each {
            it.amount -= path.amount
            auditEntryRepo.save(new StoredAuditEntry(amount: -path.amount,
                    created: new Date(), settled: it, settleId: uuid))
            sdRepo.saveAndFlush it
        }
    }

    /**
     * Recursive function that tries to find cycle for the root node in the remainder list.
     * @param root root actor of the hierarchy that we should try to return to
     * @param path currently descended path
     * @param remainder debts available for search
     */
    private Path findCycle(StoredActor root, Path path, List<StoredDebt> remainder) {
        def vertex = path.chain.last.dest // last actor in path chain
        def edges = remainder.findAll { it.src == vertex } // debts of this actor
        for (def edge : edges) {
            Path descended = path.clone()
            descended.chain << edge
            descended.amount = path.amount < edge.amount ? path.amount : edge.amount
            if (edge.dest == root) // found
                return descended

            // not found, descend and iterate
            def found = findCycle root, descended, remainder - edge // recurse
            if (found)
                return found // push up
        }
        return null
    }

    /**
     * Class tracking recursion depth and minimal amount across found cycle
     */
    @AutoClone
    class Path {
        LinkedList<StoredDebt> chain
        BigDecimal amount
    }
}
