package com.james.mathwakealarm.math

import java.math.BigDecimal

data class MathQuestion(
    val left: Int,
    val operator: Operator,
    val right: Int,
    val answer: BigDecimal,
) {
    val prompt: String = "$left ${operator.symbol} $right"

    fun isCorrect(candidate: String): Boolean {
        val normalized = candidate.trim().replace(',', '.')
        val submitted = normalized.toBigDecimalOrNull() ?: return false
        return submitted.compareTo(answer) == 0
    }
}

enum class Operator(val symbol: String) {
    ADD("+"),
    SUBTRACT("−"),
    MULTIPLY("×"),
    DIVIDE("÷"),
}
