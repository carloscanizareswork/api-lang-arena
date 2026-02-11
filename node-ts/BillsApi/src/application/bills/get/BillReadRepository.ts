import { BillSummaryDto } from "./BillSummaryDto";

export interface BillReadRepository {
  listBills(): Promise<BillSummaryDto[]>;
}
