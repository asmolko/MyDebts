package com.dao.mydebts

import com.dao.mydebts.dto.DebtApprovalRequest
import com.dao.mydebts.entities.Actor
import com.dao.mydebts.entities.StoredActor
import com.dao.mydebts.entities.StoredDebt
import com.dao.mydebts.repos.StoredActorRepo
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
class SettlementsEngingTest {
    @Autowired
    StoredDebtRepo debtRepo

    @Autowired
    StoredActorRepo actorRepo

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

        assert d1.settled
        assert d2.settled
        assert d3.settled

        assert d1.amount == 0.0
        assert d2.amount == 10.0
        assert d3.amount == 20.0

    }
}
