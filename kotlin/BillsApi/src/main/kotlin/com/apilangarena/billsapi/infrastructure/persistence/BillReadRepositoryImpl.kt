package com.apilangarena.billsapi.infrastructure.persistence

import com.apilangarena.billsapi.application.bills.get.BillReadRepository
import com.apilangarena.billsapi.application.bills.get.BillSummaryDto
import com.apilangarena.billsapi.infrastructure.persistence.repository.BillJpaRepository
import org.springframework.stereotype.Repository

@Repository
class BillReadRepositoryImpl(
    private val billJpaRepository: BillJpaRepository,
) : BillReadRepository {
    override fun listBills(): List<BillSummaryDto> {
        return billJpaRepository.listBillSummaries().map {
            BillSummaryDto(
                id = it.id,
                billNumber = it.billNumber,
                issuedAt = it.issuedAt,
                total = it.total,
                currency = it.currency,
            )
        }
    }
}
