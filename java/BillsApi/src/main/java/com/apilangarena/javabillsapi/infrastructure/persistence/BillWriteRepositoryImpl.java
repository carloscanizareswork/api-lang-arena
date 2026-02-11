package com.apilangarena.javabillsapi.infrastructure.persistence;

import com.apilangarena.javabillsapi.application.bills.create.BillWriteRepository;
import com.apilangarena.javabillsapi.application.bills.create.CreateBillResult;
import com.apilangarena.javabillsapi.domain.bills.NewBill;
import com.apilangarena.javabillsapi.domain.bills.NewBillLine;
import com.apilangarena.javabillsapi.infrastructure.persistence.entity.BillEntity;
import com.apilangarena.javabillsapi.infrastructure.persistence.entity.BillLineEntity;
import com.apilangarena.javabillsapi.infrastructure.persistence.repository.BillJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
public class BillWriteRepositoryImpl implements BillWriteRepository {
    private final BillJpaRepository billJpaRepository;

    public BillWriteRepositoryImpl(BillJpaRepository billJpaRepository) {
        this.billJpaRepository = billJpaRepository;
    }

    @Override
    public boolean existsByBillNumber(String billNumber) {
        return billJpaRepository.existsByBillNumber(billNumber);
    }

    @Override
    public CreateBillResult create(NewBill newBill) {
        BillEntity entity = new BillEntity(
            newBill.getBillNumber(),
            newBill.getIssuedAt(),
            newBill.getCustomerName(),
            newBill.getSubtotal(),
            newBill.getTax(),
            newBill.getCurrency()
        );

        for (NewBillLine line : newBill.getLines()) {
            entity.addLine(new BillLineEntity(
                line.getLineNo(),
                line.getConcept(),
                line.getQuantity(),
                line.getUnitAmount(),
                line.getLineAmount()
            ));
        }

        BillEntity saved = billJpaRepository.saveAndFlush(entity);
        Long savedId = Objects.requireNonNull(saved.getId(), "Persisted bill id was null.");

        return new CreateBillResult(
            savedId,
            saved.getBillNumber(),
            saved.getIssuedAt(),
            newBill.getSubtotal(),
            newBill.getTax(),
            newBill.getTotal(),
            saved.getCurrency()
        );
    }
}
