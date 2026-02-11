import "reflect-metadata";
import { DataSource } from "typeorm";
import { buildPgConnectionString, config } from "../../config";
import { BillEntity } from "./entities/BillEntity";
import { BillLineEntity } from "./entities/BillLineEntity";

export const appDataSource = new DataSource({
  type: "postgres",
  url: buildPgConnectionString(),
  entities: [BillEntity, BillLineEntity],
  synchronize: false,
  logging: false,
  extra: {
    max: config.postgres.maxPool
  }
});
