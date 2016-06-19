package com.dao.mydebts.repos

import com.dao.mydebts.entities.AuditEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository for managing {@link AuditEntry}s
 *
 * @author Oleg Chernovskiy
 */
@Repository
interface AuditEntryRepo extends JpaRepository<AuditEntry, String> {

}
