package com.dao.mydebts

import com.dao.mydebts.dto.DebtApprovalRequest
import com.dao.mydebts.dto.DebtCreationRequest
import com.dao.mydebts.dto.DebtsRequest
import com.dao.mydebts.dto.DebtsResponse
import com.dao.mydebts.dto.GenericResponse
import com.dao.mydebts.entities.StoredDebt
import com.dao.mydebts.repos.StoredActorRepo
import com.dao.mydebts.repos.StoredDebtRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
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

    @RequestMapping(value = "/approve", method = RequestMethod.POST)
    GenericResponse approveDebt(@RequestBody DebtApprovalRequest request) {
        if (!request.me || !request.debtIdToApprove) {
            return new GenericResponse(result: 'not approved')
        }

        def debtToApprove = debtRepo.findOne request.debtIdToApprove
        if(request.me.id != debtToApprove.dest.id) { // we are not a target of the debt
            return new GenericResponse(result: 'not approved')
        }

        debtToApprove.approvedByDest = true
        debtRepo.saveAndFlush debtToApprove
        return new GenericResponse(result: 'approved')
    }
}
