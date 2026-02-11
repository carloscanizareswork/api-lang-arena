export type CreateBillLineCommand = {
  concept: string;
  quantity: number;
  unitAmount: number;
};

export type CreateBillCommand = {
  billNumber: string;
  issuedAt: string;
  customerName: string;
  currency: string;
  tax: number;
  lines: CreateBillLineCommand[];
};
