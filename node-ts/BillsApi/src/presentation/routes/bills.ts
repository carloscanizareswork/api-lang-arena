import { FastifyInstance } from "fastify";
import { minimalPool } from "../../infrastructure/db/minimal-pool";
import { GetBillsService } from "../../application/bills/get/GetBillsService";
import { CreateBillService } from "../../application/bills/create/CreateBillService";
import { createBillSchema } from "../dto/schemas";

export function registerBillsRoutes(
  app: FastifyInstance,
  getBillsService: GetBillsService,
  createBillService: CreateBillService
): void {
  app.get("/", async () => ({ service: "node-bills-api", status: "ok" }));

  app.get("/bills-minimal", async () => {
    const result = await minimalPool.query(
      `
      SELECT b.id,
             b.bill_number AS "billNumber",
             b.issued_at::text AS "issuedAt",
             COALESCE(SUM(bl.line_amount), 0) + b.tax AS total,
             b.currency AS currency
      FROM bill b
      LEFT JOIN bill_line bl ON bl.bill_id = b.id
      GROUP BY b.id, b.bill_number, b.issued_at, b.tax, b.currency
      ORDER BY b.id;
      `
    );

    return result.rows.map((row: { id: unknown; billNumber: unknown; issuedAt: unknown; total: unknown; currency: unknown }) => ({
      id: Number(row.id),
      billNumber: String(row.billNumber),
      issuedAt: String(row.issuedAt),
      total: Number(row.total),
      currency: String(row.currency).trim()
    }));
  });

  app.get("/bills", async () => {
    return getBillsService.execute();
  });

  app.post("/bills", async (request, reply) => {
    const payload = createBillSchema.parse(request.body);

    const created = await createBillService.execute({
      billNumber: payload.billNumber,
      issuedAt: payload.issuedAt,
      customerName: payload.customerName,
      currency: payload.currency,
      tax: payload.tax,
      lines: payload.lines.map((line) => ({
        concept: line.concept,
        quantity: line.quantity,
        unitAmount: line.unitAmount
      }))
    });

    void reply.header("Location", `/bills/${created.id}`);
    return reply.status(201).send(created);
  });
}
