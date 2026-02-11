package com.apilangarena.billsapi.presentation

import com.apilangarena.billsapi.application.common.ConflictException
import com.apilangarena.billsapi.domain.common.DomainValidationException
import org.springframework.amqp.AmqpException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ProblemResponse(
    val type: String = "about:blank",
    val title: String,
    val status: Int,
    val errors: Map<String, List<String>>? = null,
)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ProblemResponse> {
        val fieldErrors = ex.bindingResult
            .allErrors
            .groupBy { error ->
                when (error) {
                    is FieldError -> error.field
                    else -> error.objectName
                }
            }
            .mapValues { (_, errors) ->
                errors.mapNotNull { it.defaultMessage }.ifEmpty { listOf("Invalid value") }
            }

        return problem(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleInvalidJson(ex: HttpMessageNotReadableException): ResponseEntity<ProblemResponse> {
        return problem(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            mapOf("request" to listOf("Invalid JSON payload.")),
        )
    }

    @ExceptionHandler(DomainValidationException::class)
    fun handleDomainValidation(ex: DomainValidationException): ResponseEntity<ProblemResponse> {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", ex.errors)
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException): ResponseEntity<ProblemResponse> {
        return problem(HttpStatus.CONFLICT, ex.message ?: "Conflict", null)
    }

    @ExceptionHandler(AmqpException::class)
    fun handleAmqp(ex: AmqpException): ResponseEntity<ProblemResponse> {
        return problem(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Message broker error: ${ex.javaClass.simpleName}",
            null,
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnhandled(ex: Exception): ResponseEntity<ProblemResponse> {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", null)
    }

    private fun problem(
        status: HttpStatus,
        title: String,
        errors: Map<String, List<String>>?,
    ): ResponseEntity<ProblemResponse> {
        return ResponseEntity
            .status(status)
            .body(
                ProblemResponse(
                    title = title,
                    status = status.value(),
                    errors = errors,
                )
            )
    }
}
