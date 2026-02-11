package bill

type FieldError struct {
	Field   string
	Message string
}

type ValidationErrors struct {
	Items []FieldError
}

func (e *ValidationErrors) Error() string {
	return "domain validation failed"
}

func NewValidationErrors() *ValidationErrors {
	return &ValidationErrors{Items: make([]FieldError, 0)}
}

func (e *ValidationErrors) Add(field string, message string) {
	e.Items = append(e.Items, FieldError{Field: field, Message: message})
}

func (e *ValidationErrors) HasErrors() bool {
	return len(e.Items) > 0
}
