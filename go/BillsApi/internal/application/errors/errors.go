package errors

type ValidationError struct {
	Fields map[string][]string
}

func (e *ValidationError) Error() string {
	return "validation failed"
}

func NewValidationError() *ValidationError {
	return &ValidationError{
		Fields: map[string][]string{},
	}
}

func (e *ValidationError) Add(field string, message string) {
	e.Fields[field] = append(e.Fields[field], message)
}

func (e *ValidationError) HasErrors() bool {
	return len(e.Fields) > 0
}

type ConflictError struct {
	Message string
}

func (e *ConflictError) Error() string {
	return e.Message
}
