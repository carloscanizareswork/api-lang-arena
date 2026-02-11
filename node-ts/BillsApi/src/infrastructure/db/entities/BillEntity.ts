import { Column, Entity, OneToMany, PrimaryGeneratedColumn } from "typeorm";
import { BillLineEntity } from "./BillLineEntity";

@Entity({ name: "bill" })
export class BillEntity {
  @PrimaryGeneratedColumn({ type: "bigint" })
  id!: string;

  @Column({ name: "bill_number", type: "text", unique: true })
  billNumber!: string;

  @Column({ name: "issued_at", type: "date" })
  issuedAt!: string;

  @Column({ name: "customer_name", type: "text" })
  customerName!: string;

  @Column({ name: "subtotal", type: "numeric", precision: 12, scale: 2 })
  subtotal!: string;

  @Column({ name: "tax", type: "numeric", precision: 12, scale: 2 })
  tax!: string;

  @Column({ name: "currency", type: "char", length: 3 })
  currency!: string;

  @OneToMany(() => BillLineEntity, (line) => line.bill)
  lines!: BillLineEntity[];
}
