import { Pool } from "pg";
import { buildPgConnectionString, config } from "../../config";

export const minimalPool = new Pool({
  connectionString: buildPgConnectionString(),
  max: config.postgres.maxPool
});
