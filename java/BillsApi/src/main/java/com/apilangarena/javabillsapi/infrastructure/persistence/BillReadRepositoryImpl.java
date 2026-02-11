package com.apilangarena.javabillsapi.infrastructure.persistence;

import com.apilangarena.javabillsapi.application.bills.get.BillReadRepository;
import com.apilangarena.javabillsapi.application.bills.get.BillSummaryDto;
import com.apilangarena.javabillsapi.infrastructure.persistence.repository.BillJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BillReadRepositoryImpl implements BillReadRepository {
    private final BillJpaRepository billJpaRepository;

    public BillReadRepositoryImpl(BillJpaRepository billJpaRepository) {
        this.billJpaRepository = billJpaRepository;
    }

    @Override
    public List<BillSummaryDto> listBills() {
        return billJpaRepository.listBillSummaries().stream()
            .map(item -> new BillSummaryDto(
                item.getId(),
                item.getBillNumber(),
                item.getIssuedAt(),
                item.getTotal(),
                item.getCurrency()
            ))
            .toList();
    }
}
