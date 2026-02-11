package rabbitmq

import (
	"context"
	"encoding/json"
	"sync"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
	billcreate "go-bills-api/internal/application/bill/create"
)

type Options struct {
	URL              string
	QueueName        string
	ReconnectBackoff time.Duration
}

type Publisher struct {
	opts       Options
	mu         sync.Mutex
	connection *amqp.Connection
	channel    *amqp.Channel
}

type envelope struct {
	EventName     string                      `json:"eventName"`
	OccurredAtUTC time.Time                   `json:"occurredAtUtc"`
	Payload       billcreate.BillCreatedEvent `json:"payload"`
}

func NewPublisher(opts Options) (*Publisher, error) {
	p := &Publisher{opts: opts}
	if p.opts.ReconnectBackoff <= 0 {
		p.opts.ReconnectBackoff = 500 * time.Millisecond
	}
	if err := p.ensureChannel(); err != nil {
		return nil, err
	}
	return p, nil
}

func (p *Publisher) PublishBillCreated(ctx context.Context, event billcreate.BillCreatedEvent) error {
	select {
	case <-ctx.Done():
		return ctx.Err()
	default:
	}

	body, err := json.Marshal(envelope{
		EventName:     "bill.created",
		OccurredAtUTC: time.Now().UTC(),
		Payload:       event,
	})
	if err != nil {
		return err
	}

	p.mu.Lock()
	defer p.mu.Unlock()

	if err := p.ensureChannel(); err != nil {
		return err
	}

	return p.channel.PublishWithContext(
		ctx,
		"",
		p.opts.QueueName,
		false,
		false,
		amqp.Publishing{
			ContentType:  "application/json",
			Body:         body,
			DeliveryMode: amqp.Persistent,
			Timestamp:    time.Now().UTC(),
		},
	)
}

func (p *Publisher) Close() error {
	p.mu.Lock()
	defer p.mu.Unlock()
	if p.channel != nil {
		_ = p.channel.Close()
		p.channel = nil
	}
	if p.connection != nil {
		if err := p.connection.Close(); err != nil {
			return err
		}
		p.connection = nil
	}
	return nil
}

func (p *Publisher) ensureChannel() error {
	if p.connection != nil && !p.connection.IsClosed() && p.channel != nil && !p.channel.IsClosed() {
		return nil
	}

	if p.channel != nil {
		_ = p.channel.Close()
		p.channel = nil
	}
	if p.connection != nil {
		_ = p.connection.Close()
		p.connection = nil
	}

	conn, err := amqp.Dial(p.opts.URL)
	if err != nil {
		return err
	}
	ch, err := conn.Channel()
	if err != nil {
		_ = conn.Close()
		return err
	}
	if _, err := ch.QueueDeclare(
		p.opts.QueueName,
		true,
		false,
		false,
		false,
		nil,
	); err != nil {
		_ = ch.Close()
		_ = conn.Close()
		return err
	}

	p.connection = conn
	p.channel = ch
	return nil
}
