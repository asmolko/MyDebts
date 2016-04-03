package com.dao.mydebts.dto;

import com.dao.mydebts.entities.Person;

/**
 * Sends approval request for known debt. The debt must have
 * approved either as debtor or creditor.
 * There is no corresponding response DTO, HTTP 200 OK answer is
 * sufficient from server.
 *
 * @// TODO: For now we trust a local client based on this answer.
 * @// TODO: The actual auth sequence is TBD later
 *
 * @author Oleg Chernovskiy on 04.04.16.
 */
public class DebtApprovalRequest {

    private Person me;

    private String debtIdToApprove;

    public Person getMe() {
        return me;
    }

    public void setMe(Person me) {
        this.me = me;
    }

    public String getDebtIdToApprove() {
        return debtIdToApprove;
    }

    public void setDebtIdToApprove(String debtIdToApprove) {
        this.debtIdToApprove = debtIdToApprove;
    }
}
