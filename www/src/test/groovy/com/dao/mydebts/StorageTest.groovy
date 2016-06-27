package com.dao.mydebts

import com.dao.mydebts.entities.StoredActor
import com.dao.mydebts.entities.StoredDebt
import com.dao.mydebts.repos.StoredActorRepo
import com.dao.mydebts.repos.StoredDebtRepo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.IntegrationTest
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@IntegrationTest(["spring.datasource.url=jdbc:h2:mem:spec;DB_CLOSE_DELAY=0;MVCC=TRUE;LOCK_MODE=1"])
@Transactional
@Rollback
class StorageTest {

    @Autowired
    StoredDebtRepo debtRepo

    @Autowired
    StoredActorRepo actorRepo

    Collection<StoredDebt> initialDebts

    @Before
    void setup() {
        // actors
        def actor1 = new StoredActor(id: '1')
        def actor2 = new StoredActor(id: '2')
        actorRepo.save([actor1, actor2])

        // debts
        def debt1 = new StoredDebt(src: actor1,
                dest: actor2,
                amount: 500,
                created: new Date(),
                approvedBySrc: true,
                approvedByDest: true)

        def debt2 = new StoredDebt(src: actor2,
                dest: actor1,
                amount: 1500,
                created: new Date(),
                approvedBySrc: true,
                approvedByDest: false)

        def debt3 = new StoredDebt(src: actor2,
                dest: actor1,
                amount: 0.0,
                created: new Date(),
                approvedBySrc: false,
                approvedByDest: true)

        def debt4 = new StoredDebt(src: actor1,
                dest: actor2,
                amount: 0,
                created: new Date(),
                approvedBySrc: false,
                approvedByDest: true)

        def debt5 = new StoredDebt(src: actor2,
                dest: actor1,
                amount: 1000,
                created: new Date(),
                approvedBySrc: false,
                approvedByDest: true)

        initialDebts = [debt1, debt2, debt3, debt4, debt5]
        debtRepo.save(initialDebts)
    }

    @Test
    void 'test repo query for non settled debts does not return zero values'() {
        def debtsFor1 = debtRepo.findByActor('1')
        def debtsFor2 = debtRepo.findByActor('2')
        [debtsFor1, debtsFor2].flatten().each {
            assert it.amount > 0
        }
    }
}
