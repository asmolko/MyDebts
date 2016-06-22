package com.dao.mydebts

import com.dao.mydebts.dto.DebtApprovalRequest
import com.dao.mydebts.entities.Actor
import com.dao.mydebts.entities.StoredActor
import com.dao.mydebts.entities.StoredDebt
import com.dao.mydebts.repos.StoredActorRepo
import com.dao.mydebts.repos.StoredAuditEntryRepo
import com.dao.mydebts.repos.StoredDebtRepo
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.boot.test.WebIntegrationTest
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest(["spring.datasource.url=jdbc:h2:mem:spec;DB_CLOSE_DELAY=0;MVCC=TRUE;LOCK_MODE=1"])
@Transactional
@Rollback
class SettlementsEngineTest {

    @Autowired
    StoredDebtRepo debtRepo

    @Autowired
    StoredActorRepo actorRepo

    @Autowired
    StoredAuditEntryRepo auditEntryRepo

    @Autowired
    DebtsController controller

    @Test
    void 'test single cycle is settled'() {
        StoredActor alice = new StoredActor('alice')
        StoredActor bob = new StoredActor('bob')
        StoredActor demon = new StoredActor('demon')
        actorRepo.save([alice, bob, demon])

        StoredDebt d1 = new StoredDebt(src: alice, dest: bob, amount: 10, created: new Date(), approvedBySrc: true, approvedByDest: true)
        StoredDebt d2 = new StoredDebt(src: bob, dest: demon, amount: 20, created: new Date(), approvedBySrc: true, approvedByDest: true)
        StoredDebt d3 = new StoredDebt(src: demon, dest: alice, amount: 30, created: new Date(), approvedBySrc: true, approvedByDest: false)

        debtRepo.save([d1, d2, d3])

        controller.approveDebt(new DebtApprovalRequest(me: new Actor('alice'), debtIdToApprove: d3.id))

        assert d3.approvedByDest

        assert d1.amount == 0.0
        assert d2.amount == 10.0
        assert d3.amount == 20.0

        // check audit creation for every debt change
        // TODO move to separate tests
        [d1, d2, d3].each {
            def audit = auditEntryRepo.findByDebtId(it.id)
            assert audit.size() == 1
            assert audit[0].amount == -10.0
        }
    }

    @Test
    void 'test two cycles are settled'() {
        StoredActor a = new StoredActor('a')
        StoredActor b = new StoredActor('b')
        StoredActor c = new StoredActor('c')
        StoredActor d = new StoredActor('d') // this is Gleb
        StoredActor e = new StoredActor('e')
        StoredActor f = new StoredActor('f')
        actorRepo.save([a, b, c, d, e, f])

        // unfinished cycle 1
        StoredDebt ab = new StoredDebt(src: a, dest: b, amount: 10, created: new Date(), approvedBySrc: true, approvedByDest: true)
        StoredDebt bc = new StoredDebt(src: b, dest: c, amount: 10, created: new Date(), approvedBySrc: true, approvedByDest: true)
        StoredDebt cd = new StoredDebt(src: c, dest: d, amount: 10, created: new Date(), approvedBySrc: true, approvedByDest: true)
        StoredDebt de = new StoredDebt(src: d, dest: e, amount: 10, created: new Date(), approvedBySrc: true, approvedByDest: true)
        StoredDebt ef = new StoredDebt(src: e, dest: f, amount: 10, created: new Date(), approvedBySrc: true, approvedByDest: true)

        // unfinished cycle 2
        StoredDebt ac = new StoredDebt(src: a, dest: c, amount: 10, created: new Date(), approvedBySrc: true, approvedByDest: true)
        StoredDebt cf = new StoredDebt(src: c, dest: f, amount: 10, created: new Date(), approvedBySrc: true, approvedByDest: true)

        // finally! completes cycles 1 and 2
        StoredDebt fa = new StoredDebt(src: f, dest: a, amount: 20, created: new Date(), approvedBySrc: true, approvedByDest: false)
        debtRepo.save([ab, bc, cd, de, ef, ac, cf, fa])

        controller.approveDebt(new DebtApprovalRequest(me: new Actor('a'), debtIdToApprove: fa.id))

        assert fa.approvedByDest

        assert ab.amount == 0.0
        assert bc.amount == 0.0
        assert cd.amount == 0.0
        assert de.amount == 0.0
        assert ef.amount == 0.0
        assert ac.amount == 0.0
        assert cf.amount == 0.0
        assert fa.amount == 0.0

        // check audit creation for every debt change
        // TODO move to separate tests
        [ab, bc, cd, de, ef, ac, cf].each {
            def audit = auditEntryRepo.findByDebtId(it.id)
            assert audit.size() == 1
            assert audit[0].amount == -10.0
        }
        def audit = auditEntryRepo.findByDebtId(fa.id)
        assert audit.size() == 2
        assert audit.every { it.amount == -10.0 }
    }
}
