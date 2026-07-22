package com.james.mathwakealarm.math

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathQuestionGeneratorTest {
    @Test
    fun generatedAnswersValidate() {
        val generator = MathQuestionGenerator(Random(42))
        repeat(1_000) {
            val question = generator.generate()
            assertTrue(question.isCorrect(question.answer.toPlainString()))
        }
    }

    @Test
    fun commaDecimalIsAccepted() {
        val question = MathQuestion(64, Operator.DIVIDE, 5, "12.8".toBigDecimal())
        assertTrue(question.isCorrect("12,8"))
    }
}
