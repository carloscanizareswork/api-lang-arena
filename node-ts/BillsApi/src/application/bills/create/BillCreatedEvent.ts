export type BillCreatedEvent = {
  billId: number;
  billNumber: string;
  issuedAt: string;
  subtotal: number;
  tax: number;
  total: number;
  currency: string;
  occurredAtUtc: string;
  source: string;
};
