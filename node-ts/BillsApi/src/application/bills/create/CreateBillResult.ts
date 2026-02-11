export type CreateBillResult = {
  id: number;
  billNumber: string;
  issuedAt: string;
  subtotal: number;
  tax: number;
  total: number;
  currency: string;
};
