package com.apilangarena.javabillsapi.application.bills.create;

import com.apilangarena.javabillsapi.application.common.ConflictException;
import com.apilangarena.javabillsapi.domain.bills.NewBill;
import com.apilangarena.javabillsapi.domain.bills.NewBillLine;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class CreateBillService {
    private final TransactionTemplate transactionTemplate;
    private final BillWriteRepository billWriteRepository;
    private final IntegrationEventPublisher integrationEventPublisher;

    public CreateBillService(
        TransactionTemplate transactionTemplate,
        BillWriteRepository billWriteRepository,
        IntegrationEventPublisher integrationEventPublisher
    ) {
        this.transactionTemplate = transactionTemplate;
        this.billWriteRepository = billWriteRepository;
        this.integrationEventPublisher = integrationEventPublisher;
    }

    public CreateBillResult execute(CreateBillCommand command) {
        CreateBillResult created = transactionTemplate.execute(status -> {
            String normalizedBillNumber = command.billNumber() == null ? "" : command.billNumber().trim();
            if (billWriteRepository.existsByBillNumber(normalizedBillNumber)) {
                throw new ConflictException("Bill number '" + command.billNumber() + "' already exists.");
            }

            List<NewBillLine> lines = new ArrayList<>();
            for (int idx = 0; idx < command.lines().size(); idx++) {
                CreateBillLineCommand line = command.lines().get(idx);
                lines.add(NewBillLine.create(
                    idx + 1,
                    line.concept(),
                    line.quantity(),
                    line.unitAmount()
                ));
            }

            NewBill newBill = NewBill.create(
                command.billNumber(),
                command.issuedAt(),
                command.customerName(),
                command.currency(),
                command.tax(),
                lines
            );

            try {
                return billWriteRepository.create(newBill);
            } catch (DataIntegrityViolationException ignored) {
                throw new ConflictException("Bill number '" + command.billNumber() + "' already exists.");
            }
        });

        if (created == null) {
            throw new IllegalStateException("Transaction did not produce a created bill result.");
        }

        integrationEventPublisher.publishBillCreated(
            new BillCreatedEvent(
                created.id(),
                created.billNumber(),
                created.issuedAt(),
                created.subtotal(),
                created.tax(),
                created.total(),
                created.currency(),
                OffsetDateTime.now(ZoneOffset.UTC),
                "java-api"
            )
        );

        return created;
    }
}
