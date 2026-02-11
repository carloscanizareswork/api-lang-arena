package bill

import (
	"math"
	"strings"
	"time"
)

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

type NewBillLine struct {
	LineNo     int
	Concept    string
	Quantity   float64
	UnitAmount float64
	LineAmount float64
}

type NewBill struct {
	BillNumber   string
	IssuedAt     time.Time
	CustomerName string
	Currency     string
	Subtotal     float64
	Tax          float64
	Total        float64
	Lines        []NewBillLine
}

type NewBillInput struct {
	BillNumber   string
	IssuedAt     time.Time
	CustomerName string
	Currency     string
	Tax          float64
	Lines        []NewBillLineInput
}

type NewBillLineInput struct {
	Concept    string
	Quantity   float64
	UnitAmount float64
}

func CreateNewBill(input NewBillInput) (NewBill, error) {
	errs := NewValidationErrors()

	billNumber := strings.TrimSpace(input.BillNumber)
	customerName := strings.TrimSpace(input.CustomerName)
	currency := strings.ToUpper(strings.TrimSpace(input.Currency))

	if billNumber == "" {
		errs.Add("billNumber", "Bill number is required.")
	}
	if len(billNumber) > 50 {
		errs.Add("billNumber", "Bill number max length is 50.")
	}
	if customerName == "" {
		errs.Add("customerName", "Customer name is required.")
	}
	if len(customerName) > 200 {
		errs.Add("customerName", "Customer name max length is 200.")
	}
	if len(currency) != 3 {
		errs.Add("currency", "Currency must be a 3-letter ISO code.")
	}
	if input.IssuedAt.IsZero() {
		errs.Add("issuedAt", "Issued date is required.")
	}
	if input.Tax < 0 {
		errs.Add("tax", "Tax cannot be negative.")
	}
	if len(input.Lines) == 0 {
		errs.Add("lines", "At least one line is required.")
	}

	lines := make([]NewBillLine, 0, len(input.Lines))
	subtotal := 0.0
	for idx, line := range input.Lines {
		billLine, lineErr := createLine(idx+1, line)
		if lineErr != nil {
			if validationErrs, ok := lineErr.(*ValidationErrors); ok {
				for _, item := range validationErrs.Items {
					errs.Add(item.Field, item.Message)
				}
			}
			continue
		}
		lines = append(lines, billLine)
		subtotal += billLine.LineAmount
	}

	if errs.HasErrors() {
		return NewBill{}, errs
	}

	subtotal = roundMoney(subtotal)
	tax := roundMoney(input.Tax)

	return NewBill{
		BillNumber:   billNumber,
		IssuedAt:     input.IssuedAt,
		CustomerName: customerName,
		Currency:     currency,
		Subtotal:     subtotal,
		Tax:          tax,
		Total:        roundMoney(subtotal + tax),
		Lines:        lines,
	}, nil
}

func createLine(lineNo int, input NewBillLineInput) (NewBillLine, error) {
	errs := NewValidationErrors()

	concept := strings.TrimSpace(input.Concept)
	if concept == "" {
		errs.Add("lines.concept", "Line concept is required.")
	}
	if len(concept) > 200 {
		errs.Add("lines.concept", "Line concept max length is 200.")
	}
	if input.Quantity <= 0 {
		errs.Add("lines.quantity", "Line quantity must be greater than zero.")
	}
	if input.UnitAmount < 0 {
		errs.Add("lines.unitAmount", "Line unit amount cannot be negative.")
	}

	if errs.HasErrors() {
		return NewBillLine{}, errs
	}

	quantity := roundMoney(input.Quantity)
	unitAmount := roundMoney(input.UnitAmount)
	lineAmount := roundMoney(quantity * unitAmount)

	return NewBillLine{
		LineNo:     lineNo,
		Concept:    concept,
		Quantity:   quantity,
		UnitAmount: unitAmount,
		LineAmount: lineAmount,
	}, nil
}

func roundMoney(value float64) float64 {
	return math.Round(value*100) / 100
}
