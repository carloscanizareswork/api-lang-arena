package com.apilangarena.billsapi.application.bills.create

import com.apilangarena.billsapi.domain.bills.NewBill

interface BillWriteRepository {
    fun existsByBillNumber(billNumber: String): Boolean
    fun create(newBill: NewBill): CreateBillResult
}
