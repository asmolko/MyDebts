package com.dao.mydebts.dto;

import com.dao.mydebts.entities.Actor;

/**
 * @author demoth
 */
public class AuditLogRequest {
    private Actor me;

    private String debtId;

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
}
