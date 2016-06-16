package com.dao.mydebts.repos

import com.dao.mydebts.entities.StoredActor
import com.dao.mydebts.entities.StoredDebt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for managing {@link StoredDebt}s
 *
 * @author Oleg Chernovskiy
 */
@Repository
interface StoredDebtRepo extends JpaRepository<StoredDebt, String> {

    @Query("select d from StoredDebt d where d.src.id = :id or d.dest.id = :id")
    List<StoredDebt> findByActor(@Param("id") String id)

    List<StoredDebt> findBySrc(StoredActor src)

    @Query("select d from StoredDebt d where d.amount != 0.0")
    List<StoredDebt> findAllNotSettled()
}
