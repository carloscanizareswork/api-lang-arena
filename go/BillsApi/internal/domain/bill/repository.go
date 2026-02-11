package bill

import "context"

type Repository interface {
	List(ctx context.Context) ([]Bill, error)
}
