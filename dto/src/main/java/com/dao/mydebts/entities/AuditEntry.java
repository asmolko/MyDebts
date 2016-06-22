package com.dao.mydebts.entities;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author demoth
 */
public class AuditEntry {
    private Date created;

    private BigDecimal amount;

    private Debt debt;

    public Debt getDebt() {
        return debt;
    }

    public void setDebt(Debt debt) {
        this.debt = debt;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
