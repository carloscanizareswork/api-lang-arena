import Fastify from "fastify";
import { config } from "./config";
import { appDataSource } from "./infrastructure/db/data-source";
import { minimalPool } from "./infrastructure/db/minimal-pool";
import { BillOrmRepository } from "./infrastructure/db/BillOrmRepository";
import { GetBillsService } from "./application/bills/get/GetBillsService";
import { CreateBillService } from "./application/bills/create/CreateBillService";
import { RabbitMqPublisher } from "./infrastructure/messaging/RabbitMqPublisher";
import { registerBillsRoutes } from "./presentation/routes/bills";
import { handleApiError } from "./presentation/error-handler";

async function bootstrap(): Promise<void> {
  await appDataSource.initialize();

  const rabbitPublisher = new RabbitMqPublisher();
  await rabbitPublisher.init();

  const repository = new BillOrmRepository();
  const getBillsService = new GetBillsService(repository);
  const createBillService = new CreateBillService(repository, rabbitPublisher);

  const app = Fastify({ logger: false });
  app.setErrorHandler(handleApiError);
  registerBillsRoutes(app, getBillsService, createBillService);

  const shutdown = async () => {
    await app.close();
    await rabbitPublisher.close();
    await minimalPool.end();
    if (appDataSource.isInitialized) {
      await appDataSource.destroy();
    }
    process.exit(0);
  };

  process.on("SIGINT", () => void shutdown());
  process.on("SIGTERM", () => void shutdown());

  await app.listen({ host: "0.0.0.0", port: config.port });
}

void bootstrap();
