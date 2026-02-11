package com.apilangarena.javabillsapi.infrastructure.persistence.repository;

import com.apilangarena.javabillsapi.infrastructure.persistence.entity.BillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BillJpaRepository extends JpaRepository<BillEntity, Long> {
    boolean existsByBillNumber(String billNumber);

    @Query("""
        select b.id as id,
               b.billNumber as billNumber,
               b.issuedAt as issuedAt,
               coalesce(sum(bl.lineAmount), 0) + b.tax as total,
               b.currency as currency
        from BillEntity b
        left join b.lines bl
        group by b.id, b.billNumber, b.issuedAt, b.tax, b.currency
        order by b.id
        """)
    List<BillSummaryProjection> listBillSummaries();
}
