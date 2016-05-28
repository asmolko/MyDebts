package com.dao.mydebts.repos

import com.dao.mydebts.entities.StoredActor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * @author Oleg Chernovskiy
 */
@Repository
interface StoredActorRepo extends JpaRepository<StoredActor, String> {

}
