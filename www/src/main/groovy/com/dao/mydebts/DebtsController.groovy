package com.dao.mydebts

import com.dao.mydebts.dto.AuditLogRequest
import com.dao.mydebts.dto.AuditLogResponse
import com.dao.mydebts.dto.DebtApprovalRequest
import com.dao.mydebts.dto.DebtCreationRequest
import com.dao.mydebts.dto.DebtDeleteRequest
import com.dao.mydebts.dto.DebtsRequest
import com.dao.mydebts.dto.DebtsResponse
import com.dao.mydebts.dto.GenericResponse
import com.dao.mydebts.entities.StoredDebt
import com.dao.mydebts.repos.StoredActorRepo
import com.dao.mydebts.repos.StoredAuditEntryRepo
import com.dao.mydebts.repos.StoredDebtRepo
import com.dao.mydebts.settlement.SettlementEngine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController

import javax.persistence.EntityNotFoundException;

/**
 * Main debt system rest controller. Manages creation/approval of debts.
 * Also serves as thin DAO layer as persist logic is fairly simple for now.
 * <p/>
 * Clients of this service are android clients, bots etc. so it can be considered as
 * server public API.
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
    private SettlementEngine settleEngine

    /**
     * Queries database for debts related to current person
     * @param request request containing user-identifying info
     * @return response containing all related debts, never null
     */
    @RequestMapping(value = "/debts", method = RequestMethod.POST)
    DebtsResponse debtsForPerson(@RequestBody DebtsRequest request) {
        def response = new DebtsResponse(me: request.me)
        response.debts = debtRepo.findByActor(request.me.id).collect { it -> it.toDto() }
        return response
    }

    /**
     * Persists received debt in database
     * @param request container with debt
     * @return response with operation status
     */
    @RequestMapping(value = "/createDebt", method = RequestMethod.POST)
    GenericResponse createDebt(@RequestBody DebtCreationRequest request) throws InvalidObjectException {
        if (!request.created) {
            throw new InvalidObjectException("Request should contain created debt!")
        }

        def entity = StoredDebt.fromDto(request.created)
        actorRepo.saveAndFlush entity.src
        actorRepo.saveAndFlush entity.dest
        StoredDebt saved = debtRepo.saveAndFlush entity
        return new GenericResponse(result: 'created', newId: saved.id)
    }

    /**
     * Approves debt and updates it in database.
     * <p/>
     * <b>Note: Triggers debts mutual settlement engine!</b>
     * @param request request containing debt- and actor-identifying info
     * @return response with operation status
     */
    @Transactional(propagation = Propagation.REQUIRED)
    @RequestMapping(value = "/approve", method = RequestMethod.POST)
    GenericResponse approveDebt(@RequestBody DebtApprovalRequest request) throws InvalidObjectException {
        if (!request.me || !request.debtIdToApprove) {
            throw new InvalidObjectException("Request should contain approver and debt id to approve!")
        }

        def debtToApprove = debtRepo.findOne request.debtIdToApprove
        // sanity checks
        if (!debtToApprove) {
            throw new EntityNotFoundException("Cannot find requested debt!")
        }

        if (request.me.id == debtToApprove.dest.id && !debtToApprove.approvedByDest) {
            debtToApprove.approvedByDest = true
            def stored = debtRepo.saveAndFlush debtToApprove
            settleEngine.relax stored
            return new GenericResponse(result: 'approved by dest')
        } else if (request.me.id == debtToApprove.src.id && !debtToApprove.approvedBySrc) {
            debtToApprove.approvedBySrc = true
            def stored = debtRepo.saveAndFlush debtToApprove
            settleEngine.relax stored
            return new GenericResponse(result: 'approved by src')
        } else {
            throw new InvalidObjectException("Can only approve unfinished debts you relate to!")
        }
    }

    /**
     * Deletes debt from database. Only non-approved debts can be deleted, and only by person
     * that approved them on creation.
     * @param request request containing debt- and actor-identifying info
     * @return response with operation status
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    GenericResponse deleteDebt(@RequestBody DebtDeleteRequest request) throws InvalidObjectException {
        if (!request.me || !request.debtIdToDelete) {
            throw new InvalidObjectException("Request should contain requestor and debt id to delete!")
        }

        def debtToDelete = debtRepo.findOne request.debtIdToDelete
        // sanity checks
        if (!debtToDelete) {
            throw new EntityNotFoundException("Cannot find debt requested!")
        }

        if (debtToDelete.approvedByDest && debtToDelete.approvedBySrc) {
            throw new InvalidObjectException("Cannot delete approved debt!")
        }

        // only concerned person can delete debt
        if (debtToDelete.src.id != request.me.id && debtToDelete.dest.id != request.me.id) {
            throw new InvalidObjectException("You are not related to this debt!")
        }

        debtRepo.delete debtToDelete
        return new GenericResponse(result: 'deleted')
    }
}
