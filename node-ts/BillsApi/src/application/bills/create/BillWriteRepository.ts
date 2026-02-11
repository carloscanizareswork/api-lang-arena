import { NewBill } from "../../../domain/bills/NewBill";
import { CreateBillResult } from "./CreateBillResult";

export interface BillWriteRepository {
  existsByBillNumber(billNumber: string): Promise<boolean>;
  createInTransaction(newBill: NewBill): Promise<CreateBillResult>;
}
