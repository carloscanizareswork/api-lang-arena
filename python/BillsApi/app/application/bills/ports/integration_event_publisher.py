from abc import ABC, abstractmethod
from typing import Any


class IntegrationEventPublisher(ABC):
    @abstractmethod
    def publish(self, event_name: str, payload: dict[str, Any]) -> None:
        raise NotImplementedError
