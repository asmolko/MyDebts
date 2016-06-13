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
        debtRepo.saveAndFlush debt1
        debtRepo.saveAndFlush debt2
    }

    // positive tests

    @Test
    void 'test debt list retrieval for person'() {
        def body = gson.gson.toJson new DebtsRequest(me : new Actor('100500'))

        mock.perform(post('/debt/debts').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.me.id', is('100500')))
            .andExpect(jsonPath('$.debts', hasSize(2)))
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
    }

    @Test
    void 'test debt creation'() {
        def storedBefore = debtRepo.findByActor '100'
        Assert.assertEquals storedBefore.size(), 0

        def src = new Actor(id: '100')
        def dest = new Actor(id: '101')
        def toCreate = new Debt(src: src, dest: dest, created: new Date(), amount: 300, approvedBySrc: true)
        def body = gson.gson.toJson new DebtCreationRequest(created : toCreate)

        mock.perform(post('/debt/createDebt').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('created')))

        def storedAfter = debtRepo.findByActor '100'
        Assert.assertEquals storedAfter.size(), 1
        Assert.assertEquals storedAfter[0].src.id, '100'
        Assert.assertEquals storedAfter[0].dest.id, '101'
        //noinspection GrDeprecatedAPIUsage - lint is actually wrong here, we compare objects, not coerced doubles!
        Assert.assertEquals storedAfter[0].amount, BigDecimal.valueOf(300)
    }

    @Test
    void 'test debt removal'() {
        def storedBefore = debtRepo.findByActor '100500'
        Assert.assertEquals storedBefore.size(), 2

        def me = new Actor(id: '100500')
        def body = gson.gson.toJson new DebtDeleteRequest(me: me, debtIdToDelete: '2')
        mock.perform(post('/debt/delete').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('deleted')))

        def storedAfter = debtRepo.findByActor '100500'
        Assert.assertEquals storedAfter.size(), 1
    }

    @Test
    void 'test debt approval'() {
        def storedBefore = debtRepo.findByActor '100500'
        Assert.assertEquals storedBefore.size(), 2
        Assert.assertEquals storedBefore[1].approvedByDest, false

        def me = new Actor(id: '100500')
        def body = gson.gson.toJson new DebtApprovalRequest(me: me, debtIdToApprove: storedBefore[1].id)
        mock.perform(post('/debt/approve').content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.result', is('approved')))

        def storedAfter = debtRepo.findByActor '100500'
        Assert.assertEquals storedAfter.size(), 2
        Assert.assertEquals storedBefore[1].approvedByDest, true
    }

    // TODO: negative tests

}
