package com.dao.mydebts.entities;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.math.BigDecimal;

/**
 * Identifies debt from one person to another in specific group matrix.
 * 
 * Created by Oleg Chernovskiy on 23.03.16.
 */
@XStreamAlias("debt")
public class DebtInfo {
    
    private Person from;
    
    private Person to;
    
    private BigDecimal amount;

    public Person getFrom() {
        return from;
    }

    public void setFrom(Person from) {
        this.from = from;
    }

    public Person getTo() {
        return to;
    }

    public void setTo(Person to) {
        this.to = to;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
