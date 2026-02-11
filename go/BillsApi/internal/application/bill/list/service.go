package list

import (
	"context"
	"time"

	"go-bills-api/internal/domain/bill"
)

type BillDTO struct {
	ID         int64     `json:"id"`
	BillNumber string    `json:"billNumber"`
	IssuedAt   time.Time `json:"issuedAt"`
	Total      float64   `json:"total"`
	Currency   string    `json:"currency"`
}

type Service struct {
	repo bill.Repository
}

func NewService(repo bill.Repository) *Service {
	return &Service{repo: repo}
}

func (s *Service) Execute(ctx context.Context) ([]BillDTO, error) {
	items, err := s.repo.List(ctx)
	if err != nil {
		return nil, err
	}

	result := make([]BillDTO, 0, len(items))
	for _, item := range items {
		result = append(result, BillDTO{
			ID:         item.ID,
			BillNumber: item.BillNumber,
			IssuedAt:   item.IssuedAt,
			Total:      item.Total.Amount,
			Currency:   item.Total.Currency,
		})
	}
	return result, nil
}
