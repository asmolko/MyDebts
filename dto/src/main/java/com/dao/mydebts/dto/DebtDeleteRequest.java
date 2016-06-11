package com.dao.mydebts.dto;

import com.dao.mydebts.entities.Actor;

/**
 * Sends deletion request for known debt. The debt must <b>not</b> be approved by both parties.
 *
 * @// TODO: For now we trust a local client based on this answer.
 * @// TODO: The actual auth sequence is TBD later
 *
 * @author Oleg Chernovskiy on 04.04.16.
 */
public class DebtDeleteRequest {

    private Actor me;

    private String debtIdToDelete;

    public Actor getMe() {
        return me;
    }

    public void setMe(Actor me) {
        this.me = me;
    }

    public String getDebtIdToDelete() {
        return debtIdToDelete;
    }

    public void setDebtIdToDelete(String debtIdToDelete) {
        this.debtIdToDelete = debtIdToDelete;
    }
}
