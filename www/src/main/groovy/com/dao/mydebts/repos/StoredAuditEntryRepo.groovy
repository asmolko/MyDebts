package com.dao.mydebts.repos

import com.dao.mydebts.entities.StoredAuditEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository for managing {@link StoredAuditEntry}s
 *
 * @author Oleg Chernovskiy
 */
@Repository
interface StoredAuditEntryRepo extends JpaRepository<StoredAuditEntry, String> {

}
