package com.apilangarena.javabillsapi.application.bills.get;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetBillsService {
    private final BillReadRepository billReadRepository;

    public GetBillsService(BillReadRepository billReadRepository) {
        this.billReadRepository = billReadRepository;
    }

    public List<BillSummaryDto> execute() {
        return billReadRepository.listBills();
    }
}
