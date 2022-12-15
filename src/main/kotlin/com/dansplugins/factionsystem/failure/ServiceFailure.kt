package com.dansplugins.factionsystem.failure

data class ServiceFailure(
    val type: ServiceFailureType,
    val message: String,
    val cause: Throwable
)

enum class ServiceFailureType {
    NOT_FOUND,
    BAD_REQUEST,
    BAD_RESPONSE,
    AUTHENTICATION_REQUIRED,
    AUTHORIZATION,
    RULES_VIOLATION,
    DUPLICATE,
    CONFLICT,
    GENERAL
}
