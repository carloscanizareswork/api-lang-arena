package com.apilangarena.javabillsapi.presentation;

import com.apilangarena.javabillsapi.application.bills.create.CreateBillCommand;
import com.apilangarena.javabillsapi.application.bills.create.CreateBillLineCommand;
import com.apilangarena.javabillsapi.application.bills.create.CreateBillService;
import com.apilangarena.javabillsapi.application.bills.get.GetBillsService;
import com.apilangarena.javabillsapi.presentation.dto.BillResponse;
import com.apilangarena.javabillsapi.presentation.dto.CreateBillRequest;
import com.apilangarena.javabillsapi.presentation.dto.CreateBillResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
public class BillsController {
    private final JdbcTemplate jdbcTemplate;
    private final GetBillsService getBillsService;
    private final CreateBillService createBillService;

    public BillsController(JdbcTemplate jdbcTemplate, GetBillsService getBillsService, CreateBillService createBillService) {
        this.jdbcTemplate = jdbcTemplate;
        this.getBillsService = getBillsService;
        this.createBillService = createBillService;
    }

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("service", "java-bills-api", "status", "ok");
    }

    @GetMapping("/bills-minimal")
    public List<BillResponse> getBillsMinimal() {
        String sql = """
            SELECT b.id,
                   b.bill_number,
                   b.issued_at,
                   COALESCE(SUM(bl.line_amount), 0) + b.tax AS total,
                   b.currency
            FROM bill b
            LEFT JOIN bill_line bl ON bl.bill_id = b.id
            GROUP BY b.id, b.bill_number, b.issued_at, b.tax, b.currency
            ORDER BY b.id;
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new BillResponse(
            rs.getLong("id"),
            rs.getString("bill_number"),
            rs.getObject("issued_at", LocalDate.class),
            rs.getBigDecimal("total"),
            rs.getString("currency").trim()
        ));
    }

    @GetMapping("/bills")
    public List<BillResponse> getBills() {
        return getBillsService.execute().stream()
            .map(item -> new BillResponse(
                item.id(),
                item.billNumber(),
                item.issuedAt(),
                item.total(),
                item.currency()
            ))
            .toList();
    }

    @PostMapping("/bills")
    public ResponseEntity<CreateBillResponse> createBill(@Valid @RequestBody CreateBillRequest request) {
        CreateBillCommand command = new CreateBillCommand(
            request.billNumber(),
            request.issuedAt(),
            request.customerName(),
            request.currency(),
            request.tax(),
            request.lines().stream()
                .map(line -> new CreateBillLineCommand(line.concept(), line.quantity(), line.unitAmount()))
                .toList()
        );

        var created = createBillService.execute(command);
        CreateBillResponse response = new CreateBillResponse(
            created.id(),
            created.billNumber(),
            created.issuedAt(),
            created.subtotal(),
            created.tax(),
            created.total(),
            created.currency()
        );

        return ResponseEntity.created(URI.create("/bills/" + created.id())).body(response);
    }
}
