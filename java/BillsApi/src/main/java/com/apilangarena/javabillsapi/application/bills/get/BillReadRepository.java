package com.apilangarena.javabillsapi.application.bills.get;

import java.util.List;

public interface BillReadRepository {
    List<BillSummaryDto> listBills();
}
