import { ConflictError } from "../../common/ConflictError";
import { NewBill, NewBillLine } from "../../../domain/bills/NewBill";
import { BillWriteRepository } from "./BillWriteRepository";
import { CreateBillCommand } from "./CreateBillCommand";
import { CreateBillResult } from "./CreateBillResult";
import { IntegrationEventPublisher } from "./IntegrationEventPublisher";

export class CreateBillService {
  constructor(
    private readonly repo: BillWriteRepository,
    private readonly publisher: IntegrationEventPublisher
  ) {}

  async execute(command: CreateBillCommand): Promise<CreateBillResult> {
    if (await this.repo.existsByBillNumber(command.billNumber.trim())) {
      throw new ConflictError(`Bill number '${command.billNumber}' already exists.`);
    }

    const lines = command.lines.map((line, i) =>
      NewBillLine.create({ lineNo: i + 1, concept: line.concept, quantity: line.quantity, unitAmount: line.unitAmount })
    );

    const newBill = NewBill.create({
      billNumber: command.billNumber,
      issuedAt: command.issuedAt,
      customerName: command.customerName,
      currency: command.currency,
      tax: command.tax,
      lines
    });

    let created: CreateBillResult;
    try {
      created = await this.repo.createInTransaction(newBill);
    } catch (error: unknown) {
      if (isUniqueViolation(error)) {
        throw new ConflictError(`Bill number '${command.billNumber}' already exists.`);
      }
      throw error;
    }

    await this.publisher.publishBillCreated({
      billId: created.id,
      billNumber: created.billNumber,
      issuedAt: created.issuedAt,
      subtotal: created.subtotal,
      tax: created.tax,
      total: created.total,
      currency: created.currency,
      occurredAtUtc: new Date().toISOString(),
      source: "node-api"
    });

    return created;
  }
}

function isUniqueViolation(error: unknown): boolean {
  const code = (error as { code?: string })?.code;
  return code === "23505";
}
