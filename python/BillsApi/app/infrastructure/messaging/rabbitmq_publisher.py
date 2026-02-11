import json
import threading
from datetime import date, datetime
from decimal import Decimal
from typing import Any

import pika

from app.application.bills.ports.integration_event_publisher import IntegrationEventPublisher


def _json_default(value: Any) -> Any:
    if isinstance(value, (datetime, date)):
        return value.isoformat()
    if isinstance(value, Decimal):
        return float(value)
    raise TypeError(f"Object of type {type(value).__name__} is not JSON serializable")


class RabbitMqIntegrationEventPublisher(IntegrationEventPublisher):
    def __init__(
        self,
        host: str,
        port: int,
        user: str,
        password: str,
        vhost: str,
        queue_name: str,
    ) -> None:
        credentials = pika.PlainCredentials(user, password)
        self._params = pika.ConnectionParameters(
            host=host,
            port=port,
            virtual_host=vhost,
            credentials=credentials,
            heartbeat=30,
            blocked_connection_timeout=30,
        )
        self._queue_name = queue_name
        self._lock = threading.Lock()
        self._connection: pika.BlockingConnection | None = None
        self._channel: pika.channel.Channel | None = None

    def publish(self, event_name: str, payload: dict[str, Any]) -> None:
        envelope = {
            "eventName": event_name,
            "occurredAtUtc": datetime.utcnow().isoformat() + "Z",
            "payload": payload,
        }
        body = json.dumps(envelope, default=_json_default).encode("utf-8")

        with self._lock:
            channel = self._ensure_channel()
            channel.basic_publish(
                exchange="",
                routing_key=self._queue_name,
                body=body,
                properties=pika.BasicProperties(
                    content_type="application/json",
                    delivery_mode=2,
                ),
            )

    def close(self) -> None:
        with self._lock:
            if self._connection and self._connection.is_open:
                self._connection.close()
            self._connection = None
            self._channel = None

    def _ensure_channel(self) -> pika.channel.Channel:
        if self._connection is None or self._connection.is_closed:
            self._connection = pika.BlockingConnection(self._params)
            self._channel = self._connection.channel()
            self._channel.queue_declare(
                queue=self._queue_name,
                durable=True,
                exclusive=False,
                auto_delete=False,
            )

        if self._channel is None or self._channel.is_closed:
            self._channel = self._connection.channel()
            self._channel.queue_declare(
                queue=self._queue_name,
                durable=True,
                exclusive=False,
                auto_delete=False,
            )

        return self._channel
