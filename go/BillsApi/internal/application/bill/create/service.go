package create

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5/pgconn"
	apperrors "go-bills-api/internal/application/errors"
	"go-bills-api/internal/domain/bill"
)

type IntegrationEventPublisher interface {
	PublishBillCreated(ctx context.Context, event BillCreatedEvent) error
}

type BillCreatedEvent struct {
	BillID        int64     `json:"billId"`
	BillNumber    string    `json:"billNumber"`
	IssuedAt      string    `json:"issuedAt"`
	Subtotal      float64   `json:"subtotal"`
	Tax           float64   `json:"tax"`
	Total         float64   `json:"total"`
	Currency      string    `json:"currency"`
	OccurredAtUTC time.Time `json:"occurredAtUtc"`
	Source        string    `json:"source"`
}

type LineInput struct {
	Concept    string  `json:"concept"`
	Quantity   float64 `json:"quantity"`
	UnitAmount float64 `json:"unitAmount"`
}

type Input struct {
	BillNumber   string      `json:"billNumber"`
	IssuedAt     time.Time   `json:"issuedAt"`
	CustomerName string      `json:"customerName"`
	Currency     string      `json:"currency"`
	Tax          float64     `json:"tax"`
	Lines        []LineInput `json:"lines"`
}

type Result struct {
	ID         int64     `json:"id"`
	BillNumber string    `json:"billNumber"`
	IssuedAt   time.Time `json:"issuedAt"`
	Subtotal   float64   `json:"subtotal"`
	Tax        float64   `json:"tax"`
	Total      float64   `json:"total"`
	Currency   string    `json:"currency"`
}

type Service struct {
	repo      bill.Repository
	publisher IntegrationEventPublisher
}

func NewService(repo bill.Repository, publisher IntegrationEventPublisher) *Service {
	return &Service{repo: repo, publisher: publisher}
}

func (s *Service) Execute(ctx context.Context, input Input) (Result, error) {
	exists, err := s.repo.ExistsByBillNumber(ctx, input.BillNumber)
	if err != nil {
		return Result{}, err
	}
	if exists {
		return Result{}, &apperrors.ConflictError{
			Message: "Bill number '" + input.BillNumber + "' already exists.",
		}
	}

	lines := make([]bill.NewBillLineInput, 0, len(input.Lines))
	for _, line := range input.Lines {
		lines = append(lines, bill.NewBillLineInput{
			Concept:    line.Concept,
			Quantity:   line.Quantity,
			UnitAmount: line.UnitAmount,
		})
	}

	newBill, err := bill.CreateNewBill(bill.NewBillInput{
		BillNumber:   input.BillNumber,
		IssuedAt:     input.IssuedAt,
		CustomerName: input.CustomerName,
		Currency:     input.Currency,
		Tax:          input.Tax,
		Lines:        lines,
	})
	if err != nil {
		return Result{}, err
	}

	createdID, err := s.repo.Create(ctx, newBill)
	if err != nil {
		var pgErr *pgconn.PgError
		if errorsAsPgUnique(err, &pgErr) {
			return Result{}, &apperrors.ConflictError{
				Message: "Bill number '" + input.BillNumber + "' already exists.",
			}
		}
		return Result{}, err
	}

	event := BillCreatedEvent{
		BillID:        createdID,
		BillNumber:    newBill.BillNumber,
		IssuedAt:      newBill.IssuedAt.Format("2006-01-02"),
		Subtotal:      newBill.Subtotal,
		Tax:           newBill.Tax,
		Total:         newBill.Total,
		Currency:      newBill.Currency,
		OccurredAtUTC: time.Now().UTC(),
		Source:        "go-api",
	}
	if err := s.publisher.PublishBillCreated(ctx, event); err != nil {
		return Result{}, err
	}

	return Result{
		ID:         createdID,
		BillNumber: newBill.BillNumber,
		IssuedAt:   newBill.IssuedAt,
		Subtotal:   newBill.Subtotal,
		Tax:        newBill.Tax,
		Total:      newBill.Total,
		Currency:   newBill.Currency,
	}, nil
}

func errorsAsPgUnique(err error, pgErr **pgconn.PgError) bool {
	if !errors.As(err, pgErr) {
		return false
	}
	return (*pgErr).Code == "23505"
}
