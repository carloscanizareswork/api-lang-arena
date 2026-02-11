package http

import (
	"context"
	"database/sql"
	"encoding/json"
	"net/http"
	"time"

	billlist "go-bills-api/internal/application/bill/list"
)

type Handler struct {
	db      *sql.DB
	service *billlist.Service
}

type billResponse struct {
	ID         int64   `json:"id"`
	BillNumber string  `json:"billNumber"`
	IssuedAt   string  `json:"issuedAt"`
	Total      float64 `json:"total"`
	Currency   string  `json:"currency"`
}

func NewHandler(db *sql.DB, service *billlist.Service) *Handler {
	return &Handler{db: db, service: service}
}

func (h *Handler) RegisterRoutes(mux *http.ServeMux) {
	mux.HandleFunc("/", h.root)
	mux.HandleFunc("/bills-minimal", h.getBillsMinimal)
	mux.HandleFunc("/bills", h.getBillsDDD)
}

func (h *Handler) root(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, map[string]string{"service": "go-bills-api", "status": "ok"})
}

func (h *Handler) getBillsMinimal(w http.ResponseWriter, r *http.Request) {
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
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
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
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
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
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	writeJSON(w, http.StatusOK, out)
}

func (h *Handler) getBillsDDD(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()

	items, err := h.service.Execute(ctx)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
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
}

func writeJSON(w http.ResponseWriter, code int, payload any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	_ = json.NewEncoder(w).Encode(payload)
}
