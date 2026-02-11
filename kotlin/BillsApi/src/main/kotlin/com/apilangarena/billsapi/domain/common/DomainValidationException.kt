package com.apilangarena.billsapi.domain.common

class DomainValidationException(val errors: Map<String, List<String>>) : RuntimeException("Domain validation failed")
