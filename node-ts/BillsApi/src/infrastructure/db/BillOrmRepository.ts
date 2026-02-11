import { QueryFailedError } from "typeorm";
import { NewBill } from "../../domain/bills/NewBill";
import { BillReadRepository } from "../../application/bills/get/BillReadRepository";
import { BillSummaryDto } from "../../application/bills/get/BillSummaryDto";
import { BillWriteRepository } from "../../application/bills/create/BillWriteRepository";
import { CreateBillResult } from "../../application/bills/create/CreateBillResult";
import { appDataSource } from "./data-source";
import { BillEntity } from "./entities/BillEntity";
import { BillLineEntity } from "./entities/BillLineEntity";

export class BillOrmRepository implements BillReadRepository, BillWriteRepository {
  async listBills(): Promise<BillSummaryDto[]> {
    const rows = await appDataSource
      .getRepository(BillEntity)
      .createQueryBuilder("b")
      .leftJoin(BillLineEntity, "bl", "bl.bill_id = b.id")
      .select("b.id", "id")
      .addSelect("b.bill_number", "billNumber")
      .addSelect("b.issued_at::text", "issuedAt")
      .addSelect("COALESCE(SUM(bl.line_amount), 0) + b.tax", "total")
      .addSelect("b.currency", "currency")
      .groupBy("b.id")
      .addGroupBy("b.bill_number")
      .addGroupBy("b.issued_at")
      .addGroupBy("b.tax")
      .addGroupBy("b.currency")
      .orderBy("b.id", "ASC")
      .getRawMany<{
        id: string;
        billNumber: string;
        issuedAt: string;
        total: string;
        currency: string;
      }>();

    return rows.map((row) => ({
      id: Number(row.id),
      billNumber: row.billNumber,
      issuedAt: row.issuedAt,
      total: Number(row.total),
      currency: row.currency.trim()
    }));
  }

  async existsByBillNumber(billNumber: string): Promise<boolean> {
    const found = await appDataSource.getRepository(BillEntity).findOne({
      select: { id: true },
      where: { billNumber },
    });
    return Boolean(found);
  }

  async createInTransaction(newBill: NewBill): Promise<CreateBillResult> {
    try {
      return await appDataSource.transaction(async (manager) => {
        const bill = manager.create(BillEntity, {
          billNumber: newBill.billNumber,
          issuedAt: newBill.issuedAt,
          customerName: newBill.customerName,
          subtotal: newBill.subtotal.toFixed(2),
          tax: newBill.tax.toFixed(2),
          currency: newBill.currency
        });

        const savedBill = await manager.save(bill);

        const lines = newBill.lines.map((line) =>
          manager.create(BillLineEntity, {
            bill: savedBill,
            lineNo: line.lineNo,
            concept: line.concept,
            quantity: line.quantity.toFixed(2),
            unitAmount: line.unitAmount.toFixed(2),
            lineAmount: line.lineAmount.toFixed(2)
          })
        );

        await manager.save(lines);

        return {
          id: Number(savedBill.id),
          billNumber: savedBill.billNumber,
          issuedAt: savedBill.issuedAt,
          subtotal: newBill.subtotal,
          tax: newBill.tax,
          total: newBill.total,
          currency: savedBill.currency.trim()
        };
      });
    } catch (error) {
      if (error instanceof QueryFailedError) {
        const code = (error as QueryFailedError & { driverError?: { code?: string } }).driverError?.code;
        if (code) {
          const typed = error as QueryFailedError & { code?: string };
          typed.code = code;
        }
      }
      throw error;
    }
  }
}
