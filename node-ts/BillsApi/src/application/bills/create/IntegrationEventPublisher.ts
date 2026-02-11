import { BillCreatedEvent } from "./BillCreatedEvent";

export interface IntegrationEventPublisher {
  publishBillCreated(event: BillCreatedEvent): Promise<void>;
}
