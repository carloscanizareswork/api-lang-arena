package com.apilangarena.billsapi.application.bills.create

import com.apilangarena.billsapi.application.common.ConflictException
import com.apilangarena.billsapi.domain.bills.NewBill
import com.apilangarena.billsapi.domain.bills.NewBillLine
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.OffsetDateTime

@Service
class CreateBillService(
    private val transactionTemplate: TransactionTemplate,
    private val billWriteRepository: BillWriteRepository,
    private val integrationEventPublisher: IntegrationEventPublisher,
) {
    fun execute(command: CreateBillCommand): CreateBillResult {
        val created = transactionTemplate.execute {
            if (billWriteRepository.existsByBillNumber(command.billNumber.trim())) {
                throw ConflictException("Bill number '${command.billNumber}' already exists.")
            }

            val lines = command.lines.mapIndexed { idx, line ->
                NewBillLine.create(
                    lineNo = idx + 1,
                    concept = line.concept,
                    quantity = line.quantity,
                    unitAmount = line.unitAmount,
                )
            }

            val newBill = NewBill.create(
                billNumber = command.billNumber,
                issuedAt = command.issuedAt,
                customerName = command.customerName,
                currency = command.currency,
                tax = command.tax,
                lines = lines,
            )

            try {
                billWriteRepository.create(newBill)
            } catch (_: DataIntegrityViolationException) {
                throw ConflictException("Bill number '${command.billNumber}' already exists.")
            }
        } ?: throw IllegalStateException("Transaction did not produce a created bill result.")

        // Publish only after transaction commit has completed.
        integrationEventPublisher.publishBillCreated(
            BillCreatedEvent(
                billId = created.id,
                billNumber = created.billNumber,
                issuedAt = created.issuedAt,
                subtotal = created.subtotal,
                tax = created.tax,
                total = created.total,
                currency = created.currency,
                occurredAtUtc = OffsetDateTime.now(),
                source = "kotlin-api",
            )
        )

        return created
    }
}
