package http

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"strings"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
	billcreate "go-bills-api/internal/application/bill/create"
	billlist "go-bills-api/internal/application/bill/list"
	apperrors "go-bills-api/internal/application/errors"
	"go-bills-api/internal/domain/bill"
)

type Handler struct {
	db            *sql.DB
	listService   *billlist.Service
	createService *billcreate.Service
}

type billResponse struct {
	ID         int64   `json:"id"`
	BillNumber string  `json:"billNumber"`
	IssuedAt   string  `json:"issuedAt"`
	Total      float64 `json:"total"`
	Currency   string  `json:"currency"`
}

type createBillLineRequest struct {
	Concept    string  `json:"concept"`
	Quantity   float64 `json:"quantity"`
	UnitAmount float64 `json:"unitAmount"`
}

type createBillRequest struct {
	BillNumber   string                  `json:"billNumber"`
	IssuedAt     string                  `json:"issuedAt"`
	CustomerName string                  `json:"customerName"`
	Currency     string                  `json:"currency"`
	Tax          float64                 `json:"tax"`
	Lines        []createBillLineRequest `json:"lines"`
}

type createBillResponse struct {
	ID         int64   `json:"id"`
	BillNumber string  `json:"billNumber"`
	IssuedAt   string  `json:"issuedAt"`
	Subtotal   float64 `json:"subtotal"`
	Tax        float64 `json:"tax"`
	Total      float64 `json:"total"`
	Currency   string  `json:"currency"`
}

func NewHandler(db *sql.DB, listService *billlist.Service, createService *billcreate.Service) *Handler {
	return &Handler{
		db:            db,
		listService:   listService,
		createService: createService,
	}
}

func (h *Handler) RegisterRoutes(mux *http.ServeMux) {
	mux.HandleFunc("/", h.wrap(h.root))
	mux.HandleFunc("/bills-minimal", h.wrap(h.getBillsMinimal))
	mux.HandleFunc("/bills", h.wrap(h.billsRoute))
}

func (h *Handler) root(w http.ResponseWriter, r *http.Request) error {
	if r.Method != http.MethodGet {
		return methodNotAllowed(r.Method, http.MethodGet)
	}
	writeJSON(w, http.StatusOK, map[string]string{"service": "go-bills-api", "status": "ok"})
	return nil
}

func (h *Handler) billsRoute(w http.ResponseWriter, r *http.Request) error {
	switch r.Method {
	case http.MethodGet:
		return h.getBillsDDD(w, r)
	case http.MethodPost:
		return h.createBillDDD(w, r)
	default:
		return methodNotAllowed(r.Method, http.MethodGet, http.MethodPost)
	}
}

func (h *Handler) getBillsMinimal(w http.ResponseWriter, r *http.Request) error {
	if r.Method != http.MethodGet {
		return methodNotAllowed(r.Method, http.MethodGet)
	}

	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()

	const query = `
		SELECT b.id,
		       b.bill_number,
		       b.issued_at,
		       COALESCE(SUM(bl.line_amount), 0) + b.tax AS total,
		       b.currency
		FROM bill b
		LEFT JOIN bill_line bl ON bl.bill_id = b.id
		GROUP BY b.id, b.bill_number, b.issued_at, b.tax, b.currency
		ORDER BY b.id;
	`

	rows, err := h.db.QueryContext(ctx, query)
	if err != nil {
		return err
	}
	defer rows.Close()

	out := make([]billResponse, 0)
	for rows.Next() {
		var id int64
		var billNumber string
		var issuedAt time.Time
		var total float64
		var currency string
		if err := rows.Scan(&id, &billNumber, &issuedAt, &total, &currency); err != nil {
			return err
		}
		out = append(out, billResponse{
			ID:         id,
			BillNumber: billNumber,
			IssuedAt:   issuedAt.Format("2006-01-02"),
			Total:      total,
			Currency:   currency,
		})
	}
	if err := rows.Err(); err != nil {
		return err
	}

	writeJSON(w, http.StatusOK, out)
	return nil
}

func (h *Handler) getBillsDDD(w http.ResponseWriter, r *http.Request) error {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()

	items, err := h.listService.Execute(ctx)
	if err != nil {
		return err
	}

	out := make([]billResponse, 0, len(items))
	for _, item := range items {
		out = append(out, billResponse{
			ID:         item.ID,
			BillNumber: item.BillNumber,
			IssuedAt:   item.IssuedAt.Format("2006-01-02"),
			Total:      item.Total,
			Currency:   item.Currency,
		})
	}

	writeJSON(w, http.StatusOK, out)
	return nil
}

func (h *Handler) createBillDDD(w http.ResponseWriter, r *http.Request) error {
	ctx, cancel := context.WithTimeout(r.Context(), 10*time.Second)
	defer cancel()

	var req createBillRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		validationErr := apperrors.NewValidationError()
		validationErr.Add("request", "Invalid JSON payload.")
		return validationErr
	}

	if validationErr := validateCreateBillRequest(req); validationErr != nil {
		return validationErr
	}

	issuedAt, _ := time.Parse("2006-01-02", req.IssuedAt)
	lines := make([]billcreate.LineInput, 0, len(req.Lines))
	for _, line := range req.Lines {
		lines = append(lines, billcreate.LineInput{
			Concept:    line.Concept,
			Quantity:   line.Quantity,
			UnitAmount: line.UnitAmount,
		})
	}

	result, err := h.createService.Execute(ctx, billcreate.Input{
		BillNumber:   req.BillNumber,
		IssuedAt:     issuedAt,
		CustomerName: req.CustomerName,
		Currency:     req.Currency,
		Tax:          req.Tax,
		Lines:        lines,
	})
	if err != nil {
		return err
	}

	writeJSON(w, http.StatusCreated, createBillResponse{
		ID:         result.ID,
		BillNumber: result.BillNumber,
		IssuedAt:   result.IssuedAt.Format("2006-01-02"),
		Subtotal:   result.Subtotal,
		Tax:        result.Tax,
		Total:      result.Total,
		Currency:   result.Currency,
	})
	return nil
}

func (h *Handler) wrap(fn func(http.ResponseWriter, *http.Request) error) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if rec := recover(); rec != nil {
				writeProblem(w, http.StatusInternalServerError, "An unexpected error occurred.", nil)
			}
		}()

		if err := fn(w, r); err != nil {
			h.writeError(w, err)
		}
	}
}

func (h *Handler) writeError(w http.ResponseWriter, err error) {
	var appValidationErr *apperrors.ValidationError
	if errors.As(err, &appValidationErr) {
		writeProblem(w, http.StatusBadRequest, "Validation failed", appValidationErr.Fields)
		return
	}

	var domainValidationErr *bill.ValidationErrors
	if errors.As(err, &domainValidationErr) {
		fields := map[string][]string{}
		for _, item := range domainValidationErr.Items {
			fields[item.Field] = append(fields[item.Field], item.Message)
		}
		writeProblem(w, http.StatusBadRequest, "Validation failed", fields)
		return
	}

	var conflictErr *apperrors.ConflictError
	if errors.As(err, &conflictErr) {
		writeProblem(w, http.StatusConflict, conflictErr.Message, nil)
		return
	}

	var amqpErr *amqp.Error
	if errors.As(err, &amqpErr) {
		writeProblem(w, http.StatusServiceUnavailable, "Message broker error.", nil)
		return
	}

	var methodErr *methodNotAllowedError
	if errors.As(err, &methodErr) {
		w.Header().Set("Allow", strings.Join(methodErr.allowedMethods, ", "))
		writeProblem(w, http.StatusMethodNotAllowed, methodErr.Error(), nil)
		return
	}

	writeProblem(w, http.StatusInternalServerError, "An unexpected error occurred.", nil)
}

func writeJSON(w http.ResponseWriter, code int, payload any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	_ = json.NewEncoder(w).Encode(payload)
}

func writeProblem(w http.ResponseWriter, status int, title string, fieldErrors map[string][]string) {
	writeJSON(w, status, map[string]any{
		"type":   "about:blank",
		"title":  title,
		"status": status,
		"errors": fieldErrors,
	})
}

func validateCreateBillRequest(req createBillRequest) *apperrors.ValidationError {
	errs := apperrors.NewValidationError()

	if strings.TrimSpace(req.BillNumber) == "" {
		errs.Add("billNumber", "Bill number is required.")
	}
	if strings.TrimSpace(req.CustomerName) == "" {
		errs.Add("customerName", "Customer name is required.")
	}
	if len(strings.TrimSpace(req.Currency)) != 3 {
		errs.Add("currency", "Currency must be a 3-letter ISO code.")
	}
	if req.Tax < 0 {
		errs.Add("tax", "Tax cannot be negative.")
	}
	issuedAt, err := time.Parse("2006-01-02", req.IssuedAt)
	if err != nil {
		errs.Add("issuedAt", "Issued date must use format YYYY-MM-DD.")
	}
	if issuedAt.IsZero() {
		errs.Add("issuedAt", "Issued date is required.")
	}
	if len(req.Lines) == 0 {
		errs.Add("lines", "At least one line is required.")
	}
	for _, line := range req.Lines {
		if strings.TrimSpace(line.Concept) == "" {
			errs.Add("lines.concept", "Line concept is required.")
		}
		if line.Quantity <= 0 {
			errs.Add("lines.quantity", "Line quantity must be greater than zero.")
		}
		if line.UnitAmount < 0 {
			errs.Add("lines.unitAmount", "Line unit amount cannot be negative.")
		}
	}

	if errs.HasErrors() {
		return errs
	}
	return nil
}

type methodNotAllowedError struct {
	method         string
	allowedMethods []string
}

func (e *methodNotAllowedError) Error() string {
	return fmt.Sprintf("Method '%s' is not allowed.", e.method)
}

func methodNotAllowed(method string, allowed ...string) error {
	return &methodNotAllowedError{
		method:         method,
		allowedMethods: allowed,
	}
}
