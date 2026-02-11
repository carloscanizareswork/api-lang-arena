package com.apilangarena.billsapi.infrastructure.persistence.repository

import com.apilangarena.billsapi.infrastructure.persistence.entity.BillEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BillJpaRepository : JpaRepository<BillEntity, Long> {
    fun existsByBillNumber(billNumber: String): Boolean

    @Query(
        """
        select b.id as id,
               b.billNumber as billNumber,
               b.issuedAt as issuedAt,
               coalesce(sum(bl.lineAmount), 0) + b.tax as total,
               b.currency as currency
        from BillEntity b
        left join b.lines bl
        group by b.id, b.billNumber, b.issuedAt, b.tax, b.currency
        order by b.id
        """
    )
    fun listBillSummaries(): List<BillSummaryProjection>
}
