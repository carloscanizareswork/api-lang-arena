package bill

import "context"

type Repository interface {
	List(ctx context.Context) ([]Bill, error)
	ExistsByBillNumber(ctx context.Context, billNumber string) (bool, error)
	Create(ctx context.Context, newBill NewBill) (int64, error)
}
