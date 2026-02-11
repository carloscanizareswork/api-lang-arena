import { z } from "zod";

export const createBillLineSchema = z.object({
  concept: z.string().trim().min(1, "Line concept is required.").max(200, "Line concept max length is 200."),
  quantity: z.number().gt(0, "Line quantity must be greater than zero."),
  unitAmount: z.number().gte(0, "Line unit amount cannot be negative.")
});

export const createBillSchema = z.object({
  billNumber: z.string().trim().min(1, "Bill number is required.").max(50, "Bill number max length is 50."),
  issuedAt: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Issued date must use format YYYY-MM-DD."),
  customerName: z.string().trim().min(1, "Customer name is required.").max(200, "Customer name max length is 200."),
  currency: z.string().trim().length(3, "Currency must be a 3-letter ISO code."),
  tax: z.number().gte(0, "Tax cannot be negative."),
  lines: z.array(createBillLineSchema).min(1, "At least one line is required.")
});

export type CreateBillRequest = z.infer<typeof createBillSchema>;
