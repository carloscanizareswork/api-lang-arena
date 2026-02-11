package com.apilangarena.billsapi.application.bills.get

import org.springframework.stereotype.Service

@Service
class GetBillsService(
    private val billReadRepository: BillReadRepository,
) {
    fun execute(): List<BillSummaryDto> = billReadRepository.listBills()
}
