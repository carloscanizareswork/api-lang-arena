package com.apilangarena.billsapi.application.bills.get

interface BillReadRepository {
    fun listBills(): List<BillSummaryDto>
}
