package com.apilangarena.billsapi.presentation

import com.apilangarena.billsapi.application.bills.create.CreateBillCommand
import com.apilangarena.billsapi.application.bills.create.CreateBillLineCommand
import com.apilangarena.billsapi.application.bills.create.CreateBillService
import com.apilangarena.billsapi.application.bills.get.GetBillsService
import com.apilangarena.billsapi.presentation.dto.BillResponse
import com.apilangarena.billsapi.presentation.dto.CreateBillRequest
import com.apilangarena.billsapi.presentation.dto.CreateBillResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.LocalDate

@RestController
class BillsController(
    private val jdbcTemplate: JdbcTemplate,
    private val getBillsService: GetBillsService,
    private val createBillService: CreateBillService,
) {
    @GetMapping("/")
    fun root(): Map<String, String> {
        return mapOf("service" to "kotlin-bills-api", "status" to "ok")
    }

    @GetMapping("/bills-minimal")
    fun getBillsMinimal(): List<BillResponse> {
        val sql =
            """
            SELECT b.id,
                   b.bill_number,
                   b.issued_at,
                   COALESCE(SUM(bl.line_amount), 0) + b.tax AS total,
                   b.currency
            FROM bill b
            LEFT JOIN bill_line bl ON bl.bill_id = b.id
            GROUP BY b.id, b.bill_number, b.issued_at, b.tax, b.currency
            ORDER BY b.id;
            """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            BillResponse(
                id = rs.getLong("id"),
                billNumber = rs.getString("bill_number"),
                issuedAt = rs.getObject("issued_at", LocalDate::class.java),
                total = rs.getBigDecimal("total"),
                currency = rs.getString("currency").trim(),
            )
        }
    }

    @GetMapping("/bills")
    fun getBills(): List<BillResponse> {
        return getBillsService.execute().map {
            BillResponse(
                id = it.id,
                billNumber = it.billNumber,
                issuedAt = it.issuedAt,
                total = it.total,
                currency = it.currency,
            )
        }
    }

    @PostMapping("/bills")
    fun createBill(@Valid @RequestBody request: CreateBillRequest): ResponseEntity<CreateBillResponse> {
        val command = CreateBillCommand(
            billNumber = request.billNumber,
            issuedAt = request.issuedAt,
            customerName = request.customerName,
            currency = request.currency,
            tax = request.tax,
            lines = request.lines.map {
                CreateBillLineCommand(
                    concept = it.concept,
                    quantity = it.quantity,
                    unitAmount = it.unitAmount,
                )
            },
        )

        val created = createBillService.execute(command)
        val response = CreateBillResponse(
            id = created.id,
            billNumber = created.billNumber,
            issuedAt = created.issuedAt,
            subtotal = created.subtotal,
            tax = created.tax,
            total = created.total,
            currency = created.currency,
        )

        return ResponseEntity.created(URI.create("/bills/${created.id}")).body(response)
    }
}
