package com.dao.mydebts.entities

import groovy.transform.Canonical

import javax.persistence.Entity
import javax.persistence.Id

/**
 * Represents actor entity.
 * <p/>Though id-only for now, this is a subject to contain some additional metadata in future,
 * hence extracted to distinct class.
 *
 * @author Oleg Chernovskiy
 */
@Entity
@Canonical
class StoredActor {

    @Id
    String id
}
