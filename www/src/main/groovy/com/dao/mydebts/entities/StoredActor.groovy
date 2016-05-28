package com.dao.mydebts.entities

import groovy.transform.Canonical

import javax.persistence.Entity
import javax.persistence.Id

/**
 * @author Oleg Chernovskiy
 */
@Entity
@Canonical
class StoredActor {

    @Id
    String id
}
