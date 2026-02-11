package com.apilangarena.billsapi.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "bill_line")
class BillLineEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    var bill: BillEntity? = null,

    @Column(name = "line_no", nullable = false)
    var lineNo: Int = 0,

    @Column(name = "concept", nullable = false, length = 200)
    var concept: String = "",

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    var quantity: BigDecimal = BigDecimal.ZERO,

    @Column(name = "unit_amount", nullable = false, precision = 12, scale = 2)
    var unitAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "line_amount", nullable = false, precision = 12, scale = 2)
    var lineAmount: BigDecimal = BigDecimal.ZERO,
)
