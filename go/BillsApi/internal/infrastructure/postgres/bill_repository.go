package postgres

import (
	"context"
	"time"

	"go-bills-api/internal/domain/bill"
	"gorm.io/gorm"
)

type BillRepository struct {
	db *gorm.DB
}

func NewBillRepository(db *gorm.DB) *BillRepository {
	return &BillRepository{db: db}
}

func (r *BillRepository) List(ctx context.Context) ([]bill.Bill, error) {
	type billRow struct {
		ID         int64     `gorm:"column:id"`
		BillNumber string    `gorm:"column:bill_number"`
		IssuedAt   time.Time `gorm:"column:issued_at"`
		Total      float64   `gorm:"column:total"`
		Currency   string    `gorm:"column:currency"`
	}

	var rows []billRow
	if err := r.db.WithContext(ctx).
		Table("bill").
		Select("id, bill_number, issued_at, total, currency").
		Order("id").
		Find(&rows).Error; err != nil {
		return nil, err
	}

	items := make([]bill.Bill, 0, len(rows))
	for _, row := range rows {
		items = append(items, bill.Bill{
			ID:         row.ID,
			BillNumber: row.BillNumber,
			IssuedAt:   row.IssuedAt,
			Total: bill.Money{
				Amount:   row.Total,
				Currency: row.Currency,
			},
		})
	}
	return items, nil
}
