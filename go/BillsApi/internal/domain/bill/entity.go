package bill

import "time"

type Money struct {
	Amount   float64
	Currency string
}

type Bill struct {
	ID         int64
	BillNumber string
	IssuedAt   time.Time
	Total      Money
}
