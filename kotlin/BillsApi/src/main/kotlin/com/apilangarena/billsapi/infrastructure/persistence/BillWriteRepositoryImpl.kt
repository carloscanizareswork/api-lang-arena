package com.apilangarena.billsapi.infrastructure.persistence

import com.apilangarena.billsapi.application.bills.create.BillWriteRepository
import com.apilangarena.billsapi.application.bills.create.CreateBillResult
import com.apilangarena.billsapi.domain.bills.NewBill
import com.apilangarena.billsapi.infrastructure.persistence.entity.BillEntity
import com.apilangarena.billsapi.infrastructure.persistence.entity.BillLineEntity
import com.apilangarena.billsapi.infrastructure.persistence.repository.BillJpaRepository
import org.springframework.stereotype.Repository

@Repository
class BillWriteRepositoryImpl(
    private val billJpaRepository: BillJpaRepository,
) : BillWriteRepository {
    override fun existsByBillNumber(billNumber: String): Boolean {
        return billJpaRepository.existsByBillNumber(billNumber)
    }

    override fun create(newBill: NewBill): CreateBillResult {
        val entity = BillEntity(
            billNumber = newBill.billNumber,
            issuedAt = newBill.issuedAt,
            customerName = newBill.customerName,
            subtotal = newBill.subtotal,
            tax = newBill.tax,
            currency = newBill.currency,
        )

        newBill.lines.forEach { line ->
            entity.addLine(
                BillLineEntity(
                    lineNo = line.lineNo,
                    concept = line.concept,
                    quantity = line.quantity,
                    unitAmount = line.unitAmount,
                    lineAmount = line.lineAmount,
                )
            )
        }

        val saved = billJpaRepository.saveAndFlush(entity)
        val savedId = requireNotNull(saved.id) { "Persisted bill id was null." }

        return CreateBillResult(
            id = savedId,
            billNumber = saved.billNumber,
            issuedAt = saved.issuedAt,
            subtotal = newBill.subtotal,
            tax = newBill.tax,
            total = newBill.total,
            currency = saved.currency,
        )
    }
}
