package com.dao.mydebts.entities;

import com.dao.mydebts.settlement.SettlementEngine
import groovy.transform.Canonical

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
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
class StoredAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    String id

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    EventType type;

    @Column(nullable = false)
    UUID settleId

    @Column(nullable = false)
    Date created = new Date()

    /**
     * When this entry was created
     */
    @Column(nullable = false)
    BigDecimal originalAmount

    /**
     * Original amount, e.g. what debt had before relaxation
     */
    @Column(nullable = false)
    BigDecimal amount

    /**
     * Amount by which debt has changed
     */
    @ManyToOne
    StoredDebt settled

    void setSettled(StoredDebt settled) {
        this.settled = settled
        this.originalAmount = settled.amount
    }

    /**
     * Debt that was relaxed
     */
    AuditEntry toDto() {
        return new AuditEntry(
                type: AuditEntry.EventType.valueOf(type.toString()),
                created: created,
                originalAmount: originalAmount,
                amount: amount,
                settleId: settleId.toString(),
                settled: settled.toDto())
    }

    static StoredAuditEntry fromDto(AuditEntry dto) {
        return new StoredAuditEntry(
                type: EventType.valueOf(dto.type.toString()),
                created: dto.created,
                originalAmount: dto.originalAmount,
                amount: dto.amount,
                settleId: UUID.fromString(dto.settleId),
                settled: StoredDebt.fromDto(dto.settled))
    }

    /**
     * Settlement unique identifier for grouping
     * This <b>must</b> be same as {@link AuditEntry.EventType}
     */
    enum EventType {
        CYCLE,
        JOIN,
        UNKNOWN
    }
}