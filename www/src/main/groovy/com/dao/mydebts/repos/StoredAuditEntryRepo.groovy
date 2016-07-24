package com.dao.mydebts.repos

import com.dao.mydebts.entities.StoredAuditEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for managing {@link StoredAuditEntry}s
 *
 * @author Daniil Bubnov
 */
@Repository
interface StoredAuditEntryRepo extends JpaRepository<StoredAuditEntry, String> {

    @Query("select e from StoredAuditEntry e where e.settled.id = :id")
    List<StoredAuditEntry> findByDebtId(@Param("id") String debtId)

    @Query("select e from StoredAuditEntry e where e.settled.src.id = :id or e.settled.dest.id = :id")
    List<StoredAuditEntry> findByUser(@Param("id") String userId)
}
