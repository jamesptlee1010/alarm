package com.james.mathwakealarm.math

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

class MathQuestionGenerator(private val random: Random = Random.Default) {
    fun generate(excludingPrompt: String? = null): MathQuestion {
        repeat(100) {
            val question = when (Operator.entries.random(random)) {
                Operator.ADD -> addition()
                Operator.SUBTRACT -> subtraction()
                Operator.MULTIPLY -> multiplication()
                Operator.DIVIDE -> division()
            }
            if (question.prompt != excludingPrompt) return question
        }
        return addition()
    }

    private fun addition(): MathQuestion {
        val left = random.nextInt(10, 100)
        val right = random.nextInt(10, 100)
        return MathQuestion(left, Operator.ADD, right, BigDecimal(left + right))
    }

    private fun subtraction(): MathQuestion {
        val first = random.nextInt(20, 120)
        val second = random.nextInt(10, first + 1)
        return MathQuestion(first, Operator.SUBTRACT, second, BigDecimal(first - second))
    }

    private fun multiplication(): MathQuestion {
        val left = random.nextInt(2, 13)
        val right = random.nextInt(2, 13)
        return MathQuestion(left, Operator.MULTIPLY, right, BigDecimal(left * right))
    }

    private fun division(): MathQuestion {
        repeat(100) {
            val divisor = random.nextInt(2, 13)
            val answerTenths = random.nextInt(20, 201)
            val productTenths = divisor * answerTenths
            if (productTenths % 10 == 0) {
                val numerator = productTenths / 10
                val answer = BigDecimal(answerTenths)
                    .divide(BigDecimal.TEN, 1, RoundingMode.UNNECESSARY)
                    .stripTrailingZeros()
                return MathQuestion(numerator, Operator.DIVIDE, divisor, answer)
            }
        }

        val divisor = random.nextInt(2, 13)
        val answer = random.nextInt(2, 21)
        return MathQuestion(divisor * answer, Operator.DIVIDE, divisor, BigDecimal(answer))
    }
}
