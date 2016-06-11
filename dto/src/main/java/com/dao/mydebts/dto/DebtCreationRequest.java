package com.dao.mydebts.dto;

import com.dao.mydebts.entities.Debt;

/**
 * This request is sent when a person creates a debt locally.
 *
 * @author Oleg Chernovskiy on 29.03.16.
 */
public class DebtCreationRequest {

    private Debt created;

    public Debt getCreated() {
        return created;
    }

    public void setCreated(Debt created) {
        this.created = created;
    }
}
