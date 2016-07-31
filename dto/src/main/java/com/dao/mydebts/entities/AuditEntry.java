package com.dao.mydebts.entities;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Audit entry representing a log record for some relaxation action. The purpose of this entity is to
 * make relaxation procedures clear to clients.
 *
 * @author demoth
 */
public class AuditEntry {

    private EventType type;

    private Date created;

    private BigDecimal originalAmount;

    private BigDecimal amount;

    private Debt settled;

    private String settleId;

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Debt getSettled() {
        return settled;
    }

    public void setSettled(Debt settled) {
        this.settled = settled;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getSettleId() {
        return settleId;
    }

    public void setSettleId(String settleId) {
        this.settleId = settleId;
    }

    enum EventType {
        CYCLE,
        JOIN,
        UNKNOWN
    }
}
