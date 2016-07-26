package com.dao.mydebts.dto;

import com.dao.mydebts.entities.Actor;

/**
 * Request DTO class for all methods from AuditController. Fields are filled
 * @author demoth
 */
public class AuditLogRequest {

    private Actor me; // required for requests for user

    private String debtId; // required for requests for debt

    private String settleId; // required for requests for group

    public Actor getMe() {
        return me;
    }

    public void setMe(Actor me) {
        this.me = me;
    }

    public String getDebtId() {
        return debtId;
    }

    public void setDebtId(String debtId) {
        this.debtId = debtId;
    }

    public String getSettleId() {
        return settleId;
    }

    public void setSettleId(String settleId) {
        this.settleId = settleId;
    }
}
