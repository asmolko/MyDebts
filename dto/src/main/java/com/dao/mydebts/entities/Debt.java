package com.dao.mydebts.entities;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Identifies debt from one person to another.
 *
 * @author Oleg Chernovskiy on 23.03.16.
 */
public class Debt {

    private String id;

    private Actor src;

    private Actor dest;

    private BigDecimal amount;

    private Date created = new Date();

    private boolean approvedBySrc = true;

    private boolean approvedByDest = false;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Actor getSrc() {
        return src;
    }

    public void setSrc(Actor src) {
        this.src = src;
    }

    public Actor getDest() {
        return dest;
    }

    public void setDest(Actor dest) {
        this.dest = dest;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public boolean isApprovedBySrc() {
        return approvedBySrc;
    }

    public void setApprovedBySrc(boolean approvedBySrc) {
        this.approvedBySrc = approvedBySrc;
    }

    public boolean isApprovedByDest() {
        return approvedByDest;
    }

    public void setApprovedByDest(boolean approvedByDest) {
        this.approvedByDest = approvedByDest;
    }
}
