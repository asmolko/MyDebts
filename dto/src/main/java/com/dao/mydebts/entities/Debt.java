package com.dao.mydebts.entities;

import java.math.BigDecimal;
import java.util.Calendar;

/**
 * Identifies debt from one person to another.
 *
 * @author Oleg Chernovskiy on 23.03.16.
 */
public class Debt {

    public static final int APPROVED_BY_CREDITOR  = 0x1;
    public static final int APPROVED_BY_DEBTOR    = 0x2;

    private String id;

    private Actor from;

    private Actor to;

    private BigDecimal amount;

    private Calendar created;

    private int approvalFlags = APPROVED_BY_CREDITOR;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Actor getFrom() {
        return from;
    }

    public void setFrom(Actor from) {
        this.from = from;
    }

    public Actor getTo() {
        return to;
    }

    public void setTo(Actor to) {
        this.to = to;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Calendar getCreated() {
        return created;
    }

    public void setCreated(Calendar created) {
        this.created = created;
    }

    public int getApprovalFlags() {
        return approvalFlags;
    }

    public void setApprovalFlags(int approvalFlags) {
        this.approvalFlags = approvalFlags;
    }
}
