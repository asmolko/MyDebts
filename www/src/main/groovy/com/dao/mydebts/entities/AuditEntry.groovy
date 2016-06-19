package com.dao.mydebts.entities;

import com.dao.mydebts.SettlementEngine
import groovy.transform.Canonical

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

/**
 * Tracks debts settlements by {@link SettlementEngine}
 *
 * @author Oleg Chernovskiy
 */
@Entity
@Canonical
class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    String id

    /**
     * Settlement unique identifier for grouping
     */
    @Column(nullable = false)
    UUID settleId

    @Column(nullable = false)
    Date created = new Date()

    /**
     * Amount by which debt has changed
     */
    @Column(nullable = false)
    BigDecimal amount

    /**
     * Debt that was relaxed
     */
    @ManyToOne
    StoredDebt settled
}