package com.apilangarena.javabillsapi.application.bills.create;

import com.apilangarena.javabillsapi.domain.bills.NewBill;

public interface BillWriteRepository {
    boolean existsByBillNumber(String billNumber);

    CreateBillResult create(NewBill newBill);
}
