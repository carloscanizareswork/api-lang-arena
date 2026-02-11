package com.apilangarena.billsapi.infrastructure.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "bill")
class BillEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,

    @Column(name = "bill_number", nullable = false, unique = true, length = 50)
    var billNumber: String = "",

    @Column(name = "issued_at", nullable = false)
    var issuedAt: LocalDate = LocalDate.now(),

    @Column(name = "customer_name", nullable = false, length = 200)
    var customerName: String = "",

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    var subtotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "tax", nullable = false, precision = 12, scale = 2)
    var tax: BigDecimal = BigDecimal.ZERO,

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "USD",

    @OneToMany(mappedBy = "bill", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var lines: MutableList<BillLineEntity> = mutableListOf(),
) {
    fun addLine(line: BillLineEntity) {
        line.bill = this
        lines.add(line)
    }
}
