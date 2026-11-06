package com.allai.app.security

import java.util.regex.Pattern

object LogRedactor {
    private val API_KEY_PATTERN = Pattern.compile(
        "(?i)(api[_-]?key|apikey|secret|token|password|passwd|pwd|auth[_-]?token|bearer)\\s*[:=]\\s*[\\S]+",
        Pattern.CASE_INSENSITIVE
    )
    private val NVAPI_PATTERN = Pattern.compile("nvapi-[A-Za-z0-9_-]+")
    private val SK_PATTERN = Pattern.compile("sk-[A-Za-z0-9_-]+")
    private val JWT_PATTERN = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
    private val EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val CREDIT_CARD_PATTERN = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b")

    /**
     * Redacts sensitive information from log output.
     * - API keys, tokens, passwords, secrets
     * - NVIDIA API keys (nvapi-...)
     * - OpenAI-style keys (sk-...)
     * - JWT tokens
     * - Email addresses
     * - Credit card numbers
     */
    fun redact(text: String): String {
        if (text.isBlank()) return text

        var result = text

        // Redact key=value patterns for sensitive keys
        result = API_KEY_PATTERN.matcher(result).replaceAll("$1=***REDACTED***")

        // Redact nvapi keys
        result = NVAPI_PATTERN.matcher(result).replaceAll("***NVAPI_KEY_REDACTED***")

        // Redact sk- keys
        result = SK_PATTERN.matcher(result).replaceAll("***SK_KEY_REDACTED***")

        // Redact JWT tokens
        result = JWT_PATTERN.matcher(result).replaceAll("***JWT_REDACTED***")

        // Redact emails
        result = EMAIL_PATTERN.matcher(result).replaceAll("***EMAIL_REDACTED***")

        // Redact credit card numbers
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("***CARD_REDACTED***")

        return result
    }

    fun redactMap(map: Map<String, Any?>): Map<String, Any?> {
        return map.mapValues { (_, value) ->
            when (value) {
                is String -> redact(value)
                else -> value.toString().let { redact(it) }
            }
        }
    }
}