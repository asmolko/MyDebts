package com.dao.mydebts

import com.dao.mydebts.dto.AuditLogRequest
import com.dao.mydebts.dto.AuditLogResponse
import com.dao.mydebts.repos.StoredAuditEntryRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for all audit-related actions
 * @author Oleg Chernovskiy
 */
@RestController
@RequestMapping(value = "audit",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
class AuditController {

    @Autowired
    private StoredAuditEntryRepo auditRepo

    /**
     * Get audit entries for debt, e.g. only these where this debt amount was changed
     * @param request request containing debt id to search for
     * @return response containing list of all entries
     */
    @RequestMapping(value = "/forDebt", method = RequestMethod.POST)
    AuditLogResponse auditForDebt(@RequestBody AuditLogRequest request) {
        AuditLogResponse response = new AuditLogResponse(me: request.me)
        response.entries = auditRepo.findByDebtId(request.debtId).collect { it.toDto() }
        return response
    }

    /**
     * Get audit for user. Returns all audit entries associated with debts related to this user (as debtor or creditor)
     * @param request request containing actor to search logs for
     * @return response containing list of all entries
     */
    @RequestMapping(value = "/forUser", method = RequestMethod.POST)
    AuditLogResponse auditForUser(@RequestBody AuditLogRequest request) {
        AuditLogResponse response = new AuditLogResponse(me: request.me)
        response.entries = auditRepo.findByUser(request.me.id).collect { it.toDto() }
        return response
    }

    /**
     * Get audit for group. Returns all audit entries participated in this group creation.
     * @param request
     * @return
     */
    @RequestMapping(value = "/forGroup", method = RequestMethod.POST)
    AuditLogResponse auditForGroup(@RequestBody AuditLogRequest request) {
        AuditLogResponse response = new AuditLogResponse(me: request.me)
        response.entries = auditRepo.findByGroup(UUID.fromString(request.settleId)).collect { it.toDto() }
        return response
    }

}
