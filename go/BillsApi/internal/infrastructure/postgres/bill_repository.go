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
		ID            int64     `gorm:"column:id"`
		BillNumber    string    `gorm:"column:bill_number"`
		IssuedAt      time.Time `gorm:"column:issued_at"`
		ComputedTotal float64   `gorm:"column:computed_total"`
		Currency      string    `gorm:"column:currency"`
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

func (r *BillRepository) ExistsByBillNumber(ctx context.Context, billNumber string) (bool, error) {
	var count int64
	if err := r.db.WithContext(ctx).
		Table("bill").
		Where("bill_number = ?", billNumber).
		Count(&count).Error; err != nil {
		return false, err
	}
	return count > 0, nil
}

func (r *BillRepository) Create(ctx context.Context, newBill bill.NewBill) (int64, error) {
	type billRow struct {
		ID           int64     `gorm:"column:id"`
		BillNumber   string    `gorm:"column:bill_number"`
		IssuedAt     time.Time `gorm:"column:issued_at"`
		CustomerName string    `gorm:"column:customer_name"`
		Subtotal     float64   `gorm:"column:subtotal"`
		Tax          float64   `gorm:"column:tax"`
		Currency     string    `gorm:"column:currency"`
	}
	type billLineRow struct {
		BillID     int64   `gorm:"column:bill_id"`
		LineNo     int     `gorm:"column:line_no"`
		Concept    string  `gorm:"column:concept"`
		Quantity   float64 `gorm:"column:quantity"`
		UnitAmount float64 `gorm:"column:unit_amount"`
		LineAmount float64 `gorm:"column:line_amount"`
	}

	row := billRow{
		BillNumber:   newBill.BillNumber,
		IssuedAt:     newBill.IssuedAt,
		CustomerName: newBill.CustomerName,
		Subtotal:     newBill.Subtotal,
		Tax:          newBill.Tax,
		Currency:     newBill.Currency,
	}

	if err := r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		if err := tx.Table("bill").Create(&row).Error; err != nil {
			return err
		}

		lines := make([]billLineRow, 0, len(newBill.Lines))
		for _, line := range newBill.Lines {
			lines = append(lines, billLineRow{
				BillID:     row.ID,
				LineNo:     line.LineNo,
				Concept:    line.Concept,
				Quantity:   line.Quantity,
				UnitAmount: line.UnitAmount,
				LineAmount: line.LineAmount,
			})
		}

		if err := tx.Table("bill_line").Create(&lines).Error; err != nil {
			return err
		}
		return nil
	}); err != nil {
		return 0, err
	}

	return row.ID, nil
}
