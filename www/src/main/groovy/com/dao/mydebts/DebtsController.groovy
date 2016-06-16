package com.dao.mydebts

import com.dao.mydebts.dto.DebtApprovalRequest
import com.dao.mydebts.dto.DebtCreationRequest
import com.dao.mydebts.dto.DebtDeleteRequest
import com.dao.mydebts.dto.DebtsRequest
import com.dao.mydebts.dto.DebtsResponse
import com.dao.mydebts.dto.GenericResponse
import com.dao.mydebts.entities.StoredDebt
import com.dao.mydebts.repos.StoredActorRepo
import com.dao.mydebts.repos.StoredDebtRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Main debt system rest controller. Manages creation/approval of debts.
 * Also serves as thin DAO layer as persist logic is fairly simple for now.
 *
 * @author Oleg Chernovskiy
 */
@RestController
@RequestMapping(value = "debt",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
class DebtsController {

    @Autowired
    private StoredDebtRepo debtRepo

    @Autowired
    private StoredActorRepo actorRepo

    @Autowired
    private SettlementEngine settleEngine;

    @RequestMapping(value = "/debts", method = RequestMethod.POST)
    DebtsResponse debtsForPerson(@RequestBody DebtsRequest request) {
        def response = new DebtsResponse(me: request.me)
        response.debts = debtRepo.findByActor(request.me.id).collect {it -> it.toDto()}
        return response
    }

    @RequestMapping(value = "/createDebt", method = RequestMethod.POST)
    GenericResponse createDebt(@RequestBody DebtCreationRequest request) {
        if (!request.created) {
            return new GenericResponse(result: 'not created')
        }

        def entity = StoredDebt.fromDto(request.created)
        actorRepo.saveAndFlush entity.src
        actorRepo.saveAndFlush entity.dest
        StoredDebt saved = debtRepo.saveAndFlush entity
        return new GenericResponse(result: 'created', newId: saved.id)
    }

    /**
     * Note: Triggers debts mutual settlement engine
     * @param request
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED)
    @RequestMapping(value = "/approve", method = RequestMethod.POST)
    GenericResponse approveDebt(@RequestBody DebtApprovalRequest request) {
        if (!request.me || !request.debtIdToApprove) {
            return new GenericResponse(result: 'invalid request')
        }

        def debtToApprove = debtRepo.findOne request.debtIdToApprove
        // sanity checks
        if (!debtToApprove) {
            return new GenericResponse(result: 'not found')
        }

        if (request.me.id != debtToApprove.dest.id) { // we are not a target of the debt
            return new GenericResponse(result: 'not approved')
        }

        debtToApprove.approvedByDest = true
        def stored = debtRepo.saveAndFlush debtToApprove
        settleEngine.relax stored
        return new GenericResponse(result: 'approved')
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    GenericResponse deleteDebt(@RequestBody DebtDeleteRequest request) {
        if (!request.me || !request.debtIdToDelete) {
            return new GenericResponse(result: 'invalid request')
        }

        def debtToDelete = debtRepo.findOne request.debtIdToDelete
        // sanity checks
        if (!debtToDelete) {
            return new GenericResponse(result: 'not found')
        }

        if(debtToDelete.approvedByDest && debtToDelete.approvedBySrc) {
            return new GenericResponse(result: 'not deleted')
        }

        // only concerned person can delete debt
        if(debtToDelete.src.id != request.me.id && debtToDelete.dest.id != request.me.id) {
            return new GenericResponse(result: 'not deleted')
        }

        debtRepo.delete debtToDelete
        return new GenericResponse(result: 'deleted')
    }
}
