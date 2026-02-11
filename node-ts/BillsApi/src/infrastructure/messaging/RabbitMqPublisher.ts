import amqplib, { Channel, ChannelModel } from "amqplib";
import { buildAmqpUrl, config } from "../../config";
import { BillCreatedEvent } from "../../application/bills/create/BillCreatedEvent";
import { IntegrationEventPublisher } from "../../application/bills/create/IntegrationEventPublisher";

export class RabbitMqPublisher implements IntegrationEventPublisher {
  private connection: ChannelModel | null = null;
  private channel: Channel | null = null;
  private connecting: Promise<Channel> | null = null;

  async init(): Promise<void> {
    await this.ensureChannel();
  }

  async publishBillCreated(event: BillCreatedEvent): Promise<void> {
    const envelope = {
      eventName: "bill.created",
      occurredAtUtc: new Date().toISOString(),
      payload: event
    };
    const payload = Buffer.from(JSON.stringify(envelope));

    try {
      const channel = await this.ensureChannel();
      channel.sendToQueue(config.rabbit.queue, payload, {
        contentType: "application/json",
        persistent: true
      });
    } catch {
      // Recover stale/broken AMQP channel and retry once.
      await this.resetConnection();
      const channel = await this.ensureChannel();
      channel.sendToQueue(config.rabbit.queue, payload, {
        contentType: "application/json",
        persistent: true
      });
    }
  }

  async close(): Promise<void> {
    await this.resetConnection();
  }

  private async ensureChannel(): Promise<Channel> {
    if (this.connection && this.channel) {
      return this.channel;
    }

    if (this.connecting) {
      return this.connecting;
    }

    this.connecting = (async () => {
      const connection = await amqplib.connect(buildAmqpUrl());
      connection.on("close", () => {
        this.connection = null;
        this.channel = null;
      });
      connection.on("error", () => {
        this.connection = null;
        this.channel = null;
      });

      const channel = await connection.createChannel();
      channel.on("close", () => {
        this.channel = null;
      });
      channel.on("error", () => {
        this.channel = null;
      });

      await channel.assertQueue(config.rabbit.queue, {
        durable: true,
        exclusive: false,
        autoDelete: false
      });

      this.connection = connection;
      this.channel = channel;
      return channel;
    })();

    try {
      return await this.connecting;
    } finally {
      this.connecting = null;
    }
  }

  private async resetConnection(): Promise<void> {
    const channel = this.channel;
    const connection = this.connection;

    this.channel = null;
    this.connection = null;
    this.connecting = null;

    if (channel) {
      try {
        await channel.close();
      } catch {
        // Ignore close errors during reset.
      }
    }
    if (connection) {
      try {
        await connection.close();
      } catch {
        // Ignore close errors during reset.
      }
    }
  }
}
