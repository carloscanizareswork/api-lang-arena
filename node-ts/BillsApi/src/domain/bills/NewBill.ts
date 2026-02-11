import { DomainValidationError } from "../common/DomainValidationError";

export type NewBillLineInput = {
  concept: string;
  quantity: number;
  unitAmount: number;
};

export class NewBillLine {
  private constructor(
    public readonly lineNo: number,
    public readonly concept: string,
    public readonly quantity: number,
    public readonly unitAmount: number,
    public readonly lineAmount: number
  ) {}

  static create(input: { lineNo: number } & NewBillLineInput): NewBillLine {
    const errors: Record<string, string[]> = {};
    const push = (field: string, msg: string) => {
      if (!errors[field]) errors[field] = [];
      errors[field].push(msg);
    };

    if (input.lineNo <= 0) push("lines.lineNo", "Line number must be greater than zero.");
    if (!input.concept || input.concept.trim() === "") push("lines.concept", "Line concept is required.");
    if (input.concept && input.concept.trim().length > 200) push("lines.concept", "Line concept max length is 200.");
    if (input.quantity <= 0) push("lines.quantity", "Line quantity must be greater than zero.");
    if (input.unitAmount < 0) push("lines.unitAmount", "Line unit amount cannot be negative.");

    if (Object.keys(errors).length > 0) {
      throw new DomainValidationError(errors);
    }

    const quantity = roundMoney(input.quantity);
    const unitAmount = roundMoney(input.unitAmount);
    return new NewBillLine(
      input.lineNo,
      input.concept.trim(),
      quantity,
      unitAmount,
      roundMoney(quantity * unitAmount)
    );
  }
}

export class NewBill {
  public readonly subtotal: number;
  public readonly total: number;

  private constructor(
    public readonly billNumber: string,
    public readonly issuedAt: string,
    public readonly customerName: string,
    public readonly currency: string,
    public readonly tax: number,
    public readonly lines: NewBillLine[]
  ) {
    this.subtotal = roundMoney(lines.reduce((acc, line) => acc + line.lineAmount, 0));
    this.total = roundMoney(this.subtotal + tax);
  }

  static create(input: {
    billNumber: string;
    issuedAt: string;
    customerName: string;
    currency: string;
    tax: number;
    lines: NewBillLine[];
  }): NewBill {
    const errors: Record<string, string[]> = {};
    const push = (field: string, msg: string) => {
      if (!errors[field]) errors[field] = [];
      errors[field].push(msg);
    };

    const billNumber = (input.billNumber ?? "").trim();
    const customerName = (input.customerName ?? "").trim();
    const currency = (input.currency ?? "").trim().toUpperCase();
    const tax = roundMoney(input.tax ?? 0);

    if (!billNumber) push("billNumber", "Bill number is required.");
    if (billNumber.length > 50) push("billNumber", "Bill number max length is 50.");
    if (!customerName) push("customerName", "Customer name is required.");
    if (customerName.length > 200) push("customerName", "Customer name max length is 200.");
    if (currency.length !== 3) push("currency", "Currency must be a 3-letter ISO code.");
    if (tax < 0) push("tax", "Tax cannot be negative.");
    if (!input.lines || input.lines.length === 0) push("lines", "At least one line is required.");

    if (Object.keys(errors).length > 0) {
      throw new DomainValidationError(errors);
    }

    return new NewBill(billNumber, input.issuedAt, customerName, currency, tax, input.lines);
  }
}

function roundMoney(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}
