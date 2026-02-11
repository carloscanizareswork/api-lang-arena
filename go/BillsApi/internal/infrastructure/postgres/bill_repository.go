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
		ComputedTotal float64 `gorm:"column:computed_total"`
		Currency   string    `gorm:"column:currency"`
	}

	var rows []billRow
	if err := r.db.WithContext(ctx).
		Table("bill").
		Select("bill.id, bill.bill_number, bill.issued_at, COALESCE(SUM(bl.line_amount), 0) + bill.tax AS computed_total, bill.currency").
		Joins("LEFT JOIN bill_line bl ON bl.bill_id = bill.id").
		Group("bill.id, bill.bill_number, bill.issued_at, bill.tax, bill.currency").
		Order("bill.id").
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
				Amount:   row.ComputedTotal,
				Currency: row.Currency,
			},
		})
	}
	return items, nil
}
