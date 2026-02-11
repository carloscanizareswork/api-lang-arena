import { BillReadRepository } from "./BillReadRepository";
import { BillSummaryDto } from "./BillSummaryDto";

export class GetBillsService {
  constructor(private readonly repo: BillReadRepository) {}

  execute(): Promise<BillSummaryDto[]> {
    return this.repo.listBills();
  }
}
