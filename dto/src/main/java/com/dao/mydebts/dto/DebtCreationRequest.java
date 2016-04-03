package com.dao.mydebts.dto;

import com.dao.mydebts.entities.Debt;

/**
 * This request is sent when a person creates a debt locally.
 * There is no corresponding response DTO, HTTP 200 OK answer is
 * sufficient from server.
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
