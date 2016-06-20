package com.dao.mydebts.entities

import groovy.transform.Canonical

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

/**
 * Main entity class for server workflow. This is created in mobile clients
 * and passed via DTOs to be persisted on server.
 *
 * @see Debt
 * @author Oleg Chernovskiy
 */
@Entity
@Canonical
class StoredDebt {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    String id

    // can't use cascade here (the case when new debt is created for existing actor)
    // persist in DAO layer prior to debt instance
    @ManyToOne(optional = false)
    StoredActor src

    // can't use cascade here (the case when new debt is created for existing actor)
    // persist in DAO layer prior to debt instance
    @ManyToOne(optional = false)
    StoredActor dest

    @Column(nullable = false)
    BigDecimal amount

    @Column(nullable = false)
    Date created

    @Column
    boolean approvedBySrc

    @Column
    boolean approvedByDest

    Debt toDto() {
        return new Debt(
                id: id,
                src: new Actor(id: src.id),
                dest: new Actor(id: dest.id),
                amount: amount,
                created: created,
                approvedBySrc: approvedBySrc,
                approvedByDest: approvedByDest
        )
    }

    static StoredDebt fromDto(Debt dto) {
        return new StoredDebt(
                id: dto.id,
                src: new StoredActor(id: dto.src.id),
                dest: new StoredActor(id: dto.dest.id),
                amount: dto.amount,
                created: dto.created,
                approvedBySrc: dto.approvedBySrc,
                approvedByDest: dto.approvedByDest
        )
    }
}
