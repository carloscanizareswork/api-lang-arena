import { Column, Entity, JoinColumn, ManyToOne, PrimaryGeneratedColumn } from "typeorm";
import { BillEntity } from "./BillEntity";

@Entity({ name: "bill_line" })
export class BillLineEntity {
  @PrimaryGeneratedColumn({ type: "bigint" })
  id!: string;

  @ManyToOne(() => BillEntity, (bill) => bill.lines, { onDelete: "CASCADE" })
  @JoinColumn({ name: "bill_id" })
  bill!: BillEntity;

  @Column({ name: "line_no", type: "integer" })
  lineNo!: number;

  @Column({ name: "concept", type: "text" })
  concept!: string;

  @Column({ name: "quantity", type: "numeric", precision: 10, scale: 2 })
  quantity!: string;

  @Column({ name: "unit_amount", type: "numeric", precision: 12, scale: 2 })
  unitAmount!: string;

  @Column({ name: "line_amount", type: "numeric", precision: 12, scale: 2 })
  lineAmount!: string;
}
