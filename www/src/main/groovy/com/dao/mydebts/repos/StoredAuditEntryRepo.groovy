package com.dao.mydebts.repos

import com.dao.mydebts.entities.StoredAuditEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for managing {@link StoredAuditEntry}s
 *
 * @author Oleg Chernovskiy
 */
@Repository
interface StoredAuditEntryRepo extends JpaRepository<StoredAuditEntry, String> {
    @Query("select e from StoredAuditEntry e where e.settled.id = :id")
    List<StoredAuditEntry> findByDebt(@Param("id") String debtId)
}
