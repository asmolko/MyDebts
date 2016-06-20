package com.dao.mydebts

import com.dao.mydebts.dto.DebtApprovalRequest
import com.dao.mydebts.dto.DebtCreationRequest
import com.dao.mydebts.dto.DebtDeleteRequest;
import com.dao.mydebts.dto.DebtsRequest;
import com.dao.mydebts.entities.Actor
import com.dao.mydebts.entities.Debt
import com.dao.mydebts.entities.StoredActor
import com.dao.mydebts.entities.StoredDebt
import com.dao.mydebts.repos.StoredActorRepo
import com.dao.mydebts.repos.StoredDebtRepo
import org.h2.mvstore.db.MVTableEngine
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.boot.test.WebIntegrationTest
import org.springframework.http.MediaType
import org.springframework.http.converter.json.GsonHttpMessageConverter
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext

import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for {@link DebtsController}
 *
 * @author Oleg Chernovskiy
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest(["spring.datasource.url=jdbc:h2:mem:spec;DB_CLOSE_DELAY=0;MVCC=TRUE;LOCK_MODE=1"]) // in-mem db
@Transactional
@Rollback // rollback on every test
class DebtsControllerTest {

    @Autowired
    WebApplicationContext webApplicationContext

    @Autowired
    GsonHttpMessageConverter gson

    @Autowired
    StoredDebtRepo debtRepo

    @Autowired
    StoredActorRepo actorRepo

    MockMvc mock

    Collection<StoredDebt> initialDebts

    /**
     * IDs will change on every transaction nevertheless, take care!
     */
    @Before
    void setUp() {
        mock = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

        // actors
        def actor1 = new StoredActor(id: '100500')
        def actor2 = new StoredActor(id: '100501')
        actorRepo.save([actor1, actor2])

        StoredDebt debt1 = new StoredDebt(src: actor1,
                                         dest: actor2,
                                         amount: 500,
                                         created: new Date(),
                                         approvedBySrc: true,
                                         approvedByDest: true)

        StoredDebt debt2 = new StoredDebt(src: actor2,
                                         dest: actor1,
                                         amount: 1500,
                                         created: new Date(),
                                         approvedBySrc: true,
                                         approvedByDest: false)

        StoredDebt debt3 = new StoredDebt(src: actor2,
                                         dest: actor1,
                                         amount: 1000,
                                         created: new Date(),
                                         approvedBySrc: false,
                                         approvedByDest: true)
        initialDebts = [debt1, debt2, debt3]
        debtRepo.saveAndFlush debt1
        debtRepo.saveAndFlush debt2
        debtRepo.saveAndFlush debt3
    }

    // positive tests

    @Test
    void 'test debt list retrieval for person'() {
        def body = gson.gson.toJson new DebtsRequest(me : new Actor('100500'))

        mock.perform(post('/debt/debts').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.me.id', is('100500')))
            .andExpect(jsonPath('$.debts', hasSize(3)))
            .andExpect(jsonPath('$.debts[0].amount', is(500)))
            .andExpect(jsonPath('$.debts[0].src.id', is('100500')))
            .andExpect(jsonPath('$.debts[0].dest.id', is('100501')))
            .andExpect(jsonPath('$.debts[0].approvedBySrc', is(true)))
            .andExpect(jsonPath('$.debts[0].approvedByDest', is(true)))
            .andExpect(jsonPath('$.debts[1].amount', is(1500)))
            .andExpect(jsonPath('$.debts[1].src.id', is('100501')))
            .andExpect(jsonPath('$.debts[1].dest.id', is('100500')))
            .andExpect(jsonPath('$.debts[1].approvedBySrc', is(true)))
            .andExpect(jsonPath('$.debts[1].approvedByDest', is(false)))
            .andExpect(jsonPath('$.debts[2].amount', is(1000)))
            .andExpect(jsonPath('$.debts[2].src.id', is('100501')))
            .andExpect(jsonPath('$.debts[2].dest.id', is('100500')))
            .andExpect(jsonPath('$.debts[2].approvedBySrc', is(false)))
            .andExpect(jsonPath('$.debts[2].approvedByDest', is(true)))
    }

    @Test
    void 'test debt creation'() {
        def storedBefore = debtRepo.findByActor '100'
        assert storedBefore.size() == 0

        def src = new Actor(id: '100')
        def dest = new Actor(id: '101')
        def toCreate = new Debt(src: src, dest: dest, created: new Date(), amount: 300, approvedBySrc: true)
        def body = gson.gson.toJson new DebtCreationRequest(created : toCreate)

        mock.perform(post('/debt/createDebt').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('created')))

        def storedAfter = debtRepo.findByActor '100'
        assert storedAfter.size() == 1
        assert storedAfter[0].src.id == '100'
        assert storedAfter[0].dest.id == '101'
        //noinspection GrDeprecatedAPIUsage - lint is actually wrong here, we compare objects, not coerced doubles!
        assert storedAfter[0].amount == BigDecimal.valueOf(300)
    }

    @Test
    void 'test debt removal'() {
        def storedBefore = debtRepo.findByActor '100500'
        assert storedBefore.size() == initialDebts.size()

        def me = new Actor(id: '100500')
        def body = gson.gson.toJson new DebtDeleteRequest(me: me, debtIdToDelete: storedBefore[1].id)
        mock.perform(post('/debt/delete').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('deleted')))

        def storedAfter = debtRepo.findByActor '100500'
        assert storedAfter.size() == 2
    }

    @Test
    void 'test debt approval by dest'() {
        def storedBefore = debtRepo.findByActor '100500'
        assert storedBefore.size() == initialDebts.size()
        assert !storedBefore[1].approvedByDest

        def me = new Actor(id: '100500')
        def body = gson.gson.toJson new DebtApprovalRequest(me: me, debtIdToApprove: storedBefore[1].id)
        mock.perform(post('/debt/approve').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('approved by dest')))

        def storedAfter = debtRepo.findByActor '100500'
        assert storedAfter.size() == initialDebts.size()
        assert storedBefore[1].approvedByDest
    }

    @Test
    void 'test debt approval by src'() {
        def storedBefore = debtRepo.findByActor '100500'
        assert storedBefore.size() == initialDebts.size()
        assert !storedBefore[2].approvedBySrc

        def me = new Actor(id: '100501')
        def body = gson.gson.toJson new DebtApprovalRequest(me: me, debtIdToApprove: storedBefore[2].id)
        mock.perform(post('/debt/approve').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('approved by src')))

        def storedAfter = debtRepo.findByActor '100500'
        assert storedAfter.size() == initialDebts.size()
        assert storedBefore[2].approvedByDest
    }

    // negative tests

    @Test
    void 'test no debts returned'() {
        def body = gson.gson.toJson new DebtsRequest(me: new Actor('100502'))
        mock.perform(post('/debt/debts').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.me.id', is('100502')))
            .andExpect(jsonPath('$.debts', hasSize(0)))
    }

    @Test
    void 'test create null Debt'() {
        def body = gson.gson.toJson new DebtCreationRequest(created: null)
        mock.perform(post('/debt/createDebt').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('not created')))
    }

    @Test
    void 'test approve invalid request without `me`' () {
        def storedBefore = debtRepo.findByActor '100500'
        assert storedBefore.size() == initialDebts.size()
        assert !storedBefore[1].approvedByDest

        def body = gson.gson.toJson new DebtApprovalRequest(me: null, debtIdToApprove: storedBefore[1].id)
        mock.perform(post('/debt/approve').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('invalid request')))
    }

    @Test
    void 'test approve invalid request without `debtIdToApprove`' () {
        def storedBefore = debtRepo.findByActor '100500'
        assert storedBefore.size() == initialDebts.size()
        assert !storedBefore[1].approvedByDest

        def me = new Actor(id: '100500')
        def body = gson.gson.toJson new DebtApprovalRequest(me: me, debtIdToApprove: null)
        mock.perform(post('/debt/approve').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('invalid request')))
    }

    @Test
    void 'test approve non existent debt' () {
        def me = new Actor(id: '100500')
        def body = gson.gson.toJson new DebtApprovalRequest(me: me, debtIdToApprove: '9001')
        mock.perform(post('/debt/approve').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('not found')))
    }

    @Test
    void 'test approval by src'() {
        def storedBefore = debtRepo.findByActor '100500'
        assert storedBefore.size() == initialDebts.size()
        assert !storedBefore[1].approvedByDest

        def me = new Actor(id: '100501')
        def body = gson.gson.toJson new DebtApprovalRequest(me: me, debtIdToApprove: storedBefore[1].id)
        mock.perform(post('/debt/approve').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('not approved')))
    }

    @Test
    void 'test delete invalid request without `me`'() {
        def body = gson.gson.toJson new DebtDeleteRequest(me: null, debtIdToDelete: '2')
        mock.perform(post('/debt/delete').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('invalid request')))
    }

    @Test
    void 'test delete invalid request without `debtIdToDelete`'() {
        def me = new Actor(id: '100501')
        def body = gson.gson.toJson new DebtDeleteRequest(me: me, debtIdToDelete: null)
        mock.perform(post('/debt/delete').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('invalid request')))
    }

    @Test
    void 'test delete non existent debt'() {
        def me = new Actor(id: '100501')
        def body = gson.gson.toJson new DebtDeleteRequest(me: me, debtIdToDelete: '9001')
        mock.perform(post('/debt/delete').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('not found')))
    }

    @Test
    void 'test delete approved debt'() {
        def me = new Actor(id: '100501')
        def storedBefore = debtRepo.findByActor '100500'
        assert storedBefore.size() == initialDebts.size()
        assert storedBefore[0].approvedByDest && storedBefore[0].approvedBySrc

        def body = gson.gson.toJson new DebtDeleteRequest(me: me, debtIdToDelete: storedBefore[0].id)
        mock.perform(post('/debt/delete').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('not deleted')))
    }

    @Test
    void 'test delete debt by unrelated person'() {
        def storedBefore = debtRepo.findByActor '100500'
        assert storedBefore.size() == initialDebts.size()
        assert !(storedBefore[1].approvedByDest && storedBefore[1].approvedBySrc)

        def me = new Actor(id: '9000')
        def body = gson.gson.toJson new DebtDeleteRequest(me: me, debtIdToDelete: storedBefore[1].id)
        mock.perform(post('/debt/delete').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('not deleted')))
        assert debtRepo.findByActor('100500').size() == initialDebts.size()
    }
}
